package winterwell.jtwitter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import oauth.signpost.exception.OAuthException;

/**
 * A client that fails a lot -- used to test error handling.
 * @author daniel
 *
 */
public class BadHttpClient extends OAuthSignpostClient {
	
	/**
	 * Probability that any given connect will fail.
	 * Set to 1 for normal connections, 0 for guaranteed failure
	 */
	public double P_SUCCESS = 0.5;
	
	/**
	 * Max milliseconds that a connection can live (for testing streaming)
	 */
	public long MAX_UPTIME = 1000*60;

	@Override
	public HttpURLConnection connect(String url, Map<String, String> vars,
			boolean authenticate) throws IOException 
	{
		if (badLuck()) {
			throw (IOException)exception;
		}
		HttpURLConnection con = super.connect(url, vars, authenticate);
		setTimeout(con);
		return con;
	}
	
	/**
	 * Exception to throw when being difficult.
	 */
	public Exception exception = new IOException("fail");

	public BadHttpClient(String consumerKey, String consumerSecret, String callback) {
		super(consumerKey, consumerSecret, callback);
	}
	
	@Override
	public HttpURLConnection post2_connect(String uri, Map<String, String> vars)
			throws IOException, OAuthException 
	{
		if (badLuck()) {
			if (exception instanceof OAuthException) throw (OAuthException)exception;
			throw (IOException)exception;
		}
		HttpURLConnection con = super.post2_connect(uri, vars);
		setTimeout(con);
		return con;
	}
	
	static Timer timer = new Timer("BadHttpClientTimer");
	
	private void setTimeout(final HttpURLConnection con) {
		timer.schedule(new TimerTask() {			
			@Override
			public void run() {
				try {
					InternalUtils.close(con.getInputStream());
					InternalUtils.close(con.getOutputStream());
				} catch (Exception e) {
				}
			}
		}, MAX_UPTIME);
	}

	Random rnd = new Random();
	
	private boolean badLuck() {
		return rnd.nextDouble() < P_SUCCESS;
	}

	public BadHttpClient(String consumerKey, String consumerSecret,
			String accessToken, String accessTokenSecret) 
	{
		super(consumerKey, consumerSecret, accessToken, accessTokenSecret);
	}

	private static final long serialVersionUID = 1L;

	
}
