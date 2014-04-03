#!/bin/bash
OLDIFS=$IFS
IFS=$(echo -en "\n\b")

shingleFileDirectoryName=shingleDir
compareFileDirectoryName=compareDir
jarpath=/home/jordan/globus/task/MultiTool.jar
shingleSize=7

echo entered

if [ $# = 0 ]; then
	echo "please specify a list of file pairs."
	echo "./compare.sh file1,file2 file1,file3 file2,file3"
	exit
fi


first=1
for path in "$@"
do
	if [ $first = 1 ]; then
		first=0
		baseDir=$1
		continue
	fi


	path1="$baseDir/$shingleFileDirectoryName/`echo $path | cut -f1 -d','`.shg"
	path2="$baseDir/$shingleFileDirectoryName/`echo $path | cut -f2 -d','`.shg"

	echo $path1 $path2
	fn1=`echo $path | cut -f1 -d','`
	fn2=`echo $path | cut -f2 -d','`	

	compareFile="$baseDir/$compareFileDirectoryName/"$fn1-$fn2".cmp"
	compareFile=$(echo $compareFile | sed "s/$shingleFileDirectoryName/$compareFileDirectoryName/")

	if [ ! -e compareFile ]; then
		java -jar $jarpath compare $path1 $path2 > $compareFile 
	fi

done
IFS=$OLDIFS
