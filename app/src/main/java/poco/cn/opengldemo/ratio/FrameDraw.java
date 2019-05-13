package poco.cn.opengldemo.ratio;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import poco.cn.opengldemo.R;
import poco.cn.opengldemo.base.BaseDraw;

/**
 * Created by lgd on 2019/5/7.
 */
public class FrameDraw extends BaseDraw
{
//    private int mTextureId = GlUtil.NO_TEXTURE;
    private int uSourceImageLoc;
//    private float[] mModelMatrix = new float[16];
//    private float[] mTexMatrix = new float[16];

    public FrameDraw(Context context)
    {
        super(context);
        createProgram(R.raw.vertex_shader_origin, R.raw.fragment_shader_origin);
//        mTexMatrix[5] = -1;
//        mTexMatrix[13] = 1;
    }

    @Override
    protected void onGetUniformLocation(int program)
    {
        uSourceImageLoc = GLES20.glGetUniformLocation(program, "sourceImage");
    }

    /**
     * 更换Bitmap
     *
     * @param bitmap Bitmap对象
     */
    public void changeBitmap(Bitmap bitmap)
    {
//        mTextureId = GlUtil.setBitmapOnTexture(mTextureId, bitmap);
    }


    @Override
    protected void onSetUniformData()
    {

    }


    @Override
    protected void onBindTexture(int textureId)
    {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mTextureId);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId);
        GLES20.glUniform1i(uSourceImageLoc, 1);
    }

    @Override
    public void release()
    {
        super.release();
//        if (mTextureId != GlUtil.NO_TEXTURE)
//        {
//            GLES20.glDeleteTextures(1, new int[mTextureId], 0);
//            mTextureId = GlUtil.NO_TEXTURE;
//        }
    }
}
