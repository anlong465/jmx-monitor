package org.sunrise.appmetrics.apistat;

import java.util.concurrent.atomic.AtomicInteger;

public class ApiCallStat {
    protected final Counter good = new Counter();
    protected final Counter bad = new Counter();

    protected int ttl = 10;
    protected final AtomicInteger runningNum = new AtomicInteger(0);

    public void callBegin() {
        runningNum.incrementAndGet();
    }

    public void callEndWithBadMs(long ms) {
        bad.recordWithMs(ms);
        callEnd();
    }

    public void callEndWithGoodMs(long ms) {
        good.recordWithMs(ms);
        callEnd();
    }

    private void callEnd() {
        runningNum.decrementAndGet();
        ttl = 10;
    }

    protected static class Counter {
        protected long countSum = 0;
        protected long durationMsSum = 0;

        public void recordWithMs(long durationMs) {
            synchronized (this) {
                this.countSum++;
                this.durationMsSum += durationMs;
            }
        }
    }

    public boolean isActive() {
        return ttl-- > 0;
    }

}
