package poco.cn.medialibs.player2.base;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/**
 * Created by: fwc
 * Date: 2018/12/4
 */
@SuppressWarnings( {"WeakerAccess"/*, "unused"*/})
public class VideoPlayer {

	@Nullable
	private String mDataSource;

	@Nullable
	private PlaySurface mSurface;

	@State
	private volatile int mState = State.IDLE;

	private boolean isLooping = false;

	@NonNull
	private final VideoDecodeThread mVideoThread;

	@NonNull
	private final AudioDecodeThread mAudioThread;

	private volatile long mPresentationTimeUs;

	/**
	 * 用于控制是否允许播放音频
	 */
	private boolean isAudioEnable = true;

	/**
	 * 音频是否准备好播放
	 */
	private volatile boolean isAudioPrepared;

	private float mVolume = 1.0f;

	@Nullable
	private OnPlayListener mListener;

	private volatile boolean isClipping;
	private volatile boolean isClipPrepared;
	private long mStartPos;
	private long mEndPos;

	private volatile boolean isSeeking;
	private volatile boolean mPendStart;

	@NonNull
	private final PlayHandler mPlayHandler;

	private volatile boolean mFixFrameRate;
	private static final int FRAME_RATE_PER_SECOND = 25;
	private final int mFrameInterval;
	private long mNextTimestamp;

	private volatile boolean isPreparing;
	private final Object mPreparingLock = new Object();

	public VideoPlayer() {
		mVideoThread = new VideoDecodeThread();
		mAudioThread = new AudioDecodeThread();

		mPlayHandler = new PlayHandler(Looper.myLooper());

		mFrameInterval = 1000 / FRAME_RATE_PER_SECOND;
	}

	public void setDataSource(@NonNull String dataSource) {
		checkState(State.IDLE);
		checkNull(dataSource);

		File file = new File(dataSource);
		if (!file.exists() || file.length() <= 0) {
			throw new RuntimeException("the file: " + dataSource + "(" + file.exists() + "," + file.length() + ") is not exist.");
		}
		mDataSource = dataSource;
	}

	public void setSurface(@NonNull PlaySurface surface) {
		checkState(State.IDLE);
		checkNull(surface);

		surface.setTimestamp(0);
		mSurface = surface;
	}

	/**
	 * 同步 prepare，可能比较耗时
	 *
	 * @throws IOException
	 */
	@SuppressWarnings("all")
	public void prepare(int id) throws IOException {
		checkState(State.IDLE);

		mState = State.PREPARING;

		prepareImpl(id);

		mState = State.PREPARED;
	}

	private void prepareImpl(int id) throws IOException {
		final PlaySurface surface = mSurface;
		final String dataSource = mDataSource;

		checkNull(dataSource);
		checkNull(surface);

		isAudioPrepared = false;

		boolean isVideoPrepare = mVideoThread.prepare(mDataSource, surface);
		if (!isVideoPrepare) {
			return;
		}
		mVideoThread.setVideoId(id);
		surface.setRotation(mVideoThread.isNeedRotate, mVideoThread.mRotation);
		mVideoThread.setLooping(isLooping);
		if (isAudioEnable) {
			isAudioPrepared = mAudioThread.prepare(mDataSource, null);
			if (isAudioPrepared) {
				mAudioThread.setLooping(isLooping);
				mAudioThread.setVolume(mVolume);
			}
		}
	}

	public void prepareAsync(int id) {

		checkState(State.IDLE);

		isPreparing = true;
		mState = State.PREPARING;

		// MediaCodec 不是线程安全的

		mVideoThread.queueEvent(new PrepareRunnable(id));
	}

	private class PrepareRunnable implements Runnable {

		private int mVideoId;

		PrepareRunnable(int videoId) {
			mVideoId = videoId;
		}

		@Override
		public void run() {
			try {
				prepareImpl(mVideoId);
			} catch (IOException e) {
				e.printStackTrace();
			}

			onPrepared();
		}
	}

	public void start() {
		if (inStates(State.PREPARED, State.PAUSE, State.FINISH)) {

			if (isSeeking) {
				mPendStart = true;
				return;
			}
			isSeeking = false;
			if (isClipping && !isClipPrepared) {
				mPendStart = true;
				return;
			}
			mPendStart = false;
			if (mState == State.FINISH) {
				restartImpl();
			} else {
				mState = State.START;
				mVideoThread.start();
				if (isAudioPrepared) {
					mAudioThread.start();
				}
			}

		}
	}

	public void restart() {
		if (inStates(State.PREPARED, State.START, State.PAUSE, State.FINISH)) {
			pause();
			isSeeking = false;
			mPendStart = false;
			restartImpl();
		}
	}

	private void restartImpl() {
		releaseFinishLock();
		releaseClipStartLock();
		releaseSeekLock();
		mPresentationTimeUs = 0;
		mState = State.START;
		mVideoThread.restart();
		if (isAudioPrepared) {
			mAudioThread.restart();
		}
	}

	public void pause() {
		mPendStart = false;
		if (inStates(State.PREPARED, State.START)) {
			mState = State.PAUSE;
			if (isAudioPrepared) {
				mAudioThread.pause();
			}
			mVideoThread.pause();
		}
	}

	/**
	 * 设置是否固定帧率回调
	 */
	public void setFixFrameRate(boolean isFixFrameRate) {
		mFixFrameRate = isFixFrameRate;
	}

	/**
	 * 是否连续播放
	 */
	public void setContinuousPlay(boolean isContinuous) {
		mVideoThread.isContinuous = isContinuous;
		mAudioThread.isContinuous = isContinuous;
	}

	/**
	 * 设置区间范围播放视频
	 *
	 * @param startPos 视频开始播放位置，单位毫秒
	 * @param endPos   视频停止播放位置，单位毫秒
	 */
	public void setRangePlay(long startPos, long endPos) {
		if (inStates(State.PREPARED, State.START, State.PAUSE, State.FINISH)) {
			startPos = Math.max(0, startPos);
			endPos = Math.min(endPos, getDuration());

			if (startPos >= endPos) {
				return;
			}

			mPendStart = false;
			isSeeking = false;
			releaseSeekLock();

			isClipping = true;
			isClipPrepared = false;
			mStartPos = startPos;
			mEndPos = endPos;

			if (mState == State.START) {
				pause();
			}

			mVideoThread.setClipping(startPos, endPos);
			mAudioThread.setClipping(startPos, endPos);
		}
	}

	/**
	 * 恢复整个视频播放
	 */
	public void recoverWholePlay() {
		if (inStates(State.PREPARED, State.START, State.PAUSE, State.FINISH)) {
			if (isClipping) {
				resetClipping();

				isSeeking = false;
				mVideoThread.isFinish = false;
				if (isAudioPrepared) {
					mAudioThread.isFinish = false;
				}
				releaseFinishLock();
				releaseClipStartLock();
				releaseSeekLock();

				mPresentationTimeUs = 0;
				mState = State.START;
				mVideoThread.recoverWholePlay();
				if (isAudioPrepared) {
					mAudioThread.recoverWholePlay();
				}
			}
		}
	}

	private void resetClipping() {
		isClipping = false;
		isClipPrepared = false;
		mStartPos = 0;
		mEndPos = 0;
	}

	public long getCurrentPosition() {
		return mPresentationTimeUs / 1000;
	}

	public long getDuration() {
		return Math.max(mVideoThread.getDuration(), mAudioThread.getDuration());
	}

	/**
	 * {@link #seekTo(long, boolean)};
	 */
	public void seekTo(long position) {
		seekTo(position, false);
	}

	/**
	 * seekTo 到一个位置，如果是区间播放，则 position 不可以超过区间范围
	 *
	 * @param position 视频位置，单位毫秒
	 * @param isExact  是否精确 seek
	 */
	public void seekTo(long position, boolean isExact) {

		// 如果上一下还没seekTo完不允许继续seek
		if (isSeeking) {
			return;
		}

		pause();
		if (isClipping) {
			if (position < mStartPos || position > mEndPos) {
				resetClipping();
				mVideoThread.resetClipping();
				if (isAudioPrepared) {
					mAudioThread.resetClipping();
				}
			}
		}
		if (inStates(State.PREPARED, State.START, State.PAUSE, State.FINISH)) {
			if (mState == State.FINISH) {
				mState = State.PAUSE;
			}
			position = Math.min(position, getDuration());
			mVideoThread.seekAudio = false;
			isSeeking = true;
			if (isExact) {
				if (isAudioPrepared) {
					mAudioThread.seekTo(position * 1000, true);
				}
			} else {
				if (isAudioPrepared) {
					mVideoThread.seekAudio = true;
				}
			}

			mVideoThread.seekTo(position * 1000, isExact);
		}
	}

	public void stop() {
		if (inStates(State.START, State.PAUSE, State.FINISH)) {
			if (isAudioPrepared) {
				mAudioThread.stop();
			}
			mVideoThread.stop();
			mState = State.STOP;
			releaseFinishLock();
			releaseClipStartLock();
			releaseSeekLock();
		}
	}

	/**
	 * 同步 reset，可能比较耗时
	 */
	public void reset() {
		if (isPreparing) {
			synchronized (mPreparingLock) {
				try {
					mPreparingLock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		stop();
		mState = State.IDLE;

		resetField();
		mVideoThread.reset();
		mAudioThread.reset();

	}

	/**
	 * 同步 release，可能比较耗时
	 */
	public void release() {
		if (mState == State.RELEASE) {
			return;
		}
		if (isPreparing) {
			synchronized (mPreparingLock) {
				try {
					mPreparingLock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		stop();

		mState = State.RELEASE;

//		resetField();
		mVideoThread.release();
		mAudioThread.release();
	}

	private void releaseFinishLock() {
		if (mVideoThread.isFinishLock || mAudioThread.isFinishLock) {
			synchronized (mFinishLock) {
				mFinishLock.notifyAll();
			}
		}
	}

	private void releaseClipStartLock() {
		if (mVideoThread.isClipStartLock || mAudioThread.isClipStartLock) {
			synchronized (mClipStartLock) {
				mClipStartLock.notifyAll();
			}
		}
	}

	private void releaseSeekLock() {
		synchronized (mSeekLock) {
			mSeekLock.notifyAll();
		}
	}

	private void resetField() {
		mPlayHandler.removeCallbacksAndMessages(null);
		mDataSource = null;
		mSurface = null;
		mPresentationTimeUs = 0;
		isClipping = false;
		isClipPrepared = false;
		mStartPos = 0;
		mEndPos = 0;
		isSeeking = false;
		mPendStart = false;
		isPreparing = false;
	}

	public void setLooping(boolean isLooping) {
		if (this.isLooping != isLooping) {
			this.isLooping = isLooping;
			mVideoThread.setLooping(isLooping);
			mAudioThread.setLooping(isLooping);
		}
	}

	public boolean isLooping() {
		return isLooping;
	}

	public void setVolume(@FloatRange(from = 0.0f, to = 1.0f) float volume) {
		if (!isAudioEnable) {
			return;
		}
		if (mVolume != volume) {
			mVolume = volume;
			mAudioThread.setVolume(volume);
		}
	}

	public float getVolume() {
		return mVolume;
	}

	@SuppressWarnings("all")
	private void checkState(@State int state) {
		if (mState != state) {
			throw new IllegalStateException("The current State: " + mState + ", target State: " + state);
		}
	}

	private boolean inStates(@NonNull int... states) {
		if (states.length > 0) {
			for (int state : states) {
				if (mState == state) {
					return true;
				}
			}
		}

		return false;
	}

	private static void checkNull(Object obj) {
		if (obj == null) {
			throw new NullPointerException();
		}
	}

	public boolean isPrepare() {
		return !(mState == State.IDLE);
	}

	public boolean isPlaying() {
		return mState == State.START;
	}

	public boolean isFinish() {
		return mState == State.FINISH;
	}

	public boolean isRangePlay() {
		return isClipping;
	}

	public boolean isPause() {
		return mState == State.PAUSE;
	}

	public boolean isSeeking() {
		return isSeeking;
	}

	@Retention(RetentionPolicy.SOURCE)
	@IntDef( {State.IDLE, State.PREPARING, State.PREPARED, State.START, State.PAUSE, State.FINISH, State.STOP, State.RELEASE})
	private @interface State {
		int IDLE = 0;
		int PREPARING = 1;
		int PREPARED = 2;
		int START = 3;
		int PAUSE = 4;
		int FINISH = 5;
		int STOP = 6;
		int RELEASE = 7;
	}

	@NonNull
	private final Object mFinishLock = new Object();

	@NonNull
	private final Object mClipStartLock = new Object();

	@NonNull
	private final Object mSeekLock = new Object();

	private class VideoDecodeThread extends DecodeThread {

		private static final int MAX_DELTA = 100;

		int mWidth;
		int mHeight;
		int mRotation;
		boolean isNeedRotate;

		volatile boolean seekAudio;

		volatile boolean isFinishLock;
		volatile boolean isClipStartLock;

		VideoDecodeThread() {
			super(true);
		}

		@Override
		@SuppressWarnings("all")
		protected boolean parseFormat(@NonNull String dataSource, @NonNull MediaFormat format) {
			isNeedRotate = false;
			int width = format.getInteger(MediaFormat.KEY_WIDTH);
			if (format.containsKey("crop-left") && format.containsKey("crop-right")) {
				width = format.getInteger("crop-right") + 1 - format.getInteger("crop-left");
			}
			int height = format.getInteger(MediaFormat.KEY_HEIGHT);
			if (format.containsKey("crop-top") && format.containsKey("crop-bottom")) {
				height = format.getInteger("crop-bottom") + 1 - format.getInteger("crop-top");
			}
			mWidth = width;
			mHeight = height;
			if (format.containsKey("rotation-degrees")) {
				mRotation = format.getInteger("rotation-degrees");
			} else {
				MediaMetadataRetriever retriever = new MediaMetadataRetriever();
				try {
					retriever.setDataSource(dataSource);
					String s = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
					mRotation = Integer.valueOf(s);
					isNeedRotate = true;
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					retriever.release();
				}
			}

			if (mRotation % 180 != 0) {
				int temp = mWidth;
				mWidth = mHeight;
				mHeight = temp;
			}

			return true;
		}

		@Override
		protected boolean isPlaying() {
			return VideoPlayer.this.isPlaying();
		}

		@Override
		protected void onStart() {
			super.onStart();
			resetNextFrame();
			VideoPlayer.this.onStart();
		}

		@Override
		protected void onSeekCompleted(long presentationTimeUs) {
			super.onSeekCompleted(presentationTimeUs);
			if (seekAudio && isAudioPrepared && isSeeking) {
				seekAudio = false;
				mAudioThread.seekTo(presentationTimeUs, true);
			}

			if (!isSeeking) {
				return;
			}

			synchronized (mSeekLock) {
				if (isAudioPrepared) {
					if (!mAudioThread.isSeekCompleted) {
						try {
							mSeekLock.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				VideoPlayer.this.onSeekCompleted();
			}
		}

		@Override
		protected void onFinish() {
			super.onFinish();
			resetNextFrame();
			if (!isFinish) {
				return;
			}
			synchronized (mFinishLock) {
				isFinishLock = true;
				if (isAudioPrepared && !mAudioThread.isFinish && isPlaying()) {
					try {
						mFinishLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				isFinishLock = false;
				if (isAudioPrepared && mAudioThread.isFinishLock) {
					mFinishLock.notifyAll();
				}
			}

			if (!isLooping) {
				mState = State.FINISH;
			}
			mPlayHandler.sendEmptyMessage(MSG_FINISH_CALLBACK);
			VideoPlayer.this.onFinish();
		}

		@Override
		protected void onClippingStart() {
			super.onClippingStart();
			if (!isClipping) {
				return;
			}
			synchronized (mClipStartLock) {
				isClipStartLock = true;
				if (isAudioPrepared && !mAudioThread.isClippingStart && isPlaying()) {
					try {
						mClipStartLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				isClipStartLock = false;
				if (isAudioPrepared && mAudioThread.isClipStartLock) {
					mClipStartLock.notifyAll();
				}
			}

			isClipPrepared = true;
			if (mPendStart) {
				VideoPlayer.this.start();
			}
			if (VideoPlayer.this.isPlaying()) {
				VideoPlayer.this.onClipStart();
			}
		}

		@Override
		protected void onClippingFinish() {
			if (!isClipping) {
				return;
			}
			super.onFinish();
			synchronized (mFinishLock) {
				isFinishLock = true;
				if (isAudioPrepared && !mAudioThread.isFinish && isPlaying()) {
					try {
						mFinishLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				isFinishLock = false;
				if (isAudioPrepared && mAudioThread.isFinishLock) {
					mFinishLock.notifyAll();
				}
			}

			if (!isLooping) {
				mState = State.FINISH;
			}
			VideoPlayer.this.onClipFinish();
		}

		@Override
		protected long PTSSync(long presentationTimeUs, boolean isRenderOnce) {
			if (isRenderOnce) {
				mPresentationTimeUs = presentationTimeUs;
				return presentationTimeUs;
			}
			if (isAudioPrepared && !mAudioThread.isFinish) {
				final long curPresentation = mPresentationTimeUs;
				final long delta = presentationTimeUs / 1000 - curPresentation / 1000;
				if (Math.abs(delta) < MAX_DELTA) {
					return presentationTimeUs;
				} else if (delta < 0) {
					return -1;
				} else {
					return presentationTimeUs + delta;
				}
			} else {
				mPresentationTimeUs = presentationTimeUs;
			}

			return presentationTimeUs;
		}

		@Override
		protected void onPositionChanged(int position) {
			super.onPositionChanged(position);
			VideoPlayer.this.onPositionChanged(position);
		}

		@Override
		protected void onSleepStart() {
			getNextFrame();
		}

		@Override
		protected void onSleepEnd() {
			resetNextFrame();
		}

		@Override
		public void reset() {
			super.reset();
			resetNextFrame();
			seekAudio = false;
		}

		@Override
		public void release() {
			super.release();
			resetNextFrame();
			seekAudio = false;
		}

		@Override
		protected long getPresentationTimeUs() {
			return mPresentationTimeUs;
		}
	}

	private void getNextFrame() {
		if (mFixFrameRate) {
			mNextTimestamp = mPresentationTimeUs / 1000 + mFrameInterval;
			mPlayHandler.sendEmptyMessageDelayed(MSG_FRAME_CALLBACK, mFrameInterval);
		}
	}

	private void resetNextFrame() {
		if (mFixFrameRate) {
			mNextTimestamp = -1;
			mPlayHandler.removeMessages(MSG_FRAME_CALLBACK);
		}
	}

	private class AudioDecodeThread extends DecodeThread {

		@Nullable
		private AudioTrack mAudioTrack;

		@Nullable
		private byte[] mAudioOutBuffer;

		private boolean isWriteData;

		private float mVolume = AudioTrack.getMaxVolume();

		volatile boolean isFinishLock;
		volatile boolean isClipStartLock;

		AudioDecodeThread() {
			super(false);
		}

		@Override
		protected boolean parseFormat(@NonNull String dataSource, @NonNull MediaFormat format) {
//			AVInfo avInfo = new AVInfo();
//			AVUtils.avInfo(dataSource, avInfo, false);
//			int audioSampleRate = avInfo.audioSampleRate;
			int audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
			if (audioSampleRate <= 0) {
				return false;
			}
//			int channelConfig = (avInfo.audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO);
			int audioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
			int channelConfig = (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO);
			int minBufferSize = AudioTrack.getMinBufferSize(audioSampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
//			int maxInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
//			if (format.containsKey("bit-width")) {
//				int bitWidth = format.getInteger("bit-width");
//			}
			if (minBufferSize <= 0) {
				return false;
			}
			try {
				mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, audioSampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
				mAudioTrack.play();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				if (mAudioTrack != null) {
					mAudioTrack.release();
					mAudioTrack = null;
				}
				return false;
			}

			setVolumeImpl(mVolume);
			isWriteData = false;

			return true;
		}

		@Override
		protected boolean isPlaying() {
			return VideoPlayer.this.isPlaying();
		}

		@Override
		protected void onStart() {
			super.onStart();
		}

		@Override
		protected void onSeekCompleted(long presentationTimeUs) {
			super.onSeekCompleted(presentationTimeUs);
			releaseSeekLock();
		}

		@Override
		protected void onFinish() {
			super.onFinish();
			final AudioTrack audioTrack = mAudioTrack;
			if (audioTrack != null && isWriteData) {
				isWriteData = false;
				audioTrack.flush();
			}
			if (!isFinish) {
				return;
			}
			synchronized (mFinishLock) {
				isFinishLock = true;
				if (!mVideoThread.isFinish && isPlaying()) {
					try {
						mFinishLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				isFinishLock = false;
				if (mVideoThread.isFinishLock) {
					mFinishLock.notifyAll();
				}
			}
		}

		@Override
		protected void onClippingStart() {
			super.onClippingStart();
			if (!isClipping) {
				return;
			}
			synchronized (mClipStartLock) {
				isClipStartLock = true;
				if (!mVideoThread.isClippingStart && isPlaying()) {
					try {
						mClipStartLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				isClipStartLock = false;
				if (mVideoThread.isClipStartLock) {
					mClipStartLock.notifyAll();
				}
			}
		}

		@Override
		protected long PTSSync(long presentationTimeUs, boolean isRenderOnce) {
			if (!isFinish || isRenderOnce) {
				mPresentationTimeUs = presentationTimeUs;
			}
			return presentationTimeUs;
		}

		@Override
		protected void onDecodeData(@NonNull MediaCodec.BufferInfo bufferInfo, @Nullable ByteBuffer buffer) {
			final AudioTrack audioTrack = mAudioTrack;
			if (audioTrack != null && buffer != null) {
				if (bufferInfo.size > 0) {
					if (mAudioOutBuffer == null || mAudioOutBuffer.length < bufferInfo.size) {
						mAudioOutBuffer = new byte[bufferInfo.size];
					}
					buffer.position(bufferInfo.offset);
					buffer.limit(bufferInfo.offset + bufferInfo.size);
					buffer.get(mAudioOutBuffer, 0, bufferInfo.size);
					buffer.clear();
					audioTrack.write(mAudioOutBuffer, 0, bufferInfo.size);
					isWriteData = true;
				}
			}
		}

		@Override
		public void stop() {
			super.stop();
			final AudioTrack audioTrack = mAudioTrack;
			if (audioTrack != null) {
				try {
					audioTrack.pause();
					if (isWriteData) {
						isWriteData = false;
						audioTrack.flush();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void reset() {
			super.reset();
			releaseAudioTrack();
		}

		@Override
		public void release() {
			super.release();
			releaseAudioTrack();
		}

		private void releaseAudioTrack() {
			mVolume = AudioTrack.getMaxVolume();
			final AudioTrack audioTrack = mAudioTrack;
			if (audioTrack != null) {
				audioTrack.release();
				mAudioTrack = null;
			}
		}

		void setVolume(float volume) {
			if (mVolume != volume) {
				setVolumeImpl(volume);
				mVolume = volume;
			}
		}

		private void setVolumeImpl(float volume) {
			final AudioTrack audioTrack = mAudioTrack;
			if (audioTrack != null) {
				volume = MathUtils.clamp(volume, AudioTrack.getMinVolume(), AudioTrack.getMaxVolume());
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					audioTrack.setVolume(volume);
				} else {
					audioTrack.setStereoVolume(volume, volume);
				}
			}
		}
	}

	public void setOnPlayListener(@Nullable OnPlayListener listener) {
		mListener = listener;
	}

	private void onPrepared() {
		isPreparing = false;
		synchronized (mPreparingLock) {
			mPreparingLock.notifyAll();
		}
		mPlayHandler.post(mPreparedRunnable);
	}

	private Runnable mPreparedRunnable = new Runnable() {
		@Override
		public void run() {
			if (inStates(State.PREPARING)) {
				mState = State.PREPARED;
				final OnPlayListener listener = mListener;
				if (listener != null) {
					listener.onPrepared(VideoPlayer.this);
				}
			}
		}
	};

	private void onStart() {
		mPlayHandler.post(mStartRunnable);
	}

	private Runnable mStartRunnable = new Runnable() {
		@Override
		public void run() {
			final OnPlayListener listener = mListener;
			if (listener != null && isPlaying()) {
				listener.onStart();
			}
		}
	};

	private void onSeekCompleted() {
		isSeeking = false;
		mPlayHandler.post(mSeekRunnable);
		if (mPendStart) {
			start();
		}
	}

	private Runnable mSeekRunnable = new Runnable() {
		@Override
		public void run() {
			final OnPlayListener listener = mListener;
			if (listener != null) {
				listener.onSeekCompleted(VideoPlayer.this);
			}
		}
	};

	private void onFinish() {
		mPlayHandler.post(mFinishRunnable);
	}

	private Runnable mFinishRunnable = new Runnable() {
		@Override
		public void run() {
			final OnPlayListener listener = mListener;
			if (listener != null) {
				listener.onFinish();
			}
		}
	};

	private void onClipStart() {
		if (isClipping) {
			mPlayHandler.post(mClipStartRunnable);
		}
	}

	private Runnable mClipStartRunnable = new Runnable() {
		@Override
		public void run() {
			final OnPlayListener listener = mListener;
			if (listener != null) {
				listener.onClipStart();
			}
		}
	};

	private void onClipFinish() {
		if (isClipping) {
			mPlayHandler.post(mClipFinishRunnable);
		}
	}

	private Runnable mClipFinishRunnable = new Runnable() {
		@Override
		public void run() {
			final OnPlayListener listener = mListener;
			if (listener != null) {
				listener.onClipFinish();
			}
		}
	};

	private void onPositionChanged(int position) {
		Message.obtain(mPlayHandler, MSG_POSITION_CHANGED, position, 0).sendToTarget();
	}

	private static final int MSG_POSITION_CHANGED = 0x12;
	private static final int MSG_FRAME_CALLBACK = 0x13;
	private static final int MSG_FINISH_CALLBACK = 0x14;

	private class PlayHandler extends Handler {

		PlayHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_POSITION_CHANGED: {
					final OnPlayListener listener = mListener;
					if (listener != null) {
						listener.onPositionChanged(msg.arg1);
					}
					break;
				}
				case MSG_FRAME_CALLBACK: {
					final PlaySurface surface = mSurface;
					final long timestamp = mNextTimestamp;
					if (surface != null) {
						surface.onFrameAvailable(timestamp);
						if (mNextTimestamp != -1) {
							getNextFrame();
						}
					}
					break;
				}
				case MSG_FINISH_CALLBACK: {
					final PlaySurface surface = mSurface;
					final long timestamp = getDuration();
					if (surface != null) {
						surface.onFrameAvailable(timestamp);
					}
					break;
				}
			}
		}
	}

	public abstract static class OnPlayListener {

		protected void onPrepared(VideoPlayer player) {
		}

		protected void onStart() {
		}

		protected void onSeekCompleted(@NonNull VideoPlayer player) {
		}

		protected void onFinish() {
		}

		protected void onClipStart() {
		}

		protected void onClipFinish() {
		}

		protected void onPositionChanged(int position) {
		}
	}

	public interface PlaySurface {

		@NonNull
		Surface getSurface();

		void setId(int id);

		void setTimestamp(long timestamp);

		boolean isValid();

		void setRotation(boolean isNeedRotate, int rotation);

		void onFrameAvailable(long timestamp);

		void setVideoDuration(long duration);
	}
}
