package org.zouzy.async.callback;

import org.zouzy.async.wrapper.WorkerWrapper;

import java.util.List;

/**
 * @author: bookiosk wrote on 2024-02-27
 **/
public class DefaultGroupCallback implements IGroupCallback {
    @Override
    public void success(List<WorkerWrapper> workerWrappers) {

    }

    @Override
    public void failure(List<WorkerWrapper> workerWrappers, Exception e) {

    }
}
