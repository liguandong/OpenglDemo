package poco.cn.medialibs.player2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

/**
 * Created by: fwc
 * Date: 2018/4/3
 * 封装当前播放的视频信息
 */
public class PlayInfo {

	public static final int ID_START_SCREEN = -110;
	public static final int ID_END_SCREEN = -111;

	@NonNull
	public final String path;
	public final int duration;

	/**
	 * 当前视频是否静音
	 */
	public boolean isMute = false;

	/**
	 * 每个视频都有头转场和尾转场
	 */
	@Nullable
	public ITransitionInfo startTransition; // 头转场信息
	@Nullable
	public ITransitionInfo endTransition;   // 尾转场信息

	/**
	 * 视频唯一 id
	 */
	public final int videoId;

	public PlayInfo(@NonNull String path, int duration, int videoId) {
		this.path = path;
		this.duration = duration;
		this.videoId = videoId;
	}

	public static PlayInfo getPlayInfo(@NonNull String path, int duration, int videoId) {
		return getPlayInfo(path, duration, videoId, false);
	}

	public static PlayInfo getPlayInfo(String path, int duration, int videoId, boolean isMute) {
		PlayInfo playInfo = new PlayInfo(path, duration, videoId);
		playInfo.isMute = isMute;
		return playInfo;
	}

	public PlayInfo Clone() {
		PlayInfo info = new PlayInfo(path, duration, videoId);
		info.isMute = isMute;
		info.startTransition = startTransition;
		info.endTransition = endTransition;
		return info;
	}

	public PlayInfo Clone(String path, int duration, int videoId) {
		PlayInfo info = new PlayInfo(path, duration, videoId);
		info.isMute = isMute;
		info.startTransition = startTransition;
		info.endTransition = endTransition;
		return info;
	}

	public static PlayInfo getEndScreenInfo(String path) {
		return new PlayInfo(path, 3000, ID_END_SCREEN);
	}

	public static PlayInfo getStartScreenInfo(String path) {
		return new PlayInfo(path, 3000, ID_START_SCREEN);
	}

//	@NonNull
//	public static Bitmap getStartScreen(@NonNull Context context,
//											   @Nullable String title,
//											   @Nullable String subTitle,
//											   @Nullable String nickname) {
//		if (title == null) {
//			title = "";
//		}
//		if (subTitle == null) {
//			subTitle = "";
//		}
//
//		Paint paint = new Paint();
//		paint.setAntiAlias(true);
//		paint.setDither(true);
//		paint.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/HuaGuangJianBaoSong.ttf"));
//		paint.setColor(0xfff8d88f);
//
//		paint.setTextSize(114);
//		int size = title.length();
//
//		Canvas canvas;
//		Bitmap titleBitmap = null;
//		float x;
//		float y;
//		Paint.FontMetrics fontMetrics;
//		int titleWidth = 0;
//		int titleHeight = 0;
//		if (size > 0) {
//			titleWidth = (int) Math.ceil(paint.measureText("年"));
//			fontMetrics = paint.getFontMetrics();
//			titleHeight = (int) Math.ceil((fontMetrics.bottom - fontMetrics.top));
//
//			titleBitmap = Bitmap.createBitmap(titleWidth, titleHeight * size, Bitmap.Config.ARGB_8888);
//			canvas = new Canvas(titleBitmap);
//			x = 0;
//			y = -fontMetrics.top;
//
//			for (int i = 0; i < size; i++) {
//				canvas.drawText(String.valueOf(title.charAt(i)), x, y, paint);
//				y += titleHeight;
//			}
//
//			titleHeight *= size;
//		}
//
//		paint.setTextSize(48);
//		size = subTitle.length();
//		Bitmap subTitleBitmap = null;
//		int subTitleWidth = 0;
//		int subTitleHeight = 0;
//		if (size > 0) {
//			subTitleWidth = (int) Math.ceil(paint.measureText(subTitle));
//			fontMetrics = paint.getFontMetrics();
//			subTitleHeight = (int) Math.ceil(fontMetrics.bottom - fontMetrics.top);
//
//			subTitleBitmap = Bitmap.createBitmap(subTitleWidth, subTitleHeight, Bitmap.Config.ARGB_8888);
//			canvas = new Canvas(subTitleBitmap);
//			x = 0;
//			y = -fontMetrics.top;
//			canvas.drawText(subTitle, x, y, paint);
//		}
//
//		Bitmap logoBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_start_screen_logo);
//
//		if (titleBitmap == null && subTitleBitmap == null) {
//			return logoBitmap;
//		}
//
//		final int logoToSubTitleDis = 32;
//		int subTitleToTitleDis = 8;
//		int topSpace = 10;
//		if (titleBitmap == null) {
//			subTitleToTitleDis = 0 ;
//			topSpace = 0;
//		}
//
//		final int outWidth = logoBitmap.getWidth() + logoToSubTitleDis
//				+ subTitleHeight + subTitleToTitleDis + titleWidth;
//
//		final int outHeight = Math.max(titleHeight, Math.max(subTitleWidth + topSpace, logoBitmap.getHeight() + topSpace));
//
//		Bitmap outBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
//		canvas = new Canvas(outBitmap);
//		x = 0;
//		y = topSpace;
//		canvas.drawBitmap(logoBitmap, x, y, null);
//
//		x += logoBitmap.getWidth() + logoToSubTitleDis;
//		if (subTitleBitmap != null) {
//			Matrix matrix = new Matrix();
//			x += subTitleHeight;
//			y = topSpace;
//			matrix.setRotate(90);
//			matrix.postTranslate(x, y);
//			canvas.drawBitmap(subTitleBitmap, matrix, null);
//		}
//
//		if (titleBitmap != null) {
//			x += subTitleToTitleDis;
//			y = 0;
//			canvas.drawBitmap(titleBitmap, x, y, null);
//		}
//
//		return outBitmap;
//	}

	@Nullable
	public static Bitmap getTitleBitmap(@NonNull Context context,
										@Nullable String title,
										@Nullable String nickname) {

		if (TextUtils.isEmpty(title) && TextUtils.isEmpty(nickname)) {
			return null;
		}

		if (title == null) {
			title = "";
		}
		if (nickname == null) {
			nickname = "";
		}

		final Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setDither(true);
		paint.setColor(Color.WHITE);

		Paint.FontMetrics fontMetrics;
		final int titleGap = 15;
		final int gap = 25;

		final int titleLength = title.length();
		paint.setTextSize(88);
		int titleWidth = 0;
		int titleHeight = 0;
		if (titleLength > 0) {
			int i = 0;
			int character;
			String s;
			while (i < titleLength) {
				character = title.codePointAt(i);
				// 如果是辅助平面字符，则i+2
				if (Character.isSupplementaryCodePoint(character)) {
					i += 2;
				} else {
					i++;
				}
				s = new String(Character.toChars(character));
				titleWidth += paint.measureText(s) + titleGap;
			}
			titleWidth -= titleGap;
			fontMetrics = paint.getFontMetrics();
			titleHeight = (int)Math.ceil(fontMetrics.bottom - fontMetrics.top);
		}

		final int nicknameLength = nickname.length();
		paint.setTextSize(40);
		final int nicknameWidth = (int)Math.ceil(paint.measureText(nickname));
		fontMetrics = paint.getFontMetrics();
		final int nicknameHeight = (int)Math.ceil(fontMetrics.bottom - fontMetrics.top);

		final int width = Math.max(titleWidth, nicknameWidth);
		int height;
		if (titleLength == 0) {
			height = nicknameHeight;
		} else if (nicknameLength > 0) {
			height = titleHeight + gap + nicknameHeight;
		} else {
			height = titleHeight;
		}

		final Bitmap titleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(titleBitmap);

		float x;
		float y;

		if (titleLength > 0) {
			x = (width - titleWidth) / 2f;
			paint.setTextSize(88);
			fontMetrics = paint.getFontMetrics();
			y = -fontMetrics.top;
			int i = 0;
			int character;
			String s;
			while (i < titleLength) {
				character = title.codePointAt(i);
				// 如果是辅助平面字符，则i+2
				if (Character.isSupplementaryCodePoint(character)) {
					i += 2;
				} else {
					i++;
				}
				s = new String(Character.toChars(character));
				canvas.drawText(s, x, y, paint);
				x += (paint.measureText(s) + titleGap);
			}
		}

		if (nicknameLength > 0) {
			x = (width - nicknameWidth) / 2f;
			paint.setTextSize(40);
			fontMetrics = paint.getFontMetrics();
			if (titleLength == 0) {
				y = -fontMetrics.top;
			} else {
				y = titleHeight + gap - fontMetrics.top;
			}
			canvas.drawText(nickname, x, y, paint);
		}

		return titleBitmap;
	}

//	@Nullable
//	public static PlayInfo getPlayInfo(String path) {
//		if (TextUtils.isEmpty(path)) {
//			return null;
//		}
//
//		File file = new File(path);
//		if (!file.exists()) {
//			return null;
//		}
//
//		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//		retriever.setDataSource(path);
//		String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
//		long duration = Long.valueOf(durationStr);
//		return new PlayInfo(path, duration);
//	}
}
