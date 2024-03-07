package depend;



import jdk.internal.net.http.common.Log;
import org.bookiosk.async.callback.ICallback;
import org.bookiosk.async.entity.ExecuteResult;
import org.bookiosk.async.worker.IWorker;
import org.bookiosk.async.wrapper.WorkerWrapper;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author bookiosk wrote on 2024-02-27
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
