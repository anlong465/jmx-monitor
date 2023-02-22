package org.sunrise.jmx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileCleanHook implements Runnable {
    private static final List<String> toDeleteFiles = new ArrayList<String>();

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
            try {
                toDeleteFiles.remove(f.getCanonicalPath());
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void run() {
        for(String path : toDeleteFiles) {
            try {
                File f = new File(path);
                if (f.exists()) f.delete();
            } catch (Throwable ex) {
                JmxAgentLogger.info(ex.getMessage());
            }
        }
        JmxAgentLogger.close();
    }

    private static FileCleanHook hook = null;
    public static synchronized void init() {
        if (hook == null) {
            hook = new FileCleanHook();
            Runtime.getRuntime().addShutdownHook(new Thread(hook));
        }
    }
}
