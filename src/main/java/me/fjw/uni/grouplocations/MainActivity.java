package me.fjw.uni.grouplocations;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.location.LocationManagerCompat;
import androidx.core.os.CancellationSignal;
import androidx.core.util.Consumer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public class MainActivity extends AppCompatActivity {

    public class SharedData {
        public SharedData() {
            setupRequestLocation();
        }
        // This key is for testing on my local machine.
        private String authKey = "59aadb01555247e9ebcbf78c8ce3f6a08b696f0fe39e8861afbc371055b7db1b";
        private double latitude = 0;
        private double longitude = 0;

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

        @JavascriptInterface
        public double getLatitude() {
            return latitude;
        }

        @JavascriptInterface
        public double getLongitude() {
            return longitude;
        }

        // TODO: Actually check permissions
        @SuppressLint("MissingPermission")
        public void setupRequestLocation() {
            LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            manager.requestLocationUpdates("gps", 1000, 10, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
            });
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent fgService = new Intent(this, LocationService.class);
        startService(fgService);

        SharedData data = new SharedData();

        WebView optionsView = findViewById(R.id.options);

        CookieManager.getInstance().setAcceptThirdPartyCookies(optionsView, true);

        optionsView.getSettings().setJavaScriptEnabled(true);
        optionsView.getSettings().setAllowUniversalAccessFromFileURLs(true);

        optionsView.addJavascriptInterface(data, "sharedData");
        optionsView.loadUrl("file:///android_asset/web/listing/index.html");

        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d("providers", "Printing providers:");
        for (String provider : manager.getAllProviders()) {
            Log.d("providers", provider);
        }

    }
}