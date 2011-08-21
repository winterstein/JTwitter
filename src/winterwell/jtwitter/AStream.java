package winterwell.jtwitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.Twitter.Status;

/**
 * Internal base class for UserStream and TwitterStream
 * @author daniel
 */
abstract class AStream {

	// TODO
	/**
	 * Attempt the first, and, only the first, reconnect to user streams immediately upon failure.
If reconnect succeeds, clear the failure time.
If reconnect fails, wait for an increasing period of time before attempting the next reconnect – an exponential back-off. Wait a random number of seconds between 20 and 40 seconds. Double this value on each subsequent connection failure. Limit this value to the maximum of a random number of seconds between 240 and 300 seconds.
Do not resume REST API polling immediately after a stream failure. Wait at least a minute or two after the initial failure before you begin REST API polling. This delay is crucial to prevent dog-piling the REST API in the event of a minor hiccup on the streaming API.
Perform a final REST API poll after a successful connection is established to ensure that there is no data loss.
	 */

	@Override
	protected void finalize() throws Throwable {
		// TODO scream blue murder if this is actually needed
		close();
	}

	/**
	 * @return the recent events. Calling this will clear the list of events.
	 */
	public List<TwitterEvent> popEvents() {
		read();
		List evs = getEvents();
		events = new ArrayList();
		return evs;
	}

	/**
	 * @return the recent events. Calling this will clear the list of tweets.
	 */
	public List<ITweet> popTweets() {
		read();
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
	
	void read() {
		assert readThread.isAlive();
		String[] jsons = readThread.popJsons();		
		for (String json : jsons) {
			try {
				JSONObject jo = new JSONObject(json);
				// the 1st object for a user stream is a list of friend ids
				JSONArray _friends = jo.optJSONArray("friends");
				if (_friends != null) {
					friends = new ArrayList(_friends.length());
					for (int i = 0, n = _friends.length(); i < n; i++) {
						friends.add(_friends.getLong(i));
					}
					continue;
				}
				
				// tweets
				// TODO DMs??
				System.out.println(jo);
				if (jo.has("text")) {
					Status tweet = new Twitter.Status(jo, null);
					// de-duplicate??
					if (tweets.contains(tweet)) {
						System.out.println("DUPLICATE TWEET!");
						continue;
					}
					tweets.add(tweet);
					continue;
				}
				
				// Events
				String eventType = jo.optString("event");
				if (eventType != "") {
					TwitterEvent event = new TwitterEvent(jo, jtwit);
					events.add(event);
				}
			} catch (JSONException e) {
				throw new TwitterException.Parsing(json, e);
			}
		}
	}

	/**
	 * How many messages prior-to-connecting to retrieve
	 * 
	 * @param previousCount
	 *            Up to 150,000 but subject to change.
	 * 
	 *            Negative values are allowed -- they mean the stream will
	 *            terminate when it reaches the end of the historical messages.
	 */
	public void setPreviousCount(int previousCount) {
		this.previousCount = previousCount;
	}

	
	public AStream(IHttpClient client) {
		this.client = client;
		this.jtwit = new Twitter(null, client);
		// Twitter send 30 second keep-alive pulses, but ask that
		// you wait 3 cycles before disconnecting
		client.setTimeout(90 * 1000);
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
			readThread.start();
		} catch (Exception e) {
			throw new TwitterException(e);
		}
	}

	abstract HttpURLConnection connect2() throws IOException;

	public void close() {
		if (readThread != null) {
			readThread.pleaseStop();
		}
		URLConnectionHttpClient.close(stream);
	}


	final IHttpClient client;

	List<TwitterEvent> events = new ArrayList<TwitterEvent>();
	List<ITweet> tweets = new ArrayList();


	/**
	 * Needed for constructing some objects.
	 */
	private Twitter jtwit;

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
	private final InputStream is;
	private volatile boolean stopFlag;
	private IOException ex;

	/**
	 * start dropping messages after this.
	 */
	private static final int MAX_BUFFER = 1000000;
	
	public StreamGobbler(InputStream is) {
		super("gobbler:" + is.toString());
		setDaemon(true);
		this.is = is;
	}

	final List<String> jsons = new ArrayList();
	/**
	 * count of the number of tweets this gobbler had to drop
	 * due to buffer size
	 */
	private int forgotten;
	
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
		return "StreamGobbler:"+jsons;
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
			while (!stopFlag) {
				int len = readLength(br);
				readJson(br, len);
			}
		} catch (IOException ioe) {
			if (stopFlag) {
				// we were told to stop already so ignore
				return;
			}
			ioe.printStackTrace();
			ex = ioe;
		}
	}

	private void readJson(BufferedReader br, int len) throws IOException {
		assert len > 0;
		char[] sb = new char[len];
		int cnt = 0;
		while(len>0) {
			int rd = br.read(sb, cnt, len);		
			if (rd == -1)
				throw new IOException("end of stream");
			cnt += rd;
			len -= rd;
		}		
		synchronized (jsons) {
			jsons.add(new String(sb));
			// forget a batch?
			if (jsons.size() > MAX_BUFFER) {
				forgotten += 1000;
				for(int i=0; i<1000; i++) {
					jsons.remove(0);
				}
			}
		}		
	}

	private int readLength(BufferedReader br) throws IOException {
		StringBuilder numSb = new StringBuilder();		
		while(true) {
			int ich = br.read();		
			if (ich == -1)
				throw new IOException("end of stream");
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
