package poco.cn.medialibs.player2;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.Surface;

import poco.cn.medialibs.gles.GlUtil;
import poco.cn.medialibs.player2.base.VideoPlayer;


/**
 * Created by: fwc
 * Date: 2018/12/6
 * Surface 的封装
 */
public class GLSurface implements IPlayer.ISurface, VideoPlayer.PlaySurface {

	private int mTextureId;

	@NonNull
	private final SurfaceTexture mSurfaceTexture;

	@NonNull
	private final Surface mSurface;

	private int mId = -1;

	private volatile long mTimestamp;

	private boolean isNeedRotate;
	private int mRotation;
	private float[] mRotateMatrix;

	@NonNull
	private final float[] mTexMatrix = new float[16];

	@Nullable
	private volatile SurfaceTexture.OnFrameAvailableListener mListener;

	private volatile boolean isStart = false;

	private volatile boolean isRelease;

	private boolean isLock = false;

	private volatile long mVideoDuration;

	public GLSurface() {
		mTextureId = GlUtil.createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);

		mSurfaceTexture = new SurfaceTexture(mTextureId);

		mSurface = new Surface(mSurfaceTexture);

		Matrix.setIdentityM(mTexMatrix, 0);
	}

	public int getTextureId() {
		return mTextureId;
	}

	public void updateTexImage() {
		mSurfaceTexture.updateTexImage();
	}

	public void setOnFrameAvailableListener(SurfaceTexture.OnFrameAvailableListener listener) {
		mListener = listener;
		mSurfaceTexture.setOnFrameAvailableListener(listener);
	}

	@Override
	public void onFrameAvailable(long timestamp) {
		// Play Thread
		if (timestamp > mTimestamp && isStart && !isLock) {
			mTimestamp = timestamp;
			SurfaceTexture.OnFrameAvailableListener listener = mListener;
			if (listener != null) {
				listener.onFrameAvailable(mSurfaceTexture);
			}
		}
	}

	@NonNull
	@Override
	public Surface getSurface() {
		return mSurface;
	}

	@Override
	public void setId(int id) {
		mId = id;
		isStart = id >= 0;
	}

	private static float[] sTempMatrix = new float[16];

	public float[] wrapFrameMatrix(float[] mtx) {
		// 主要针对 Android 4.4 以下，视频解码器没有根据视频的旋转角度旋转视频画面
		// 因此需要在这里进行判断，确定是否要手动旋转视频画面
		if (isNeedRotate && mRotation != 0) {
			if (mRotateMatrix == null) {
				mRotateMatrix = new float[16];
				Matrix.setIdentityM(mRotateMatrix, 0);
				Matrix.rotateM(mRotateMatrix, 0, mRotation, 0, 0, 1);
			}

			Matrix.multiplyMM(sTempMatrix, 0, mtx, 0, mRotateMatrix, 0);
			return sTempMatrix;
		}

		return mtx;
	}

	public void resetWrapMatrix() {
		mRotateMatrix = null;
	}

	public float[] getTransformMatrix() {
		mSurfaceTexture.getTransformMatrix(mTexMatrix);
		return mTexMatrix;
	}

	public int getId() {
		return mId;
	}

	@Override
	public void setTimestamp(long timestamp) {
		if (!isLock) {
			mTimestamp = timestamp;
		}
	}

	public long getTimestamp() {
		return mTimestamp;
//		return mSurfaceTexture.getTimestamp() / 1000000;
	}

	@Override
	public void setRotation(boolean isNeedRotate, int rotation) {
		this.isNeedRotate = isNeedRotate;
		mRotation = rotation;
		mRotateMatrix = null;
	}

	@Override
	public void lock() {
		isLock = true;
	}

	@Override
	public void unlock(int timestamp) {
		mTimestamp = timestamp;
		isLock = false;
	}

	@Override
	public void setVideoDuration(long duration) {
		mVideoDuration = duration;
	}

	public long getVideoDuration() {
		return mVideoDuration;
	}

	@Override
	public boolean isValid() {
		return mSurface.isValid() && !isRelease;
	}

	public void reset() {
		isStart = false;
		mId = -1;
		mTimestamp = 0;
		mVideoDuration = 0;
		isNeedRotate = false;
		mRotation = 0;
		mRotateMatrix = null;
		isLock = false;
	}

	public void release() {
		isRelease = true;
		reset();
		if (mSurface.isValid()) {
			mSurface.release();
		}

		mSurfaceTexture.setOnFrameAvailableListener(null);
		mSurfaceTexture.release();
		if (mTextureId == 0) {
			GLES20.glDeleteTextures(1, new int[] {mTextureId}, 0);
			mTextureId = 0;
		}
	}
}
