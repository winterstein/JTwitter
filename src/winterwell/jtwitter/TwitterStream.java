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
public class TwitterStream extends UserStream {

	public TwitterStream(IHttpClient client) {
		super(client);
	}
	
	KMethod method = KMethod.sample;
	
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
		
	HttpURLConnection connect2() throws IOException {
			String url = "http://stream.twitter.com/1/statuses/"+method+".json?delimited=length";
			Map<String, String> vars = new HashMap();
			HttpURLConnection con = client.connect(url, vars, true);
			return con;
	}
		
}


