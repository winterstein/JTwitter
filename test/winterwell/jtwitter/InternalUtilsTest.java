package winterwell.jtwitter;

import org.junit.Test;

public class InternalUtilsTest {

	@Test
	public void testEncode() {
		String enc = InternalUtils.encode("+Justin Bieber");
		assert enc.equals("%2BJustin%20Bieber") : enc;
	}

}
