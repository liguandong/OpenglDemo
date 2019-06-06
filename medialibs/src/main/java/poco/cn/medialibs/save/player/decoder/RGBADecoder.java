package poco.cn.medialibs.save.player.decoder;


import poco.cn.medialibs.media.AVPixelFormat;

/**
 * Created by: fwc
 * Date: 2018/10/19
 */
public class RGBADecoder extends AbsDecoder {

	public RGBADecoder(int bufferSize) {
		super(bufferSize, AVPixelFormat.AV_PIX_FMT_RGBA);
	}
}
