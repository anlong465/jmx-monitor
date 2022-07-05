package org.sunrise.jmx.metric;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;

public class JVMThreadCollector {
    public static void getGCMetrics(Map<String, Number> guageMap, Map<String, Number> counterMap) {
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();
        guageMap.put("jvm_thread_active_total", thread.getThreadCount());
        guageMap.put("jvm_thread_peak_total", thread.getPeakThreadCount());
        guageMap.put("jvm_thread_daemon_total", thread.getDaemonThreadCount());

//        NanoTimeWatcher watcher = new NanoTimeWatcher();
        long[] deadlocked = thread.findDeadlockedThreads();
        guageMap.put("jvm_thread_deadlocked", (deadlocked == null) ? 0 : deadlocked.length);
//        JmxAgentLogger.info("findDeadlockedThreads used time = " + watcher.passedAndReset());

        long[] ids = thread.getAllThreadIds();
        ThreadInfo[] infos = thread.getThreadInfo(ids);
        ThreadStat stat = new ThreadStat();
        for(ThreadInfo info : infos) {
            if (info == null) {
                stat.terminated++;
                continue;
            }
            Thread.State state = info.getThreadState();
            if (state.equals(Thread.State.NEW)) {
                stat.created++;
            } else if (state.equals(Thread.State.RUNNABLE)) {
                stat.running++;
            } else if (state.equals(Thread.State.BLOCKED)) {
                stat.blocked++;
            } else if (state.equals(Thread.State.WAITING)) {
                stat.waiting++;
            } else if (state.equals(Thread.State.TIMED_WAITING)) {
                stat.waiting++;
            } else if (state.equals(Thread.State.TERMINATED)) {
                stat.terminated++;
            }
        }
//        JmxAgentLogger.info("calculateThreadType used time = " + watcher.passed());

        guageMap.put("jvm_thread_new", stat.created);
        guageMap.put("jvm_thread_running", stat.running);
        guageMap.put("jvm_thread_blocked", stat.blocked);
        guageMap.put("jvm_thread_waiting", stat.waiting);
        guageMap.put("jvm_thread_terminated", stat.terminated);

    }

    private static class ThreadStat {
        int created = 0;
        int running = 0;
        int blocked = 0;
        int waiting = 0;
        int terminated = 0;
    }
}
