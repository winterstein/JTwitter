package winterwell.jtwitter.ecosystem;

import java.util.Map;

import winterwell.json.JSONException;
import winterwell.json.JSONObject;
import winterwell.jtwitter.InternalUtils;
import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.TwitterException;
import winterwell.jtwitter.URLConnectionHttpClient;
import winterwell.jtwitter.User;

/**
 * Access the PeerIndex ranking system.
 * 
 * TODO Status: BROKEN by changes at PeerIndex. See:
 * 
 * > The new PeerIndex API... https://developers.peerindex.com
 
 * 
 * @author daniel
 * @testedby {@link PeerIndexTest}
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
				(user.screenName==null? "twitter_screen_name":"twitter_id"), (user.screenName==null? user.id : user.screenName),
				"api_key", API_KEY
		);
		
		String json = client.getPage("https://api.peerindex.com/1/actor/basic.json", 
				vars, false);
		try {
			JSONObject jo = new JSONObject(json);
			return new PeerIndexProfile(jo);
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}
}
