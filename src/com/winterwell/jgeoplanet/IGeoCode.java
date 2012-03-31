package com.winterwell.jgeoplanet;

import java.util.concurrent.atomic.AtomicInteger;

public interface IGeoCode {

	/**
	 * Returns the best-guess {@link IPlace} whose name matches the query
	 * to some extent.
	 * @param locationDescription Must not be null or empty
	 * @param confidence Optional - can be null. If not null:<br>
	 *  - The value on calling indicates desired minimum confidence -- a high
	 *  value encourages the geocoder to employ more expensive but certain methods (support is optional).<br> 
	 *  - The value on return will be the _estimated_ confidence 
	 * for the result in [0,1] (so 0=no confidence, 1=total confidence). 
	 * E.g. if there were 2 equally valid outcomes & the geocoder just
	 * picked at random, then confidence = 0.5 (50%).<br>
	 * 
	 * @return place for the description, never null
	 * @throws PlaceNotFoundException if there are no matches for the query
	 * @throws GeoPlanetException on general errors
	 */
	IPlace getPlace(String locationDescription, MFloat confidence);
	
}
