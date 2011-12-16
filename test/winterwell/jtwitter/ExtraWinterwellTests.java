package winterwell.jtwitter;

import java.io.IOException;

import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.utils.io.XStreamBinaryConverter;

public class ExtraWinterwellTests {


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
