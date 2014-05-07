package winterwell.jtwitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.MalformedInputException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import winterwell.json.JSONArray;
import winterwell.json.JSONObject;
import winterwell.jtwitter.Twitter.KRequestType;
import winterwell.jtwitter.guts.Base64Encoder;
import winterwell.utils.reporting.Log;

/**
 * A simple http client that uses the built in URLConnection class.
 * <p>
 * Provides Twitter-focused error-handling, generating the right
 * TwitterException. Also has a retry-on-error mode which can help smooth out
 * Twitter's sometimes intermittent service. See
 * {@link #setRetryOnError(boolean)}.
 * 
 * @author Daniel Winterstein
 * Includes code by vlad@myjavatools.com under Apache license version 2.0
 * 
 */
public class URLConnectionHttpClient implements Twitter.IHttpClient,
		Serializable, Cloneable {
	private static final int dfltTimeOutMilliSecs = 10 * 1000;

	private static final long serialVersionUID = 1L;

	private Map<String, List<String>> headers;

	int minRateLimit;

	protected String name;

	private String password;

	private Map<String, RateLimit> rateLimits = Collections.synchronizedMap(new HashMap());

	/**
	 * If true, will wait 1/2 second and make a 2nd request when presented with
	 * a server error (E50X). Only retries once -- a 2nd fail will throw an exception.
	 * 
	 * This policy handles most Twitter server glitches.
	 */
	boolean retryOnError;
	
	@Override
	public boolean isRetryOnError() {
		return retryOnError;
	}

	protected int timeout = dfltTimeOutMilliSecs;

	private boolean htmlImpliesError = true;

	private boolean gzip = false;
	
	/**
	 * Set whether or not to request gzipped responses.
	 * The default is true.
	 */
	public void setGzip(boolean gzip) {
		this.gzip = gzip;
	}
	
	/**
	 * @param htmlImpliesError default is true. If true, an html response will
	 * be treated as a server error & generate a TwitterException.E50X 
	 */
	public void setHtmlImpliesError(boolean htmlImpliesError) {
		this.htmlImpliesError = htmlImpliesError;
	}

	public URLConnectionHttpClient() {
		this(null, null);
	}

	public URLConnectionHttpClient(String name, String password) {
		this.name = name;
		this.password = password;
		assert (name != null && password != null)
				|| (name == null && password == null);
	}

	@Override
	public boolean canAuthenticate() {
		return name != null && password != null;
	}

	@Override
	public HttpURLConnection connect(String url, Map<String, String> vars,
			boolean authenticate) throws IOException 
	{
		// Stop early to protect limits?		
		String resource = checkRateLimit(url);
		// Build the full url
		if (vars != null && vars.size() != 0) {
			// add get variables
			StringBuilder uri = new StringBuilder(url);
			if (url.indexOf('?') == -1) {
				uri.append("?");
			} else if (!url.endsWith("&")) {
				uri.append("&");
			}
			for (Entry e : vars.entrySet()) {
				if (e.getValue() == null) {
					continue;
				}
				String ek = InternalUtils.encode(e.getKey());
				assert !url.contains(ek + "=") : url + " " + vars;
				uri.append(ek + "=" + InternalUtils.encode(e.getValue()) + "&");
			}
			url = uri.toString();
		}
		// Setup a connection
		HttpURLConnection connection = (HttpURLConnection) new URL(url)
				.openConnection();
		// Authenticate
		if (authenticate) {
			setAuthentication(connection, name, password);
		}
		// To keep the search API happy - which wants either a referrer or a
		// user agent
		// AZ: User-Agent and Host are required for getting gzipped responses  
		connection.setRequestProperty("User-Agent", "JTwitter/" + Twitter.version);
		connection.setRequestProperty("Host", "api.twitter.com");
		if (gzip) {
			connection.setRequestProperty("Accept-Encoding", "gzip");
		}
		connection.setDoInput(true);
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);
		connection.setConnectTimeout(timeout);
		// Open a connection
		processError(connection, resource);
		processHeaders(connection, resource);
		return connection;
	}

	@Override
	public Twitter.IHttpClient copy() {
		return clone();
	}
	
	/**
	 * Identical to {@link #copy()}
	 */
	@Override
	public URLConnectionHttpClient clone() {
		try {
			URLConnectionHttpClient c = (URLConnectionHttpClient) super.clone();
			c.name = name;
			c.password = password;
			c.gzip = gzip;			
			c.htmlImpliesError = htmlImpliesError;
			c.setRetryOnError(retryOnError);
			c.setTimeout(timeout);
			c.setMinRateLimit(minRateLimit);
			c.rateLimits = rateLimits; // Share the rate limit info			
//			c.rateLimits.putAll(rateLimits); // Copy it			
			return c;
		} catch(CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}

	protected final void disconnect(HttpURLConnection connection) {
		if (connection == null)
			return;
		try {
			connection.disconnect();
		} catch (Throwable t) {
			// ignore
		}
	}

	private String getErrorStream(HttpURLConnection connection) {
		try {
			return InternalUtils.read(connection.getErrorStream());
		} catch (NullPointerException e) {
			return null;
		}
	}

	@Override
	public String getHeader(String headerName) {
		if (headers == null)
			return null;		
		List<String> vals = headers.get(headerName);
		if (vals==null) {
			// Seen April 2014 -- lowercase X-Rate-Limit headers?!
			vals = headers.get(headerName.toLowerCase());
		}
		return vals == null || vals.isEmpty() ? null : vals.get(0);
	}

	/**
	 * @return The user's screen-name, if known, or null, or "?user"
	 */
	String getName() {
		return name;
	}
	
	
	public Map<String, RateLimit> getRateLimits() {
		return rateLimits;
	}

	/**
	 * Call Twitter to get the rate limit.
	 * @return latest rate limits (which will also be cached here for fast checks)
	 */
	public Map<String, RateLimit> updateRateLimits() {
		Map<String, String> vars = null; // request only some resources?
		String json = getPage(Twitter.DEFAULT_TWITTER_URL+"/application/rate_limit_status.json", vars, true);
		
		JSONObject jo = new JSONObject(json).getJSONObject("resources");

		Collection<JSONObject> families = (Collection<JSONObject>) jo.getMap().values();
		for (JSONObject family : families) {
			for(String res : family.getMap().keySet()) {
				// FIXME remove any :id :slug stuff from res
				JSONObject jrl = (JSONObject) family.getMap().get(res);
				RateLimit rl = new RateLimit(jrl);
				rateLimits.put(res, rl);
			}
		}		
		
		return getRateLimits();
	}


	@Override
	public final String getPage(String url, Map<String, String> vars,
			boolean authenticate) throws TwitterException 
	{		
		assert url != null;
		InternalUtils.count(url);
		// This method handles the retry behaviour.
		try {
			// Do the actual work
			String json = getPage2(url, vars, authenticate);
			// ?? Test for and treat html as an error??
			if (htmlImpliesError && 
				(json.startsWith("<!DOCTYPE html") || json.startsWith("<html"))) {
				// whitelist: sometimes we do expect html
				if (url.startsWith("https://twitter.com")/*used by flush()*/) {
					// OK
				} else {
					String meat = InternalUtils.stripTags(json);
					throw new TwitterException.E50X(meat);
				}
			}
			return json;			
		} catch (IOException e) {
			if ( ! retryOnError) throw getPage2_ex(e, url);
			try {
				// wait half a second before retrying
				Thread.sleep(500);
				return getPage2(url, vars, authenticate);
			} catch (Exception e2) {
				throw getPage2_ex(e, url);
			}
		} catch (TwitterException.E50X e) {
			if ( ! retryOnError) throw getPage2_ex(e, url);
			try {
				// wait half a second before retrying
				Thread.sleep(500);
				return getPage2(url, vars, authenticate);
			} catch (Exception e2) {
				throw getPage2_ex(e, url);
			}
		}
	}

	/**
	 * Called on error. What to throw? 
	 */
	private TwitterException getPage2_ex(Exception ex, String url) {
		if (ex instanceof TwitterException) return (TwitterException) ex;
		if (ex instanceof SocketTimeoutException) {
			return new TwitterException.Timeout(url);
		}
		if (ex instanceof IOException) {
			return new TwitterException.IO((IOException) ex);
		}
		return new TwitterException(ex);
	}
	/**
	 * Does the actual work for {@link #getPage(String, Map, boolean)}
	 * 
	 * @param url
	 * @param vars
	 * @param authenticate
	 * @return page if successful
	 * @throws IOException 
	 */
	private String getPage2(String url, Map<String, String> vars,
			boolean authenticate) throws IOException {
		HttpURLConnection connection = null;
		try {
			connection = connect(url, vars, authenticate);
			InputStream inStream = connection.getInputStream();
			// AZ: gunzip if twitter indicates it's gzipped content
			// TODO Use this in streaming too (but see dev.twitter.com note about sub-classing!)
			String contentEncoding = connection.getContentEncoding();
			if ("gzip".equals(contentEncoding)) {
				inStream = new GZIPInputStream(inStream);
			}
			// Read in the web page
			String page = InternalUtils.read(inStream);
			// Done
			return page;
		} catch(MalformedInputException ex) {
			// provide some debug info
			throw new IOException(ex+" enc:"+connection.getContentEncoding());
		} finally {
			disconnect(connection);
		}		
	}

	@Override
	public RateLimit getRateLimit(KRequestType reqType) {
		return rateLimits.get(reqType.rateLimit);
	}
	




	@Override
	public final String post(String uri, Map<String, String> vars,
			boolean authenticate) throws TwitterException 
	{		
		InternalUtils.count(uri);
		try {
			// do the actual work
			String json = post2(uri, vars, authenticate);
			// ?? Test for and treat html as an error??
			return json;
		} catch (TwitterException.E50X e) {
			if ( ! retryOnError) throw getPage2_ex(e, uri);
			try {
				// wait half a second before retrying
				Thread.sleep(500);
				return post2(uri, vars, authenticate);
			} catch (Exception e2) {
				throw getPage2_ex(e, uri);
			}
		} catch (SocketTimeoutException e) {
			if ( ! retryOnError) throw getPage2_ex(e, uri);
			try {
				// wait half a second before retrying
				Thread.sleep(500);
				return post2(uri, vars, authenticate);
			} catch (Exception e2) {
				throw getPage2_ex(e, uri);
			}
		} catch (Exception e) {
			throw getPage2_ex(e, uri);
		}
	}

	private String post2(String uri, Map<String, String> vars,
			boolean authenticate) throws Exception 
	{
		HttpURLConnection connection = null;
		try {
			connection = post2_connect(uri, vars);
			// Get the response
			String response = InternalUtils.read(connection
					.getInputStream());
			return response;
		} finally {
			disconnect(connection);
		}
	}

	@Override
	public HttpURLConnection post2_connect(String uri, Map<String, String> vars)
			throws Exception 
	{
		String resource = checkRateLimit(uri);
		InternalUtils.count(uri);
		HttpURLConnection connection = (HttpURLConnection) new URL(uri)
				.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		// post methods are alwasy with authentication
		setAuthentication(connection, name, password);

		connection.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		connection.setReadTimeout(timeout);
		connection.setConnectTimeout(timeout);
		// build the post body
		String payload = post2_getPayload(vars);
		connection.setRequestProperty("Content-Length", "" + payload.length());
		OutputStream os = connection.getOutputStream();
		os.write(payload.getBytes());
		InternalUtils.close(os);
		// check connection & process the envelope
		processError(connection, resource);
		processHeaders(connection, resource);
		return connection;
	}

	protected String checkRateLimit(String url) {
		String resource = RateLimit.getResource(url);
		RateLimit limit = rateLimits.get(resource);
		
		if (limit != null && limit.getRemaining() <= minRateLimit
			&& ! limit.isOutOfDate()) 
		{
			throw new TwitterException.PreEmptiveRateLimit(
					"Pre-emptive rate-limit block for "+limit+" for "+url+ " based on minimum limit"+ minRateLimit);
		}
		return resource;
	}

	/**
	 * 
	 * @param vars Keys & values will be url-encoded. 
	 * Special case: if there is 1 key "", then just the value is returned (url-encoded).
	 * @return
	 */
	protected String post2_getPayload(Map<String, String> vars) {
		if (vars == null || vars.isEmpty())
			return "";
		StringBuilder encodedData = new StringBuilder();
		
		// Special case: Just send a body (no key-value encoding)?
		if (vars.size()==1) {
			String key = vars.keySet().iterator().next();
			if ("".equals(key)) {
				String val = InternalUtils.encode(vars.get(key));
				return val;
			}
		}

		for (String key : vars.keySet()) {
			String val = InternalUtils.encode(vars.get(key));
			encodedData.append(InternalUtils.encode(key));
			encodedData.append('=');
			encodedData.append(val);
			encodedData.append('&');
		}
		encodedData.deleteCharAt(encodedData.length() - 1);
		return encodedData.toString();
	}

	/**
	 * Throw an exception if the connection failed
	 * 
	 * @param connection
	 */
	 final void processError(HttpURLConnection connection, String resource) {
		try {
			int code = connection.getResponseCode();
			if (code == 200)
				return;
			URL url = connection.getURL();
			// any explanation?
			String error = processError2_reason(connection);
			// which error?
			if (code == 401) {
				if (error.contains("Basic authentication is not supported"))
					throw new TwitterException.UpdateToOAuth();
				throw new TwitterException.E401(error + "\n" + url + " ("
						+ (name == null ? "anonymous" : name) + ")");
			}
			if (code == 400 && error.startsWith("code 215")) {
				// Twitter-error-code 215 "Bad Authentication data" uses http-code 400, though 401 makes more sense.
				throw new TwitterException.E401(error);
			}
			if (code == 403) {
				// separate out the 403 cases
				processError2_403(connection, resource, url, error);
			}
			if (code == 404) {
				// user deleted?
				if (error != null && error.contains("deleted"))
					// Note: This is a 403 exception
					throw new TwitterException.SuspendedUser(error+ "\n"+ url);
				throw new TwitterException.E404(error + "\n" + url);
			}
			if (code == 406)
				// Hm: It might be nice to have info on post variables here 
				throw new TwitterException.E406(error + "\n" + url);
			if (code == 413)
				throw new TwitterException.E413(error + "\n" + url);
			if (code == 416)
				throw new TwitterException.E416(error + "\n" + url);
			if (code == 420)
				throw new TwitterException.TooManyLogins(error + "\n" + url);
			if (code >= 500 && code < 600)
				throw new TwitterException.E50X(error + "\n" + url);

			// Over the rate limit?
			processError2_rateLimit(connection, resource, code, error);

			// redirect??
			if (code>299 && code<400) {
				String locn = connection.getHeaderField("Location");
				throw new TwitterException(code + " " + error + " " + url+" -> "+locn);
			}
			
			// just report it as a vanilla exception
			throw new TwitterException(code + " " + error + " " + url);

		} catch (SocketTimeoutException e) {
			URL url = connection.getURL();
			throw new TwitterException.Timeout(timeout + "milli-secs for "
					+ url);
		} catch (ConnectException e) {
			// probably also a time out
			URL url = connection.getURL();
			throw new TwitterException.Timeout(url.toString());
		} catch (SocketException e) {
			// treat as a server error - because it probably is
			// (yes, it could also be an error at your end)
			throw new TwitterException.E50X(e.toString());
		} catch (IOException e) {
			throw new TwitterException(e);
		}
	}

	private String processError2_reason(HttpURLConnection connection) throws IOException {
		// Try for a helpful message from Twitter
		String errorPage = readErrorPage(connection);
		if (errorPage != null) {
			try {			
				JSONObject je = new JSONObject(errorPage);
				Object error = je.get("errors");
				if (error instanceof JSONArray) {
					JSONObject err = ((JSONArray)error).getJSONObject(0);
					return "code "+err.get("code")+": "+err.getString("message");
				} else if (error instanceof String) {
					return (String) error;
				}
			} catch (Exception e) {
				// guess not!				
			}				
		}
		
		// normal error channels
		String error = connection.getResponseMessage();
		Map<String, List<String>> connHeaders = connection.getHeaderFields();
		List<String> errorMessage = connHeaders.get(null);
		if (errorMessage != null && !errorMessage.isEmpty()) {
			error += "\n" + errorMessage.get(0);
		}
		if (errorPage != null && !errorPage.isEmpty()) {
			error += "\n" + errorPage;
		}		
		return error;
	}

	private void processError2_403(HttpURLConnection connection, String resource, URL url, String errorPage) {
		// is this a "too old" exception?
		String _name = name==null? "anon" : name;
		if (errorPage == null) {
			throw new TwitterException.E403(url + " (" + _name+ ")");
		}
		// Rate limit?
		if (errorPage.startsWith("code 185") || errorPage.contains("Wow, that's a lot of Twittering!")) {
			// store the rate limit info
			processHeaders(connection, resource);
			throw new TwitterException.RateLimit(errorPage);
		}
		if (errorPage.contains("too old"))
			throw new TwitterException.BadParameter(errorPage + "\n" + url);
		// is this a suspended user exception?
		if (errorPage.contains("suspended"))
			throw new TwitterException.SuspendedUser(errorPage +": "+getName()+"\n" + url);
		// this can be caused by looking up is-follower wrt a suspended
		// account
		if (errorPage.contains("Could not find"))
			throw new TwitterException.SuspendedUser(errorPage + "\n" + url);
		if (errorPage.contains("too recent"))
			throw new TwitterException.TooRecent(errorPage + "\n" + url);
		if (errorPage.contains("already requested to follow"))
			throw new TwitterException.Repetition(errorPage + "\n" + url);
		if (errorPage.contains("duplicate"))
			throw new TwitterException.Repetition(errorPage);
		if (errorPage.contains("unable to follow more people"))
			throw new TwitterException.FollowerLimit(name + " " + errorPage);
		if (errorPage.contains("application is not allowed to access"))
			throw new TwitterException.AccessLevel(name + " " + errorPage);
		throw new TwitterException.E403(errorPage + "\n" + url + " (" + _name+ ")");
	}

	private void processError2_rateLimit(HttpURLConnection connection, String resource,
			int code, String error) 
	{
		boolean rateLimitExceeded = error.contains("Rate limit exceeded");
		if (rateLimitExceeded) {
			// store the rate limit info
			processHeaders(connection, resource);
			throw new TwitterException.RateLimit(getName() + ": " + error);
		}
		// The Rate limiter can sometimes cause a 400 Bad Request
		if (code == 400) {
			try {
				String json = getPage(
						"http://twitter.com/account/rate_limit_status.json",
						null, password != null);
				JSONObject obj = new JSONObject(json);
				int hits = obj.getInt("remaining_hits");
				if (hits < 1)
					throw new TwitterException.RateLimit(error);
			} catch (Exception e) {
				// oh well
			}
		}
	}

	/**
	 * Cache headers for {@link #getHeader(String)}
	 * 
	 * @param connection
	 */
	protected final void processHeaders(HttpURLConnection connection, String resource) {
		headers = connection.getHeaderFields();
		updateRateLimits(resource);
	}

	static String readErrorPage(HttpURLConnection connection) {
		InputStream stream = connection.getErrorStream();
		if (stream == null) {
			return null;
		}
		try {
			// gunzip the page if twitter indicates it
			if ("gzip".equals(connection.getHeaderField("Content-Encoding"))) {
				stream = new GZIPInputStream(stream);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					stream));
			final int bufSize = 8192; // this is the default BufferredReader
			// buffer size
			StringBuilder sb = new StringBuilder(bufSize);
			char[] cbuf = new char[bufSize];
			while (true) {
				try {
					int chars = reader.read(cbuf);
					if (chars == -1) {
						break;
					}
					sb.append(cbuf, 0, chars);
				} catch (IOException e) {
					// when i/o error occurs, simply return intermediate result if any
					if (sb.length()== 0) {
						return null;
					}
					return sb.toString();
				}
			}
			return sb.toString();
		} catch (IOException e) {
			// oh well, simply discard it
			return null;
		} finally {
			InternalUtils.close(stream);
		}
	}

	/**
	 * Set a header for basic authentication login.
	 */
	protected void setAuthentication(URLConnection connection, String name,
			String password) {
		if (name==null || password==null) {
			// You probably want to use OAuthSignpostClient!
			throw new TwitterException.E401("Authentication requested but no authorisation details are set!");
		}
		String token = name + ":" + password;
		String encoding = Base64Encoder.encode(token);
		// Hack for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6459815
		encoding = encoding.replace("\r\n", "");
		connection.setRequestProperty("Authorization", "Basic " + encoding);
	}

	/**
	 * Use this to protect your Twitter API rate-limit. E.g. if you want to keep
	 * some credit in reserve for core activity. 0 by default. 
	 * this http client object will start pre-emptively throwing rate-limit
	 * exceptions when it gets down to the specified level.
	 */
	public void setMinRateLimit(int minRateLimit) {
		this.minRateLimit = minRateLimit;
	}

	/**
	 * False by default. Setting this to true switches on a robustness
	 * workaround: when presented with a 50X server error, the system will wait
	 * 1/2 a second and make a second attempt.
	 */
	@Override
	public void setRetryOnError(boolean retryOnError) {
		this.retryOnError = retryOnError;
	}

	@Override
	public void setTimeout(int millisecs) {
		this.timeout = millisecs;
	}

	@Override
	public String toString() {
		return getClass().getName() + "[name=" + name + ", password="
				+ (password == null ? "null" : "XXX") + "]";
	}

	/**
	 * {@link #processHeaders(HttpURLConnection)} MUST have been called first.
	 */
	void updateRateLimits(String resource) {
		if (resource==null) return;
		String limit = getHeader("X-Rate-Limit-Limit");		
		if (limit == null) {
			return;
		}
		String remaining = getHeader("X-Rate-Limit-Remaining");
		String reset = getHeader("X-Rate-Limit-Reset");
		rateLimits.put(resource, new RateLimit(limit, remaining, reset));		
	}

}
