#!/bin/bash
OLDIFS=$IFS
IFS=$(echo -en "\n\b")

shingleFileDirectoryName=shingleDir
compareFileDirectoryName=compareDir
workDir=workDir
jarpath=/home/jordan/globus/task/MultiTool.jar
runningCount=0
runningConcat=""
comparesPerCluster=20
fileCount=0

if [ $# -ne 1 ]; then
	echo "please specify a path."	
	exit
fi

for shingleFile in $(find "$1/$shingleFileDirectoryName" -type f)
do
	files[$fileCount]=$( echo $shingleFile | grep -Eio "[a-z0-9]*\.shg" | sed "s/\.shg//")
	fileCount=`expr $fileCount + 1`
done

for i in `seq 0 $fileCount`;
do
	for(( j=$i; j<$fileCount; j++))
	do
		cmpFileName=${files[$i]}-${files[$j]}.cmp
		if [ ! -e $cmpFileName ]; then
			
			runningConcat="$runningConcat ${files[$i]},${files[$j]}"
			runningCount=`expr $runningCount + 1`
	echo rc $runningCount
			if [ $runningCount = $comparesPerCluster ]; then
				## SCHElDULE THIS
				
				echo "./compareGroup.sh $1 $runningConcat"

				runningCount=0
				runningConcat=""
				
			fi
		fi
	done 
done 

if [ $runningCount != 0 ]; then
	## SCHEDULE THIS
	echo $1 $runningConcat
	echo "./compareGroup.sh $1 $runningConcat"
fi
echo $fileCount
IFS=$OLDIFS
