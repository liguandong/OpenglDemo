package poco.cn.medialibs.player2;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import poco.cn.medialibs.player2.base.SeekDataPool;


/**
 * Created by: fwc
 * Date: 2019/1/2
 * 主要用于包装 MultiPlayerImpl，提供线程切换的封装
 */
public class MultiPlayer implements IMessage {

	@Nullable
	private volatile Context mContext;

	@Nullable
	private volatile MultiPlayerImpl mMultiPlayerImpl;

	private HandlerThread mPlayThread;
	private final PlayHandler mPlayHandler;

	@NonNull
	private final SeekDataPool mSeekDataPool;

	public MultiPlayer(@NonNull Context context) {
		mContext = context;

		mPlayThread = new HandlerThread("Play Thread");
		mPlayThread.start();
		mPlayHandler = new PlayHandler(mPlayThread.getLooper());
		mPlayHandler.sendEmptyMessage(MSG_INIT);

		mSeekDataPool = new SeekDataPool();
	}

	public void setOnPlayListener(@Nullable OnPlayListener listener) {
		Message.obtain(mPlayHandler, MSG_SET_LISTENER, listener).sendToTarget();
	}

	public void setSurface(MultiSurface surface) {
		Message.obtain(mPlayHandler, MSG_SET_SURFACE, surface).sendToTarget();
	}

	public void setPlayInfos(PlayInfo... playInfos) {
		Message.obtain(mPlayHandler, MSG_SET_PLAY_INFOS, playInfos).sendToTarget();
	}

	public void prepare(int index) {
		Message.obtain(mPlayHandler, MSG_PREPARE, index, 0).sendToTarget();
	}

	public void start() {
		Message.obtain(mPlayHandler, MSG_START).sendToTarget();
	}

	public void pause() {
		Message.obtain(mPlayHandler, MSG_PAUSE).sendToTarget();
	}

	public void seekTo(int index, int position, boolean isExact) {
		SeekDataPool.SeekData seekData = mSeekDataPool.obtain();
		seekData.position = position;
		seekData.isExact = isExact;
		Message.obtain(mPlayHandler, MSG_SEEK_TO, index, 0, seekData).sendToTarget();
	}

	public void seekToEnd(int index) {
		Message.obtain(mPlayHandler, MSG_SEEK_TO_END, index, 0).sendToTarget();
	}

	public void reset() {
		Message.obtain(mPlayHandler, MSG_RESET).sendToTarget();
	}

	public void release() {
		Message.obtain(mPlayHandler, MSG_RELEASE).sendToTarget();
	}

	public void setVolume(float volume) {
		Message.obtain(mPlayHandler, MSG_SET_VOLUME, volume).sendToTarget();
	}

	public void setLooping(boolean looping) {
		Message.obtain(mPlayHandler, MSG_SET_LOOPING, looping).sendToTarget();
	}

	public void changeVideoMute(int index, boolean isMute) {
		Message.obtain(mPlayHandler, MSG_CHANGE_VIDEO_MUTE, index, 0, isMute).sendToTarget();
	}

	public void queueEvent(Runnable r) {
		mPlayHandler.post(r);
	}

	public void startTransition(int transitionStartTime, int transitionEndTime) {
		Message.obtain(mPlayHandler, MSG_START_TRANSITION, transitionStartTime, transitionEndTime).sendToTarget();
	}

	public void setStartTransition(boolean isStartTransition) {
		Message.obtain(mPlayHandler, MSG_SET_START_TRANSITION, isStartTransition).sendToTarget();
	}

	public void updateTransition(ITransitionInfo start, ITransitionInfo end) {
		Pair<ITransitionInfo, ITransitionInfo> pair = new Pair<>(start, end);
		Message.obtain(mPlayHandler, MSG_UPDATE_TRANSITION, pair).sendToTarget();
	}

	public void restartTransition() {
		pause();
		Message.obtain(mPlayHandler, MSG_RESTART_TRANSITION).sendToTarget();
	}

	public void updateTransitionTime(int time) {
		Message.obtain(mPlayHandler, MSG_UPDATE_TRANSITION_TIME, time, 0).sendToTarget();
	}

	public void exitTransition() {
		Message.obtain(mPlayHandler, MSG_EXIT_TRANSITION).sendToTarget();
	}

	public void setRangePlay(int startPos, int endPos) {
		Message.obtain(mPlayHandler, MSG_SET_RANGE_PLAY, startPos, endPos).sendToTarget();
	}

	private class PlayHandler extends Handler {

		PlayHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_INIT: {
					final Context context = mContext;
					if (context != null) {
						mMultiPlayerImpl = new MultiPlayerImpl(context);
					}
					break;
				}
				case MSG_SET_LISTENER: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					final OnPlayListener listener = (OnPlayListener)msg.obj;
					if (multiPlayer != null) {
						multiPlayer.setOnPlayListener(listener);
					}
					break;
				}
				case MSG_SET_SURFACE: {
					MultiSurface surface = (MultiSurface)msg.obj;
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.setMultiSurface(surface);
					}
					break;
				}
				case MSG_SET_PLAY_INFOS: {
					final PlayInfo[] playInfos = (PlayInfo[])msg.obj;
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null && playInfos != null) {
						multiPlayer.setPlayInfos(playInfos);
					}
					break;
				}
				case MSG_PREPARE: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.prepare(msg.arg1);
					}
					break;
				}
				case MSG_START: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.start();
					}
					break;
				}
				case MSG_PAUSE: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.pause();
					}
					break;
				}
				case MSG_SEEK_TO: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					final SeekDataPool.SeekData seekData = (SeekDataPool.SeekData)msg.obj;
					if (multiPlayer != null) {
						multiPlayer.seekTo(msg.arg1, (int)seekData.position, seekData.isExact);
					}
					mSeekDataPool.recycle(seekData);
					break;
				}
				case MSG_SEEK_TO_END: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.seekToEnd(msg.arg1);
					}
					break;
				}
				case MSG_RESET: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.reset();
					}
					break;
				}
				case MSG_RELEASE: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.release();
					}
					if (mPlayThread != null) {
						mPlayThread.quitSafely();
						mPlayThread = null;
					}
					break;
				}
				case MSG_SET_VOLUME: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.setVolume((float)msg.obj);
					}
					break;
				}
				case MSG_SET_LOOPING: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.setLooping((boolean)msg.obj);
					}
					break;
				}
				case MSG_CHANGE_VIDEO_MUTE: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.changeVideoMute(msg.arg1, (boolean)msg.obj);
					}
					break;
				}
				case MSG_START_TRANSITION: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.startTransition(msg.arg1, msg.arg2);
					}
					break;
				}
				case MSG_UPDATE_TRANSITION: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					@SuppressWarnings("unchecked")
					Pair<ITransitionInfo, ITransitionInfo> pair = (Pair<ITransitionInfo, ITransitionInfo>)msg.obj;
					if (multiPlayer != null) {
						multiPlayer.updateTransition(pair.first, pair.second);
					}
					break;
				}
				case MSG_RESTART_TRANSITION: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.restartTransition();
					}
					break;
				}
				case MSG_UPDATE_TRANSITION_TIME: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.updateTransitionTime(msg.arg1);
					}
					break;
				}
				case MSG_EXIT_TRANSITION: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.exitTransition();
					}
					break;
				}
				case MSG_SET_START_TRANSITION: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.setStartTransition((boolean)msg.obj);
					}
					break;
				}
				case MSG_SET_RANGE_PLAY: {
					final MultiPlayerImpl multiPlayer = mMultiPlayerImpl;
					if (multiPlayer != null) {
						multiPlayer.setRangePlay(msg.arg1, msg.arg2);
					}
					break;
				}
			}
		}
	}
}
