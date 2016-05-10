#!/bin/sh
#Number of Points
b=400000
#Number of Cluster
k=100
#total points generated = k * b
		echo ********GENERATING $b INPUT POINTS EACH IN $k CLUSTERS 
		python ./randomclustergen/generaterawdata.py -c $k  -p $b -o input/cluster.csv