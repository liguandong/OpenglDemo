package poco.cn.opengldemo.video.draw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import androidx.annotation.NonNull;
import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import poco.cn.medialibs.gles.BufferPool;
import poco.cn.medialibs.gles.OffscreenBuffer;
import poco.cn.opengldemo.R;

/**
 * Created by lgd on 2019/4/19.
 */
public class RenderManager
{
    private Context mContext;

    private int mWidth;

    private int mHeight;

    /**
     * FrameBuffer 对象池，用于复用 FrameBuffer 对象
     */
    private BufferPool mBufferPool;


    /**
     * 记录当前在使用的 FrameBuffer
     */
    private SparseArray<OffscreenBuffer> mDrawBuffers = new SparseArray<>();


    private YUVFrame mYUVFrame;

    private NoneDraw mNoneDraw;
    private WatermarkFilter mVideoLogo;
    private Bitmap mLogo;
    private boolean mBlenEnable;

    public RenderManager(Context context)
    {
        mContext = context;
        mYUVFrame = new YUVFrame(mContext);
        mNoneDraw = new NoneDraw(mContext);
        mVideoLogo = new WatermarkFilter(mContext);
        mLogo = BitmapFactory.decodeResource(context.getResources(), R.drawable.interphpoto_logo_1);
        mVideoLogo.changeBitmap(mLogo);

    }

    public void setSurfaceSize(int width,int height)
    {
        if(mWidth == width && mHeight == height){
            return;
        }
        mWidth = width;
        mHeight = height;
        mBufferPool = new BufferPool(width, height, 3);

        float scaleRatio = 1f;
        if ((float)mWidth / mHeight == 9f / 16) {
            scaleRatio = 0.8f;
        } else if (mWidth == mHeight) {
            scaleRatio = 1.4f;
        }
        int size = Math.max(mWidth, mHeight);
        float scale = size / 1920f; // 1080; // 0.5625     200 / 1080/1.4 *0.5625
        float scaleX = (float)mLogo.getWidth() / mWidth * scaleRatio * scale;
        float scaleY = (float)mLogo.getHeight() / mHeight * scaleRatio * scale;
        float x = 1 - scaleX - 0.0145f;
        float y = scaleY - 1 + 0.018f * mWidth / mHeight;
        mVideoLogo.calculatePosition(x, y, scaleX, scaleY);

    }

    /**
     * 根据纹理 id 和相应的矩阵绘制视频帧
     * @param textureId OES 纹理 id
     * @param mvpMatrix model matrix
     * @param texMatrix texture matrix
     * @return 返回渲染后的纹理 id
     */
    public int drawFrame(int textureId, float[] mvpMatrix, float[] texMatrix) {
        OffscreenBuffer buffer = getBuffer();

        buffer.bind();
        mYUVFrame.drawFrame(textureId, mvpMatrix, texMatrix);

        // 从帧缓冲区中读取数据生成 Bitmap
        ByteBuffer buf = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
        buf.rewind();
        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buf);
//
        buffer.unbind();

        return buffer.getTextureId();
    }

    @NonNull
    private OffscreenBuffer getBuffer() {
        OffscreenBuffer buffer = mBufferPool.obtain();
        mDrawBuffers.put(buffer.getTextureId(), buffer);
        return buffer;
    }
    public void resetBuffer(int textureId) {
        OffscreenBuffer buffer = mDrawBuffers.get(textureId);
        if (buffer != null) {
            buffer.recycle();
            mDrawBuffers.remove(textureId);
        }
    }

    public void release()
    {


    }

    public void drawWater()
    {
        blenEnable(true);
        mVideoLogo.draw();
        blenEnable(false);
    }

    public void blenEnable(boolean enable)
    {
        if(enable == mBlenEnable){
            return;
        }
        mBlenEnable = enable;
        if(enable){
            GLES20.glDepthMask(false);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);
//            GLES20.glBlendFuncSeparate(GLES20.GL_ONE,GLES20.GL_ONE_MINUS_SRC_ALPHA,GLES20.GL_ONE,GLES20.GL_ONE);
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        }else{
            GLES20.glDisable(GLES20.GL_BLEND);
            GLES20.glDepthMask(true);
        }
    }

    public void drawTexture(int curTextureId)
    {
        mNoneDraw.draw(curTextureId);
        resetBuffer(curTextureId);
    }

    public void drawTransition(AbsTransition transition, int curTextureId,
                               int nextTextureId, int x, int y,
                               int width, int height) {
//        transition.setBlur(getBlur(transition.getBlurType()));
        transition.setViewport(x, y, width, height);
        transition.draw(curTextureId, nextTextureId);

        resetBuffer(curTextureId);
        resetBuffer(nextTextureId);
    }

    /**
     * 绘制转场
     * @param transition    转场的 Filter
     * @param curTextureId  第一个视频纹理 id
     * @param nextTextureId 第二个视频纹理 id
     * @param x             可视区域的左上点的 x 坐标
     * @param y             可视区域的左上点的 y 坐标
     * @param width         可视区域的宽度
     * @param height        可视区域的高度
     */
//    public void drawTransition(AbsTransition transition, int curTextureId,
//                               int nextTextureId, int x, int y,
//                               int width, int height) {
//        transition.setBlur(getBlur(transition.getBlurType()));
//        transition.setViewport(x, y, width, height);
//        transition.draw(curTextureId, nextTextureId);
//
//        resetBuffer(curTextureId);
//        resetBuffer(nextTextureId);
//    }

}
