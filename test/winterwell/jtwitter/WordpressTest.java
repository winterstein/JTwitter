package winterwell.jtwitter;


import java.util.List;

import junit.framework.TestCase;

/**
 * Unit tests for JTwitter used with wordpress.com.
 *
 * @author daniel
 */
public class WordpressTest
extends TestCase // Comment out to remove the JUnit dependency
{	
	
	public void testGet() {
		// You need to provide a valid user name & password!
		Twitter jtwit = new Twitter("spoonmcguffin", "?");
		jtwit.setAPIRootUrl("https://twitter-api.wordpress.com");
		
		Status s = jtwit.getStatus();
		System.out.println(s.getUser()+": "+s.getUser().getDescription());
		List<Status> meSaid = jtwit.getUserTimeline();
		System.out.println(meSaid);
		
		jtwit.setStatus("WordPress & the Twitter API::I'm posting this using the Twitter API and JTwitter (http://winterwell.com/software/jtwitter.php). It would be too long for Twitter.\n\nLet's see if it works on WordPress!");
	}
	
}
