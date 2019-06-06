package poco.cn.medialibs.media;


import android.util.Log;

import java.util.ArrayList;

import poco.cn.medialibs.media.avmediaplayer.AVMediaPlayer;
import poco.cn.medialibs.media.avmediaplayer.AVMediaPlayerSynchronizer;

public class AVMultiAudioSynchronizer implements AVMediaPlayerSynchronizer
{

    public static class MultiAudioInput
    {
        private String mFile;
        private int mStart;
        private AVMediaPlayer mMediaPlayer;
        private AVMediaPlayer mHostPlayer;
        private Thread mSyncThread;
        private boolean mPausing = false;
        private boolean mPrepared = false;
        public MultiAudioInput(AVMediaPlayer hostPlayer, String file, int start)
        {
            mHostPlayer = hostPlayer;
            mFile = file;
            mStart = start;
        }

        public boolean prepare()
        {
            try {
                if(mMediaPlayer != null)
                {
                    mMediaPlayer.stop();
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }

                mMediaPlayer = new AVMediaPlayer(true, false);
                mMediaPlayer.setVideoSource(new String[]{mFile});
                mMediaPlayer.prepare();
                mPrepared = true;
                return true;
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            return false;
        }

        public void start()
        {
            if(mPrepared == false) {
                return;
            }
            if(mSyncThread == null)
            {
                mSyncThread = new Thread(mSyncRunnable);
                mSyncThread.start();
            }
            mPausing = false;
        }

        public void pause()
        {
            if(mPrepared == false) {
                return;
            }
            if(mMediaPlayer != null && mMediaPlayer.isPlaying())
            {
                mMediaPlayer.pause();
                mPausing = true;
            }
        }

        public void stop()
        {
            if(mPrepared == false) {
                return;
            }
            if(mMediaPlayer != null)
            {
                mMediaPlayer.stop();
            }
            if(mSyncThread != null)
            {
                mSyncThread.interrupt();
                try {
                    mSyncThread.join();
                }
                catch(Exception e)
                {}
                mSyncThread = null;
            }
        }

        public void seek(long time)
        {
            if(mPrepared == false) {
                return;
            }
            time = time + mStart;
            if(mMediaPlayer != null)
            {
                if(time > mMediaPlayer.getDuration())
                {
                    time = mMediaPlayer.getDuration();
                }
                Log.d("hwq", "seek:"+time);
                if(time >= 0) {
                    mMediaPlayer.seek(time, true);
                }
                else
                {
                    mMediaPlayer.seek(0, true);
                }
            }
        }

        public void release()
        {
            if(mSyncThread != null)
            {
                mSyncThread.interrupt();
                try {
                    mSyncThread.join();
                }
                catch(Exception e)
                {}
                mSyncThread = null;
            }
            if(mMediaPlayer != null) {
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }

        private Runnable mSyncRunnable = new Runnable() {
            @Override
            public void run() {
                while(mSyncThread != null)
                {
                    if(mHostPlayer != null)
                    {
                        long pts = mHostPlayer.getCurrentPosition();
                        pts += mStart;
                        if(pts >= 0)
                        {
                            if(mMediaPlayer.getDuration() > pts) {
                                if(mHostPlayer.isPlaying()
                                        && mMediaPlayer.isPlaying() == false) {
                                    mMediaPlayer.start();
                                }
                            }
                            else
                            {
                                if(mMediaPlayer.isPlaying()) {
                                    mMediaPlayer.stop();
                                }
                            }
                            if(mHostPlayer.isPlaying() == false && mMediaPlayer.isPlaying() == true) {
                                if(mPausing) {
                                    mMediaPlayer.pause();
                                } else {
                                    mMediaPlayer.stop();
                                }
                            }
                        }
                        else
                        {
                            if(mMediaPlayer.isPlaying()) {
                                mMediaPlayer.pause();
                            }
                        }
                    }
                    try {
                        Thread.sleep(1);
                    }
                    catch(Exception e)
                    {
                        break;
                    }
                }
            }
        };
    }

    private ArrayList<MultiAudioInput> mInputs = new ArrayList<MultiAudioInput>();
    public void addInput(MultiAudioInput input)
    {
        mInputs.add(input);
    }

    @Override
    public boolean prepare() {
        //Log.d("hwq", "sync prepare");
        synchronized(mInputs) {
            for(int i = 0; i < mInputs.size(); i++) {
                if(mInputs.get(i).prepare() == false)
                {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public void seek(long time) {
        //Log.d("hwq", "sync seek");
        synchronized(mInputs) {
            for(int i = 0; i < mInputs.size(); i++) {
                mInputs.get(i).seek(time);
            }
        }
    }

    @Override
    public void start() {
        //Log.d("hwq", "sync start");
        synchronized(mInputs) {
            for(int i = 0; i < mInputs.size(); i++) {
                mInputs.get(i).start();
            }
        }
    }

    @Override
    public void stop() {
        //Log.d("hwq", "sync stop");
        synchronized(mInputs) {
            for(int i = 0; i < mInputs.size(); i++) {
                mInputs.get(i).stop();
            }
        }
    }

    @Override
    public void pause() {
        //Log.d("hwq", "sync pause");
        synchronized(mInputs) {
            for(int i = 0; i < mInputs.size(); i++) {
                mInputs.get(i).pause();
            }
        }
    }

    public void release()
    {
        synchronized(mInputs) {
            for(int i = 0; i < mInputs.size(); i++) {
                mInputs.get(i).release();
            }
            mInputs.clear();
        }
    }
}
