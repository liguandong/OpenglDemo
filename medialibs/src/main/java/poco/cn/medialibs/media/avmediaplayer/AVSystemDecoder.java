package poco.cn.medialibs.media.avmediaplayer;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

import poco.cn.medialibs.media.AVError;
import poco.cn.medialibs.media.AVFrameInfo;
import poco.cn.medialibs.media.AVInfo;
import poco.cn.medialibs.media.AVNative;
import poco.cn.medialibs.media.AVPixelFormat;
import poco.cn.medialibs.media.AVSampleFormat;
import poco.cn.medialibs.media.PixelConverter;

public class AVSystemDecoder extends AVMediaDecoder
{
    private static final int TIMEOUT_USEC = 8000;

    private MediaExtractor mAExtractor;
    private MediaExtractor mVExtractor;
    private AVFFDecoder mADecoder;
    private MediaCodec mVDecoder;
    private int mVIndex = -1;
    private int mAIndex = -1;
    private ByteBuffer mSampleBuffer = null;
    private ByteBuffer[] mVDecoderOutputBuffers;
    private int mColorFormat;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private Thread mConvThread;
    private long mEnd = -1;
    private long mStart = -1;
    private boolean mSeek;
    private AVInfo mInfo = new AVInfo();
    private int mStride;
    private int mSliceHeight;
    private int mPixelFormat = AVPixelFormat.AV_PIX_FMT_YUV420P;
    private PixelConverter mPixConv;
    private boolean mOutput;
    private boolean mAsync;
    private Surface mSurface;
    private AVFrameInfo mConvFrame = new AVFrameInfo();
    private AVFrameInfo mConvOutputFrame;
    private int mDataType = AVNative.DATA_FMT_ARRAY_BYTE;
    private boolean mVOutputDone;
    private boolean mAOutputDone;
    private Object mUpdateSurfaceSync = new Object();
    private MediaFormat mAFormat;
    private MediaFormat mVFormat;
    private int mOutputSize;
    private int mOutputWidth;
    private int mOutputHeight;
    private boolean mSurfaceConfigured;
    private AssetManager mAssetManager;
    private boolean mRotate;


    @Override
    public boolean create() {
        release();
        mPixConv = new PixelConverter();
        mVExtractor = new MediaExtractor();
        mAExtractor = new MediaExtractor();
        mSampleBuffer = ByteBuffer.allocateDirect(1024*1024);
        return true;
    }

    @Override
    public boolean open(String file) {
        try
        {
            mRotate = false;
            synchronized(this) {
                long n = System.currentTimeMillis();
                setDataSource(mVExtractor, file);
                int numTracks = mVExtractor.getTrackCount();
                for(int i = 0; i < numTracks; i++) {
                    MediaFormat format = mVExtractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if(mime.startsWith("video/")) {
                        mVExtractor.selectTrack(i);
                        mVIndex = i;
                    }
                    if(mime.startsWith("audio/")) {
                        mAIndex = i;
                    }
                }
                if(mAIndex != -1) {
                    setDataSource(mAExtractor, file);
                    numTracks = mAExtractor.getTrackCount();
                    for(int i = 0; i < numTracks; i++) {
                        MediaFormat format = mAExtractor.getTrackFormat(i);
                        String mime = format.getString(MediaFormat.KEY_MIME);
                        if(mime.startsWith("audio/")) {
                            mAExtractor.selectTrack(i);
                            mAIndex = i;
                        }
                    }
                }
                Log.d("hwq", "init "+(System.currentTimeMillis()-n));
                if(mAIndex != -1) {
                    mAFormat = mAExtractor.getTrackFormat(mAIndex);
                    mInfo.audioSampleRate = mAFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    mInfo.audioChannels = mAFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    mInfo.audioDuration = (int) mAFormat.getLong(MediaFormat.KEY_DURATION) / 1000;
                    byte[] csd0 = null;
                    if(mAFormat.containsKey("csd-0")) {
                        ByteBuffer b = mAFormat.getByteBuffer("csd-0");
                        b.position(0);
                        csd0 = new byte[b.limit()];
                        b.get(csd0);
                        b.position(0);
                    }
                    mADecoder = new AVFFDecoder();
                    if(mADecoder.create(mAFormat, csd0, null, AVSampleFormat.AV_SAMPLE_FMT_S16) == false)
                    {
                        mADecoder.release();
                        mADecoder = null;
                        mAIndex = -1;
                        mInfo.audioDuration = 0;
                    }
                }
                if(mVIndex != -1) {
                    try {
                        mVFormat = mVExtractor.getTrackFormat(mVIndex);
                        mInfo.width = mVFormat.getInteger(MediaFormat.KEY_WIDTH);
                        mInfo.height = mVFormat.getInteger(MediaFormat.KEY_HEIGHT);
                        mInfo.videoDuration = (int) mVFormat.getLong(MediaFormat.KEY_DURATION) / 1000;
                        if(mVFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
                            mInfo.videoBitRate = mVFormat.getInteger(MediaFormat.KEY_BIT_RATE);
                        }
                        if(mVFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                            mInfo.videoRotation = mVFormat.getInteger(MediaFormat.KEY_ROTATION);
                        } else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                        {
                            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                            try {
                                mediaMetadataRetriever.setDataSource(file);
                                String rotation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                                if(rotation != null) {
                                    mInfo.videoRotation = Integer.parseInt(rotation);
                                    if(mInfo.videoRotation != 0)
                                    {
                                        mRotate = true;
                                    }
                                }
                            }
                            catch(Exception e)
                            {
                            }
                            mediaMetadataRetriever.release();
                        }
                        if(mOutput == true || (mOutput == false && mSurface != null)) {
                            updateSurface();
                        }
                    } catch(Exception e) {
                    }
                }
                if(mVIndex != -1 || mADecoder != null) {
                    mInfo.duration = mInfo.videoDuration;
                    if(mInfo.audioDuration > mInfo.duration) {
                        mInfo.duration = mInfo.audioDuration;
                    }
                    return true;
                } else {
                    release();
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    private void setDataSource(MediaExtractor extractor, String file) throws Exception
    {
        if(file.startsWith("file:///android_asset")) {
            if(mAssetManager != null) {
                String source = file.substring(file.indexOf('/', 9)+1);
                AssetFileDescriptor fd = mAssetManager.openFd(source);
                extractor.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            }
            else
            {
                throw new RuntimeException("使用了assets目录，但未调用setAssetManager");
            }
        }
        else {
            extractor.setDataSource(file);
        }
    }

    @Override
    public boolean needRotate() {
        return mRotate;
    }

    @Override
    public boolean seek(long time, boolean seekKeyFrame) {
        if(mVExtractor == null && mAExtractor == null)
            return false;

        //Log.d("hwq", "decoder seek in "+time);
        synchronized(this) {
            if(time < 0) {
                time = 0;
            }
            mStart = 0;
            if(!seekKeyFrame) {
                mStart = time;
            }
            mSeek = true;
            mVOutputDone = false;
            mAOutputDone = false;
            if(mVDecoder != null) {
                mVDecoder.flush();
            }
            if(mADecoder != null) {
                mADecoder.flush();
            }
            if(mVExtractor != null) {
                synchronized(mVExtractor) {
                    mVExtractor.seekTo(time * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                }
            }
            if(seekKeyFrame && mVIndex != -1 && mAIndex != -1) {
                int index = mVExtractor.getSampleTrackIndex();
                if(index != -1) {
                    mAExtractor.seekTo(mVExtractor.getSampleTime(), MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                }
                else {
                    mAExtractor.seekTo(time * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                }
            }
            else
            {
                if(mAExtractor != null) {
                    synchronized(mAExtractor) {
                        mAExtractor.seekTo(time * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    }
                }
            }
            //Log.d("hwq", "decoder seek out");
        }
        return true;
    }

    @Override
    public synchronized boolean nextVideoFrame(Object buffer, AVFrameInfo info) {
        if(mVDecoder == null && mVIndex != -1) {
            if(mOutput == false && (mSurface == null || mVDecoder == null))
            {
                try {
                    for(int i = 0; i < 100; i++) {
                        Thread.sleep(10);
                        if(mSurface != null && mVDecoder != null) {
                            synchronized(mVDecoder) {
                            }
                            break;
                        }
                    }
                } catch(Exception e) {
                }
            }
        }
        if(mVIndex == -1 || mVDecoder == null || (mEnd != -1 && mStart > mEnd))
        {
            return false;
        }
        synchronized(mVDecoder) {
            if(mStart > 0 && mSeek == false) {
                mSeek = true;
                mVExtractor.seekTo(mStart * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                if(mAExtractor != null) {
                    int index = mVExtractor.getSampleTrackIndex();
                    if(index != -1) {
                        mAExtractor.seekTo(mVExtractor.getSampleTime(), MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    }
                }
            }
        }
        if(mVDecoder == null || mVExtractor == null)
        {
            return false;
        }
        synchronized(mVDecoder) {
            while(!mVOutputDone) {
                long n = System.currentTimeMillis();
                ByteBuffer[] buffers = mVDecoder.getInputBuffers();
                int index = mVDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                if(index >= 0) {

                    ByteBuffer buf = buffers[index];
                    int size = mVExtractor.readSampleData(buf, 0);
                    if(size < 0) {
                        mVDecoder.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        long presentationTimeUs = mVExtractor.getSampleTime();
                        mVDecoder.queueInputBuffer(index, 0, size, presentationTimeUs, 0);
                        mVExtractor.advance();
                    }
                }

                if(!mVOutputDone) {
                    int decoderStatus = mVDecoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                    if(decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    } else if(decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        mVDecoderOutputBuffers = mVDecoder.getOutputBuffers();
                    } else if(decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat format = mVDecoder.getOutputFormat();
                        mInfo.width = format.getInteger(MediaFormat.KEY_WIDTH);
                        mInfo.height = format.getInteger(MediaFormat.KEY_HEIGHT);
                        int strideWidth = mInfo.width;
                        int sliceHeight = mInfo.height;
                        if(format.containsKey(MediaFormat.KEY_STRIDE)) {
                            strideWidth = format.getInteger(MediaFormat.KEY_STRIDE);
                        }
                        if(format.containsKey(MediaFormat.KEY_SLICE_HEIGHT)) {
                            sliceHeight = format.getInteger(MediaFormat.KEY_SLICE_HEIGHT);
                        }
                        int cropRight = 0;
                        int cropLeft = 0;
                        int cropTop = 0;
                        int cropBottom = 0;
                        mStride = mInfo.width;
                        mSliceHeight = mInfo.height;
                        if(strideWidth > 0 && sliceHeight > 0) {
                            cropRight = strideWidth - mInfo.width;
                            cropBottom = sliceHeight - mInfo.height;
                            mStride = strideWidth;
                            mSliceHeight = sliceHeight;
                        }
                        if(format.containsKey("crop-left") && format.containsKey("crop-right")) {
                            cropLeft = format.getInteger("crop-left");
                            cropRight = mStride - format.getInteger("crop-right") - 1;
                        }
                        if(format.containsKey("crop-top") && format.containsKey("crop-bottom")) {
                            cropTop = format.getInteger("crop-top");
                            cropBottom = mSliceHeight - format.getInteger("crop-bottom") - 1;
                        }
                        mInfo.width = mStride - (cropRight + cropLeft);
                        mInfo.height = mSliceHeight - (cropBottom + cropTop);

                        if(mOutputSize > 1) {
                            int min = mInfo.width < mInfo.height ? mInfo.width : mInfo.height;
                            if(mOutputSize < min) {
                                if(mInfo.width < mInfo.height) {
                                    mOutputWidth = mOutputSize;
                                    mOutputHeight = mOutputSize * mInfo.height / mInfo.width;
                                } else {
                                    mOutputHeight = mOutputSize;
                                    mOutputWidth = mOutputSize * mInfo.width / mInfo.height;
                                }
                            }
                        }
                        if(mOutputWidth == 0 || mOutputHeight == 0) {
                            mOutputWidth = mInfo.width;
                            mOutputHeight = mInfo.height;
                        }
                        if(mPixConv.create(mOutputWidth, mOutputHeight, mPixelFormat)) {
                            mPixConv.setSrcCrop(cropLeft, cropRight, cropTop, cropBottom);
                        }
                        mColorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                    } else if(decoderStatus < 0) {
                    } else if(decoderStatus >= 0){ // decoderStatus >= 0
                        if(mBufferInfo.presentationTimeUs / 1000 >= mStart) {
                            info.pts = (int) (mBufferInfo.presentationTimeUs / 1000);
                            info.width = mInfo.width;
                            info.height = mInfo.height;
                            //Log.d("hwq", "video " + info.pts);
                            mVDecoder.releaseOutputBuffer(decoderStatus, mSurface != null && mOutput == false);
                            if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                mVOutputDone = true;
                            }
                            return true;
                        } else {
                            if((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                mVOutputDone = true;
                            }
                        }
                        mVDecoder.releaseOutputBuffer(decoderStatus, false);
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean nextAudioFrame(Object buffer, AVFrameInfo info) {
        if(mAIndex == -1 || mADecoder == null || (mEnd != -1 && mStart > mEnd))
        {
            return false;
        }
        synchronized(mADecoder) {
            if(mStart > 0 && mSeek == false) {
                mSeek = true;
                if(mVExtractor != null) {
                    mVExtractor.seekTo(mStart * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    int index = mVExtractor.getSampleTrackIndex();
                    if(index != -1) {
                        mAExtractor.seekTo(mVExtractor.getSampleTime(), MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    }
                }
                else
                {
                    mAExtractor.seekTo(mStart * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                }
            }
        }
        synchronized(mADecoder) {
            while(!mAOutputDone) {
                mSampleBuffer.position(0);
                int size = mAExtractor.readSampleData(mSampleBuffer, 0);
                if(size >= 0) {
                    if(size > 0) {
                        mSampleBuffer.position(0);
                        mSampleBuffer.limit(size);
                        mADecoder.sendPacket(mSampleBuffer, size, mAExtractor.getSampleTime(), mAExtractor.getSampleTime(), 0);
                    }
                    mAExtractor.advance();
                }
                else
                {
                    mADecoder.sendPacket(null, 0, 0, 0, 0);
                    mAOutputDone = true;
                }
                int ret = mADecoder.receiveFrame(info, AVNative.DATA_FMT_ARRAY_BYTE, null);
                if(ret >= 0)
                {
                    return true;
                }
                else if(ret == AVError.EAGAIN)
                {
                    continue;
                }
                else
                {
                    mAOutputDone = true;
                }
            }
        }
        return false;
    }

    @Override
    public void start() {
        if(mOutput) {
            startConvThread();
        }
    }

    @Override
    public void stop() {
        stopConvThread();
    }

    @Override
    public void release() {

        if(mVDecoder != null)
        {
            synchronized(mVDecoder) {
                mVDecoder.release();
            }
            mVDecoder = null;
        }
        if(mADecoder != null)
        {
            synchronized(mADecoder) {
                mADecoder.release();
            }
            mADecoder = null;
        }
        if(mAExtractor != null)
        {
            synchronized(mAExtractor) {
                mAExtractor.release();
            }
            mAExtractor = null;
        }
        if(mVExtractor != null)
        {
            synchronized(mVExtractor) {
                mVExtractor.release();
            }
            mVExtractor = null;
        }
        if(mPixConv != null) {
            mPixConv.release();
            mPixConv = null;
        }
    }

    @Override
    public AVInfo getAVInfo() {
        return mInfo;
    }

    @Override
    public AVFrameInfo getAsyncFrame() {
        return mConvOutputFrame;
    }

    @Override
    public void setSurface(Surface surface) {
        mSurface = surface;
        updateSurface();
    }

    @Override
    public void setOutput(boolean output, boolean async) {
        mOutput = output;
        mAsync = async;
        updateSurface();
    }

    @Override
    public void setOutputFormat(int pixFormat, int dataType) {
        mDataType = dataType;
        mPixelFormat = pixFormat;
    }

    @Override
    public void setAudioSpeed(float speed) {

    }

    @Override
    public void setAssetManager(AssetManager assetManager) {
        mAssetManager = assetManager;
    }

    @Override
    public void setOutputSize(int size) {
        mOutputSize = size;
    }

    @Override
    public void setOutputSize(int width, int height) {
        mOutputWidth = width;
        mOutputHeight = height;
    }

    private void updateSurface()
    {
        if(mVExtractor == null){
            return;
        }
        synchronized(mUpdateSurfaceSync) {
            if(mVDecoder == null) {
                if(mVFormat != null) {
                    try {
                        String mime = mVFormat.getString(MediaFormat.KEY_MIME);
                        mVDecoder = MediaCodec.createDecoderByType(mime);
                        synchronized(mVDecoder) {
                            if(mOutput == false) {
                                mVDecoder.configure(mVFormat, mSurface, null, 0);
                                mSurfaceConfigured = mSurface != null;
                            } else {
                                mVDecoder.configure(mVFormat, null, null, 0);
                                mSurfaceConfigured = false;
                            }
                            mVDecoder.start();
                        }
                    } catch(Exception e) {
                        if(mVDecoder != null) {
                            mVDecoder.release();
                            mVDecoder = null;
                        }
                    }
                }
                return;
            }

            if(mOutput == false) {
                if(mSurfaceConfigured == false && mSurface != null && mVDecoder != null) {
                    if(rebuildVDecoder())
                    {
                        mSurfaceConfigured = true;
                    }
                }
                else if(mSurfaceConfigured == true && mSurface == null && mVDecoder != null)
                {
                    rebuildVDecoder();
                    mSurfaceConfigured = false;
                }
            } else {
                if(mSurfaceConfigured) {
                    rebuildVDecoder();
                    mSurfaceConfigured = false;
                }
            }
        }
    }

    private boolean rebuildVDecoder()
    {
        synchronized(this) {
            synchronized(mVDecoder) {
                if(Build.VERSION.SDK_INT < 21) {
                    mVDecoder.release();
                    try {
                        String mime = mVFormat.getString(MediaFormat.KEY_MIME);
                        mVDecoder = MediaCodec.createDecoderByType(mime);
                    } catch(IOException e) {
                        e.printStackTrace();
                        if(mVDecoder != null) {
                            mVDecoder.release();
                            mVDecoder = null;
                        }
                    }
                } else {
                    mVDecoder.reset();
                }
                if(mVDecoder != null) {
                    mVDecoder.configure(mVFormat, mSurface, null, 0);
                    mVDecoder.start();
                    return true;
                }
            }
        }
        return false;
    }

    private void startConvThread()
    {
        if(mConvThread == null) {
            mConvThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    runConv();
                }
            });
            mConvThread.start();
        }
    }

    private void stopConvThread()
    {
        if(mConvThread != null)
        {
            mConvThread.interrupt();
            mConvThread = null;
        }
    }

    private void runConv()
    {
        long lastpts = -100000;
        AVFrameInfo frame = null;
        while(mConvThread != null)
        {
            frame = null;
            synchronized(mConvFrame) {
                if(mConvFrame.pts != lastpts && mConvFrame.data != null) {
                    lastpts = mConvFrame.pts;
                    frame = new AVFrameInfo();
                    frame.pts = mConvFrame.pts;
                    frame.width = mConvFrame.width;
                    frame.height = mConvFrame.height;
                    frame.data = mConvFrame.data;
                }
            }
            if(frame != null)
            {
                int pixelFormat = 0;
                if(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar == mColorFormat) {
                    pixelFormat = AVPixelFormat.AV_PIX_FMT_NV12;
                } else if(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar == mColorFormat) {
                    pixelFormat = AVPixelFormat.AV_PIX_FMT_YUV420P;
                }
                frame.data = mPixConv.conv((ByteBuffer)frame.data, mDataType, mStride, mSliceHeight, pixelFormat);
                mConvOutputFrame = frame;
            }
            else
            {
                try {
                    Thread.sleep(1);
                }
                catch(Exception e)
                {}
            }
        }
        mConvThread = null;
    }
}
