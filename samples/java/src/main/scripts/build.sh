#!/bin/bash
createBinFloder()
{
	binDir="bin"
	if [ -d "$binDir" ];then
		echo
	else
		mkdir "$binDir"
	fi
}
build_java()
{ 
  	createBinFloder
	if [ -z ${JAVA_HOME} ]; then
		echo "JAVA_HOME is undefined, use default"
		JAVA_HOME="/usr/java/jdk1.8.0_144"
	fi
   
	jniLayer="./native/jni"
	javaClassPath="./bin/class"
	#${JAVA_HOME}/bin/javac src/*.java -d bin -Xlint:unchecked
	${JAVA_HOME}/bin/javah -d $jniLayer -classpath $javaClassPath AgoraJavaRecording
}

build_cpp()
{
	make -f ./native/.makefile JNIINCLUDEPATH=$JNI_PATH
}
clean_java()
{
	rm -f bin/*.class
	rm -rf bin/io
}
clean_cpp()
{
	make clean -f ./native/.makefile
}
build()
{
	pre_set
	build_java
	build_cpp
	echo "build all done!"
}
clean()
{ 
	#clean_java
	clean_cpp
	echo "clean all done!"
}

pre_set()
{
	JNI_PATH=${JAVA_HOME}/include
	export JNI_PATH

	CLASSPATH=`pwd`/bin
	export CLASSPATH

	LD_LIBRARY_PATH=`pwd`/bin
	export LD_LIBRARY_PATH

	tar -xzvf ../../tools/ffmpeg.tar.gz -C ../../tools/
	chmod +x ../../tools/ffmpeg
	chmod +x ../../bin/AgoraCoreService
}

cmdhelp()
{
	echo "Usage:"
	echo "source build.sh pre_set jni_path"
	echo "$1 build"
	echo "$1 clean"

	#echo "$1 build_java"
	#echo "$1 build_cpp"
	#echo "$1 clean_java"
	#echo "$1 clean_cpp"
}
run()
{
	for param in $@
	do
	case $param in
		"build")
  			build
			;;
		"clean")
			clean
			;;
		"build_java")
			build_java
			;;
		"build_cpp")
			#build_cpp
			;;
		"clean_java")
			#clean_java
			;;
		"clean_cpp")
			#clean_cpp
			;;
	   *)
	   cmdhelp $0
	   ;;
	esac
	done
}
if [ $# -eq "0" ];then
	cmdhelp $0
elif [ $# -eq "1" ];then
	run $1
elif [ $# -eq "2" ];then
	if [ $1 == "pre_set" ];then
		pre_set $2
	else
		cmdhelp $0
	fi
else
	cmdhelp $0
fi