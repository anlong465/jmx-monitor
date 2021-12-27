package org.sunrise.jmx;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.sunrise.jmx.agent.CommonUtil;
import org.sunrise.jmx.agent.JmxAgentRunnable;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.List;

public class JvmAttacher {
    private static final String javaHome = System.getProperty("java.home");
    private static final String javaClassPath = System.getProperty("java.class.path");
    public static void main(String[] args) throws FileNotFoundException {
        String pid = initEnv(args);

        if (args.length == 1) {
            String javaCmd = javaHome + "/bin/java";
            String cp = javaClassPath;
            if (!cp.contains("tools.jar")) {
                File f = new File(javaHome, "lib/tools.jar");
                if (f.exists()) {
                    cp += ":" + f.getPath();
                } else {
                    f = new File(javaHome, "..");
                    f = new File(f, "lib/tools.jar");
                    if (f.exists()) {
                        cp += ":" + f.getPath();
                    }
                }
            }

            String nodeName = CommonUtil.getenv(new String[] {"_PAAS_NODE_NAME", "NODE_NAME"});
            String hostPort = CommonUtil.getenv(new String[] {"_PAAS_HOST_PORT", "HOST_PORT"}, "5555");

            String[] cmds = {javaCmd, "-cp", cp, "org.sunrise.jmx.JvmAttacher", args[0], pid, nodeName, hostPort};
            System.out.println(Arrays.toString(cmds));
            Runtime rt = Runtime.getRuntime();
            try {
                rt.exec(cmds);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else {
            prepare(args[0], args[1], pid, args[2], args[3]);
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        close();
    }

    private static void prepare(String agentJarPath, String parentPid, String pid, String nodeName, String hostPort) {
        int count = 1000;
        //agentArgs "ownerId sleep-time [jmxServerUrl auth] selfId"
        String agentArgs = "0 30000 " + JmxAgentRunnable.getDefaultJmxServerUrl(nodeName, hostPort) + " unused 0";
        while (count-- > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("\nTry to attach JVM with " + agentJarPath + " [" + count + "] ... ");

            try {
                if (attach(agentJarPath, agentArgs, parentPid, pid)) {
                    break;
                }
            } catch (Throwable th) {
                th.printStackTrace();
            }

            flush();
        }
    }

    private static boolean attach(String agentJarPath, String agentArgs, String parentPid, String pid) {
        List<VirtualMachineDescriptor> vmds = VirtualMachine.list();

        if (vmds.size() == 0) {
            System.out.println("Empty VirtualMachine.list()");
        }

        for (VirtualMachineDescriptor vmd : vmds) {
            System.out.println("=========================================================");
            System.out.println(vmd);

            if (pid.equals(vmd.id()) || parentPid.equals(vmd.id())) {
                continue;
            }
            try {
                VirtualMachine vm = VirtualMachine.attach(vmd);
                vm.loadAgent(agentJarPath, agentArgs);
                vm.detach();
                System.out.println(vmd.id() + " was attached successfully!!!");
                return true;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static void flush() {
        try {
            if (bos != null) bos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void close() {
        try {
            if (bos != null) {
                bos.flush();
                bos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedOutputStream bos = null;
    private static String initEnv(String[] args) throws FileNotFoundException {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String procName = runtime.getName(); // format: "pid@hostname"

        int pos = procName.indexOf('@');
        String pid = procName.substring(0, pos);

        bos = new BufferedOutputStream(
                new FileOutputStream("/tmp/JmxAttacher-" + pid + ".log"));
        PrintStream ps = new PrintStream(bos);
        System.setOut(ps);
        System.setErr(ps);

        System.out.println("JVM Runtime info: ");
        System.out.println("\tjava.home = " + javaHome);
        System.out.println("\tjava.class.path = " + javaClassPath);

        System.out.println("JvmAttacher arguments: ");
        for(String arg : args) {
            System.out.println("\t" + arg);
        }
        flush();
        return pid;
    }
}
