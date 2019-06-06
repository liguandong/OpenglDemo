package poco.cn.medialibs.save.player;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Created by: fwc
 * Date: 2018/10/19
 */
public interface PlayerFactory {

	@NonNull
	SoftPlayer createPlayer(Context context);
}
