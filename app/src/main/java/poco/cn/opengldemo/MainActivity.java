package poco.cn.opengldemo;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
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
}
