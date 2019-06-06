package poco.cn.medialibs.media;

import android.graphics.Bitmap;

public class AVGif {
    private int mId = -1;

    /**
     * 创建GIF编码器
     * @param output        保存的文件路径
     * @param width         原始图像的宽
     * @param height        原始图像的高
     * @param globalPalette 是否使用全局调色板
     *                      如果为true，步骤为create->writePaletteFrame(frame:0~n)->generatePalette->writeFrame(frame:0~n)->close
     *                      如果为false，步骤为create->writeFrame(frame:0~n)->close
     * @return              是否成功
     */
    public boolean create(String output, int width, int height, boolean globalPalette)
    {
        mId = AVNative.AVGifCreate(output, width, height, globalPalette);
        return mId != -1;
    }

    /**
     * 写入一帧图像，用于生成全局调色板
     *
     * @param bitmap    RGBA的Bitmap
     * @return          是否成功
     */
    public boolean writePaletteFrame(Bitmap bitmap)
    {
        if(mId != -1 && bitmap != null) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int size = w * h;
            int[] pixels = new int[size];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            return AVNative.AVGifWritePaletteFrame(mId, pixels, w, h) >= 0;
        }
        return false;
    }

    /**
     * 写入一帧图像，用于生成全局调色板
     * @param pixels    RGBA像素数据
     * @param w         对应pixels的宽度
     * @param h         对应pixels的高度
     * @return          是否成功
     */
    public boolean writePaletteFrame(int[] pixels, int w, int h)
    {
        if(mId != -1 && pixels != null) {
            return AVNative.AVGifWritePaletteFrame(mId, pixels, w, h) >= 0;
        }
        return false;
    }

    /**
     * 生成最终的全局调色板，此函数必须在所有图像帧都调用过writePaletteFrame之后调用
     *
     * @return          是否成功
     */
    public boolean generatePalette()
    {
        if(mId != -1) {
            return AVNative.AVGifGeneratePalette(mId) >= 0;
        }
        return false;
    }

    /**
     * 写入一帧数据
     * @param bitmap    RGBA的Bitmap
     * @param duration  该帧的显示时长
     * @return          是否成功
     */
    public boolean writeFrame(Bitmap bitmap, int duration)
    {
        if(mId != -1 && bitmap != null) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int size = w * h;
            int[] pixels = new int[size];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            return AVNative.AVGifWriteFrame(mId, pixels, w, h, duration) >= 0;
        }
        return false;
    }

    /**
     * 写入一帧数据
     * @param pixels    RGBA像素数据
     * @param w         对应pixels的宽度
     * @param h         对应pixels的高度
     * @param duration  该帧的显示时长
     * @return          是否成功
     */
    public boolean writeFrame(int[] pixels, int w, int h, int duration)
    {
        if(mId != -1 && pixels != null) {
            return AVNative.AVGifWriteFrame(mId, pixels, w, h, duration) >= 0;
        }
        return false;
    }

    /**
     * 结束GIF编码，必须调用此函数才能生成最终文件
     */
    public void close()
    {
        if(mId != -1) {
            AVNative.AVGifClose(mId);
        }
    }

    /**
     * 释放GIF编码器
     */
    public void release()
    {
        if(mId != -1) {
            AVNative.AVGifRelease(mId);
            mId = -1;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }
}
