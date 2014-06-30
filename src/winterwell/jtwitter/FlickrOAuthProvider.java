package winterwell.jtwitter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.basic.HttpURLConnectionRequestAdapter;
import oauth.signpost.http.HttpRequest;

public class FlickrOAuthProvider extends DefaultOAuthProvider {

	public FlickrOAuthProvider(String requestTokenEndpointUrl,
			String accessTokenEndpointUrl, String authorizationWebsiteUrl) {
		super(requestTokenEndpointUrl, accessTokenEndpointUrl, authorizationWebsiteUrl);
	}

	@Override
	protected HttpRequest createRequest(String endpointUrl) throws MalformedURLException,
    IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(endpointUrl).openConnection();
		connection.setRequestMethod("GET");
		connection.setAllowUserInteraction(false);
		connection.setRequestProperty("Content-Length", "0");
		return new HttpURLConnectionRequestAdapter(connection);
	}
	
}
