package poco.cn.medialibs.utils;

import android.text.TextUtils;

import java.io.File;

/**
 * Created by Simon Meng on 2018/8/16.
 * Guangzhou Beauty Information Technology Co.,Ltd
 */
public class FileUtil
{

    public static boolean createDirectory(String path) {
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            return file.mkdirs();
        }
        return false;
    }


    public static boolean isFileExist(String path) {
        boolean isFileExist = false;
        if (!TextUtils.isEmpty(path) && new File(path).exists()) {
            isFileExist = true;
        }
        return isFileExist;
    }

    public static boolean deleteFile(String path) {
        boolean isDeleteSuccessFul = false;
        if (!TextUtils.isEmpty(path) && isFileExist(path)) {
            File file = new File(path);
            if (file.isDirectory()) {
                isDeleteSuccessFul = file.delete();
                if (!isDeleteSuccessFul) {
                    for (File item : file.listFiles()) {
                        item.delete();
                    }
                    isDeleteSuccessFul = file.delete();
                }
            } else {
                isDeleteSuccessFul = file.delete();
            }
        }
        return isDeleteSuccessFul;
    }

    public static void emptyDirecotyFile(String direcoty) {
        if (isFileExist(direcoty)) {
            File file = new File(direcoty);
            for (File item : file.listFiles()) {
                deleteFile(item.getAbsolutePath());
            }
        }
    }



    public static String makeCacheFileName(String cacheDirectory) {
        final String fileSuffix = ".img";
        int randomNumber = (int)(Math.random() * 1000000);
        return cacheDirectory + File.separator + randomNumber + fileSuffix;
    }
}
