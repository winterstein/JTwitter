package winterwell.jtwitter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
	
}
