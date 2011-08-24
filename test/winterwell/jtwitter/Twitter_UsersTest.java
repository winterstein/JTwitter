package winterwell.jtwitter;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import winterwell.jtwitter.Twitter.User;

public class Twitter_UsersTest {

	@Test
	public void testGetRelationshipInfo() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter_Users tu = new Twitter_Users(jtwit);
		List<User> users = tu.getRelationshipInfo(Arrays.asList("winterstein", "spoonmcguffin", "stephenfry", "jtwittest2"));
		User w = users.get(users.indexOf(new User("winterstein")));
		User jtwit2 = users.get(users.indexOf(new User("jtwittest2")));
		User fry = users.get(users.indexOf(new User("stephenfry")));
		
		boolean jtwitFollowsWinterstein = jtwit.isFollowing("winterstein");
		boolean jtwitFollowsFry = jtwit.isFollowing("stephenfry");
		if (!jtwitFollowsFry) {
			jtwit.follow("stephenfry");
		}
		User fryb = jtwit.show("stephenfry");
		assert fry.isFollowedByYou();
		assert fryb.isFollowedByYou();
		assert ! fry.isFollowingYou();
		assert ! fryb.isFollowingYou();		
	}

}
