package poco.cn.opengldemo.base;

import android.content.Context;
import android.opengl.GLES20;

import poco.cn.medialibs.gles.GlUtil;

/**
 * Created by lgd on 2019/5/30.
 */
public class TextureDraw extends BaseDraw
{
    protected int mSurfaceW;
    protected int mSurfaceH;
    protected int uSourceImageLoc;
    //    private final float[] mTempModelMatrix = new float[16];

    public TextureDraw(Context context)
    {
        super(context);
//        Matrix.setIdentityM(mTempModelMatrix, 0);
        // 翻转纹理
    }

    public void setViewSize(int surfaceW, int surfaceH)
    {
        this.mSurfaceW = surfaceW;
        this.mSurfaceH = surfaceH;
    }

    public int getSurfaceW()
    {
        return mSurfaceW;
    }

    public int getSurfaceH()
    {
        return mSurfaceH;
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

    public void onDraw(int textureId)
    {
        draw(textureId, GlUtil.IDENTITY_MATRIX, GlUtil.IDENTITY_MATRIX);
    }
}
