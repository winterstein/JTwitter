package winterwell.jtwitter;

import java.util.Date;

import winterwell.json.JSONObject;

/**
 * Info on your Twitter API usage - how many calls do you have to use?
 * 
 * @testedby {@link RateLimitTest}
 * @author daniel
 */
public final class RateLimit {
	
	/**
	 * Pseudo value
	 */
	public static final String RES_STREAM_USER = "/stream/user";
	/**
	 * Pseudo value
	 */
	public static final String RES_STREAM_KEYWORD = "/stream/keyword";

	public static final String RES_USERS_BULK_SHOW = "/users/lookup";
	public static final String RES_USERS_SHOW1 = "/users/show";
	public static final String RES_USER_TIMELINE = "/statuses/user_timeline";
	public static final String RES_MENTIONS = "/statuses/mentions_timeline";
	public static final String RES_SEARCH = "/search/tweets";
	public static final String RES_STATUS_SHOW = "/statuses/show";
	public static final String RES_USERS_SEARCH = "/users/search";
	public static final String RES_FRIENDSHIPS_SHOW = "/friendships/show";
	public static final String RES_TRENDS = "/trends/place";
	public static final String RES_LISTS_SHOW = "/lists/show";


	/*
	 * We use lazy parsing for efficiency (most of these objects will never be
	 * examined).
	 */	
	private String limit;
	private String remaining;
	private String reset;
	private transient Date _reset;

	public RateLimit(String limit, String remaining, String reset) {
		this.limit = limit;
		this.remaining = remaining;
		this.reset = reset;
	}

	RateLimit(JSONObject jrl) {
		this(jrl.getString("limit"), jrl.getString("remaining"), jrl.getString("reset"));
	}

	public int getLimit() {
		return Integer.valueOf(limit);
	}

	public int getRemaining() {
		return Integer.valueOf(remaining);
	}

	/**
	 * @return The date at which the limit will be reset.
	 */
	public Date getReset() {
		if (_reset==null) _reset = InternalUtils.parseDate(reset);
		return _reset;
	}

	/**
	 * @return true if the reset time has passed, so this rate limit no longer
	 *         applies.
	 */
	public boolean isOutOfDate() {
		return getReset().getTime() < System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return remaining;
	}

	/**
	 * Wait until the reset date. This will put the thread to sleep until the
	 * reset date (regardless of whether you still have remaining calls or not).
	 * Does nothing if the reset date has passed.
	 */
	public void waitForReset() {
		Long r = Long.valueOf(reset);
		long now = System.currentTimeMillis();
		long wait = r - now;
		if (wait < 0)
			return;
		try {
			Thread.sleep(wait);
		} catch (InterruptedException e) {
			// wrap this for convenience??
			throw new TwitterException(e);
		}
	}

	/**
	 * See https://dev.twitter.com/docs/rate-limiting/1.1/limits
	 * @param url
	 * @return the resource name this is counted as
	 */
	public static String getResource(String url) {
		// Take the first 2 bits of path		
		if ( ! url.startsWith(Twitter.DEFAULT_TWITTER_URL)) {
			return null;
		}
		int s = Twitter.DEFAULT_TWITTER_URL.length();
		int e = url.indexOf(".json", s);
		if (e==-1) return null;
		int e1 = url.indexOf("/", s+1);
		if (e1==-1 || e1 > e) {			
			return url.substring(s, e);
		}
		int e2 = url.indexOf("/", e1+1);
		if (e2==-1 || e2 > e) {
			return url.substring(s, e);
		}
		return url.substring(s, e2);
	}
}
