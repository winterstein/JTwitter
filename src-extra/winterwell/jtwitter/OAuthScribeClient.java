package winterwell.jtwitter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import winterwell.json.JSONException;
import winterwell.json.JSONObject;
import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.KRequestType;

/**
 * It is recommended that you use {@link OAuthSignpostClient} instead. OAuth
 * based login using Scribe (http://github.com/fernandezpablo85/scribe). <i>You
 * need version 1.x of Scribe!</i>
 * <p>
 * Example Usage (desktop based):
 * 
 * <pre>
 * <code>
 * 	OAuthScribeClient client = new OAuthScribeClient(JTWITTER_OAUTH_KEY, JTWITTER_OAUTH_SECRET, "oob");
 * 	Twitter jtwit = new Twitter("yourtwittername", client);
 * 	// open the authorisation page in the user's browser
 * 	client.authorizeDesktop();
 * 	// get the pin
 * 	String v = client.askUser("Please enter the verification PIN from Twitter");
 * 	client.setAuthorizationCode(v);	
 * 	// use the API!
 * 	jtwit.setStatus("Messing about in Java");
 *  	</code>
 * </pre>
 * 
 * @author daniel
 * 
 *         <p>
 *         There are alternative OAuth libraries you can use:
 * @see OAuthSignpostClient This is the "officially supported" JTwitter OAuth
 *      client.
 * @see OAuthHttpClient
 */
public class OAuthScribeClient implements IHttpClient {

	@Override
	public boolean isRetryOnError() {
		return false;
	}
	/**
	 * This consumer key (and secret) allows you to get up and running fast.
	 * However you are strongly advised to register your own app at
	 * http://dev.twitter.com Then use your own key and secret. This will be
	 * less confusing for users, and it protects you incase the JTwitter key
	 * gets changed.
	 */
	public static final String JTWITTER_OAUTH_KEY = "Cz8ZLgitPR2jrQVaD6ncw";

	/**
	 * For use with {@link #JTWITTER_OAUTH_KEY}
	 */
	public static final String JTWITTER_OAUTH_SECRET = "9FFYaWJSvQ6Yi5tctN30eN6DnXWmdw0QgJMl7V6KGI";

	/**
	 * <p>
	 * <i>Convenience method for desktop apps only - does not work in
	 * Android</i>
	 * </p>
	 * 
	 * Opens a popup dialog asking the user to enter the verification code. (you
	 * would then call {@link #setAuthorizationCode(String)}). This is only
	 * relevant when using out-of-band instead of a callback-url. This is a
	 * convenience method -- you will probably want to build your own UI around
	 * this.
	 * <p>
	 * <i>This method requires Swing. It will not work on all devices.</i>
	 * 
	 * @param question
	 *            e.g. "Please enter the authorisation code from Twitter"
	 * @return
	 */
	public static String askUser(String question) {
		// This cumbersome approach avoids importing Swing classes
		// It will create a runtime exception on Android
		// -- but will allow the rest of the class to be used.
		// JOptionPane.showInputDialog(question);
		try {
			Class<?> JOptionPaneClass = Class
					.forName("javax.swing.JOptionPane");
			Method showInputDialog = JOptionPaneClass.getMethod(
					"showInputDialog", Object.class);
			return (String) showInputDialog.invoke(null, question);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("deprecation")
	private static String encode(Object x) {
		return URLEncoder.encode(String.valueOf(x));
	}

	private Token accessToken;

	private String callbackUrl;

	private String consumerKey;
	private String consumerSecret;
	
//	private final Map<KRequestType, RateLimit> rateLimits = new EnumMap<KRequestType, RateLimit>(KRequestType.class);
	
	private Token requestToken;
	private boolean retryingFlag;
	private boolean retryOnError;
	private OAuthService scribe;
	// TODO use this!
	private int timeout;

	/**
	 * 
	 * @param consumerKey
	 * @param consumerSecret
	 * @param callbackUrl
	 *            Servlet that will get the verifier sent to it, or "oob" for
	 *            out-of-band (user copies and pastes the pin /** Opens a popup
	 *            dialog asking the user to enter the verification code. (you
	 *            would then call {@link #setAuthorizationCode(String)}). This
	 *            is only relevant when using out-of-band instead of a
	 *            callback-url. This is a convenience method -- you will
	 *            probably want to build your own UI around this.
	 * 
	 * @param question
	 *            e.g. "Please enter the authorisation code from Twitter"
	 * @return
	 */
	public OAuthScribeClient(String consumerKey, String consumerSecret,
			String callbackUrl) {
		assert consumerKey != null && consumerSecret != null
				&& callbackUrl != null;
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.callbackUrl = callbackUrl;
		init();
	}

	/**
	 * Use this if you already have an accessToken for the user. You can then go
	 * straight to using the API without having to authorise again.
	 * 
	 * @param consumerKey
	 * @param consumerSecret
	 * @param accessToken
	 */
	public OAuthScribeClient(String consumerKey, String consumerSecret,
			Token accessToken) {
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.accessToken = accessToken;
		init();
	}

	/**
	 * Redirect the user's browser to Twitter's authorise page. You will need to
	 * collect the verifier pin - either from the callback servlet, or from the
	 * user (out-of-band).
	 * <p>
	 * <i>This method requires Swing. It will not work on all devices.</i>
	 * 
	 * @see #authorizeUrl()
	 */
	public void authorizeDesktop() {
		URI uri = authorizeUrl();
		try {
			// This cumbersome approach avoids importing Swing classes
			// It will create a runtime exception on Android
			// -- but will allow the rest of the class to be used.
			// Desktop d = Desktop.getDesktop();
			Class<?> desktopClass = Class.forName("java.awt.Desktop");
			Method getDesktop = desktopClass.getMethod("getDesktop");
			Object d = getDesktop.invoke(null);
			// d.browse(uri);
			Method browse = desktopClass.getMethod("browse", URI.class);
			browse.invoke(d, uri);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return url to direct the user to for authorisation.
	 */
	public URI authorizeUrl() {
		try {
			requestToken = scribe.getRequestToken();
			String url = "https://api.twitter.com/oauth/authorize?oauth_token="
					+ requestToken.getToken()
			// +"&oauth_callback="+callbackUrl
			;
			return new URI(url);
		} catch (URISyntaxException e) {
			throw new TwitterException(e);
		}
	}

	@Override
	public boolean canAuthenticate() {
		return accessToken != null;
	}

	@Override
	public HttpURLConnection connect(String url, Map<String, String> vars,
			boolean b) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IHttpClient copy() {
		OAuthScribeClient c = new OAuthScribeClient(consumerKey,
				consumerSecret, accessToken);
		c.callbackUrl = callbackUrl;
		c.setTimeout(timeout);
		c.setRetryOnError(retryOnError);
		return c;
	}

	/**
	 * @return the access token, if set.
	 */
	public Token getAccessToken() {
		return accessToken;
	}

	/**
	 * TODO not implemented yet. Please see {@link URLConnectionHttpClient} for
	 * example code.
	 */
	@Override
	public String getHeader(String headerName) throws RuntimeException {
		throw new RuntimeException("TODO: not implemented yet");
	}

	@Override
	public String getPage(String uri, Map<String, String> vars,
			boolean authenticate) throws TwitterException {
		try {
			assert canAuthenticate();
			if (vars != null && vars.size() != 0) {
				uri += "?";
				for (Entry<String, String> e : vars.entrySet()) {
					if (e.getValue() == null) {
						continue;
					}
					uri += encode(e.getKey()) + "=" + encode(e.getValue())
							+ "&";
				}
			}
			OAuthRequest request = new OAuthRequest(Verb.GET, uri);
			// request.setTimeout(timeout);
			scribe.signRequest(accessToken, request);
			Response response = request.send();
			processError(response);
			return response.getBody();

			// retry on error?
		} catch (TwitterException.E50X e) {
			if (!retryOnError || retryingFlag)
				throw e;
			try {
				retryingFlag = true;
				Thread.sleep(1000);
				return getPage(uri, vars, authenticate);
			} catch (InterruptedException ex) {
				throw new TwitterException(ex);
			} finally {
				retryingFlag = false;
			}
		}
	}

	@Override
	public RateLimit getRateLimit(KRequestType reqType) {
		return null; //rateLimits.get(reqType);
	}
	
	/**
	 * @deprecated // TODO update for v1.1
	 */
	public Map<String, RateLimit> getRateLimits() {
		return Collections.EMPTY_MAP; //rateLimits;
	}

	/**
	 * @return the request token, if one has been created via
	 *         {@link #authorizeUrl()}.
	 */
	public Token getRequestToken() {
		return requestToken;
	}

	private void init() {
		/*
		Properties props = new Properties();
		// hard coded for efficiency & why not?
		// props.load(YahooEqualizer.class.getResourceAsStream("twitter.properties"));
		props.put("request.token.url", "http://twitter.com/oauth/request_token");
		props.put("access.token.verb", "POST");
		props.put("request.token.verb", "POST");
		props.put("access.token.url", "http://twitter.com/oauth/access_token");
		props.put("consumer.key", consumerKey);
		props.put("consumer.secret", consumerSecret);
		if (callbackUrl != null) {
			props.put("callback.url", callbackUrl);
		}
		 */
		ServiceBuilder serviceBuilder = new ServiceBuilder().apiKey(consumerKey).apiSecret(consumerSecret).provider(TwitterApi.class);
		if (callbackUrl != null) {
			serviceBuilder.callback(callbackUrl);
		}
		scribe = serviceBuilder.build();
	}

	@Override
	public String post(String uri, Map<String, String> vars,
			boolean authenticate) throws TwitterException {
		try {
			assert canAuthenticate();
			OAuthRequest request = new OAuthRequest(Verb.POST, uri);
			if (vars != null && vars.size() != 0) {
				for (Entry<String, String> e : vars.entrySet()) {
					if (e.getValue() == null) {
						continue;
					}					
					request.addBodyParameter(e.getKey(), e.getValue());
				}
			}
			// request.setTimeout(timeout);
			scribe.signRequest(accessToken, request);
			Response response = request.send();
			processError(response);
			return response.getBody();

			// retry on error?
		} catch (TwitterException.E50X e) {
			if (!retryOnError || retryingFlag)
				throw e;
			try {
				retryingFlag = true;
				Thread.sleep(1000);
				return getPage(uri, vars, authenticate);
			} catch (InterruptedException ex) {
				throw new TwitterException(ex);
			} finally {
				retryingFlag = false;
			}
		}
	}

	@Override
	public HttpURLConnection post2_connect(String uri, Map<String, String> vars)
			throws TwitterException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Throw an exception if the connection failed
	 * 
	 * @param response
	 */
	void processError(Response response) {
		int code = response.getCode();
		if (code == 200)
			return;
		Map<String, String> headers = response.getHeaders();
		String error = headers.get(null);
		if (code == 401)
			throw new TwitterException.E401(error);
		if (code == 403)
			throw new TwitterException.E403(error);
		if (code == 404)
			throw new TwitterException.E404(error);
		if (code >= 500 && code < 600)
			throw new TwitterException.E50X(error);
		boolean rateLimitExceeded = error.contains("Rate limit exceeded");
		if (rateLimitExceeded)
			throw new TwitterException.RateLimit(error);
		// Rate limiter can sometimes cause a 400 Bad Request
		if (code == 400) {
			String json = getPage(
					"http://twitter.com/account/rate_limit_status.json", null,
					true);
			try {
				JSONObject obj = new JSONObject(json);
				int hits = obj.getInt("remaining_hits");
				if (hits < 1)
					throw new TwitterException.RateLimit(error);
			} catch (JSONException e) {
				// oh well
			}
		}
		// just report it as a vanilla exception
		throw new TwitterException(code + " " + error);
	}

	/**
	 * Set the authorisation code (aka the verifier). This is only relevant when
	 * using out-of-band instead of a callback-url.
	 * 
	 * @param verifier
	 *            a pin code which Twitter gives the user
	 * @throws RuntimeException
	 *             Scribe throws an exception if the verifier is invalid
	 */
	public void setAuthorizationCode(String verifier) throws RuntimeException {
		accessToken = scribe.getAccessToken(requestToken, new Verifier(verifier));
	}

	/**
	 * False by default. Setting this to true switches on a robustness
	 * workaround: when presented with a 50X server error, the system will wait
	 * 1 second and make a second attempt. This is NOT thread safe.
	 */
	@Override
	public void setRetryOnError(boolean retryOnError) {
		this.retryOnError = retryOnError;
	}

	/**
	 * This does not do anything at present!
	 */
	@Deprecated
	@Override
	public void setTimeout(int millisecs) {
		this.timeout = millisecs;
	}

	// TODO can we call this whenever a RateLimit exception is thrown?
	public void updateRateLimits(KRequestType reqType) {
		if (true) return; // TODO update for v1.1
//		String limit = null, remaining = null, reset = null;
//		switch (reqType) {
//		case NORMAL:
//		case SHOW_USER:
//			limit = getHeader("X-RateLimit-Limit");
//			remaining = getHeader("X-RateLimit-Remaining");
//			reset = getHeader("X-RateLimit-Reset");
//			break;
//		case SEARCH:
//		case SEARCH_USERS:
//			limit = getHeader("X-FeatureRateLimit-Limit");
//			remaining = getHeader("X-FeatureRateLimit-Remaining");
//			reset = getHeader("X-FeatureRateLimit-Reset");
//			break;
//		}
//		if (limit != null) {
//			rateLimits.put(reqType, new RateLimit(limit, remaining, reset));
//		}
	}

}
