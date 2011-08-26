/**
 * 
 */
package winterwell.jtwitter;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import winterwell.jtwitter.AStream.Outage;
import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.Twitter.User;
import winterwell.utils.TodoException;

/**
 * @deprecated There are bugs on Twitter's end -- the messages returned by this
 * stream may not include all the messages to a user. The results vary from 
 * user to user!
 * <p>
 * Connect to the streaming API.
 * <p>
 * This class picks up the following tweets: <br>
 * 
 *  - Tweets by you <br>
 *  - Tweets that mention you <br>
 *  - Tweets by people you follow IF {@link #setWithFollowings(boolean)} is true. <br>
 *  - Retweets of your messages. <br>
 *  - Retweets made by you.
 *  
 * <p>
 * Duplicate messages may be delivered when reconnecting to the Streaming API.

 * @author Daniel
 * @testedby {@link UserStreamTest}
 */
public class UserStream extends AStream {
	
	/**
	 * @return people who the user follows -- at the point when the stream
	 * last connected.
	 */
	public Collection<Long> getFriends() {
		// TODO update the friends list from follow events??
		return friends;
	}	
	
	/**
	 * Use the REST API to fill in: mentions of you.
	 * <p>
	 * Missed follow events are automatically generated on reconnect. 
	 * 
	 * <p>From <i>dev.twitter.com</i>:<br>
	 * Do not resume REST API polling immediately after a stream failure. 
		// Wait at least a minute or two after the initial failure before you begin REST API polling. 
		// This delay is crucial to prevent dog-piling the REST API in the event of a minor hiccup on 
		// the streaming API.
		 *</p>
	 */
	@Override
	public void fillInOutages() throws UnsupportedOperationException, TwitterException {
		Outage[] outs = outages.toArray(new Outage[0]);
		// protect our original object from edits
		User self = jtwit.getSelf();
		Twitter jtwit2 = new Twitter(self.getScreenName(), jtwit.getHttpClient());
		for (Outage outage : outs) {			
			jtwit2.setSinceId(outage.sinceId);
			jtwit2.setUntilDate(new Date(outage.untilTime));
			jtwit2.setMaxResults(100000); // hopefully not needed!
			// fetch
			if (withFollowings) {
				// TODO pull in network activity
				throw new UnsupportedOperationException("TODO");
			}			
			// get mentions of you
			List<Status> mentions = jtwit2.getMentions();
			for (Status status : mentions) {
				if (tweets.contains(status)) continue;
				tweets.add(status);
			}
			// get your traffic
			List<Status> updates = jtwit2.getUserTimelineWithRetweets(self.getScreenName());
			for (Status status : updates) {
				if (tweets.contains(status)) continue;
				tweets.add(status);
			}
			// success			
			outages.remove(outage);
		}
		// Missed follow events are sort of OK: the reconnect will update friends
	}
	
	boolean withFollowings;

	public UserStream(Twitter jtwit) {
		super(jtwit);
	}

	HttpURLConnection connect2() throws IOException {
		String url = "https://userstream.twitter.com/2/user.json?delimited=length";
		Map<String, String> vars = InternalUtils.asMap("with",
				(withFollowings ? "followings" : "user"));
		HttpURLConnection con = client.connect(url, vars, true);
		return con;
	}

	/**
	 * @param withFollowings if true, pick up all tweets by the people the user follows. 
	 */
	public void setWithFollowings(boolean withFollowings) {
		assert ! isConnected();
		this.withFollowings = withFollowings;
	}

}
