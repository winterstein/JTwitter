package winterwell.jtwitter;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class TwitterGeoTest {

	@Test
	public void testGeoSearchString() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		TwitterGeo tg = new TwitterGeo(jtwit);
		{
			List eds = tg.geoSearch("Edinburgh");
			System.out.println(eds);
		}
	}

}
