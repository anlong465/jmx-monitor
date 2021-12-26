package org.sunrise.k8s;

import org.sunrise.jmx.metric.FileUtils;
import org.sunrise.jmx.metric.JmxConstants;

import java.io.IOException;
import java.util.List;

public class K8sPodMgr implements PodMgr {
    public String fetchPodOverlayRoot(String pid) throws IOException {
        String fileName = JmxConstants.NODE_PROC + pid + "/mountinfo";
        List<String> lines = FileUtils.readFileLines(fileName);
        for (String line : lines) {
            int pos = line.indexOf("upperdir=");
            if (pos > 0) {
                line = line.substring(pos + 9);
                pos = line.indexOf(',');
                line = line.substring(0, pos - 4);
                return line;
            }
        }
        return null;
    }

}
