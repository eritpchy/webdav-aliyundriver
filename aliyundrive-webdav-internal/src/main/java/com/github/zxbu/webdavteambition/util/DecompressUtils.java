package com.github.zxbu.webdavteambition.util;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;


public class DecompressUtils {
    public static byte[] decompressXZInMemory(File file) throws IOException {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            return decompressXZInMemory(fin);
        } finally {
            IOUtils.closeQuietly(fin);
        }
    }
    public static byte[] decompressXZInMemory(InputStream fin) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedInputStream in = null;
        XZCompressorInputStream xzIn = null;
        try {
            in = new BufferedInputStream(fin);
            xzIn = new XZCompressorInputStream(in);
            final byte[] buffer = new byte[8192];
            int n = 0;
            while (-1 != (n = xzIn.read(buffer))) {
                baos.write(buffer, 0, n);
            }
            return baos.toByteArray();
        } finally {
            IOUtils.closeQuietly(xzIn);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(baos);
        }
    }

    public static void unxz(File file, File targetDir) throws IOException, ArchiveException {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            unxz(fin, targetDir);
            return;
        } finally {
            IOUtils.closeQuietly(fin);
        }
    }

    public static void unxz(InputStream inputStream, File targetDir) throws IOException, ArchiveException {
        ArchiveInputStream archiveInputStream = null;
        try {
            byte[] tarBytes = decompressXZInMemory(inputStream);

            archiveInputStream = new ArchiveStreamFactory()
                    .createArchiveInputStream(new ByteArrayInputStream(tarBytes));
            ArchiveEntry entry = null;
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (!archiveInputStream.canReadEntryData(entry)) {
                    // log something?
                    continue;
                }
                String name = fileName(targetDir, entry);
                File f = new File(name);
                if (entry.isDirectory()) {
                    if (!f.isDirectory() && !f.mkdirs()) {
                        System.out.println("failed to create directory " + f);
                    }
                } else {
                    File parent = f.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        System.out.println("failed to create directory " + parent);
                    }
                    OutputStream o = null;
                    try {
                        o = new FileOutputStream(f);
                        IOUtils.copy(archiveInputStream, o);
                    } finally {
                        IOUtils.closeQuietly(o);
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(archiveInputStream);
        }

    }

    private static String fileName(File targetDir, ArchiveEntry entry) {
        return new File(targetDir, entry.getName()).getAbsolutePath();
    }
}