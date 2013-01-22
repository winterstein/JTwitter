/**
 * 
 */
package winterwell.jtwitter.ecosystem;

import org.junit.Test;

import winterwell.jtwitter.Twitter;

/**
 * @author daniel
 *
 */
public class TwitterCounterTest {

	/**
	 * Test method for {@link winterwell.jtwitter.ecosystem.TwitterCounter#getStats(java.lang.Number)}.
	 */
	@Test
	public void testGetStats() {
		Twitter jtwit = new Twitter();
		TwitterCounter tc = new TwitterCounter("041483206a4f8f06463de14e1b95e967"); // JTwitter key
		Long id = 6663112L; //jtwit.users().show("winterstein").id;
		System.out.println(id);
		Object stats = tc.getStats(id);
		System.out.println(stats);
	}

}
