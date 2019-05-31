/*
 *
 * Triangle.java
 *
 * Created by Wuwang on 2016/9/30
 */
package poco.cn.opengldemo.special;

import android.content.Context;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import poco.cn.medialibs.gles.GlUtil;
import poco.cn.opengldemo.R;

/**
 * Description:
 */
public class Circle
{

    private FloatBuffer vertexBuffer;
    private int mProgram;
    static final int COORDS_PER_VERTEX = 3;
    private int mPositionHandle;
    private int mColorHandle;
//    private float[] mMVPMatrix = new float[16];

    //顶点之间的偏移量
    private final int vertexStride = 0; // 每个顶点四个字节

    private int mMatrixHandler;

    private float radius = 1.0f;
    private int n = 360;  //切割份数

    private float[] shapePos;

    private float height = 0.0f;

    //设置颜色，依次为红绿蓝和透明通道
    float color[] = {0.0f, 0.0f, 0.0f, 1.0f};

    private Context mContext;

    public Circle(Context context)
    {
        this.mContext = context;
        shapePos = createPositions();
        ByteBuffer bb = ByteBuffer.allocateDirect(
                shapePos.length * 4);
        bb.order(ByteOrder.nativeOrder());

        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(shapePos);
        vertexBuffer.position(0);
        mProgram = GlUtil.createProgram(mContext, R.raw.vertex_color_shader, R.raw.fragment_shader_color);
    }

    private float[] createPositions()
    {
        ArrayList<Float> data = new ArrayList<>();
        data.add(0.0f);             //设置圆心坐标
        data.add(0.0f);
        data.add(height);
        float angDegSpan = 360f / n;
        for (float i = 0; i < 360 + angDegSpan; i += angDegSpan)
        {
            data.add((float) (radius * Math.sin(i * Math.PI / 180f)));
            data.add((float) (radius * Math.cos(i * Math.PI / 180f)));
            data.add(height);
        }
        float[] f = new float[data.size()];
        for (int i = 0; i < f.length; i++)
        {
            f[i] = data.get(i);
        }
        return f;
    }

    public void onDraw(float[] matrix)
    {
        //将程序加入到OpenGLES2.0环境
        GLES20.glUseProgram(mProgram);
        //获取变换矩阵vMatrix成员句柄
        mMatrixHandler = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        //指定vMatrix的值
        GLES20.glUniformMatrix4fv(mMatrixHandler, 1, false, matrix, 0);
        //获取顶点着色器的vPosition成员句柄
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        //启用三角形顶点的句柄
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        //准备三角形的坐标数据
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);
        //获取片元着色器的vColor成员的句柄
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        //设置绘制三角形的颜色
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);
        //绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, shapePos.length / 3);
        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

}
