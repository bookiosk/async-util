package io.github.bookiosk;

import io.github.bookiosk.config.AsyncLogConfig;

/**
 * @author bookiosk 2024-02-29
 */
public class AsyncManager {

    public volatile static AsyncLogConfig config;

    private static void setConfigMethod(AsyncLogConfig config) {
        AsyncManager.config = config;
    }

    public static AsyncLogConfig getConfig() {
        if (config == null) {
            synchronized (AsyncManager.class) {
                if (config == null) {
                    setConfigMethod(new AsyncLogConfig());
                }
            }
        }
        return config;
    }
}
