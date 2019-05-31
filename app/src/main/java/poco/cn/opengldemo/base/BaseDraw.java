package poco.cn.opengldemo.base;

import android.content.Context;
import android.opengl.GLES20;
import androidx.annotation.RawRes;

import poco.cn.medialibs.gles.Drawable2d;
import poco.cn.medialibs.gles.GlUtil;

/**
 * Created by lgd on 2019/4/19.
 * 读取顶点数据——执行顶点着色器——组装图元——光栅化图元——执行片元着色器——写入帧缓冲区——显示到屏幕上。
 * 加载顶点和片元着色器
 * 确定需要绘制图形的坐标和颜色数据
 * 创建program对象，连接顶点和片元着色器，链接program对象。
 * 设置视图窗口(viewport)。
 * 将坐标数据颜色数据传入OpenGL ES程序中
 * 使颜色缓冲区的内容显示到屏幕上。
 */
public abstract class BaseDraw {

    protected Context mContext;

    private int mProgram = 0;
    private Drawable2d mDrawable2d = new Drawable2d();

    protected int aPositionLoc;
    protected int aTextureCoordLoc;

    protected int uMVPMatrixLoc;
    protected int uTexMatrixLoc;

    public BaseDraw(Context context) {
        mContext = context;
    }

    protected void createProgram(@RawRes int vertexShader, @RawRes int fragmentShader) {
        mProgram = GlUtil.createProgram(mContext, vertexShader, fragmentShader);

        aPositionLoc = GLES20.glGetAttribLocation(mProgram, "aPosition");
        aTextureCoordLoc = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        uMVPMatrixLoc = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        uTexMatrixLoc = GLES20.glGetUniformLocation(mProgram, "uTexMatrix");

        onGetUniformLocation(mProgram);
    }

    public final void draw(int textureId, float[] mvpMatrix, float[] texMatrix) {
        onUseProgram();
        onSetUniformData();
        onBindTexture(textureId);
        onDraw(mvpMatrix, texMatrix);
    }

    protected void onUseProgram(){
        GLES20.glUseProgram(mProgram);
    }

    /**
     * 获取Uniform变量位置
     */
    protected abstract void onGetUniformLocation(int program);

    /**
     * 设置Uniform变量数据
     */
    protected abstract void onSetUniformData();

    /**
     * 绑定纹理
     */
    protected abstract void onBindTexture(int textureId);

    /**
     * 绘制
     */
    protected void onDraw(float[] mvpMatrix, float[] texMatrix) {

        if (uMVPMatrixLoc != -1) {
            GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0);
        }

        if (uTexMatrixLoc != -1) {
            GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0);
        }

        if (aPositionLoc != -1) {
            GLES20.glEnableVertexAttribArray(aPositionLoc);
            GLES20.glVertexAttribPointer(aPositionLoc, mDrawable2d.getCoordsPerVertex(),
                    GLES20.GL_FLOAT, false, mDrawable2d.getVertexStride(), mDrawable2d.getVertexArray());
        }

        if (aTextureCoordLoc != -1) {
            GLES20.glEnableVertexAttribArray(aTextureCoordLoc);
            GLES20.glVertexAttribPointer(aTextureCoordLoc, 2,
                    GLES20.GL_FLOAT, false, mDrawable2d.getTexCoordStride(), mDrawable2d.getTexCoordArray());
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mDrawable2d.getVertexCount());

        if (aPositionLoc != -1) {
            GLES20.glDisableVertexAttribArray(aPositionLoc);
        }

        if (aTextureCoordLoc != -1) {
            GLES20.glDisableVertexAttribArray(aTextureCoordLoc);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
    }

    public void release() {
        if (mProgram != 0) {
            GLES20.glDeleteProgram(mProgram);
            mProgram = 0;
        }
    }
}
