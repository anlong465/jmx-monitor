package org.sunrise.appmetrics.threadpool;

import org.sunrise.appmetrics.Metric;
import org.sunrise.appmetrics.MetricsMonitor;

public abstract class ThreadPoolMetricsMonitor extends MetricsMonitor {
    public ThreadPoolMetricsMonitor(String id) {
        super("thread_pool", id);
    }

    public abstract int getMaximumPoolSize();
    public abstract int getRunningTasks();
    public abstract int getWaitingTasks();

    private final Metric.Guage[] guageMetrics = new Metric.Guage[] {
            new Metric.Guage() {
                @Override
                public String getMetricName() {
                    return "maxSize";
                }

                @Override
                public Number getMetric() {
                    return getMaximumPoolSize();
                }
            },
            new Metric.Guage() {
                @Override
                public String getMetricName() {
                    return "active";
                }

                @Override
                public Number getMetric() {
                    return getRunningTasks();
                }
            },
            new Metric.Guage() {
                @Override
                public String getMetricName() {
                    return "waiting";
                }

                @Override
                public Number getMetric() {
                    return getWaitingTasks();
                }
            }
    };

    @Override
    public Metric.Guage[] getGuageMetrics() {
        return guageMetrics;
    }

}
