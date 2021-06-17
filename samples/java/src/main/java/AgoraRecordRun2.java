import com.chebei.ams.server.AmsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;

import java.io.IOException;

/**
 * 功能说明：<br>
 * 系统名称：<br>
 * 模块名称：com.chebei.record<br>
 * 系统版本：V1.0.0<br>
 * 开发人员：Aaron.Zhang<br>
 * 开发时间：2017-02-14 20:28<br>
 * 功能描述：<br>
 */
@Configuration
@Controller
public class AgoraRecordRun2 {

	private final static Logger logger = LoggerFactory.getLogger(AgoraRecordRun2.class);

	public static void main(String[] args) throws IOException {

		AmsServer.main(null);

		logger.info("Chebei-Record2-Server Starts Successfully...");
	}
}