package poco.cn.medialibs.media;

import java.util.ArrayList;
import java.util.List;

public class SoundSwitcher {
    private byte[] mWaveData;
    private int mSamplePerSecond = 20;
    private int mMaxVolume = 0;

    public SoundSwitcher()
    {
    }

    /**
     * 加载音乐文件，生成波形数据
     * @param sound 音乐文件
     */
    public boolean loadSound(String sound)
    {
        AVInfo avInfo = new AVInfo();
        byte[] data = AVUtils.avAudioWave(sound, mSamplePerSecond, avInfo, null);
        if(data != null)
        {
            mWaveData = data;
            mMaxVolume = 0;
            for(int i = 0; i < data.length; i++) {
                int v = data[i];
                if(v < 0)
                {
                    v = -v;
                }
                if(v > mMaxVolume)
                {
                    mMaxVolume = v;
                }
            }
        }
        return data != null;
    }

    /**
     * 设置采样间隔
     * @param value 每秒钟采样多少个数据
     */
    public void setSamplePerSecond(int value)
    {
        mSamplePerSecond = value;
    }

    /**
     * 获取每秒钟采样个数
     */
    public int getSamplePerSecond()
    {
        return mSamplePerSecond;
    }

    /**
     * 获取当前音频的最大音量
     */
    public int getMaxVolume()
    {
        return mMaxVolume;
    }

    /**
     * 获取切换时间点
     */
    public List<Long> getSwitchPoints(int volume)
    {
        if(mWaveData == null)
        {
            return null;
        }
        ArrayList<Long> points = new ArrayList<Long>();
        int last = 0;
        int sampleDur = 1000/mSamplePerSecond;
        for(int i = 0; i < mWaveData.length; i++) {
            int v = mWaveData[i];
            if(v < 0)
            {
                v = -v;
            }
            if(v >= volume && last < volume)
            {
                points.add((long)i*sampleDur);
            }
            last = v;
        }
        return points;
    }
}
