package poco.cn.opengldemo.special;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import poco.cn.opengldemo.R;
import poco.cn.opengldemo.base.BaseDraw;

/**
 * Created by lgd on 2019/5/27.
 */
public class SquareWindowDraw extends BaseDraw
{

    private int mViewW;
    private int mViewH;
    //临时矩阵
    private final float[] mTempModelMatrix = new float[16];
    private final float[] mTempTexMatrix = new float[16];
    private int uSourceImageLoc;
    private float scaleX = 0.5f;
    private float scaleY = 0.5f;

    public SquareWindowDraw(Context context)
    {
        super(context);

        createProgram(R.raw.vertex_shader_origin, R.raw.fragment_shader_origin);
    }


    public void setViewSize(int w, int h)
    {
        mViewW = w;
        mViewH = h;

    }

    @Override
    protected void onGetUniformLocation(int program)
    {
        uSourceImageLoc = GLES20.glGetUniformLocation(program, "sourceImage");
    }

    @Override
    protected void onSetUniformData()
    {

    }

    @Override
    protected void onBindTexture(int textureId)
    {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uSourceImageLoc, 1);

    }


    public void draw(int textureId1, int textureId2, float[] mvpMatrix, float[] texMatrix)
    {
        GLES20.glViewport(0, 0, mViewW, mViewH);
        draw(textureId1, mvpMatrix, texMatrix);

        int w = (int) (mViewW * scaleX);
        int h = (int) (mViewH * scaleY);
        int l = (int) (mViewW - w) / 2;
        int t = (int) (mViewH - h) / 2;
        GLES20.glViewport(l, t, w, h);
        Matrix.setIdentityM(mTempModelMatrix, 0);
        if (scaleX > scaleY)
        {
            Matrix.scaleM(mTempModelMatrix, 0, 1f, scaleX / scaleY, 1f);
        } else
        {
            Matrix.scaleM(mTempModelMatrix, 0, scaleY / scaleX, 1f, 1f);
        }
        Matrix.multiplyMM(mTempModelMatrix, 0, mTempModelMatrix, 0, mvpMatrix, 0);

        draw(textureId2, mTempModelMatrix, texMatrix);
    }

    public void setScale(float scaleX, float scaleY)
    {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }
}
