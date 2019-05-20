package poco.cn.opengldemo.base;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.Nullable;
import poco.cn.opengldemo.utils.ShareData;

/**
 * Created by lgd on 2018/1/11.
 */

public class FrameNinePalacesView extends View
{
    private static final String TAG = "FrameNinePalacesView";
    private Paint mRingPaint; //边框
    private Paint mLinePaint; //线段
    private int strokeW = ShareData.PxToDpi_xhdpi(3);
    private int strokeW1 = ShareData.PxToDpi_xhdpi(2);
    private ScaleGestureDetector mScaleGestureDetector;
    protected GestureDetector mGestureDetector;

    //    private OnScaleChangedListener mScaleChangeListener;
    private OnViewListener mOnViewDragListener;

    public FrameNinePalacesView(Context context)
    {
        this(context, null);
    }

    public FrameNinePalacesView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    private void init()
    {
        mRingPaint = new Paint();
        mRingPaint.setAntiAlias(true);
        mRingPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setColor(Color.WHITE);
        mRingPaint.setStrokeWidth(strokeW);

        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setColor(Color.WHITE);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(strokeW1);

        mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener()
        {
            @Override
            public boolean onScale(ScaleGestureDetector detector)
            {
                float scaleFactor = detector.getScaleFactor();
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
//                FrameNinePalacesView.this.onScale(scaleFactor, focusX, focusY);
//                if (mScaleChangeListener != null)
//                {
//                    mScaleChangeListener.onScaleChange(scaleFactor, focusX, focusY);
//                }
                if (mOnViewDragListener != null)
                {
//                    mOnViewDragListener.onScaleChange(scaleFactor, (focusX - getWidth() / 2) / (float) getRealWidth(), (getHeight() / 2 - focusY ) / (float) getRealHeight());
                    mOnViewDragListener.onScaleChange(scaleFactor, focusX, focusY);
                }
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector)
            {
                ViewParent parent = getParent();
                if (parent != null)
                {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector)
            {
                if (mOnViewDragListener != null)
                {
                    mOnViewDragListener.onScaleEnd();
                }
            }
        });

        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener()
        {
            @Override
            public void onLongPress(MotionEvent e)
            {
                super.onLongPress(e);
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
            {
                if (mScaleGestureDetector.isInProgress())
                {
                    return false;
                }
                if (mOnViewDragListener != null)
                {
                    mOnViewDragListener.onDrag(-distanceX, -distanceY);
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e)
            {
                if (mOnViewDragListener != null)
                {
                    mOnViewDragListener.onClick();
                }
                return true;
            }

            //            @Override
//            public boolean onSingleTapUp(MotionEvent e)
//            {
//                if (mOnViewDragListener != null)
//                {
//                    mOnViewDragListener.onClick();
//                }
//                return true;
//            }

            @Override
            public boolean onDoubleTap(MotionEvent e)
            {
                float x = e.getX();
                float y = e.getY();
//                if(mOnViewDragListener != null){
//                    mOnViewDragListener.onDoubleClick(x,y);
//                }
                return true;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        canvas.drawRect(strokeW / 2, strokeW / 2, getRealWidth() - strokeW / 2, getRealHeight() - strokeW / 2, mRingPaint);
        canvas.drawLine(0, getRealHeight() / 3, getRealWidth(), getRealHeight() / 3, mLinePaint);
        canvas.drawLine(0, getRealHeight() * 2 / 3, getRealWidth(), getRealHeight() * 2 / 3, mLinePaint);
        canvas.drawLine(getRealWidth() / 3, 0, getRealWidth() / 3, getRealHeight(), mLinePaint);
        canvas.drawLine(getRealWidth() * 2 / 3, 0, getRealWidth() * 2 / 3, getRealHeight(), mLinePaint);
        canvas.restore();
    }

    public int getRealWidth()
    {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    public int getRealHeight()
    {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean handled = false;
        switch (event.getAction())
        {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mOnViewDragListener != null)
                {
                    mOnViewDragListener.onUp();
                }
                handled = true;
                break;
        }

        handled |= mScaleGestureDetector.onTouchEvent(event) | mGestureDetector.onTouchEvent(event);
        if (handled)
        {
            return true;
        } else
        {
            return super.onTouchEvent(event);
        }
    }

//    public void setScaleChangeListener(OnScaleChangedListener scaleChangeListener)
//    {
//        this.mScaleChangeListener = scaleChangeListener;
//    }

    public void setOnViewDragListener(OnViewListener onViewDragListener)
    {
        this.mOnViewDragListener = onViewDragListener;
    }

    public interface OnViewListener
    {

        void onDrag(float dx, float dy);

        void onScaleChange(float scale, float focusX, float focusY);

        void onScaleEnd();

        void onClick();

        void onUp();

//        void onDown();
    }

//    public interface OnScaleChangedListener
//    {
//        void onScaleChange(float scale, float focusX, float focusY);
//    }
}
