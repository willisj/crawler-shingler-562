#!/bin/bash  

set -x #echo on

#take the input file and shingle it
java -jar MultiTool.jar shingle testFile 7 > allShingles

# peek at the whole file and see how many shingles we got (33)
cat allShingles | nl | tail -n 1

# grab some shingles with overlap for two files
cat allShingles | head -n 20 > shingle1
cat allShingles | tail -n 20 > shingle2

java -jar MultiTool.jar compare shingle1 shingle2
