package org.sunrise.jmx.agent;

import org.sunrise.jmx.FileCleanHook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MetricExchanger {
    private File fileToWrite;
    private File fileToRead;
    private MetricExchanger(String root, String fileNameToWrite, String fileNameToRead) {
//        System.out.println("MetricExchanger --> root: " + root);
        try {
            fileToWrite = new File(root, fileNameToWrite).getCanonicalFile();
            fileToRead = new File(root, fileNameToRead);

            FileCleanHook.add(fileToWrite);
//            FileCleanHook.add(fileToRead);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public void close() {
        FileCleanHook.delete(fileToWrite);
//        FileCleanHook.delete(fileToRead);
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
