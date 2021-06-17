#!/bin/sh
#该脚本为Linux下启动java程序的通用脚本。
#即可以作为开机自启动service脚本被调用，也可以作为启动java程序的独立脚本来使用。
#
#警告!!!：该脚本stop部分使用系统kill命令来强制终止指定的java程序进程。
#在杀死进程前，未作任何条件检查。在某些情况下，如程序正在进行文件或数据库写操作，
#可能会造成数据丢失或数据不完整。如果必须要考虑到这类情况，则需要改写此脚本，
#增加在执行kill命令前的一系列检查。
#
#
###################################
#环境变量及程序执行参数
#需要根据实际环境以及Java程序名称来修改这些参数
###################################
#JDK所在路径
if [ -z ${JAVA_HOME} ]; then
	echo "JAVA_HOME is undefined, use default"
	JAVA_HOME="/usr/java/jdk1.8.0_144"
fi

#执行程序启动所使用的系统用户，考虑到安全，推荐不使用root帐号
RUNNING_USER=chebei

#Java程序所在的目录（classes的上一级目录）
cd `dirname $0`
#JNI路径
JNI_PATH=${JAVA_HOME}/include
export JNI_PATH

APP_HOME=./bin
#APP_HOME=/home/geelu/geelu-proxy

LD_LIBRARY_PATH=${APP_HOME}
export LD_LIBRARY_PATH

#需要启动的Java主程序（main方法类）
APP_MAIN_CLASS=AgoraRecordRun2

#应用启动参数
APP_ARGUMENTS=$2

#拼凑完整的classpath参数，包括指定lib目录下所有的jar
CLASSPATH=${CLASSPATH}:${APP_HOME}:${APP_HOME}/class:${APP_HOME}/classes:${APP_HOME}/conf
for i in ${APP_HOME}/*.jar;do
	CLASSPATH=${CLASSPATH}:${i}
done
for j in ${APP_HOME}/lib/*.jar; do
	CLASSPATH=${CLASSPATH}:${j}
done

#java虚拟机启动参数
JAVA_OPTS=""
JAVA_OPTS=${JAVA_OPTS}"-server -Xmx4096M -Xms4096M -XX:MaxNewSize=2048M "
JAVA_OPTS=${JAVA_OPTS}"-Djava.awt.headless=true -verbose:gc -Xloggc:logs/gc.log -XX:+PrintGCTimeStamps -XX:+PrintGCDetails"
#JAVA_OPTS=${JAVA_OPTS}"-server -Xmx1g -Xms512m -XX:SurvivorRatio=3 -XX:PermSize=128m -Xss256k -XX:+DisableExplicitGC -XX:+UseParNewGC "
#JAVA_OPTS=${JAVA_OPTS}"-XX:+CMSParallelRemarkEnabled -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection "
#JAVA_OPTS=${JAVA_OPTS}"-XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseFastAccessorMethods "
#JAVA_OPTS=${JAVA_OPTS}"-XX:SurvivorRatio=4 -XX:LargePageSizeInBytes=10m -XX:CompileThreshold=10000"

###################################
#(函数)判断程序是否已启动
#
#说明：
#使用JDK自带的JPS命令及grep命令组合，准确查找pid
#jps 加 l 参数，表示显示java的完整包路径
#使用awk，分割出pid ($1部分)，及Java程序名称($2部分)
###################################
#初始化psId变量（全局）
psId=0

checkPid() {
	javaPs=`${JAVA_HOME}/bin/jps -l | grep ${APP_MAIN_CLASS}`

	if [ -n "$javaPs" ]; then
		psId=`echo ${javaPs} | awk '{print $1}'`
	else
		psId=0
	fi
}

daemonPid=0
checkDaemonPid() {
	daemonPs=`ps -ef | grep ./daemon.sh | grep -v grep`

	if [ -n "$daemonPs" ]; then
                daemonPid=`echo ${daemonPs} | awk '{print $2}'`
        else
                daemonPid=0
        fi
}

###################################
#(函数)启动程序
#
#说明：
#1. 首先调用checkPid函数，刷新$psId全局变量
#2. 如果程序已经启动（$psId不等于0），则提示程序已启动
#3. 如果程序没有被启动，则执行启动命令行
#4. 启动命令执行后，再次调用checkPid函数
#5. 如果步骤4的结果能够确认程序的pid,则打印[OK]，否则打印[Failed]
#注意：echo -n 表示打印字符后，不换行
#注意: "nohup 某命令 >/dev/null 2>&1 &" 的用法
###################################
start() {
	checkPid

	if [ ${psId} -ne 0 ]; then
		echo "================================"
		echo "warn: $APP_MAIN_CLASS already started! (pid=$psId)"
		echo "================================"
	else
		echo -n "Starting $APP_MAIN_CLASS ..."
		JAVA_CMD="nohup $JAVA_HOME/bin/java $JAVA_OPTS  -classpath $CLASSPATH $APP_MAIN_CLASS $APP_ARGUMENTS >/dev/null 2>&1 &"
		#su ${RUNNING_USER} -c "$JAVA_CMD"
		eval "${JAVA_CMD}"
		checkPid
		if [ ${psId} -ne 0 ]; then
			echo "(pid=$psId) [OK]"
			checkDaemonPid
			if [ ${daemonPid} -ne 0 ]; then
				echo "daemon process already exists"
			else
				# 启动守护进程
                        	nohup ./daemon.sh >/dev/null 2>&1 &
                        	checkDaemonPid
                        	if [ ${daemonPid} -ne 0 ]; then
                                	echo "(daemonPid=$daemonPid) [OK]"
                        	else
                                	echo "[Daemon Failed]"
                       		fi
			fi
		else
			echo "[Failed]"
		fi
	fi
}

###################################
#(函数)停止程序
#
#说明：
#1. 首先调用checkPid函数，刷新$psId全局变量
#2. 如果程序已经启动（$psId不等于0），则开始执行停止，否则，提示程序未运行
#3. 使用kill -9 pid命令进行强制杀死进程
#4. 执行kill命令行紧接其后，马上查看上一句命令的返回值: $?
#5. 如果步骤4的结果$?等于0,则打印[OK]，否则打印[Failed]
#6. 为了防止java程序被启动多次，这里增加反复检查进程，反复杀死的处理（递归调用stop）。
#注意：echo -n 表示打印字符后，不换行
#注意: 在shell编程中，"$?" 表示上一句命令或者一个函数的返回值
###################################
stop() {
	checkPid

	if [ ${psId} -ne 0 ]; then
		echo -n "Stopping $APP_MAIN_CLASS ...(pid=$psId) "
		#su - ${RUNNING_USER} -c "kill -9 $psId"
		eval "kill -9 ${psId}"
		if [ $? -eq 0 ]; then
			echo "[OK]"
		else
			echo "[Failed]"
		fi

		checkPid
		if [ ${psId} -ne 0 ]; then
			stop
		fi

		checkDaemonPid
		if [ ${daemonPid} -ne 0 ]; then
                	echo "killing daemon process..."
			eval "kill -9 ${daemonPid}"
			if [ $? -eq 0 ]; then
				echo "kill daemon process ok"
			else
				echo "kill daemon process failed!!!!"
			fi
                else
                        echo "daemon process not exist"
                fi
	else
		echo "================================"
		echo "warn: $APP_MAIN_CLASS is not running"
		echo "================================"
	fi
}

###################################
#(函数)检查程序运行状态
#
#说明：
#1. 首先调用checkPid函数，刷新$psId全局变量
#2. 如果程序已经启动（$psId不等于0），则提示正在运行并表示出pid
#3. 否则，提示程序未运行
###################################
status() {
	checkPid

	if [ ${psId} -ne 0 ];  then
		echo "$APP_MAIN_CLASS is running! (pid=$psId)"
	else
		echo "$APP_MAIN_CLASS is not running"
	fi
}

###################################
#(函数)打印系统环境参数
###################################
info() {
	echo "System Information:"
	echo "****************************"
	echo `head -n 1 /etc/issue`
	echo `uname -a`
	echo
	echo "JAVA_HOME=$JAVA_HOME"
	echo `${JAVA_HOME}/bin/java -version`
	echo
	echo "APP_HOME=$APP_HOME"
	echo "APP_MAIN_CLASS=$APP_MAIN_CLASS"
	echo "****************************"
}

###################################
#读取脚本的第一个参数($1)，进行判断
#参数取值范围：{start|stop|restart|status|info}
#如参数不在指定范围之内，则打印帮助信息
###################################
case "$1" in
	'start')
		start
		;;
	'start-init')
		APP_ARGUMENTS="init"
		start
		;;
	'stop')
		stop
		;;
	'restart')
		stop
		start
		;;
	'status')
		status
		;;
	'info')
		info
		;;
	*)
		echo "Usage: $0 {start|start-init|stop|restart|status|info}"
		exit 1
		;;
esac
exit 0
