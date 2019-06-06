package poco.cn.medialibs.media.avmediaplayer;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.PlaybackParams;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import poco.cn.medialibs.media.AVError;
import poco.cn.medialibs.media.AVFrameInfo;
import poco.cn.medialibs.media.AVInfo;
import poco.cn.medialibs.media.AVNative;
import poco.cn.medialibs.media.AVUtils;

public class AVMediaPlayer {
    private static final int OK = 0;
    private static final int EOF = -1;
    private static final int WAIT = -2;
    private static final int UNKNOWN = -3;

    private static final int MESSAGE_COMPLETE = 1;
    private static final int MESSAGE_PROGRESS = 2;
    private static final int MESSAGE_VIDEOCHANGED = 3;
    private static final int MESSAGE_PREPARED = 4;
    private static final int MESSAGE_SEEKCOMPLETE = 5;
    private static final int MESSAGE_ERROR = 6;
    private Object mVideoSync = new Object();
    private Object mAudioSync = new Object();
    private boolean mHardwareDecode;
    private AudioTrack mAudioTrack;
    private boolean mPaused;
    private boolean mStoped = true;
    private boolean mVideoSeeking = false;
    private boolean mAudioSeeking = false;
    private boolean mPausingBeforeSeeking = false;
    private boolean mSeekOperating = false;
    private boolean mAudioComplete = false;
    private boolean mVideoComplete = false;
    private Thread mAudioDecodeThread;
    private Thread mVideoDecodeThread;
    private Thread mAudioPlayThread;
    private Thread mVideoPlayThread;
    private ArrayBlockingQueue<AVFrameInfo> mAudioQueue = new ArrayBlockingQueue<AVFrameInfo>(1);
    private ArrayBlockingQueue<AVFrameInfo> mVideoQueue = new ArrayBlockingQueue<AVFrameInfo>(1);
    private OnPlayStatusListener mOnPlayStatusListener;
    private OnPositionListener mOnPositionListener;
    private OnPreparedListener mOnPreparedListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnErrorListener mOnErrorListener;
    private long mCurrentPosition;
    private long mLastPosition;
    private long mTime;
    private long mEscaped = 0;
    private boolean mRepeat = false;
    private long mTimeStart = 0;
    private long mTimeEnd = 0;
    private boolean mAccurateTime;
    private float mVolume = 1;
    private int mProgressNotifyInterval = 1000;
    private IDrawFrameCallback mDrawFrameCallback;
    private AVVideoComp mVideoComp;
    private double mSpeed = 1.0f;
    private int mSampleRate;
    private int mChannels = 2;
    private boolean mPreparing;
    private long mCurVPts;
    private boolean mOutput;
    private ValueAnimator mSpeedAnimator;
    private long mSeekTime = 0;
    private boolean mSeekKeyFrame = false;
    private Thread mSeekThread;
    private AVMediaPlayerSynchronizer mSynchronizer;
    private boolean mSeekSynced = false;
    private boolean mAudioLoopExited = true;
    private boolean mVideoLoopExited = true;
    private boolean mAudioPlayExited = true;
    private boolean mVideoPlayExited = true;

    public interface IDrawFrameCallback
    {
        /**
         * 帧绘制回调
         * @param player 播放器实例
         * @param frame 保存帧相关绘制数据
         */
        void onDrawFrame(AVMediaPlayer player, AVFrameInfo frame);
    }

    public interface OnPlayStatusListener
    {
        /**
         * 视频播放完毕回调
         * @param player 播放器实例
         */
        void onCompletion(AVMediaPlayer player);

        /**
         * 视频切换回调
         * @param player 播放器实例
         * @param index  视频的数组索引值
         * @param file   文件
         * @param info   视频信息
         * @param rotate 是否需要对画面进行旋转,true的话需要对显示的画面进行旋转，可通过getRotation获取旋转角度
         */
        void onVideoChanged(AVMediaPlayer player, int index, String file, AVInfo info, boolean rotate);
    }

    public interface OnPositionListener
    {
        /**
         * 播放进度回调
         * @param player 播放器实例
         */
        void onPosition(AVMediaPlayer player);
    }

    public interface OnPreparedListener
    {
        /**
         * 视频加载完毕回调
         */
        void onPrepared(AVMediaPlayer mp);
    }

    public interface OnSeekCompleteListener
    {
        /**
         * seek完毕回调
         */
        void onSeekComplete(AVMediaPlayer mp);
    }

    public interface OnErrorListener
    {
        /**
         * 出错回调
         */
        void onError(AVMediaPlayer mp, int code);
    }

    /**
     * 构造播放器
     * @param hardware     是否使用硬件解码，小于等于720p的视频，软硬差不多。大于720p的视频用硬解会快一点，2k以上视频用硬解会明显快很多，4k以上视频用硬解部分机器可能无法解码。
     *                     软解因为没有与系统和硬件相关，因此稳定性最佳。
     *                     硬解暂时发现处理大分辨率视频，或用多线程同时处理多个较大视频时部分机型会无法解码。
     *                     硬解因为解码部分未使用cpu，因此cpu占用率比软解低。
     * @param output       是否需要输出图像数据，true的解码速度会比false慢
     * @return             是否成功
     */
    public AVMediaPlayer(boolean hardware, boolean output)
    {
        mOutput = output;
        mHardwareDecode = hardware;
        mVideoComp = new AVVideoComp();
    }

    /**
     * 设置AssetManager，用于支持播放assets目录下的文件
     * @param assetManager AssetManager
     */
    public void setAssetManager(AssetManager assetManager)
    {
        mVideoComp.setAssetManager(assetManager);
    }

    /**
     * 设置播放视频文件
     * @param files 视频文件，支持多个文件，当做一个视频播放
     */
    public void setVideoSource(String[] files)
    {
        for(String file : files) {
            if(!AVUtils.isFileExist(file)) {
                throw new IllegalArgumentException("the input file not found.");
            }
        }
        synchronized(mAudioSync) {
            synchronized(mVideoSync) {
                mVideoComp.create(files);
            }
        }
    }

    /**
     * 设置Surface,不要在调用这个接口后立即释放surface（调用Surface.release函数）,否则如果接口调用顺序有问题则无法绘制图像
     */
    public void setSurface(Surface surface)
    {
        Log.d("hwq", "setSurface");
        mVideoComp.setSurface(surface);
    }

    /**
     * 设置同步器，用于同步播放器的播放状态，播放器的seek,start,stop，pause的操作将通过同步器回调给调用方
     */
    public void setSynchronizer(AVMediaPlayerSynchronizer synchronizer)
    {
        mSynchronizer = synchronizer;
    }

    /**
     * 设置是否异步输出帧图像数据，异步输出的帧数据通过getAsyncFrame获取，否则由IDrawFrameCallback的onDrawFrame返回
     * @param async 是否异步输出帧图像数据
     */
    public void setAsyncOutput(boolean async)
    {
        mVideoComp.setOutput(mOutput, async);
    }

    /**
     * 设置输出的图像格式和数据类型
     * @param pixFormat    指定解码输出格式，值为AVPixelFormat.*，如需输出Bitmap，应指定为AV_PIX_FMT_RGBA或AV_PIX_FMT_RGB565
     * @param dataType     指定输出数据类型，值为AVNative.DATA_FMT_*， 支持byte[],int[],Bitmap
     */
    public void setOutputFormat(int pixFormat, int dataType)
    {
        mVideoComp.setOutputFormat(pixFormat, dataType);
    }

    /**
     * 设置输出帧图像数据的大小
     * @param size 最短边的size
     */
    public void setOutputSize(int size)
    {
        if(mVideoComp != null)
        {
            mVideoComp.setOutputSize(size);
        }
    }

    /**
     * 限制播放的区域
     * @param start     开始点
     * @param end       结束点,为0表示不设置结束点
     * @param accurate  是否精确定位，否则以最近的关键帧为起始点，为true的话开始播放或回位的速度慢一些
     */
    public void setScope(int start, int end, boolean accurate)
    {
        if(start > end && end > 0){
            return;
        }
        mAccurateTime = accurate;
        mTimeStart = start;
        mTimeEnd = end;
        if(mVideoComp != null)
        {
            mVideoComp.setEndPosition(mTimeEnd);
        }
    }

    /**
     * 设置播放速度
     * @param speed    大于1是慢速，小于1快速
     */
    public void setSpeed(double speed)
    {
        mEscaped += mCurVPts * (speed - mSpeed);
        mSpeed = speed;
        if(mAudioTrack != null) {
            synchronized(mAudioTrack) {
                if(mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    mAudioTrack.stop();
                }
                mAudioTrack.release();
                int minBufferSize = AudioTrack.getMinBufferSize((int)mSampleRate, mChannels>1? AudioFormat.CHANNEL_OUT_STEREO: AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if(mSpeed != 1) {
                    //加速后如果缓冲区不够大，AudioTrack底层可能会挂
                    minBufferSize *= 8;
                }
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, mChannels>1? AudioFormat.CHANNEL_OUT_STEREO: AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
                synchronized(mAudioTrack) {
                    mAudioTrack.setStereoVolume(mVolume, mVolume);
                    setAudioSpeed(mSampleRate, (float)mSpeed);
                    mAudioTrack.play();
                }
            }
        }
    }

    /**
     * 设置播放速度
     * @param speed         大于1是慢速，小于1快速
     * @param interpolator  插值器
     * @param duration      插值器对应的时间
     */
    public void setSpeed(final double speed, Interpolator interpolator, int duration)
    {
        if(interpolator == null || isPlaying() == false) {
            if(mSpeedAnimator != null)
            {
                mSpeedAnimator.cancel();
                mSpeedAnimator = null;
            }
            setSpeed(speed);
        }
        else
        {
            double temp = mSpeed;
            if(mSpeedAnimator != null)
            {
                mSpeedAnimator.cancel();
                mSpeedAnimator = null;
            }
            mSpeedAnimator = new ValueAnimator();
            mSpeedAnimator.setInterpolator(interpolator);
            mSpeedAnimator.setDuration(duration);
            mSpeedAnimator.setFloatValues(new float[]{(float)temp,(float)speed});
            mSpeedAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if(isPlaying()) {
                        double speed = (Float) animation.getAnimatedValue();
                        setSpeed(speed);
                    }
                }
            });
            mSpeedAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    setSpeed(speed);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            mSpeedAnimator.start();
        }

    }

    /**
     * 是否循环播放
     */
    public void setLooping(boolean looping)
    {
        mRepeat = looping;
    }

    /**
     * 音量调整
     */
    public void setVolume(float volume)
    {
        mVolume = volume;
        if(mAudioTrack != null) {
            synchronized(mAudioTrack) {
                if(mAudioTrack != null) {
                    mAudioTrack.setStereoVolume(volume, volume);
                }
            }
        }
    }

    /**
     * 获取音量
     */
    public float getVolume()
    {
        return mVolume;
    }

    /**
     * 播放器状态回调
     */
    public void setOnPlayStatusListener(OnPlayStatusListener l)
    {
        mOnPlayStatusListener = l;
    }

    /**
     * 播放器就绪回调
     */
    public void setOnPreparedListener(OnPreparedListener listener)
    {
        mOnPreparedListener = listener;
    }

    /**
     * 播放进度回调
     * @param l 监听器
     * @param updateInterval 回调的时间间隔
     */
    public void setOnPositionListener(OnPositionListener l, int updateInterval)
    {
        if(updateInterval < 1)
        {
            updateInterval = 1;
        }
        mProgressNotifyInterval = updateInterval;
        mOnPositionListener = l;
    }

    /**
     * seek完毕回调
     */
    public void setOnSeekCompleteListener(OnSeekCompleteListener l)
    {
        mOnSeekCompleteListener = l;
    }

    /**
     * 出错回调
     */
    public  void setOnErrorListener(OnErrorListener l)
    {
        mOnErrorListener = l;
    }

    /**
     * 帧绘制回调，输出方式为同步的时候，帧图像数据由frame.data返回
     */
    public void setDrawFrameCallback(IDrawFrameCallback cb)
    {
        mDrawFrameCallback = cb;
    }

    /**
     * 获取视频宽度
     */
    public int getVideoWidth()
    {
        return mVideoComp.getVideoWidth();
    }

    /**
     * 获取视频高度
     */
    public int getVideoHeight()
    {
        return mVideoComp.getVideoHeight();
    }

    /**
     * 获取旋转角度
     */
    public int getRotation()
    {
        return mVideoComp.getRotation();
    }

    /**
     * 读取异步帧，setAsyncOutput(true)时通过这个函数获取帧数据
     * @return 返回帧数据及相关信息
     */
    public AVFrameInfo getAsyncFrame()
    {
        return mVideoComp.getAsyncFrame();
    }

    /**
     * 加载视频
     */
    public boolean prepare()
    {
        return prepare(false);
    }

    /**
     * 加载视频，异步
     */
    public void prepareAsync()
    {
        prepare(true);
    }

    /**
     * 获取视频总时长
     */
    public long getDuration()
    {
        if(mVideoComp != null) {
            return mVideoComp.getDuration();
        }
        return 0;
    }

    /**
     * 获取当前播放位置
     */
    public long getCurrentPosition()
    {
        return mCurrentPosition;
    }

    /**
     * 是否播放中
     */
    public boolean isPlaying()
    {
        return mStoped == false && mPaused == false;
    }

    /**
     * 是否循环播放
     */
    public boolean isLooping()
    {
        return mRepeat;
    }

    /**
     *  告诉播放器正在拖动seek控件，调用这个函数后将不会回调OnPositionListener的onPosition接口，以避免滑块闪动
     */
    public void seekStart()
    {
        mSeekOperating = true;
        mPausingBeforeSeeking = mPaused;
    }

    /**
     *  调用seekStart后必须调用这个函数来结束拖动状态
     */
    public void seekEnd()
    {
        mSeekOperating = false;
        if(mPausingBeforeSeeking == false && mStoped == false)
        {
            start();
        }
    }

    /**
     * seek
     */
    public void seek(long time, boolean seekKeyFrame)
    {
        if(time < mTimeStart)
        {
            time = mTimeStart;
        }
        synchronized(mAudioSync) {
            synchronized(mVideoSync) {
                pause();
            }
        }
        mSeekTime = time;
        mSeekKeyFrame = seekKeyFrame;
        if(mSeekThread == null)
        {
            mSeekThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    runSeek();
                    mSeekThread = null;
                }
            });
            mSeekThread.start();
        }
    }

    /**
     * 播放
     */
    public boolean start()
    {
        if(mVideoComp == null || mVideoComp.isPrepared() == false) {
            return false;
        }
        if(mAudioDecodeThread == null) {
            if(mTimeStart > 0)
            {
                seekTo(mTimeStart, !mAccurateTime);
            }
            else if(mAudioComplete && mVideoComplete)
            {
                mVideoComp.seek(0, true);
            }

            mAudioLoopExited = false;
            mVideoLoopExited = false;
            mAudioPlayExited = false;
            mVideoPlayExited = false;
            mPaused = false;
            mStoped = false;
            mAudioComplete = false;
            mVideoComplete = false;
            updatePosition(0);

            mAudioDecodeThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    audioLoop();
                }
            });
            mAudioDecodeThread.start();

            mVideoDecodeThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    videoLoop();
                }
            });
            mVideoDecodeThread.start();

            mAudioPlayThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    audioPlayLoop();
                }
            });
            mAudioPlayThread.start();

            mVideoPlayThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    videoPlayLoop();
                }
            });
            mVideoPlayThread.start();
        }
        else if(mPaused && mStoped == false)
        {
            mPaused = false;
            mStoped = false;
            mTime = System.currentTimeMillis();
            synchronized(mAudioSync) {
                synchronized(mVideoSync) {
                    if(mAudioTrack != null && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED) {
                        synchronized(mAudioTrack) {
                            mAudioTrack.play();
                        }
                    }
                }
            }
        }
        mVideoComp.start();
        if(mSynchronizer != null) {
            mSynchronizer.start();
        }
        return true;
    }

    /**
     * 暂停
     */
    public void pause()
    {
        if(mSpeedAnimator != null)
        {
            mSpeedAnimator.cancel();
            mSpeedAnimator = null;
        }
        mTime = System.currentTimeMillis();
        mPaused = true;
        if(mAudioTrack != null)
        {
            synchronized(mAudioTrack) {
                if(mAudioTrack != null && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    mAudioTrack.pause();
                }
            }
            if(mSynchronizer != null) {
                mSynchronizer.pause();
            }
        }
    }

    /**
     * 停止
     */
    public void stop()
    {
        mPaused = true;
        mStoped = true;
        if(mSpeedAnimator != null)
        {
            mSpeedAnimator.cancel();
            mSpeedAnimator = null;
        }
        if(mVideoComp != null) {
            mVideoComp.stop();
        }
        Thread thread = mAudioDecodeThread;
        if(thread != null)
        {
            thread.interrupt();
        }
        thread = mVideoDecodeThread;
        if(thread != null)
        {
            thread.interrupt();
        }
        thread = mAudioPlayThread;
        if(thread != null)
        {
            thread.interrupt();
        }
        thread = mVideoPlayThread;
        if(thread != null)
        {
            thread.interrupt();
        }
        //等待播放结束
        try {
            while(mAudioLoopExited == false
                    || mVideoLoopExited == false
                    || mAudioPlayExited == false
                    || mVideoPlayExited == false) {
                Thread.sleep(1);
            }
        }
        catch(Exception e)
        {
        }
    }

    /**
     * 释放播放器
     */
    public void release()
    {
        //stop会等待播放结束，为了避免死锁，不要放在下面的synchronized里
        stop();

        synchronized(mAudioSync) {
            synchronized(mVideoSync) {
                if(mVideoComp != null) {
                    mVideoComp.release();
                }
            }
        }
    }

    private void seekSync(long time)
    {
        if(mSeekSynced == false)
        {
            mSeekSynced = true;
            if(mSynchronizer != null)
            {
                mSynchronizer.seek(time);
            }
        }
    }

    private boolean prepare(boolean async)
    {
        if(mVideoComp == null){
            return false;
        }

        final ArrayList<Integer> result = new ArrayList<Integer>();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                long n = System.currentTimeMillis();
                synchronized(mAudioSync) {
                    synchronized(mVideoSync) {
                        if(mVideoComp.isPrepared()) {
                            Message msg = new Message();
                            msg.what = MESSAGE_PREPARED;
                            mHandler.sendMessage(msg);
                            Log.d("hwq", "has prepared");
                            result.add(1);
                            return;
                        }
                        if(mSynchronizer != null) {
                            if(false == mSynchronizer.prepare())
                            {
                                Message msg = new Message();
                                msg.what = MESSAGE_ERROR;
                                mHandler.sendMessage(msg);
                                return;
                            }
                        }
                        if(mVideoComp.prepare(mHardwareDecode)) {
                            AVFrameInfo info = new AVFrameInfo();
                            nextVideoFrame(info);

                            Message msg = new Message();
                            msg.what = MESSAGE_PREPARED;
                            mHandler.sendMessage(msg);
                            Log.d("hwq", "prepare "+(System.currentTimeMillis()-n));
                            result.add(1);
                        }
                        else
                        {
                            Message msg = new Message();
                            msg.what = MESSAGE_ERROR;
                            mHandler.sendMessage(msg);
                        }
                    }
                }
            }
        });
        thread.start();

        if(async == false)
        {
            try {
                thread.join();
                return result.size() > 0;
            }
            catch(Exception e)
            {}
        }
        return false;
    }

    private void updatePosition(long position)
    {
        mCurrentPosition = position;
        if(mCurrentPosition - mLastPosition >= mProgressNotifyInterval
                || mCurrentPosition < mLastPosition
                || mCurrentPosition == 0)
        {
            mLastPosition = mCurrentPosition;
            if(mOnPositionListener != null && mSeekOperating == false)
            {
                Message msg = new Message();
                msg.what = MESSAGE_PROGRESS;
                mHandler.sendMessage(msg);
            }
        }
    }

    private synchronized void loopLeave()
    {
        synchronized(mAudioSync) {
            synchronized(mVideoSync) {
                if(mAudioComplete && mVideoComplete) {
                    mPaused = true;
                    mStoped = true;
                    mSeekSynced = false;
                    boolean sendMessage = (mAudioDecodeThread != null) || (mVideoDecodeThread != null);
                    mAudioDecodeThread = null;
                    mVideoDecodeThread = null;
                    if(mAudioPlayThread != null) {
                        mAudioPlayThread.interrupt();
                        mAudioPlayThread = null;
                        sendMessage = true;
                    }
                    if(mVideoPlayThread != null) {
                        mVideoPlayThread.interrupt();
                        mVideoPlayThread = null;
                        sendMessage = true;
                    }
                    if(mAudioTrack != null) {
                        synchronized(mAudioTrack) {
                            if(mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                                mAudioTrack.stop();
                            }
                            mAudioTrack.release();
                        }
                        mAudioTrack = null;
                        sendMessage = true;
                    }
                    if(mSynchronizer != null) {
                        mSynchronizer.stop();
                    }
                    if(mOnPlayStatusListener != null && sendMessage) {
                        Message msg = new Message();
                        msg.what = MESSAGE_COMPLETE;
                        mHandler.sendMessage(msg);
                    }
                }
            }
        }
    }

    private boolean checkComplete()
    {
        if(mVideoComplete && mAudioComplete)
        {
            if(mRepeat == false) {
                return true;
            }
            else
            {
                synchronized(mAudioSync) {
                    synchronized(mVideoSync) {
                        if(!mVideoComp.seek(mTimeStart, !mAccurateTime)) {
                            return true;
                        }
                    }
                }
                mVideoSeeking = true;
                mVideoComplete = false;
                mAudioComplete = false;
                updatePosition(mTimeStart);
                mTime = System.currentTimeMillis();
                mEscaped = mTimeStart;
                return false;
            }
        }
        return false;
    }

    private void audioLoop()
    {
        if(mVideoComp == null) {
            return;
        }
        mAudioLoopExited = false;
        mAudioComplete = false;
        mVideoComp.setEndPosition(mTimeEnd);
        while(!mStoped) {
            if(mPaused || mAudioComplete) {
                try {
                    Thread.sleep(5);
                } catch(Exception e) {
                    break;
                }
                if(mRepeat == false && checkComplete())
                {
                    break;
                }
                continue;
            }

            AVFrameInfo audio = new AVFrameInfo();
            int code = nextAudioFrame(audio);
            if(code == OK) {
                updatePosition(audio.pts);
                seekSync(audio.pts);
                if(mAudioSeeking == false) {
                    try {
                        mAudioQueue.put(audio);
                    } catch(Exception e) {
                        break;
                    }
                }
                mAudioSeeking = false;
            }
            else
            {
                if(code != WAIT) {
                    mAudioComplete = true;
                    if(checkComplete()) {
                        break;
                    }
                }
                else
                {
                    try {
                        Thread.sleep(5);
                    } catch(Exception e) {
                        break;
                    }
                }
            }
        }
        mAudioComplete = true;
        loopLeave();
        mAudioLoopExited = true;
    }

    private void videoLoop()
    {
        if(mVideoComp == null) {
            return;
        }
        int count = 0;
        long time = 0;
        long n = System.currentTimeMillis();
        mVideoLoopExited = false;
        mVideoComplete = false;
        mTime = System.currentTimeMillis();
        mEscaped = 0;
        mVideoComp.setEndPosition(mTimeEnd);
        while(!mStoped) {
            if((mPaused && mVideoSeeking == false) || mVideoComplete) {
                try {
                    Thread.sleep(5);
                } catch(Exception e) {
                    break;
                }
                if(mRepeat == false && checkComplete())
                {
                    break;
                }
                continue;
            }

            AVFrameInfo video = new AVFrameInfo();
            n = System.currentTimeMillis();
            int code = nextVideoFrame(video);
            time += (System.currentTimeMillis()-n);
            if(code == OK) {
                count++;
                AVVideo v = mVideoComp.getCurrent();
                if(v != null && v.info.audioDuration == 0) {
                    updatePosition(video.pts);
                    seekSync(video.pts);
                }
                mCurVPts = video.pts;
                boolean seeking = mVideoSeeking;
                long pts = (long)(video.pts*mSpeed);
                try {
                    mVideoQueue.put(video);
                    long c = System.currentTimeMillis();
                    mEscaped += (c - mTime);
                    mTime = c;
                    if(seeking)
                    {
                        mEscaped = pts;
                        mVideoSeeking = false;
                    }
                    long sleep = pts - mEscaped;
                    if(sleep > 0) {
                        //Log.d("hwq", "sleep "+sleep + ",pts "+pts);
                        Thread.sleep(sleep);
                    }
                } catch(Exception e) {
                    break;
                }
            }
            else
            {
                if(code != WAIT) {
                    mVideoComplete = true;
                    if(checkComplete()) {
                        break;
                    }
                }
                else
                {
                    try {
                        Thread.sleep(5);
                    } catch(Exception e) {
                        break;
                    }
                }
            }
        }
        mVideoComplete = true;
        loopLeave();
        if(count > 0) {
            Log.d("hwq", "avg:" + time / count);
        }
        mVideoLoopExited = true;
    }

    private void audioPlayLoop()
    {
        mAudioPlayExited = false;
        while(!mStoped) {
            if(mPaused) {
                try {
                    Thread.sleep(5);
                } catch(Exception e) {
                    break;
                }
                continue;
            }
            try {
                AVFrameInfo info = mAudioQueue.take();
                playSound((byte[]) info.data, info.sampleRate, info.channels);
            } catch(Exception e) {
                e.printStackTrace();
                break;
            }
        }
        mAudioPlayExited = true;
    }

    private void videoPlayLoop()
    {
        mVideoPlayExited = false;
        while(!mStoped) {
            try {
                AVFrameInfo info = mVideoQueue.take();
                if(mDrawFrameCallback != null)
                {
                    mDrawFrameCallback.onDrawFrame(this, info);
                }
            } catch(Exception e) {
                break;
            }
        }
        mVideoPlayExited = true;
    }

    private void playSound(byte[] data, int sampleRate, int channels)
    {
        buildAudioTrack(sampleRate, channels);
        mChannels = channels;
        mSampleRate = sampleRate;
        if(mAudioTrack != null)
        {
            synchronized(mAudioTrack) {
                mAudioTrack.write(data, 0, data.length);
            }
        }
    }

    private void buildAudioTrack(int sampleRate, int channels) {
        if(mAudioTrack == null || mChannels != channels || mSampleRate != sampleRate) {
            if(mAudioTrack != null) {
                synchronized(mAudioTrack) {
                    if(mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        mAudioTrack.stop();
                    }
                    mAudioTrack.release();
                }
                mAudioTrack = null;
            }
            int minBufferSize = AudioTrack.getMinBufferSize((int)sampleRate, channels>1? AudioFormat.CHANNEL_OUT_STEREO: AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if(mSpeed != 1) {
                //加速后如果缓冲区不够大，AudioTrack底层可能会挂
                minBufferSize *= 8;
            }
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channels>1? AudioFormat.CHANNEL_OUT_STEREO: AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
            synchronized(mAudioTrack) {
                mAudioTrack.setStereoVolume(mVolume, mVolume);
                setAudioSpeed(sampleRate, (float)mSpeed);
                mAudioTrack.play();
            }
        }
    }

    private void setAudioSpeed(int sampleRate, float speed)
    {
        if(mAudioTrack != null) {
            synchronized(mAudioTrack) {
                if(Build.VERSION.SDK_INT >= 23) {
                    try {
                        PlaybackParams params = mAudioTrack.getPlaybackParams();
                        if(speed == 1) {
                            params.setSpeed(1);
                        }
                        else {
                            params.setSpeed(1.0f / (float) speed);
                        }
                        mAudioTrack.setPlaybackParams(params);
                    } catch(Exception e) {
                        mAudioTrack.setPlaybackRate((int) (sampleRate / speed));
                    }
                } else {
                    if(speed >= 0.25 && speed <= 4) {
                        mVideoComp.setAudioSpeed(1.0f / speed);
                    }
                    else
                    {
                        mAudioTrack.setPlaybackRate((int) (sampleRate / speed));
                    }
                }
            }
        }
    }

    private int nextVideoFrame(AVFrameInfo info)
    {
        synchronized(mVideoSync) {
            if(mVideoComp == null) {
                return UNKNOWN;
            }
            return mVideoComp.nextVideoFrame(null, info);
        }
    }

    private int nextAudioFrame(AVFrameInfo info)
    {
        synchronized(mAudioSync)
        {
            if(mVideoComp == null) {
                return UNKNOWN;
            }
            return mVideoComp.nextAudioFrame(null, info);
        }
    }

    private void runSeek()
    {
        synchronized(mAudioSync) {
            synchronized(mVideoSync) {
                long time;
                boolean seekKeyFrame;
                do {
                    time = mSeekTime;
                    seekKeyFrame = mSeekKeyFrame;
                    if(mVideoComp.seek(time, seekKeyFrame)) {
                        mVideoSeeking = true;
                        mAudioSeeking = true;
                        mAudioComplete = false;
                        mVideoComplete = false;
                        updatePosition(time);
                        mSeekSynced = false;
                    }
                }while((time != mSeekTime || seekKeyFrame != mSeekKeyFrame) && mStoped == false && mVideoComp != null);
                if(mAudioTrack != null)
                {
                    synchronized(mAudioTrack) {
                        boolean isPlaying = mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
                        if(isPlaying)
                        {
                            mAudioTrack.pause();
                        }
                        mAudioTrack.flush();
                        mAudioQueue.clear();
                        if(isPlaying)
                        {
                            mAudioTrack.play();
                        }
                    }
                }
            }
        };
        Message msg = new Message();
        msg.what = MESSAGE_SEEKCOMPLETE;
        mHandler.sendMessage(msg);
    }

    private void seekTo(long time, boolean seekKeyFrame)
    {
        synchronized(mAudioSync) {
            synchronized(mVideoSync) {
                pause();
                if(mVideoComp.seek(time, seekKeyFrame)) {
                    mVideoSeeking = true;
                    mAudioSeeking = true;
                    mAudioComplete = false;
                    mVideoComplete = false;
                    updatePosition(time);
                }
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    private Handler mHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if(msg.what == MESSAGE_COMPLETE) {
                if(mOnPlayStatusListener != null) {
                    mOnPlayStatusListener.onCompletion(AVMediaPlayer.this);
                }
            }
            else if(msg.what == MESSAGE_PROGRESS)
            {
                if(mOnPositionListener != null) {
                    mOnPositionListener.onPosition(AVMediaPlayer.this);
                }
            }
            else if(msg.what == MESSAGE_VIDEOCHANGED)
            {
                if(mOnPlayStatusListener != null) {
                    mOnPlayStatusListener.onVideoChanged(AVMediaPlayer.this, mVideoComp.getCurrentIndex(), mVideoComp.getCurrent().file, mVideoComp.getCurrent().info, mVideoComp.needRotate());
                }
            }
            else if(msg.what == MESSAGE_ERROR)
            {
                if(mOnErrorListener != null) {
                    mOnErrorListener.onError(AVMediaPlayer.this, AVError.UNKNOWN);
                }
            }
            else if(msg.what == MESSAGE_PREPARED)
            {
                if(mOnPreparedListener != null) {
                    mOnPreparedListener.onPrepared(AVMediaPlayer.this);
                }
            }
            else if(msg.what == MESSAGE_SEEKCOMPLETE)
            {
                if(mOnSeekCompleteListener != null) {
                    mOnSeekCompleteListener.onSeekComplete(AVMediaPlayer.this);
                }
            }

        }
    };

    private class AVVideo extends AVMediaDecoder
    {
        public String file;
        public AVInfo info = new AVInfo();
        public boolean audioEnd;
        public boolean videoEnd;
        public int frameReadCount;
        private AVMediaDecoder mDecoder;

        public boolean isEnd()
        {
            return audioEnd && videoEnd;
        }

        public boolean hasVideo()
        {
            return info.videoDuration > 0;
        }

        public boolean hasAudio()
        {
            return info.audioDuration > 0;
        }

        public void setEnd(boolean end)
        {
            audioEnd = end;
            videoEnd = end;
        }

        @Override
        public boolean needRotate() {
            return mDecoder.needRotate();
        }

        public boolean seek(long time, boolean seekKeyFrame)
        {
            boolean ret = mDecoder.seek(time, seekKeyFrame);
            if(ret) {
                frameReadCount = 0;
            }
            return ret;
        }

        public void reset()
        {
            audioEnd = false;
            videoEnd = false;
            seek(0, true);
        }

        public boolean nextVideoFrame(Object buffer, AVFrameInfo info)
        {
            if(mDecoder.nextVideoFrame(buffer, info)) {
                frameReadCount++;
                return true;
            }
            return false;
        }

        public boolean nextAudioFrame(Object buffer, AVFrameInfo info)
        {
            return mDecoder.nextAudioFrame(buffer, info);
        }

        @Override
        public void setAssetManager(AssetManager assetManager) {
            mDecoder.setAssetManager(assetManager);
        }

        @Override
        public void setAudioSpeed(float speed) {
            mDecoder.setAudioSpeed(speed);
        }

        @Override
        public AVInfo getAVInfo() {
            return info;
        }

        @Override
        public AVFrameInfo getAsyncFrame() {
            return mDecoder.getAsyncFrame();
        }

        @Override
        public boolean create() {
            if(mDecoder == null) {
                if(mOutput == false && mHardwareDecode == true) {
                    mDecoder = new AVSystemDecoder();
                }
                else
                {
                    mDecoder = new AVNativeDecoder(mHardwareDecode);
                }
            }
            return mDecoder.create();
        }

        @Override
        public boolean open(String file) {
            if(mDecoder.open(file))
            {
                info = mDecoder.getAVInfo();
                return true;
            }
            return false;
        }

        @Override
        public void start() {
            mDecoder.start();
        }

        @Override
        public void stop() {
            mDecoder.stop();
        }

        @Override
        public void release() {
            mDecoder.release();
        }

        @Override
        public void setSurface(Surface surface) {
            mDecoder.setSurface(surface);
        }

        @Override
        public void setOutput(boolean output, boolean async) {
            mDecoder.setOutput(output, async);
        }

        @Override
        public void setOutputFormat(int pixFormat, int dataType) {
            mDecoder.setOutputFormat(pixFormat, dataType);
        }

        @Override
        public void setOutputSize(int size) {
            mDecoder.setOutputSize(size);
        }

        @Override
        public void setOutputSize(int width, int height) {
            mDecoder.setOutputSize(width, height);
        }
    }

    private class AVVideoComp
    {
        private AVVideo mCurrent;
        private long mEndPosition;
        private long mPtsOffset;
        private boolean mPrepared = false;
        private Surface mSurface;
        private boolean mAsync = true;
        private boolean mOutput = false;
        private int mOutputSize = 0;
        private int mPixelFormat = 0;
        private int mDataType = AVNative.DATA_FMT_ARRAY_BYTE;
        private ArrayList<AVVideo> mVideos = new ArrayList<AVVideo>();
        private String[] mInputs;
        private AssetManager mAssetManager;

        public void setAssetManager(AssetManager assetManager)
        {
            mAssetManager = assetManager;
            for(AVVideo v : mVideos) {
                v.setAssetManager(mAssetManager);
            }
        }

        public boolean create(String[] inputs)
        {
            if(inputs == null || inputs.length == 0)
            {
                return false;
            }
            if(mInputs != null && inputs.length == mInputs.length)
            {
                boolean equals = true;
                for(int i = 0; i < inputs.length; i++)
                {
                    if(!inputs[i].equals(mInputs[i]))
                    {
                        equals = false;
                        break;
                    }
                }
                if(equals == true)
                {
                    return true;
                }
            }

            release();
            for(int i = 0; i < inputs.length; i++) {
                AVVideo info = new AVVideo();
                info.file = inputs[i];
                if(info.file == null)
                    return false;
                if(info.create() == false)
                {
                    return false;
                }
                info.setAssetManager(mAssetManager);
                if(mSurface != null) {
                    info.setSurface(mSurface);
                }
                info.setOutputSize(mOutputSize);
                info.setOutput(mOutput, mAsync);
                info.setOutputFormat(mPixelFormat, mDataType);
                mVideos.add(info);
            }
            mInputs = inputs;
            return true;
        }

        public boolean prepare(boolean hardware)
        {
            for(AVVideo v : mVideos) {
                if(false == v.open(v.file))
                {
                    return false;
                }
            }
            mPrepared = true;
            nextVideo(false);
            return true;
        }

        public boolean isPrepared()
        {
            return mPrepared;
        }

        public boolean seek(long time, boolean seekKeyFrame)
        {
            long total = getDuration();
            if(time > total){
                time = total;
            }
            if(mTimeEnd > 0 && time > mTimeEnd)
            {
                time = mTimeEnd;
            }
            if(time < 0){
                time = 0;
            }
            if(time < mTimeStart)
            {
                time = mTimeStart;
            }
            long duration = 0;
            for(int i = 0; i < mVideos.size(); i++) {
                AVVideo v = mVideos.get(i);
                if(time <= duration + v.info.duration)
                {
                    time -= duration;
                    for(int j = 0; j < mVideos.size(); j++) {
                        mVideos.get(j).setEnd(j < i);
                    }
                    nextVideo(false);
                    return v.seek(time, seekKeyFrame);
                }
                duration += v.info.duration;
            }
            return false;
        }

        public void setSurface(Surface surface)
        {
            mSurface = surface;
            for(AVVideo v : mVideos) {
                v.setSurface(surface);
            }
        }

        public void setOutputSize(int size)
        {
            mOutputSize = size;
            for(AVVideo v : mVideos) {
                v.setOutputSize(size);
            }
        }

        public void setOutput(boolean output, boolean async)
        {
            mOutput = output;
            mAsync = async;
            for(AVVideo v : mVideos) {
                v.setOutput(output, async);
            }
        }

        public void setOutputFormat(int pixFormat, int dataType)
        {
            mPixelFormat = pixFormat;
            mDataType = dataType;
            for(AVVideo v : mVideos) {
                v.setOutputFormat(pixFormat, dataType);
            }
        }

        public void setAudioSpeed(float speed)
        {
            for(AVVideo v : mVideos) {
                v.setAudioSpeed(speed);
            }
        }

        public int getVideoWidth()
        {
            AVVideo video = mCurrent;
            if(video == null || video.info == null)
            {
                return 0;
            }
            return video.info.width;
        }

        public int getVideoHeight()
        {
            AVVideo video = mCurrent;
            if(video == null || video.info == null)
            {
                return 0;
            }
            return video.info.height;
        }

        public int getRotation()
        {
            AVVideo video = mCurrent;
            if(video == null || video.info == null)
            {
                return 0;
            }
            return video.info.videoRotation;
        }

        public AVFrameInfo getAsyncFrame()
        {
            AVVideo video = mCurrent;
            if(video == null)
            {
                return null;
            }
            return video.getAsyncFrame();
        }

        public boolean needRotate()
        {
            AVVideo video = mCurrent;
            if(video == null)
            {
                return false;
            }
            return video.needRotate();
        }

        public void start()
        {
            for(AVVideo v : mVideos) {
                v.start();
            }
        }

        public void stop()
        {
            for(AVVideo v : mVideos) {
                v.stop();
            }
        }

        public void release()
        {
            for(AVVideo v : mVideos) {
                v.release();
            }
            mVideos.clear();
            mInputs = null;
            mPrepared = false;
        }

        public void setEndPosition(long position)
        {
            mEndPosition = position;
        }

        public AVVideo getCurrent()
        {
            return mCurrent;
        }

        public int getCurrentIndex()
        {
            return mVideos.indexOf(mCurrent);
        }

        public long getDuration()
        {
            long duration = 0;
            for(AVVideo v : mVideos) {
                duration += v.info.duration;
            }
            return duration;
        }

        private int nextVideoFrame(Object buffer, AVFrameInfo info)
        {
            AVVideo video = mCurrent;
            if(video == null)
            {
                return EOF;
            }
            boolean ret = video.nextVideoFrame(buffer, info);
            info.pts += mPtsOffset;
            if(ret == false || (mEndPosition > 0 && info.pts >= mEndPosition))
            {
                info.data = null;
                video.videoEnd = true;
                if(video.isEnd() == false)
                {
                    return WAIT;
                }
                if(mEndPosition > 0 && info.pts >= mEndPosition)
                {
                    return EOF;
                }
                AVVideo next = nextVideo(true);
                if(next != null)
                {
                    return nextVideoFrame(buffer, info);
                }
                return EOF;
            }
            else
            {
                return OK;
            }
        }

        private int nextAudioFrame(Object buffer, AVFrameInfo info)
        {
            AVVideo video = mCurrent;
            if(video == null)
            {
                return EOF;
            }
            //视频读取第一帧有可能很慢，在这里同步一下
            if(video.frameReadCount == 0 && video.hasVideo())
            {
                int count = 0;
                while(video.frameReadCount == 0 && video.videoEnd == false)
                {
                    try {
                        Thread.sleep(2);
                    }
                    catch(Exception e)
                    {
                        break;
                    }
                    count++;
                    if(count > 5000)
                    {
                        break;
                    }
                }
            }
            boolean ret = video.nextAudioFrame(buffer, info);
            info.pts += mPtsOffset;
            if(ret == false || (mEndPosition > 0 && info.pts >= mEndPosition))
            {
                info.data = null;
                video.audioEnd = true;
                if(video.isEnd() == false)
                {
                    return WAIT;
                }
                if(mEndPosition > 0 && info.pts >= mEndPosition)
                {
                    return EOF;
                }
                AVVideo next = nextVideo(true);
                if(next != null)
                {
                    return nextAudioFrame(buffer, info);
                }
                return EOF;
            }
            else
            {
                return OK;
            }
        }

        private synchronized AVVideo nextVideo(boolean resetTheNext)
        {
            mPtsOffset = 0;
            AVVideo video = null;
            for(AVVideo v : mVideos) {
                if(v.isEnd() == false)
                {
                    video = v;
                    break;
                }
                mPtsOffset += v.info.duration;
            }
            if(mCurrent != video)
            {
                if(video != null) {
                    if(mCurrent != null && mOutput == false){
                        mCurrent.setSurface(null);
                        video.setSurface(mSurface);
                    }
                    mCurrent = video;
                    if(resetTheNext) {
                        video.reset();
                    }
                }
                if(mOnPlayStatusListener != null && video != null)
                {
                    Message msg = new Message();
                    msg.what = MESSAGE_VIDEOCHANGED;
                    mHandler.sendMessage(msg);
                }
            }
            return video;
        }
    }
}
