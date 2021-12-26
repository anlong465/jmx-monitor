package org.sunrise.jmx.metric.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MetricsIndex extends MetricsFile {
    private final static String METRICS_INDEX_FILE = "metrics.idx";
    private final static long METRICS_INDEX_FILE_SIZE = 4 + 4 + MetricsFactory.METRICS_MAX_UNIT * 8;

    public MetricsIndex(String nodeName, boolean canWrite) throws IOException {
        super(nodeName, METRICS_INDEX_FILE, canWrite, METRICS_INDEX_FILE_SIZE);
    }

    public MetricsIndex(File parent, boolean canWrite) throws IOException {
        super(parent, METRICS_INDEX_FILE, canWrite, METRICS_INDEX_FILE_SIZE);
    }

    public int getNextIndex() {
        int index = mbb.getInt(4);

        int nextIndex = (index + 1) % MetricsFactory.METRICS_MAX_UNIT;
        mbb.putInt(4, nextIndex);
        mbb.putLong(8 + index * 8, System.currentTimeMillis());
        mbb.force();

        return index;
    }

    public List<Integer> getIndexCandidates(long startTime, long endTime) {
        List<Integer> result = new ArrayList<>();
        int pos = 8;
        for(int i = 0; i < MetricsFactory.METRICS_MAX_UNIT; i++) {
            long time = mbb.getLong(pos);
            pos += 8;
            if (time >= startTime && time < endTime) {
                result.add(i);
            }
        }

        System.out.println("getIndexCandidates: " + result);

        return result;
    }


    @Override
    public void initMetricsFile() {
        mbb.putInt(0, MetricsFactory.METRICS_MAX_UNIT);
        mbb.putInt(4, 0);

        for(int i = 0; i < MetricsFactory.METRICS_MAX_UNIT; ) {
            i++;
            mbb.putLong(i, 0);
        }

        mbb.force();
    }
}
