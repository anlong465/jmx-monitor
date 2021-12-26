package org.sunrise.jmx.metric.mbean;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.Map;
import java.util.Set;

public abstract class MBeanMetricCollector {
    protected final ObjectName id;
    public MBeanMetricCollector(ObjectName id) {
        this.id = id;
    }

    private Map<String, Number> guageMap;
    private Map<String, Number> counterMap;
    public void process(MBeanServer mServer, Map<String, Number> guageMap, Map<String, Number> counterMap) {
        this.counterMap = counterMap;
        this.guageMap = guageMap;
        process(mServer);
    }

    protected void process(MBeanServer mServer) {
        Set<ObjectName> mbeans = mServer.queryNames(id, null);
        process(mServer, mbeans);
    }

    protected void process(MBeanServer mServer, Set<ObjectName> mbeans) {
        for(ObjectName on : mbeans) {
            process(mServer, on);
        }
    }

    protected void addGuageMetric(String key, Number value) {
        guageMap.put(key, value);
    }

    protected void addCounterMapMetric(String key, Number value) {
        counterMap.put(key, value);
    }

    public abstract void process(MBeanServer mServer, ObjectName on);
}
