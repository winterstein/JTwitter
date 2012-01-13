package winterwell.jtwitter;

import org.junit.Test;

import winterwell.json.JSONException;
import winterwell.jtwitter.TwitterException.Parsing;


public class TwitterExceptionTest {


	@Test
	public void testE50X() {
		String page = new URLConnectionHttpClient().getPage("http://bbc.co.uk", null, false);
		TwitterException.E50X e50x = new TwitterException.E50X(page);
		String msg = e50x.getMessage();
		assert msg.length() < 300;
		System.out.println(msg);
	}
	
	@Test
	public void testParsing() {
		Parsing ex = new TwitterException.Parsing(null, new JSONException("dummy"));
		System.out.println(ex);
//		ex.printStackTrace();
	}
}
