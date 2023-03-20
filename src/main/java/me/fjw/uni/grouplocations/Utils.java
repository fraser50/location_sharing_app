package me.fjw.uni.grouplocations;

import android.location.Location;

import java.io.File;

public class Utils {
    public static void deleteEverything(File f) {
        if (f.isDirectory()) {
            for (File childFile : f.listFiles()) {
                deleteEverything(childFile);
            }

        }

        f.delete();
    }

    public static Location generateLocation(float latitude, float longitude) {
        Location loc = new Location("");
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        return loc;
    }
}
