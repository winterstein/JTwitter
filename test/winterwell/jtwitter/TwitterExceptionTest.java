package winterwell.jtwitter;

import org.json.JSONException;
import org.junit.Test;

import winterwell.jtwitter.TwitterException.Parsing;


public class TwitterExceptionTest {

	@Test
	public void testParsing() {
		Parsing ex = new TwitterException.Parsing(null, new JSONException("dummy"));
		System.out.println(ex);
//		ex.printStackTrace();
	}
}
