package winterwell.jtwitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import winterwell.json.JSONArray;
import winterwell.json.JSONException;
import winterwell.json.JSONObject;

//TODO: update this description
/**
 * A Twitter list, which uses lazy-fetching of its members.
 * <p>
 * The methods of this object will call Twitter when they need to, and store the
 * results. E.g. the first call to {@link #size()} might require a call to
 * Twitter, but subsequent calls will not.
 * <p>
 * WARNING: Twitter only returns list members in batches of 20. So reading a
 * large list can be slow and use quite a few calls to Twitter.
 * <p>
 * To find out what lists you or another user has, see
 * {@link Twitter#getLists()} and {@link Twitter#getLists(String)}.<br>
 * To find out what lists you or another user are *in*, see
 * {@link Twitter#getListsContainingMe()} and
 * {@link Twitter#getListsContaining(String, boolean)}.
 * 
 * @see Twitter
 * @author daniel
 * 
 */
public class TwitterList {

	static List<TwitterList> getLists(final String listsJson) throws JSONException {
		final JSONArray jarr;
		if (listsJson.charAt(0) == '{') {
			final JSONObject wrapper = new JSONObject(listsJson);
			jarr = wrapper.getJSONArray("lists");
		} else
			jarr = new JSONArray(listsJson);
		if (jarr.length() == 0)
			return Collections.emptyList();
		final List<TwitterList> lists = new ArrayList<TwitterList>(jarr.length());
		for (int i = 0; i < jarr.length(); i++) {
			final JSONObject li = jarr.getJSONObject(i);
			final TwitterList twList = new TwitterList(li);
			lists.add(twList);
		}
		return lists;
	}

	private final boolean _private;

	private final String description;

	private final long id;

	private final int memberCount;

	private final String name;

	/**
	 * never null (but may be a dummy object)
	 */
	private final User owner;

	private final String slug;

	private final int subscriberCount;

	TwitterList(final JSONObject jobj) throws JSONException {
		this.memberCount = jobj.getInt("member_count");
		this.subscriberCount = jobj.getInt("subscriber_count");
		this.name = jobj.getString("name");
		this.slug = jobj.getString("slug");
		this.id = jobj.getLong("id");
		this._private = "private".equals(jobj.optString("mode"));
		this.description = jobj.optString("description");
		final JSONObject user = jobj.getJSONObject("user");
		this.owner = new User(user, null);
	}

	public long getId() {
		return id;
	}

	public int getMemberCount() {
		return memberCount;
	}

	public String getSlug() {
		return slug;
	}

	public String getDescription() {
		return description;
	}

	public String getName() {
		return name;
	}

	public User getOwner() {
		return owner;
	}

	public int getSubscriberCount() {
		return subscriberCount;
	}

	public boolean isPrivate() {
		return _private;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + owner + "." + name + "]";
	}

}

