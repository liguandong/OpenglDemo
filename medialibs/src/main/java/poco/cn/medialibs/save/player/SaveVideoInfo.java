package poco.cn.medialibs.save.player;

import android.opengl.Matrix;

/**
 * Created by: fwc
 * Date: 2018/5/25
 */
public class SaveVideoInfo {

	public final int id;
	public final String path;
	public final int width;
	public final int height;
	public final long duration;
	public final int rotation;
	public final boolean isMute;

	public float[] modelMatrix = new float[16];
	public float[] texMatrix = new float[16];

	public SaveVideoInfo(int id, String path, int width, int height, long duration, int rotation, boolean isMute) {
		this.id = id;
		this.path = path;
		this.width = width;
		this.height = height;
		this.duration = duration;
		this.rotation = rotation;
		this.isMute = isMute;

		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.setIdentityM(texMatrix, 0);

//		Matrix.rotateM();
	}
}
