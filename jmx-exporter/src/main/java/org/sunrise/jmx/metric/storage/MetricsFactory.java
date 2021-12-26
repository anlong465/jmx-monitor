package org.sunrise.jmx.metric.storage;

import org.sunrise.jmx.metric.JmxConstants;
import org.sunrise.jmx.agent.MetricTimer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MetricsFactory {
    public final static String METRICS_DATA_ROOT = "/jmx-metrics-data";
    public final static int METRICS_MAX_UNIT = 512;

    private static MetricsIndex selfMetricsIndex = null;
    private static MetricsData selfMetricsData = null;

    public static void init(String nodeName) throws IOException {
        selfMetricsIndex = new MetricsIndex(nodeName, true);
        selfMetricsData = new MetricsData(nodeName, true);
    }

    private static long estimatedNextPublishTime = 0;

    public static synchronized long saveMetrics(String nodeName, String metrics) {
        int index = selfMetricsIndex.getNextIndex();
        selfMetricsData.saveMetrics(index, metrics);

        return getNextPrometheusPullTime();
    }
    public static synchronized long getNextPrometheusPullTime() {
        if (estimatedNextPublishTime > System.currentTimeMillis()) {
            return estimatedNextPublishTime;
        } else {
            MetricsPublish pub = null;
            try {
                pub = new MetricsPublish(false);
                long lastTime = pub.getLastPublishTime();
                lastTime += MetricTimer.METRICS_INTERNAL_MS;
                if (lastTime > System.currentTimeMillis()) {
                    estimatedNextPublishTime = lastTime;
                    return estimatedNextPublishTime;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (pub != null) pub.close();
            }
        }

        return 0;
    }

    public static List<String> readMetrics() throws IOException {
        List<String> result = new ArrayList<>();

        MetricsPublish pub = null;
        try {
            pub = new MetricsPublish(true);
            long startTime = pub.getLastPublishTime();
            long endTime = pub.getNewPublishTime();
            estimatedNextPublishTime = endTime + MetricTimer.METRICS_INTERNAL_MS;

            long endTimeWindow = endTime - MetricTimer.METRICS_INTERNAL_MS;
            if (startTime == 0 || startTime < endTimeWindow) {
                startTime = endTimeWindow;
            }
            startTime -= JmxConstants.METRICS_INTERNAL_OFFSET;
            endTime -= JmxConstants.METRICS_INTERNAL_OFFSET;
//            System.out.println("startTime: " + startTime);System.out.println("endTime: " + endTime);
            File nodesRoot = new File(METRICS_DATA_ROOT, "nodes");
            File[] nodes = nodesRoot.listFiles();
            for(File node : nodes) {
                readMetrics(result, node, startTime, endTime);
            }
        } finally {
            if (pub != null) pub.close();
        }

        return result;
    }

    private static void readMetrics(List<String> result, File nodeRoot, long startTime, long endTime) {
        MetricsIndex metricsIndex = null;
        MetricsData metricsData = null;
        try {
            metricsIndex = new MetricsIndex(nodeRoot, false);
            metricsData = new MetricsData(nodeRoot, false);
            metricsData.readMetrics(result, metricsIndex.getIndexCandidates(startTime, endTime));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (metricsIndex != null) metricsIndex.close();
            if (metricsData != null) metricsData.close();
        }
    }

}
