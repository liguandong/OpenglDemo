package poco.cn.medialibs.glview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * Created by: fwc
 * Date: 2017/12/19
 */
public class GLTextureView extends TextureView implements TextureView.SurfaceTextureListener {

	private Renderer mRenderer;
	private RenderThread mRenderThread;
	private Handler mRenderHandler;

	private boolean mDetached;

	private boolean isPreserveEGLOnPause = true;

	public GLTextureView(Context context) {
		this(context, null);
	}

	public GLTextureView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setSurfaceTextureListener(this);
	}

	public void setRenderer(Renderer renderer) {
		if (renderer == null) {
			throw new IllegalArgumentException("renderer must not be null");
		}

		if (mRenderThread != null) {
			throw new IllegalStateException("setRenderer has already been called for this instance.");
		}

		mRenderer = renderer;
		mRenderThread = new RenderThread(renderer);
		mRenderThread.start();
		mRenderHandler = mRenderThread.getThreadHandler();
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		mRenderThread.onSurfaceCreated(surface);
		mRenderThread.onSurfaceChanged(width, height);
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		mRenderThread.onSurfaceChanged(width, height);
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		mRenderThread.onSurfaceDestroy();
		return true;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		if (mDetached && mRenderer != null) {
			mRenderThread = new RenderThread(mRenderer);
			mRenderThread.start();
			mRenderHandler = mRenderThread.getThreadHandler();
			mRenderThread.setPreserveEGLOnPause(isPreserveEGLOnPause);
		}
		mDetached = false;
	}

	@Override
	protected void onDetachedFromWindow() {
		if (mRenderThread != null) {
			mRenderThread.requestExitAndWait();
		}
		mDetached = true;
		super.onDetachedFromWindow();
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			if (mRenderThread != null) {
				mRenderThread.requestExitAndWait();
			}
		} finally {
			super.finalize();
		}
	}

	public void requestRender() {
		mRenderThread.requestRender();
	}

	public void setPreserveEGLOnPause(boolean preserve) {
		isPreserveEGLOnPause = preserve;
		if (mRenderThread != null) {
			mRenderThread.setPreserveEGLOnPause(preserve);
		}
	}

	public void queueEvent(Runnable r) {
		if (r == null) {
			throw new IllegalArgumentException("r must not be null");
		}
		if(mRenderThread.isAlive()){
			mRenderHandler.post(r);
		}
	}
}
