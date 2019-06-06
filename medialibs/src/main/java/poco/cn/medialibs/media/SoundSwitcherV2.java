package poco.cn.medialibs.media;


import java.util.ArrayList;
import java.util.List;

import poco.cn.medialibs.media.utils.FFT;

/**
 * create by : lxh on 2019/3/13 15:25
 * Description:
 */
public class SoundSwitcherV2 {

    private boolean mCreateSuccess;
    private List<Float> mSpectralFlux = new ArrayList<>();
    private ArrayList<float[]> mDataList;
    private List<Integer> mSwitchList;
    private int mSampleRate;
    private int mSampleCount;//只支持2的次方大小，eg:1024
    private int mDuration;

    private static final int THRESHOLD_WINDOW_SIZE = 12;//1s43
    private static final float MULTIPLIER = 1.5f;
    private static final int INTERVAL_FRAME = 6;//14
    private float mValueInterval = 0.25f;

    /**
     * 加载音频文件
     *
     * @param file aac音频文件,支持assets目录文件，见AVUtils.setAssetManager函数说明
     */
    public boolean loadSound(String file) {
        if (mSwitchList != null) {
            mSwitchList.clear();
        }
        if (mSpectralFlux != null) {
            mSpectralFlux.clear();
        }

        AVInfo avInfo = new AVInfo();
        AVUtils.avInfo(file,avInfo,true);
        mDuration = avInfo.duration;

        AVAudioDecoder decoder = new AVAudioDecoder();
        mCreateSuccess = decoder.create(file, AVNative.DATA_FMT_ARRAY_BYTE);

        if (!mCreateSuccess) {
            return false;
        }

        AVFrameInfo avFrameInfo = new AVFrameInfo();
        byte[] srcArr = null;
        mDataList = new ArrayList<>();
        int bytesPerSample = 2;// format s16 16bit

        while ((srcArr = (byte[]) decoder.nextFrame(avFrameInfo)) != null) {
            mSampleRate = avFrameInfo.sampleRate;
            int bytesPerFrame = bytesPerSample * avFrameInfo.channels;
            int sampleCount = srcArr.length / bytesPerFrame;
            List<float[]> channels = new ArrayList<>();
            float[] dstArr = new float[sampleCount];
            mSampleCount = sampleCount;
            int srcOffset = 0;
            for (int ch = 0; ch < avFrameInfo.channels; ++ch) {
                if (channels.size() <= ch) {
                    float[] channel = new float[sampleCount];
                    channels.add(channel);
                }
                convertByteToFloat(srcArr, srcOffset, bytesPerFrame, channels.get(ch), 0, sampleCount);
                srcOffset += bytesPerSample;
            }

            for (int i = 0; i < sampleCount; i++) {
                if (channels.size() > 0) {
                    float data = 0;
                    for (int c = 0; c < channels.size(); c++) {
                        data += channels.get(c)[i];
                    }
                    dstArr[i] = data / channels.size();
                }
            }

            mDataList.add(dstArr);
        }
        decoder.release();
        return mCreateSuccess;
    }

    /**
     * 设置节拍过滤范围，0-1
     *
     * @param interval float 0-1,值越大，能取到的节拍点越少。
     */
    public void setValueInterval(float interval) {
        if (mValueInterval == interval) {
            return;
        }
        if (interval < 0) {
            interval = 0;
        }
        if (interval > 1) {
            interval = 1;
        }
        mValueInterval = interval;
        if (mSwitchList != null) {
            mSwitchList.clear();
        }
    }

    public List<Integer> getSwitchPoints() {
        return getSwitchPoints(mValueInterval);
    }

    /**
     * 获取节拍切换时间点
     * 时间单位：毫秒
     *
     * @param interval 节拍过滤范围，0-1，值越大，能取到的节拍点越少。
     */
    public List<Integer> getSwitchPoints(float interval) {
        setValueInterval(interval);
        if (mSwitchList == null || mSwitchList.size() == 0) {
            if (mCreateSuccess) {
                if (mDataList != null && mDataList.size() > 0) {
                    mSwitchList = calcSwitcherPoint(mDataList, mSampleRate, mSampleCount);
                }
            }
        }
        return mSwitchList;
    }


    private static void convertByteToFloat(byte[] input, int inputOffset, int bytesPerFrame, float[] output, int outputOffset, int sampleCount) {
        int endCount = outputOffset + sampleCount;
        for (int sample = outputOffset; sample < endCount; ++sample) {
            output[sample] = (float) (input[inputOffset + 1] << 8 | input[inputOffset] & 255) * 3.0517578E-5F;
            inputOffset += bytesPerFrame;
        }
    }

    private List<Float> fftToFloat(ArrayList<float[]> dataList, int sampleRate, int sampleCount) {
        if (mSpectralFlux != null && mSpectralFlux.size() > 0) {
            return mSpectralFlux;
        }

        if (!checkSampleCount(sampleCount)) {
            return null;
        }

        FFT fft = new FFT(sampleCount, sampleRate);
        float[] spectrum = new float[sampleCount / 2 + 1];
        float[] lastSpectrum = new float[sampleCount / 2 + 1];

        for (float[] samples : dataList) {
            fft.forward(samples);
            System.arraycopy(spectrum, 0, lastSpectrum, 0, spectrum.length);
            System.arraycopy(fft.getSpectrum(), 0, spectrum, 0, spectrum.length);

            float flux = 0;
            for (int i = 0; i < spectrum.length; i++) {
                float value = (spectrum[i] - lastSpectrum[i]);
                flux += value < 0 ? 0 : value;
            }
            mSpectralFlux.add(flux);
        }
        return mSpectralFlux;
    }

    private List<Integer> calcSwitcherPoint(ArrayList<float[]> dataList, int sampleRate, int sampleCount) {

        if (!checkSampleCount(sampleCount)) {
            return null;
        }
        List<Float> result = new ArrayList<>();
        List<Item> switcherPoint = new ArrayList<>();
        List<Float> threshold = new ArrayList<>();

        List<Float> spectralFlux = fftToFloat(dataList, sampleRate, sampleCount);

        //平均预测值
        for (int i = 0; i < spectralFlux.size(); i++) {
            int start = Math.max(0, i - THRESHOLD_WINDOW_SIZE);
            int end = Math.min(spectralFlux.size() - 1, i + THRESHOLD_WINDOW_SIZE);
            float mean = 0;
            for (int j = start; j <= end; j++)
                mean += spectralFlux.get(j);
            mean /= (end - start);
            threshold.add(mean * MULTIPLIER);
        }

        int count = 0;
        float sum = 0;
        float maxDValue = 0;
        for (int i = 0; i < spectralFlux.size(); i++) {
            if (spectralFlux.get(i) > threshold.get(i)) {
                count++;
                float temp = spectralFlux.get(i) - threshold.get(i);
                if (spectralFlux.get(i) - threshold.get(i) > maxDValue) {
                    maxDValue = spectralFlux.get(i) - threshold.get(i);
                }
                sum += temp;
            }
        }

        maxDValue = maxDValue * mValueInterval;
        float avg = sum / count;
        float thresholdV = maxDValue == 0 ? avg : maxDValue;

        for (int i = 0; i < spectralFlux.size(); i++) {
            float currentThreshold = threshold.get(i) + thresholdV;

            if (spectralFlux.get(i) > currentThreshold) {
                result.add(spectralFlux.get(i));
            } else {
                result.add(threshold.get(i));
            }
        }

        int intervalTime = (int) (getTime(1, sampleRate, sampleCount) * INTERVAL_FRAME);

        for (int i = 0; i < result.size(); i++) {
            if (result.get(i) > threshold.get(i)) {
                if (switcherPoint.size() > 0) {
                    Item lastItem = switcherPoint.get(switcherPoint.size() - 1);
                    long currentTime = getTime(i, sampleRate, sampleCount);
                    if (currentTime - lastItem.time >= intervalTime) {
                        switcherPoint.add(newItem(i, sampleRate, sampleCount));
                    }
                } else {
                    switcherPoint.add(newItem(i, sampleRate, sampleCount));
                }
            }
        }

        List<Integer> switcherList = new ArrayList<>();
        for (int i = 0; i < switcherPoint.size(); i++) {
            Item item = switcherPoint.get(i);
            switcherList.add(item.time);
        }
        return switcherList;
    }

    private int getTime(int position, int sampleRate, int sampleCount) {
        return (1000 / (sampleRate / sampleCount) * position);
    }

    private boolean checkSampleCount(int sampleCount) {
        return sampleCount > 0 && ((sampleCount & (sampleCount - 1)) == 0);
    }

    public boolean isCorrectSample() {
        return checkSampleCount(mSampleCount);
    }

    public float getInterval() {
        return mValueInterval;
    }

    public int getDuration(){
        return mDuration;
    }

    public boolean hasValue() {
        return mSwitchList != null && mSwitchList.size() > 0;
    }

    private Item newItem(int index, int sampleRate, int sampleCount) {
        Item item = new Item();
        item.index = index;
        item.time = getTime(index, sampleRate, sampleCount);
        return item;
    }

    private static class Item {
        int index;
        int time;
    }

}
