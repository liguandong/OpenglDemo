/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package poco.cn.medialibs.encode;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

import poco.cn.medialibs.gles.L;

/**
 * This class wraps up the core components used for surface-input video encoding.
 * <p>
 * Once created, frames are fed to the input surface.  Remember to provide the presentation
 * time stamp, and always call drainEncoder() before swapBuffers() to ensure that the
 * producer side doesn't get backed up.
 * <p>
 * This class is not thread-safe, with one exception: it is valid to use the input surface
 * on one thread, and drain the output on a different thread.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class VideoEncoderCore {

	private static final String TAG = "VideoEncoderCore";

	private static final int TIMEOUT_USEC = 10000;

	private Surface mInputSurface;
	private MediaMuxer mMuxer;
	private MediaCodec mEncoder;
	private MediaCodec.BufferInfo mBufferInfo;
	private int mTrackIndex;
	private boolean mMuxerStarted;

	private boolean isError;

	private boolean isRelease;

	/**
	 * Configures encoder and muxer state, and prepares the input Surface.
	 */
	public VideoEncoderCore(EncodeConfig config) throws IOException {
		mBufferInfo = new MediaCodec.BufferInfo();

		MediaFormat format = MediaFormat.createVideoFormat(config.mimeType, config.width, config.height);

		// Set some properties.  Failing to specify some of these can cause the MediaCodec
		// configure() call to throw an unhelpful exception.
		int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
		format.setInteger(MediaFormat.KEY_BIT_RATE, config.bitRate);
//		format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
		format.setInteger(MediaFormat.KEY_FRAME_RATE, config.frameRate);
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.iFrameInterval);

		L.d(TAG, "format: " + format);

		// Create a MediaCodec encoder, and configure it with our format.  Get a Surface
		// we can use for input and wrap it with a class that handles the EGL work.

//		MediaCodecInfo info = EncodeUtils.selectCodec(config.mimeType, colorFormat);
//		if (info != null) {
//			mEncoder = MediaCodec.createByCodecName(info.getName());
//		} else {
//			mEncoder = MediaCodec.createEncoderByType(config.mimeType);
//		}
		mEncoder = MediaCodec.createEncoderByType(config.mimeType);
		try {
			mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			mInputSurface = mEncoder.createInputSurface();
			mEncoder.start();
		} catch (Exception e) {
			isError = true;
			if (mEncoder != null) {
				try {
					mEncoder.release();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			throw new MediaCodecNotSupportException(e.getMessage());
		}

		// Create a MediaMuxer.  We can't add the video track and start() the muxer here,
		// because our MediaFormat doesn't have the Magic Goodies.  These can only be
		// obtained from the encoder after it has started processing data.
		//
		// We're not actually interested in multiplexing audio.  We just want to convert
		// the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
		mMuxer = new MediaMuxer(config.outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

		if (config.rotation != 0) {
			mMuxer.setOrientationHint(config.rotation);
		}
		mTrackIndex = -1;
		mMuxerStarted = false;
	}

	/**
	 * Returns the encoder's input surface.
	 */
	public Surface getInputSurface() {
		return mInputSurface;
	}

	/**
	 * Releases encoder resources.
	 */
	public void release() {
		L.d(TAG, "releasing encoder objects");
		isRelease = true;
		if (isError) {
			return;
		}
		if (mEncoder != null) {
			try {
				mEncoder.stop();
				mEncoder.release();
				mEncoder = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (mMuxer != null) {
			// TODO: stop() throws an exception if you haven't fed it any data.
			// TODO: Keep track of frames submitted, and don't call stop() if we haven't written anything.
			try {
				mMuxer.stop();
				mMuxer.release();
				mMuxer = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Extracts all pending data from the encoder and forwards it to the muxer.
	 * <p>
	 * If endOfStream is not set, this returns when there is no more data to drain.  If it
	 * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
	 * Calling this with endOfStream set should be done once, right before stopping the muxer.
	 * <p>
	 * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
	 * not recording audio.
	 */
	public void drainEncoder(boolean endOfStream) {

		if (isError || isRelease) {
			return;
		}

		L.d(TAG, "drainEncoder(" + endOfStream + ")");

		if (endOfStream) {
			L.d(TAG, "sending EOS to encoder");
			try {
				mEncoder.signalEndOfInputStream();
			} catch (Exception e){
				e.printStackTrace();
				isError = true;
				throw new RuntimeException();
			}
		}

		ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
		while (!isRelease && !isError) {
			int encoderStatus;
			try {
				encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
			} catch (Exception e) {
				e.printStackTrace();
				isError = true;
				throw new RuntimeException();
			}
			if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
				// no output available yet
				if (!endOfStream) {
					break;      // out of while
				} else {
					L.d(TAG, "no output available, spinning to await EOS");
				}
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				// not expected for an encoder
				encoderOutputBuffers = mEncoder.getOutputBuffers();
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				// should happen before receiving buffers, and should only happen once
				if (mMuxerStarted) {
					throw new RuntimeException("format changed twice");
				}
				MediaFormat newFormat = mEncoder.getOutputFormat();
				L.d(TAG, "encoder output format changed: " + newFormat);

				// now that we have the Magic Goodies, start the muxer
				mTrackIndex = mMuxer.addTrack(newFormat);
				mMuxer.start();
				mMuxerStarted = true;
			} else if (encoderStatus < 0) {
				L.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
				// let's ignore it
			} else {
				ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
				if (encodedData == null) {
					throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
				}

				if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
					// The codec config data was pulled out and fed to the muxer when we got
					// the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
					L.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
					mBufferInfo.size = 0;
				}

				if (mBufferInfo.size != 0) {
					if (!mMuxerStarted) {
						throw new RuntimeException("muxer hasn't started");
					}

					// adjust the ByteBuffer values to match BufferInfo (not needed?)
					encodedData.position(mBufferInfo.offset);
					encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

					mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);

					L.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" + mBufferInfo.presentationTimeUs);
				}

				mEncoder.releaseOutputBuffer(encoderStatus, false);

				if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					if (!endOfStream) {
						L.w(TAG, "reached end of stream unexpectedly");
					} else {
						L.d(TAG, "end of stream reached");
					}
					break;      // out of while
				}
			}
		}
	}

	public static class EncodeConfig {

		public static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
		public static final int FRAME_RATE = 30;               // 30fps
		public static final int IFRAME_INTERVAL = 1;           // 1 seconds between I-frames
		public static final int BIT_RATE = 5 * 1024 * 1024;    // 5Mb/s

		public final int width;
		public final int height;
		public final String outputPath;

		public String mimeType = MIME_TYPE;
		public int bitRate = BIT_RATE;
		public int frameRate = FRAME_RATE;
		public int iFrameInterval = IFRAME_INTERVAL;
		public int rotation;

		public EncodeConfig(int width, int height, int frameRate, String outputPath) {
			this.width = width;
			this.height = height;
			this.outputPath = outputPath;

			this.frameRate = frameRate;

			bitRate = (int)(0.25f * FRAME_RATE * width * height);
		}
	}
}
