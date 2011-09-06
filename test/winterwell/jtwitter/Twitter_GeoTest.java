package winterwell.jtwitter;

import java.util.List;

import org.junit.Test;

public class Twitter_GeoTest {

	@Test
	public void testGeoSearchString() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter_Geo tg = new Twitter_Geo(jtwit);
		{
			List eds = tg.geoSearch("Edinburgh");
			System.out.println(eds);
		}
	}

}
