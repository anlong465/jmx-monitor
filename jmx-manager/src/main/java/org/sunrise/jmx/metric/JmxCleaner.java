package org.sunrise.jmx.metric;

import java.io.File;

public class JmxCleaner {
    private static File jmxLoggerRoot = null;
    static {
        jmxLoggerRoot = new File("/tmp", "jmx-agent");
        jmxLoggerRoot.mkdirs();
    }

    private final static long MILLIS_PER_MINUTE = 1000L * 60;
    private final static long MAX_IDLE_MILLIS = MILLIS_PER_MINUTE * 60 * 24;
    private final static long NEXT_CHECK_INTERNAL_MILLIS = MILLIS_PER_MINUTE * 120;

    private static long nextCheckMillis = 0;
    public static void check() {
        if (jmxLoggerRoot == null) return;
        try {
            if (nextCheckMillis < System.currentTimeMillis()) {
                long oldMillis = System.currentTimeMillis() - MAX_IDLE_MILLIS;
                File[] kids = jmxLoggerRoot.listFiles();
                for(File kid : kids) {
                    String name = kid.getName();
                    if (name.endsWith(".out")) {
                        if (kid.lastModified() < oldMillis) {
                            try {
                                Double.parseDouble(name.substring(0, name.length()-4));
                                kid.delete();
                            } catch (Throwable ex){}
                        }
                    }
                }
                nextCheckMillis = System.currentTimeMillis() + NEXT_CHECK_INTERNAL_MILLIS;
            }
        } catch (Throwable th) {}
    }

}
