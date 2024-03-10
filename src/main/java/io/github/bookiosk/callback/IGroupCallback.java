package io.github.bookiosk.callback;


import io.github.bookiosk.wrapper.WorkerWrapper;

import java.util.List;

/**
 * 如果是异步执行整组的话，可以用这个组回调。不推荐使用
 * @author bookiosk
 */
public interface IGroupCallback {
    /**
     * 成功后，可以从wrapper里去getWorkResult
     * @param workerWrappers 执行成功的组合体
     */
    void success(List<WorkerWrapper> workerWrappers);
    /**
     * 失败了，也可以从wrapper里去getWorkResult
     * @param workerWrappers 执行失败的组合体
     * @param e 失败原因
     */
    void failure(List<WorkerWrapper> workerWrappers, Exception e);
}
