package org.sunrise.k8s;

import com.alibaba.fastjson.JSONObject;
import org.sunrise.jmx.metric.VMInfo;

import java.io.IOException;

public interface ContainerMgr {
    public boolean fetchContainerInfo(VMInfo vmi) throws IOException;

    public JSONObject getContainerInfo(JSONObject config);
    public String getContainerID(String pid) throws IOException;

    public String getContainerConfigFilePath(String containerId);

}
