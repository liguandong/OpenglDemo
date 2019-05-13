package poco.cn.opengldemo.ratio2;

import android.opengl.Matrix;

/**
 * Created by lgd on 2019/5/10.
 */
public class RenderInfo
{
    ImagePlayInfo2 playInfo2;
    /**
     * 图片的基础矩阵
     */
    public final float[] modelMatrix = new float[16];
    public final float[] texMatrix = new float[16];
    private float viewRatio = 1f;

    public RenderInfo(ImagePlayInfo2 imagePlayInfo2)
    {
        this.playInfo2 = imagePlayInfo2;
    }

    public void setViewRatio(float viewRatio)
    {
        this.viewRatio = viewRatio;
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(texMatrix, 0);
    }

    public void getModeMatrix()
    {
        Matrix.setIdentityM(modelMatrix, 0);
    }
    public void getTextMatrix()
    {
        Matrix.setIdentityM(modelMatrix, 0);
    }

}
