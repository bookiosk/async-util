package io.github.bookiosk.callback;

import io.github.bookiosk.entity.ExecuteResult;

/**
 * 默认回调类，如果不设置的话，会默认给这个回调
 *
 * @author bookiosk
 */
public class DefaultCallback<T, V> implements ICallback<T, V> {

    @Override
    public void result(boolean success, T param, ExecuteResult<V> workResult) {

    }

}
