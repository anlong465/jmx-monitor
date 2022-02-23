package org.sunrise.jmx.metric.mbean;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class DruidDatasourceMetricCollector extends DatasourceMetricCollector {
    public DruidDatasourceMetricCollector() throws MalformedObjectNameException {
        super(new ObjectName("com.alibaba.druid.pool:type=DruidDataSource,*"));
    }

    @Override
    public DatasourceMetric processDatasource(MBeanServer mServer, ObjectName on) {
        try {
            DatasourceMetric dm = new DatasourceMetric();

            dm.jndiName = on.getKeyProperty("name");
            dm.maxSize = (Integer) mServer.getAttribute(on, "MaxActive");
            dm.waiting = (Integer) mServer.getAttribute(on, "WaitThreadCount");
            dm.idle = (Integer) mServer.getAttribute(on, "PoolingCount");
            dm.used = (Integer) mServer.getAttribute(on, "ActiveCount");

            return dm;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
