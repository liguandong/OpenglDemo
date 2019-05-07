package poco.cn.opengldemo.video.draw;

import android.content.Context;
import androidx.annotation.RawRes;

/**
 * Created by lgd on 2019/4/28.
 */
public abstract class LinearTransition extends AbsTransition
{
    public LinearTransition(Context context)
    {
        super(context);
        int programe = getProgram();
        if(programe != 0){
            setProgram(programe);
        }else{
            createProgram(getVertexShaderRes(),getFragmentShaderRes());
        }
    }

    protected int getProgram()
    {
        return 0;
    }

    @RawRes
    protected int getVertexShaderRes() {
        return 0;
    }

    @RawRes
    protected int getFragmentShaderRes() {
        return 0;
    }
}
