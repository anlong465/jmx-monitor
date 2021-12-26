package org.sunrise.k8s;

import java.io.IOException;

public class K8s1v5ContainerMgr extends K8sContainerMgr {
    public String getContainerID(String pid) throws IOException {
        String containerId = super.getContainerID(pid);
        if (containerId.length() == 0) return containerId;
        if (containerId.startsWith("docker-") && containerId.endsWith(".scope")) {
            int len = containerId.length();
            return containerId.substring(7, len - 6);
        }
        throw new RuntimeException("Failed to figure out Kubernetes 1.5 containerID from ["
                                    + containerId + "] for pid [" + pid + "]");
    }
}
