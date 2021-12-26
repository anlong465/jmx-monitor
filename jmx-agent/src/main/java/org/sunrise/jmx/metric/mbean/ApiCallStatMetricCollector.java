package org.sunrise.jmx.metric.mbean;

import javax.management.*;
import java.util.*;

public class ApiCallStatMetricCollector extends MBeanMetricCollector {
    private final List<ApiCallStatPair> callStats = new ArrayList<ApiCallStatPair>();

    public ApiCallStatMetricCollector() throws MalformedObjectNameException {
        super(new ObjectName("org.sunrise.api:type=ApiStat"));
    }

    private ApiCallStatPair getApiCallStatPair(String apiId) {
        for(ApiCallStatPair pair : callStats) {
            if (pair.apiId.equals(apiId)) return pair;
        }
        ApiCallStatPair pair = new ApiCallStatPair(apiId);
        callStats.add(pair);
        return pair;
    }

    @Override
    public void process(MBeanServer mServer, ObjectName on) {
        try {
            String res = (String) mServer.getAttribute(on,"ApiCallStat");
            if (res != null && res.trim().length() > 0) {
                String[] items = res.split("\n");
                //test1: 429, 71478179356, 42, 2083342
                for(String item : items) {
                    item = item.trim();
                    int pos = item.indexOf(":");
                    String apiId = item.substring(0, pos);
                    String[] metrics = item.substring(pos + 1).split(",");
                    ApiCallStatPair pair = getApiCallStatPair(apiId);
                    pair.record(Long.parseLong(metrics[0].trim()),
                            Long.parseLong(metrics[1].trim()),
                            Long.parseLong(metrics[2].trim()),
                            Long.parseLong(metrics[3].trim()));
                }
            }

            for(int i = callStats.size() - 1; i >= 0; i--) {
                ApiCallStatPair pair = callStats.get(i);
                if (pair.ttl-- > 0) {
                    String prefix = "apiId " + pair.apiId + " api_stat_";

                    addGuageMetric(prefix + "good_count", pair.latest.goodCount - pair.previous.goodCount);
                    addGuageMetric(prefix + "bad_count", pair.latest.badCount - pair.previous.badCount);
                    addGuageMetric(prefix + "good_ms", (pair.latest.goodNano - pair.previous.goodNano)/1000000);
                    addGuageMetric(prefix + "bad_ms", (pair.latest.badNano - pair.previous.badNano)/1000000);
                } else {
                    callStats.remove(i);
                }
            }
        } catch (InstanceNotFoundException e) {
//            e.printStackTrace();
        } catch (MBeanException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static class ApiCallStat {
        public long goodCount = 0;
        public long goodNano = 0;
        public long badCount = 0;
        public long badNano = 0;
    }

    private static class ApiCallStatPair {
        private final String apiId;
        public ApiCallStatPair(String apiId) {
            this.apiId = apiId;
        }
        public ApiCallStat previous = new ApiCallStat();
        public ApiCallStat latest = new ApiCallStat();
        private int ttl = 10;

        public void record(long goodCount, long goodNano, long badCount, long badNano) {
            ApiCallStat prev = this.previous;
            this.previous = this.latest;
            this.latest = prev;

            this.latest.goodCount = goodCount;
            this.latest.goodNano = goodNano;
            this.latest.badCount = badCount;
            this.latest.badNano = badNano;
            ttl = 10;
        }
    }


}
