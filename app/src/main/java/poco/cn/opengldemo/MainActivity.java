package poco.cn.opengldemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import poco.cn.opengldemo.utils.ShareData;
import poco.cn.opengldemo.video.view.GLVideoViewV2;
import poco.cn.opengldemo.video.view.VideoBaseInfo;

/**
 * 视频播放，自定义opengl渲染
 */
public class MainActivity extends AppCompatActivity
{

    private GLVideoViewV2 mVideoView;
    private String[] must;

    //    /storage/emulated/0/DCIM/InterPhoto/InterPhoto_1556093181910.mp4  1:1
//    /storage/emulated/0/DCIM/InterPhoto/InterPhoto_1556093222209.mp4    9:16
//    /storage/emulated/0/DCIM/InterPhoto/InterPhoto_1556093247104.mp4  16:9
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ShareData.InitData(this);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView()
    {
        mVideoView = (GLVideoViewV2) findViewById(R.id.mVideoView);

        must = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};
        boolean hasPerminsion = true;
        for (String p : must)
        {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED){
                hasPerminsion = false;
                //请求权限

            }
        }
        if(hasPerminsion){
            startVideo();
        }else
        {
            ActivityCompat.requestPermissions(this, must, 1);
        }

    }

    private void startVideo()
    {



        VideoBaseInfo info1 = VideoBaseInfo.get("/storage/emulated/0/DCIM/InterPhoto/InterPhoto_1556093181910.mp4");
        VideoBaseInfo info16 = VideoBaseInfo.get("/storage/emulated/0/DCIM/InterPhoto/InterPhoto_1556093222209.mp4");
        VideoBaseInfo info19 = VideoBaseInfo.get("/storage/emulated/0/DCIM/InterPhoto/InterPhoto_1556093247104.mp4");


        VideoBaseInfo[] baseInfos = new VideoBaseInfo[1];
//        baseInfos[0] = info1;
        baseInfos[0] = info16;
//        baseInfos[0] = info19;
        mVideoView.setVideoInfos(baseInfos);
        mVideoView.start();
        mVideoView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mVideoView.isPlaying())
                {
                    mVideoView.pause();
                } else
                {
                    mVideoView.resume();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPerminsion = true;
        for (String p : must)
        {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED){
                hasPerminsion = false;
                //请求权限
            }
        }
        if(hasPerminsion){
            startVideo();
        }

    }
}
