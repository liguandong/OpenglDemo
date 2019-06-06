package poco.cn.opengldemo.audio;

import android.media.AudioFormat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;

/**
 * Created by lgd on 2019/5/31.
 */
public interface IRecord
{
    int SAMPLE_RATE_INHZ = 44100;
    int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({State.IDLE, State.PREPARED, State.START,
           State.STOP, State.RELEASE})
    @interface State {
        int IDLE = 0;
        int PREPARED = 2;
        int START = 3;
        int STOP = 6;
        int RELEASE = 7;
    }
    void setOutPath(String path);

    void start();

    void stop();

    void release();
}
