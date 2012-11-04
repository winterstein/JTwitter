package winterwell.jtwitter;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class Twitter_UsersStressTest {

	/**
	 * Test the cursor-based API for getting many followers.
	 * Slightly intermittent
	 */
	@Test
	public void testGetManyFollowers() {
		Twitter tw = TwitterTest.newTestTwitter();
		tw.setMaxResults(10000); // we don't want to run the test for ever.
		String victim = "psychovertical";
		User user = tw.getUser(victim);
		assert user.followersCount < 10000 : "More than 10000 followers; choose a different victim or increase the maximum results";
		Set<User> followers = new HashSet(tw.getFollowers(victim));
		Set<Long> followerIDs = new HashSet(tw.getFollowerIDs(victim));
		// psychovertical has about 600 followers, as of 14/12/09
		assert user.followersCount == followers.size();
		assert user.followersCount == followerIDs.size();
	}

}
