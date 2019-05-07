package poco.cn.opengldemo.video.view;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.SparseArray;

import java.util.List;

import androidx.annotation.NonNull;
import poco.cn.medialibs.glview.GLTextureView;
import poco.cn.medialibs.player2.GLMultiSurface;
import poco.cn.medialibs.player2.PlayInfo;
import poco.cn.opengldemo.video.draw.PlayRender;
import poco.cn.opengldemo.video.draw.RenderInfo;

/**
 * Created by lgd on 2019/4/19.
 */
public class RenderView extends GLTextureView
{
    @NonNull
    private final Context mContext;

    @NonNull
    private final Handler mHandler;

    @NonNull
    final PlayRender mPlayRenderer;
    private OnSurfaceListener mOnSurfaceListener;

    public RenderView(Context context)
    {
        this(context, null);
    }

    public RenderView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mPlayRenderer = new PlayRender(context);
        mPlayRenderer.setOnRenderListener(mOnRenderListener);

        setRenderer(mPlayRenderer);
    }

    private GLMultiSurface.OnFrameAvailableListener mOnFrameAvailableListener = new GLMultiSurface.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(GLMultiSurface surface) {
            requestRender();
        }
    };


    public void setOnSurfaceListener(OnSurfaceListener listener)
    {
        mOnSurfaceListener = listener;
    }

    PlayRender.OnRenderListener mOnRenderListener = new PlayRender.OnRenderListener()
    {
        @Override
        public void onSurfaceCreated(@NonNull final GLMultiSurface glMultiSurface)
        {
            glMultiSurface.setOnFrameAvailableListener(mOnFrameAvailableListener);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mOnSurfaceListener != null) {
                        mOnSurfaceListener.onSurfaceCreated(glMultiSurface);
                    }
                }
            });
        }

        @Override
        public void onSurfaceChanged(final int width, final int height)
        {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mOnSurfaceListener != null) {
                        mOnSurfaceListener.onSurfaceChanged(width, height);
                    }
                }
            });
        }

        @Override
        public void onSurfaceDestroyed()
        {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mOnSurfaceListener != null) {
                        mOnSurfaceListener.onSurfaceDestroyed();
                    }
                }
            });
        }
    };

    /**
     * 设置 RenderInfo 渲染信息数组
     */
    public void setRenderInfoArray(@NonNull List<VideoPlayInfo> videoInfos, @NonNull List<PlayInfo> playInfos) {
        final int size = videoInfos.size();
        final SparseArray<RenderInfo> sparseArray = new SparseArray<>(size);
        VideoPlayInfo info;
        for (int i = 0; i < size; i++) {
            info = videoInfos.get(i);
            sparseArray.put(info.id, info.getRenderInfo(playInfos.get(i)));
        }
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mPlayRenderer.setRenderInfoArray(sparseArray);
            }
        });
    }

    public void release()
    {
        mOnRenderListener = null;
        mOnSurfaceListener = null;
        mPlayRenderer.release();
    }

    /**
     * 更换画幅时调用，用于更新 Surface 显示的信息
     * @param playRatio   视频画幅
     * @param leftMargin  显示 Surface 距离左边的距离
     * @param leftRatio   占整个 Surface 大小的比例
     * @param topMargin   显示 Surface 距离上面的距离
     * @param topRatio    占整个 Surface 大小的比例
     */
    public void setLeftAndTop(final int playRatio, final int leftMargin, final float leftRatio,
                              final int topMargin,  final float topRatio) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mPlayRenderer.setLeftAndTop(playRatio, leftMargin, leftRatio, topMargin, topRatio);
            }
        });
    }

    public interface OnSurfaceListener
    {

        void onSurfaceCreated(@NonNull GLMultiSurface glMultiSurface);

        void onSurfaceChanged(int width, int height);

        void onSurfaceDestroyed();
    }
}
