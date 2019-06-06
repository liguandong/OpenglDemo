package poco.cn.medialibs.media;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import poco.cn.medialibs.media.avmediaplayer.AVMediaPlayer;


public class AVVideoView extends GLSurfaceView{
    private String[] mFiles;
    private AVMediaPlayerRenderer mMediaPlayer = null;

    public AVVideoView(Context context) {
        this(context, null);
    }

    public AVVideoView(Context context, boolean output) {
        this(context, null, output);
    }

    public AVVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, false);
    }

    public AVVideoView(Context context, AttributeSet attrs, boolean output) {
        super(context, attrs);

        setEGLContextClientVersion(2);
        mMediaPlayer = new AVMediaPlayerRenderer(this, output);
        setRenderer(mMediaPlayer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    public void setVideoSource(String[] files) {
        mFiles = files;
        mMediaPlayer.setVideoSource(mFiles);
        setKeepScreenOn(true);
    }

    public AVMediaPlayer getPlayer()
    {
        if(mMediaPlayer != null) {
            return mMediaPlayer.getPlayer();
        }
        return null;
    }

    public void setOnSeekCompleteListener(AVMediaPlayer.OnSeekCompleteListener l)
    {
        mMediaPlayer.getPlayer().setOnSeekCompleteListener(l);
    }

    public void setOnPositionListener(AVMediaPlayer.OnPositionListener l, int updateInterval)
    {
        mMediaPlayer.getPlayer().setOnPositionListener(l, updateInterval);
    }

    public void setOnErrorListener(AVMediaPlayer.OnErrorListener l)
    {
        mMediaPlayer.getPlayer().setOnErrorListener(l);
    }

    public void setOnPreparedListener(AVMediaPlayer.OnPreparedListener l)
    {
        mMediaPlayer.getPlayer().setOnPreparedListener(l);
    }

    public void setOnPlayStatusListener(AVMediaPlayer.OnPlayStatusListener l)
    {
        mMediaPlayer.setOnPlayStatusListener(l);
    }

    public void release() {
        mMediaPlayer.release();
    }

    public boolean start() {
        return mMediaPlayer.getPlayer().start();
    }

    public void stop() {
        mMediaPlayer.getPlayer().stop();
    }

    public void pause() {
        mMediaPlayer.getPlayer().pause();
    }

    public void seekTo(int msec, boolean seekKeyFrame) {
        mMediaPlayer.getPlayer().seek(msec, seekKeyFrame);
    }

    public boolean isPlaying() {
        return mMediaPlayer.getPlayer().isPlaying();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mMediaPlayer.updateDisplay();
    }
}
