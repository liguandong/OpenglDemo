package poco.cn.opengldemo.special;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import poco.cn.medialibs.gles.GlUtil;
import poco.cn.opengldemo.R;
import poco.cn.opengldemo.utils.GlMatrixTools;

/**
 * Created by lgd on 2019/5/23.
 */
public class MultiWindowRender implements GLSurfaceView.Renderer
{
    private Context context;
    private SquareWindowDraw squareWindowDraw;
    private CirCleWindowDraw cirCleWindowDraw;
    private Bitmap bitmap;
    private int mTextureId = GlUtil.NO_TEXTURE;

    private float[] mModelMatrix = new float[16];
    private float[] mTexMatrix = new float[16];
    private int mViewWidth;
    private int mViewHeight;
    private GlMatrixTools mMatrixTools;
    //临时矩阵
    private final float[] mTempModelMatrix = new float[16];
    private final float[] mTempTexMatrix = new float[16];

    private int type = 2;
    private RounderWindowDraw rounderWindowDraw;

    //    RenderInfo
    public MultiWindowRender(Context context)
    {
        this.context = context;
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.img16_9);
        mMatrixTools = new GlMatrixTools();
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        squareWindowDraw = new SquareWindowDraw(context);
        cirCleWindowDraw = new CirCleWindowDraw(context);
        rounderWindowDraw = new RounderWindowDraw(context);
        if (mTextureId == GlUtil.NO_TEXTURE && bitmap != null)
        {
            mTextureId = GlUtil.createTexture(bitmap);
        }

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        mViewWidth = width;
        mViewHeight = height;
        squareWindowDraw.setViewSize(mViewWidth, mViewHeight);
        cirCleWindowDraw.setViewSize(mViewWidth, mViewHeight);
        rounderWindowDraw.setViewSize(mViewWidth, mViewHeight);
        GLES20.glViewport(0, 0, mViewWidth, mViewHeight);
        float ratio = mViewWidth / (float) mViewHeight;
        if (ratio < 1)
        {
            mMatrixTools.ortho(-ratio, ratio, -1, 1, 3, 7);
        } else
        {
            mMatrixTools.ortho(-1, 1, -1 / ratio, 1 / ratio, 3, 7);
        }
        mMatrixTools.setCamera(0, 0, 3, 0, 0, 0, 0, 1, 0);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.setIdentityM(mTexMatrix, 0);
        // 翻转纹理
        mTexMatrix[5] = -1;
        mTexMatrix[13] = 1;
//        centerCrop();
        fixCenter();
//        if (imgRatio > ratio)
//        {
//            Matrix.scaleM(mModelMatrix, 0, imgRatio, 1, 0);
//        }
//
//        float sclae = Math.max()
    }

    @Override
    public void onDrawFrame(GL10 gl)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//        GLES20.glClearColor(0.2f, 0, 0, 1f);
        Matrix.setIdentityM(mTempModelMatrix, 0);
        Matrix.multiplyMM(mTempModelMatrix, 0, mModelMatrix, 0, mTempModelMatrix, 0);
        if(type == 1)
        {
            squareWindowDraw.draw(mTextureId, mTextureId, mMatrixTools.getFinalMatrix(mTempModelMatrix), mTexMatrix);
        }else if(type == 2){
//            cirCleWindowDraw.draw(mTextureId, mTextureId, mMatrixTools.getFinalMatrix(mTempModelMatrix), mTexMatrix);
//            cirCleWindowDraw.draw(mTextureId, mTextureId, mMatrixTools.getFinalMatrix(mTempModelMatrix), mTexMatrix);
            cirCleWindowDraw.draw(mTextureId, mTextureId, mMatrixTools.getFinalMatrix(mTempModelMatrix), mTexMatrix);
        }else{
            rounderWindowDraw.draw(mTextureId, mTextureId, mMatrixTools.getFinalMatrix(mTempModelMatrix), mTexMatrix);

        }
    }


    public void fixCenter()
    {
        Matrix.setIdentityM(mModelMatrix, 0);
        float ratio = mViewWidth / (float) mViewHeight;
        float imgRatio = bitmap.getWidth() / (float) bitmap.getHeight();
        Matrix.scaleM(mModelMatrix, 0, imgRatio, 1, 0);
        if (ratio > 1)
        {
            float scale = Math.min(1 / imgRatio, 1 / ratio);
            Matrix.scaleM(mModelMatrix, 0, scale, scale, 0);
        } else
        {
            float scale = Math.min(ratio / imgRatio, 1 / ratio);
            Matrix.scaleM(mModelMatrix, 0, scale, scale, 0);
        }
    }

    public void centerCrop()
    {
        Matrix.setIdentityM(mModelMatrix, 0);
        float ratio = mViewWidth / (float) mViewHeight;
        float imgRatio = bitmap.getWidth() / (float) bitmap.getHeight();
        Matrix.scaleM(mModelMatrix, 0, imgRatio, 1, 0);
        if (ratio > 1)
        {
            float scale = Math.max(1 / imgRatio, 1 / ratio);
            Matrix.scaleM(mModelMatrix, 0, scale, scale, 0);
        } else
        {
            float scale = Math.max(ratio / imgRatio, 1 / ratio);
            Matrix.scaleM(mModelMatrix, 0, scale, scale, 0);
        }
    }

    public void setBitmap(Bitmap bitmap)
    {
        this.bitmap = bitmap;
        if (mTextureId != GlUtil.NO_TEXTURE)
        {

        }
        mTextureId = GlUtil.NO_TEXTURE;
        if (mTextureId == GlUtil.NO_TEXTURE && bitmap != null)
        {
            mTextureId = GlUtil.createTexture(bitmap);
        }
//        fixCenter();
        centerCrop();
    }

    public void setBitmap2(Bitmap bitmap)
    {

    }

    public void setScale(float scaleX, float scaleY)
    {
        squareWindowDraw.setScale(scaleX, scaleY);
        cirCleWindowDraw.setScale(scaleX, scaleY);
        rounderWindowDraw.setScale(scaleX, scaleY);
    }

    public void setType(int i)
    {
        type = i;
    }

    public void setRadius(float r)
    {
        rounderWindowDraw.setRadius(r);
    }
}
