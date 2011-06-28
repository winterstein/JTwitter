package winterwell.jtwitter;

import static org.junit.Assert.*;

import org.junit.Test;

import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.User;

public class TwitterAccountTest {

	@Test
	public void testVerifyCredentials() {
		{
			Twitter jtwit = TwitterTest.newTestTwitter();
			TwitterAccount ta = new TwitterAccount(jtwit);
			User testUser = ta.verifyCredentials();
			assert testUser != null;
		}
		try {
			IHttpClient client = new OAuthSignpostClient("a", "b", "c", "d");
			Twitter jtwit = new Twitter(null, client);
			TwitterAccount ta = new TwitterAccount(jtwit);
			User testUser = ta.verifyCredentials();
			fail();
		} catch (TwitterException.E401 e) {
			// correct
		}
	}

	@Test
	public void testGetAccessLevel() {
		{
			Twitter jtwit = TwitterTest.newTestTwitter();
			TwitterAccount ta = new TwitterAccount(jtwit);
			Object al = ta.getAccessLevel();
			System.out.print(al);
		}
		{
			IHttpClient client = new OAuthSignpostClient("a", "b", "c", "d");
			Twitter jtwit = new Twitter(null, client);
			TwitterAccount ta = new TwitterAccount(jtwit);
			Object al = ta.getAccessLevel();
		}
	}

}
