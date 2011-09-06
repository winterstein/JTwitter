package winterwell.jtwitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import winterwell.jtwitter.Twitter.ITweet;
import winterwell.utils.Printer;
import winterwell.utils.containers.Containers;
import winterwell.utils.time.TUnit;
import winterwell.utils.time.Time;

public class TwitterStreamTest {

	@Test
	public void testConnect() throws InterruptedException {
		Twitter jtwit = new TwitterTest().newTestTwitter();
		TwitterStream ts = new TwitterStream(jtwit);
		ts.connect();
		Thread.sleep(5000);
		System.out.println(ts.popTweets());
		ts.close();
	}
	
	@Test
	public void testSampler() throws InterruptedException {
		Twitter jtwit2 = TwitterTest.newTestTwitter2();
		TwitterStream sampler = new TwitterStream(jtwit2);
		sampler.setAutoReconnect(true);
		sampler.connect();		
		int count = 0;
		while(true) {
			Thread.sleep(5000);			
			sampler.fillInOutages();
			List<ITweet> tweets = sampler.popTweets();
			sampler.popEvents(); sampler.popSystemEvents();			
			count += tweets.size();
			System.out.print("\t"+count);
		}
//		sampler.close();
	}
	
	@Test
	public void testTooManyStreams() throws InterruptedException {
		Twitter jtwit2 = TwitterTest.newTestTwitter2();
		TwitterStream sampler = new TwitterStream(jtwit2);
		sampler.setAutoReconnect(true);
		sampler.connect();
		Thread.sleep(100);
		try {
			TwitterStream sampler2 = new TwitterStream(jtwit2);
			sampler2.connect();
			assert false;
		} catch (Exception e) {
			// good
		}
		Thread.sleep(500);
		sampler.close();
		assert ! sampler.isConnected();
		TwitterStream sampler2 = new TwitterStream(jtwit2);
		sampler2.connect();
		Thread.sleep(500);						
		sampler2.close();
	}
	
	@Test
	public void testConnectWithTerm() throws InterruptedException {
		Twitter jtwit = new TwitterTest().newTestTwitter();
		TwitterStream ts = new TwitterStream(jtwit);
		List<String> blob = new ArrayList<String>();
		blob.add(jtwit.getSelf().screenName);
		ts.setTrackKeywords(blob);
		ts.connect();
		Thread.sleep(20000);
		List<ITweet> list = ts.popTweets();
		ts.close();
	}

	@Test
	public void testLocations() throws InterruptedException {
		Twitter jtwit = new TwitterTest().newTestTwitter();
		TwitterStream ts = new TwitterStream(jtwit);
		ts.connect();
		HashMap<String,Double> infodist = new HashMap();
		for(int i=0; i<100; i++){
			Thread.sleep(1000);
			List<ITweet> tweets = ts.popTweets();			
			for (ITweet iTweet : tweets) {
				String tloc = iTweet.getLocation();
				String uloc = iTweet.getUser().getLocation();
				String cse = "t:"+(tloc==null)+" u:"+(uloc==null);
				Containers.plus(infodist, cse, 1);			
			}
			Printer.out(infodist);
		}
		ts.close();
	}

	/**
	 * A sanity test, is TwitterStream *really* ignoring jtwit, but not jtwittest?
	 * @throws InterruptedException 
	 */
	@Test
	public void scratchTest() throws InterruptedException{
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter jtwit2 = TwitterTest.newTestTwitter2();
		String jtSName = jtwit.getSelf().screenName;
		String jt2SName = jtwit2.getSelf().screenName;
				//It's Spoon McGuffin!
		String [] JTWITTER_OAUTH_TOKENS = {"15071035-t2BQhajV14wmE5EJXPw9RZWOOjFdqGpVygNqEUdqg","KyWa8Fe1tDcXjABwEvdrjxEu5xetqz4AAmOfFnLlI8"};
		OAuthSignpostClient client = new OAuthSignpostClient(
				OAuthSignpostClient.JTWITTER_OAUTH_KEY,
				OAuthSignpostClient.JTWITTER_OAUTH_SECRET,
				JTWITTER_OAUTH_TOKENS[0], JTWITTER_OAUTH_TOKENS[1]);
		Twitter spoon = new Twitter("spoonmcguffin", client);
		TwitterStream tspoon = new TwitterStream(spoon);
		List<String> blob2 = new ArrayList<String>();
		blob2.add("xoappleox");
		blob2.add("xobearox");
		blob2.add("xocornox");
		blob2.add("xodoorox");
		int salt = new Random().nextInt(100000);
		tspoon.setTrackKeywords(blob2);
		tspoon.connect();
		
		Status s = jtwit.setStatus("xoappleox " + salt);
		Thread.sleep(1000);
		System.out.println(s);

		Status s2 = jtwit.setStatus("xobearox " + salt);
		Thread.sleep(1000);
		System.out.println(s2);

		Status s3 = jtwit2.setStatus("xocornox " + salt);
		Thread.sleep(1000);
		System.out.println(s3);

		Status s4 = jtwit2.setStatus("xodoorox " + salt);
		Thread.sleep(1000);
		System.out.println(s4);
		
		List<ITweet> inc = tspoon.popTweets();
		String ph = "";
		//Yes... it's ignoring jtwit.
		for (ITweet tw : inc){
			System.out.println(tw.toString());
			assert ! (tw.getText().contains("xoappleox"));
		}
		
	}
	
	@Test
	public void testTSGetMentions() throws InterruptedException {
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter jtwit2 = TwitterTest.newTestTwitter2();
		String jtSName = jtwit.getSelf().screenName;
		String jt2SName = jtwit2.getSelf().screenName;
		
		List<String> blob2 = new ArrayList<String>();
		blob2.add("xoappleox");
		blob2.add("xobearox");
		blob2.add("xocornox");
		blob2.add("xodoorox");
		
		TwitterStream ts = new TwitterStream(jtwit);
		ts.setTrackKeywords(blob2);
		ts.connect();

		TwitterStream ts2 = new TwitterStream(jtwit2);
		
		ts2.setTrackKeywords(blob2);
		ts2.connect();

		
		//V simple test. Get jtwit2 to send 2 messages, see if it picks it up.
		Time time = new Time().plus(1, TUnit.HOUR);
		int salt = new Random().nextInt(100000);
		String messageText = "Cripes! This is UserST! " + salt + " "; 
		String messageText2 = "Public mess This is UserST2! " + salt + " ";
		
		Status s = jtwit.setStatus("xoappleox " + salt);
		Thread.sleep(1000);
		System.out.println(s);

		Status s2 = jtwit.setStatus("xobearox " + salt);
		Thread.sleep(1000);
		System.out.println(s2);

		Status s3 = jtwit2.setStatus("xocornox " + salt);
		Thread.sleep(1000);
		System.out.println(s3);

		Status s4 = jtwit2.setStatus("xodoorox " + salt);
		Thread.sleep(1000);
		System.out.println(s4);
		

		List<ITweet> tweets = ts.popTweets();
		List<TwitterEvent> evs = ts.popEvents();
		List<ITweet> tweets2 = ts2.popTweets();
		List<TwitterEvent> evs2 = ts2.popEvents();
		
		//Both messages should appear here, for both users.
		boolean m1Present = false; 
		boolean m2Present = false;
		
		ts.close();
		ts2.close();
	}

	
}
