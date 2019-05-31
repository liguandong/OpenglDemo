package poco.cn.opengldemo.special;

import android.content.Context;
import android.opengl.GLES20;

import androidx.annotation.FloatRange;
import poco.cn.opengldemo.R;
import poco.cn.opengldemo.base.TextureDraw;

/**
 * Created by lgd on 2019/5/30.
 */
public class RounderDraw extends TextureDraw
{
    private int uRadiusImageLoc;
    private int uRatioImageLoc;
    private float radius = 0.5f;

    public RounderDraw(Context context)
    {
        super(context);

        createProgram(R.raw.vertex_shader_rounder, R.raw.fragment_shader_rounder);
    }

    /**
     * 已宽为标准 ，给着色器时会判断范围
     *
     * @param radius
     */
    public void setRadius(@FloatRange(from = 0.0, to = 0.5) float radius)
    {
        this.radius = radius;
    }


    private float getFixRadius()
    {
        float ratio = mSurfaceW / (float) mSurfaceH;
        float r = radius;
        if(ratio < 1)
        {
            r = r * ratio;
        }
        return r;
    }

    @Override
    protected void onGetUniformLocation(int program)
    {
        super.onGetUniformLocation(program);
        uRadiusImageLoc = GLES20.glGetUniformLocation(program, "radius");
        uRatioImageLoc = GLES20.glGetUniformLocation(program, "ratio");
    }

    @Override
    protected void onSetUniformData()
    {
        super.onSetUniformData();
        GLES20.glUniform1f(uRatioImageLoc, mSurfaceW / (float) mSurfaceH);
        GLES20.glUniform1f(uRadiusImageLoc, getFixRadius());
    }
}
