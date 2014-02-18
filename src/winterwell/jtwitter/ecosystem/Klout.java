package winterwell.jtwitter.ecosystem;

import java.util.Map;

import winterwell.json.JSONObject;
import winterwell.jtwitter.InternalUtils;
import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.URLConnectionHttpClient;

/**
 * Klout influence scores -- not very reliable, but then what is?
 * @author daniel
 * @testedby {@link KloutTest}
 */
public class Klout {

	final String API_KEY;
	
	public Klout(String apiKey) {
		this.API_KEY = apiKey;
	}
	
	IHttpClient client = new URLConnectionHttpClient();


	public String getKloutID(String twitterName) {		
		Map vars = InternalUtils.asMap("key", API_KEY, "screenName", twitterName);				
		String json = client.getPage(
				"http://api.klout.com/v2/identity.json/twitter", vars, false);
		JSONObject jo = new JSONObject(json);
		return jo.getString("id");
	}
	
	public double getScore(Object kloutID) {		
		JSONObject jo = getScoreObject(kloutID);
		return jo.getDouble("score");
	}
	
	public JSONObject getScoreObject(Object kloutID) {		
		Map vars = InternalUtils.asMap("key", API_KEY);				
		String json = client.getPage("http://api.klout.com/v2/user.json/"+kloutID+"/score", vars, false);
		JSONObject jo = new JSONObject(json);
		return jo;
	}

}
