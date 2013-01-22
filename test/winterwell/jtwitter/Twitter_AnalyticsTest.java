/**
 * 
 */
package winterwell.jtwitter;

import org.junit.Test;

/**
 * @author daniel
 *
 */
public class Twitter_AnalyticsTest {

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter_Analytics#getUrlCount(java.lang.String)}.
	 */
	@Test
	public void testGetUrlCount() {
		Twitter_Analytics analytics = new Twitter().analytics();
		int count = analytics.getUrlCount("http://sodash.com");
		assert count > 0;
	}

}
