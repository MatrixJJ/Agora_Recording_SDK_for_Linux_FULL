import io.agora.recording.common.Common;

/**
 * 功能说明：<h1></h1>
 * 系统名称：<br>
 * 模块名称：com.chebei.record.core<br>
 * 系统版本：V1.0.0<br>
 * 开发人员：Aaron.Zhang<br>
 * 开发时间：2018-02-06 10:51<br>
 * 功能描述：<br>
 * @author Aaron.Zhang
 */
public interface AgoraJavaRecordCallback {

	void nativeObjectRef(long nativeHandle);

	void onLeaveChannel(int reason);

	void onError(int error, int statCode);

	void onWarning(int warn);

	void onUserOffline(long uid, int reason);

	void onUserJoined(long uid, String recordingDir);

	void audioFrameReceived(long uid, int type, Common.AudioFrame frame);

	void videoFrameReceived(long uid, int type, Common.VideoFrame frame, int rotation);

	void stopCallBack();

	void recordingPathCallBack(String path);

}