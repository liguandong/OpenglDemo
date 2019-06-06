package poco.cn.opengldemo.audio;

import android.media.AudioManager;
import android.media.MediaPlayer;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Created by lgd on 2019/6/5.
 */
public class AudioPlayer
{
    private static final int IDLE = 0;
    private static final int PREPARED = 1;
    private static final int START = 2;
    private static final int PAUSE = 3;
    private static final int RELEASE = 4;

    @NonNull
    private final MediaPlayer mMediaPlayer;

    private int mState = IDLE;

    private List<PlayAudioInfo> mAudioInfos = new ArrayList<>();

    private PlayAudioInfo mCurAudioInfo;

    public AudioPlayer()
    {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(mOnCompletionListener);

    }


    public void setCurTime(int position)
    {
        if (mState == START || mState == PAUSE)
        {
            if(mCurAudioInfo != null){
                if (mCurAudioInfo.startTime <= position && position < mCurAudioInfo.endTime )
                {
                    if(mState == PAUSE){
                        try
                        {
                            mMediaPlayer.seekTo(mCurAudioInfo.fromTime + (position - mCurAudioInfo.startTime));
                        } catch (IllegalStateException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }else{
                    pausePlayer();
                    mCurAudioInfo = null;
                }
            }
            if(mCurAudioInfo == null)
            {
                List<PlayAudioInfo> audioInfos = mAudioInfos;
                if (audioInfos != null && !audioInfos.isEmpty())
                {
                    final int size = audioInfos.size();
                    int i = 0;
                    PlayAudioInfo info;
                    for (; i < size; i++)
                    {
                        info = audioInfos.get(i);
                        if (info.startTime <= position && position < info.endTime)
                        {
                            break;
                        }
                    }

                    if (i < size)
                    {
                        info = audioInfos.get(i);
                        try
                        {
                            mMediaPlayer.reset();
                            mMediaPlayer.setVolume(info.volume, info.volume);
                            mMediaPlayer.setDataSource(info.path);
                            mMediaPlayer.setLooping(false);
                            mMediaPlayer.prepare();
                            mMediaPlayer.seekTo(info.fromTime);
                            mMediaPlayer.start();
                            mCurAudioInfo = info;
                        } catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public void setAudioInfos(List<PlayAudioInfo> infos)
    {
        if (mState != IDLE)
        {
            return;
        }
        mAudioInfos.clear();
        if (infos == null || infos.isEmpty())
        {
            return;
        }

        mAudioInfos.addAll(infos);
    }

    public void updateVolume()
    {
        if(mState == START || mState == PAUSE){
            if(mCurAudioInfo != null)
            {
                mMediaPlayer.setVolume(mCurAudioInfo.volume,mCurAudioInfo.volume);
            }
        }
    }

    public void prepare()
    {
        if (mState != IDLE)
        {
            return;
        }
        mState = PREPARED;
    }

    public void start()
    {
        if (mState == PREPARED || mState == PAUSE)
        {
            mState = START;
            if(mCurAudioInfo != null){
                mMediaPlayer.start();
            }
        }
    }

    public void pause()
    {
        if (mState == START)
        {
            pausePlayer();
            mState = PAUSE;
        }
    }

    private void pausePlayer()
    {
        if (mMediaPlayer.isPlaying())
        {
            try
            {
                mMediaPlayer.pause();
            } catch (IllegalStateException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void reset()
    {
        try
        {
            mMediaPlayer.reset();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        mState = IDLE;
    }

    public void release()
    {
        try
        {
            if (mMediaPlayer.isPlaying())
            {
                mMediaPlayer.pause();
            }
            mMediaPlayer.release();
        } catch (IllegalStateException e)
        {
            e.printStackTrace();
        }
        mState = RELEASE;
    }

    @SuppressWarnings("all")
    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener()
    {
        @Override
        public void onCompletion(MediaPlayer mp)
        {
            mCurAudioInfo = null;
        }
    };
}
