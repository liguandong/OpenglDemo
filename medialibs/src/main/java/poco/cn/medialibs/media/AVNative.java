package poco.cn.medialibs.media;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import java.nio.ByteBuffer;

/**
 * Created by hwq on 2018/4/23.
 */

public class AVNative {
    static {
        System.loadLibrary("fm4");
    }

    public static final int DATA_FMT_ARRAY_BYTE = 0;
    public static final int DATA_FMT_ARRAY_INT = 1;
    public static final int DATA_FMT_BITMAP = 2;
    public static final int DATA_FMT_ARRAY_FLOAT = 3;

    public interface AVFrameReceiver
    {
        /**
         * 解码帧回调
         * @param data        帧数据，支持byte[],int[],Bitmap，具体类型由调用解码函数时确定
         * @param width       帧宽
         * @param height      帧高
         * @param pts         帧所在时间戳
         * @return            返回false表示继续解码，返回true表示终止解码
         */
        boolean onFrame(Object data, int width, int height, double pts);
    }

    /**
     * 设置AssetManager，接口支持传递assets目录文件作为输入文件时需要调用此函数，否则不支持。传递assets文件时，文件名以 file:///android_asset/文件名 的形式传递
     * @param mgr  AssetManager
     */
    public static native void SetAssetManager(AssetManager mgr);

    /**
     * 获取视频相关信息，注意：由于底层会对AVInfo的属性进行赋值，请勿对AVInfo进行混淆加密，否则会出错
     * @param input       输入文件，可以是音频或视频
     * @param info        输出视频相关信息
     * @param detail      是否需要获取详细信息，如maxGop,minGop,keyFrameCount,frameCount，为true的话耗时更长
     * @return            >=0表示成功
     */
    public static native int AVInfo(String input, AVInfo info, boolean detail);

    /**
     * 分割视频
     * @param input     原视频路径
     * @param time      分割时间点，单位毫秒
     * @param out1      视频分割保存路径1，后缀名需要与输入保持一致
     * @param out2      视频分割保存路径2，后缀名需要与输入保持一致
     * @return          >=0表示成功
     */
    public static native int AVSegment(String input, int time, String out1, String out2);

    /**
     * 裁剪音视频
     * @param input      输入文件，可以是音频或视频
     * @param out        视频保存路径，后缀名需要与输入保持一致
     * @param start      开始时间，单位毫秒
     * @param end        结束时间，单位毫秒
     * @return           >=0表示成功
     */
    public static native int AVClip(String input, String out, int start, int end);

    /**
     * 合并音视频
     *
     * @param aInputPath    音频输入文件(aac,wav..等),支持assets目录文件，见setAssetManager函数说明
     * @param vInputPath    视频或视频流文件(h264,h265,mp4..等)
     * @param rotation   0，90,180,270
     * @param append     是否在视频本身的旋转值基础上加上rotation
     * @param isBaseOnVideo 合并输出视频的时长是否基于视频或者视频流的时长:
     *                      true : 以输入的视频时长为准，如果输入的音频时长较短，视频末尾没有声音；
     *                      如果输入的音频时长较长，舍弃超出部分音频；
     *                      false: 以输入的音频时长为准，如果输入的视频时长较短，视频末尾画面不动。
     *                      如果输入的视频时长较长，舍弃超出部分视频
     * @param outputPath    视频保存路径
     * @return 是否成功
     */
    public static native int AVMerge(String aInputPath, String vInputPath,int rotation,boolean append,boolean isBaseOnVideo,String outputPath);

    /**
     * 连接多个音视频，支持音频和音频连接，输出支持mp4和aac格式
     * @param inputs    输入视频文件，个数必须大于等于2，暂无输入文件格式要求，但必须是全部为音频或视频文件
     * @param out       视频保存路径
     * @return          >=0表示成功
     */
    public static native int AVConcat(String[] inputs, String out);

    /**
     * 视频帧读取
     * @param input        视频路径
     * @param start        解码开始时间，<= 0表示不指定开始时间，从头开始解码
     * @param end          解码结束时间，-1表示不指定结束时间，直到所有帧解码完毕才返回
     * @param dataType     指定onFrame回调data的类型，值为AVNative.DATA_FMT_*， 支持byte[],int[],Bitmap
     * @param pixFormat    指定解码输出格式，值为AVPixelFormat.*，如需输出Bitmap，应指定为AV_PIX_FMT_RGBA或AV_PIX_FMT_RGB565
     * @param cb           解码回调，解码完一帧后通过此方法回调给调用者，AVFrameReceiver.onFrame方法的返回值可以控制是否继续解码，如为false表示继续，true表示中断解码
     * @return             >=0表示成功
     */
    public static native int AVDecode(String input, int start, int end, int dataType, int pixFormat, AVFrameReceiver cb);

    /**
     * 视频旋转
     * @param input        视频路径
     * @param out          视频保存路径，后缀名需要与输入保持一致
     * @param rotation     0，90,180,270
     * @param append       是否在视频本身的旋转值基础上加上rotation
     * @return             >=0表示成功
     */
    public static native int AVRotate(String input, String out, int rotation, boolean append);

    /**
     * 视频加速
     * @param input   原视频路径
     * @param out     视频保存路径，后缀名需要与输入保持一致
     * @param speed   原始速度*speed=目标速度
     * @return        >=0表示成功
     */
    public static native int AVSpeed(String input, String out, float speed);

    /**
     * 视频缩放
     * @param input         原视频路径
     * @param out           视频保存路径
     * @param fps           帧率
     * @param fixFps        是否固定帧率，如果为true表示输出帧率一定会等于fps，否则fps只作为上限，如果原始帧率小于fps，那么以原始帧率为准
     * @param vcodecOpts    视频编码器选项，示例：crf=28,xxx=xxx或crf=28，用逗号分隔选项，=号前为key，后为value。
     *                      crf选项用于设置视频输出的质量，取值范围为0~51，其中0为无损模式，数值越大，画质越差，生成的文件却越小。
     *                      从主观上讲，18~28是一个合理的范围。18被认为是视觉无损的（从技术角度上看当然还是有损的），它的输出视频质量和输入视频相当。
     * @param size          输出的视频大小。该数值表示输出视频最短边的大小，如原视频是720*1280，如果size为640，那么输出的视频尺寸为640*1136。
     *                      如果原始大小比size要小的话，输出的尺寸保持原始大小
     * @param bitRate       视频编码比特率，如果为0或指定了crf选项，则由接口自行决定比特率。
     *                      请注意该参数必须是2的倍数，否则函数内部会自动调整输出宽高为2的倍数
     * @param hwdecode      是否使用硬件解码，大于720p的视频用硬解会快一点，2k以上视频用硬解会明显快很多，4k以上视频用硬解部分机器可能无法解码。
     *                      软解因为没有与系统和硬件相关，因此稳定性最佳。硬解暂时发现处理大分辨率视频，或用多线程同时处理多个较大视频时部分机型会无法解码。
     * @param vcodecId      指定输出视频编码格式(见AVCodecID)，如果为零表示使用原始视频编码格式
     * @param listener      进度监听器
     * @return              >=0表示成功
     */
    public static native int AVResize(String input, String out, int fps, boolean fixFps, String vcodecOpts, int size, int bitRate, boolean hwdecode, int vcodecId,  AVListener listener);

    /**
     * 视频重新封装，主要用于调整mp4 moov box位置
     * @param input         原视频路径
     * @param out           视频保存路径，后缀名需要与输入保持一致
     * @return              >=0表示成功
     */
    public static native int AVRemuxer(String input, String out);

    /**
     * 提取视频文件中的音频
     * @param input   视频路径
     * @param out     音频保存路径,支持wav,aac格式
     * @return        >=0表示成功
     */
    public static native int AVAudioExtract(String input, String out);

    /**
     * 音量调节和前后声音渐变
     * @param input        输入文件，可以是音频或视频
     * @param out          保存路径。如果是视频后缀名需要与输入保持一致。如果是音频可以不同格式，支持wav,aac
     * @param volume       音量，1为原始音量，否则为提高或降低音量
     * @param fadeIn       否需要渐入（视频开始时声音从低到高变化）
     * @param timeFadeIn   渐入时长
     * @param fadeOut      否需要渐出（视频结束时声音从高到低变化）
     * @param timeFadeOut  渐出时长
     * @return             >=0表示成功
     */
    public static native int AVAudioVolume(String input, String out, float volume, boolean fadeIn, int timeFadeIn, boolean fadeOut, int timeFadeOut);

    /**
     * 替换视频中的声音
     * @param input        视频路径
     * @param out          视频保存路径，后缀名需要与输入保持一致
     * @param audio        用来替换的音频文件,如果为null或""则表示清除视频声音，可以是mp4，mp3,aac,wav等格式
     * @param repeat       当视频时长大于音频时长的时候，是否重复音频
     * @param fadeIn       否需要渐入（视频开始时声音从低到高变化）
     * @param timeFadeIn   渐入时长
     * @param fadeOut      否需要渐出（视频结束时声音从高到低变化）
     * @param timeFadeOut  渐出时长
     * @return             >=0表示成功
     */
    public static native int AVAudioReplace(String input, String out, String audio, boolean repeat, boolean fadeIn, int timeFadeIn, boolean fadeOut, int timeFadeOut);

    /**
     * 两个声音混音，输入可以是音频也可以是视频
     * @param input        输入文件，可以是音频也可以是视频
     * @param audio        用来混音的音频文件，可以是mp4，mp3,aac,wav等格式
     * @param out          视频保存路径
     * @param volumeInput  input的音量调整，1为原始音量，否则为提高或降低音量
     * @param volumeAudio  audio的音量调整，1为原始音量，否则为提高或降低音量
     * @param repeat       当视频时长大于音频时长的时候，是否重复音频
     * @param justAudioFade 是否只是对添加的音效做渐变处理
     * @param fadeIn       否需要渐入（视频开始时声音从低到高变化）
     * @param timeFadeIn   渐入时长
     * @param fadeOut      否需要渐出（视频结束时声音从高到低变化）
     * @param timeFadeOut  渐出时长
     * @return             >=0表示成功
     */
    public static native int AVAudioMix(String input, String audio, String out, float volumeInput, float volumeAudio, boolean repeat, boolean justAudioFade, boolean fadeIn, int timeFadeIn, boolean fadeOut, int timeFadeOut);

    /**
     * 多个音频文件混音
     * @param audios       输入文件，至少是2个或2个以上，可以是mp4，mp3,aac,wav等格式
     * @param dontFadeIndexs 指定不需要做渐变的audios下标，可以指定一个或者多个。null表示全部都做渐变
     * @param out          保存路径，支持wav,aac
     * @param fadeIn       否需要渐入（视频开始时声音从低到高变化）
     * @param timeFadeIn   渐入时长
     * @param fadeOut      否需要渐出（视频结束时声音从高到低变化）
     * @param timeFadeOut  渐出时长
     * @return             >=0表示成功
     */
    public static native int AVAudioMultiMix(String[] audios, int[] dontFadeIndexs, String out, boolean fadeIn, int timeFadeIn, boolean fadeOut, int timeFadeOut);

    /**
     * 音频格式转换
     * @param input         输入文件，可以是mp4，mp3,aac,wav等格式
     * @param out           保存路径，支持wav,aac格式
     * @param sampleRate    音频采样率,除wav外，其它输出格式仅支持48000以下采样率，具体数值请查阅资料，常用采样率为48000,44100
     * @return              >=0表示成功
     */
    public static native int AVAudioConvert(String input, String out, int sampleRate);

    /**
     * 音频延长
     * @param input         输入音频文件
     * @param out           保存路径，后缀名需要与输入保持一致
     * @param srcClipStart  原音频文件的裁剪起始点，不裁剪传0
     * @param srcClipEnd    原音频文件的裁剪结束点，不裁剪传-1
     * @param newDuration   指定输出时长
     * @return              >=0表示成功
     */
    public static native int AVAudioRepeat(String input, String out, int srcClipStart, int srcClipEnd, int newDuration);

    /**
     * 创建空白音频
     * @param out           保存路径，支持wav,aac格式
     * @param sampleRate    采样率，除wav外，其它输出格式仅支持48000以下采样率，具体数值请查阅资料，常用采样率为48000,44100
     * @param duration      时长
     * @return              >=0表示成功
     */
    public static native int AVAudioCreateBlankAudio(String out, int sampleRate, int duration);

    /**
     * 生成音频波形
     * @param input         输入文件，可以是mp4，mp3,aac,wav等格式
     * @param sampleRate    每秒钟的音频波形个数
     * @param info          输出，视频相关信息
     * @param progress      进度回调，回调实时更新波形的buffer，如需实时绘制可以在回调种处理。不需要可以传null
     * @return              波形数据，数值范围是-127~127。
     */
    public static native byte[] AVAudioWave(String input, float sampleRate, AVInfo info, AVWaveProgress progress);

    /**
     * 清除指定时间片段的声音并对视频片段加减速处理
     * @param input         输入文件，可以是mp4，mp3,aac,wav等格式
     * @param out           保存路径，后缀名需要与输入保持一致
     * @param clearPts      时间段数组，格式为[开始 结束 加速因子 开始 结束 加速因子]，时间单位为毫秒，加速因子：大于1是减速，小于1是加速
     * @return              >=0表示成功
     */
    public static native int AVAudioClear(String input, String out, float[] clearPts);

    /**
     * 视频转GIF
     * @param input         输入文件，可以是任意视频格式
     * @param size          输出GIF最大边尺寸
     * @param fps           输出帧率,0表示不改变帧率
     * @param highQuality   是否高质量输出，true：质量高，耗时多，false：质量低，耗时少
     * @param progress      进度回调
     * @return              >=0表示成功
     */
    public static native int AVVideoToGif(String input, String out, int size, int fps, boolean highQuality, AVListener progress);

    /**
     * 图片处理
     * @param bitmap 要处理的图片
     * @param filterOpts filter的描述
     */
    public static native Bitmap AVImageProcess(Bitmap bitmap, String filterOpts);

    /**
     * 声音处理
     * @param inputs 输入文件
     * @param out 输出文件
     * @param filterOpts filter的描述，输入源前缀为a，后缀为1~n，例如a1,a2。示例如下：
     *                   [a1]atrim=4:34,afade=t=in:st=4:duration=4,afade=t=out:st=30:duration=4,volume=0.5[v1];[a2]volume=0.8[v2];[v1][v2]amix=inputs=2:duration=longest
     *                   以上命令先对a1做了一次裁剪，接着做了淡入和淡出、音量调整，然后输出为v1。对a2做了音量调整，输出为v2。最后对v1,v2做了混音。
     * @param progress 进度回调
     */
    public static native int AVAudioProcess(String[] inputs, String out, String filterOpts, AVListener progress);

    /**
     * 创建解码器，解码器相关接口已由AVVideoDecoder类封装，建议使用AVVideoDecoder类，不要直接调用这些函数
     * @param input        输入文件，可以是音频也可以是视频
     * @param pixFormat    指定解码输出格式，值为AVPixelFormat.*，如需输出Bitmap，应指定为AV_PIX_FMT_RGBA或AV_PIX_FMT_RGB565
     * @param hardware     是否使用硬件解码，大于720p的视频用硬解会快一点，2k以上视频用硬解会明显快很多，4k以上视频用硬解部分机器可能无法解码。
     * @return             解码器id,后续操作需传入对应的解码器id进行操作
     */
    protected static native int AVDecoderCreate(String input, int pixFormat, boolean hardware);

    /**
     * Seek到指定时间，可重复调用
     * @param id            解码器id
     * @param time          时间戳
     * @param seekKeyFrame  是否以关键帧为起点，如果为true的话，下一次调用AVDecoderNextFrame返回的帧一定是关键帧，但帧时间戳可能比time小很多。为false的话，能确保返回的帧时间戳与time极其接近，但下一次调用AVDecoderNextFrame时所花的时间会比较长
     * @return              >=0表示成功
     */
    protected static native int AVDecoderSeek(int id, int time, boolean seekKeyFrame);

    /**
     * Seek到指定帧
     * @param id            解码器id
     * @param index         第几帧
     * @return              >=0表示成功
     */
    protected static native int AVDecoderSeekByFrameIndex(int id, int index);

    /**
     * 设置裁剪值，相对原始视频尺寸
     * @param id            解码器id
     * @param left          左边裁剪多少个像素
     * @param top           顶部裁剪多少个像素
     * @param right         右边裁剪多少个像素
     * @param bottom        底部裁剪多少个像素
     */
    protected static native void AVDecoderSetCrop(int id, int left, int top, int right, int bottom);

    /**
     * 设置输出帧的旋转值
     * @param id            解码器id
     * @param rotation      旋转角度，90/180/270
     */
    protected static native void AVDecoderSetRotation(int id, int rotation);

    /**
     * 设置输出图像大小
     * @param id            解码器id
     * @param width         宽
     * @param height        高
     */
    protected static native void AVDecoderSetSize(int id, int width, int height);

    /**
     * 设置输出图像大小
     * @param id            解码器id
     * @param size          最短边的像素，0表示使用原始大小
     */
    protected static native void AVDecoderSetSize2(int id, int size);

    /**
     * 读取下一帧
     * @param id            解码器id
     * @param dataType      指定输出数据类型，值为AVNative.DATA_FMT_*， 支持byte[],int[],Bitmap
     * @param buffer        传入上一次调用AVDecoderNextFrame时的返回值
     * @param info          用户接收解码的帧信息
     * @return              返回帧数据，具体类型由dataType确定
     */
    protected static native Object AVDecoderNextFrame(int id, int dataType, Object buffer, AVFrameInfo info);

    /**
     * 释放解码器
     * @param id            解码器id
     */
    protected static native void AVDecoderRelease(int id);

    /**
     * 创建GIF编码器
     * @param output        保存的文件路径
     * @param width         原始图像的宽
     * @param height        原始图像的高
     * @param globalPalette 是否使用全局调色板
     *                      如果为true，步骤为AVGifCreate->AVGifWritePaletteFrame(frame:0~n)->AVGifGeneratePalette->AVGifWriteFrame(frame:0~n)->AVGifClose
     *                      如果为false，步骤为AVGifCreate->AVGifWriteFrame(frame:0~n)->AVGifClose
     * @return              >=0表示成功
     */
    protected static native int AVGifCreate(String output, int width, int height, boolean globalPalette);

    /**
     * 写入一帧图像，用于生成全局调色板
     * @param id        编码器id
     * @param pixels    RGBA像素数据
     * @param width     对应pixels的宽度
     * @param height    对应pixels的高度
     * @return          >=0表示成功
     */
    protected static native int AVGifWritePaletteFrame(int id, int[] pixels, int width, int height);

    /**
     * 生成最终的全局调色板，此函数必须在所有图像帧都调用过AVGifWritePaletteFrame之后调用
     * @param id        编码器id
     * @return          >=0表示成功
     */
    protected static native int AVGifGeneratePalette(int id);

    /**
     * 写入一帧数据
     * @param id        编码器id
     * @param pixels    RGBA像素数据
     * @param width     对应pixels的宽度
     * @param height    对应pixels的高度
     * @param duration  该帧的显示时长
     * @return          是否成功
     */
    protected static native int AVGifWriteFrame(int id, int[] pixels, int width, int height, int duration);

    /**
     * 结束GIF编码，必须调用此函数才能生成最终文件
     * @param id        编码器id
     */
    protected static native void AVGifClose(int id);

    /**
     * 释放GIF编码器
     * @param id        编码器id
     */
    protected static native void AVGifRelease(int id);

    /**
     * 是否mp4的moov在mdat后面
     * @param input       mp4文件
     * @return            是否moov在mdat后面
     */
    public static native boolean IsMoovOnBack(String input);

    /**
     * 像素格式转换器生成
     * @param dstWidth       目标图像数据宽度
     * @param dstHeight      目标图像数据高度
     * @param dstPixelFormat 目标像素格式
     * @return               转换器id,后续操作需传入对应的转换器id进行操作
     */
    public static native int PixelConverterCreate(int dstWidth, int dstHeight, int dstPixelFormat);

    /**
     * 设置裁剪参数
     * @param id             像素格式转换器id
     * @param cropLeft       左边裁剪多少像素
     * @param cropRight      右边裁剪多少像素
     * @param cropTop        顶部裁剪多少像素
     * @param cropBottom     底部裁剪多少像素
     */
    public static native void PixelConverterSetCrop(int id, int cropLeft, int cropRight, int cropTop, int cropBottom);
    /**
     * 像素格式转换
     * @param id             像素格式转换器id
     * @param data           输入数据
     * @param dataType       指定输出数据类型，值为AVNative.DATA_FMT_*， 支持byte[],int[],Bitmap
     * @param buffer         传入上一次调用PixelConvert时的返回值
     * @param srcWidth       源图像数据宽度
     * @param srcHeight      源图像数据高度
     * @param srcPixelFormat 源图像数据格式
     * @return               具体类型由dataType确定
     */
    public static native Object PixelConverterConv(int id, ByteBuffer data, int dataType, Object buffer, int srcWidth, int srcHeight, int srcPixelFormat);

    /**
     * 释放像素格式转换器
     * @param id            解码器id
     */
    protected static native void PixelConverterRelease(int id);

    //------------------audio------------------
    /**
     * 创建解码器，解码器相关接口已由AVAudioDecoder类封装，建议使用AVAudioDecoder类，不要直接调用这些函数
     * @param input        输入音频文件
     * @return             解码器id,后续操作需传入对应的解码器id进行操作
     */
    public static native int AVAudioDecoderCreate(String input);

    /**
     * Seek到指定时间
     * @param id            解码器id
     * @param time          时间戳
     * @return              >=0表示成功
     */
    public static native int AVAudioDecoderSeek(int id,int time);

    /**
     * 读取下一帧
     * @param id            解码器id
     * @param dataType      指定输出数据类型，值为AVNative.DATA_FMT_*， 支持byte[]..
     * @param buffer        传入上一次调用AVDecoderNextFrame时的返回值
     * @param info          用户接收解码的帧信息
     * @return              返回帧数据，具体类型由dataType确定
     */
    public static native Object AVAudioDecoderNextFrame(int id, int dataType, Object buffer, AVFrameInfo info);

    /**
     * 释放解码器
     * @param id            解码器id
     */
    public static native void AVAudioDecoderRelease(int id);
}
