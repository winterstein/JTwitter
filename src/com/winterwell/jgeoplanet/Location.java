package com.winterwell.jgeoplanet;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import winterwell.utils.reporting.Log;

/**
 * A geographical point-location expressed as a latitude and longitude.
 *
 * @author Joe Halliwell <joe@winterwell.com>
 */
public class Location implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * in metres
	 */
	private final static double DIAMETER_OF_EARTH = 6378.1 * 2 * 1000;


	
	public final double longitude;
	public final double latitude;

	/**
	 * Construct a new location object. Handy for computing distances.
	 * 
	 * @param latitude the latitiude of the location. Must be >-90 and <90
	 * @param longitude the longitude of the location. Will be normalised to between >-180 and <180
	 * @throws IllegalArgumentException if the co-ordinates aren't valid (i.e. bad latitude)
	 */
	public Location(double latitude, double longitude) throws IllegalArgumentException {
		// Normalise the lat/long coords
		if (latitude < -90 || latitude > 90) {
			throw new IllegalArgumentException("Invalid latitude: " + latitude+", "+longitude);
		}
		if (longitude < -180 || longitude > 180) {
			longitude = longitude % 360;
			if (longitude > 180) longitude = 360 - longitude;
			else if (longitude < -180) longitude = longitude + 360;
			assert longitude >= -180 || longitude <= 180 : longitude;
		}
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * Returns the latitude of this location.
	 * @return the latitude of this location.
	 */
	public double getLatitude() {
		return latitude;
	}
	
	public double[] getLatLong() {
		return new double[]{latitude,longitude};
	}
	
	
	/**
	 * Returns  the longitude of this location. A number between -180 and 180.
	 * @return the longitude of this location.
	 */
	public double getLongitude() {
		return longitude;
	}


	/**
	 * Rough and ready distance in metres between this location
	 * and the specified other.
	 * Uses the Haversine formula.
	 * @see http://en.wikipedia.org/wiki/Great-circle_distance
	 * @param other
	 * @return distance in *metres*
	 */
	public Dx distance(Location other) {
		final double lat = latitude * Math.PI / 180;
		final double lon = longitude * Math.PI / 180;
		final double olat = other.latitude * Math.PI / 180;
		final double olon = other.longitude * Math.PI / 180;

		double sin2lat = Math.sin((lat - olat)/2);
		sin2lat = sin2lat * sin2lat;
		double sin2long = Math.sin((lon - olon)/2);
		sin2long = sin2long * sin2long;
		double m = DIAMETER_OF_EARTH * Math.asin(
				Math.sqrt(sin2lat + Math.cos(lat) * Math.cos(olat) * sin2long));
		return new Dx(m, LengthUnit.METRE);
	}


	/**
	 * (approximate) location offset from this one. 
	 * FIXME test!
	 * @param metresEast The movement is not capped. 
	 * @param metresNorth The movement will be capped at the N/S pole if it runs past.
	 * @return a new Location
	 */
	public Location move(double metresNorth, double metresEast) {

		double RadiusOfEarth = DIAMETER_OF_EARTH/2; 
		
		//Coordinate offsets in radians
		double dLat = metresNorth/RadiusOfEarth;
		double dLon = metresEast/(RadiusOfEarth*Math.cos(Math.PI*latitude/180));
		
		//OffsetPosition, decimal degrees
		double latO = latitude + dLat * 180/Math.PI;
		double lonO = longitude + dLon * 180/Math.PI;
		 
		return new Location(latO, lonO);
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(latitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Location other = (Location) obj;
		if (Double.doubleToLongBits(latitude) != Double
				.doubleToLongBits(other.latitude)) {
			return false;
		}
		if (Double.doubleToLongBits(longitude) != Double
				.doubleToLongBits(other.longitude)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "(" + latitude + " N, " + longitude + " E)";
	}

	/** 
	 * 
	 * @returns say 12.5343,-45.43434
	 */
	public String toSimpleCoords(){
		return latitude + "," + longitude;
	}
	
	// NB: will not pick up a trailing . See bug #9788
	public static final Pattern latLongLocn = Pattern.compile(
			"(\\S+:)?\\s*(-?[\\d\\.]*\\d),\\s*(-?[\\d\\.]*\\d)\\.?\\s*");

	/**
	 * Try to parse a string as a latitude/longitude pair.
	 * @param locnDesc
	 * @return Location or null on failure
	 */
	public static Location parse(String locnDesc) {
		// Is it a longitude/latitude pair?
		Matcher m = latLongLocn.matcher(locnDesc);
		if ( ! m.matches()) return null;
		String lat = m.group(2);
		String lng = m.group(3);
		try {
			double _lat = Double.valueOf(lat);
			if (Math.abs(_lat) > 90) {
				// Bogus latitude -- beyond a pole!
				// NB: longitude we allow to loop.
				return null;
			}
			return new Location(_lat, Double.valueOf(lng));
		} catch(NumberFormatException ex) {
			// e.g. 5.50978.58,-0.27849719
			Log.w("Location.parse", locnDesc+" "+ex);
			return null;
		}
	}

}