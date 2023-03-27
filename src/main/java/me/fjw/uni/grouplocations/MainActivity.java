package me.fjw.uni.grouplocations;

import static androidx.camera.core.CameraSelector.LENS_FACING_BACK;
import static androidx.camera.view.CameraController.IMAGE_ANALYSIS;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.os.CancellationSignal;
import androidx.core.os.ExecutorCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends AppCompatActivity {
    // Whether the app should always update, regardless of version (useful for development)
    public static final boolean ALWAYS_UPDATE = false;
    private Camera cam;
    private ProcessCameraProvider provider;

    private SharedData data;

    private String messageReceived;

    private long lastForcedUpdate = 0L;

    // This class is used to store methods that are callable from inside the WebView.
    public class SharedData {
        public MainActivity activity;
        public SharedData() {
            //setupRequestLocation();
        }
        // This key is for testing on my local machine.
        private String authKey = "";
        private double latitude = 0;
        private double longitude = 0;

        private LocationBinder service;

        private String QRValue;

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
        public void signOut() {
            Log.d("ui", "Signing out...");
            File f = new File(getFilesDir().getAbsolutePath() + File.separator + "auth.txt");
            f.delete();
            authKey = "";
            CookieManager.getInstance().setCookie(getAPI(), "");

            // Restart service after deleting auth.txt

            Log.d("ui", "Attempting to reboot service...");

            try {
                service.getService().getClient().closeBlocking();
                Log.d("ui", "Service reboot successful!");
            } catch (InterruptedException e) {
                Log.e("ui", "Failed to close LocationClient", e);
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

        @JavascriptInterface
        public void scanQRCode() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        openQRCode();
                    }
                }
            });
        }

        @JavascriptInterface
        public void openSettings() {
            showSettings();
        }

        @JavascriptInterface
        public String getQRValue() {
            return QRValue;
        }

        public void setService(LocationBinder service) {
            this.service = service;
        }
        public void setQRValue(String QRValue) {
            this.QRValue = QRValue;
        }

        @JavascriptInterface
        public String getMessageReceived() {
            return messageReceived;
        }

        @JavascriptInterface
        public void sendMessage(String message) {
            try {
                service.sendWS(message);

            } catch (Exception e) {
                Log.e("ui", "Error occurred when WebView tried to send WS message", e);
            }
        }

        public void setMessageReceived(String message) {
            messageReceived = message;
        }

        @JavascriptInterface
        public ArrayWrapper<WebViewPair<WebViewFloat, WebViewFloat>> getPreviousLocations() {
            if (service.getService().getClient() == null) return null;

            return service.getService().getClient().getLastLocations();
        }

        @JavascriptInterface
        public WebViewPair<WebViewFloat, WebViewFloat> getCoordinates() {
            if (service.getService().getClient() == null) return null;

            return service.getService().getClient().getCoordinates();
        }

        @JavascriptInterface
        public String getAppType() {
            return "v1";
        }

        @JavascriptInterface
        public void forceUpdate() {

            long currentTime = System.currentTimeMillis();

            if (currentTime - lastForcedUpdate < 1000 * 60 * 10) {
                Log.d("main_activity", "Refusing to force UI update");
                return;
            }

            lastForcedUpdate = currentTime;
            updateUI(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadMainPage();
                        }
                    });
                }
            });
        }

        @JavascriptInterface
        public int getVersion() {
            File versionFile = new File(getFilesDir().getAbsolutePath() + File.separator + "version.txt");
            int currentVersion = -1;

            if (versionFile.exists()) {
                Scanner scan = null;
                try {
                    scan = new Scanner(versionFile);
                    currentVersion = scan.nextInt();
                    scan.close();
                } catch (FileNotFoundException e) {

                }
            }

            return currentVersion;
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent disableOptimisations = new Intent();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (!powerManager.isIgnoringBatteryOptimizations("me.fjw.uni.grouplocations")) {
                Log.d("main_activity", "Requesting to ignore battery optimisations");
                disableOptimisations.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                disableOptimisations.setData(Uri.parse("package:me.fjw.uni.grouplocations"));
                startActivity(disableOptimisations);
            }
        }

        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel("GroupLocChannel", "Group Locations", NotificationManager.IMPORTANCE_DEFAULT);
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(channel);
        }

        data = new SharedData();
        data.activity = this;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

        Intent fgService = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(fgService);
        }
        bindService(fgService, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocationBinder binder = (LocationBinder) service;
                data.setService(binder);

                data.service.setReceiveCallback(new Runnable() {
                    @Override
                    public void run() {
                        String receivedMessage = data.service.getService().getClient().getReceiveMessage();

                        // Notify JS code running in WebView that a WS message has been received
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                data.setMessageReceived(receivedMessage);
                                WebView optionsView = findViewById(R.id.options);
                                optionsView.loadUrl("javascript:onWSMessage()");
                            }
                        });
                    }
                });

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, BIND_ABOVE_CLIENT);
        try {
            data.loadAuthKeyFromFile();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        WebView.setWebContentsDebuggingEnabled(true);
        WebView optionsView = findViewById(R.id.options);

        CookieManager.getInstance().setAcceptThirdPartyCookies(optionsView, true);

        // Needs JS as well as file access so the UI files can be loaded by the browser.
        optionsView.getSettings().setJavaScriptEnabled(true);
        optionsView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        optionsView.getSettings().setAllowFileAccess(true);

        // Provides access to the SharedData instance.
        optionsView.addJavascriptInterface(data, "sharedData");

        // Request an update, and after completion, load the main page.
        updateUI(new Runnable() {
            @Override
            public void run() {
                loadMainPage();
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public void openQRCode() {
        Log.d("main_activity","QR Code scanner opened");
        findViewById(R.id.options).setVisibility(View.GONE);

        PreviewView previewV = findViewById(R.id.cameraPreview);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[] {Manifest.permission.CAMERA},1);
        }

        MainActivity mainActivity = this;

        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    provider = providerFuture.get();

                    Preview preview = new Preview.Builder().build();

                    CameraSelector selector = new CameraSelector.Builder().requireLensFacing(LENS_FACING_BACK).build();

                    preview.setSurfaceProvider(previewV.getSurfaceProvider());

                    BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build();

                    BarcodeScanner scanner = BarcodeScanning.getClient(options);

                    ImageAnalysis.Analyzer analyzer = new ImageAnalysis.Analyzer() {
                        @Override
                        @androidx.camera.core.ExperimentalGetImage
                        public void analyze(@NonNull ImageProxy image) {
                            Image mediaImage = image.getImage();

                            if (mediaImage != null) {
                                InputImage input = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());

                                Task<List<Barcode>> resultsTask = scanner.process(input);
                                resultsTask.addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                                    @Override
                                    public void onSuccess(List<Barcode> barcodes) {
                                        image.close();
                                        //Log.d("qr_scanner", "Searching for codes");
                                        if (barcodes.size() == 0) return;
                                        Log.d("qr_scanner", "Successfully scanned codes!");
                                        for (Barcode barcode : barcodes) {
                                            Log.d("qr_scanner", barcode.getRawValue());

                                            data.setQRValue(barcode.getRawValue());
                                            WebView view = findViewById(R.id.options);

                                            view.loadUrl("javascript:processQR()");
                                            break;
                                        }

                                        onCancel(null);
                                    }
                                });
                            }
                        }
                    };

                    ImageAnalysis analysis = new ImageAnalysis.Builder().build();
                    analysis.setAnalyzer(getMainExecutor(), analyzer);

                    cam = provider.bindToLifecycle(mainActivity, selector, preview, analysis);

                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, getMainExecutor());
        previewV.setVisibility(View.VISIBLE);
        findViewById(R.id.cancelBtn).setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            showSettings();
            return true;
        }

        return false;
    }

    @Override
    protected void onStop() {
        Log.d("main_activity", "App stopped");
        super.onStop();
        WebView options = findViewById(R.id.options);
        options.onPause();
        options.pauseTimers();
    }

    public void onCancel(View v) {
        findViewById(R.id.cameraPreview).setVisibility(View.GONE);
        findViewById(R.id.cancelBtn).setVisibility(View.GONE);
        findViewById(R.id.options).setVisibility(View.VISIBLE);

        provider.unbindAll();
    }


    @Override
    protected void onPause() {
        Log.d("main_activity", "App paused");
        super.onPause();

        WebView options = findViewById(R.id.options);
        options.onPause();
        options.pauseTimers();
    }

    @Override
    protected void onResume() {
        Log.d("main_activity", "App resumed");
        super.onResume();

        WebView options = findViewById(R.id.options);
        options.onResume();
        options.resumeTimers();
    }

    @Override
    protected void onRestart() {
        Log.d("main_activity", "App restarted");
        super.onRestart();

        WebView options = findViewById(R.id.options);
        options.onResume();
        options.resumeTimers();
    }

    @Override
    protected void onDestroy() {
        Log.d("main_activity", "App destroyed");
        super.onDestroy();

        WebView options = findViewById(R.id.options);
        options.onPause();
        options.pauseTimers();

        options.destroy();
    }

    public void showSettings() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WebView optionsView = findViewById(R.id.options);
                optionsView.addJavascriptInterface(data.service, "settings");
                optionsView.addJavascriptInterface(new Object() {
                    @JavascriptInterface
                    public void close() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                optionsView.removeJavascriptInterface("settings");
                                optionsView.removeJavascriptInterface("closer");
                                loadMainPage();
                            }
                        });
                    }
                }, "closer");

                optionsView.loadUrl("file:///android_asset/settings.html");
            }
        });
    }

    /**
     * This method will update the UI and will notify the given callback.
     * @param callback The callback to be called when the UI is updated successfully. This will run on the UI thread.
     */
    public void updateUI(Runnable callback) {
        new Thread() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void run() {
                // This is true if an error occurs during the zip extraction process
                boolean errorCatastrophic = false;
                try {
                    URL url = new URL(data.getAPI() + "/versioninfo");
                    URLConnection conn = url.openConnection();
                    conn.connect();
                    InputStreamReader reader = new InputStreamReader(url.openStream());

                    StringBuilder sb = new StringBuilder();
                    char[] c = new char[4096];

                    // Download response from server
                    int result = reader.read(c);

                    while (result != -1) {
                        for (int i=0; i<result; i++) {
                            sb.append(c[i]);
                        }

                        result = reader.read(c);
                    }

                    reader.close();

                    String response = sb.toString();
                    Log.d("ui_updater", "Got response");
                    Log.d("ui_updater", response);

                    // Get current local version
                    File versionFile = new File(getFilesDir().getAbsolutePath() + File.separator + "version.txt");
                    int currentVersion = -1;

                    if (versionFile.exists()) {
                        Scanner scan = new Scanner(versionFile);
                        currentVersion = scan.nextInt();
                        scan.close();
                    }

                    // Compare the current installed version with the newest version the server has available

                    JSONObject jsonRsp = new JSONObject(response);

                    int remoteVersion = jsonRsp.getInt("version");

                    // If the remote version is newer, update

                    if (remoteVersion > currentVersion || ALWAYS_UPDATE || !new File(getFilesDir() + File.separator + "ui").exists()) {
                        Log.d("ui_updater", "Version " + remoteVersion + " is newer than " + currentVersion);
                        File zipFile = new File(getFilesDir() + File.separator + "ui.zip");
                        if (zipFile.exists()) zipFile.delete();

                        // Download and save new version zip
                        URL dlURL = new URL(data.getAPI() + "/version.zip");
                        InputStream istream = dlURL.openStream();
                        OutputStream ostream = new FileOutputStream(zipFile);

                        // TODO: Perhaps make this work on API 22
                        FileUtils.copy(istream, ostream);

                        istream.close();
                        ostream.close();

                        errorCatastrophic = true;

                        // Delete existing installation

                        File uiFolder = new File(getFilesDir() + File.separator + "ui");

                        Utils.deleteEverything(uiFolder);

                        uiFolder.mkdir();

                        // Extract files from zip
                        ZipFile zip = new ZipFile(zipFile);

                        Enumeration<? extends ZipEntry> entries = zip.entries();

                        while (entries.hasMoreElements()) {
                            ZipEntry entry = entries.nextElement();

                            File extractedFile = new File(uiFolder.getAbsolutePath() + File.separator + entry.getName());
                            // Check that extracted files will not be placed outside of the installation directory
                            if (extractedFile.getAbsolutePath().startsWith(uiFolder.getAbsolutePath())) {
                                Log.d("ui_updater", "Extracting " + extractedFile.getAbsolutePath());

                                if (entry.isDirectory()) {
                                    extractedFile.mkdirs();
                                    Log.d("ui_updater", "Is directory");
                                    continue;
                                }

                                Log.d("ui_updater", "Creating parent directory");

                                extractedFile.getParentFile().mkdirs();

                                Log.d("ui_updater", "Getting input stream");

                                istream = zip.getInputStream(entry);
                                Log.d("ui_updater", "Getting output stream");
                                ostream = new FileOutputStream(extractedFile);

                                Log.d("ui_updater", "Copying file");

                                FileUtils.copy(istream, ostream);

                                Log.d("ui_updater", "Finished copying, closing streams.");
                                istream.close();
                                ostream.close();
                                Log.d("ui_updater", "Streams successfully closed.");
                            } else {
                                Log.w("ui_updater", "Incorrect path for " + extractedFile.getAbsolutePath());
                            }
                        }

                        Log.d("ui_updater", "Finished extracting.");
                    }

                    // Update local version number

                    if (versionFile.exists()) versionFile.delete();

                    FileWriter writer = new FileWriter(versionFile);
                    writer.write("" + remoteVersion);
                    writer.close();

                    runOnUiThread(callback);

                } catch (IOException e) {
                    Log.e("ui_updater", "IO exception occurred", e);

                    if (errorCatastrophic) {
                        Log.d("ui_updater", "Error has resulted in issues during file extraction, displaying error page");

                        // Delete UI folder
                        File uiFolder = new File(getFilesDir() + File.separator + "ui");

                        Utils.deleteEverything(uiFolder);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadMainPage();
                            }
                        });

                    } else {
                        runOnUiThread(callback);
                    }

                } catch (JSONException e) {
                    Log.e("ui_updater", "Version response from server was malformed", e);
                    runOnUiThread(callback);
                }
            }
        }.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void loadMainPage() {
        WebView options = findViewById(R.id.options);
        File uiFolder = new File(getFilesDir() + File.separator + "ui");

        if (uiFolder.exists()) {
            options.removeJavascriptInterface("updateManager");
            options.loadUrl("file://" + new File(getFilesDir() + File.separator + "ui" + File.separator + "index.html").getAbsolutePath());

        } else {
            options.addJavascriptInterface(new Object() {
                @JavascriptInterface
                public void update() {
                    updateUI(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadMainPage();
                                }
                            });
                        }
                    });
                }

            }, "updateManager");
            options.loadUrl("file:///android_asset/updateissue.html");
        }
    }
}