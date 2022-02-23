package org.sunrise.jmx.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.sunrise.jmx.metric.JmxMaster;
import org.sunrise.jmx.server.svc.ClusterNodes;

import java.io.IOException;

@SpringBootApplication
public class AppMain {
    private static final Logger logger = LoggerFactory.getLogger(AppMain.class);

    public static void main(String[] args) throws IOException {
        SpringApplication.run(AppMain.class, args);
        new Thread(JmxMaster.getInstance()).start();
        initSlaveMonitor();
        ClusterNodes.init();
    }

    private static void initSlaveMonitor() {
        String slaves = System.getenv("JVM_SLAVES");
        if (slaves == null) {
            return;
        }
        slaves = slaves.trim();
        if (slaves.length() == 0) {
            return;
        }

        Runtime rt = Runtime.getRuntime();

        String[] cmds = {"", "-cp", System.getenv("JVM_SLAVE_CP"),
                        "org.sunrise.jmx.mgr.JmxSlave", ""};

        String[] items = slaves.split(",");
        for(String item : items) {
            try {
                int pos = item.indexOf('=');
                String vmType = item.substring(0, pos).trim();
                String javaCmd = item.substring(pos + 1).trim();
                cmds[0] = javaCmd;
                cmds[4] = vmType;

                rt.exec(cmds);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
