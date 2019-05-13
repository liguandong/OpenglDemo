package poco.cn.opengldemo.base;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

/**
 * Created by: fwc
 * Date: 2017/5/24
 */
public class BlackMaskView extends View {

	private int mWidth;
	private int mHeight;

	private Paint mPaint;

	private int mTop;
	private int mBottom;

	private int mLeft;
	private int mRight;

	private ValueAnimator mValueAnimator;

	private int[] mBlackBounds = new int[4];

	public BlackMaskView(Context context) {
		this(context, null);
	}

	public BlackMaskView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);

		init();
	}

	private void init() {
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setColor(Color.BLACK);
	}

	public void setColor(int color) {
		mPaint.setColor(color);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mWidth = w;
		mHeight = h;
	}

	@Override
	protected void onDraw(Canvas canvas) {

		// 画上面
		canvas.drawRect(0, 0, mWidth, mTop, mPaint);

		// 画下面
		canvas.drawRect(0, mHeight - mBottom, mWidth, mHeight, mPaint);

		// 画左面
		canvas.drawRect(0, 0, mLeft, mHeight, mPaint);

		// 画右面
		canvas.drawRect(mWidth - mRight, 0, mWidth, mHeight, mPaint);
	}

	public void startAnim(final int left, final int top, final int right, final int bottom) {

		mBlackBounds[0] = left;
		mBlackBounds[1] = top;
		mBlackBounds[2] = right;
		mBlackBounds[3] = bottom;

		if (mValueAnimator != null && mValueAnimator.isRunning()) {
			mValueAnimator.removeAllUpdateListeners();
			mValueAnimator.cancel();
			mValueAnimator = null;
		}

		final RectF rectF = new RectF(mLeft, mTop, mRight, mBottom);

		mValueAnimator = ValueAnimator.ofFloat(0, 1);
		mValueAnimator.setInterpolator(new LinearInterpolator());
		mValueAnimator.setDuration(200);
		mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float value = (float)animation.getAnimatedValue();
				mLeft = (int)((left - rectF.left) * value + rectF.left);
				mTop = (int)((top - rectF.top) * value + rectF.top);
				mRight = (int)((right - rectF.right) * value + rectF.right);
				mBottom = (int)((bottom - rectF.bottom) * value + rectF.bottom);

				ViewCompat.postInvalidateOnAnimation(BlackMaskView.this);
			}
		});
		mValueAnimator.start();
	}

	public void setBounds(final int left, final int top, final int right, final int bottom) {

		mBlackBounds[0] = left;
		mBlackBounds[1] = top;
		mBlackBounds[2] = right;
		mBlackBounds[3] = bottom;

		mLeft = left;
		mTop = top;
		mRight = right;
		mBottom = bottom;
		ViewCompat.postInvalidateOnAnimation(BlackMaskView.this);
	}

	public int[] getBlackBounds() {
		return mBlackBounds;
	}

	@Keep
	public void setMaskAlpha(float alpha) {
		mPaint.setAlpha((int) (alpha * 255f));
		ViewCompat.postInvalidateOnAnimation(this);
	}
}
