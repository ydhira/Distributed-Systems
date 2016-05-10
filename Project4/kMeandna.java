import java.util.*;
import java.io.*;
import java.lang.Math.*;

import org.apache.hadoop.*;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;


import java.net.*;
import org.apache.hadoop.fs.*;


public class kMeandna {
	
	private static ArrayList<String> allCentroids = new ArrayList<String>();

	//gievn 2 strings, returns the different of characters in them
	public static int differenceString(String dnaStrand, String centroid){
	
		int count = 0 ;
		for(int i = 0; i < dnaStrand.length(); i++){
			if(!Character.valueOf(dnaStrand.charAt(i)).equals(Character.valueOf(centroid.charAt(i)))){
			   count++;
			}
		}
		return count;
	}
	
	//given an arraylist of integers, returns the index of the maximum int
	public static int giveMax( int[] A){
		int a = A[0];
		int t = A[1];
		int c = A[2];
		int g = A[3];
		
		int max = a;
		int index = 0;
		
		if (t > max){
			max = t;
			index = 1;
		}
		if (c > max){
			max = c;
			index = 2;
		}
		if (g > max){
			max = g;
			index = 3;
		}
		return index;
		
	}
	
	/*MAP CLASS*/
	public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
		
		public void configure(JobConf job){
			try{
				//get the File inside the Cache
				Path[] cacheFiles = DistributedCache.getLocalCacheFiles(job);
				if (cacheFiles != null && cacheFiles.length > 0) {
					allCentroids.clear();
					String line;
					BufferedReader cacheReader = new BufferedReader(new FileReader(cacheFiles[0].toString()));
					//open the file that is added
					try{
						int ind = 0;
						line = cacheReader.readLine();
						//read all the centroids and add them to the list of allCentroids
						while (line != null){
							allCentroids.add(ind,line  );
							line = cacheReader.readLine();
							ind++;
						}
					}
					
					finally{
						cacheReader.close();
					}
				}
			}
			
			catch(Exception e){
				System.out.println(e);
			}
		}
		
		//implements the map logic for this program
		public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter report) throws IOException {
			
			String line = value.toString();
			Scanner scanner = new Scanner(line);
			
			while(scanner.hasNext()) {
				
				String token = scanner.next();
				int dmin = Integer.MAX_VALUE;
				int index = 0;
				int n = 0;
				//for each centroid, get the distance of the point
				for (String centroid : allCentroids){
					
					int distance = differenceString(token , centroid) ;
					//if the poitn is closer to the centroid, then remember this centroid
					if (distance < dmin){
						dmin = distance;
						n = index; 
					}
					index++;
				}
				
				//collect the centroid and the point belonging to this centroid
				Text centroid2 = new Text();
				centroid2.set(allCentroids.get(n));
				Text point = new Text();
				point.set(token);
				
				output.collect(centroid2, point);
			}

		}
	}	
	
	/*REDUCE CLASS*/
	public static class Reduce extends MapReduceBase implements Reducer<Text, Text, NullWritable, Text> {
		//implements the reduce logic for this program
		public void reduce(Text key, Iterator<Text> values, OutputCollector<NullWritable, Text> output, Reporter report) throws IOException {
			
			int[] parts;
			ArrayList<int []> allSum = new ArrayList<int []>();
			int length = key.toString().length();
			//keep track of the numebr of A,T,C,G seen for every index of the new centroidt 
			//that needs to be created
			for (int i =0; i<length; i++){
				parts = new int[4];
				allSum.add(parts);
			}
			
			while(values.hasNext()){
				String dnaStrand = values.next().toString();
				//Go through the value and at each index, see which letter is seen
				//remember that in the list 
				for (int j =0 ; j<length ; j++){
					
					if (Character.valueOf(dnaStrand.charAt(j)).equals('a')){
						(allSum.get(j))[0] = (allSum.get(j))[0] + 1;
					}
					
					if (Character.valueOf(dnaStrand.charAt(j)).equals('t')){
						(allSum.get(j))[1] = (allSum.get(j))[1] + 1;
					}
					
					if (Character.valueOf(dnaStrand.charAt(j)).equals('c')){
						(allSum.get(j))[2] = (allSum.get(j))[2] + 1;
					}
					
					if (Character.valueOf(dnaStrand.charAt(j)).equals('g')){
						(allSum.get(j))[3] = (allSum.get(j))[3] + 1;
					}
				}
			}
			ArrayList<Character> buildingCen = new ArrayList<Character>();
			//which ever character is maximum at each index, add that to the index 
			//of the new centroid
			for (int i =0; i<length; i++){
				int max = giveMax(allSum.get(i));
				if (max == 0){
					buildingCen.add(i , 'a');
				}	
				if (max == 1){
					buildingCen.add(i ,'t');
				}
				if (max == 2){
					buildingCen.add(i,'c');
				}
				if (max == 3){
					buildingCen.add(i,'g');
				}
			}
			//build the new centroid by the array of chars
			StringBuilder builder = new StringBuilder(buildingCen.size());
			for(Character ch: buildingCen)
			{
				builder.append(ch);
			}
			//we dont have any key, so it is null. the value is the new centroid
			String newCen = builder.toString();
			Text word = new Text();
			word.set(newCen);
			output.collect(NullWritable.get(), word);
			
		}
	}
	
	public static void main(String args[]) throws Exception{

		int iteration = 0;
		
		String outFileDir = args[1] + System.nanoTime();
		int noOfClusters = Integer.parseInt(args[2]);
	
		Path outFile = new Path(outFileDir + "/initial_centroids");
		
		FileSystem fs = FileSystem.get(new Configuration());
		
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fs.create(outFile)));
		
		int ind = 0;
		int count = 0;
		
		FileStatus[] status2 = fs.listStatus(new Path(args[0])); 
		//For the case if we have more than one inout file and the initial centroids 
		//required are greater than the number of points in one file. So need to open 
		//the other files
		for (int i=0;i<status2.length;i++){
					
			Path pathFile = status2[i].getPath();
			String namePathFile = pathFile.toString();
			BufferedReader br1 = new BufferedReader(new InputStreamReader(fs.open(status2[i].getPath())));
			String line = br1.readLine();
			while (count < noOfClusters && line!= null)
			{

				br.write(line + "\n");
				line = br1.readLine();
				count++;
			}
			br1.close();
		}		
		br.close();
	
		while ( iteration < 5 ){
			JobConf conf = new JobConf(kMeandna.class);
			//add the file to the distributed Cache.
			if (iteration == 0 ){
				DistributedCache.addCacheFile(outFile.toUri(), conf);
			}
			else{
				//add that file which has the result of the previous iteration
				Path newCenFile = new Path(outFileDir + "/next_centroids.txt");
				
				//if there are more than 1 file from the previous iteration then put them all in one file
				//and then work on it				
				BufferedWriter brw = new BufferedWriter(new OutputStreamWriter(fs.create(newCenFile)));
				FileStatus[] status = fs.listStatus(new Path(outFileDir));
				for (int i=0;i<status.length;i++){
					
					Path pathFile = status[i].getPath();
					String namePathFile = pathFile.toString();
					
					if ( ! namePathFile.contains("_logs") && ! namePathFile.contains("next_centroids") ){
						BufferedReader brr =new BufferedReader(new InputStreamReader(fs.open(status[i].getPath())));
						String line2;
						line2=brr.readLine();
						while (line2 != null){
							brw.write(line2 + "\n");
							line2=brr.readLine();
						}
						brr.close();
					}
				}
		
				DistributedCache.addCacheFile(newCenFile.toUri(), conf);
				brw.close();
			}
			
			conf.setJobName("KMeans DNA");
			
			conf.setMapperClass(Map.class);
			conf.setReducerClass(Reduce.class);
			
			conf.setInputFormat(TextInputFormat.class);
			conf.setOutputFormat(TextOutputFormat.class);
			
			conf.setOutputKeyClass(Text.class);
			conf.setOutputValueClass(Text.class);
			
			conf.setNumReduceTasks(1);
			
			FileInputFormat.setInputPaths(conf, new Path(args[0]));
			
			outFileDir = args[1] + System.nanoTime();
			
			FileOutputFormat.setOutputPath(conf, new Path(outFileDir) );
			
			JobClient.runJob(conf);
			iteration ++;
		}
		fs.close();		
	}
	
}