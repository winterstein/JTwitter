package com.winterwell.jgeoplanet;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ISO3166Test {


	@Test
	public void testUAE() {
		ISO3166 iso = new ISO3166();
		// Q from Fadi
		String syria1 = iso.getCountryCode("سورية");
		String syria2 = iso.getCountryCode("سوريا");
		assert syria1.equals("SY");
		assert syria2.equals("SY");
		// UAE
		String ae = iso.getCountryCode("United Arab Emirates");
		Assert.assertEquals("AE", ae);
	}

		
	@Test
	public void testGetNameForCountryCode() {
		ISO3166 iso = new ISO3166();
		assert iso.getName("gb").equals("United Kingdom");
		assert iso.getName("GB").equals("United Kingdom");		
		assert iso.getName("Britain").equals("United Kingdom");
		System.out.println(iso.getName("US"));
		
		assert iso.getName("SY").startsWith("Syrian") : iso.getName("SY");
		System.out.println(iso.getName("NO"));
	}
	
	@Test
	public void testAFew() {
		ISO3166 iso = new ISO3166();
		assert iso.getCountryCode("United Kingdom").equals("GB");
		assert iso.getCountryCode("USA").equals("US");
	}
	
	@Test
	public void testGetEveryDayName() {
		ISO3166 iso = new ISO3166();
		assert iso.getEverydayName("gb").equals("United Kingdom");
		assert iso.getEverydayName("GB").equals("United Kingdom");
		assert iso.getEverydayName("US").equals("United States");
		assert iso.getEverydayName("SY").equals("Syria") : iso.getEverydayName("SY");
		assert iso.getEverydayName("PS").equals("Palestine");
		assert iso.getEverydayName("LY").equals("Libya");
		assert iso.getEverydayName("FR").equals("France");		
	}
	
	@Test
	public void testGetAllNames() {
		ISO3166 iso = new ISO3166();
		List<String> names = iso.getAllNamesForCode("GB");
		assert names.contains("britain") : names;
		{
			List<String> ros = iso.getAllNamesForCode("RO");
			assert ! ros.contains("RO") : ros;
		}
	}
	
	@Test
	public void testGetCountryCode() {
		ISO3166 iso = new ISO3166();
		assert iso.getCountryCode("Britain").equals("GB");
		assert iso.getCountryCode("United Kingdom").equals("GB");
		assert iso.getCountryCode("UK").equals("GB");
		assert iso.getCountryCode("U.K.").equals("GB") : iso.getCountryCode("U.K.");
		assert iso.getCountryCode("France").equals("FR");
		assert iso.getCountryCode("india").equals("IN");
		assert iso.getCountryCode("USA").equals("US");
		assert iso.getCountryCode("United States").equals("US");
		assert iso.getCountryCode("GB").equals("GB");
		assert iso.getCountryCode("Misr").equals("EG");
	}

}
