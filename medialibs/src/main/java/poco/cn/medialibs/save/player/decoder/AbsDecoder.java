package poco.cn.medialibs.save.player.decoder;


import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import poco.cn.medialibs.media.AVFrameInfo;
import poco.cn.medialibs.media.AVInfo;
import poco.cn.medialibs.media.AVNative;
import poco.cn.medialibs.media.AVUtils;
import poco.cn.medialibs.media.AVVideoDecoder;

/**
 * Created by: fwc
 * Date: 2018/10/19
 */
public abstract class AbsDecoder implements Runnable {

	private static final int TIMEOUT = 10000;

	private static final int FRAME_INTERVAL = 1000 / 35;

	private static final BufferInfo FINISH_INFO = new BufferInfo(0);

	private AVVideoDecoder mDecoder;

	private String mVideoPath;

	private final ArrayBlockingQueue<BufferInfo> mQueue;
	private final ArrayBlockingQueue<BufferInfo> mPool;

	private volatile boolean isDecodeFinish;

	private int mLastPts = -FRAME_INTERVAL;

	private volatile boolean isStop;

	/**
	 * 视频时长，单位毫秒
	 */
	private long mDuration;

	private final int mPixelFormat;

	AbsDecoder(int bufferSize, int pixelFormat) {
		mQueue = new ArrayBlockingQueue<>(bufferSize);
		mPool = new ArrayBlockingQueue<>(bufferSize);

		mPixelFormat = pixelFormat;
	}

	public void setDataSource(String videoPath) {
		File file = new File(videoPath);
		if (!file.exists() || !file.isFile()) {
			throw new IllegalArgumentException("the file is not exist");
		}

		if (!file.canRead()) {
			throw new RuntimeException("the file can not read");
		}

		mVideoPath = videoPath;
	}

	private int mAddErrorFrameCount;
	private static final int TIME_ERROR_STEP = 5;

	public void setDuration(long duration) {
		mDuration = duration;
		AVInfo avInfo = new AVInfo();
		boolean success = AVUtils.avInfo(mVideoPath, avInfo, false);
		if (success) {
			long temp = duration - avInfo.videoDuration;
			if (temp > FRAME_INTERVAL) {
				while (temp > 0) {
					mAddErrorFrameCount++;
					temp -= TIME_ERROR_STEP;
				}
			}
		}
	}

	public BufferInfo getBufferInfo() throws InterruptedException {
		if (isStop || (isDecodeFinish && mQueue.isEmpty())) {
			return null;
		}

		BufferInfo info = mQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
		return info == FINISH_INFO ? null : info;
	}

	public void recycle(BufferInfo info) {
		if (!isDecodeFinish && info != null) {
			mPool.offer(info);
		}
	}

	public void stop() {
		isStop = true;
	}

	public void release() {
		isDecodeFinish = true;
	}

	@Override
	public void run() {
		if (mVideoPath == null) {
			throw new IllegalStateException();
		}

		prepare();

		try {
			guardedRun();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			if (mDecoder != null) {
				mDecoder.release();
				mDecoder = null;
			}
			mQueue.clear();
			mPool.clear();
		}
	}

	private void prepare() {
		isDecodeFinish = false;

		mDecoder = AVVideoDecoder.build(false);
		mDecoder.create(mVideoPath, AVNative.DATA_FMT_ARRAY_BYTE, mPixelFormat);
		mDecoder.setDataReusable(true);
	}

	private void guardedRun() throws InterruptedException {

		Object data;
		int ptsDelta;
		AVFrameInfo info = new AVFrameInfo();
		int timestamp;
		int startPosition = -1;
		int addTimeError = 0;
		while (!isStop && !isDecodeFinish) {
			data = mDecoder.nextFrame(info);
			if (!(data instanceof byte[])) {
				break;
			}
			if (mAddErrorFrameCount > 0) {
				mAddErrorFrameCount--;
				addTimeError += TIME_ERROR_STEP;
			}
			timestamp = info.pts + addTimeError;
			if (timestamp > mDuration) {
				break;
			}
			if (startPosition == -1) {
				startPosition = timestamp;
			}
			timestamp -= startPosition;

			ptsDelta = timestamp - mLastPts;
			if (ptsDelta >= FRAME_INTERVAL && ptsDelta < 1000) {
				putBuffer((byte[])data, timestamp, info.width, info.height);
				mLastPts = timestamp;
			}
		}

		isDecodeFinish = true;
		mQueue.put(FINISH_INFO);
	}

	private void putBuffer(byte[] bytes, int timestamp, int width, int height) {

		BufferInfo bufferInfo = mPool.poll();
		if (bufferInfo == null) {
			bufferInfo = new BufferInfo(bytes.length);
		} else if (bufferInfo.data == null || bufferInfo.data.length != bytes.length) {
			bufferInfo.data = new byte[bytes.length];
		}

		System.arraycopy(bytes, 0, bufferInfo.data, 0, bytes.length);
		bufferInfo.width = width;
		bufferInfo.height = height;
		bufferInfo.timestamp = timestamp * 1000;

		try {
			mQueue.put(bufferInfo);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static class BufferInfo {

		BufferInfo(int size) {
			if (size > 0) {
				data = new byte[size];
			}
		}

		public byte[] data;
		public int width;
		public int height;

		/**
		 * 时间戳，单位微秒
		 */
		public long timestamp;
	}
}
