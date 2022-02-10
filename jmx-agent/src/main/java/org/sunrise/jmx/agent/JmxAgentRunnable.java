package org.sunrise.jmx.agent;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.UUID;

public class JmxAgentRunnable implements Runnable {
    private static long GLOBAL_OWNER_ID = 0;
    private final String jmxServerUrl;
    private final String selfId;
    private final long ownerId;
    private final long sleepMS;
    private final String auth;
    private final boolean makeSelfId;

    public JmxAgentRunnable(long ownerId, String selfId, long sleepMS, String jmxServerUrl, String auth) {
        GLOBAL_OWNER_ID = ownerId;
        this.jmxServerUrl = jmxServerUrl;
        this.ownerId = ownerId;

        if ("0".equals(selfId)) {
            makeSelfId = true;
            this.selfId = makeSelfId();
//            makePidFile();
        } else {
            makeSelfId = false;
            this.selfId = selfId;
        }

        this.sleepMS = sleepMS;
        this.auth = "Basic " + auth;
        if (JmxMetricPusher.uploadToSvrCount != 3) JmxMetricPusher.uploadToSvrCount = 3;
        System.out.println("ownerId: " + ownerId);
        System.out.println("selfId: " + selfId + ", --> " + this.selfId);
        System.out.println("sleepMS: " + sleepMS);
        System.out.println("jmxServerUrl: " + jmxServerUrl);
    }

    public void run() {
        while(this.ownerId == JmxAgentRunnable.GLOBAL_OWNER_ID) {
            try {
                String metrics = JmxMetricCollector.getJVMMetricAsJsonString();
//                System.out.println("JMX Metrics: " + selfId + " --> " + metrics);

                Long nextMetricTime = JmxMetricPusher.pushMetrics(jmxServerUrl, auth, selfId, metrics);
                if (nextMetricTime != null) {
                    if (makeSelfId && nextMetricTime == 0) {
                        makePidFile();
                        nextMetricTime = JmxMetricPusher.pushMetrics(jmxServerUrl, auth, selfId, metrics);
                    }
                    MetricTimer.setNextMetricTime(nextMetricTime);
                } else {
                    MetricTimer.resetNextMetricTime();
                }
            } catch (Throwable th) {
                MetricTimer.resetNextMetricTime();
                CommonUtil.logException(th);
            }

//            long toSleep = MetricTimer.getNextMetricCollectTime() - System.currentTimeMillis();
            long toSleep = MetricTimer.getNextMetricTime() - System.currentTimeMillis();

            if (toSleep > 0) {
                try {
                    Thread.sleep(toSleep);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private static String makeSelfId() {
        String tmpdir = System.getProperty("java.io.tmpdir");
        if (tmpdir == null) tmpdir = "/tmp";

        String selfId = UUID.randomUUID().toString().replaceAll("-", "");

        String[] items = tmpdir.substring(1).split("/");
        selfId = selfId + "-" + items[0];
        for(int i = 1; i < items.length; i++) {
            String item = items[i];
            if (item.contains(CommonUtil.SELF_ID_SEPARATOR)) break;
            selfId += CommonUtil.SELF_ID_SEPARATOR + item;
        }

        return selfId;
    }

    private void makePidFile() {
        String tmpdir = System.getProperty("java.io.tmpdir");
        if (tmpdir == null) tmpdir = "/tmp";

        String javaCmd = CommonUtil.getJavaCmd();

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String procName = runtime.getName(); // format: "pid@hostname"
        int pos = procName.indexOf('@');
        String pid = procName.substring(0, pos);

        File f = new File(tmpdir, selfId);
        try {
            FileUtil.writeContent(f, tmpdir + "\n" + javaCmd + "\n" + pid +
                    "\n" + System.getProperty("user.name"));
            System.out.println("makePidFile: " + f);
            f.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare pidFile", e);
        }
    }


    public static Thread getDefaultAgent(String hostPort) {
        String jmxServerUrl = getDefaultJmxServerUrl(null, hostPort);
        if (jmxServerUrl == null) {
            return null;
        }

        long ownerId = System.nanoTime();
        if (ownerId > 0) ownerId = -1 * ownerId;

        Runnable r = new JmxAgentRunnable(ownerId, "0", 30000, jmxServerUrl, "");
        Thread th = new Thread(r);
        th.setDaemon(true);
        return th;
    }

    public static String getDefaultJmxServerUrl(String nodeName, String hostPort) {
        if (nodeName == null) {
            nodeName = CommonUtil.getenv(new String[] {"_PAAS_NODE_NAME", "NODE_NAME"});
        }
        if (nodeName == null) {
            String procName = ManagementFactory.getRuntimeMXBean().getName(); // format: "pid@hostname"

            int pos = procName.indexOf('@');
            nodeName = procName.substring(pos + 1);
            System.err.println("Failed to get NodeName from environment, and use the default hostname: " + nodeName);
        }
        if (hostPort != null) {
            try {
                Integer.parseInt(hostPort);
            } catch (NumberFormatException ex) {
                hostPort = CommonUtil.getenv(new String[] {"_PAAS_HOST_PORT", "HOST_PORT"}, "5555");
            }
        } else {
            hostPort = CommonUtil.getenv(new String[] {"_PAAS_HOST_PORT", "HOST_PORT"}, "5555");
        }

        return "http://" + nodeName + ":" + hostPort + "/upload/metrics/";
    }

    public static boolean isInitiatedByServer() {
        return GLOBAL_OWNER_ID > 0;
    }

}
