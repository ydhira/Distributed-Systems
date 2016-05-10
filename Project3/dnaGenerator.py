import sys
import csv
import numpy
import getopt
import math
import random 


def usage():
    print '$> python generaterawdata.py <required args> [optional args]\n' + \
        '\t-c <#>\t\tNumber of clusters to generate\n' + \
        '\t-p <#>\t\tNumber of points per cluster\n' + \
        '\t-o <file>\tFilename for the output of the raw data\n' + \
        '\t-v [#]\t\tMaximum coordinate value for points\n'  

def stringDistance(point, pair):
	count = 0
	for i in range (0, len(point)):
		if point[i] is not pair[i]:
			count+=1
	return count

def tooClose(point, points, minDist):
    '''
    Computes the difference between the string and all strings
    in the list, and if any strings in the list are closer than minDist,
    this method returns true.
    '''
    for pair in points:
		if stringDistance(point, pair) < minDist:
			return True

    return False

def handleArgs(args):
    # set up return values
    numClusters = -1
    numPoints = -1
    output = None
    maxValue = 10

    try:
        optlist, args = getopt.getopt(args[1:], 'c:p:v:o:')
    except getopt.GetoptError, err:
        print str(err)
        usage()
        sys.exit(2)

    for key, val in optlist:
        # first, the required arguments
        if   key == '-c':
            numClusters = int(val)
        elif key == '-p':
            numPoints = int(val)
        elif key == '-o':
            output = val
        # now, the optional argument
        elif key == '-v':
            maxValue = float(val)

    # check required arguments were inputted  
    if numClusters < 0 or numPoints < 0 or \
            maxValue < 1 or \
            output is None:
        usage()
        sys.exit()
    return (numClusters, numPoints, output, \
            maxValue)


def concatenateString(start, dist):
	startList = list(start)
	indexList = random.sample( [i for i in range(0,len(startList))], dist ) ##got a k length list of unique elements from the input list sequence
	for i in indexList:
		dna = ['a', 't', 'c', 'g']
		dna.remove(startList[i])
		startList[i] = random.choice(dna)
		
	return ''.join(startList)
	
	
def drawOrigin(maxValue):
     return (''.join(random.choice(['a', 't', 'g', 'c'])
            for i in xrange(maxValue)))

# start by reading the command line
numClusters, \
numPoints, \
output, \
maxValue = handleArgs(sys.argv)

writer = csv.writer(open(output, "w"))

# step 1: generate each 2D centroid
centroids_radii = []
minDistance = 0
for i in range(0, numClusters):
    centroid_radius = drawOrigin(maxValue)
    # is it far enough from the others?
    while (tooClose(centroid_radius, centroids_radii, minDistance)):
        centroid_radius = drawOrigin(maxValue)
    centroids_radii.append(centroid_radius)

# step 2: generate the points for each centroid
points = []
minClusterVar = 0
maxClusterVar = 0.5 * maxValue

for i in range(0, numClusters):

    # compute the variance for this cluster
    variance = numpy.random.uniform(minClusterVar, maxClusterVar)
    cluster = centroids_radii[i]
    for j in range(0, numPoints):
        # generate a 2D point with specified variance
        # point is normally-distributed around centroids[i]
        dist = int(abs(numpy.random.normal(0, variance)))
        if dist > maxValue :
		    dist = maxValue
        # write the points out
        writer.writerow([concatenateString(cluster, dist)])
