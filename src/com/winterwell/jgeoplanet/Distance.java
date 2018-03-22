package com.winterwell.jgeoplanet;

public class Distance {

	static final double pi180 = Math.PI / 180;
	static final double EARTH_RADIUS = 6378137;
	
	/**
	 * Spherical Earth, so not quite exact
	 * @param a
	 * @param b
	 * @return
	 */
	public static Dx getHaversineDistance(Location a, Location b) {		
		double arcLatA = a.latitude * pi180;
	 	double arcLatB = a.latitude * pi180;
		double x = Math.cos(arcLatA) * Math.cos(arcLatB) * Math.cos((a.longitude - b.longitude) * pi180);
		double y = Math.sin(arcLatA) * Math.sin(arcLatB);
		double s = x + y;
		if (s > 1) {
			s = 1;
		}
		if (s < -1) {
			s = -1;
		}
		double alpha = Math.acos(s);
		double distance = alpha * EARTH_RADIUS;
		return new Dx(distance);

		
	}
	
}
