/**
 * 
 */
package winterwell.jtwitter;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.Twitter.Status;

/**
 * Connect to the streaming API.
 * 
 * <p>
 * Duplicate messages may be delivered when reconnecting to the Streaming API.
 * <p>
 * Status: This class is in an early stage, and may change.
 * 
 * @author Daniel
 * @testedby {@link UserStreamTest}
 */
public class UserStream extends AStream {
	
	boolean withFollowings;

	public UserStream(IHttpClient client) {
		super(client);
	}

	HttpURLConnection connect2() throws IOException {
		String url = "https://userstream.twitter.com/2/user.json?delimited=length";
		Map<String, String> vars = Twitter.asMap("with",
				(withFollowings ? "followings" : "user"));
		HttpURLConnection con = client.connect(url, vars, true);
		return con;
	}

	public void setWithFollowings(boolean withFollowings) {
		this.withFollowings = withFollowings;
	}

}
