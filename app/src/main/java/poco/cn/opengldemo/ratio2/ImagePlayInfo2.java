package poco.cn.opengldemo.ratio2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.RectF;
import android.opengl.Matrix;

/**
 * Created by lgd on 2019/5/10.
 * {@link poco.cn.opengldemo.ratio.ImagePlayInfo} 在旋转动画过程中变形了，原因是x和y轴不是同时乘以缩放因子
 * <p>
 * 要保持不变形缩放缩放时x轴，y轴要乘相同的缩放因子, 在乘缩放因子之前要计算好在视图对应缩放值
 * initScaleX,initScaleY,在后续的变化这两个值是不变的，即使旋转了
 * <p>
 * 当前视区x轴[-1,1],y轴[-1,-1];
 * 画幅是在视区的限制区域
 */
public class ImagePlayInfo2
{
    private final int width;
    private final int height;

    private static final int MAX_SCALE = 4;  //CENTER_CROP 基础下可以再放大
//    private final float showRatio;

    private final float initScaleX;  //  根据宽高比， 初始在[-1,1]FIXE_CENTER的对应值
    private final float initScaleY;  //根据宽高比， 初始在[-1,1]FIXE_CENTER的对应值
    private float curScale = 1f;  //  整体缩放值，在[-1,-1]的FIX_CENTER，初始化是1f

    private float curAngle;
    @Deprecated  //没考虑旋转
    private float maxScale = 1f;  // CENTER_CROP的缩放值，根据showRatio;
    @Deprecated  //没考虑旋转
    private float minScale = 1f;  // FIX_CENTER的缩放值，根据showRatio;

    private float transitionX; //纹理中心点的x位移值
    private float transitionY; //纹理中心点的y位移值


    public final float[] modelMatrix = new float[16];
    public final float[] texMatrix = new float[16];
    private boolean doingAnim;

    private float showRatio = 1f; //画幅比例

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

    /**
     * FIX_CENRER的缩放值，
     * FIX_CENTER要保持其中一条长边, 可能两种情况长边scaleX = 1f 或者长边scaleY =  1/showRatio，取可最小缩放的值,因为有一条边长即可
     *
     * @param angle
     * @return
     */
    public float getMinScale(float angle)
    {
        float scale = 1f;
        if (showRatio > 1f)
        {
            if (angle % 180 == 0)
            {
                scale = Math.min(1 / initScaleX, 1 / showRatio / initScaleY);
            } else
            {
                scale = Math.min(1 / initScaleY, 1 / showRatio / initScaleX);
            }
        } else
        {
            if (angle % 180 == 0)
            {
                scale = Math.min(showRatio / initScaleX, 1 / initScaleY);
            } else
            {
                scale = Math.min(showRatio / initScaleY, 1 / initScaleX);
            }
        }
        return scale;
    }

    public float getMinScale()
    {
        return getMinScale(curAngle);
    }

    /**
     * CENTER_CROP的缩放值
     * CENTER_CROP 要保持一条短边，可能scaleX = 1f 或者scaleY = 1/showRatio；CENTER_CROP要保持一条短边,
     * 可能两种情短边scaleX = 1/showRatio 或者短边scaleY =  1f，取最大可缩放的值? why，当前状态一条短边，另一条肯定是长边
     *
     * @param angle
     * @return
     */
    public float getMaxScale(float angle)
    {
        float scale = 1f;
        if (showRatio > 1f)
        {
            if (angle % 180 == 0)
            {
                scale = Math.max(1 / initScaleX, 1 / showRatio / initScaleY);
            } else
            {
                scale = Math.max(1 / initScaleY, 1 / showRatio / initScaleX);
            }
        } else
        {
            if (angle % 180 == 0)
            {
                scale = Math.max(showRatio / initScaleX, 1 / initScaleY);
            } else
            {
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
        transitionX = 0;
        transitionY = 0;
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

    //https://blog.csdn.net/faithmy509/article/details/82705368
    private void initMatrix()
    {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(texMatrix, 0);
        //根据前乘关系和opengl坐标系模式， Matrix.先写的后执行
        Matrix.translateM(modelMatrix, 0, transitionX, transitionY, 0f);
        Matrix.rotateM(modelMatrix, 0, curAngle, 0f, 0f, 1f);
        Matrix.scaleM(modelMatrix, 0, curScale * initScaleX, curScale * initScaleY, 1f);
    }

    /**
     * @param right
     * @param refresh
     */
    public void rotate(boolean right, Runnable refresh)
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
        valueAnimator.setDuration(300);
        float fromTransitionX = transitionX;
        float fromTransitionY = transitionY;
        float finalFromDegree = fromDegree;
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                float f = (float) animation.getAnimatedValue();
                curScale = (toScale - fromScale) * f + fromScale;
                curAngle = (int) ((toDegree - finalFromDegree) * f + finalFromDegree);

                transitionX = (1 - f ) * fromTransitionX;
                transitionY = (1 - f ) * fromTransitionY;
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
        float fromTransitionX = transitionX;
        float fromTransitionY = transitionY;
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(300);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                float f = (float) animation.getAnimatedValue();
                curScale = (toScale - fromScale) * f + fromScale;
                transitionX = (1 - f ) * fromTransitionX;
                transitionY = (1 - f ) * fromTransitionY;
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

    /**
     * 纹理的显示框不允许超过showRatio框, 若小于showRatio框则居中
     */
    private void checkBound()
    {
        float x, y;
        if (showRatio > 1)
        {
            x = 1f;
            y = 1 / showRatio;
        } else
        {
            y = 1f;
            x = showRatio;
        }
        RectF showRect = new RectF();
        showRect.set(-x, y, x, -y);

        x = (curAngle % 180 == 0 ? initScaleX : initScaleY) * curScale;
        y = (curAngle % 180 == 0 ? initScaleY : initScaleX) * curScale;
        RectF curRect = new RectF();
        curRect.set(-x, y, x, -y);

        if (curRect.width() < showRect.width())
        {
            transitionX = 0;
        } else if (curRect.left + transitionX > showRect.left)
        {
            transitionX = showRect.left - curRect.left;
        } else if (curRect.right + transitionX < showRect.right)
        {
            transitionX = showRect.right - curRect.right;
        }
        // opengl 和android y轴翻转
        if (-curRect.height() < -showRect.height())
        {
            transitionY = 0;
        } else if (curRect.bottom + transitionY > showRect.bottom)
        {
            transitionY = showRect.bottom - curRect.bottom;
        } else if (curRect.top + transitionY < showRect.top)
        {
            transitionY = showRect.top - curRect.top;
        }
    }

    /**
     * 为什么在缩放的情况下也保持对应的滑动值?
     * 因为最后的矩阵先缩放后旋转后平移，平移不受缩放值影响
     *
     * @param x
     * @param y
     */
    public void translate(float x, float y)
    {
        transitionX += x;
        transitionY += y;
        checkBound();
        initMatrix();
    }

    /**
     * 基准点缩放，首先对自身缩放值缩放， 然后对象的中心点会根据缩放比向基准点靠近或者远离
     * 放大会远离，缩小会靠近
     * <p>
     * focusX * scale    换算成纹理缩放计算实际的位置
     *
     * @param scale
     * @param focusX
     * @param focusY
     */
    public void scale(float scale, float focusX, float focusY)
    {
        // 基准点缩放，
        curScale *= scale;
        float min = getMinScale();
        float max = getMaxScale();
        transitionX += (focusX * scale - transitionX) * (1 - scale);
        transitionY += (focusY * scale - transitionY) * (1 - scale);
        if (curScale < min)
        {
            curScale = min;
        }
        if (curScale > max * MAX_SCALE)
        {
            curScale = max * MAX_SCALE;
        }
        checkBound();
        initMatrix();
    }
}
