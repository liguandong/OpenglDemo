package poco.cn.medialibs.gles;

/**
 * Created by: fwc
 * Date: 2017/12/18
 */
public class BufferPool {

	private final int mWidth;
	private final int mHeight;

	private int mLimitNewSize;

	private OffscreenBuffer mHead;
	private int mPoolSize = 0;

	public BufferPool(int width, int height, int limitNewSize) {
		mWidth = width;
		mHeight = height;

		mLimitNewSize = limitNewSize;
	}

	public OffscreenBuffer obtain() {

		if (mHead != null) {
			OffscreenBuffer buffer = mHead;
			mHead = buffer.next;
			buffer.next = null;
			mPoolSize--;
			return buffer;
		}

		mLimitNewSize--;
		if (mLimitNewSize < 0) {
			throw new RuntimeException("can not new more FrameBuffer.");
		}

		OffscreenBuffer buffer = new OffscreenBuffer(mWidth, mHeight);
		buffer.pool = this;
		return buffer;
	}

	public void recycle(OffscreenBuffer buffer) {
		if (buffer != null) {
			buffer.next = mHead;
			mHead = buffer;
			mPoolSize++;
		}
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public void release() {

		OffscreenBuffer curr = mHead, next;
		while (curr != null) {
			next = curr.next;
			curr.next = null;
			curr.pool = null;
			curr.release();

			curr = next;
		}

		mHead = null;
		mLimitNewSize = 0;
		mPoolSize = 0;
	}
}
