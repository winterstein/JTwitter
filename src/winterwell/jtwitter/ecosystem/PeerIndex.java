package winterwell.jtwitter.ecosystem;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import winterwell.jtwitter.InternalUtils;
import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.TwitterException;
import winterwell.jtwitter.URLConnectionHttpClient;
import winterwell.jtwitter.User;

/**
 * Access the PeerIndex ranking system
 * @author daniel
 *
 */
public class PeerIndex {

	final String API_KEY;
	
	public PeerIndex(String apiKey) {
		this.API_KEY = apiKey;
	}
	
	IHttpClient client = new URLConnectionHttpClient();
	
	/**
	 * @param screenName a Twitter screen-name
	 * @return
	 */
	public PeerIndexProfile getProfile(User user) {
		Map vars = InternalUtils.asMap(
				"id", user.screenName==null? user.id : user.screenName,
				"api_key", API_KEY
		);
		String json = client.getPage("http://api.peerindex.net/version/profile/show.json", 
				vars, false);
		try {
			JSONObject jo = new JSONObject(json);
			return new PeerIndexProfile(jo);
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}
}
