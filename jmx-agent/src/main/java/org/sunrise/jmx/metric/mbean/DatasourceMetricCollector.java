package org.sunrise.jmx.metric.mbean;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public abstract class DatasourceMetricCollector extends MBeanMetricCollector {
    public DatasourceMetricCollector(ObjectName id) {
        super(id);
    }

    @Override
    public void process(MBeanServer mServer, ObjectName on) {
        DatasourceMetric result = processDatasource(mServer, on);
        if (result != null) {
            String prefix = "jndi " + result.jndiName + " jvm_ds_";

            addGuageMetric(prefix + "max_size", result.maxSize);
            addGuageMetric(prefix + "waiting", result.waiting);
            addGuageMetric(prefix + "idle", result.idle);
            addGuageMetric(prefix + "used", result.used);
        }
    }

    public abstract DatasourceMetric processDatasource(MBeanServer mServer, ObjectName on);

    public static class DatasourceMetric {
        String jndiName;
        int maxSize = 0;
//        int size;
        int waiting = 0;
        int idle = 0;
        int used = 0;
    }
}
