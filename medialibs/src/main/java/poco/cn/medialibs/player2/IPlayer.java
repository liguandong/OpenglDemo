package poco.cn.medialibs.player2;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by: fwc
 * Date: 2018/12/6
 */
public interface IPlayer {

	void setDataSource(@NonNull String path);

	void setSurface(@NonNull ISurface surface);

	void setId(int videoId);

	void prepare();

	void start();

	void restart();

	void seekTo(int position);

	void seekTo(int position, boolean isExact);

	void seekToEnd();

	void setRangePlay(long startPos, long endPos);

	void recoverWholePlay();

	void pause();

	void stop();

	void reset();

	void release();

	void setOnPlayListener(@Nullable OnPlayListener listener);

	void setLooping(boolean isLooping);

	boolean isLooping();

	void setVolume(float volume);

	float getVolume();

	boolean isPrepare();

	boolean isPlaying();

	boolean isPause();

	boolean isSeeking();

	long getCurrentPosition();

	long getDuration();

	void lock();

	void unlock(int timestamp);

	interface OnPlayListener {
		void onStart();

		void onSeekCompleted(@NonNull IPlayer player);

		void onFinish();

		void onRangeStart();

		void onRangeFinish();

		void onPositionChanged(int position);
	}

	interface ISurface {
		void lock();
		void unlock(int timestamp);
	}

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({State.IDLE, State.PREPARING, State.PREPARED, State.START,
			State.PAUSE, State.FINISH, State.STOP, State.RELEASE})
	@interface State {
		int IDLE = 0;
		int PREPARING = 1;
		int PREPARED = 2;
		int START = 3;
		int PAUSE = 4;
		int FINISH = 5;
		int STOP = 6;
		int RELEASE = 7;
	}
}
