package org.sunrise.jmx.metric.mbean;

import org.sunrise.jmx.agent.CommonUtil;
import org.sunrise.jmx.metric.JVMGCCollector;
import org.sunrise.jmx.metric.JVMMemoryCollector;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import java.util.Collection;
import java.util.Set;

public class GCMemoryMetricCollector  extends MBeanMetricCollector {
    public GCMemoryMetricCollector() throws MalformedObjectNameException {
        super(new ObjectName("java.lang:type=GarbageCollector,*"));
    }

    private GCMemoryStat currentStat = new GCMemoryStat();
    private GCMemoryStat previousStat = null;

    @Override
    protected void process(MBeanServer mServer, Set<ObjectName> mbeans) {
        if (currentStat != null) {
            currentStat = null;
            for(ObjectName on : mbeans) {
                process(mServer, on);
            }
            if (currentStat != null) {
                if (previousStat != null) {
                    addGuageMetric("jvm_gc_minor_memory_freed",
                            CommonUtil.bytes2MB(currentStat.memoryFreedByMinorGC - previousStat.memoryFreedByMinorGC));
                    addGuageMetric("jvm_gc_full_memory_freed",
                            CommonUtil.bytes2MB(currentStat.memoryFreedByFullGC - previousStat.memoryFreedByFullGC));
                }
                previousStat = currentStat;
            }
        }
//        processLastGC(mServer, mbeans);
    }

    protected void processLastGC(MBeanServer mServer, Set<ObjectName> mbeans) {
        GCMemoryUsage usageAfterGc = new GCMemoryUsage();
        GCMemoryUsage usageBeforeGc = new GCMemoryUsage();
        for(ObjectName on : mbeans) {
            try {
                CompositeData lastGC = (CompositeData) mServer.getAttribute(on, "LastGcInfo");
                if (lastGC == null) continue;

                dump("LastGcInfo(" + on.getKeyProperty("name") + ")", lastGC);
                CompositeType lastGCType = lastGC.getCompositeType();
                for(String key : lastGCType.keySet()) {
                    if (key.endsWith("sageAfterGc")) {
                        processLastGC(usageAfterGc, (TabularData) lastGC.get(key), "afterGc");
                    } else
                    if (key.endsWith("sageBeforeGc")) {
                        processLastGC(usageBeforeGc, (TabularData) lastGC.get(key), "beforeGc");
                    }
                }
            } catch (Exception e) {
                CommonUtil.logException(e);
            }
        }
        addGuageMetric("jvm_heap_mp_afterGc_used_mb", CommonUtil.bytes2MB(usageAfterGc.heapMemoryUsed));
        addGuageMetric("jvm_nonheap_mp_afterGc_used_mb", CommonUtil.bytes2MB(usageAfterGc.nonheapMemoryUsed));
        addGuageMetric("jvm_heap_mp_beforeGc_used_mb", CommonUtil.bytes2MB(usageBeforeGc.heapMemoryUsed));
        addGuageMetric("jvm_nonheap_mp_beforeGc_used_mb", CommonUtil.bytes2MB(usageBeforeGc.nonheapMemoryUsed));
    }

    private void processLastGC(GCMemoryUsage usage, TabularData gcInfo, String type) {
        Collection<?> items = gcInfo.values();
        for(Object item : items) {
            CompositeData mp = (CompositeData) item;

            String mpName = JVMMemoryCollector.refineMemoryPoolName((String) mp.get("key"));

            CompositeData value = (CompositeData) mp.get("value");
            long memoryUsed = (Long) value.get("used");

            String prefix;
            if (JVMMemoryCollector.isHeapMemoryPool(mpName)) {
                usage.heapMemoryUsed += memoryUsed;
            } else {
                usage.nonheapMemoryUsed += memoryUsed;
            }

            prefix = "poolName " + mpName + " jvm_mp_" + type + "_used_mb";
            addGuageMetric(prefix, CommonUtil.bytes2MB(memoryUsed));
        }
    }


    private void dump(String prefix, CompositeData cd) {
        CompositeType type = cd.getCompositeType();
        for(String key : type.keySet()) {
            Object value = cd.get(key);
            if (value instanceof CompositeData) {
                dump(prefix +  "->" + key, (CompositeData) value);
            } else
            if (value instanceof TabularData) {
                dump(prefix +  "->" + key, (TabularData) value);
            } else {
                System.out.println(prefix +  "->" + key + "=" + value);
            }
        }
    }
    private void dump(String prefix, TabularData td) {
        Collection<?> items = td.values();
        int i = 0;
        for(Object item : items) {
            dump(prefix +  "[" + (i++) + "]", (CompositeData) item);
        }
    }

    @Override
    public void process(MBeanServer mServer, ObjectName on) {
        getMemoryFreedByGC(mServer, on, "TotalMemoryFreed");

//        try {
//            System.out.println("=========" + on.toString());
//            MBeanInfo beanInfo = mServer.getMBeanInfo(on);
//            MBeanAttributeInfo[] attrInfos = beanInfo.getAttributes();
//            MBeanOperationInfo[] operInfos = beanInfo.getOperations();
//            if (attrInfos != null) {
//                for(MBeanAttributeInfo attrInfo : attrInfos) {
//                    if ("LastGcInfo".equals(attrInfo.getName())) continue;
//                    System.out.println("**************: \t" + attrInfo.getName() + "="
//                            + mServer.getAttribute(on, attrInfo.getName()));
//                }
//            }
//            if (operInfos != null) {
//                for(MBeanOperationInfo operInfo : operInfos) {
//                    System.out.println("**************: \t" + operInfo.getName());
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

    private boolean getMemoryFreedByGC(MBeanServer mServer, ObjectName on, String attrName) {
        try {
            Long value = (Long) mServer.getAttribute(on, attrName);

            if (currentStat == null) {
                currentStat = new GCMemoryStat();
            }

            String gcName = on.getKeyProperty("name");
            if (JVMGCCollector.isFullGC(gcName)) {
                currentStat.memoryFreedByFullGC += value;
            } else {
                currentStat.memoryFreedByMinorGC += value;
            }

            return true;
        } catch (AttributeNotFoundException e) {
        } catch (Exception e) {
            CommonUtil.logException(e);
        }
        return false;
    }

    private static class GCMemoryStat {
        long memoryFreedByMinorGC = 0;
        long memoryFreedByFullGC = 0;
    }

    private static class GCMemoryUsage {
        long heapMemoryUsed = 0;
        long nonheapMemoryUsed = 0;
    }

}
