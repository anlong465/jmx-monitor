package org.sunrise.jmx.agent.jdk7;

import org.sunrise.jmx.agent.CommonUtil;
import org.sunrise.jmx.metric.mbean.MBeanMetricCollector;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

public class JDK7MetricCollector extends MBeanMetricCollector {
    public JDK7MetricCollector() throws MalformedObjectNameException {
        super(null);
    }

    @Override
    protected void process(MBeanServer mServer) {
        List<BufferPoolMXBean> pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        for(BufferPoolMXBean buffer : pools) {
            String name =  "pool " + buffer.getName() + " jvm_buffer_";
            addCounterMetric(name + "count", buffer.getCount());
            addCounterMetric(name + "used", CommonUtil.bytes2MB((buffer.getMemoryUsed())));
            addCounterMetric(name + "max", CommonUtil.bytes2MB(buffer.getTotalCapacity()));
        }
    }


    @Override
    public void process(MBeanServer mServer, ObjectName on) {
    }
}
