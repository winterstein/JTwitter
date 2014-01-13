package winterwell.jtwitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import winterwell.json.JSONArray;
import winterwell.json.JSONException;
import winterwell.json.JSONObject;

import com.winterwell.jgeoplanet.GeoCodeQuery;
import com.winterwell.jgeoplanet.IGeoCode;
import com.winterwell.jgeoplanet.IPlace;
import com.winterwell.jgeoplanet.Location;
import com.winterwell.jgeoplanet.MFloat;

/**
 * Twitter's geolocation support. Use {@link Twitter#geo()} to get one of these
 * objects.
 * <p>
 * Conceptually, this is an extension of {@link Twitter}. The methods are here
 * because Twitter was getting crowded.
 * 
 * @see Twitter#setMyLocation(double[])
 * @see Twitter#setSearchLocation(double, double, String)
 * @see Status#getLocation()
 * 
 * @author Daniel Winterstein
 * @testedby {@link Twitter_GeoTest}
 */
public class Twitter_Geo implements IGeoCode {

	private double accuracy;

	private final Twitter jtwit;

	/**
	 * Use {@link Twitter#geo()} to get one.
	 * 
	 * @param jtwit
	 */
	Twitter_Geo(Twitter jtwit) {
		assert jtwit != null;
		this.jtwit = jtwit;
	}

	public List geoSearch(double latitude, double longitude) {
		throw new RuntimeException();
	}

	public List<Place> geoSearch(String query) {
		// quick-fail if we know we're rate limited??
//		if (jtwit.isRateLimited(KRequestType.NORMAL, 1)) {
//			throw new TwitterException.RateLimit("enhance your calm");
//		}		
		String url = jtwit.TWITTER_URL + "/geo/search.json";
		Map vars = InternalUtils.asMap("query", query);
		if (accuracy != 0) {
			vars.put("accuracy", String.valueOf(accuracy));
		}
		String json = jtwit.getHttpClient().getPage(url, vars, true);
		try {
			JSONObject jo = new JSONObject(json);
			JSONObject jo2 = jo.getJSONObject("result");
			JSONArray arr = jo2.getJSONArray("places");
			List places = new ArrayList(arr.length());
			for (int i = 0; i < arr.length(); i++) {
				JSONObject _place = arr.getJSONObject(i);
				// interpret it - maybe pinch code from jGeoPlanet?
				// https://dev.twitter.com/docs/api/1/get/geo/id/%3Aplace_id
				Place place = new Place(_place);
				places.add(place);
			}
			return places;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	public List geoSearchByIP(String ipAddress) {
		throw new RuntimeException();
	}

	/**
	 * @param woeid
	 * @return regions from which you can get trending info
	 * @see Twitter#getTrends(Number)
	 */
	public List<Place> getTrendRegions() {
		String json = jtwit.getHttpClient().getPage(
				jtwit.TWITTER_URL + "/trends/available.json", null, false);
		try {
			JSONArray json2 = new JSONArray(json);
			List<Place> trends = new ArrayList();
			for (int i = 0; i < json2.length(); i++) {
				JSONObject ti = json2.getJSONObject(i);
				Place place = new Place(ti);
				trends.add(place);
			}
			return trends;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	public void setAccuracy(double metres) {
		this.accuracy = metres;
	}

	@Override
	public Place getPlace(String locationDescription) {
		Map<IPlace, Double> places = getPlace(new GeoCodeQuery(locationDescription));
		if (places.isEmpty()) return null;
		return (Place) InternalUtils.getBest(places);
	}

	@Override
	public Map<IPlace, Double> getPlace(GeoCodeQuery query) {
		if (query.desc==null || query.desc.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		List<Place> places = geoSearch(query.desc);
		if (places.size()==0) return Collections.EMPTY_MAP;
		// a unique answer?
		if (places.size()==1) {
			return Collections.singletonMap((IPlace)places.get(0), 0.8);
		}		
		return InternalUtils.prefer(query, places, IPlace.TYPE_CITY, 0.8);
	}

	@Override
	public Boolean matches(GeoCodeQuery query, IPlace place) {
		// What can we test easily?
		return InternalUtils.geoMatch(query, place);
	}

	

}
