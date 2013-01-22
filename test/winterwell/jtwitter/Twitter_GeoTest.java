package winterwell.jtwitter;

import java.util.List;

import org.junit.Test;

public class Twitter_GeoTest {

	@Test
	public void testGeoSearchString() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter_Geo tg = new Twitter_Geo(jtwit);
		{
			List<Place> eds = tg.geoSearch("Cairo");
			for (Place p : eds) {
				System.out.println(p+" "+p.getCountryCode());	
			}			
			// NO EGYPT?!
		}
		{
			List<Place> eds = tg.geoSearch("Edinburgh");
//			URLConnectionHttpClient client = (URLConnectionHttpClient) jtwit.getHttpClient();
//			System.out.println(client.headers);
			System.out.println(eds);
			Place ed = eds.get(0);
			System.out.println(ed.getId());
//			System.out.println(ed.getBoundingBox());
			System.out.println(ed.getCentroid());
			System.out.println(ed.getType());
//			assert ed.getType().equals(IPlace.TYPE_CITY);
		}
		{
			List<Place> eds = tg.geoSearch("New York");
			System.out.println(eds);
			Place ed = eds.get(0);
			System.out.println(ed.getId());
			System.out.println(ed.getBoundingBox());
			System.out.println(ed.getCentroid());
			System.out.println(ed.getType());
//			assert ed.getType().equals(IPlace.TYPE_CITY);
		}
		{
			List<Place> eds = tg.geoSearch("Paris");
			System.out.println(eds);
			Place ed = eds.get(0);
			System.out.println(ed.getId());
			System.out.println(ed.getBoundingBox());
			System.out.println(ed.getCentroid());
			System.out.println(ed.getType());
//			assert ed.getType().equals(IPlace.TYPE_CITY);
		}
		{
			List<Place> eds = tg.geoSearch("Russia");
			System.out.println(eds);
			Place ed = eds.get(0);
			System.out.println(ed.getId());
			System.out.println(ed.getBoundingBox());
			System.out.println(ed.getCentroid());
			System.out.println(ed.getType());
//			assert ed.getType().equals(IPlace.TYPE_CITY);
		}
	}

}
