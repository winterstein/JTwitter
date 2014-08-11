package winterwell.jtwitter.ecosystem;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import winterwell.jtwitter.InternalUtils;
import winterwell.jtwitter.Status;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.TwitterException;
import winterwell.jtwitter.URLConnectionHttpClient;

public class TwitLonger {
	
	IHttpClient http;

	private Twitter jtwit;
	
	/**
	 * Read only access for lengthening tweets
	 */
	public TwitLonger() {
		http = new URLConnectionHttpClient();
	}
	
	/**
	 * Read/write access
	 * @param jtwitter
	 * @param twitlongerApiKey
	 * @param twitlongerAppName
	 */
	public TwitLonger(Twitter jtwitter, String twitlongerApiKey, String twitlongerAppName) {
		this.twitlongerApiKey = twitlongerApiKey;
		this.twitlongerAppName = twitlongerAppName;		
		http = jtwitter.getHttpClient();
		this.jtwit = jtwitter;
		if (twitlongerApiKey == null || twitlongerAppName == null) {
			throw new IllegalStateException("Incomplete Twitlonger api details");
		}

	}

	private String twitlongerApiKey;

	private String twitlongerAppName;

	/**
	 * Use twitlonger.com to post a lengthy tweet. See twitlonger.com for more
	 * details on their service.
	 * 
	 * @param message Must be &gt; 140 chars
	 * @param inReplyToStatusId
	 *            Can be null if this isn't a reply
	 * @return A Twitter status using a truncated message with a link to
	 *         twitlonger.com
	 */
	public Status updateLongStatus(String message, Number inReplyToStatusId) {
		assert twitlongerApiKey != null : "Wrong constructor used -- you must supply an api-key to post";
		if (message.length() < 141) {
			throw new IllegalArgumentException("Message too short ("
					+ inReplyToStatusId
					+ " chars). Just post a normal Twitter status. ");
		}
		String url = "http://www.twitlonger.com/api_post";
		Map<String, String> vars = InternalUtils.asMap("application",
				twitlongerAppName, "api_key", twitlongerApiKey, 
				"username", jtwit.getScreenName(), 
				"message", message);
		if (inReplyToStatusId != null && inReplyToStatusId.doubleValue() != 0) {
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
		Status s = jtwit.updateStatus(shortMsg, inReplyToStatusId);

		m = idTag.matcher(response);
		ok = m.find();
		if ( ! ok) {
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

	

	
	static final Pattern contentTag = Pattern.compile(
			"<content>(.+?)<\\/content>", Pattern.DOTALL);
	
	static final Pattern idTag = Pattern.compile("<id>(.+?)<\\/id>",
			Pattern.DOTALL);


	/**
	 * @param truncatedStatus
	 *            If this is a twitlonger.com truncated status, then call
	 *            twitlonger to fetch the full text.
	 * @return the full status message. If this is not a twitlonger status, this
	 *         will just return the status text as-is.
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
		if (!ok)
			throw new TwitterException.TwitLongerException(
					"TwitLonger call failed", response);
		String longMsg = m.group(1).trim();
		return longMsg;
	}

}
