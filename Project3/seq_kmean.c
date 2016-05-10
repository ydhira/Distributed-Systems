#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <limits.h>
#include <time.h>

typedef struct {
	float x;
	float y;
} Point;

typedef struct{
	Point centroid;
}Cluster;


int noOfTimesKMeans = 5;
int totalNoPoints;
int noOfClusters;

int kMeans(Point allPoints[], Cluster KClusters[]){
	
	int threshold =0;
	int * membership = malloc( sizeof(int) * totalNoPoints);
	
	//Doing the kMeans 5 many times
	while ( threshold < noOfTimesKMeans){
		int i;
		int j;
		int n = 0;
		
		for (i=0; i < totalNoPoints; i++){
			Point p = allPoints[i];
			float dmin = 500.0;
			
			//for every point, see which cluster it belongs to 
			for(j=0 ; j< noOfClusters; j++){
				Cluster k = KClusters[j];
				float distance = sqrt((pow((p.x - k.centroid.x), 2))+(pow((p.y - k.centroid.y),2)));
				if (distance < dmin){
					dmin = distance;
					n = j;
				}
			}
			//change the memberhsip of the point p if it closer to this centroid
			if (membership[i] != n){
				membership[i]=n;
			}
		}
		
		int k=0;
		for (k =0; k<noOfClusters; k++){
			int p = 0;
			int l = 0;
			int size = 0;
			
			Point* PointsinCluster = malloc( sizeof(Point) * totalNoPoints ) ;
			//for every cluster see which points are closer to it
			for (p=0; p< totalNoPoints; p++){
				if (membership[p] == k){
					PointsinCluster[l] = allPoints[p];
					size++;
					l++;
				}
			}
			
			//for every point in this cluster, get the sum of the X, Y
			int m = 0;
			float sumX = 0.00;
			float sumY = 0.00;
			for (m=0; m<size; m++){
				sumX +=  PointsinCluster[m].x;
				sumY +=  PointsinCluster[m].y;
			}
			//calculate the new centroid for this cluster 
			Point newP ;
			if (size!= 0){
				newP.x = (sumX / size);
				newP.y = (sumY / size);
				KClusters[k].centroid = newP;
			}
		}
		
		
		threshold ++;
	}
	
	//Just for testing
	int ll=0;
	for (ll =0; ll<noOfClusters;ll++){
		float xmean = KClusters[ll].centroid.x;
		float ymean = KClusters[ll].centroid.y;
		printf("For Centroid %d , x-value is %f, y-value is %f \n", ll, xmean, ymean);
	}
	
}


int main (int argc , char* argv[]){
	
	clock_t begin, end;
	double time_spent;
	begin = clock();
	
	printf("arg: %d \n", argc);
	if (argc != 7){
		if (strcmp(argv[0],"-h")){
			printf("./seq_kmeans -n <noofpoints> -c <noofclusters>  -f <inputFile> \n");
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
	
	Point* allPoints = malloc( sizeof(Point) * totalNoPoints );
	Cluster* KClusters = malloc( sizeof(Cluster) * noOfClusters );  
	
	
	FILE *infile =  fopen(inFileName, "r" );
	if ( infile ==NULL){
		perror("File doesnt exist. \n");
		exit(EXIT_FAILURE);
	}
	
	char line [100];
	char * token;
	int added = 0;
	
	//read from the file
	while ( (fgets(line, 100, infile))!=NULL){
		int i=0;

		token =  strtok(line, ",") ;
		
		Point *pp = malloc(sizeof(Point));
		Point p = (*pp);
		
		while( token != NULL ) 
		{
			if ( i==0){
				
				p.x = atof(token);			}
			if (i ==1){
				
				p.y = atof(token);
			}
			i++;
			token = strtok(NULL, ",");
		}
		//add to the list
		allPoints[added] = p;
		added++;
	}
	
	fclose(infile);
	
	//from the list of all points, get the first noOfClusters many centroids
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
     //run the kMeans on the points and the centroids
	kMeans(allPoints, KClusters);
	free(allPoints);	
	free(KClusters);
	
	end = clock();
	time_spent = (double) (end - begin)/CLOCKS_PER_SEC;
	printf("Time spent is: %f \n", time_spent);
}
