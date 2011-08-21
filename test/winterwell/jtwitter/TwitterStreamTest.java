package winterwell.jtwitter;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import winterwell.jtwitter.Twitter.ITweet;
import winterwell.utils.Printer;
import winterwell.utils.containers.Containers;

public class TwitterStreamTest {

	@Test
	public void testConnect() throws InterruptedException {
		Twitter jtwit = new TwitterTest().newTestTwitter();
		TwitterStream ts = new TwitterStream(jtwit.getHttpClient());
		ts.connect();
		Thread.sleep(5000);
		System.out.println(ts.readThread.popJsons());
		ts.close();
	}
	

	@Test
	public void testLocations() throws InterruptedException {
		Twitter jtwit = new TwitterTest().newTestTwitter();
		TwitterStream ts = new TwitterStream(jtwit.getHttpClient());
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

}
