package winterwell.jtwitter;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Support for Twitter's geo location features.
 * <p>
 * Status: experimental & subject to change!
 * @author Daniel Winterstein
 *
 */
public class Place implements Serializable {
	private static final long serialVersionUID = 1L;
	private String id;
	private String type;
	private String countryCode;
	private String name;
	private String country;
	private List<LatLong> boundingBox;
	
	/**
	 * @return list of lat/long pairs
	 */
	public List<LatLong> getBoundingBox() {
		return boundingBox;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	/**
	 * Note: this is not a number.
	 */
	public String getId() {
		return id;
	}


	/**
	 * @return e.g. "city"
	 */
	public String getType() {
		return type;
	}	

	public String getCountryCode() {
		return countryCode;
	}



	public String getName() {
		return name;
	}



	public String getCountryName() {
		return country;
	}



	public Place(JSONObject _place) throws JSONException {
//		e.g. {"id":"0a3e119020705b64","place_type":"city",
		//"bounding_box":{"type":"Polygon",
		//"coordinates":[[[-95.519568,37.303542],[-95.227853,37.303542],[-95.227853,37.383978],[-95.519568,37.383978]]]},
//		"name":"Parsons","attributes":{},
//		"country_code":"US",
//		"url":"http://api.twitter.com/1/geo/id/0a3e119020705b64.json",
//		"full_name":"Parsons, KS","country":"United States"}		
		id = InternalUtils.jsonGet("id", _place);
		if (id==null) {	// a Yahoo ID?
			id = InternalUtils.jsonGet("woeid", _place);
			// TODO Test Me!
			// TODO should we have a separate id field for Yahoo?
		}
		type = InternalUtils.jsonGet("place_type", _place);
		name = InternalUtils.jsonGet("full_name", _place);
		if (name==null) name = InternalUtils.jsonGet("name", _place);
		countryCode = InternalUtils.jsonGet("country_code", _place);
		country = InternalUtils.jsonGet("country", _place);		
		// bounding box
		if (_place.has("bounding_box")) {
			JSONObject bbox = _place.getJSONObject("bounding_box");
			JSONArray coords = bbox.getJSONArray("coordinates");
			// pointless nesting?
			coords = coords.getJSONArray(0);
			List<LatLong> coordinates = new ArrayList();
			for(int i=0,n=coords.length(); i<n; i++) {
				// these are longitude, latitude pairs
				JSONArray pt = coords.getJSONArray(i);
				LatLong x = new LatLong(pt.getDouble(1), pt.getDouble(0));
				coordinates.add(x);
			}
			this.boundingBox = coordinates;
		}
	}
	
	/**
	 * A latitude-longitude coordinate.
	 */
	public static final class LatLong extends AbstractList<Double> {

		public final double latitude;
		public final double longitude;

		public LatLong(double latitude, double longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
		}

		@Override
		public Double get(int index) {
			return index==0? latitude : longitude;
		}

		@Override
		public int size() {
			return 2;
		}		
	}

	/**
	 * Call this to get a JSON object with a lot of details.
	 * 
	 * TODO wrap this in TwitterPlace
	 */
	public String getInfoUrl() {
		return "http://api.twitter.com/1/geo/id/"+id+".json";
	}
}
