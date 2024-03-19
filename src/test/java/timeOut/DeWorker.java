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
public class DeWorker implements IWorker<String, User>, ICallback<String, User> {

    @Override
    public User action(String object, Map<String, WorkerWrapper> allWrappers) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new User("user0");
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
        System.out.println("worker0 的结果是：" + workResult.getResult());
    }

}
