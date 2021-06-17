import com.chebei.ams.client.sdk.inst.AmsClient;
import com.chebei.ams.message.constant.EventReturnCode;
import com.chebei.ams.message.event.Event;
import com.chebei.ams.message.utils.EventUtils;
import com.chebei.ams.processor.ext.utils.MapUtils;
import com.chebei.ams.thread.ThreadPoolFactory;
import com.chebei.record.util.SpringUtils;
import io.agora.recording.common.Common;
import io.agora.recording.common.Common.MIXED_AV_CODEC_TYPE;
import io.agora.recording.common.RecordingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

/**
 * 功能说明：<h1></h1>
 * 系统名称：<br>
 * 模块名称：com.chebei.record.service<br>
 * 系统版本：V1.0.0<br>
 * 开发人员：Aaron.Zhang<br>
 * 开发时间：2018-02-05 16:50<br>
 * 功能描述：<br>
 *
 * @author Aaron.Zhang
 */
class AgoraRecordRunner implements AgoraJavaRecordCallback {

	private static Logger logger = LoggerFactory.getLogger(AgoraRecordRunner.class);

	private static final int SLEEP_TIME = 500;

	private static final int LOOP_COUNT = 40;

	private AgoraJavaRecording agoraJavaRecording;

	private OnFinishListener onFinishListener;

	private boolean onChannel = false;

	private long nativeHandle;

	private String appId;

	private int uid;

	private int clientUid;

	private String channelName;

	private String channelKey;

	private String appLiteDir;

	private String recordRootDir;

	private String recordingPath;

	private AmsClient routerClient = SpringUtils.getBean(AmsClient.class);

	private ThreadPoolFactory threadPoolFactory = SpringUtils.getBean(ThreadPoolFactory.class);

	AgoraRecordRunner(OnFinishListener onFinishListener) {
		this.onFinishListener = onFinishListener;
	}

	void createChannel() {
		int channelProfile = 0;

		String decryptionMode = "";
		String secret = "";
		String mixResolution = "360,640,15,500";

		// 300s -- 暂且由10s修改成30s，机器面签查询题目的时间比较长
		int idleLimitSec = 30;

		String cfgFilePath = "";

		// 40000
		int lowUdpPort = 0;
		// 40004
		int highUdpPort = 0;

		boolean isAudioOnly = false;
		boolean isVideoOnly = false;
		boolean isMixingEnabled = true;
		MIXED_AV_CODEC_TYPE mixedVideoAudio =MIXED_AV_CODEC_TYPE.MIXED_AV_DEFAULT;

		int getAudioFrame = Common.AUDIO_FORMAT_TYPE.AUDIO_FORMAT_DEFAULT_TYPE.ordinal();
		int getVideoFrame = Common.VIDEO_FORMAT_TYPE.VIDEO_FORMAT_DEFAULT_TYPE.ordinal();
		int streamType = Common.REMOTE_VIDEO_STREAM_TYPE.REMOTE_VIDEO_STREAM_HIGH.ordinal();
		int captureInterval = 5;
		int triggerMode = 0;

		int width = 0;
		int height = 0;
		int fps = 0;
		int kbps = 0;
		int count = 0;

		if (appId == null || channelName == null || channelKey == null || appLiteDir == null || recordRootDir == null) {
			throw new RuntimeException("参数不能为空");
		}

		agoraJavaRecording = new AgoraJavaRecording();
		final RecordingConfig config = new RecordingConfig();
		config.channelProfile = Common.CHANNEL_PROFILE_TYPE.values()[channelProfile];
		config.idleLimitSec = idleLimitSec;
		config.isVideoOnly = isVideoOnly;
		config.isAudioOnly = isAudioOnly;
		config.isMixingEnabled = isMixingEnabled;
		config.mixResolution = mixResolution;
		config.mixedVideoAudio = mixedVideoAudio;
		config.appliteDir = appLiteDir;
		config.recordFileRootDir = recordRootDir;
		config.cfgFilePath = cfgFilePath;
		config.secret = secret;
		config.decryptionMode = decryptionMode;
		config.lowUdpPort = lowUdpPort;
		config.highUdpPort = highUdpPort;
		config.captureInterval = captureInterval;
		config.decodeAudio = Common.AUDIO_FORMAT_TYPE.values()[getAudioFrame];
		config.decodeVideo = Common.VIDEO_FORMAT_TYPE.values()[getVideoFrame];
		config.streamType = Common.REMOTE_VIDEO_STREAM_TYPE.values()[streamType];
		config.triggerMode = triggerMode;

		/*
		 * change log_config Facility per your specific purpose like agora::base::LOCAL5_LOG_FCLT
		 * Default:USER_LOG_FCLT.
		 *
		 * agoraJavaRecording.setFacility(LOCAL5_LOG_FCLT);*/
		logger.info(System.getProperty("java.library.path"));

		agoraJavaRecording.isMixMode = isMixingEnabled;
		agoraJavaRecording.profile_type = Common.CHANNEL_PROFILE_TYPE.values()[channelProfile];
		if (isMixingEnabled && !isAudioOnly) {
			String[] sourceStrArray = mixResolution.split(",");
			if (sourceStrArray.length != 4) {
				logger.info("Illegal resolution:" + mixResolution);
				return;
			}
			agoraJavaRecording.width = Integer.valueOf(sourceStrArray[0]);
			agoraJavaRecording.height = Integer.valueOf(sourceStrArray[1]);
			agoraJavaRecording.fps = Integer.valueOf(sourceStrArray[2]);
			agoraJavaRecording.kbps = Integer.valueOf(sourceStrArray[3]);
		}

		agoraJavaRecording.clientUid = this.clientUid;

		agoraJavaRecording.setAgoraJavaRecordCallback(this);

		// run jni event loop , or start a new thread to do it
		ThreadPoolFactory threadPoolFactory = SpringUtils.getBean(ThreadPoolFactory.class);
		threadPoolFactory.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				agoraJavaRecording.createChannel(appId, channelKey, channelName, uid, config);
			}
		});

		logger.info("create channel finished...");
	}

	void leaveChannel() {
		logger.info("leaveChannel: " + nativeHandle);
		agoraJavaRecording.leaveChannel(nativeHandle);
	}

	@Override
	public void nativeObjectRef(long nativeHandle) {
		this.onChannel = true;

		logger.info("nativeObjectRef: " + nativeHandle);
		this.nativeHandle = nativeHandle;
	}

	@Override
	public void onLeaveChannel(int reason) {
		this.onChannel = false;
	}

	@Override
	public void onError(int error, int statCode) {

	}

	@Override
	public void onWarning(int warn) {

	}

	@Override
	public void onUserOffline(long uid, int reason) {

	}

	@Override
	public void onUserJoined(long uid, String recordingDir) {
		// 用户加入到频道才有视频会录制出来
	}

	@Override
	public void audioFrameReceived(long uid, int type, Common.AudioFrame frame) {

	}

	@Override
	public void videoFrameReceived(long uid, int type, Common.VideoFrame frame, int rotation) {

	}

	@Override
	public void stopCallBack() {
		logger.info("stopCallBack...");
		threadPoolFactory.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				if (recordingPath != null && recordingPath.length() > 0) {
					if (!recordingPath.endsWith("/")) {
						recordingPath += "/";
					}
					Future<String> future = threadPoolFactory.getExecutor().submit(new Callable<String>() {
						@Override
						public String call() throws Exception {
							for (int i = 0; i < LOOP_COUNT; i++) {
								// 判断recordingPath目录下是否有recording2-done.txt
								File dictionary = new File(recordingPath);
								String[] fileNames = dictionary.list();
								if (fileNames != null) {
									for (String fileName : fileNames) {
										if (fileName.startsWith("recording") && fileName.endsWith("done.txt")) {
											return "record success";
										}
									}
								}
								Thread.sleep(SLEEP_TIME);
							}
							return "can not find file 'recording**done.txt'";
						}
					});
					try {
						String result = future.get(SLEEP_TIME * LOOP_COUNT, TimeUnit.MILLISECONDS);
						logger.info(result);
					} catch (InterruptedException | ExecutionException | TimeoutException e) {
						logger.error(e.getMessage(), e);
					}
					try {
						String command = "python ../../tools/video_convert.py -f " + recordingPath;
						logger.info("execute command: " + command);
						Process process = Runtime.getRuntime().exec(command);
						int exitValue = process.waitFor();
						if (0 != exitValue) {
							logger.error("call shell failed. error code is :" + exitValue);
						}
						// 判断recordingPath目录下是否有convert-done.txt
						File file = new File(recordingPath + "convert-done.txt");
						if (file.exists()) {
							logger.info("convert success");
						} else {
							logger.error("convert fail");
						}
					} catch (IOException | InterruptedException e) {
						logger.error(e.getMessage(), e);
					}
				}
				// 回调接口
				try {
					Event reqEvent = EventUtils.createRequestEvent(1003301, MapUtils.newHashMap("channel_name", "recording_path",
							channelName, recordingPath));
					Event resEvent = routerClient.postSynEvent("router", reqEvent);
					if (resEvent.getReturnCode() != EventReturnCode.I_OK) {
						logger.error("回调失败: " + resEvent);
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
				// 结束
				if (onFinishListener != null) {
					onFinishListener.onFinish(channelName);
				}
			}
		});
	}

	@Override
	public void recordingPathCallBack(String path) {
		logger.info("recordingPathCallBack：" + path);
		recordingPath = path;
	}

	public boolean isOnChannel() {
		return onChannel;
	}

	public void setOnChannel(boolean onChannel) {
		this.onChannel = onChannel;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public int getClientUid() {
		return clientUid;
	}

	public void setClientUid(int clientUid) {
		this.clientUid = clientUid;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getChannelKey() {
		return channelKey;
	}

	public void setChannelKey(String channelKey) {
		this.channelKey = channelKey;
	}

	public String getAppLiteDir() {
		return appLiteDir;
	}

	public void setAppLiteDir(String appLiteDir) {
		this.appLiteDir = appLiteDir;
	}

	public String getRecordRootDir() {
		return recordRootDir;
	}

	public void setRecordRootDir(String recordRootDir) {
		this.recordRootDir = recordRootDir;
	}

	interface OnFinishListener {
		/**
		 * 当结束录制的时候回调
		 *
		 * @param channelName 频道名称
		 */
		void onFinish(String channelName);

		void onUserJoined(String channelName);
	}
}