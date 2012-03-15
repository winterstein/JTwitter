package winterwell.jtwitter;

import java.util.Map;

import winterwell.json.JSONObject;
import winterwell.jtwitter.Twitter.IHttpClient;

/**
 * Example Usage:
 * <pre><code>
 * Twitter twitter;
 * int cnt = twitter.analytics().getUrlCount("http://example.com");
 * </code></pre>
 * 
 * <p>
 * Current features: a count of how often a url has been tweeted.
 * <p>
 * We anticipate that Twitter will provide more of an analytics API over time!
 * @author daniel
 *
 */
public class Twitter_Analytics {
	
	private IHttpClient http;

	/**
	 * To get a Twitter_Analytics object, use the Twitter.analytics() method.
	 * @param http
	 */
	Twitter_Analytics(IHttpClient http) {
		this.http = http;
	}
	
	/**
	 * @param url Note that the count is for the exact link. 
	 * E.g. "http://example.com" and "http://www.example.com" are considered separate.
	 * @return number of times this link has been (publicly) posted.<br>
	 * NB: The count begins from July 2010 (when Twitter launched the Tweet Button); earlier postings aren't counted.
	 */
	public int getUrlCount(String url) {
		Map vars = InternalUtils.asMap("url", url);
		String json = http.getPage("http://urls.api.twitter.com/1/urls/count.json", 
				vars, false);
		JSONObject jo = new JSONObject(json);
		return jo.getInt("count");
	}
	
}
