package edu.asu.cse512.functions;

import org.apache.spark.api.java.function.PairFunction;

import com.vividsolutions.jts.geom.Coordinate;

import scala.Tuple2;

public class CoordinatePairFunction implements PairFunction<Coordinate, Coordinate, Boolean> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Tuple2<Coordinate, Boolean> call(Coordinate coordinate) throws Exception {
		return new Tuple2<Coordinate, Boolean>(coordinate, Boolean.TRUE);
	}

}
