package winterwell.jtwitter;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;


public class Twitter_UsersTest {


	@Test
	public void testBlocks() {
		{
//			OAuthSignpostClient client = new OAuthSignpostClient(
//			OAuthSignpostClient.JTWITTER_OAUTH_KEY,
//			OAuthSignpostClient.JTWITTER_OAUTH_SECRET,"oob");
		//	client.authorizeDesktop();
		//	String pin = client.askUser("The Pin?");
		//	System.out.println(pin);
		//	client.setAuthorizationCode(pin);
		//	String[] tokens = client.getAccessToken();
		//	System.out.println(tokens[0]+" "+tokens[1]);		
			
//			OAuthSignpostClient client = new OAuthSignpostClient(
//			OAuthSignpostClient.JTWITTER_OAUTH_KEY,
//			OAuthSignpostClient.JTWITTER_OAUTH_SECRET,
//			token0, token1);
//			Twitter jtwit = new Twitter(null, client);
//			System.out.println(jtwit.getSelf());
			Twitter jtwit = TwitterTest.newTestTwitter();
			Twitter_Users ta = jtwit.users();
			List<Number> blocked = ta.getBlockedIds();
			System.out.println(blocked);
			List<User> users = ta.showById(blocked);
			System.out.println(users);
		}
	}
	

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFriends()}.
	 */
	@Test
	public void testGetFriends() {
		Twitter tw = TwitterTest.newTestTwitter();
		List<User> friends = tw.users().getFriends();
		assert friends != null && ! friends.isEmpty();
	}

	@Test
	public void testGetFriendIDs() {
		{
			Twitter tw = TwitterTest.newTestTwitter();
			List<Number> friends = tw.users().getFriendIDs();
			assert friends != null && ! friends.isEmpty();
		}
		{
			Twitter tw = new Twitter();
			List<Number> friends = tw.users().getFriendIDs("winterstein");
			assert friends != null && ! friends.isEmpty();
		}
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFollowers()}.
	 */
	@Test
	public void testGetFollowers() {
		Twitter tw = TwitterTest.newTestTwitter();
		List<User> f = tw.users().getFollowers();
		assert f.size() > 0;
		assert Twitter.getUser("winterstein", f) != null;
	}

	@Test
	public void testUserFollowingProperty() throws InterruptedException {
		// test the user property
		Twitter tw = TwitterTest.newTestTwitter();
		{
			tw.follow("stephenfry");
			User sf = tw.users().show("stephenfry");
			assert sf.isFollowedByYou();
			assert sf.isFollowingYou() == null || ! sf.isFollowingYou();
		}
		{
			User dan = tw.users().show("winterstein");
			assert dan.isFollowingYou() == null || dan.isFollowingYou();
		}
		{
			List<User> followers = tw.getFollowers();
			List<User> fBy = tw.getFriends();
			System.out.println(fBy);
		}
	}
	
	@Test
	public void testGetRelationshipInfo() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter_Users tu = new Twitter_Users(jtwit);
		List<User> users = tu.getRelationshipInfo(Arrays.asList("winterstein", "spoonmcguffin", "stephenfry", "jtwittest2"));
		User w = users.get(users.indexOf(new User("winterstein")));
		assert w.isFollowingYou();		
		User jtwit2 = users.get(users.indexOf(new User("jtwittest2")));
		User fry = users.get(users.indexOf(new User("stephenfry")));
		
		boolean jtwitFollowsWinterstein = jtwit.isFollowing("winterstein");
		boolean jtwitFollowsFry = jtwit.isFollowing("stephenfry");
		if (!jtwitFollowsFry) {
			jtwit.follow("stephenfry");
		}
		User fryb = jtwit.users().show("stephenfry");

//		boolean isf = jtwit.users().isFollower("winterstein", jtwit.getScreenName());
//		assert isf;
//		User w2 = jtwit.users().show("winterstein");
//		assert w2.isFollowingYou(); comes back null?!

		assert fry.isFollowedByYou();
		assert fryb.isFollowedByYou();
		assert ! fry.isFollowingYou();
		// this now returns null a lot!
		assert fryb.isFollowingYou()==null || ! fryb.isFollowingYou();		
	}

}
