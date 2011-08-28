package winterwell.jtwitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.Twitter.User;
import winterwell.utils.reporting.Log;

/**
 * Internal base class for UserStream and TwitterStream.
 * <p>
 * Warning from Twitter: Consuming applications must tolerate duplicate statuses, 
 *  out-of-order statuses and non-status messages.
 * 
 * @author daniel
 */
public abstract class AStream {

	/**
	 * Forget the past. Clears all current queues of tweets, etc.
	 */
	public void clear() {
		outages.clear();
		popEvents();
		popSystemEvents();
		popTweets();
	}
	
	/**
	 * 10 minutes
	 */
	private static final int MAX_WAIT_SECONDS = 600;

	@Override
	protected void finalize() throws Throwable {
		// TODO scream blue murder if this is actually needed
		close();
	}

	/**
	 * @return the recent events. Calling this will clear the list of events.
	 */
	public List<TwitterEvent> popEvents() {
		List evs = getEvents();
		events = new ArrayList();
		return evs;
	}
	
	/**
	 * @return the recent system events, such as "delete this status". 
	 * Calling this will clear the list of system events.
	 */
	public List<TwitterEvent> popSystemEvents() {		
		List evs = getSystemEvents();
		sysEvents = new ArrayList();
		return evs;
	}
	
	
	/**
	 * @return the recent system events, such as "delete this status". */
	public List<Object> getSystemEvents() {
		read();
		return sysEvents;
	}

	/**
	 * @return the recent events. Calling this will clear the list of tweets.
	 */
	public List<ITweet> popTweets() {
		List<ITweet> ts = getTweets();
		tweets = new ArrayList();
		return ts;
	}
	
	public List<TwitterEvent> getEvents() {
		read();
		return events;
	}
	
	public List<ITweet> getTweets() {
		read();
//		// re-order?? Or do we not care TODO test this is the right way round
//		Collections.sort(tweets, new Comparator<ITweet>() {
//			@Override
//			public int compare(ITweet t1, ITweet t2) {
//				return t1.getCreatedAt().compareTo(t2.getCreatedAt());
//			}
//		});
		return tweets;
	}

	List<Long> friends;

	private int forgotten;

	private boolean autoReconnect;
	
	/**
	 * @return the number of messages (which could be tweets, events, or
	 * system events) which the stream has dropped to stay within it's
	 * (very generous) bounds.
	 * <p>
	 * Best practice is to NOT rely on this for memory management.
	 * You should call {@link #popEvents()}, {@link #popSystemEvents()}
	 * and {@link #popTweets()} regularly to clear the buffers.
	 */
	public int getForgotten() {
		return forgotten;
	}
	
	/**
	 * 
	 * @param yes If true, attempt to connect if disconnected. true by default.
	 */
	public void setAutoReconnect(boolean yes) {
		autoReconnect = yes;
	}
	
	void read() {		
		String[] jsons = readThread.popJsons();		
		for (String json : jsons) {				
			try {
				read2(json);
			} catch (JSONException e) {
				throw new TwitterException.Parsing(json, e);
			}
		}		
		if (readThread.isAlive() && readThread.ex == null) return;
		// close all
		URLConnectionHttpClient.close(stream);
		if (readThread.isAlive()) {
			readThread.pleaseStop();
			readThread.interrupt();
		}
		// The connection is down!		
		if ( ! autoReconnect) {
			throw new TwitterException(readThread.ex);
		}		
		// reconnect
		reconnect();
		// store the outage
		outages.add(new Outage(lastId, System.currentTimeMillis()));
		// paranoia: avoid memory leaks
		if (outages.size() > 100000) {
			for(int i=0; i<1000; i++) {
				outages.remove(0);
			}
			// add an arbitrary number to the forgotten count: 10 per outage
			forgotten += 10000;
		}
	}
	
	final List<Outage> outages = new ArrayList();

	private BigInteger lastId = BigInteger.ZERO;

	boolean fillInFollows = true;
	
	public List<Outage> getOutages() {
		return outages;
	}
	
	/**
	 * Use the REST API to fill in when possible. 
	 * <p>
	 * In accordance with best-practice, this method will skip over 
	 * very recent outages (which will be picked up by subsequent calls 
	 * to {@link #fillInOutages()}).</p> 
	 * <p>From <i>dev.twitter.com</i>:<br>
	 * Do not resume REST API polling immediately after a stream failure. 
		Wait at least a minute or two after the initial failure before you begin REST API polling. 
		This delay is crucial to prevent dog-piling the REST API in the event of a minor hiccup on 
		the streaming API.
		 *</p> 
	 */
	public final void fillInOutages() throws UnsupportedOperationException {
		if (outages.isEmpty()) return;
		Outage[] outs = outages.toArray(new Outage[0]);
		// protect our original object from edits
		User self = jtwit.getSelf();
		Twitter jtwit2 = new Twitter(self.getScreenName(), jtwit.getHttpClient());
		for (Outage outage : outs) {
			// too recent? wait at least 1 minute
			if (System.currentTimeMillis() - outage.untilTime < 60000) {
				continue;
			}
			jtwit2.setSinceId(outage.sinceId);
			jtwit2.setUntilDate(new Date(outage.untilTime));
			jtwit2.setMaxResults(100000); // hopefully not needed!
			// fetch
			fillInOutages2(jtwit, outage);
			// success			
			outages.remove(outage);
		}
	}
	
	/**
	 * 
	 * @param jtwit2 with screenname, auth-token, sinceId and untilDate all set up
	 * @param outage 
	 */
	abstract void fillInOutages2(Twitter jtwit2, Outage outage);

	public static final class Outage implements Serializable {
		private static final long serialVersionUID = 1L;
		final BigInteger sinceId;
		final long untilTime;
		public Outage(BigInteger sinceId, long untilTime) {
			super();
			this.sinceId = sinceId;
			this.untilTime = untilTime;
		}		
	}
	
	public void reconnect() {		
		// Try again as advised by dev.twitter.com:
		// 1. straightaway		
		try {
			connect();
			return;
		} catch (TwitterException.E40X e) {
			throw e;
		} catch (Exception e) {
			// oh well
			Log.report(""+e);
		}
		// 2. Exponential back-off. Wait a random number of seconds between 20 and 40 seconds. 
		// Double this value on each subsequent connection failure. 
		// Limit this value to the maximum of a random number of seconds between 240 and 300 seconds.
		int wait = 20 + new Random().nextInt(40);
		int waited = 0;
		while(waited < MAX_WAIT_SECONDS) {
			try {
				Thread.sleep(wait*1000);
				waited += wait;				
				if (wait<300) wait = wait*2;
				connect();	
				// success :)
				return;
			} catch (TwitterException.E40X e) {
				throw e;
			} catch (Exception e) {
				// oh well
				Log.report(""+e);
			}			
		}
		throw new TwitterException.E50X("Could not connect to streaming server");      
	}
	
	public boolean isConnected() {
		return readThread != null && readThread.isAlive() && readThread.ex==null;
	}
	
	private void read2(String json) throws JSONException {
		JSONObject jo = new JSONObject(json);
		// the 1st object for a user stream is a list of friend ids
		JSONArray _friends = jo.optJSONArray("friends");
		if (_friends != null) {
			read3_friends(_friends);
			return;
		}
		
		// tweets
		// TODO DMs?? They don't seem to get sent!
		//System.out.println(jo);
		if (jo.has("text")) {
			Status tweet = new Twitter.Status(jo, null);
			// de-duplicate a bit locally (this is rare -- perhaps don't bother??)
			if (tweets.contains(tweet)) {
				return;
			}
			tweets.add(tweet);
			// track the last id for tracking outages
			if (tweet.id.compareTo(lastId) > 0) {
				lastId = tweet.id;
			}
			forgotten += forgetIfFull(tweets);
			return;
		}
		
		// Events
		String eventType = jo.optString("event");
		if (eventType != "") {
			TwitterEvent event = new TwitterEvent(jo, jtwit);
			events.add(event);
			forgotten += forgetIfFull(events);
			return;
		}
		// Deletes and other system events, like limits
		JSONObject del = jo.optJSONObject("delete");
		if (del!=null) {
			JSONObject s = del.getJSONObject("status");
			BigInteger id = new BigInteger(s.getString("id_str"));
			long userId = s.getLong("user_id");
			Status deadTweet = new Status(null, null, id, null);
			// prune local (which is unlikely to do much)
			boolean pruned = tweets.remove(deadTweet);
			if ( ! pruned) {
				sysEvents.add(new Object[]{"delete", deadTweet, userId});
				forgotten += forgetIfFull(sysEvents);
			}
			return;
		}
		// 	e.g.	{"limit":{"track":1234}}
		JSONObject limit = jo.optJSONObject("limit");
		if (limit!=null) {
			int cnt = limit.optInt("track");
			if (cnt==0) {	
				System.out.println(jo); // API change :( - a new limit object		
			}
			sysEvents.add(new Object[]{"limit", cnt});
			forgotten += cnt;
			return;
		}
		// ??
		System.out.println(jo);
	}

	private void read3_friends(JSONArray _friends) throws JSONException {
		List<Long> oldFriends = friends;
		friends = new ArrayList(_friends.length());
		for (int i = 0, n = _friends.length(); i < n; i++) {
			long fi = _friends.getLong(i);
			friends.add(fi);
		}
		if (oldFriends==null || ! fillInFollows) return;
		
		// This is after a reconnect -- did we miss any follow events?				
		HashSet<Long> friends2 = new HashSet(friends);
		friends2.removeAll(oldFriends);
		if (friends2.isEmpty()) return;
		Twitter_Users tu = new Twitter_Users(jtwit);
		List<User> newFriends = tu.showById(friends2);
		User you = jtwit.getSelf();
		for (User nf : newFriends) {					
			TwitterEvent e = new TwitterEvent(new Date(), 
					you, TwitterEvent.Type.FOLLOW, nf, null);
			events.add(e);					
		}
		forgotten += forgetIfFull(events);		
	}

	static int forgetIfFull(List incoming) {
		// forget a batch?
		if (incoming.size() < StreamGobbler.MAX_BUFFER) return 0;
		int chop = StreamGobbler.MAX_BUFFER / 10;
		for(int i=0; i<chop; i++) {
			incoming.remove(0);
		}		
		return chop;
	}

	/**
	 * How many messages prior-to-connecting to retrieve.
	 * Twitter bug: Currently this does not work!
	 * 
	 * @param previousCount
	 *            Up to 150,000 but subject to change.
	 * 
	 *            Negative values are allowed -- they mean the stream will
	 *            terminate when it reaches the end of the historical messages.
	 *            
	 * @deprecated Twitter need to fix this :(
	 */
	public void setPreviousCount(int previousCount) {
		this.previousCount = previousCount;
	}

	
	public AStream(Twitter jtwit) {
		this.client = jtwit.getHttpClient();
		this.jtwit = jtwit;
		// Twitter send 30 second keep-alive pulses, but ask that
		// you wait 3 cycles before disconnecting
		client.setTimeout(91 * 1000);
		// this.user = jtwit.getScreenName();
		// if (user==null) {
		//
		// }
	}

	public void connect() {
		close();
		try {
			HttpURLConnection con = connect2();
			stream = con.getInputStream();
			readThread = new StreamGobbler(stream);
			readThread.setName("Gobble:"+toString());
			readThread.start();
			// check the connection took
			Thread.sleep(10);
			if ( ! isConnected()) {
				throw new TwitterException(readThread.ex);
			}
		} catch (TwitterException e) {
			throw e;
		} catch (Exception e) {
			throw new TwitterException(e);
		}
	}

	abstract HttpURLConnection connect2() throws Exception;

	public void close() {
		if (readThread != null) {
			readThread.pleaseStop();
		}
		URLConnectionHttpClient.close(stream);
	}


	final IHttpClient client;

	List<TwitterEvent> events = new ArrayList();
	List<ITweet> tweets = new ArrayList();
	List<Object> sysEvents = new ArrayList();

	/**
	 * Needed for constructing some objects.
	 */
	final Twitter jtwit;

	int previousCount;

	StreamGobbler readThread;

	private InputStream stream;


}



/**
 * Gobble output from a twitter stream. Create then call start().
 * Expects length delimiters
 * This does not process the json it receives.
 * 
 */
final class StreamGobbler extends Thread {
	long offTime;
	private final InputStream is;
	private volatile boolean stopFlag;
	IOException ex;

	/**
	 * start dropping messages after this.
	 */
	static final int MAX_BUFFER = 1000000;
	
	public StreamGobbler(InputStream is) {
//		super("gobbler");
		setDaemon(true);
		this.is = is;
	}

	/**
	 * Use synchronised blocks when editing this
	 */
	final List<String> jsons = new ArrayList();
	
	/**
	 * count of the number of tweets this gobbler had to drop
	 * due to buffer size
	 */
	int forgotten;
	
	/**
	 * Read off the collected json snippets for processing
	 * @return
	 */
	public String[] popJsons() {
		synchronized (jsons) {
			String[] arr = jsons.toArray(new String[jsons.size()]);
			jsons.clear();
			return arr;
		}		
	}
	
	@Override
	public String toString() {
		return getName()+"["+jsons.size()+"]";
	}

	/**
	 * Request that the thread should finish. If the thread is hung waiting for
	 * output, then this will not work.
	 */
	public void pleaseStop() {
		URLConnectionHttpClient.close(is);
		stopFlag = true;
	}

	@Override
	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			while ( ! stopFlag) {
				int len = readLength(br);
				readJson(br, len);
			}
		} catch (IOException ioe) {
			if (stopFlag) {
				// we were told to stop already so ignore
				return;
			}
			System.out.println(ioe); //.printStackTrace();
			ex = ioe;
			offTime = System.currentTimeMillis();
		}
	}

	private void readJson(BufferedReader br, int len) throws IOException {
		assert len > 0;
		char[] sb = new char[len];
		int cnt = 0;
		while(len>0) {
			int rd = br.read(sb, cnt, len);		
			if (rd == -1) {
				throw new IOException("end of stream");
//				continue;
			}
			cnt += rd;
			len -= rd;
		}		
		synchronized (jsons) {
			jsons.add(new String(sb));
			// forget a batch?
			forgotten += AStream.forgetIfFull(jsons);
		}		
	}

	private int readLength(BufferedReader br) throws IOException {
		StringBuilder numSb = new StringBuilder();		
		while(true) {
			int ich = br.read();		
			if (ich == -1) {
				throw new IOException("end of stream "+this);
//				continue;
			}
			char ch = (char) ich;
			if (ch=='\n' || ch=='\r') {
				// ignore leading whitespace, stop otherwise
				if (numSb.length()==0) continue;
				break;
			}
			assert Character.isDigit(ch) : ch;
			numSb.append(ch);
		}
		return Integer.valueOf(numSb.toString());
	}
}
