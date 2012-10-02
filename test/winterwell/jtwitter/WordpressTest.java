package winterwell.jtwitter;


import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;

import junit.framework.TestCase;


import winterwell.json.JSONException;
import winterwell.json.JSONObject;
import winterwell.jtwitter.Twitter.KEntityType;
import winterwell.jtwitter.Twitter.KRequestType;
import winterwell.jtwitter.Twitter.TweetEntity;
import winterwell.jtwitter.TwitterException.E401;
import winterwell.jtwitter.TwitterException.E403;
import winterwell.jtwitter.TwitterException.E404;
import winterwell.jtwitter.TwitterException.SuspendedUser;
import winterwell.utils.NoUTR;
import winterwell.utils.Printer;
import winterwell.utils.Utils;
import winterwell.utils.time.TUnit;
import winterwell.utils.time.Time;
import winterwell.utils.web.XStreamUtils;

/**
 * Unit tests for JTwitter used with WordPress.
 *
 * @author daniel
 */
public class WordpressTest
extends TestCase // Comment out to remove the JUnit dependency
{	
	
	public void testGet() {
		// You need to provide a valid user name & password!
		Twitter jtwit = new Twitter("spoonmcguffin", "?");
		jtwit.setAPIRootUrl("https://twitter-api.wordpress.com");
		
		Status s = jtwit.getStatus();
		System.out.println(s.getUser()+": "+s.getUser().getDescription());
		List<Status> meSaid = jtwit.getUserTimeline();
		System.out.println(meSaid);
		
		jtwit.setStatus("WordPress & the Twitter API::I'm posting this using the Twitter API and JTwitter (http://winterwell.com/software/jtwitter.php). It would be too long for Twitter.\n\nLet's see if it works on WordPress!");
	}
	
}
