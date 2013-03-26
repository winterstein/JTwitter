/**
 * 
 */
package winterwell.jtwitter.ecosystem;

import org.junit.Test;

import winterwell.jtwitter.User;

/**
 * @author daniel
 *
 */
public class PeerIndexTest {

	static final String API_KEY = "ksyrp23pzagppj857gg3j2sr";
	
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
		
		PeerIndexProfile cfc = peerIndex.getProfile(new User("cfctruth"));
		System.out.println(cfc);
	}

}
