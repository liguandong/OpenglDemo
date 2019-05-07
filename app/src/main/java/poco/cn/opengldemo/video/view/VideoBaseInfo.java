package poco.cn.opengldemo.video.view;

import android.media.MediaMetadataRetriever;
import androidx.annotation.Nullable;


/**
 * Created by: fwc
 * Date: 2017/9/20
 */
public class VideoBaseInfo
{

	public String path;
	public int width;
	public int height;
	public long duration;
	public int rotation;

	public VideoBaseInfo Clone() {
		VideoBaseInfo info = new VideoBaseInfo();
		info.path = path;
		info.width = width;
		info.height = height;
		info.duration = duration;
		info.rotation = rotation;

		return info;
	}

	public static VideoBaseInfo getEndScreenInfo(String path) {
		VideoBaseInfo info = new VideoBaseInfo();
		info.path = path;
		info.width = info.height = 1080;
		info.duration = 3000;
		info.rotation = 0;

		return info;
	}

	public static VideoBaseInfo getStartScreenInfo(String path) {
		VideoBaseInfo info = new VideoBaseInfo();
		info.path = path;
		info.width = 1920;
		info.height = 1080;
		info.duration = 3000;
		info.rotation = 0;

		return info;
	}

	@Nullable
	public static VideoBaseInfo get(String videoPath) {
		VideoBaseInfo info = null;
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		String width, height, duration, rotation;
		try {
			mmr.setDataSource(videoPath);
			width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
			height = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
			duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
			rotation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);

			info = new VideoBaseInfo();
			info.path = videoPath;
			info.width = Integer.valueOf(width);
			info.height = Integer.valueOf(height);
			info.duration = Long.valueOf(duration);
			info.rotation = Integer.valueOf(rotation);
		} catch (Exception e) {
			e.printStackTrace();
			info = null;
		} finally {
			mmr.release();
		}

		if (info == null) {
			info = new VideoBaseInfo();
//			AVInfo avInfo = new AVInfo();
//			AVUtils.avInfo(videoPath, avInfo, false);
//			info.path = videoPath;
//			info.width = avInfo.width;
//			info.height = avInfo.height;
//			info.duration = avInfo.duration;
//			info.rotation = avInfo.videoRotation;
		}

		return info;
	}

}
