package poco.cn.medialibs.player2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import poco.cn.medialibs.player2.base.VideoPlayer;


/**
 * Created by: fwc
 * Date: 2018/12/6
 */
public class DefaultPlayer implements IPlayer
{

	@NonNull
	private final VideoPlayer mPlayer;
//	private final VideoPlayer mPlayer;

	private int mId;

	private OnPlayListener mOnPlayListener;

	private boolean isPrepared;

	private boolean mPendingStart;
	private boolean mPendingRestart;

	private int mPendingSeek = -1;
	private boolean isExactSeek;

	@Nullable
	private ISurface mSurface;

	DefaultPlayer() {
		mPlayer = new VideoPlayer();
		mPlayer.setContinuousPlay(true);
//		mPlayer.setFixFrameRate(true);
		mPlayer.setOnPlayListener(mOnPlayListenerImpl);
	}

	@Override
	public void setDataSource(@NonNull String path) {
		mPlayer.setDataSource(path);
	}

	@Override
	public void setSurface(@NonNull ISurface surface) {
		if (!(surface instanceof VideoPlayer.PlaySurface)) {
			throw new RuntimeException();
		}
		mSurface = surface;
		mPlayer.setSurface((VideoPlayer.PlaySurface)surface);
	}

	@Override
	public void setId(int videoId) {
		mId = videoId;
	}

	@Override
	public boolean isPrepare() {
		return mPlayer.isPrepare();
	}

	@Override
	public boolean isPlaying() {
		return mPlayer.isPlaying();
	}

	@Override
	public boolean isSeeking() {
		return mPlayer.isSeeking();
	}

	@Override
	public void prepare() {
		if (isPrepared) {
			throw new IllegalStateException();
		}
		mPlayer.prepareAsync(mId);
	}

	@Override
	public void start() {
		mPendingStart = false;
		if (isPrepared) {
			mPlayer.start();
		} else {
			mPendingStart = true;
		}
	}

	@Override
	public void restart() {
		mPendingRestart = false;
		if (isPrepared) {
			mPlayer.restart();
		} else {
			mPendingSeek = -1;
			mPendingStart = false;
			mPendingRestart = true;
		}
	}

	@Override
	public void seekTo(int position) {
		mPendingSeek = -1;
		isExactSeek = false;
		if (isPrepared) {
			mPlayer.seekTo(position);
		} else {
			mPendingStart = false;
			mPendingRestart = false;
			mPendingSeek = position;
		}
	}

	@Override
	public void seekTo(int position, boolean isExact) {
		mPendingSeek = -1;
		if (isPrepared) {
			mPlayer.seekTo(position, true);
		} else {
			mPendingStart = false;
			mPendingRestart = false;
			mPendingSeek = position;
			isExactSeek = true;
		}
	}

	@Override
	public void seekToEnd() {
		final long duration = getDuration();
		if (duration > 0) {
			seekTo((int)duration - 1, true);
		}
	}

	@Override
	public void setRangePlay(long startPos, long endPos) {
		mPlayer.setRangePlay(startPos, endPos);
	}

	@Override
	public void recoverWholePlay() {
		mPlayer.recoverWholePlay();
	}

	@Override
	public void pause() {
		mPendingStart = false;
		mPendingRestart = false;
		mPlayer.pause();
	}

	@Override
	public void stop() {
		mPlayer.stop();
	}

	@Override
	public void lock() {
		ISurface surface = mSurface;
		if (surface != null) {
			surface.lock();
		}
	}

	@Override
	public void unlock(int timestamp) {
		ISurface surface = mSurface;
		if (surface != null) {
			surface.unlock(timestamp);
		}
	}

	@Override
	public void reset() {
		resetField();
		unlock(0);
		mPlayer.reset();
	}

	@Override
	public void release() {
		resetField();
		unlock(0);
		mPlayer.release();
	}

	private void resetField() {
		mPendingStart = false;
		isPrepared = false;
		mPendingSeek = -1;
		isExactSeek = false;
		mSurface = null;
	}

	@Override
	public void setOnPlayListener(@Nullable OnPlayListener listener) {
		mOnPlayListener = listener;
	}

	@Override
	public void setLooping(boolean isLooping) {
		mPlayer.setLooping(isLooping);
	}

	@Override
	public boolean isLooping() {
		return mPlayer.isLooping();
	}

	@Override
	public void setVolume(float volume) {
		mPlayer.setVolume(volume);
	}

	@Override
	public float getVolume() {
		return mPlayer.getVolume();
	}

	@Override
	public boolean isPause() {
		return mPlayer.isPause();
	}

	@Override
	public long getCurrentPosition() {
		return mPlayer.getCurrentPosition();
	}

	@Override
	public long getDuration() {
		return mPlayer.getCurrentPosition();
	}

	@SuppressWarnings("all")
	private VideoPlayer.OnPlayListener mOnPlayListenerImpl = new VideoPlayer.OnPlayListener() {

		@Override
		protected void onPrepared(VideoPlayer player) {
			isPrepared = true;
			if (mPendingSeek > -1) {
				player.seekTo(mPendingSeek, isExactSeek);
				mPendingSeek = -1;
				isExactSeek = false;
			}

			if (mPendingStart) {
				mPendingStart = false;
				player.start();
			} else if (mPendingRestart) {
				mPendingRestart = false;
				player.restart();
			}
		}

		@Override
		protected void onStart() {
			final OnPlayListener listener = mOnPlayListener;
			if (listener != null) {
				listener.onStart();
			}
		}

		@Override
		protected void onSeekCompleted(@NonNull VideoPlayer player) {
			final OnPlayListener listener = mOnPlayListener;
			if (listener != null) {
				listener.onSeekCompleted(DefaultPlayer.this);
			}
		}

		@Override
		protected void onFinish() {
			final OnPlayListener listener = mOnPlayListener;
			if (listener != null) {
				listener.onFinish();
			}
		}

		@Override
		protected void onClipStart() {
			final OnPlayListener listener = mOnPlayListener;
			if (listener != null) {
				listener.onRangeStart();
			}
		}

		@Override
		protected void onClipFinish() {
			final OnPlayListener listener = mOnPlayListener;
			if (listener != null) {
				listener.onRangeFinish();
			}
		}

		@Override
		protected void onPositionChanged(int position) {
			final OnPlayListener listener = mOnPlayListener;
			if (listener != null) {
				listener.onPositionChanged(position);
			}
		}
	};
}
