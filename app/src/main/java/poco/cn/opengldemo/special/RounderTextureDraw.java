package poco.cn.opengldemo.special;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import poco.cn.opengldemo.R;
import poco.cn.opengldemo.base.TextureDraw;

/**
 * Created by lgd on 2019/5/27.
 */
public class RounderTextureDraw extends TextureDraw
{
    //临时矩阵
    private final float[] mTempModelMatrix = new float[16];
    private final float[] mTempTexMatrix = new float[16];
    private float scaleX = 0.5f;
    private float scaleY = 0.5f;

    private RounderDraw rounderDraw;

    public RounderTextureDraw(Context context)
    {
        super(context);

        createProgram(R.raw.vertex_shader_origin, R.raw.fragment_shader_origin);
        Matrix.setIdentityM(mTempModelMatrix, 0);
        Matrix.setIdentityM(mTempTexMatrix, 0);
        rounderDraw = new RounderDraw(context);
    }

    @Override
    public void setViewSize(int w, int h)
    {
        super.setViewSize(w, h);
        rounderDraw.setViewSize((int)(w * scaleX), (int)(h * scaleY));
    }

    public void draw(int textureId1, int textureId2,float ratio, float[] mvpMatrix, float[] texMatrix)
    {
        GLES20.glViewport(0, 0, mSurfaceW, mSurfaceH);
        draw(textureId1, mvpMatrix, texMatrix);

        rounderDraw.setRatio(ratio);
        int l = (mSurfaceW - rounderDraw.getSurfaceW())/2;
        int t = (mSurfaceH - rounderDraw.getSurfaceH())/2;
        GLES20.glViewport(l, t, rounderDraw.getSurfaceW(), rounderDraw.getSurfaceH());
        Matrix.setIdentityM(mTempModelMatrix, 0);
        if (scaleX > scaleY)
         {
            Matrix.scaleM(mTempModelMatrix, 0, 1f, scaleX / scaleY, 1f);
        } else
        {
            Matrix.scaleM(mTempModelMatrix, 0, scaleY / scaleX, 1f, 1f);
        }
        Matrix.multiplyMM(mTempModelMatrix, 0, mTempModelMatrix, 0, mvpMatrix, 0);
        rounderDraw.draw(textureId2, mTempModelMatrix, texMatrix);
    }

    public void setScale(float scaleX, float scaleY)
    {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        rounderDraw.setViewSize((int)(mSurfaceW * scaleX),(int)(mSurfaceH * scaleY));
    }

    public void blenEnable1(boolean enable)
    {
        if (enable)
        {
            GLES20.glDepthMask(false);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);
            GLES20.glBlendFuncSeparate(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_ONE, GLES20.GL_ONE);
//            GLES20.glBlendFunc(GLES20.GL_SRC_COLOR,GLES20.GL_SRC_ALPHA_SATURATE);
        } else
        {
            GLES20.glDisable(GLES20.GL_BLEND);
            GLES20.glDepthMask(true);
        }
    }

    public void setRadius(float r)
    {
        rounderDraw.setRadius(r);
    }
}
