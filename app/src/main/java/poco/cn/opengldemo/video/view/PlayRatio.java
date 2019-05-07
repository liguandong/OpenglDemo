package poco.cn.opengldemo.video.view;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by: fwc
 * Date: 2017/10/18
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({PlayRatio.RATIO_9_16, PlayRatio.RATIO_16_9, PlayRatio.RATIO_235_1, PlayRatio.RATIO_1_1, PlayRatio.RATIO_3_4})
public @interface PlayRatio
{
	int RATIO_9_16 = 1;
	int RATIO_16_9 = 2;
	int RATIO_235_1 = 3;
	int RATIO_1_1 = 4;
	int RATIO_3_4 = 5; // 1.7.5新增
}
