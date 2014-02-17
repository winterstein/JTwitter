package com.winterwell.jgeoplanet;

import java.util.Map;


public interface IGeoCode {
	
	/**
	 * @param query Cannot be null.
	 * @param place Cannot be null.
	 * @return true if place fits this query, e.g. London is in the UK, false if it doesn't.
	 * null if we don't know
	 */
	public Boolean matches(GeoCodeQuery query, IPlace place);
	
	/**
	 * Returns the best-guess {@link IPlace} whose name matches the query
	 * to some extent.
	 * 
	 * @param locationDescription Must not be null or empty	 * 
	 * @return place for the description, or null
	 * 
	 * @throws GeoPlanetException on general errors
	 */
	IPlace getPlace(String locationDescription);
	

	/**
	 *
	 * @return Map of candidate results, with the relative confidence.
	 * Confidence is a score in the 0-1 range. It doesn't have any definite meaning (though
	 * we might informally think of it as like a probability).
	 * <p>
	 * E.g. a query for "Paris" might return
	 * {Paris-France: 0.98, Paris-Texas: 0.1, other: 0.1}
	 * <p>
	 * Can be empty, never null.
	 *
	 */
	Map<IPlace,Double> getPlace(GeoCodeQuery query);

}
