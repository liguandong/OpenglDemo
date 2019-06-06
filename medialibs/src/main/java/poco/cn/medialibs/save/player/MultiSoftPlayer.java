package poco.cn.medialibs.save.player;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import poco.cn.medialibs.utils.FileUtil;

/**
 * Created by: fwc
 * Date: 2017/12/27
 */
public class MultiSoftPlayer {

	private static final int IDLE = 0;
	private static final int PREPARED = 1;
	private static final int START = 2;
	private static final int RELEASE = 4;

	private Context mContext;

	@Nullable
	private SoftPlayer mCurPlayer;

	@Nullable
	private SoftPlayer mNextPlayer;

	private SoftTexture mCurTexture;

	@Nullable
	private SoftTexture mNextTexture;

	private SaveVideoInfo[] mVideoInfos;
	private int mCurIndex = 0;

	private int mState = IDLE;

	private boolean isFinish = false;

	private boolean mCurStart;

	private OnPlayListener mOnPlayListener;

	private boolean mShouldExit;

	private int mTransitionTime;

	@Nullable
	private PlayerFactory mFactory;

	public MultiSoftPlayer(Context context) {
		this(context, null);
	}

	public MultiSoftPlayer(Context context, @Nullable PlayerFactory factory) {
		mContext = context;
		initPlayer(factory);
	}

	private void initPlayer(@Nullable PlayerFactory factory) {

		if (factory == null) {
			mCurPlayer = new SoftPlayer();
			mNextPlayer = new SoftPlayer();
		} else {
			mFactory = factory;
			mCurPlayer = factory.createPlayer(mContext);
			mNextPlayer = factory.createPlayer(mContext);
		}
	}

	public void setVideoPaths(SaveVideoInfo... videoInfos) {

		if (mState != IDLE) {
			throw new IllegalStateException();
		}

		if (videoInfos == null || videoInfos.length == 0) {
			throw new IllegalArgumentException("the videoPaths are null or empty");
		}

		List<SaveVideoInfo> infoList = new ArrayList<>(videoInfos.length);
		for (SaveVideoInfo info : videoInfos) {
			if (FileUtil.isFileExist(info.path)) {
				infoList.add(info);
			}
		}

		final int size = infoList.size();
		if (size == 0) {
			throw new IllegalArgumentException("the videoPaths are null or empty");
		}
		mVideoInfos = new SaveVideoInfo[size];
		for (int i = 0; i < size; i++) {
			mVideoInfos[i] = infoList.get(i);
		}
	}

	public void setSurface(SoftTexture curTexture, @Nullable SoftTexture nextTexture) {

		if (mState != IDLE) {
			throw new IllegalStateException();
		}

		if (curTexture == null) {
			throw new IllegalArgumentException("the surface is null");
		}

		mCurTexture = curTexture;
		mNextTexture = nextTexture;
	}

	public void setOnPlayListener(OnPlayListener listener) {
		mOnPlayListener = listener;
	}

	public void prepare() {

		if (mCurPlayer == null) {
			return;
		}

		if (mState != IDLE) {
			throw new IllegalStateException();
		}

		if (mVideoInfos == null) {
			throw new RuntimeException("must call setVideoInfos() before");
		}

		if (mCurTexture == null) {
			throw new RuntimeException("must call setSurface() before");
		}

		if (mCurIndex < mVideoInfos.length) {
			mCurPlayer.setSoftTexture(mCurTexture);
			final SaveVideoInfo info = mVideoInfos[mCurIndex];
			mCurPlayer.setDataSource(info.path);
			mCurPlayer.setRotation(info.rotation);
			mCurPlayer.setDuration(info.duration);

			mCurPlayer.prepare();

			// 提前准备下一个视频
			if (mNextTexture != null && mNextPlayer != null) {
				mNextPlayer.setSoftTexture(mNextTexture);
				prepareNextPlayer();
			}
		}

		mState = PREPARED;
	}

	private void prepareNextPlayer() {

		if (mNextPlayer != null && mCurIndex + 1 < mVideoInfos.length) {
			int nextIndex = mCurIndex + 1;
			final SaveVideoInfo info = mVideoInfos[nextIndex];
			mNextPlayer.setDataSource(info.path);
			mNextPlayer.setDuration(info.duration);
			mNextPlayer.setRotation(info.rotation);

			mNextPlayer.prepare();
		}
	}

	public void start() {

		if (mState != PREPARED) {
			throw new IllegalStateException();
		}

		if (mCurPlayer == null) {
			return;
		}

		mState = START;
		mCurPlayer.startDecoder();

		long timestamp;
		try {
			while (!mShouldExit) {

				timestamp = mCurPlayer.getFrame();
				if (timestamp == 0 && !mCurStart) {
					onVideoStart();
				}

				if (mNextPlayer != null && mNextPlayer.isPlaying()) {
					mNextPlayer.getFrame(timestamp / 1000, mCurPlayer.getDuration(), mTransitionTime);
				}
				mCurPlayer.notifyFrameAvailable();

				if (timestamp == -1) {
					onVideoFinish();
				}

				if (isFinish) {
					break;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (mShouldExit) {
			if (mCurPlayer != null) {
				mCurPlayer.stop();
			}
			if (mNextPlayer != null) {
				mNextPlayer.stop();
			}
		}
	}

	public void requestExit() {
		mShouldExit = true;
	}

	private void onVideoStart() {
		mCurStart = true;
		if (mOnPlayListener != null) {
			mOnPlayListener.onStart(mCurIndex);
		}
	}

	private void onVideoFinish() {

		if (mOnPlayListener != null) {
			mOnPlayListener.onFinish(mCurIndex);
		}

		mCurStart = false;
		mTransitionTime = 0;
		if (mCurIndex + 1 < mVideoInfos.length) {
			resetCurPlayer();

			SoftPlayer temp = mCurPlayer;
			mCurPlayer = mNextPlayer;
			mNextPlayer = temp;

			mCurIndex++;
			if (mCurPlayer != null && !mCurPlayer.isPlaying()) {
				mCurPlayer.startDecoder();
			}

			prepareNextPlayer();
		} else {
			isFinish = true;
		}
	}

	private void resetCurPlayer() {
		if (mCurPlayer != null) {
			mCurPlayer.release();
			if (mFactory != null) {
				mCurPlayer = mFactory.createPlayer(mContext);
			} else {
				mCurPlayer = new SoftPlayer();
			}
			mCurPlayer.setSoftTexture(mCurIndex % 2 == 0 ? mCurTexture : mNextTexture);
		}
	}

	public int getCurrentIndex() {
		return mCurIndex;
	}

	public void startNext(int transitionTime) {

		if (mState != START) {
			throw new IllegalStateException();
		}

		mTransitionTime = transitionTime;
		if (mNextPlayer != null) {
			mNextPlayer.startDecoder();
		}
	}

	public void release() {
		if (mCurPlayer != null) {
			mCurPlayer.release();
			mCurPlayer = null;
		}

		if (mNextPlayer != null) {
			mNextPlayer.release();
			mNextPlayer = null;
		}

		mState = RELEASE;
	}

	public interface OnPlayListener {
		void onStart(int index);
		void onFinish(int index);
	}
}
