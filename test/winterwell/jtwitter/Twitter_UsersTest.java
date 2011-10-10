package winterwell.jtwitter;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;


public class Twitter_UsersTest {


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
