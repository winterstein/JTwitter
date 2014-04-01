package com.winterwell.jgeoplanet;



/**
 * Describes a geocoding request.
 * Any aspect of this might be null.
 * 
 * Common scenarios:
 * 
 * 1. desc is set, all others are null
 * 2. locn is set (in which case ignore any other info as low-grade by comparison)
 * 3. desc, timezone and lang are set, all others are null
 * 4. timezone and lang are set, all others are null
 * 
 * Alpha Version requirements: (1) and (2) -- don't bother with timezone yet. 
 *
 * @author daniel
 */
public class GeoCodeQuery {
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((country == null) ? 0 : country.hashCode());
		result = prime * result + ((desc == null) ? 0 : desc.hashCode());
		result = prime * result + ((lang == null) ? 0 : lang.hashCode());
		result = prime * result + ((locn == null) ? 0 : locn.hashCode());
		result = prime * result + (reqGeometry ? 1231 : 1237);
		result = prime * result + (reqLocn ? 1231 : 1237);
		result = prime * result + (reqOnlyCity ? 1231 : 1237);
		result = prime * result + (reqOnlyCountry ? 1231 : 1237);
		result = prime * result
				+ ((timezone == null) ? 0 : timezone.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GeoCodeQuery other = (GeoCodeQuery) obj;
		if (country == null) {
			if (other.country != null)
				return false;
		} else if (!country.equals(other.country))
			return false;
		if (desc == null) {
			if (other.desc != null)
				return false;
		} else if (!desc.equals(other.desc))
			return false;
		if (lang == null) {
			if (other.lang != null)
				return false;
		} else if (!lang.equals(other.lang))
			return false;
		if (locn == null) {
			if (other.locn != null)
				return false;
		} else if (!locn.equals(other.locn))
			return false;
		if (reqGeometry != other.reqGeometry)
			return false;
		if (reqLocn != other.reqLocn)
			return false;
		if (reqOnlyCity != other.reqOnlyCity)
			return false;
		if (reqOnlyCountry != other.reqOnlyCountry)
			return false;
		if (timezone == null) {
			if (other.timezone != null)
				return false;
		} else if (!timezone.equals(other.timezone))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	/**
	 * If true, the query results must include latitude/longitude. 
	 */
	public boolean reqLocn;
	/**
	 * If true, the query results must include shape geometry. 
	 */
	public boolean reqGeometry;
	/**
	 * If true, the query results need only specify country and city (and the search may be simplified/shortened accordingly). 
	 */
	public boolean reqOnlyCity;
	/**
	 * If true, the query results need only specify country (and the search may be simplified/shortened accordingly). 
	 */
	public boolean reqOnlyCountry;
	
	@Override
	public String toString() {
		return "GeoCodeQuery["+desc+"]";
	}
	
	/**
	 * Create a blank query. You must set _some_ properties before using it!
	 */
	public GeoCodeQuery() {	
	}
	
	public GeoCodeQuery(String desc) {
		this.desc = desc;
	}
	
	/**
	 * Free form text description.
	 */
	public String desc;

	/**
	 * Latitude / longitude coordinates.
	 */
	public Location locn;


	/**
	 * @return the bounding box of this Place. Can be null if unknown.
	 */
	public BoundingBox bbox; 
	
	/**
	 * Must use {@link ISO3166} codes.
	 * This is a hint at the country to which the place belongs -- it can be wrong.
	 */
	public String country;
		
	public String city;

	/**
	 * Must use {@link ISO639} codes.
	 * This is a hint at the language used in the description -- it can be wrong.
	 */
	public String lang;

	/**
	 * Can use human labels e.g. "London" or "Pacific Standard Time (US & Canada)", or GMT offsets, e.g. "+0100"
	 * There are many quasi-formats: UTC-20, GMT+0100, GMT+1, PST, EST which it would be nice to support.
	 */
	public String timezone;

	/**
	 * Type of place wanted. Values are: {@link IPlace#TYPE_CITY}, {@link IPlace#TYPE_COUNTRY}.
	 */
	public String type;

	public String getCountryCode() {
		return country;
	}

	public BoundingBox getBoundingBox() {
		return bbox;
	}

	public boolean isEmpty() {
		return (desc==null || desc.isEmpty())
				&& locn==null && bbox==null
				&& city==null && country==null
				&& (timezone==null || lang==null);
	}	

}
