package org.sunrise.jmx.metric.storage;

import org.sunrise.jmx.metric.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public abstract class MetricsFile {
    private RandomAccessFile raf = null;
    private FileChannel fc = null;
    protected MappedByteBuffer mbb;

    public MetricsFile(String nodeName, String fileName, boolean canWrite, long maxSize) throws IOException {
        File root = new File(MetricsFactory.METRICS_DATA_ROOT + "/nodes", nodeName);
        if (!root.exists()) {
            root.mkdirs();
        }
        init(root, fileName, canWrite, maxSize);
    }
    public MetricsFile(File parent, String fileName, boolean canWrite, long maxSize) throws IOException {
        init(parent, fileName, canWrite, maxSize);
    }

    private void init(File parent, String fileName, boolean canWrite, long maxSize) throws IOException {
        File metricsFile = new File(parent, fileName);
        boolean isNewFile = false;
        if (!metricsFile.exists()) {
            metricsFile.createNewFile();
            isNewFile = true;
        }

        raf = new RandomAccessFile(metricsFile, canWrite ? "rw":"r");
        fc = raf.getChannel();
        if (isNewFile && canWrite) {
            ByteBuffer buf = ByteBuffer.allocate((int) maxSize);
            fc.write(buf);
            fc.force(true);
        }

        mbb = fc.map(canWrite ? FileChannel.MapMode.READ_WRITE : FileChannel.MapMode.READ_ONLY, 0, maxSize);

        if (isNewFile && canWrite) {
            initMetricsFile();
        }
    }


    public abstract void initMetricsFile();

    public void close() {
        mbb = null;
        FileUtils.doClose(fc);
        FileUtils.doClose(raf);
    }
}
