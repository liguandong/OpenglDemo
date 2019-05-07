package poco.cn.medialibs.player2.base;

/**
 * Created by: fwc
 * Date: 2018/12/5
 */
public class SeekDataPool {

	private final Object mLock = new Object();

	private SeekData mHead;

	public synchronized SeekData obtain() {

		synchronized (mLock) {
			if (mHead != null) {
				final SeekData seekData = mHead;
				mHead = seekData.next;
				seekData.next = null;
				return seekData;
			}
		}

		return new SeekData();
	}

	public void recycle(SeekData seekData) {
		if (seekData != null) {
			synchronized (mLock) {
				seekData.next = mHead;
				mHead = seekData;
			}
		}
	}

	public static class SeekData {
		public long position;
		public boolean isExact;
		private SeekData next;
	}
}
