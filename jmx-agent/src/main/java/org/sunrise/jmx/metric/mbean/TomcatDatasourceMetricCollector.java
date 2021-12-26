package org.sunrise.jmx.metric.mbean;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class TomcatDatasourceMetricCollector extends DatasourceMetricCollector {
    public TomcatDatasourceMetricCollector() throws MalformedObjectNameException {
        super(new ObjectName("Catalina:type=DataSource,*"));
    }

    @Override
    public DatasourceMetric processDatasource(MBeanServer mServer, ObjectName on) {
        try {
            DatasourceMetric dm = new DatasourceMetric();

            dm.jndiName = on.getKeyProperty("name");
            dm.waiting = 0;
            dm.maxSize = (Integer) mServer.getAttribute(on, "maxTotal");
            dm.idle = (Integer) mServer.getAttribute(on, "numIdle");
            dm.used = (Integer) mServer.getAttribute(on, "numActive");

            return dm;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
