package me.fjw.uni.grouplocations;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

public class LocationClient extends WebSocketClient {
    private String authKey;
    private URI uri;

    public LocationClient(URI uri, String authKey) {
        super(uri);
        this.authKey = authKey;
        this.uri = uri;
    }
    private String generateFullRequest(String type, Object obj) throws JSONException {
        JSONObject fullReq = new JSONObject();
        fullReq.put("type", type);
        fullReq.put("body", obj);

        return fullReq.toString();
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

    @Override
    public void onMessage(String message) {
        Log.d("ws_client", "Message received");
        Log.d("ws_client", message);
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

}
