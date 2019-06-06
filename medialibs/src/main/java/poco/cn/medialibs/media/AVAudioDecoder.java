package poco.cn.medialibs.media;

import android.graphics.Bitmap;

/**
 * create by : lxh on 2019/3/22
 * Description:
 */

public class AVAudioDecoder {
    private int mId = -1;
    private int mDataType = AVNative.DATA_FMT_ARRAY_BYTE;
    private Object mBuffer;
    private boolean mBufferReuse = true;
    private Object mSync = new Object();
    private boolean mFirstFrame = true;
    private String mFile;
    private int mTime;

    public AVAudioDecoder() {

    }

    /**
     * 创建解码器
     *
     * @param file     输入文件
     * @param dataType 指定输出数据类型，值为AVNative.DATA_FMT_*
     * @return 是否成功
     */
    public boolean create(String file, int dataType) {
        mDataType = dataType;
        mFile = file;
        mFirstFrame = true;
        mId = AVNative.AVAudioDecoderCreate(file);
        return mId != -1;
    }

    /**
     * seek到指定时间，可重复调用
     *
     * @param time 时间戳
     * @return 是否成功
     */
    public boolean seek(int time) {
        mTime = time;
        synchronized (mSync) {
            return AVNative.AVAudioDecoderSeek(mId, time) >= 0;
        }
    }


    /**
     * 决定是否每次调用nextFrame都返回同一个对象，只改变对象的内容。
     *
     * @param reuse true，调用nextFrame每次都返回同一个对象，只是改变了对象的内容。false，调用nextFrame每次都返回一个新的对象
     */
    public void setDataReusable(boolean reuse) {
        mBufferReuse = reuse;
    }

    /**
     * 读取下一帧
     *
     * @param info 输出，调用前不需要赋值，用于接收解码的帧信息
     * @return 返回帧数据，具体类型由dataType确定，为了提高效率，接口会重用Bitmap，建议不要对返回的Bitmap进行recycle操作
     */
    public Object nextFrame(AVFrameInfo info) {
        synchronized (mSync) {
            if (mId == -1) {
                return null;
            }
            if (mDataType == AVNative.DATA_FMT_BITMAP && mBuffer instanceof Bitmap) {
                if (((Bitmap) mBuffer).isRecycled()) {
                    mBuffer = null;
                }
            }
            mBuffer = AVNative.AVAudioDecoderNextFrame(mId, mDataType, mBufferReuse ? mBuffer : null, info);
            mFirstFrame = false;
            return mBuffer;
        }
    }

    /**
     * 释放解码器
     */
    public void release() {
        synchronized (mSync) {
            if (mId != -1) {
                AVNative.AVAudioDecoderRelease(mId);
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
