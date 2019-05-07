package poco.cn.medialibs.player2;

/**
 * Created by: fwc
 * Date: 2018/8/16
 */
public interface ITransitionInfo
{

	int getId();
	int getTime();

	void setTime(int time);

	boolean isBlendTransition();
}
