package poco.cn.medialibs.media;

/**
 * Created by hwq on 2018/5/4.
 *
 * 注意：由于底层会对AVInfo的属性进行赋值，请勿对AVFrameInfo进行混淆加密，否则会出错
 */

public class AVFrameInfo {
    public static final int AVMEDIA_TYPE_VIDEO = 0;
    public static final int AVMEDIA_TYPE_AUDIO = 1;
    public int width;
    public int height;
    public int sampleRate;
    public int format;
    public int channels;
    public int pts;
    public int mediatype;
    public Object data;
}
