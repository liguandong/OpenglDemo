package poco.cn.opengldemo.ratio2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.opengl.Matrix;
import android.widget.ImageView;

/**
 * Created by lgd on 2019/5/10.
 * {@link poco.cn.opengldemo.ratio.ImagePlayInfo} 在旋转动画过程中变形了，原因是x和y轴不是同时乘以缩放因子
 * <p>
 * 要保持不变形缩放缩放时x轴，y轴要乘相同的缩放因子, 在乘缩放因子之前要计算好在视图对应缩放值
 * initScaleX,initScaleY,在后续的变化这两个值是不变的，即使旋转了
 * <p>
 * 1 . 在这里计算以[-1,-1]的规划坐标系为标准，不考虑viewRatio的比例，谨考虑showRatio和VideoRatio
 * 在{@link FrameRender2} 再计算showRatio和 viewRatio的影响
 * 2 . showRatio是[-1,1]的限定区域  videoRatio在showRatio区域FIX_CENTER的缩放值为1f
 * 3. 计算FIX_CENTER和CENTER_CROP的缩放值,初始化矩阵
 */
public class ImagePlayInfo2
{
    private final int width;
    private final int height;

    private static final int MAX_SCALE = 2;  //CENTER_CROP 基础下可以再放大
//    private final float showRatio;

    public ImageView.ScaleType scaleType = ImageView.ScaleType.CENTER_CROP;
    private final float initScaleX;  //  根据宽高比， 初始在[-1,1]FIXE_CENTER的对应值
    private final float initScaleY;  //根据宽高比， 初始在[-1,1]FIXE_CENTER的对应值
    private float curScale = 1f;  //  整体缩放值，在[-1,-1]的FIX_CENTER，初始化是1f

    private float curAngle;
    @Deprecated  //没考虑旋转
    private float maxScale = 1f;  // CENTER_CROP的缩放值，根据showRatio;
    @Deprecated  //没考虑旋转
    private float minScale = 1f;  // FIX_CENTER的缩放值，根据showRatio;

    private float transitionX;
    private float transitionY;


    public final float[] modelMatrix = new float[16];
    public final float[] texMatrix = new float[16];
    private boolean doingAnim;

    private float showRatio = 1f;
    public ImagePlayInfo2(int width, int height)
    {
//        this.showRatio = showRatio;
        this.width = width;
        this.height = height;
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(texMatrix, 0);
        final float videoRatio = getVideoRatio();
        float scaleX;
        float scaleY;
        if (videoRatio > 1f)
        {
            scaleX = 1f;
            scaleY = 1f / videoRatio;
        } else
        {
            scaleX = videoRatio;
            scaleY = 1f;
        }
        initScaleX = scaleX;
        initScaleY = scaleY;
        curScale = 1f;
        curAngle = 0f;
    }

    public float getMinScale(float angle)
    {
        float scale = 1f;
        if (showRatio > 1f)
        {
            if (angle % 180 == 0)
            {
                scale = Math.min(1 / initScaleX, 1 / showRatio / initScaleY);
            }else{
                scale = Math.min(1 / initScaleY, 1 / showRatio / initScaleX);
            }
        }else
        {
            if (angle % 180 == 0)
            {
                scale = Math.min(showRatio / initScaleX, 1 / initScaleY);
            }else{
                scale = Math.min(showRatio / initScaleY, 1 / initScaleX);
            }
        }
        return scale;
    }

    public float getMinScale()
    {
        return getMinScale(curAngle);
    }


    public float getMaxScale(float angle)
    {
        float scale = 1f;
        if (showRatio > 1f)
        {
            if (angle % 180 == 0)
            {
                scale = Math.max(1 / initScaleX, 1 / showRatio / initScaleY);
            }else{
                scale = Math.max(1 / initScaleY, 1 / showRatio / initScaleX);
            }
        }else
        {
            if (angle % 180 == 0)
            {
                scale = Math.max(showRatio / initScaleX, 1 / initScaleY);
            }else{
                scale = Math.max(showRatio / initScaleY, 1 / initScaleX);
            }
        }
        return scale;
    }

    public float getMaxScale()
    {
        return getMaxScale(curAngle);
    }

    public void setShowRatio2(float showRatio)
    {
        this.showRatio = showRatio;
        curScale = getMaxScale(curAngle);
        initMatrix();
    }

    //没有考虑旋转角度
    @Deprecated
    public void setShowRatio(float showRatio)
    {
        //自行画图,FIX_CENTER showRatio包住videoRatio，CENTER_CROP反之
        if (showRatio > 1f)
        {
            //  此时 initScaleX = 1f, initScaleY = 1f/ videoRatio， FIX_CENTER要保持其中一条长边, 可能两种情况长边scaleX = 1f 或者长边scaleY =  1/showRatio，取可最小缩放的值？因为有一条边长即可
            minScale = Math.min(1 / initScaleX, 1 / showRatio / initScaleY);
            // CENTER_CROP 要保持一条短边，可能scaleX = 1f 或者scaleY = 1/showRatio；CENTER_CROP要保持一条短边, 可能两种情短边scaleX = 1/showRatio 或者短边scaleY =  1f，取最大可缩放的值? why，当前状态一条短边，另一条肯定是长边
            maxScale = Math.max(1 / initScaleX, 1 / showRatio / initScaleY);
        } else
        {
            //  此时 initScaleX = 1f/ videoRatio, initScaleY = 1f
            minScale = Math.min(showRatio / initScaleX, 1 / initScaleY);
            maxScale = Math.max(showRatio / initScaleX, 1 / initScaleY);
        }
        curScale = maxScale;
        initMatrix();
    }

    private void initMatrix()
    {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(texMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, curAngle, 0f, 0f, 1f);
        Matrix.scaleM(modelMatrix, 0, initScaleX, initScaleY, 1f);
        Matrix.scaleM(modelMatrix, 0, curScale, curScale, 1f);
    }

    /**
     * @param right
     * @param refresh
     */
    public void rotate(boolean right,Runnable refresh)
    {
        if (doingAnim)
        {
            return;
        }
        float fromDegree = (curAngle + 360) % 360;
        float toDegree = (curAngle + (right ? -90 : 90) + 360) % 360;
        if (fromDegree == 0 && toDegree == 270)
        {
            fromDegree = 360;
        } else if (fromDegree == 270 && toDegree == 0)
        {
            fromDegree = -90;
        }
        doingAnim = true;
        float fromScale = curScale;
        float toScale = getMaxScale(toDegree);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1f);
        valueAnimator.setDuration(1000);
        float finalFromDegree = fromDegree;
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                float p = (float) animation.getAnimatedValue();
                curScale = (toScale - fromScale) * p + fromScale;
                curAngle = (int) ((toDegree - finalFromDegree) * p + finalFromDegree);
                initMatrix();
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
                curScale = scale;
                initMatrix();
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
        if (doingAnim || curScale == getMinScale())
        {
            return;
        }
        scaleAnim(curScale, getMinScale(), refresh);
    }

    public void resetScale(Runnable refresh)
    {
        if (doingAnim || curScale == getMaxScale())
        {
            return;
        }
        scaleAnim(curScale, getMaxScale(), refresh);
    }

    public float getVideoRatio()
    {
        int w = width;
        int h = height;
        return w / (float) h;
    }
}
