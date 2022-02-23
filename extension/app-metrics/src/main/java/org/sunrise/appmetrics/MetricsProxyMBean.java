package org.sunrise.appmetrics;

import java.util.Map;

public interface MetricsProxyMBean {
    public Map<String, Number>[] getMetrics();
}
