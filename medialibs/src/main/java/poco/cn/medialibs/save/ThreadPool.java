package poco.cn.medialibs.save;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import poco.cn.medialibs.BuildConfig;

/**
 * Created by: fwc
 * Date: 2018/1/9
 */
public class ThreadPool
{

	private static final int THREAD_COUNT = 3;

	private volatile static ThreadPool sInstance;

	@Nullable
	private ExecutorService mService;

	private ThreadPool() {
		mService = Executors.newFixedThreadPool(THREAD_COUNT);
	}

	public static ThreadPool getInstance() {
		if (sInstance == null) {
			synchronized (ThreadPool.class) {
				if (sInstance == null) {
					sInstance = new ThreadPool();
				}
			}
		}

		return sInstance;
	}

	public void execute(Runnable runnable) {
		ExecutorService service = mService;
		if (runnable != null && service != null) {
			service.execute(runnable);
		} else if (BuildConfig.DEBUG) {
			throw new RuntimeException("the runnable is null");
		}
	}

	public void release() {
		if (mService != null) {
			mService.shutdown();
			mService = null;
		}

		sInstance = null;
	}
}
