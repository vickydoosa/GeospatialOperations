package edu.asu.cse512;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import com.vividsolutions.jts.geom.Geometry;

import edu.asu.cse512.functions.CPolyUnionMapFunction;
import edu.asu.cse512.functions.GeometryFileMapFunction;

/**
 * @author hdworker
 *
 */
public class Union {

	public Union(String appName, String master, String inputFile,
			String outputFile) {
		this.master = master;
		this.appName = appName;
		this.inputFile = inputFile;
		this.outputFile = outputFile;
	}

	private String inputFile;

	private String outputFile;

	private String appName;

	private String master;
	
	private SparkConf conf;
	
	private JavaSparkContext context;
	
	private FileType fileType;
	
	private int partitions;
	
	private static int DEFAULT_PARTITIONS = 4;

	/*
	 * Main function, take two parameter as input, output
	 * 
	 * @param inputLocation
	 * 
	 * @param outputLocation
	 */
	public static void main(String[] args) {
		// Initialize, need to remove existing in output file location.

		// Implement

		// Output your result, you need to sort your result!!!
		// And,Don't add a additional clean up step delete the new generated
		// file...

		String appName = "SparkApp";
		String master = "local";

		String in;
		String out;

		if (!validateInputs(args)) {
			// Invalid inputs
			System.exit(1);
		}

		in = args[0].trim();
		out = args[1].trim();

		Union union = new Union(appName, master, in, out);
		// set the type of files to be read and stored
		union.setFileType(FileType.TEXT);
		// set the num of partitions
		union.setPartitions(2);

		try {
			union.geometryUnion();
		} catch (Exception e) {
			// Exception
		} finally {
			union.closeSpark();
		}

	}
	
	private void init() throws Exception {
		conf = new SparkConf().setAppName(appName).setMaster(
				master);
		context = new JavaSparkContext(conf);
		
		// num of partitions to default
		if (this.partitions == 0)
			this.partitions = DEFAULT_PARTITIONS;
		
		// set file type
		if (this.fileType == null)
			this.fileType = FileType.HDFS;
	}

	/**
	 * Union operation
	 */
	public void geometryUnion() throws Exception {
		// initialize apache spark and java context
		init();
		
		// read the input file
		JavaRDD<String> inputFileRDD = context.textFile(inputFile, partitions);

		// convert the input file to collections of Geometry shapes
		JavaRDD<Geometry> polygonsRDD = inputFileRDD.mapPartitions(new GeometryFileMapFunction());
		
		// Cascade union polygons
		JavaRDD<Geometry> cascadedPolygonRDD = polygonsRDD.mapPartitions(new CPolyUnionMapFunction());
		
		// Merge all partitions and execute the cascaded polygon union
		// it has only one polygon object
		JavaRDD<Geometry> finalPolygonRDD = cascadedPolygonRDD.coalesce(1).mapPartitions(new CPolyUnionMapFunction());
		
		finalPolygonRDD.cache();
		
		Geometry finalPolygon = finalPolygonRDD.first();
		
		int numGeoms = finalPolygon.getNumGeometries();
		
		if (numGeoms > 0) {
			// has multiple polygons
			
		}
		
		// output polygons
		System.out.println(finalPolygonRDD.collect());
		
		// Collect origins 
		
		// 
		
	}
	
	public void closeSpark() {
		try {
			if (this.context != null)
				this.context.close();
		} catch (Exception e) {
			// continue
		}
	}

	/**
	 * @param args
	 * @return valid
	 */
	private static boolean validateInputs(String args[]) {
		return (null != args && args.length == 2);
	}

	public String getInputFile() {
		return inputFile;
	}

	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getMaster() {
		return master;
	}

	public void setMaster(String master) {
		this.master = master;
	}

	public FileType getFileType() {
		return fileType;
	}

	public void setFileType(FileType fileType) {
		this.fileType = fileType;
	}

	public int getPartitions() {
		return partitions;
	}

	public void setPartitions(int partitions) {
		this.partitions = partitions;
	}

}

enum FileType {
	HDFS,
	TEXT
}
