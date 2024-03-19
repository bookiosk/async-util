package timeOut;


import depend.User;
import io.github.bookiosk.callback.ICallback;
import io.github.bookiosk.entity.ExecuteResult;
import io.github.bookiosk.worker.IWorker;
import io.github.bookiosk.wrapper.WorkerWrapper;

import java.util.Map;

/**
 * @author bookiosk
 */
public class DeWorker1 implements IWorker<String, User>, ICallback<String, User> {

    @Override
    public User action(String result, Map<String, WorkerWrapper> allWrappers) {
        System.out.println("par1的入参来自于par0： " + result);
        System.out.println("父节点的请求参数" + allWrappers.get("first").getExecuteResult());
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
    public void result(boolean success, String param, ExecuteResult<User> workResult) {
        System.out.println("worker1 的结果是：" + workResult.getResult());
    }

}
