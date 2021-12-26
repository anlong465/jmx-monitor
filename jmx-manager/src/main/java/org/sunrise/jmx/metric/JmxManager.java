package org.sunrise.jmx.metric;

import com.sun.tools.attach.*;
import org.sunrise.jmx.agent.JmxMetricCollector;
import org.sunrise.jmx.agent.MetricTimer;
import org.sunrise.k8s.K8sPodMgr;
import org.sunrise.k8s.PodMgr;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;


public abstract class JmxManager implements Runnable {
//    private static final Logger logger = LoggerFactory.getLogger(JmxManager.class);
    private static final String agentJarName = "jmx-agent-1.0.jar";
    private static final String agentJarDestination = "/tmp/" + agentJarName;
    private static final String agentJarSource = "/app/" + agentJarName;

    protected List<VMInfo> allVms = new ArrayList<>();

    protected String nodeName;
    protected String agentArgs;
    protected VMInfo selfVmi = new VMInfo();

    protected PodMgr podMgr = new K8sPodMgr();

    protected final String vmType;
    protected boolean isK8sPod = true;
    protected JmxManager(String vmType) {
        this.vmType = vmType;

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String procName = runtime.getName(); // format: "pid@hostname"

        int pos = procName.indexOf('@');
        selfVmi.id = procName.substring(0, pos);
        selfVmi.podName = selfVmi.id;
        if (vmType == null || vmType.length() == 0) {
            selfVmi.containerName = "jmx-exporter";
        } else {
            selfVmi.containerName = vmType + "-jmx-exporter";
        }
        selfVmi.namespace = System.getenv("POD_NAMESPACE");
        if (selfVmi.namespace == null || selfVmi.namespace.trim().length() == 0) {
            selfVmi.namespace = "User_" + System.getProperty("user.name");
            isK8sPod = false;
            JmxConstants.resetForPrimer();
        }
        selfVmi.vmType = vmType;

        nodeName = System.getenv("NODE_NAME");
        if (nodeName == null && selfVmi.podName != null) {
            nodeName = procName.substring(pos + 1);
        }
        MetricTimer.setSinglePull(System.getenv("SINGLE_PULL"));

        agentArgs = selfVmi.id + " " + MetricTimer.METRICS_INTERNAL_MS + " ";
    }


    protected void checkJVM() {
        List<VirtualMachineDescriptor> vmds = VirtualMachine.list();

        for(VirtualMachineDescriptor vmd : vmds) {
//            System.out.println("=========================================================");
//            System.out.println(vmd);

            if (vmd.id().equals(selfVmi.id)) {
                String selfMetrics = JmxMetricCollector.getJVMMetricAsJsonString();
                selfVmi.setMetrics(selfMetrics);
//                saveMetrics(selfVmi, selfMetrics);
                continue;
            }

            try {
                processVM(vmd.id(), vmd);
            } catch (Throwable e) {
//                logger.error("Failed to attach JVM: {}", vmd);
                System.err.println("Failed to attach JVM: " + vmd);
                e.printStackTrace();
            }
        }
        for(int i = allVms.size() - 1; i >= 0; i--) {
            VMInfo vmi = allVms.get(i);
            if (vmi.isRemovable()) {
                allVms.remove(i);
            }
        }
    }

    protected abstract String saveMetrics(VMInfo vmi, String metrics);

    public void processVM(String pid, VirtualMachineDescriptor vmd) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException {
        VMInfo vmi = findVMInfo(pid);
        if (vmi != null) {
            vmi.active();
            return;
        }
        allVms.add(attachVM(pid, vmd));

    }

    protected VMInfo attachVM(String pid, VirtualMachineDescriptor vmd) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException {
        VMInfo vmi = new VMInfo();
        vmi.id = pid;
        vmi.vmType = vmType;

        if (isK8sPod) copyAgent(vmi.id);

        VirtualMachine vm;
        if (vmd != null) {
            vm = VirtualMachine.attach(vmd);
        } else {
            vm = VirtualMachine.attach(pid);
        }

//        System.out.println("before loadAgent");
        try {
            vm.loadAgent(agentJarDestination, agentArgs + vmi.id);
        } catch (AgentLoadException e) {
            if (!"0".equals(e.getMessage())) {
                throw e;
            }
        }
        vmi.vmSystemProperties = vm.getSystemProperties();
//        System.out.println("SystemProperties:\n" + vm.getSystemProperties());
//        System.out.println("AgentProperties:\n" + vm.getAgentProperties());
        vmi.setTmpdir(vmi.vmSystemProperties.getProperty("java.io.tmpdir"));

//        System.out.println("after loadAgent");

        vm.detach();

        return vmi;
    }

    private void copyAgent(String pid) throws IOException {
        File source = new File(agentJarSource);
        try {
            copyAgent(source, JmxConstants.NODE_PROC + pid + "/root" + agentJarDestination);
        } catch (IOException ex) {
            String overlayRoot = podMgr.fetchPodOverlayRoot(pid);
            if (overlayRoot == null) {
                throw new RuntimeException("Failed to figure out POD overlay Root for pid: " + pid);
            }
            overlayRoot = JmxConstants.NODE_ROOT + overlayRoot;
            copyAgent(source, overlayRoot + "diff/tmp/" + agentJarName);
            copyAgent(source, overlayRoot + "merged/tmp/" + agentJarName);
        }
    }

    private boolean copyAgent(File source, String destinationPath) throws IOException {
        File destination = new File(destinationPath);

        if (!destination.exists() || source.length() != destination.length()) {
            FileUtils.copyFile(source, destination);
            return true;
        }
        return false;
    }

    public VMInfo findVMInfo(String id) {
        for(VMInfo vm: allVms) {
            if (id.equals(vm.id)) {
                return vm;
            }
        }
        return null;
    }

    @Override
    public void run() {
        long nextTime = System.currentTimeMillis();
        while(true) {
            nextTime += MetricTimer.METRICS_INTERNAL_MS;
            MetricTimer.setNextMetricTime(nextTime);
            checkJVM();

            long toSleep = nextTime - System.currentTimeMillis();
            if (toSleep > 10) {
                try {
                    Thread.sleep(toSleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}

