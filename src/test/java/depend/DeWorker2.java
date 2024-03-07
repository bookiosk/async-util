package depend;


import org.bookiosk.async.callback.ICallback;
import org.bookiosk.async.entity.ExecuteResult;
import org.bookiosk.async.worker.IWorker;
import org.bookiosk.async.wrapper.WorkerWrapper;

import java.util.Map;

/**
 * @author bookiosk wrote on 2024-02-27
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
