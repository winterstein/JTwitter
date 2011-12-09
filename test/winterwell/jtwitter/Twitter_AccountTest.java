package winterwell.jtwitter;

import static org.junit.Assert.fail;

import org.junit.Test;

import winterwell.jtwitter.Twitter.IHttpClient;

public class Twitter_AccountTest {


	@Test
	public void testVerifyCredentials() {
		{
			Twitter jtwit = TwitterTest.newTestTwitter();
			Twitter_Account ta = new Twitter_Account(jtwit);
			User testUser = ta.verifyCredentials();
			assert testUser != null;
		}
		try {
			IHttpClient client = new OAuthSignpostClient("a", "b", "c", "d");
			Twitter jtwit = new Twitter(null, client);
			Twitter_Account ta = new Twitter_Account(jtwit);
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
			Twitter_Account ta = new Twitter_Account(jtwit);
			Object al = ta.getAccessLevel();
			System.out.print(al);
		}
		{
			IHttpClient client = new OAuthSignpostClient("a", "b", "c", "d");
			Twitter jtwit = new Twitter(null, client);
			Twitter_Account ta = new Twitter_Account(jtwit);
			Object al = ta.getAccessLevel();
		}
	}
	
	
}
