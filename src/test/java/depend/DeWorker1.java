package depend;


import org.bookiosk.async.callback.ICallback;
import org.bookiosk.async.entity.ExecuteResult;
import org.bookiosk.async.worker.IWorker;
import org.bookiosk.async.wrapper.WorkerWrapper;

import java.util.Map;

/**
 * @author bookiosk wrote on 2024-02-27
 */
public class DeWorker1 implements IWorker<ExecuteResult<User>, User>, ICallback<ExecuteResult<User>, User> {

    @Override
    public User action(ExecuteResult<User> result, Map<String, WorkerWrapper> allWrappers) {
        System.out.println("par1的入参来自于par0： " + result.getResult());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new User("user1");
    }


    @Override
    public User defaultValue() {
        return new User("default User");
    }

    @Override
    public void begin() {
        //System.out.println(Thread.currentThread().getName() + "- start --" + System.currentTimeMillis());
    }

    @Override
    public void result(boolean success, ExecuteResult<User> param, ExecuteResult<User> workResult) {
        System.out.println("worker1 的结果是：" + workResult.getResult());
    }

}
