package com.callrecorder.android;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Created by jokinkuang on 2017/2/21.
 */
public class SDCardUtils {
    private static String TAG = "SDCardUtils";

    public static boolean isMounted() {
        boolean ret = false;
        ret = Environment.getExternalStorageState().equalsIgnoreCase(
                Environment.MEDIA_MOUNTED);
        return ret;
    }

    public static void ensureDirExists(File dirFile) {
        if (dirFile.exists() && dirFile.isFile()) {
            try {
                copyFile(dirFile, new File(dirFile.getPath()+ new Random().nextInt()));
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
            dirFile.delete();
        }
        if (! dirFile.exists()) {
            dirFile.mkdirs();
        }
    }

    public static void createDir(String dirPath, boolean nomedia) {
        ensureDirExists(new File(dirPath));
        if (nomedia) {
            File nomediafile = new File(dirPath + "/.nomedia");
            try {
                nomediafile.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    /** Bytes */
    public static long getAvailableSpace(String path) {
        StatFs stat = new StatFs(path);
        long availableBlocks;
        long blockSize;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            availableBlocks = stat.getAvailableBlocksLong();
            blockSize = stat.getBlockSizeLong();
        } else {
            availableBlocks = stat.getAvailableBlocks();
            blockSize = stat.getBlockSize();
        }
        return availableBlocks * blockSize;
    }

    /** Bytes */
    public static long getTotalSpace(String path) {
        StatFs stat = new StatFs(path);
        long blocksCount;
        long blockSize;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blocksCount = stat.getBlockCountLong();
            blockSize = stat.getBlockSizeLong();
        } else {
            blocksCount = stat.getBlockCount();
            blockSize = stat.getBlockSize();
        }
        return blocksCount * blockSize;
    }

    private static final int MAX_BUFF_SIZE = 1024 * 1024;
    private static final int MIN_BUFF_SIZE = 4096;
    public static void copyFile(File src, File des) throws IOException {
        if (des.exists()) {
            des.delete();
        }
        des.createNewFile();

        FileInputStream in = new FileInputStream(src);
        int length = in.available();
        if (length == 0) {
            length = MIN_BUFF_SIZE;
        } else if (length >= MAX_BUFF_SIZE) {
            length = MAX_BUFF_SIZE;
        }
        FileOutputStream out = new FileOutputStream(des);
        byte[] buffer = new byte[length];
        while (true) {
            int ins = in.read(buffer);
            if (ins == -1) {
                in.close();
                out.flush();
                out.close();
                return;
            } else {
                out.write(buffer, 0, ins);
            }
        }
    }
}
