package com.winterwell.jgeoplanet;

/**
 * Superclass for other exceptions thrown by the library.
 * This exception is also thrown for network and protocol errors.
 * 
 * @author Joe Halliwell <joe@winterwell.com>
 */
public class GeoPlanetException extends RuntimeException {

	private static final long serialVersionUID = 3442552352655929043L;
	
	GeoPlanetException(Exception e) {
		super(e);
	}

	GeoPlanetException(String message) {
		super(message);
	}
}
