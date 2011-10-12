package winterwell.jtwitter;

import org.junit.Test;

public class InternalUtilsTest {

	@Test
	public void testEncode() {
		String enc = InternalUtils.encode("+Justin Bieber");
		assert enc.equals("%2BJustin%20Bieber") : enc;
	}
	

	@Test
	public void testStripUrls() throws Exception {
		String stripped = InternalUtils.stripUrls("hello foo.com http://www.whatever/blah?a=b&c=1-2 yeah");
		assert stripped.equals("hello foo.com  yeah") : stripped;
	}

}
