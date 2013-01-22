package winterwell.jtwitter;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import winterwell.jtwitter.Twitter.ITweet;
import winterwell.jtwitter.TwitterException.E413;
import winterwell.utils.NoUTR;
import winterwell.utils.Printer;
import winterwell.utils.StrUtils;
import winterwell.utils.Utils;
import winterwell.utils.containers.Containers;
import winterwell.utils.time.TUnit;
import winterwell.utils.time.Time;

public class TwitterStreamTest {

	@Test
	public void testTooManyKeywords() throws InterruptedException  {
		try {
			Twitter jtwit = new TwitterTest().newTestTwitter();
			TwitterStream ts = new TwitterStream(jtwit);
			List<String> terms = new ArrayList();
			for(int i=0; i<800; i++) {
				terms.add(Utils.getRandomString(6));
			}
			ts.setTrackKeywords(terms);
			
			ts.connect();
			Thread.sleep(5000);
			List<ITweet> tweets = ts.popTweets();
			assert false : tweets.size();
		} catch (E413 ex) {
			Printer.out("correctly stopped on 413");
			// good
		}
	}
	

	@Test
	public void testMegaData() throws InterruptedException  {
		Twitter jtwit = new TwitterTest().newTestTwitter();
		TwitterStream ts = new TwitterStream(jtwit);
		// let's get some data
		List<String> terms = Arrays.asList(
			"football", "justin", "sex", "america", "ball", " drink", "hello", "sport", "game", "twitter", "lol",
			"justinbieber", "lady", "ladygaga", "rock", "good");
		ts.setTrackKeywords(terms);			
		ts.connect();
		Thread.sleep(2000);
		List<ITweet> tweets = ts.popTweets();
		List<TwitterEvent> events = ts.popEvents();
		List<Object[]> sys = ts.popSystemEvents();		
		System.out.println(tweets.size()+"\t"+events.size()+"\t"+sys.size());
		Printer.out(sys);
	}

	@Test
	public void testThins(){
		String lon = "manchester united, manchester utd, ironman uk, ironman 70.3, ironman, wales, track cycling world championships, track world championships, cycling, london triathlon, blenheim, triathlon, british cycling, maximuscle, #ghitsquad, gatorade, hit squad, maximuscleuk, cycling team sky, rider team sky, gatoradeuk, #winfromwithin, @gatoradeuk miami heat, gatorade wales, gatorade liverpool, gatorade brighton, gatorade england, gatorade plymouth, gatorade glasgow, gatorade manchester, gatorade belfast, gatorade britain, gatorade coventry, gatorade edinburgh, gatorade leeds, gatorade birmingham, gatorade ireland, gatorade scotland, gatorade newcastle, gatorade nottingham, gatorade london, gatorade bristol, gatorade leicester, gatorade uk, gatorade cardiff, order wales, order liverpool, order brighton, order england, order plymouth, order glasgow, order manchester, order belfast, order britain, order coventry, order edinburgh, order leeds, order birmingham, order ireland, order scotland, order newcastle, order nottingham, order london, order bristol, order leicester, order uk, order cardiff, purchase wales, purchase liverpool, purchase brighton, purchase england, purchase plymouth, purchase glasgow, purchase manchester, purchase belfast, purchase britain, purchase coventry, purchase edinburgh, purchase leeds, purchase birmingham, purchase ireland, purchase scotland, purchase newcastle, purchase nottingham, purchase london, purchase bristol, purchase leicester, purchase uk, purchase cardiff, buy wales, buy liverpool, buy brighton, buy england, buy plymouth, buy glasgow, buy manchester, buy belfast, buy britain, buy coventry, buy edinburgh, buy leeds, buy birmingham, buy ireland, buy scotland, buy newcastle, buy nottingham, buy london, buy bristol, buy leicester, buy uk, buy cardiff, get wales, get liverpool, get brighton, get england, get plymouth, get glasgow, get manchester, get belfast, get britain, get coventry, get edinburgh, get leeds, get birmingham, get ireland, get scotland, get newcastle, get nottingham, get london, get bristol, get leicester, get uk, get cardiff, sell wales, sell liverpool, sell brighton, sell england, sell plymouth, sell glasgow, sell manchester, sell belfast, sell britain, sell coventry, sell edinburgh, sell leeds, sell birmingham, sell ireland, sell scotland, sell newcastle, sell nottingham, sell london, sell bristol, sell leicester, sell uk, sell cardiff, gatoradeuk and track cycling, gatoradeuk and track pendleton, gatoradeuk and track v_pendleton, gatorade and track cycling, gatorade and track pendleton, gatorade and track v_pendleton, gatorade uk cycling, gatorade uk pendleton, gatorade uk v_pendleton, gatorade cycling, gatorade pendleton, gatorade v_pendleton, masterclass cycling, masterclass pendleton, masterclass v_pendleton, gatoradeuk cycling, gatoradeuk pendleton, gatoradeuk v_pendleton, leicester rugby, leicester tigers, cycling gatorade, cycling highfive, cycling science in sport, cycling s.i.s, cycling high five, cycling cnp, cycling maxifuel, cycling maximuscle, cycling maxitone, cycling high 5, cycling high5, cycling myprotein, cycling my protein, cycling powerade, cycling for goodness shakes, cycling lucozade, cycling cnp professional, cycling maxiraw, cyclist gatorade, cyclist highfive, cyclist science in sport, cyclist s.i.s, cyclist high five, cyclist cnp, cyclist maxifuel, cyclist maximuscle, cyclist maxitone, cyclist high 5, cyclist high5, cyclist myprotein, cyclist my protein, cyclist powerade, cyclist for goodness shakes, cyclist lucozade, cyclist cnp professional, cyclist maxiraw, runner science in sport, runner my protein, runner high5, runner maximuscle, runner myprotein, runner cnp, runner powerade, runner highfive, runner high five, runner gatorade, runner high 5, runner maxitone, runner s.i.s, runner for goodness shakes, runner cnp professional, runner maxiraw, runner lucozade, runner maxifuel, marathon science in sport, marathon my protein, marathon high5, marathon maximuscle, marathon myprotein, marathon cnp, marathon powerade, marathon highfive, marathon high five, marathon gatorade, marathon high 5, marathon maxitone, marathon s.i.s, marathon for goodness shakes, marathon cnp professional, marathon maxiraw, marathon lucozade, marathon maxifuel, 10k science in sport, 10k my protein, 10k high5, 10k maximuscle, 10k myprotein, 10k cnp, 10k powerade, 10k highfive, 10k high five, 10k gatorade, 10k high 5, 10k maxitone, 10k s.i.s, 10k for goodness shakes, 10k cnp professional, 10k maxiraw, 10k lucozade, 10k maxifuel, half marathon science in sport, half marathon my protein, half marathon high5, half marathon maximuscle, half marathon myprotein, half marathon cnp, half marathon powerade, half marathon highfive, half marathon high five, half marathon gatorade, half marathon high 5, half marathon maxitone, half marathon s.i.s, half marathon for goodness shakes, half marathon cnp professional, half marathon maxiraw, half marathon lucozade, half marathon maxifuel, running science in sport, running my protein, running high5, running maximuscle, running myprotein, running cnp, running powerade, running highfive, running high five, running gatorade, running high 5, running maxitone, running s.i.s, running for goodness shakes, running cnp professional, running maxiraw, running lucozade, running maxifuel, 5k science in sport, 5k my protein, 5k high5, 5k maximuscle, 5k myprotein, 5k cnp, 5k powerade, 5k highfive, 5k high five, 5k gatorade, 5k high 5, 5k maxitone, 5k s.i.s, 5k for goodness shakes, 5k cnp professional, 5k maxiraw, 5k lucozade, 5k maxifuel";
		List<String> lis = StrUtils.split(lon);
		Printer.out(lis.size());
	}
	
	/**
	 * A real-ish example from a radian6 probe, these terms were what it got on its first run. The plan is to
	 * see what twitter makes of it. Do we get kicked off?
	 * @throws InterruptedException
	 * @throws FileNotFoundException
	 */
	@Test @NoUTR
	public void testTooManyKeywords2_R6Probes() throws InterruptedException, FileNotFoundException{
		// Sorry :/
		boolean thisRunsForeverDontRunIt = false;
		if (thisRunsForeverDontRunIt) return;
		String longString = "manchester united, manchester utd, maximuscle, maximuscleuk, ironman uk, ironman 70.3, ironman, wales, track cycling world championships, track world championships, cycling, london triathlon, blenheim, triathlon, british cycling, #ghitsquad, gatorade, hit squad, cycling team sky, rider team sky, runner science in sport, runner my protein, runner high5, runner maximuscle, runner myprotein, runner cnp, runner powerade, runner highfive, runner high five, runner gatorade, runner high 5, runner maxitone, runner s.i.s, runner for goodness shakes, runner cnp professional, runner maxiraw, runner lucozade, runner maxifuel, marathon science in sport, marathon my protein, marathon high5, marathon maximuscle, marathon myprotein, marathon cnp, marathon powerade, marathon highfive, marathon high five, marathon gatorade, marathon high 5, marathon maxitone, marathon s.i.s, marathon for goodness shakes, marathon cnp professional, marathon maxiraw, marathon lucozade, marathon maxifuel, 10k science in sport, 10k my protein, 10k high5, 10k maximuscle, 10k myprotein, 10k cnp, 10k powerade, 10k highfive, 10k high five, 10k gatorade, 10k high 5, 10k maxitone, 10k s.i.s, 10k for goodness shakes, 10k cnp professional, 10k maxiraw, 10k lucozade, 10k maxifuel, half marathon science in sport, half marathon my protein, half marathon high5, half marathon maximuscle, half marathon myprotein, half marathon cnp, half marathon powerade, half marathon highfive, half marathon high five, half marathon gatorade, half marathon high 5, half marathon maxitone, half marathon s.i.s, half marathon for goodness shakes, half marathon cnp professional, half marathon maxiraw, half marathon lucozade, half marathon maxifuel, running science in sport, running my protein, running high5, running maximuscle, running myprotein, running cnp, running powerade, running highfive, running high five, running gatorade, running high 5, running maxitone, running s.i.s, running for goodness shakes, running cnp professional, running maxiraw, running lucozade, running maxifuel, 5k science in sport, 5k my protein, 5k high5, 5k maximuscle, 5k myprotein, 5k cnp, 5k powerade, 5k highfive, 5k high five, 5k gatorade, 5k high 5, 5k maxitone, 5k s.i.s, 5k for goodness shakes, 5k cnp professional, 5k maxiraw, 5k lucozade, 5k maxifuel, leicester rugby, leicester tigers, exeter rugby, ali brownlee, alistair brownlee, jonathan brownlee, jonny brownlee, bbc @gatorade, bbc gatorade, bbc #gatorade, bbc protein shakes, bbc sports drinks, bbc sports nutrition, bbc lucozade, bbc @gatoradeuk, bbc powerade, #panorama @gatorade, #panorama gatorade, #panorama #gatorade, #panorama protein shakes, #panorama sports drinks, #panorama sports nutrition, #panorama lucozade, #panorama @gatoradeuk, #panorama powerade, panarama @gatorade, panarama gatorade, panarama #gatorade, panarama protein shakes, panarama sports drinks, panarama sports nutrition, panarama lucozade, panarama @gatoradeuk, panarama powerade, panorama @gatorade, panorama gatorade, panorama #gatorade, panorama protein shakes, panorama sports drinks, panorama sports nutrition, panorama lucozade, panorama @gatoradeuk, panorama powerade, the truth about sports products @gatorade, the truth about sports products gatorade, the truth about sports products #gatorade, the truth about sports products protein shakes, the truth about sports products sports drinks, the truth about sports products sports nutrition, the truth about sports products lucozade, the truth about sports products @gatoradeuk, the truth about sports products powerade, awards iwanrunner, premiership iwanrunner, #winfromwithin, hydration #lia2012, drink #lia2012, etonmanorrfc gatoradesos, etonmanorrfc rugby, etonmanorrfc r.f.c, etonmanorrfc gatorade, etonmanorrfc save our season, etonmanorrfc rfc, eton manor gatoradesos, eton manor rugby, eton manor r.f.c, eton manor gatorade, eton manor save our season, eton manor rfc, etonmanor gatoradesos, etonmanor rugby, etonmanor r.f.c, etonmanor gatorade, etonmanor save our season, etonmanor rfc, emrfc gatoradesos, emrfc rugby, emrfc r.f.c, emrfc gatorade, emrfc save our season, emrfc rfc, victoria pendleton, antifattax, timbrabants 02perform, timbrabants gseriespro, timbrabants 01prime, timbrabants prime, timbrabants gseries, timbrabants g series, timbrabants 03recover, timbrabants recover, timbrabants perform, timbrabants g series pro, @jojackson2012 02perform, @jojackson2012 gseriespro, @jojackson2012 01prime, @jojackson2012 prime, @jojackson2012 gseries, @jojackson2012 g series, @jojackson2012 03recover, @jojackson2012 recover, @jojackson2012 perform, @jojackson2012 g series pro, @usainbolt 02perform, @usainbolt gseriespro, @usainbolt 01prime, @usainbolt prime, @usainbolt gseries, @usainbolt g series, @usainbolt 03recover, @usainbolt recover, @usainbolt perform, @usainbolt g series pro, jonny_brownlee 02perform, jonny_brownlee gseriespro, jonny_brownlee 01prime, jonny_brownlee prime, jonny_brownlee gseries, jonny_brownlee g series, jonny_brownlee 03recover, jonny_brownlee recover, jonny_brownlee perform, jonny_brownlee g series pro, iwanrunner 02perform, iwanrunner gseriespro, iwanrunner 01prime, iwanrunner prime, iwanrunner gseries, iwanrunner g series, iwanrunner 03recover, iwanrunner recover, iwanrunner perform, iwanrunner g series pro, serenawilliams 02perform, serenawilliams gseriespro, serenawilliams 01prime, serenawilliams prime, serenawilliams gseries, serenawilliams g series, serenawilliams 03recover, serenawilliams recover, serenawilliams perform, serenawilliams g series pro, ryanlochte 02perform, ryanlochte gseriespro, ryanlochte 01prime, ryanlochte prime, ryanlochte gseries, ryanlochte g series, ryanlochte 03recover, ryanlochte recover, ryanlochte perform, ryanlochte g series pro, alibrownleetri 02perform, alibrownleetri gseriespro, alibrownleetri 01prime, alibrownleetri prime, alibrownleetri gseries, alibrownleetri g series, alibrownleetri 03recover, alibrownleetri recover, alibrownleetri perform, alibrownleetri g series pro, simba100m 02perform, simba100m gseriespro, simba100m 01prime, simba100m prime, simba100m gseries, simba100m g series, simba100m 03recover, simba100m recover, simba100m perform, simba100m g series pro, jojackson2012 02perform, jojackson2012 gseriespro, jojackson2012 01prime, jojackson2012 prime, jojackson2012 gseries, jojackson2012 g series, jojackson2012 03recover, jojackson2012 recover, jojackson2012 perform, jojackson2012 g series pro, landondonovan 02perform, landondonovan gseriespro, landondonovan 01prime, landondonovan prime, landondonovan gseries, landondonovan g series, landondonovan 03recover, landondonovan recover, landondonovan perform, landondonovan g series pro, usainbolt 02perform, usainbolt gseriespro, usainbolt 01prime, usainbolt prime, usainbolt gseries, usainbolt g series, usainbolt 03recover, usainbolt recover, usainbolt perform, usainbolt g series pro, joannejackson86 02perform, joannejackson86 gseriespro, joannejackson86 01prime, joannejackson86 prime, joannejackson86 gseries, joannejackson86 g series, joannejackson86 03recover, joannejackson86 recover, joannejackson86 perform, joannejackson86 g series pro"; 
		List<String> terms = StrUtils.split(longString);
		Printer.out("size is " + terms.size());
		
		try {
			
			Twitter jtwit = new TwitterTest().newTestTwitter();
			TwitterStream ts = new TwitterStream(jtwit);
			ts.setTrackKeywords(terms);
			ts.connect();
			
			while(true){
			Thread.sleep(5000);
			List<ITweet> tweets = ts.popTweets();
			int cnt = 0;
			Printer.out("got " + tweets.size() + " tweets, sample below");
			for (ITweet twt: tweets){
				if (cnt < 5){
					Printer.out(twt.getDisplayText());
				}
				cnt++;
			}
			}
		} catch (Exception e) {
			Printer.out("Failure. Twitter rejected it!");
			e.printStackTrace();
		}
	}

	/**
	 * Starting 2 streams leads to 
	 * winterwell.jtwitter.TwitterException: java.io.IOException: end of stream
	 */
	@Test
	public void testTooManyStreams2() {		
		try {
			Twitter jtwit = new TwitterTest().newTestTwitter();
			TwitterStream ts = new TwitterStream(jtwit);
			ts.setTrackKeywords(Arrays.asList("hello"));
			ts.connect();
			Utils.sleep(500);
			assert ts.isAlive();
			
			// Let's see what Twitter themselves say
			TwitterStream.user2stream.clear();
			
			Twitter jtwit2 = new TwitterTest().newTestTwitter();
			TwitterStream ts2 = new TwitterStream(jtwit2);
			ts2.setTrackKeywords(Arrays.asList("world"));
			ts2.connect();
			
			Utils.sleep(1000);			
			
			System.out.println(ts.getTweets());
			System.out.println(ts2.getTweets());
		} catch(TwitterException ex) {
			System.out.println(ex);
			return;
		} catch (Exception e) {
			System.out.println(e);
			return;
		}
		
		assert false;
	}

		
	
	@Test
	public void testConnect() throws InterruptedException {
		{	// sample
			Twitter jtwit = new TwitterTest().newTestTwitter();
			TwitterStream ts = new TwitterStream(jtwit);
			ts.connect();
			Thread.sleep(5000);
			List<ITweet> tweets = ts.popTweets();
			System.out.println(tweets);
			for (ITweet iTweet : tweets) {
				User u = iTweet.getUser();
				System.out.println(u.screenName+"\t"+u.followersCount+"\t"+u.getLocation());
			}
			ts.close();			
		}
		{	// search
			Twitter jtwit = new TwitterTest().newTestTwitter();
			TwitterStream ts = new TwitterStream(jtwit);
			ts.setTrackKeywords(Arrays.asList("london"));
			ts.connect();
			Thread.sleep(5000);
			List<ITweet> tweets = ts.popTweets();
			System.out.println(tweets);
			for (ITweet iTweet : tweets) {
				User u = iTweet.getUser();
				System.out.println(u.screenName+"\t"+u.followersCount+"\t"+u.getLocation());
			}
			ts.close();
		}
	}
	
//	@Test
	public void testBadConnectionFor5Minutes() throws InterruptedException {
		Twitter jtwit = new TwitterTest().newBadTestTwitter();
		BadHttpClient bhc = (BadHttpClient) jtwit.getHttpClient();
		bhc.MAX_UPTIME = 30*1000;
		bhc.P_SUCCESS = 0.4;
		TwitterStream ts = new TwitterStream(jtwit);
		ts.setAutoReconnect(true);
		ts.reconnect();
		for(int i=0; i<5; i++) {
			Thread.sleep(60*1000);
			List<TwitterEvent> events = ts.popEvents();
			List<ITweet> tweets = ts.popTweets();
			List<Object[]> sysEvs = ts.popSystemEvents();
		}
		assert ts.isAlive();
		ts.close();				
		System.out.println(ts.getOutages());
	}
	
	
	@Test
	public void testSampler() throws InterruptedException {
		Twitter jtwit2 = TwitterTest.newTestTwitter2();
		TwitterStream sampler = new TwitterStream(jtwit2);
		sampler.setAutoReconnect(true);
		sampler.connect();		
		int count = 0;
		for(int i=0; i<10; i++) {
			Thread.sleep(5000);			
			sampler.fillInOutages();
			List<ITweet> tweets = sampler.popTweets();
			sampler.popEvents(); sampler.popSystemEvents();			
			count += tweets.size();
			System.out.print("\t"+count);
		}
		sampler.close();
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
			System.out.println(e);
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
	public void testTrack() throws InterruptedException {
		Twitter jtwit = new TwitterTest().newTestTwitter();
		TwitterStream ts = new TwitterStream(jtwit);
		ts.setTrackKeywords(Arrays.asList("alice", "bob", "carol", "david"));
		ts.connect();
		Thread.sleep(2000);
		List<ITweet> list = ts.popTweets();
		assert ! list.isEmpty();
		System.out.println(list);
		ts.close();
	}
	
	
	@Test
	public void testConnectWithTerm() throws InterruptedException {
		Twitter jtwit = new TwitterTest().newTestTwitter();
		TwitterStream ts = new TwitterStream(jtwit);
		List<String> blob = new ArrayList<String>();
		blob.add("happy");
		ts.setTrackKeywords(blob);
		ts.connect();
		Thread.sleep(5000);
		List<ITweet> list = ts.popTweets();
		System.out.println(list);
		ts.close();
		assert ! list.isEmpty(); // Twitter always has some happiness
	}

	@Test
	public void testLocations() throws InterruptedException {
		Twitter jtwit = new TwitterTest().newTestTwitter();
		TwitterStream ts = new TwitterStream(jtwit);
		ts.connect();
		HashMap<String,Double> infodist = new HashMap();
		for(int i=0; i<10; i++){
			Thread.sleep(1000);
			List<ITweet> tweets = ts.popTweets();			
			for (ITweet iTweet : tweets) {
				String tloc = iTweet.getLocation();
				String uloc = iTweet.getUser().getLocation();
				String cse = "t:"+(tloc==null)+" u:"+(uloc==null);
				Containers.plus(infodist, cse, 1);			
			}
			System.out.println(infodist);
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
	public void testStreamAndSearch(){
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter jtwit2 = TwitterTest.newTestTwitter2();
		String jtSName = jtwit.getSelf().screenName;
		String jt2SName = jtwit2.getSelf().screenName;
		
		List<String> blob2 = new ArrayList<String>();
		blob2.add("hello");
		
		TwitterStream ts = new TwitterStream(jtwit);
		ts.setTrackKeywords(blob2);
		ts.connect();

		TwitterStream ts2 = new TwitterStream(jtwit2);
		
		ts2.setTrackKeywords(blob2);
		ts2.connect();
		
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
