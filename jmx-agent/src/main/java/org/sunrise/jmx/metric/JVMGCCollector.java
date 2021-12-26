package org.sunrise.jmx.metric;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

public class JVMGCCollector {
    private static GCStat previousStat = null;

    public static void getGCMetrics(Map<String, Number> guageMap) {
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        GCStat currentStat = new GCStat();

        for(GarbageCollectorMXBean gc : gcs) {
            String name = gc.getName();
//            System.out.println("------------- GC Name: " + name);
            if (isFullGC(name)) {
                currentStat.fullGCCount += gc.getCollectionCount();
                currentStat.fullGCTime += gc.getCollectionTime();
            } else {
                currentStat.minorGCCount += gc.getCollectionCount();
                currentStat.minorGCTime += gc.getCollectionTime();
            }
        }

        if (previousStat != null) {
            guageMap.put("jvm_gc_minor_count",
                    currentStat.minorGCCount - previousStat.minorGCCount);
            guageMap.put("jvm_gc_minor_time_ms",
                    currentStat.minorGCTime - previousStat.minorGCTime);
            guageMap.put("jvm_gc_full_count",
                    currentStat.fullGCCount - previousStat.fullGCCount);
            guageMap.put("jvm_gc_full_time_ms",
                    currentStat.fullGCTime - previousStat.fullGCTime);
        }
        previousStat = currentStat;
    }

    private static class GCStat {
        long minorGCCount = 0;
        long minorGCTime = 0;
        long fullGCCount = 0;
        long fullGCTime = 0;
    }

    public static boolean isFullGC(String gcName) {
        return gcName.contains("MarkSweep") || gcName.contains("Old Collector")
                || gcName.contains("global") || gcName.contains("G1 Old ");
    }
}
