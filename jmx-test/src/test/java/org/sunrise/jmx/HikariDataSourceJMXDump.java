package org.sunrise.jmx;

import com.zaxxer.hikari.HikariDataSource;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Set;

public class HikariDataSourceJMXDump {
    public static void main(String[] args) throws ReflectionException, InstanceNotFoundException, IntrospectionException, AttributeNotFoundException, MBeanException {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://10.1.2.96:3306/dtm?useUnicode=true&serverTimezone=UTC&useSSL=false");
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        ds.setUsername("dtm");
        ds.setPassword("dtm");
        try {
            ds.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        MBeanServer mServer = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectInstance> allBeans = mServer.queryMBeans(null, null);

        String beanName = "com.zaxxer.hikari:type=Pool";
        String beanAttrName = null;
        for(ObjectInstance bean: allBeans) {
            ObjectName on = bean.getObjectName();
            String onName = on.toString();
            if (onName.startsWith("java.") || onName.startsWith("javax.")) {
                continue;
            }
            System.out.println("******: " + onName + "\n");

            if (on.toString().contains(beanName)) {
//                System.out.println("******: " + onName + "\n");
                MBeanInfo beanInfo = mServer.getMBeanInfo(on);
                MBeanAttributeInfo[] attrInfos = beanInfo.getAttributes();
                MBeanOperationInfo[] operInfos = beanInfo.getOperations();
                if (attrInfos != null && attrInfos.length > 0)
                    for(MBeanAttributeInfo attrInfo : attrInfos) {
                        if (beanAttrName == null) {
                            System.out.println("**************: \t" + attrInfo.getName() + "\n");
                        } else {
                            if(attrInfo.getName().equals(beanAttrName)) {
                                System.out.println("**************: \t" + beanAttrName);
                                System.out.println("=" + mServer.getAttribute(on, beanAttrName).toString());
                                System.out.println();
                                break;
                            }
                        }
                    }

                if (operInfos != null && operInfos.length > 0)
                    for(MBeanOperationInfo operInfo : operInfos) {
                        if (beanAttrName == null) {
                            System.out.println("**************: \t" + operInfo.getName() + "\n");
                        } else {
                            if(operInfo.getName().equals(beanAttrName)) {
                                System.out.println("**************: \t" + beanAttrName);
                                System.out.println(" result: \n");
                                System.out.println(mServer.invoke(on, beanAttrName, null, null).toString());
                                System.out.println();
                                break;
                            }
                        }
                    }
                break;
            }
        }




    }
}
