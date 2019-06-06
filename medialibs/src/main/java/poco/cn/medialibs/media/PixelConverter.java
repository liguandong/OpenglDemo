package poco.cn.medialibs.media;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

public class PixelConverter {
    private int mId = -1;
    private int mDataType = AVNative.DATA_FMT_BITMAP;
    private Object mBuffer;
    private Object mSync = new Object();

    /**
     * 像素格式转换器生成
     * @param dstWidth       目标图像数据宽度
     * @param dstHeight      目标图像数据高度
     * @param dstFormat      目标像素格式
     * @return               转换器id,后续操作需传入对应的转换器id进行操作
     */
    public boolean create(int dstWidth, int dstHeight, int dstFormat)
    {
        mId = AVNative.PixelConverterCreate(dstWidth, dstHeight, dstFormat);
        return mId != -1;
    }

    /**
     * 设置裁剪参数
     * @param cropLeft       左边裁剪多少像素
     * @param cropRight      右边裁剪多少像素
     * @param cropTop        顶部裁剪多少像素
     * @param cropBottom     底部裁剪多少像素
     */
    public void setSrcCrop(int cropLeft, int cropRight, int cropTop, int cropBottom)
    {
        if(mId != -1) {
            AVNative.PixelConverterSetCrop(mId, cropLeft, cropRight, cropTop, cropBottom);
        }
    }

    /**
     * 像素格式转换
     * @param data          输入数据
     * @param dataType      指定输出数据类型，值为AVNative.DATA_FMT_*， 支持byte[],int[],Bitmap
     * @param width         源图像数据宽度
     * @param height        源图像数据高度
     * @param format        源图像数据格式
     * @return              具体类型由dataType确定
     */
    public Object conv(ByteBuffer data, int dataType, int width, int height, int format)
    {
        synchronized(mSync)
        {
            if(mId == -1)
            {
                return null;
            }
            if(mDataType == AVNative.DATA_FMT_BITMAP && mBuffer instanceof Bitmap)
            {
                if(((Bitmap)mBuffer).isRecycled())
                {
                    mBuffer = null;
                }
            }
            mBuffer = AVNative.PixelConverterConv(mId, data, dataType, mBuffer, width, height, format);
            return mBuffer;
        }
    }

    /**
     * 释放转换器
     */
    public void release()
    {
        synchronized(mSync) {
            if(mId != -1) {
                AVNative.AVDecoderRelease(mId);
                mId = -1;
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }
}
