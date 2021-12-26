package org.sunrise.jmx.server.svc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class NodePrometheusCollector {
    private static final Logger logger = LoggerFactory.getLogger(NodePrometheusCollector.class);

    private final static List<String> labelsNameWithoutCluster =
            Arrays.asList("_namespace", "_pod", "_container", "_node");
    private final static List<String> labelsNameWithCluster =
            Arrays.asList("_namespace", "_pod", "_container", "_node", "_cluster");

    private List<String> labelsName = null;
    private String clusterName = null;
    private Map<String, GaugeMetricFamily> guageMap = new HashMap<>();
    private Map<String, CounterMetricFamily> counterMap = new HashMap<>();

    public NodePrometheusCollector(String clusterName) {
        if (clusterName == null || clusterName.trim().length() == 0) {
            labelsName = labelsNameWithoutCluster;
        } else {
            labelsName = labelsNameWithCluster;
            this.clusterName = clusterName.trim();
        }
    }

    public String getClusterName() {
        return clusterName;
    }

    protected List<String> refineLabelValues(String values, String nodeName) {
        List<String> result = new ArrayList<>();
        for(String item : values.split(",")) {
            result.add(item);
        }
        result.add(nodeName);
        if (clusterName != null) {
            result.add(clusterName);
        }
        return result;
    }

    public Enumeration<MetricFamilySamples> collectAsEnumeration() {
        List<MetricFamilySamples> metricSamples = collect();
        final Iterator<MetricFamilySamples> iterator = metricSamples.iterator();
        return new Enumeration<MetricFamilySamples>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public MetricFamilySamples nextElement() {
                return iterator.next();
            }
        };
    }

    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> metricSamples = new ArrayList<>();
        for(String key : guageMap.keySet()) {
            metricSamples.add(guageMap.get(key));
        }
        for(String key : counterMap.keySet()) {
            metricSamples.add(counterMap.get(key));
        }
        return metricSamples;
    }

    public void addNodeMetrics(String nodeName, String[] metrics) {
//        String shortNodeName;
//        int pos = nodeName.indexOf('.');
//        if (pos > 0) {
//            shortNodeName = nodeName.substring(0, pos);
//        } else {
//            shortNodeName = nodeName;
//        }

        for(String item : metrics) {
            addMetric(nodeName, item);
        }
    }

    public void addMetric(String nodeName, String metric) {
        logger.debug("addMetric: {} --> {}", nodeName, metric);
        int pos = metric.indexOf('#');
        if (pos < 0) {
            logger.error("addMetric: Empty metric for {}", nodeName);
            return;
        }
        List<String> labelsValue = refineLabelValues(metric.substring(0, pos), nodeName);
        JSONObject json = JSON.parseObject(metric.substring(pos + 1));

        if (json == null) {
            logger.debug("Failed to parse metrics to JSON: {}", metric);
            return;
        }
        guageCollector(labelsValue, json.getJSONObject("guage"));
        counterCollector(labelsValue, json.getJSONObject("counter"));

    }

    private synchronized void counterCollector(List<String> labelsValue, JSONObject metrics) {
        for(String keyName : metrics.keySet()) {
            MetricInfo mi = new MetricInfo(keyName, labelsName, labelsValue);

            CounterMetricFamily metricFamily = counterMap.get(mi.name);
            if (metricFamily == null) {
                metricFamily = new CounterMetricFamily(mi.name, mi.name, mi.labelsName);
                counterMap.put(mi.name, metricFamily);
            }
            metricFamily.addMetric(mi.labelsValue, metrics.getDouble(keyName));
        }
    }

    private synchronized void guageCollector(List<String> labelsValue, JSONObject metrics) {
        for(String keyName : metrics.keySet()) {
            MetricInfo mi = new MetricInfo(keyName, labelsName, labelsValue);

            GaugeMetricFamily metricFamily = guageMap.get(mi.name);
            if (metricFamily == null) {
                metricFamily = new GaugeMetricFamily(mi.name, mi.name, mi.labelsName);
                guageMap.put(mi.name, metricFamily);
            }
            metricFamily.addMetric(mi.labelsValue, metrics.getDouble(keyName));
        }
    }

    private static class MetricInfo {
        protected String name;
        protected List<String> labelsValue;
        protected List<String> labelsName;

        public MetricInfo(String keyName, List<String> labelsName, List<String> labelsValue) {
            int pos = keyName.lastIndexOf(' ');
            if (pos > 0) {
                name = keyName.substring(pos + 1).trim();
                this.labelsName = new ArrayList<>();
                this.labelsValue = new ArrayList<>();
                this.labelsName.addAll(labelsName);
                this.labelsValue.addAll(labelsValue);

                String labelNameValue = keyName.substring(0, pos).trim();
                pos = labelNameValue.indexOf(' ');
                this.labelsName.add(labelNameValue.substring(0, pos).trim());
                this.labelsValue.add(labelNameValue.substring(pos + 1).trim());
            } else {
                name = keyName;
                this.labelsValue = labelsValue;
                this.labelsName = labelsName;
            }
        }
    }


}