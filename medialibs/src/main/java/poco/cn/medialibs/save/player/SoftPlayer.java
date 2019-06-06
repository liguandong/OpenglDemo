package poco.cn.medialibs.save.player;


import poco.cn.medialibs.save.ThreadPool;
import poco.cn.medialibs.save.player.decoder.AbsDecoder;
import poco.cn.medialibs.save.player.decoder.RGBADecoder;
import poco.cn.medialibs.save.player.decoder.YUVDecoder;

/**
 * Created by: fwc
 * Date: 2017/12/27
 */
public class SoftPlayer {

	public static final int RGBA_FRAME_TYPE = 0;
	public static final int YUV_FRAME_TYPE = 1;

	private static final int IDLE = 0;
	private static final int PREPARED = 1;
	private static final int START = 2;
	private static final int RELEASE = 4;

	private String mDataSource;
	private SoftTexture mSoftTexture;

	private AbsDecoder mDecoder;

	private long mDuration;
	private int mRotation;

	private int mState = IDLE;

	private ThreadPool mThreadPool;

	private int mFrameType = RGBA_FRAME_TYPE;

	private int mBufferSize = 2;

	public SoftPlayer() {
		mThreadPool = ThreadPool.getInstance();
	}

	public void setFrameType(int frameType) {
		mFrameType = frameType;
	}

	public void setDecodeBufferSize(int bufferSize) {
		if (bufferSize > 0) {
			mBufferSize = bufferSize;
		}
	}

	public void setDataSource(String dataSource) {

		if (mState != IDLE) {
			return;
		}

		mDataSource = dataSource;
	}

	public void setDuration(long duration) {
		mDuration = duration;
	}

	public void setRotation(int rotation) {
		mRotation = rotation;
	}

	public void setSoftTexture(SoftTexture softTexture) {

		if (mState != IDLE) {
			return;
		}

		if (softTexture == null) {
			throw new IllegalArgumentException("the softTexture is null");
		}

		mSoftTexture = softTexture;
	}

	public void prepare() {

		if (mState != IDLE) {
			return;
		}

		if (mDataSource == null) {
			throw new RuntimeException("the mDataSource is null");
		}

		if (mSoftTexture == null) {
			throw new RuntimeException("the mSoftTexture is null");
		}

		if (mFrameType == RGBA_FRAME_TYPE) {
			mDecoder = new RGBADecoder(mBufferSize);
		} else if (mFrameType == YUV_FRAME_TYPE) {
			mDecoder = new YUVDecoder(mBufferSize);
		} else {
			throw new RuntimeException("the frame type: " + mFrameType + " is not support.");
		}
		mDecoder.setDataSource(mDataSource);
		mDecoder.setDuration(mDuration);

		mState = PREPARED;
	}

	public void start() {

		if (mState != PREPARED) {
			return;
		}

		mState = START;

		mThreadPool.execute(mDecoder);

		boolean finish = false;

		try {
			while (!finish) {
				finish = getFrame() == -1;
				notifyFrameAvailable();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		if (mDecoder != null) {
			mDecoder.stop();
		}
	}

	public boolean isPlaying() {
		return mState == START;
	}

	void startDecoder() {

		if (mState != PREPARED) {
			return;
		}
		mState = START;

		mThreadPool.execute(mDecoder);
	}

	/**
	 * 获取一帧数据
	 * @return 当前帧的时间戳，返回-1表示视频结束
	 */
	long getFrame() throws InterruptedException {

		if (mState != START) {
			return -1;
		}

		AbsDecoder.BufferInfo bufferInfo = mDecoder.getBufferInfo();
		if (bufferInfo != null) {
			mSoftTexture.setByteBuffer(bufferInfo.data, bufferInfo.width,
									   bufferInfo.height, mRotation, bufferInfo.timestamp);
			mDecoder.recycle(bufferInfo);
			return bufferInfo.timestamp;
		} else {
			mSoftTexture.setTimestamp(mDuration * 1000);
		}

		return -1;
	}

	void getFrame(long position, long duration, int transitionTime) throws InterruptedException {

		if (transitionTime == 0 || position == -1) {
			getFrame();
			return;
		}

		if (position < duration - transitionTime || position > duration) {
			return;
		}

//		long curPosition = mSoftTexture.getTimestamp();
//		if (duration - position + curPosition <= transitionTime) {
//			getFrame();
//		}
		getFrame();
	}

	public long getDuration() {
		return mDuration;
	}

	public void notifyFrameAvailable() {
		mSoftTexture.notifyFrameAvailable();
	}

	public void release() {
		mState = RELEASE;
		if (mDecoder != null) {
			mDecoder.stop();
			mDecoder.release();
		}
	}
}
