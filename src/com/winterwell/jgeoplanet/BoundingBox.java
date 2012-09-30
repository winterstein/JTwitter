package com.winterwell.jgeoplanet;

import winterwell.json.JSONException;
import winterwell.json.JSONObject;


/**
 * A region of the Earth's surface defined by four corners and some
 * great circles.
 * <p>
 * Regions that cover a pole are not supported.
 * </p>
 * @author Joe Halliwell <joe@winterwell.com>
 */
public class BoundingBox {

	final Location northEast;
	final Location southWest;
	
	public BoundingBox(Location northEast, Location southWest) {
		if (northEast.latitude < southWest.latitude) {
			throw new IllegalArgumentException("North east corner is south of south west corner");
		}
		this.northEast = northEast;
		this.southWest = southWest;
	}
	
	public Location getCenter() {
		Location ne = northEast;
		Location sw = southWest;
		// FIXME check for wrap-around & pick the smaller slice of the Earth!
		// e.g. Russia ne.latitude=20, sw.latitude=-170
		// we should do -170=190, centre = 105 -- but we say -75
		double tempLat = (ne.latitude + sw.latitude)/2;
		if (Math.abs(ne.latitude - sw.latitude) > 90) {
			if (tempLat <= 0){tempLat += 90;}
			else {tempLat -= 90;}
		}
		double tempLong = (ne.longitude + sw.longitude)/2;
		//Edge case, goes over the +180 / -180 line if the mid point is in the positive#
		//It should be added to -180, if it's in the negative it should be added to 180
		if (Math.abs(ne.longitude - sw.longitude) > 180) {
			if (tempLong <= 0){tempLong += 180;}
			else {tempLong -= 180;}							
		}
		Location tempCentroid= new Location(tempLat,tempLong);
		return tempCentroid;
	}
	
	BoundingBox(JSONObject bbox) throws JSONException {
		this(	getLocation(bbox.getJSONObject("northEast")), 
				getLocation(bbox.getJSONObject("southWest")));
	}
	
	public BoundingBox(Location centre, Dx radius) {
		double r = radius.getMetres();
		northEast = centre.move(r, r);
		southWest = centre.move(-r, -r);
	}

	public Location getNorthEast() {
		return northEast;
	}
	
	public Location getSouthWest() {
		return southWest;
	}
	
	public Location getNorthWest() {
		return new Location(northEast.latitude, southWest.longitude);
	}
	
	public Location getSouthEast() {
		return new Location(southWest.latitude, northEast.longitude);
	}
	
	/**
	 * Determine whether the specified location is contained within this bounding
	 * box.
	 * @param location the location to test
	 * @return true if the location is within this bounding box. False otherwise.
	 */
	public boolean contains(Location location) {
		if (location.latitude > northEast.latitude) return false;
		if (location.latitude < southWest.latitude) return false;
		if (northEast.longitude < 0	&& southWest.longitude >= 0 && southWest.longitude > northEast.longitude) {
			if (location.longitude < 0 && location.longitude > northEast.longitude) return false;
			if (location.longitude >= 0 && location.longitude < southWest.longitude) return false;
		}
		else {
			if (location.longitude > northEast.longitude) return false;
			if (location.longitude < southWest.longitude) return false;
		}
		return true;
	}
		
	/**
	 * Determine whether the specified bounding box is completely contained 
	 * within this one.
	 * @param other the bounding box to test
	 * @return true if the other bounding box is completely contained within this one. False otherwise.
	 */
	public boolean contains(BoundingBox other) {
		return (contains(other.southWest) && contains(other.northEast));
	}
	
	public boolean intersects(BoundingBox other) {
		return (contains(other.northEast)
				|| contains(other.southWest)
				|| contains(other.getNorthWest())
				|| contains(other.getSouthEast()));
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((northEast == null) ? 0 : northEast.hashCode());
		result = prime * result
				+ ((southWest == null) ? 0 : southWest.hashCode());
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
		BoundingBox other = (BoundingBox) obj;
		if (northEast == null) {
			if (other.northEast != null)
				return false;
		} else if (!northEast.equals(other.northEast))
			return false;
		if (southWest == null) {
			if (other.southWest != null)
				return false;
		} else if (!southWest.equals(other.southWest))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BoundingBox [northEast=" + northEast + ", southWest="
				+ southWest + "]";
	}

	static Location getLocation(JSONObject jo) throws JSONException {
		return new Location(
				jo.getDouble("latitude"), jo.getDouble("longitude"));
	}

	public boolean isPoint() {
		return northEast.equals(southWest);
	}
	
}
