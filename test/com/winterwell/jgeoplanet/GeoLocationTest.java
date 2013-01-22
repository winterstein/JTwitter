package com.winterwell.jgeoplanet;

import org.junit.Test;

public class GeoLocationTest {

	@Test
	public void testParse() {
		Location ed = Location.parse("55, -3.5");
		assert ed != null;
		System.out.println(ed);		
	}

}
