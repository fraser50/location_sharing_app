package me.fjw.uni.grouplocations;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

public class LocationClient extends WebSocketClient {
    private String authKey;
    private URI uri;
    private LocationManager manager;
    private LocationService service;

    private volatile float latitude;
    private volatile float longitude;

    private volatile float[] lastLatitudes = new float[3];
    private volatile float[] lastLongitudes = new float[3];

    private LocationListener locationHandler;

    public static final int IDLE_LOCATION_INTERVAL = 30000;
    public static final int ACTIVE_LOCATION_INTERVAL = 1000;

    private boolean trackerActive = false;

    private boolean activeTracking = false;

    private String receiveMessage;

    private long lastLocationSend = 0L;

    private boolean currentlyTracked = false;

    private long lastLocationCheck = 0L;

    public LocationClient(URI uri, String authKey, LocationManager manager, Context baseContext, LocationService service) {
        super(uri);
        this.authKey = authKey;
        this.uri = uri;
        this.manager = manager;
        this.service = service;

        locationHandler = new LocationListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onLocationChanged(@NonNull Location location) {
                latitude = (float) location.getLatitude();
                longitude = (float) location.getLongitude();

                lastLatitudes[2] = lastLatitudes[1];
                lastLongitudes[2] = lastLongitudes[2];

                lastLatitudes[1] = lastLatitudes[0];
                lastLongitudes[1] = lastLongitudes[0];

                lastLatitudes[0] = latitude;
                lastLongitudes[0] = longitude;

                try {
                    send(generateFullRequest("locudate", new JSONObject()));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                long currentTime = System.currentTimeMillis();

                if (currentTime - lastLocationCheck > 1000 * 60) {
                    boolean onCampus = isLocationOnCampus(latitude, longitude);
                    boolean shouldNotify = false;
                    String notifyTitle = null;
                    String notifyDesc = null;

                    if (onCampus || service.isExtendedTracking() && !currentlyTracked) {
                        shouldNotify = true;
                        notifyTitle = "You have entered the tracking zone";
                        notifyDesc = "People in your groups can now see your location.";
                        currentlyTracked = true;
                    }

                    if (!(onCampus || service.isExtendedTracking()) && currentlyTracked) {
                        shouldNotify = true;
                        notifyTitle = "You have left the tracking zone";
                        notifyDesc = "Nobody else can see your location updates";
                        currentlyTracked = false;
                    }

                    if (shouldNotify) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            Notification n = new Notification.Builder(service.getApplicationContext(), NotificationChannel.DEFAULT_CHANNEL_ID)
                                    .setContentTitle(notifyTitle)
                                    .setContentText(notifyDesc)
                                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                                    .setContentIntent(null)
                                    .setTicker("Ticker")
                                    .setChannelId("GroupLocChannel")
                                    .build();

                            NotificationManager notificationManager = (NotificationManager) service.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.notify(3, n);
                        }
                    }

                    lastLocationCheck = currentTime;
                }

                Log.d("ws_client", "Updating location");
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
                Log.d("ws_client", "Provider enabled: " + provider);
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Log.d("ws_client", "Provider disabled: " + provider);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("ws_client", "Status changed: " + provider + " " + status);
            }
        };

        setupLocationUpdates(baseContext, false);
    }

    public String generateFullRequest(String type, Object obj) throws JSONException {
        JSONObject fullReq = new JSONObject();
        fullReq.put("type", type);
        fullReq.put("body", obj);

        return fullReq.toString();
    }

    /**
     * This method takes a latitude and longitude and returns true if the location is on campus.
     * @param latitude
     * @param longitude
     * @return
     */
    public boolean isLocationOnCampus(float latitude, float longitude) {
        // Check if location tracking within area
        Location currentLoc = new Location("");
        currentLoc.setLatitude(latitude);
        currentLoc.setLongitude(longitude);

        boolean withinUni = currentLoc.distanceTo(service.uniLoc) < LocationService.UNI_PERMITTED_DISTANCE;

        // If the device is within the tracking zone, check that they are not within any of the exclusion zones
        if (withinUni) {
            for (Pair<Location, Integer> exclusionPair : service.excludedLocations) {
                Location exclusionLocation = exclusionPair.first;
                int exclusionDistance = exclusionPair.second;

                if (currentLoc.distanceTo(exclusionLocation) < exclusionDistance) {
                    withinUni = false;
                    if (!service.isExtendedTracking()) Log.d("ws_client", "User is within exclusion zone, refusing tracking");
                    break;
                }
            }
        }

        return withinUni;
    }

    public void sendLocationUpdate(float latitude, float longitude) {
        // Check if user has disabled location tracking
        if (!service.isLocationServicesEnabled()) {
            Log.d("ws_client", "Refusing to send location, user has disabled sharing");
            return;
        }

        boolean withinUni = isLocationOnCampus(latitude, longitude);

        // Refuse to share location if device not on HW campus and they haven't opted into extended tracking
        if (!withinUni && !service.isExtendedTracking()) {
            JSONObject locationReq = new JSONObject();
            try {
                locationReq.put("onCampus", false);
                send(generateFullRequest("location", locationReq));

            } catch (JSONException e) {
                Log.d("ws_client", "JSON Error on sending location refusal");
            }
            return;
        }

        Location currentLoc = new Location("");
        currentLoc.setLatitude(latitude);
        currentLoc.setLongitude(longitude);

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastLocationSend > 1000 * 60 * 20) {
            Log.d("ws_client", "Sending tracking notification");
            // Notify user that their location is being tracked
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Notification n = new Notification.Builder(service.getApplicationContext(), NotificationChannel.DEFAULT_CHANNEL_ID)
                        .setContentTitle("Your location has been shared recently")
                        .setContentText("Somebody in one of your groups has the map open.")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentIntent(null)
                        .setTicker("Ticker")
                        .setChannelId("GroupLocChannel")
                        .build();

                NotificationManager notificationManager = (NotificationManager) service.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(2, n);
            }
        }

        lastLocationSend = currentTime;

        JSONObject locationReq = new JSONObject();
        try {
            locationReq.put("latitude", latitude);
            locationReq.put("longitude", longitude);
            locationReq.put("activelyTracking", activeTracking);
            locationReq.put("withinUni", withinUni);
            locationReq.put("distance", currentLoc.distanceTo(service.uniLoc));
            locationReq.put("date", new Date().toGMTString());

            for (int i=0; i<3; i++) {
                boolean prevPosWithinUni = service.isExtendedTracking() || isLocationOnCampus(lastLatitudes[i], lastLongitudes[i]);
                locationReq.put("prevLocLat" + i, prevPosWithinUni ? lastLatitudes[i] : 0);
                locationReq.put("prevLocLong" + i, prevPosWithinUni ? lastLongitudes[i] : 0);
            }

            send(generateFullRequest("location", locationReq));

        } catch (JSONException e) {
            Log.d("ws_client", "JSON Error on sending location");
        }


    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d("ws_client", "Connection opened, verifying hostname...");
        HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
        SSLSocket sock = (SSLSocket) getSocket();
        SSLSession s = sock.getSession();
        if (!hv.verify(uri.getHost(), s)) {
            Log.w("ws_client", "Hostname is not valid! Closing connection");
            close();
            return;
        }
        JSONObject authReq = new JSONObject();
        try {
            authReq.put("authKey", authKey);
            send(generateFullRequest("auth", authReq));
            Log.d("ws_client", "sent auth request");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMessage(String message) {
        Log.d("ws_client", "Message received");
        Log.d("ws_client", message);

        Date currentDate = new Date();

        if (currentDate.after(LocationService.CUTOFF_DATE)) {
            Log.d("ws_client", "Received message after cutoff date, stopping ws client.");
            close();
            return;
        }

        // Retrieve the response type
        try {
            JSONObject fullReq = new JSONObject(message);
            String type = fullReq.getString("responsible_req");

            // TODO: Send received message to WebView

            JSONObject body = fullReq.getJSONObject("body");

            switch (type) {
                case "auth":
                    if (body.getString("status").equals("success")) {
                        Log.d("ws_client", "Authentication successful!");

                    } else {
                        Log.d("ws_client", "Authentication failure, closing connection.");
                        close();
                    }
                case "location_request":
                    //if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //    return;
                    //}

                    sendLocationUpdate(latitude, longitude);
                    break;

                default:
                    Log.d("ws_client", "Received unknown message type '" + type + "'");
                    break;
            }

            receiveMessage = message;
            // Call receive callback when a non-auth message is received
            if (service.getReceiveCallback() != null && !type.equals("auth")) {
                service.getReceiveCallback().run();
            }

        } catch (JSONException e) {
            Log.d("ws_client", "Invalid JSON message received!");
            Log.e("ws_client", "Here is the JSON error", e);
        }

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {
        Log.e("ws_client", "websocket error", ex);
    }

    // https://github.com/TooTallNate/Java-WebSocket/wiki/No-such-method-error-setEndpointIdentificationAlgorithm
    @Override
    protected void onSetSSLParameters(SSLParameters sslParameters) {
    }

    public void setupLocationUpdates(Context baseContext, boolean motionDetected) {
        activeTracking = motionDetected;
        ContextCompat.getMainExecutor(baseContext).execute(new Runnable() {
            @Override
            public void run() {
                if (trackerActive) {
                    manager.removeUpdates(locationHandler);
                }

                if (ActivityCompat.checkSelfPermission(baseContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(baseContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.w("ws_client", "Location access has not been granted!");
                    //return;
                }

                Log.d("ws_client", "location update rate: " + (motionDetected ? ACTIVE_LOCATION_INTERVAL : IDLE_LOCATION_INTERVAL));

                manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, motionDetected ?
                        ACTIVE_LOCATION_INTERVAL : IDLE_LOCATION_INTERVAL, 0f, locationHandler);

                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, motionDetected ?
                        ACTIVE_LOCATION_INTERVAL : IDLE_LOCATION_INTERVAL,0f, locationHandler);

                trackerActive = true;
            }
        });
    }

    public void terminateLocationUpdates(Context baseContext) {
        if (locationHandler != null) {
            ContextCompat.getMainExecutor(baseContext).execute(new Runnable() {
                @Override
                public void run() {
                    if (locationHandler != null) {
                        manager.removeUpdates(locationHandler);
                    }
                }
            });
        }
    }

    public String getReceiveMessage() {
        return receiveMessage;
    }

    public WebViewPair<WebViewFloat, WebViewFloat> getCoordinates() {
        Log.d("ws_client", "Latitude: " + latitude + " | Longitude: " + longitude);
        return new WebViewPair<>(new WebViewFloat(latitude), new WebViewFloat(longitude));
    }

    public ArrayWrapper<WebViewPair<WebViewFloat, WebViewFloat>> getLastLocations() {
        WebViewPair<WebViewFloat, WebViewFloat>[] webPrevPositions = new WebViewPair[lastLatitudes.length];

        for (int i=0; i< lastLatitudes.length; i++) {
            webPrevPositions[i] = new WebViewPair<>(new WebViewFloat(lastLatitudes[i]), new WebViewFloat(lastLongitudes[i]));
        }

        return new ArrayWrapper<>(webPrevPositions);
    }
}