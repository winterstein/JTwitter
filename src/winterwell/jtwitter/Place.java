package winterwell.jtwitter;

import java.io.Serializable;

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
		// TODO bounding box
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
