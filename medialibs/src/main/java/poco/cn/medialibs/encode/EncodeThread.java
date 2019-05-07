package poco.cn.medialibs.encode;

import android.view.Surface;

import java.io.IOException;

/**
 * Created by: fwc
 * Date: 2017/9/21
 */
public class EncodeThread implements Runnable {

	private VideoEncoderCore mEncoderCore;

	private boolean mShouldExit = false;
	private volatile boolean mExited = false;

	private boolean mRequestEncode = false;

	private final Object mLock = new Object();

	private volatile boolean isError = false;

	public EncodeThread(VideoEncoderCore.EncodeConfig config) throws IOException {
		mEncoderCore = new VideoEncoderCore(config);
	}

	public Surface getEncodeSurface() {
		return mEncoderCore.getInputSurface();
	}

	@Override
	public void run() {

		Thread.currentThread().setName("EncodeThread");

		try {
			guardedRun();
		} catch (Exception e) {
			e.printStackTrace();
			isError = true;
		} finally {
			synchronized (mLock) {
				mExited = true;
				mLock.notifyAll();
			}
		}
	}

	private void guardedRun() throws InterruptedException {

		try {
			while (true) {
				synchronized (mLock) {
					while (true) {
						if (mShouldExit) {
							return;
						}

						if (mRequestEncode) {
							mRequestEncode = false;
							break;
						}

						mLock.wait();
					}
				}
				mEncoderCore.drainEncoder(false);
			}
		} finally {
			mEncoderCore.drainEncoder(true);
			mEncoderCore.release();
		}
	}

	public void encode() {
		synchronized (mLock) {
			mRequestEncode = true;
			mLock.notifyAll();
		}
	}

	public void finish() {
		synchronized (mLock) {
			mShouldExit = true;
			mLock.notifyAll();
		}
	}

	@SuppressWarnings("all")
	public boolean waitForFinish() {

		finish();

		// 等待退出
		while (!mExited) {}
		return !isError;
	}
}
