package org.sunrise.jmx.agent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Properties;

public class CommonUtil {
    public final static String SELF_ID_SEPARATOR="_";
    public static String getJavaCmd() {
        String javaCmd = System.getProperty("sun.java.command");
        if (javaCmd != null) {
            return getJavaCmd(javaCmd);
        } else {
            Properties props = System.getProperties();
            for(Object key : props.keySet()) {
                String keyStr = (String) key;
                if (keyStr.startsWith("proc")) {
                    return keyStr;
                }
            }
        }
        return null;
    }

    public static String getJavaCmd(String javaCmd) {
        String firstParam = null;
        int pos = javaCmd.indexOf(' ');
        if (pos > 0) {
            firstParam = javaCmd.substring(pos + 1);

            javaCmd = javaCmd.substring(0, pos);
            pos = firstParam.indexOf(' ');
            if (pos > 0) {
                firstParam = firstParam.substring(0, pos);
            }

            pos = firstParam.lastIndexOf('/');
            if (pos > 0) {
                firstParam = firstParam.substring(pos + 1);
            }
        }

        pos = javaCmd.lastIndexOf('/');  // jar file path
        if (pos > 0) {
            javaCmd = javaCmd.substring(pos + 1);
        } else {
            int len = javaCmd.length();
            String suffix = javaCmd.substring(len - 4, len);
            if (!suffix.toLowerCase().equals(".jar")) { // it should be one java class
                pos = javaCmd.lastIndexOf('.');
                if (pos > 0) javaCmd = javaCmd.substring(pos + 1);
            }
        }

        return firstParam == null ? javaCmd : javaCmd + " " + firstParam;
    }

    public static String getenv(String[] keys, String defaultValue) {
        for(String key : keys) {
            String value = System.getenv(key);
            if (value != null) {
                return value.trim();
            }
        }
        return defaultValue;
    }

    public static String getenv(String[] keys) {
        return getenv(keys, null);
    }

    private static final long MB = 1024L * 1024L;
    public static double bytes2MB(double bytes) {
        BigDecimal b = new BigDecimal( bytes/MB);
        return b.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double ms2Second(double milliseconds) {
        BigDecimal b = new BigDecimal( milliseconds/1000);
        return b.setScale(3, RoundingMode.HALF_EVEN).doubleValue();
    }
}
