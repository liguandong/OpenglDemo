package poco.cn.medialibs.player2.base;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Author: Comit
 * Date:   2018/12/4
 * Time:   10:46 PM
 */
abstract class DecodeThread {

	private static final int TIMEOUT_USEC = 10000;

	private static final int MSG_DECODE_WORK = 0x10;
	private static final int MSG_RELEASE = 0x11;
	private static final int MSG_RESTART = 0x12;
	private static final int MSG_SEEK_TO = 0x13;
	private static final int MSG_CLIPPING = 0x14;
	private static final int MSG_RESTART_RANGE = 0x15;
	private static final int MSG_RECOVER_PLAY = 0x16;

	@NonNull
	private HandlerThread mDecodeThread;

	@NonNull
	private DecodeHandler mDecodeHandler;

	/**
	 * 标记是视频解码还是音频解码
	 */
	private final boolean isVideoDecode;

	@Nullable
	private String mDataSource;

	@Nullable
	private MediaCodec mDecoder;

	@Nullable
	private MediaExtractor mExtractor;

	@Nullable
	private BufferQueue mBufferQueue;

	/**
	 * 播放 Surface，以及相关的播放信息
	 */
	@Nullable
	private VideoPlayer.PlaySurface mSurface;

	/**
	 * 标记是否开始解码了
	 */
	private boolean isInputBufferQueued;

	/**
	 * 标记是否准备好
	 */
	private boolean isPrepared;

	/**
	 * 标记是否是循环的
	 */
	private boolean isLooping;

	/**
	 * 释放锁，确保视频解码线程和音频解码都释放完毕
	 */
	private final Object mReleaseLock = new Object();

	/**
	 * 标记是否已释放
	 */
	private boolean isRelease;

	/**
	 * 视频或音频的时长
	 */
	private long mDuration;

	/**
	 * 标记是否播放结束
	 */
	volatile boolean isFinish;

	/**
	 * 标记是否 seek 完成
	 */
	volatile boolean isSeekCompleted;

	/**
	 * 用于 seek 数据复用
	 */
	@NonNull
	private SeekDataPool mSeekDataPool;

	/**
	 * 用于准确 seek
	 */
	private volatile long isSeekPosition = -1;

	/**
	 * 标记是否是区间播放
	 */
	private boolean isClipping;

	/**
	 * 区间播放开始位置
	 */
	private long mStartPos;

	/**
	 * 区间播放结束位置
	 */
	private long mEndPos;

	/**
	 * 标记区间播放是否开始
	 */
	volatile boolean isClippingStart;

	/**
	 * 视频的 ic
	 */
	private int mVideoId;

	/**
	 * 是否连续播放，减少播放卡顿问题
	 */
	boolean isContinuous;

	/**
	 * 记录视频第一帧的时间戳，有些视频不为 0
	 */
	private long mFirstFrameTime;

	/**
	 * 标记是否调用了 release
	 */
	private volatile boolean isReleased;

	DecodeThread(boolean isVideoDecode) {
		this.isVideoDecode = isVideoDecode; // 标记是视频解码还是音频解码

		if (isVideoDecode) {
			mDecodeThread = new HandlerThread("Video Decode Thread", Process.THREAD_PRIORITY_URGENT_DISPLAY);
		} else {
			mDecodeThread = new HandlerThread("Audio Decode Thread", Process.THREAD_PRIORITY_URGENT_AUDIO);
		}
		mDecodeThread.start(); // 开启线程
		mDecodeHandler = new DecodeHandler(mDecodeThread.getLooper());

		mSeekDataPool = new SeekDataPool();
	}

	void setVideoId(int videoId) {
		mVideoId = videoId;
	}

	/**
	 * 准备播放
	 * @param dataSource   视频路径
	 * @param surface      PlaySurface 对象，封装了 Surface 等渲染信息，如果是音频解码，为 null
	 * @return 是否成功
	 */
	boolean prepare(@NonNull String dataSource, @Nullable VideoPlayer.PlaySurface surface) throws IOException {
		MediaCodec mediaCodec = mDecoder;
		mDecoder = null;
		if (mediaCodec != null) { // 如果 MediaCodec 不为 null，先释放
			try {
				mediaCodec.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		isPrepared = false;

		mExtractor = new MediaExtractor();
		mExtractor.setDataSource(dataSource);
		int trackIndex = selectTrack(mExtractor, isVideoDecode ? "video/" : "audio/");
		if (trackIndex < 0) {
			releaseExtractor();
			return false;
		}
		mExtractor.selectTrack(trackIndex);
		MediaFormat format = mExtractor.getTrackFormat(trackIndex);
		String mimeType = format.getString(MediaFormat.KEY_MIME);
		if (format.containsKey(MediaFormat.KEY_DURATION)) { // 获取时长，一些手机可能无法通过 MediaExtractor 获取到
			mDuration = format.getLong(MediaFormat.KEY_DURATION);
		} else {
			mDuration = getVideoDuration(dataSource);
		}
		if (surface != null) {
			// 通知渲染层那边，该视频的时长，主要用于转场控制
			surface.setVideoDuration(mDuration / 1000);
		}
		if (!parseFormat(dataSource, format)) {
			releaseExtractor();
			return false;
		}

		resetField();
		mDataSource = dataSource;
		isInputBufferQueued = false;

		// 获取视频第一帧的时间戳，有些视频不为 0
		mFirstFrameTime = mExtractor.getSampleTime();

		int prepareError = 0;

		int repeatCount = 1;
		if (isVideoDecode) { // 如果是视频解码，最大重试次数为 3
			repeatCount = 3;
		}

		while (prepareError < repeatCount) {
			try {
				mediaCodec = MediaCodec.createDecoderByType(mimeType);
				if ((surface != null && !surface.isValid()) || isReleased) {
					return false;
				}
				mediaCodec.configure(format, surface != null ? surface.getSurface() : null, null, 0);
				try {
					mediaCodec.start();
				} catch (Exception e) {
					e.printStackTrace();
					releaseDecoder();
					break;
				}
				mBufferQueue = new BufferQueue(mediaCodec);
				isPrepared = true;
				mSurface = surface;
				mDecoder = mediaCodec;

				return true;
			} catch (Exception e) {
				e.printStackTrace();
				resetBufferQueue();
				releaseDecoder();
				prepareError++;
			}
		}

		// 如果配置失败，释放 MediaExtractor
		releaseExtractor();

		return false;
	}

	void setLooping(boolean isLooping) {
		this.isLooping = isLooping;
	}

	long getDuration() {
		return mDuration / 1000;
	}

	/**
	 * 开始播放
	 */
	public void start() {
		if (isPrepared) {
			if (isClipping && isFinish) {
				restartRangePlay();
				return;
			}
			mRenderStartTime = -1;
			isSeekPosition = -1;
			mDecodeHandler.removeMessages(MSG_SEEK_TO);
			mDecodeHandler.removeMessages(MSG_DECODE_WORK);
			mDecodeHandler.sendEmptyMessage(MSG_DECODE_WORK);
		}
	}

	/**
	 * 暂停播放
	 */
	void pause() {
		mDecodeHandler.removeMessages(MSG_DECODE_WORK);
		mDecodeHandler.removeMessages(MSG_RESTART);
		mDecodeHandler.removeMessages(MSG_RESTART_RANGE);
	}

	/**
	 * 重头开始播放
	 */
	void restart() {
		if (isPrepared) {
			mDecodeHandler.removeCallbacksAndMessages(null);
			mDecodeHandler.sendEmptyMessage(MSG_RESTART);
		}
	}

	/**
	 * 如果当前是区间播放，恢复整段视频播放
	 */
	void recoverWholePlay() {
		if (isPrepared) {
			mDecodeHandler.removeCallbacksAndMessages(null);
			mDecodeHandler.sendEmptyMessage(MSG_RECOVER_PLAY);
		}
	}

	/**
	 * 重头开始播放，针对区间播放
	 */
	private void restartRangePlay() {
		if (isPrepared) {
			mDecodeHandler.removeCallbacksAndMessages(null);
			mDecodeHandler.sendEmptyMessage(MSG_RESTART_RANGE);
		}
	}

	/**
	 * seekTo到某位置
	 *
	 * @param position 单位微秒
	 * @param isExact  是否精确seek
	 */
	void seekTo(long position, boolean isExact) {
		if (isPrepared) {
			final SeekDataPool.SeekData seekData = mSeekDataPool.obtain();
			seekData.position = position;
			seekData.isExact = isExact;
			mDecodeHandler.removeMessages(MSG_DECODE_WORK);
			mDecodeHandler.removeMessages(MSG_SEEK_TO);
			Message.obtain(mDecodeHandler, MSG_SEEK_TO, seekData).sendToTarget();
		}
	}

	/**
	 * 设置区间播放的开始和结束位置
	 */
	void setClipping(long startPos, long endPos) {
		mDecodeHandler.removeMessages(MSG_DECODE_WORK);
		mDecodeHandler.removeMessages(MSG_CLIPPING);
		mDecodeHandler.removeMessages(MSG_SEEK_TO);
		mDecodeHandler.removeMessages(MSG_RESTART);
		mDecodeHandler.removeMessages(MSG_RESTART_RANGE);
		Message.obtain(mDecodeHandler, MSG_CLIPPING, new LongPair(startPos * 1000, endPos * 1000)).sendToTarget();
	}

	void queueEvent(Runnable event) {
		mDecodeHandler.post(event);
	}

	@NonNull
	private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

	/**
	 * 标记解码数据是否填充完毕
	 */
	private boolean isSawInputEOS;

	/**
	 * 标记解码是否已完成
	 */
	private boolean isSawOutputEOS;

	/**
	 * 记录开始渲染第一帧的时间
	 */
	private long mRenderStartTime = -1;

	/**
	 * 解码任务，以及一些相关控制
	 * 主要是通过不断的发送 {@link #MSG_DECODE_WORK} 的消息，然后会调用到这个方法来进行不断解码
	 * @param isSeek 当前是否是 seek 下
	 */
	private void doDecodeWork(final boolean isSeek) {
		final MediaCodec decoder = mDecoder;
		final MediaExtractor extractor = mExtractor;
		final BufferQueue bufferQueue = mBufferQueue;
		if (decoder == null || extractor == null || bufferQueue == null) {
			return;
		}

		if (!isSeek && !isPlaying()) { // 如果不是 seek 并且当前状态没有在播放，直接 return，不需要解码
			return;
		}

		if (isVideoDecode) { // 如果是视频解码，需要判断当前 Surface 是否有效
			final VideoPlayer.PlaySurface surface = mSurface;
			if (surface == null || !surface.isValid()) {
				return;
			}
		}

		if (!isSawInputEOS) { // 判断解码数据是否填充完毕
			int inputBufIndex;
			try {
				inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			if (inputBufIndex >= 0) {
				ByteBuffer inputBuf = bufferQueue.getInputBuffer(inputBufIndex);
				int chunkSize = extractor.readSampleData(inputBuf, 0); // 从 MediaExtractor 下获取数据
				if (chunkSize < 0) { // 数据获取完毕
					decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
					isSawInputEOS = true;
				} else {
					long presentationTimeUs = extractor.getSampleTime();

					if (isVideoDecode && mFirstFrameTime > 0) { // 如果是视频解码并且第一帧的时间不为 0，修复时间戳问题
						presentationTimeUs -= mFirstFrameTime;
					}
					decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, presentationTimeUs, 0);
					extractor.advance();
					isInputBufferQueued = true;
				}
			}
		}

		if (!isSawOutputEOS) { // 判断解码是否已完成
			int decoderStatus;
			try {
				decoderStatus = decoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			if (decoderStatus >= 0) {
				boolean doLoop = false;
				boolean isEnd = false;
				if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) { // 解码结束
					isEnd = true;
					if (isLooping) { // 如果是循环模式，重头开始播放
						doLoop = true;
					} else {
						isSawOutputEOS = true;
					}
				} else if (isSeekPosition > mBufferInfo.presentationTimeUs) {
					// 如果是准确 seek，isSeekPosition 不为 -1，
					// 这里是判断是否解码到了准确 seek 的位置
					// 如果没有就忽略这次的解码，直接返回
					// 注意 releaseOutputBuffer 的第二个参数，它决定是否渲染解码后的视频帧
					decoder.releaseOutputBuffer(decoderStatus, false);
					if (!isPlaying()) {
						Message.obtain(mDecodeHandler, MSG_DECODE_WORK, true).sendToTarget();
					} else {
						isSeekPosition = -1;
						mDecodeHandler.removeMessages(MSG_DECODE_WORK);
						mDecodeHandler.sendEmptyMessage(MSG_DECODE_WORK);
					}
					return;
				} else if (isClipping) { // 如果是区间播放下
					if (mStartPos > mBufferInfo.presentationTimeUs) { // 判断是否到达了区间播放的开始点，类似于准确 seek 的处理
						decoder.releaseOutputBuffer(decoderStatus, false);
						Message.obtain(mDecodeHandler, MSG_DECODE_WORK, true).sendToTarget();
						return;
					}

					if (!isClippingStart) { // 通知区间播放开始
						isClippingStart = true;
						onClippingStart();
					}
					if (isPlaying()) {
						Message.obtain(mDecodeHandler, MSG_DECODE_WORK).sendToTarget();
					}
				}

				if (!isEnd && mBufferInfo.presentationTimeUs == 0 && !isClipping) { // 通知视频播放开始
					onStart();
				}

				long presentationNano;
				boolean doNotDelay = false; // 标记是否需要延迟
				int positionChanged = -1; // 当前播放的位置点，单位毫秒
				if (isEnd && mBufferInfo.presentationTimeUs == 0) { // 有一些手机解码结束时的时间戳为 0
					doNotDelay = true;
					presentationNano = 0;
				} else {
					// 涉及到音视频播放同步，这里主要是视频向音频同步
					// 如果返回值 presentationNano 大于 0，说明解码比较快，需要延迟渲染
					presentationNano = PTSSync(mBufferInfo.presentationTimeUs, isSeek);
					if (!isSeek && isVideoDecode) { // 如果不是 seek 并且是视频解码，记录当前播放的时间点
						positionChanged = (int)(presentationNano / 1000);
					}
				}
				boolean isSlow = false; // 标记当前解码是否慢了
				if (presentationNano >= 0) {
					presentationNano *= 1000;
					if (mRenderStartTime < 0) {
						mRenderStartTime = System.nanoTime() - presentationNano;
					}

					if (!doNotDelay) {
						long delay = mRenderStartTime + presentationNano - System.nanoTime(); // 计算需要延迟的时间
						if (delay > 0) {
							onSleepStart(); // 通知延迟开始
							sleep(delay);  // 通过 Thread#sleep() 休眠线程来实现延迟
							onSleepEnd();  // 通知延迟结束
						}
					}
				} else {
					isSlow = true;
				}

				boolean doRender = false;
				if (isVideoDecode) { // 如果是视频解码
					// 下面是决定是否要将解码的视频帧渲染到 Surface 上
					if (isSeek) {
						doRender = true;
					} else {
						doRender = !isSlow && (mBufferInfo.size != 0) && isPlaying();
					}
				} else {
					if (!isSlow && isPlaying() && !isSeek) {
						// 回调解码后的数据，主要是音频解码
						onDecodeData(mBufferInfo, bufferQueue.getOutputBuffer(decoderStatus));
					}
				}

				if (mSurface != null && !doNotDelay) { // 针对视频解码，给 Surface 设置时间戳信息，用于在 GL 渲染那边确定转场渲染
					mSurface.setId(mVideoId);
					long timestamp = mBufferInfo.presentationTimeUs / 1000;
					if (isEnd) {
						timestamp = mDuration / 1000;
					}
					mSurface.setTimestamp(timestamp);
				}

				if (positionChanged != -1) { // 通知播放位置改变
					onPositionChanged(positionChanged);
				}
				try {
					// 注意 releaseOutputBuffer 的第二个参数，它决定是否渲染解码后的视频帧
					decoder.releaseOutputBuffer(decoderStatus, doRender);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}

				if (isSeek) { // 如果是 seek，需要通知 seek 已完成
					onSeekCompleted(mBufferInfo.presentationTimeUs);
					if (isEnd) { // 如果播放结束，通知外面播放结束
						onFinish();
					}
					return;
				} else if (isClipping) { // 如果是区间播放
					if (isEnd || mEndPos <= mBufferInfo.presentationTimeUs) { // 播放结束，回调 onClippingFinish 接口
						isSawOutputEOS = true;
						onClippingFinish();
						if (isLooping) {
							startClipping(mStartPos);
						}
						return;
					}
				}

				if (isEnd) { // 播放结束
					// 减少连续播放的卡顿
					if (!isContinuous) {
						long presentationTimeUs = mBufferInfo.presentationTimeUs;
						if (presentationTimeUs == 0) {
							presentationTimeUs = getPresentationTimeUs();
						}
						long delay = (mDuration - presentationTimeUs) * 1000;
						if (delay > 0) {
							onSleepStart();
							sleep(delay);
							onSleepEnd();
						}
					}
					// 通知播放结束
					onFinish();
				}

				if (doLoop && isPlaying()) { // 如果是循环模式并且状态是在播放中，重新开始播放
					isFinish = false;
					mRenderStartTime = -1;
					extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
					isSawInputEOS = false;
				}
			} else if (isSeek) { // 如果是 seek，继续通知解码
				if (!isPlaying()) {
					Message.obtain(mDecodeHandler, MSG_DECODE_WORK, true).sendToTarget();
				} else {
					isSeekPosition = -1;
					mDecodeHandler.removeMessages(MSG_DECODE_WORK);
					mDecodeHandler.sendEmptyMessage(MSG_DECODE_WORK);
				}
				return;
			}
		}

		if (!isSawInputEOS || !isSawOutputEOS || isPlaying()) { // 如果解码数据没填充完，或解码还没结束，或当前是播放状态，继续解码
			mDecodeHandler.sendEmptyMessage(MSG_DECODE_WORK);
		}
	}

	private void sleep(long delay) {
		long millis = delay / 1000000;
		int nanos = (int)(delay % 1000000);
		try {
			Thread.sleep(millis, nanos);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stop() {

	}

	public void reset() {
		if (isPrepared) {
			mDecodeHandler.removeCallbacksAndMessages(null);
			waitForRelease();
			mDecodeHandler.removeCallbacksAndMessages(null);
		}
	}

	public void release() {
		isReleased = true;
		if (isPrepared) {
			mDecodeHandler.removeCallbacksAndMessages(null);
			waitForRelease();
			mDecodeHandler.removeCallbacksAndMessages(null);
			stopDecodeThread();
		}
	}

	private void waitForRelease() {
		isRelease = false;
		isPrepared = false;
		mDecodeHandler.sendEmptyMessage(MSG_RELEASE);
		synchronized (mReleaseLock) {
			while (!isRelease) {
				try {
					mReleaseLock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void releaseResource() {
		resetField();
		releaseExtractor();
		resetBufferQueue();
		releaseDecoder();
		isRelease = true;
		synchronized (mReleaseLock) {
			mReleaseLock.notifyAll();
		}
	}

	private void stopDecodeThread() {
		mDecodeThread.interrupt();
		mDecodeThread.quitSafely();
		try {
			mDecodeThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void releaseExtractor() {
		final MediaExtractor extractor = mExtractor;
		if (extractor != null) {
			extractor.release();
			mExtractor = null;
		}
	}

	private void resetBufferQueue() {
		final BufferQueue bufferQueue = mBufferQueue;
		if (bufferQueue != null) {
			bufferQueue.reset();
			mBufferQueue = null;
		}
	}

	private void releaseDecoder() {
		final MediaCodec mediaCodec = mDecoder;
		if (mediaCodec != null) {
			if (isInputBufferQueued) {
				isInputBufferQueued = false;
				try {
					mediaCodec.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			try {
				mediaCodec.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				mediaCodec.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
			mDecoder = null;
		}
	}

	/**
	 * 重置所有字段
	 */
	private void resetField() {
		isPrepared = false;
		isSawInputEOS = false;
		isSawOutputEOS = false;
		mRenderStartTime = -1;
		isLooping = false;
		isFinish = false;
		mSurface = null;
		isSeekPosition = -1;
		isClipping = false;
		mStartPos = 0;
		mEndPos = 0;
		isClippingStart = false;
		isSeekCompleted = false;
		mDataSource = null;
	}

	private class DecodeHandler extends Handler {

		DecodeHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_DECODE_WORK:
					boolean isRenderOnce = false;
					if (msg.obj instanceof Boolean) {
						isRenderOnce = true;
					}
					doDecodeWork(isRenderOnce);
					break;
				case MSG_RELEASE:
					releaseResource();
					break;
				case MSG_RESTART:
					restartDecode();
					break;
				case MSG_SEEK_TO:
					seekToImpl((SeekDataPool.SeekData)msg.obj);
					break;
				case MSG_CLIPPING:
					setClippingPos((LongPair)msg.obj);
					break;
				case MSG_RESTART_RANGE:
					if (isClipping && isFinish) {
						startClipping(mStartPos);
					}
					break;
				case MSG_RECOVER_PLAY:
					recoverWholePlayImpl();
					break;
			}
		}
	}

	/**
	 * 重新开始解码
	 */
	private void restartDecode() {

		final MediaCodec decoder = mDecoder;
		final MediaExtractor extractor = mExtractor;
		if (decoder == null || extractor == null) {
			return;
		}

		if (isInputBufferQueued) { // 是否已经往解码器塞数据
			isInputBufferQueued = false;
			try {
				decoder.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (isClipping) { // 是否是区间播放
			startClipping(mStartPos);
			return;
		}

		isSeekPosition = -1;
		isSeekCompleted = false;
		isFinish = false;
		mRenderStartTime = -1;
		extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
		if (extractor.getSampleTime() < 0 && mDataSource != null) {
			MediaExtractor extractor1 = new MediaExtractor();
			try {
				extractor1.setDataSource(mDataSource);
			} catch (IOException e) {
				e.printStackTrace();
			}
			int trackIndex = selectTrack(extractor1, isVideoDecode ? "video/" : "audio/");
			if (trackIndex < 0) {
				extractor1.release();
				extractor1 = null;
			}

			if (extractor1 != null) {
				releaseExtractor();
				extractor1.selectTrack(trackIndex);
				mExtractor = extractor1;
			}
		}
		isSawInputEOS = false;
		isSawOutputEOS = false;
		mDecodeHandler.sendEmptyMessage(MSG_DECODE_WORK);
	}

	/**
	 * seek 的实现
	 * @param seekData seek 的信息
	 */
	private void seekToImpl(@NonNull SeekDataPool.SeekData seekData) {
		isSeekPosition = -1;
		isSeekCompleted = false;
		final MediaCodec decoder = mDecoder;
		final MediaExtractor extractor = mExtractor;
		if (decoder == null || extractor == null) {
			return;
		}

		if (isInputBufferQueued) { // 是否已经往解码器塞数据
			isInputBufferQueued = false;
			try {
				decoder.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (seekData.isExact) { // 如果是准确 seek
			if (isVideoDecode) { // 如果是视频解码
				extractor.seekTo(seekData.position, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
			} else {
				extractor.seekTo(seekData.position, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
			}
			isSeekPosition = seekData.position;
		} else {
			extractor.seekTo(seekData.position, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
		}

		mSeekDataPool.recycle(seekData);
		mRenderStartTime = -1;
		isSawInputEOS = false;
		isSawOutputEOS = false;
		isFinish = false;

		// 通知开始解码
		if (!isPlaying()) {
			Message.obtain(mDecodeHandler, MSG_DECODE_WORK, true).sendToTarget();
		} else {
			isSeekPosition = -1;
			mDecodeHandler.removeMessages(MSG_DECODE_WORK);
			mDecodeHandler.sendEmptyMessage(MSG_DECODE_WORK);
		}
	}

	/**
	 * 设置区间播放信息
	 */
	private void setClippingPos(@NonNull LongPair clippingPos) {
		isSeekPosition = -1;
		isClipping = true;
		mStartPos = clippingPos.first;
		mEndPos = clippingPos.second;
		startClipping(mStartPos);
	}

	/**
	 * 退出区间播放，恢复整段视频播放
	 */
	void resetClipping() {
		isClipping = false;
		mStartPos = 0;
		mEndPos = 0;
	}

	/**
	 * 开始区间播放
	 * @param startPos 区间播放开始点
	 */
	private void startClipping(long startPos) {
		final MediaExtractor extractor = mExtractor;
		final MediaCodec decoder = mDecoder;
		if (decoder == null || extractor == null) {
			return;
		}
		if (isInputBufferQueued) { // 是否已经往解码器塞数据
			isInputBufferQueued = false;
			try {
				decoder.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (isVideoDecode) { // 如果是视频解码，需要准确 seek 到那位置
			extractor.seekTo(startPos, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
		} else {
			extractor.seekTo(startPos, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
		}

		isClippingStart = false;
		isSeekPosition = -1;
		mRenderStartTime = -1;
		isSawInputEOS = false;
		isSawOutputEOS = false;
		isFinish = false;
		isSeekCompleted = false;
		// 通知开始解码
		Message.obtain(mDecodeHandler, MSG_DECODE_WORK, true).sendToTarget();
	}

	private void recoverWholePlayImpl() {
		resetClipping();

		restartDecode();
	}

	protected abstract boolean parseFormat(@NonNull String dataSource, @NonNull MediaFormat format);

	protected abstract boolean isPlaying();

	protected abstract long PTSSync(long presentationTimeUs, boolean isRenderOnce);

	protected long getPresentationTimeUs() {
		return 0;
	}

	protected void onDecodeData(@NonNull MediaCodec.BufferInfo bufferInfo, @Nullable ByteBuffer buffer) {
	}

	protected void onStart() {

	}

	protected void onFinish() {
		isFinish = true;
		isSeekPosition = -1;
		final MediaCodec decoder = mDecoder;
		if (decoder != null && isInputBufferQueued) {
			isInputBufferQueued = false;
			try {
				decoder.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		isClippingStart = false;
	}

	protected void onClippingStart() {

	}

	protected void onClippingFinish() {
		onFinish();
	}

	protected void onSeekCompleted(long presentationTimeUs) {
		isSeekCompleted = true;
	}

	protected void onPositionChanged(int position) {

	}

	protected void onSleepStart() {

	}

	protected void onSleepEnd() {

	}

	private static int selectTrack(MediaExtractor extractor, @NonNull String mimeType) {
		int numTracks = extractor.getTrackCount();
		for (int i = 0; i < numTracks; i++) {
			MediaFormat format = extractor.getTrackFormat(i);
			String mime = format.getString(MediaFormat.KEY_MIME);
//			if (mime.contains("unknown")) {
//				continue;
//			}
			if (mime.startsWith(mimeType)) {
				return i;
			}
		}

		return -1;
	}

	@SuppressWarnings("deprecation")
	private static class BufferQueue {

		private MediaCodec mMediaCodec;

		BufferQueue(MediaCodec mediaCodec) {
			mMediaCodec = mediaCodec;
		}

		ByteBuffer getInputBuffer(int index) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				return mMediaCodec.getInputBuffer(index);
			}

			return mMediaCodec.getInputBuffers()[index];
		}

		ByteBuffer getOutputBuffer(int index) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				return mMediaCodec.getOutputBuffer(index);
			}

			return mMediaCodec.getOutputBuffers()[index];
		}

		void reset() {
			mMediaCodec = null;
		}
	}

	private static class LongPair {
		final long first;
		final long second;

		LongPair(long first, long second) {
			this.first = first;
			this.second = second;
		}
	}

	/**
	 * 获取视频的时长
	 */
	private static long getVideoDuration(String path) {
		final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		try {
			retriever.setDataSource(path);
			String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
			if (!TextUtils.isEmpty(durationStr)) {
				return Long.valueOf(durationStr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			retriever.release();
		}

		return 0;
	}
}
