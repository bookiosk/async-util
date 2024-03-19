package timeOut;


import depend.User;
import io.github.bookiosk.executor.Async;
import io.github.bookiosk.wrapper.WorkerWrapper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;


/**
 * bookiosk wrote on 2024-02-27
 */
public class Test {

    public Test() {
        super();
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        DeWorker w = new DeWorker();
        DeWorker1 w1 = new DeWorker1();
        WorkerWrapper<String, User> workerWrapper =  WorkerWrapper.<String, User>builder()
                .worker(w)
                .callback(w)
                .id("first")
                .build();

        WorkerWrapper<String, User> workerWrapper2 = WorkerWrapper.<String, User>builder()
                .worker(w1)
                .callback(w1)
                .param("哈哈哈哈")
                .id("second")
                .depend(workerWrapper)
                .build();

        Async.beginWork(1700, workerWrapper);
        System.out.println(workerWrapper.getExecuteResult());
        System.out.println(workerWrapper2.getExecuteResult());
        Async.shutDown();
        ForkJoinPool forkJoinPool = new ForkJoinPool();
    }
}
