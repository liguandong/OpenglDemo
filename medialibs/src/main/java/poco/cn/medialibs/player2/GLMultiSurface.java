package poco.cn.medialibs.player2;

import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by: fwc
 * Date: 2018/12/6
 */
public final class GLMultiSurface implements SurfaceTexture.OnFrameAvailableListener {

	@NonNull
	private GLSurface mCurSurface;

	@NonNull
	private GLSurface mNextSurface;

	@NonNull
	private GLSurface mNextNextSurface;

	@Nullable
	private volatile OnFrameAvailableListener mListener;

	private boolean isChangingSurface;

	@Nullable
	private OnSurfaceChangeListener mOnSurfaceChangeListener;

	public GLMultiSurface(@NonNull OnSurfaceChangeListener listener) {

		mOnSurfaceChangeListener = listener;

		mCurSurface = new GLSurface();
		mCurSurface.setOnFrameAvailableListener(this);

		mNextSurface = new GLSurface();
		mNextSurface.setOnFrameAvailableListener(this);

		mNextNextSurface = new GLSurface();
		mNextNextSurface.setOnFrameAvailableListener(this);
	}

	public void updateTexImage() {
		try {
			mCurSurface.updateTexImage();
			mNextSurface.updateTexImage();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onFrameAvailable(SurfaceTexture surfaceTexture) {
		final OnFrameAvailableListener listener = mListener;
		if (listener != null) {
			listener.onFrameAvailable(GLMultiSurface.this);
		}
	}

	@NonNull
	public GLSurface getCurSurface() {
		if (isChangingSurface) {
			return mNextSurface;
		}
		return mCurSurface;
	}

	@NonNull
	public GLSurface getNextSurface() {
		if (isChangingSurface) {
			return mNextNextSurface;
		}
		return mNextSurface;
	}

	@NonNull
	public GLSurface getNextNextSurface() {
		if (isChangingSurface) {
			return mCurSurface;
		}
		return mNextNextSurface;
	}

	public void onChangeSurface(boolean isTransitionMode) {
		// GL Thread
		isChangingSurface = true;

		if (!isTransitionMode) {

			final GLSurface tempSurface = mCurSurface;
			tempSurface.reset();
			mCurSurface = mNextSurface;
			mNextSurface = mNextNextSurface;
			mNextNextSurface = tempSurface;
		}

		isChangingSurface = false;

		if (mOnSurfaceChangeListener != null) {
			mOnSurfaceChangeListener.onChange(isTransitionMode);
		}
	}

	public void resetWrapMatrix() {
		mCurSurface.resetWrapMatrix();
		mNextSurface.resetWrapMatrix();
		mNextNextSurface.resetWrapMatrix();
	}

	public void resetSurface() {
		// GL Thread
		if (mOnSurfaceChangeListener != null) {
			mOnSurfaceChangeListener.onChange(false);
		}
	}

	public void release() {
		// GL Thread
		mListener = null;
		mOnSurfaceChangeListener = null;
		mCurSurface.release();
		mNextSurface.release();
		mNextNextSurface.release();
		mListener = null;
	}

	public void setOnFrameAvailableListener(OnFrameAvailableListener listener) {
		mListener = listener;
	}

	public interface OnFrameAvailableListener {
		void onFrameAvailable(GLMultiSurface surface);
	}

	public interface OnSurfaceChangeListener {
		void onChange(boolean isTransitionMode);
	}
}
