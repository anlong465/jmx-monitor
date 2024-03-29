package org.sunrise.jmx.agent;

import org.sunrise.jmx.JmxAgentLogger;
import org.sunrise.jmx.metric.*;
import org.sunrise.jmx.metric.mbean.*;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class JmxMetricCollector {
    private static void mapToJson(Map<String, Number> metrics, StringBuffer sb) {
        sb.append("{");
        boolean nonFirst = false;
        for(String key : metrics.keySet()) {
            if (nonFirst) {
                sb.append(",");
            } else {
                nonFirst = true;
            }
            sb.append("\"").append(key).append("\":").append(metrics.get(key));
        }
        sb.append("}");
    }

    public static String getJVMMetricAsJsonString(String procRoot) {
        Map<String, Map<String, Number>> metrics = getJVMMetric(procRoot);
        if (metrics.isEmpty()) return null;

        StringBuffer sb = new StringBuffer("{");
        int i = 0;
        for(String key : metrics.keySet()) {
            if ((i++) != 0) sb.append(",");
            sb.append("\"").append(key).append("\":");
            mapToJson(metrics.get(key), sb);
        }
        sb.append("}");

        return sb.toString();
    }

    private static int tryMBeanCount = 3;
    private static MBeanMetricCollector[] beanHandlers;
    static {
        try {
            beanHandlers = new MBeanMetricCollector[] {
                    new WebSphereDatasourceMetricCollector(),
                    new DruidDatasourceMetricCollector(),
                    new TomcatDatasourceMetricCollector(),
                    new GCMemoryMetricCollector(),
                    new ProxyMetricCollector()
            };
        } catch (MalformedObjectNameException e) {
            dumpException(e);
        }
    }
    private static MBeanMetricCollector getJDKMetricCollector(String jdkClassName, String metricCollectorClassName) {
        try {
            Class.forName(jdkClassName);

            try {
                Class<?> clazz = Class.forName(metricCollectorClassName);
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                return (MBeanMetricCollector) constructor.newInstance();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {}

        return null;
    }
    private static final MBeanMetricCollector jdk7MetricCollector = getJDKMetricCollector(
            "java.lang.management.BufferPoolMXBean",
            "org.sunrise.jmx.agent.jdk7.JDK7MetricCollector");

    public static Map<String, Map<String, Number>> getJVMMetric(String procRoot) {
        Map<String, Map<String, Number>> result = new HashMap<String, Map<String, Number>>();

        Map<String, Number> guageMap = new HashMap<String, Number>();
        result.put("guage", guageMap);
        Map<String, Number> counterMap = new HashMap<String, Number>();
        result.put("counter", counterMap);

        if (procRoot != null) {
            NodeMetricCollector.getMetrics(procRoot, guageMap);
        }

        JVMMemoryCollector.getMemoryMetrics(guageMap);
        JVMGCCollector.getGCMetrics(guageMap);
        JVMThreadCollector.getGCMetrics(guageMap, counterMap);
        JVMClassLoadingCollector.getGCMetrics(guageMap, counterMap);
        if (jdk7MetricCollector != null) jdk7MetricCollector.process(null, guageMap, counterMap);

        if (tryMBeanCount > 0) {
//            NanoTimeWatcher watcher = new NanoTimeWatcher();
            try {
                MBeanServer mServer = ManagementFactory.getPlatformMBeanServer();
                for(MBeanMetricCollector handler : beanHandlers) {
                    handler.process(mServer, guageMap, counterMap);
                }
                if (tryMBeanCount != 3) tryMBeanCount = 3;
            } catch (Throwable e) {
                tryMBeanCount--;
                dumpException(e);
            }
//            JmxAgentLogger.info("MBeanServer.usedTime = " + watcher.passedAndReset());
        }

        return result;
    }

    private static void dumpException(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        JmxAgentLogger.info(sw.toString());
    }

}
