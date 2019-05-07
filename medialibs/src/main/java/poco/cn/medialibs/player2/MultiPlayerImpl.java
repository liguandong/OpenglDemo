package poco.cn.medialibs.player2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;

import poco.cn.medialibs.utils.FileUtil;


/**
 * Created by: fwc
 * Date: 2018/12/6
 */
@SuppressWarnings("WeakerAccess")
public final class MultiPlayerImpl {

	@NonNull
	private final Context mContext;

	@NonNull
	private IPlayer mPlayer1;

	@NonNull
	private IPlayer mPlayer2;

	@IPlayer.State
	private int mState = IPlayer.State.IDLE;

	@Nullable
	private MultiSurface mSurface;

	/**
	 * 是否单视频播放
	 */
	private boolean isSingleVideo;

	@Nullable
	private PlayInfo[] mPlayInfos;

	/**
	 * 当前播放的视频 index
	 */
	private int mCurrentIndex = -1;

	/**
	 * 播放的音量 0.0 ~ 1.0
	 */
	private float mVolume = 1f;

	private boolean isLooping = true;

	@NonNull
	private final Handler mPlayHandler;

	@Nullable
	private OnPlayListener mOnPlayListener;

	/**
	 * 调用 seekTo 时用于标记 mPlayer2 是否也调用了 seekTo
	 * 这样在调用 start 播放时根据这个标记是否需要播放 mPlayer2
	 */
	private boolean isNextVideoSeek = false;

	/**
	 * 转场模式下开始播放的时间点
	 */
	private int mTransitionStartTime = -1;

	/**
	 * 转场模式下结束播放的时间点
	 */
	private int mTransitionEndTime = -1;

	/**
	 * 用于区分是片头还是片尾转场
	 */
	private boolean isStartTransition;

	public MultiPlayerImpl(@NonNull Context context) {
		mContext = context;

		mPlayer1 = new DefaultPlayer();
		mPlayer2 = new DefaultPlayer();

		mPlayHandler = new Handler(Looper.myLooper());
	}

	/**
	 * 设置 OnPlayListener 监听
	 * @see OnPlayListener
	 */
	public void setOnPlayListener(@Nullable OnPlayListener listener) {
		mOnPlayListener = listener;
	}

	/**
	 * 设置 MultiSurface
	 * @see MultiSurface
	 */
	public void setMultiSurface(@NonNull MultiSurface surface) {
		if (mState != IPlayer.State.IDLE) {
			throw new IllegalStateException();
		}

		mSurface = surface;
	}

	/**
	 * 设置播放的视频信息
	 * @see PlayInfo
	 */
	public void setPlayInfos(@NonNull PlayInfo... playInfos) {
		if (mState != IPlayer.State.IDLE) {
			throw new IllegalStateException();
		}

		if (playInfos.length == 0) {
			throw new IllegalArgumentException("the playInfos are empty");
		}

		isSingleVideo = playInfos.length == 1;

		mPlayInfos = new PlayInfo[playInfos.length];
		for (int i = 0; i < playInfos.length; i++) {
			mPlayInfos[i] = playInfos[i].Clone();
		}
	}

	/**
	 * 在 {@link #start()} 前需准备
	 * @param index 视频下标，决定从哪个视频开始播放
	 */
	public void prepare(int index) {
		if (mState != IPlayer.State.IDLE) {
			throw new IllegalStateException();
		}

		final MultiSurface surface = mSurface;
		final PlayInfo[] playInfos = mPlayInfos;
		checkNull(surface);
		checkNull(playInfos);

		mCurrentIndex = MathUtils.clamp(index, 0, playInfos.length - 1);

		preparePlayer(mPlayer1, surface.getCurSurface(), playInfos[mCurrentIndex], mOnPlayListenerInner);
		prepareNextVideo(surface.getNextSurface());
		mState = IPlayer.State.PREPARED;
	}

	/**
	 * 准备 Player
	 * @param player    Player 实例 {@link IPlayer}
	 * @param surface   ISurface 实例 {@link IPlayer.ISurface}
	 * @param info      PlayInfo 视频播放信息 {@link PlayInfo}
	 * @param listener  OnPlayListener 监听器 {@link IPlayer.OnPlayListener}
	 */
	private void preparePlayer(@Nullable IPlayer player, @Nullable IPlayer.ISurface surface, @NonNull PlayInfo info, @Nullable IPlayer.OnPlayListener listener) {
		if (player == null || surface == null) {
			return;
		}
		if(info == null || !FileUtil.isFileExist(info.path)){
			return;
		}
		player.reset();
		player.setOnPlayListener(listener);
		if (info.isMute) {
			player.setVolume(0);
		} else {
			player.setVolume(mVolume);
		}

		player.setDataSource(info.path);
		player.setSurface(surface);
		player.setId(info.videoId);
		player.prepare();
	}

	/**
	 * 准备 mPlayer2
	 * @param sur face ISurface 实例 {@link IPlayer.ISurface}
	 */
	private void prepareNextVideo(@Nullable IPlayer.ISurface surface) {
		final PlayInfo[] playInfos = mPlayInfos;
		if (isSingleVideo || playInfos == null || surface == null) {
			return;
		}

		int next = getNextIndex();
		if (next >= 0 && next < playInfos.length) {
			preparePlayer(mPlayer2, surface, playInfos[next], null);
		}
	}

	/**
	 * 获取下一个视频下标
	 */
	private int getNextIndex() {
		final PlayInfo[] playInfos = mPlayInfos;
		if (playInfos == null) {
			return -1;
		}
		int next = mCurrentIndex + 1;
		if (isLooping) {
			next = next % playInfos.length;
		}

		return next;
	}

	/**
	 * 开始播放视频
	 */
	public void start() {
		mPlayHandler.removeCallbacks(mEndTransitionStart);
		if (inStates(IPlayer.State.PREPARED, IPlayer.State.PAUSE, IPlayer.State.FINISH)) {
			if (mRangeStartPos >= 0 && mRangeEndPos >= 0) {
				mPendingStart = true;
				return;
			}
			resetSeekState();
			mPlayer1.start();

			if (mPlayer2.isPause() || isNextVideoSeek) {
				mPlayer2.start();
				isNextVideoSeek = false;
			}

			mState = IPlayer.State.START;
		}
	}

	/**
	 * 提前播放下一个视频，主要用在添加了 Blend 类型的转场
	 */
	private void startNext() {
		if (mState == IPlayer.State.START) {
			mPlayer2.start();
		}
	}

	/**
	 * 暂停播放视频
	 */
	public void pause() {
		mPendingStart = false;
		mPlayHandler.removeCallbacks(mEndTransitionStart);
		if (inStates(IPlayer.State.PREPARED, IPlayer.State.START)) {
			mPlayer1.pause();

			if (mPlayer2.isPlaying()) {
				mPlayer2.pause();
			}

			mState = IPlayer.State.PAUSE;
		}
	}

	/**
	 * 用于标记是否 seek 结束了
	 * 注意，当且仅当是精确 seek 时才用到
	 */
	private boolean isSeekCompleted = true;

	/**
	 * 当 isSeekCompleted 为 false 用于记录 seek 的视频下标
	 */
	private int mIndex = -1;

	/**
	 * 当 isSeekCompleted 为 false 用于记录 seek 的视频位置
	 */
	private int mPosition = -1;

	/**
	 * 重置 seek 的状态
	 */
	private void resetSeekState() {
		isSeekCompleted = true;
		mIndex = -1;
		mPosition = -1;
		mRangeStartPos = -1;
		mRangeEndPos = -1;
		mPendingStart = false;
	}

	/**
	 * seekTo 到某一个视频的某个位置
	 * @param index    视频下标
	 * @param position 视频位置
	 * @param isExact  是否精确 seekTo
	 */
	public void seekTo(int index, int position, boolean isExact) {

		if (isExact && !isSeekCompleted) { // 如果是准确 seek 并且还没 seek 完成，等待
			mIndex = index;
			mPosition = position;
			return;
		}

		// 重置 seek 的状态
		resetSeekState();

		final PlayInfo[] playInfos = mPlayInfos;
		final MultiSurface surface = mSurface;
		if (playInfos == null || surface == null) {
			return;
		}
		isSeekCompleted = false;
		index = MathUtils.clamp(index, 0, playInfos.length - 1);

		if (isSingleVideo) { // 是否是单视频模式
			if (!mPlayer1.isPrepare()) {
				reset();
				prepare(0);
			}
			mPlayer1.seekTo(position, isExact);
			return;
		}

		isNextVideoSeek = false;

		final int currDuration = playInfos[index].duration;
		if (position >= currDuration) {
			position = currDuration - 1;
		} else if (position < 0) {
			position = 0;
		}

		int time = 0;
		if (index > 0) {
			ITransitionInfo item = playInfos[index - 1].endTransition;
			if (item != null && item.isBlendTransition()) { // 如果设置了转场并且是 Blend Transition
				time = item.getTime();
			}
		}
		if (position < time && position != 0) { // 如果当前时间点位于 Blend Transition 中
			// 将当前播放视频的 index 设置为上一个视频的 index
			mCurrentIndex = index - 1;

			// 重新播放器实例
			mPlayer1.reset();
			mPlayer2.reset();

			// 先初始化当前播放视频信息
			preparePlayer(mPlayer1, surface.getCurSurface(), playInfos[mCurrentIndex], mOnPlayListenerInner);
			// 初始化下一个播放的视频信息
			prepareNextVideo(surface.getNextSurface());

			final int duration = playInfos[mCurrentIndex].duration;

			// 分别对两个播放器进行 seek 操作
			mPlayer1.seekTo(duration - position, isExact);
			mPlayer2.seekTo(position, isExact);

			isNextVideoSeek = true;
			return;
		}

		time = 0;
		ITransitionInfo item = playInfos[index].endTransition;
		if (item != null && item.isBlendTransition()) {
			time = item.getTime();
		}
		if (mCurrentIndex == index) { // 如果当前播放视频的 index 和要准备 seek 的 index 一样，直接 seek
			seekToInner(position, currDuration, time, isExact);
		} else {
			mCurrentIndex = index;

			// 重新播放器实例
			mPlayer1.reset();
			mPlayer2.reset();

			// 先初始化当前播放视频信息
			preparePlayer(mPlayer1, surface.getCurSurface(), playInfos[mCurrentIndex], mOnPlayListenerInner);
			// 初始化下一个播放的视频信息
			prepareNextVideo(surface.getNextSurface());

			seekToInner(position, currDuration, time, isExact);
		}
	}

	private void seekToInner(int position, int duration, int time, boolean isExact) {
		mPlayer1.seekTo(position, isExact);

		boolean isInTransition = position >= duration - time && position <= duration;
		if (time != 0 && isInTransition) { // 如果当前时间点位于 Blend Transition 中，Player2 实例也要 seek
			mPlayer2.seekTo(duration - position, isExact);
			isNextVideoSeek = true;
		}
	}

	/**
	 * seekTo 到当前播放的视频的末尾
	 * @param index  视频下标
	 */
	public void seekToEnd(int index) {
		resetSeekState();
		mPlayer1.seekToEnd();
	}

	/**
	 * 重置播放器状态
	 */
	public void reset() {
		resetSeekState();
		isNextVideoSeek = false;
		mPlayHandler.removeCallbacksAndMessages(null);
		mCurrentIndex = -1;
		mPlayer1.reset();
		mPlayer2.reset();

		if (mSurface != null) {
			mSurface.resetSurface();
		}

		mState = IPlayer.State.IDLE;
	}

	/**
	 * 释放播放器资源
	 */
	public void release() {
		resetSeekState();
		isNextVideoSeek = false;
		mState = IPlayer.State.RELEASE;

		mPlayHandler.removeCallbacksAndMessages(null);
		mCurrentIndex = -1;
		mPlayer1.release();
		mPlayer2.release();
	}

	/**
	 * 设置播放的音量
	 * @param volume 声音音量，0.0 ~ 1.0
	 */
	public void setVolume(@FloatRange(from = 0.0, to = 1.0) float volume) {
		mVolume = volume;
		final PlayInfo[] playInfos = mPlayInfos;
		if (playInfos == null) {
			return;
		}
		final int index = mCurrentIndex;

		if (index >= 0 && index < playInfos.length) {
			if (!playInfos[index].isMute) {
				mPlayer1.setVolume(volume);
			}
		}
		int next = index + 1;
		if (isLooping) {
			next = next % playInfos.length;
		}
		if (next >= 0 && index < playInfos.length) {
			if (!playInfos[next].isMute) {
				mPlayer2.setVolume(volume);
			}
		}
	}

	/**
	 * 获取当前播放的音量
	 */
	public float getVolume() {
		return mVolume;
	}

	/**
	 * 设置是否循环播放
	 */
	public void setLooping(boolean isLooping) {
		this.isLooping = isLooping;
	}

	/**
	 * 获取当前播放是否是循环播放
	 */
	public boolean isLooping() {
		return isLooping;
	}

	/**
	 * 进入转场模式并开始播放
	 * @param transitionStartTime 转场模式下开始播放的时间点
	 * @param transitionEndTime   转场模式下结束播放的时间点
	 */
	public void startTransition(int transitionStartTime, int transitionEndTime) {
		mPlayHandler.removeCallbacks(mEndTransitionStart);
		mTransitionStartTime = transitionStartTime;
		mTransitionEndTime = transitionEndTime;
		mPlayer1.seekTo(transitionStartTime, true);
		mPlayer1.start();
		if (!isSingleVideo) {
			mPlayer2.seekTo(0, true);
		}
	}

	/**
	 * 在转场模式下更新转场信息
	 * @param start    ITransitionInfo 实例 {@link ITransitionInfo}
	 * @param end      ITransitionInfo 实例 {@link ITransitionInfo}
	 */
	public void updateTransition(ITransitionInfo start, ITransitionInfo end) {
		mPlayHandler.removeCallbacks(mEndTransitionStart);
		final PlayInfo[] playInfos = mPlayInfos;
		if (playInfos == null) {
			return;
		}
		if (isSingleVideo) {
			if (isStartTransition) {
				playInfos[0].startTransition = start;
			} else {
				playInfos[0].endTransition = end;
			}
			return;
		}
		if (playInfos.length == 2) {
			playInfos[0].endTransition = start;
			playInfos[1].startTransition = end;
		}
	}

	/**
	 * 在转场模式下重新播放
	 */
	public void restartTransition() {
		if (isSingleVideo) {
			mPlayer1.seekTo(mTransitionStartTime, true);
			mPlayer1.start();
			mState = IPlayer.State.START;
			return;
		}
		restartTransitionImpl();
		mState = IPlayer.State.START;
	}

	/**
	 * 在转场模式下更新转场的播放时间（速度）
	 */
	public void updateTransitionTime(int time) {
		mPlayHandler.removeCallbacks(mEndTransitionStart);
		final PlayInfo[] playInfos = mPlayInfos;
		if (playInfos == null) {
			return;
		}

		if (isSingleVideo) {
			if (isStartTransition) {
				if (playInfos[0].startTransition != null) {
					playInfos[0].startTransition.setTime(time);
				}
			} else {
				if (playInfos[0].endTransition != null) {
					playInfos[0].endTransition.setTime(time);
				}
			}
			return;
		}
		if (playInfos.length == 2) {
			ITransitionInfo item = mPlayInfos[0].endTransition;
			if (item == null) {
				return;
			}

			item.setTime(time);
			item = mPlayInfos[1].startTransition;
			if (item != null) {
				item.setTime(time);
			}
		}
	}

	/**
	 * 退出转场模式
	 */
	public void exitTransition() {
		mPlayHandler.removeCallbacks(mEndTransitionStart);
		mPlayer1.unlock(0);
		mTransitionStartTime = -1;
		mTransitionEndTime = -1;
		isStartTransition = false;
	}

	/**
	 * 在转场模式下用来标记是片头转场还是片尾转场
	 * @param isStartTransition  为 true 时是片头转场
	 */
	public void setStartTransition(boolean isStartTransition) {
		this.isStartTransition = isStartTransition;
	}

	/**
	 * 设置某一个视频播放是否静音
	 * @param index   视频下标
	 * @param isMute  是否静音，为 true 即静音
	 */
	public void changeVideoMute(int index, boolean isMute) {
		final PlayInfo[] playInfos = mPlayInfos;
		if (playInfos == null) {
			return;
		}
		if (index < 0 || index >= playInfos.length) {
			return;
		}

		playInfos[index].isMute = isMute;
		if (mCurrentIndex == index) {
			if (isMute) {
				mPlayer1.setVolume(0);
			} else {
				mPlayer1.setVolume(mVolume);
			}
		}
	}

	/**
	 * 用于判断当前的播放状态
	 */
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

	/**
	 * 在 seek 未结束是用于记录区间播放的开始时间点
	 */
	private int mRangeStartPos = -1;

	/**
	 * 在 seek 未结束是用于记录区间播放的结束时间点
	 */
	private int mRangeEndPos = -1;

	/**
	 * 设置区间播放后当还没准备完成时（即等待 seek 完成），此时调用 start 开始播放时需要记录
	 */
	private boolean mPendingStart;

	/**
	 * 在单视频模式下设置区间播放的开始时间点和结束时间点
	 * @param startPos   区间播放的开始时间点
	 * @param endPos     区间播放的结束时间点
	 */
	public void setRangePlay(int startPos, int endPos) {
		if (!isSeekCompleted) { // 等待 seekTo 完成
			mRangeStartPos = startPos;
			mRangeEndPos = endPos;
			return;
		}
		mRangeStartPos = -1;
		mRangeEndPos = -1;
		if (startPos == -1 && endPos == -1) {
			mPlayer1.recoverWholePlay();
		} else {
			mPlayer1.setRangePlay(startPos, endPos);
		}
		if (mPendingStart) {
			start();
		}
	}

	/**
	 * 当前视频播放结束处理
	 * @param index  视频下标
	 */
	private void onVideoFinish(int index) {
		// Play Thread
		final PlayInfo[] playInfos = mPlayInfos;
		final MultiSurface surface = mSurface;
		if (playInfos == null || surface == null) {
			return;
		}

		if (isSingleVideo) { // 单视频模式
			if (mTransitionStartTime >= 0) { // 是否是转场模式
				if (isStartTransition) { // 片头转场
					mEndTransitionStart.run();
				} else {
					// 片尾转场
					mPlayHandler.postDelayed(mEndTransitionStart, 100);
				}
				return;
			}
			if (isLooping) { // 是否是循环播放
				if (mState == IPlayer.State.PAUSE) {
					mPlayer1.seekTo(0, true);
					mPlayer1.pause();
				} else if (mState == IPlayer.State.START) {
					mPlayer1.restart();
				}
			}
			return;
		}

		if (mTransitionStartTime >= 0) {
			// 转场模式
			if (index == 0) {
				// 不交换播放器
				mCurrentIndex = 1;
				mPlayer1.setOnPlayListener(null);
				mPlayer2.setOnPlayListener(mOnPlayListenerInner);
				mPlayer2.start();
				mPlayer1.lock();
				surface.onChangeSurface(true);
				mPlayer1.seekTo(mTransitionStartTime, true);
			} else {
				// 转场模式下重新开始播放
				restartTransitionImpl();
			}
			return;
		}

		if (index == mCurrentIndex) {
			mPlayer1.setOnPlayListener(null);
			mPlayer1.reset();
			mCurrentIndex = getNextIndex();
			if (mCurrentIndex < playInfos.length) {
				// 交换播放器实例
				final IPlayer player = mPlayer1;
				mPlayer1 = mPlayer2;
				mPlayer2 = player;

				final IPlayer.ISurface nextNextSurface = surface.getNextNextSurface();
				// 通知 GL Thread 那边需要交换 Surface
				surface.onChangeSurface(false);

				mPlayer1.setOnPlayListener(mOnPlayListenerInner);
				if (mState == IPlayer.State.START) {
					mPlayer1.start();
				}

				// 准备下一个视频的播放
				prepareNextVideo(nextNextSurface);
			}
		}
	}

	/**
	 * 在转场模式下，播放片尾时，用于延迟一下重新开始播放，确保片尾转场完全合上
	 * 注意，后来在 PlaySurface 下增加了
	 * setVideoDuration() 方法在 GL 渲染修复了视频时长的误差，可以确保尾转场完全合上
	 */
	private Runnable mEndTransitionStart = new Runnable() {
		@Override
		public void run() {
			mPlayer1.unlock(mTransitionStartTime);
			mPlayer1.seekTo(mTransitionStartTime, true);
			mPlayer1.start();
		}
	};

	/**
	 * 转场模式下重新开始播放
	 */
	private void restartTransitionImpl() {
		mPlayHandler.removeCallbacks(mEndTransitionStart);
		mCurrentIndex = 0;
		mPlayer2.pause();
		mPlayer2.setOnPlayListener(null);
		mPlayer1.setOnPlayListener(mOnPlayListenerInner);
		mPlayer1.unlock(mTransitionStartTime);
		mPlayer2.seekTo(0, true);
		mPlayer1.start();
		if (mSurface != null) {
			mSurface.resetSurface();
		}
	}

	private IPlayer.OnPlayListener mOnPlayListenerInner = new IPlayer.OnPlayListener() {
		@Override
		public void onStart() {
			final OnPlayListener listener = mOnPlayListener;
			final int index = Math.max(mCurrentIndex, 0);
			if (listener != null) {
				listener.onStart(index);
			}
		}

		@Override
		public void onSeekCompleted(@NonNull IPlayer player) {
			isSeekCompleted = true;
			if (mIndex != -1) {
				seekTo(mIndex, mPosition, true);
			} else if (mRangeStartPos != -1 && mRangeEndPos != -1) {
				setRangePlay(mRangeStartPos, mRangeEndPos);
			}
			final OnPlayListener listener = mOnPlayListener;
			final int index = Math.max(mCurrentIndex, 0);
			if (listener != null) {
				listener.onSeekCompleted(index, (int)player.getCurrentPosition());
			}
		}

		@Override
		public void onFinish() {
			isSeekCompleted = true;
			final OnPlayListener listener = mOnPlayListener;
			final int index = Math.max(mCurrentIndex, 0);
			if (listener != null) {
				listener.onFinish(index);
			}
			onVideoFinish(index);
		}

		@Override
		public void onRangeStart() {
			final OnPlayListener listener = mOnPlayListener;
			if (listener != null) {
				listener.onRangeStart();
			}
		}

		@Override
		public void onRangeFinish() {
			final OnPlayListener listener = mOnPlayListener;
			if (listener != null) {
				listener.onRangeFinish();
			}
		}

		@Override
		public void onPositionChanged(int position) {
			final int index = Math.max(mCurrentIndex, 0);
			if (mTransitionStartTime >= 0 && position >= mTransitionEndTime) {
				// 转场模式
				if (isSingleVideo) {
					mPlayer1.seekTo(mTransitionStartTime, true);
					mPlayer1.start();
					return;
				} else if (index == 1) {
					restartTransitionImpl();
					return;
				}
			}
			final PlayInfo[] playInfos = mPlayInfos;
			if (!isSingleVideo && index < playInfos.length - 1 && !mPlayer2.isPlaying()) {
				PlayInfo playInfo = playInfos[index];
				if (playInfo.endTransition != null) {
					final ITransitionInfo info = playInfo.endTransition;
					if (info.isBlendTransition() && position >= playInfo.duration - info.getTime()) {
						startNext();
					}
				}
			}
			final OnPlayListener listener = mOnPlayListener;
			if (listener != null) {
				listener.onPositionChanged(index, position);
			}
		}
	};

	private static void checkNull(@Nullable Object obj) {
		if (obj == null) {
			throw new NullPointerException();
		}
	}
}
