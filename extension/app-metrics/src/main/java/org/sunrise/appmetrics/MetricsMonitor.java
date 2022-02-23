package org.sunrise.appmetrics;

import java.util.HashMap;
import java.util.Map;

public abstract class MetricsMonitor {
    protected final String type;
    protected final String id;
    private final String prefix;
    private Metric.Guage[] guages = null;
    private Metric.Counter[] counters = null;

    public MetricsMonitor(String type, String id) {
        this.type = type;
        this.id = id;

        prefix = "id " + id + " proxy_" + type + "_";

        MetricsProxy.getInstance().register(this);
    }

    public final void stop() {
        MetricsProxy.getInstance().unregister(this);
    }

    public abstract boolean isActive();
    public abstract Metric.Guage[] getGuageMetrics();

    public Metric.Counter[] getCounterMetrics() {
        return new Metric.Counter[0];
    }

    private void checkInit() {
        if (guages != null || counters != null) return;

        guages = getGuageMetrics();
        if (guages == null) {
            guages = new Metric.Guage[0];
        }

        counters = getCounterMetrics();
        if (counters == null) {
            counters = new Metric.Counter[0];
        }

        Map<String, String> metricNameClass = new HashMap<String, String>();
        for(Metric.Guage guage : guages) {
            checkMetricName(guage.getMetricName(), "guage", metricNameClass);
        }
        for(Metric.Counter counter : counters) {
            checkMetricName(counter.getMetricName(), "counter", metricNameClass);
        }
    }

    private void checkMetricName(String metricName, String metricClass, Map<String, String> metricNameClass) {
        if (metricNameClass.containsKey(metricName)) {
            throw new RuntimeException("One metric[class=" + metricClass + ", name="
                    + metricName + "] already existed!");
        }
        metricNameClass.put(metricName, metricClass);
    }

    public void exportMetrics(Map<String, Number> guageResults, Map<String, Number> countersResult) {
        checkInit();
        for(Metric guage : guages) {
            guageResults.put(prefix + guage.getMetricName(), guage.getMetric());
        }
        for(Metric counter : counters) {
            countersResult.put(prefix + counter.getMetricName(), counter.getMetric());
        }
    }

}
