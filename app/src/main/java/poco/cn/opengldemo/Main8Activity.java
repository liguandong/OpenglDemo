package poco.cn.opengldemo;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import poco.cn.opengldemo.audio.AudioPlayer;
import poco.cn.opengldemo.audio.PlayAudioInfo;

public class Main8Activity extends AppCompatActivity implements View.OnClickListener
{
    protected Button playAudio;
    protected Button stopPlay;
    protected SeekBar seekBar;
    private AudioPlayer audioPlayer;

    private List<PlayAudioInfo> audioPlayerList;

    private Handler handler = new Handler();
    private PlayAudioInfo playAudioInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main8);
        initView();
    }


    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.playAudio)
        {
            mStartTime = System.currentTimeMillis();
            audioPlayer.setCurTime(0);
            handler.removeCallbacks(mTimeRunnable);
            handler.post(mTimeRunnable);
            audioPlayer.start();
        } else if (view.getId() == R.id.stopPlay)
        {
            handler.removeCallbacks(mTimeRunnable);
            audioPlayer.pause();
        }
    }

    private long mStartTime;
    private Runnable mTimeRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            long recordTime = System.currentTimeMillis() - mStartTime;
            if (recordTime < 10000)
            {
                audioPlayer.setCurTime((int) recordTime);
                handler.post(this);
            }else{
                audioPlayer.pause();
            }
        }
    };

    private void initView()
    {
        playAudio = (Button) findViewById(R.id.playAudio);
        playAudio.setOnClickListener(Main8Activity.this);
        stopPlay = (Button) findViewById(R.id.stopPlay);
        stopPlay.setOnClickListener(Main8Activity.this);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnClickListener(Main8Activity.this);

        audioPlayer = new AudioPlayer();

        seekBar.setMax(100);
        seekBar.setProgress(100);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                float a = progress / (float) seekBar.getMax();
                playAudioInfo.volume = a;
                audioPlayer.updateVolume();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });
        audioPlayerList = new ArrayList<>();

        String path = Environment.getExternalStorageDirectory() + File.separator + "east.aac";
        playAudioInfo = new PlayAudioInfo(path);
        playAudioInfo.startTime = 0;
        playAudioInfo.endTime = 5000;
        playAudioInfo.fromTime = 5000;
        playAudioInfo.toTime = 5000;
        audioPlayerList.add(playAudioInfo);

        PlayAudioInfo playAudioInfo = new PlayAudioInfo(path);
        playAudioInfo.startTime = 5001;
        playAudioInfo.endTime = 10000;
        playAudioInfo.fromTime = 0;
        playAudioInfo.toTime = 5000;

        audioPlayerList.add(playAudioInfo);
        audioPlayer.setAudioInfos(audioPlayerList);
        audioPlayer.prepare();



    }
}
