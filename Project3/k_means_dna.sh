#!/bin/sh
#Number of Points per cluster 
b=100
#Number of Cluster
k=4
#no.of generating points = k * b
		echo ********GENERATING $b INPUT POINTS EACH IN $k CLUSTERS 
		python ./randomclustergen/dnaGenerator.py -c $k  -p $b -o input/DnaCluster.csv