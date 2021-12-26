package org.sunrise.jmx.metric;

import org.sunrise.jmx.agent.CommonUtil;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JVMMemoryCollector {
    private final static List<String> heapMemoryPoolNames = new ArrayList<String>();
    public static void getMemoryMetrics(Map<String, Number> guageMap) {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();

        MemoryUsage heapUsage = memory.getHeapMemoryUsage();
        MemoryUsage nonheapUsage = memory.getNonHeapMemoryUsage();

        dump(guageMap, heapUsage, nonheapUsage,"jvm");
        dump(guageMap, heapUsage, "jvm_heap");
        dump(guageMap, nonheapUsage, "jvm_nonheap");

        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for(MemoryPoolMXBean mp : memoryPoolMXBeans) {
            String prefix;
            String name = refineMemoryPoolName(mp.getName());
            if (mp.getType().equals(MemoryType.HEAP)) {
                if (!heapMemoryPoolNames.contains(name)) {
                    heapMemoryPoolNames.add(name);
                }
                prefix = "jvm_heap_mp";
            } else {
                prefix = "jvm_nonheap_mp";
            }
            prefix = "poolName " + name + " " + prefix;
            dump(guageMap, mp.getUsage(), prefix);
        }
    }

    public static boolean isHeapMemoryPool(String name) {
        return heapMemoryPoolNames.contains(name);
    }

    public static String refineMemoryPoolName(String name) {
        return name.replaceAll("'","").replaceAll("-","_");
    }

    private static void dump(Map<String, Number> result, MemoryUsage mu, String prefix) {
        long max = mu.getMax();
        long committed = mu.getCommitted();
        long used = mu.getUsed();
        if (max < committed) max = committed;
        result.put(prefix + "_max_mb", CommonUtil.bytes2MB(max));
        result.put(prefix + "_used_mb", CommonUtil.bytes2MB(used));
        result.put(prefix + "_committed_mb", CommonUtil.bytes2MB(committed));
    }

    private static void dump(Map<String, Number> result, MemoryUsage mu1, MemoryUsage mu2, String prefix) {
        long max1 = mu1.getMax();
        long committed1 = mu1.getCommitted();
        if (max1 < committed1) max1 = committed1;
        long max2 = mu2.getMax();
        long committed2 = mu2.getCommitted();
        if (max2 < committed2) max2 = committed2;
        result.put(prefix + "_max_mb", CommonUtil.bytes2MB(max1 + max2));
        result.put(prefix + "_used_mb", CommonUtil.bytes2MB(mu1.getUsed() + mu2.getUsed()));
        result.put(prefix + "_committed_mb", CommonUtil.bytes2MB(committed1 + committed2));
    }
}
