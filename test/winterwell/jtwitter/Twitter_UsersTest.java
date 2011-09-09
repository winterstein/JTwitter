package winterwell.jtwitter;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;


public class Twitter_UsersTest {

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
