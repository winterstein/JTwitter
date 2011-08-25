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
import winterwell.utils.TodoException;

/**
 * Connect to the streaming API.
 * <p>
 * Duplicate messages may be delivered when reconnecting to the Streaming API.
 * <p>
 * Status: This class is in an early stage, and may change. 
 * @author Daniel
 */
public class TwitterStream extends AStream {

	/**
	 * 
	 * @param client This will have it's timeout set to 90 seconds.
	 * So you probably don't want to reuse the object with the REST api. 
	 */
	public TwitterStream(Twitter jtwit) {
		super(jtwit);
	}
	
	KMethod method = KMethod.sample;
	private List<String> track;
	private List<Long> follow;
	
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
	void setMethod(KMethod method) {
		this.method = method;
	}
		
	HttpURLConnection connect2() throws IOException {
			String url = "http://stream.twitter.com/1/statuses/"+method+".json?delimited=length";
			Map<String, String> vars = new HashMap();
			if (follow!=null) {
				vars.put("follow", InternalUtils.join(follow, 0, Integer.MAX_VALUE));
			}
			if (track!=null) {
				vars.put("track", InternalUtils.join(track, 0, Integer.MAX_VALUE));	
			}
			// FIXME need to use post for long sets of vars :(
			HttpURLConnection con = client.connect(url, vars, true);
			return con;
	}

	/**
	 * , 5,000 follow userids and 
	 * @param userIds
	 */
	public void setFollowUsers(List<Long> userIds) {
		method = KMethod.filter;
		follow = userIds;
	}
	
	/**
	25 0.1-360 degree location boxes.
	
	Only tweets that are both created using the Geotagging API and are placed from within a tracked bounding box will be included in the stream – the user’s location field is not used to filter tweets
	
	@param boundingBoxes
	Each element consists of longitude/latitude south-west, north-east.	 
	*/
	public void setLocation(List<double[]> boundingBoxes) {
		method = KMethod.filter;
		throw new TodoException();		
	}
	
	/**
	 * See https://dev.twitter.com/docs/streaming-api/methods#track
	 * @param keywords The default access level allows up to 400 track keywords.
	 */
	public void setTrackKeywords(List<String> keywords) {		
		this.track = keywords;
		method = KMethod.filter;
	}
}


