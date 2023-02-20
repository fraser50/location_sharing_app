package me.fjw.uni.grouplocations;

import static androidx.camera.view.CameraController.IMAGE_ANALYSIS;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.os.CancellationSignal;
import androidx.core.os.ExecutorCompat;
import androidx.core.util.Consumer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    public class SharedData {
        public MainActivity activity;
        public SharedData() {
            setupRequestLocation();
        }
        // This key is for testing on my local machine.
        private String authKey = "";
        private double latitude = 0;
        private double longitude = 0;

        @JavascriptInterface
        public String getAuthKey() {
            return authKey;
        }

        @JavascriptInterface
        public String getAPI() {
            return "https://uni.fjw.me";
        }

        @JavascriptInterface
        public void setAuthKey(String authKey) {
            this.authKey = authKey;
        }

        @JavascriptInterface
        public void loadAuthKey() {
            CookieManager.getInstance().setCookie(getAPI(), "key=" + authKey + "; SameSite=None; Secure");
        }

        @JavascriptInterface
        public void saveAuthKey() throws IOException {
            File f = new File(getFilesDir().getAbsolutePath() + File.separator + "auth.txt");

            FileWriter writer = new FileWriter(f);

            writer.write(authKey);

            writer.close();
        }

        public void loadAuthKeyFromFile() throws FileNotFoundException {
            File f = new File(getFilesDir().getAbsolutePath() + File.separator + "auth.txt");
            if (f.exists()) {
                // TODO: Add proper error handling
                Scanner scan = new Scanner(f);

                authKey = scan.next();

                scan.close();
            }
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

            //manager.requestLocationUpdates("gps", 1000, 10, new LocationListener() {
            //    @Override
            //    public void onLocationChanged(@NonNull Location location) {
            //        latitude = location.getLatitude();
            //        longitude = location.getLongitude();
            //    }
            //});
        }

        @JavascriptInterface
        public void scanQRCode() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    openQRCode();
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
        data.activity = this;
        try {
            data.loadAuthKeyFromFile();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        WebView.setWebContentsDebuggingEnabled(true);
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

    public void openQRCode() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        LifecycleCameraController camControl = new LifecycleCameraController(getApplicationContext());

        PreviewView preview = findViewById(R.id.cameraPreview);

        camControl.setEnabledUseCases(IMAGE_ANALYSIS);
        camControl.bindToLifecycle(this);
        camControl.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(getApplicationContext()), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {

            }
        });
        preview.setVisibility(View.VISIBLE);
        preview.setController(camControl);
        findViewById(R.id.options).setVisibility(View.INVISIBLE);
    }
}