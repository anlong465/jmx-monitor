package org.sunrise.k8s;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.sunrise.jmx.metric.FileUtils;
import org.sunrise.jmx.metric.JmxConstants;
import org.sunrise.jmx.metric.VMInfo;

import java.io.IOException;
import java.util.List;

public class K8sContainerMgr implements ContainerMgr {
    public boolean fetchContainerInfo(VMInfo vmi) throws IOException {
        String containerId = getContainerID(vmi.id);
        if (containerId.length() == 0) {  // not container based process
            return false;
        }
        String configFilePath = getContainerConfigFilePath(containerId);
        String config = FileUtils.readFileContent(configFilePath);
        JSONObject json = JSON.parseObject(config);
        JSONObject labels = getContainerInfo(json);

        vmi.containerName = labels.getString("io.kubernetes.container.name");
        vmi.namespace = labels.getString("io.kubernetes.pod.namespace");
        vmi.podName = labels.getString("io.kubernetes.pod.name");
        return true;
    }

    public JSONObject getContainerInfo(JSONObject config) {
        return config.getJSONObject("Config").getJSONObject("Labels");
    }

    public String getContainerID(String pid) throws IOException {
        String fileName = JmxConstants.NODE_PROC + pid + "/cgroup";
        List<String> lines = FileUtils.readFileLines(fileName);
        for (String line : lines) {
            if (line.contains(":cpuset:/")) {
                int pos = line.lastIndexOf('/');
                String containerId = line.substring(pos + 1);
                return containerId;
            }
        }
        throw new RuntimeException("Something wrong to figure out containerID via pid");
    }

    public String getContainerConfigFilePath(String containerId) {
        return JmxConstants.NODE_VAR + "lib/docker/containers/" + containerId + "/config.v2.json";
    }

}
