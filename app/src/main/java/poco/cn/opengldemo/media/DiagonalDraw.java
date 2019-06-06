package poco.cn.opengldemo.media;

import android.content.Context;
import android.opengl.GLES20;

import poco.cn.opengldemo.R;
import poco.cn.opengldemo.base.TextureDraw;

/**
 * Created by lgd on 2019/6/6.
 */
public class DiagonalDraw extends TextureDraw
{
    float diagonal = 0.0f;
    private int uRatioLoc;
    private int uScreenSizeLoc;

    public void setDiagonal(float diagonal)
    {
        this.diagonal = diagonal;
    }

    public DiagonalDraw(Context context)
    {
        super(context);
        createProgram(R.raw.vertex_shader_origin, R.raw.fragment_shader_diagonal);
    }

    @Override
    protected void onGetUniformLocation(int program)
    {
        super.onGetUniformLocation(program);
        uRatioLoc = GLES20.glGetUniformLocation(program, "diagonal");
        uScreenSizeLoc = GLES20.glGetUniformLocation(program, "screenSize");
    }

    @Override
    protected void onSetUniformData()
    {
        super.onSetUniformData();
        GLES20.glUniform1f(uRatioLoc, diagonal);
        GLES20.glUniform2f(uScreenSizeLoc, mSurfaceW,mSurfaceH);
    }

}
