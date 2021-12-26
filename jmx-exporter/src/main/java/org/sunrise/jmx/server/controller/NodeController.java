package org.sunrise.jmx.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.sunrise.jmx.metric.JmxMaster;
import org.sunrise.jmx.agent.MetricTimer;
import org.sunrise.jmx.metric.VMInfo;
import org.sunrise.jmx.server.svc.ClusterNodes;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

@Controller
public class NodeController {
    private static final Logger logger = LoggerFactory.getLogger(NodeController.class);
    @RequestMapping(value = ClusterNodes.URL_METRICS)
    public void nodeMetrics(@RequestParam("nextMetricTime") long nextMetricTime,
                            HttpServletResponse response) throws IOException {
        long internal = JmxMaster.getInstance().checkIntervalWithLastPull();
        if (internal < 0) {
            logger.debug("NeighborMetricsRequest at {}.", nextMetricTime);
            PrintWriter writer = response.getWriter();
            JmxMaster.getInstance().exposeJVMMetrics(writer);
            writer.flush();
        } else {
            logger.debug("NeighborMetricsRequest is ignored as only {}ms since last pulling", internal);
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    @RequestMapping(value = ClusterNodes.URL_REGISTER)
    public void register(@RequestParam("name") String nodeName,
                              HttpServletResponse response) throws IOException {
        logger.info("register node: " + nodeName);
        List<String> result = ClusterNodes.registerNode(nodeName);
        if (result.size() == 0) return;

        StringBuffer sb = new StringBuffer();
        boolean isFirst = true;
        for(String node : result) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(",");
            }
            sb.append(node);
        }
        response.getWriter().append(sb.toString());
    }

    @RequestMapping(value = ClusterNodes.URL_BROADCAST)
    public void broadcast(@RequestParam("name") String nodeName,
                          HttpServletResponse response) throws IOException {
        ClusterNodes.broadcastNode(nodeName);
        response.getWriter().append("OK");
    }

    @PostMapping(value = "/upload/metrics/{pid}")
    public void uploadMetrics(@PathVariable("pid") String pid, @RequestBody String metrics,
                              HttpServletResponse response) throws IOException {
        String result = JmxMaster.getInstance().saveMetrics(pid, metrics);
        logger.debug("get metrics from {} and response {}", pid, result);
        response.getWriter().append(result);
    }

    @PostMapping(value = "/check/jvm/{pid}")
    public void checkJVM(@PathVariable("pid") String pid, HttpServletResponse response) throws IOException {
        try {
            JmxMaster.getInstance().processVM(pid, null);
            response.getWriter().append("OK").flush();
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            response.getWriter().append(sw.toString()).flush();
        }
    }

    @PostMapping(value = "/slave/jvm/{vmType}")
    public void registerSlaveJvm(@PathVariable("vmType") String vmType, @RequestBody String vms,
                              HttpServletResponse response) throws IOException {
        logger.info("{} jvm: {}", vmType, vms);
        if (vms != null) {
            String[] items = vms.trim().split("\n");
            for(String item : items) {
                int pos = item.indexOf('#');
                String id = item.substring(0, pos);

                VMInfo vmi = JmxMaster.getInstance().findVMInfo(id);
                if (vmi != null) {
                    vmi.active();
                } else {
                    String tmpdir = item.substring(pos + 1);
                    JmxMaster.getInstance().addVM(vmType, id, tmpdir);
                }
            }
        }
        response.getWriter().append("" + MetricTimer.getNextMetricTime()).flush();
    }
}