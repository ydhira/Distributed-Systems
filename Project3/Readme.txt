******************************
********* How to Run *********
******************************

* seq_kmean.c
- Sequential implementation of KMeans on 2D points 
Run :  ./seq_kmean -n <#> -c <#> -f <#>
	-n : number of data set points
	-c : number of clusters
	-f : input file 
	
* mpi_kmean.c
- MPI implementation of KMeans on 2D points 
Run :  mpiexec -f machinefile -n 4 ./mpi_kmean -n <#> -c <#> -p <#> -f <#>
	-n : number of data set points
	-c : number of clusters
	-p : number of virtual machines 
	-f : input file
	
NOTE:
THE DNA implementation only works with DNA Strands of length 10. So please 
run it on length 10 DNA strands. I can make it general but did not have time 
in the end.  	
	
* seq_kmean_dna.c
- Sequential implementation of KMeans on DNA Strands 
Run :  ./seq_kmean_dna -n <#> -c <#> -f <#>
	-n : number of data set points
	-c : number of clusters
	-f : input file

* mpi_kmean_dna.c
- MPI implementation of KMeans on DNA Strands  
Run :  mpiexec -f machinefile -n 4 ./mpi_kmean -n <#> -c <#> -p <#> -f <#>
	-n : number of data set points
	-c : number of clusters
	-p : number of virtual machines 
	-f : input file