package poco.cn.medialibs.player2;

/**
 * Created by: fwc
 * Date: 2018/4/4
 */
interface IMessage {

	int MSG_INIT = 0;
	int MSG_SET_SURFACE = 1;
	int MSG_SET_PLAY_INFOS = 2;
	int MSG_PREPARE = 3;
	int MSG_START = 4;
	int MSG_PAUSE = 5;
	int MSG_SEEK_TO = 6;
	int MSG_RESET = 7;
	int MSG_RELEASE = 8;
	int MSG_SET_VOLUME = 9;
	int MSG_SET_LOOPING = 10;
	int MSG_CHANGE_VIDEO_MUTE = 11;
	int MSG_VIDEO_FINISH = 12;

	int MSG_START_TRANSITION = 13;
	int MSG_RESTART_TRANSITION = 14;
	int MSG_EXIT_TRANSITION = 15;
	int MSG_UPDATE_TRANSITION_TIME = 16;
	int MSG_UPDATE_TRANSITION = 17;
	int MSG_SET_START_TRANSITION = 18;
	int MSG_UPDATE_FRAME = 19;
	int MSG_SEEK_TO_END = 20;

	int MSG_SET_LISTENER = 21;
	int MSG_SET_RANGE_PLAY = 22;
}
