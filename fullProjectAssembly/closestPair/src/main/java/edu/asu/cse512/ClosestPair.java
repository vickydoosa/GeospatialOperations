package edu.asu.cse512;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;

import scala.Tuple2;

/**
 * Hello world!
 *
 */

class Point implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public double x;
	public double y;

	Point() {

	}

	Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return x + "," + y;
	}

}

class Pair implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public Point A;
	public Point B;
	double distanceLength;

	Pair() {

	}

	Pair(Point A, Point B) {
		this.A = A;
		this.B = B;
		distanceLength = findDistance(A, B);
	}

	@Override
	public String toString() {
		return "Pair [A=" + A + ", B=" + B + ", distanceLength="
				+ distanceLength + "]";
	}

	public double findDistance(Point A1, Point A2) {
		double dist;
		dist = Math.sqrt(Math.pow((A1.x - A2.x), 2)
				+ Math.pow((A1.y - A2.y), 2));
		return dist;
	}
}

public class ClosestPair {
	/*
	 * Main function, take two parameter as input, output
	 * 
	 * @param inputLocation
	 * 
	 * @param outputLocation
	 */
	public static void main(String[] args) throws FileNotFoundException,
			UnsupportedEncodingException {
		// Initialize, need to remove existing in output file location.
		
		String appName = "Group6-ClosestPair";
		

		SparkConf conf = new SparkConf().setAppName(appName);
		JavaSparkContext sc = new JavaSparkContext(conf);

		JavaRDD<String> lines = sc.textFile(args[0]);
		// JavaRDD<String> lines =
		// sc.textFile("/home/hdworker/Downloads/TestCaseTA/ClosestPairTestData.csv",
		// partitions);
		// JavaRDD<String> lines = sc.textFile(inputFilePath);

		JavaRDD<Point> points = lines
				.mapPartitions(new FlatMapFunction<Iterator<String>, Point>() {

					/**
			 * 
			 */
					private static final long serialVersionUID = 1L;

					public Iterable<Point> call(Iterator<String> line) {
						String sep = ",";
						String eachline;
						String[] xyvals;
						Point eachPoint;
						List<Point> pointsFromInputFile = new ArrayList<Point>();
						while (line.hasNext()) {
							eachline = line.next();
							xyvals = eachline.split(sep);
							double p1, p2;
							if (null == xyvals || xyvals.length != 2)
								continue;
							try {
								p1 = Double.parseDouble(xyvals[0]);
								p2 = Double.parseDouble(xyvals[1]);
							} catch (Exception e) {
								continue;
							}

							eachPoint = new Point(p1, p2);
							pointsFromInputFile.add(eachPoint);
						}
						return pointsFromInputFile;
					}
				});

		// System.out.println(points.collect().size());

		JavaPairRDD<Point, Point> allPointTuples = points.cartesian(points);

		// System.out.println("Total cart: " + allPointTuples.collect().size());

		JavaPairRDD<Point, Point> distinctPointTuples = allPointTuples
				.filter(new Function<Tuple2<Point, Point>, Boolean>() {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					public Boolean call(Tuple2<Point, Point> tuple)
							throws Exception {
						return !((tuple._1.x == tuple._2.x) && (tuple._1.y == tuple._2.y));
					}
				});

		// System.out.println("Dist cart: " +
		// distinctPointTuples.collect().size());

		JavaRDD<Pair> pairs = distinctPointTuples
				.mapPartitions(new FlatMapFunction<Iterator<Tuple2<Point, Point>>, Pair>() {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					public Iterable<Pair> call(
							Iterator<Tuple2<Point, Point>> tuples)
							throws Exception {
						// TODO Auto-generated method stub
						List<Pair> pairsFromTuples = new ArrayList<Pair>();
						// Pair singlePair = new Pair();
						Tuple2<Point, Point> tuple;
						while (tuples.hasNext()) {
							tuple = tuples.next();
							// singlePair.A = tuples.next()._1;
							// singlePair.B = tuples.next()._2;
							Pair singlePair = new Pair(tuple._1(), tuple._2());
							pairsFromTuples.add(singlePair);
						}
						return pairsFromTuples;
					}
				});


		// System.out.println("Num of partitions charan: " + y.collect());

		Pair minDistPair = pairs.reduce(new Function2<Pair, Pair, Pair>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public Pair call(Pair a, Pair b) throws Exception {
				// TODO Auto-generated method stub
				return (a.distanceLength < b.distanceLength ? a : b);
			}
		});

		// System.out.println(minDistPair);

		Point closestpointA = minDistPair.A;
		Point closestpointB = minDistPair.B;

		List<Point> closestPoints = new ArrayList<Point>();

		// SORTING THE TWO POINTS BASED ON X_COORDINATE AND THEN ON Y-COORDINATE
		if (closestpointA.x < closestpointB.x) {
			closestPoints.add(closestpointA);
			closestPoints.add(closestpointB);
		} else if (closestpointA.x > closestpointB.x) {
			closestPoints.add(closestpointB);
			closestPoints.add(closestpointA);
		} else {
			if (closestpointA.y < closestpointB.y) {
				closestPoints.add(closestpointA);
				closestPoints.add(closestpointB);
			} else {
				closestPoints.add(closestpointB);
				closestPoints.add(closestpointA);
			}
		}

		// System.out.println(closestPoints.toString());

		JavaRDD<Point> closestRDD = sc.parallelize(closestPoints);

		closestRDD.saveAsTextFile(args[1]);
		// closestRDD.saveAsTextFile("/home/hdworker/out.csv");
		// closestRDD.saveAsTextFile(outputFilePath);

		// Output your result, you need to sort your result!!!
		// And,Don't add a additional clean up step delete the new generated
		// file...
		sc.close();
	}
}
