package org.sunrise.appmetrics.apistat;

import org.sunrise.appmetrics.Metric;
import org.sunrise.appmetrics.MetricsMonitor;
import org.sunrise.appmetrics.MetricsProxy;

public class ApiCallStatMetricsMonitor extends MetricsMonitor {
    private final static String METRICS_TYPE = "apistat";

    private final ApiCallStat stat = new ApiCallStat();
    private ApiCallStatMetricsMonitor(String id) {
        super(METRICS_TYPE, id);
    }

    @Override
    public boolean isActive() {
        return stat.isActive();
    }
    public void callBegin() {
        stat.callBegin();
    }

    public void callEndWithBadMs(long ms) {
        stat.callEndWithBadMs(ms);
    }

    public void callEndWithGoodMs(long ms) {
        stat.callEndWithGoodMs(ms);
    }

    @Override
    public Metric.Guage[] getGuageMetrics() {
        return guages;
    }
    private final Metric.Guage[] guages = new Metric.Guage[] {
            new Metric.Guage() {
                @Override
                public String getMetricName() {
                    return "running";
                }
                @Override
                public Number getMetric() {
                    return stat.runningNum.get();
                }
            },
            new Metric.LongCounterToGuage() {
                @Override
                public String getMetricName() {
                    return "good_count";
                }
                @Override
                public Long getCounter() {
                    return stat.good.countSum;
                }
            },
            new Metric.LongCounterToGuage() {
                @Override
                public String getMetricName() {
                    return "good_ms";
                }
                @Override
                public Long getCounter() {
                    return stat.good.durationMsSum;
                }
            },
            new Metric.LongCounterToGuage() {
                @Override
                public String getMetricName() {
                    return "bad_count";
                }
                @Override
                public Long getCounter() {
                    return stat.bad.countSum;
                }
            },
            new Metric.LongCounterToGuage() {
                @Override
                public String getMetricName() {
                    return "bad_ms";
                }
                @Override
                public Long getCounter() {
                    return stat.bad.durationMsSum;
                }
            }
    };

    public static ApiCallStatMetricsMonitor getMonitor(String id) {
        MetricsMonitor monitor = MetricsProxy.getInstance().getMetricsMonitor(METRICS_TYPE, id);
        if (monitor != null) {
            return (ApiCallStatMetricsMonitor) monitor;
        }

        synchronized (ApiCallStatMetricsMonitor.class) {
            monitor = MetricsProxy.getInstance().getMetricsMonitor(METRICS_TYPE, id);
            if (monitor != null) {
                return (ApiCallStatMetricsMonitor) monitor;
            }

            return new ApiCallStatMetricsMonitor(id);
        }
    }
}
