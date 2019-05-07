package poco.cn.medialibs.gles;

import android.util.Log;

import poco.cn.medialibs.BuildConfig;

/**
 * Log统一管理类
 */
public class L {

	/**
	 * 是否需要打印bug，可以在application的onCreate函数里面初始化
	 */
	public static boolean isDebug = BuildConfig.DEBUG;

	private static final String TAG = "comit";

	public static void i(String msg) {
		if (isDebug) Log.i(TAG, msg);
	}

	public static void d(String msg) {
		if (isDebug) Log.d(TAG, msg);
	}

	public static void e(String msg) {
		if (isDebug) Log.e(TAG, msg);
	}

	public static void v(String msg) {
		if (isDebug) Log.v(TAG, msg);
	}

	public static void w(String msg) {
		if (isDebug) {
			Log.w(TAG, msg);
		}
	}

	// 下面是传入自定义tag的函数
	public static void i(String tag, String msg) {
		if (isDebug) Log.i(tag, msg);
	}

	public static void d(String tag, String msg) {
		if (isDebug) Log.d(tag, msg);
	}

	public static void e(String tag, String msg) {
		if (isDebug) Log.e(tag, msg);
	}

	public static void v(String tag, String msg) {
		if (isDebug) Log.v(tag, msg);
	}

	public static void w(String tag, String msg) {
		if (isDebug) {
			Log.w(tag, msg);
		}
	}
}
