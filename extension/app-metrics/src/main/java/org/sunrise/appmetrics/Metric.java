package org.sunrise.appmetrics;

public interface Metric {
    public String getMetricName();
    public Number getMetric();

    public static interface Guage extends Metric {
    }
    public static interface Counter extends Metric {
    }

    public abstract class CounterToGuage implements Guage {
        private Number latest = null;

        public Number getMetric() {
            if (latest == null) {
                latest = getCounter();
                return latest;
            }

            Number previous = latest;
            latest = getCounter();
            return  (Double) latest - (Double) previous;
        }

        public abstract Number getCounter();
    }

}
