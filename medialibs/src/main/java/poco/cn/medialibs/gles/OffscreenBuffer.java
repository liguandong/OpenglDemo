package poco.cn.medialibs.gles;

import android.opengl.GLES20;

/**
 * Created by: fwc
 * Date: 2017/5/5
 */
public class OffscreenBuffer {

	private int mTextureId = GlUtil.NO_TEXTURE;

	private int mFrameBufferId = 0;

//	private int mRenderBufferId = 0;

	private int mWidth;

	private int mHeight;

	OffscreenBuffer next;

	BufferPool pool;

	public OffscreenBuffer(int width, int height) {

		mWidth = width;
		mHeight = height;

		mTextureId = GlUtil.createTexture(GLES20.GL_TEXTURE_2D);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

		int[] genbuf = new int[1];
		GLES20.glGenFramebuffers(1, genbuf, 0);
		mFrameBufferId = genbuf[0];
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);

//		GLES20.glGenRenderbuffers(1, genbuf, 0);
//		mRenderBufferId = genbuf[0];
//		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mRenderBufferId);
//		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);

		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
									  GLES20.GL_TEXTURE_2D, mTextureId, 0);
//		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
//										 GLES20.GL_RENDERBUFFER, mRenderBufferId);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);

		unbind();
	}

	public void bind() {

//		GLES20.glViewport(0, 0, mWidth, mHeight);

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
	}

	public void unbind() {
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
	}

	public void unbind(int bufferId) {
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bufferId);
	}

	public void recycle() {
		if (pool != null) {
			pool.recycle(this);
		}
	}

	public void release() {
		GLES20.glDeleteTextures(1, new int[] {mTextureId}, 0);
//		GLES20.glDeleteRenderbuffers(1, new int[] {mRenderBufferId}, 0);
		GLES20.glDeleteFramebuffers(1, new int[] {mFrameBufferId}, 0);
	}

	public int getTextureId() {
		return mTextureId;
	}

	public int getFrameBufferId() {
		return mFrameBufferId;
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}
}
