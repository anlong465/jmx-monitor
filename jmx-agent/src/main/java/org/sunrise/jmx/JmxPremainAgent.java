package org.sunrise.jmx;

import org.sunrise.jmx.agent.JmxAgentRunnable;

public class JmxPremainAgent {

    public static void premain(String args) {
        System.out.println(" premain starting ... " + args);
        Thread th = JmxAgentRunnable.getDefaultAgent(args);
        if (th != null) {
            th.start();
        }
    }

}
