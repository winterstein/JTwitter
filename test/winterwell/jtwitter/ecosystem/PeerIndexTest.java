/**
 * 
 */
package winterwell.jtwitter.ecosystem;

import static org.junit.Assert.*;

import org.junit.Test;

import winterwell.jtwitter.User;

/**
 * @author daniel
 *
 */
public class PeerIndexTest {

	static final String API_KEY = "ffb5b54460954cd4edbba1c08b04d802";
	
	/**
	 * Test method for {@link winterwell.jtwitter.ecosystem.PeerIndex#show(java.lang.String)}.
	 */
	@Test
	public void testShow() {
		PeerIndex peerIndex = new PeerIndex(API_KEY);
		PeerIndexProfile dan = peerIndex.getProfile(new User("winterstein"));
		System.out.println(dan);
		PeerIndexProfile joe = peerIndex.getProfile(new User("joehalliwell"));
		System.out.println(joe);
	}

}
