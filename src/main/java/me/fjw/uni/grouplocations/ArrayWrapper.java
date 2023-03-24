package me.fjw.uni.grouplocations;

import android.webkit.JavascriptInterface;

/**
 * This class provides JS code access to java array elements.
 */
public class ArrayWrapper<T> {
    private T[] array;

    public ArrayWrapper(T[] array) {
        this.array = array;
    }

    @JavascriptInterface
    public int getLength() {
        return array.length;
    }

    @JavascriptInterface
    public T getItem(int i) {
        return array[i];
    }
}
