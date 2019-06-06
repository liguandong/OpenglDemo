package poco.cn.medialibs.media;

import android.graphics.Bitmap;

/**
 * Created by hwq on 2018/5/4.
 */

class AVVideoSoftwareDecoder  extends AVVideoDecoder{
    private int mId = -1;
    private int mDataType = AVNative.DATA_FMT_BITMAP;
    private Object mBuffer;
    private boolean mBufferReuse = true;
    private Object mSync = new Object();
    private boolean mHardwareDecode;
    private boolean mFirstFrame = true;
    private String mFile;
    private int mSize;
    private int mPixelFormat;
    private int mWidth;
    private int mHeight;
    private int mTime;
    private boolean mSeekKeyFrame;
    private int mSeekIndex;

    /**
     * 构造解码器
     * @param hardware     是否使用硬件解码，小于等于720p的视频，软硬差不多。大于720p的视频用硬解会快一点，2k以上视频用硬解会明显快很多，4k以上视频用硬解部分机器可能无法解码。
     *                     软解因为没有与系统和硬件相关，因此稳定性最佳。
     *                     硬解暂时发现处理大分辨率视频，或用多线程同时处理多个较大视频时部分机型会无法解码。
     *                     硬解因为解码部分未使用cpu，因此cpu占用率比软解低。
     */
    public AVVideoSoftwareDecoder(boolean hardware)
    {
        mHardwareDecode = hardware;
    }

    /**
     * 创建解码器
     * @param file         输入文件，可以是音频也可以是视频
     * @param dataType     指定输出数据类型，值为AVNative.DATA_FMT_*， 支持byte[],int[],Bitmap
     * @param pixFormat    指定解码输出格式，值为AVPixelFormat.*，如需输出Bitmap，应指定为AV_PIX_FMT_RGBA或AV_PIX_FMT_RGB565
     * @return             是否成功
     */
    public boolean create(String file, int dataType, int pixFormat)
    {
        mPixelFormat = pixFormat;
        mDataType = dataType;
        mFile = file;
        mFirstFrame = true;
        mId = AVNative.AVDecoderCreate(file, pixFormat, mHardwareDecode);
        if(mId != -1)
        {
            if(mSize > 0)
            {
                setSize(mSize);
            }
            else if(mWidth > 0 && mHeight > 0)
            {
                setSize(mWidth, mHeight);
            }
        }
        else if(mHardwareDecode)
        {
            mHardwareDecode = false;
            return create(file, dataType, pixFormat);
        }
        return mId != -1;
    }

    /**
     * seek到指定时间，可重复调用
     * @param time          时间戳
     * @param seekKeyFrame  是否以关键帧为起点，如果为true的话，下一次调用nextFrame返回的帧一定是关键帧，但帧时间戳可能比time小很多。为false的话，能确保返回的帧时间戳与time极其接近，但下一次调用nextFrame时所花的时间会比较长
     * @return              是否成功
     */
    public boolean seek(int time, boolean seekKeyFrame)
    {
        mTime = time;
        mSeekKeyFrame = seekKeyFrame;
        synchronized(mSync) {
            return AVNative.AVDecoderSeek(mId, time, seekKeyFrame) >= 0;
        }
    }

    /**
     * Seek到指定帧
     * @param index         第几帧
     * @return              是否成功
     */
    public boolean seekByFrameIndex(int index)
    {
        mSeekIndex = index;
        return AVNative.AVDecoderSeekByFrameIndex(mId, index) >= 0;
    }

    /**
     * 决定是否每次调用nextFrame都返回同一个对象，只改变对象的内容。
     * @param reuse          true，调用nextFrame每次都返回同一个对象，只是改变了对象的内容。false，调用nextFrame每次都返回一个新的对象
     */
    public void setDataReusable(boolean reuse)
    {
        mBufferReuse = reuse;
    }

    /**
     * 设置输出图像大小
     * @param width         宽
     * @param height        高
     */
    public void setSize(int width, int height)
    {
        mWidth = width;
        mHeight = height;
        if(mId != -1) {
            AVNative.AVDecoderSetSize(mId, width, height);
        }
    }

    /**
     * 设置输出图像大小
     * @param size         最短边像素，0表示使用原始大小
     */
    public void setSize(int size)
    {
        mSize = size;
        if(mId != -1) {
            AVNative.AVDecoderSetSize2(mId, size);
        }
    }

    /**
     * 设置裁剪值，相对原始视频尺寸
     * @param left          左边裁剪多少个像素
     * @param top           顶部裁剪多少个像素
     * @param right         右边裁剪多少个像素
     * @param bottom        底部裁剪多少个像素
     */
    public void setCrop(int left, int top, int right, int bottom)
    {
        if(mId != -1) {
            AVNative.AVDecoderSetCrop(mId, left, top, right, bottom);
        }
    }

    /**
     * 读取下一帧
     * @param info          输出，调用前不需要赋值，用于接收解码的帧信息
     * @return              返回帧数据，具体类型由dataType确定，为了提高效率，接口会重用Bitmap，建议不要对返回的Bitmap进行recycle操作
     */
    public Object nextFrame(AVFrameInfo info)
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
            mBuffer = AVNative.AVDecoderNextFrame(mId, mDataType, mBufferReuse?mBuffer:null, info);
            if(mBuffer == null && mFirstFrame && mHardwareDecode)
            {
                //硬解失败，改用软解
                if(mFile != null) {
                    release();
                    mHardwareDecode = false;
                    mId = AVNative.AVDecoderCreate(mFile, mPixelFormat, mHardwareDecode);
                    if(mId != -1) {
                        if(mSize > 0) {
                            setSize(mSize);
                        } else if(mWidth > 0 && mHeight > 0) {
                            setSize(mWidth, mHeight);
                        }
                        if(mTime > 0)
                        {
                            seek(mTime, mSeekKeyFrame);
                            mTime = 0;
                        }
                        else if(mSeekIndex > 0)
                        {
                            seekByFrameIndex(mSeekIndex);
                            mSeekIndex = 0;
                        }
                        mBuffer = AVNative.AVDecoderNextFrame(mId, mDataType, null, info);
                    }
                }
            }
            mFirstFrame = false;
            return mBuffer;
        }
    }

    /**
     * 释放解码器
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
