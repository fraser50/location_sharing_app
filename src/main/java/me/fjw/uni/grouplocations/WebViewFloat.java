package me.fjw.uni.grouplocations;

import android.webkit.JavascriptInterface;

public class WebViewFloat {
    private final float primitiveValue;

    public WebViewFloat(float primitiveValue) {
        this.primitiveValue = primitiveValue;
    }

    @JavascriptInterface
    public float get() {
        return primitiveValue;
    }
}
