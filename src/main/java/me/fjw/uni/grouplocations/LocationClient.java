package me.fjw.uni.grouplocations;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
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

    private LocationListener locationHandler;

    public static final int IDLE_LOCATION_INTERVAL = 30000;
    public static final int ACTIVE_LOCATION_INTERVAL = 1000;

    private boolean trackerActive = false;

    private boolean activeTracking = false;

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

    private String generateFullRequest(String type, Object obj) throws JSONException {
        JSONObject fullReq = new JSONObject();
        fullReq.put("type", type);
        fullReq.put("body", obj);

        return fullReq.toString();
    }

    public void sendLocationUpdate(float latitude, float longitude) {
        // Check if user has disabled location tracking
        if (!service.isLocationServicesEnabled()) {
            Log.d("ws_client", "Refusing to send location, user has disabled sharing");
            return;
        }

        // Check if location tracking within area
        Location currentLoc = new Location("");
        currentLoc.setLatitude(latitude);
        currentLoc.setLongitude(longitude);

        boolean withinUni = currentLoc.distanceTo(service.uniLoc) < LocationService.UNI_PERMITTED_DISTANCE;

        // TODO: If user is not in uni, and they're not participating in study 2, set coordinates to 0

        JSONObject locationReq = new JSONObject();
        try {
            locationReq.put("latitude", latitude);
            locationReq.put("longitude", longitude);
            locationReq.put("activelyTracking", activeTracking);
            locationReq.put("withinUni", withinUni);
            locationReq.put("distance", currentLoc.distanceTo(service.uniLoc));
            locationReq.put("date", new Date().toGMTString());

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

        // Retrieve the response type
        try {
            JSONObject fullReq = new JSONObject(message);
            String type = fullReq.getString("responsible_req");

            // TODO: Send received message to WebView

            JSONObject body = fullReq.getJSONObject("body");

            switch (type) {
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
                    return;
                }
                manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, motionDetected ?
                        ACTIVE_LOCATION_INTERVAL : IDLE_LOCATION_INTERVAL, 0.5f, locationHandler);

                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, motionDetected ?
                        ACTIVE_LOCATION_INTERVAL : IDLE_LOCATION_INTERVAL,0.5f, locationHandler);

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
}