package poco.cn.opengldemo.video.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import poco.cn.medialibs.player2.GLMultiSurface;
import poco.cn.medialibs.player2.MultiPlayer;
import poco.cn.medialibs.player2.MultiSurface;
import poco.cn.medialibs.player2.OnPlayListener;
import poco.cn.medialibs.player2.PlayInfo;
import poco.cn.opengldemo.base.BlackMaskView;
import poco.cn.opengldemo.base.PlayRatio;
import poco.cn.opengldemo.utils.ShareData;

/**
 * Created by lgd on 2019/4/18.
 * <p>
 * 设置宽高，显示比例， 根据宽高比和显示比 计算 surface宽高
 * 设置路径， 初始化 opengl 缩放矩阵  {@link VideoPlayInfo}
 */
public class GLVideoViewV2 extends FrameLayout
{
    private static final int STATE_IDLE = 1;
    private static final int STATE_START = 2;
    private static final int STATE_PAUSE = 3;
    private static final int STATE_RELEASE = 4;

    @NonNull
    private final Context mContext;
    private final Handler mHandler;

    @NonNull
    private final MultiPlayer mMultiPlayer;
    private RenderView mRenderView;

    private int mState = STATE_IDLE;

    private int mViewWidth = ShareData.m_screenWidth;
    private int mViewHeight = ShareData.m_screenWidth;

    /**
     * 所有视频数据信息（控制视频渲染）
     */
    @NonNull
    private final List<VideoPlayInfo> mVideoPlayInfos = new ArrayList<>();

    /**
     * 播放的视频数据信息（控制视频播放）
     */
    @NonNull
    private final List<PlayInfo> mPlayInfos = new ArrayList<>();

    private BlackMaskView mBlackMaskView;

    /**
     * 显示画幅
     */
    @PlayRatio
    private int mPlayRatio = 0;
    private boolean playing;
    private float mShowRatio;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mSurfaceLeft;
    private int mSurfaceTop;
    private boolean hasSurface;
    private boolean mPendingStart;
    private int mStartIndex;
    private boolean mPendingPause;
    private int mSeekIndex;
    private long mSeekPosition;
    private float mLeftRatio;
    private float mTopRatio;

    public GLVideoViewV2(@NonNull Context context)
    {
        this(context, null);
    }

    public GLVideoViewV2(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mMultiPlayer = new MultiPlayer(context);
        initViews();
    }

    private void initViews()
    {
        mRenderView = new RenderView(mContext);
        mRenderView.setOnSurfaceListener(mOnSurfaceListener);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mRenderView, params);

        mMultiPlayer.setOnPlayListener(mOnPlayListener);


        mBlackMaskView = new BlackMaskView(mContext);
        mBlackMaskView.setColor(0x4f0d0d0d);
        params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mBlackMaskView, params);

        setPlayRatio(PlayRatio.RATIO_9_16);
    }

    /**
     * 设置播放的视频信息
     */
    public void setVideoInfos(VideoBaseInfo... videoInfos)
    {
        if (mState != STATE_IDLE)
        {
            return;
        }
        if (videoInfos == null || videoInfos.length == 0)
        {
            return;
        }
        mVideoPlayInfos.clear();
        mPlayInfos.clear();
        VideoPlayInfo videoPlayInfo;
        for (VideoBaseInfo info : videoInfos)
        {
            videoPlayInfo = new VideoPlayInfo(info);
            mVideoPlayInfos.add(videoPlayInfo);
            mPlayInfos.add(PlayInfo.getPlayInfo(info.path, (int) info.duration, videoPlayInfo.id));
        }
        if (mPlayRatio != 0)
        {
            initVideoPlayMatrix();
        }
    }

    /**
     * 设置画幅比例
     */
    public void setPlayRatio(int playRatio)
    {
        if (mPlayRatio != playRatio)
        {
            mPlayRatio = playRatio;

            float showRatio;

            switch (playRatio)
            {
                case PlayRatio.RATIO_1_1:
                    showRatio = 1;
                    break;
                case PlayRatio.RATIO_9_16:
                    showRatio = 9f / 16;
                    break;
                case PlayRatio.RATIO_16_9:
                    showRatio = 16f / 9;
                    break;
                case PlayRatio.RATIO_235_1:
                    showRatio = 2.35f;
                    break;
                case PlayRatio.RATIO_3_4:
                    showRatio = 3f / 4;
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            final int viewWidth = mViewWidth;
            final int viewHeight = mViewHeight;
            final float viewRatio = viewWidth / (float) viewHeight;
            int surfaceWidth;
            int surfaceHeight;
            if (viewRatio > showRatio)
            {
                surfaceHeight = viewHeight;
                surfaceWidth = (int) (surfaceHeight * showRatio + 0.5f);
            } else
            {
                surfaceWidth = viewWidth;
                surfaceHeight = (int) (surfaceWidth / showRatio + 0.5f);
            }
            mShowRatio = showRatio;

            mSurfaceWidth = surfaceWidth;
            mSurfaceHeight = surfaceHeight;

            int delta = viewWidth - surfaceWidth;
            delta += delta & 1; //取偶
            mSurfaceLeft = delta / 2;

            delta = viewHeight - surfaceHeight;
            delta += delta & 1;
            mSurfaceTop = delta / 2;

            if (mSurfaceTop != 0)
            {
                mBlackMaskView.startAnim(0, mSurfaceTop, 0, mSurfaceTop);
            } else if (mSurfaceLeft != 0)
            {
                mBlackMaskView.startAnim(mSurfaceLeft, 0, mSurfaceLeft, 0);
            } else
            {
                mBlackMaskView.startAnim(0, 0, 0, 0);
            }
            float halfSize = viewWidth / 2;
            mLeftRatio = (mSurfaceLeft - halfSize) / halfSize;
            halfSize = viewHeight / 2;
            mTopRatio = (halfSize - mSurfaceTop) / halfSize;
            mRenderView.setLeftAndTop(playRatio, mSurfaceLeft, mLeftRatio, mSurfaceTop, mTopRatio);


//            mSurfaceLeft


            if (!mPlayInfos.isEmpty())
            {
                initVideoPlayMatrix();
            }

            shouldRequestRender();

        }
    }

    private void shouldRequestRender()
    {
        if (mState == STATE_PAUSE)
        {
            mRenderView.requestRender();
        }
    }

    private void initVideoPlayMatrix()
    {
        for (VideoPlayInfo info : mVideoPlayInfos)
        {
            final float viewRatio = mViewWidth / (float) mViewHeight;
            info.init(viewRatio, mShowRatio);
        }
        mRenderView.setRenderInfoArray(mVideoPlayInfos, mPlayInfos);
    }

    private OnPlayListener mOnPlayListener = new OnPlayListener()
    {
        @Override
        public void onStart(int index)
        {
            super.onStart(index);
        }
    };

    private RenderView.OnSurfaceListener mOnSurfaceListener = new RenderView.OnSurfaceListener()
    {
        @Override
        public void onSurfaceCreated(@NonNull GLMultiSurface glMultiSurface)
        {
            if (mState == STATE_RELEASE)
            {
                return;
            }
            mMultiPlayer.setSurface(MultiSurface.create(mRenderView, glMultiSurface));
            hasSurface = true;
            if (mPendingStart)
            {
                mPendingStart = false;
                startImpl();
            }
        }

        @Override
        public void onSurfaceChanged(int width, int height)
        {

        }

        @Override
        public void onSurfaceDestroyed()
        {
            hasSurface = false;
            if (mState != STATE_RELEASE)
            {
                mMultiPlayer.reset();
                mState = STATE_IDLE;
            }
        }
    };

    public void release()
    {
        mState = STATE_RELEASE;
        mMultiPlayer.release();
        mVideoPlayInfos.clear();
        mMultiPlayer.setOnPlayListener(mOnPlayListener);
        mMultiPlayer.release();
        if (mRenderView != null)
        {
            mRenderView.release();
            mRenderView = null;
        }
    }


    public void start()
    {
        if (mState == STATE_PAUSE)
        {
            resume();
            return;
        }
        if (mState != STATE_IDLE)
        {
            return;
        }
        if (mVideoPlayInfos.isEmpty())
        {
            return;
        }

        if (hasSurface)
        {
            startImpl();
        } else
        {
            mPendingStart = true;
        }
    }

    private void startImpl()
    {
        final int startIndex = mStartIndex;
        mStartIndex = 0;
        mMultiPlayer.setPlayInfos(mPlayInfos.toArray(new PlayInfo[0]));
        mMultiPlayer.prepare(startIndex);
        mMultiPlayer.start();
        mState = STATE_START;
//        if (mPendingPause)
//        {
//            pause();
//            //防止黑屏?
//            seekTo(mSeekIndex, mSeekPosition);
//        }
    }

//    public void seekTo(int index, long position)
//    {
//
//    }


    public boolean isPlaying()
    {
        return mState == STATE_START && !mPendingPause;
    }

    public void setPlaying(boolean playing)
    {
        this.playing = playing;
    }

    public void pause()
    {
        if (mState != STATE_START)
        {
            return;
        }
        mPendingPause = false;
        mMultiPlayer.pause();
        mState = STATE_PAUSE;

    }

    public void resume()
    {
        if (mState != STATE_PAUSE)
        {
            return;
        }
        mMultiPlayer.start();
        mState = STATE_START;
    }
}
