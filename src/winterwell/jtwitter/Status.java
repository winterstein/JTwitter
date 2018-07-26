package winterwell.jtwitter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.json.JSONArray;
import com.winterwell.json.JSONException;
import com.winterwell.json.JSONObject;

import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.Twitter.KEntityType;
import winterwell.jtwitter.Twitter.TweetEntity;

/**
 * A Twitter status post. .toString() returns the status text.
 * <p>
 * Notes: This is a finalised data object. It exposes its fields for convenient
 * access. If you want to change your status, use
 * {@link Twitter#setStatus(String)} and {@link Twitter#destroyStatus(Status)}.
 * 
 * This class reuses part of Twitter's twitter-text project. Taken from Extractor.java
 * (https://github.com/twitter/twitter-text/blob/master/java/src/com/twitter/Extractor.java)
 * on 2016-07-14. Reproduced under the Apache license.
 */
public final class Status implements ITweet {
	/**
	 * regex for @you mentions
	 */
	static final Pattern AT_YOU_SIR = Pattern.compile("@(\\w+)");

	private static final String FAKE = "fake";

	private static final long serialVersionUID = 1L;

	/**
	 * Convert from a json array of objects into a list of tweets.
	 * 
	 * @param json
	 *            can be empty, must not be null
	 * @throws TwitterException
	 */
	public static List<Status> getStatuses(String json) throws TwitterException {
		if (json.trim().equals(""))
			return Collections.emptyList();
		try {
			JSONArray array = new JSONArray(json);
			return getStatuses(array);
		} catch (JSONException e) {
			// Is it an html error page? E.g. when Twitter is really hosed
			if (json.startsWith("<")) {
				throw new TwitterException.E50X(InternalUtils.stripTags(json));
			}
			throw new TwitterException.Parsing(json, e);
		}
	}
	
	public static List<Status> getStatuses(JSONArray array) throws TwitterException {
		List<Status> tweets = new ArrayList<Status>();
		
		for (Object element : array) {
			if (JSONObject.NULL.equals(element)) {
				continue;
			}
			JSONObject obj = (JSONObject) element;
			Status tweet = new Status(obj, null);
			tweets.add(tweet);
		}
		
		return tweets;
	}

	/**
	 * Search results use a slightly different protocol! In particular w.r.t.
	 * user ids and info.
	 * 
	 * @param searchResults
	 * @return search results as Status objects - but with dummy users! The
	 *         dummy users have a screenname and a profile image url, but no
	 *         other information. This reflects the current behaviour of the
	 *         Twitter API.
	 */
	static List<Status> getStatusesFromSearch(Twitter tw, String json) {
		try {
			JSONObject searchResults = new JSONObject(json);
			List<Status> users = new ArrayList<Status>();
			JSONArray arr = searchResults.getJSONArray("statuses");
			for (int i = 0; i < arr.length(); i++) {
				JSONObject obj = arr.getJSONObject(i);
//				String userScreenName = obj.getString("from_user");
//				String profileImgUrl = obj.getString("profile_image_url");
//				User user = new User(userScreenName);
//				user.profileImageUrl = InternalUtils.URI(profileImgUrl);
				Status s = new Status(obj, null);
				users.add(s);
			}
			return users;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	/**
	 * @param object
	 * @return place, location, failing which geo coordinates
	 * @throws JSONException
	 */
	static Object jsonGetLocn(JSONObject object) throws JSONException {
		String _location = InternalUtils.jsonGet("location", object);
		// no blank strings
		if (_location != null && _location.length() == 0) {
			_location = null;
		}
		JSONObject _place = object.optJSONObject("place");
		if (_location != null) {
			// normalise UT (UberTwitter?) locations
			Matcher m = InternalUtils.latLongLocn.matcher(_location);
			if (m.matches()) {
				_location = m.group(2) + "," + m.group(3);
			}
			return _location; // should we also check geo and place for extra
								// info??
		}
		// Twitter place
		if (_place != null) {
			Place place = new Place(_place, null);
			place.geocoder = Twitter_Geo.class;
			return place;
		}
		JSONObject geo = object.optJSONObject("geo");
		if (geo != null && geo != JSONObject.NULL) {
			JSONArray latLong = geo.getJSONArray("coordinates");
			_location = latLong.get(0) + "," + latLong.get(1);
		}
		// TODO place (when is this set?)
		return _location;
	}

	public final Date createdAt;

	private EnumMap<KEntityType, List<TweetEntity>> entities;

	private boolean favorited;

	/**
	 * Warning: use equals() not == to compare these!
	 */
	public final BigInteger id;

	/**
	 * Often null (even when this Status is a reply). This is the in-reply-to
	 * status id as reported by Twitter.
	 */
	public final BigInteger inReplyToStatusId;

	private String location;

	/**
	 * null, except for official retweets when this is the original retweeted
	 * Status.
	 */
	private Status original;

	private Place place;

	/**
	 * Represents the number of times a status has been retweeted using
	 * _new-style_ retweets. -1 if unknown.
	 */
	public final int retweetCount;
	/**
	 * Number of likes for this tweet. -1 if unknown.
	 */
	public final int favoriteCount;
	
	boolean sensitive;

	/**
	 * E.g. "web" vs. "im"<br>
	 * For apps this will return an a-tag, e.g. 
	 * "&lt;a href="http://blackberry.com/twitter" rel="nofollow"&gt;Twitter for BlackBerry®&lt;/a&gt;"
	 * You'll probably want to discard the a-tag wrapper by using {@link #getSource()} instead.
	 * <p>
	 * "fake" if this Status was made locally or from an RSS feed rather than
	 * retrieved from Twitter json (as normal).
	 */
	public final String source;	
	
	/** The actual status text. */
	public final String text;

	/**
	 * Rarely null.
	 * <p>
	 * When can this be null?<br>
	 * - If creating a "fake" tweet via
	 * {@link Status#Status(User, String, long, Date)} and supplying a null User!
	 * - For the original of a retweet -- Twitter needn't tell us the details :(
	 */
	public final User user;

	private String lang;

	private transient String _rawtext;

	private boolean retweet;

	private boolean quotedStatus;
	
	/**
	 * The current Twitter spec states that leading @mentions will be displayed
	 * as a separate element (of the form "in reply to Full Name") on the web
	 * interface - likewise, attachment URLs will be hidden.
	 * This has not been rolled out but some users report seeing it in A/B
	 * testing (as of March 2017).
	 * The display_text_range property denotes which regions of the tweet text
	 * are which.
	 */
	private int displayStart;
	private int displayEnd;

	/**
	 * A debugging convenience: Keep the raw json object (but don't save it).
	 */
	private transient JSONObject raw;
	
	public boolean isRetweet() {
		return retweet;
	}
	public boolean isQuotedStatus() {
		return quotedStatus;
	}
	
	/**
	 * BCP 47 language identifiers. 
	 * See the list of languages on Twitter's advanced search page. 
	 * <br>
	 * WARNING: Twitter's language detection is NOT reliable!
	 * 
	 * @return language code, or null if no language could be detected.
	 */
	public String getLang() {
		return lang;
	}

//	private String[] withheldIn;
	
//	/**
	// Should we have this??
//	 * @return usually null!
//	 * Otherwise, a list of country codes for where this tweet has been censored.
//	 */
//	public String[] getWithheldIn() {
//		return withheldIn;
//	}
	
	private static final Pattern RT_AUTHOR = Pattern.compile("RT @([^: ]+):");

	/**
	 * @param object
	 * @param user
	 *            Set when parsing the json returned for a User. null when
	 *            parsing the json returned for a Status.
	 * @throws TwitterException
	 */
	@SuppressWarnings("deprecation")
	public Status(JSONObject object, User user) throws TwitterException {
		this.raw = object;
		try {
			String _id = object.optString("id_str");
			id = new BigInteger(_id == "" ? object.get("id").toString() : _id);
			
			// Extended tweets: REST API mode
			// Depending on whether this was obtained with param tweet_mode=extended,
			// either "text" or "full_text" might be present.
			_rawtext = InternalUtils.jsonGet("full_text", object);
			if (_rawtext == null) {
				_rawtext = InternalUtils.jsonGet("text", object);
			}
			
			// Entities (switched on by Twitter.setIncludeTweetEntities(true))
			JSONObject jsonEntities = object.optJSONObject("entities");
			
			// Display range: Start and end of region of tweet text which should actually be displayed.
			JSONArray displayRange = object.optJSONArray("display_text_range");
			
			// Streaming API bundles all extended-tweet data into its own field.
			JSONObject extended = object.optJSONObject("extended_tweet");
			if (extended != null) {
				String fullText = extended.optString("full_text");
				if (fullText != null) _rawtext = fullText;
				
				JSONObject _entities = extended.optJSONObject("entities");
				if (_entities != null) jsonEntities = _entities;
				
				JSONArray _displayRange = extended.optJSONArray("display_text_range");
				if (_displayRange != null) displayRange = _displayRange;
			}
			
			// Extract display range
			if (displayRange != null) {
				Integer _displayStart = displayRange.optInt(0);
				if (_displayStart != null) displayStart = _displayStart;
				Integer _displayEnd = displayRange.optInt(0);
				if (_displayEnd != null) displayEnd = _displayEnd;
			}
			
			// retweet?
			JSONObject retweeted = object.optJSONObject("retweeted_status");			
			if (retweeted != null) {
				retweet = true;
				if (retweeted.has("user")) {
					original = new Status(retweeted, null);
				} else {
					// no user info?! Seen repeatedly August 2015. Fix up from the text					
					Matcher m = RT_AUTHOR.matcher(_rawtext);
					if (m.find()) {
						String srcAuthorName = m.group(1);
						User srcAuthor = new User(srcAuthorName);
						original = new Status(retweeted, srcAuthor);
					} else {
						// No author info :(
						original = new Status(retweeted, null);
					}					
				}
			}
			
			// quoted tweet?
			JSONObject quoted = object.optJSONObject("quoted_status");
			if (quoted != null) {
				quotedStatus = true;
				try {
					original = new Status(quoted, null);
				} catch	(Throwable ex) {
					InternalUtils.log("bad.json", "Quoted status could not be parsed: "+ex);
				}
			}
			
			// text!			
			String _text = _rawtext;
			// Twitter have started truncating RTs -- let's fix the text up if we can
			// Feb/March 2015: We should get entities from the original in ALL cases (not just marked as truncated)
			// or else there's a risk of getting truncated entites. -- Alex
//			boolean truncated = object.optBoolean("truncated"); // This can lie (bugs seen March 2013) -- so let's also check the text
			String rtStart = null;
			if (original!=null && _text.startsWith("RT ")) {
				rtStart = "RT @"+original.getUser()+": ";
				_text = rtStart+original.getText();				
			} else {
				_text = InternalUtils.unencode(_text); // bugger - this screws up the indices in tweet entities
			}
			text = _text;
			
			// date
			String c = InternalUtils.jsonGet("created_at", object);
			createdAt = InternalUtils.parseDate(c);
			// source - sometimes encoded (search), sometimes not
			// (timelines)!
			String src = InternalUtils.jsonGet("source", object);
			source = src!=null&&src.contains("&lt;") ? InternalUtils.unencode(src) : src;
			// threading
			String irt = InternalUtils.jsonGet("in_reply_to_status_id", object);
			if (irt == null || irt.length()==0) {
				// Twitter doesn't give in-reply-to for retweets and quote tweets
				// - but since we have the info, let's make it available
				inReplyToStatusId = original == null ? null : original.getId();
			} else {
				inReplyToStatusId = new BigInteger(irt);
			}
			favorited = object.optBoolean("favorited");
			
			// set user
			if (user != null) {
				this.user = user;
			} else {
				JSONObject jsonUser = object.optJSONObject("user");
				// null user happens in very rare circumstances, which I
				// have not pinned down yet.
				if (jsonUser == null) {
					this.user = null;					
				} else if (jsonUser.opt("screen_name")==null) {
					// Seen a bug where the jsonUser is just
					// {"id":24147187,"id_str":"24147187"}
					// Not sure when/why this happens
					String _uid = jsonUser.optString("id_str");
					BigInteger userId = new BigInteger(_uid == "" ? object.get(
							"id").toString() : _uid);
					this.user = new User(null, userId);
				} else {
					// normal JSON case
					this.user = new User(jsonUser, this);
				}
			}
			// location if geocoding is on
			Object _locn = Status.jsonGetLocn(object);
			location = _locn == null ? null : _locn.toString();
			if (_locn instanceof Place) {
				place = (Place) _locn;
			}
			// language if specified
			String _lang = object.optString("lang");
			lang = "und".equals(_lang)? null : _lang;

			retweetCount = object.optInt("retweet_count", -1);
			favoriteCount = object.optInt("favorite_count", -1);
			// favourites??
			
			// ignore this as it can be misleading: true is reliable, false isn't
			// retweeted = object.optBoolean("retweeted");
			
			// Note: Twitter filters out dud @names
			if (jsonEntities != null) {
				entities = new EnumMap<Twitter.KEntityType, List<TweetEntity>>(
						KEntityType.class);
				setupEntities(_rawtext, rtStart, jsonEntities);								
			}
			
			// censorship flags
			// Should we have this??
//			String withheld = object.optString("withheld_in_countries");
//			if (withheld!=null && withheld.length()!=0) {
//				withheldIn = withheld.split(", ");
//			}
//			"withheld_scope": "status" or "user"
			sensitive = object.optBoolean("possibly_sensitive");
		} catch (JSONException e) {
			throw new TwitterException.Parsing(null, e);
		}
	}

	private void setupEntities(String _rawtext, String rtStart,
			JSONObject jsonEntities) {
		if (rtStart!=null) {
			// truncation! the entities returned are likely to be duds -- adjust from the original instead
			int rt = rtStart.length();
			for (KEntityType type : KEntityType.values()) {
				List<TweetEntity> es = original.getTweetEntities(type);
				if (es==null) continue;
				ArrayList rtEs = new ArrayList(es.size());
				for (TweetEntity e : es) {
					TweetEntity rte = new TweetEntity(this, e.type, 
							/* safety checks on length are paranoia (could be removed) */
							Math.min(rt+e.start, text.length()), Math.min(rt+e.end, text.length()), e.display);
					rtEs.add(rte);
				}
				entities.put(type, rtEs);
			}	
			return;
		}
		// normal case
		for (KEntityType type : KEntityType.values()) {
			List<TweetEntity> es = TweetEntity.parse(this, _rawtext, type,
					jsonEntities);
			entities.put(type, es);
		}		
	}

	/**
	 * Create a *fake* Status object. This does not represent a real tweet!
	 * Uses: few and far between. There is no real contract as to how objects
	 * made in this way will behave.
	 * <p>
	 * If you want to post a tweet (and hence get a real Status object), use
	 * {@link Twitter#setStatus(String)}.
	 * 
	 * @param user
	 *            Can be null or bogus -- provided that's OK with your code.
	 * @param text
	 *            Can be null or bogus -- provided that's OK with your code.
	 * @param id
	 *            Can be null or bogus -- provided that's OK with your code.
	 * @param createdAt
	 *            Can be null -- provided that's OK with your code.
	 */
	@Deprecated
	public Status(User user, String text, Number id, Date createdAt) {
		this.text = text;
		this.user = user;
		this.createdAt = createdAt;
		this.id = id == null ? null
				: (id instanceof BigInteger ? (BigInteger) id : new BigInteger(
						id.toString()));
		inReplyToStatusId = null;
		source = FAKE;
		retweetCount = -1;
		favoriteCount = -1;
	}

	/**
	 * Tests by class=Status and tweet id number
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Status other = (Status) obj;
		return id.equals(other.id);
	}

	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * @return The Twitter id for this post. This is used by some API methods.
	 */
	@Override
	public BigInteger getId() {
		return id;
	}

	@Override
	public String getLocation() {
		return location;
	}

	/**
	 * @return list of \@mentioned people (there is no guarantee that these
	 *         mentions are for correct Twitter screen-names). May be empty,
	 *         never null. Screen-names are always lowercased -- unless
	 *         {@link Twitter#CASE_SENSITIVE_SCREENNAMES} is switched on.
	 */
	@Override
	public List<String> getMentions() {
		// TODO test & use this
		// List<TweetEntity> ms = entities.get(KEntityType.user_mentions);
		Matcher m = AT_YOU_SIR.matcher(text);
		List<String> list = new ArrayList<String>(2);
		while (m.find()) {
			// skip email addresses (and other poorly formatted things)
			if (m.start() != 0
					&& Character.isLetterOrDigit(text.charAt(m.start() - 1))) {
				continue;
			}
			String mention = m.group(1);
			// enforce lower case? (normally yes)
			if (!Twitter.CASE_SENSITIVE_SCREENNAMES) {
				mention = mention.toLowerCase();
			}
			list.add(mention);
		}
		return list;
	}

	/**
	 * Only set for official new-style retweets and quoted tweets. This is the original retweeted
	 * Status. null otherwise.
	 */
	public Status getOriginal() {
		return original;
	}

	@Override
	public Place getPlace() {
		return place;
	}
	

	/**
	 * E.g. "web" vs. "im"<br>
	 * WARNING: this is different from the field {@link #source}. This method will remove the wrapping a-tag.
	 * <p>
	 * "fake" if this Status was made locally or from an RSS feed rather than
	 * retrieved from Twitter json (as normal).
	 */
	public String getSource() {
		return InternalUtils.stripTags(source);
	}

	/** The actual status text. This is also returned by {@link #toString()}.
	 * NB: This can be longer than 140 chars for a retweet. */
	@Override
	public String getText() {
		return text;
	}

	@Override
	public List<TweetEntity> getTweetEntities(KEntityType type) {
		return entities == null ? null : entities.get(type);
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	/**
	 * true if this has been marked as a favourite by the authenticating user
	 */
	public boolean isFavorite() {
		return favorited;
	}

	/**
	 * A <i>self-applied</i> label for sensitive content (eg. X-rated images).
	 * Obviously, you can only rely on this label if the tweeter is reliably
	 * setting it.
	 * 
	 * @return true=kinky, false=family-friendly
	 */
	public boolean isSensitive() {
		return sensitive;
	}

	/**
	 * @return The text of this status. E.g. "Kicking fommil's arse at
	 *         Civilisation."
	 */

	@Override
	public String toString() {
		return text;
	}

	/**
	 * @return text, with the t.co urls replaced with their originals (if known).
	 * Use-case: for filtering based on text contents, when we want to
	 * match against the full url.
	 * Note: this does NOT resolve short urls from bit.ly etc. 
	 */
	public String getDisplayText() {
		return getDisplayText2(this);
	}

	/**
	 * Expand urls
	 * @param tweet
	 * @return
	 */
	static String getDisplayText2(ITweet tweet) {
		try {
			List<TweetEntity> es = tweet.getTweetEntities(KEntityType.urls);
			String _text = tweet.getText();
			IndexConverter fixer = new IndexConverter(_text);
			
			if (es==null || es.size()==0) {
				// Is it a truncated retweet? That should be handled in the constructor.			
				return _text;
			}
			
			StringBuilder sb = new StringBuilder(200);
			int i = 0;
			
			// sort by 
			for (TweetEntity entity : es) {
				// What? there are invalid entities?
				if (entity.start < i) {				
					InternalUtils.log("jtwitter", "#escalate bogus entity ordering in "+tweet.getId()+" "+entity+" in "+_text);
					continue;
				}
				
				if (i > _text.length() || entity.start > _text.length()) {
					String raw = tweet instanceof Status? ((Status)tweet)._rawtext : null;
					InternalUtils.log("jtwitter", "#escalate bogus entity in "+tweet.getId()+" "+tweet.getClass().getSimpleName()+" "+entity+" in "+_text+" raw:"+raw);
					continue;
				}
				
				// replace the short-url with the display version
				sb.append(_text.substring(i, fixer.codePointsToCodeUnits(entity.start)));
				sb.append(entity.displayVersion());
				i = fixer.codePointsToCodeUnits(entity.end);
			}					
			if (i < _text.length()) {
				sb.append(_text.substring(i));
			}
			return sb.toString();
		} catch(Exception ex) {
			// paranoid fallback, in case odd characters manage to throw the display text off 
			InternalUtils.log("tweet.text.error", "getDisplayText for "+tweet.getClass()+" "+tweet.getId()+" "+ex+" from "+tweet.getText());
			return tweet.getText();
		}
	}

	public String getUrl() {
		return "https://twitter.com/"+user.screenName+"/status/"+id;
	}


	/**
	 * Java String indices work on the assumption that every character is 2 bytes wide.
	 * Characters outside the Basic Multilingual Plane are 4 bytes wide & so occupy two
	 * chars in a String. This includes most emoji, among others.
	 * When Twitter presents substring indices (eg for replacing Entities with their
	 * display equivalents) it treats non-BMP characters as single characters, meaning
	 * the indices for an entity appearing after a non-BMP char will be off by one.
	 * (Or two, if there were two non-BMP characters before it, etc.)
	 * This class converts the character / code-point indices for the String it is initialised with.
	 *
	 * This code is part of Twitter's twitter-text project. Taken from Extractor.java
	 * (https://github.com/twitter/twitter-text/blob/master/java/src/com/twitter/Extractor.java)
	 * on 2016-07-14. Reproduced under the Apache license.
	 *
	 * An efficient converter of indices between code points and code units.
	 */
	private static final class IndexConverter {
		protected final String text;
			// Keep track of a single corresponding pair of code unit and code point
			// offsets so that we can re-use counting work if the next requested
			// entity is near the most recent entity.
			protected int codePointIndex = 0;
			protected int charIndex = 0;
			
			IndexConverter(String text) {
			this.text = text;
		}
		
		/**
		 * @param _charIndex Index into the string measured in code units.
		 * @return The code point index that corresponds to the specified character index.
		 */
		int codeUnitsToCodePoints(int _charIndex) {
			if (_charIndex < this.charIndex) {
				this.codePointIndex -= text.codePointCount(_charIndex, this.charIndex);
			} else {
				this.codePointIndex += text.codePointCount(this.charIndex, _charIndex);
			}
			this.charIndex = _charIndex;
			
			// Make sure that charIndex never points to the second code unit of a
			// surrogate pair.
			if (_charIndex > 0 && Character.isSupplementaryCodePoint(text.codePointAt(_charIndex - 1))) {
				this.charIndex -= 1;
			}
			return this.codePointIndex;
		}
		
		/**
		 * @param _codePointIndex Index into the string measured in code points.
		 * @return the code unit index that corresponds to the specified code point index.
		 */
		int codePointsToCodeUnits(int _codePointIndex) {
			// Note that offsetByCodePoints accepts negative indices.
			this.charIndex = text.offsetByCodePoints(this.charIndex, _codePointIndex - this.codePointIndex);
			this.codePointIndex = _codePointIndex;
			return this.charIndex;
		}
	}
}