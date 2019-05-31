package poco.cn.opengldemo.special;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import poco.cn.medialibs.gles.Drawable2d;
import poco.cn.medialibs.gles.GlUtil;
import poco.cn.opengldemo.R;

/**
 * Created by lgd on 2019/5/23.
 */
public class MuiltPortDraw
{
    private final Context mContext;
//    private int mTextureId1 = GlUtil.NO_TEXTURE;
//    private int mTextureId2 = GlUtil.NO_TEXTURE;

    private int uFirstImageLoc;
    private int uSecondImageLoc;

    private float[] mModelMatrix = new float[16];
    private float[] mTexMatrix = new float[16];
    private int uScaleLoc;
    private int mProgram;
    private int aPositionLoc;
    private int aTextureCoordLoc;

    private float scaleX = 0.5f;
    @NonNull
    private final Drawable2d mDrawable2d = new Drawable2d();
    private int uMVPMatrixLoc;
    private int uTexMatrixLoc;
    private float scaleY;
    private int mViewW;
    private int mViewH;



    public MuiltPortDraw(Context context)
    {
        mContext = context;
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.setIdentityM(mTexMatrix, 0);
        // 翻转纹理
        mTexMatrix[5] = -1;
        mTexMatrix[13] = 1;

        createProgram(R.raw.vertex_multport_shader, R.raw.fragment_multport);

    }

    protected void createProgram(@RawRes int vertexShader, @RawRes int fragmentShader)
    {
        if (vertexShader != 0 && fragmentShader != 0)
        {
            mProgram = GlUtil.createProgram(mContext, vertexShader, fragmentShader);
            onGetUniformLocation(mProgram);
        }
    }

    public void setViewSize(int w,int h)
    {
        mViewW = w;
        mViewH = h;
    }

//    protected void setProgram(int program)
//    {
//        if (program == 0)
//        {
//            throw new RuntimeException("the program is 0.");
//        }
//        mProgram = program;
//
//        onGetUniformLocation(program);
//    }

    protected void onGetUniformLocation(int program)
    {
        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition");
        aTextureCoordLoc = GLES20.glGetAttribLocation(program, "aTextureCoord");
        uFirstImageLoc = GLES20.glGetUniformLocation(program, "firstImage");
        uSecondImageLoc = GLES20.glGetUniformLocation(program, "secondImage");
        uScaleLoc = GLES20.glGetUniformLocation(program, "scaleX");

        uMVPMatrixLoc = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        uTexMatrixLoc = GLES20.glGetUniformLocation(mProgram, "uTexMatrix");

    }

    public void draw(int textureId1, int textureId2, float[] mvpMatrix, float[] texMatrix)
    {
        onUseProgram();
        onSetUniformData();
        onBindTexture(textureId1, textureId2);
        onDraw(mvpMatrix, texMatrix);
    }

    protected void onDraw(float[] mvpMatrix, float[] texMatrix)
    {
        if (uMVPMatrixLoc != -1)
        {
            GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0);
        }

        if (uTexMatrixLoc != -1)
        {
            GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0);
        }

        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, mDrawable2d.getCoordsPerVertex(),
                GLES20.GL_FLOAT, false,
                mDrawable2d.getVertexStride(), mDrawable2d.getVertexArray());

        GLES20.glEnableVertexAttribArray(aTextureCoordLoc);
        GLES20.glVertexAttribPointer(aTextureCoordLoc, 2, GLES20.GL_FLOAT,
                false, mDrawable2d.getTexCoordStride(),
                mDrawable2d.getTexCoordArray());

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mDrawable2d.getVertexCount());

        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTextureCoordLoc);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glUseProgram(0);
    }

    protected void onUseProgram()
    {
        GLES20.glUseProgram(mProgram);
    }

    protected void onBindTexture(int textureId1, int textureId2)
    {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId1);
        GLES20.glUniform1i(uFirstImageLoc, 0);

        if (uSecondImageLoc >= 0 && textureId2 >= 0)
        {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId2);
            GLES20.glUniform1i(uSecondImageLoc, 1);
        }
    }

    protected void onSetUniformData()
    {
        if (uScaleLoc >= 0)
        {
            GLES20.glUniform1f(uScaleLoc, scaleX);
        }
    }

    public void release()
    {
        if (mProgram != 0)
        {
            GLES20.glDeleteProgram(mProgram);
            mProgram = 0;
        }
    }

    public void setScaleX(float f)
    {
        scaleX = f;
//        if (uScaleLoc >= 0)
//        {
//            GLES20.glUniform1f(uScaleLoc, scaleX);
//        }
    }

    public void setScaleY(float f)
    {
        scaleY = f;
    }
}
