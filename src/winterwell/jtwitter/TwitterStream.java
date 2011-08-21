package winterwell.jtwitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.ITweet;

/**
 * Connect to the streaming API.
 * <p>
 * Duplicate messages may be delivered when reconnecting to the Streaming API.
 * <p>
 * Status: This class is in an early stage, and may change. 
 * @author Daniel
 */
public class TwitterStream {

	private final IHttpClient client;

	public TwitterStream(IHttpClient client) {
		this.client = client;
	}
	
	KMethod method = KMethod.sample;
	private InputStream stream;
	
	public static enum KMethod {
		/**
		 * Follow hashtags, users or regions
		 */
		filter,
		/**
		 * Garden-hose: a sample of tweets, suitable for trend analysis.
		 */
		sample, 
		/** Requires special access privileges */
		links, 
		/** New-style retweets. Requires special access privileges */
		retweet, 
		/** Everything! Requires special access privileges */
		firehose
	}
	
	/**
	 * Set the method. The default is "sample", as this is the only one which
	 * works with no extra settings.
	 * @param method
	 */
	public void setMethod(KMethod method) {
		this.method = method;
	}
	
	StreamGobbler readThread; 
		
	public void connect() {
		close();
		try {
			String url = "http://stream.twitter.com/1/statuses/"+method+".json?delimited=length";
			Map<String, String> vars = new HashMap();
			HttpURLConnection con = client.connect(url, vars, true);
			stream = con.getInputStream();
			readThread = new StreamGobbler(stream);
			readThread.start();
		} catch (Exception e) {
			throw new TwitterException(e);
		}
	}
	
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	public void close() {
		if (readThread!=null) readThread.pleaseStop();
		URLConnectionHttpClient.close(stream);				
	}
	
	public List<ITweet> read() {
		assert readThread.isAlive();
		String[] jsons = readThread.popJsons();
		for (String json : jsons) {
			
		}
		throw new RuntimeException("TODO");
	}
	
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
