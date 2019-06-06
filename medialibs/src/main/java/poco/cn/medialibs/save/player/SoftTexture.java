package poco.cn.medialibs.save.player;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;

import androidx.annotation.Nullable;

/**
 * Created by: fwc
 * Date: 2017/12/27
 */
public class SoftTexture {

	private final int mTextureId;

	private ByteBuffer mByteBuffer;
	private int mWidth;
	private int mHeight;
	private int mRotation;

	private long mTimestamp;

	private OnFrameAvailableListener mOnFrameAvailableListener;

	private boolean hasSetBuffer;

	private byte[] mFrameData;

	public SoftTexture() {
		mTextureId = 0;
	}

	public SoftTexture(int textureId) {

		if (textureId <= 0) {
			throw new IllegalArgumentException("the texture id is error");
		}

		mTextureId = textureId;
	}

	void setByteBuffer(byte[] data, int width, int height, int rotation, long timestamp) {

		if (data == null || width <= 0 || height <= 0) {
			throw new IllegalArgumentException();
		}

		if (mTextureId == 0) {
			if (mFrameData == null || mFrameData.length != data.length) {
				mFrameData = new byte[data.length];
			}
			System.arraycopy(data, 0, mFrameData, 0, data.length);
		} else {
			if (mByteBuffer == null) {
				mByteBuffer = ByteBuffer.allocateDirect(data.length);
			} else if (mByteBuffer.capacity() < data.length) {
				mByteBuffer.clear();
				mByteBuffer = ByteBuffer.allocateDirect(data.length);
			}

			mByteBuffer.clear();
			mByteBuffer.put(data);
			mByteBuffer.flip();
		}

		mWidth = width;
		mHeight = height;
		mRotation = rotation;

		mTimestamp = timestamp;

		hasSetBuffer = true;
	}

	void setTimestamp(long timestamp) {
		mTimestamp = timestamp;
	}

	void notifyFrameAvailable() {
		if (mOnFrameAvailableListener != null) {
			mOnFrameAvailableListener.onFrameAvailable(this);
		}
	}

	public void updateTexImage() {
		if (mByteBuffer == null || !hasSetBuffer || mTextureId == 0) {
			return;
		}
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mByteBuffer);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		hasSetBuffer = false;
	}

	@Nullable
	public byte[] getFrameData() {
		return mFrameData;
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public void getTransformMatrix(float[] mtx) {
		if (mtx.length != 16) {
			throw new IllegalArgumentException();
		}

		Matrix.setIdentityM(mtx, 0);

		if (mTextureId != 0) {
			// 翻转纹理
			mtx[5] = -1;
			mtx[13] = 1;

			MatrixUtils.rotateM(mtx, 0, mRotation);
		}
	}

	public long getTimestamp() {
		return mTimestamp;
	}

	public void setOnFrameAvailableListener(OnFrameAvailableListener listener) {
		mOnFrameAvailableListener = listener;
	}

	public void release() {
		if (mByteBuffer != null) {
			mByteBuffer.clear();
		}
	}

	public interface OnFrameAvailableListener {
		void onFrameAvailable(SoftTexture softTexture);
	}
}
