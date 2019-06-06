package poco.cn.opengldemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import poco.cn.opengldemo.audio.VoiceOverRecordMgr;

public class Main7Activity extends AppCompatActivity implements View.OnClickListener
{

    protected Button startRecord;
    protected Button stopRecord;
    protected Button playAudio;
    protected Button stopPlay;
    protected SeekBar seekBar;

    private VoiceOverRecordMgr voiceOverRecordMgr;
    private String outPath;
    private MediaPlayer mediaPlayer;
    private String[] must;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main7);
        initView();
        outPath = Environment.getExternalStorageDirectory() + File.separator + "openglDemo" + File.separator + "audio.aac";
        voiceOverRecordMgr = new VoiceOverRecordMgr();
        voiceOverRecordMgr.setListener(new VoiceOverRecordMgr.OnRecordListener()
        {
            @Override
            public void onRecordFinish(String path)
            {
                startRecord.setText("startRecord");

            }

            @Override
            public void onStopEnable(boolean canStop)
            {

            }

            @Override
            public void onProgressChanged(float progress, long recordTime)
            {
                startRecord.setText("recording:" + recordTime);
            }
        });
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setLooping(true);  // 设置循环播放
        mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener()
        {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra)
            {
                return false;
            }
        });

        must = new String[]{Manifest.permission.RECORD_AUDIO};
        boolean hasPerminsion = true;
        for (String p : must)
        {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
            {
                hasPerminsion = false;
                //请求权限

            }
        }
        if (!hasPerminsion)
        {
            ActivityCompat.requestPermissions(this, must, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1)
        {
            boolean hasPerminsion = true;
            for (String p : must)
            {
                if (ContextCompat.checkSelfPermission(Main7Activity.this, p) != PackageManager.PERMISSION_GRANTED)
                {
                    hasPerminsion = false;
                    //请求权限
                }
            }
            if (hasPerminsion)
            {
                Toast.makeText(Main7Activity.this, "请开启权限", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.startRecord)
        {
            if (mediaPlayer.isPlaying())
            {
                mediaPlayer.stop();
            }
            voiceOverRecordMgr.startRecord(4000, outPath);

        } else if (view.getId() == R.id.stopRecord)
        {
            voiceOverRecordMgr.stopRecord();

        } else if (view.getId() == R.id.playAudio)
        {
            try
            {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(outPath);
                mediaPlayer.prepare();
                mediaPlayer.start();
                playAudio.setText("playing");
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        } else if (view.getId() == R.id.stopPlay)
        {
            mediaPlayer.stop();
            playAudio.setText("playAudio");
        }
    }

    private void initView()
    {
        startRecord = (Button) findViewById(R.id.startRecord);
        startRecord.setOnClickListener(Main7Activity.this);
        stopRecord = (Button) findViewById(R.id.stopRecord);
        stopRecord.setOnClickListener(Main7Activity.this);
        playAudio = (Button) findViewById(R.id.playAudio);
        playAudio.setOnClickListener(Main7Activity.this);
        stopPlay = (Button) findViewById(R.id.stopPlay);
        stopPlay.setOnClickListener(Main7Activity.this);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                float a = progress/(float)seekBar.getMax();
                voiceOverRecordMgr.setVolume(a);
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
    }
}
