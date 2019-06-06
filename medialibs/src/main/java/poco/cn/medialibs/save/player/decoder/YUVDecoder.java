package poco.cn.medialibs.save.player.decoder;


import poco.cn.medialibs.media.AVPixelFormat;

/**
 * Created by: fwc
 * Date: 2018/10/19
 */
public class YUVDecoder extends AbsDecoder {

	public YUVDecoder(int bufferSize) {
		super(bufferSize, AVPixelFormat.AV_PIX_FMT_NV21);
	}
}
