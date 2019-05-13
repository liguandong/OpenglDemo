package poco.cn.opengldemo;

import android.app.Application;

import poco.cn.opengldemo.utils.ShareData;

/**
 * Created by lgd on 2019/5/9.
 */
public class MyApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        ShareData.InitData(this);
    }
}
