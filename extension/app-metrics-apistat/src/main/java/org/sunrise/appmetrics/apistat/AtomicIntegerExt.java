package org.sunrise.appmetrics.apistat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerExt {
    AtomicInteger atomic;
    IntegerPeriodStat stat = new IntegerPeriodStat();

    public AtomicIntegerExt(int initialValue) {
        atomic = new AtomicInteger(initialValue);
    }

    public int incrementAndGet() {
        return stat.stat(atomic.incrementAndGet());
    }

    public int decrementAndGet() {
        return stat.stat(atomic.decrementAndGet());
    }

    public int getAndAdd(int delta) {
        return stat.stat(atomic.getAndAdd(delta));
    }

    public Number get() {
        return stat.getAvg();
    }

    public static class IntegerPeriodStat {
        private int lastValue = 0;
        private long beginMillis = System.currentTimeMillis();
        private long lastMillis = beginMillis;
        private long valueSum = 0;

        public int stat(int value) {
            synchronized (this) {
                long current = System.currentTimeMillis();
                if (lastValue > 0) {
                    valueSum += (current - lastMillis) * lastValue;
                }
                lastMillis = current;
                lastValue = value;
            }

            return value;
        }

        public double getAvg() {
            long total;
            long period;
            synchronized (this) {
                long current = System.currentTimeMillis();
                if (lastValue > 0) {
                    total = valueSum + (current - lastMillis) * lastValue;
                } else {
                    total = valueSum;
                }
                period = current - beginMillis;

                valueSum = 0;
                beginMillis = current;
                lastMillis = current;
            }
            if (total == 0) return 0;
            double avg = ((double) total) / period;

            BigDecimal bd = new BigDecimal(avg);
            return bd.setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
    }
}
