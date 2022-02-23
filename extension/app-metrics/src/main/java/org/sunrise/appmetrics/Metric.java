package org.sunrise.appmetrics;

public interface Metric {
    public String getMetricName();
    public Number getMetric();

    public static interface Guage extends Metric {
    }
    public static interface Counter extends Metric {
    }

    public abstract class CounterToGuage<T extends Number> implements Guage {
        private T latest = null;

        public T getMetric() {
            if (latest == null) {
                latest = getCounter();
                return latest;
            }

            T previous = latest;
            latest = getCounter();
            return  minus(latest, previous);
        }

        public abstract T minus(T first, T second);

        public abstract T getCounter();
    }

    public abstract class LongCounterToGuage extends CounterToGuage<Long> {
        public Long minus(Long first, Long second) {
            return first - second;
        }
    }

    public abstract class IntegerCounterToGuage extends CounterToGuage<Integer> {
        public Integer minus(Integer first, Integer second) {
            return first - second;
        }
    }

    public abstract class DoubleCounterToGuage extends CounterToGuage<Double> {
        public Double minus(Double first, Double second) {
            return first - second;
        }
    }
}
