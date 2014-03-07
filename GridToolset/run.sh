#!/bin/bash  

set -x #echo on

#take the input file and shingle it
java -jar MultiTool.jar shingle 1591C3BFB24BC0E6F799AC27CEF9533C.pgf 7 > allShingles

# peek at the whole file and see how many shingles we got (33)
cat allShingles | nl | tail -n 1

# grab some shingles with overlap for two files
cat allShingles | head -n 20 > 1591C3BFB24BC0E6F799AC27CEF9533C_A
cat allShingles | tail -n 20 > 1591C3BFB24BC0E6F799AC27CEF9533C_B

java -jar MultiTool.jar compare 1591C3BFB24BC0E6F799AC27CEF9533C_A 1591C3BFB24BC0E6F799AC27CEF9533C_B
