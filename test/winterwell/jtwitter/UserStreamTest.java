package winterwell.jtwitter;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.Twitter.User;
import winterwell.utils.Utils;

public class UserStreamTest {

	@Test
	public void testRead() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		UserStream us = new UserStream(jtwit.getHttpClient());
		us.setPreviousCount(100);
		us.setWithFollowings(true);
		us.connect();
		// Do some stuff
		
		// Favorite
//		List<Status> ht = jtwit.getHomeTimeline();
//		Status s = ht.get(0);
//		jtwit.setFavorite(s, ! s.isFavorite());
		
//		jtwit.setStatus("streaming test "+new Random().nextInt(1000));
		
		// TODO nothing in the stream?! Are the auth keys OK?
		jtwit.sendMessage("winterstein", "Please ignore streamed message "+new Random().nextInt(1000));
		
		TwitterAccount ta = new TwitterAccount(jtwit);
//		ta.setProfile(null, null,
//				new String[]{
//				"UK", "Edinburgh", "I is in your twitters LOL", "Scotland"
//				}[(int)(System.currentTimeMillis() % 4)], null);
		Utils.sleep(2000);		
		TwitterList tl = new TwitterList("StreamTest"+new Random().nextInt(1000), jtwit, true, "Just a test list");
//		tl.add(new User("winterstein"));
//		Utils.sleep(4000);
//		tl.remove(new User("winterstein"));
		
		if (jtwit.isFollowing("winterstein")) {
			jtwit.stopFollowing("winterstein");
		} else {
			jtwit.follow("winterstein");
		}
		Utils.sleep(4000);
		tl.delete();

		while(true) {
			List<ITweet> tweets = us.popTweets();
			List<TwitterEvent> evs = us.popEvents();
			if (! tweets.isEmpty()) System.out.println(tweets);
			if (! evs.isEmpty()) System.out.println(evs);
		}		
	}

}
