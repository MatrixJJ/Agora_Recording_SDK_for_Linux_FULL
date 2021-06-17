#########################################################################  
# 守护进程脚本
#########################################################################  
#!/bin/bash  
num=1  
iNum=1  
echo $$  
while(( $num < 5 ))  
do  
	sn=`ps -ef | grep AgoraRecordRun2 | grep -v grep |awk '{print $2}'`
	echo $sn  
	if [ "${sn}" = "" ]    #如果为空,表示进程未启动
	then
		let "iNum++"  
		echo $iNum    
		./execute.sh start
		echo restart ok !
	else
		echo running !
	fi
sleep 5 
done