package winterwell.jtwitter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.User;

/**
 * API calls relating to users and relationships (the social network).
 * Use {@link Twitter#users()} to get one.
 * <p>
 * Conceptually, this is an extension of {@link Twitter}. The methods are
 * here because Twitter was getting crowded.
 * 
 * @author Daniel
 */
public class Twitter_Users {

	private final Twitter jtwit;
	private final IHttpClient http;

	Twitter_Users(Twitter jtwit) {
		this.jtwit = jtwit;
		http = jtwit.getHttpClient();
	}


	/**
	 * Lookup user info. This is done in batches of 100. Users can look up at
	 * most 1000 users in an hour.
	 *
	 * @param screenNames Can be empty (in which case we avoid wasting an API call)
	 * @return user objects for screenNames. Warning 1: This may be less than
	 *         the full set if Twitter returns an error part-way through (e.g.
	 *         you hit your rate limit). Warning 2: the ordering may be
	 *         different from the screenNames parameter
	 * @see #showById(List)
	 */
	public List<User> show(List<String> screenNames) {
		if (screenNames.isEmpty()) return Collections.EMPTY_LIST;
		return jtwit.bulkShow2("/users/lookup.json", String.class, screenNames);
	}
	
	

	/**
	 * Lookup user info. Same as {@link #show(List)}, but works with Twitter
	 * user-ID numbers.
	 *
	 * @param userIds. Can be empty (in which case we avoid making a wasted API call).
	 */
	public List<User> showById(Collection<? extends Number> userIds) {
		if (userIds.isEmpty()) return Collections.EMPTY_LIST;
		return jtwit.bulkShow2("/users/lookup.json", Number.class, userIds);
	}
	
	/**
	 * Bulk-fetch relationship info by screen-name.
	 * @param screenNames Can be empty
	 * @return User objects which are mostly blank, but which have {@link User#isFollowingYou()}
	 * and {@link User#isFollowedByYou()} set (plus name, screenname and id).
	 * @see #getRelationshipInfoById(List)
	 */
	public List<User> getRelationshipInfo(List<String> screenNames) {
		if (screenNames.isEmpty()) return Collections.EMPTY_LIST;
		List<User> users = jtwit.bulkShow2("/friendships/lookup.json", String.class, screenNames);
		return users;
	}

	/**
	 * Bulk-fetch relationship info by user-id.
	 * @param userIDs Can be empty
	 * @return User objects which are mostly blank, but which have {@link User#isFollowingYou()}
	 * and {@link User#isFollowedByYou()} set (plus name, screenname and id).
	 * @see #getRelationshipInfo(List)
	 */
	public List<User> getRelationshipInfoById(List<? extends Number> userIDs) {
		if (userIDs.isEmpty()) return Collections.EMPTY_LIST;
		List<User> users = jtwit.bulkShow2("/friendships/lookup.json", Number.class, userIDs);
		return users;
	}
	
	/**
	 * @return an array of numeric user ids the authenticating user is blocking.
	 * Use {@link #showById(Collection)} if you want to convert thse into User objects.
	 */
	public List<Number> getBlockedIds() {
		String json = http.getPage(jtwit.TWITTER_URL+"/blocks/blocking/ids.json", null, true);
		try {
			JSONArray arr = new JSONArray(json);
			List<Number> ids = new ArrayList(arr.length());
			for(int i=0,n=arr.length(); i<n; i++) {
				ids.add(arr.getLong(i));
			}
			return ids;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}
	
	public User reportSpammer(String screenName) {
		HashMap vars = new HashMap();
		vars.put("screen_name", screenName);			
		// Returns if the authenticating user is blocking a target user. 
		// Will return the blocked user's object if a block exists, and error with 
		// a HTTP 404 response code otherwise.
		String json = http.post(jtwit.TWITTER_URL+"/report_spam.json", vars, true);
		return InternalUtils.user(json);
	}
	
	/**
	 * blocks/create: Blocks screenName from following the authenticating user. 
	 * In addition the blocked user will not show in the authenticating users mentions 
	 * or timeline (unless retweeted by another user). If a follow or friend relationship 
	 * exists it is destroyed.
	 * @param screenName
	 * @return info on the (now blocked) user
	 * @see #unblock(String)
	 */
	public User block(String screenName) {
		HashMap vars = new HashMap();
		vars.put("screen_name", screenName);			
		// Returns if the authenticating user is blocking a target user. 
		// Will return the blocked user's object if a block exists, and error with 
		// a HTTP 404 response code otherwise.
		String json = http.post(jtwit.TWITTER_URL+"/blocks/create.json", vars, true);
		return InternalUtils.user(json);
	}
	
	/**
	 * blocks/destroy: Un-blocks screenName for the authenticating user. 
	 * Returns the un-blocked user when successful. If relationships existed 
	 * before the block was instated, they will not be restored.
	 * @param screenName
	 * @return the now un-blocked User
	 * @see #block(String)
	 */
	public User unblock(String screenName) {
		HashMap vars = new HashMap();
		vars.put("screen_name", screenName);			
		// Returns if the authenticating user is blocking a target user. 
		// Will return the blocked user's object if a block exists, and error with 
		// a HTTP 404 response code otherwise.
		String json = http.post(jtwit.TWITTER_URL+"/blocks/destroy.json", vars, true);
		return InternalUtils.user(json);
	}
	
	public boolean isBlocked(String screenName) {		
		try {
			HashMap vars = new HashMap();
			vars.put("screen_name", screenName);			
			// Returns if the authenticating user is blocking a target user. 
			// Will return the blocked user's object if a block exists, and error with 
			// a HTTP 404 response code otherwise.
			String json = http.getPage(jtwit.TWITTER_URL+"/blocks/exists.json", vars, true);
			return true;
		} catch (TwitterException.E404 e) {
			return false;
		}
	}
	
	public boolean isBlocked(Long userId) {		
		try {
			HashMap vars = new HashMap();
			vars.put("user_id", Long.toString(userId));			
			// Returns if the authenticating user is blocking a target user. 
			// Will return the blocked user's object if a block exists, and error with 
			// a HTTP 404 response code otherwise.
			String json = http.getPage(jtwit.TWITTER_URL+"/blocks/exists.json", vars, true);
			return true;
		} catch (TwitterException.E404 e) {
			return false;
		}
	}
}
