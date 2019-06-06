package poco.cn.medialibs.media.avmediaplayer;

import android.view.Surface;

import java.nio.ByteBuffer;

public class AVNativeRenderer {
    static {
        System.loadLibrary("fm4");
    }
    static native int  nativeCreate();
    static native void nativeSetSurface(int id, Surface surface);
    static native void nativeDrawFrame(int id, ByteBuffer yuv, int format, int width, int height, int stride);
    static native void nativeRelease(int id);

    private int     mId = -1;
    public AVNativeRenderer()
    {
        mId = nativeCreate();
    }

    public void setSurface(Surface surface){
        if(mId != -1) {
            nativeSetSurface(mId, surface);
        }
    }

    public void drawFrame(ByteBuffer yuv, int format, int width, int height, int stride)
    {
        if(mId != -1) {
            nativeDrawFrame(mId, yuv, format, width, height, stride);
        }
    }

    public void release()
    {
        if(mId != -1) {
            nativeRelease(mId);
            mId = -1;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }
}
