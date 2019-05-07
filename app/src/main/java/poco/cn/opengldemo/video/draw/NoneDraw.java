package poco.cn.opengldemo.video.draw;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import poco.cn.medialibs.gles.GlUtil;
import poco.cn.opengldemo.R;

/**
 * Created by lgd on 2019/4/24.
 */
public class NoneDraw extends BaseDraw
{
    private int uSourceImageLoc;

    private float[] mTexMatrix = new float[16];
    public NoneDraw(Context context)
    {
        super(context);
        Matrix.setIdentityM(mTexMatrix, 0);

        // 翻转纹理
//        mTexMatrix[5] = -1;
//        mTexMatrix[13] = 1;

        createProgram(R.raw.vertex_shader_none, R.raw.fragment_origin_shader);
    }


    @Override
    protected void onGetUniformLocation(int program)
    {
        uSourceImageLoc = GLES20.glGetUniformLocation(program, "sourceImage");
    }

    @Override
    protected void onBindTexture(int textureId)
    {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uSourceImageLoc, 2);
    }

    public void draw(int textureId)
    {
        super.draw(textureId, GlUtil.IDENTITY_MATRIX, mTexMatrix);
    }


    @Override
    protected void onSetUniformData()
    {

    }

    @Override
    public void release()
    {
        super.release();
    }
}
