import io.agora.recording.common.Common;
import io.agora.recording.common.RecordingConfig;
import io.agora.recording.common.RecordingEngineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * 功能说明：<h1></h1>
 * 系统名称：<br>
 * 模块名称：com.chebei.record.service<br>
 * 系统版本：V1.0.0<br>
 * 开发人员：Aaron.Zhang<br>
 * 开发时间：2018-02-05 16:56<br>
 * 功能描述：<br>
 */
public class AgoraJavaRecording {

	private static Logger logger = LoggerFactory.getLogger(AgoraJavaRecording.class);

	// java run status flag
	private boolean stopped = false;
	public boolean isMixMode = false;
	public int width = 0;
	public int height = 0;
	public int fps = 0;
	public int kbps = 0;
	private String storageDir = "./";
	private long aCount = 0;
	private long count = 0;
	private long size = 0;
	public Common.CHANNEL_PROFILE_TYPE profile_type;
	private Vector<Long> m_peers = new Vector<>();
	public long mNativeHandle = 0;

	public int clientUid;

	private AgoraJavaRecordCallback agoraJavaRecordCallback;

	private boolean IsMixMode() {
		return isMixMode;
	}

	/*
	 * Brief: load Cpp library
	 */
	static {
		System.loadLibrary("recording");
	}

	/*
	 * Brief: This method will create a recording engine instance and join a channel, and then start recording.
	 *
	 * @param channelId  A string providing the unique channel id for the AgoraRTC session
	 * @param channelKey  This parameter is optional if the user uses a static key, or App ID. In this case, pass NULL as the parameter value. More details refer to http://docs-origin.agora.io/en/user_guide/Component_and_Others/Dynamic_Key_User_Guide.html
	 * @param uid  The uid of recording client
	 * @param config  The config of current recording
	 * @return true: Method call succeeded. false: Method call failed.
	 */
	public native boolean createChannel(String appId, String channelKey, String name, int uid, RecordingConfig config);

	/*
	 * Brief: Stop recording
	 * @param nativeHandle  The recording engine
	 * @return true: Method call succeeded. false: Method call failed.
	 */
	public native boolean leaveChannel(long nativeHandle);

	/*
	 * Brief: Set the layout of video mixing
	 * @param nativeHandle  The recording engine
	 * @param layout layout setting
	 * @return 0: Method call succeeded. <0: Method call failed.
	 */
	public native int setVideoMixingLayout(long nativeHandle, Common.VideoMixingLayout layout);

	/*
	 * Brief: Start service under manually trigger mode
	 * @param nativeHandle  The recording engine
	 * @return 0: Method call succeeded. <0: Method call failed.
	 */
	public native int startService(long nativeHandle);

	/*
	 * Brief: Stop service under manually trigger mode
	 * @param nativeHandle  The recording engine
	 * @return 0: Method call succeeded. <0: Method call failed.
	 */
	public native int stopService(long nativeHandle);

	/*
	 * Brief: Get recording properties
	 * @param nativeHandle  The recording engine
	 * @return io.agora.recording.common.RecordingEngineProperties
	 */
	public native RecordingEngineProperties getProperties(long nativeHandle);

	/*
	 * Brief: When call createChannel successfully, JNI will call back this method to set the recording engine.
	 */
	public void nativeObjectRef(long nativeHandle) {
		mNativeHandle = nativeHandle;
		if (this.agoraJavaRecordCallback != null) {
			this.agoraJavaRecordCallback.nativeObjectRef(nativeHandle);
		}
	}

	/*
	 * Brief: Callback when recording application successfully left the channel
	 * @param reason  leave path reason, please refer to the define of LEAVE_PATH_CODE
	 */
	public void onLeaveChannel(int reason) {
		logger.info("AgoraJavaRecording onLeaveChannel,code:" + reason);
		stopped = true;
		if (this.agoraJavaRecordCallback != null) {
			this.agoraJavaRecordCallback.onLeaveChannel(reason);
		}
	}

	/*
	 * Brief: Callback when an error occurred during the runtime of recording engine
	 * @param error  Error code, please refer to the define of ERROR_CODE_TYPE
	 * @param error  State code, please refer to the define of STAT_CODE_TYPE
	 */
	public void onError(int error, int stat_code) {
		logger.info("AgoraJavaRecording onError,error:" + error + ",stat code:" + stat_code);
		stopped = true;
		if (this.agoraJavaRecordCallback != null) {
			this.agoraJavaRecordCallback.onError(error, stat_code);
		}
	}

	/*
	 * Brief: Callback when an warning occurred during the runtime of recording engine
	 * @param warn  Warning code, please refer to the define of WARN_CODE_TYPE
	 */
	public void onWarning(int warn) {
		logger.info("AgoraJavaRecording onWarning,warn:" + warn);
		if (this.agoraJavaRecordCallback != null) {
			this.agoraJavaRecordCallback.onWarning(warn);
		}
	}

	/*
	 * Brief: Callback when a user left the channel or gone offline
	 * @param uid  user ID
	 * @param reason  offline reason, please refer to the define of USER_OFFLINE_REASON_TYPE
	 */
	public void onUserOffline(long uid, int reason) {
		logger.info("AgoraJavaRecording onUserOffline uid:" + uid + ",offline reason:" + reason);
		m_peers.remove(uid);
		PrintUsersInfo(m_peers);
		SetVideoMixingLayout();
		if (this.agoraJavaRecordCallback != null) {
			this.agoraJavaRecordCallback.onUserOffline(uid, reason);
		}
	}

	/*
	 * Brief: Callback when another user successfully joined the channel
	 * @param uid  user ID
	 * @param recordingDir  user recorded file directory
	 */
	public void onUserJoined(long uid, String recordingDir) {
		logger.info("onUserJoined uid:" + uid + ",recordingDir:" + recordingDir);
		storageDir = recordingDir;
		m_peers.add(uid);
		PrintUsersInfo(m_peers);
		// When the user joined, we can re-layout the canvas
		SetVideoMixingLayout();
		if (this.agoraJavaRecordCallback != null) {
			this.agoraJavaRecordCallback.onUserJoined(uid, recordingDir);
		}
	}

	/*
	 * Brief: Callback when received a audio frame
	 * @param uid  user ID
	 * @param type  type of audio frame, please refer to the define of AudioFrame
	 * @param frame  reference of received audio frame
	 */
	public void audioFrameReceived(long uid, int type, Common.AudioFrame frame) {
		// logger.info("java demo audioFrameReceived,uid:"+uid+",type:"+type);
		byte[] byt = null;
		String path = storageDir + Long.toString(uid);
		if (type == 0) {//pcm
			path += ".pcm";
			byt = frame.pcm.pcmBuf;
		} else if (type == 1) {//aac
			path += ".aac";
			byt = frame.aac.aacBuf;
		}
		ByteBuffer buf =ByteBuffer.wrap(byt) ;
		WriteBytesToFileClassic(buf, path);
		if (this.agoraJavaRecordCallback != null) {
			this.agoraJavaRecordCallback.audioFrameReceived(uid, type, frame);
		}
		buf = null;
		path = null;
		frame = null;
	}

	/*
	 * Brief: Callback when received a video frame
	 * @param uid  user ID
	 * @param type  type of video frame, please refer to the define of VideoFrame
	 * @param frame  reference of received video frame
	 * @param rotation rotation of video
	 */
	public void videoFrameReceived(long uid, int type, Common.VideoFrame frame, int rotation) {// rotation:0, 90, 180, 270
		String path = storageDir + Long.toString(uid);
		byte[] bytes = null;
		// logger.info("java demo videoFrameReceived,uid:"+uid+",type:"+type);
		if (type == 0) {//yuv
			path += ".yuv";
			bytes = frame.yuv.buf;
			if (bytes == null) {
				logger.info("java demo videoFrameReceived null");
			}
		} else if (type == 1) {//h264
			path += ".h264";
			bytes = frame.h264.buf;
		} else if (type == 2) { // jpg
			path += "_" + GetNowDate() + ".jpg";
			bytes = frame.jpg.buf;
		}
		ByteBuffer buf = ByteBuffer.wrap(bytes) ;
		WriteBytesToFileClassic(buf, path);
		if (this.agoraJavaRecordCallback != null) {
			this.agoraJavaRecordCallback.videoFrameReceived(uid, type, frame, rotation);
		}
		buf = null;
		path = null;
		frame = null;
	}

	/*
	 * Brief: Callback when JNI layer exited
	 */
	public void stopCallBack() {
		logger.info("java demo receive stop from JNI ");
		stopped = true;
		if (this.agoraJavaRecordCallback != null) {
			this.agoraJavaRecordCallback.stopCallBack();
		}
	}

	/*
	 * Brief: Callback when call createChannel successfully
	 * @param path recording file directory
	 */
	public void recordingPathCallBack(String path) {
		storageDir = path;
		if (this.agoraJavaRecordCallback != null) {
			this.agoraJavaRecordCallback.recordingPathCallBack(path);
		}
	}

	public int SetVideoMixingLayout() {
		Common ei = new Common();
		Common.VideoMixingLayout layout = ei.new VideoMixingLayout();

		if (!IsMixMode()) {
			return -1;
		}

		layout.canvasHeight = height;
		layout.canvasWidth = width;
		layout.backgroundColor = "#23b9dc";
		layout.regionCount = (int) (m_peers.size());

		if (this.clientUid != 0) {
			if (!m_peers.isEmpty()) {
				logger.info("java setVideoMixingLayout m_peers is not empty, start layout");
				int max_peers = (profile_type == Common.CHANNEL_PROFILE_TYPE.CHANNEL_PROFILE_COMMUNICATION ? 7 : 16);
				Common.VideoMixingLayout.Region[] regionList = new Common.VideoMixingLayout.Region[m_peers.size()];
				regionList[0] = layout.new Region();
				regionList[0].uid = clientUid;
				regionList[0].x = 0.f;
				regionList[0].y = 0.f;
				regionList[0].width = 1.f;
				regionList[0].height = 1.f;
			//	regionList[0].zOrder = 0;
				regionList[0].alpha = 1.f;
				regionList[0].renderMode = 0;
				float f_width = width;
				float f_height = height;
				float canvasWidth = f_width;
				float canvasHeight = f_height;
				float viewWidth = 0.235f;
				float viewHEdge = 0.012f;
				float viewHeight = viewWidth * (canvasWidth / canvasHeight);
				float viewVEdge = viewHEdge * (canvasWidth / canvasHeight);

				int i = 1;
				for (Long m_peer : m_peers) {
					if (m_peer == clientUid) {
						continue;
					}
					if (i >= m_peers.size()) {
						return -1;
					}
					if (i >= max_peers) {
						break;
					}
					regionList[i] = layout.new Region();
					regionList[i].uid = m_peer;
					float f_x = (i - 1) % 4;
					float f_y = (i - 1) / 4;
					float xIndex = f_x;
					float yIndex = f_y;
					regionList[i].x = xIndex * (viewWidth + viewHEdge) + viewHEdge;
					regionList[i].y = 1 - (yIndex + 1) * (viewHeight + viewVEdge);
					regionList[i].width = viewWidth;
					regionList[i].height = viewHeight;
					regionList[i].alpha = (i + 1);
					regionList[i].renderMode = 0;
					i++;
				}
				layout.regions = regionList;
			} else {
				layout.regions = null;
			}
		} else {
			if (!m_peers.isEmpty()) {
				logger.info("java setVideoMixingLayout m_peers is not empty, start layout");
				int max_peers = (profile_type == Common.CHANNEL_PROFILE_TYPE.CHANNEL_PROFILE_COMMUNICATION ? 7 : 16);
				Common.VideoMixingLayout.Region[] regionList = new Common.VideoMixingLayout.Region[m_peers.size()];
				regionList[0] = layout.new Region();
				regionList[0].uid = m_peers.get(0);
				regionList[0].x = 0.f;
				regionList[0].y = 0.f;
				regionList[0].width = 1.f;
				regionList[0].height = 1.f;
				regionList[0].alpha = 1.f;
				regionList[0].renderMode = 0;
				float f_width = width;
				float f_height = height;
				float canvasWidth = f_width;
				float canvasHeight = f_height;
				float viewWidth = 0.235f;
				float viewHEdge = 0.012f;
				float viewHeight = viewWidth * (canvasWidth / canvasHeight);
				float viewVEdge = viewHEdge * (canvasWidth / canvasHeight);
				for (int i = 1; i < m_peers.size(); i++) {
					if (i >= max_peers) {
						break;
					}
					regionList[i] = layout.new Region();

					regionList[i].uid = m_peers.get(i);
					float f_x = (i - 1) % 4;
					float f_y = (i - 1) / 4;
					float xIndex = f_x;
					float yIndex = f_y;
					regionList[i].x = xIndex * (viewWidth + viewHEdge) + viewHEdge;
					regionList[i].y = 1 - (yIndex + 1) * (viewHeight + viewVEdge);
					regionList[i].width = viewWidth;
					regionList[i].height = viewHeight;
					regionList[i].alpha = (i + 1);
					regionList[i].renderMode = 0;
				}
				layout.regions = regionList;
			} else {
				layout.regions = null;
			}
		}
		return setVideoMixingLayout(mNativeHandle, layout);
	}

	public void WriteBytesToFileClassic(ByteBuffer byteBuffer, String fileDest) {
		if (byteBuffer == null) {
			logger.info("WriteBytesToFileClassic but byte buffer is null!");
			return;
		}
		byte[] data = new byte[byteBuffer.capacity()];
		((ByteBuffer) byteBuffer.duplicate().clear()).get(data);
		try {
			FileOutputStream fos = new FileOutputStream(fileDest, true);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(data);
			bos.flush();
			bos.close();
			fos = null;
			bos = null;
			data = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String GetNowDate() {
		String temp_str = "";
		Date dt = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		temp_str = sdf.format(dt);
		return temp_str;
	}

	public void PrintUsersInfo(Vector vector) {
		logger.info("user size:" + vector.size());
		for (Long l : m_peers) {
			logger.info("user:" + l);
		}
	}

	public void setAgoraJavaRecordCallback(AgoraJavaRecordCallback agoraJavaRecordCallback) {
		this.agoraJavaRecordCallback = agoraJavaRecordCallback;
	}

}