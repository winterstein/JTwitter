/**
 * 
 */
package winterwell.jtwitter;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.TwitterStream.KMethod;

	/**
	 * Connect to the streaming API.
	 * <p>
	 * Status: This class is in an early stage, and may change. 
	 * @author Daniel
	 */
	public class UserStream {

		private final IHttpClient client;

		public UserStream(Twitter jtwit) {
			this.client = jtwit.getHttpClient();
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
