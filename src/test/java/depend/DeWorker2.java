package depend;


import io.github.bookiosk.callback.ICallback;
import io.github.bookiosk.entity.ExecuteResult;
import io.github.bookiosk.worker.IWorker;
import io.github.bookiosk.wrapper.WorkerWrapper;

import java.util.Map;

/**
 * @author bookiosk
 */
public class DeWorker2 implements IWorker<ExecuteResult<User>, String>, ICallback<ExecuteResult<User>, String> {

    @Override
    public String action(ExecuteResult<User> result, Map<String, WorkerWrapper> allWrappers) {
        System.out.println("par2的入参来自于par1： " + result.getResult());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result.getResult().getName();
    }


    @Override
    public String defaultValue() {
        return "default";
    }

    @Override
    public void begin() {
        //System.out.println(Thread.currentThread().getName() + "- start --" + System.currentTimeMillis());
    }

    @Override
    public void result(boolean success, ExecuteResult<User> param, ExecuteResult<String> workResult) {
        System.out.println("worker2 的结果是：" + workResult.getResult());
    }

}
