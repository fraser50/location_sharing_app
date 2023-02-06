package me.fjw.uni.grouplocations;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public class MainActivity extends AppCompatActivity {

    public class SharedData {
        // This key is for testing on my local machine.
        private String authKey = "59aadb01555247e9ebcbf78c8ce3f6a08b696f0fe39e8861afbc371055b7db1b";

        @JavascriptInterface
        public String getAuthKey() {
            return authKey;
        }

        @JavascriptInterface
        public String getAPI() {
            return "http://192.168.1.184:8080";
        }

        @JavascriptInterface
        public void setAuthKey(String authKey) {
            this.authKey = authKey;
        }

        @JavascriptInterface
        public void loadAuthKey() {
            CookieManager.getInstance().setCookie(getAPI(), "key=" + authKey);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedData data = new SharedData();

        WebView optionsView = findViewById(R.id.options);

        CookieManager.getInstance().setAcceptThirdPartyCookies(optionsView, true);

        optionsView.getSettings().setJavaScriptEnabled(true);
        optionsView.getSettings().setAllowUniversalAccessFromFileURLs(true);

        optionsView.addJavascriptInterface(data, "sharedData");
        optionsView.loadUrl("file:///android_asset/web/listing/index.html");
    }
}