package poco.cn.medialibs.utils;

import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

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

    public static void MakeFolder(String path)
    {
        try
        {
            if(path != null)
            {
                File file = new File(path);
                if(!(file.exists() && file.isDirectory()))
                {
                    file.mkdirs();
                }
            }
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }
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

    /**
     * 重命名或者复制到指定路径
     * @param from  源文件路径
     * @param to    目标路径
     */
    public static void renameOrCopy(String from, String to) {

        boolean renameSuccess;
        try {
            renameSuccess = new File(from).renameTo(new File(to));
        } catch (Exception e) {
            e.printStackTrace();
            renameSuccess = false;
        }

        if (!renameSuccess) {
            copyFile(from, to);
        }
    }

    /**
     * 复制文件
     *
     * @param in 源文件路径
     * @param out 目标文件路径
     */
    public static void copyFile(String in, String out) {
        fileChannelCopy(new File(in), new File(out));
    }

    /**
     * 使用文件通道的方式复制文件
     * @param s 源文件
     * @param t 复制到的新文件
     */
    public static void fileChannelCopy(File s, File t) {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(s).getChannel();
            out = new FileOutputStream(t).getChannel();
            in.transferTo(0, in.size(), out);//连接两个通道，并且从in通道读取，然后写入out通道
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
