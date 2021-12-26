package org.sunrise.jmx;

import org.sunrise.jmx.agent.JmxAgentRunnable;

public class JmxAgent {

    /**
     * @param agentArgs "ownerId sleep-time [jmxServerUrl auth] selfId"
     **/
    public static void agentmain(final String agentArgs) {
        System.out.println("agentmain: " + agentArgs);
        String[] ids = agentArgs.split(" ");

        Runnable r;
        if (ids.length == 5) {
            r = new JmxAgentRunnable(Long.parseLong(ids[0]), ids[4], Long.parseLong(ids[1]), ids[2], ids[3]);
        } else if (ids.length == 3) {
            r = new JmxAgentRunnable(Long.parseLong(ids[0]), ids[2], Long.parseLong(ids[1]), null, null);
        } else {
            throw new RuntimeException("Unrecognized agentmain arguments: " + agentArgs);
        }

        Thread th = new Thread(r);
        th.setDaemon(true);
        th.start();
    }

}

