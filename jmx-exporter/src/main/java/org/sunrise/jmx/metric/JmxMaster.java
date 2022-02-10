package org.sunrise.jmx.metric;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunrise.jmx.agent.CommonUtil;
import org.sunrise.jmx.agent.MetricTimer;
import org.sunrise.jmx.metric.storage.MetricsFactory;
import org.sunrise.jmx.server.svc.NodePrometheusCollector;
import org.sunrise.k8s.ContainerMgr;
import org.sunrise.k8s.K8sContainerMgr;
import org.sunrise.k8s.K8s1v5ContainerMgr;
import org.sunrise.k8s.OcpContainerMgr;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class JmxMaster extends JmxManager {
    private static final Logger logger = LoggerFactory.getLogger(JmxMaster.class);
    private final ContainerMgr containerApi;
    private JmxMaster(String vmType) {
        super(vmType);
        String clusterType = System.getenv("CLUSTER_TYPE");
        if (clusterType != null) {
            clusterType = clusterType.trim().toLowerCase();
        }
        if ("ocp".equals(clusterType)) {
            containerApi = new OcpContainerMgr();
        } else if ("k8s1.5".equals(clusterType)) {
            containerApi = new K8s1v5ContainerMgr();
        } else {
            containerApi = new K8sContainerMgr();
        }
        String podIP = System.getenv("POD_IP");
        if (podIP == null) {
            podIP = nodeName;
        }
        String selfURL = "http://" + podIP + ":5555" + "/upload/metrics/";

        agentArgs += selfURL + " " + getBasicAuth() + " ";
        logger.info(agentArgs);

    }
    protected VMInfo attachVM(String pid, VirtualMachineDescriptor vmd) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException {
        VMInfo vmi = super.attachVM(pid, vmd);
        fetchContainerInfo(vmi);
        return vmi;
    }
    private void fetchContainerInfo(VMInfo vmi) throws IOException {
        if (!isK8sPod || !containerApi.fetchContainerInfo(vmi)) {
            String javaCmd = vmi.vmSystemProperties.getProperty("sun.java.command");
            logger.debug("fetchContainerInfo: sun.java.command={}", javaCmd);

            vmi.containerName = CommonUtil.getJavaCmd(javaCmd);
            vmi.namespace = "User_" + vmi.vmSystemProperties.getProperty("user.name");
            vmi.podName = vmi.id;
        }
        vmi.vmSystemProperties = null;
    }
    public void addVM(String vmType, String id, String tmpdir) {
        VMInfo vmi = new VMInfo();
        vmi.id = id;
        vmi.setTmpdir(tmpdir);
        vmi.vmType = vmType;

        try {
            fetchContainerInfo(vmi);
            if ("jmx-exporter".equals(vmi.containerName)) {
                vmi.containerName = vmType + "-jmx-exporter";
            }
            allVms.add(vmi);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String saveMetrics(String pid, String metrics) {
        if (metrics == null || metrics.trim().length() == 0) {
            return "" + MetricsFactory.getNextPrometheusPullTime();
        }
        VMInfo vmi;
        if (pid.length() > 20) {
            vmi = findVMInfoViaPidFile(pid);
        } else {
            vmi = findVMInfo(pid);
        }
        if (vmi == null) return "0";
        return saveMetrics(vmi, metrics);
    }

    public VMInfo findVMInfoViaPidFile(String pidFileName) {
        for(VMInfo vm: allVms) {
            if (pidFileName.equals(vm.pidFileName)) {
                return vm;
            }
        }
        String pid = null;
        File pidFile = null;

        int pos = pidFileName.indexOf('-');
        String pidFileParent = pidFileName.substring(pos + 1).replaceAll(CommonUtil.SELF_ID_SEPARATOR, "/");

        if (isK8sPod) {
            File procRoot = new File(JmxConstants.NODE_PROC);
            File[] pidFiles = procRoot.listFiles();
            if (pidFiles != null) for(File fileItem : pidFiles) {
                if (!fileItem.isDirectory()) continue;
                pid = fileItem.getName();
                try {
                    Integer.parseInt(pid);
                    File podRoot = new File(fileItem, "root");
                    if (pos > 0) {
                        podRoot = new File(podRoot, pidFileParent);
                    }
                    pidFile = FileUtils.findFile(podRoot, pidFileName);
                    if (pidFile != null) break;
                } catch (NumberFormatException ignored) {
                }
            }
        } else {
            File procRoot = new File("/", pidFileParent);
            logger.info(pidFileName + ": " + procRoot.getAbsolutePath());
            pidFile = FileUtils.findFile(procRoot, pidFileName);
        }

        if (pidFile != null) {
            try {
                List<String> lines = FileUtils.readFileLines(pidFile.getAbsolutePath());
                if (pid == null) pid = lines.get(2);

                VMInfo vmi = super.findVMInfo(pid);
                if (vmi == null) {
                    vmi = new VMInfo();
                    vmi.id = pid;
                    vmi.vmType = "Unknown";

                    if(isK8sPod) {
                        fetchContainerInfo(vmi);
                    } else {
                        vmi.containerName = lines.get(1);
                        vmi.namespace = "User_" + lines.get(3);
                        vmi.podName = pid;
                    }

                    vmi.setTmpdir(lines.get(0));

                    allVms.add(vmi);
                    logger.info("allVms.size()=" + allVms.size());
                }
                vmi.pidFileName = pidFileName;

                pidFile.delete();
                logger.info("Delete pidFile: " + pidFile);
                return vmi;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        logger.warn("Failed to fetch VMInfo for {}.", pidFileName);

        return null;
    }

    protected String saveMetrics(VMInfo vmi, String metrics) {
        vmi.setMetrics(metrics);

        return "" + MetricTimer.getNextMetricTime();
    }

    public long checkIntervalWithLastPull() {
        return System.currentTimeMillis() - selfVmi.metricExpireTime;
    }

    public void exposeJVMMetrics(PrintWriter writer) {
        String metrics = selfVmi.getMetrics();
        if (metrics != null) {
            writer.append(metrics).append("\n");
        } else {
            logger.error("Empty metrics for JmxMaster");
        }
        for(VMInfo vmi : allVms) {
            metrics = vmi.getMetrics();
            if (metrics != null) {
                writer.append(metrics).append("\n");
            } else {
                logger.info("Empty metrics for {}", vmi.id);
            }
        }
    }

    public void exposeJVMMetrics(NodePrometheusCollector collector) {
        String metrics = selfVmi.getMetrics();
        if (metrics != null) {
            collector.addMetric(nodeName, metrics);
        } else {
            logger.error("Empty metrics for JmxMaster");
        }
        for(VMInfo vmi : allVms) {
            metrics = vmi.getMetrics();
            if (metrics != null) {
                collector.addMetric(nodeName, metrics);
            } else {
                logger.info("Empty metrics for {}", vmi.id);
            }
        }
    }


    private static JmxMaster server = null;
    public static JmxMaster getInstance() {
        if (server == null) {
            String vmType = System.getenv("JVM_MASTER");
            server = new JmxMaster(vmType);
        }
        return server;
    }

    public static String getUser() {
        return "jmx";
    }
    private final static String password = UUID.randomUUID().toString();
    public static String getPassword() {
        return password;
    }
    private static String basicAuth = null;
    public static String getBasicAuth() {
        if (basicAuth == null) {
            String auth = getUser() + ":" + getPassword();
            byte[] encodeBytes = Base64.encodeBase64(auth.getBytes());
            basicAuth = new String(encodeBytes, StandardCharsets.UTF_8);
        }
        return basicAuth;
    }
}
