package winterwell.jtwitter;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.utils.gui.GuiUtils;

public class Twitter_AccountTest {


	@Test
	public void testVerifyCredentials() {
		{
			Twitter jtwit = TwitterTest.newTestTwitter();
			Twitter_Account ta = new Twitter_Account(jtwit);
			User testUser = ta.verifyCredentials();
			System.out.println(testUser);
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
	
	@Test(timeout=120000)
	public void testUpdateProfileImage() {
		{
			Twitter jtwit = TwitterTest.newTestTwitter();
			Twitter_Account ta = new Twitter_Account(jtwit);
			File file = GuiUtils.selectFile("Pick an image", null);
			User u = ta.setProfileImage(file);
			System.out.println(u.getName()+" "+u.getProfileImageUrl());
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
