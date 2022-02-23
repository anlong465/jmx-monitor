package org.sunrise.appmetrics.threadpool;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolExecutorMetricsMonitor extends ThreadPoolMetricsMonitor {
    private final ThreadPoolExecutor pool;
    public ThreadPoolExecutorMetricsMonitor(String id, ThreadPoolExecutor pool) {
        super(id);
        this.pool = pool;
    }

    @Override
    public int getMaximumPoolSize() {
        return pool.getMaximumPoolSize();
    }

    @Override
    public int getRunningTasks() {
        return pool.getActiveCount();
    }

    @Override
    public int getWaitingTasks() {
        return pool.getQueue().size();
    }

    @Override
    public boolean isActive() {
        return !pool.isShutdown() && !pool.isTerminated() && !pool.isTerminating();
    }

    public static ThreadPoolExecutorMetricsMonitor monitorSingleThreadExecutor(String id, ExecutorService single)
            throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = single.getClass();
        Field f = clazz.getField("e");
        f.setAccessible(true);
        ThreadPoolExecutor pool = (ThreadPoolExecutor) f.get(single);
        return new ThreadPoolExecutorMetricsMonitor(id, pool);
    }
}
