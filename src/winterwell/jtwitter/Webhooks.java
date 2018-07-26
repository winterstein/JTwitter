package winterwell.jtwitter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.winterwell.json.JSONArray;
import com.winterwell.json.JSONObject;

/**
 * Utility classes for managing Webhooks and Account Activity Subscriptions
 * 
 * @author roscoe
 *
 */
public class Webhooks {
	public static class WebhookList {
		public String environment_name;
		public List<Webhook> webhooks;

		public WebhookList(JSONObject base) {
			environment_name = base.getString("environment_name");
			webhooks = new ArrayList<Webhook>();
			JSONArray baseWebhooks = base.getJSONArray("webhooks");
			for (Object baseWebhook : baseWebhooks) {
				if (baseWebhook instanceof JSONObject) {
					webhooks.add(new Webhook((JSONObject) baseWebhook));
				}
			}
		}
	}

	public static class Webhook {
		public BigInteger id;
		public String url;
		public Boolean valid;
		public Date created_timestamp;

		public Webhook(JSONObject base) {
			id = new BigInteger(base.getString("id"));
			url = base.getString("url");
			valid = base.getBoolean("valid");
			created_timestamp = InternalUtils.parseDate(base.getString("created_timestamp"));
		}
	}

	public static class SubscriptionList {
		public String environment;
		public BigInteger application_id;
		public List<Subscription> subscriptions;

		public SubscriptionList(JSONObject base) {
			environment = base.getString("environment");
			application_id = new BigInteger(base.getString("application_id"));
			subscriptions = new ArrayList<Subscription>();
			JSONArray baseSubscriptions = base.getJSONArray("subscriptions");
			for (Object baseSubscription : baseSubscriptions) {
				if (baseSubscription instanceof JSONObject) {
					subscriptions.add(new Subscription((JSONObject) baseSubscription));
				}
			}
		}
	}

	public static class Subscription {
		public BigInteger user_id;

		public Subscription(JSONObject base) {
			user_id = new BigInteger(base.getString("user_id"));
		}
	}

	public static class SubscriptionsCount {
		public String account_name;
		public Integer subscriptions_count;

		public SubscriptionsCount(JSONObject base) {
			account_name = base.getString("account_name");
			subscriptions_count = base.getInt("subscriptions_count");
		}
	}

	public static class WebhookEvent {
		public BigInteger for_user_id;
		public JSONArray tweet_create_events;
		public JSONArray tweet_delete_events;
		public JSONArray direct_message_events;
		public JSONArray favorite_events;
		public JSONArray follow_events;
		public JSONArray block_events;
		public JSONArray mute_events;
		public JSONArray user_event;
		public JSONArray direct_message_indicate_typing_events;
		public JSONArray direct_message_mark_read_events;

		public WebhookEvent(JSONObject base) {
			for_user_id = new BigInteger(base.getString("for_user_id"));
			tweet_create_events = base.optJSONArray("tweet_create_events");
			tweet_delete_events = base.optJSONArray("tweet_delete_events");
			direct_message_events = base.optJSONArray("direct_message_events");
			favorite_events = base.optJSONArray("favorite_events");
			follow_events = base.optJSONArray("follow_events");
			block_events = base.optJSONArray("block_events");
			mute_events = base.optJSONArray("mute_events");
			user_event = base.optJSONArray("user_event");
			direct_message_indicate_typing_events = base.optJSONArray("direct_message_indicate_typing_events");
			direct_message_mark_read_events = base.optJSONArray("direct_message_mark_read_events");
		}
	}
}
