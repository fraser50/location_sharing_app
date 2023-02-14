package me.fjw.uni.grouplocations;

import android.app.Service;
import android.content.Intent;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class LocationService extends Service {
    // This is the thread that runs the background tasks
    private Thread worker;

    @Override
    public IBinder onBind(Intent intent) {
        return null; // TODO: Communication between app and service
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Only run the worker thread once
        if (worker != null) return START_STICKY;

        // Just a simple test to check that the service is running properly
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                int counter = 0;
                while (true) {
                    try {
                        URL url = new URL("http://192.168.1.184:9999/" + counter);
                        URLConnection conn = url.openConnection();
                        conn.connect();
                        conn.getHeaderFields();
                        counter++;
                    } catch (MalformedURLException e) {
                        //throw new RuntimeException(e);
                    } catch (IOException e) {
                        //throw new RuntimeException(e);
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                }

            }
        });

        worker.start();



        return START_STICKY;
    }
}