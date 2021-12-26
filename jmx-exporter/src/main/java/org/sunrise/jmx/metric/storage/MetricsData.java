package org.sunrise.jmx.metric.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MetricsData extends MetricsFile {
    private final static String METRICS_DATA_FILE = "metrics.dat";
    private final static int METRICS_MAX_SIZE = 16 * 1024;

    private final static long METRICS_DATA_FILE_SIZE = ((long) MetricsFactory.METRICS_MAX_UNIT) * METRICS_MAX_SIZE;

    public MetricsData(String nodeName, boolean canWrite) throws IOException {
        super(nodeName, METRICS_DATA_FILE, canWrite, METRICS_DATA_FILE_SIZE);
    }

    public MetricsData(File parent, boolean canWrite) throws IOException {
        super(parent, METRICS_DATA_FILE, canWrite, METRICS_DATA_FILE_SIZE);
    }

    @Override
    public void initMetricsFile() {
        for(int i = 0, pos = 0; i < MetricsFactory.METRICS_MAX_UNIT; i++) {
            mbb.putInt(pos, 0);
            pos += METRICS_MAX_SIZE;
        }
    }

    public void saveMetrics(int index, String metrics) {
        System.out.println("======================");
        System.out.println("index: " + index);
        System.out.println(metrics);
        System.out.println("======================\n");
        byte[] bytes = metrics.getBytes(StandardCharsets.UTF_8);

        int pos = index * METRICS_MAX_SIZE;
        mbb.putInt(pos, bytes.length);

//        mbb.put(bytes);
        pos += 4;
        for(int i = 0; i  < bytes.length; i++) {
            mbb.put(pos++, bytes[i]);
        }

        mbb.force();
    }

    public List<String> readMetrics(List<Integer> indexes) {
        return readMetrics(new ArrayList<String>(), indexes);
    }

    public List<String> readMetrics(List<String> result, List<Integer> indexes) {
        for(Integer index : indexes) {
            int pos = index * METRICS_MAX_SIZE;
            int len = mbb.getInt(pos);
            if (len < 1) continue;
            byte[] bytes = new byte[len];

//            mbb.get(bytes);
            pos += 4;
            for(int i = 0; i < len; i++) {
                bytes[i] = mbb.get(pos++);
            }

            String metric = new String(bytes, StandardCharsets.UTF_8);
            System.out.println("Index: " + index);
            System.out.println(metric);
            result.add(metric);
        }
        return result;
    }
}
