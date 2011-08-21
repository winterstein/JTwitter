/**
 * 
 */
package winterwell.jtwitter;

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
public class UserStream {

	private final IHttpClient client;

	List<TwitterEvent> events = new ArrayList<TwitterEvent>();
	List<ITweet> tweets = new ArrayList();
	
	List<Long> friends;

	/**
	 * Needed for constructing some objects.
	 */
	private Twitter jtwit;

	int previousCount;

	StreamGobbler readThread;

	private InputStream stream;

	boolean withFollowings;

	public UserStream(IHttpClient client) {
		this.client = client;
		this.jtwit = new Twitter(null, client);
		// Twitter send 30 second keep-alive pulses, but ask that
		// you wait 3 cycles before disconnecting
		client.setTimeout(90 * 1000);
		// this.user = jtwit.getScreenName();
		// if (user==null) {
		//
		// }
	}

	public void close() {
		if (readThread != null) {
			readThread.pleaseStop();
		}
		URLConnectionHttpClient.close(stream);
	}

	public void connect() {
		close();
		try {
			String url = "https://userstream.twitter.com/2/user.json?delimited=length";
			Map<String, String> vars = Twitter.asMap("with",
					(withFollowings ? "followings" : "user"));
			HttpURLConnection con = client.connect(url, vars, true);
			stream = con.getInputStream();
			readThread = new StreamGobbler(stream);
			readThread.start();
		} catch (Exception e) {
			throw new TwitterException(e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		// TODO scream blue murder if this is actually needed
		close();
	}

	/**
	 * @return the recent events. Calling this will clear the list of events.
	 */
	public List<TwitterEvent> popEvents() {
		read();
		List evs = events;
		events = new ArrayList();
		return evs;
	}

	/**
	 * @return the recent events. Calling this will clear the list of tweets.
	 */
	public List<ITweet> popTweets() {
		read();
		List<ITweet> ts = tweets;
		tweets = new ArrayList();
		return ts;
	}
	
	public List<TwitterEvent> getEvents() {
		read();
		return events;
	}
	
	public List<ITweet> getTweets() {
		read();
		return tweets;
	}

	void read() {
		assert readThread.isAlive();
		String[] jsons = readThread.popJsons();		
		for (String json : jsons) {
			try {
				JSONObject jo = new JSONObject(json);
				// the 1st object is a list of friend ids
				JSONArray _friends = jo.optJSONArray("friends");
				if (_friends != null) {
					friends = new ArrayList(_friends.length());
					for (int i = 0, n = _friends.length(); i < n; i++) {
						friends.add(_friends.getLong(i));
					}
					continue;
				}
				
				// TODO tweets
				System.out.println(jo);
				if (jo.has("text")) {
					Status tweet = new Twitter.Status(jo, null);
					tweets.add(tweet);
					continue;
				}
				
				// Events
				String eventType = jo.optString("event");
				if (eventType != "") {
					TwitterEvent event = new TwitterEvent(jo, jtwit);
					events.add(event);
				}
			} catch (JSONException e) {
				throw new TwitterException.Parsing(json, e);
			}
		}
	}

	/**
	 * How many messages prior-to-connecting to retrieve
	 * 
	 * @param previousCount
	 *            Up to 150,000 but subject to change.
	 * 
	 *            Negative values are allowed -- they mean the stream will
	 *            terminate when it reaches the end of the historical messages.
	 */
	public void setPreviousCount(int previousCount) {
		this.previousCount = previousCount;
	}

	public void setWithFollowings(boolean withFollowings) {
		this.withFollowings = withFollowings;
	}

}
