package poco.cn.medialibs.media;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Created by: hwq
 * Date: 2018/5/9
 */
public class AVUtils {

    private AVUtils() {
    }

    /**
     * 视频缩放(软解or硬解，软编)
     *
     * @param inputPath  原视频路径
     * @param fps        帧率
     * @param fixFps     是否固定帧率，如果为true表示输出帧率一定会等于fps，否则fps只作为上限，如果原始帧率小于fps，那么以原始帧率为准
     * @param vcodecOpts 视频编码器选项，示例：crf=28,xxx=xxx或crf=28，用逗号分隔选项，=号前为key，后为value。
     *                   crf选项用于设置视频输出的质量，取值范围为0~51，其中0为无损模式，数值越大，画质越差，生成的文件却越小。
     *                   从主观上讲，18~28是一个合理的范围。18被认为是视觉无损的（从技术角度上看当然还是有损的），它的输出视频质量和输入视频相当。
     * @param size       输出的视频大小。该数值表示输出视频最短边的大小，如原视频是720*1280，如果size为640，那么输出的视频尺寸为640*1136。
     *                   如果原始大小比size要小的话，输出的尺寸保持原始大小
     * @param bitRate    视频编码比特率，如果为0或指定了crf选项，则由接口自行决定比特率。
     *                   请注意该参数必须是2的倍数，否则函数内部会自动调整输出宽高为2的倍数
     * @param hwdecode   是否使用硬件解码，大于720p的视频用硬解会快一点，2k以上视频用硬解会明显快很多，4k以上视频用硬解部分机器可能无法解码。
     *                   软解因为没有与系统和硬件相关，因此稳定性最佳。硬解暂时发现处理大分辨率视频，或用多线程同时处理多个较大视频时部分机型会无法解码。
     * @param vcodecId   指定输出视频编码格式(见AVCodecID)，如果为零表示使用原始视频编码格式
     * @param listener   进度监听器
     * @param outputPath 视频保存路径
     * @return 是否成功
     */
    public static boolean avResize(String inputPath, int fps, boolean fixFps, String vcodecOpts, int size, int bitRate, boolean hwdecode, int vcodecId, AVListener listener, String outputPath) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }

        int result = AVNative.AVResize(inputPath, outputPath, fps, fixFps, vcodecOpts, size, bitRate, hwdecode, vcodecId, listener);
        AVTracer.getInstance().addMethod("AVResize", result >= 0, new String[]{inputPath}, inputPath, outputPath, fps, fixFps, vcodecOpts, size, bitRate, hwdecode, vcodecId, listener);
        return result >= 0;
    }

    /**
     * 设置AssetManager，接口支持传递assets目录文件作为输入文件时需要调用此函数，否则不支持。此函数可只调用一次，不需重复调用。传递assets文件时，文件名以 file:///android_asset/文件名 的形式传递
     *
     * @param mgr AssetManager
     */
    public static void setAssetManager(AssetManager mgr) {
        if (mgr != null) {
            AVNative.SetAssetManager(mgr);
        }
    }

    /**
     * 获取视频相关信息，注意：由于底层会对AVInfo的属性进行赋值，请勿对AVInfo进行混淆加密，否则会出错
     *
     * @param inputPath 输入文件，可以是音频或视频，支持assets目录文件，见setAssetManager函数说明
     * @param info      输出视频相关信息
     * @param detail    是否需要获取详细信息，如maxGop,minGop,keyFrameCount,frameCount，为true的话耗时更长
     * @return 是否成功
     */
    public static boolean avInfo(String inputPath, AVInfo info, boolean detail) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }

        int result = AVNative.AVInfo(inputPath, info, detail);
        return result >= 0;
    }

    /**
     * 视频加速
     *
     * @param inputPath  原视频路径，支持assets目录文件，见setAssetManager函数说明
     * @param speedRatio 加速因子
     * @param outputPath 视频保存路径，后缀名需要与输入保持一致
     * @return 是否成功
     */
    public static boolean avSpeed(String inputPath, float speedRatio, String outputPath) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }

        int result = AVNative.AVSpeed(inputPath, outputPath, 1 / speedRatio);
        AVTracer.getInstance().addMethod("AVSpeed", result >= 0, new String[]{inputPath}, inputPath, outputPath, 1 / speedRatio);
        return result >= 0;
    }

    /**
     * 裁剪音视频
     *
     * @param inputPath  输入文件，可以是音频或视频，支持assets目录文件，见setAssetManager函数说明
     * @param start      开始时间，单位毫秒
     * @param end        结束时间，单位毫秒
     * @param outputPath 保存路径，后缀名需要与输入保持一致
     * @return 是否成功
     */
    public static boolean avClip(String inputPath, long start, long end, String outputPath) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }

        if (start >= end) {
            throw new IllegalArgumentException("the time is not correct.");
        }

        int result = AVNative.AVClip(inputPath, outputPath, (int) start, (int) end);
        AVTracer.getInstance().addMethod("AVClip", result >= 0, new String[]{inputPath}, inputPath, outputPath, (int) start, (int) end);
        return result >= 0;
    }

    /**
     * 合并音视频
     *
     * @param aInputPath    音频输入文件(aac,wav..等),支持assets目录文件，见setAssetManager函数说明
     * @param vInputPath    视频或视频流文件(h264,h265,mp4..等)
     * @param isBaseOnVideo 合并输出视频的时长是否基于视频或者视频流的时长:
     *                      true : 以输入的视频时长为准，如果输入的音频时长较短，视频末尾没有声音；
     *                      如果输入的音频时长较长，舍弃超出部分音频；
     *                      false: 以输入的音频时长为准，如果输入的视频时长较短，视频末尾画面不动。
     *                      如果输入的视频时长较长，舍弃超出部分视频
     * @param outputPath    视频保存路径
     * @return 是否成功
     */
    public static boolean avMerge(String aInputPath, String vInputPath, boolean isBaseOnVideo, String outputPath) {
        return avMerge(aInputPath, vInputPath, 0, true, isBaseOnVideo, outputPath);
    }


    /**
     * 合并音视频
     *
     * @param aInputPath    音频输入文件(aac,wav..等),支持assets目录文件，见setAssetManager函数说明
     * @param vInputPath    视频或视频流文件(h264,h265,mp4..等)
     * @param rotation      0，90,180,270
     * @param append        是否在视频本身的旋转值基础上加上rotation
     * @param isBaseOnVideo 合并输出视频的时长是否基于视频或者视频流的时长:
     *                      true : 以输入的视频时长为准，如果输入的音频时长较短，视频末尾没有声音；
     *                      如果输入的音频时长较长，舍弃超出部分音频；
     *                      false: 以输入的音频时长为准，如果输入的视频时长较短，视频末尾画面不动。
     *                      如果输入的视频时长较长，舍弃超出部分视频
     * @param outputPath    视频保存路径
     * @return 是否成功
     */
    public static boolean avMerge(String aInputPath, String vInputPath, int rotation, boolean append, boolean isBaseOnVideo, String outputPath) {
        if (!isFileExist(vInputPath) && !isFileExist(aInputPath)) {
            throw new IllegalArgumentException("input file not found.");
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }
        if (aInputPath == null) {
            aInputPath = "";
        }
        if (vInputPath == null) {
            vInputPath = "";
        }
        int result = AVNative.AVMerge(aInputPath, vInputPath, rotation, append, isBaseOnVideo, outputPath);
        AVTracer.getInstance().addMethod("AVMerge", result >= 0, new String[]{aInputPath, vInputPath, outputPath}, aInputPath, vInputPath, outputPath);
        return result >= 0;
    }

    /**
     * 分割视频
     *
     * @param inputPath   原视频路径，支持assets目录文件，见setAssetManager函数说明
     * @param splitTime   分割时间点，单位毫秒
     * @param outputPath1 视频分割保存路径1，后缀名需要与输入保持一致
     * @param outputPath2 视频分割保存路径2，后缀名需要与输入保持一致
     * @return 是否成功
     */
    public static boolean avSegment(String inputPath, long splitTime, String outputPath1, String outputPath2) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        if (!isPathExist(outputPath1)) {
            throw new IllegalArgumentException("the path: " + outputPath1 + " not found.");
        }
        if (!isPathExist(outputPath2)) {
            throw new IllegalArgumentException("the path: " + outputPath2 + " not found.");
        }

        int result = AVNative.AVSegment(inputPath, (int) splitTime, outputPath1, outputPath2);
        AVTracer.getInstance().addMethod("AVSegment", result >= 0, new String[]{inputPath}, inputPath, (int) splitTime, outputPath1, outputPath2);
        return result >= 0;
    }

    /**
     * 连接多个音视频，支持音频和音频连接，输出支持mp4和aac格式
     *
     * @param files      输入音视频文件，个数必须大于等于2，暂无输入文件格式要求，但必须是全部为音频或视频文件。支持assets目录文件，见setAssetManager函数说明
     * @param outputPath 视频保存路径，如果为音频后缀名必须是aac
     * @return 是否成功
     */
    public static boolean avConcat(String[] files, String outputPath) {
        for (String file : files) {
            if (!isFileExist(file)) {
                throw new IllegalArgumentException("the input file not found.");
            }
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }

        int result = AVNative.AVConcat(files, outputPath);
        AVTracer.getInstance().addMethod("AVConcat", result >= 0, files, files, outputPath);
        return result >= 0;
    }

    /**
     * 视频帧读取
     *
     * @param inputPath 视频路径。支持assets目录文件，见setAssetManager函数说明
     * @param start     解码开始时间，<= 0表示不指定开始时间，从头开始解码
     * @param end       解码结束时间，-1表示不指定结束时间，直到所有帧解码完毕才返回
     * @param dataType  指定onFrame回调data的类型，值为AVNative.DATA_FMT_*， 支持byte[],int[],Bitmap
     * @param pixFormat 指定解码输出格式，值为AVPixelFormat.*，如需输出Bitmap，应指定为AV_PIX_FMT_RGBA或AV_PIX_FMT_RGB565
     * @param cb        解码回调，解码完一帧后通过此方法回调给调用者，AVFrameReceiver.onFrame方法的返回值可以控制是否继续解码，如为false表示继续，true表示中断解码
     * @return 是否成功
     */
    public static boolean avDecode(String inputPath, long start, long end, int dataType, int pixFormat, AVNative.AVFrameReceiver cb) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }

        int result = AVNative.AVDecode(inputPath, (int) start, (int) end, dataType, pixFormat, cb);
        return result >= 0;
    }

    /**
     * 读取一帧图像
     *
     * @param inputPath    视频路径。支持assets目录文件，见setAssetManager函数说明
     * @param time         指定帧所在时间戳
     * @param dataType     指定返回数据类型，值为AVNative.DATA_FMT_*， 支持byte[],int[],Bitmap
     * @param pixFormat    指定解码输出格式，值为AVPixelFormat.*，如需输出Bitmap，应指定为AV_PIX_FMT_RGBA或AV_PIX_FMT_RGB565
     * @param hardware     是否是硬件解码
     * @param seekKeyFrame 是否以关键帧为起点，如果为true的话，下一次调用nextFrame返回的帧一定是关键帧，但帧时间戳可能比time小很多。为false的话，能确保返回的帧时间戳与time极其接近，但所花的时间会比较长
     * @param size         输出图像最短边大小,为0表示使用原始大小
     * @return 解码后的帧数据，需要进行强转为与dataType一致的类型，例如
     * Bitmap bmp = (Bitmap)avDecodeOneFrame(xxx, 1000, AVNative.DATA_FMT_BITMAP, AVPixelFormat.AV_PIX_FMT_RGBA);
     */
    public static Object avDecodeOneFrame(String inputPath, long time, int dataType, int pixFormat, boolean hardware, boolean seekKeyFrame, int size) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }

        AVVideoDecoder decoder = AVVideoDecoder.build(hardware);
        decoder.create(inputPath, dataType, pixFormat);
        decoder.seek((int) time, seekKeyFrame);
        if (size > 0) {
            decoder.setSize(size);
        }
        AVFrameInfo info = new AVFrameInfo();
        Object data = decoder.nextFrame(info);
        decoder.release();
        return data;
    }

    /**
     * 读取一帧图像
     *
     * @param inputPath    视频路径。支持assets目录文件，见setAssetManager函数说明
     * @param time         指定帧所在时间戳
     * @param hardware     是否是硬件解码
     * @param seekKeyFrame 是否以关键帧为起点，如果为true的话，下一次调用nextFrame返回的帧一定是关键帧，但帧时间戳可能比time小很多。为false的话，能确保返回的帧时间戳与time极其接近，但所花的时间会比较长
     * @param size         输出图像最短边大小,为0表示使用原始大小
     * @return 解码后的帧图片
     */
    public static Bitmap avDecodeOneFrame(String inputPath, long time, boolean hardware, boolean seekKeyFrame, int size) {
        return (Bitmap) avDecodeOneFrame(inputPath, time, AVNative.DATA_FMT_BITMAP, AVPixelFormat.AV_PIX_FMT_RGBA, hardware, seekKeyFrame, size);
    }

    /**
     * 读取一帧图像
     *
     * @param inputPath 视频路径。支持assets目录文件，见setAssetManager函数说明
     * @param time      指定帧所在时间戳
     * @param size      输出图像最短边大小,为0表示使用原始大小
     * @return 解码后的帧图片
     */
    public static Bitmap avDecodeOneFrame(String inputPath, long time, int size) {
        return (Bitmap) avDecodeOneFrame(inputPath, time, AVNative.DATA_FMT_BITMAP, AVPixelFormat.AV_PIX_FMT_RGBA, false, false, size);
    }

    /**
     * 读取一帧图像
     *
     * @param inputPath 视频路径。支持assets目录文件，见setAssetManager函数说明
     * @param time      指定帧所在时间戳
     * @return 解码后的帧图片
     */
    public static Bitmap avDecodeOneFrame(String inputPath, long time) {
        return (Bitmap) avDecodeOneFrame(inputPath, time, AVNative.DATA_FMT_BITMAP, AVPixelFormat.AV_PIX_FMT_RGBA, false, false, 0);
    }

    /**
     * 视频旋转
     *
     * @param inputPath  视频路径。支持assets目录文件，见setAssetManager函数说明
     * @param rotation   0，90,180,270
     * @param append     是否在视频本身的旋转值基础上加上rotation
     * @param outputPath 视频保存路径，后缀名需要与输入保持一致
     * @return 是否成功
     */
    public static boolean avRotate(String inputPath, int rotation, boolean append, String outputPath) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }

        int result = AVNative.AVRotate(inputPath, outputPath, rotation, append);
        return result >= 0;
    }

    /**
     * 视频重新封装，主要用于调整mp4 moov box位置
     *
     * @param inputPath  原视频路径
     * @param outputPath 视频保存路径，后缀名需要与输入保持一致
     * @return 是否成功
     */
    public static boolean avRemuxer(String inputPath, String outputPath) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }

        int result = AVNative.AVRemuxer(inputPath, outputPath);
        AVTracer.getInstance().addMethod("AVRemuxer", result >= 0, new String[]{inputPath}, inputPath, outputPath);
        return result >= 0;
    }

    /**
     * 是否mp4的moov在mdat后面
     *
     * @param inputPath mp4文件
     * @return 是否moov在mdat后面
     */
    public static boolean isMoovOnBack(String inputPath) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }

        if (!inputPath.toLowerCase().endsWith(".mp4")) {
            return false;
        }

        return AVNative.IsMoovOnBack(inputPath);
    }

    /**
     * 提取视频文件中的音频
     *
     * @param inputPath  视频路径。支持assets目录文件，见setAssetManager函数说明
     * @param outputPath 音频保存路径,支持wav,aac格式
     * @return 是否成功
     */
    public static boolean avAudioExtract(String inputPath, String outputPath) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }

        int result = AVNative.AVAudioExtract(inputPath, outputPath);
        AVTracer.getInstance().addMethod("AVAudioExtract", result >= 0, new String[]{inputPath}, inputPath, outputPath);
        return result >= 0;
    }

    /**
     * 音量调节和前后声音渐变
     *
     * @param inputPath   输入文件，可以是音频或视频。支持assets目录文件，见setAssetManager函数说明
     * @param volume      音量，1为原始音量，否则为提高或降低音量
     * @param fadeIn      否需要渐入（视频开始时声音从低到高变化）
     * @param timeFadeIn  渐入时长
     * @param fadeOut     否需要渐出（视频结束时声音从高到低变化）
     * @param timeFadeOut 渐出时长
     * @param outputPath  保存路径。如果是视频后缀名需要与输入保持一致。如果是音频可以不同格式，支持wav,aac
     * @return 是否成功
     */
    public static boolean avAudioVolume(String inputPath, float volume, boolean fadeIn, int timeFadeIn, boolean fadeOut, int timeFadeOut, String outputPath) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }

        int result = AVNative.AVAudioVolume(inputPath, outputPath, volume, fadeIn, timeFadeIn, fadeOut, timeFadeOut);
        AVTracer.getInstance().addMethod("AVAudioVolume", result >= 0, new String[]{inputPath}, inputPath, outputPath, volume, fadeIn, timeFadeIn, fadeOut, timeFadeOut);
        return result >= 0;
    }

    /**
     * 替换视频中的声音
     *
     * @param inputPath   视频路径。支持assets目录文件，见setAssetManager函数说明
     * @param audioPath   用来替换的音频文件,如果为null或""则表示清除视频声音，可以是mp4，mp3,aac,wav等格式
     * @param repeat      当视频时长大于音频时长的时候，是否重复音频
     * @param fadeIn      否需要渐入（视频开始时声音从低到高变化）
     * @param timeFadeIn  渐入时长
     * @param fadeOut     否需要渐出（视频结束时声音从高到低变化）
     * @param timeFadeOut 渐出时长
     * @param outputPath  视频保存路径，后缀名需要与输入保持一致
     * @return 是否成功
     */
    public static boolean avAudioReplace(String inputPath, String audioPath, boolean repeat, boolean fadeIn, int timeFadeIn, boolean fadeOut, int timeFadeOut, String outputPath) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }

        int result = AVNative.AVAudioReplace(inputPath, outputPath, audioPath, repeat, fadeIn, timeFadeIn, fadeOut, timeFadeOut);
        AVTracer.getInstance().addMethod("AVAudioReplace", result >= 0, new String[]{inputPath}, inputPath, outputPath, audioPath, repeat, fadeIn, timeFadeIn, fadeOut, timeFadeOut);
        return result >= 0;
    }

    /**
     * 两个声音混音，输入可以是音频也可以是视频
     *
     * @param inputPath     输入文件，可以是音频也可以是视频。支持assets目录文件，见setAssetManager函数说明
     * @param audioPath     用来混音的音频文件，可以是mp4，mp3,aac,wav等格式
     * @param volumeInput   inputPath的音量调整，1为原始音量，否则为提高或降低音量
     * @param volumeAudio   audioPath的音量调整，1为原始音量，否则为提高或降低音量
     * @param repeat        当视频时长大于音频时长的时候，是否重复音频
     * @param justAudioFade 是否只是对添加的音效做渐变处理
     * @param fadeIn        否需要渐入（视频开始时声音从低到高变化）
     * @param timeFadeIn    渐入时长
     * @param fadeOut       否需要渐出（视频结束时声音从高到低变化）
     * @param timeFadeOut   渐出时长
     * @param outputPath    视频保存路径，视频后缀名需要与输入保持一致，音频支持wav,aac
     * @return 是否成功
     */
    public static boolean avAudioMix(String inputPath, String audioPath, float volumeInput, float volumeAudio, boolean repeat, boolean justAudioFade, boolean fadeIn, int timeFadeIn, boolean fadeOut, int timeFadeOut, String outputPath) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }

        int result = AVNative.AVAudioMix(inputPath, audioPath, outputPath, volumeInput, volumeAudio, repeat, justAudioFade, fadeIn, timeFadeIn, fadeOut, timeFadeOut);
        AVTracer.getInstance().addMethod("AVAudioMix", result >= 0, new String[]{inputPath}, inputPath, audioPath, outputPath, volumeInput, volumeAudio, repeat, justAudioFade, fadeIn, timeFadeIn, fadeOut, timeFadeOut);
        return result >= 0;
    }

    /**
     * 多个音频文件混音
     *
     * @param audios         输入文件，至少是2个或2个以上，可以是mp4，mp3,aac,wav等格式。支持assets目录文件，见setAssetManager函数说明
     * @param dontFadeIndexs 指定不需要做渐变的audios下标，可以指定一个或者多个。null表示全部都做渐变
     * @param fadeIn         否需要渐入（视频开始时声音从低到高变化）
     * @param timeFadeIn     渐入时长
     * @param fadeOut        否需要渐出（视频结束时声音从高到低变化）
     * @param timeFadeOut    渐出时长
     * @param outputPath     保存路径，支持wav,aac
     * @return 是否成功
     */
    public static boolean avAudioMix(String[] audios, int[] dontFadeIndexs, boolean fadeIn, int timeFadeIn, boolean fadeOut, int timeFadeOut, String outputPath) {
        for (String file : audios) {
            if (!isFileExist(file)) {
                throw new IllegalArgumentException("the input file not found.");
            }
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }

        int result = AVNative.AVAudioMultiMix(audios, dontFadeIndexs, outputPath, fadeIn, timeFadeIn, fadeOut, timeFadeOut);
        AVTracer.getInstance().addMethod("AVAudioMultiMix", result >= 0, audios, outputPath, fadeIn, timeFadeIn, fadeOut, timeFadeOut);
        return result >= 0;
    }

    /**
     * 音频格式转换
     *
     * @param inputPath  输入文件，可以是mp4，mp3,aac,wav等格式。支持assets目录文件，见setAssetManager函数说明
     * @param outputPath 保存路径，支持wav,aac格式
     * @param sampleRate 音频采样率,除wav外，其它输出格式仅支持48000以下采样率，具体数值请查阅资料，常用采样率为48000,44100
     * @return 是否成功
     */
    public static boolean avAudioConvert(String inputPath, String outputPath, int sampleRate) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }

        int result = AVNative.AVAudioConvert(inputPath, outputPath, sampleRate);
        AVTracer.getInstance().addMethod("AVAudioConvert", result >= 0, new String[]{inputPath}, inputPath, outputPath, sampleRate);
        return result >= 0;
    }

    /**
     * 音频延长
     *
     * @param inputPath    输入音频文件，如果inputPath是视频，outputPath的后缀应该是.aac。支持assets目录文件，见setAssetManager函数说明
     * @param srcClipStart 原音频文件的裁剪起始点，不裁剪传0
     * @param srcClipEnd   原音频文件的裁剪结束点，不裁剪传-1
     * @param newDuration  指定输出时长
     * @param outputPath   保存路径，格式需要与输入保持一致
     * @return 是否成功
     */
    public static boolean avAudioRepeat(String inputPath, long srcClipStart, long srcClipEnd, long newDuration, String outputPath) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        if (!isPathExist(outputPath)) {
            throw new IllegalArgumentException("the path: " + outputPath + " not found.");
        }

        int result = AVNative.AVAudioRepeat(inputPath, outputPath, (int) srcClipStart, (int) srcClipEnd, (int) newDuration);
        AVTracer.getInstance().addMethod("AVAudioRepeat", result >= 0, new String[]{inputPath}, inputPath, outputPath, (int) srcClipStart, (int) srcClipEnd, (int) newDuration);
        return result >= 0;
    }

    /**
     * 创建空白音频（没有声音）
     *
     * @param outputPath 保存路径，支持wav,aac格式
     * @param sampleRate 采样率，除wav外，其它输出格式仅支持48000以下采样率，具体数值请查阅资料，常用采样率为48000,44100
     * @param duration   时长
     * @return 是否成功
     */
    public static boolean avAudioCreateBlankAudio(String outputPath, int sampleRate, long duration) {
        int result = AVNative.AVAudioCreateBlankAudio(outputPath, sampleRate, (int) duration);
        AVTracer.getInstance().addMethod("AVAudioCreateBlankAudio", result >= 0, null, outputPath, sampleRate, (int) duration);
        return result >= 0;
    }

    /**
     * 生成音频波形
     *
     * @param inputPath       输入文件，可以是mp4，mp3,aac,wav等格式
     * @param samplePerSecond 每秒钟的音频波形个数
     * @param info            输出，音频长度等相关信息
     * @param progress        进度回调，回调实时更新波形的buffer，如需实时绘制可以在回调种处理。不需要可以传null
     * @return 波形数据，数值范围是-127~127
     */
    public static byte[] avAudioWave(String inputPath, float samplePerSecond, AVInfo info, AVWaveProgress progress) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        return AVNative.AVAudioWave(inputPath, samplePerSecond, info, progress);
    }

    /**
     * 清除指定时间片段的声音并对视频片段加减速处理
     *
     * @param input    输入文件，可以是mp4，mp3,aac,wav等格式
     * @param out      保存路径，后缀名需要与输入保持一致
     * @param clearPts 时间段数组，格式为[开始 结束 加速因子 开始 结束 加速因子]，时间单位为毫秒，加速因子：大于1是减速，小于1是加速
     * @return 是否成功
     */
    public static boolean avAudioClear(String input, String out, float[] clearPts) {
        if (!isFileExist(input)) {
            throw new IllegalArgumentException("the file: " + input + " not found.");
        }
        return AVNative.AVAudioClear(input, out, clearPts) >= 0;
    }

    /**
     * 生成音频波形Bitmap
     *
     * @param inputPath       输入文件，可以是mp4，mp3,aac,wav等格式
     * @param samplePerSecond 每秒钟的波形个数
     * @param height          生成图片的高度
     * @param minHeight       最小绘制波形的高度，如果为0，则当波形幅度是0的时候不进行该波形的绘制
     * @param waveScale       波形的缩放系数，范围0~1，不需要缩放传1
     * @param sampleWidth     单个波形的绘制宽度。生成的Bitmap宽度 = (音频时长 * samplePerSecond) * (sampleWidth + sampleGap)
     * @param sampleGap       波形之间的间隔
     * @param sampleColor     波形的绘制颜色
     * @param bgColor         Bitmap的背景色
     * @param info            输出，音频长度等相关信息
     * @return
     */
    public static Bitmap avCreateWaveBitmap(String inputPath, float samplePerSecond, int height, int minHeight, float waveScale, int sampleWidth, int sampleGap, int sampleColor, int bgColor, AVInfo info) {
        Bitmap bmp = null;
        if (info == null) {
            info = new AVInfo();
        }
        if (samplePerSecond < 0.01f) {
            samplePerSecond = 0.01f;
        }
        byte[] wave = avAudioWave(inputPath, samplePerSecond, info, null);
        if (wave != null && wave.length > 0) {
            if (sampleWidth < 1) {
                sampleWidth = 1;
            }
            if (height < 1) {
                height = 1;
            }
            int space = sampleWidth + sampleGap;
            bmp = Bitmap.createBitmap(wave.length * space, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            canvas.drawColor(bgColor);
            Paint paint = new Paint();
            paint.setColor(sampleColor);
            int yCenter = height / 2;
            for (int i = 0; i < wave.length; i++) {
                int v = wave[i];
                v = height * v / 256;
                if (v < 0) {
                    v = -v;
                }
                v *= waveScale;
                if (v < minHeight) {
                    v = minHeight;
                }
                int x = i * space;
                canvas.drawRect(x, yCenter - v, x + sampleWidth, yCenter, paint);
                canvas.drawRect(x, yCenter, x + sampleWidth, yCenter + v, paint);
            }
        }
        return bmp;
    }

    /**
     * 视频转GIF
     *
     * @param inputPath   输入文件，可以是任意视频格式
     * @param size        输出GIF最大边尺寸
     * @param fps         输出帧率,0表示不改变帧率
     * @param highQuality 是否高质量输出，true：质量高，耗时多，false：质量低，耗时少
     * @param progress    进度回调
     * @return 是否成功
     */
    public static boolean avVideoToGif(String inputPath, String out, int size, int fps, boolean highQuality, AVListener progress) {
        if (!isFileExist(inputPath)) {
            throw new IllegalArgumentException("the file: " + inputPath + " not found.");
        }
        if (!isPathExist(out)) {
            throw new IllegalArgumentException("the path: " + out + " not found.");
        }
        int result = AVNative.AVVideoToGif(inputPath, out, size, fps, highQuality, progress);
        AVTracer.getInstance().addMethod("AVVideoToGif", result >= 0, new String[]{inputPath}, inputPath, out, size, fps, highQuality, progress);
        return result >= 0;
    }

    /**
     * 硬解速度测试
     *
     * @param video 输入文件，可以是任意视频格式
     * @return 每帧解码平均时间
     */
    public static int avCheckHwDecoderSpeed(String video) {
        try {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(video);
            return avCheckHwDecoderSpeed(extractor);
        } catch (Exception e) {
        }
        return 0;
    }

    /**
     * 硬解速度测试，使用assets目录下的speedtest.mp4进行测试
     *
     * @return 每帧解码平均时间
     */
    public static int avCheckHwDecoderSpeed(AssetManager am) {
        try {
            MediaExtractor extractor = new MediaExtractor();
            AssetFileDescriptor afd = am.openFd("speedtest.mp4");
            extractor.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            return AVUtils.avCheckHwDecoderSpeed(extractor);
        } catch (Exception e) {
        }
        return 0;
    }

    /**
     * 硬解速度测试
     *
     * @return 每帧解码平均时间
     */
    public static int avCheckHwDecoderSpeed(MediaExtractor extractor) {
        int frameCount = 0;
        int time = 0;
        long n = 0;
        try {
            MediaFormat format = null;
            int count = extractor.getTrackCount();
            for (int i = 0; i < count; i++) {
                format = extractor.getTrackFormat(i);
                if (format.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                    extractor.selectTrack(i);
                    break;
                }
                format = null;
            }
            if (format != null) {
                int width = format.getInteger(MediaFormat.KEY_WIDTH);
                int height = format.getInteger(MediaFormat.KEY_HEIGHT);
                MediaCodec decoder = null;
                try {
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    decoder = MediaCodec.createDecoderByType(mime);
                    decoder.configure(format, null, null, 0);
                    decoder.start();

                    ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
                    ByteBuffer[] decoderOutputBuffers = decoder.getOutputBuffers();
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    boolean outputDone = false;
                    boolean inputDone = false;
                    while (!outputDone) {

                        if (!inputDone) {
                            int inputBufIndex = decoder.dequeueInputBuffer(8000);
                            if (inputBufIndex >= 0) {
                                ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                                int chunkSize = extractor.readSampleData(inputBuf, 0);
                                if (chunkSize < 0) {
                                    decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                    inputDone = true;
                                } else {
                                    long presentationTimeUs = extractor.getSampleTime();
                                    decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, presentationTimeUs, 0);
                                    extractor.advance();
                                }
                            }
                        }

                        if (!outputDone) {
                            int decoderStatus = decoder.dequeueOutputBuffer(bufferInfo, 8000);
                            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                                decoderOutputBuffers = decoder.getOutputBuffers();
                            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            } else if (decoderStatus < 0) {

                            } else { // decoderStatus >= 0
                                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    outputDone = true;
                                } else {
                                    if (n > 0) {
                                        time += (System.currentTimeMillis() - n);
                                        frameCount++;
                                        if (frameCount >= 6) {
                                            break;
                                        }
                                    }
                                    n = System.currentTimeMillis();
                                }
                                decoder.releaseOutputBuffer(decoderStatus, false);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                extractor.release();
                if (decoder != null) {
                    decoder.release();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (frameCount > 0) {
            return time / frameCount;
        }
        return 0;
    }

    /**
     * 图片处理
     *
     * @param bitmap     要处理的图片
     * @param filterOpts filter的描述，filter间用逗号分隔。示例：filter1=参数1:参数2:参数3=子参数,filter2,filter3=参数1:参数2。
     *                   filter请参考ffmpeg官网说明，http://www.ffmpeg.org/ffmpeg-filters.html，请查看Video Filters节点
     */
    public static Bitmap avImageProcess(Bitmap bitmap, String filterOpts) {
        if (bitmap == null || bitmap.isRecycled()) {
            throw new IllegalArgumentException("the bitmap is unavailable.");
        }
        return AVNative.AVImageProcess(bitmap, filterOpts);
    }

    /**
     * 声音处理
     *
     * @param inputs     输入文件
     * @param out        输出文件
     * @param filterOpts filter的描述，输入源前缀为a，后缀为1~n，例如a1,a2。示例如下：
     *                   [a1]atrim=4:34,afade=t=in:st=4:duration=4,afade=t=out:st=30:duration=4,volume=0.5[v1];[a2]volume=0.8[v2];[v1][v2]amix=inputs=2:duration=longest
     *                   以上命令先对a1做了一次裁剪，接着做了淡入和淡出、音量调整，然后输出为v1。对a2做了音量调整，输出为v2。最后对v1,v2做了混音。
     *                   filter的语法见avImageProcess说明
     * @param progress   进度回调
     */
    public static boolean avAudioProcess(String[] inputs, String out, String filterOpts, AVListener progress) {
        return AVNative.AVAudioProcess(inputs, out, filterOpts, progress) >= 0;
    }

    /**
     * 判断所给文件是否存在
     *
     * @param path 文件路径
     * @return 是否存在
     */
    public static boolean isFileExist(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        if (path.startsWith("file:///android_asset/")) {
            return true;
        }

        File file = new File(path);
        return file.exists() && file.length() > 0;
    }

    /**
     * 判断所给文件所在目录是否存在
     *
     * @param path 文件路径
     * @return 是否存在
     */
    public static boolean isPathExist(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        File file = new File(path).getParentFile();
        return file != null && file.exists();
    }
}