package org.sunrise.jmx.metric;

import org.sunrise.jmx.agent.JmxMetricPusher;
import org.sunrise.jmx.agent.MetricTimer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class JmxSlave extends JmxManager {
    private String postCheckUrl;
    private JmxSlave(String vmType) throws IOException {
        super(vmType);
        selfVmi.setTmpdir(System.getProperty("java.io.tmpdir"));
        BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream("/tmp/jmx-slave-" + vmType + ".log"));
        PrintStream ps = new PrintStream(bos);
        System.setOut(ps);
        System.setErr(ps);
        postCheckUrl = "http://localhost:8080/slave/jvm/" + vmType;
    }

    private JmxSlave(String vmType, String hostPort) throws IOException {
        this(vmType);
        String host = selfVmi.podName;
        selfVmi.podName = selfVmi.id;
        selfVmi.namespace = "N/A";

        postCheckUrl = "http://" + host + ":" + hostPort + "/slave/jvm/" + vmType;
    }

    @Override
    protected String saveMetrics(VMInfo vmi, String metrics) {
        return JmxMetricPusher.saveToLocalFile(vmi.id, metrics);
    }

    @Override
    protected void checkJVM() {
        super.checkJVM();

        StringBuffer sb = new StringBuffer();

        sb.append(selfVmi.id).append("#").append(selfVmi.getTmpdir());
        for(VMInfo vmi : allVms) {
            sb.append("\n").append(vmi.id).append("#").append(vmi.getTmpdir());
        }

        try {
            String result = postCheck(postCheckUrl, sb.toString());
            if (result != null) {
                long nextMetricTime = Long.parseLong(result);
                MetricTimer.setNextMetricTime(nextMetricTime);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length == 1) {
                new JmxSlave(args[0]).run();
            } else {
                new JmxSlave(args[0], args[1]).run();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static String postCheck(String url, String requestBody) throws IOException {
        HttpURLConnection connect = (HttpURLConnection) new URL(url).openConnection();
        connect.setDoInput(true);
        connect.setDoOutput(true);
        connect.setRequestProperty("Content-Type", "text/plain");
        connect.setRequestProperty("Content-Length", String.valueOf(requestBody.length()));
        connect.setRequestMethod("POST");

        OutputStream os = connect.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.write(requestBody);
        writer.flush();
        writer.close();
        os.close();

        int responseCode = connect.getResponseCode();
        System.out.println("responseCode=" + responseCode);
        if (responseCode == 200) { // Success
            BufferedReader brd = new BufferedReader(new InputStreamReader(
                    connect.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = brd.readLine()) != null) {
                sb.append(line).append("\n");
            }
            brd.close();
            connect.disconnect();
            String result = sb.toString();
            System.out.println("response result: " + result);

            return result;
        }

        return null;
    }

}
