package io.github.bookiosk.callback;

import io.github.bookiosk.entity.ExecuteResult;

/**
 * 每个执行单元执行完毕后，会回调该接口
 * 需要监听执行结果的，实现该接口即可
 * <p>
 * 对每个worker(IWorker接口子类)的回调。
 * worker执行完毕后，会回调该接口，带着执行成功、失败、原始入参、和详细的结果
 *
 * @author bookiosk
 */
@FunctionalInterface
public interface ICallback<T, V> {
    /**
     * 任务开始的监听
     */
    default void begin() {

    }

    /**
     * 耗时操作执行完毕后，就给value注入值
     *
     * @param success 是否成功
     * @param param 参数
     * @param executeResult 执行结果
     */
    void result(boolean success, T param, ExecuteResult<V> executeResult);
}
