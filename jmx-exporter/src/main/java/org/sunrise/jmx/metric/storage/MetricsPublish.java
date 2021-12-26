package org.sunrise.jmx.metric.storage;

import org.sunrise.jmx.metric.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class MetricsPublish {
    private final static String METRICS_PUBLISH_FILE = "metrics.pub";
    private final static long METRICS_PUBLISH_FILE_SIZE = 8;
    private RandomAccessFile raf = null;
    private FileChannel fc = null;
    protected MappedByteBuffer mbb;

    public MetricsPublish(boolean canWrite) throws IOException {
        File pubFile = new File(MetricsFactory.METRICS_DATA_ROOT, METRICS_PUBLISH_FILE);
        boolean isNewFile = false;
        if (pubFile.exists()) {
            if (pubFile.length() != METRICS_PUBLISH_FILE_SIZE) {
                isNewFile = true;
                FileUtils.initFile(pubFile, (int) METRICS_PUBLISH_FILE_SIZE);
            }
        } else {
            pubFile.createNewFile();
            isNewFile = true;
            FileUtils.initFile(pubFile, (int) METRICS_PUBLISH_FILE_SIZE);
        }

        raf = new RandomAccessFile(pubFile, canWrite ? "rw":"r");
        fc = raf.getChannel();
        mbb = fc.map(canWrite ? FileChannel.MapMode.READ_WRITE : FileChannel.MapMode.READ_ONLY,
                0, METRICS_PUBLISH_FILE_SIZE);

        if (isNewFile && canWrite) {
            initMetricsFile();
        }
    }

    private void initMetricsFile() {
        mbb.putLong(0, 0);
        mbb.force();
    }

    public void close() {
        mbb = null;
        FileUtils.doClose(fc);
        FileUtils.doClose(raf);
    }

    public long getLastPublishTime() {
        return mbb.getLong(0);
    }

    public long getNewPublishTime() {
        FileLock lock = null;
        try {
            lock = fc.lock();
            long ts = System.currentTimeMillis();
            mbb.putLong(0, ts);
            mbb.force();
            return ts;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (lock != null) {
                try { lock.release(); } catch (IOException e) { }
            }
        }
        return 0;
    }
}
