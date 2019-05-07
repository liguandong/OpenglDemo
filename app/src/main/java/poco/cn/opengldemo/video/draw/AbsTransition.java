package poco.cn.opengldemo.video.draw;

import android.content.Context;
import android.opengl.GLES20;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.core.math.MathUtils;
import android.view.animation.Interpolator;

import poco.cn.medialibs.gles.BufferPool;
import poco.cn.medialibs.gles.Drawable2d;
import poco.cn.medialibs.gles.GlUtil;
import poco.cn.medialibs.gles.OffscreenBuffer;


/**
 * Created by: fwc
 * Date: 2019/1/4
 * 转场 Filter 抽象类
 */
public abstract class AbsTransition
{

	@NonNull
	protected final Context mContext;

	private int mProgram = 0;

	private int aPositionLoc;
	private int aTextureCoordLoc;

	private int uFirstImageLoc;
	private int uSecondImageLoc;

	protected float mProgress;
	private int uProgressLoc;

	@Nullable
	private Interpolator mInterpolator;

	@NonNull
	private final Drawable2d mDrawable2d = new Drawable2d();

	/**
	 * 兼容旧版本转场
	 */
	private int uLeftLoc;
	private int uTopLoc;

	protected boolean isUseViewport = true;
	/**
	 * 可视区域
	 */
	private int mViewX;
	private int mViewY;
	protected int mViewWidth;
	protected int mViewHeight;

	/**
	 * 画布区域
	 */
	private int mWidth;
	private int mHeight;

//	@Nullable
//	protected NoneFilter mNoneFilter;

	@NonNull
	private static ThreadLocal<BufferPool> sBufferPool = new ThreadLocal<>();

	protected AbsTransition(@NonNull Context context) {
		mContext = context;
	}

	protected void createProgram(@RawRes int vertexShader, @RawRes int fragmentShader) {
		if (vertexShader != 0 && fragmentShader != 0) {
			mProgram = GlUtil.createProgram(mContext, vertexShader, fragmentShader);
			onGetUniformLocation(mProgram);
		}
	}

	protected void setProgram(int program) {
		if (program == 0) {
			throw new RuntimeException("the program is 0.");
		}
		mProgram = program;

		onGetUniformLocation(program);
	}

	@CallSuper
	protected void onGetUniformLocation(int program) {
		aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition");
		aTextureCoordLoc = GLES20.glGetAttribLocation(program, "aTextureCoord");
		uFirstImageLoc = GLES20.glGetUniformLocation(program, "firstImage");
		uSecondImageLoc = GLES20.glGetUniformLocation(program, "secondImage");
		uProgressLoc = GLES20.glGetUniformLocation(program, "progress");
		uLeftLoc = GLES20.glGetUniformLocation(program, "left");
		uTopLoc = GLES20.glGetUniformLocation(program, "top");
	}

	public void draw(int textureId1, int textureId2) {
		if (isUseViewport) {
			onGLViewport();
		}
		onUseProgram();
		onSetUniformData();
		onBindTexture(textureId1, textureId2);
		onDraw();
	}

	protected void onGLViewport() {
		GLES20.glViewport(mViewX, mViewY, mViewWidth, mViewHeight);
	}

	protected void onUseProgram() {
		GLES20.glUseProgram(mProgram);
	}

	@CallSuper
	protected void onSetUniformData() {
		if (uProgressLoc >= 0) {
			GLES20.glUniform1f(uProgressLoc, mProgress);
		}
		if (uLeftLoc >= 0) {
			GLES20.glUniform1f(uLeftLoc, 0f);
		}
		if (uTopLoc >= 0) {
			GLES20.glUniform1f(uTopLoc, 0f);
		}
	}

	protected void onBindTexture(int textureId1, int textureId2) {
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId1);
		GLES20.glUniform1i(uFirstImageLoc, 0);

		if (uSecondImageLoc >= 0 && textureId2 >= 0) {
			GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId2);
			GLES20.glUniform1i(uSecondImageLoc, 1);
		}
	}

	protected void onDraw() {
		GLES20.glEnableVertexAttribArray(aPositionLoc);
		GLES20.glVertexAttribPointer(aPositionLoc, mDrawable2d.getCoordsPerVertex(),
									 GLES20.GL_FLOAT, false,
									 mDrawable2d.getVertexStride(), mDrawable2d.getVertexArray());

		GLES20.glEnableVertexAttribArray(aTextureCoordLoc);
		GLES20.glVertexAttribPointer(aTextureCoordLoc, 2, GLES20.GL_FLOAT,
									 false, mDrawable2d.getTexCoordStride(),
									 mDrawable2d.getTexCoordArray());

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mDrawable2d.getVertexCount());

		GLES20.glDisableVertexAttribArray(aPositionLoc);
		GLES20.glDisableVertexAttribArray(aTextureCoordLoc);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		GLES20.glUseProgram(0);
	}

	public void setProgress(float progress) {
		progress = MathUtils.clamp(progress, 0f, 1f);
		if (mInterpolator != null) {
			progress = mInterpolator.getInterpolation(progress);
		}
		onProgressChanged(MathUtils.clamp(progress, 0f, 1f));
	}

	protected void onProgressChanged(float progress) {
		mProgress = progress;
	}

	/**
	 * 是否需要绘制下一个视频的画面，针对 Blend Transition
	 */
	public boolean shouldRenderNext() {
		return false;
	}

	/**
	 * 设置插值器，用于改变进度的变化曲线
	 */
	public void setInterpolator(@Nullable Interpolator interpolator) {
		mInterpolator = interpolator;
	}

	/**
	 * 设置当前的转场 id
	 */
	public void setTransitionId(int transitionId) {

	}

//	/**
//	 * 设置需要的模糊 Filter
//	 */
//	public void setBlur(@NonNull IBlur blur) {
//
//	}

	/**
	 * 转场需要的模糊类型
	 */
//	public int getBlurType() {
//		return IBlur.NONE;
//	}

	/**
	 * 可视区域，用于 {@link GLES20#glViewport(int, int, int, int)}
	 */
	public void setViewport(int x, int y, int width, int height) {
		mViewX = x;
		mViewY = y;
		mViewWidth = width;
		mViewHeight = height;
	}

	/**
	 * 设置画布的尺寸
	 */
	public void setSize(int width, int height) {
		mWidth = width;
		mHeight = height;
	}

	/**
	 * 判断是否绘制完转场效果，主要是用于片尾
	 */
	public boolean isFinish() {
		return mProgress >= 1.0f;
	}

	public void addProgress(float progress) {
		if (progress > 0) {
			progress = MathUtils.clamp(progress + mProgress, 0, 1);
			setProgress(progress);
		}
	}

	@NonNull
	protected OffscreenBuffer getBuffer() {
		final BufferPool bufferPool = sBufferPool.get();
		if (bufferPool == null) {
			throw new NullPointerException("please init buffer.");
		}
		return bufferPool.obtain();
	}

	public static void initBuffer(int width, int height) {
		BufferPool bufferPool = sBufferPool.get();
		if (bufferPool != null && bufferPool.getWidth() == width && bufferPool.getHeight() == height) {
			return;
		}

		if (bufferPool != null) {
			bufferPool.release();
		}

		bufferPool = new BufferPool(width, height, 2);
		sBufferPool.set(bufferPool);
	}

//	public void setNoneFilter(@Nullable NoneFilter filter) {
//		mNoneFilter = filter;
//	}

	public static void releaseBuffer() {
		final BufferPool bufferPool = sBufferPool.get();
		if (bufferPool != null) {
			bufferPool.release();
		}
		sBufferPool.set(null);
	}

	@CallSuper
	public void release() {
//		mNoneFilter = null;
		if (mProgram != 0) {
			GLES20.glDeleteProgram(mProgram);
			mProgram = 0;
		}
	}
}
