package winterwell.jtwitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Status: DRAFT!
 * Twitter's geolocation support.
 * 
 * @see Twitter#setMyLocation(double[])
 * @see Twitter#setSearchLocation(double, double, String)
 * @see Twitter.Status#getLocation()
 * 
 * @author daniel
 */
public class TwitterGeo {

	private final Twitter jtwit;
	private double accuracy;

	public TwitterGeo(Twitter jtwit) {
		assert jtwit.getHttpClient().canAuthenticate();
		this.jtwit = jtwit;
	}
	
	public void setAccuracy(double metres) {
		this.accuracy = metres;
	}
	
	
	public List geoSearch(String query) {
		String url = jtwit.TWITTER_URL+"/geo/search.json";
		Map vars = Twitter.asMap(
				"query", query
		);
		if (accuracy != 0) {
			vars.put("accuracy", String.valueOf(accuracy));
		}
		String json = jtwit.getHttpClient().getPage(url, vars, jtwit.getHttpClient().canAuthenticate());
		JSONObject jo = new JSONObject(json);
		JSONObject jo2 = jo.getJSONObject("result");
		JSONArray arr = jo2.getJSONArray("places");
		List places = new ArrayList(arr.length());
		for (int i=0; i<arr.length(); i++) {
			JSONObject place = arr.getJSONObject(i);
			// interpret it - maybe pinch code from jGeoPlanet?
//			https://dev.twitter.com/docs/api/1/get/geo/id/%3Aplace_id
			places.add(place);
		}
		return places;
	}
	
}
