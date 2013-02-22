package winterwell.jtwitter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import winterwell.json.JSONArray;
import winterwell.json.JSONException;
import winterwell.json.JSONObject;
import winterwell.jtwitter.Twitter.IHttpClient;

/**
 * API calls relating to twitter lists and memberships/subscriptions. <br/>
 * TODO: work in progress, move the rest of the methods from {@link Twitter} related to lists to this class!
 * @author azeef
 */
public class Twitter_Lists {

	private final Twitter jtwit;
	private final IHttpClient http;

	Twitter_Lists(final Twitter jtwit) {
		this.jtwit = jtwit;
		this.http = jtwit.getHttpClient();
	}

	// GET METHODS /////////////////////////////////////////////////////////

	/** 
	 * gets the lists the user identified by the {@code userId} owns
	 * @deprecated use {@link #getListsAll(Long)} since version 1.1 twitter no longer supports getting only the list the user is subscribed to...
	 */
	@Deprecated
	public List<TwitterList> getLists(final Long userId) {
		return getListsAll(userId);
	}

	/**
	 * gets the lists the user identified by the {@code screenName} owns
	 * @deprecated use {@link #getListsAll(String)} since version 1.1 twitter no longer supports getting only the list the user is subscribed to...
	 */
	@Deprecated
	public List<TwitterList> getLists(final String screenName) {
		return getListsAll(screenName);
	}

	/** gets all the lists the user identified by the {@code userId} subscribes to */
	public List<TwitterList> getListsAll(final Long userId) {
		final String url = jtwit.TWITTER_URL + (isAPI11(jtwit) ? "/lists/list.json" : "/lists.json");
		final Map<String, String> vars = InternalUtils.asMap("user_id", userId.toString());
		return getTwitterLists(url, vars);
	}

	/** gets the lists the user identified by the {@code screenName} subscribes to */
	public List<TwitterList> getListsAll(final String screenName) {
		final String url = jtwit.TWITTER_URL + (isAPI11(jtwit) ? "/lists/list.json" : "/lists.json");
		final Map<String, String> vars = InternalUtils.asMap("screen_name", screenName);
		return getTwitterLists(url, vars);
	}

	/**
	 * returns the list members for the list identified by the {@code listId}
	 * <p>
	 * WARNING: twitter returns members in batches of 20, so this method can call twitter up to 25 times (500 max list size)
	 * </p>
	 */
	public List<User> getListMembers(final Long listId) {
		final String url = jtwit.TWITTER_URL + "/lists/members.json";
		final Map<String, String> vars = getListVars(listId);
		long cursor = -1;
		final List<User> users = new LinkedList<User>();
		while (cursor != 0) {
			vars.put("cursor", Long.toString(cursor));
			final String json = http.getPage(url, vars, true);
			try {
				final JSONObject jobj = new JSONObject(json);
				final JSONArray jarr = jobj.getJSONArray("users");
				final List<User> users1page = User.getUsers2(jarr);
				users.addAll(users1page);
				cursor = jobj.getLong("next_cursor");
			} catch (final JSONException e) {
				throw new TwitterException.Parsing("Could not parse user list", e);
			}
		}
		return users;
	}

	/**
	 * returns the subscribers to the list identified by the {@code listId}
	 * <p>
	 * WARNING: twitter returns subscribers in batches of 20, so this method can call twitter a lot of times!
	 * </p>
	 */
	public List<User> getListSubscribers(final Long listId) {
		final String url = jtwit.TWITTER_URL + "/lists/subscribers.json";
		final Map<String, String> vars = getListVars(listId);
		long cursor = -1;
		final List<User> users = new LinkedList<User>();
		while (cursor != 0) {
			vars.put("cursor", Long.toString(cursor));
			final String json = http.getPage(url, vars, true);
			try {
				final JSONObject jobj = new JSONObject(json);
				final JSONArray jarr = jobj.getJSONArray("users");
				final List<User> users1page = User.getUsers2(jarr);
				users.addAll(users1page);
				cursor = jobj.getLong("next_cursor");
			} catch (final JSONException e) {
				throw new TwitterException.Parsing("Could not parse user list", e);
			}
		}
		return users;
	}

	/**
	 * returns the timeline for the list identified by the {@code listId}; all standard parameters for loading timelines apply, so
	 * {@link Twitter#setCount(Integer)}, {@link Twitter#setIncludeRTs(boolean)}, {@link Twitter#setIncludeTweetEntities(boolean)},
	 * {@link Twitter#setMaxResults(int)}, {@link Twitter#setSinceId(Number)} and {@link Twitter#setUntilId(Number)} can all be called before
	 * calling this method to influence the tweets fetched
	 */
	public List<Status> getListTimeline(final Long listId) {
		final Map<String, String> vars = getListVars(listId);
		jtwit.addStandardishParameters(vars);
		return jtwit.getStatuses(jtwit.TWITTER_URL + "/lists/statuses.json", vars, true); // getting statuses for a twitter list requires authentication
	}

	/** retrieves the list identified by the {@code listId} */
	public TwitterList show(final Long listId) {
		final String url = jtwit.TWITTER_URL + "/lists/show.json";
		final Map<String, String> vars = getListVars(listId);
		return getTwitterList(url, vars);
	}
	
	/** retrieves the list identified by the {@code slug} and {@code ownerScreenName} */
	public TwitterList show(final String slug, String ownerScreenName) {
		final String url = jtwit.TWITTER_URL + "/lists/show.json";
		final Map<String, String> vars = getListVars(slug, ownerScreenName, null);
		return getTwitterList(url, vars);
	}
	
	/** retrieves the list identified by the {@code slug} and {@code ownerId} */
	public TwitterList show(final String slug, long ownerId) {
		final String url = jtwit.TWITTER_URL + "/lists/show.json";
		final Map<String, String> vars = getListVars(slug, null, ownerId);
		return getTwitterList(url, vars);
	}

	// POST METHODS ////////////////////////////////////////////////////////

	/**
	 * updates the description for the list identified by the {@code listId}; the authenticated user should own the list
	 */
	public TwitterList setDescription(final Long listId, final String description) {
		final String url = jtwit.TWITTER_URL + "/lists/update.json";
		final Map<String, String> vars = getListVars(listId);
		vars.put("description", description);
		return postTwitterList(url, vars);
	}

	/**
	 * sets the list identified by the {@code listId} to private or public, depending on the value of {@code isPrivate}; the authenticated user
	 * should own the list
	 */
	public TwitterList setPrivate(final Long listId, final boolean isPrivate) {
		final String url = jtwit.TWITTER_URL + "/lists/update.json";
		final Map<String, String> vars = getListVars(listId);
		vars.put("mode", isPrivate ? "private" : "public");
		return postTwitterList(url, vars);
	}

	/**
	 * deletes the list identified by the {@code listId}; the authenticated user should own the list
	 * @return
	 */
	public TwitterList delete(final Long listId) {
		final String url = jtwit.TWITTER_URL + "/lists/destroy.json";
		final Map<String, String> vars = getListVars(listId);
		return postTwitterList(url, vars);
	}

	/** creates a new twitterlist for the authenticated user */
	public TwitterList create(final String listName, final boolean isPrivate, final String description) {
		final String url = jtwit.TWITTER_URL + "/lists/create.json";
		final Map<String, String> listVars = InternalUtils.asMap("name", listName, "mode", isPrivate ? "private" : "public", "description", description);
		return postTwitterList(url, listVars);
	}

	/**
	 * add a user identified by the {@code userId} to the list identified by the {@code listId}. the authenticated user should own the list and
	 * list size is limited to 500 users
	 */
	public TwitterList addMember(final Long listId, final Long userId) {
		final String url = jtwit.TWITTER_URL + "/lists/members/create.json";
		final Map<String, String> listVars = getListVars(listId);
		listVars.put("user_id", userId.toString());
		return postTwitterList(url, listVars);
	}

	/**
	 * add a user identified by the {@code screenName} to the list identified by the {@code listId}. the authenticated user should own the list and
	 * list size is limited to 500 users
	 */
	public TwitterList addMember(final Long listId, final String screenName) {
		final String url = jtwit.TWITTER_URL + "/lists/members/create.json";
		final Map<String, String> listVars = getListVars(listId);
		listVars.put("screen_name", screenName);
		return postTwitterList(url, listVars);
	}

	/**
	 * adds all users identified by the {@code userIds} to the list identified by the {@code listId}. the authenticated user should own the list
	 * and list size is limited to 500 users. this method will call twitter with at most 100 users at a time.
	 */
	public TwitterList addAllById(final Long listId, final List<Long> userIds) {
		final String url = jtwit.TWITTER_URL + "/lists/members/create_all.json";
		final Map<String, String> listVars = getListVars(listId);
		final int batchSize = 100;
		TwitterList theList = null;
		for (int i = 0; i < userIds.size(); i += batchSize) {
			final String userIdsStr = InternalUtils.join(userIds, i, i + batchSize);
			listVars.put("user_id", userIdsStr);
			theList = postTwitterList(url, listVars);
		}
		return theList;
	}

	/**
	 * adds all users identified by the {@code screenNames} to the list identified by the {@code listId}. the authenticated user should own the
	 * list and list size is limited to 500 users. this method will call twitter with at most 100 users at a time.
	 */
	public TwitterList addAllByScreenName(final Long listId, final List<String> screenNames) {
		final String url = jtwit.TWITTER_URL + "/lists/members/create_all.json";
		final Map<String, String> listVars = getListVars(listId);
		final int batchSize = 100;
		TwitterList theList = null;
		for (int i = 0; i < screenNames.size(); i += batchSize) {
			final String screenNamesStr = InternalUtils.join(screenNames, i, i + batchSize);
			listVars.put("screen_name", screenNamesStr);
			theList = postTwitterList(url, listVars);
		}
		return theList;
	}

	/**
	 * remove a user identified by the {@code userId} from the list identified by the {@code listId}. the authenticated user should own the list
	 */
	public TwitterList removeMember(final Long listId, final Long userId) {
		final String url = jtwit.TWITTER_URL + "/lists/members/destroy.json";
		final Map<String, String> map = getListVars(listId);
		map.put("user_id", userId.toString());
		return postTwitterList(url, map);

	}

	/**
	 * remove a user identified by the {@code screenName} from the list identified by the {@code listId}. the authenticated user should own the
	 * list
	 */
	public TwitterList removeMember(final Long listId, final String screenName) {
		final String url = jtwit.TWITTER_URL + "/lists/members/destroy.json";
		final Map<String, String> map = getListVars(listId);
		map.put("screen_name", screenName);
		return postTwitterList(url, map);
	}

	/**
	 * removes all users identified by the {@code userIds} from the list identified by the {@code listId}. the authenticated user should own the
	 * list. this method will call twitter with at most 100 users at a time.
	 */
	public TwitterList removeAllById(final Long listId, final List<Long> userIds) {
		final String url = jtwit.TWITTER_URL + "/lists/members/destroy_all.json";
		final Map<String, String> listVars = getListVars(listId);
		final int batchSize = 100;
		TwitterList theList = null;
		for (int i = 0; i < userIds.size(); i += batchSize) {
			final String userIdsStr = InternalUtils.join(userIds, i, i + batchSize);
			listVars.put("user_id", userIdsStr);
			theList = postTwitterList(url, listVars);
		}
		return theList;
	}

	/**
	 * removes all users identified by the {@code userIds} from the list identified by the {@code listId}. the authenticated user should own the
	 * list. this method will call twitter with at most 100 users at a time.
	 */
	public TwitterList removeAllByScreenName(final Long listId, final List<String> screenNames) {
		final String url = jtwit.TWITTER_URL + "/lists/members/destroy_all.json";
		final Map<String, String> listVars = getListVars(listId);
		final int batchSize = 100;
		TwitterList theList = null;
		for (int i = 0; i < screenNames.size(); i += batchSize) {
			final String screenNamesStr = InternalUtils.join(screenNames, i, i + batchSize);
			listVars.put("screen_name", screenNamesStr);
			theList = postTwitterList(url, listVars);
		}
		return theList;
	}

	private List<TwitterList> getTwitterLists(final String url, final Map<String, String> vars) {
		final String listsJson = http.getPage(url, vars, http.canAuthenticate());
		try {
			return TwitterList.getLists(listsJson);
		} catch (final JSONException e) {
			throw new TwitterException.Parsing("Could not parse response", e);
		}
	}

	private Map<String, String> getListVars(final Long listId) {
		final Map<String, String> vars = InternalUtils.asMap("list_id", listId);
		return vars;
	}
	
	private Map<String, String> getListVars(final String listSlug, final String ownerScreenName, final Long ownerId) {
		if (listSlug == null || (ownerScreenName == null && ownerId == null)) {
			throw new IllegalArgumentException("both listSlug and one of ownerScreenName or ownerId need to be provided");
		}
		if (ownerScreenName != null && ownerId != null)
			throw new IllegalArgumentException("only one of ownerScreenName or ownerId can be provided");
		final Map<String, String> vars = InternalUtils.asMap("slug", listSlug);
		if (ownerScreenName != null) {
			vars.put("owner_screen_name", ownerScreenName);
		} else if (ownerId != null) {
			vars.put("owner_id", Long.toString(ownerId.longValue()));
		}
		return vars;
	}

	private TwitterList getTwitterList(final String url, final Map<String, String> listVars) {
		final String json = http.getPage(url, listVars, http.canAuthenticate());
		return toTwitterList(json);
	}

	private TwitterList postTwitterList(final String url, final Map<String, String> listVars) {
		final String json = http.post(url, listVars, true);
		return toTwitterList(json);
	}

	private TwitterList toTwitterList(final String json) {
		try {
			final JSONObject jobj = new JSONObject(json);
			return new TwitterList(jobj);
		} catch (final JSONException e) {
			throw new TwitterException.Parsing("Could not parse response", e);
		}
	}
	
	/** test if the twitter api we're talking to is the new v1.1 API; some endpoints are changed in that new api */
	private static boolean isAPI11(Twitter jtwit) {
		return jtwit.TWITTER_URL.endsWith("1.1");
	}
}
