package poco.cn.medialibs.media.avmediaplayer;

import android.content.res.AssetManager;
import android.view.Surface;

import poco.cn.medialibs.media.AVFrameInfo;
import poco.cn.medialibs.media.AVInfo;
import poco.cn.medialibs.media.AVNative;
import poco.cn.medialibs.media.AVUtils;

public class AVNativeDecoder extends AVMediaDecoder
{
    static {
        System.loadLibrary("fm4");
    }
    static native int       AVMediaPlayerCreate();
    static native boolean   AVMediaPlayerOpen(int id, String input, boolean hardware, AVInfo info);
    static native void      AVMediaPlayerSetSurface(int id, Surface surface);
    static native void      AVMediaPlayerSetOutput(int id, boolean output, boolean async);
    static native void      AVMediaPlayerSetOutputFormat(int id, int format);
    static native void      AVMediaPlayerSetAudioSpeed(int id, float speed);
    static native void      AVMediaPlayerSetOutputSize(int id, int size);
    static native void      AVMediaPlayerSetOutputWH(int id, int width, int height);
    static native boolean   AVMediaPlayerSeek(int id, long time, boolean seekKeyFrame);
    static native boolean   AVMediaPlayerGetAsyncFrame(int id, int dataType, Object buffer, AVFrameInfo info);
    static native boolean   AVMediaPlayerNextVideoFrame(int id, int dataType, Object buffer, AVFrameInfo info);
    static native boolean   AVMediaPlayerNextAudioFrame(int id, Object buffer, AVFrameInfo info);
    static native void      AVMediaPlayerStart(int id);
    static native void      AVMediaPlayerStop(int id);
    static native void      AVMediaPlayerRelease(int id);

    private int     mId = -1;
    private AVInfo  mInfo = new AVInfo();
    private boolean mHardware;
    private int     mDataType = AVNative.DATA_FMT_ARRAY_BYTE;
    private AssetManager mAssetManager;

    public AVNativeDecoder(boolean hardware)
    {
        mHardware = hardware;
    }

    public void setSurface(Surface surface)
    {
        if(mId != -1) {
            AVMediaPlayerSetSurface(mId, surface);
        }
    }

    public void setOutput(boolean output, boolean async) {
        if(mId != -1) {
            AVMediaPlayerSetOutput(mId, output, async);
        }
    }

    @Override
    public void setAssetManager(AssetManager assetManager) {
        mAssetManager = assetManager;
        AVUtils.setAssetManager(assetManager);
    }

    public void setOutputFormat(int pixFormat, int dataType)
    {
        mDataType = dataType;
        if(mId != -1) {
            AVMediaPlayerSetOutputFormat(mId, pixFormat);
        }
    }

    public void setAudioSpeed(float speed)
    {
        if(mId != -1) {
            AVMediaPlayerSetAudioSpeed(mId, speed);
        }
    }

    public void setOutputSize(int size)
    {
        if(mId != -1) {
            AVMediaPlayerSetOutputSize(mId, size);
        }
    }

    public void setOutputSize(int width, int height)
    {
        if(mId != -1) {
            AVMediaPlayerSetOutputWH(mId, width, height);
        }
    }

    @Override
    public AVInfo getAVInfo() {
        return mInfo;
    }

    @Override
    public AVFrameInfo getAsyncFrame()
    {
        AVFrameInfo info = new AVFrameInfo();
        if(mId != -1) {
            if(AVMediaPlayerGetAsyncFrame(mId, mDataType, null, info))
            {
                return info;
            }
        }
        return null;
    }

    public boolean create()
    {
        mId = AVMediaPlayerCreate();
        return mId != -1;
    }

    public boolean open(String file)
    {
        if(file.startsWith("file:///android_asset")) {
            if(mAssetManager == null)
            {
                throw new RuntimeException("使用了assets目录，但未调用setAssetManager");
            }
        }
        if(mId != -1) {
            return AVMediaPlayerOpen(mId, file, mHardware, mInfo);
        }
        return false;
    }

    @Override
    public void start() {
        if(mId != -1) {
            AVMediaPlayerStart(mId);
        }
    }

    @Override
    public boolean needRotate() {
        return false;
    }

    public boolean seek(long time, boolean seekKeyFrame)
    {
        boolean ret = AVMediaPlayerSeek(mId, time, seekKeyFrame);
        return ret;
    }

    public boolean nextVideoFrame(Object buffer, AVFrameInfo info)
    {
        if(AVMediaPlayerNextVideoFrame(mId, mDataType, buffer, info)) {
            return true;
        }
        return false;
    }

    public boolean nextAudioFrame(Object buffer, AVFrameInfo info)
    {
        return AVMediaPlayerNextAudioFrame(mId, buffer, info);
    }

    @Override
    public void stop() {
        if(mId != -1) {
            AVMediaPlayerStop(mId);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    public void release()
    {
        if(mId != -1) {
            AVMediaPlayerRelease(mId);
            mId = -1;
        }
    }
}