package org.sunrise.jmx.metric;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.util.Map;

public class JVMClassLoadingCollector {
    public static void getGCMetrics(Map<String, Number> guageMap, Map<String, Number> counterMap) {
        ClassLoadingMXBean classLoading = ManagementFactory.getClassLoadingMXBean();
        counterMap.put("jvm_class_active_total", classLoading.getLoadedClassCount());
        counterMap.put("jvm_class_unloaded_total", classLoading.getUnloadedClassCount());
    }
}
