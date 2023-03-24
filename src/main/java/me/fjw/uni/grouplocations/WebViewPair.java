package me.fjw.uni.grouplocations;

import android.webkit.JavascriptInterface;

/**
 * A class similar to the Android Pair class where the elements can be retrieved from JS code.
 */
public class WebViewPair<F, S> {
    public F first;
    public S second;

    public WebViewPair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    @JavascriptInterface
    public F getFirst() {
        return first;
    }

    @JavascriptInterface
    public S getSecond() {
        return second;
    }
}
