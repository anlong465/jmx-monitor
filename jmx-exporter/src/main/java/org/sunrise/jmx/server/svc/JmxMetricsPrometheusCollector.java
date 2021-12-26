package org.sunrise.jmx.server.svc;

import java.io.IOException;
import java.util.*;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.sunrise.jmx.metric.storage.MetricsFactory;

public class JmxMetricsPrometheusCollector extends Collector {
    List<String> labelsName = Arrays.asList("namespace", "podName", "containerName");

    @Override
    public List<io.prometheus.client.Collector.MetricFamilySamples> collect() {
        List<MetricFamilySamples> results = new ArrayList<>();
        Map<String, GaugeMetricFamily> guageMap = new HashMap<>();

        try {
            List<String> all = MetricsFactory.readMetrics();
            for(String metrics : all) {
                System.out.println("********************************");
                System.out.println(metrics);
                System.out.println("********************************");
                String[] items = metrics.split("\n");
                int pos = items[0].indexOf('=');
                String labelsValue = items[0].substring(pos + 1).trim();
                JSONObject json = JSON.parseObject(items[1]);
                List<String> values = Arrays.asList(labelsValue.split(","));

                guageCollector(guageMap, values, json.getJSONObject("guage"));
            }

            for(String key : guageMap.keySet()) {
                results.add(guageMap.get(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return results;
    }

    private void guageCollector(Map<String, GaugeMetricFamily> guageMap, List<String> labelsValue, JSONObject metrics) {
        for(String metricName : metrics.keySet()) {
            GaugeMetricFamily metricFamily = guageMap.get(metricName);
            if (metricFamily == null) {
                metricFamily = new GaugeMetricFamily(metricName, metricName, labelsName);
                guageMap.put(metricName, metricFamily);
            }
            metricFamily.addMetric(labelsValue, metrics.getDouble(metricName));
        }
    }

}