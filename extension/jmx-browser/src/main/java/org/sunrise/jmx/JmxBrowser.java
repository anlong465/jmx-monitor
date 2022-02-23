package org.sunrise.jmx;

import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import javax.management.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;

public class JmxBrowser {
    public static void main(String[] args) throws FileNotFoundException {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String procName = runtime.getName(); // format: "pid@hostname"

        int pos = procName.indexOf('@');
        String selfId = procName.substring(0, pos);

        String javaClassPath = System.getProperty("java.class.path");
        String[] jars = javaClassPath.split(":");
        if (jars.length > 1) {
            for(String jar : jars) {
                if (jar.contains("jmx-browser")) {
                    javaClassPath = jar;
                    break;
                }
            }
        }

        StringBuilder sb = new StringBuilder( "-" + selfId);
        if (args != null && args.length > 0) {
            for(int i = 1; i < args.length; i++) {
                sb.append(" ").append(args[i]);
            }
//            for(String arg : args) sb.append(" ").append(arg);
        }
        String agentArgs = sb.toString();

        System.out.println("javaClassPath: " + javaClassPath);
        System.out.println("agentArgs: " + agentArgs);

        List<VirtualMachineDescriptor> vmds = VirtualMachine.list();
        if(vmds.size() == 0) {
            System.out.println("Could not find any JVM");
        }

        for(VirtualMachineDescriptor vmd : vmds) {
            if (vmd.id().equals(selfId)) {
                continue;
            }

            if (args != null && args.length > 0) {
                if (!vmd.id().equals(args[0])) continue;
            }

            System.out.println("=========================================================");
            System.out.println(vmd);
            System.out.println("---------------------------------------------------------");

            VirtualMachine vm = null;
            BufferedReader br = null;
            try {
                try {
                    vm = VirtualMachine.attach(vmd);
                    vm.loadAgent(javaClassPath, vmd.id() + agentArgs);
                } catch (AgentLoadException e) {
                    if (!"0".equals(e.getMessage())) {
                        throw e;
                    }
                }
                Properties vmSystemProperties = vm.getSystemProperties();
//                System.out.println(vmSystemProperties);

                String tmpDir = vmSystemProperties.getProperty("java.io.tmpdir");
                System.out.println(tmpDir);
                if (tmpDir.charAt(0) != '/') {
                    File f = new File(vmSystemProperties.getProperty("user.home"), tmpDir);
//                    System.out.println(f.getCanonicalPath());
                    tmpDir = f.getCanonicalPath();
                }
                File vmInfo = new File(tmpDir, vmd.id() + "-" + selfId + ".txt");
                System.out.println(vmInfo.getAbsolutePath());
                br = new BufferedReader(new FileReader(vmInfo));
                String line;
                while((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                if (br != null) { try { br.close(); } catch (IOException e) {}}
                if (vm != null) { try { vm.detach(); } catch (IOException e) {}}
            }
            System.out.println("_________________________________________________________");
        }
    }

    public static void agentmain(final String agentArgs) {
        String[] items = agentArgs.split(" ");

        String infoFileName = items[0] + ".txt";
        String beanName = (items.length > 1) ? items[1] : null;
        String beanAttrName = (items.length > 2) ? items[2] : null;

        File infoFile = new File(System.getProperty("java.io.tmpdir"), infoFileName);
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(infoFile));
            MBeanServer mServer = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectInstance> allBeans = mServer.queryMBeans(null, null);
            bw.write("Total beans: " + allBeans.size() + "\n");

            if (beanName == null) {
                List<String> list = new ArrayList<String>();
                for(ObjectInstance bean: allBeans) {
                    ObjectName on = bean.getObjectName();
                    String onName = on.toString();
                    list.add("******: " + onName + "\n");
                }
                Collections.sort(list);
                for(String item : list) {
                    bw.write(item);
                }
            } else {
                for(ObjectInstance bean: allBeans) {
                    ObjectName on = bean.getObjectName();
                    String onName = on.toString();

                    if (!onName.contains(beanName)) continue;

                    bw.write("******: " + onName + "\n");
                    MBeanInfo beanInfo = mServer.getMBeanInfo(on);
                    MBeanAttributeInfo[] attrInfos = beanInfo.getAttributes();
                    MBeanOperationInfo[] operInfos = beanInfo.getOperations();
                    if (attrInfos != null && attrInfos.length > 0) {
                        for (MBeanAttributeInfo attrInfo : attrInfos) {
                            if (beanAttrName == null) {
                                bw.write("**************: \t" + attrInfo.getName() + "\n");
                            } else {
                                if (attrInfo.getName().equals(beanAttrName)) {
                                    bw.write("**************: \t" + beanAttrName);
                                    bw.write("=" + mServer.getAttribute(on, beanAttrName).toString());
                                    bw.newLine();
                                    break;
                                }
                            }
                        }
                    }

                    if (operInfos != null && operInfos.length > 0) {
                        for (MBeanOperationInfo operInfo : operInfos) {
                            if (beanAttrName == null) {
                                bw.write("==============: \t" + operInfo.getName() + "\n");
                            } else {
                                if (operInfo.getName().equals(beanAttrName)) {
                                    bw.write("==============: \t" + beanAttrName);
                                    bw.write(" result: \n");
                                    bw.write(mServer.invoke(on, beanAttrName, null, null).toString());
                                    bw.newLine();
                                    break;
                                }
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (bw != null) { try { bw.close(); } catch (IOException e) {}}
        }

    }

}
