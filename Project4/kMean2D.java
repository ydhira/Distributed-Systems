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


public class kMean2D {
	
	private static ArrayList<String> allCentroids = new ArrayList<String>();
	/*  MAP CLASS */
	
	public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
		
		//This is run only once per Job. It opens the file in the Cache and reads the 
		//file for the centroids. Stores them in the list calles allCentroids
		
		public void configure(JobConf job){
			try{
				//get the file from the cache. 
				//open it and read the centroids from it 
				//store the centroids in the allCentroids array
				Path[] cacheFiles = DistributedCache.getLocalCacheFiles(job);
				if (cacheFiles != null && cacheFiles.length > 0) {
					allCentroids.clear();
					String line;
					BufferedReader cacheReader = new BufferedReader(new FileReader(cacheFiles[0].toString()));
					try{
						int ind = 0;
						line = cacheReader.readLine();
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
		
		/*implements the map logic of the program*/
		public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter report) throws IOException {
		
			String line = value.toString();
			Scanner scanner = new Scanner(line);
			
			while(scanner.hasNext()) {
				//for each value, parse the x and the y out.
				String token = scanner.next();
				String [] parts = token.split(",");
				double x_value = Double.parseDouble(parts[0]);
				double y_value = Double.parseDouble(parts[1]);
				Double min1 = Double.MAX_VALUE;
				int n =0;
				int index = 0;
				//calculate the distance of the point to every centroids
				//remember the centroid with the least distance
				for (String c : allCentroids){
					String [] parts2 = c.split(",");
					double x_valueCen = Double.parseDouble(parts2[0]);
					double y_valueCen = Double.parseDouble(parts2[1]);
					
					Double distance = Math.sqrt(Math.pow((x_valueCen - x_value),2) + Math.pow((y_valueCen - y_value),2));
					if (distance <  min1){
						min1 = distance;
						n = index;
					}
					index++;
				}
				//out the key as the centroi and the point which points to it
				Text centroid = new Text();
				centroid.set(allCentroids.get(n));
				Text point = new Text();
				point.set(token);
				
				output.collect(centroid, point);
			}
		}
	}
	
	/*REDUCE CLASS*/
	public static class Reduce extends MapReduceBase implements Reducer<Text, Text, NullWritable, Text> {
		/*implements the reduce logic of the program*/
		public void reduce(Text key, Iterator<Text> values, OutputCollector<NullWritable, Text> output, Reporter report) throws IOException {
	
			double sumX = 0.0;
			double sumY = 0.0;
			int size = 0;
			while(values.hasNext()) {
				//get the value and parse it
				String [] parts = values.next().toString().split(",");
				//add the x to the sumX and y to the sumY
				double x_value = Double.parseDouble(parts[0]);
				double y_value = Double.parseDouble(parts[1]);
				sumX += x_value;
				sumY += y_value;
				size++;
			}
			
			//calculate the new centroid with the average value of the X and Y
			double newX = sumX / size;
			double newY = sumY / size;
			String newCen = Double.toString(newX) + "," + Double.toString(newY);
						
			//output the new centroid found
			Text word = new Text();
			word.set(newCen);
			output.collect(NullWritable.get(), word); 
		}		
	}
	
	public static void main(String args[]) throws Exception{

		int iteration = 0;
		
		String outFileDir = args[1] + System.nanoTime();
		int totalPoints = Integer.parseInt(args[2]);
		int noOfClusters = Integer.parseInt(args[3]);
		int perCluster = totalPoints / noOfClusters;
		
		Path outFile = new Path(outFileDir + "/initial_centroids.txt");
		FileSystem fs = FileSystem.get(new Configuration());
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fs.create(outFile)));
		
		int ind = 0;
		int ind2 = 0;
		int count = 0;
		
		//open the initital data file and write to the initial_centroids 
		//file the intital centroids.
		//handles more than one file in the input dir 
		FileStatus[] status2 = fs.listStatus(new Path(args[0])); //will give me all the files in the input 
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
			
			JobConf conf = new JobConf(kMean2D.class);
			
			//add the file of centroid to the cache with every iteration
			if (iteration == 0 ){	
				DistributedCache.addCacheFile(outFile.toUri(), conf);
			}
			
			else{
				Path newCenFile = new Path(outFileDir + "/next_centroids.txt");
				BufferedWriter brw = new BufferedWriter(new OutputStreamWriter(fs.create(newCenFile)));
				FileStatus[] status = fs.listStatus(new Path(outFileDir));
				
				//for every output file created in the previous iteration, read that file and 
				//write the result to a one new file and put that file in the cache 
				
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
			
			conf.setJobName("KMeans 2D");
			
			conf.set("dfs.block.size", "67108864");
			
			conf.setMapperClass(Map.class);
			conf.setReducerClass(Reduce.class);
	
			conf.setInputFormat(TextInputFormat.class);
			conf.setOutputFormat(TextOutputFormat.class);
			
			conf.setOutputKeyClass(Text.class);
			conf.setOutputValueClass(Text.class);
			
			conf.setNumReduceTasks(10);
			
			FileInputFormat.setInputPaths(conf, args[0]); 
			outFileDir = args[1] + System.nanoTime();
			FileOutputFormat.setOutputPath(conf, new Path(outFileDir) );
			JobClient.runJob(conf);
			iteration ++;
		}
		fs.close();
	}
}