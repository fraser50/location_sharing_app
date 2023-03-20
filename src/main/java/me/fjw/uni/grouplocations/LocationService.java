package me.fjw.uni.grouplocations;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.SSLSocketFactory;

public class LocationService extends Service implements SensorEventListener {
    // This is the thread that runs the background tasks
    private Thread worker;

    // User-configurable fields
    private boolean locationServicesEnabled = true;

    // Has the user opted in to more detailed location tracking (outside of HW campus)?
    private boolean extendedTracking = false;

    private static final String WS_API = "wss://localuni.fjw.me";

    private File settingsFile;

    private long lastReadingTime = 0;
    private int lowReadingCount = 0; // How many low readings have been detected in the last n seconds
    private long resetCount = 5000; // How long between potential movements are allowed before the count resets
    private float readingThreshold = 0.7f; // The threshold where anything lower counts as the device moving
    private long lastLowReadingTime = 0;
    private int countsRequired = 5; // The number of movement reading needed before considering the device as having moved

    private boolean frequentTracking = false;

    private LocationClient client;

    public Location uniLoc;

    // How close the device has to be to Heriot-Watt to be considered within the tracking area.
    public static final double UNI_PERMITTED_DISTANCE = 350;

    // List of circle areas that even if the user is within the tracking zone, they will not be tracked (halls)
    public List<Pair<Location, Integer>> excludedLocations = new ArrayList<>();

    // Date at which the app functionality will forcibly stop working. (17th of April 2023 at 3:14AM GMT)
    // The app may be shut down at an earlier date, however, the date below cannot be changed on the server-side.
    public static Date CUTOFF_DATE = new Date(1684293255000L);

    private Runnable receiveCallback;

    @Override
    public IBinder onBind(Intent intent) {
        return new LocationBinder(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            Intent mainActivityIntent = new Intent(this, MainActivity.class);

            PendingIntent pi = PendingIntent.getActivity(this, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE);

            NotificationChannel channel = new NotificationChannel("GroupLocChannel", "Group Locations", NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.createNotificationChannel(channel);

            Notification n = new Notification.Builder(this, NotificationChannel.DEFAULT_CHANNEL_ID)
                    .setContentTitle("Group Locations")
                    .setContentText("This is the service used by the university location tracking service to update your location in the background. Settings can be changed by opening the Group Locations app and pressing the settings button at the top-left.")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(pi)
                    .setTicker("Ticker")
                    .setChannelId("GroupLocChannel")
                    .build();

            startForeground(1, n);
        }

        // Location close to Heriot-Watt main reception
        uniLoc = new Location("");
        // 55.91069233323829, -3.3209063483970733
        uniLoc.setLatitude(55.91069233323829);
        uniLoc.setLongitude(-3.3209063483970733);

        // Mark halls as excluded locations where users shall not be tracked

        // George BBurnett, Lord Thomson, Robin Smith, Anna Macleod, Mary Fergusson, and Murial Spark halls.
        excludedLocations.add(new Pair<>(Utils.generateLocation(55.907994707708575f, -3.3265917927534145f), 200));

        // Christina Miller, Lord Home, Robert Bryson halls.
        excludedLocations.add(new Pair<>(Utils.generateLocation(55.906359220871316f, -3.3238933937540454f), 210));

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

        //client = new LocationClient(u, authKey, (LocationManager) getSystemService(Context.LOCATION_SERVICE), getBaseContext(), LocationService.this);
        worker = new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {
                    try {

                        Date currentDate = new Date();

                        // Don't attempt to run the service if the date is after the cutoff date.
                        if (currentDate.after(CUTOFF_DATE)) {
                            Log.d("ws_client", "Cutoff date reached, refusing to start.");
                            return;
                        }

                        // Authentication
                        String authKey;
                        File f = new File(getFilesDir().getAbsolutePath() + File.separator + "auth.txt");
                        try {
                            Scanner scan = new Scanner(f);
                            authKey = scan.next();
                            scan.close();
                        } catch (FileNotFoundException e) {
                            Log.d("ws_client", "auth.txt not found!");
                            Thread.sleep(30000);
                            continue;
                        }

                        Log.d("ws_client", "Auth key loaded successfully!");
                        Log.d("ws_client", "Connecting to " + WS_API);
                        client = new LocationClient(u, authKey, (LocationManager) getSystemService(Context.LOCATION_SERVICE), getBaseContext(), LocationService.this);

                        client.run();
                        client.terminateLocationUpdates(getBaseContext());
                        Log.d("ws_client", "stopped running");
                    } catch (Exception e) {
                        Log.d("ws_client", "Unknown error occurred, restarting client...");

                        if (client != null) {
                            try {
                                client.terminateLocationUpdates(getBaseContext());

                            } catch (Exception ex) {
                                client.terminateLocationUpdates(getBaseContext());
                            }
                        }
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                }

            }
        });

        worker.start();

        // Register accelerometer listener (used for detecting when the user is walking)
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] values = event.values;

            float x = values[0];
            float y = values[1];
            float z = values[2];

            float g = ((x*x) + (y*y) + (z*z)) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);

            long newReadingTime = System.currentTimeMillis();
            lowReadingCount = newReadingTime - lastLowReadingTime > resetCount ? 0 : lowReadingCount;

            if (g <= readingThreshold) {
                lowReadingCount++;
                lastLowReadingTime = newReadingTime;
                Log.d("ws_client", "g: " + g);

                if (lowReadingCount == countsRequired) {
                    Log.d("ws_client", "Device moved.");

                    client.setupLocationUpdates(getBaseContext(), true);
                    frequentTracking = true;

                    JSONObject jobj = new JSONObject();
                    try {
                        jobj.put("activelyMoving", true);
                        client.send(client.generateFullRequest("motion", jobj));

                    } catch (JSONException | WebsocketNotConnectedException e) {

                    }

                }
            }

            // If the last reading was 10 seconds ago, disable frequent tracking mode
            if (newReadingTime - lastLowReadingTime > 10000 && frequentTracking) {
                client.setupLocationUpdates(getBaseContext(), false);
                frequentTracking = false;
            }

            lastReadingTime = newReadingTime;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public LocationClient getClient() {
        return client;
    }

    public void setReceiveCallback(Runnable r) {
        this.receiveCallback = r;
    }

    public Runnable getReceiveCallback() {
        return receiveCallback;
    }
}