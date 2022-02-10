package org.sunrise.jmx.metric;

import org.sunrise.jmx.agent.MetricExchanger;
import org.sunrise.jmx.agent.MetricTimer;

import java.io.IOException;
import java.util.Properties;

public class VMInfo {
    private final static int MAX_AGE = 2;
    public String id;
    public String namespace;
    public String podName;
    public String containerName;
    private String metrics = null;
    private int age = MAX_AGE;
    private String tmpdir = "/tmp";
    public String vmType = null;
    public String pidFileName = null;
    private MetricExchanger exchanger = null;
    public Properties vmSystemProperties = null;

    public long metricExpireTime = System.currentTimeMillis() + 40L*1000;

    public void active() {
        age = MAX_AGE;
    }

    public boolean isRemovable() {
        if (--age < 0) {
            if (exchanger != null) exchanger.close();
            return true;
        } else {
            return false;
        }
    }

    public void setTmpdir(String tmpdir) {
        if (tmpdir != null) {
            tmpdir = tmpdir.trim();
            if (tmpdir.length() > 0) {
                this.tmpdir = tmpdir;
            }
        }
    }

    public String getTmpdir() {
        return tmpdir;
    }

    public void setMetrics(String metrics) {
        if (metrics != null && metrics.trim().length() > 0) {
            this.metrics = metrics.trim();
            metricExpireTime = System.currentTimeMillis() + 40L*1000;
            active();
        }
    }

    public String getMetrics() {
        long current = System.currentTimeMillis();
        if (current > metricExpireTime) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        sb.append(namespace).append(',')
          .append(podName).append(',')
          .append(containerName).append('#');
        if (metrics == null) {
            if (exchanger == null) {
                exchanger = MetricExchanger.makeExchangerForServerToClient(JmxConstants.NODE_PROC, tmpdir, id);
            }

            try {
                String result = exchanger.writeRead("" + MetricTimer.getNextMetricTime());
                if (result != null && result.trim().length() > 0) {
                    active();
                    sb.append(result);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            sb.append(metrics);
//            metrics = null;
        }

        if (MetricTimer.isSinglePull()) {
            metricExpireTime = current;
        }

        return sb.toString();
    }
}
