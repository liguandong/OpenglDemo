package poco.cn.opengldemo.audio;

import android.media.MediaRecorder;

import java.io.IOException;

/**
 * Created by lgd on 2019/5/31.
 */
public class VoiceMediaRecord implements IRecord
{
    private MediaRecorder mr = null;
    @State
    private int mState = State.IDLE;

    public VoiceMediaRecord()
    {
        super();
        mr = new MediaRecorder();
//        mr.setOutputFile(path);
    }

    @Override
    public void setOutPath(String path)
    {
        if (mState == State.RELEASE)
        {
            return;
        }
        mState = State.PREPARED;
        mr.reset();
        mr.setAudioChannels(2);
        mr.setAudioSamplingRate(SAMPLE_RATE_INHZ);
        mr.setAudioEncodingBitRate(AUDIO_FORMAT);
        mr.setAudioSource(MediaRecorder.AudioSource.MIC);  //音频输入源
        mr.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);   //设置输出格式
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);   //设置编码格式
        mr.setOutputFile(path);
        try
        {
            mr.prepare();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void start()
    {
        if (mState == State.PREPARED)
        {
            mState = State.START;
            mr.start();  //开始录制
        }
    }

    @Override
    public void stop()
    {
        if (mState == State.START)
        {
            mState = State.STOP;
            mr.stop();
        }
    }


    @Override
    public void release()
    {
        stop();
        mr.release();
        mState = State.RELEASE;
    }
}
