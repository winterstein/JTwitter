package winterwell.jtwitter;

import java.util.List;

import org.junit.Test;

import com.winterwell.jgeoplanet.IPlace;

public class Twitter_GeoTest {

	@Test
	public void testGeoSearchString() {
		Twitter jtwit = new Twitter(); //TwitterTest.newTestTwitter();
		Twitter_Geo tg = new Twitter_Geo(jtwit);
		{
			List<Place> eds = tg.geoSearch("Edinburgh");
			System.out.println(eds);
			Place ed = eds.get(0);
			System.out.println(ed.getId());
			System.out.println(ed.getBoundingBox());
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
