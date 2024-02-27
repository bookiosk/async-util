package org.zouzy.async.callback;

import org.zouzy.async.entity.ExecuteResult;

/**
 * 默认回调类，如果不设置的话，会默认给这个回调
 *
 * @author: bookiosk wrote on 2024-02-27
 **/
public class DefaultCallback<T, V> implements ICallback<T, V> {

    @Override
    public void result(boolean success, T param, ExecuteResult<V> workResult) {

    }

}
