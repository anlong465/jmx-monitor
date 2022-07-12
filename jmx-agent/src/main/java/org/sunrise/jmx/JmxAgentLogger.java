package org.sunrise.jmx;

import org.sunrise.jmx.agent.FileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JmxAgentLogger {
    private static File loggerFile = null;
    private static BufferedWriter bw = null;
    private static long startMs = System.currentTimeMillis();
    static {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        int pos = jvmName.indexOf('@');
        if (pos > 0) {
            jvmName = jvmName.substring(0, pos);
        }
        File root = new File("/tmp", "jmx-agent");
        root.mkdirs();

        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmm");

        loggerFile = new File(root, sf.format(new Date()) + "." + jvmName + ".out");
        try {
            bw = new BufferedWriter(new FileWriter(loggerFile));
            bw.write(System.getProperty("sun.java.command"));
            bw.newLine();
            bw.write("===============================================================\n");
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
        long duration = System.currentTimeMillis() - startMs;
        if (bw != null) {
            try {
                bw.write("===============================================================\n");
                bw.write("Duration: " + duration + "ms\n");
                bw.flush();

                bw.close();
            } catch (IOException e) {
            }
        }
        if (duration < 10000) {
            StringBuffer sb = new StringBuffer("\n\n");

            String fileName = loggerFile.getName();
            sb.append(fileName).append(":\n");
            try {
                FileUtil.readContent(loggerFile, sb);
                loggerFile.delete();
                File summary = new File(loggerFile.getParentFile(), fileName.substring(0, 8) + ".out");
                FileUtil.writeContent(summary, sb.toString());
            } catch (IOException e) {
            }
        }
    }
}
