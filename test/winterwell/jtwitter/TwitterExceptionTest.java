package winterwell.jtwitter;

import org.junit.Test;

import winterwell.json.JSONException;
import winterwell.jtwitter.TwitterException.Parsing;


public class TwitterExceptionTest {

	@Test
	public void testE403() {
		TwitterException.E403 foo = new TwitterException.E403("foo");
		assert foo.code == 0;
		TwitterException.E403 bar = new TwitterException.E403("code 108: bar");
		assert bar.code == 108;
		TwitterException.E403 none = new TwitterException.E403(null);
		assert none.code == 0;
		TwitterException.E403 nn = new TwitterException.E403(99, "ninety nine");
		assert nn.code == 99;
	}

	@Test
	public void testE50X() {
		URLConnectionHttpClient con = new URLConnectionHttpClient();
		con.setHtmlImpliesError(false);
		String page = con.getPage("http://bbc.co.uk", null, false);
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
