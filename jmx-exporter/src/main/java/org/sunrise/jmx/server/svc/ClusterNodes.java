package org.sunrise.jmx.server.svc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.sunrise.jmx.agent.MetricTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ClusterNodes {
    private static final Logger logger = LoggerFactory.getLogger(ClusterNodes.class);
    private final static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private static String HOST_PORT;
    public final static String URL_REGISTER = "/node/register";
    public final static String URL_BROADCAST = "/node/broadcast";
    public final static String URL_METRICS = "/node/metrics";
    private final String selfNodeName;
    private final List<String> otherNodesList = new ArrayList<>();

    private ClusterNodes(String selfNodeName, List<String> otherNodesList) {
        this.selfNodeName = (selfNodeName == null) ? "" : selfNodeName.trim().toLowerCase();
        this.otherNodesList.addAll(otherNodesList);
    }

    private static ClusterNodes instance = null;
    public static void init() {
        String selfNodeName = System.getenv("NODE_NAME");
        ClusterNodes.HOST_PORT = System.getenv("HOST_PORT");
        String allNodes = System.getenv("CLUSTER_NODES");

        logger.debug("Init ClusterNodes: {} --> {}", selfNodeName, allNodes);
        List<String> nodesList = new ArrayList<>();
        if (allNodes != null && allNodes.trim().length() > 0) {
            String[] nodes = allNodes.split(",");
            for(String node : nodes) {
                node = node.trim().toLowerCase();
                if (!selfNodeName.equals(node)) {
                    nodesList.add(node);
                }
            }
        }

        instance = new ClusterNodes(selfNodeName, nodesList);

        cachedThreadPool.submit(new RegisterRunnable(nodesList, selfNodeName));
    }

    public static void addNeighborNodes(String nodes) {
        logger.info("addNeighborNodes: " + nodes);
        if (nodes == null) return;
        String[] items = nodes.split(",");
        if (items.length == 0) return;
        synchronized (instance) {
            for(String node : items) {
                node = node.trim().toLowerCase();
                if (!instance.otherNodesList.contains(node) &&
                        !instance.selfNodeName.equals(node)) {
                    instance.otherNodesList.add(node);
                }
            }
        }
        logger.debug("after addNeighborNodes: {} --> {}",
                instance.selfNodeName,
                instance.otherNodesList);

    }

    public static List<String> registerNode(String nodeName) {
        logger.info("registerNode: " + nodeName);
        List<String> result = new ArrayList<>();
        result.addAll(instance.otherNodesList);
        boolean knownNode = false;
        synchronized (instance) {
            if (instance.otherNodesList.contains(nodeName)) {
                knownNode = true;
            } else {
                instance.otherNodesList.add(nodeName);
            }
        }
        if (!knownNode && result.size() > 0) {
            Runnable r = new BroadcastRunnable(result, nodeName);
            cachedThreadPool.submit(r);
        }
        logger.debug("after registerNode: {} --> {}",
                instance.selfNodeName,
                instance.otherNodesList);
        return result;
    }

    public static void broadcastNode(String nodeName) {
        logger.info("broadcastNode: " + nodeName);
        synchronized (instance) {
            if (!instance.selfNodeName.equals(nodeName) && !instance.otherNodesList.contains(nodeName)) {
                instance.otherNodesList.add(nodeName);
            }
        }
        logger.debug("after broadcastNode: {} --> {}",
                instance.selfNodeName,
                instance.otherNodesList);
    }

    public static List<Future<?>> collectNeighborMetrics(NodePrometheusCollector collector) {
        List<Future<?>> results = new ArrayList<>();
        for (String node : instance.otherNodesList) {
            MetricRunnable r = new MetricRunnable(node, collector);
            results.add(cachedThreadPool.submit(r));
        }
        return results;
    }

    private abstract static class NodeCommunicationRunnable implements Runnable {
        final List<String> nodesList;
        final String nodeName;
        final String action;
        NodeCommunicationRunnable(List<String> nodesList, String nodeName, String action) {
            this.nodesList = nodesList;
            this.nodeName = nodeName;
            this.action = action;
        }

        public abstract void postAction(String actionResponse);

        @Override
        public void run() {
            while (true) {
                for(int i = nodesList.size() - 1; i >= 0; i--) {
                    String url = "http://" + nodesList.get(i) + ":" + HOST_PORT + action + "?name=" + nodeName;
                    try {
                        RestTemplate restTemplate = new RestTemplate(new OkHttp3ClientHttpRequestFactory());
                        String result = restTemplate.getForObject(url, String.class);
                        logger.info("{} via {} with result: {}", action, url, result);
                        postAction(result);

                        nodesList.remove(i);
                    } catch (Throwable th) {
                        logger.debug("Failed to {} via {} with reason: {}", action, url, th.getMessage());
                    }

                }
                if (nodesList.size() == 0) break;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class RegisterRunnable extends NodeCommunicationRunnable {
        RegisterRunnable(List<String> nodesList, String selfNodeName) {
            super(nodesList, selfNodeName, URL_REGISTER);
        }

        @Override
        public void postAction(String actionResponse) {
            if (actionResponse != null) {
                addNeighborNodes(actionResponse);
            }
        }
    }

    private static class BroadcastRunnable extends NodeCommunicationRunnable {
        BroadcastRunnable(List<String> nodesList, String broadcastNodeName) {
            super(nodesList, broadcastNodeName, URL_BROADCAST);
        }

        @Override
        public void postAction(String actionResponse) {
        }
    }

    private static class MetricRunnable implements Runnable {
        String node;
        NodePrometheusCollector collector;
        public MetricRunnable(String node, NodePrometheusCollector collector) {
            this.node = node;
            this.collector = collector;
        }

        @Override
        public void run() {
            RestTemplate restTemplate = new RestTemplate(new OkHttp3ClientHttpRequestFactory());
            String url = "http://" + node + ":" + HOST_PORT + URL_METRICS
                    + "?nextMetricTime=" + MetricTimer.getNextMetricTime();
//            if (collector.getClusterName() != null) {
//                url += "&cluster=" + collector.getClusterName();
//            }
            try {
                String result = restTemplate.getForObject(url, String.class);
                if (result == null) return;
                result = result.trim();
                collector.addNodeMetrics(node, result.split("\n"));
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

}
