package poco.cn.opengldemo.video.draw;

import android.content.Context;
import android.opengl.GLES20;

import poco.cn.opengldemo.R;


/**
 * Created by lgd on 2019/4/28.
 */
public class MaskExpandTransition extends LinearTransition
{
    private int uMaskLoc;
    private float[] mMask;
    public MaskExpandTransition(Context context)
    {
        super(context);
        mMask = new float[] {0, 0, 0, 1};
    }
    @Override
    protected int getVertexShaderRes() {
        return R.raw.vertex_transition_shader;
    }

    @Override
    protected int getFragmentShaderRes() {
        return R.raw.fragment_transition_expand;
    }

    @Override
    protected void onGetUniformLocation(int program)
    {
        super.onGetUniformLocation(program);
        uMaskLoc = GLES20.glGetUniformLocation(program,"mask");
    }

    @Override
    protected void onSetUniformData()
    {
        super.onSetUniformData();
        if(mMask != null){
            GLES20.glUniform4fv(uMaskLoc,1,mMask,0);
        }
    }
}
