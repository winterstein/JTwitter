package winterwell.jtwitter.ecosystem;

import java.text.ParseException;
import java.util.Map;

import winterwell.json.JSONException;
import winterwell.json.JSONObject;
import winterwell.jtwitter.InternalUtils;
import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.TwitterException;
import winterwell.jtwitter.URLConnectionHttpClient;

/**
 * Client for the twittercounter.com service
 * @author daniel
 */
public class TwitterCounter {

	final String apiKey;
	IHttpClient client = new URLConnectionHttpClient();
	
	/**
	 * Access the low-level http client.
	 * @deprecated Not many use-cases. 
	 */	
	public IHttpClient getClient() {
		return client;
	}
	
	public TwitterCounter(String twitterCounterApiKey) {
		this.apiKey = twitterCounterApiKey;
	}
	
	public TwitterCounterStats getStats(Number twitterUserId) {
		Map<String, String> vars = InternalUtils.asMap(
				"twitter_id", twitterUserId,
				"apikey", apiKey
//				 ,"count", 20 // the number of days for which you would like stats returned (if available). Default (and for now maximum) is 14 days (two weeks)
				);				
		String json = client.getPage("http://api.twittercounter.com/", vars, false);
		try {
			JSONObject jo = new JSONObject(json);
			return new TwitterCounterStats(jo);
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		} catch (ParseException e) {
			throw new TwitterException.Parsing(json, e);
		}		
	}		
}
