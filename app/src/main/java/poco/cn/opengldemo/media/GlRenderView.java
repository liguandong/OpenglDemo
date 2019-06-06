package poco.cn.opengldemo.media;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import poco.cn.medialibs.glview.GLTextureView;
import poco.cn.medialibs.glview.Renderer;
import poco.cn.medialibs.player2.GLSurface;
import poco.cn.opengldemo.utils.GlMatrixTools;
import poco.cn.opengldemo.video.view.VideoBaseInfo;

/**
 * Created by lgd on 2019/6/6.
 */
public class GlRenderView extends FrameLayout
{
    private GLTextureView glTextureView;
    private MyRenderer renderer;
    private MediaPlayer mediaPlayer;
    private GLSurface glSurface;
    private final VideoBaseInfo info1;
    private final VideoBaseInfo info16;
    private final VideoBaseInfo info19;
    private final VideoBaseInfo curInfo;


    public GlRenderView(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
//        setBackgroundColor(Color.GRAY);
        LayoutParams fl = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        glTextureView = new GLTextureView(context);
        glTextureView.setOpaque(false);
        addView(glTextureView, fl);

        renderer = new MyRenderer();
        glTextureView.setRenderer(renderer);

        mediaPlayer = new MediaPlayer();
        info1 = VideoBaseInfo.get("/storage/emulated/0/DCIM/InterPhoto/InterPhoto_1556093181910.mp4");
        info16 = VideoBaseInfo.get("/storage/emulated/0/DCIM/InterPhoto/InterPhoto_1556093222209.mp4");
        info19 = VideoBaseInfo.get("/storage/emulated/0/DCIM/InterPhoto/InterPhoto_1556093247104.mp4");
//        mediaPlayer.setDataSource();
        curInfo = info1;
    }

    public void setF(float f)
    {
        renderer.diagonalDraw.setDiagonal(f);
        glTextureView.requestRender();
    }


    class MyRenderer implements Renderer
    {
        private GlMatrixTools mMatrixTools;
        public DiagonalDraw diagonalDraw;

        public MyRenderer()
        {
            mMatrixTools = new GlMatrixTools();
        }

        @Override
        public void onSurfaceCreated()
        {
            glSurface = new GLSurface();
            glSurface.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener()
            {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture)
                {
                    glTextureView.requestRender();
                }
            });
            post(new Runnable()
            {
                @Override
                public void run()
                {
                    mediaPlayer.setSurface(glSurface.getSurface());
                    try
                    {
                        mediaPlayer.setDataSource(curInfo.path);
                        mediaPlayer.setLooping(true);
                        mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener()
                        {
                            @Override
                            public boolean onInfo(MediaPlayer mp, int what, int extra)
                            {
                                return false;
                            }
                        });
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            });

            diagonalDraw = new DiagonalDraw(getContext());
        }

        @Override
        public void onSurfaceChanged(int width, int height)
        {
            float ratio = width / (float) height;
            float videoRatio = curInfo.width / (float) curInfo.height;
            GLES20.glViewport(0, 0, width, height);
            diagonalDraw.setViewSize(width,height);
            if (videoRatio > ratio)
            {
                mMatrixTools.frustum(-ratio /videoRatio ,ratio  /videoRatio,-1,1, 3, 7);
            } else
            {
                mMatrixTools.frustum(-1, 1,-videoRatio / ratio, videoRatio / ratio, 3, 7);
            }
            mMatrixTools.setCamera(0, 0, 3, 0, 0, 0, 0, 1, 0);

        }

        @Override
        public void onDrawFrame()
        {
//            diagonalDraw.draw(glSurface.getTextureId(),mMatrixTools.getFinalMatrix(), glSurface.getTransformMatrix());
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//            GLES20.glClearColor(1, 1, 0, 1f);
            diagonalDraw.draw(glSurface.getTextureId(),mMatrixTools.getFinalMatrix(),glSurface.getTransformMatrix());
            glSurface.updateTexImage();
        }

        @Override
        public void onSurfaceDestroyed()
        {
            mediaPlayer.pause();
            mediaPlayer.release();
        }
    }

}
