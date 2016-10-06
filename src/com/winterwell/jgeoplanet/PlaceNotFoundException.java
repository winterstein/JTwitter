package com.winterwell.jgeoplanet;

/**
 * Thrown when a place, specified by a WOE ID or search term could not be found.
 * @author Joe Halliwell <joe@winterwell.com>
 */
public class PlaceNotFoundException
extends RuntimeException 
{

	private static final long serialVersionUID = 6987782024287635157L;

	private final String placeName;

	public PlaceNotFoundException(String placeName) {
		super(placeName + " could not be located");
		this.placeName = placeName;
	}

	/**
	 * Return the name (or WOE ID) that could not be found.
	 * NB If a WOEID the representation has the form "11231 (WOE ID)"
	 * @return the name (or WOE ID) that could not be found
	 */
	public String getPlaceName() {
		return placeName;
	}
}
