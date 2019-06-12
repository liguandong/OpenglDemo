package poco.cn.opengldemo.video.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.RectF;
import android.opengl.Matrix;

import androidx.annotation.NonNull;
import poco.cn.medialibs.player2.PlayInfo;
import poco.cn.medialibs.save.player.MatrixUtils;
import poco.cn.medialibs.save.player.SaveVideoInfo;
import poco.cn.opengldemo.video.draw.RenderInfo;

/**
 * Created by lgd on 2019/4/24.
 * 控制视频渲染
 */
public class VideoPlayInfo
{

    private static final int MAX_SCALE = 2;

    private static int sVideoId = 0;

    public final int id;
    public final VideoBaseInfo data;

    public final float[] modelMatrix = new float[16];
    public final float[] texMatrix = new float[16];

    private float curScale = 1f;

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

    @NonNull
//    public CurveEffect.Params curve;

    private RectF mRectF = new RectF();

    private boolean doingAnim = false;

    public VideoPlayInfo(VideoBaseInfo videoInfo)
    {
        this(sVideoId++, videoInfo);
    }

    private VideoPlayInfo(int id, @NonNull VideoBaseInfo videoInfo)
    {
        this.id = id;

        data = videoInfo.Clone();

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(texMatrix, 0);

//        curve = new CurveEffect.Params();
    }

    public void init(float viewRatio, float showRatio)
    {
        curScale = 1;
        curAngle = 0;
        initScale(viewRatio, showRatio);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.scaleM(modelMatrix, 0, initScaleX, initScaleY, 1);

        modelScaleX = initScaleX;
        modelScaleY = initScaleY;

        Matrix.setIdentityM(texMatrix, 0);
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

    public void translate(float dx, float dy, float left, float top)
    {

        if (doingAnim)
        {
            return;
        }

        dy = -dy;

        float bottom = -top;
        float right = -left;

        mapRect();

        if (mRectF.right - mRectF.left >= 2 * right)
        {
            if (mRectF.left + dx > left)
            {
                dx = left - mRectF.left;
            }

            if (mRectF.right + dx < right)
            {
                dx = right - mRectF.right;
            }
        } else
        {
            dx = 0;
        }


        if (mRectF.top - mRectF.bottom >= 2 * top)
        {
            if (mRectF.top + dy < top)
            {
                dy = top - mRectF.top;
            }

            if (mRectF.bottom + dy > bottom)
            {
                dy = bottom - mRectF.bottom;
            }
        } else
        {
            dy = 0;
        }


        if (dx != 0 || dy != 0)
        {
            MatrixUtils.translateM(modelMatrix, 0, dx, dy, 0);
        }
    }

    /**
     * 缩放
     */
    public void scale(float scale, float left, float top)
    {

        if (doingAnim)
        {
            return;
        }

        float right = -left;
        float bottom = -top;

        if (curScale * scale > MAX_SCALE)
        {
            scale = MAX_SCALE / curScale;
        } else if (curScale * scale < minScale)
        {
            scale = minScale / curScale;
        }

//		MatrixUtils.scaleM(modelMatrix, 0, scale, scale, 1);
        Matrix.scaleM(modelMatrix, 0, scale, scale, 1);
        modelScaleX *= scale;
        modelScaleY *= scale;

        curScale *= scale;

//		if (scale < 1) {
//			checkBounds(left, top, right, bottom);
//		}

        checkFitCenter(left, top, right, bottom);
    }

    /**
     * 双击缩放
     */
    public void doubleScale(float left, float top, Runnable refresh)
    {
        if (doingAnim)
        {
            return;
        }

        float fromScale;
        if (curScale < 1)
        {
            fromScale = curScale;
            curScale = 1;
        } else if (curScale == MAX_SCALE)
        {
            fromScale = MAX_SCALE;
            curScale = 1;
        } else
        {
            fromScale = curScale;
            curScale = MAX_SCALE;
        }

        scaleAnim(fromScale, curScale, left, top, refresh);
    }

    /**
     * 缩放结束
     */
    public void scaleFinish(float left, float top, Runnable refresh)
    {

        float right = -left;
        float bottom = -top;

        float dx = 0, dy = 0;
        mapRect();
        final float showWidth = right * 2;
        final float showHeight = top * 2;

        float width = mRectF.right - mRectF.left;
        float dx1 = mRectF.left - left;
        float dx2 = mRectF.right - right;
        if (width <= showWidth)
        {
            dx = -(dx1 + dx2) / 2;
        } else
        {
            if (dx1 > 0 && dx2 > 0)
            {
                dx = -Math.min(dx1, dx2);
            } else if (dx1 < 0 && dx2 < 0)
            {
                dx = -Math.max(dx1, dx2);
            }
        }

        float height = mRectF.top - mRectF.bottom;
        float dy1 = mRectF.top - top;
        float dy2 = mRectF.bottom - bottom;
        if (height <= showHeight)
        {
            dy = -(dy1 + dy2) / 2;
        } else
        {
            if (dy1 > 0 && dy2 > 0)
            {
                dy = -Math.min(dy1, dy2);
            } else if (dy1 < 0 && dy2 < 0)
            {
                dy = -Math.max(dy1, dy2);
            }
        }

        if (dx != 0 || dy != 0)
        {
            translateAnim(dx, dy, refresh);
        }
    }

    public void scaleToMin(float left, float top, Runnable refresh)
    {
        if (doingAnim || curScale == minScale)
        {
            return;
        }

        float fromScale = curScale;
        curScale = minScale;
        scaleAnim(fromScale, curScale, left, top, refresh);
    }

    public void resetScale(float left, float top, Runnable refresh)
    {
        if (doingAnim || curScale == 1)
        {
            return;
        }

        float fromScale = curScale;
        curScale = 1;

        scaleAnim(fromScale, curScale, left, top, refresh);
    }


    private void translateAnim(final float dx, final float dy, final Runnable refresh)
    {
        doingAnim = true;
        final float startX = modelMatrix[12];
        final float startY = modelMatrix[13];
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                float value = (float) animation.getAnimatedValue();
                modelMatrix[12] = startX + dx * value;
                modelMatrix[13] = startY + dy * value;
                refresh.run();
            }
        });
        animator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                doingAnim = false;
            }
        });
        animator.setDuration(300);
        animator.start();
    }

    private void scaleAnim(float fromScale, float toScale, final float left, final float top, final Runnable refresh)
    {
        doingAnim = true;
//		final float right = -left;
//		final float bottom = -top;
        ValueAnimator animator = ValueAnimator.ofFloat(fromScale, toScale);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                float scale = (float) animation.getAnimatedValue();

                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.scaleM(modelMatrix, 0, initScaleX, initScaleY, 1);
                MatrixUtils.scaleM(modelMatrix, 0, scale, scale, 1);
                if (curAngle != 0)
                {
                    Matrix.setIdentityM(mTempMatrix, 0);
                    Matrix.rotateM(mTempMatrix, 0, curAngle, 0, 0, 1);
                    MatrixUtils.multiplyMM(modelMatrix, 0, modelMatrix, 0, mTempMatrix, 0);
                }
//				checkBounds(left, top, right, bottom);
                refresh.run();
            }
        });
        modelScaleX = initScaleX * toScale;
        modelScaleY = initScaleY * toScale;
        animator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                doingAnim = false;
            }
        });
        animator.setDuration(300);
        animator.start();
    }

    private void mapRect()
    {
        mRectF.left = -modelMatrix[0] + modelMatrix[4] + modelMatrix[12];
        mRectF.top = -modelMatrix[1] + modelMatrix[5] + modelMatrix[13];
        mRectF.right = modelMatrix[0] - modelMatrix[4] + modelMatrix[12];
        mRectF.bottom = modelMatrix[1] - modelMatrix[5] + modelMatrix[13];

        float temp;
        if (mRectF.left > mRectF.right)
        {
            temp = mRectF.left;
            mRectF.left = mRectF.right;
            mRectF.right = temp;
        }
        if (mRectF.bottom > mRectF.top)
        {
            temp = mRectF.bottom;
            mRectF.bottom = mRectF.top;
            mRectF.top = temp;
        }
    }

    private void checkFitCenter(float left, float top, float right, float bottom)
    {
        float dx = 0;
        float dy = 0;

        mapRect();

        if (mRectF.left > left && mRectF.right < right)
        {
            dx = (mRectF.left + mRectF.right);
        }

        if (mRectF.top < top && mRectF.bottom > bottom)
        {
            dy = -(mRectF.top + mRectF.bottom);
        }

        if (dx != 0 || dy != 0)
        {
            MatrixUtils.translateM(modelMatrix, 0, dx, dy, 0);
        }
    }

    /**
     * 边界检测
     */
    private void checkBounds(float left, float top, float right, float bottom)
    {
        float dx = 0;
        float dy = 0;

        mapRect();
        if (mRectF.left > left)
        {
            dx = left - mRectF.left;
        }

        if (mRectF.right < right)
        {
            dx = right - mRectF.right;
        }

        if (mRectF.top < top)
        {
            dy = top - mRectF.top;
        }

        if (mRectF.bottom > bottom)
        {
            dy = bottom - mRectF.bottom;
        }

        if (dx != 0 || dy != 0)
        {
            MatrixUtils.translateM(modelMatrix, 0, dx, dy, 0);
        }
    }

    /**
     * 视频旋转
     */
    public void rotate(boolean right, float viewRatio, float showRatio, Runnable refresh)
    {

        if (doingAnim)
        {
            return;
        }

        float fromDegree = (curAngle + 360) % 360;

        curAngle += right ? -90 : 90;
        curAngle = (curAngle + 360) % 360;

        initScale(viewRatio, showRatio);

        curScale = 1f;

        float toDegree = (curAngle + 360) % 360;
        if (fromDegree == 0 && toDegree == 270)
        {
            fromDegree = 360;
        } else if (fromDegree == 270 && toDegree == 0)
        {
            fromDegree = -90;
        }

//		final float fromScaleX = modelMatrix[0];
//		final float fromScaleY = modelMatrix[5];
        rotateAnim(modelScaleX, initScaleX, modelScaleY, initScaleY, fromDegree, toDegree, refresh);
        modelScaleX = initScaleX;
        modelScaleY = initScaleY;
    }

    private final float[] mTempMatrix = new float[16];
    private float modelScaleX;
    private float modelScaleY;

    private void rotateAnim(final float fromScaleX, final float toScaleX, final float fromScaleY, final float toScaleY, final float fromDegree, final float toDegree, final Runnable refresh)
    {
        doingAnim = true;
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                float ratio = (float) animation.getAnimatedValue();

                float scaleX = (toScaleX - fromScaleX) * ratio + fromScaleX;
                float scaleY = (toScaleY - fromScaleY) * ratio + fromScaleY;
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1);

                float degree = (toDegree - fromDegree) * ratio + fromDegree;
                Matrix.setIdentityM(mTempMatrix, 0);
                Matrix.rotateM(mTempMatrix, 0, degree, 0, 0, 1);
                MatrixUtils.multiplyMM(modelMatrix, 0, modelMatrix, 0, mTempMatrix, 0);

                refresh.run();
            }
        });
        animator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                doingAnim = false;
            }
        });
        animator.setDuration(200);
        animator.start();
    }


    public float getVideoRatio(float showRatio)
    {
        int w = data.width;
        int h = data.height;
        if ((data.rotation + curAngle) % 180 != 0)
        {
            w = data.height;
            h = data.width;
        }

        final float videoRatio = w / (float) h;
        return Math.abs(showRatio - videoRatio) < 0.05f ? showRatio : videoRatio;
    }

    public float[] getSaveMatrix(float left, float top)
    {

        float scaleX = 1;
        float scaleY = 1;
        if (left != 0)
        {
            scaleX = 1 / Math.abs(left);
        }
        if (top != 0)
        {
            scaleY = 1 / Math.abs(top);
        }

        float[] saveMatrix = new float[16];
        Matrix.setIdentityM(saveMatrix, 0);
        Matrix.scaleM(saveMatrix, 0, scaleX, scaleY, 1);
        MatrixUtils.multiplyMM(saveMatrix, 0, saveMatrix, 0, modelMatrix, 0);

        return saveMatrix;
    }

    public SaveVideoInfo getSaveVideoInfo(float left, float top, boolean isMute)
    {
        SaveVideoInfo info = new SaveVideoInfo(id, data.path, data.width, data.height, data.duration, data.rotation, isMute);
        System.arraycopy(getSaveMatrix(left, top), 0, info.modelMatrix, 0, 16);
        System.arraycopy(texMatrix, 0, info.texMatrix, 0, 16);
        return info;
    }

    public SaveVideoInfo getStartScreenSaveInfo()
    {
        SaveVideoInfo info = new SaveVideoInfo(id, data.path, data.width, data.height, data.duration, data.rotation, true);
        System.arraycopy(modelMatrix, 0, info.modelMatrix, 0, 16);
        System.arraycopy(texMatrix, 0, info.texMatrix, 0, 16);
        return info;
    }

    public RenderInfo getRenderInfo(@NonNull PlayInfo playInfo)
    {
        RenderInfo renderInfo = new RenderInfo(playInfo.duration);
        System.arraycopy(modelMatrix, 0, renderInfo.modelMatrix, 0, modelMatrix.length);
        System.arraycopy(texMatrix, 0, renderInfo.texMatrix, 0, texMatrix.length);

//        renderInfo.startTransition = playInfo.startTransition;
//        renderInfo.endTransition = playInfo.endTransition;

        return renderInfo;
    }

    public VideoPlayInfo changeVideoInfo(VideoBaseInfo videoInfo)
    {
        VideoPlayInfo info = new VideoPlayInfo(id, videoInfo);
        info.initScaleX = initScaleX;
        info.initScaleY = initScaleY;
        System.arraycopy(modelMatrix, 0, info.modelMatrix, 0, 16);
        System.arraycopy(texMatrix, 0, info.texMatrix, 0, 16);
        info.curScale = curScale;
        info.curAngle = curAngle;
        info.minScale = minScale;
//        info.curve = curve.Clone();

        return info;
    }

    public VideoPlayInfo copyVideoInfo(VideoBaseInfo videoInfo)
    {
        VideoPlayInfo info = new VideoPlayInfo(videoInfo);
        info.initScaleX = initScaleX;
        info.initScaleY = initScaleY;
        System.arraycopy(modelMatrix, 0, info.modelMatrix, 0, 16);
        System.arraycopy(texMatrix, 0, info.texMatrix, 0, 16);
        info.curScale = curScale;
        info.curAngle = curAngle;
        info.minScale = minScale;
//        info.curve = curve.Clone();

        return info;
    }

    private static float checkScale(float scale)
    {
        return (int) (scale * 100) / 100f;
    }

    public static void reset()
    {
        sVideoId = 0;
    }
}