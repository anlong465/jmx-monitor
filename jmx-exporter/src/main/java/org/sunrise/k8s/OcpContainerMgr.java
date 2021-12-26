package org.sunrise.k8s;

import com.alibaba.fastjson.JSONObject;
import org.sunrise.jmx.metric.JmxConstants;

import java.io.IOException;

public class OcpContainerMgr extends K8sContainerMgr {
    public JSONObject getContainerInfo(JSONObject config) {
        return config.getJSONObject("annotations");
    }

    public String getContainerID(String pid) throws IOException {
        String containerId = super.getContainerID(pid);
        if (containerId.length() == 0) return containerId;
        if (containerId.startsWith("crio-") && containerId.endsWith(".scope")) {
            int len = containerId.length();
            return containerId.substring(5, len - 6);
        }
        throw new RuntimeException("Failed to figure out OCP containerID from ["
                                    + containerId + "] for pid [" + pid + "]");
    }

    public String getContainerConfigFilePath(String containerId) {
        return JmxConstants.NODE_VAR + "lib/containers/storage/overlay-containers/"
                + containerId + "/userdata/config.json";
    }

}
