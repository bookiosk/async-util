package org.bookiosk.async.config;

/**
 * @author bookiosk 2024-02-29
 */
public class AsyncLogConfig {

    /**
     * 是否打印操作日志
     */
    private Boolean isLog = false;

    /**
     * 日志等级（trace、debug、info、warn、error、fatal），此值与 logLevelInt 联动
     */
    private String logLevel = "trace";

    /**
     * 日志等级 int 值（1=trace、2=debug、3=info、4=warn、5=error、6=fatal），此值与 logLevel 联动
     */
    private int logLevelInt = 1;

    /**
     * 是否打印彩色日志
     */
    private Boolean isColorLog = null;

    public Boolean getIsLog() {
        return isLog;
    }

    public void setIsLog(Boolean log) {
        isLog = log;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public int getLogLevelInt() {
        return logLevelInt;
    }

    public void setLogLevelInt(int logLevelInt) {
        this.logLevelInt = logLevelInt;
    }

    public Boolean getIsColorLog() {
        return isColorLog;
    }

    public void setIsColorLog(Boolean colorLog) {
        isColorLog = colorLog;
    }
}
