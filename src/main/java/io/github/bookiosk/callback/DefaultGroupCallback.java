package io.github.bookiosk.callback;

import io.github.bookiosk.wrapper.WorkerWrapper;

import java.util.List;

/**
 * @author bookiosk
 */
public class DefaultGroupCallback implements IGroupCallback {
    @Override
    public void success(List<WorkerWrapper> workerWrappers) {

    }

    @Override
    public void failure(List<WorkerWrapper> workerWrappers, Exception e) {

    }
}
