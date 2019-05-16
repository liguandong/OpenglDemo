package poco.cn.opengldemo.ratio2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import poco.cn.opengldemo.base.BlackMaskView;
import poco.cn.opengldemo.base.PlayRatio;
import poco.cn.opengldemo.utils.ShareData;

/**
 * Created by lgd on 2019/5/8.
 */
public class GlFrameView2 extends FrameLayout
{
    private GLSurfaceView glSurfaceView;
    private BlackMaskView mBlackMaskView;

    @PlayRatio
    private int mPlayRatio = PlayRatio.RATIO_1_1;
    private ImagePlayInfo2 imagePlayInfo;

    private FrameRender2 frameRender;


    private int mViewWidth = ShareData.m_screenWidth;
    private int mViewHeight = ShareData.m_screenWidth;

    private int mSurfaceLeft;
    private int mSurfaceTop;

    private float mLeftRatio;
    private float mTopRatio;

    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private float mShowRatio;
    private FrameNinePalacesView ninePalacesView;


    public GlFrameView2(@NonNull Context context)
    {
        this(context, null);
    }

    public GlFrameView2(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        LayoutParams fl = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        glSurfaceView = new GLSurfaceView(getContext());
        glSurfaceView.setEGLContextClientVersion(2);
        addView(glSurfaceView, fl);

        frameRender = new FrameRender2(getContext());
        glSurfaceView.setRenderer(frameRender);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mBlackMaskView = new BlackMaskView(getContext());
        mBlackMaskView.setColor(0xff0d0d0d);
        fl = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mBlackMaskView, fl);

        ninePalacesView = new FrameNinePalacesView(getContext());
        ninePalacesView.setVisibility(View.GONE);
        fl = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(ninePalacesView, fl);
        ninePalacesView.setOnViewDragListener(new FrameNinePalacesView.OnViewListener()
        {
            @Override
            public void onDrag(float dx, float dy)
            {
                //也可以用 mSurfaceWidth 和  mSurfaceHeight
                int min = Math.min(ninePalacesView.getRealWidth(),ninePalacesView.getRealHeight());
                imagePlayInfo.translate(dx / min, -dy/min);
                glSurfaceView.requestRender();
            }

            @Override
            public void onScaleChange(float scale, float focusX, float focusY)
            {
                //也可以用 mSurfaceWidth 和  mSurfaceHeight
                int min = Math.min(ninePalacesView.getRealWidth(),ninePalacesView.getRealHeight());
                focusX = (focusX - ninePalacesView.getWidth() / 2) / (float) min;
                focusY = (ninePalacesView.getHeight() / 2 - focusY) / (float) min;
                imagePlayInfo.scale(scale, focusX, focusY);
                glSurfaceView.requestRender();
            }

            @Override
            public void onScaleEnd()
            {

            }

            @Override
            public void onClick()
            {

            }

            @Override
            public void onUp()
            {

            }
        });

        enterFrameMode();


    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w == 0 || h == 0)
        {
            return;
        }
        if (w != mViewWidth || h != mViewHeight)
        {
            int ratio = mPlayRatio;
            mPlayRatio = -1;
            mViewWidth = w;
            mViewHeight = h;
            setPlayRatio(ratio);
        }
    }

    private Runnable mRefreshRunnable = new Runnable()
    {
        @Override
        public void run()
        {
//            frameRender.setMatrix(imagePlayInfo.modelMatrix, imagePlayInfo.texMatrix);
            glSurfaceView.requestRender();
        }
    };

    /**
     * 缩放播放画面至最小
     */
    public void scaleToMin()
    {
        imagePlayInfo.scaleToMin(mRefreshRunnable);
    }

    /**
     * 重置播放画面的缩放
     */
    public void resetScale()
    {

        imagePlayInfo.resetScale(mRefreshRunnable);
    }

    /**
     * 旋转播放画面
     *
     * @param right 是否向右旋转
     */
    public void rotateFrame(boolean right)
    {
        imagePlayInfo.rotate(right, mRefreshRunnable);
    }


    /**
     * 进入画幅页面
     */
    public void enterFrameMode()
    {
        ninePalacesView.setVisibility(View.VISIBLE);
        ObjectAnimator animator = ObjectAnimator.ofFloat(mBlackMaskView, "maskAlpha", 1, 0.6f);
        animator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
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
    public void exitFrameMode()
    {
        ninePalacesView.setVisibility(View.GONE);
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
        imagePlayInfo = new ImagePlayInfo2(bitmap.getWidth(), bitmap.getHeight());
        if (mPlayRatio != 0)
        {
            imagePlayInfo.setShowRatio2(getShowRatio());
            frameRender.setImageInfo(imagePlayInfo);
        }
        glSurfaceView.requestRender();
    }


    /**
     * 设置view的大小，必须一开始设置，默认为屏幕宽高
     *
     * @param width  view宽度
     * @param height view高度
     */
    public void setViewSize(int width, int height)
    {
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
                ninePalacesView.setPadding(0, mSurfaceTop, 0, mSurfaceTop);
                mBlackMaskView.startAnim(0, mSurfaceTop, 0, mSurfaceTop);
            } else if (mSurfaceLeft != 0)
            {
                ninePalacesView.setPadding(mSurfaceLeft, 0, mSurfaceLeft, 0);
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
                    frameRender.setLeftAndTop(showRatio, mSurfaceLeft, mLeftRatio, mSurfaceTop, mTopRatio);

                }
            });

            if (imagePlayInfo != null)
            {
                imagePlayInfo.setShowRatio2(showRatio);
//                imagePlayInfo.init(viewRatio, showRatio);
//                frameRender.setMatrix(imagePlayInfo.modelMatrix, imagePlayInfo.texMatrix);
            }
            glSurfaceView.requestRender();
        }
    }
}
