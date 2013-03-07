package winterwell.jtwitter;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import org.junit.Test;

import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.KEntityType;
import winterwell.jtwitter.Twitter.TweetEntity;
import winterwell.utils.io.XStreamBinaryConverter;

public class ExtraWinterwellTests {

	
//	@Test
	public void testAuth() throws Exception {
		OAuthSignpostClient client = new OAuthSignpostClient(
				OAuthSignpostClient.JTWITTER_OAUTH_KEY,
				OAuthSignpostClient.JTWITTER_OAUTH_SECRET,
				"oob");
		client.authorizeDesktop();
		String v = client.askUser("Please enter the verification PIN from Twitter");
		client.setAuthorizationCode(v);
		// Optional: store the authorisation token details
		String[] accessToken = client.getAccessToken();
		System.out.println(accessToken[0]+" "+accessToken[1]);
	}
	
	@Test
	public void testLinkTruncation() throws Exception {
		Twitter tt = TwitterTest.newTestTwitter();
		Status s = tt.getStatus(new BigInteger("154915377170747392"));
		List<TweetEntity> urls = s.getTweetEntities(KEntityType.urls);
		System.out.println(urls);
		System.out.println(s);
		assert ! s.getText().contains("http://t.co ...");
//				"RT @pozorvlak: Delighted to see Alan Bundy (@winterstein's PhD supervisor, IIRC) in the New Year's Honour's list: http://soda.sh/xbE");
		
		BigInteger id = new BigInteger("154915015965683712");
		tt.setIncludeTweetEntities(true);
		Status s2 = tt.getStatus(id);
		System.out.println(s2);
		System.out.println(s2.getDisplayText());
		List<TweetEntity> urls2 = s2.getTweetEntities(KEntityType.urls);
		TweetEntity te = urls2==null? null : urls2.get(0); // this tweet-entity sucks too :(
		System.out.println(urls2);
		assert ! s2.getText().contains("http://t.co ...");
		
		List<Status> joes = tt.getUserTimeline("joehalliwell");
		for (Status status : joes) {
			System.out.println(status);
		}
	}
	

	@Test
	public void testSerialisation() throws IOException {
		Twitter tt = TwitterTest.newTestTwitter();
		IHttpClient client = tt.getHttpClient();
		XStreamBinaryConverter conv = new XStreamBinaryConverter();
		{// serialise
			String s = conv.toString(client);
			IHttpClient c2 = (IHttpClient) conv.fromString(s);
		}
		{// serialise
			String s = conv.toString(tt);
			Twitter tt2 = (Twitter) conv.fromString(s);
		}
	}
}
