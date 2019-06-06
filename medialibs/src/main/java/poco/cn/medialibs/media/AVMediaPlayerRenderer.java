package poco.cn.medialibs.media;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.Surface;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import poco.cn.medialibs.media.avmediaplayer.AVMediaPlayer;

public class AVMediaPlayerRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener
{
    private static final String vertexShaderString =
                "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uSTMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main() {\n" +
                        "gl_Position = uMVPMatrix * aPosition;\n" +
                        "vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                "}";

    private static final String fragmentShaderString =
                "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                    "gl_FragColor=texture2D(sTexture, vTextureCoord);\n" +
                "}";

    private GLSurfaceView mSurfaceView;                                                              //外部传入的GLSurfaceView
    private int mProgram = 0;                                                                                    //Program着色器程序的id
    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;
    private int mTextureID;
    private SurfaceTexture mSurfaceTexture;
    private float[] mSTMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private Drawable2d mDrawable2d = new Drawable2d();
    private AVMediaPlayer mAVMeidaPlayer;
    private AVMediaPlayer.OnPlayStatusListener mOnPlayStatusListener;
    private Surface mSurface;
    private String mFile;
    private boolean mRotate;

    //构造方法
    public AVMediaPlayerRenderer(GLSurfaceView glSurfaceView, boolean output)
    {
        mSurfaceView = glSurfaceView;
        mAVMeidaPlayer = new AVMediaPlayer(true, output);
        mAVMeidaPlayer.setOnPlayStatusListener(new AVMediaPlayer.OnPlayStatusListener() {
            @Override
            public void onCompletion(AVMediaPlayer player) {
                if(mOnPlayStatusListener != null)
                {
                    mOnPlayStatusListener.onCompletion(player);
                }
            }

            @Override
            public void onVideoChanged(AVMediaPlayer player, int index, String file, AVInfo info, boolean rotate) {
                mRotate = rotate;
                updateDisplay();
                if(mOnPlayStatusListener != null)
                {
                    mOnPlayStatusListener.onVideoChanged(player, index, file, info, rotate);
                }
            }
        });
    }

    public void setOnPlayStatusListener(AVMediaPlayer.OnPlayStatusListener l)
    {
        mOnPlayStatusListener = l;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig)
    {
        //这个必须在onSurfaceCreated中，否则失败
        //初始化着色器，类似于告GPU当传进去数据的时候采用什么样的规则。
        initShaders();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height)
    {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10)
    {
        //Log.d("hwq", "onDrawFrame");
        displayImage();
    }

    public void updateDisplay()
    {
        float screenRatio = (float)mSurfaceView.getWidth()/mSurfaceView.getHeight();
        float videoRatio = (float)mAVMeidaPlayer.getVideoWidth()/mAVMeidaPlayer.getVideoHeight();
        if(mAVMeidaPlayer.getRotation() % 180 == 90)
        {
            videoRatio = 1 / videoRatio;
        }
        if (videoRatio > screenRatio){
            Matrix.orthoM(mMVPMatrix,0,-1f,1f,-videoRatio/screenRatio,videoRatio/screenRatio,-1f,1f);
        }
        else {
            Matrix.orthoM(mMVPMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1f, 1f, -1f, 1f);
        }
        if(mRotate) {
            Matrix.rotateM(mMVPMatrix, 0, -mAVMeidaPlayer.getRotation(), 0, 0, 1);
        }
    }

    public AVMediaPlayer getPlayer()
    {
        return mAVMeidaPlayer;
    }

    //初始化着色器，类似于告GPU当传进去数据的时候采用什么样的规则。
    private void initShaders()
    {
        mProgram = createProgram(vertexShaderString, fragmentShaderString);
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureID = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mSurface = new Surface(mSurfaceTexture);
        mAVMeidaPlayer.setSurface(mSurface);
        return;
    }

    public void setVideoSource(final String[] files)
    {
        mAVMeidaPlayer.setVideoSource(files);
        mAVMeidaPlayer.prepareAsync();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300);
                }
                catch(Exception e)
                {}
                mAVMeidaPlayer.setVideoSource(files);
                mAVMeidaPlayer.setSurface(mSurface);
                mAVMeidaPlayer.prepare();
            }
        }).start();
    }

    public void release()
    {
        mAVMeidaPlayer.release();
        mSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                GLES20.glDeleteProgram(mProgram);
            }
        });
    }

    private void displayImage()
    {
        GLES20.glUseProgram(mProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);

        //Clear
        //清除颜色设为黑色，把整个窗口清除为当前的清除颜色，glClear（）的唯一参数表示需要被清除的缓冲区。
        GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT| GLES20.GL_COLOR_BUFFER_BIT);

        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mSTMatrix);

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glVertexAttribPointer(maPositionHandle, mDrawable2d.getCoordsPerVertex(), GLES20.GL_FLOAT, false, mDrawable2d.getVertexStride(), mDrawable2d.getVertexArray());

        GLES20.glEnableVertexAttribArray(maTextureHandle);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false, mDrawable2d.getTexCoordStride(), mDrawable2d.getTexCoordArray());

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mDrawable2d.getVertexCount());

        GLES20.glFinish();

        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maTextureHandle);
        GLES20.glUseProgram(0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
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

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //Log.d("hwq", "onFrameAvailable");
        mSurfaceView.requestRender();
    }
}



