package me.fjw.uni.grouplocations;

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
}
