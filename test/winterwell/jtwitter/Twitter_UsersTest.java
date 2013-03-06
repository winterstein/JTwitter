package winterwell.jtwitter;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import winterwell.json.JSONException;
import winterwell.jtwitter.TwitterException.E401;
import winterwell.jtwitter.TwitterException.E403;
import winterwell.jtwitter.TwitterException.E404;
import winterwell.jtwitter.TwitterException.SuspendedUser;


public class Twitter_UsersTest {


	/**
	 * This tested a bug in {@link OAuthSignpostClient}
	 * @throws InterruptedException
	 */
//	@Test
	public void tstFollowFollow() throws InterruptedException {
		int lag = 2000; //300000;
		OAuthSignpostClient client = new OAuthSignpostClient(
				OAuthSignpostClient.JTWITTER_OAUTH_KEY, OAuthSignpostClient.JTWITTER_OAUTH_SECRET, "oob");
		Twitter tw = new Twitter("forkmcguffin", client);
		// open the authorisation page in the user's browser
		client.authorizeDesktop();
		// get the pin
		String v = client.askUser("Please enter the verification PIN from Twitter");
		client.setAuthorizationCode(v);

		User u = tw.follow("winterstein");

		Thread.sleep(lag);

		User u2 = tw.follow("winterstein");
	}
	

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFriends(java.lang.String)}.
	 */
	@Test
	public void testGetFriendsString() {
		Twitter tw = TwitterTest.newTestTwitter();
		List<User> friends = tw.getFriends("winterstein");
		assert friends != null;
	}
	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFriendsTimeline()}.
	 */
	@Test
	public void testGetFriendsTimeline() {
		Twitter tw = TwitterTest.newTestTwitter();
		List<Status> ft = tw.getFriendsTimeline();
		assert ft.size() > 0;
	}


	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#follow(java.lang.String)}.
	 */
	@Test
	public void testFollowAndStopFollowing() throws InterruptedException {
		int lag = 1000; //300000;
		Twitter tw = TwitterTest.newTestTwitter();
		tw.flush();
		List<User> friends = tw.users().getFriends();
		if ( ! tw.users().isFollowing("winterstein")) {
			tw.users().follow("winterstein");
			Thread.sleep(lag);
		}
		assert tw.isFollowing("winterstein") : friends;

		// Stop
		User h = tw.users().stopFollowing("winterstein");
		assert h != null;
		Thread.sleep(lag);
		assert ! tw.users().isFollowing("winterstein") : friends;

		// break where no friendship exists
		User h2 = tw.users().stopFollowing("winterstein");
		assert h2==null;

		// Follow
		tw.users().follow("winterstein");
		Thread.sleep(lag);
		assert tw.users().isFollowing("winterstein") : friends;

		try {
			User suspended = tw.users().follow("Alysha6822");
			assert false : "Trying to follow a suspended user should throw an exception";
		} catch (TwitterException e) {
		}
	}

	@Test
	public void testSuspendedAccounts() throws JSONException {
		Twitter tw = TwitterTest.newTestTwitter();
		try {
			User leo = tw.show("lottoeurooffers");
			System.out.println(leo);
		} catch (Exception e) {
			System.out.println(e);
		}
		try {
			tw.users().show("ykarya35a4wr");
		} catch (SuspendedUser e) {
		} catch (E404 e) {
		}
		List<User> users = tw.bulkShow(Arrays.asList("winterstein", "ykarya35a4wr"));
		assert ! users.isEmpty();
		try {
			tw.users().isFollowing("ykarya35a4wr");
		} catch (SuspendedUser e) {
		} catch (E404 e) {
		}
		try {
			tw.users().follow("ykarya35a4wr");
		} catch (SuspendedUser e) {
		} catch (E404 e) {
		}
		try {
			tw.users().stopFollowing("ykarya35a4wr");
		} catch (SuspendedUser e) {
		} catch (E404 e) {
		}
		try {
			tw.getUserTimeline("ykarya35a4wr");
		} catch (SuspendedUser e) {
		} catch (E404 e) {
		}
	}

	@Test
	public void testProtectedAccounts() {
		Twitter tw = TwitterTest.newTestTwitter();
		try {
			tw.show("acwright");
		} catch (SuspendedUser e) {
			assert false;
		} catch (E403 e) {
		}
		try {
			tw.isFollowing("acwright");
		} catch (SuspendedUser e) {
			assert false;
		} catch (E403 e) {
		}
		try {
			tw.isFollower("acwright", "stephenfry");
		} catch (SuspendedUser e) {
			assert false;
		} catch (E403 e) {
		}
		try {
			tw.getUserTimeline("acwright");
		} catch (SuspendedUser e) {
			assert false;
		} catch (E403 e) {
		} catch (E401 e) {
		}
	}


	@Test
	public void testDeletedUser() {
		Twitter tw = TwitterTest.newTestTwitter();
		// NB Once Twitter delete an account, it will 404 (rather than 403)
		try {
			tw.show("radio_kulmbach");
			assert false;
		} catch (TwitterException.SuspendedUser ex) {
			// OK
		} catch (TwitterException.E404 ex) {
			// OK
		}
	}

	@Test
	public void testSearchUsers() {
		Twitter tw = TwitterTest.newTestTwitter();

		List<User> users = tw.searchUsers("Nigel Griffiths");
		System.out.println(users);

		// AND Doesn't work!
		List<User> users2 = tw.searchUsers("Fred near:Scotland");
		assert ! users.isEmpty();
	}
	

	@Test
	public void testNotifications() {
		Twitter tw = TwitterTest.newTestTwitter();
		User user = tw.users().leaveNotifications("jtwit2");
		System.out.println(user);
	}


	@Test
	public void testBulkShow() {
		Twitter tw = TwitterTest.newTestTwitter();
		List<User> users = tw.users().show(Arrays.asList(
				"winterstein", "joehalliwell", "annettemees", "bogus1yudsah"));
		System.out.println(users);
		assert users.size() == 3 : users; // no bogus user
		assert users.get(1).description != null;		
	}
	
	@Test
	public void testBulkShowAllBogus() {
		Twitter tw = TwitterTest.newTestTwitter();
		List<User> users = tw.users().show(Arrays.asList(
				"bogus1gyuvjwu", "bogus2yudsah"));
		System.out.println(users);
		assert users.isEmpty() : users; // no bogus user		
		
		List<User> users2 = tw.users().showById(Arrays.asList(1, 2));
		System.out.println(users2);
		assert users2.isEmpty() : users2; // no bogus user
	}

	@Test
	public void testBulkShowById() {
		Twitter tw = TwitterTest.newTestTwitter();
		List<Long> userIds = Arrays.asList(32L, 34L, 45L, 12435562L);
		List<User> users = tw.bulkShowById(userIds);
		assert users.size() == 2 : users;
	}

	
	@Test
	public void testShowBulk() {
		{	// a small bulk!
			Twitter tw = TwitterTest.newTestTwitter();
			List<User> users = tw.users().show(Arrays.asList("mcfc","winterstein"));
			for (User user : users) {
				System.out.println(user.getScreenName()+"\t"+user.getLocation()+"\t"+user.getPlace()+"\t"+user.getId());
			}
		}
		{	// anonymous -- only in version 1			
			Twitter tw = new Twitter();
			tw.setAPIRootUrl("http://api.twitter.com/1");
			List<User> users = tw.users().show(Arrays.asList("joehalliwell","winterstein"));
			for (User user : users) {
				System.out.println(user.getScreenName()+"\t"+user.getLocation()+"\t"+user.getPlace()+"\t"+user.getId());
			}
		}
	}
	

	@Test
	public void testShowById() {
		{
			Twitter tw = TwitterTest.newTestTwitter();
			List<Long> userIds = Arrays.asList(14573900L, 6663112L);
			List<User> users = tw.users().showById(userIds);
			for (User user : users) {
				System.out.println(user.getScreenName()+"\t"+user.getLocation()+"\t"+user.getPlace()+"\t"+user.getId());
			}
		}
	}

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
		Twitter tw = TwitterTest.newTestTwitter();
		{
			List<Number> friends = tw.users().getFriendIDs();
			assert friends != null && ! friends.isEmpty();
		}
		{
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
	public void testIsFollower() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter_Users tu = new Twitter_Users(jtwit);
		boolean jtwit_follows_fry = tu.isFollowing("stephenfry");
		boolean fry_follows_jtwit = tu.isFollower("stephenfry");
		assert ! fry_follows_jtwit;
		assert jtwit_follows_fry;
	}
	
	@Test
	public void testGetRelationshipInfo() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter_Users tu = new Twitter_Users(jtwit);
		List<User> users = tu.getRelationshipInfo(Arrays.asList("winterstein", "spoonmcguffin", "stephenfry", "jtwittest2"));
		assert ! users.isEmpty();
		
		User w = users.get(users.indexOf(new User("winterstein")));		
		assert w.isFollowingYou();		
		boolean jtwitFollowsWinterstein = jtwit.isFollowing("winterstein");
		
		User jtwit2 = users.get(users.indexOf(new User("jtwittest2")));
		
		User fry = users.get(users.indexOf(new User("stephenfry")));				
		boolean jtwitFollowsFry = jtwit.isFollowing("stephenfry");
		if ( ! jtwitFollowsFry) {
			jtwit.follow("stephenfry");
		}
		User fryb = jtwit.users().show("stephenfry");

		assert fry.isFollowedByYou();
		assert fryb.isFollowedByYou();
		assert ! fry.isFollowingYou();
		
		// this now returns null a lot!
		assert fryb.isFollowingYou()==null || ! fryb.isFollowingYou();		
	}

}
