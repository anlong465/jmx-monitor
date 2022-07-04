package org.sunrise.jmx.metric;

import org.sunrise.jmx.agent.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeMetricCollector {
    private final static long G = 1024*1024*1024;

    public static void getMetrics(String procRoot, Map<String, Number> guageMap) {
        File[] roots = File.listRoots();
        for(File root : roots) {
            guageMap.put("root " + root.getAbsolutePath() + " node_disk_maxSize", root.getTotalSpace()/G);
            guageMap.put("root " + root.getAbsolutePath() + " node_disk_freeSize", root.getUsableSpace()/G);
        }

        getMemMetrics(procRoot, guageMap);
        getCPUMetrics(procRoot, guageMap);
        getDiskIOMetrics(procRoot, guageMap);
    }

    private static void getMemMetrics(String procRoot, Map<String, Number> guageMap) {
        BufferedReader br = null;
        String oneLine = null;
        List<String> lines = new ArrayList<String>();
        int count = 0;

        try {
            br = new BufferedReader(new FileReader(new File(procRoot, "meminfo")));
            while((oneLine = br.readLine()) != null) {
                oneLine = oneLine.trim();
                if (oneLine.length() > 0){
                    lines.add(oneLine);
                    if (++count > 5) break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtil.doClose(br);
        }
        guageMap.put("node_mem_total", getMemMetric(lines.get(0)));
        guageMap.put("node_mem_free", getMemMetric(lines.get(1)));
        guageMap.put("node_mem_buff_cache", getMemMetric(lines.get(3)) + getMemMetric(lines.get(4)));
    }
    private static double getMemMetric(String metricLine) {
        int pos1 = metricLine.indexOf(':');
        int pos2 = metricLine.lastIndexOf(' ');
        return Double.parseDouble(metricLine.substring(pos1 + 1, pos2).trim())/1024;
    }

    private static void getCPUMetrics(String procRoot, Map<String, Number> guageMap) {
        BufferedReader br = null;
        boolean found = false;
        String oneLine = null;
        try {
            br = new BufferedReader(new FileReader(new File(procRoot, "stat")));
            while((oneLine = br.readLine()) != null) {
                if (oneLine.startsWith("cpu ")) {
                    found = true;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtil.doClose(br);
        }

        if (found) {
            CPUStat cpuStat = new CPUStat(oneLine);
            if (preCPUStat != null) {
                guageMap.put("node_cpu_total", cpuStat.total - preCPUStat.total);
                guageMap.put("node_cpu_user", cpuStat.user - preCPUStat.user);
                guageMap.put("node_cpu_idle", cpuStat.idle - preCPUStat.idle);
                guageMap.put("node_cpu_iowait", cpuStat.iowait - preCPUStat.iowait);
            }

            preCPUStat = cpuStat;
        }
    }

    private static CPUStat preCPUStat = null;
    private static class CPUStat {
        long total = 0;
        long user = 0;
        long idle = 0;
        long iowait = 0;

        public CPUStat(String stat) {
            String[] items = stat.split("\\s+");

            user = Long.parseLong(items[1]);
            total = user;

            total += Long.parseLong(items[2]);
            total += Long.parseLong(items[3]);

            idle = Long.parseLong(items[4]);
            total += idle;

            iowait = Long.parseLong(items[5]);
            total += iowait;

            for(int i = 6; i< items.length; i++) {
                total += Long.parseLong(items[i]);
            }
        }
    }

    private static void getDiskIOMetrics(String procRoot, Map<String, Number> guageMap) {
        BufferedReader br = null;
        String oneLine = null;
        List<String> lines = new ArrayList<String>();

        try {
            br = new BufferedReader(new FileReader(new File(procRoot, "diskstats")));
            while((oneLine = br.readLine()) != null) {
                oneLine = oneLine.trim();
                if (oneLine.length() > 0){
                    lines.add(oneLine);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtil.doClose(br);
        }

        boolean firstTime = false;
        if(preDiskStats == null) {
            firstTime = true;
            preDiskStats = new HashMap<String, DiskIOStat>();
        }
        for(String line : lines) {
            DiskIOStat pre = null;
            String[] items = line.split("\\s+");
            if (firstTime) {
                char lastCharOfDevName = items[2].charAt(items[2].length() - 1);
                if (lastCharOfDevName >= '0' && lastCharOfDevName <= '9') {
                    continue;
                }
            } else {
                pre = preDiskStats.get(items[2]);
                if(pre == null) continue;
            }

            DiskIOStat ioStat = new DiskIOStat(items);
            if (pre != null) {
                String prefix = "disk " + items[2] + " node_disk_";
//                guageMap.put(prefix + "totalMs", ioStat.millis - pre.millis);
                guageMap.put(prefix + "readNum", ioStat.readNum - pre.readNum);
                guageMap.put(prefix + "readHalfKB", ioStat.readHalfKB - pre.readHalfKB);
                guageMap.put(prefix + "readMs", ioStat.readMs - pre.readMs);
                guageMap.put(prefix + "writeNum", ioStat.writeNum - pre.writeNum);
                guageMap.put(prefix + "writeHalfKB", ioStat.writeHalfKB - pre.writeHalfKB);
                guageMap.put(prefix + "writeMs", ioStat.writeMs - pre.writeMs);
            }
            preDiskStats.put(items[2], ioStat);
        }
    }

    private static Map<String, DiskIOStat> preDiskStats = null;
    private static class DiskIOStat {
//        long millis = System.currentTimeMillis();
        long readNum = 0;
        long readHalfKB = 0;
        long readMs = 0;
        long writeNum = 0;
        long writeHalfKB = 0;
        long writeMs = 0;

        public DiskIOStat(String[] items) {
            readNum = Long.parseLong(items[3]);
            readHalfKB = Long.parseLong(items[5]);
            readMs = Long.parseLong(items[6]);
            writeNum = Long.parseLong(items[7]);
            writeHalfKB = Long.parseLong(items[9]);
            writeMs = Long.parseLong(items[10]);
        }
    }

}
