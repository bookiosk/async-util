package io.github.bookiosk.entity;

import io.github.bookiosk.enums.ResultState;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author bookiosk
 */
public class WorkState<V> {

    /**
     * 标记该事件是否已经被处理过了，譬如已经超时返回false了，后续rpc又收到返回值了，则不再二次回调
     * 使用AtomicInteger是为了能保证高并发情况state的原子性
     *  <p>
     * 1-finish, 2-error, 3-working
     */
    public static final int INIT = 0;
    public static final int FINISH = 1;
    public static final int ERROR = 2;
    public static final int WORKING = 3;
    private volatile Integer state;
    private volatile ExecuteResult<V> executeResult;

    public static <V> WorkState<V> defaultWorkState() {
        return new WorkState<>(INIT, ExecuteResult.defaultResult());
    }

    public WorkState(Integer state, ExecuteResult<V> executeResult) {
        this.state = state;
        this.executeResult = executeResult;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public ExecuteResult<V> getExecuteResult() {
        return executeResult;
    }

    public void setExecuteResult(ExecuteResult<V> executeResult) {
        this.executeResult = executeResult;
    }
}
