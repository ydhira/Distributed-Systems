#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <mpi.h>
#include <stddef.h>

#define TAG_X 1
#define TAG_Y 2

typedef struct {
	float x;
	float y;
} Point;

typedef struct{
	Point centroid;
}Cluster;

typedef struct{
	float sumX;
	float sumY;
	int size;
} myTuple;


int noOfTimesKMeans = 5;
int totalNoPoints;
int noOfClusters;
int noOfProcessors;
int noElemsPerProc;


myTuple* kMeans(Point allPoints[], Cluster KClusters[], int myRank){
		
	int * membership = malloc( sizeof(int) * noElemsPerProc);
	
	myTuple* masterTuples = malloc (noOfClusters * sizeof(myTuple) );
	
	int i;
	int j;
	int n =0;
	
	
	//See which cluster a point belongs to and change the membership then 
	for (i=0; i < noElemsPerProc; i++){
		Point p = allPoints[i];
		float dmin = 10000.0;
		for(j=0 ; j< noOfClusters; j++){
			Cluster k = KClusters[j];
			float distance = sqrt((pow((p.x - k.centroid.x), 2))+(pow((p.y - k.centroid.y),2)));
			if (distance < dmin){
				dmin = distance;
				n = j;
			}
		}
		if (membership[i] != n){
			membership[i]=n;
			//printf("Changed the memebership \n");
		}
	}
	
	//Go through every cluster 
	int k=0;
	for (k =0; k<noOfClusters; k++){
		int p = 0;
		int l = 0;
		int size = 0;
		//gather all the points in the same cluster. PointsinCluster contains that 

		Point* PointsinCluster = malloc( sizeof(Point) * noElemsPerProc ) ;
		
		for (p=0; p< noElemsPerProc; p++){
			if (membership[p] == k){
				PointsinCluster[l] = allPoints[p];
				//printf("what point is added in PointsinCluster %f,     %f \n", allPoints[p].x, allPoints[p].y );
				size++;
				l++;
			}
		}
		
		//sum up the x values and the y values for all the points in the same cluster. 
		int m = 0;
		float sumX = 0.00;
		float sumY = 0.00;
		//printf("size is %d for this cluster %d \n", size, k);
		for (m=0; m<size; m++){
			sumX +=  PointsinCluster[m].x;
			sumY +=  PointsinCluster[m].y;
			// printf("Point in Cluster %f,     %f \n", PointsinCluster[m].x, PointsinCluster[m].y );
			// printf("summ at inter stage is %f ,   %f\n", sumX, sumY);
		}
		
		//For this cluster k , send the sum of the X's and the sum of Y's and the size 
		if (myRank!=0){
			//printf("Process %d: Send sumX %f sumY %f size %d\n" , myRank, sumX, sumY, size);
			MPI_Send(&sumX, 1, MPI_FLOAT, 0, myRank, MPI_COMM_WORLD); 
			MPI_Send(&sumY, 1, MPI_FLOAT, 0, myRank, MPI_COMM_WORLD); 
			MPI_Send(&size, 1, MPI_INT, 0, myRank, MPI_COMM_WORLD); 
		}
		
		else{
			//printf ("Master: Made the tuples for cluster %d \n" , k);
			myTuple t;
			t.sumX = sumX;
			t.sumY = sumY;
			t.size = size;
			masterTuples[k] = t;
		}
		free(PointsinCluster);
	}	
	// int ll=0;
	// for (ll =0; ll<noOfClusters;ll++){
		// float xmean = KClusters[ll].centroid.x;
		// float ymean = KClusters[ll].centroid.y;
		// printf("For Centroid %d , x-value is %f, y-value is %f \n", ll, xmean, ymean);
	// }
	free(membership);
	return masterTuples;
}
	
	
	
	
int main (int argc , char* argv[]){
	
	if (argc != 9){
		if (strcmp(argv[0],"-h")){
			printf("./mpi_kmeans -n <noofpoints> -c <noofclusters> -p <noOfVirtualMachines>  -f <inputFile>\n");
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
	char*  inFileName = argv[8];
	
	FILE *infile =  fopen(inFileName, "r" );
	
	
	noElemsPerProc = (totalNoPoints / noOfProcessors) ;
		
	Point* recvBuff = malloc( sizeof(Point) * noElemsPerProc);
	int myRank;    
	MPI_Status status;
	
	MPI_Init(&argc, &argv);
	MPI_Comm_rank(MPI_COMM_WORLD, &myRank);
	
	
	//////////// create a MPI type for struct Point ///////////////
    const int nitems=2;
    int blocklengths[2] = {1,1};
    MPI_Datatype types[2] = {MPI_FLOAT, MPI_FLOAT};
    MPI_Datatype mpi_point_type;
    MPI_Aint offsets[2];

    offsets[0] = offsetof(Point, x);
    offsets[1] = offsetof(Point, y);

    MPI_Type_create_struct(nitems, blocklengths, offsets, types, &mpi_point_type);
    MPI_Type_commit(&mpi_point_type);
	////////////////// create a MPI type for struct Cluster ///////////////////////
	
	const int nitems1=1;
    int blocklengths1[1] = {1};
    MPI_Datatype types1[1] = {mpi_point_type};
    MPI_Datatype mpi_cluster_type;
    MPI_Aint offsets1[1];

    offsets1[0] = offsetof(Cluster, centroid);

    MPI_Type_create_struct(nitems1, blocklengths1, offsets1, types1, &mpi_cluster_type);
    MPI_Type_commit(&mpi_cluster_type);
	
	/////////////////////////////////////////
	
	Point* allPoints = malloc( sizeof(Point) *totalNoPoints );
	Cluster* KClusters = malloc( sizeof(Cluster) * noOfClusters );
	
	int threshold = 0; 
	while (threshold < 5){
		if(myRank == 0){
			double endTime, startTime;
			
			//MASTER 
			
				//PARSING THE FILE AND PUTTING INTO ALLPOINTS ARRAY.
				if (threshold == 0){
					startTime = MPI_Wtime();
					//DONE ONLY THE FIRST TIME 
					
					if ( infile ==NULL){
						
						perror("File doesnt exist. \n");
						exit(EXIT_FAILURE);
					}
				
					char line [100];
					char * token;
					int added = 0;
					while ( (fgets(line, 100, infile))!=NULL){
						int i=0;

						//printf(line );
						token =  strtok(line, ",") ;
					
						Point *pp = malloc(sizeof(Point));
						Point p = (*pp);
						while( token != NULL ) 
						{
							//printf ("%.9f \n", atof(token) );
							if ( i==0){
							
								p.x = atof(token);			}
							if (i ==1){
							
								p.y = atof(token);
							}
							i++;
							token = strtok(NULL, ",");
						}
						//printf("value of added is %d and totalPoints are %d \n" , added, totalNoPoints);
						allPoints[added] = p;
						added++;
					}
				
					//printf("DONE ADDNG THE POINTSssssssssssssssssssssssss \n");
					fclose(infile);
					
					//you can randomly generate them as well
					int i; 
					for (i =0; i<noOfClusters; i++){
						
						Cluster * c = malloc(sizeof (Cluster));
						
						Point *pp = malloc(sizeof(Point));
						Point p2 = (*pp);
						p2.x  =allPoints[i].x;
						p2.y  =allPoints[i].y;
						(*c).centroid = p2;  

						KClusters[i] = (*c);
					}	
					int j;
					for(j=1; j<noOfProcessors; j++){
						int startIndex = j * noElemsPerProc;
						int endIndex = startIndex + noElemsPerProc;
						
					//	printf("Master Sending ");
						
						MPI_Send(&allPoints[startIndex], noElemsPerProc, mpi_point_type, j, j, MPI_COMM_WORLD); 
						//MPI_Send(KClusters, noOfClusters, mpi_cluster_type, j, j, MPI_COMM_WORLD);
					}
				}
				////////////////SEND THE ELEMENTS NOW////////////////////////////
				
				int j;
				for(j=1; j<noOfProcessors; j++){
					int startIndex = j * noElemsPerProc;
					int endIndex = startIndex + noElemsPerProc;
					
				//	printf("Master Sending ");
					
					//MPI_Send(&allPoints[startIndex], noElemsPerProc, mpi_point_type, j, j, MPI_COMM_WORLD); 
					MPI_Send(KClusters, noOfClusters, mpi_cluster_type, j, j, MPI_COMM_WORLD);
				}
				
			//	printf("Iteration %d Master %d \n\n",threshold, myRank);
				
				//printf("Master: Send done \n");
				//////////DO MY OWN KMEANS/////////////////////////////////
				
				Point* masterArray = malloc(sizeof(Point) * noElemsPerProc );
				int h;
				for (h=0; h< noElemsPerProc ;h++){
					masterArray[h] = allPoints[h];
				}
				
				myTuple* masterKmeans = kMeans (masterArray, KClusters, myRank);
				//printf ("DONE DOING MASTER KMEANS \n");
				//////////RECIEVE THE ELEMENTS////////////////////////////
				
				//The master need to receive in a loop as well? right ?
				
				float whole_sumX[noOfClusters] ;
				float whole_sumY[noOfClusters] ;
				int whole_sizes[noOfClusters];
				int index = 0;
				int indexP ;
				float sumX, sumY = 0;
				int size = 0;
				int v;
				for (v =0; v<noOfClusters ;v++){
					whole_sumX[v] = masterKmeans[v].sumX;
					whole_sumY[v] = masterKmeans[v].sumY;
					whole_sizes[v] = masterKmeans[v].size;
				}
				
				for (indexP =1 ; indexP <noOfProcessors ; indexP ++){
					for (index =0 ; index <noOfClusters; index ++){
						
						//printf("Recieving the result back from Process %d and for cluster %d \n", indexP, index);
					//	printf("MAster : receiving");
						
						MPI_Recv(&sumX, 1, MPI_FLOAT, indexP, indexP, MPI_COMM_WORLD, &status);
						MPI_Recv(&sumY, 1, MPI_FLOAT, indexP, indexP, MPI_COMM_WORLD, &status);
						MPI_Recv(&size, 1, MPI_INT, indexP, indexP, MPI_COMM_WORLD, &status);
						
					//	printf("Process %d: Receive sumX is: %f  sumY : %f  size: %d \n", indexP, sumX, sumY, size );
						
						whole_sumX[index] += sumX;
						whole_sumY[index] += sumY;
						whole_sizes[index] += size;
						
					}
				}			
				
				// int test;
				// for (test =0; test<noOfClusters ; test++){
					
					// printf("For centroid %d , sumX is: %f , sumY : %f , size: %d \n", test, whole_sumX[test], whole_sumY[test], whole_sizes[test] );
				// }
				
				//////////AVERAGE THE ELEMENTS, Calculate the new centroid ////////////////////////////	
				int cen = 0;
				for ( cen = 0; cen < noOfClusters ; cen++){
					Point newP ;
					int thisSize = whole_sizes[cen];
					if (thisSize!= 0){
						newP.x = (whole_sumX[cen] / thisSize);
						newP.y = (whole_sumY[cen] / thisSize);
						KClusters[cen].centroid = newP;
					}
				}
				
				if (threshold == 4){
					
					endTime = MPI_Wtime();
					
					int ll;
					for (ll =0; ll<noOfClusters;ll++){
						float xmean = KClusters[ll].centroid.x;
						float ymean = KClusters[ll].centroid.y;
						
						printf(" For Centroid %d , x-value is %f, y-value is %f \n", ll, xmean, ymean);
					}
					printf("Time Taken = %f\n",(endTime - startTime));
					//free(allPoints);
				}
				
			}
		 else{
		 //SLAVE
		 //////////RECIEVE THE ELEMENTS////////////////////////////
		//	printf("Slave: Receive \n");
			
		//	printf("Iteration %d Slave %d \n\n",threshold, myRank);
			if (threshold == 0){ 
				MPI_Recv(recvBuff, noElemsPerProc, mpi_point_type, 0, myRank, MPI_COMM_WORLD, &status);
			}
		//	printf("Iteration %d Slave %d RP\n\n",threshold, myRank);
			MPI_Recv(KClusters, noOfClusters, mpi_cluster_type, 0, myRank, MPI_COMM_WORLD, &status);
			/////////DO THE KMEANS////////////////////////////////////
		//	printf("Iteration %d Slave %d \n\n",threshold, myRank);

			kMeans(recvBuff, KClusters, myRank);
		
		 /////////SEND THEM BACK///////////////////////////////////
			
			//the kmeans function send them back as well 
			
		 }				
		 threshold ++;
	}
	
	//HOW TO FREE STUFF HERE?
	// int f = 0;
	// for (f =0; f< totalNoPoints; f++){
		// free(&allPoints[f]);
	// }
	
	// for (f =0; f< noOfClusters ;f++){
		//free(&KClusters[f].centroid);
		// free(&KClusters[f]);
	// }
	
	printf ("DONE!!!!!");
	
	//free(KClusters);
	
	MPI_Finalize();
}
