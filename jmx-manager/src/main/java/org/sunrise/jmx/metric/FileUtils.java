package org.sunrise.jmx.metric;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileUtils {
    public static void doClose(Closeable item) {
        if (item != null) {
            try {
                item.close();
            } catch (IOException e) {}
        }
    }

    public static void copyFile(File source, File destination) throws IOException {
        FileInputStream  input = null;
        FileOutputStream output = null;
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            input = new FileInputStream(source);
            sourceChannel = input.getChannel();
            output = new FileOutputStream(destination);
            destChannel = output.getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } finally {
            doClose(sourceChannel);
            doClose(destChannel);
            doClose(input);
            doClose(output);
        }
    }

    public static void initFile(File f, int size) throws IOException {
        FileOutputStream output = null;
        FileChannel destChannel = null;
        try {
            output = new FileOutputStream(f);
            destChannel = output.getChannel();
            byte[] bytes = new byte[size];
            for(int i = 0; i < size; i++) {
                bytes[i] = 0;
            }
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            destChannel.write(bb);
            destChannel.force(true);
        } finally {
            doClose(destChannel);
            doClose(output);
        }
    }

    public static List<String> readFileLines(String fileName) throws IOException {
        Path p = FileSystems.getDefault().getPath(fileName);
        List<String> result = Files.readAllLines(p, StandardCharsets.UTF_8);
        return result;
    }

    public static String readFileContent(String fileName) throws IOException {
        Path p = FileSystems.getDefault().getPath(fileName);
        byte[] bytes = Files.readAllBytes(p);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static File findFile(File root, String fileName) {
        if (root == null) return null;
        if (root.isFile()) {
            if (root.getName().equals(fileName)) {
                return root;
            } else {
                return null;
            }
        }
        File[] kids = root.listFiles();
        if (kids == null || kids.length == 0) return null;
        for(File kid : kids) {
            if (kid.isFile() && kid.getName().equals(fileName)) {
                return kid;
            }
        }
        for(File kid : kids) {
            if (kid.isDirectory()) {
                File f = findFile(kid, fileName);
                if (f != null) return f;
            }
        }
        return null;
    }

}
