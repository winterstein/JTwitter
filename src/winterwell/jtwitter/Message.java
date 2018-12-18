package winterwell.jtwitter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;

import com.winterwell.json.JSONArray;
import com.winterwell.json.JSONObject;

import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.Twitter.KEntityType;
import winterwell.jtwitter.Twitter.TweetEntity;

/**
 * A Twitter direct message. Fields are null if unset.
 * 
 * TODO are there more fields now? check the raw json
 */
public final class Message implements ITweet {

	private static final long serialVersionUID = 1L;

	
	@Override
	public String getDisplayText() {
		return Status.getDisplayText2(this);
	}
		
	public static List<Message> getMessages(JSONArray array) {
		List<Message> msgs = new ArrayList<Message>();
		for (Object element : array) {
			if (JSONObject.NULL.equals(element)) {
				continue;
			}
			JSONObject obj = (JSONObject) element;
			Message msg = new Message(obj);
			msgs.add(msg);
		}
		return msgs;
	}

	private final Date createdAt;
	private EnumMap<KEntityType, List<TweetEntity>> entities;

	public final Number id;

	/**
	 * Equivalent to {@link Status#inReplyToStatusId} *but null by default*. If
	 * you want to use this, you must set it yourself. The field is just a
	 * convenient storage place. Strangely Twitter don't report the previous ID
	 * for messages.
	 */
	public Number inReplyToMessageId;

	private String location;

	private Place place;
	private User recipient;
	private User sender;
	private final BigInteger recipientId;
	private final BigInteger senderId;
	public final String text;

	/**
	 * @deprecated Used for deleted messages only
	 * @param id
	 */
	Message(User dummyUser, Number id) {
		this.sender = dummyUser;
		this.id = id;
		this.recipient = null;
		this.createdAt = null;
		this.text = null;
		this.senderId = BigInteger.valueOf(dummyUser.id);
		this.recipientId = null;
		
	}
	
	/**
	 * @param obj
	 * @throws JSONException
	 * @throws TwitterException
	 */
	/*
	Message(JSONObject obj) throws JSONException, TwitterException {
		String _id = obj.getString("id_str");
		id = new BigInteger(_id==null? ""+obj.get("id") : _id);
		String _text = obj.getString("text");
		text = InternalUtils.unencode(_text);
		String c = InternalUtils.jsonGet("created_at", obj);
		createdAt = InternalUtils.parseDate(c);
		
		// Requests to direct_messages/show.json put the originator in "user" instead of "sender"
		JSONObject senderJSON = obj.optJSONObject("sender");
		if(senderJSON == null) {
			senderJSON = obj.getJSONObject("user");
		}
		sender = new User(senderJSON, null);
		
		// recipient - for messages you sent
		Object recip = obj.opt("recipient");
		if (recip instanceof JSONObject) { // Note JSONObject.has is dangerously
											// misleading
			recipient = new User((JSONObject) recip, null);
		} else {
			recipient = null;
		}
		JSONObject jsonEntities = obj.optJSONObject("entities");
		if (jsonEntities != null) {
			// Note: Twitter filters out dud @names
			entities = new EnumMap<Twitter.KEntityType, List<TweetEntity>>(KEntityType.class);
			for (KEntityType type : KEntityType.values()) {
				List<TweetEntity> es = TweetEntity.parse(this, _text, type,	jsonEntities);
				entities.put(type, es);
			}
		}
		// geo-location?
		Object _locn = Status.jsonGetLocn(obj);
		location = _locn == null ? null : _locn.toString();
		if (_locn instanceof Place) {
			place = (Place) _locn;
		}
	}
	*/
	
	/**
	 * Parse a Message from the format we get from webhooks and GET direct_messages/etc
	 * @param event The message object
	 */
	public Message(JSONObject event) {
		String _id = event.getString("id");
		id = new BigInteger(_id);
		String created_timestamp = event.getString("created_timestamp");
		createdAt = new Date();
		createdAt.setTime(Long.parseLong(created_timestamp));
		
		JSONObject messageCreate = event.getJSONObject("message_create");
		senderId = new BigInteger(messageCreate.getString("sender_id"));
		JSONObject target = messageCreate.getJSONObject("target");
		recipientId = new BigInteger(target.getString("recipient_id"));
				
		JSONObject messageData = messageCreate.getJSONObject("message_data");
		text = messageData.getString("text");
		JSONObject jsonEntities = event.optJSONObject("entities");
		if (jsonEntities != null) {
			// Note: Twitter filters out dud @names
			entities = new EnumMap<Twitter.KEntityType, List<TweetEntity>>(KEntityType.class);
			for (KEntityType type : KEntityType.values()) {
				List<TweetEntity> es = TweetEntity.parse(this, text, type,	jsonEntities);
				entities.put(type, es);
			}
		}
		// TODO Parse attachment field??
	}
	
	/**
	 * Set up sender/recipient full objects
	 * @param users Map from numeric user IDs to user JSON (delivered in webhooks update)
	 */
	public void setPeople(JSONObject users) {
		sender = new User(users.getJSONObject(senderId.toString()), null);
		recipient = new User(users.getJSONObject(recipientId.toString()), null);
	}
	
	/**
	 * Set up sender/recipient full objects
	 * @param sender
	 * @param recipient
	 */
	public void setPeople(User sender, User recipient) {
		this.sender = sender;
		this.recipient = recipient;
	}
	

	/**
	 * Tests by class=Message and tweet id number
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
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
		if (id instanceof Long) {
			return BigInteger.valueOf(id.longValue());
		}
		return (BigInteger) id;
	}

	@Override
	public String getLocation() {
		return location;
	}

	@Override
	public List<String> getMentions() {
		return Collections.singletonList(recipient.screenName);
	}

	@Override
	public Place getPlace() {
		return place;
	}

	/**
	 * @return the recipient (for messages sent by the authenticating user)
	 */
	public User getRecipient() {
		return recipient;
	}

	public User getSender() {
		return sender;
	}
	
	public Number getSenderId() {
		return senderId;
	}

	public Number getRecipientId() {
		return recipientId;
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public List<TweetEntity> getTweetEntities(KEntityType type) {
		return entities == null ? null : entities.get(type);
	}

	/**
	 * This is equivalent to {@link #getSender()}
	 */
	@Override
	public User getUser() {
		return getSender();
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return text;
	}

}