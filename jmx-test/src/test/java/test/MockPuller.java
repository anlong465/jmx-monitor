package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MockPuller {
    public static void main(String[] args) {
        String url = "http://jmx-exploer-default.apps.gu.cp.com/ClusterMetrics";
        if (args != null && args.length > 0) {
            url = args[0];
        }
        System.out.println("url: " + url);

        long nextTime = System.currentTimeMillis();
        while(true) {
            nextTime += 30L*1000;
            System.out.println("\n======================================================================");
            try {
                String result = pullMetrics(url);
                if (result != null) {
                    String[] lines = result.split("\n");
                    for(String line : lines) {
                        if (line.contains("jvm_heap_used_mb")) {
                            System.out.print("\t");
                            System.out.println(line);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            long toSleep = nextTime - System.currentTimeMillis();
            System.out.println("toSleep: " + toSleep);
            if (toSleep > 10) {
                try {
                    Thread.sleep(toSleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String pullMetrics(String url) throws IOException {

        HttpURLConnection connect = (HttpURLConnection) new URL(url).openConnection();
//        connect.setDoInput(true);
//        connect.setDoOutput(true);
        connect.setRequestProperty("Content-Type", "application/json");
        connect.setRequestMethod("GET");

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
//            System.out.println("response result: " + result);

            return result;
        }

        return null;
    }

}
