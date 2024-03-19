package io.github.bookiosk.wrapper;

import io.github.bookiosk.callback.DefaultCallback;
import io.github.bookiosk.callback.ICallback;
import io.github.bookiosk.entity.Builder;
import io.github.bookiosk.entity.ExecuteResult;
import io.github.bookiosk.enums.ResultState;
import io.github.bookiosk.exception.SkippedException;
import io.github.bookiosk.executor.timer.SystemClock;
import io.github.bookiosk.worker.IWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 用于组合了worker和callback,一对一的包装,是一个最小的调度单元。
 * 通过编排wrapper之间的关系,达到组合各个worker顺序的目的。
 *
 * @author bookiosk
 */
public class WorkerWrapper<T, V> {

    /**
     * 该wrapper的唯一标识
     */
    private final String id;
    /**
     * worker将来要处理的param
     */
    private T param;
    private final IWorker<T, V> worker;
    private final ICallback<T, V> callback;
    /**
     * 在自己后面的wrapper，如果没有，自己就是末尾；如果有一个，就是串行；如果有多个，有几个就需要开几个线程
     */
    private List<WorkerWrapper<?, ?>> nextWrappers;
    /**
     * 依赖的wrappers，有2种情况，1:必须依赖的全部完成后，才能执行自己 2:依赖的任何一个、多个完成了，就可以执行自己
     * 通过must字段来控制是否依赖项必须完成
     * DependWrapper 是在WorkerWrapper增加了是否必须依赖的属性后的封装个体
     */
    private List<DependWrapper> dependWrappers;
    /**
     * 标记该事件是否已经被处理过了，譬如已经超时返回false了，后续rpc又收到返回值了，则不再二次回调
     * 使用AtomicInteger是为了能保证高并发情况state的原子性
     *  <p>
     * 1-finish, 2-error, 3-working
     */
    private final AtomicInteger state = new AtomicInteger(INIT);

    private static final int INIT = 0;
    private static final int FINISH = 1;
    private static final int ERROR = 2;
    private static final int WORKING = 3;

    /**
     * 该map存放所有wrapper的id和wrapper映射
     */
    private Map<String, WorkerWrapper> forParamUseWrappers;
    /**
     * 也是个钩子变量，用来存临时的结果
     */
    private volatile ExecuteResult<V> executeResult = ExecuteResult.defaultResult();
    /**
     * 是否在执行自己前，去校验nextWrapper的执行结果<p>
     * 1   4
     * -------3
     * 2
     * 如这种在4执行前，可能3已经执行完毕了（被2执行完后触发的），那么4就没必要执行了。
     * 注意，该属性仅在nextWrapper数量<=1时有效，>1时的情况是不存在的
     */
    private volatile boolean needCheckNextWrapperResult = true;

    private static final Logger logger = LoggerFactory.getLogger(WorkerWrapper.class);

    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

    private final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();
    private final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();

    //—————————————————————— 基础属性部分的获取 ——————————————————————

    private int getState() {
        readLock.lock();
        try {
            return state.get();
        } finally {
            readLock.unlock();
        }
    }

    public ExecuteResult<V> getExecuteResult() {
        return executeResult;
    }

    public List<WorkerWrapper<?, ?>> getNextWrappers() {
        return nextWrappers;
    }

    private void setNeedCheckNextWrapperResult(boolean needCheckNextWrapperResult) {
        this.needCheckNextWrapperResult = needCheckNextWrapperResult;
    }

    public void setParam(T param) {
        this.param = param;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkerWrapper<?, ?> that = (WorkerWrapper<?, ?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    //—————————————————————— 构造器部分 ——————————————————————//

    private WorkerWrapper(String id, IWorker<T, V> worker, T param) {
        this(id, worker, param, new DefaultCallback<>());
    }

    private WorkerWrapper(String id, IWorker<T, V> worker, T param, ICallback<T, V> callback) {
        if (worker == null) {
            throw new NullPointerException("worker is null");
        }
        this.id = id;
        this.worker = worker;
        this.param = param;
        this.callback = callback != null ? callback : new DefaultCallback<>();
    }

    //—————————————————————— 新增方法部分 ——————————————————————//

    /**
     * @param executorService     执行用的线程池
     * @param remainTime          剩余时间
     * @param forParamUseWrappers
     */
    public void work(ExecutorService executorService, long remainTime, Map<String, WorkerWrapper> forParamUseWrappers) {
        work(executorService, null, remainTime, forParamUseWrappers);
    }

    private void work(ExecutorService executorService, WorkerWrapper fromWrapper, long remainTime, Map<String, WorkerWrapper> forParamUseWrappers) {
        this.forParamUseWrappers = forParamUseWrappers;
        //将自己放到所有wrapper的集合里去
        forParamUseWrappers.put(id, this);
        long now = SystemClock.now();
        if (remainTime <= 0) {
            fastFail(INIT, null);
            beginNext(executorService, now, remainTime);
            return;
        }
        // 如果已经执行过了就直接执行下一个
        // 因为可能自己上依赖步骤有多个,然后其中一个依赖步骤执行完成了轮到自己执行了,另外一个依赖步骤执行完毕,又进来该方法就不执行了
        // 如果想要设置可重复执行,建议配置成依赖步骤全执行完再执行的策略
        if (getState() == FINISH || getState() == ERROR) {
            beginNext(executorService, now, remainTime);
            return;
        }

        //如果在执行前需要校验nextWrapper的状态
        if (needCheckNextWrapperResult) {
            //如果自己的next链上有已经出结果或已经开始执行的任务了，自己就不用继续了
            if (!checkNextWrapperResult()) {
                fastFail(INIT, new SkippedException());
                beginNext(executorService, now, remainTime);
                return;
            }
        }
        //如果没有前置步骤依赖，说明自己就是第一批要执行的
        if (dependWrappers == null || dependWrappers.isEmpty()) {
            fire();
            beginNext(executorService, now, remainTime);
        } else if (dependWrappers.size() == 1) {
            //只有一个依赖则必须等依赖执行完成,不然你就开并行别开串行
            doDependsOneJob(fromWrapper);
            beginNext(executorService, now, remainTime);
        } else {
            //有多个依赖时
            doDependsJobs(executorService, dependWrappers, fromWrapper, now, remainTime);
        }
    }

    /**
     * 判断自己下游链路上，是否存在已经出结果的或已经开始执行的
     * 如果没有返回true，如果有返回false
     */
    private boolean checkNextWrapperResult() {
        //如果自己就是最后一个，或者后面有并行的多个，就返回true
        if (nextWrappers == null || nextWrappers.size() != 1) {
            return getState() == INIT;
        }
        WorkerWrapper<?, ?> nextWrapper = nextWrappers.get(0);
        //继续校验自己的next的状态
        return nextWrapper.getState() == INIT && nextWrapper.checkNextWrapperResult();
    }

    /**
     * 进行下一个任务
     */
    private void beginNext(ExecutorService executorService, long now, long remainTime) {
        //花费的时间
        long costTime = SystemClock.now() - now;
        if (nextWrappers == null) {
            return;
        }
        if (nextWrappers.size() == 1) {
            nextWrappers.get(0).work(executorService, WorkerWrapper.this, remainTime - costTime, forParamUseWrappers);
            return;
        }
        CompletableFuture[] futures = new CompletableFuture[nextWrappers.size()];
        for (int i = 0; i < nextWrappers.size(); i++) {
            int finalI = i;
            futures[i] = CompletableFuture.runAsync(() -> nextWrappers.get(finalI).work(executorService, WorkerWrapper.this, remainTime - costTime, forParamUseWrappers), executorService);
        }
        try {
            CompletableFuture.allOf(futures).get(remainTime - costTime, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("beginNext failed, error message : ", e);
        }
    }

    private void doDependsOneJob(WorkerWrapper dependWrapper) {
        // 如果前置结果超时了 设置超时结果返回
        if (ResultState.TIMEOUT == dependWrapper.getExecuteResult().getResultState()) {
            executeResult = defaultTimeOutResult();
            fastFail(INIT, null);
        } else if (ResultState.EXCEPTION == dependWrapper.getExecuteResult().getResultState()) {
            executeResult = defaultExResult(dependWrapper.getExecuteResult().getException());
            fastFail(INIT, null);
        } else {
            //前面任务正常完毕了，该自己了
            fire();
        }
    }

    private synchronized void doDependsJobs(ExecutorService executorService, List<DependWrapper> dependWrappers, WorkerWrapper fromWrapper, long now, long remainTime) {
        // 如果当前任务已经完成了，依赖的其他任务拿到锁再进来时，不需要执行下面的逻辑了。
        if (getState() != INIT) {
            return;
        }
        boolean nowDependIsMust = false;
        // 创建必须完成的上游wrapper集合
        Set<DependWrapper> mustWrapper = new HashSet<>();
        // 默认设置不强制依赖步骤
        for (DependWrapper dependWrapper : dependWrappers) {
            if (dependWrapper.isMust()) {
                mustWrapper.add(dependWrapper);
            }
            // 如果进入当前worker的fromWrapper在dependWrappers里面
            if (dependWrapper.getDependWrapper().equals(fromWrapper)) {
                nowDependIsMust = dependWrapper.isMust();
            }
        }
        // 如果全部是不必须的条件，那么只要到了这里，就执行自己。
        if (mustWrapper.isEmpty()) {
            // 前置步骤超时了,直接给自己强制设置为失败
            if (ResultState.TIMEOUT == fromWrapper.getExecuteResult().getResultState()) {
                fastFail(INIT, null);
            } else {
                fire();
            }
            beginNext(executorService, now, remainTime);
            return;
        }
        // 如果存在需要必须完成的(!mustWrapper.isEmpty())，且fromWrapper不是必须的，就什么也不干等到must的来再开始干活
        if (!nowDependIsMust) {
            return;
        }
        // 如果fromWrapper是必须的
        boolean existNoFinish = false;
        boolean hasError = false;
        // 先判断前面必须要执行的依赖任务的执行结果，如果有任何一个失败，那就不用走action了，直接给自己设置为失败，进行下一步就是了
        for (DependWrapper dependWrapper : mustWrapper) {
            WorkerWrapper<?, ?> workerWrapper = dependWrapper.getDependWrapper();
            ExecuteResult<?> tempWorkResult = workerWrapper.getExecuteResult();
            // 为null或者isWorking，说明它依赖的某个任务还没执行到或没执行完
            if (workerWrapper.getState() == INIT || workerWrapper.getState() == WORKING) {
                existNoFinish = true;
                break;
            }
            if (ResultState.TIMEOUT == tempWorkResult.getResultState()) {
                executeResult = defaultTimeOutResult();
                hasError = true;
                break;
            }
            if (ResultState.EXCEPTION == tempWorkResult.getResultState()) {
                executeResult = defaultExResult(workerWrapper.getExecuteResult().getException());
                hasError = true;
                break;
            }
        }
        // 只要有失败的
        if (hasError) {
            fastFail(INIT, null);
            beginNext(executorService, now, remainTime);
            return;
        }
        // 如果上游都没有失败，分为两种情况，一种是都finish了，一种是有的在working
        // 都finish的话
        if (!existNoFinish) {
            //上游都finish了，进行自己
            fire();
            beginNext(executorService, now, remainTime);
        }
    }


    /**
     * 执行自己的job.具体的执行是在另一个线程里,但判断阻塞超时是在work线程
     */
    private void fire() {
        //阻塞取结果
        executeResult = workerDoJob();
    }

    private boolean fastFail(int expectStage, Exception exception) {
        writeLock.lock();
        try {
            // 试图将它从expect状态,改成Error
            if (!compareAndSetState(expectStage, ERROR)) {
                return false;
            }
            // 尚未处理过结果
            if (checkIsNullResult()) {
                if (exception == null) {
                    executeResult = defaultTimeOutResult();
                } else {
                    executeResult = defaultExResult(exception);
                }
            }
            callback.result(false, param, executeResult);
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    private boolean fastSuccess(int expectStage, V resultValue) {
        writeLock.lock();
        try {
            // 如果状态不是在working,说明别的地方已经修改了
            // 是的话就设置FINISH状态并在后面处理executeResult值
            if (!compareAndSetState(expectStage, FINISH)) {
                writeLock.unlock();
                return false;
            }

            executeResult.setResultState(ResultState.SUCCESS);
            executeResult.setResult(resultValue);
            //回调成功
            callback.result(true, param, executeResult);
        } finally {
            writeLock.unlock();
        }
        return true;
    }

    /**
     * @return 当前work执行状态是否是默认状态
     */
    private boolean checkIsNullResult() {
        return ResultState.DEFAULT == executeResult.getResultState();
    }

    /**
     * 具体的单个worker执行任务
     */
    private ExecuteResult<V> workerDoJob() {
        // 如果当前执行过了就返回执行结果 避免重复执行
        if (!checkIsNullResult()) {
            return executeResult;
        }
        try {
            // 如果已经不是INIT状态了，说明正在被执行或已执行完毕。这一步很重要，可以保证任务不被重复执行
            // 是的话就设置WORKING状态并在后面的action执行job工作
            if (!compareAndSetState(INIT, WORKING)) {
                return executeResult;
            }
            // 开启callback监听
            callback.begin();
            //执行耗时操作
            V resultValue = worker.action(param, forParamUseWrappers);
            // 设置阶段成功并开启回diao
            fastSuccess(WORKING, resultValue);
            // 将当前worker的执行结果返回
            return executeResult;
        } catch (Exception e) {
            // 如果当前执行过了就返回执行结果 避免重复回调
            if (!checkIsNullResult()) {
                return executeResult;
            }
            // 获取异常并将当前worker快速失败
            fastFail(WORKING, e);
            return executeResult;
        }
    }

    private ExecuteResult<V> defaultTimeOutResult() {
        executeResult.setResultState(ResultState.TIMEOUT);
        executeResult.setResult(worker.defaultValue());
        return executeResult;
    }

    private ExecuteResult<V> defaultExResult(Exception exception) {
        executeResult.setResultState(ResultState.EXCEPTION);
        executeResult.setResult(worker.defaultValue());
        executeResult.setException(exception);
        return executeResult;
    }

    private boolean compareAndSetState(int expect, int update) {
        return this.state.compareAndSet(expect, update);
    }

    /**
     * 总控制台超时，停止所有任务
     */
    public void stopNow() {
        if (getState() == INIT || getState() == WORKING) {
            fastFail(getState(), null);
        }
    }

    private void addNext(WorkerWrapper<?, ?> workerWrapper) {
        if (nextWrappers == null) {
            nextWrappers = new ArrayList<>();
        }
        //避免添加重复
        for (WorkerWrapper wrapper : nextWrappers) {
            if (workerWrapper.equals(wrapper)) {
                return;
            }
        }
        nextWrappers.add(workerWrapper);
    }

    private void addDepend(WorkerWrapper<?, ?> workerWrapper, boolean must) {
        addDepend(new DependWrapper(workerWrapper, must));
    }

    private void addDepend(DependWrapper dependWrapper) {
        if (dependWrappers == null) {
            dependWrappers = new ArrayList<>();
        }
        //如果依赖的是重复的同一个，就不重复添加了
        for (DependWrapper wrapper : dependWrappers) {
            if (wrapper.equals(dependWrapper)) {
                return;
            }
        }
        dependWrappers.add(dependWrapper);
    }

    public static <T, V> WorkerWrapperBuilder<T, V> builder() {
        return new WorkerWrapperBuilder<>();
    }

    //—————————————————————— Builder方法部分 ——————————————————————//

    public static class WorkerWrapperBuilder<W, C> implements Builder<WorkerWrapper<W, C>> {
        /**
         * 该wrapper的唯一标识
         */
        private String id = UUID.randomUUID().toString();
        /**
         * worker将来要处理的param
         */
        private W param;
        private IWorker<W, C> worker;
        private ICallback<W, C> callback;
        /**
         * 自己后面的所有
         */
        private List<WorkerWrapper<?, ?>> nextWrappers;
        /**
         * 自己依赖的所有
         */
        private List<DependWrapper> dependWrappers;
        /**
         * 存储强依赖于自己的wrapper集合
         */
        private Set<WorkerWrapper<?, ?>> selfIsMustSet;

        private boolean needCheckNextWrapperResult = true;

        public WorkerWrapperBuilder<W, C> id(String id) {
            if (id != null) {
                this.id = id;
            }
            return this;
        }

        public WorkerWrapperBuilder<W, C> worker(IWorker<W, C> worker) {
            this.worker = worker;
            return this;
        }

        public WorkerWrapperBuilder<W, C> param(W w) {
            this.param = w;
            return this;
        }

        public WorkerWrapperBuilder<W, C> needCheckNextWrapperResult(boolean needCheckNextWrapperResult) {
            this.needCheckNextWrapperResult = needCheckNextWrapperResult;
            return this;
        }

        public WorkerWrapperBuilder<W, C> callback(ICallback<W, C> callback) {
            this.callback = callback;
            return this;
        }

        public WorkerWrapperBuilder<W, C> depend(WorkerWrapper<?, ?>... wrappers) {
            if (wrappers == null) {
                return this;
            }
            for (WorkerWrapper<?, ?> wrapper : wrappers) {
                depend(wrapper);
            }
            return this;
        }

        public WorkerWrapperBuilder<W, C> depend(WorkerWrapper<?, ?> wrapper) {
            return depend(wrapper, true);
        }

        public WorkerWrapperBuilder<W, C> depend(WorkerWrapper<?, ?> wrapper, boolean isMust) {
            if (wrapper == null) {
                return this;
            }
            DependWrapper dependWrapper = new DependWrapper(wrapper, isMust);
            if (dependWrappers == null) {
                dependWrappers = new ArrayList<>();
            }
            dependWrappers.add(dependWrapper);
            return this;
        }

        public WorkerWrapperBuilder<W, C> next(WorkerWrapper<?, ?> wrapper) {
            return next(wrapper, true);
        }

        public WorkerWrapperBuilder<W, C> next(WorkerWrapper<?, ?> wrapper, boolean selfIsMust) {
            if (nextWrappers == null) {
                nextWrappers = new ArrayList<>();
            }
            nextWrappers.add(wrapper);

            //强依赖自己
            if (selfIsMust) {
                if (selfIsMustSet == null) {
                    selfIsMustSet = new HashSet<>();
                }
                selfIsMustSet.add(wrapper);
            }
            return this;
        }

        public WorkerWrapperBuilder<W, C> next(WorkerWrapper<?, ?>... wrappers) {
            if (wrappers == null) {
                return this;
            }
            for (WorkerWrapper<?, ?> wrapper : wrappers) {
                next(wrapper);
            }
            return this;
        }

        public WorkerWrapper<W, C> build() {
            WorkerWrapper<W, C> wrapper = new WorkerWrapper<>(id, worker, param, callback);
            wrapper.setNeedCheckNextWrapperResult(needCheckNextWrapperResult);
            if (dependWrappers != null) {
                for (DependWrapper workerWrapper : dependWrappers) {
                    workerWrapper.getDependWrapper().addNext(wrapper);
                    wrapper.addDepend(workerWrapper);
                }
            }
            if (nextWrappers != null) {
                for (WorkerWrapper<?, ?> workerWrapper : nextWrappers) {
                    boolean must = false;
                    if (selfIsMustSet != null && selfIsMustSet.contains(workerWrapper)) {
                        must = true;
                    }
                    workerWrapper.addDepend(wrapper, must);
                    wrapper.addNext(workerWrapper);
                }
            }
            return wrapper;
        }
    }
}
