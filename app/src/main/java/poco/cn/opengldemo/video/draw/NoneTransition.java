package poco.cn.opengldemo.video.draw;

import android.content.Context;

import poco.cn.opengldemo.R;


/**
 * Created by lgd on 2019/4/24.
 */
public class NoneTransition extends AbsTransition
{
    public NoneTransition(Context context)
    {
        super(context);
        createProgram(R.raw.vertex_transition_shader, R.raw.fragment_transition_none);
    }
}
