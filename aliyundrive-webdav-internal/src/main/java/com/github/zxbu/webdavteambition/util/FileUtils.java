package com.github.zxbu.webdavteambition.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class FileUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    public static void closeCloseable(Object cloeable) {
        try {
            if (cloeable == null) return;
            if (cloeable instanceof Closeable) {
                ((Closeable) cloeable).close();
            }
        } catch (Exception ignored) {
        }
    }

    public static void write(InputStream is, File file) throws IOException {
        if (file.exists()) {
            if (!file.isFile()) {
                file.delete();
            }
        }
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(is);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);

            byte buffer[] = new byte[8192];
            int count;
            while ((count = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, count);
            }
        } finally {
            closeCloseable(bis);
            closeCloseable(bos);
            closeCloseable(fos);
        }
    }

    public static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getContent(File file) {
        if (file == null) {
            return null;
        }
        if (!file.exists()) {
            return null;
        }
        if (!file.isFile()) {
            return null;
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            return convertStreamToString(fileInputStream);
        } catch (Exception e) {
            LOGGER.error("getContent", e);
        } finally {
            closeCloseable(fileInputStream);
        }
        return null;
    }

}
