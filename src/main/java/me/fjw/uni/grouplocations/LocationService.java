package me.fjw.uni.grouplocations;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.FileUtils;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.net.URI;
import java.nio.CharBuffer;
import java.util.Scanner;

public class LocationService extends Service {
    // This is the thread that runs the background tasks
    private Thread worker;

    // User-configurable fields
    private boolean locationServicesEnabled = true;

    // Has the user opted in to more detailed location tracking (outside of HW campus)?
    private boolean extendedTracking = false;

    private static final String WS_API = "wss://localuni.fjw.me";

    private File settingsFile;

    @Override
    public IBinder onBind(Intent intent) {
        return new LocationBinder(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        settingsFile = new File(getFilesDir().getAbsolutePath() + File.separator + "settings.json");

        loadSettings(settingsFile);
        // Only run the worker thread once
        if (worker != null) return START_STICKY;

        URI u;

        Log.d("ws_client", "Preparing to start client");

        try {
            u = new URI(WS_API);
        } catch (Exception e) {
            Log.d("ws_client", e.toString());
            return START_STICKY;
        }

        Log.d("ws_client", "Finished creating URI, loading auth key...");

        String authKey;
        File f = new File(getFilesDir().getAbsolutePath() + File.separator + "auth.txt");
        try {
            Scanner scan = new Scanner(f);
            authKey = scan.next();
            scan.close();
        } catch (FileNotFoundException e) {
            Log.d("ws_client", "auth.txt not found!");
            return START_STICKY;
        }

        Log.d("ws_client", "Auth key loaded successfully!");

        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                LocationClient client = new LocationClient(u, authKey, (LocationManager) getSystemService(Context.LOCATION_SERVICE), getBaseContext(), LocationService.this);
                Log.d("ws_client", "Connecting to " + WS_API);
                //SocketFactory factory = SocketFactory.getDefault();
                //client.setSocketFactory(factory);
                client.run();
                Log.d("ws_client", "stopped running");
            }
        });

        worker.start();



        return START_STICKY;
    }

    // Save user configured settings to the given file
    private void saveSettings(File f) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("locationServicesEnabled", isLocationServicesEnabled());
            obj.put("extendedTracking", isExtendedTracking());
        } catch (JSONException e) {
            Log.e("ws_client", "JSON error when saving settings file", e);
        }
    }

    public void loadSettings(File f) {
        try {
            Scanner scan = new Scanner(f);

            String text = scan.next();

            JSONObject obj = new JSONObject(text);

            locationServicesEnabled = obj.getBoolean("locationServicesEnabled");
            extendedTracking = obj.getBoolean("extendedTracking");

        } catch (FileNotFoundException e) {
            Log.d("ws_client", "Settings file doesn't exist, using default values.");

        } catch (JSONException e) {
            Log.e("ws_client", "JSON exception occurred when loading settings file", e);
        }
    }

    public boolean isLocationServicesEnabled() {
        return locationServicesEnabled;
    }

    public void setLocationServicesEnabled(boolean locationServicesEnabled) {
        this.locationServicesEnabled = locationServicesEnabled;
        saveSettings(settingsFile);
    }

    public boolean isExtendedTracking() {
        return extendedTracking;
    }

    public void setExtendedTracking(boolean extendedTracking) {
        this.extendedTracking = extendedTracking;
        saveSettings(settingsFile);
    }
}