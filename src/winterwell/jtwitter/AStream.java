package winterwell.jtwitter;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import winterwell.json.JSONArray;
import winterwell.json.JSONException;
import winterwell.json.JSONObject;
import winterwell.jtwitter.AStream.IListen;
import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.ITweet;

/**
 * Internal base class for UserStream and TwitterStream.
 * <p>
 * Warning from Twitter: Consuming applications must tolerate duplicate
 * statuses, out-of-order statuses (upto 3 seconds of scrambling) and non-status
 * messages.
 * <p>
 * <h3>Threading</h3>
 * Streams create a gobbler thread which consumes the output from Twitter. They
 * are then accessed on a polling basis from a second thread. You can also
 * register a listener for push notifications in the gobbler thread. They are
 * thread-safe for this usage -- but not thread-safe for multi-threaded polling
 * (which would be confusing anyway, cos polling typically consumes data).
 * 
 * @author daniel
 */
public abstract class AStream implements Closeable {

	// TODO
	// reconnects MAX_RECONNECTS_PER_HOUR

	/**
	 * Use these for push-notification of incoming tweets and stream activity.
	 * 
	 * WARNING: listeners should be fast. They run in the gobbler thread, which
	 * may be switched off by Twitter if it can't keep up with the flow.
	 * 
	 * @see AStream#popTweets() etc. for pull-based notification.
	 */
	public static interface IListen {
		/**
		 * @param event
		 * @return true to pass this on to any other, earlier-added, listeners.
		 *         false to stop earlier listeners from hearing this event.
		 */
		boolean processEvent(TwitterEvent event);

		/**
		 * @param obj
		 *            Miscellaneous Twitter messages, such as limits & deletes
		 * @return true to pass this on to any other, earlier-added, listeners.
		 *         false to stop earlier listeners from hearing this event.
		 */
		boolean processSystemEvent(Object[] obj);

		/**
		 * @param tweet
		 * @return true to pass this on to any other, earlier-added, listeners.
		 *         false to stop earlier listeners from hearing this event.
		 */
		boolean processTweet(ITweet tweet);
	}

	/**
	 * What, Twitter had an outage? Here are the details.
	 * @author daniel
	 */
	public static final class Outage implements Serializable {
		private static final long serialVersionUID = 1L;
		/**
		 * The id received just before the outage. i.e. start
		 */
		public final BigInteger sinceId;
		/**
		 * The Java timecode when the stream went back online. i.e. end
		 */
		public final long untilTime;

		public Outage(BigInteger sinceId, long untilTime) {
			super();
			this.sinceId = sinceId;
			this.untilTime = untilTime;
		}

		@Override
		public String toString() {
			return "Outage[id:" + sinceId + " to time:" + untilTime + "]";
		}
	}

	/**
	 * Start dropping messages after this.
	 */
	public static int MAX_BUFFER = 10000;

	/**
	 * 10 minutes
	 */
	private static final int MAX_WAIT_SECONDS = 600;

	static int forgetIfFull(List incoming) {
		// forget a batch?
		if (incoming.size() < MAX_BUFFER)
			return 0;
		int chop = MAX_BUFFER / 10;
		for (int i = 0; i < chop; i++) {
			incoming.remove(0);
		}
		return chop;
	}

	/**
	 * A blob of text has come in off the wires... what is it?
	 * @param jo
	 * @param jtwitr
	 * @return
	 * @throws JSONException
	 */
	static Object read3_parse(JSONObject jo, Twitter jtwitr)
			throws JSONException {
		// tweets
		if (jo.has("text")) {
			Status tweet = new Status(jo, null);
			return tweet;
		}
		// DMs
		if (jo.has("direct_message")) {
			Message dm = new Message(jo.getJSONObject("direct_message"));
			return dm;
		}

		// Events
		String eventType = jo.optString("event");
		if (eventType != "") {
			TwitterEvent event = new TwitterEvent(jo, jtwitr);
			return event;
		}
		// Deletes and other system events, like limits
		JSONObject del = jo.optJSONObject("delete");
		if (del != null) {
			boolean isDM = false;
			JSONObject s = del.optJSONObject("status");
			if (s==null) {
				s = del.getJSONObject("direct_message");
				isDM = true;
			}
			BigInteger id = new BigInteger(s.getString("id_str"));
			BigInteger userId = new BigInteger(s.getString("user_id"));
			ITweet deadTweet;			
			User dummyUser = new User(null, userId);
			if (isDM) {								
				deadTweet = new Message(dummyUser, id);
			} else {
				deadTweet = new Status(dummyUser, null, id, null);				
			}									
			return new Object[] { "delete", deadTweet, userId};
		}
		// e.g. {"limit":{"track":1234}}
		JSONObject limit = jo.optJSONObject("limit");
		if (limit != null) {
			int cnt = limit.optInt("track");
			if (cnt == 0) {
				System.out.println(jo); // API change :( - a new limit object
			}
			return new Object[] { "limit", cnt };
		}
		// e.g. "disconnect":{"code":7,"stream_name":"XXXX-userstreamxxxx","reason":"admin logout"}		
		JSONObject disconnect = jo.optJSONObject("disconnect");
		if (disconnect != null) {			
			return new Object[] { "disconnect", disconnect};
		}
		// ??
		System.out.println(jo);
		return new Object[]{"unknown", jo};
	}

	boolean autoReconnect;

	final IHttpClient client;

	List<TwitterEvent> events = new ArrayList();

	boolean fillInFollows = true;

	/**
	 * The number of messages (which could be tweets, events, or system
	 * events) which the stream has dropped to stay within it's bounds.
	 */
	private int forgotten;

	/**
	 * Only used by UserStream. See {@link UserStream#getFriends()} 
	 */
	List<Number> friends;

	/**
	 * Needed for constructing some objects.
	 */
	final Twitter jtwit;

	private BigInteger lastId = BigInteger.ZERO;

	final List<IListen> listeners = new ArrayList(0);

	final List<Outage> outages = Collections.synchronizedList(new ArrayList());

	int previousCount;

	StreamGobbler readThread;

	InputStream stream;

	List<Object[]> sysEvents = new ArrayList();

	List<ITweet> tweets = new ArrayList();

	/**
	 * default: false
	 * If true, json is only sent to listeners, and polling based access 
	 * via {@link #getTweets()} will return no results.
	 */
	boolean listenersOnly;

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

	/**
	 * Add a listener to the front of the queue. WARNING: listeners need to be
	 * fast (see javadoc notes on {@link IListen})
	 * 
	 * @param listener
	 */
	public void addListener(IListen listener) {
		synchronized (listeners) {
			// remove if already there
			listeners.remove(listener);
			// add to the front of the list
			listeners.add(0, listener);
		}
	}

	/**
	 * The stream will track outages during use (provided
	 * {@link #setAutoReconnect(boolean)} is true). This method allows you to
	 * manually add outages (which can then be filled in using
	 * {@link #fillInOutages()}) -- e.g. to cover restarting Java.
	 * 
	 * @param outage
	 */
	public void addOutage(Outage outage) {
		// TODO handle overlaps with an existing outage by merging??
		for (int i = 0; i < outages.size(); i++) {
			Outage o = outages.get(i);
			if (o.sinceId.compareTo(outage.sinceId) > 0) {
				// insert here
				outages.add(i, outage);
				return;
			}
		}
		// add to the end
		outages.add(outage);
	}

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
	 * A closed stream can be restarted.
	 */
	// Note: this does NOT close the gobbler if called from the gobbler thread!
	// But it always closes the input-stream.
	@Override
	synchronized public void close() {
		// close the gobbler (unless it's the gobbler who's calling this)
		if (readThread != null && Thread.currentThread() != readThread) {
			readThread.pleaseStop();
			// we mean it!
			if (readThread.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					/// ignore
				}
				readThread.interrupt();
			}
			readThread = null;
		}
		InternalUtils.close(stream);
		stream = null;
	}

	/**
	 * Connect to Twitter.
	 * <p>
	 * Do nothing if we already have a good connection. Bad or partly formed
	 * connections will be closed.
	 * <p>
	 * Auto-reconnect is ignored here: if there's an exception it will be thrown
	 * and a reconnect will not be attempted. This gives a fast-return.
	 * 
	 * @see #reconnect()
	 */
	synchronized public void connect() throws TwitterException {
		if (isConnected())
			return;
		// close all first
		close();

		assert readThread == null || readThread.stream == this : this;

		HttpURLConnection con = null;

		try {
			con = connect2();
			stream = con.getInputStream();
			if (readThread == null) {
				readThread = new StreamGobbler(this);
				readThread.setName("Gobble:" + toString());
				readThread.start();
			} else {
				// we're being started from the gobbler itself
				assert Thread.currentThread() == readThread : this;
				assert readThread.stream == this : readThread;
			}
			// check the connection took
			if (isConnected())
				return;
			Thread.sleep(10);
			if ( ! isConnected()) {
				throw new TwitterException(readThread.ex);
			}
		} catch (Exception e) {
			if (e instanceof TwitterException)
				throw (TwitterException) e;
//			Doesn't catch anything: if (con!=null && client instanceof URLConnectionHttpClient) {
//				((URLConnectionHttpClient) client).processError(con);
//			}			
			throw new TwitterException(e);
		}
	}

	abstract HttpURLConnection connect2() throws Exception;

	/**
	 * Use the REST API to fill in outages when possible. Filled-in outages will
	 * be removed from the list. 
	 * WARNING: This swallows exceptions! (but check the return value)
	 * <p>
	 * In accordance with best-practice, this method will skip over very recent
	 * outages (which will be picked up by subsequent calls to
	 * {@link #fillInOutages()}).
	 * </p>
	 * <p>
	 * From <i>dev.twitter.com</i>:<br>
	 * Do not resume REST API polling immediately after a stream failure. Wait
	 * at least a minute or two after the initial failure before you begin REST
	 * API polling. This delay is crucial to prevent dog-piling the REST API in
	 * the event of a minor hiccup on the streaming API.
	 * </p>
	 * @return null if all OK, the Exception raised otherwise.
	 */
	public final Exception fillInOutages() throws UnsupportedOperationException {
		if (outages.size() == 0)
			return null;
		Outage[] outs = outages.toArray(new Outage[0]);
		// protect our original object from edits and threading-issues
		Twitter jtwit2 = new Twitter(jtwit);		
		Exception ex = null;
		for (Outage outage : outs) {
			// too recent? wait at least 1 minute
			if (System.currentTimeMillis() - outage.untilTime < 60000) {
				continue;
			}
			boolean ok = outages.remove(outage);
			if ( ! ok) continue; // already done or dropped
			try {						
				jtwit2.setSinceId(outage.sinceId);
				jtwit2.setUntilDate(new Date(outage.untilTime));
				jtwit2.setMaxResults(100000); // hopefully not needed!
				// fetch
				fillInOutages2(jtwit2, outage);
				// success
			} catch(Throwable e) {
				// fail -- put it back on the queue				
				outages.add(outage);
				if (e instanceof Exception) {
					ex = (Exception) e;
				}
			}			
		}
		return ex;
	}

	/**
	 * 
	 * @param jtwit2
	 *            with screenname, auth-token, sinceId and untilDate all set up
	 * @param outage
	 */
	abstract void fillInOutages2(Twitter jtwit2, Outage outage);

	@Override
	protected void finalize() throws Throwable {
		// TODO scream blue murder if this is actually needed
		close();
	}

	/**
	 * @return never null
	 */
	public final List<TwitterEvent> getEvents() {
		read();
		return events;
	}

	/**
	 * @return the number of messages (which could be tweets, events, or system
	 *         events) which the stream has dropped to stay within it's (very
	 *         generous) bounds.
	 *         <p>
	 *         Best practice is to NOT rely on this for memory management. You
	 *         should call {@link #popEvents()}, {@link #popSystemEvents()} and
	 *         {@link #popTweets()} regularly to clear the buffers.
	 */
	public final int getForgotten() {
		return forgotten;
	}

	/**
	 * @return the list outages so far. Hopefully empty, never null.
	 *         <p>
	 *         This is the actual list used. You can remove items from this list
	 *         to quietly forget about them. Use {@link #addOutage(Outage)} to
	 *         add items in the correct order. The list size is capped to avoid
	 *         memory leakage.
	 */
	public final List<Outage> getOutages() {
		return outages;
	}

	/**
	 * @return the recent system events, such as "delete this status".
	 * @see #popSystemEvents()
	 */
	public final List<Object[]> getSystemEvents() {
		read();
		return sysEvents;
	}

	public final List<ITweet> getTweets() {
		read();
		// // re-order?? Or do we not care TODO test this is the right way round
		// Collections.sort(tweets, new Comparator<ITweet>() {
		// @Override
		// public int compare(ITweet t1, ITweet t2) {
		// return t1.getCreatedAt().compareTo(t2.getCreatedAt());
		// }
		// });
		return tweets;
	}

	/**
	 * @return true if connected, or if trying to reconnect. Note: false for
	 *         streams which have not yet been connected!
	 */
	public final boolean isAlive() {
		if (isConnected())
			return true;
		if (!autoReconnect)
			return false;
		// is this trying to reconnect -- or has it failed for good?
		return readThread != null && readThread.isAlive()
				&& !readThread.stopFlag;
	}

	/**
	 * Many users will want to use {@link #isAlive()} instead, which takes into
	 * account auto-reconnect behaviour.
	 * 
	 * @return true if connected to Twitter without error (and not in the middle
	 *         of a stop-sequence).
	 * @see #isAlive()
	 */
	public final boolean isConnected() {
		return readThread != null && readThread.isAlive()
				&& readThread.ex == null
				/* so technically this counts a requested stop as an actual stop */
				&& !readThread.stopFlag;
	}

	/**
	 * @return the recent events. Calling this will clear the list of events.
	 * never null
	 * <p>
	 * Thread safety: If two threads both call popEvents / getEvents, then there is a race condition,
	 * and the caller should synchronize appropriately.
	 */
	public final List<TwitterEvent> popEvents() {
		List evs = getEvents();
		events = new ArrayList();
		return evs;
	}

	/**
	 * <h4>System Events</h4>
	 * ["delete", Status] This tweet has been deleted from Twitter<br>
	 * ["limit", int skipped_tweets] See https://dev.twitter.com/discussions/2655<br>
	 * ["exception", Exception]<br>
	 * ["reconnect", milliseconds_offline]<br>
	 * ["disconnect", JSONObject e.g. {"reason":"admin logout","stream_name":"mystream","code":7}]<br>
	 * 
	 * @return the recent system events. Calling this will clear the list of system events.
	 * <p>
	 * Thread safety: If two threads both call popSystemEvents / getSystemEvents, then there is a race condition,
	 * and the caller should synchronize appropriately.
	 */
	public final List<Object[]> popSystemEvents() {
		List<Object[]> evs = getSystemEvents();
		sysEvents = new ArrayList();
		return evs;
	}

	/**
	 * @return the recent events. Calling this will clear the list of tweets.
	 * <p>
	 * Thread safety: If two threads both call popTweets / getTweets, then there is a race condition,
	 * and the caller should synchronize appropriately.
	 */
	public final List<ITweet> popTweets() {
		List<ITweet> ts = getTweets();
		// TODO is 
		tweets = new ArrayList();
		return ts;
	}

	private final void read() {
		if (readThread!=null) {
			String[] jsons = readThread.popJsons();
			for (String json : jsons) {
				try {
					read2(json);
				} catch (JSONException e) {
					throw new TwitterException.Parsing(json, e);
				}
			}
		}
		if (isConnected())
			return;
		// NOT connected?!
		// orderly shutdown? that's OK
		if (readThread != null && readThread.stopFlag)
			return;
		// Dead/zombie thread? Clean Up!
		Exception ex = readThread == null? null : readThread.ex;
		// close all
		close();
		// The connection is down!
		if ( ! autoReconnect) {
			if (ex instanceof TwitterException) throw (TwitterException)ex;
			throw new TwitterException(ex);
		}
		// reconnect using a different thread
		reconnect();
	}

	private void read2(String json) throws JSONException {
		JSONObject jobj = new JSONObject(json);

		// the 1st object for a UserStream is a list of friend ids
		JSONArray _friends = jobj.optJSONArray("friends");
		if (_friends != null) {
			read3_friends(_friends);
			return;
		}

		// parse the json
		Object object = read3_parse(jobj, jtwit);

		// tweets & DMs
		if (object instanceof ITweet) {
			ITweet tweet = (ITweet) object;
			// de-duplicate a bit locally (this is rare -- perhaps don't
			// bother??)
			if (tweets.contains(tweet))
				return;
			tweets.add(tweet);
			// track the last Status id for tracking outages 
			// (NB: Message ids are different & less generally useful)
			if (tweet instanceof Status) {
				BigInteger id = ((Status) tweet).id;
				if (id.compareTo(lastId) > 0) {
					lastId = id;
				}
			}
			forgotten += forgetIfFull(tweets);
			return;
		}

		// Events
		if (object instanceof TwitterEvent) {
			TwitterEvent event = (TwitterEvent) object;
			events.add(event);
			forgotten += forgetIfFull(events);
			return;
		}
		// Deletes and other system events, like limits
		if (object instanceof Object[]) {
			Object[] sysEvent = (Object[]) object;
			// process it...
			// ...delete?
			if ("delete".equals(sysEvent[0])) {
				ITweet deadTweet = (ITweet) sysEvent[1];
				// prune local (which is unlikely to do much)
				boolean pruned = tweets.remove(deadTweet);
				if (pruned) return; // No need to keep this event around
			} else if ("limit".equals(sysEvent[0])) {
				// ...we got rate-limited?
				Integer cnt = (Integer) sysEvent[1];				
				forgotten += cnt;
			}
			// store the sys-event
			sysEvents.add(sysEvent);
			forgotten += forgetIfFull(sysEvents);
			return;
		}
		// ??
		System.out.println(jobj);
	}

	private void read3_friends(JSONArray _friends) throws JSONException {
		List<Number> oldFriends = friends;
		friends = new ArrayList(_friends.length());
		for (int i = 0, n = _friends.length(); i < n; i++) {
			long fi = _friends.getLong(i);
			friends.add(fi);
		}
		if (oldFriends == null || ! fillInFollows)
			return;

		// This is after a reconnect -- did we miss any follow events?
		HashSet<Number> friends2 = new HashSet(friends);
		friends2.removeAll(oldFriends);
		if (friends2.size() == 0)
			return;
		Twitter_Users tu = new Twitter_Users(jtwit);
		List<User> newFriends = tu.showById(friends2);
		User you = jtwit.getSelf();
		for (User nf : newFriends) {
			TwitterEvent e = new TwitterEvent(new Date(), you,
					TwitterEvent.Type.FOLLOW, nf, null);
			events.add(e);
		}
		forgotten += forgetIfFull(events);
	}

	/**
	 * (Re)connect to Twitter. This can take upto 15 minutes before it gives up!
	 * Although it will give up straight away on user error (E40X exceptions).
	 * <p>
	 * Stores outage information if appropriate.
	 */
	synchronized void reconnect() {
		// do the reconnect (can be slow)
		long now = System.currentTimeMillis();
		reconnect2();
		long dt = System.currentTimeMillis() - now;
		addSysEvent(new Object[]{"reconnect", dt});

		// store the outage
		// TODO merge small outages
		if (lastId != BigInteger.ZERO) {
			outages.add(new Outage(lastId, System.currentTimeMillis()));
			// paranoia: avoid memory leaks
			if (outages.size() > 100000) {
				for (int i = 0; i < 1000; i++) {
					outages.remove(0);
				}
				// add an arbitrary number to the forgotten count: 10 per outage
				forgotten += 10000;
			}
		}
	}

	/**
	 * Add a sys-event outside of the normal run of events-received-from-Twitter.
	 * Used for events about the connection (e.g. it's gone down)
	 * @param sysEvent
	 */
	void addSysEvent(Object[] sysEvent) {
		sysEvents.add(sysEvent);
		if (listeners.size()==0) return;
		synchronized (listeners) {
			try {
				for (IListen listener : listeners) {
					boolean carryOn = listener.processSystemEvent(sysEvent);
					// hide from earlier listeners?
					if (!carryOn) {
						break;
					}
				}
			} catch (Exception e) {
				// swallow it & keep the stream flowing
				e.printStackTrace();
			}
		}
	}

	private void reconnect2() {
		// Try again as advised by dev.twitter.com:
		// 1. straightaway
		try {
			connect();
			return;
		} catch (TwitterException.E40X e) {
			// User error (e.g. TooManyLogins) -- don't keep trying
			throw e;
		} catch (Exception e) {
			// oh well
			System.out.println(e);
		}
		// 2. Exponential back-off. Wait a random number of seconds between 20
		// and 40 seconds.
		// Double this value on each subsequent connection failure.
		// Limit this value to the maximum of a random number of seconds between
		// 240 and 300 seconds.
		int wait = 20 + new Random().nextInt(40);
		int waited = 0;
		while (waited < MAX_WAIT_SECONDS) {
			try {
				Thread.sleep(wait * 1000);
				waited += wait;
				if (wait < 300) {
					wait = wait * 2;
				}
				connect();
				// success :)
				return;
			} catch (TwitterException.E40X e) {
				throw e;
			} catch (Exception e) {
				// oh well
				System.out.println(e);
			}
		}
		throw new TwitterException.E50X("Could not connect to streaming server");
	}

	synchronized void reconnectFromGobblerThread() {
		assert Thread.currentThread() == readThread || readThread==null : this;
		if (isConnected())
			return;
		reconnect();
	}

	public boolean removeListener(IListen listener) {
		synchronized (listeners) {
			return listeners.remove(listener);
		}
	}

	/**
	 * 
	 * @param yes
	 *            If true, attempt to connect if disconnected. true by default.
	 */
	public void setAutoReconnect(boolean yes) {
		autoReconnect = yes;
	}

	/**
	 * How many messages prior-to-connecting to retrieve. Twitter bug: Currently
	 * this does not work!
	 * 
	 * @param previousCount
	 *            Up to 150,000 but subject to change.
	 * 
	 *            Negative values are allowed -- they mean the stream will
	 *            terminate when it reaches the end of the historical messages.
	 * 
	 * @deprecated Twitter need to fix this :(
	 */
	@Deprecated
	public void setPreviousCount(int previousCount) {
		this.previousCount = previousCount;
	}

}

/**
 * Gobble output from a twitter stream. Create then call start(). Expects length
 * delimiters This does not process the json it receives.
 * 
 */
final class StreamGobbler extends Thread {

	Exception ex;

	/**
	 * count of the number of tweets this gobbler had to drop due to buffer size
	 */
	int forgotten;

	/**
	 * Use synchronised blocks when editing this
	 */
	private ArrayList<String> jsons = new ArrayList();

//	long offTime;

	volatile boolean stopFlag;

	final AStream stream;

	public StreamGobbler(AStream stream) {
		setDaemon(true);
		this.stream = stream;
	}

	@Override
	protected void finalize() throws Throwable {
		if (stream != null) {
			InternalUtils.close(stream.stream);
		}
	}

	/**
	 * Request that the thread should finish. If the thread is hung waiting for
	 * output, then this will not work.
	 */
	public void pleaseStop() {
		if (stream != null) {
			InternalUtils.close(stream.stream);
		}
		stopFlag = true;
	}

	/**
	 * Read off the collected json snippets for processing
	 * 
	 * @return
	 */
	public synchronized String[] popJsons() {
		String[] arr = jsons.toArray(new String[jsons.size()]);
		// jsons.clear(); This wasn't really working for good memory management
		jsons = new ArrayList();
		return arr;
	}

	private void readJson(BufferedReader br, int len) throws IOException {
		// Read len chars from the stream
		assert len > 0;
		char[] sb = new char[len];
		int cnt = 0;
		while (len > 0) {
			int rd = br.read(sb, cnt, len);
			if (rd == -1)
				throw new IOException("end of stream");
			// continue;
			cnt += rd;
			len -= rd;
		}
		
		String json = new String(sb);
		if ( ! stream.listenersOnly) {
			synchronized (this) {
				jsons.add(json);
				// forget a batch?
				forgotten += AStream.forgetIfFull(jsons);
			}
		}

		// push notifications
		readJson2_notifyListeners(json);
	}

	private void readJson2_notifyListeners(String json) {
		if (stream.listeners.size() == 0)
			return;
		synchronized (stream.listeners) {
			try {
				JSONObject jo = new JSONObject(json);
				Object obj = AStream.read3_parse(jo, stream.jtwit);
				for (IListen listener : stream.listeners) {
					boolean carryOn;
					if (obj instanceof ITweet) {
						carryOn = listener.processTweet((ITweet) obj);
					} else if (obj instanceof TwitterEvent) {
						carryOn = listener.processEvent((TwitterEvent) obj);
					} else {
						carryOn = listener.processSystemEvent((Object[]) obj);
					}
					// hide from earlier listeners?
					if (!carryOn) {
						break;
					}
				}
			} catch (Exception e) {
				// swallow it & keep the stream flowing
				e.printStackTrace();
			}
		}
	}

	/**
	 * Read a number from the stream -- which is the length of the next message.
	 * @param br
	 * @return
	 * @throws IOException
	 */
	private int readLength(BufferedReader br) throws IOException {
		StringBuilder numSb = new StringBuilder();
		while (true) {
			int ich = br.read();
			if (ich == -1)
				throw new IOException("end of stream " + this);
			// continue;
			char ch = (char) ich;
			if (ch == '\n' || ch == '\r') {
				// ignore leading whitespace, stop otherwise
				if (numSb.length() == 0) {
					continue;
				}
				// done!
				break;
			}
			// collect digits
			assert Character.isDigit(ch) : ch;
			assert numSb.length() < 10 : numSb; // paranoia
			numSb.append(ch);
		}
		return Integer.valueOf(numSb.toString());
	}

	@Override
	public void run() {
		while (!stopFlag) {			
			assert stream.stream != null : stream;
			try {
				InputStreamReader isr = new InputStreamReader(stream.stream);
				BufferedReader br = new BufferedReader(isr);
				while (!stopFlag) {
					int len = readLength(br);
					readJson(br, len);
				}
			} catch (Exception ioe) {
				if (stopFlag) {
					// we were told to stop already so ignore
					return;
				}
				ex = ioe;
//				offTime = System.currentTimeMillis();
				// TODO log this as a sys-event
				stream.addSysEvent(new Object[]{"exception", ex});
				// try a reconnect?
				if (!stream.autoReconnect)
					return; // no - break out of the loop
				// Note: the thread can also hang or die, so we also do
				// reconnects from
				// the AStream.read() method.
				try {
					stream.reconnectFromGobblerThread();
					assert stream.stream != null : stream;
				} catch (Exception e) {
					// #fail
					ex = e;
					return;
				}
			}
		}
	}

	@Override
	public String toString() {
		return getName() + "[" + jsons.size() + "]";
	}
}
