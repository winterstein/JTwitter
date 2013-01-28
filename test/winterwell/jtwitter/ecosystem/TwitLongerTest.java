/**
 * 
 */
package winterwell.jtwitter.ecosystem;

import winterwell.jtwitter.Status;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterTest;

/**
 * @author daniel
 *
 */
public class TwitLongerTest {


	public void testTwitLonger() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><twitlonger>\n"
	+"<post>\n"
	+"	<id>448efb379cea1098ebe2c1fe453a1cdc</id>\n"
+"		<link>http://www.twitlonger.com/show/448efb379cea1098ebe2c1fe453a1cdc</link>\n"
+"		<short>http://tl.gd/2lc0qb</short>\n"
+"		<content>This is a long test status. Sorry if I ramble. But sometimes 140 characters is just too short.\n"
+"You know what I (cont) http://tl.gd/2lc0qb</content>\n"
+"	</post>\n"
+"</twitlonger>";

		assert TwitLonger.contentTag.matcher(xml).find();

		Twitter twitter = TwitterTest.newTestTwitter();
		twitter.setupTwitlonger("sodash", "MyTwitlongerApiKey"); // FIXME
		Status s = twitter.updateLongStatus(
				"This is a long test status. Sorry if I ramble. But sometimes 140 characters is just too short.\n"
				+"You know what I mean?\n\n"
				+"So thank-you to TwitLonger for providing this service.\n"
				+":)", null);
		System.out.println(s);
	}
}
