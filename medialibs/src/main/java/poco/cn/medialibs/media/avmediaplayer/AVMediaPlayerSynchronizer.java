package poco.cn.medialibs.media.avmediaplayer;

public interface AVMediaPlayerSynchronizer {
    boolean prepare();
    void    seek(long time);
    void    start();
    void    stop();
    void    pause();
}
