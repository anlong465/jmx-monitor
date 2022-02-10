package org.sunrise.jmx.agent;

import java.io.*;

public class MetricExchanger {
    private File fileToWrite;
    private File fileToRead;
    private MetricExchanger(String root, String fileNameToWrite, String fileNameToRead) {
//        System.out.println("MetricExchanger --> root: " + root);
        fileToWrite = new File(root, fileNameToWrite);
        fileToWrite.deleteOnExit();
        fileToRead = new File(root, fileNameToRead);
        fileToRead.deleteOnExit();
    }

    public String writeRead(String content) throws IOException {
        try {
            FileUtil.writeContent(fileToWrite, content);
        } catch (FileNotFoundException fnf) {
            fileToWrite = new File(fileToWrite.getAbsolutePath());
            FileUtil.writeContent(fileToWrite, content);
        }

        try {
            return FileUtil.readContent(fileToRead);
        } catch (FileNotFoundException fnf) {
            fileToRead = new File(fileToRead.getAbsolutePath());
            return FileUtil.readContent(fileToRead);
        }
    }

    public static MetricExchanger makeExchangerForClientToServer(String selfId) {
        String root = System.getProperty("java.io.tmpdir");
        if (root == null) {
            root = "/tmp";
        }
        return new MetricExchanger(root,
                "metrics-" + selfId + ".c2s",
                "metrics-" + selfId + ".s2c");
    }

    public static MetricExchanger makeExchangerForServerToClient(String root, String tmpdir, String clientId) {
        return new MetricExchanger(root + clientId + "/root" + tmpdir,
                "metrics-" + clientId + ".s2c",
                "metrics-" + clientId + ".c2s");
    }
}
