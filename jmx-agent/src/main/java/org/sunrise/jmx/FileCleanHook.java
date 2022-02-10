package org.sunrise.jmx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileCleanHook implements Runnable {
    private static List<String> toDeleteFiles = new ArrayList<String>();

    public static void add(String path) {
        if (path != null && !toDeleteFiles.contains(path)) {
            synchronized (toDeleteFiles) {
                toDeleteFiles.add(path);
            }
        }
    }

    public static void add(File f) {
        if (f != null) {
            try {
                add(f.getCanonicalPath());
            } catch (IOException e) {
            }
        }
    }

    public static void delete(File f) {
        if (f == null) return;
        try {
            if (f.exists()) {
                f.delete();
            }
        } catch (Throwable ex) {
        }

        synchronized (toDeleteFiles) {
            toDeleteFiles.remove(f.getAbsolutePath());
        }
    }

    @Override
    public void run() {
        for(String path : toDeleteFiles) {
            try {
                File f = new File(path);
                if (f != null && f.exists()) f.delete();
            } catch (Throwable ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    public static void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(new FileCleanHook()));
    }
}