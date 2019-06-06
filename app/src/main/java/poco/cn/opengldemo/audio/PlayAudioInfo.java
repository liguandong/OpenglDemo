package poco.cn.opengldemo.audio;

/**
 * Created by: fwc
 * Date: 2019/2/26
 */
public class PlayAudioInfo implements Comparable<PlayAudioInfo>
{
    public int fromTime;
    public int toTime;
    public int startTime;
    public int endTime;
    public int duration;
    public final String path;
    public float volume = 1.0f;
    public PlayAudioInfo(String path)
    {
        this.path = path;

    }

    @Override
    public int compareTo(PlayAudioInfo o)
    {
        return startTime - o.startTime;
    }
}
