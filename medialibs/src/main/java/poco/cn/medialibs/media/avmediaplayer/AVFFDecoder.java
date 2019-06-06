package poco.cn.medialibs.media.avmediaplayer;

import android.media.MediaFormat;

import java.nio.ByteBuffer;

import poco.cn.medialibs.media.AVError;
import poco.cn.medialibs.media.AVFrameInfo;

public class AVFFDecoder {
    static {
        System.loadLibrary("fm4");
    }
    static native int  nativeCreate(MediaFormat format, byte[] csd0, byte[] csd1, int outoutFormat);
    static native int  nativeSendPacket(int id, ByteBuffer packet, long limit, long pts, long dts, long duration);
    static native int  nativeReceiveFrame(int id, AVFrameInfo info, int dataType, Object buffer);
    static native void nativeFlush(int id);
    static native void nativeRelease(int id);

    private int mId = -1;
    public boolean create(MediaFormat format, byte[] csd0, byte[] csd1, int outputFormat)
    {
        mId = nativeCreate(format, csd0, csd1, outputFormat);
        return mId != -1;
    }

    public int sendPacket(ByteBuffer packet, long limit, long pts, long dts, long duration)
    {
        if(mId != -1) {
            return nativeSendPacket(mId, packet, limit, pts, dts, duration);
        }
        return AVError.UNKNOWN;
    }

    public int receiveFrame(AVFrameInfo info, int dataType, Object buffer)
    {
        if(mId != -1) {
            return nativeReceiveFrame(mId, info, dataType, buffer);
        }
        return AVError.UNKNOWN;
    }

    public void flush()
    {
        if(mId != -1) {
            nativeFlush(mId);
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
