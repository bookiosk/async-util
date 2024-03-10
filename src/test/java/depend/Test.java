package depend;

import io.github.bookiosk.entity.ExecuteResult;
import io.github.bookiosk.executor.Async;
import io.github.bookiosk.wrapper.WorkerWrapper;

import java.util.concurrent.ExecutionException;


/**
 * bookiosk wrote on 2024-02-27
 */
public class Test {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        DeWorker w = new DeWorker();
        DeWorker1 w1 = new DeWorker1();
        DeWorker2 w2 = new DeWorker2();
        WorkerWrapper<ExecuteResult<User>, String> workerWrapper2 =  WorkerWrapper.<ExecuteResult<User>, String>builder()
                .worker(w2)
                .callback(w2)
                .id("third")
                .build();

        WorkerWrapper<ExecuteResult<User>, User> workerWrapper1 = WorkerWrapper.<ExecuteResult<User>, User>builder()
                .worker(w1)
                .callback(w1)
                .id("second")
                .next(workerWrapper2)
                .build();

        WorkerWrapper<String, User> workerWrapper = WorkerWrapper.<String, User>builder()
                .worker(w)
                .param("0")
                .id("first")
                .next(workerWrapper1, true)
                .callback(w)
                .build();
        //虽然尚未执行，但是也可以先取得结果的引用，作为下一个任务的入参。V1.2前写法，需要手工给
        //V1.3后，不用给wrapper setParam了，直接在worker的action里自行根据id获取即可.参考dependnew包下代码
        ExecuteResult<User> result = workerWrapper.getExecuteResult();
        ExecuteResult<User> result1 = workerWrapper1.getExecuteResult();
        workerWrapper1.setParam(result);
        workerWrapper2.setParam(result1);

        Async.beginWork(3500, workerWrapper);

        System.out.println(workerWrapper2.getExecuteResult());
        Async.shutDown();
    }
}
