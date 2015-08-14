/**
 * 
 */
package winterwell.jtwitter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import winterwell.jtwitter.Twitter.ITweet;

/**
 * @WARNING There are bugs on Twitter's end -- the messages returned by this
 *          stream may not include all the messages to a user. The results vary
 *          from user to user!<br>
 *          Recommendation: Use {@link TwitterStream} with the keyword filter
 *          "@screenname" to get messages to you.
 *          <p>
 *          Connect to the streaming API.
 *          <p>
 *          This class picks up the following tweets: <br>
 * 
 *          - Tweets by you <br>
 *          - Tweets that mention you <br>
 *          - Tweets by people you follow IF {@link #setWithFollowings(boolean)}
 *          is true. <br>
 *          - Direct messages (DMs) to you<nr> - Retweets of your messages. <br>
 *          - Retweets made by you.
 * 
 *          <p>
 *          Duplicate messages may be delivered when reconnecting to the
 *          Streaming API.
 * 
 *          TODO test out url-signing over header-signing -- c.f. http://groups
 *          .google.com/group/twitter-development-talk/browse_thread
 *          /thread/420c4b555198aa6c/f85e2507b7f65e39?pli=1
 * 
 *          "figured it out on my own. must use HTTP GET with OAuth params
 *          passed in URI string. Not mentioned in the documentation. Wasted
 *          many hours figuring out this stuff would be clarified if someone
 *          updated the docs and made some examples."
 * 
 * 
 * @author Daniel
 * @testedby {@link UserStreamTest}
 */
public class UserStream extends AStream {

	boolean withFollowings;

	public UserStream(Twitter jtwit) {
		super(jtwit);
	}

	@Override
	HttpURLConnection connect2() throws IOException {
		InternalUtils.log(LOGTAG, "connect2()... "+this);
		connect3_rateLimit();
		// API version 2?! Yes, this is right.
		String url = "https://userstream.twitter.com/2/user.json?delimited=length";
		Map<String, String> vars = new HashMap();
		if (withFollowings) {
			vars = InternalUtils.asMap("with",
					(withFollowings ? "followings" : "user"));
		}
		HttpURLConnection con = client.connect(url, vars, true);
		return con;
	}

	/**
	 * Protect the rate limits & _help_ you avoid annoying Twitter (only
	 * locally! And forgetful! Do NOT rely on this)
	 */
	private void connect3_rateLimit() {
		if (jtwit.getScreenName() == null)
			return; // dunno
		AStream s = user2stream.get(jtwit.getScreenName());
		if (s != null && s.isConnected()) {
			throw new TwitterException.TooManyLogins("One account, one UserStream");
		}
		// memory paranoia
		if (user2stream.size() > 500) {
			// oh well -- forget stuff (this Map is just a safety check)			
			user2stream.clear();
		}
		user2stream.put(jtwit.getScreenName(), this);
	}
	
	/**
	 * Used to help avoid breaking api limits.
	 */
	private final static ConcurrentHashMap<String, AStream> user2stream = new ConcurrentHashMap();
	
	/**
	 * Use the REST API to fill in: mentions of you. Missed you-follow-them
	 * events are automatically generated on reconnect.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	int fillInOutages2(Twitter jtwit2, Outage outage)
			throws UnsupportedOperationException, TwitterException {
		int cnt = 0;
		// fetch
		{	// get mentions of you		
			List<Status> mentions = jtwit2.getMentions();
			InternalUtils.log(LOGTAG, "fillIn mentions "+jtwit2.getSinceId()+": "+mentions.size());
			for (Status status : mentions) {
				if (tweets.contains(status)) {
					continue;
				}
				tweets.add(status);
				cnt++;
			}
		}
		if (withFollowings) { // Get your and network stuff.
			// Can't get too many results, rate-limiting is severe (15x20 results per 15 mins) for this resource
			jtwit2.setMaxResults(100);
			List<Status> updates = jtwit2.getHomeTimeline();
			InternalUtils.log(LOGTAG, "fillIn from-you "+jtwit2.getSinceId()+": "+updates.size());
			for (Status status : updates) {
				if (tweets.contains(status)) {
					continue;
				}
				tweets.add(status);
				cnt++;
			}
			// NB: 100k was the original setting -- see fillInOutages()
			jtwit2.setMaxResults(100000);
		} 	else {	// get your traffic
			List<Status> updates = jtwit2.getUserTimeline(jtwit2.getScreenName());
			InternalUtils.log(LOGTAG, "fillIn from-you "+jtwit2.getSinceId()+": "+updates.size());
			for (Status status : updates) {
				if (tweets.contains(status)) {
					continue;
				}
				tweets.add(status);
				cnt++;
			}
		}
		{	// different since-id for DMs??
//			jtwit2.setSinceId(outage.sinceDMId);
//			jtwit2.setUntilId(outage.untilDMId);			
			jtwit2.setSinceId(InternalUtils.addTimeToStatusId(outage.sinceDMId, -5000L));
			jtwit2.setUntilId(InternalUtils.addTimeToStatusId(outage.untilDMId, 5000L));
			
			jtwit2.setMaxResults(100000);
			List<Message> dms = jtwit2.getDirectMessages();
			// debug info for latency issues
			String dmids = "";
			for (Message message : dms) {
				if (message==null) continue; // paranoia
				dmids += message.getId()+" ";
			}
			InternalUtils.log(LOGTAG, "fillIn DMs "+jtwit2.getSinceId()+": "+dms.size()+" "+dmids);
			for (ITweet dm : dms) {
				if (tweets.contains(dm)) {
					continue;
				}
				tweets.add(dm);
				cnt++;
			}
		}
		// Missed follow events are sort of OK: the reconnect will update
		// friends
		return cnt;
	}

	/**
	 * @return people who the user follows -- at the point when the stream last
	 *         connected.
	 */
	public Collection<Number> getFriends() {
		// ??update the friends list from follow events? But we might miss some during an outage.
		return friends;
	}

	/**
	 * @param withFollowings
	 *            if true, pick up all tweets by the people the user follows.
	 */
	public void setWithFollowings(boolean withFollowings) {
		assert !isConnected();
		this.withFollowings = withFollowings;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("UserStream");
		sb.append("["+jtwit.getScreenNameIfKnown());
		if (withFollowings) sb.append(" +followings");
		sb.append("]");
		return sb.toString();
	}
}
