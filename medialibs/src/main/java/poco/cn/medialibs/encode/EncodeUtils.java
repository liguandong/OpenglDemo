package poco.cn.medialibs.encode;

import android.annotation.TargetApi;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.Nullable;

/**
 * Created by: fwc
 * Date: 2017/9/21
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class EncodeUtils {

	public static int checkEncodeSize(int size) {
		return size / 16 * 16;
	}

	@Nullable
	public static MediaCodecInfo selectCodec(String mimeType, int colorFormat) {
		int numCodecs = MediaCodecList.getCodecCount();
		for (int i = 0; i < numCodecs; i++) {
			MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

			if (codecInfo.isEncoder()) {
				String[] types = codecInfo.getSupportedTypes();
				for (String type : types) {
					if (type.equalsIgnoreCase(mimeType)) {
						for (int format : codecInfo.getCapabilitiesForType(type).colorFormats) {
							if (format == colorFormat) {
								return codecInfo;
							}
						}
					}
				}
			}
		}
		return null;
	}

	@Nullable
	public static MediaCodecInfo selectCodec(String mimeType, int colorFormat, MediaFormat mediaFormat) {
		int numCodecs = MediaCodecList.getCodecCount();
		for (int i = 0; i < numCodecs; i++) {
			MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

			if (codecInfo.isEncoder()) {
				String[] types = codecInfo.getSupportedTypes();
				MediaCodecInfo.CodecCapabilities capabilities;
				for (String type : types) {
					if (type.equalsIgnoreCase(mimeType)) {
						capabilities = codecInfo.getCapabilitiesForType(type);
						for (int format : capabilities.colorFormats) {
							if (format == colorFormat) {
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
									if (capabilities.isFormatSupported(mediaFormat)) {
										return codecInfo;
									}
								} else {
									return codecInfo;
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	@Nullable
	public static MediaCodecInfo selectCodec(String mimeType, int colorFormat,
											 int width, int height, double frameRate) {
		int numCodecs = MediaCodecList.getCodecCount();
		for (int i = 0; i < numCodecs; i++) {
			MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

			if (codecInfo.isEncoder()) {
				String[] types = codecInfo.getSupportedTypes();
				MediaCodecInfo.CodecCapabilities capabilities;
				MediaCodecInfo.VideoCapabilities videoCapabilities;
				for (String type : types) {
					if (type.equalsIgnoreCase(mimeType)) {
						capabilities = codecInfo.getCapabilitiesForType(type);
						for (int format : capabilities.colorFormats) {
							if (format == colorFormat) {
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
									videoCapabilities = capabilities.getVideoCapabilities();
									if (videoCapabilities == null) {
										continue;
									}
									if (frameRate <= 0) {
										if (videoCapabilities.isSizeSupported(width, height)) {
											return codecInfo;
										}
									} else {
										if (videoCapabilities.areSizeAndRateSupported(width, height, frameRate)) {
											return codecInfo;
										}
									}
								} else {
									return codecInfo;
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * 获取视频编码支持的大小
	 *
	 * @param videoWidth 视频宽度
	 * @param videoHeight 视频高度
	 * @param maxWidth 限制最大宽度
	 * @param maxHeight 限制最大高度
	 * @return 最终大小
	 */
	public static int[] getVideoSupportSize(int videoWidth, int videoHeight, int maxWidth, int maxHeight) {

		int[] size = new int[2];

		if (videoWidth <= maxWidth && videoHeight <= maxHeight) {
			size[0] = videoWidth;
			size[1] = videoHeight;
		} else {
			float scaleX = videoWidth * 1f / maxWidth;
			float scaleY = videoHeight * 1f / maxHeight;

			if (scaleX > scaleY) {
				size[0] = maxWidth;
				size[1] = (int)(videoHeight / scaleX);
			} else {
				size[0] = (int)(videoWidth / scaleY);
				size[1] = maxHeight;
			}
		}

		return size;
	}
}
