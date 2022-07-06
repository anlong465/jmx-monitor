package org.sunrise.appmetrics.apistat;

import org.sunrise.appmetrics.Metric;
import org.sunrise.appmetrics.MetricsMonitor;
import org.sunrise.appmetrics.MetricsProxy;

import java.lang.reflect.Method;

public class ApiCallStatMetricsMonitor extends MetricsMonitor {
    private final static String METRICS_TYPE = "apistat";

    private final ApiCallStat stat = new ApiCallStat();
    private ApiResultChecker resultChecker = null;
    private boolean ready = false;
    private ApiCallStatMetricsMonitor(String id) {
        super(METRICS_TYPE, id);
    }

    @Override
    public boolean isActive() {
        return stat.isActive();
    }

    public void callBegin(Method method, String checkSuccessRule) {
        stat.callBegin();

        if (ready) return;
        synchronized (this) {
            if (ready) return;
            try {
                ready = true;
                if (checkSuccessRule != null && checkSuccessRule.trim().length() > 0) {
                    this.resultChecker = new ApiResultChecker(method, checkSuccessRule.trim());
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public void callEndWithMs(boolean isSuccess, long ms) {
        if (isSuccess) {
            stat.callEndWithGoodMs(ms);
        } else {
            stat.callEndWithBadMs(ms);
        }
    }

    public boolean isCallSuccess(Object ret, Throwable th) {
        try {
            if (th == null && (resultChecker == null || resultChecker.isResultSuccess(ret))) {
                return true;
            } else {
                return false;
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            return true;
        }
    }

    public boolean callEndWithMs(Object ret, Throwable th, long ms) {
        try {
            if (th == null && (resultChecker == null || resultChecker.isResultSuccess(ret))) {
                stat.callEndWithGoodMs(ms);
                return true;
            } else {
                stat.callEndWithBadMs(ms);
                return false;
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            return true;
        }
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
