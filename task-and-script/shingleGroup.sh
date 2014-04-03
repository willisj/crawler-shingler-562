#!/bin/bash
OLDIFS=$IFS
IFS=$(echo -en "\n\b")


# usage: ./shingleGroup.sh <path/to/file/1.pgf> <path/to/file/2.pgf> ... <path/to/file/n.pfg>

#	prints out the names of created files
#	writes files to the shingle file directory with a .shg extension
#	assumes that the page and shingle file directories are siblings
#

pageFileDirectoryName=crawlDir
shingleFileDirectoryName=shingleDir
jarpath=/home/jordan/globus/task/MultiTool.jar
shingleSize=7

if [ $# = 1 ]; then
	echo "please specify a path."	
	exit
fi

for path in "$@"
do
	shingleFile=$(echo $path | sed "s/$pageFileDirectoryName/$shingleFileDirectoryName/" | sed "s/\.pgf$/\.shg/");

	if [ ! -e `dirname $path` ]; then
		mkdir `dirname $path`
	fi

	if [ ! -e $shingleFile ]; then
		( java -jar $jarpath shingle $path $shingleSize ) > $shingleFile
		echo $path
	fi
done
IFS=$OLDIFS
