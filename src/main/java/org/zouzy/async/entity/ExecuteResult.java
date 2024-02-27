package org.zouzy.async.entity;

import org.zouzy.async.enums.ResultState;

public class ExecuteResult<V> {

    /**
     * 执行的结果
     */
    private V result;
    /**
     * 执行的结果状态
     */
    private ResultState resultState;
    /**
     * 执行中抛出的异常
     */
    private Exception exception;

    public ExecuteResult(V result, ResultState resultState) {
        this(result, resultState, null);
    }

    public ExecuteResult(V result, ResultState resultState, Exception ex) {
        this.result = result;
        this.resultState = resultState;
        this.exception = ex;
    }

    public static <V> ExecuteResult<V> defaultResult() {
        return new ExecuteResult<>(null, ResultState.DEFAULT);
    }

    @Override
    public String toString() {
        return "ExecuteResult{" +
                "result=" + result +
                ", resultState=" + resultState +
                ", exception=" + exception +
                '}';
    }

    public V getResult() {
        return result;
    }

    public void setResult(V result) {
        this.result = result;
    }

    public ResultState getResultState() {
        return resultState;
    }

    public void setResultState(ResultState resultState) {
        this.resultState = resultState;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
