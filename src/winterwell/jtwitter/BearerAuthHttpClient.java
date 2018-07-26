package winterwell.jtwitter;

import java.net.URLConnection;

/**
 *	For use with Twitter API methods which require application-level authentication via bearer token.
 *	https://developer.twitter.com/en/docs/basics/authentication/api-reference/token.html
 *	Use the application key and secret to request a bearer token from https://api.twitter.com/oauth2/token
 *	(or use a previously cached token).
 *	Call setBearerToken(String) before use, and call GET/POST/DELETE methods with arg "authentication" == true.
 * @author roscoe
 *
 */
public class BearerAuthHttpClient extends URLConnectionHttpClient {

	private String bearerToken;
	
	public void setBearerToken(String bearerToken) {
		this.bearerToken = bearerToken;
	}
	
	/**
	 * Set a header for basic authentication login.
	 */
	@Override
	protected void setAuthentication(URLConnection connection) {
		if (bearerToken == null) {
			throw new TwitterException.E401("Bearer token requested but no token provided!");
		}
		connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
	}
}
