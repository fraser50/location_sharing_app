package me.fjw.uni.grouplocations;

import android.annotation.SuppressLint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.NonNull;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

public class LocationClient extends WebSocketClient {
    private String authKey;
    private URI uri;
    private LocationManager manager;

    private float latitude;
    private float longitude;

    public LocationClient(URI uri, String authKey, LocationManager manager) {
        super(uri);
        this.authKey = authKey;
        this.uri = uri;
        this.manager = manager;

        setupLocationUpdates();
    }

    private String generateFullRequest(String type, Object obj) throws JSONException {
        JSONObject fullReq = new JSONObject();
        fullReq.put("type", type);
        fullReq.put("body", obj);

        return fullReq.toString();
    }

    public void sendLocationUpdate(float latitude, float longitude) {
        /*
        TODO: Check that device within tracking zone, and location sharing enabled by user,
        and location sharing hasn't been disabled by the server.
         */

        JSONObject locationReq = new JSONObject();
        try {
            locationReq.put("latitude", latitude);
            locationReq.put("longitude", longitude);

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
            String type = fullReq.getString("type");

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

    @SuppressLint("MissingPermission")
    private void setupLocationUpdates() {
        manager.requestLocationUpdates("gps", 1000, 10, new LocationListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onLocationChanged(@NonNull Location location) {
                latitude = (float) location.getLatitude();
                longitude = (float) location.getLongitude();
            }
        });
    }

}
