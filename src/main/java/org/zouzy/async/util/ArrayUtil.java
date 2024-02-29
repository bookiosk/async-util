package org.zouzy.async.util;

/**
 * @author bookiosk 2024-02-29
 */
public class ArrayUtil {
    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }
}
