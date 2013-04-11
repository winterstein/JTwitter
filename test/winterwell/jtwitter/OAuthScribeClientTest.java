package winterwell.jtwitter;

import java.util.List;

import org.scribe.model.Token;





public class OAuthScribeClientTest 
//extends TestCase
{

	public void testSimple() {
		OAuthScribeClient client = new OAuthScribeClient(OAuthScribeClient.JTWITTER_OAUTH_KEY, OAuthScribeClient.JTWITTER_OAUTH_SECRET, "oob");
		Twitter jtwit = new Twitter(TwitterTest.TEST_USER, client);
		// open the authorisation page in the user's browser
		client.authorizeDesktop();
		// get the pin
		String v = OAuthScribeClient.askUser("Please enter the verification PIN from Twitter");
		client.setAuthorizationCode(v);	
		// use the API!
		// This works
		assert jtwit.isValidLogin();
		// This works
		List<Message> dms = jtwit.getDirectMessages();
		// This fails with a 401?! - nope now it passes
		jtwit.setStatus("Using OAuth to tweet");
	}
	
	public void testRecreatingScribe() {
		OAuthScribeClient client = new OAuthScribeClient(OAuthScribeClient.JTWITTER_OAUTH_KEY, OAuthScribeClient.JTWITTER_OAUTH_SECRET, "oob");
		// open the authorisation page in the user's browser
		client.authorizeDesktop();
		// get the pin
		String v = OAuthScribeClient.askUser("Please enter the verification PIN from Twitter");
		client.setAuthorizationCode(v);		
		Token token = client.getAccessToken();
		
		// remake client
		OAuthScribeClient client2 = new OAuthScribeClient(OAuthScribeClient.JTWITTER_OAUTH_KEY, OAuthScribeClient.JTWITTER_OAUTH_SECRET, token);		
		Twitter jtwit2 = new Twitter(TwitterTest.TEST_USER, client2);
		
		// use the API!
		assert jtwit2.isValidLogin();
		jtwit2.setStatus("Using reconstituted OAuth to tweet");
	}
	
}
