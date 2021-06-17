import com.chebei.ams.processor.ext.utils.MapUtils;
import com.chebei.record.util.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * 功能说明：<h1></h1>
 * 系统名称：<br>
 * 模块名称：com.chebei.record.service<br>
 * 系统版本：V1.0.0<br>
 * 开发人员：Aaron.Zhang<br>
 * 开发时间：2018-02-05 16:26<br>
 * 功能描述：<br>
 * @author Aaron.Zhang
 */
@Qualifier
public class AgoraService extends BaseService implements AgoraRecordRunner.OnFinishListener {

	private static Logger logger = LoggerFactory.getLogger(BaseService.class);

	private static final Integer NONE_USER = 0;

	private static final Integer HAS_USER = 1;

	private Map<String, AgoraRecordRunner> map;

	private Map<String, Integer> userJoinedStatusMap;

	private String appId;

	private int uid;

	private String channel;

	private String channelKey;

	@Value("${app.lite.dir}")
	private String appLiteDir;

	@Value("${record.root.dir}")
	private String recordRootDir;

	public AgoraService() {
		map = new HashMap<>();
		userJoinedStatusMap = new HashMap<>();
	}

	public Map<String, Object> createChannel(Map<String, Object> paramMap) {
		String channelName = MapUtils.getString(paramMap, "channel_name");
		AgoraRecordRunner agoraRecordRunner = map.get(channelName);
		if (agoraRecordRunner != null) {
			return fail(-1, "已经有相同channelName在录制");
		}
		agoraRecordRunner = new AgoraRecordRunner(this);
		map.put(channelName, agoraRecordRunner);
		userJoinedStatusMap.put(channelName, NONE_USER);
		try {
			agoraRecordRunner.setAppId(MapUtils.getString(paramMap, "app_id"));
			agoraRecordRunner.setUid(MapUtils.getIntValue(paramMap, "uid"));
			agoraRecordRunner.setClientUid(MapUtils.getIntValue(paramMap, "client_uid"));
			agoraRecordRunner.setChannelName(channelName);
			agoraRecordRunner.setChannelKey(MapUtils.getString(paramMap, "channel_key"));
			agoraRecordRunner.setAppLiteDir(appLiteDir);
			agoraRecordRunner.setRecordRootDir(recordRootDir);

			agoraRecordRunner.createChannel();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return fail(-1, "create channel failed...");
		}
		return success(0, "");
	}

	public Map<String, Object> leaveChannel(Map<String, Object> paramMap) {
		String channelName = MapUtils.getString(paramMap, "channel_name");
		AgoraRecordRunner agoraRecordRunner = map.get(channelName);
		if (agoraRecordRunner == null) {
			return fail(-1, "没有该channelName在录制");
		}
		try {
			agoraRecordRunner.leaveChannel();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return fail(-1, "leave channel failed...");
		}
		return success(0, "");
	}

	public Map<String, Object> hasUserJoined(Map<String, Object> paramMap) {
		String channelName = MapUtils.getString(paramMap, "channel_name");
		AgoraRecordRunner agoraRecordRunner = map.get(channelName);
		if (agoraRecordRunner == null) {
			return fail(-1, "没有该channelName在录制");
		}
		Integer userJoined = userJoinedStatusMap.get(channelName);
		if (userJoined == null) {
			return fail(-1, "没有该channelName在录制");
		}

		return success(0, "", "user_joined", String.valueOf(userJoined));
	}

	/**
	 * 当结束录制的时候回调
	 *
	 * @param channelName 频道名称
	 */
	@Override
	public void onFinish(String channelName) {
		if (map.containsKey(channelName)) {
			map.put(channelName, null);
			userJoinedStatusMap.put(channelName, null);
		}
	}

	/**
	 * 当有用户加入频道的时候回调
	 *
	 * @param channelName 频道名称
	 */
	@Override
	public void onUserJoined(String channelName) {
		if (userJoinedStatusMap.containsKey(channelName)) {
			userJoinedStatusMap.put(channelName, HAS_USER);
		}
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

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
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

}