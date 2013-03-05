package winterwell.jtwitter;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



/**
 * Connect to the streaming API.
 * <p>
 * Duplicate messages may be delivered when reconnecting to the Streaming API.
 * 
 * @author Daniel
 */
public class TwitterStream extends AStream {

	public static enum KMethod {
		/**
		 * Follow hashtags, users or regions
		 */
		filter,

		/** Everything! Requires special access privileges! */
		firehose,

		/** Requires special access privileges! */
		links,

		/**
		 * New-style retweets. Requires special access privileges! From
		 * dev.twitter.com: <i>Few applications require this level of access.
		 * Creative use of a combination of other resources and various access
		 * levels can satisfy nearly every application use case.</i>
		 * */
		retweet,

		/**
		 * Spritzer or Garden-hose: A sample of tweets, suitable for trend
		 * analysis. <br>
		 * The default level (spritzer) is roughly 1% of all public tweets. <br>
		 * The upgraded level (garden-hose - apply to Twitter for this) is 10%. <br>
		 * In both cases the algorithm is based on the tweet-id modulo 100.
		 */
		sample
	}

	/**
	 * Maximum number of keywords which most of us can track.
	 * @see #setTrackKeywords(List)
	 */
	public static int MAX_KEYWORDS = 400;

	/**
	 * Maximum character length of a tracked keyword or phrase.
	 * @see #setTrackKeywords(List)
	 */
	public static final int MAX_KEYWORD_LENGTH = 60;

	/**
	 * Maximum users who can be tracked.
	 * @see #setFollowUsers(List)
	 */
	public static final int MAX_USERS = 5000;

	/**
	 * Used to help avoid breaking api limits.
	 */
	static Map<String, AStream> user2stream = new ConcurrentHashMap();

	private List<Long> follow;

	private List<double[]> locns;

	KMethod method = KMethod.sample;

	private List<String> track;

	/**
	 * 
	 * @param client
	 *            This will have it's timeout set to 90 seconds. So you probably
	 *            don't want to reuse the object with the REST api.
	 */
	public TwitterStream(Twitter jtwit) {
		super(jtwit);
	}

	@Override
	HttpURLConnection connect2() throws Exception {
		connect3_rateLimit();

		String url = "https://stream.twitter.com/"+Twitter.API_VERSION+"/statuses/"+method+".json";
		Map<String, String> vars = new HashMap();		
		if (follow != null && follow.size() != 0) {
			vars.put("follow", InternalUtils.join(follow, 0, Integer.MAX_VALUE));
		}
		if (track != null && track.size() != 0) {
			vars.put("track", InternalUtils.join(track, 0, Integer.MAX_VALUE));
		}
		// If filtering, check we have a filter
		if (vars.isEmpty() && method==KMethod.filter) {
			throw new IllegalStateException("No filters set for "+this);
		}
		vars.put("delimited", "length");
		// use post in case it's a long set of vars
		HttpURLConnection con = client.post2_connect(url, vars);
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
		if (s != null && s.isConnected())
			throw new TwitterException.TooManyLogins(
					"One account, one stream (running: "
							+ s
							+ "; trying to run"
							+ this
							+ ").\n	But streams OR their filter parameters, so one stream can do a lot.");
		
		// memory paranoia
		if (user2stream.size() > 500) {
			// oh well -- forget stuff (this Map is just a safety check)
			user2stream = new ConcurrentHashMap<String, AStream>();
		}
		user2stream.put(jtwit.getScreenName(), this);
	}

	@Override
	void fillInOutages2(Twitter jtwit2, Outage outage) {
		if (method != KMethod.filter)
			throw new UnsupportedOperationException();
		// keywords?
		if (track != null) {
			for (String keyword : track) {
				List<Status> msgs = jtwit.search(keyword);
				for (Status status : msgs) {
					if (tweets.contains(status)) {
						continue;
					}
					tweets.add(status);
				}
			}
		}
		
		// users?
		if (follow != null) {
			for (Long user : follow) {
				List<Status> msgs = jtwit.getUserTimeline(user);
				for (Status status : msgs) {
					if (tweets.contains(status)) {
						continue;
					}
					tweets.add(status);
				}
			}
		}
		// regions?
		if (locns != null && ! locns.isEmpty())
			throw new UnsupportedOperationException("TODO"); // TODO
	}

	/**
	 * @return Can be null
	 */
	public List<String> getTrackKeywords() {
		return track;
	}

	/**
	 * @param userIds Upto 5,000 userids to follow
	 * @throws IllegalArgumentException if userIds is too big
	 */
	public void setFollowUsers(List<Long> userIds) throws IllegalArgumentException {
		method = KMethod.filter;
		if (userIds!=null && userIds.size() > MAX_USERS) {
			throw new IllegalArgumentException("Track upto 5000 users - not "+userIds.size());
		}
		follow = userIds;
	}
	
	/**
	 * @return user-ids which are followed, or null.
	 */
	public List<Long> getFollowUsers() {
		return follow;
	}

	/**
	 * TODO This is not implemented yet!
	 * 25 0.1-360 degree location boxes.
	 * 
	 * Only tweets that are both created using the Geotagging API and are placed
	 * from within a tracked bounding box will be included in the stream – the
	 * user’s location field is not used to filter tweets
	 * 
	 * @param boundingBoxes
	 *            Each element consists of longitude/latitude south-west,
	 *            north-east.
	 */
	@Deprecated // TODO
	public void setLocation(List<double[]> boundingBoxes) {
		method = KMethod.filter;
		this.locns = boundingBoxes;
		throw new RuntimeException("TODO! Not implemented yet (sorry)");
	}

	/**
	 * Set the method. The default is "sample", as this is the only one which
	 * works with no extra settings.
	 * 
	 * @param method
	 */
	void setMethod(KMethod method) {
		this.method = method;
	}

	/**
	 * See https://dev.twitter.com/docs/streaming-api/methods#track
	 * <p> 
	 * Terms are exact-matched, and also exact-matched ignoring punctuation. 
	 * Each term may be up to 60 characters long.
	 * <p>
	 * Exact matching on phrases, that is, keywords with spaces, 
	 * is not supported. Keywords containing punctuation will only exact match 
	 * tokens and, other than keywords prefixed by # and @, will tend to 
	 * never match. Non-space separated languages, such as CJK and 
	 * Arabic, are currently unsupported as tokenization only occurs on 
	 * whitespace and punctuation. Other UTF-8 phrases should exact match 
	 * correctly, but will not substitute similar characters to their 
	 * least-common-denominator. For all these cases, consider falling back 
	 * to the Search REST API.
	 * 
	 * @param keywords
	 *            The default access level allows up to 400 track keywords
	 *            (exceeding this will give an exception -- adjust {@link #MAX_KEYWORDS} if you
	 *            have special privileges).
	 *            You can include phrases, separating words with a space.
	 * @see TwitterStream#MAX_KEYWORDS
	 * @see TwitterStream#MAX_KEYWORD_LENGTH
	 */
	public void setTrackKeywords(List<String> keywords) {
		if (keywords.size() > MAX_KEYWORDS) {
			throw new IllegalArgumentException("Too many tracked terms: "+keywords.size()+" ("+MAX_KEYWORDS+" limit)");
		}
		// check them for length
		for (String kw : keywords) {
			if (kw.length() > MAX_KEYWORD_LENGTH) {
				throw new IllegalArgumentException("Track term too long: "+kw+" (60 char limit)");
			}
		}	
		// we don't check >400 'cos you might have special access
		this.track = keywords;
		method = KMethod.filter;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("TwitterStream");
		sb.append("[" + method);
		if (track != null) {
			sb.append(" track:" + InternalUtils.join(track, 0, 5));
		}
		if (follow != null && follow.size() > 0) {
			sb.append(" follow:" + InternalUtils.join(follow, 0, 5));
		}
		if (locns != null) {
			sb.append(" in:" + InternalUtils.join(locns, 0, 5));
		}
		sb.append(" by:" + jtwit.getScreenNameIfKnown());		
		sb.append("]");
		return sb.toString();
	}

	/**
	 * default: false
	 * If true, json is only sent to listeners, and polling based access 
	 * via {@link #getTweets()} will return no results.
	 * @see #addListener(IListen)
	 */
	public void setListenersOnly(boolean listenersOnly) {
		this.listenersOnly = listenersOnly;
	}

}
