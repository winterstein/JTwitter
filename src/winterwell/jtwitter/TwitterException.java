package winterwell.jtwitter;

import java.io.IOException;
import java.text.ParseException;

import org.json.JSONException;

import winterwell.jtwitter.TwitterAccount.KAccessLevel;




/**
 * A runtime exception for when Twitter requests don't work. All
 * {@link Twitter} methods can throw this.
 * <p>
 * This contains several subclasses which should be thrown to mark
 * different problems. Error handling is particularly important as
 * Twitter tends to be a bit flaky.
 * <p>
 * I believe unchecked exceptions are preferable to checked ones,
 * because they avoid the problems caused by swallowing exceptions.
 * But if you don't like runtime exceptions, just edit this class.
 * 
 * @author Daniel Winterstein
 */
public class TwitterException extends RuntimeException {
	
	/**
	 * Problems reading the JSON returned by Twitter. 
	 * This should not normally occur!
	 * This indicates either a change in the API, or a bug in JTwitter. 
	 */
	public static class Parsing extends TwitterException {
		private static final long serialVersionUID = 1L;
		Parsing(String json, JSONException e) {
			super(clip(json, 280), e);
		}
		public Parsing(String date, ParseException e) {
			super(date, e);
		}
		/**
		 * Convenience to shorten a potentially long string.
		 */
		private static String clip(String json, int len) {
			return json==null? null : 
				json.length()<=len? json : json.substring(len)+"...";
		}		
	}
	
	/**
	 * Thrown if you poll too frequently.
	 */
	public static class TooRecent extends E403 {
		TooRecent(String msg) {
			super(msg);
		}
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Exception thrown when Twitter doesn't like a parameter. E.g.
	 * if you set a since_id which goes back too far, you'll see this. 
	 * <p>
	 * This extends E403 because Twitter uses http code 403 (forbidden)
	 * to signal this.
	 */
	public static class BadParameter extends E403 {
		private static final long serialVersionUID = 1L;
		public BadParameter(String msg) {
			super(msg);
		}
	}

	/**
	 * Exception thrown when trying to query a suspended account.
	 * Note that *deleted* accounts may generate an E404 instead. 
	 * <p>
	 * This extends E403 because Twitter uses http code 403 (forbidden)
	 * to signal this.
	 */
	public static class SuspendedUser extends E403 {
		private static final long serialVersionUID = 1L;
		SuspendedUser(String msg) {
			super(msg);
		}
	}
	
	/**
	 * Subclass of 403 thrown when you try to do something twice, like
	 * post the same status.
	 * This is only thrown for immediate repetition. You may get a plain 
	 * E403 instead for less blatant repetition.
	 */
	public static class Repetition extends E403 {
		private static final long serialVersionUID = 1L;
		public Repetition(String tweet) {
			super("Already tweeted! "+tweet);
		}
	}
	
	/**
	 * Subclass of 403 thrown when you follow too many people.
	 */
	public static class FollowerLimit extends E403 {
		private static final long serialVersionUID = 1L;
		public FollowerLimit(String msg) {
			super(msg);
		}
	}
	
	/**
	 * Subclass of 403 thrown when you breach the access level of the
	 * app / oauth-token.
	 * @see TwitterAccount#getAccessLevel()
	 */
	public static class AccessLevel extends E403 {
		private static final long serialVersionUID = 1L;
		public AccessLevel(String msg) {
			super(msg);
		}
	}

	/**
	 * Something has gone wrong. Occasionally Twitter behaves strangely.
	 */
	public static class Unexplained extends TwitterException {
		public Unexplained(String msg) {
			super(msg);
		}
		private static final long serialVersionUID = 1L;
	}
	
	/**
	 * A timeout exception - probably caused by Twitter being overloaded.
	 */
	public static class Timeout extends TwitterException {
		public Timeout(String string) {
			super(string);
		}
		private static final long serialVersionUID = 1L;
	}
	
	/**
	 * An IO exception, eg. a network issue.
	 * Call {@link #getCause()} to get the original IOException
	 */
	public static class IO extends TwitterException {
		public IO(IOException e) {
			super(e);
		}
		@Override
		public IOException getCause() {
			return (IOException) super.getCause();
		}
		private static final long serialVersionUID = 1L;
	}
	
	/**
	 * A code 50X error (e.g. 502) - indicating something went wrong at 
	 * Twitter's end. The API equivalent of the Fail Whale. 
	 * Usually retrying in a minute will fix this.
	 * <p>
	 * Note: some socket exceptions are assumed to be server errors - because 
	 * they probably are - but could conceivably be caused by an error 
	 * in your internet connection.
	 */
	public static class E50X extends TwitterException {
		public E50X(String string) {
			// sometimes Twitter sends a full web page by mistake
			super(string.length() > 1000? string.substring(0, 1000)+"..." : string);
		}
		private static final long serialVersionUID = 1L;
	}
	/**
	 * A Forbidden exception. This is thrown if the authenticating used does not have
	 * the right to make a request.
	 * Possible causes: 
	 *  - Accessing a suspended account (ie. trying to look at messages from a spambot)
	 *  - Accessing a protected stream
	 *  - Repeatedly posting the same status
	 *  - If search is passed a sinceId which is too old. Though the 
	 *  API documentation suggests a 404 should be thrown instead.
	 */
	public static class E403 extends TwitterException {
		public E403(String string) {
			super(string);
		}
		private static final long serialVersionUID = 1L;
	}
	/**
	 * An unauthorised exception. This is thrown (eg) if a password is wrong
	 * or a login is required.
	 */
	public static class E401 extends TwitterException {
		public E401(String string) {
			super(string);
		}
		private static final long serialVersionUID = 1L;
	}

	public static class UpdateToOAuth extends E401 {
		private static final long serialVersionUID = 1L;
		public UpdateToOAuth() {
			super("You need to switch to OAuth. Twitter no longer support basic authentication.");
		}
	}

	private static final long serialVersionUID = 1L;
	
	private String additionalInfo = "";
	
	/**
	 * Wrap an exception as a TwitterException.
	 */
	TwitterException(Exception e) {
		super(e);
		// avoid gratuitous nesting of exceptions
		assert !(e instanceof TwitterException) : e;
	}
	
	TwitterException(String msg, Exception e) {
		super(msg, e);
		// avoid gratuitous nesting of exceptions
		assert !(e instanceof TwitterException) : e;
	}

	/**
	 * @param string
	 */
	public TwitterException(String string) {
		super(string);
	}
	
	public TwitterException(String string, String additionalInfo) {
		this(string);
		this.setAdditionalInfo(additionalInfo);
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	/**
	 * Indicates a 404: resource does not exist error from Twitter.
	 * Note: *Used* to be thrown in relation to suspended users (e.g. spambots)
	 * These now get a 403, as of August 2010.
	 */
	public static class E404 extends TwitterException {
		public E404(String string) {
			super(string);
		}
		private static final long serialVersionUID = 1L;		
	}
	/**
	 * Indicates a rate limit error (i.e. you've over-used Twitter)
	 */
	public static class RateLimit extends TwitterException {
		public RateLimit(String string) {
			super(string);
		}
		private static final long serialVersionUID = 1L;		
	}
	
	/**
	 * Exception thrown if something goes wrong with twilonger.com 
	 * integration for long tweets.
	 */
	public static class TwitLongerException extends TwitterException {
		public TwitLongerException(String string, String details) {
			super(string, details);
		}
		private static final long serialVersionUID = 1L;		
	}
}
