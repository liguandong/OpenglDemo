package poco.cn.medialibs.glview;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;

import java.lang.ref.WeakReference;

import poco.cn.medialibs.gles.EglCore;
import poco.cn.medialibs.gles.WindowSurface;

/**
 * Created by: fwc
 * Date: 2018/5/11
 */
public class RenderThread extends Thread {

	private static final int MSG_SURFACE_CREATED = 1;
	private static final int MSG_SURFACE_CHANGED = 2;
	private static final int MSG_REQUEST_RENDER = 3;
	private static final int MSG_SURFACE_DESTROY = 4;
	private static final int MSG_RELEASE = 5;

	private int mPriority;
	private RenderHandler mRenderHandler;

	@NonNull
	private final WeakReference<Renderer> mRenderer;

	private volatile boolean isQuit;

	RenderThread(Renderer renderer) {
		super("Render Thread");

		mRenderer = new WeakReference<>(renderer);

		mPriority = Process.THREAD_PRIORITY_DEFAULT;
	}

	@Override
	public void run() {
		isQuit = false;
		Looper.prepare();
		synchronized (this) {
			mRenderHandler = new RenderHandler(mRenderer);
			notifyAll();
		}
		Process.setThreadPriority(mPriority);
		Looper.loop();
		isQuit = true;
	}

	@NonNull
	Handler getThreadHandler() {
		synchronized (this) {
			while (mRenderHandler == null) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return mRenderHandler;
	}

	public void onSurfaceCreated(Object surface) {
		if (!isQuit) {
			Message.obtain(mRenderHandler, MSG_SURFACE_CREATED, surface).sendToTarget();
		}
	}

	public void onSurfaceChanged(int width, int height) {
		if (!isQuit) {
			Message.obtain(mRenderHandler, MSG_SURFACE_CHANGED, width, height).sendToTarget();
		}
	}

	public void requestRender() {
		if (Thread.currentThread() == this) {
			mRenderHandler.requestRender();
		} else {
			mRenderHandler.removeMessages(MSG_REQUEST_RENDER);
			if (!isQuit) {
				Message.obtain(mRenderHandler, MSG_REQUEST_RENDER).sendToTarget();
			}
		}
	}

//	public void queueEvent(Runnable r) {
//		if (r == null) {
//			throw new IllegalArgumentException("r must not be null");
//		}
//		mRenderHandler.post(r);
//	}

	void onSurfaceDestroy() {
		if (!isQuit) {
			Message.obtain(mRenderHandler, MSG_SURFACE_DESTROY).sendToTarget();
		}
	}

	void requestExitAndWait() {
		if (!isQuit) {
			Message.obtain(mRenderHandler, MSG_RELEASE).sendToTarget();
		}
	}

	void setPreserveEGLOnPause(boolean preserve) {
		if (mRenderHandler != null) {
			mRenderHandler.isPreserveEGLOnPause = preserve;
		}
	}

	public static class RenderHandler extends Handler {

		@NonNull
		private final WeakReference<Renderer> mRenderer;

		@Nullable
		private EglCore mEglCore;

		@Nullable
		private WindowSurface mWindowSurface;

		private boolean mShouldCallDestroy;

		private int mWidth;
		private int mHeight;

		private boolean mWaitingSurface;

		private volatile boolean isPreserveEGLOnPause = true;

		private boolean mPendingRender;

		private RenderHandler(@NonNull WeakReference<Renderer> renderer) {
			mRenderer = renderer;
			mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_SURFACE_CREATED:
					onSurfaceCreated(msg.obj);
					break;
				case MSG_SURFACE_CHANGED:
					onSurfaceChanged(msg.arg1, msg.arg2);
					break;
				case MSG_REQUEST_RENDER:
					requestRender();
					break;
				case MSG_SURFACE_DESTROY:
					onSurfaceDestroy();
					break;
				case MSG_RELEASE:
					onRelease();
					break;
			}
		}

		private void onSurfaceCreated(Object surface) {
			if (mWindowSurface != null) {
				throw new IllegalStateException("mWindowSurface is not null.");
			}

			if (mEglCore == null) { // Recreate EGL
				mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
			}

			if (surface instanceof SurfaceTexture) {
				mWindowSurface = new WindowSurface(mEglCore, (SurfaceTexture)surface);
			} else if (surface instanceof Surface) {
				Surface s = (Surface)surface;
				if (!s.isValid()) {
					return;
				}
				mWindowSurface = new WindowSurface(mEglCore, (Surface)surface, false);
			} else {
				throw new RuntimeException();
			}

			mWindowSurface.makeCurrent();
			Renderer renderer = mRenderer.get();
			if (renderer != null && !mWaitingSurface) {
				renderer.onSurfaceCreated();
				mShouldCallDestroy = true;
			}
		}

		private void onSurfaceChanged(int width, int height) {
			if (mWindowSurface == null) {
				return;
//				throw new IllegalStateException("mWindowSurface is null.");
			}

			mWidth = width;
			mHeight = height;
			Renderer renderer = mRenderer.get();
			if (renderer != null && !mWaitingSurface) {
				renderer.onSurfaceChanged(mWidth, mHeight);
			}

			final boolean doRender = mWaitingSurface || mPendingRender;
			mWaitingSurface = false;
			if (doRender) {
				requestRender();
			}
		}

		private void requestRender() {
			mPendingRender = false;
			Renderer renderer = mRenderer.get();
			if (renderer != null && canRender()) {
				renderer.onDrawFrame();
				if (mWindowSurface != null) {
					mWindowSurface.swapBuffers();
				}
			} else {
				mPendingRender = true;
			}
		}

		private boolean canRender() {
			return mWindowSurface != null && mWidth > 0 && mHeight > 0;
		}

		private void onSurfaceDestroy() {
			if (isPreserveEGLOnPause) {
				releaseSurface();
				mWaitingSurface = true;
			} else {
				// Meizu M5 Note 一定要释放 EGL 上下文
				Renderer renderer = mRenderer.get();
				if (renderer != null && mShouldCallDestroy) {
					mShouldCallDestroy = false;
					renderer.onSurfaceDestroyed();
				}
				releaseSurface();
				mWaitingSurface = false;
				releaseGL();
			}
		}

		private void onRelease() {
			removeCallbacksAndMessages(null);

			Renderer renderer = mRenderer.get();
			if (renderer != null && mShouldCallDestroy) {
				mShouldCallDestroy = false;
				renderer.onSurfaceDestroyed();
			}
			releaseSurface();
			releaseGL();

			quitSafely();
		}

		private void releaseSurface() {
			if (mWindowSurface != null) {
				mWindowSurface.release();
				mWindowSurface = null;
			}
			mWidth = 0;
			mHeight = 0;
			mPendingRender = false;
		}

		private void releaseGL() {
			if (mEglCore != null) {
				mEglCore.release();
				mEglCore = null;
			}
		}

		private void quitSafely() {
			Looper looper = Looper.myLooper();
			if (looper != null) {
				looper.quitSafely();
			}
		}
	}
}
