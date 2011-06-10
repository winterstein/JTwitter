package winterwell.jtwitter;

import static org.junit.Assert.*;

import org.junit.Test;

import winterwell.utils.Printer;

public class TwitterStreamTest {

	@Test
	public void testConnect() throws InterruptedException {
		Twitter jtwit = new TwitterTest().newTestTwitter();
		TwitterStream ts = new TwitterStream(jtwit.getHttpClient());
		ts.connect();
		Thread.sleep(5000);
		Printer.out(ts.readThread.popJsons());
		ts.close();
	}

}
