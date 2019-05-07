package poco.cn.medialibs.glview;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by: fwc
 * Date: 2018/5/11
 */
public class GLSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

	private Renderer mRenderer;
	private RenderThread mRenderThread;

	private boolean mDetached;

	private Handler mRenderHandler;

	private boolean isPreserveEGLOnPause = true;

	public GLSurfaceView(Context context) {
		this(context, null);
	}

	public GLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
	}

	public void setRenderer(Renderer renderer) {
		if (renderer == null) {
			throw new IllegalArgumentException("renderer must not be null");
		}

		if (mRenderThread != null) {
			throw new IllegalStateException("setRenderer has already been called for this instance.");
		}

		mRenderer = renderer;
		mRenderThread = new RenderThread(mRenderer);
		mRenderThread.start();
		mRenderHandler = mRenderThread.getThreadHandler();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mRenderThread.onSurfaceCreated(holder.getSurface());
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		mRenderThread.onSurfaceChanged(width, height);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mRenderThread.onSurfaceDestroy();
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
		mRenderHandler.post(r);
	}
}
