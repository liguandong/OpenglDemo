package poco.cn.medialibs.glview;

/**
 * Created by: fwc
 * Date: 2018/5/11
 */
public interface Renderer
{

	void onSurfaceCreated();

	void onSurfaceChanged(int width, int height);

	void onDrawFrame();

	void onSurfaceDestroyed();
}