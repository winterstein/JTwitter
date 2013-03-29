package winterwell.jtwitter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import winterwell.json.JSONArray;
import winterwell.json.JSONException;
import winterwell.json.JSONObject;

import com.winterwell.jgeoplanet.BoundingBox;
import com.winterwell.jgeoplanet.IPlace;
import com.winterwell.jgeoplanet.Location;

/**
 * Support for Twitter's geo location features.
 * 
 * @author Daniel Winterstein
 * 
 */
public class Place implements IPlace, Serializable {

	private static final long serialVersionUID = 1L;
	private BoundingBox boundingBox;
	private String country;
	private String countryCode;
	private List<Location> geometry;
	private String id;
	private String name;

	private String type;
	private Place parent;

	@Override
	public IPlace getParent() {
		return parent;
	}
	
	public Place(JSONObject _place) throws JSONException {
		// e.g. {"id":"0a3e119020705b64","place_type":"city",
		// "bounding_box":{"type":"Polygon",
		// "coordinates":[[[-95.519568,37.303542],[-95.227853,37.303542],[-95.227853,37.383978],[-95.519568,37.383978]]]},
		// "name":"Parsons","attributes":{},
		// "country_code":"US",
		// "url":"http://api.twitter.com/1/geo/id/0a3e119020705b64.json",
		// "full_name":"Parsons, KS","country":"United States"}
		id = InternalUtils.jsonGet("id", _place);
		if (id == null) { // a Yahoo ID?
			id = InternalUtils.jsonGet("woeid", _place);
			// TODO Test Me!
			// TODO should we have a separate id field for Yahoo?
		}
		type = InternalUtils.jsonGet("place_type", _place);
		// name and full_name seem to be much the same, e.g.
		// "City of Edinburgh"?
		name = InternalUtils.jsonGet("full_name", _place);
		if (name == null) {
			name = InternalUtils.jsonGet("name", _place);
		}
		countryCode = InternalUtils.jsonGet("country_code", _place);
		country = InternalUtils.jsonGet("country", _place);
		Object _parent = _place.opt("contained_within");
		// Just the first (?? should we try to poke the chain in here?)
		if (_parent instanceof JSONArray){
			JSONArray pa = (JSONArray) _parent;
			_parent = pa.length()==0? null : pa.get(0);
		}
		if (_parent!=null) {
			this.parent = new Place((JSONObject) _parent);
		}
		// bounding box
		Object bbox = _place.opt("bounding_box");
		if (bbox instanceof JSONObject) {
			// probably the 4 corners of a box
			List<Location> bb = parseCoords((JSONObject) bbox);
			double n=-90, e=-180, s=90, w=180;
			for (Location ll : bb) {
				n = Math.max(ll.latitude, n);
				s = Math.min(ll.latitude, s);
				e = Math.max(ll.longitude, e);
				w = Math.min(ll.longitude, w);
			}
			this.boundingBox = new BoundingBox(new Location(n,e), new Location(s,w));
		}
		Object geo = _place.opt("geometry");
		if (geo instanceof JSONObject) {
			this.geometry = parseCoords((JSONObject) geo);
		}
	}

	/**
	 * @return list of lat/long pairs. Can be null
	 */
	public BoundingBox getBoundingBox() {
		return boundingBox;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public String getCountryName() {
		return country;
	}

	/**
	 * @return list of lat/long pairs. Usually null
	 */
	public List<Location> getGeometry() {
		return geometry;
	}

	/**
	 * Twitter ID for this place. Note: this is not a number.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Call this to get a JSON object with a lot of details.
	 * 
	 * TODO wrap this in TwitterPlace
	 */
	public String getInfoUrl() {
		return "http://api.twitter.com/1/geo/id/" + id + ".json";
	}

	public String getName() {
		return name;
	}

	/**
	 * @return e.g. "city", "admin" Often "admin" (which covers anything), so
	 *         it's not clear how useful this is!
	 */
	public String getType() {
		return type;
	}

	private List<Location> parseCoords(JSONObject bbox) throws JSONException {
		JSONArray coords = bbox.getJSONArray("coordinates");
		// pointless nesting?
		coords = coords.getJSONArray(0);
		List<Location> coordinates = new ArrayList();
		for (int i = 0, n = coords.length(); i < n; i++) {
			// these are longitude, latitude pairs
			JSONArray pt = coords.getJSONArray(i);
			Location x = new Location(pt.getDouble(1), pt.getDouble(0));
			coordinates.add(x);
		}
		return coordinates;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public Location getCentroid() {
		if (boundingBox==null) return null;
		return boundingBox.getCenter();
	}

	@Override
	public String getUID() {
		return id+"@twitter";
	}
}
