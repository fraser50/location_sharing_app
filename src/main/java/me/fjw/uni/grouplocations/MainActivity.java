package me.fjw.uni.grouplocations;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebView optionsView = findViewById(R.id.options);

        optionsView.getSettings().setJavaScriptEnabled(true);
        optionsView.loadUrl("file:///android_asset/web/listing/index.html");
    }
}