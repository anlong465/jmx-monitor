package org.sunrise.jmx.agent;

import org.sunrise.jmx.JmxAgentLogger;

import java.io.*;

public class FileUtil {
    public static void writeContent(File fileToWrite, String content) throws IOException {
        FileWriter fw = null;
        BufferedWriter bw = null;

        try {
            fw = new FileWriter(fileToWrite, true);
            bw = new BufferedWriter(fw);
            bw.write(content);
            bw.flush();
        } catch (IOException ex) {
            JmxAgentLogger.info("org.sunrise.jmx.agent.FileUtil.writeContent(): " + ex.toString());
        } finally {
            doClose(bw);
            doClose(fw);
        }
    }

    public static String readContent(File fileToRead) throws IOException {
        if (!fileToRead.exists()) return null;

        StringBuffer sb = new StringBuffer();
        readContent(fileToRead, sb);
        return sb.toString();
    }

    public static void readContent(File fileToRead, StringBuffer sb) throws IOException {
        if (!fileToRead.exists()) return;

        FileReader fr = null;
        BufferedReader br = null;

        try {
            fr = new FileReader(fileToRead);
            br = new BufferedReader(fr);

            String line;
            while((line=br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            doClose(br);
            doClose(fr);
        }
    }


    public static void doClose(Closeable item) {
        if (item != null) {
            try {
                item.close();
            } catch (IOException ignored) {}
        }
    }
}
