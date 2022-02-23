package org.sunrise.jmx.metric.mbean;

import org.sunrise.jmx.agent.CommonUtil;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Map;

public class ProxyMetricCollector extends MBeanMetricCollector {
    public ProxyMetricCollector(ObjectName id) throws MalformedObjectNameException {
        super(new ObjectName("org.sunrise.metrics:type=Proxy"));
    }

    @Override
    public void process(MBeanServer mServer, ObjectName on) {
        try {
            Map<String, Number>[] res = (Map<String, Number>[]) mServer.getAttribute(on,"Metrics");
            for(String key : res[0].keySet()) {
                addGuageMetric(key, res[0].get(key));
            }
            for(String key : res[1].keySet()) {
                addCounterMetric(key, res[1].get(key));
            }
        } catch (InstanceNotFoundException e) {

        } catch (Exception e) {
            CommonUtil.logException(e);
        }
    }
}
