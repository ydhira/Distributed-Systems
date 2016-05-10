#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <limits.h>
#include <time.h>

int totalNoPoints;
int noOfClusters;

//Takes in 2 strings and returns the difference of characters in them
int differenceString(char dnaStrand[], char centroid[]){
	
	int i ;
	int count = 0 ;
	for ( i=0 ; i< 10 ; i ++){
		if (dnaStrand[i] != centroid[i]){
			count ++ ;
		}
	}
	return count;
}

//returns the kmeans for the DNA strands 
void kMeans( char* allDnaStrands[], char* allCentroids[]){

	int * membership = malloc( sizeof(int) * totalNoPoints);
	int threshold= 0;
	while( threshold <5){
		//For every point, see which centroid is near to it
		int i, j ;
		int n = 0 ;
		for (i =0 ; i< totalNoPoints ; i++){
			
			char* dnaStrand = allDnaStrands[i];
			int dmin = 5;
			
			for (j =0 ; j< noOfClusters ; j++){
				
				char * centroid = allCentroids[j];
				int distance = differenceString(dnaStrand , centroid) ;
				if (distance < dmin){
					dmin = distance;
					n = j; 
				}
			}
			if (membership[i] != n){
					membership[i]=n;
			}		
		}
		
		//All the stands belong to a centroid now 
		int k =0; 
		
		for (k =0; k<noOfClusters; k++){
			int p = 0;
			int size = 0;
			
			char** PointsinCluster = malloc( sizeof(char*) * totalNoPoints ) ;
			for (i=0; i< totalNoPoints ;i++){
				PointsinCluster[i] = malloc(11 * sizeof(char));
			}
			for (p=0; p< totalNoPoints; p++){
				if (membership[p] == k){
					PointsinCluster[size] = allDnaStrands[p];
					size++;
				}
			}
			
			//I know what Strands belong to a centroi. Take an average over them 
			int m= 0;
			int i =0 ;
			char* newCentroid = malloc(11 * sizeof(char)); 
			newCentroid[10] = '\0';
			for ( i =0 ; i < 10 ; i++){
				int numA = 0;
				int numT = 0;
				int numC = 0;
				int numG = 0;
				for (m=0 ; m < size; m++){

					if ( PointsinCluster[m][i]  == 'a'){
						numA ++;
					 }
					 if ( PointsinCluster[m][i] == 't'){
						numT ++;
					 }
					 if ( PointsinCluster[m][i] == 'c'){
						numC ++;
					 }if ( PointsinCluster[m][i] == 'g'){
						numG ++;
					 }
				}
				
				//got the first indices of everyone now
				//See which chars occurs most
				if ((numG > numC) && (numG > numA) && (numG > numT)){
					newCentroid[i] = 'g';
				}
				
				else if ((numA >= numC) && (numA >= numG) && (numA >= numT)){
					newCentroid[i] = 'a';
				}
				
				else if ((numC >= numA) && (numC >= numG) && (numC >= numT)){
					newCentroid[i] = 'c';
					
				}
				
				else if ((numT >= numC) && (numT >= numG) && (numT >= numA)){
					newCentroid[i] = 't';
					
				}
				
			}
			//Set the new centroid just calculated
			allCentroids[k] = newCentroid;
			
			for (i=0; i< totalNoPoints ;i++){
				free(PointsinCluster[i]);
			}

			free(PointsinCluster);
		}
		threshold++;
	}
	
	free(membership);
	
	
	int test =0 ;
	for (test =0; test < noOfClusters ; test++){  
		printf("For cluster %d, the centroid is %s \n", test,  allCentroids[test]);
	}

}

int main (int argc , char* argv[]){
	clock_t begin, end;
	double time_spent;
	begin = clock();
	if (argc != 7){
		if (strcmp(argv[0],"-h")){
			printf("./seq_kmean_dna -n <noofpoints> -c <noofclusters> -f <inputFile> \n");
			return 0;
		}
		else{
			printf("wrong no. of arguments\n");
			return -1;
		}
	}
	
	totalNoPoints = atoi(argv[2]);
	noOfClusters = atoi(argv[4]);
	char*  inFileName = argv[6];
	
	FILE *infile =  fopen(inFileName, "r" );
	if ( infile ==NULL){
		perror("File doesnt exist. \n");
		exit(EXIT_FAILURE);
	}
	
	char **allDnaStrands;
	char **allCentroids;
	
	allDnaStrands = malloc (totalNoPoints * sizeof(char*));
	int i;
	for (i=0; i< totalNoPoints ;i++){
		allDnaStrands[i] = malloc(11 * sizeof(char));
	}
	
	
	int added = 0;
	char line [100];
	//read from the file
	while ( (fgets(allDnaStrands[added], 100, infile))!=NULL){
		added++;
	}	
	
	for (added =0 ; added<totalNoPoints ; added ++) { 
		allDnaStrands[added][10] = '\0';
	}
	

	fclose(infile);
	allCentroids = malloc (noOfClusters * sizeof(char*));
	for (i=0; i< noOfClusters ;i++){
		allCentroids[i] = malloc(11 * sizeof(char));
	}
	int perCluster = totalNoPoints / noOfClusters;
	//choose the centroids depending on the number of clusters 
	//and then get the middle element from there 
	for (i =0; i<noOfClusters; i++){
		allCentroids[i] = allDnaStrands[(perCluster/2) + (i*perCluster)];
	}
	
	//run the kMeans function
	kMeans(allDnaStrands, allCentroids);
	
	free(allDnaStrands);
	free(allCentroids);
	
	end = clock();
	time_spent = (double) (end - begin)/CLOCKS_PER_SEC;
	printf("Time spent is: %f \n", time_spent);
}