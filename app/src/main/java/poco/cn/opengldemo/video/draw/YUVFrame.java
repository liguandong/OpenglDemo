package poco.cn.opengldemo.video.draw;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import poco.cn.medialibs.gles.Drawable2d;
import poco.cn.medialibs.gles.GlUtil;
import poco.cn.opengldemo.R;


/**
 * Created by: fwc
 * Date: 2017/12/13
 */
public class YUVFrame
{

	private int mProgram = 0;

	private Drawable2d mDrawable2d = new Drawable2d();

	private int aPositionLoc;
	private int aTextureCoordLoc;

	private int uMVPMatrixLoc;
	private int uTexMatrixLoc;

	private int sTextureLoc;

	public YUVFrame(Context context) {

		mProgram = GlUtil.createProgram(context, R.raw.vertex_shader, R.raw.fragment_shader_ext);

		aPositionLoc = GLES20.glGetAttribLocation(mProgram, "aPosition");
		aTextureCoordLoc = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
		uMVPMatrixLoc = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
		uTexMatrixLoc = GLES20.glGetUniformLocation(mProgram, "uTexMatrix");

		sTextureLoc = GLES20.glGetUniformLocation(mProgram, "sTexture");
	}

	public void drawFrame(int textureId, float[] mvpMatrix, float[] texMatrix) {

		GLES20.glUseProgram(mProgram);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
		GLES20.glUniform1i(sTextureLoc, 0);

		GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0);
		GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0);

		GLES20.glEnableVertexAttribArray(aPositionLoc);
		GLES20.glVertexAttribPointer(aPositionLoc, mDrawable2d.getCoordsPerVertex(),
									 GLES20.GL_FLOAT, false, mDrawable2d.getVertexStride(), mDrawable2d.getVertexArray());

		GLES20.glEnableVertexAttribArray(aTextureCoordLoc);
		GLES20.glVertexAttribPointer(aTextureCoordLoc, 2,
									 GLES20.GL_FLOAT, false, mDrawable2d.getTexCoordStride(), mDrawable2d.getTexCoordArray());

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mDrawable2d.getVertexCount());

		GLES20.glDisableVertexAttribArray(aPositionLoc);
		GLES20.glDisableVertexAttribArray(aTextureCoordLoc);

		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

		GLES20.glUseProgram(0);
	}

	public void release() {

		if (mProgram != 0) {
			GLES20.glDeleteProgram(mProgram);
			mProgram = 0;
		}
	}
}
