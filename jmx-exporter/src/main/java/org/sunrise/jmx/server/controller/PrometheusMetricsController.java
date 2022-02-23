package org.sunrise.jmx.server.controller;

import io.prometheus.client.exporter.common.TextFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.sunrise.jmx.metric.JmxMaster;
import org.sunrise.jmx.server.svc.ClusterNodes;
import org.sunrise.jmx.server.svc.NodePrometheusCollector;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Future;

@Controller
//@Lazy(value=true)
public class PrometheusMetricsController {
    private static final Logger logger = LoggerFactory.getLogger(PrometheusMetricsController.class);

    @RequestMapping("/ClusterMetrics")
    public void clusterMetrics(@Nullable @RequestParam("cluster") String clusterName,
                               HttpServletResponse response) throws IOException {
        long internal = JmxMaster.getInstance().checkIntervalWithLastPull();
        if (internal < 0) {
            logger.info("ClusterMetricsRequest");
            NodePrometheusCollector collector = new NodePrometheusCollector(clusterName);
            List<Future<?>> neighbors = ClusterNodes.collectNeighborMetrics(collector);

            JmxMaster.getInstance().exposeJVMMetrics(collector);

            while (true) {
                for(int i = neighbors.size() - 1; i>= 0; i--) {
                    Future<?> f = neighbors.get(i);
                    if (f.isDone()) {
                        neighbors.remove(i);
                    }
                }
                if (neighbors.size() == 0) break;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            metrics(response, collector);
        } else {
            logger.info("ClusterMetricsRequest is ignored as only {}ms since last pulling", internal);
            metrics(response);
        }
    }

    @RequestMapping("/NodeMetrics")
    public void nodeMetrics(@Nullable @RequestParam("cluster") String clusterName,
                            HttpServletResponse response) throws IOException {
        long internal = JmxMaster.getInstance().checkIntervalWithLastPull();
        if (internal < 0) {
            logger.debug("NodeMetricsRequest");
            NodePrometheusCollector collector = new NodePrometheusCollector(clusterName);
            JmxMaster.getInstance().exposeJVMMetrics(collector);
            metrics(response, collector);
        } else {
            logger.info("NodeMetricsRequest is ignored as only {}ms since last pulling", internal);
            metrics(response);
        }

    }

    private void metrics(HttpServletResponse response, NodePrometheusCollector collector) throws IOException {
        metrics(response);

        PrintWriter writer = response.getWriter();
        TextFormat.write004(writer, collector.collectAsEnumeration());
        writer.flush();
    }

    private void metrics(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(TextFormat.CONTENT_TYPE_004);
    }
}