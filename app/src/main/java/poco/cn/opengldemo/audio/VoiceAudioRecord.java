package poco.cn.opengldemo.audio;

import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lgd on 2019/5/31.
 */
public class VoiceAudioRecord implements IRecord
{
    private ExecutorService poolExecutor = Executors.newSingleThreadExecutor();
    private AudioRecord audioRecord = null;  // 声明 AudioRecord 对象
    private int recordBufSize = 0; // 声明recoordBufffer的大小字段
    private boolean isRecording = false;
    private File mRecordingFile;
    @State
    private int mState = State.IDLE;
    /**
     * 音频最小buffer大小
     */
    private static final int READ_SIZE = 2048;

    public VoiceAudioRecord()
    {
        super();
        recordBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT);//计算最小缓冲区
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_INHZ, CHANNEL_CONFIG,
                AUDIO_FORMAT, recordBufSize);//创建AudioRecorder对象
    }

    @Override
    public void setOutPath(String path)
    {
        //创建一个流，存放从AudioRecord读取的数据
        if (mState == State.RELEASE)
        {
            return;
        }
        mState = State.PREPARED;
        mRecordingFile = new File(path);
        if (mRecordingFile.exists())
        {//音频文件保存过了删除
            mRecordingFile.delete();
        }
        try
        {
            mRecordingFile.createNewFile();//创建新文件
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public void start()
    {
        if (audioRecord == null || audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED)
        {
            return;
        }
        if (poolExecutor.isShutdown())
        {
            poolExecutor = Executors.newSingleThreadExecutor();
        }
        poolExecutor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                //标记为开始采集状态
                isRecording = true;
                try
                {
                    //获取到文件的数据流
                    DataOutputStream mDataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(mRecordingFile, true)));
                    byte[] buffer = new byte[recordBufSize];
                    audioRecord.startRecording();//开始录音
                    //getRecordingState获取当前AudioReroding是否正在采集数据的状态
                    while (isRecording && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
                    {
                        int bufferReadResult = audioRecord.read(buffer, 0, recordBufSize);
                        for (int i = 0; i < bufferReadResult; i++)
                        {
                            mDataOutputStream.write(buffer[i]);
                        }
                    }
                    mDataOutputStream.close();
                } catch (Throwable t)
                {
                    stop();
                }
            }
        });
    }

    @Override
    public void stop()
    {
        isRecording = false;
        //停止录音，回收AudioRecord对象，释放内存
        if (audioRecord != null)
        {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
            {
                audioRecord.stop();
            }
        }
    }

    @Override
    public void release()
    {
        if (audioRecord != null)
        {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
            {
                audioRecord.stop();
            }
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED)
            {
                audioRecord.release();
            }
        }
        if (poolExecutor != null)
        {
            poolExecutor.shutdown();
        }
    }
}
