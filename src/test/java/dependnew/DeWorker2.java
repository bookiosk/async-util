package dependnew;


import io.github.bookiosk.callback.ICallback;
import io.github.bookiosk.entity.ExecuteResult;
import io.github.bookiosk.worker.IWorker;
import io.github.bookiosk.wrapper.WorkerWrapper;

import java.util.Map;

/**
 * @author wuweifeng wrote on 2019-11-20.
 */
public class DeWorker2 implements IWorker<User, String>, ICallback<User, String> {

    @Override
    public String action(User object, Map<String, WorkerWrapper> allWrappers) {
        System.out.println("-----------------");
        System.out.println("par1的执行结果是： " + allWrappers.get("second").getExecuteResult());
        System.out.println("取par1的结果作为自己的入参，并将par1的结果加上一些东西");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        User user1 = (User) allWrappers.get("second").getExecuteResult().getResult();
        return user1.getName() + " worker2 add";
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
    public void result(boolean success, User param, ExecuteResult<String> workResult) {
        System.out.println("worker2 的结果是：" + workResult.getResult());
    }

}
