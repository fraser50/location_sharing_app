package me.fjw.uni.grouplocations;

import android.app.Service;
import android.content.Intent;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import javax.net.SocketFactory;

public class LocationService extends Service {
    // This is the thread that runs the background tasks
    private Thread worker;
    private static final String WS_API = "wss://uni.fjw.me";

    @Override
    public IBinder onBind(Intent intent) {
        return null; // TODO: Communication between app and service
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
                LocationClient client = new LocationClient(u, authKey);
                Log.d("ws_client", "Connecting to " + WS_API);
                SocketFactory factory = SocketFactory.getDefault();
                client.setSocketFactory(factory);
                client.run();
                Log.d("ws_client", "stopped running");
            }
        });

        worker.start();



        return START_STICKY;
    }
}