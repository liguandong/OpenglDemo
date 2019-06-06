package poco.cn.opengldemo.audio;

import android.os.Handler;
import android.os.SystemClock;


/**
 * Created by lgd on 2019/2/22.
 */
public class VoiceOverRecordMgr
{
    private int mMinVideoDuration = 300;

    private long mStartRecordTime = -1;
    private boolean canStop = false;
    private IRecord iRecord = null;
    private long maxDuration;
    private Handler handler;
    private String outPath;
    public boolean canStop()
    {
        return canStop;
    }

    public VoiceOverRecordMgr()
    {
        super();
        iRecord = new VoiceMediaRecord();
        handler = new Handler();
    }

    //开始录制
    public void startRecord(long duration, String path)
    {
        handler.removeCallbacks(mTimeRunnable);
        mStartRecordTime = SystemClock.elapsedRealtime();
        maxDuration = duration;
        outPath = path;
        iRecord.setOutPath(path);
        iRecord.start();
        handler.post(mTimeRunnable);
    }

    //停止录制，资源释放
    public void stopRecord()
    {
        if (iRecord != null)
        {
            iRecord.stop();
//            iRecord.release();
        }
        handler.removeCallbacks(mTimeRunnable);
    }

    public void clear()
    {
        iRecord.stop();
    }

    private Runnable mTimeRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            long recordTime = SystemClock.elapsedRealtime() - mStartRecordTime;
            if (recordTime > 0)
            {
                float progress = recordTime / (float) maxDuration;
                if (progress >= 1f)
                {
                    stopRecord();
                    if (mListener != null)
                    {
                        mListener.onRecordFinish(outPath);
                    }
                    return;
                } else
                {
                    if (mListener != null)
                    {
                        if (!canStop && recordTime >= mMinVideoDuration)
                        {
                            canStop = true;
                            mListener.onStopEnable(true);
                        } else if (canStop && recordTime < mMinVideoDuration)
                        {
                            mListener.onStopEnable(false);
                        }
                        mListener.onProgressChanged(progress, recordTime);
                    }
                }
            }
            handler.post(this);
        }
    };

    private OnRecordListener mListener;

    public void setListener(OnRecordListener mListener)
    {
        this.mListener = mListener;
    }

    public void setVolume(float a)
    {
//        volume = a;

    }

    public interface OnRecordListener
    {
        void onRecordFinish(String path);

        void onStopEnable(boolean canStop);

        void onProgressChanged(float progress, long recordTime);
    }
}
