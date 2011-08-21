package winterwell.jtwitter;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.Twitter.User;

public class TwitterEvent {

	
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
		System.out.println(type);
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
}
