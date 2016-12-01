package com.winterwell.jgeoplanet;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Test;

import com.winterwell.jtwitter.InternalUtils;

public class LocalGeocoderTest {

	@Test
	public void testFindArabia() throws Exception {		
		LocalGeocoder lg = new LocalGeocoder();
		{	// malaysia			
			Location locn = new Location(1.29306, 103.856); 
			IPlace place = lg.getPlace(locn);
			System.out.println(place+" "+place.getCountryCode());
			assert ! place.getCountryCode().equals("MA") : place;
		}
		{
			Location locn = new Location(24.2992, 54.6973); // Abu Dhabi
			IPlace place = lg.getPlace(locn);
			assert place != null;
			System.out.println(place+" "+place.getCountryCode());
			String is = new ISO3166().getCountryCode("United Arab Emirates");
			assert place.getCountryCode().equals(is) : place;
		}
		{
			Location locn = new Location(36.8065, 10.1815); // Tunis
			IPlace place = lg.getPlace(locn);
			assert place != null;
			System.out.println(place+" "+place.getCountryCode());
			String is = new ISO3166().getCountryCode("Tunisia");
			assert place.getCountryCode().equals(is) : place+" "+place.getCountryCode()+" != "+is;
		}
		{
			Location locn = new Location(2.0469, 45.3182); // Mogadishu
			IPlace place = lg.getPlace(locn);
			assert place != null;
			System.out.println(place+" "+place.getCountryCode());
			String is = new ISO3166().getCountryCode("Somalia");
			assert place.getCountryCode().equals(is) : place+" "+place.getCountryCode()+" != "+is;
		}
		{
			Location locn = new Location(31.3547, 34.3088); // Gaza
			IPlace place = lg.getPlace(locn);
			assert place != null;
			System.out.println(place+" "+place.getCountryCode());
			String is = new ISO3166().getCountryCode("Palestine");
			String cn = new ISO3166().getName(place.getCountryCode());
			assert place.getCountryCode().equals(is) : place+" "+place.getCountryCode()+" != "+is;
		}
		{
			Location locn = new Location(31.9038, 35.2034); // Ramallah - too close to Jerusalem
			IPlace place = lg.getPlace(locn);
			assert place != null;
			System.out.println(place+" "+place.getCountryCode());
			String is = new ISO3166().getCountryCode("Palestine");
			assert place.getCountryCode().equals(is) || place.getCountryCode().equals("IL") : place+" "+place.getCountryCode()+" != "+is;
		}
	}

	@Test
	public void testFindIsrael() throws Exception {
		LocalGeocoder lg = new LocalGeocoder();
//		Location locn = new Location(32.0853, 34.7818); // Tel Aviv
		Location locn = new Location(31.7683, 35.2137); // Jerusalam
//		Location locn = new Location(55.9533, -3.1883); // Edinburgh		
		IPlace place = lg.getPlace(locn);
		assert place != null;
		System.out.println(place);	// Israel
		String is = new ISO3166().getCountryCode("Israel");
		assert place.getCountryCode().equals(is) : place;
	}
	
	@Test
	public void testFindMoscow() throws Exception {
		LocalGeocoder lg = new LocalGeocoder();
		IPlace place = lg.getPlace("Moscow");
		assert place != null;
		System.out.println(place);	// Israel
		assert place.getCountryCode().equals(new ISO3166().getCountryCode("Russia")) : place;
	}
	
	@Test
	public void testGetPlaceLocation() throws Exception {
		LocalGeocoder lg = new LocalGeocoder();
		Location locn = new Location(55.9, -3.1);
		IPlace place = lg.getPlace(locn);
		assert place != null;
		System.out.println(place);	// UK
		{
			IPlace plce = lg.getPlace("55.9, -3.1");
			System.out.println(plce);
		}
		{
//			IPlace plce = lg.getPlace("41.687567,-72.787022"); This is not unique
//			System.out.println(plce);
		}
	}
	
	@Test
	public void testFindCairo() throws Exception {
		LocalGeocoder lg = new LocalGeocoder();
		IPlace c1 = lg.getPlace("Cairo");
		System.out.println(c1+"\t"+(c1==null?"":c1.getCountryCode()));
		IPlace c2 = lg.getPlaceLenient("Cairo");
		System.out.println(c2+"\t"+(c2==null?"":c2.getCountryCode()));
	}
	
	/**
	 * TODO support "UK - France" as matching UK or France.
	 * @throws Exception
	 */
//	@Test TODO
	public void test2Countries() throws Exception {
		LocalGeocoder lg = new LocalGeocoder();		
		GeoCodeQuery query = new GeoCodeQuery();
		query.country = "GB";
		IPlace place = new SimplePlace("London / Paris");
		
		IPlace c1 = lg.getPlace(place.getName());
		System.out.println(c1+"\t"+(c1==null?"":c1.getCountryCode()));

		Boolean m = lg.matches(query, place);
		assert m;
	}
	

	@Test
	public void testFindLondon() throws Exception {
		LocalGeocoder lg = new LocalGeocoder();
		Map<IPlace, Double> places = lg.getPlace(new GeoCodeQuery("London"));
		IPlace c1 = InternalUtils.getBest(places);
		System.out.println(c1+"\t"+(c1==null?"":c1.getCountryCode()));
		IPlace c2 = lg.getPlaceLenient("London");
		System.out.println(c2+"\t"+(c2==null?"":c2.getCountryCode()));
	}
	
	@Test
	public void testGetPlaceLenient() throws Exception {
		{
			String cn = "jo";
			Pattern p = Pattern.compile("\\b"+cn+"\\b", Pattern.CASE_INSENSITIVE);
			assert p.matcher("jo").find();
			assert ! p.matcher("joao").find();
			assert p.matcher("amman, jo").find();
		}
		try {
			LocalGeocoder lg = new LocalGeocoder();
			IPlace place = lg.getPlaceLenient("João Pessoa - PB");
			if (place!=null) {
				String country = place.getCountryCode();
				assert ! country.equals("JO") : place;
			}
		} catch (PlaceNotFoundException e) {
			// ok
		}
		LocalGeocoder lg = new LocalGeocoder();
		{			
			try {
				IPlace place0 = lg.getPlace("CA (USA)");
				assert place0 == null;
			} catch(PlaceNotFoundException ex) {
				// OK
			}
			IPlace place = lg.getPlaceLenient("CA (USA)");			
			String country = place.getCountryCode();
			assert country.equals("US") : place;			
		}
	}
	
	@Test
	public void testGetPlaceLenient2() {
		LocalGeocoder lg = new LocalGeocoder();
		{
			IPlace place = lg.getPlaceLenient2("korea...晋州....");
			String country = place.getCountryCode();
			assert country.equals("KR") : place;
		}
	}
	

	@Test
	public void testGetPlaceCities() throws Exception {
		LocalGeocoder lg = new LocalGeocoder();
		{
			IPlace place = lg.getPlace("London");
			assert place != null;
			System.out.println(place+" "+place.getCountryCode());
		}
		{
			IPlace place = lg.getPlaceLenient("São Paulo");
			assert place != null;
			System.out.println(place+" "+place.getCountryCode());
		}
		{
			IPlace place0 = lg.getPlaceLenient("Mexico City");
			assert place0 != null;
			IPlace place = lg.getPlaceLenient("Ciudad de México");
			assert place != null;
			System.out.println(place+" "+place.getCountryCode());
		}
		{
			String tokyo = "東京";
			IPlace place0 = lg.getPlace(tokyo);
			System.out.println(place0+"\t"+place0.getCountryCode());
		}
	}
	
	@Test
	public void testGetPlaceString() throws Exception {		
		ISO3166 iso = new ISO3166();
		LocalGeocoder lg = new LocalGeocoder();				
		String[] egs = new String[]{"France=FR", " Saitama Japan=JP","Kyoto Japan=JP","Australia=AU",
				"Egypt=EG", "Misr=EG","Mossoró-RN/Brasil=BR"};
		for(String eg : egs) {
			String[] nameCode = eg.split("=");
			IPlace place = lg.getPlaceLenient(nameCode[0]);
			assert place != null;
			System.out.println(eg+" = "+place+" in "+place.getCountryCode());
			assert iso.getCountryCode(place.getCountryCode()).equals(nameCode[1]) : place;
		}				
	}
	
	/**
	 * TODO seems to work, could do with some assertions, bit of a rush today tho.
	 * @throws IOException
	 * @throws PlaceNotFoundException
	 */
	@Test
	public void testPlacesTempLocation() throws IOException, PlaceNotFoundException{
		LocalGeocoder lg = new LocalGeocoder();
		SimplePlace spuk = (SimplePlace) lg.getPlaceLenient("uk");
		Location spukc = spuk.getCentroid();
		SimplePlace spv = (SimplePlace) lg.getPlaceLenient("vietnam");
		Location spvc = spv.getCentroid();
		SimplePlace spnz = (SimplePlace) lg.getPlaceLenient("new zealand");
		Location spnzc = spnz.getCentroid();
		SimplePlace sps = (SimplePlace) lg.getPlaceLenient("mongolia");
		Location spsc = sps.getCentroid();
		SimplePlace spr = (SimplePlace) lg.getPlaceLenient("russia");
		Location sprc = spr.getCentroid();

		
		String pl = "";
		
	}
}

