package poco.cn.opengldemo.ratio;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.RectF;
import android.opengl.Matrix;

/**
 * Created by lgd on 2019/5/7.
 */
public class ImagePlayInfo
{
    /**
     * 计算初始矩阵，初始化缩放值 CENTER_CROP模式
     * 最小缩放值 FIX_CENTER
     * 旋转，当前缩放，最大缩放，
     */


    private final int width;
    private final int height;

    private static final int MAX_SCALE = 2;

    public final float[] modelMatrix = new float[16];
    public final float[] texMatrix = new float[16];

    private float curScale = 1f;  //

    private int curAngle = 0;

    /**
     * 初始化的scale，旋转后会改变
     */
    private float initScaleX;
    private float initScaleY;

    /**
     * 最小的缩放比例，旋转后会改变
     */
    private float minScale;

    private RectF mRectF = new RectF();

    private boolean doingAnim = false;

    private final float[] mTempMatrix = new float[16];
//    private float modelScaleX;
//    private float modelScaleY;

    public ImagePlayInfo(int width, int height)
    {
        this.width = width;
        this.height = height;
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(texMatrix, 0);
    }

    /**
     * 根据比例计算,  画面铺满CENTER_CROP
     *
     * @param viewRatio
     * @param showRatio
     */
    public void init(float viewRatio, float showRatio)
    {
        curScale = 1f;
        curAngle = 0;
        initScale(viewRatio, showRatio);
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.scaleM(modelMatrix, 0, initScaleX, initScaleY, 1f);

//        modelScaleX = initScaleX;
//        modelScaleY = initScaleY;

        Matrix.setIdentityM(texMatrix, 0);
    }

    public float getVideoRatio(float showRatio) {
        int w = width;
        int h = height;
        if ((curAngle) % 180 != 0) {
            w = height;
            h = width;
        }

        final float videoRatio = w / (float)h;
        return Math.abs(showRatio - videoRatio) < 0.05f ? showRatio : videoRatio;
    }


    private void initScale(float viewRatio, float showRatio)
    {
        final float videoRatio = getVideoRatio(showRatio);
        float scaleX;
        float scaleY;

        //画图明朗，外框viewRatio，内框showRatio 和 videoRatio，  内框videoRatio画面需要缩放铺满showRatio内框
        if (viewRatio > showRatio)
        {
            if (videoRatio > showRatio)
            {
                //y不用缩放， x 按照videoRatio比例
                scaleX = videoRatio / viewRatio;
                scaleY = 1;
            } else
            {
                // x 缩放到 showRatio ， y 也跟随缩放比例
                scaleX = showRatio / viewRatio;
                scaleY = showRatio / videoRatio;
            }
        } else
        {
            if (videoRatio > showRatio)
            {
                //y缩放到1/showRatio，x也根据比例
                scaleX = videoRatio / showRatio;
                scaleY = viewRatio / showRatio;
            } else
            {
                //x不缩放， y 按照1/videoRatio缩放
                scaleX = 1;
                scaleY = viewRatio / videoRatio;
            }
        }

        initScaleX = scaleX;
        initScaleY = scaleY;

        final float minRatio = showRatio / viewRatio;
        //最小FIX CENTER,要保留一条长边， 假设minRatio = 1，x和y要保留一条长边，在x和y选择可缩放1的最小值
        if (minRatio > 1)
        {
            //根据比例, y可以继续缩放 1/ minRatio
            minScale = Math.min(1 / initScaleX, 1 / initScaleY / minRatio);
        } else
        {
            //根据比例, x可以继续缩放minRatio
            minScale = Math.min(minRatio / initScaleX, 1 / initScaleY);
        }

    }

    private void scaleAnim(float fromScale, float toScale, final Runnable refresh)
    {
        doingAnim = true;
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(fromScale, toScale);
        valueAnimator.setDuration(300);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                float scale = (float) animation.getAnimatedValue();
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.scaleM(modelMatrix, 0, initScaleX, initScaleY, 1f);
                Matrix.scaleM(modelMatrix, 0, scale, scale, 1f);
                if (curAngle != 0) {
                    Matrix.rotateM(modelMatrix,0,curAngle,0f, 0f, 1f);
//                    Matrix.setIdentityM(mTempMatrix, 0);
//                    Matrix.rotateM(mTempMatrix, 0, curAngle, 0, 0, 1);
//                    MatrixUtils.multiplyMM(modelMatrix, 0, modelMatrix, 0, mTempMatrix, 0);
                }
                curScale = scale;
                refresh.run();

            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                doingAnim = false;
            }
        });
        valueAnimator.start();
    }

    public void scaleToMin(Runnable refresh)
    {
        if (doingAnim || curScale == minScale)
        {
            return;
        }
        float fromScale = curScale;
        curScale = minScale;
        scaleAnim(fromScale, curScale, refresh);
    }

    public void resetScale(Runnable refresh)
    {
        if (doingAnim || curScale == 1f)
        {
            return;
        }

        float fromScale = curScale;
        curScale = 1;

        scaleAnim(fromScale, curScale,refresh);
    }

    /**
     *
     * @param right
     * @param viewRatio
     * @param showRatio
     * @param refresh
     */
    public void rotate(boolean right, float viewRatio, float showRatio, Runnable refresh)
    {
        if (doingAnim)
        {
            return;
        }
        float fromDegree = (curAngle + 360) % 360;
        curAngle = (curAngle + (right ? -90 : 90) + 360) % 360;

        float toDegree = curAngle;
        if (fromDegree == 0 && toDegree == 270) {
            fromDegree = 360;
        } else if (fromDegree == 270 && toDegree == 0) {
            fromDegree = -90;
        }

        // 旋转后, initScaleX和initScaleY都变化了， 不能像scaleAnim()那样变化
        float curScaleX = initScaleX * curScale;
        float curScaleY = initScaleY * curScale;

        initScale(viewRatio, showRatio);
        curScale = 1f;
        doingAnim = true;
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1f);
        valueAnimator.setDuration(3000);
        float finalFromDegree = fromDegree;
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                float p = (float) animation.getAnimatedValue();
                Matrix.setIdentityM(modelMatrix, 0);
                float scaleX = (initScaleX - curScaleX) * p + curScaleX;
                float scaleY = (initScaleY - curScaleY) * p + curScaleY;
                Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1f);
                int degree = (int) ((toDegree - finalFromDegree) * p + finalFromDegree);
                Matrix.rotateM(modelMatrix, 0,degree, 0f, 0f, 1f);

//                Matrix.setIdentityM(mTempMatrix,0);
//                Matrix.scaleM(mTempMatrix, 0, scaleX, scaleY, 1f);
//                Matrix.multiplyMM(modelMatrix,0,modelMatrix,0,mTempMatrix,0);


//                if (curAngle != 0) {
//
//                }
                refresh.run();

            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                doingAnim = false;
            }
        });
        valueAnimator.start();

    }


//    public void rotate(boolean right, float viewRatio, float showRatio, Runnable refresh)
//    {
//        if (doingAnim)
//        {
//            return;
//        }
//        float fromDegree = (curAngle + 360) % 360;
//        curAngle = (curAngle + (right ? -90 : 90) + 360) % 360;
//
//        float toDegree = curAngle;
//        if (fromDegree == 0 && toDegree == 270) {
//            fromDegree = 360;
//        } else if (fromDegree == 270 && toDegree == 0) {
//            fromDegree = -90;
//        }
//
//        // 旋转后, initScaleX和initScaleY都变化了， 不能像scaleAnim()那样变化
//        float curScaleX = initScaleX * curScale;
//        float curScaleY = initScaleY * curScale;
//        initScale(viewRatio, showRatio);
//        curScale = 1f;
//        doingAnim = true;
//        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1f);
//        valueAnimator.setDuration(3000);
//        float finalFromDegree = fromDegree;
//        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
//        {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation)
//            {
//                float p = (float) animation.getAnimatedValue();
//                Matrix.setIdentityM(modelMatrix, 0);
//                float scaleX = (initScaleX - curScaleX) * p + curScaleX;
//                float scaleY = (initScaleY - curScaleY) * p + curScaleY;
//                Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1f);
//                int degree = (int) ((toDegree - finalFromDegree) * p + finalFromDegree);
//                Matrix.rotateM(modelMatrix, 0,degree, 0f, 0f, 1f);
//
////                Matrix.setIdentityM(mTempMatrix,0);
////                Matrix.scaleM(mTempMatrix, 0, scaleX, scaleY, 1f);
////                Matrix.multiplyMM(modelMatrix,0,modelMatrix,0,mTempMatrix,0);
//
//
////                if (curAngle != 0) {
////
////                }
//                refresh.run();
//
//            }
//        });
//        valueAnimator.addListener(new AnimatorListenerAdapter()
//        {
//            @Override
//            public void onAnimationEnd(Animator animation)
//            {
//                doingAnim = false;
//            }
//        });
//        valueAnimator.start();
//
//    }
}
