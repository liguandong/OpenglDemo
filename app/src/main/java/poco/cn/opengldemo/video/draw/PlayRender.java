package poco.cn.opengldemo.video.draw;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import poco.cn.medialibs.glview.Renderer;
import poco.cn.medialibs.player2.GLMultiSurface;
import poco.cn.medialibs.player2.GLSurface;

/**
 * Created by lgd on 2019/4/19.
 */
public class PlayRender implements Renderer
{
    private final Context mContext;
    /**
     * Surface 的封装，传到播放器里面，是 GL Thread 和 Play Thread 通信的中介
     */
    @Nullable
    private GLMultiSurface mGLMultiSurface;

    @Nullable
    private RenderManager mRenderManager;

    /**
     * 播放视频的相关渲染信息，根据 video id 获取
     */
    @NonNull
    private SparseArray<RenderInfo> mRenderInfoArray = new SparseArray<>();
//    private SparseArray<RenderInfo> mRenderInfoArray = new SparseArray<>();


    /**
     * 临时矩阵，用于计算 model matrix
     */
    @NonNull
    private final float[] mModelMatrix = new float[16];

    /**
     * 临时矩阵，用于计算 texture matrix
     */
    @NonNull
    private final float[] mTexMatrix = new float[16];

    /**
     * 缩放矩阵
     */
    @NonNull
    private final float[] mScaleMatrix = new float[16];


    @Nullable
    private OnRenderListener mOnRenderListener;
    private boolean isRelease;

    /**
     * 整个 Surface 的大小
     */
    private int mViewWidth;
    private int mViewHeight;

    /**
     * 当前显示画面的大小
     */
    private int mShowWidth;
    private int mShowHeight;
    private int mLeftMargin;
    private int mTopMargin;
    private int mPlayRatio;
    private float mLeftRatio;
    private float mTopRatio;

    public PlayRender(@NonNull Context context)
    {
        super();
        mContext = context;
        Matrix.setIdentityM(mScaleMatrix, 0);
    }

    @Override
    public void onSurfaceCreated()
    {
        mGLMultiSurface = new GLMultiSurface(mOnSurfaceChangeListener);
        if (mOnRenderListener != null)
        {
            mOnRenderListener.onSurfaceCreated(mGLMultiSurface);
        }
        if (mRenderManager != null)
        {
            mRenderManager.release();
        }
        mRenderManager = new RenderManager(mContext);

    }


    @Override
    public void onSurfaceChanged(int width, int height)
    {
        mViewWidth = width;
        mViewHeight = height;

        mShowWidth = mViewWidth - 2 * mLeftMargin;
        mShowHeight = mViewHeight - 2 * mTopMargin;

//        mLeftMargin = mViewWidth/10;
//        mTopMargin = mViewHeight/10;
//        mShowWidth = mViewWidth - 2 * mLeftMargin;
//        mShowHeight = mViewHeight - 2 * mTopMargin;

        mRenderManager.setSurfaceSize(mShowWidth, mShowHeight);

        // 初始化转场用的 FrameBuffer 的大小
        AbsTransition.initBuffer(mShowWidth, mShowHeight);

//        Matrix.setIdentityM(mScaleMatrix, 0);


        if (mOnRenderListener != null)
        {
            mOnRenderListener.onSurfaceChanged(width, height);
        }
    }

    @Override
    public void onDrawFrame()
    {
        if (isRelease)
        {
            return;
        }
        final GLMultiSurface glMultiSurface = mGLMultiSurface;
        final RenderManager renderManager = mRenderManager;
        if (glMultiSurface == null || renderManager == null)
        {
            return;
        }
        glMultiSurface.updateTexImage();

//        mShowWidth = mViewWidth;
//        mShowHeight = mViewHeight;
        GLES20.glViewport(0, 0, mShowWidth, mShowHeight);
        GLES20.glClearColor(0.4f, 0, 0, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        final GLSurface curSurface = glMultiSurface.getCurSurface();
        final int curId = curSurface.getId();
        if (curId < 0)
        { // 当前视频 id 不正确
            return;
        }

        final RenderInfo curRenderInfo = mRenderInfoArray.get(curId);
        if (curRenderInfo == null)
        {
            return;
        }

        int curTextureId;
        RenderInfo renderInfo = curRenderInfo;
        long timestamp = curSurface.getTimestamp();
        long duration = curSurface.getVideoDuration();

        Matrix.multiplyMM(mTexMatrix, 0, curRenderInfo.texMatrix, 0, curSurface.getTransformMatrix(), 0);

        //左右缩放
        Matrix.multiplyMM(mModelMatrix, 0, mScaleMatrix, 0, curRenderInfo.modelMatrix, 0);

        curTextureId = mRenderManager.drawFrame(curSurface.getTextureId(), curSurface.wrapFrameMatrix(mModelMatrix), mTexMatrix);

        MaskExpandTransition maskExpandTransition = new MaskExpandTransition(mContext);
        float f = (2000 - timestamp) * 1.0f / 2000;
        maskExpandTransition.setProgress(f);

//        mRenderManager.drawTransition(maskExpandTransition,curTextureId,mLeftMargin, mTopMargin, mShowWidth, mShowHeight);

        mRenderManager.drawTransition(maskExpandTransition, curTextureId, -1, mLeftMargin, mTopMargin, mShowWidth, mShowHeight);

//        mRenderManager.drawTexture(curTextureId);

        mRenderManager.drawWater();

//        renderManager.drawTransition(transition, curTextureId, nextTextureId,
//                mLeftMargin, mTopMargin, mShowWidth, mShowHeight);

    }

    /**
     * 设置播放视频的相关渲染信息
     */
    public void setRenderInfoArray(@NonNull SparseArray<RenderInfo> infoArray)
    {
        mRenderInfoArray = infoArray;
    }


    @Override
    public void onSurfaceDestroyed()
    {
        isRelease = true;
        if (mOnRenderListener != null)
        {
            mOnRenderListener.onSurfaceDestroyed();
        }
    }

    private GLMultiSurface.OnSurfaceChangeListener mOnSurfaceChangeListener = new GLMultiSurface.OnSurfaceChangeListener()
    {
        @Override
        public void onChange(boolean isTransitionMode)
        {
            // 交换 Surface 信息时的回调
//            isDrawNext = isTransitionMode;
//            isFinish = isTransitionMode && TransitionItem.isBlendTransition(mTransitionId);
        }
    };

    public void setOnRenderListener(@Nullable OnRenderListener listener)
    {
        mOnRenderListener = listener;
    }

    public void release()
    {
        isRelease = true;

//        mFilterParamArray.clear();
//        mAdjustInfoArray.clear();
//        mCurveArray.clear();
        mRenderInfoArray.clear();

    }

    /**
     * 更换画幅时调用，用于更新 Surface 显示的信息
     *
     * @param playRatio  视频画幅
     * @param leftMargin 显示 Surface 距离左边的距离
     * @param leftRatio  占整个 Surface 大小的比例
     * @param topMargin  显示 Surface 距离上面的距离
     * @param topRatio   占整个 Surface 大小的比例
     */
    public void setLeftAndTop(int playRatio, int leftMargin, float leftRatio, int topMargin, float topRatio)
    {
        mPlayRatio = playRatio;
        mLeftMargin = leftMargin;
        mTopMargin = topMargin;
        mLeftRatio = Math.abs(leftRatio);
        mTopRatio = Math.abs(topRatio);

        // 计算缩放矩阵，和计算保存视频时的 model matrix 一样的计算方式

        //为什么要缩放， 矩阵本来是在glview(0,0,viewW,viewH)画， 结果在glview(leftMargin,topMargin,showW,showH)画，要缩放
        float scaleX = 1;
        float scaleY = 1;
        if (mLeftRatio != 0) {
            scaleX = 1 / Math.abs(mLeftRatio);
        }
        if (mTopRatio != 0) {
            scaleY = 1 / Math.abs(mTopRatio);
        }
        Matrix.setIdentityM(mScaleMatrix, 0);
        Matrix.scaleM(mScaleMatrix, 0, scaleX, scaleY, 1);

        mShowWidth = mViewWidth - 2 * mLeftMargin;
        mShowHeight = mViewHeight - 2 * mTopMargin;

        final RenderManager manager = mRenderManager;
        if (manager != null) {
            // 更新 FrameBuffer 的大小
            manager.setSurfaceSize(mShowWidth, mShowHeight);
        }
        // 重新初始化转场用的 FrameBuffer 的大小
        AbsTransition.initBuffer(mShowWidth, mShowHeight);

    }

    public interface OnRenderListener
    {

        void onSurfaceCreated(@NonNull GLMultiSurface glMultiSurface);

        void onSurfaceChanged(int width, int height);

        void onSurfaceDestroyed();
    }

}
