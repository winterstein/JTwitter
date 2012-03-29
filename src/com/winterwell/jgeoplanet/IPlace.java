package com.winterwell.jgeoplanet;


public interface IPlace {

	// TODO id on Yahoo/Twitter?
//	long getId();
	
	String getName();

	String getCountryName();

	/**
	 * @return the centroid (centre of mass) of this Place.
	 * Often approximate!
	 */
	Location getCentroid();
	
	/**
	 * @return the bounding box of this Place. Can be null if unknown.
	 */
	BoundingBox getBoundingBox();
	
}
