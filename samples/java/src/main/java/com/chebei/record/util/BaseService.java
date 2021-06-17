package com.chebei.record.util;

import com.chebei.ams.client.sdk.inst.AmsClient;
import com.chebei.ams.client.sdk.util.AmsClientUtil;
import com.chebei.ams.message.event.Event;
import com.chebei.ams.processor.ext.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Map;

public abstract class BaseService {

	static Log log = LogFactory.getLog(BaseService.class);

	static final Integer RETCODE_PARAM_MISS = -106;

	static final String ERRMSG_PARAM_MISS = "param %s is missed";

	public static final String ERROR_NO = "error_no";

	public static final String ERROR_INFO = "error_info";

	@Autowired
	private AmsClient routerClient;

	Map<String, Object> paramBlankCheck(Map<String, Object> paramMap, String... params) {
		for (int i = 0; i < params.length; i++) {
			String paramValue = MapUtils.getString(paramMap, params[i]);
			if (StringUtils.isBlank(paramValue)) {
				return fail(RETCODE_PARAM_MISS, String.format(ERRMSG_PARAM_MISS, params[i]));
			}
		}
		return null;
	}

	protected boolean check(Map<String, Object> paramMap) {
		if (paramMap == null) {
			return false;
		}
		Object errorNo = paramMap.get(ERROR_NO);
		boolean ret = false;
		if (errorNo == null) {
			ret = true;
		} else if ("0".equals(errorNo) || errorNo.equals(0)) {
			ret = true;
		}
		return ret;
	}

	public static final Map<String, Object> successMap = Collections.unmodifiableMap(MapUtils.newHashMap(ERROR_NO, ERROR_INFO, 0, "执行成功"));

	public static final Map<String, Object> failMap = Collections.unmodifiableMap(MapUtils.newHashMap(ERROR_NO, ERROR_INFO, -1, "执行失败"));

	public static Map<String, Object> fail() {
		return fail();
	}

	public Map<String, Object> fail(Integer retCode, String errMsg) {
		return MapUtils.newHashMap(ERROR_NO, ERROR_INFO, retCode, errMsg);
	}

	public Map<String, Object> fail(Integer retCode, String errMsg, String... args) {
		Map<String, Object> retMap = MapUtils.newHashMap(ERROR_NO, ERROR_INFO, retCode, errMsg);
		Map<String, Object> otherParamMap = MapUtils.newHashMap(args);
		retMap.putAll(otherParamMap);
		return retMap;
	}

	protected static Map<String, Object> success() {
		return successMap;
	}

	protected Map<String, Object> success(Integer errorNo, String errorInfo) {
		Map<String, Object> retMap = MapUtils.newHashMap(ERROR_NO, ERROR_INFO, errorNo, errorInfo);
		return retMap;
	}

	protected Map<String, Object> success(Integer errorNo, String errorInfo, String... args) {
		Map<String, Object> retMap = MapUtils.newHashMap(ERROR_NO, ERROR_INFO, errorNo, errorInfo);
		Map<String, Object> otherParamMap = MapUtils.newHashMap(args);
		retMap.putAll(otherParamMap);
		return retMap;
	}

	protected Map<String, Object> getDataFromRouter(int funcId, Map<String, Object> reqMap) {
		try {
			return AmsClientUtil.sendReceive(routerClient, "router", funcId, reqMap);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	protected Event getEventFromRouter(int funcId, Map<String, Object> reqMap) {
		try {
			return AmsClientUtil.postSynEvent(routerClient, "router", funcId, reqMap);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	protected Event getEventFromRouter(Event event) {
		try {
			return routerClient.postSynEvent("router", event);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}
}