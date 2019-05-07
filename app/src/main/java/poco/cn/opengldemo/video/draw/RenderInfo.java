package poco.cn.opengldemo.video.draw;

import android.opengl.Matrix;
import androidx.annotation.NonNull;

/**
 * Created by lgd on 2019/4/22.
 */
public class RenderInfo
{
    @NonNull
    public final float[] modelMatrix;

    @NonNull
    public final float[] texMatrix;

    public final long duration;

    public RenderInfo(long duration)
    {
        modelMatrix = new float[16];
        texMatrix = new float[16];

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(texMatrix, 0);

        this.duration = duration;
    }

    public RenderInfo Clone()
    {
        final RenderInfo renderInfo = new RenderInfo(duration);
        System.arraycopy(modelMatrix, 0, renderInfo.modelMatrix, 0, 16);
        System.arraycopy(texMatrix, 0, renderInfo.texMatrix, 0, 16);
        return renderInfo;
    }


}
