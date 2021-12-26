package org.sunrise.jmx.metric.mbean;

import javax.management.*;

public class WebSphereDatasourceMetricCollector extends DatasourceMetricCollector {
    public WebSphereDatasourceMetricCollector() throws MalformedObjectNameException {
        super(new ObjectName("*:type=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,*"));
    }

    @Override
    public DatasourceMetric processDatasource(MBeanServer mServer, ObjectName on) {
        try {
            Object res = mServer.invoke(on, "showPoolContents", new Object[0], new String[0]);
            if (res == null) return null;

            String[] lines = res.toString().split("\n");
            DatasourceMetric dm = new DatasourceMetric();
            for(String line : lines) {
                int pos = line.indexOf('=');
                if (pos <= 0) {
                    continue;
                }
                String key = line.substring(0, pos).trim();
                String value = line.substring(pos + 1).trim();
                if ("jndiName".equals(key)) {
                    dm.jndiName = value;
                } else if ("maxPoolSize".equals(key)) {
                    dm.maxSize = Integer.parseInt(value);
                } else if ("waiting".equals(key)) {
                    dm.waiting = Integer.parseInt(value);
                } else if ("available".equals(key)) {
                    dm.idle = Integer.parseInt(value);
                } else if ("shared".equals(key) || "unshared".equals(key)) {
                    dm.used += Integer.parseInt(value);
                }
            }
            return dm;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
