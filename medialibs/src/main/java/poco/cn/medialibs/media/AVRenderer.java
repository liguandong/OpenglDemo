package poco.cn.medialibs.media;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AVRenderer implements GLSurfaceView.Renderer

{
    //顶点数组(物体表面坐标取值范围是-1到1,数组坐标：左下，右下，左上，右上)
    private static float[] vertexVertices = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f,  1.0f,
    };


    //像素，纹理数组(纹理坐标取值范围是0-1，坐标原点位于左下角,数组坐标：左上，右上，左下，右下,如果先左下，图像会倒过来)
    private static float[] textureVertices = {
            0.0f,  1.0f,
            1.0f,  1.0f,
            0.0f,  0.0f,
            1.0f,  0.0f,
    };

    //shader的vsh源码字符串
    private static final String vertexShaderString =
            "attribute vec4 vertexIn;" +
                    "attribute vec2 textureIn;" +
                    "varying vec2 textureOut;" +
                    "void main() {" +
                    "gl_Position = vertexIn;" +
                    "textureOut = textureIn;" +
                    "}";

    //shader的fsh源码字符串
//    private static final String yuvFragmentShaderString =
//            "precision mediump float;" +
//                    "uniform sampler2D tex_y;" +
//                    "uniform sampler2D tex_u;" +
//                    "uniform sampler2D tex_v;" +
//                    "varying vec2 textureOut;" +
//                    "void main() {" +
//                    "vec4 c = vec4((texture2D(tex_y, textureOut).r - 16./255.) * 1.164);" +
//                    "vec4 U = vec4(texture2D(tex_u, textureOut).r - 128./255.);" +
//                    "vec4 V = vec4(texture2D(tex_v, textureOut).r - 128./255.);" +
//                    "c += V * vec4(1.596, -0.813, 0, 0);" +
//                    "c += U * vec4(0, -0.392, 2.017, 0);" +
//                    "c.a = 1.0;" +
//                    "gl_FragColor = c;" +
//                    "}";

    private static final String nv21FragmentShaderCode =
            "precision highp float;\n" +
                    "varying vec2 v_texCoord;\n" +
                    "uniform sampler2D y_texture;\n" +
                    "uniform sampler2D uv_texture;\n" +
                    "void main(void) {\n" +
                    "   float y, u, v, r, g, b;\n" +

                    "   //We had put the Y values of each pixel to the R,G,B components by GL_LUMINANCE,\n" +
                    "   //that's why we're pulling it from the R component, we could also use G or B\n" +
                    "   y = texture2D(y_texture, v_texCoord).r;\n" +

                    "   //We had put the U and V values of each pixel to the A and R,G,B components of the\n" +
                    "   //texture respectively using GL_LUMINANCE_ALPHA. Since U,V bytes are interspread\n" +
                    "   //in the texture, this is probably the fastest way to use them in the shader\n" +
                    "   u = texture2D(uv_texture, v_texCoord).a - 0.5;\n" +
                    "   v = texture2D(uv_texture, v_texCoord).r - 0.5;\n" +

                    "   //The numbers are just YUV to RGB conversion constants\n" +
                    "   r = y + 1.402 * v;\n" +
                    "   g = y - 0.34414 * u - 0.71414 * v;\n" +
                    "   b = y + 1.772 * u;\n" +

                    "   //We finally set the RGB color of our pixel\n" +
                    "   gl_FragColor = vec4(r, g, b, 1.0);\n" +

                    "}";

    private static final String yuvFragmentShaderString =
            "precision highp float;" +
            "varying highp vec2 textureOut;" +
            "uniform sampler2D tex_y;" +
            "uniform sampler2D tex_u;" +
            "uniform sampler2D tex_v;" +
            "void main(void)" +
            "{" +
            "mediump vec3 yuv;" +
            "lowp vec3 rgb;" +
            "yuv.x = texture2D(tex_y, textureOut).r;" +
            "yuv.y = texture2D(tex_u, textureOut).r - 0.5;" +
            "yuv.z = texture2D(tex_v, textureOut).r - 0.5;" +
            "rgb = mat3( 1,   1,   1," +
            "0,       -0.39465,  2.03211," +
            "1.13983,   -0.58060,  0) * yuv;" +
            "gl_FragColor = vec4(rgb, 1.0);" +
            "}";

    //着色器用的顶点属性索引 position是由3个（x,y,z）组成，
    private int mAttribVertex = 0;
    //着色器用的像素，纹理属性索引 而颜色是4个（r,g,b,a）
    private int mAttribTexture = 0;
    private GLSurfaceView mSurfaceView;                                                              //外部传入的GLSurfaceView
    private int mProgram = 0;                                                                                    //Program着色器程序的id
    private ByteBuffer mVertexVerticesBuffer = null;                                                           //定义顶点数组
    private ByteBuffer mTextureVerticesBuffer = null;                                                          //定义像素纹理数组
    private boolean mInitialized;										       //存放yuv数据的buf指针，申请buffer在外面
    private ByteBuffer mYuvY = null;                                                               //分用于渲染的变量
    private ByteBuffer mYuvU = null;                                                               //分用于渲染的变量
    private ByteBuffer mYuvV = null;                                                               //分用于渲染的变量;
    private int mYuvWidth = 0;											                            //数据宽
    private int mYuvHeight = 0;										                            //数据高
    private int mTextureIdY, mTextureIdU, mTextureIdV;                                         //纹理的名称，并且，该纹理的名称在当前的应用中不能被再次使用。
    private int mTextureUniformY, mTextureUniformU, mTextureUniformV;                            //用于纹理渲染的变量

    //构造方法
    public AVRenderer(GLSurfaceView glSurfaceView)
    {
        mSurfaceView = glSurfaceView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig)
    {
        //这个必须在onSurfaceCreated中，否则失败
        //初始化着色器，类似于告GPU当传进去数据的时候采用什么样的规则。
        initShaders();
        mInitialized = true;
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height)
    {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10)
    {
        if(mSurfaceView != null && mYuvY != null)
        {
            displayImage();
        }
    }

    //接口初始化
    public void drawFrame(byte[] yuv, int yuvWidth, int yuvHeight)
    {
        mYuvWidth = yuvWidth;
        mYuvHeight = yuvHeight;
        boolean first = false;
        if(mYuvY == null){
            first = true;
            mYuvY = ByteBuffer.allocate(mYuvWidth * mYuvHeight);
        }
        if(mYuvU == null){
            mYuvU = ByteBuffer.allocate(mYuvWidth * mYuvHeight/4);
        }
        if(mYuvV == null){
            mYuvV = ByteBuffer.allocate(mYuvWidth * mYuvHeight/4);
        }
        long n = System.currentTimeMillis();
        mYuvY.clear();
        mYuvY.put(yuv, 0, mYuvWidth * mYuvHeight);
        mYuvY.position(0);
        mYuvU.clear();
        mYuvU.put(yuv, mYuvWidth * mYuvHeight, mYuvWidth * mYuvHeight / 4);
        mYuvU.position(0);
        mYuvV.clear();
        mYuvV.put(yuv, mYuvWidth * mYuvHeight + mYuvWidth * mYuvHeight / 4, mYuvWidth * mYuvHeight / 4);
        mYuvV.position(0);
        Log.d("hwq", "put:"+(System.currentTimeMillis() - n));

//        if(first) {
//            ByteBuffer yuvBuffer = ByteBuffer.allocateDirect(mYuvWidth * mYuvHeight + mYuvWidth * mYuvHeight / 2);
//            yuvBuffer.clear();
//            yuvBuffer.put(mYuvY);
//            yuvBuffer.put(mYuvU);
//            yuvBuffer.put(mYuvV);
//            yuvBuffer.position(0);
////            byte[] dd = new byte[yuv.length];
////            yuvBuffer.get(dd);
//            mYuvY.position(0);
//            mYuvU.position(0);
//            mYuvV.position(0);
//
//            PixelConverter conv = new PixelConverter();
//            conv.create(mYuvWidth, mYuvHeight, AVPixelFormat.AV_PIX_FMT_RGBA);
//            Bitmap bmp = (Bitmap) conv.conv(yuvBuffer, AVNative.DATA_FMT_BITMAP, mYuvWidth, mYuvHeight, AVPixelFormat.AV_PIX_FMT_YUV420P);
//            if(bmp != null) {
//                try {
//                    FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "out.png");
//                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
//                    fos.close();
//                } catch(Exception e) {
//                }
//            }
//        }
        mSurfaceView.requestRender();
    }

    //初始化着色器，类似于告GPU当传进去数据的时候采用什么样的规则。
    private void initShaders()
    {
        int error = 0;
        createBuffers(vertexVertices, textureVertices);
        mProgram = createProgram(vertexShaderString, yuvFragmentShaderString);
        mAttribVertex = GLES20.glGetAttribLocation(mProgram, "vertexIn");
        if (mAttribVertex == -1)
        {
        }
        mAttribTexture = GLES20.glGetAttribLocation(mProgram, "textureIn");
        if (mAttribTexture == -1)
        {
        }
        //获取片源着色器源码中的变量,用于纹理渲染
        mTextureUniformY = GLES20.glGetUniformLocation(mProgram, "tex_y");
        mTextureUniformU = GLES20.glGetUniformLocation(mProgram, "tex_u");
        mTextureUniformV = GLES20.glGetUniformLocation(mProgram, "tex_v");

        //初始化纹理
        int[] texturesY = new int[1];
        GLES20.glGenTextures(1, texturesY,0);
        mTextureIdY = texturesY[0];
        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIdY);
        //设置该纹理的一些属性
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        int[] texturesU = new int[1];
        GLES20.glGenTextures(1, texturesU,0);
        mTextureIdU = texturesU[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIdU);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        int[] texturesV = new int[1];
        GLES20.glGenTextures(1, texturesV,0);
        mTextureIdV = texturesV[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIdV);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return;
    }

    private void displayImage()
    {
        int ret = 0;

        GLES20.glUseProgram(mProgram);

        //Clear
        //清除颜色设为黑色，把整个窗口清除为当前的清除颜色，glClear（）的唯一参数表示需要被清除的缓冲区。
        GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //定义顶点数组,android平台要在这里做其他平台在initshader中，否则显示不出来图像
        GLES20.glVertexAttribPointer(mAttribVertex, 2, GLES20.GL_FLOAT, false, 0, mVertexVerticesBuffer);
        //启用属性数组，android平台要在这里做其他平台在initshader中，否则显示不出来图像
        GLES20.glEnableVertexAttribArray(mAttribVertex);
        //定义像素纹理数组，android平台要在这里，做其他平台在initshader中，否则显示不出来图像
        GLES20. glVertexAttribPointer(mAttribTexture, 2, GLES20.GL_FLOAT, false, 0, mTextureVerticesBuffer);
        //启用属性数组，android平台要在这里做其他平台在initshader中，否则显示不出来图像
        GLES20.glEnableVertexAttribArray(mAttribTexture);
        //显卡中有N个纹理单元（具体数目依赖你的显卡能力），每个纹理单元（GL_TEXTURE0、GL_TEXTURE1等）都有GL_TEXTURE_1D、GL_TEXTURE_2D等
        //Y
        //选择当前活跃的纹理单元
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //允许建立一个绑定到目标纹理的有名称的纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIdY);
        //根据指定的参数，生成一个2D纹理（Texture）。相似的函数还有glTexImage1D、glTexImage3D。
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mYuvWidth, mYuvHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, mYuvY);
        GLES20.glUniform1i(mTextureUniformY, 0);     //设置纹理，按照前面设置的规则怎样将图像或纹理贴上（参数和选择的活跃纹理单元对应，GL_TEXTURE0）
        //U
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIdU);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mYuvWidth/2, mYuvHeight/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, mYuvU);
        GLES20.glUniform1i(mTextureUniformU, 1);
        //V
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIdV);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mYuvWidth/2, mYuvHeight/2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, mYuvV);
        GLES20.glUniform1i(mTextureUniformV, 2);

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 4);

        GLES20.glUseProgram(0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        //单缓冲显示
        GLES20.glFlush();
        GLES20.glDisableVertexAttribArray(mAttribVertex);
        GLES20.glDisableVertexAttribArray(mAttribTexture);
    }

    /**
     * create program and load shaders, fragment shader is very important.
     */
    private int createProgram(String vertexSource, String fragmentSource) {
        // create shaders
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        // just check
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, pixelShader);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    /**
     * create shader with given source.
     */
    private int loadShader(int shaderType, String source)
    {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0)
            {
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    /**
     * these two buffers are used for holding vertices, screen vertices and texture vertices.
     */
    private void createBuffers(float[] vert, float[] coord)
    {
        mVertexVerticesBuffer = ByteBuffer.allocateDirect(vert.length * 4);
        mVertexVerticesBuffer.order(ByteOrder.nativeOrder());
        mVertexVerticesBuffer.asFloatBuffer().put(vert);
        mVertexVerticesBuffer.position(0);
        if (mTextureVerticesBuffer == null) {
            mTextureVerticesBuffer = ByteBuffer.allocateDirect(coord.length * 4);
            mTextureVerticesBuffer.order(ByteOrder.nativeOrder());
            mTextureVerticesBuffer.asFloatBuffer().put(coord);
            mTextureVerticesBuffer.position(0);
        }
    }

}


