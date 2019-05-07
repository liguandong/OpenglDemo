package poco.cn.medialibs.player2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import poco.cn.medialibs.glview.GLTextureView;

/**
 * Created by: fwc
 * Date: 2018/12/6
 * 主要用于 Play Thread 和 GL Thread 通信
 */
public final class MultiSurface {

	@NonNull
	private final WeakReference<GLTextureView> mGLTextureView;

	@NonNull
	private final WeakReference<GLMultiSurface> mGLMultiSurface;

	private MultiSurface(@NonNull GLTextureView glTextureView, @NonNull GLMultiSurface surface) {
		mGLTextureView = new WeakReference<>(glTextureView);
		mGLMultiSurface = new WeakReference<>(surface);
	}

	public static MultiSurface create(@NonNull GLTextureView glTextureView, @NonNull GLMultiSurface surface) {
		return new MultiSurface(glTextureView, surface);
	}

	@Nullable
	public IPlayer.ISurface getCurSurface() {
		final GLMultiSurface glMultiSurface = mGLMultiSurface.get();
		if (glMultiSurface != null) {
			return glMultiSurface.getCurSurface();
		}
		return null;
	}

	@Nullable
	public IPlayer.ISurface getNextSurface() {
		final GLMultiSurface glMultiSurface = mGLMultiSurface.get();
		if (glMultiSurface != null) {
			return glMultiSurface.getNextSurface();
		}
		return null;
	}

	@Nullable
	public IPlayer.ISurface getNextNextSurface() {
		final GLMultiSurface glMultiSurface = mGLMultiSurface.get();
		if (glMultiSurface != null) {
			return glMultiSurface.getNextNextSurface();
		}
		return null;
	}

	public void onChangeSurface(final boolean isTransitionMode) {
		final GLTextureView glTextureView = mGLTextureView.get();
		if (glTextureView != null) {
			glTextureView.queueEvent(new Runnable() {
				@Override
				public void run() {
					final GLMultiSurface glMultiSurface = mGLMultiSurface.get();
					if (glMultiSurface != null) {
						glMultiSurface.onChangeSurface(isTransitionMode);
					}
				}
			});
		}
	}

	public void resetSurface() {
		final GLTextureView glTextureView = mGLTextureView.get();
		if (glTextureView != null) {
			glTextureView.queueEvent(new Runnable() {
				@Override
				public void run() {
					final GLMultiSurface glMultiSurface = mGLMultiSurface.get();
					if (glMultiSurface != null) {
						glMultiSurface.resetSurface();
					}
				}
			});
		}
	}
}
