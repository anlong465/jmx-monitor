package org.sunrise.jmx.metric;

public class JmxConstants {
    public static String NODE_ROOT = "/._";
    public static String NODE_PROC = NODE_ROOT + "/proc/";
    public static String NODE_VAR = NODE_ROOT + "/var/";

    public static final long METRICS_INTERNAL_OFFSET = 1000;

    public static void resetForPrimer() {
        NODE_ROOT = "";
        NODE_PROC = NODE_ROOT + "/proc/";
        NODE_VAR = NODE_ROOT + "/var/";
    }

}
