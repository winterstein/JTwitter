package winterwell.jtwitter;

public class ExampleCodeTest {

	public static void main(String[] args) {
		// 1. Get authorised
		// On Android: use AndroidTwitterLogin
		// On the desktop...
		// Make an oauth client (you'll want to change this bit)
		OAuthSignpostClient oauthClient = new OAuthSignpostClient(OAuthSignpostClient.JTWITTER_OAUTH_KEY, 
				OAuthSignpostClient.JTWITTER_OAUTH_SECRET, "oob");
		// Open the authorisation page in the user's browser. On a desktop, we can do that like this:
		oauthClient.authorizeDesktop();
		// get the pin
		String v = oauthClient.askUser("Please enter the verification PIN from Twitter");
		oauthClient.setAuthorizationCode(v);
		// Store the authorisation token details for future use
		String[] accessToken = oauthClient.getAccessToken();
		// Next time we can use new OAuthSignpostClient(OAUTH_KEY, OAUTH_SECRET, 
//		      accessToken[0], accessToken[1]) to avoid authenticating again.

		// 2. Make a Twitter object
		Twitter twitter = new Twitter("my-name", oauthClient);
		// Print Daniel Winterstein's status
		System.out.println(twitter.getStatus("winterstein"));
		// Set my status
		twitter.setStatus("Messing about in Java");
	}
}
