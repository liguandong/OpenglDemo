package poco.cn.opengldemo.video.view;

import android.graphics.RectF;
import android.opengl.Matrix;

import androidx.annotation.NonNull;
import poco.cn.medialibs.player2.PlayInfo;
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
    }

    /**
     * 根据比例计算  每个视频的缩放，
     *
     * @param viewRatio
     * @param showRatio
     */
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
        float videoRatio = getVideoRatio(showRatio);
        float scaleX;
        float scaleY;
        //画图明朗，外框viewRatio，内框showRatio 和 videoRatio
        if (viewRatio > showRatio)
        {
            //x变化因子
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
            //y为变化因子

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

    }

    /**
     * 缩放
     */
    public void scale(float scale, float left, float top)
    {


    }

    /**
     * 缩放结束
     */
    public void scaleFinish(float left, float top, Runnable refresh)
    {

    }

    public void scaleToMin(float left, float top, Runnable refresh)
    {

    }

    public void resetScale(float left, float top, Runnable refresh)
    {

    }

    private void translateAnim(final float dx, final float dy, final Runnable refresh)
    {

    }

    private void scaleAnim(float fromScale, float toScale, final float left, final float top, final Runnable refresh)
    {

    }

    private void mapRect()
    {

    }

    private void checkFitCenter(float left, float top, float right, float bottom)
    {

    }

    /**
     * 视频旋转
     */
    public void rotate(boolean right, float viewRatio, float showRatio, Runnable refresh)
    {

    }

    private final float[] mTempMatrix = new float[16];
    private float modelScaleX;
    private float modelScaleY;

    private void rotateAnim(final float fromScaleX, final float toScaleX, final float fromScaleY, final float toScaleY, final float fromDegree, final float toDegree, final Runnable refresh)
    {

    }

    public float getVideoRatio(float showRatio)
    {
        int w = data.width;
        int h = data.height;

        final float videoRatio = w / (float) h;
        return Math.abs(showRatio - videoRatio) < 0.05f ? showRatio : videoRatio;
    }

    public RenderInfo getRenderInfo(PlayInfo playInfo)
    {
        RenderInfo renderInfo = new RenderInfo(playInfo.duration);
        System.arraycopy(modelMatrix,0,renderInfo.modelMatrix,0,modelMatrix.length);
        System.arraycopy(texMatrix,0,renderInfo.texMatrix,0,texMatrix.length);
//        renderInfo.startTransition = playInfo.startTransition;
//        renderInfo.endTransition = playInfo.endTransition;
        return renderInfo;
    }
}
