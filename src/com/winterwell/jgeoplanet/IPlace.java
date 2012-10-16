package com.winterwell.jgeoplanet;


public interface IPlace {

	/**
	 * Place-type for cities. Geocoders should try to use this when applicable.
	 * Users are warned that this is not guaranteed.
	 */
	public static final String TYPE_CITY = "city";
	/**
	 * Place-type for countries. Geocoders should try to use this when applicable.
	 * Users are warned that this is not guaranteed.
	 */
	public static final String TYPE_COUNTRY = "country";
	
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

	/**
	 * @return type of place. Values are geocoder specific, though {@link #TYPE_CITY}
	 * and {@link #TYPE_COUNTRY} should be universal.
	 * Can be null.
	 */
	String getType();

	/**
	 * An id which should be unique across services & stable
	 * across time & servers
	 * (i.e. the same place will give the same answer tomorrow
	 * or on a different computer).<br>
	 * In Winterwell projects, we usually call this an XId (eXternal-Id).
	 * <p>
	 * No hard guarantees are made for these properties!
	 * Implementations should make best reasonable efforts.
	 * 
	 * @return service-specific-id@service-identifier, where service-identifier
	 * must not include "@".
	 * E.g. "54732176@twitter" 
	 */
	String getUID();
	
}
