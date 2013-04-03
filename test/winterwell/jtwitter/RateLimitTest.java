package winterwell.jtwitter;

import org.junit.Test;

public class RateLimitTest {

	@Test
	public void testGetResource() {
		{
			String r = RateLimit.getResource("https://api.twitter.com/1.1/direct_messages.json?id=foo");
			assert r.equals("/direct_messages") : r;
		}
		{
			String r = RateLimit.getResource("https://api.twitter.com/1.1/direct_messages/sent.json?id=foo");
			assert r.equals("/direct_messages/sent") : r;
		}
		{
			String r = RateLimit.getResource("https://api.twitter.com/1.1/friends/ids.json");
			assert r.equals("/friends/ids") : r;
		}
		{
			String r = RateLimit.getResource("https://api.twitter.com/1.1/friends/ids/foo/bar.json");
			assert r.equals("/friends/ids") : r;
		}
	}

}
