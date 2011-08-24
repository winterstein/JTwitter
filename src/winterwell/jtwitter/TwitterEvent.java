package winterwell.jtwitter;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.Twitter.User;

public class TwitterEvent {

	public static interface Type {
		public static final String FOLLOW = "follow";
		public static final String FAVORITE = "favorite";
		public static final String UNFAVORITE = "unfavorite";
		public static final String LIST_CREATED = "list_created";
		/**
		 * Indicates changes to the user's profile -- eg. their picture or location.
		 */
		public static final String USER_UPDATE = "user_update";		
		public static final String ADDED_TO_LIST = "list_member_added";
		public static final String REMOVED_FROM_LIST = "list_member_removed";
	}
	
	/**
	 * The user who was affected, or who owns the affected object.
	 */
	public final User target;
	/**
	 * The user who initiated the event
	 */
	public final User source;
	/**
	 * What type of event this is. Known values:
	 * <ul>
	 * <li> follow
	 * <li> favorite, unfavorite
	 * <li> user_update: Changes to the user's profile
	 * <li> list_created
	 * <li> list_member_added, list_member_removed
	 * </ul>
	 * See the {@link Type} constants for known definitions. 
	 */
	public final String type;
	public final Date createdAt;
	private Object targetObject;
	
	public Date getCreatedAt() {
		return createdAt;
	}
	
	/**
	 * The user who was affected, or who owns the affected object.
	 */
	public User getTarget() {
		return target;
	}
	
	/**
	 * The user who initiated the event
	 */
	public User getSource() {
		return source;
	}
	
	public String getType() {
		return type;
	}
	
	public TwitterEvent(JSONObject jo, Twitter jtwit) throws JSONException {
		type = jo.getString("event");
		target = new User(jo.getJSONObject("target"), null);
		source = new User(jo.getJSONObject("source"), null);
		createdAt = Twitter.parseDate(jo.getString("created_at"));
		// TODO how can we tell what this is??
		JSONObject to = jo.optJSONObject("target_object");
		if (to==null) {
			return;
		}
		if (to.has("member_count")) {
			targetObject = new TwitterList(to, jtwit);
		} else {
			targetObject = to;
		}
	}

	
	/** The affected object, if not a user. 
	 * E.g. For a favorite event, 
	 * target=the owner of the favorited tweet, 
	 * target object=the actual favorited tweet.
	 * Can be null.
	 */
	public Object getTargetObject() {
		return targetObject;
	}

	@Override
	public String toString() {
		return source+" "+type+" "+target+" "+getTargetObject();
	}

	/**
	 * Convenience method for filtering events. E.g. given a
	 * <code>TwitterEvent event</code> use
	 * <code>event.is(TwitterEvent.Type.FOLLOW)</code> to pick
	 * out follow events.
	 * 
	 * @param type
	 * @return true if this is an event of the given type.
	 */
	public boolean is(String type) {
		return this.type.equals(type);
	}
}