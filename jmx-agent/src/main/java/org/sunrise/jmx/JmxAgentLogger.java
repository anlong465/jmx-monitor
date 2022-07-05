package org.sunrise.jmx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;

public class JmxAgentLogger {
    private static File loggerFile = null;
    private static BufferedWriter bw = null;
    static {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        int pos = jvmName.indexOf('@');
        if (pos > 0) {
            jvmName = jvmName.substring(0, pos);
        }
        File root = new File("/tmp", "jmx-agent");
        root.mkdirs();

        loggerFile = new File(root, jvmName + ".out");
        try {
            bw = new BufferedWriter(new FileWriter(loggerFile));
            bw.write(System.getProperty("sun.java.command"));
            bw.newLine();
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void info(String message) {
        if (bw != null) {
            try {
                if (message != null && message.length() > 0) {
                    message += "\n";
                } else {
                    message = "\n";
                }
                bw.write(message);
            } catch (IOException e) {
            }
        }
    }

    public static void touch() {
        if (bw != null) {
            try {
                bw.flush();
            } catch (IOException e) {
            }
        }
        loggerFile.setLastModified(System.currentTimeMillis());
    }

    public static void close() {
        if (bw != null) {
            try {
                bw.close();
            } catch (IOException e) {
            }
        }
    }
}
