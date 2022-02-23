package org.sunrise.appmetrics;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricsProxy implements MetricsProxyMBean {
    private final List<MetricsMonitor> monitors = new ArrayList<MetricsMonitor>();
    public MetricsMonitor getMetricsMonitor(String type, String id) {
        for(MetricsMonitor item : monitors) {
            if (item.type.equals(type) && item.id.equals(id)) {
                return item;
            }
        }

        return null;
    }

    public void register(MetricsMonitor monitor) {
        synchronized (monitors) {
            for(MetricsMonitor item : monitors) {
                if (item.type.equals(monitor.type) && item.id.equals(monitor.id)) {
                    throw new RuntimeException("Failed to register MetricsMonitor as [type=" + monitor.type
                            + ", id=" + monitor.id + "] already registered!");
                }
            }
            this.monitors.add(monitor);
        }
    }

    public void unregister(MetricsMonitor monitor) {
        synchronized (monitors) {
            this.monitors.remove(monitor);
        }
    }

    private List<MetricsMonitor> deadList = new ArrayList<MetricsMonitor>();
    public Map<String, Number>[] getMetrics() {
        Map<String, Number>[] results = new Map[] {
                new HashMap<String, Number>(),
                new HashMap<String, Number>()
        };

        for(MetricsMonitor monitor : monitors) {
            if (monitor.isActive()) {
                monitor.exportMetrics(results[0], results[1]);
            } else {
                deadList.add(monitor);
            }
        }
        if (deadList.size() > 0) {
            synchronized (monitors) {
                for(MetricsMonitor monitor : deadList) {
                    monitors.remove(monitor);
                }
            }
            deadList.clear();
        }
        return results;
    }

    private static MetricsProxy instance = null;
    public static MetricsProxy getInstance() {
        if (instance == null) {
            synchronized (MetricsProxy.class) {
                if (instance == null) {
                    instance = new MetricsProxy();
                    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                    try {
                        ObjectName mbeanName = new ObjectName("org.sunrise.metrics:type=Proxy");
                        server.registerMBean(instance, mbeanName);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to register MBean", e);
                    }
                }
            }
        }
        return instance;
    }

}
