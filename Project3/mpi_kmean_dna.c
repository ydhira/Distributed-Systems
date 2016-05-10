#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <mpi.h>
#include <stddef.h>

int totalNoPoints;
int noOfClusters;
int noOfProcessors;
int noElemsPerProc;

typedef struct{
	int sumA;
	int sumT;
	int sumC;
	int sumG;
} myTuple;

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


//returns the Kmeans for the points given to the Slave
void kMeansSlaves(int m, char* allDnaStrands [], char allCentroids[][m], int myRank){
	int y =0 ;

	int * membership = malloc( sizeof(int) * noElemsPerProc);
		
	//For every point, see which centroid is near to it
	int i, j ;
	int n = 0;
	for (i =0 ; i< noElemsPerProc ; i++){
		
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
		//change the membership for this point if it near to the j centroid
		if (membership[i] != n){
			membership[i] = n;
		}		
	}
	
	//All the stands belong to a centroid now 
	int k =0;
	
	for (k =0; k<noOfClusters; k++){
		myTuple tuplesPerCluster[10];
		int p = 0;
		int l = 0;
		int size = 0;
		
		char** PointsinCluster = malloc( sizeof(char*) * noElemsPerProc ) ;
		
		for (i=0; i< noElemsPerProc ;i++){
			PointsinCluster[i] = malloc(11 * sizeof(char));
		}
		
		
		
		for (p=0; p< noElemsPerProc; p++){
			if (membership[p] == k){
				PointsinCluster[l] = allDnaStrands[p];
				size++;
				l++;
			}
		}
		
		
		
		//I know what Strands belong to a centroid. Take an average over them 
		int m= 0;
		int i =0 ;
		char* newCentroid = malloc(11 * sizeof(char)); 
		
		//For every index, and for every point in that cluster, 
		//see which character occurs the most at that index, 
		//and choose that as the new character in the new centroid
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
			
			//send the sum of the characters forevery index and for every centroid			
			MPI_Send(&numA, 1, MPI_INT, 0, myRank, MPI_COMM_WORLD); 
			MPI_Send(&numT, 1, MPI_INT, 0, myRank, MPI_COMM_WORLD); 
			MPI_Send(&numC, 1, MPI_INT, 0, myRank, MPI_COMM_WORLD);
			MPI_Send(&numG, 1, MPI_INT, 0, myRank, MPI_COMM_WORLD);
		}
		
		free(PointsinCluster);
	}
	
	free(membership);
}


//returns the kMeans for the Master points
myTuple** kMeansMaster(int m, char* allDnaStrands [], char allCentroids[][m], int myRank){
	int * membership = malloc( sizeof(int) * noElemsPerProc);
	int threshold= 0;
	
	myTuple** masterTuples = malloc (noOfClusters * sizeof(myTuple*)) ;
	
	//For every point, see which centroid is near to it

	int i, j; 
	int n = 0;
	for (i =0 ; i< noElemsPerProc ; i++){
		
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
		myTuple* tuplesPerCluster = malloc (10 * sizeof(myTuple));
		int p = 0;
		int l = 0;
		int size = 0;
		
		char** PointsinCluster = malloc( sizeof(char*) * noElemsPerProc ) ;
		
		for (i=0; i< noElemsPerProc ;i++){
			PointsinCluster[i] = malloc(11 * sizeof(char));
		}
		
		for (p=0; p< noElemsPerProc; p++){
			if (membership[p] == k){
				PointsinCluster[l] = allDnaStrands[p];
				size++;
				l++;
			}
		}
		
		//I know what Strands belong to a centroid. Take an average over them 
		int m= 0;
		
		int i =0 ;
		
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
			
			//Creates a new myTuple for the for 
			//every cluster and for every index 

			myTuple* mp = malloc( sizeof(myTuple));
			myTuple m = *mp;
			
			m.sumA = numA;
			m.sumT = numT;
			m.sumC = numC;
			m.sumG = numG;
		
			tuplesPerCluster[i] = m;
			
		}
		
		masterTuples[k] = tuplesPerCluster;
		free(PointsinCluster);
	}

	free(membership);
	return masterTuples;
}

int main (int argc , char* argv[]){
	
	if (argc != 9){
		if (strcmp(argv[0],"-h")){
			printf("./mpi_kmean_dna -n <noofpoints> -c <noofclusters>  -p <noOfProcessors> -f <inputFile> \n");
			return 0;
		}
		else{
			printf("wrong no. of arguments\n");
			return -1;
		}
	}
	
	totalNoPoints = atoi(argv[2]);
	noOfClusters = atoi(argv[4]);
	noOfProcessors = atoi(argv[6]);
	noElemsPerProc = (totalNoPoints / noOfProcessors) ;
	
	char*  inFileName = argv[8];
	
	
	int myRank;    
	MPI_Status status;
	
	MPI_Init(&argc, &argv);
	MPI_Comm_rank(MPI_COMM_WORLD, &myRank);
	
	char **allDnaStrands;

	char allCentroids[noOfClusters][11];
	
	allDnaStrands = malloc (totalNoPoints * sizeof(char*));

	int threshold = 0;
	while (threshold < 5){
		 
		if (myRank == 0){
			double endTime, startTime;
			if (threshold == 0){
				startTime = MPI_Wtime();
				FILE *infile =  fopen(inFileName, "r" );
				if ( infile ==NULL){
					perror("File doesnt exist. \n");
					exit(EXIT_FAILURE);
				}
				
				int added = 0;				
				char string[100];
				while ( fgets(string, 100, infile)!=NULL){

					string[10] = '\0';
					allDnaStrands[added] = malloc(11 * sizeof(char));
					allDnaStrands[added] = strdup(string);
					added++;
				}	
				
				for (added =0 ; added<totalNoPoints ; added ++) { 
					allDnaStrands[added][10] = '\0';
				}
				

				
				fclose(infile);

				
				int l, i;
				fflush(stdout); 
				for (i =0; i<noOfClusters ; i++){

					int perCluster = totalNoPoints / noOfClusters;
					for (l=0; l < 11; l++)
					{
						
						allCentroids[i][l] = allDnaStrands[(perCluster/2) + (i*perCluster)][l];
					}
				}

				
				int j;
				for(j=1; j<noOfProcessors; j++){
					int startIndex = j * noElemsPerProc;
					int endIndex = startIndex + noElemsPerProc;
					
					int  p =0 ; 
					for ( p =0 ; p < noElemsPerProc ; p++){
						int h =0 ; 
						for ( h = 0 ; h< 11 ; h++){
						   
							MPI_Send(&allDnaStrands[startIndex + p][h], 1, MPI_CHAR, j, j, MPI_COMM_WORLD); 
						}
					}
				}
			}

			/////////////////SEND THE Clusters Everytime NOW////////////////////////
			
			
			int j;
			for(j=1; j<noOfProcessors; j++){
				
				
				//Send all the centroid string one by one
				int l = 0;
				int y = 0;
				for ( l =0 ; l < noOfClusters ; l++){
					for ( y =0 ; y < 11 ; y++){
						MPI_Send(&allCentroids[l][y], 1, MPI_CHAR, j, j, MPI_COMM_WORLD);
					}
				}
			}
			
			/////////////////DO OWN KMEANS/////////////////////////////////
			
			char** masterArray = malloc(sizeof(char *) * noElemsPerProc );
			int i;
			for (i=0; i< noElemsPerProc ;i++){
				masterArray[i] = malloc(11 * sizeof(char));
			}
			int h;
			for (h=0; h< noElemsPerProc ;h++){
				masterArray[h] = allDnaStrands[h];
			}
			
			myTuple** masterTuples =malloc (noOfClusters * sizeof(myTuple*)) ;
			for ( i =0 ; i<noOfClusters ; i++){
				for ( h =0 ; h < 10 ;h ++){ 
					masterTuples[i] = malloc(10 * sizeof(myTuple));
				}
			}	
			
			masterTuples = kMeansMaster( 11, masterArray , allCentroids , myRank );
			
			int u,  v;

			///////////////////RECEIVE THE ELEMETNS////////////////////////
			int numA =0 ;
			int numT =0 ;
			int numC =0 ;
			int numG = 0;
			int index =0 ;
			int indexP =0;
			int clus  = 0;
			for (indexP =1 ; indexP <noOfProcessors ; indexP ++){
				for (clus =0 ; clus <noOfClusters; clus ++){
					for (index =0 ; index <10; index++){

						
						MPI_Recv(&numA, 1, MPI_INT, indexP, indexP, MPI_COMM_WORLD, &status);
						MPI_Recv(&numT, 1, MPI_INT, indexP, indexP, MPI_COMM_WORLD, &status);
						MPI_Recv(&numC, 1, MPI_INT, indexP, indexP, MPI_COMM_WORLD, &status);
						MPI_Recv(&numG, 1, MPI_INT, indexP, indexP, MPI_COMM_WORLD, &status);
						
						masterTuples[clus][index].sumA += numA;
						masterTuples[clus][index].sumT += numT;
						masterTuples[clus][index].sumC += numC;
						masterTuples[clus][index].sumG += numG;
						
					}
				}	
			}		
			
			////////////////////FIND NEW CENTROID//////////////////////////////////		
			int m= 0;
			int numAA, numTT, numCC, numGG;
			for ( j =0 ; j< noOfClusters ; j++){
				char* newCentroid = malloc(11 * sizeof(char));
				for ( i =0 ; i < 10 ; i++){
					numAA = masterTuples[j][i].sumA;
					numTT = masterTuples[j][i].sumT;
					numCC = masterTuples[j][i].sumC;
					numGG = masterTuples[j][i].sumG;
					
					if ((numGG > numCC) && (numGG > numAA) && (numGG > numTT)){
					

						newCentroid[i] = 'g';
						
					}
					
					else if ((numAA >= numCC) && (numAA >= numGG) && (numAA >= numTT)){
						

						newCentroid[i] = 'a';
						
					}
					
					else if ((numCC >= numAA) && (numCC >= numGG) && (numCC >= numTT)){
						

						newCentroid[i] = 'c';
						
					}
					
					else if ((numTT >= numCC) && (numTT >= numGG) && (numTT >= numAA)){

						newCentroid[i] = 't';
						
					}
					
				}
				newCentroid[10] = '\0';
				int l =0;
				for (l=0; l < 10; l++)
				{
					
					allCentroids[j][l] = newCentroid[l];
				}
			}
				
			if (threshold == 4){
				endTime = MPI_Wtime();
				int u;
				for ( u =0 ; u<noOfClusters ; u++){
					printf("Final Centroid is %s \n" , allCentroids[u]);
				}
				printf("Time Taken = %f\n",(endTime - startTime));
			}
		}
		
		//SLAVE
		else{
			char** recvBuff = malloc( sizeof(char*) * noElemsPerProc);
			int v;
			for (v=0; v< noElemsPerProc ;v++){
				recvBuff[v] = malloc(11 * sizeof(char));
			}
			char oneStrand;
			char cenI ; 	
			if (threshold == 0){
				int  p =0;
				for (p =0; p<noElemsPerProc ; p++){
					int h =0 ;
					for (h =0 ; h< 11 ; h++){
						MPI_Recv(&oneStrand, 1, MPI_CHAR, 0, myRank, MPI_COMM_WORLD, &status);
						
						recvBuff[p][h] = oneStrand;
					}
					
				}
				int v = 0;

			}
			
			int recC = 0 ;
			int j = 0; 
			for (recC = 0 ; recC < noOfClusters ; recC ++){
				for (j =0 ; j<11 ; j++){
					MPI_Recv(&cenI, 1, MPI_CHAR, 0, myRank, MPI_COMM_WORLD, &status);
					allCentroids[recC][j]  = cenI ;
				}
			}
			
			/////////DO THE KMEANS////////////////////////////////////
			kMeansSlaves(11, recvBuff, allCentroids, myRank);
		}
		threshold ++;
		
	}

	 MPI_Finalize();
	
	
}