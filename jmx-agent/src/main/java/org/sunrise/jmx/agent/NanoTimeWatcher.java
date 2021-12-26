package org.sunrise.jmx.agent;

public class NanoTimeWatcher {
    public final static long NANOSECOND_PER_MILLISECOND = 1000L * 1000L;
    private long nanoTime = System.nanoTime();

    public void reset() {
        nanoTime = System.nanoTime();
    }

    public long passed() {
        return System.nanoTime() - nanoTime;
    }

    public long passedAndReset() {
        long old = nanoTime;
        nanoTime = System.nanoTime();
        return nanoTime - old;
    }


}
