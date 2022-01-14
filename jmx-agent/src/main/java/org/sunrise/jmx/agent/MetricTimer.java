package org.sunrise.jmx.agent;

public class MetricTimer {
//    private final static long JVM_SCAN_GAP = 20L * 1000;
//    public final static long METRIC_COLLECT_GAP = 25L * 1000;
    public static long METRICS_INTERNAL_MS = 30L*1000;
    public static long nextMetricTime = 0;

    public static long getNextMetricTime() {
        if (nextMetricTime == 0) {
            nextMetricTime = System.currentTimeMillis() + METRICS_INTERNAL_MS;
        }
        return nextMetricTime;
    }

    public static void resetNextMetricTime() {
        nextMetricTime = System.currentTimeMillis() + METRICS_INTERNAL_MS;
    }

    public static void setNextMetricTime(long next) {
        nextMetricTime = next;
    }

    private static boolean validForPullOnce = false;
    public static void setSinglePull(String singlePull) {
        if (singlePull != null && "true".equals(singlePull.trim().toLowerCase())) {
            validForPullOnce = true;
        }
    }

    public static boolean isSinglePull() {
        return validForPullOnce;
    }

    private static final long MILLS_PER_SECOND = 1000L;
    private static long MIN_METRIC_INTERNAL = 20L * MILLS_PER_SECOND;

}

