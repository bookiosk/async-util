package org.zouzy.async;

import org.zouzy.async.config.AsyncConfig;

/**
 * @author bookiosk 2024-02-29
 */
public class AsyncManager {

    public volatile static AsyncConfig config;

    private static void setConfigMethod(AsyncConfig config) {
        AsyncManager.config = config;
    }

    public static AsyncConfig getConfig() {
        if (config == null) {
            synchronized (AsyncManager.class) {
                if (config == null) {
                    setConfigMethod(new AsyncConfig());
                }
            }
        }
        return config;
    }
}
