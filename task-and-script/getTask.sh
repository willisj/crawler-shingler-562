if [ -d "task" ]; then
  echo "Please delete the old task directory first."
  exit
fi

echo "Deleting old temp.. "
rm -r tasktemp 2> /dev/null

echo "creating temporary directory"
mkdir tasktemp

cd tasktemp

echo "Downloading jar"
wget https://github.com/willisj/crawler-shingler-562/raw/master/GridToolset/MultiTool.jar

echo "Downloading demo script ..."
wget https://raw.github.com/willisj/crawler-shingler-562/master/GridToolset/run.sh 

echo "Downloading the test file"
wget https://raw.github.com/willisj/crawler-shingler-562/master/GridToolset/1591C3BFB24BC0E6F799AC27CEF9533C.pgf

echo "Setting up crawler test"
echo "http://uwindsor.ca" > urlPool
touch domainOutput

cd ..

echo "Setting script +x ..."
chmod +x tasktemp/run.sh

echo "renaming tasktemp folder to task "
mv tasktemp task

echo "Done!"
