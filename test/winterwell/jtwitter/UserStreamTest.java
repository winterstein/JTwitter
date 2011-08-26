package winterwell.jtwitter;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.Twitter.User;
import winterwell.utils.StrUtils;
import winterwell.utils.Utils;

public class UserStreamTest {

	/**
	 * ARGH! 
	 * This picks up mentions fine here in the test.
	 * BUT when tested live for @winterstein, it ONLY picks up mentions if the tweet
	 * starts "@winterstein...". Mid-tweet mentions get lost?!
	 * NB: This is reproducible using SoDash 
	 */
	@Test
	public void testRead() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		UserStream us = new UserStream(jtwit);
		us.setPreviousCount(100);
		us.setWithFollowings(false); // no need to hear what JTwitTest2 has to say		
		// -- unless it's too us
		us.setAutoReconnect(true);
		us.connect();
		
		// let's also try with a TwitterStream
		TwitterStream us2 = new TwitterStream(jtwit);
		us2.setTrackKeywords(Arrays.asList("@jtwit"));
		User me = jtwit.getSelf();
//		us2.setFollowUsers(Arrays.asList(me.id));
		us2.setAutoReconnect(true);
		us2.connect();
		
		// Do some stuff		
		int salt = new Random().nextInt(1000);
		Twitter jtwit2 = TwitterTest.newTestTwitter2();
		if ( ! jtwit.isFollowing(jtwit2.getScreenName())) {
			jtwit.follow(jtwit2.getScreenName());
		}
		if ( ! jtwit2.isFollowing("jtwit")) {
			jtwit2.follow("jtwit");
		}
		jtwit2.setStatus("Public hello to @jtwit "+salt);
		jtwit2.setStatus("@jtwit Public hello v2 "+salt);
		Status m = jtwit2.setStatus("Public shout by tester2 about monkeys: they're cute "+salt);
		jtwit2.sendMessage("jtwit", "Private hello to jtwit "+salt);
		
		jtwit.setStatus("@jtwittest2 Public hello from jtwit "+salt);
		jtwit.sendMessage("jtwittest2", "Private hello from jtwit "+salt);
		Status w = jtwit.setStatus("Public shout by tester1 about whales: they're big "+salt);
		
		// retweets
		jtwit2.retweet(w);
		jtwit.retweet(m);
		
		// Favorite
//		List<Status> ht = jtwit.getHomeTimeline();
//		Status s = ht.get(0);
//		jtwit.setFavorite(s, ! s.isFavorite());
				
		Twitter_Account ta = new Twitter_Account(jtwit);
//		ta.setProfile(null, null,
//				new String[]{
//				"UK", "Edinburgh", "I is in your twitters LOL", "Scotland"
//				}[(int)(System.currentTimeMillis() % 4)], null);
		Utils.sleep(2000);		
//		TwitterList tl = new TwitterList("StreamTest"+new Random().nextInt(1000), jtwit, true, "Just a test list");
//		tl.add(new User("winterstein"));
//		Utils.sleep(4000);
//		tl.remove(new User("winterstein"));		
		if (jtwit.isFollowing("winterstein")) {
			jtwit.stopFollowing("winterstein");
		} else {
			jtwit.follow("winterstein");
		}
		Utils.sleep(2000);
//		tl.delete();

//		List<TwitterEvent> sys = us.popSystemEvents();
		List<ITweet> tweets = us.popTweets();
		List<TwitterEvent> evs = us.popEvents();
		if ( ! tweets.isEmpty()) System.out.println(StrUtils.join(tweets,"\n"));
		if ( ! evs.isEmpty()) System.out.println(evs);
		
		List<TwitterEvent> sys2 = us2.popSystemEvents();
		List<ITweet> tweets2 = us2.popTweets();
		List<TwitterEvent> evs2 = us2.popEvents();
		if ( ! tweets2.isEmpty()) System.out.println(StrUtils.join(tweets2,"\n"));
		if ( ! evs2.isEmpty()) System.out.println(evs2);
		
		us.close();		
		us2.close();
	}

	
}
