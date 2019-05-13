package poco.cn.opengldemo.ratio;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import poco.cn.opengldemo.utils.ShareData;
import poco.cn.opengldemo.base.BlackMaskView;
import poco.cn.opengldemo.base.PlayRatio;

/**
 * Created by lgd on 2019/5/8.
 *
 *
 *
 */
public class GlFrameView extends FrameLayout
{
    private GLSurfaceView glSurfaceView;
    private BlackMaskView mBlackMaskView;

    @PlayRatio
    private int mPlayRatio = PlayRatio.RATIO_1_1;
    private ImagePlayInfo imagePlayInfo;

    private FrameRender frameRender;


    private int mViewWidth = ShareData.m_screenWidth;
    private int mViewHeight = ShareData.m_screenWidth;

    private int mSurfaceLeft;
    private int mSurfaceTop;

    private float mLeftRatio;
    private float mTopRatio;

    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private float mShowRatio;


    public GlFrameView(@NonNull Context context)
    {
        this(context, null);
    }

    public GlFrameView(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        LayoutParams fl = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        glSurfaceView = new GLSurfaceView(getContext());
        glSurfaceView.setEGLContextClientVersion(2);
        addView(glSurfaceView, fl);

        frameRender = new FrameRender(getContext());
        glSurfaceView.setRenderer(frameRender);

        mBlackMaskView = new BlackMaskView(getContext());
        mBlackMaskView.setColor(0xff0d0d0d);
        fl = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mBlackMaskView, fl);

        enterFrameMode();
    }

    private Runnable mRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            frameRender.setMatrix(imagePlayInfo.modelMatrix, imagePlayInfo.texMatrix);
            glSurfaceView.requestRender();
        }
    };

    /**
     * 缩放播放画面至最小
     */
    public void scaleToMin() {
        imagePlayInfo.scaleToMin( mRefreshRunnable);
    }

    /**
     * 重置播放画面的缩放
     */
    public void resetScale() {

        imagePlayInfo.resetScale(mRefreshRunnable);
    }

    /**
     * 旋转播放画面
     * @param right 是否向右旋转
     */
    public void rotateFrame(boolean right) {

        final float viewRatio = mViewWidth / (float)mViewHeight;
        imagePlayInfo.rotate(right, viewRatio, mShowRatio, mRefreshRunnable);
    }



    /**
     * 进入画幅页面
     */
    public void enterFrameMode() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(mBlackMaskView, "maskAlpha", 1, 0.6f);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mBlackMaskView.setColor(0xff1a1a1a);
                mBlackMaskView.setMaskAlpha(0.6f);
            }
        });
        animator.setDuration(300);
        animator.start();
        glSurfaceView.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                frameRender.setFullMode(true);
            }
        });
    }

    /**
     * 退出画幅页面
     */
    public void exitFrameMode() {
        mBlackMaskView.setColor(0xff0d0d0d);
        mBlackMaskView.setMaskAlpha(0.6f);
        ObjectAnimator animator = ObjectAnimator.ofFloat(mBlackMaskView, "maskAlpha", 0.6f, 1f);
        animator.setDuration(300);
        animator.start();
        glSurfaceView.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                frameRender.setFullMode(false);
            }
        });
    }

    public void setBitmap(Bitmap bitmap)
    {
//        imagePlayInfo
        frameRender.setBitmap(bitmap);
        imagePlayInfo = new ImagePlayInfo(bitmap.getWidth(), bitmap.getHeight());
        if (mPlayRatio != 0)
        {
            float vRatio = mViewWidth * 1.0f / mViewHeight;
            imagePlayInfo.init(vRatio, getShowRatio());
            frameRender.setMatrix(imagePlayInfo.modelMatrix, imagePlayInfo.texMatrix);
        }
        glSurfaceView.requestRender();
    }


    /**
     * 设置view的大小，必须一开始设置，默认为屏幕宽高
     *
     * @param width  view宽度
     * @param height view高度
     */
    public void setViewSize(int width, int height) {
        mViewWidth = width;
        mViewHeight = height;
    }

    private float getShowRatio()
    {

        float showRatio;
        switch (mPlayRatio)
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
        return showRatio;
    }

    /**
     * 设置画幅比例
     */
    public void setPlayRatio(int playRatio)
    {
        if (mPlayRatio != playRatio)
        {
            mPlayRatio = playRatio;

            float showRatio = getShowRatio();

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
            delta += delta & 1;
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

            float halfSize = viewWidth / 2f;
            mLeftRatio = (mSurfaceLeft - halfSize) / halfSize;
            halfSize = viewHeight / 2f;
            mTopRatio = (halfSize - mSurfaceTop) / halfSize;
            glSurfaceView.queueEvent(new Runnable()
            {
                @Override
                public void run()
                {
                    frameRender.setLeftAndTop(playRatio, mSurfaceLeft, mLeftRatio, mSurfaceTop, mTopRatio);

                }
            });

            if (imagePlayInfo != null)
            {
                imagePlayInfo.init(viewRatio, showRatio);
                frameRender.setMatrix(imagePlayInfo.modelMatrix, imagePlayInfo.texMatrix);
            }
            glSurfaceView.requestRender();
        }
    }
}
