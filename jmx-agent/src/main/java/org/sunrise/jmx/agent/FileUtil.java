package org.sunrise.jmx.agent;

import java.io.*;

public class FileUtil {
    public static void writeContent(File fileToWrite, String content) throws IOException {
        FileWriter fw = null;
        BufferedWriter bw = null;

        try {
            fw = new FileWriter(fileToWrite);
            bw = new BufferedWriter(fw);
            bw.write(content);
            bw.flush();
        } catch (FileNotFoundException ex) {
            System.out.println(ex.toString());
        } finally {
            doClose(bw);
            doClose(fw);
        }
    }

    public static String readContent(File fileToRead) throws IOException {
        if (!fileToRead.exists()) return null;

        FileReader fr = null;
        BufferedReader br = null;

        try {
            fr = new FileReader(fileToRead);
            br = new BufferedReader(fr);

            StringBuffer sb = new StringBuffer();
            String line;
            while((line=br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
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
