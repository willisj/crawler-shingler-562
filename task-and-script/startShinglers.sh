#!/bin/bash
OLDIFS=$IFS
IFS=$(echo -en "\n\b")

pageFileDirectoryName=crawlDir
shingleFileDirectoryName=shingleDir
workDir=workDir
jarpath=/home/jordan/globus/task/MultiTool.jar
runningCount=0
runningConcat=""
pagesPerCluster=20

if [ $# -ne 1 ]; then
	echo "please specify a path."	
	exit
fi


for dir in $(find "$1/$pageFileDirectory" -type d)
do
	for pageFile in $(echo `find $dir -type f` |grep \.pgf$| tr ' ' '\n')
	do
		shingleFile=$(echo $pageFile | sed "s/$pageFileDirectoryName/$shingleFileDirectoryName/");
		if [ ! -e $shingleFile ]; then
			echo $shingleFile
			if [ $runningCount = $pagesPerCluster ]; then
				runningCount=0

				## SEND THESE COMMANDS TO THE SCHEDULER
				./shingleGroup.sh $runningConcat
				runningConcat=""				
			fi
			runningConcat="$runningConcat $pageFile"
			runningCount=`expr $runningCount + 1`
		fi 
	done
done


if [ $runningCount != 0 ]; then
	## SEND THESE COMMANDS TO THE SCHEDULER
	./shingleGroup.sh $runningConcat
fi

IFS=$OLDIFS
