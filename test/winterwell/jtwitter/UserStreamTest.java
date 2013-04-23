package winterwell.jtwitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import winterwell.jtwitter.AStream.IListen;
import winterwell.jtwitter.Twitter.ITweet;
import winterwell.utils.Printer;
import winterwell.utils.StrUtils;
import winterwell.utils.Utils;
import winterwell.utils.time.TUnit;
import winterwell.utils.time.Time;

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
		Twitter jtwit2 = TwitterTest.newTestTwitter2();
		jtwit2.users().stopFollowing(jtwit.getSelf());
		Utils.sleep(200);
		
		UserStream us = new UserStream(jtwit);
		us.setPreviousCount(100);
		us.setWithFollowings(false); // no need to hear what JTwitTest2 has to say		
		// -- unless it's too us
		us.setAutoReconnect(true);
		us.connect();
		us.addListener(new IListen() {			
			@Override
			public boolean processTweet(ITweet tweet) {
				Printer.out("Heard: "+tweet);
				return true;
			}
			
			@Override
			public boolean processSystemEvent(Object[] obj) {
				Printer.out("Heard", obj);
				return true;
			}
			
			@Override
			public boolean processEvent(TwitterEvent event) {
				Printer.out("Heard: "+event);
				return true;
			}
		});
		
		// let's also try with a TwitterStream
		TwitterStream us2 = new TwitterStream(jtwit);
		us2.setTrackKeywords(Arrays.asList("@jtwit"));
		User me = jtwit.getSelf();
		us2.setFollowUsers(Arrays.asList(me.id));
		us2.setAutoReconnect(true);
		us2.connect();
		
		// Do some stuff		
		int salt = new Random().nextInt(1000);		
		if ( ! jtwit.isFollowing(jtwit2.getScreenName())) {
			jtwit.users().follow(jtwit2.getScreenName());
		}
		if ( ! jtwit2.isFollowing("jtwit")) {
			jtwit2.users().follow("jtwit");
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
		
		List<Object[]> sys2 = us2.popSystemEvents();
		List<ITweet> tweets2 = us2.popTweets();
		List<TwitterEvent> evs2 = us2.popEvents();
		if ( ! tweets2.isEmpty()) System.out.println(StrUtils.join(tweets2,"\n"));
		if ( ! evs2.isEmpty()) System.out.println(evs2);
		
		us.close();		
		us2.close();
	}

	/**
	 * Checks if UserStream gets mentions, 2 way due to the irregularites as shown in
	 * {@link winterwell.jtwitter.TwitterTest.#testSendMention2()}. This seems to
	 * work.
	 * @throws InterruptedException 
	 */
	@Test
	public void testUSGetMentions() throws InterruptedException {
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter jtwit2 = TwitterTest.newTestTwitter2();
		
		UserStream us = new UserStream(jtwit);
		us.setPreviousCount(100);
		us.setWithFollowings(false); // no need to hear what JTwitTest2 has to say		
		// -- unless it's too us
		us.setAutoReconnect(true);
		us.connect();
		
		UserStream us2 = new UserStream(jtwit2);
		us2.setPreviousCount(100);
		us2.setWithFollowings(false); // no need to hear what JTwitTest2 has to say		
		// -- unless it's too us
		us2.setAutoReconnect(true);
		us2.connect();
		
		//V simple test. Get jtwit2 to send 2 messages, see if it picks it up.
		String jtSName = jtwit.getSelf().screenName;
		String jt2SName = jtwit2.getSelf().screenName;
		Time time = new Time().plus(1, TUnit.HOUR);
		int salt = new Random().nextInt(100000);
		String messageText = "Cripes! This is UserST! " + salt + " "; 
		
		Status s = jtwit.setStatus("@" + jt2SName + " " + messageText + " from " + jtSName + " " + time);
		Thread.sleep(1000);
		System.out.println(s);

		Status s2 = jtwit.setStatus(messageText + "@" + jt2SName + " from " + jtSName + " " + time);
		Thread.sleep(1000);
		System.out.println(s2);

		String messageText2 = "Public mess This is UserST2! " + salt + " ";
		Status s3 = jtwit2.setStatus("@" + jtSName + " " + messageText2 + " from " + jt2SName + " " + time);
		Thread.sleep(1000);
		System.out.println(s3);

		Status s4 = jtwit2.setStatus(messageText2 + "@" + jtSName + " from " + jt2SName + " " + time);
		Thread.sleep(1000);
		System.out.println(s4);
		

		List<ITweet> tweets = us.popTweets();
		List<ITweet> tweets2 = us2.popTweets();
		//Both messages should appear here, for both users.
		
		System.out.println("\n"+jtSName+" found: "+tweets.size());
		System.out.println(tweets);
		System.out.println("\n"+jt2SName+" found: "+tweets2.size());
		System.out.println(tweets2);
		
		List missed1 = new ArrayList();
		List missed2 = new ArrayList();
		for(Status _s : new Status[]{s,s2,s3,s4}) {
			if ( ! tweets.contains(_s)) {
				missed1.add(_s);
			}
			if ( ! tweets2.contains(_s)) {
				missed2.add(_s);
			}
		}
		assert missed1.isEmpty() : missed1;
		assert missed2.isEmpty() : missed2;
		
		us.close();
		us2.close();
	}

	/**
	 * This shows that we don't get direct messages from UserStream
	 * @throws InterruptedException
	 */
	@Test
	public void testUSGetDMs() throws InterruptedException {
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter jtwit2 = TwitterTest.newTestTwitter2();
		UserStream us = new UserStream(jtwit);
		us.setPreviousCount(0);
		us.setWithFollowings(false); // no need to hear what JTwitTest2 has to say		
		// -- unless it's too us
		us.setAutoReconnect(true);
		us.connect();
		
		UserStream us2 = new UserStream(jtwit2);
		us2.setPreviousCount(0);
		us2.setWithFollowings(false); // no need to hear what JTwitTest2 has to say		
		// -- unless it's too us
		us2.setAutoReconnect(true);
		us2.connect();
		
		Time time = new Time().plus(1, TUnit.HOUR);
		String timeStr = (time.getHour()+1) + " " + time.getMinutes() + " " + time.getSeconds();
		int salt = new Random().nextInt(100000);
		String messageText = "Dee EMM UStream!" + salt;
		jtwit.sendMessage(jtwit2.getSelf().screenName, messageText + " I'm jtwit " + time);
		jtwit2.sendMessage(jtwit.getSelf().screenName, messageText + " I'm jtwittest2 " + time);
		Thread.sleep(10000);
		
		
		List<ITweet> tweetsRand = us.getTweets();
		List<ITweet> tweets = us.popTweets();
		List<TwitterEvent> evs = us.popEvents();
		List<ITweet> tweets2 = us2.popTweets();
		List<TwitterEvent> evs2 = us2.popEvents();
		
		boolean weGetSomething = false;
		weGetSomething = weGetSomething||!tweets.isEmpty();
		weGetSomething = weGetSomething||!tweets2.isEmpty();
		weGetSomething = weGetSomething||!evs.isEmpty();
		weGetSomething = weGetSomething||!evs2.isEmpty();
		assert weGetSomething;
		String placeHolder="";
	}
	
	/**
	 * This tests favoriting, and may be tricky.
	 * @throws InterruptedException
	 */
	@Test
	public void testUSGetFaves() throws InterruptedException {
		Twitter jtwit = TwitterTest.newTestTwitter();
		UserStream us = new UserStream(jtwit);
		us.setPreviousCount(0);
		us.setWithFollowings(false); // no need to hear what JTwitTest2 has to say		
		// -- unless it's too us
		us.setAutoReconnect(true);
		us.connect();
		
		List<Status> stats = jtwit.search("hello");
		Status astatus = stats.get(0);
		jtwit.setFavorite(astatus, true);
		
		List<ITweet> tweetsRand = us.getTweets();
		List<ITweet> tweets = us.popTweets();
		List<TwitterEvent> evs = us.popEvents();
		
		for (TwitterEvent twitterEvent : evs) {
			System.out.println(twitterEvent);
			if (twitterEvent.is(TwitterEvent.Type.FAVORITE)) {
				ITweet tweet = (ITweet) twitterEvent.getTargetObject();
				System.out.println("	Favorited "+tweet);
			}
		}
		
		boolean weGetSomething = ! evs.isEmpty();
		assert weGetSomething;
		
		assert evs.get(0).getTargetObject() instanceof ITweet;
		
		String placeHolder="";
		us.close();
	}


	@Test
	public void testUserStreamAndTwitterStream() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		
		UserStream us = new UserStream(jtwit);
		TwitterStream ts = new TwitterStream(jtwit);
		ts.setTrackKeywords(Arrays.asList("@"+jtwit.getScreenName()));
		
		us.connect();
		
		Utils.sleep(800);
		assert us.isConnected();
		assert us.isAlive();
		
		ts.connect();
		
		Utils.sleep(800);
		assert ts.isConnected();
		assert ts.isAlive();
		assert us.isConnected();
		assert us.isAlive();
		
		Twitter jtwit2 = TwitterTest.newTestTwitter();
		int salt = new Random().nextInt(1000);
		Status s = jtwit2.updateStatus("@"+jtwit.getScreenName()+" wotcha "+salt+" :)");
		System.out.println(s);
		
		Utils.sleep(1000);
		
		List<ITweet> tweets = us.getTweets();
		List<ITweet> tweets2 = ts.getTweets();
		
		assert ts.isConnected();
		assert ts.isAlive();
		assert us.isConnected();
		assert us.isAlive();
		
		// Pick up is failing?! But that's not what we're testing here
		System.out.println(tweets);
		System.out.println(tweets2);
	}
}
