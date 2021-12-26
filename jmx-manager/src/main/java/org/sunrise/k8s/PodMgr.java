package org.sunrise.k8s;

import java.io.IOException;

public interface PodMgr {
    public abstract String fetchPodOverlayRoot(String pid) throws IOException;
}
