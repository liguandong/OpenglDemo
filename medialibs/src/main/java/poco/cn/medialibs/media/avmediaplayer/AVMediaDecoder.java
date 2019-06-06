package poco.cn.medialibs.media.avmediaplayer;

import android.content.res.AssetManager;
import android.view.Surface;

import poco.cn.medialibs.media.AVFrameInfo;
import poco.cn.medialibs.media.AVInfo;

public abstract class AVMediaDecoder {
    public abstract boolean create();
    public abstract boolean open(String file);
    public abstract boolean seek(long time, boolean seekKeyFrame);
    public abstract boolean nextVideoFrame(Object buffer, AVFrameInfo info);
    public abstract boolean nextAudioFrame(Object buffer, AVFrameInfo info);
    public abstract boolean needRotate();
    public abstract void start();
    public abstract void stop();
    public abstract void release();
    public abstract void setSurface(Surface surface);
    public abstract void setOutput(boolean output, boolean async);
    public abstract void setOutputFormat(int pixFormat, int dataType);
    public abstract void setOutputSize(int size);
    public abstract void setOutputSize(int width, int height);
    public abstract void setAudioSpeed(float speed);
    public abstract void setAssetManager(AssetManager assetManager);
    public abstract AVFrameInfo getAsyncFrame();
    public abstract AVInfo getAVInfo();
}
