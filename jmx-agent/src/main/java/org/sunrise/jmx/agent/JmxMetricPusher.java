package org.sunrise.jmx.agent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class JmxMetricPusher {
    protected static int uploadToSvrCount = 3;
    public static Long pushMetrics(String jmxServerUrl, String auth, String selfId, String metrics) {
        if (metrics == null) return null;

        String result = null;
        if (uploadToSvrCount > 0) {
            try {
                result = uploadToSvr(jmxServerUrl, auth, selfId, metrics);
                if (uploadToSvrCount != 3) uploadToSvrCount = 3;
            } catch (Throwable e) {
                CommonUtil.logException(e);
                if (JmxAgentRunnable.isInitiatedByServer()) {
                    uploadToSvrCount--;
                    result = saveToLocalFile(selfId, metrics);
                }
            }
        } else {
            result = saveToLocalFile(selfId, metrics);
        }

        if (result != null) {
            return Long.parseLong(result.trim());
        }

        return null;
    }

    private static String uploadToSvr(String jmxServerUrl, String auth, String selfId, String requestBody) throws IOException {
//        System.out.println("uploadToSvr: " + jmxServerUrl + selfId);

        HttpURLConnection connect = (HttpURLConnection) new URL(jmxServerUrl + selfId).openConnection();
        connect.setDoInput(true);
        connect.setDoOutput(true);
        connect.setRequestProperty("Content-Type", "application/json");
        connect.setRequestProperty("Content-Length", String.valueOf(requestBody.length()));
        connect.setRequestMethod("POST");
        connect.setRequestProperty("Authorization", auth);
        connect.setConnectTimeout(2000);
        connect.setReadTimeout(2000);

        OutputStream os = connect.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(requestBody);
        writer.flush();
        writer.close();
        os.close();

        int responseCode = connect.getResponseCode();
//        System.out.println("responseCode=" + responseCode);
        if (responseCode == 200) { // Success
            BufferedReader brd = new BufferedReader(new InputStreamReader(
                    connect.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = brd.readLine()) != null) {
                sb.append(line).append("\n");
            }
            brd.close();
            connect.disconnect();
            String result = sb.toString();
//            System.out.println("response result: " + result);

            return result;
        }

        return null;
    }

    private static MetricExchanger clientToServer = null;
    public static String saveToLocalFile(String selfId, String body) {
        try {
            if (clientToServer == null) {
                clientToServer = MetricExchanger.makeExchangerForClientToServer(selfId);
            }

            return clientToServer.writeRead(body);
        } catch (Throwable e) {
            clientToServer = null;
            e.printStackTrace();
            return null;
        }
    }

}

