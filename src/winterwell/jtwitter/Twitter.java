package winterwell.jtwitter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import winterwell.jtwitter.TwitterException.E401;
import winterwell.jtwitter.TwitterException.E403;
import winterwell.jtwitter.TwitterException.SuspendedUser;

/**
 * Java wrapper for the Twitter API version {@value #version}
 * <p>
 * Example usage:<br>
 * First, you should get the user to authorise access via OAuth. There are a
 * couple of ways of doing this -- we show one below -- see
 * {@link OAuthSignpostClient} for more details.
 * <p>
 * Note that you don't need to do this for some operations - e.g. you can look
 * up public posts without logging in (use the {@link #Twitter()} constructor.
 * You can also - for now! - use username and password to login, but Twitter
 * plan to switch this off soon.
 *
 * <code><pre>
	// First, OAuth to login: Make an oauth client
	OAuthSignpostClient oauthClient = new OAuthSignpostClient(JTWITTER_OAUTH_KEY, JTWITTER_OAUTH_SECRET, "oob");
    // open the authorisation page in the user's browser
    oauthClient.authorizeDesktop(); // Note: this only works on desktop PCs
    // or direct the user to the webpage given jby oauthClient.authorizeUrl()
    // get the pin from the user since we're using "oob" instead of a callback servlet
    String v = oauthClient.askUser("Please enter the verification PIN from Twitter");
    oauthClient.setAuthorizationCode(v);
	// You can store the authorisation token details for future use
    Object accessToken = client.getAccessToken();
</pre></code>
 *
 * Now we can access Twitter: <code><pre>
	// Make a Twitter object
	Twitter twitter = new Twitter("my-name", oauthClient);
	// Print Winterstein's status
	System.out.println(twitter.getStatus("winterstein"));
	// Set my status
	twitter.updateStatus("Messing about in Java");
</pre></code>
 *
 * <p>
 * If you can handle callbacks, then the OAuth login can be streamlined. You
 * need a webserver and a servlet (eg. use Jetty or Tomcat) to handle callbacks.
 * Replace "oob" with your callback url. Direct the user to
 * client.authorizeUrl(). Twitter will then call your callback with the request
 * token and verifier (authorisation code).
 * </p>
 * <p>
 * See {@link http://www.winterwell.com/software/jtwitter.php} for more
 * information about this wrapper. See
 * {@link http://dev.twitter.com/doc} for more information about
 * the Twitter API.
 * <p>
 * Notes:
 * <ul>
 * <li>This wrapper takes care of all url-encoding/decoding.
 * <li>This wrapper will throw a runtime exception (TwitterException) if a
 * methods fails, e.g. it cannot connect to Twitter.com or you make a bad
 * request.
 * <li>Note that Twitter treats old-style retweets (those made by sending a
 * normal tweet beginning "RT @whoever") differently from new-style retweets
 * (those made using the retweet API). The differences are documented in various
 * methods.
 * <li>Most methods are available via this class (Twitter), except for list support (in
 * {@link TwitterList} - though {@link #getLists()} is here) and some
 * profile/account settings (in {@link Twitter_Account}).
 * <li>This class is NOT thread safe. If you're using multiple threads,
 * it is best to create separate Twitter objects (which is fine).
 * </ul>
 *
 * <h4>Copyright and License</h4>
 * This code is copyright (c) Winterwell Associates 2008/2009 and (c) winterwell
 * Mathematics Ltd, 2007 except where otherwise stated. It is released as
 * open-source under the LGPL license. See <a
 * href="http://www.gnu.org/licenses/lgpl.html"
 * >http://www.gnu.org/licenses/lgpl.html</a> for license details. This code
 * comes with no warranty or support.
 *
 * <h4>Change List</h4>
 * The change list is kept online at:
 * {@link http://www.winterwell.com/software/changelist.txt}
 *
 * @author Daniel Winterstein
 */
public class Twitter implements Serializable {
	private static final long serialVersionUID = 1L;	
	
	/**
	 * Geo-location API methods.
	 */
	public Twitter_Geo geo() {
		return new Twitter_Geo(this);
	}
	
	/**
	 * User and social-network related API methods.
	 */
	public Twitter_Users users() {
		return new Twitter_Users(this);
	}
	
	/**
	 * API methods relating to your account.
	 */
	public Twitter_Account account() {
		return new Twitter_Account(this);
	}
	
	public static enum KEntityType {
		urls, user_mentions, hashtags
	}

	/**
	 * The different types of API request. These can have different
	 * rate limits.
	 */
	public static enum KRequestType {
		NORMAL, SEARCH, SHOW_USER,
		/** this is X-Feature Class "namesearch" in the response headers*/
		SEARCH_USERS, 
		UPLOAD_MEDIA
	}

	/**
	 * A special slice of text within a tweet.

	 * @see Twitter#setIncludeTweetEntities(boolean)
	 */
	public final static class TweetEntity implements Serializable {
		private static final long serialVersionUID = 1L;
		/**
		 * start of the entity in the contents String, inclusive
		 */
		public final int start;
		/**
		 * end of the entity in the contents String, exclusive
		 */
		public final int end;
		private final ITweet tweet;
		private final String display;
		public final KEntityType type;

		/**
		 * @return For a url: the expanded version
		 * For a user-mention: the user's name
		 */
		public String displayVersion() {
			return display==null? toString() : display;
		}

		TweetEntity(ITweet tweet, KEntityType type, JSONObject obj) throws JSONException
		{
			JSONArray indices = obj.getJSONArray("indices");
			this.start = indices.getInt(0);
			this.end = indices.getInt(1);
			assert start>=0 && end >= start : obj;
			this.tweet = tweet;
			this.type = type;
			switch(type) {
			case urls:
				Object eu = obj.get("expanded_url");
				display = JSONObject.NULL.equals(eu)? null : (String) eu; break;
			case user_mentions:
				display = obj.getString("name"); break;
			default:
				display = null;
			}
		}

		/**
		 * The slice of text in the tweet.
		 * E.g. for a url, this will be the *shortened* version.
		 * @see #displayVersion()
		 */
		@Override
		public String toString() {
			return tweet.getText().substring(start, end);
		}

		static List<TweetEntity> parse(ITweet tweet, KEntityType type, JSONObject jsonEntities) throws JSONException
		{
			JSONArray arr = jsonEntities.optJSONArray(type.toString());
			ArrayList<TweetEntity> list = new ArrayList<TweetEntity>(arr.length());
			for(int i=0; i<arr.length(); i++) {
				JSONObject obj = arr.getJSONObject(i);
				TweetEntity te = new TweetEntity(tweet, type, obj);
				list.add(te);
			}
//			"user_mentions":[{"id":19720954,"name":"Lilly Hunter","indices":[0,10],"screen_name":"LillyLyle"}
			return list;
		}
	}


	/**
	 * Change this to access sites other than Twitter that support the Twitter
	 * API.
	 */
	String TWITTER_URL = "http://api.twitter.com/1";

	/**
	 * Search has to go through a separate url (Twitter's decision, June 2010).
	 */
	private static final String TWITTER_SEARCH_URL = "http://search.twitter.com";

	/**
	 * Set this to access sites other than Twitter that support the Twitter API.
	 * E.g. WordPress or Identi.ca. Note that not all methods may work! Also,
	 * search uses a separate url and is not affected by this method (it will
	 * continue to point to Twitter).
	 *
	 * @param url
	 *            Format: "http://domain-name", e.g. "http://twitter.com" by
	 *            default.
	 */
	public void setAPIRootUrl(String url) {
		assert url.startsWith("http://") || url.startsWith("https://");
		assert !url.endsWith("/") : "Please remove the trailing / from " + url;
		TWITTER_URL = url;
	}

	/**
	 * Use to register per-page callbacks for long-running searches. To stop the
	 * search, return true.
	 *
	 */
	public interface ICallback {
		public boolean process(List<Status> statuses);
	}

	/**
	 * Interface for an http client - e.g. allows for OAuth to be used instead.
	 * The standard version is {@link OAuthSignpostClient}.
	 * <p>
	 * If creating your own version, please provide support for throwing the
	 * right subclass of TwitterException - see
	 * {@link URLConnectionHttpClient#processError(java.net.HttpURLConnection)}
	 * for example code.
	 *
	 * @author Daniel Winterstein
	 */
	public static interface IHttpClient {

		/**
		 * Whether this client is setup to do authentication when contacting the
		 * Twitter server. Note: This is a fast method that does not call the
		 * server, so it does not check whether the access token or password is
		 * valid. See {Twitter#isValidLogin()} or
		 * {@link Twitter_Account#verifyCredentials()} if you need to check a
		 * login.
		 * */
		boolean canAuthenticate();

		/**
		 * Send an HTTP GET request and return the response body. Note that this
		 * will change all line breaks into system line breaks!
		 *
		 * @param uri
		 *            The uri to fetch
		 * @param vars
		 *            get arguments to add to the uri
		 * @param authenticate
		 *            If true, use authentication. The authentication method
		 *            used depends on the implementation (basic-auth, OAuth). It
		 *            is an error to use true if no authentication details have
		 *            been set.
		 *
		 * @throws TwitterException
		 *             for a variety of reasons
		 * @throws TwitterException.E404
		 *             for resource-does-not-exist errors
		 */
		String getPage(String uri, Map<String, String> vars,
				boolean authenticate) throws TwitterException;

		/**
		 * Send an HTTP POST request and return the response body.
		 *
		 * @param uri
		 *            The uri to post to.
		 * @param vars
		 *            The form variables to send. These are URL encoded before
		 *            sending.
		 * @param authenticate
		 *            If true, send user authentication
		 * @return The response from the server.
		 *
		 * @throws TwitterException
		 *             for a variety of reasons
		 * @throws TwitterException.E404
		 *             for resource-does-not-exist errors
		 */
		String post(String uri, Map<String, String> vars, boolean authenticate)
				throws TwitterException;

		/**
		 * Set the timeout for a single get/post request. This is an optional
		 * method - implementations can ignore it!
		 *
		 * @param millisecs
		 */
		void setTimeout(int millisecs);

		/**
		 * Fetch a header from the last http request.
		 * This is inherently NOT thread safe.
		 * Headers from error messages should (probably) be cached.
		 * @param headerName
		 * @return header value, or null if unset
		 */
		String getHeader(String headerName);

		/**
		 * Lower-level GET method.
		 * @param url
		 * @param vars
		 * @param authenticate
		 * @return
		 * @throws IOException
		 */
		HttpURLConnection connect(String url, Map<String, String> vars,
				boolean authenticate) throws IOException;
		
		/**
		 * Lower-level POST method.
		 * @param uri
		 * @param vars
		 * @return a freshly opened authorised connection
		 * @throws TwitterException
		 */
		HttpURLConnection post2_connect(String uri, Map<String, String> vars) 
			throws Exception;

		/**
		 * Update the rate limits for the given type of api call.
		 */
		void updateRateLimits(KRequestType rType);

		/**
		 * @see Twitter#getRateLimit(KRequestType)
		 * This is where the Twitter method is implemented.
		 */
		RateLimit getRateLimit(KRequestType reqType);

		/**
		 * @return a copy of this client. The copy can share structure, but it MUST be
		 * safe for passing to a new thread to be used in parallel with the original. 
		 */
		IHttpClient copy();

	}

	/**
	 * This gives common access to features that are common to both
	 * {@link Message}s and {@link Status}es.
	 *
	 * @author daniel
	 *
	 */
	public static interface ITweet extends Serializable {


		/**
		 * @return the location of this tweet. Can be null, never blank.
		 * This can come from geo-tagging or the user's location.
		 * This may be a place name, or in the form "latitude,longitude" if
		 * it came from a geo-tagged source.
		 * <p>
		 * Note: This will be set if Twitter supply any geo-information.
		 * We extract a location from geo and place objects.  
		 */
		String getLocation();

		/**
		 * @return more information on the location of this tweet. 
		 * This is usually null!
		 */
		Place getPlace();
		
		/**
		 * Twitter wrap urls with their own url-shortener (as a defence against malicious tweets).
		 * You are recommended to direct people to the Twitter-url, but use the
		 * original url for display.
		 * <p>
		 * Entity support is off by default. Request entity support by setting
		 * {@link Twitter#setIncludeTweetEntities(boolean)}.
		 * Twitter do NOT support entities for search :(
		 *
		 * @param type urls, user_mentions, or hashtags
		 * @return the text entities in this tweet, or null if the info was not supplied.
		 */
		List<TweetEntity> getTweetEntities(KEntityType type);
		
		Date getCreatedAt();

		/**
		 * Twitter IDs are numbers - but they can exceed the
		 * range of Java's signed long.
		 *
		 * @return The Twitter id for this post. This is used by some API
		 *         methods. This may be a Long or a BigInteger.
		 */
		Number getId();

		/** The actual status text. This is also returned by {@link #toString()} */
		String getText();

		/** The User who made the tweet */
		User getUser();

		/**
		 * @return list of screen-names this message is to. May be empty, never null.
		 * For Statuses, this is anyone mentioned in the message.
		 * For DMs, this is a wrapper round {@link Message#getRecipient()}.
		 * <p>
		 * Notes: This method is in ITweet as a convenience to allow the same code to process both
		 * Statuses and Messages where possible. It would be better named "getRecipients()",
		 * but for historical reasons it isn't.
		 */
		List<String> getMentions();

	}

	/**
	 * This rather dangerous global toggle switches off lower-casing
	 * on Twitter screen-names.
	 * <p>
	 * Screen-names are case insensitive as far as Twitter is concerned.
	 * However you might want to preserve the case people use
	 * for display purposes.
	 * <p>
	 * false by default.
	 */
	public static boolean CASE_SENSITIVE_SCREENNAMES;

	/**
	 * JTwitter version
	 */
	public final static String version = "2.3";

	// TODO
	// c.f. https://dev.twitter.com/discussions/1059
	Status updateStatusWithMedia(String statusText, Number inReplyToStatusId, File media) { 

		// should we trim statusText??
		// TODO support URL shortening
		if (statusText.length() > 160) {
			throw new IllegalArgumentException(
					"Status text must be 160 characters or less: "
							+ statusText.length() + " " + statusText);
		}
		Map<String, String> vars = InternalUtils.asMap("status", statusText);

		// add in long/lat if set
		if (myLatLong != null) {
			vars.put("lat", Double.toString(myLatLong[0]));
			vars.put("long", Double.toString(myLatLong[1]));
		}

		if (sourceApp != null)
			vars.put("source", sourceApp);
		if (inReplyToStatusId != null) {
			// TODO remove this legacy check
			double v = inReplyToStatusId.doubleValue();
			assert v!=0 && v!=-1;
			vars.put("in_reply_to_status_id", inReplyToStatusId.toString());
		}
//		media[]
//		possibly_sensitive
		// place_id
		// display_coordinates
		String result = null;
		try {
			result = http.post( // WithMedia
//					TWITTER_URL + 
					"http://upload.twitter.com/1/statuses/update_with_media.json", vars,
					true);
			http.updateRateLimits(KRequestType.UPLOAD_MEDIA);
			Status s = new Status(new JSONObject(result), null);
			return s;
		} catch (E403 e) {
			// test for repetition (which gets a 403)
			Status s = getStatus();
			if (s != null && s.getText().equals(statusText)) {
				throw new TwitterException.Repetition(s.getText());
			}
			throw e;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(result, e);
		}
	}

	/**
	 * Convenience method: Finds a user with the given screen-name from the
	 * list.
	 *
	 * @param screenName
	 *            aka login name
	 * @param users
	 * @return User with the given name, or null.
	 */
	public static User getUser(String screenName, List<User> users) {
		assert screenName != null && users != null;
		for (User user : users) {
			if (screenName.equals(user.screenName))
				return user;
		}
		return null;
	}

	/**
	 *
	 * @param args
	 *            Can be used as a command-line tweet tool. To do so, enter 3
	 *            arguments: name, password, tweet
	 *
	 *            If empty, prints version info.
	 */
	public static void main(String[] args) {
		// Post a tweet if we are handed a name, password and tweet
		if (args.length == 3) {
			Twitter tw = new Twitter(args[0], args[1]);
			// int s = 0;
			// List<Long> fids = tw.getFollowerIDs();
			// for (Long fid : fids) {
			// User f = tw.follow(""+fid);
			// if (f!=null) s++;
			// }
			Status s = tw.setStatus(args[2]);
			System.out.println(s);
			return;
		}
		System.out.println("Java interface for Twitter");
		System.out.println("--------------------------");
		System.out.println("Version " + version);
		System.out.println("Released under LGPL by Winterwell Associates Ltd.");
		System.out
				.println("See source code, JavaDoc, or http://winterwell.com for details on how to use.");
	}

	private String sourceApp = "jtwitterlib";

	/**
	 * Gets used once then reset to null by
	 * {@link #addStandardishParameters(Map)}. Gets updated in the while loops
	 * of methods doing a get-all-pages.
	 */
	Integer pageNumber;

	private Number sinceId;
	private Number untilId;

	private Date sinceDate;

	private Date untilDate;

	/**
	 * Provides support for fetching many pages
	 */
	private int maxResults = -1;

	final IHttpClient http;

	/**
	 * Twitter login name. Can be null even if we have authentication when using
	 * OAuth.
	 */
	private String name;

	/**
	 * Create a Twitter client without specifying a user. This is an easy way to
	 * access public posts. But you can't post of course.
	 */
	public Twitter() {
		this(null, new URLConnectionHttpClient());
	}
	
	/**
	 * Copy constructor. Use this to pass cloned Twitter objects for multi-threaded work.
	 * @param jtwit
	 */
	public Twitter(Twitter jtwit) {
		this(jtwit.getScreenName(), jtwit.http.copy());
	}

	/**
	 * Java wrapper for the Twitter API.
	 *
	 * @param name
	 *            the authenticating user's name, if known. Can be null.
	 * @param client
	 */
	public Twitter(String name, IHttpClient client) {
		this.name = name;
		http = client;
	}

	/**
	 * WARNING: Twitter no longer supports name/password basic authentication.
	 * This constructor is only for non-Twitter sites, such as identi.ca.
	 *
	 * @param screenName
	 *            The name of the user. Only used by some methods.
	 * @param password
	 *            The password of the user.
	 *
	 * @Deprecated Twitter have switched off basic authentication! Use an OAuth
	 *             client such as {@link OAuthSignpostClient} with
	 *             {@link #Twitter(String, IHttpClient)}
	 */
	@Deprecated
	public Twitter(String screenName, String password) {
		this(screenName, new URLConnectionHttpClient(screenName, password));
	}

	/**
	 * Add in since_id, page and count, if set. This is called by methods that
	 * return lists of statuses or messages.
	 *
	 * @param vars
	 * @return vars
	 */
	private Map<String, String> addStandardishParameters(
			Map<String, String> vars) {
		if (sinceId != null)
			vars.put("since_id", sinceId.toString());
		if (untilId != null) {
			vars.put("max_id", untilId.toString());
		}
		if (pageNumber != null) {
			vars.put("page", pageNumber.toString());
			// this is used once only
			pageNumber = null;
		}
		if (count != null) {
			vars.put("count", count.toString());
		}
		if (tweetEntities) {
			vars.put("include_entities", "1");
		}
		if (includeRTs) {
			vars.put("include_rts", "1");
		}
		return vars;
	}

	/**
	 * TODO merge with {@link #maxResults}??
	 */
	Integer count;

	private String lang;

	private BigInteger maxId;

	/**
	 * *Some* methods - the timeline ones for example - allow a count of
	 * number-of-tweets to return.
	 *
	 * @param count
	 *            null for default behaviour. 200 is the current maximum.
	 *            Twitter may reject or ignore high counts.
	 */
	public void setCount(Integer count) {
		this.count = count;
	}


	/**
	 * Equivalent to {@link #follow(String)}. C.f.
	 * http://apiwiki.twitter.com/Migrating-to-followers-terminology
	 *
	 * @param username
	 *            Required. The screen name of the user to befriend.
	 * @return The befriended user.
	 * @deprecated Use {@link #follow(String)} instead, which is equivalent.
	 */
	@Deprecated
	public User befriend(String username) throws TwitterException {
		return follow(username);
	}

	/**
	 * Equivalent to {@link #stopFollowing(String)}.
	 *
	 * @deprecated Please use {@link #stopFollowing(String)} instead.
	 */
	@Deprecated
	public User breakFriendship(String username) {
		return stopFollowing(username);
	}

	/**
	 * Filter keeping only those messages that come between sinceDate and
	 * untilDate (if either or both are set).
	 * The Twitter API used to offer this, but we now have to do it client side.
	 * @see #setSinceId(Number)
	 *
	 * @param list
	 * @return filtered list (a copy)
	 */
	private <T extends ITweet> List<T> dateFilter(List<T> list) {
		if (sinceDate == null && untilDate == null)
			return list;
		ArrayList<T> filtered = new ArrayList<T>(list.size());
		for (T message : list) {
			// assume OK if Twitter is being stingy on the info
			if (message.getCreatedAt() == null) {
				filtered.add(message);
				continue;
			}
			if (untilDate != null && untilDate.before(message.getCreatedAt())) continue;
			if (sinceDate != null && sinceDate.after(message.getCreatedAt())) continue;
			// ok
			filtered.add(message);
		}
		return filtered;
	}

	/**
	 * Deletes the status specified by the required ID parameter. The
	 * authenticating user must be the author of the specified status.
	 *
	 * @see #destroy(ITweet)
	 */
	public void destroyStatus(Number id) throws TwitterException {
		String page = post(TWITTER_URL + "/statuses/destroy/" + id + ".json",
				null, true);
		// Note: Sends two HTTP requests to Twitter rather than one: Twitter
		// appears
		// not to make deletions visible until the user's status page is
		// requested.
		flush();
		assert page != null;
	}

	/**
	 * Deletes the given status. Equivalent to {@link #destroyStatus(int)}. The
	 * authenticating user must be the author of the status post.
	 *
	 * @deprecated in favour of {@link #destroy(ITweet)}. This method will be
	 *             removed by the end of 2010.
	 * @see #destroy(ITweet)
	 */
	@Deprecated
	public void destroyStatus(Status status) throws TwitterException {
		destroyStatus(status.getId());
	}

	/**
	 * Deletes the given Status or Message. The authenticating user must be the
	 * author of the status post.
	 */
	public void destroy(ITweet tweet) throws TwitterException {
		if (tweet instanceof Status) {
			destroyStatus(tweet.getId());
		} else {
			destroyMessage((Message) tweet);
		}
	}

	/**
	 * Destroy a direct message.
	 *
	 * @param dm
	 */
	private void destroyMessage(Message dm) {
		String page = post(TWITTER_URL + "/direct_messages/destroy/" + dm.id
				+ ".json", null, true);
		assert page != null;
	}


	/**
	 * Deletes the direct message specified by the ID. The
	 * authenticating user must be the author of the specified status.
	 *
	 * @see #destroy(ITweet)
	 */
	public void destroyMessage(Number id) {
		String page = post(TWITTER_URL + "/direct_messages/destroy/" + id
				+ ".json", null, true);
		assert page != null;
	}

	void flush() {
		// This seems to prompt twitter to update in some cases!
		http.getPage("http://twitter.com/" + name, null, true);
	}

	/**
	 * @see Twitter_Users#follow(String)
	 */
	@Deprecated
	public User follow(String username) throws TwitterException {
		return users().follow(username);
	}

	/**
	 * @see Twitter_Users#follow(User) 
	 */
	@Deprecated
	public User follow(User user) {
		return follow(user.screenName);
	}
	
	
	/**
	 * Returns a list of the direct messages sent to the authenticating user.
	 * <p>
	 * Note: the Twitter API makes this available in rss if that's of interest.
	 */
	public List<Message> getDirectMessages() {
		return getMessages(TWITTER_URL + "/direct_messages.json",
				standardishParameters());
	}

	/**
	 * Returns a list of the direct messages sent *by* the authenticating user.
	 */
	public List<Message> getDirectMessagesSent() {
		return getMessages(TWITTER_URL + "/direct_messages/sent.json",
				standardishParameters());
	}

	/**
	 * The most recent 20 favourite tweets. (Note: This can use page - and page
	 * only - to fetch older favourites).
	 */
	public List<Status> getFavorites() {
		return getStatuses(TWITTER_URL + "/favorites.json",
				standardishParameters(), true);
	}

	public void setFavorite(Status status, boolean isFavorite) {
		try {
			String uri = isFavorite ? TWITTER_URL + "/favorites/create/"
					+ status.id + ".json" : TWITTER_URL + "/favorites/destroy/"
					+ status.id + ".json";
			http.post(uri, null, true);
		} catch (E403 e) {
			// already a favorite?
			if (e.getMessage() != null
					&& e.getMessage().contains("already favorited")) {
				throw new TwitterException.Repetition(
						"You have already favorited this status.");
			}
			// just a normal 403
			throw e;
		}
	}

	/**
	 * The most recent 20 favourite tweets for the given user. (Note: This can
	 * use page - and page only - to fetch older favourites).
	 *
	 * @param screenName
	 *            login-name.
	 */
	public List<Status> getFavorites(String screenName) {
		Map<String, String> vars = InternalUtils.asMap("screen_name", screenName);
		return getStatuses(TWITTER_URL + "/favorites.json",
				addStandardishParameters(vars), http.canAuthenticate());
	}

	/**
	 * @see Twitter_Users#getFollowerIDs()
	 */
	@Deprecated
	public List<Number> getFollowerIDs() throws TwitterException {
		return users().getFollowerIDs();
	}

	/**
	 * @see Twitter_Users#getFollowerIDs(String)
	 */
	@Deprecated
	public List<Number> getFollowerIDs(String screenName) throws TwitterException {
		return users().getFollowerIDs(screenName);
	}

	/**
	 * @see Twitter_Users#getFollowers()
	 */
	@Deprecated
	public List<User> getFollowers() throws TwitterException {
		return users().getFollowers();
	}

	/**
	 * @see Twitter_Users#getFollowers(String)
	 */
	@Deprecated
	public List<User> getFollowers(String username) throws TwitterException {
		return users().getFollowers(username);
	}

	/**
	 * @see Twitter_Users#getFriendIDs()
	 */
	@Deprecated
	public List<Number> getFriendIDs() throws TwitterException {
		return users().getFriendIDs();
	}

	/**
	 * @see Twitter_Users#getFriendIDs(String)
	 */
	@Deprecated
	public List<Number> getFriendIDs(String screenName) throws TwitterException {
		return users().getFriendIDs(screenName);
	}

	/**
	 * @see Twitter_Users#getFriends()
	 */
	@Deprecated
	public List<User> getFriends() throws TwitterException {
		return users().getFriends();
	}

	/**
	 * @see Twitter_Users#getFriendss(String)
	 */
	@Deprecated
	public List<User> getFriends(String username) throws TwitterException {
		return users().getFriends(username);
	}

	/**
	 * Returns the 20 most recent statuses posted in the last 24 hours from the
	 * authenticating user and that user's friends.
	 * @deprecated Replaced by {@link #getHomeTimeline()}
	 */
	@Deprecated
	public List<Status> getFriendsTimeline() throws TwitterException {
		return getHomeTimeline();
	}

	/**
	 * Returns the 20 most recent statuses posted in the last 24 hours from the
	 * authenticating user and that user's friends, including retweets.
	 */
	public List<Status> getHomeTimeline() throws TwitterException {
		assert http.canAuthenticate();
		return getStatuses(TWITTER_URL + "/statuses/home_timeline.json",
				standardishParameters(), true);
	}

	/**
	 *
	 * @param url
	 * @param var
	 * @param isPublic
	 *            Value to set for Message.isPublic
	 * @return
	 */
	private List<Message> getMessages(String url, Map<String, String> var) {
		// Default: 1 page
		if (maxResults < 1) {
			List<Message> msgs = Message.getMessages(http.getPage(url, var,
					true));
			msgs = dateFilter(msgs);
			return msgs;
		}
		// Fetch all pages until we run out
		// -- or Twitter complains in which case you'll get an exception
		pageNumber = 1;
		List<Message> msgs = new ArrayList<Message>();
		while (msgs.size() <= maxResults) {
			String p = http.getPage(url, var, true);
			List<Message> nextpage = Message.getMessages(p);
			nextpage = dateFilter(nextpage);
			msgs.addAll(nextpage);
			if (nextpage.size() < 20)
				break;
			pageNumber++;
			var.put("page", Integer.toString(pageNumber));
		}
		return msgs;
	}

	/**
	 * @return Login name of the authenticating user, or null if not set.
	 * <p>
	 * Will call Twitter to find out if null but oauth is set.
	 * @see #getSelf()
	 */
	public String getScreenName() {
		// load if need be
		getSelf();		
		return name;
	}

	/**
	 * Returns the 20 most recent statuses from non-protected users who have set
	 * a custom user icon. Does not require authentication.
	 * <p>
	 * Note: Twitter cache-and-refresh this every 60 seconds, so there is little
	 * point calling it more frequently than that.
	 */
	public List<Status> getPublicTimeline() throws TwitterException {
		return getStatuses(TWITTER_URL + "/statuses/public_timeline.json",
				standardishParameters(), false);
	}

	/**
	 * How many normal rate limit calls do you have left?
	 * This calls Twitter, which makes it slower than
	 * {@link #getRateLimit(KRequestType)} but it's up-to-date
	 * and safe against threads and other-programs using the same
	 * allowance.
	 * <p>
	 * This may update getRateLimit(KRequestType) for NORMAL requests,
	 * but sadly it doesn't fetch rate-limit info on other request types.
	 *
	 * @return the remaining number of API requests available to the
	 *         authenticating user before the API limit is reached for the
	 *         current hour. <i>If this is zero or negative you should stop using
	 *         Twitter with this login for a bit.</i> Note: Calls to
	 *         rate_limit_status do not count against the rate limit.
	 * @see #getRateLimit(KRequestType)
	 */
	public int getRateLimitStatus() {
		String json = http.getPage(TWITTER_URL
				+ "/account/rate_limit_status.json", null, http
				.canAuthenticate());
		try {
			JSONObject obj = new JSONObject(json);
			int hits = obj.getInt("remaining_hits");
			// Update the RateLimit objects
//			http.updateRateLimits(KRequestType.NORMAL); no header info sent!
			if (http instanceof URLConnectionHttpClient) {
				URLConnectionHttpClient _http = (URLConnectionHttpClient) http;
				RateLimit rateLimit = new RateLimit(
						obj.getString("hourly_limit"), Integer.toString(hits),
						obj.getString("reset_time"));
				_http.rateLimits.put(KRequestType.NORMAL, rateLimit);
			}
			return hits;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	/**
	 * Returns the 20 most recent replies/mentions (status updates with
	 * @username) to the authenticating user. Replies are only available to the
	 *            authenticating user; you can not request a list of replies to
	 *            another user whether public or protected.
	 *            <p>
	 *            This is exactly the same as {@link #getMentions()}!
	 *            Twitter changed their API & terminology - we are (currently)
	 *            keeping both methods.
	 *            <p>
	 *            When paging, this method can only go back up to 800 statuses.
	 *            <p>
	 *            Does not include new-style retweets.
	 * @deprecated Use #getMentions() for preference
	 */
	public List<Status> getReplies() throws TwitterException {
		return getMentions();
	}
	
	/**
	 * Returns the 20 most recent replies/mentions (status updates with
	 * @username) to the authenticating user. Replies are only available to the
	 *            authenticating user; you can not request a list of replies to
	 *            another user whether public or protected.
	 *            <p>
	 *            This is exactly the same as {@link #getReplies()}
	 *            <p>
	 *            When paging, this method can only go back up to 800 statuses.
	 *            <p>
	 *            Does not include new-style retweets.
	 */
	public List<Status> getMentions() {
		return getStatuses(TWITTER_URL + "/statuses/mentions.json",
				standardishParameters(), true);
	}

	/**
	 * @return those of your tweets that have been retweeted. It's a bit of a
	 *         strange one this. You can then query who retweeted you.
	 */
	public List<Status> getRetweetsOfMe() {
		String url = TWITTER_URL + "/statuses/retweets_of_me.json";
		Map<String, String> vars = addStandardishParameters(new HashMap<String, String>());
		String json = http.getPage(url, vars, true);
		return Status.getStatuses(json);
	}

	/**
	 * @return your lists, ie. the one's you made.
	 */
	public List<TwitterList> getLists() {
		return getLists(name);
	}

	/**
	 * Convenience for {@link #getListsContaining(String, boolean)}.
	 * @return lists that you are a member of.
	 * Warning: currently limited to a maximum of 20 results.
	 */
	public List<TwitterList> getListsContainingMe() {
		return getListsContaining(name, false);
	}

	/**
	 * @param screenName
	 * @param filterToOwned If true, only return lists which the user owns.
	 * @return lists of which screenName is a member.
	 * NOTE: currently limited to a maximum of 20 lists!
	 */
	public List<TwitterList> getListsContaining(String screenName, boolean filterToOwned) {
		assert screenName != null;
		try {
			String url = TWITTER_URL + "/lists/memberships.json";
			Map<String, String> vars = InternalUtils.asMap("screen_name", screenName);
			if (filterToOwned) {
				assert http.canAuthenticate();
				vars.put("filter_to_owned_lists", "1");
			}
			String listsJson = http.getPage(url, vars, http.canAuthenticate());
			JSONObject wrapper = new JSONObject(listsJson);
			JSONArray jarr = (JSONArray) wrapper.get("lists");
			List<TwitterList> lists = new ArrayList<TwitterList>();
			for (int i = 0; i < jarr.length(); i++) {
				JSONObject li = jarr.getJSONObject(i);
				TwitterList twList = new TwitterList(li, this);
				lists.add(twList);
			}
			return lists;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(null, e);
		}
	}

	/**
	 * @param screenName
	 * @return the (first 20) lists created by the given user
	 */
	public List<TwitterList> getLists(String screenName) {
		assert screenName != null;
		try {
			String url = TWITTER_URL + "/" + screenName + "/lists.json";
			String listsJson = http.getPage(url, null, true);
			JSONObject wrapper = new JSONObject(listsJson);
			JSONArray jarr = (JSONArray) wrapper.get("lists");
			List<TwitterList> lists = new ArrayList<TwitterList>();
			for (int i = 0; i < jarr.length(); i++) {
				JSONObject li = jarr.getJSONObject(i);
				TwitterList twList = new TwitterList(li, this);
				lists.add(twList);
			}
			return lists;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(null, e);
		}
	}

	/**
	 * @return Retweets of this tweet. This attempts to cover new-style and old-style "manual" retweets.
	 * It does so by making retweet call and a search call. It will miss edited retweets though.
	 */
	public List<Status> getRetweets(Status tweet) {
		String url = TWITTER_URL + "/statuses/retweets/" + tweet.id + ".json";
		Map<String, String> vars = addStandardishParameters(new HashMap<String, String>());
		String json = http.getPage(url, vars, true);
		List<Status> newStyle = Status.getStatuses(json);
		try {
			// // Should we also do by search and merge the two lists?
			 StringBuilder sq = new StringBuilder();
			 sq.append("\"RT @"+tweet.getUser().getScreenName()+": ");
			 if (sq.length()+tweet.text.length()+1 > 140) {
				 int i = tweet.text.lastIndexOf(' ', 140-sq.length()-1);
				 String words = tweet.text.substring(0, i);
				 sq.append(words);
			 } else {
				 sq.append(tweet.text);
			 }
			 sq.append('"');
			 List<Status> oldStyle = search(sq.toString());
			 // merge them
			 newStyle.addAll(oldStyle);
			 Collections.sort(newStyle, InternalUtils.NEWEST_FIRST);
			 return newStyle;
		} catch (TwitterException e) {
			// oh well
			return newStyle;
		}
	}

	/**
	 * Show users who (new-style) retweeted the given tweet. Can use count (up
	 * to 100) and page. This does not include old-style retweeters!
	 *
	 * @param tweet
	 *            You can use a "fake" Status created via
	 *            {@link Status#Status(User, String, long, Date)} if you know
	 *            the id number.
	 */
	public List<User> getRetweeters(Status tweet) {
		String url = TWITTER_URL + "/statuses/" + tweet.id
				+ "/retweeted_by.json";
		Map<String, String> vars = addStandardishParameters(new HashMap<String, String>());
		String json = http.getPage(url, vars, true);
		List<User> users = User.getUsers(json);
		return users;
	}

	/**
	 * @return The current status of the user. Warning: this is null if (a)
	 *         unset (ie if this user has never tweeted), or (b) their last six
	 *         tweets were all new-style retweets!
	 */
	public Status getStatus() throws TwitterException {
		Map<String, String> vars = InternalUtils.asMap("count", 6);
		String json = http.getPage(
				TWITTER_URL + "/statuses/user_timeline.json", vars, true);
		List<Status> statuses = Status.getStatuses(json);
		if (statuses.size() == 0)
			return null;
		return statuses.get(0);
	}

	/**
	 * Returns a single status, specified by the id parameter below. The
	 * status's author will be returned inline.
	 *
	 * @param id
	 *            The numerical ID of the status you're trying to retrieve.
	 */
	public Status getStatus(Number id) throws TwitterException {
		Map vars = tweetEntities? InternalUtils.asMap("include_entities", "1") : null;
		String json = http.getPage(TWITTER_URL + "/statuses/show/" + id
				+ ".json", vars, http.canAuthenticate());
		try {
			return new Status(new JSONObject(json), null);
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	/**
	 * @return The current status of the given user.
	 *         <p>
	 *         Warning: this can be null if the user has been doing enough
	 *         new-style retweets. This is due to flaws in the Twitter API.
	 */
	public Status getStatus(String username) throws TwitterException {
		assert username != null;
		// new-style retweets can cause blanks in your timeline
		// show(username).status is just as vulnerable
		// grab a few tweets to give some robustness
		Map<String, String> vars = InternalUtils.asMap("id", username, "count", 6);
		String json = http.getPage(
				TWITTER_URL + "/statuses/user_timeline.json", vars, false);
		List<Status> statuses = Status.getStatuses(json);
		if (statuses.size() == 0)
			return null;
		return statuses.get(0);
	}

	/**
	 * Does the grunt work for paged status fetching
	 * @param url
	 * @param var
	 * @param authenticate
	 * @return
	 */
	private List<Status> getStatuses(final String url, Map<String, String> var,
			boolean authenticate) {
		// Default: 1 page
		if (maxResults < 1) {
			List<Status> msgs = Status.getStatuses(http.getPage(url, var,
					authenticate));
			msgs = dateFilter(msgs);
			return msgs;
		}
		// Fetch all pages until we reach the desired maxResults, or run out
		// -- or Twitter complains in which case you'll get an exception
		// Use status ids for paging, rather than page number, because this
		// allows for "drift" when new tweets are posted during the paging.
		maxId = null;
//		pageNumber = 1;
		List<Status> msgs = new ArrayList<Status>();
		while (msgs.size() <= maxResults) {
			String json = http.getPage(url, var, authenticate);
			List<Status> nextpage = Status.getStatuses(json);
			// This test replaces size<20. It requires an extra call to Twitter.
			// But it fixes a bug whereby retweets aren't counted and can thus cause
			// the system to quit early.
			if (nextpage.isEmpty()) break;
			// Next page must start strictly before this one
			maxId = nextpage.get(nextpage.size()-1).id.subtract(BigInteger.ONE);
			//System.out.println(maxId + " -> " + nextpage.get(0).id);

			msgs.addAll(dateFilter(nextpage));
//			pageNumber++;
			var.put("max_id", maxId.toString());
		}
		// rate limit update
		http.updateRateLimits(KRequestType.NORMAL);
		return msgs;
	}



	/**
	 * Have we got enough results for the current search?
	 *
	 * @param list
	 * @return false if maxResults is set to -1 (ie, unlimited) or if list
	 *         contains less than maxResults results.
	 */
	boolean enoughResults(List list) {
		return (maxResults != -1 && list.size() >= maxResults);
	}


	/**
	 * Returns the most recent statuses from the
	 * authenticating user. 20 by default.
	 */
	public List<Status> getUserTimeline() throws TwitterException {
		return getStatuses(TWITTER_URL + "/statuses/user_timeline.json",
				standardishParameters(), true);
	}

	/**
	 * Returns the most recent statuses from the given user.
	 * <p>
	 * This will return 20 results by default, though {@link #setMaxResults(int)}
	 * can be used to fetch multiple pages.
	 * 
	 * Note that if you exclude new-style retweets (via {@link #setIncludeRTs(boolean)})
	 * then this can return less than 20 results -- it can even return none if the latest
	 * 20 are all retweets.
	 * <p>
	 * There is a cap of 3200 tweets
	 *  - this is the farthest back you can go down a user timeline!
	 * <p>
	 * This method will authenticate if it can (i.e. if the Twitter object has a
	 * username and password). Authentication is needed to see the posts of a
	 * private user.
	 *
	 * @param screenName
	 *            Can be null. Specifies the screen name of the user for whom to
	 *            return the user_timeline.
	 * @throws TwitterException.E401 if the user has protected their tweets,
	 * and you do not have access.
	 * @throws TwitterException.SuspendedUser if the user has been suspended
	 */
	public List<Status> getUserTimeline(String screenName)
			throws TwitterException {
		Map<String, String> vars = InternalUtils.asMap("screen_name", screenName);
		addStandardishParameters(vars);
		// Should we authenticate?
		boolean authenticate = http.canAuthenticate();
		try {
			return getStatuses(TWITTER_URL + "/statuses/user_timeline.json", vars,
					authenticate);
		} catch (E401 e) {
			// Bug in Twitter: this can be a suspended user
			//  - in which case this will generate a SuspendedUser exception
			isSuspended(screenName);
			throw e;
		}
	}
	
	/**
	 * Equivalent to {@link #getUserTimeline(String)}, but takes a numeric
	 * user-id instead of a screen-name.
	 * @param userId
	 * @return tweets by userId
	 */
	public List<Status> getUserTimeline(Long userId)
		throws TwitterException {
		Map<String, String> vars = InternalUtils.asMap("user_id", userId);
		addStandardishParameters(vars);
		// Authenticate if we can (for protected streams)
		boolean authenticate = http.canAuthenticate();
		try {
			return getStatuses(TWITTER_URL + "/statuses/user_timeline.json", vars,
					authenticate);
		} catch (E401 e) {
			// Bug in Twitter: this can be a suspended user
			//  - in which case this will generate a SuspendedUser exception
//			isSuspended(userId); // TODO?
			throw e;
		}
	}

	boolean includeRTs = true;
	
	/**
	 * true by default. If true, lists of tweets will include new-style retweets.
	 * If false, they won't (execpt for the retweet-specific calls).
	 * @param includeRTs
	 */
	public void setIncludeRTs(boolean includeRTs) {
		this.includeRTs = includeRTs;
	}
	
	/**
	 * @deprecated Use {@link #setIncludeRTs(boolean)} instead to control retweet behaviour.
	 * 
	 * Returns the most recent statuses posted
	 * by the given user. Unlike {@link #getUserTimeline(String)}, this includes
	 * new-style retweets.
	 * <p>
	 * This will return 20 by default, though {@link #setMaxResults(int)}
	 * can be used to fetch multiple pages. There is a cap of 3200 tweets
	 *  - this is the farthest back you can go down a user timeline!
	 * <p>
	 * This method will authenticate if it can (i.e. if the Twitter object has a
	 * username and password). Authentication is needed to see the posts of a
	 * private user.
	 *
		@param screenName
	 *            Can be null. Specifies the screen name of the user for whom to
	 *            return the user_timeline.
	 *            
	 */
	public List<Status> getUserTimelineWithRetweets(String screenName)
			throws TwitterException
	{
		Map<String, String> vars = InternalUtils.asMap("screen_name", screenName,
				"include_rts", "1");
		addStandardishParameters(vars);
		// Should we authenticate?
		boolean authenticate = http.canAuthenticate();
		try {
			return getStatuses(TWITTER_URL + "/statuses/user_timeline.json", vars,
					authenticate);
		} catch (E401 e) {
			isSuspended(screenName);
			throw e;
		}
	}

	/**
	 * Generate an exception if the use is suspended.
	 * This is used as a work-around for misleading error codes returned by Twitter.
	 * @param screenName
	 * @throws SuspendedUser
	 */
	private void isSuspended(String screenName) throws SuspendedUser {
		show(screenName);
	}

	/**
	 * Note: does NOT work for search() methods (not supported by Twitter).
	 * @param tweetEntities Set to true to enable {@link Status#getTweetEntities(KEntityType)}, false
	 * if you don't care.
	 * Default is true.
	 */
	public void setIncludeTweetEntities(boolean tweetEntities) {
		this.tweetEntities = tweetEntities;
	}

	boolean tweetEntities = true;

	private String resultType;

	/**
	 * @see Twitter_Users#isFollower(String)
	 */
	@Deprecated
	public boolean isFollower(String userB) {
		return isFollower(userB, name);
	}

	/**
	 * @see Twitter_Users#isFollower(String, String)
	 */
	@Deprecated
	public boolean isFollower(String followerScreenName,
			String followedScreenName) {
		return users().isFollower(followerScreenName, followedScreenName);
	}

	/**
	 * @see Twitter_Users#isFollowing(String)
	 */
	@Deprecated
	public boolean isFollowing(String userB) {
		return isFollower(name, userB);
	}

	/**
	 * @see Twitter_Users#isFollowing(User)
	 */
	@Deprecated
	public boolean isFollowing(User user) {
		return isFollowing(user.screenName);
	}

	/**
	 * Are the login details used for authentication valid?
	 *
	 * @return true if OK, false if unset or invalid
	 * @see Twitter_Account#verifyCredentials() which returns user info
	 */
	public boolean isValidLogin() {
		if (!http.canAuthenticate())
			return false;
		try {
			Twitter_Account ta = new Twitter_Account(this);
			User u = ta.verifyCredentials();
			return true;
		} catch (TwitterException.E403 e) {
			return false;
		} catch (TwitterException.E401 e) {
			return false;
		} catch (TwitterException e) {
			throw e;
		}
	}

	


	/**
	 * Wrapper for {@link IHttpClient#post(String, Map, boolean)}.
	 */
	private String post(String uri, Map<String, String> vars,
			boolean authenticate) throws TwitterException {
		String page = http.post(uri, vars, authenticate);
		return page;
	}
	
	/**
	 * Optional. Specifies what type of search results you would prefer 
	 * to receive. The current default is "mixed." Valid values:<br>
{@link #SEARCH_MIXED}: Include both popular and real time results in the response.<br>
{@link #SEARCH_RECENT}: return only the most recent results in the response<br>
{@link #SEARCH_POPULAR}: return only the most popular results in the response.<br>
	 * @param resultType
	 */
	public void setSearchResultType(String resultType) {
		this.resultType = resultType;
	}
	
	public static final String SEARCH_MIXED = "mixed";
	public static final String SEARCH_RECENT = "recent";
	public static final String SEARCH_POPULAR = "popular";

	/**
	 * Perform a search of Twitter.
	 * <p>
	 * Warning: the User objects returned by a search (as part of the Status
	 * objects) are dummy-users. The only information that is set is the user's
	 * screen-name and a profile image url. This reflects the current behaviour
	 * of the Twitter API. If you need more info, call {@link #show(String)}
	 * with the screen name.
	 * <p>
	 * This supports {@link #maxResults} and pagination. A language filter can
	 * be set via {@link #setLanguage(String)} Location can be set via
	 * {@link #setSearchLocation(double, double, String)}
	 *
	 * Other advanced search features can be done via the query string. E.g.<br>
	 * "from:winterstein" - tweets from user winterstein<br>
	 * "to:winterstein" - tweets start with @winterstein<br>
	 * "source:jtwitter" - originating from the application JTwitter - your
	 * query must also must contain at least one keyword parameter. <br>
	 * "filter:links" - tweets contain a link<br>
	 * "apples OR pears" - or ("apples pears" would give you apples <i>and</i>
	 * pears).
	 *
	 * @param searchTerm
	 *            This can include several space-separated keywords, #tags and @username
	 *            (for mentions), and use quotes for \"exact phrase\" searches.
	 * @param callback
	 *            an object whose process() method will be called on each new
	 *            page of results.
	 * @param rpp
	 *            results per page. 100 is the default 
	 * @return search results - up to maxResults if maxResults is
	 *         positive, or rpp if maxResults is negative/zero.
	 */
	public List<Status> search(String searchTerm, ICallback callback, int rpp) {
		searchTerm = search2_bugHack(searchTerm);
		Map<String, String> vars;
		if (maxResults < 100 && maxResults > 0) {
			// Default: 1 page
			vars = getSearchParams(searchTerm, maxResults);
		} else {
			vars = getSearchParams(searchTerm, rpp);
		}
		// Fetch all pages until we run out
		// -- or Twitter complains in which case you'll get an exception
		List<Status> allResults = new ArrayList<Status>(Math.max(maxResults,
				rpp));
		String url = TWITTER_SEARCH_URL + "/search.json";
		int localPageNumber = 1; // pageNumber is nulled by getSearchParams
		do {
			pageNumber = localPageNumber;
			vars.put("page", Integer.toString(pageNumber));
			String json = http.getPage(url, vars, false);
			http.updateRateLimits(KRequestType.SEARCH);
			List<Status> stati = Status.getStatusesFromSearch(this, json);
			int numResults = stati.size();
			stati = dateFilter(stati);
			allResults.addAll(stati);
			if (callback != null) {
				// the callback may tell us to stop, by returning true
				if (callback.process(stati))
					break;
			}
			if (numResults < rpp) { // We've reached the end of the results
				break;
			}
			// paranoia
			localPageNumber++;
		} while (allResults.size() < maxResults);
		// null for the next method
		pageNumber = null;
		return allResults;
	}


	/**
	 * This fixes a couple of bugs in Twitter's search API:
	 * 
	 * 1. Searches using OR and a location return gibberish,
	 * unless they also include a -term. Strangely that seems to fix things. 
	 * So we just add one if needed.<br>
	 * 
	 * 2. Searches that start and end with quotes, and use an OR have problems:
	 *  they become AND searches with the OR turned into a keyword. E.g.
	 *  /"apple" OR "pear"/ acts like /"apple" AND or AND "pear"/ 
	 * <p>
	 * It should be tested periodically whether we need this.
	 * See {@link TwitterTest#testSearchBug()}, 
	 * {@link TwitterTest#testSearchBug2()}
	 * 
	 * @param searchTerm
	 * @return e.g. "apples OR pears" (near Edinburgh) goes to "apples OR pears -kfz" (near Edinburgh) 
	 */
	private String search2_bugHack(String searchTerm) {
		// zero-length is valid with location
		if (searchTerm.isEmpty()) return searchTerm;
		// bug 1: a OR b near X fails
		if (searchTerm.contains(" OR ") && ! searchTerm.contains("-")
				&& geocode != null) {
			return searchTerm+" -kfz"; // add a -gibberish term
		}
		// bug 2: "a" OR "b" fails
		if (searchTerm.contains(" OR ") && searchTerm.charAt(0) == '"'
			&& searchTerm.charAt(searchTerm.length()-1) == '"') {
			return searchTerm+" -kfz"; // add a -gibberish term
		}
		// hopefully fine as-is
		return searchTerm;
	}

	/**
	 * What is the current rate limit status? Do we need to throttle back our
	 * usage? This is the cached info from the last call of that type.
	 * <p>
	 * Note: The RateLimit object is created using cached info from a previous
	 * Twitter call. So this method is quick (it doesn't require a fresh call
	 * to Twitter), but the RateLimit object isn't available until after you
	 * make a call of the right type to Twitter.
	 * <p>
	 * Status: Headin towards stable, but still a bit experimental.
	 * @param reqType Different methods have separate rate limits.
	 * @return the last rate limit advice received, or null if unknown.
	 * @see #getRateLimitStatus()
	 */
	public RateLimit getRateLimit(KRequestType reqType) {
		return http.getRateLimit(reqType);
	}

	/**
	 * Report a user for being a spammer.
	 * @param screenName
	 */
	public void reportSpam(String screenName) {
		http.getPage(TWITTER_URL+"/version/report_spam.json",
					InternalUtils.asMap("screen_name", screenName), true);
	}

	/**
	 * Retweet (new-style) a tweet without any edits. You can also retweet by
	 * starting a status using the RT @username microformat. (this is an
	 * old-style retweet).
	 *
	 * @param tweet
	 *            Note: you cannot retweet your own tweets.
	 * @return your retweet
	 */
	public Status retweet(Status tweet) {
		try {
			String result = post(TWITTER_URL + "/statuses/retweet/"
					+ tweet.getId() + ".json", null, true);
			return new Status(new JSONObject(result), null);

			// error handling
		} catch (E403 e) {
			List<Status> rts = getRetweetsByMe();
			for (Status rt : rts) {
				if (tweet.equals(rt.getOriginal())) {
					throw new TwitterException.Repetition(rt.getText());
				}
			}
			throw e;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(null, e);
		}
	}

	/**
	 * Perform a search of Twitter. Convenience wrapper for
	 * {@link #search(String, ICallback, int)} with no callback 
	 * and fetching one pages worth of results.
	 */
	public List<Status> search(String searchTerm) {
		return search(searchTerm, null, 100);
	}

	/**
	@see Twitter_Users#searchUsers(String)
	 */
	@Deprecated
	public List<User> searchUsers(String searchTerm) {
		return users().searchUsers(searchTerm);
	}

	/**
	 * @param searchTerm
	 * @param rpp
	 * @return
	 */
	private Map<String, String> getSearchParams(String searchTerm, int rpp) {
		Map<String, String> vars = InternalUtils.asMap("rpp", Integer.toString(rpp), "q", searchTerm);
		if (sinceId != null)
			vars.put("since_id", sinceId.toString());
		if (untilId != null) {
			// It's unclear from the docs whether this will work
			// c.f. https://dev.twitter.com/docs/api/1/get/search
			vars.put("max_id", untilId.toString());
		}
		// since date is no longer supported. until is though?!
		// if (sinceDate != null) vars.put("since", df.format(sinceDate));
		if (untilDate != null)
			vars.put("until", InternalUtils.df.format(untilDate));
		if (lang != null)
			vars.put("lang", lang);
		if (geocode != null)
			vars.put("geocode", geocode);
		if (resultType!=null) {
			vars.put("result_type", resultType);
		}
		addStandardishParameters(vars);
		return vars;
	}

	/**
	 * Used by search
	 */
	private String geocode;

	private double[] myLatLong;

	private String twitlongerAppName;

	private String twitlongerApiKey;

	public static long PHOTO_SIZE_LIMIT;

	/**
	 * Set this to allow the use of twitlonger via
	 * {@link #updateLongStatus(String, long)}. To get an api-key for your app,
	 * contact twitlonger as described here: http://www.twitlonger.com/api
	 *
	 * @param twitlongerAppName
	 * @param twitlongerApiKey
	 */
	public void setupTwitlonger(String twitlongerAppName,
			String twitlongerApiKey) {
		this.twitlongerAppName = twitlongerAppName;
		this.twitlongerApiKey = twitlongerApiKey;
	}

	/**
	 * Restricts {@link #search(String)} to tweets by users located within a
	 * given radius of the given latitude/longitude.
	 * <p>
	 * The location of a tweet is preferably taken from the Geotagging API,
	 * but will fall back to the Twitter profile.
	 *
	 * @param latitude
	 * @param longitude
	 * @param radius
	 *            E.g. 3.5mi or 2km. Must be <2500km
	 */
	public void setSearchLocation(double latitude, double longitude,
			String radius) {
		assert radius.endsWith("mi") || radius.endsWith("km") : radius;
		geocode = latitude + "," + longitude + "," + radius;
	}

	/**
	 * Set the location for your tweets.<br>
	 *
	 * Warning: geo-tagging parameters are ignored if geo_enabled for the user
	 * is false (this is the default setting for all users unless the user has
	 * enabled geolocation in their settings)!
	 *
	 * @param latitudeLongitude
	 *            Can be null (which is the default), in which case your tweets
	 *            will not carry location data.
	 *            <p>
	 *            The valid ranges for latitude is -90.0 to +90.0 (North is
	 *            positive) inclusive. The valid ranges for longitude is -180.0
	 *            to +180.0 (East is positive) inclusive.
	 *
	 * @see #setSearchLocation(double, double, String) which is completely
	 *      separate.
	 */
	public void setMyLocation(double[] latitudeLongitude) {
		myLatLong = latitudeLongitude;
		if (myLatLong == null)
			return;
		if (Math.abs(myLatLong[0]) > 90)
			throw new IllegalArgumentException(myLatLong[0]
					+ " is not within +/- 90");
		if (Math.abs(myLatLong[1]) > 180)
			throw new IllegalArgumentException(myLatLong[1]
					+ " is not within +/- 180");
	}

	/**
	 * Sends a new direct message (DM) to the specified user from the authenticating
	 * user. This is a private message!
	 *
	 * @param recipient
	 *            Required. The screen name of the recipient user.
	 * @param text
	 *            Required. The text of your direct message. Keep it under 140
	 *            characters! This should *not* include the "d username" portion
	 * @return the sent message
	 * @throws TwitterException.E403
	 *             if the recipient is not following you. (you can \@mention
	 *             anyone but you can only dm people who follow you).
	 */
	public Message sendMessage(String recipient, String text)
			throws TwitterException {
		assert recipient != null && text != null : recipient+" "+text;
		assert ! text.startsWith("d " + recipient) : recipient+" "+text;
		if (text.length() > 140)
			throw new IllegalArgumentException("Message is too long.");
		Map<String, String> vars = InternalUtils.asMap("user", recipient, "text", text);
		if (tweetEntities) vars.put("include_entities", "1");
		String result=null;
		try {
			// post it
			result = post(TWITTER_URL + "/direct_messages/new.json", vars, true);
			// sadly the response doesn't include rate-limit info
			return new Message(new JSONObject(result));
		} catch (JSONException e) {
			throw new TwitterException.Parsing(result, e);
		} catch (TwitterException.E404 e) {
			// suspended user?? TODO investigate
			throw new TwitterException.E404(e.getMessage()+" with recipient="+recipient+", text="+text);
		}
	}

	/**
	 * @param maxResults
	 *            if greater than zero, requests will attempt to fetch as many
	 *            pages as are needed! -1 by default, in which case most methods
	 *            return the first 20 statuses/messages. Zero is not allowed.
	 *            <p>
	 *            If setting a high figure, you should usually also set a
	 *            sinceId or sinceDate to limit your Twitter usage. Otherwise
	 *            you can easily exceed your rate limit.
	 */
	public void setMaxResults(int maxResults) {
		assert maxResults != 0;
		this.maxResults = maxResults;
	}

	/**
	 * @param pageNumber
	 *            null (the default) returns the first page. Pages are indexed
	 *            from 1. This is used once only! Then it is reset to null
	 */
	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	/**
	 * Date based filter on statuses and messages. This is done client-side as
	 * Twitter have - for their own inscrutable reasons - pulled support for
	 * this feature. Use {@link #setSinceId(Number)} for preference.
	 * <p>
	 * If using this, you probably also want to increase
	 * {@link #setMaxResults(int)} - otherwise you get at most 20, and possibly
	 * less (since the filtering is done client side).
	 *
	 * @param sinceDate
	 */
	public void setSinceDate(Date sinceDate) {
		this.sinceDate = sinceDate;
	}

	/**
	 * @param untilDate
	 *            the untilDate to set
	 */
	public void setUntilDate(Date untilDate) {
		this.untilDate = untilDate;
	}

	/**
	 * @return the untilDate
	 */
	public Date getUntilDate() {
		return untilDate;
	}

	/**
	 * Narrows the returned results to just those statuses created after the
	 * specified status id. This will be used until it is set to null. Default
	 * is null.
	 * <p>
	 * If using this, you probably also want to increase
	 * {@link #setMaxResults(int)} (otherwise you just get the most recent 20).
	 *
	 * @param statusId
	 */
	public void setSinceId(Number statusId) {
		sinceId = statusId;
	}
	
	/**
	 * If set, return results older than this.
	 * @param untilId aka max_id 
	 */
	public void setUntilId(Number untilId) {
		this.untilId = untilId;
	}

	/**
	 * Set the source application. This will be mentioned on Twitter alongside
	 * status updates (with a small label saying source: myapp).
	 *
	 * <i>In order for this to work, you must first register your app with
	 * Twitter and get a source name from them! You must also use OAuth to
	 * connect.</i>
	 *
	 * @param sourceApp
	 *            jtwitterlib by default. Set to null for no source.
	 */
	public void setSource(String sourceApp) {
		this.sourceApp = sourceApp;
	}

	/**
	 * Sets the authenticating user's status.
	 * <p>
	 * Identical to {@link #updateStatus(String)}, but with a Java-style name
	 * (updateStatus is the Twitter API name for this method).
	 *
	 * @param statusText
	 *            The text of your status update. Must not be more than 160
	 *            characters and should not be more than 140 characters to
	 *            ensure optimal display.
	 * @return The posted status when successful.
	 */
	public Status setStatus(String statusText) throws TwitterException {
		return updateStatus(statusText);
	}

	/**
	 * @see Twitter_Users#show(String)
	 */
	@Deprecated
	public User show(String screenName) throws TwitterException, TwitterException.SuspendedUser {
		return users().show(screenName);
	}

	/**
	 * @see Twitter_Users#show(Number)
	 */
	@Deprecated
	public User show(Number userId) {
		return users().show(userId);
	}
	
	/**
	 * @deprecated Use {@link Twitter_Users#show(List)} instead
	 */
	public List<User> bulkShow(List<String> screenNames) {
		return users().show(screenNames);
	}


	/**
	 * @deprecated Use {@link #showById(List)} instead
	 */
	public List<User> bulkShowById(List<? extends Number> userIds) {
		return users().showById(userIds);
	}
	

	/**
	 * @see Twitter_Users#getUser(String)
	 */
	@Deprecated
	public User getUser(String screenName) {
		return show(screenName);
	}

	/**
	 * @see Twitter_Users#getUser(long)
	 */
	@Deprecated
	public User getUser(long userId) {
		return show(userId);
	}

	/**
	 * Split a long message up into shorter chunks suitable for use with
	 * {@link #setStatus(String)} or {@link #sendMessage(String, String)}.
	 *
	 * @param longStatus
	 * @return longStatus broken into a list of max 140 char strings
	 */
	public List<String> splitMessage(String longStatus) {
		// Is it really long?
		if (longStatus.length() <= 140)
			return Collections.singletonList(longStatus);
		// Multiple tweets for a longer post
		List<String> sections = new ArrayList<String>(4);
		StringBuilder tweet = new StringBuilder(140);
		String[] words = longStatus.split("\\s+");
		for (String w : words) {
			// messages have a max length of 140
			// plus the last bit of a long tweet tends to be hidden on
			// twitter.com, so best to chop 'em short too
			if (tweet.length() + w.length() + 1 > 140) {
				// Emit
				tweet.append("...");
				sections.add(tweet.toString());
				tweet = new StringBuilder(140);
				tweet.append(w);
			} else {
				if (tweet.length() != 0)
					tweet.append(" ");
				tweet.append(w);
			}
		}
		// Final bit
		if (tweet.length() != 0)
			sections.add(tweet.toString());
		return sections;
	}

	/**
	 * Map with since_id, page and count, if set. This is called by methods that
	 * return lists of statuses or messages.
	 */
	private Map<String, String> standardishParameters() {
		return addStandardishParameters(new HashMap<String, String>());
	}

	/**
	 * @see Twitter_Users#stopFollowing(String)
	 */
	@Deprecated
	public User stopFollowing(String username) {
		return users().stopFollowing(username);
	}

	/**
	 * @see Twitter_Users#stopFollowing(User)
	 */
	@Deprecated
	public User stopFollowing(User user) {
		return stopFollowing(user.screenName);
	}

	/**
	 * Updates the authenticating user's status.
	 *
	 * @param statusText
	 *            The text of your status update. Must not be more than 160
	 *            characters and should not be more than 140 characters to
	 *            ensure optimal display.
	 * @return The posted status when successful.
	 */
	public Status updateStatus(String statusText) {
		return updateStatus(statusText, null);
	}

	/**
	 * Updates the authenticating user's status and marks it as a reply to the
	 * tweet with the given ID.
	 *
	 * @param statusText
	 *            The text of your status update. Must not be more than 160
	 *            characters and should not be more than 140 characters to
	 *            ensure optimal display.
	 *
	 *
	 * @param inReplyToStatusId
	 *            The ID of the tweet that this tweet is in response to. The
	 *            statusText must contain the username (with an "@" prefix) of
	 *            the owner of the tweet being replied to for for Twitter to
	 *            agree to mark the tweet as a reply. <i>null</i> to leave this
	 *            unset.
	 *
	 * @return The posted status when successful.
	 *         <p>
	 *         Warning: the microformat for direct messages is supported. BUT:
	 *         the return value from this method will be null, and not the
	 *         direct message. Other microformats (such as follow) may result in
	 *         an exception being thrown.
	 *
	 * @throws TwitterException
	 *             if something goes wrong. There is a rare (but not rare
	 *             enough) bug whereby Twitter occasionally returns a success
	 *             code but the wrong tweet. If this happens, the update may or
	 *             may not have worked - wait a bit & check.
	 */
	public Status updateStatus(String statusText, Number inReplyToStatusId)
			throws TwitterException {
		// should we trim statusText??
		// TODO support URL shortening
		if (statusText.length() > 160) {
			throw new IllegalArgumentException(
					"Status text must be 160 characters or less: "
							+ statusText.length() + " " + statusText);
		}
		Map<String, String> vars = InternalUtils.asMap("status", statusText);

		// add in long/lat if set
		if (myLatLong != null) {
			vars.put("lat", Double.toString(myLatLong[0]));
			vars.put("long", Double.toString(myLatLong[1]));
		}

		if (sourceApp != null)
			vars.put("source", sourceApp);
		if (inReplyToStatusId != null) {
			// TODO remove this legacy check
			double v = inReplyToStatusId.doubleValue();
			assert v!=0 && v!=-1;
			vars.put("in_reply_to_status_id", inReplyToStatusId.toString());
		}
		String result;
		try {
			result = http.post(TWITTER_URL + "/statuses/update.json", vars,
					true);
		} catch (E403 e) {
			// test for repetition (which gets a 403)
			Status s = getStatus();
			if (s != null && s.getText().equals(statusText)) {
				throw new TwitterException.Repetition(s.getText());
			}
			throw e;
		}
		try {
			Status s = new Status(new JSONObject(result), null);
			// Weird bug: Twitter occasionally rejects tweets?!
			// TODO does this still happen or have they fixed it? Hard to know
			// with an intermittent bug!
			// Sanity check...
			String targetText = statusText.trim();
			String returnedStatusText = s.text.trim();
			// strip the urls to remove the effects of the t.co shortener
			// (obviously this weakens the safety test, but failure would be 
			// a corner case of a corner case)
			targetText = stripUrls(targetText);
			returnedStatusText = stripUrls(returnedStatusText);			
			if (returnedStatusText.equals(targetText))
				return s;
			{	// is it a direct message? - which doesn't return the true status
				String st = statusText.toLowerCase();
				if (st.startsWith("dm") || st.startsWith("d")) {
					return null;
				}
			}
			// try waiting and rechecking - maybe it did work after all
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// igore the interruption & just report the weirdness
				throw new TwitterException.Unexplained(
						"Unexplained failure for tweet: expected \""
								+ statusText + "\" but got "
								+ returnedStatusText);
			}
			Status s2 = getStatus();
			if (s2 != null && targetText.equals(stripUrls(s2.text.trim()))) {
				// Log.report("Weird transitory bug in Twitter update status with "+targetText);
				return s2;
			}
			throw new TwitterException.Unexplained(
					"Unexplained failure for tweet: expected \"" + statusText
							+ "\" but got " + s2);
		} catch (JSONException e) {
			throw new TwitterException.Parsing(result, e);
		}
	}

	String stripUrls(String text) {
		return InternalUtils.URL_REGEX.matcher(text).replaceAll("");
	}
	
	static final Pattern contentTag = Pattern.compile(
			"<content>(.+?)<\\/content>", Pattern.DOTALL);
	static final Pattern idTag = Pattern.compile("<id>(.+?)<\\/id>",
			Pattern.DOTALL);

	/**
	 * @return true if {@link #setupTwitlonger(String, String)} has been used to
	 *         provide twitlonger.com details.
	 * @see #updateLongStatus(String, long)
	 */
	public boolean isTwitlongerSetup() {
		return twitlongerApiKey != null && twitlongerAppName != null;
	}

	/**
	 * Use twitlonger.com to post a lengthy tweet. See twitlonger.com for more
	 * details on their service.
	 * <p>
	 * Note: You need to have called {@link #setupTwitlonger(String, String)}
	 * before calling this.
	 *
	 * @param message
	 * @param inReplyToStatusId Can be null if this isn't a reply
	 * @return A Twitter status using a truncated message with a link to
	 *         twitlonger.com
	 * @see #setupTwitlonger(String, String)
	 */
	public Status updateLongStatus(String message, Number inReplyToStatusId) {
		if (twitlongerApiKey == null || twitlongerAppName == null) {
			throw new IllegalStateException(
					"Twitlonger api details have not been set! Call #setupTwitlonger() first.");
		}
		if (message.length() < 141) {
			throw new IllegalArgumentException("Message too short ("
					+ inReplyToStatusId
					+ " chars). Just post a normal Twitter status. ");
		}
		String url = "http://www.twitlonger.com/api_post";
		Map<String, String> vars = InternalUtils.asMap("application", twitlongerAppName,
				"api_key", twitlongerApiKey, "username", name, "message",
				message);
		if (inReplyToStatusId != null) {
			assert inReplyToStatusId.doubleValue() != 0 && inReplyToStatusId.doubleValue() != -1; // FIXME remove
			vars.put("in_reply", inReplyToStatusId.toString());
		}
		// ?? set direct_message 0/1 as appropriate if allowing long DMs
		String response = http.post(url, vars, false);
		Matcher m = contentTag.matcher(response);
		boolean ok = m.find();
		if (!ok) {
			throw new TwitterException.TwitLongerException(
					"TwitLonger call failed", response);
		}
		String shortMsg = m.group(1).trim();

		// Post to Twitter
		Status s = updateStatus(shortMsg, inReplyToStatusId);

		m = idTag.matcher(response);
		ok = m.find();
		if (!ok) {
			// weird - but oh well
			return s;
		}
		String id = m.group(1);

		// Once a message has been successfully posted to Twitlonger and
		// Twitter, it would be really useful to send back the Twitter ID for
		// the message. This will allow users to manage their Twitlonger posts
		// and delete not only the Twitlonger post, but also the Twitter post
		// associated with it. It will also makes replies much more effective.
		try {
			url = "http://www.twitlonger.com/api_set_id";
			vars.remove("message");
			vars.remove("in_reply");
			vars.remove("username");
			vars.put("message_id", "" + id);
			vars.put("twitter_id", "" + s.getId());
			http.post(url, vars, false);
		} catch (Exception e) {
			// oh well
		}

		// done
		return s;
	}

	/**
	 * @param truncatedStatus
	 *            If this is a twitlonger.com truncated status, then call
	 *            twitlonger to fetch the full text.
	 * @return the full status message. If this is not a twitlonger status, this
	 *         will just return the status text as-is.
	 * @see #updateLongStatus(String, long)
	 */
	public String getLongStatus(Status truncatedStatus) {
		// regex for http://tl.gd/ID
		int i = truncatedStatus.text.indexOf("http://tl.gd/");
		if (i == -1)
			return truncatedStatus.text;
		String id = truncatedStatus.text.substring(i + 13).trim();
		String response = http.getPage("http://www.twitlonger.com/api_read/"
				+ id, null, false);
		Matcher m = contentTag.matcher(response);
		boolean ok = m.find();
		if (!ok) {
			throw new TwitterException.TwitLongerException(
					"TwitLonger call failed", response);
		}
		String longMsg = m.group(1).trim();
		return longMsg;
	}
	

	/**
	 * Set a language filter for search results. Note: This only applies to
	 * search results.
	 *
	 * @param language
	 *            ISO code for language. Can be null for all languages.
	 *            <p>
	 *            Note: there are multiple different ISO codes! Twitter supports
	 *            ISO 639-1. http://en.wikipedia.org/wiki/ISO_639-1
	 */
	public void setLanguage(String language) {
		lang = language;
	}

	/**
	 * The length of a url after t.co shortening.
	 * Currently 20 characters.
	 * <p>
	 * Use updateConfiguration() if you want to get the latest settings from Twitter.
	 */
	public static int LINK_LENGTH = 20;
	
//	/**
//	 * The length of an https url after t.co shortening.
//	 * This is just 1 more than {@link #LINK_LENGTH}
//	 * <p>
//	 * Use updateConfiguration() if you want to get the latest settings from Twitter.
//	 */
//	public static int LINK_LENGTH_HTTPS = LINK_LENGTH+1;
	
	/**
	 * Update info on Twitter's configuration -- such as shortened url lengths. 
	 */
	public void updateConfiguration() {
		String json = http.getPage(TWITTER_URL+"help/configuration.format", null, false);
		try {
			JSONObject jo = new JSONObject(json);
			LINK_LENGTH = jo.getInt("short_url_length");
	//		LINK_LENGTH_HTTPS = jo.getInt("short_url_length_https"); LINK_LENGTH + 1
	//		characters_reserved_per_media -- this is just LINK_LENGTH 
	//		max_media_per_upload // 1!
			PHOTO_SIZE_LIMIT = jo.getLong("photo_size_limit");
	//		photo_sizes
	//		short_url_length_https
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}
	
	/**
	 * @return the latest global trending topics on Twitter
	 */
	public List<String> getTrends() {
		return getTrends(1);
	}
	
	/**
	 * @param a Yahoo Where-on-Earth ID. c.f. 
	 * http://developer.yahoo.com/geo/geoplanet/
	 * @return the latest regional trending topics on Twitter
	 * @see Twitter_Geo#getTrendRegions()
	 */
	public List<String> getTrends(Number woeid) {
		String jsonTrends = http.getPage(
				TWITTER_URL + "/trends/"+woeid+".json", null, false);
		try {
			JSONArray jarr = new JSONArray(jsonTrends);
			JSONObject json1 = jarr.getJSONObject(0);
			JSONArray json2 = json1.getJSONArray("trends");
			List<String> trends = new ArrayList<String>();
			for (int i = 0; i < json2.length(); i++) {
				JSONObject ti = json2.getJSONObject(i);
				String t = ti.getString("name");
				trends.add(t);
			}
			return trends;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(jsonTrends, e);
		}
	}
	
	

	/**
	 * Provides access to the {@link IHttpClient} which manages the low-level
	 * authentication, posts and gets.
	 */
	public IHttpClient getHttpClient() {
		return http;
	}

	/**
	 * Provides support for fetching many pages. -1 indicates "give me as much
	 * as Twitter will let me have."
	 */
	public int getMaxResults() {
		return maxResults;
	}

	/**
	 * @return retweets that you have made using "new-style" retweets rather
	 *         than the RT microfromat. These are your tweets, i.e. they begin
	 *         "RT @whoever: ". You can get the original tweet via
	 *         {@link Status#getOriginal()}
	 */
	public List<Status> getRetweetsByMe() {
		String url = TWITTER_URL + "/statuses/retweeted_by_me.json";
		Map<String, String> vars = addStandardishParameters(new HashMap<String, String>());
		String json = http.getPage(url, vars, true);
		return Status.getStatuses(json);
	}

	/**
	 * @param type
	 * @param minCalls Standard value = 1.
	 * The minimum number of calls which should be available.
	 * @return true if this is currently rate-limited, & should
	 * not be used for a while. false = OK
	 * @see #getRateLimit(KRequestType) for more info
	 * @see #getRateLimitStatus() for guaranteed up-to-date info
	 */
	public boolean isRateLimited(KRequestType reqType, int minCalls) {
		// Check NORMAL first
		if (reqType!=KRequestType.NORMAL) {
			boolean isLimited = isRateLimited(KRequestType.NORMAL, minCalls);
			if (isLimited) return true;
		}
		RateLimit rl = getRateLimit(reqType);
		// assume things are OK (except for NORMAL which we quickly check by calling Twitter)
		if (rl==null) {
			if (reqType==KRequestType.NORMAL) {
				int rls = getRateLimitStatus();
				return rls >= minCalls;
			}
			return false;
		}
		// in credit?
		if (rl.getRemaining() >= minCalls) return false;
		// out of date?
		if (rl.getReset().getTime() < System.currentTimeMillis()) return false;
		// nope - you're over the limit
		return true;
	}
	
	/**
	 * @return you, or null if this is an anonymous Twitter object.
	 * <p>
	 * This will cache the result if it makes an API call.
	 */
	public User getSelf() {
		if (self!=null) return self;
		if ( ! http.canAuthenticate()) {
			if (name!=null) {
				// not sure this case makes sense, but we may as well handle it
				self = new User(name);
				return self;
			}
			return null;
		}
		account().verifyCredentials();
		name = self.getScreenName();
		return self;
	}
	
	/**
	 * The user. Can be null. Can be a "fake-user" (screenname-only) object.
	 */
	User self;

}
