package winterwell.jtwitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import winterwell.jtwitter.AStream.Outage;
import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.Twitter.User;
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("TwitterStream");
		sb.append("["+method);
		if (track!=null) sb.append(" track:"+InternalUtils.join(track, 0, 3));
		if (follow!=null) sb.append(" follow:"+InternalUtils.join(follow, 0, 3));
		if (locns!=null) sb.append(" in:"+InternalUtils.join(locns, 0, 3));
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public void fillInOutages() throws UnsupportedOperationException {
		if (outages.isEmpty()) return;
		if (method != KMethod.filter) throw new UnsupportedOperationException();
		Outage[] outs = outages.toArray(new Outage[0]);		
		// protect our original object from edits
		User self = jtwit.getSelf();
		Twitter jtwit2 = new Twitter(self.getScreenName(), jtwit.getHttpClient());
		for (Outage outage : outs) {			
			jtwit2.setSinceId(outage.sinceId);
			jtwit2.setUntilDate(new Date(outage.untilTime));
			jtwit2.setMaxResults(100000); // hopefully not needed!
			// keywords?
			if (track!=null) {
				for(String keyword : track) {
					List<Status> msgs = jtwit.search(keyword);
					for (Status status : msgs) {
						if (tweets.contains(status)) continue;
						tweets.add(status);
					}
				}
			}
			// users?
			if (follow!=null) {
				for(Long user : follow) {
					List<Status> msgs = jtwit.getUserTimeline(user);
					for (Status status : msgs) {
						if (tweets.contains(status)) continue;
						tweets.add(status);
					}
				}
			}
			// regions?
			if (locns != null && ! locns.isEmpty()) {
				throw new UnsupportedOperationException("TODO"); // TODO
			}
			// success			
			outages.remove(outage);
		}
	}
	
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
	private List<double[]> locns;
	
	public static enum KMethod {
		/**
		 * Follow hashtags, users or regions
		 */
		filter,
		/**
		 * Spritzer or Garden-hose: 
		 * a sample of tweets, suitable for trend analysis.<br>
		 * The default level (spritzer) is roughly 1% of all public tweets.<br>
		 * The upgraded level (garden-hose - apply to Twitter for this) is 10%.<br>
		 * In both cases the algorithm is based on the tweet-id modulo 100.
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
		this.locns = boundingBoxes;
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


