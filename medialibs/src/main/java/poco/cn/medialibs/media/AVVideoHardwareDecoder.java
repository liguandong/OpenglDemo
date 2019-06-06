package poco.cn.medialibs.media;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AVVideoHardwareDecoder extends AVVideoDecoder{
    private static final int TIMEOUT_USEC = 10000;

    private MediaCodec mDecoder;
    private MediaExtractor mExtractor;
    private MediaFormat mFormat;
    private int mWidth;
    private int mHeight;
    private boolean mInputDone;
    private boolean mOutputDone;
    private boolean mStarted;
    private int mColorFormat;
    private int mDataType;
    private long mStartTime;
    private int mPixelFormat;
    private String mFile;
    private int mCropLeft = 0;
    private int mCropTop = 0;
    private int mCropRight = 0;
    private int mCropBottom = 0;
    private PixelConverter mPixConv;
    private Object mSync = new Object();

    /**
     * 创建解码器
     * @param file         输入文件，可以是音频也可以是视频
     * @param dataType     指定输出数据类型，值为AVNative.DATA_FMT_*， 支持byte[],int[],Bitmap
     * @param pixFormat    指定解码输出格式，值为AVPixelFormat.*，如需输出Bitmap，应指定为AV_PIX_FMT_RGBA或AV_PIX_FMT_RGB565
     * @return             是否成功
     */
    public boolean create(String file, int dataType, int pixFormat)
    {
        synchronized(mSync)
        {
            mFile = file;
            mDataType = dataType;
            mPixelFormat = pixFormat;
            try {
                mExtractor = new MediaExtractor();
                mExtractor.setDataSource(file);
                int trackIndex = selectVideoTrack(mExtractor);
                if (trackIndex < 0) {
                    throw new RuntimeException("No video track found in " + file);
                }
                mExtractor.selectTrack(trackIndex);
                mFormat = mExtractor.getTrackFormat(trackIndex);

                mWidth = mFormat.getInteger(MediaFormat.KEY_WIDTH);
                mHeight = mFormat.getInteger(MediaFormat.KEY_HEIGHT);

                mPixConv = new PixelConverter();

//                MediaFormat format = new MediaFormat();
//                format.setInteger(MediaFormat.KEY_WIDTH, mWidth);
//                format.setInteger(MediaFormat.KEY_HEIGHT, mHeight);
                //format.setInteger(MediaFormat.KEY_FRAME_RATE, mFormat.getInteger(MediaFormat.KEY_FRAME_RATE));
                //format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, mFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE));

//                ByteBuffer b0 = mFormat.getByteBuffer("csd-0");
//                ByteBuffer b1 = mFormat.getByteBuffer("csd-1");
//                ByteBuffer csd0 = ByteBuffer.allocate(b0.limit());
//                ByteBuffer csd1 = ByteBuffer.allocate(b1.limit());
//
//                byte[] bytes = new byte[b0.limit()];
//                b0.get(bytes);
//                csd0.put(bytes);
//
//                bytes = new byte[b1.limit()];
//                b1.get(bytes);
//                csd1.put(bytes);
//
//                csd0.rewind();
//                csd1.rewind();
//                //b0.rewind();
//                //b1.rewind();
//
//                format.setByteBuffer("csd-0", csd0);
//                format.setByteBuffer("csd-1", csd1);
//                format.setString(MediaFormat.KEY_MIME, mFormat.getString(MediaFormat.KEY_MIME));
                //format.setLong(MediaFormat.KEY_DURATION, mFormat.getLong(MediaFormat.KEY_DURATION));
                try {
                    String mime = mFormat.getString(MediaFormat.KEY_MIME);
                    mDecoder = MediaCodec.createDecoderByType(mime);
                    mDecoder.configure(mFormat, null, null, 0);
                    mDecoder.start();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * seek到指定时间，可重复调用
     * @param time          时间戳
     * @param seekKeyFrame  是否以关键帧为起点，如果为true的话，下一次调用nextFrame返回的帧一定是关键帧，但帧时间戳可能比time小很多。为false的话，能确保返回的帧时间戳与time极其接近，但下一次调用nextFrame时所花的时间会比较长
     * @return              是否成功
     */
    public boolean seek(int time, boolean seekKeyFrame)
    {
        synchronized(mSync) {
            if(mOutputDone && mInputDone) {
                mOutputDone = false;
                mInputDone = false;
                if(Build.VERSION.SDK_INT < 21) {
                    if(mStarted) {
                        mDecoder.stop();
                        mStarted = false;
                    }
                    if(mDecoder != null) {
                        mDecoder.release();
                        mDecoder = null;
                    }
                    try {
                        String mime = mFormat.getString(MediaFormat.KEY_MIME);
                        mDecoder = MediaCodec.createDecoderByType(mime);
                        mDecoder.configure(mFormat, null, null, 0);
                        mDecoder.start();
                    } catch(IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                } else {
                    mDecoder.reset();
                    mDecoder.configure(mFormat, null, null, 0);
                    mDecoder.start();
                }
            }
            mStartTime = (long) time * 1000l;
            mExtractor.seekTo(mStartTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            if(seekKeyFrame == true) {
                mStartTime = 0;
            }
        }
        return true;
    }

    @Override
    public boolean seekByFrameIndex(int index) {
        return false;
    }

    @Override
    public void setSize(int size) {

    }

    @Override
    public void setCrop(int left, int top, int right, int bottom) {

    }

    public void setSize(int width, int height)
    {
    }

    /**
     * 决定是否每次调用nextFrame都返回同一个对象，只改变对象的内容。
     * @param reuse          true，调用nextFrame每次都返回同一个对象，只是改变了对象的内容。false，调用nextFrame每次都返回一个新的对象
     */
    public void setDataReusable(boolean reuse)
    {

    }

    /**
     * 读取下一帧
     * @param info          输出，调用前不需要赋值，用于接收解码的帧信息
     * @return              返回帧数据，具体类型由dataType确定，为了提高效率，接口会重用Bitmap，建议不要对返回的Bitmap进行recycle操作
     */
    public Object nextFrame(AVFrameInfo info)
    {
        synchronized(mSync) {
            if(mDecoder == null){
                return null;
            }
            ByteBuffer[] decoderInputBuffers = mDecoder.getInputBuffers();
            ByteBuffer[] decoderOutputBuffers = mDecoder.getOutputBuffers();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            mStarted = true;

            while(!mOutputDone) {

                if(!mInputDone) {
                    int inputBufIndex = mDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if(inputBufIndex >= 0) {
                        ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                        int chunkSize = mExtractor.readSampleData(inputBuf, 0);
                        if(chunkSize < 0) {
                            mDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            mInputDone = true;
                        } else {
                            long presentationTimeUs = mExtractor.getSampleTime();
                            mDecoder.queueInputBuffer(inputBufIndex, 0, chunkSize, presentationTimeUs, 0);
                            mExtractor.advance();
                        }
                    }
                }

                if(!mOutputDone) {
                    int decoderStatus = mDecoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                    if(decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    } else if(decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        decoderOutputBuffers = mDecoder.getOutputBuffers();
                    } else if(decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mFormat = mDecoder.getOutputFormat();
                        mWidth = mFormat.getInteger(MediaFormat.KEY_WIDTH);
                        mHeight = mFormat.getInteger(MediaFormat.KEY_HEIGHT);
                        int strideWidth = mWidth;
                        int sliceHeight = mHeight;
                        if(mFormat.containsKey(MediaFormat.KEY_STRIDE))
                        {
                            strideWidth = mFormat.getInteger(MediaFormat.KEY_STRIDE);
                        }
                        if(mFormat.containsKey(MediaFormat.KEY_SLICE_HEIGHT))
                        {
                            sliceHeight = mFormat.getInteger(MediaFormat.KEY_SLICE_HEIGHT);
                        }
                        if(strideWidth > 0 && sliceHeight > 0) {
                            mCropRight = strideWidth - mWidth;
                            mCropBottom = sliceHeight - mHeight;
                            mWidth = strideWidth;
                            mHeight = sliceHeight;
                        }
                        if(mFormat.containsKey("crop-left") && mFormat.containsKey("crop-right"))
                        {
                            mCropLeft = mFormat.getInteger("crop-left");
                            mCropRight = mWidth - mFormat.getInteger("crop-right") - 1;
                        }
                        if(mFormat.containsKey("crop-top") && mFormat.containsKey("crop-bottom"))
                        {
                            mCropTop = mFormat.getInteger("crop-top");
                            mCropBottom = mHeight - mFormat.getInteger("crop-bottom") - 1;
                        }
                        if(mPixConv.create(mWidth-(mCropRight+mCropLeft), mHeight-(mCropBottom+mCropTop), mPixelFormat))
                        {
                            mPixConv.setSrcCrop(mCropLeft, mCropRight, mCropTop, mCropBottom);
                        }
                        mColorFormat = mFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                    } else if(decoderStatus < 0) {

                    } else { // decoderStatus >= 0
                        if(bufferInfo.presentationTimeUs >= mStartTime) {
                            ByteBuffer buffer = decoderOutputBuffers[decoderStatus];

                            int pixelFormat = 0;
                            if(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar == mColorFormat) {
                                pixelFormat = AVPixelFormat.AV_PIX_FMT_NV12;
                            } else if(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar == mColorFormat) {
                                pixelFormat = AVPixelFormat.AV_PIX_FMT_YUV420P;
                            }
                            Object obj = mPixConv.conv(buffer, mDataType, mWidth, mHeight, pixelFormat);
                            info.width = mWidth;
                            info.height = mHeight;
                            info.pts = (int) (bufferInfo.presentationTimeUs / 1000);
                            mDecoder.releaseOutputBuffer(decoderStatus, false);
                            if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                mOutputDone = true;
                            }
                            return obj;
                        }
                        else {
                            if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                mOutputDone = true;
                            }
                        }

                        mDecoder.releaseOutputBuffer(decoderStatus, false);
                    }
                }
            }
            mStarted = !mOutputDone;
        }
        return null;
    }

    /**
     * 释放解码器
     */
    public void release()
    {
        synchronized(mSync) {
            if(mStarted) {
                mDecoder.stop();
                mStarted = false;
            }

            if(mDecoder != null) {
                mDecoder.release();
                mDecoder = null;
            }

            if(mPixConv != null) {
                mPixConv.release();
                mPixConv = null;
            }

            if(mExtractor != null) {
                mExtractor.release();
                mExtractor = null;
            }
        }
    }

    private int selectVideoTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                return i;
            }
        }

        return -1;
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }
}
