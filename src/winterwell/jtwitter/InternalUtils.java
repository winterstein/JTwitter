package winterwell.jtwitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import winterwell.json.JSONException;
import winterwell.json.JSONObject;

import com.winterwell.jgeoplanet.IGeoCode;
import com.winterwell.jgeoplanet.IPlace;
import com.winterwell.jgeoplanet.MFloat;

/**
 * Utility methods used in Twitter. This class is public in case anyone else
 * wants to use these methods. WARNING: they don't really form part of the
 * JTwitter API, and may be changed or reorganised in future versions.
 * <p>
 * NB: Some of these are copies (sometimes simplified) of methods in
 * winterwell.utils.Utils
 * 
 * @author daniel
 * @testedby {@link InternalUtilsTest}
 */
public class InternalUtils {

	/**
	 * Utility method for {@link IGeoCode}rs
	 * @param places
	 * @param prefType
	 * @param confidence
	 * @param baseConfidence
	 * @return
	 */
	public static <P extends IPlace> P prefer(List<P> places, String prefType,
			MFloat confidence, float baseConfidence) 
	{
		assert places.size() != 0;
		assert baseConfidence >= 0 && baseConfidence <= 1;
		// prefer cities (or whatever)
		List cities = new ArrayList();
		for (IPlace place : places) {
			if (prefType.equals(place.getType())) {
				cities.add(place);
			}
		}
		if (cities.size()!=0 && cities.size()!=places.size()) {			
			if (confidence!=null) {
				float conf = 0.95f*baseConfidence / cities.size();
				confidence.value = conf;
			}
			places = cities;
		} else {
			if (confidence!=null) confidence.set(baseConfidence/places.size());
		}
		// pick the first
		return places.get(0);
	}


	public static String stripUrls(String text) {
		return URL_REGEX.matcher(text).replaceAll("");
	}
	
	public static final Pattern TAG_REGEX = Pattern.compile("<!?/?[\\[\\-a-zA-Z][^>]*>", Pattern.DOTALL);
	
	static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * The date format used by Marko from Marakana. This is needed for *some*
	 * installs of Status.Net, though not for Identi.ca.
	 */
	static final DateFormat dfMarko = new SimpleDateFormat(
			"EEE MMM dd HH:mm:ss ZZZZZ yyyy");

	/**
	 * Matches latitude, longitude, including with the UberTwitter UT: prefix
	 * Group 2 = latitude, Group 3 = longitude.
	 * <p>
	 * Weird: I saw this as an address - "ÜT: 25.324488,55.376224t" Is it just a
	 * one-off typo? Should we match N/S/E/W markers?
	 */
	// ?? unify this with Location#parse()?
	public static final Pattern latLongLocn = Pattern
			.compile("(\\S+:)?\\s*(-?[\\d\\.]+)\\s*,\\s*(-?[\\d\\.]+)");

	static final Comparator<Status> NEWEST_FIRST = new Comparator<Status>() {
		@Override
		public int compare(Status o1, Status o2) {
			return -o1.id.compareTo(o2.id);
		}
	};

	public static final Pattern REGEX_JUST_DIGITS = Pattern.compile("\\d+");

	/**
	 * Matches urls. Note: Excludes any trailing .
	 * 
	 * TODO Twitter's definition of a url is bit looser!
	 * 
	 * @testedy {@link WebUtilsTest#testUrlRegex()}
	 */
	static final Pattern URL_REGEX = Pattern
			.compile("[hf]tt?ps?://[a-zA-Z0-9_%\\-\\.,\\?&\\/=\\+'~#!\\*:]+[a-zA-Z0-9_%\\-&\\/=\\+]");

	static ConcurrentHashMap<String, Long> usage;

	/**
	 * Create a map from a list of key, value pairs. An easy way to make small
	 * maps, basically the equivalent of {@link Arrays#asList(Object...)}. If
	 * the value is null, the key will not be included.
	 */
	@SuppressWarnings("unchecked")
	public static Map asMap(Object... keyValuePairs) {
		assert keyValuePairs.length % 2 == 0;
		Map m = new HashMap(keyValuePairs.length / 2);
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			Object v = keyValuePairs[i + 1];
			if (v == null) {
				continue;
			}
			m.put(keyValuePairs[i], v);
		}
		return m;
	}

	public static void close(OutputStream output) {
		if (output == null)
			return;
		// Flush (annoying that this is not part of Closeable)
		try {
			output.flush();
		} catch (Exception e) {
			// Ignore
		} finally {
		// Close
		try {
			output.close();
		} catch (IOException e) {
			// Ignore
		}
		}
	}
	
	public static void close(InputStream input) {
		if (input == null)
			return;
		// Close
		try {
			input.close();
		} catch (IOException e) {
			// Ignore
		}
	}

	/**
	 * Count API usage for api usage stats.
	 * 
	 * @param url
	 */
	static void count(String url) {
		if (usage == null)
			return;
		// ignore parameters
		int i = url.indexOf("?");
		if (i != -1) {
			url = url.substring(0, i);
		}
		// for clarity
		i = url.indexOf("/1/");
		if (i != -1) {
			url = url.substring(i + 3);
		}
		// some calls - eg statuses/show - include the tweet id
		url = url.replaceAll("\\d+", "");
		// non-blocking (we could just ignore the race condition I suppose)
		for (int j = 0; j < 100; j++) { // give up if you lose >100 races
			Long v = usage.get(url);
			boolean done;
			if (v == null) {
				Long old = usage.putIfAbsent(url, 1L);
				done = old == null;
			} else {
				long nv = v + 1;
				done = usage.replace(url, v, nv);
			}
			if (done) {
				break;
			}
		}
	}

	static String encode(Object x) {
		String encd;
		try {
			encd = URLEncoder.encode(String.valueOf(x), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// This shouldn't happen as UTF-8 is standard
			encd = URLEncoder.encode(String.valueOf(x));
		}
		return encd.replace("+", "%20");
	}

	/**
	 * @return a map of API endpoint to count-of-calls. null if switched off
	 *         (which is the default).
	 * 
	 * @see #setTrackAPIUsage(boolean)
	 */
	static public ConcurrentHashMap<String, Long> getAPIUsageStats() {
		return usage;
	}

	/**
	 * Convenience method for making Dates. Because Date is a tricksy bugger of
	 * a class.
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @return date object
	 */
	public static Date getDate(int year, String month, int day) {
		try {
			Field field = GregorianCalendar.class.getField(month.toUpperCase());
			int m = field.getInt(null);
			Calendar date = new GregorianCalendar(year, m, day);
			return date.getTime();
		} catch (Exception x) {
			throw new IllegalArgumentException(x.getMessage());
		}
	}

	/**
	 * A very tolerant boolean reader 
	 * @param obj
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	static Boolean getOptBoolean(JSONObject obj, String key)
			throws JSONException {
		Object o = obj.opt(key);
		if (o == null || o.equals(JSONObject.NULL))
			return null;
		if (o instanceof Boolean) {
			return (Boolean) o;
		}
		if (o instanceof String) {
			String os = (String) o;		
			if (os.equalsIgnoreCase("true")) return true;		
			if (os.equalsIgnoreCase("false")) return false;
		}
		// Wordpress returns some random shite :(
		if (o instanceof Integer) {
			int oi = (Integer)o;
			if (oi==1) return true;
			if (oi==0 || oi==-1) return false;
		}
		System.err.println("JSON parse fail: "+o + " (" + key + ") is not boolean");
		return null;
	}

	/**
	 * Join a slice of the list
	 * 
	 * @param screenNamesOrIds
	 * @param first
	 *            Inclusive
	 * @param last
	 *            Exclusive. Can be > list.size (will be truncated).
	 * @return
	 */
	static String join(List screenNamesOrIds, int first, int last) {
		StringBuilder names = new StringBuilder();
		for (int si = first, n = Math.min(last, screenNamesOrIds.size()); si < n; si++) {
			names.append(screenNamesOrIds.get(si));
			names.append(",");
		}
		// pop the final ","
		if (names.length() != 0) {
			names.delete(names.length() - 1, names.length());
		}
		return names.toString();
	}
	
	/**
	 * Join the list
	 * 
	 * @param screenNames
	 * @return
	 */
	public static String join(String[] screenNames) {
		StringBuilder names = new StringBuilder();
		for (int si = 0, n = screenNames.length; si < n; si++) {
			names.append(screenNames[si]);
			names.append(",");
		}
		// pop the final ","
		if (names.length() != 0) {
			names.delete(names.length() - 1, names.length());
		}
		return names.toString();
	}

	/**
	 * Helper method to deal with JSON-in-Java weirdness
	 * 
	 * @return Can be null
	 * */
	protected static String jsonGet(String key, JSONObject jsonObj) {
		assert key != null : jsonObj;
		assert jsonObj != null;
		Object val = jsonObj.opt(key);
		if (val == null)
			return null;
		if (JSONObject.NULL.equals(val))
			return null;
		String s = val.toString();
		return s;
	}

	static Date parseDate(String c) {
		if (InternalUtils.REGEX_JUST_DIGITS.matcher(c).matches())
			return new Date(Long.valueOf(c));
		try {
			Date _createdAt = new Date(c);
			return _createdAt;
		} catch (Exception e) { // Bug reported by Marakana with *some*
								// Status.Net sites
			try {
				Date _createdAt = InternalUtils.dfMarko.parse(c);
				return _createdAt;
			} catch (ParseException e1) {
				throw new TwitterException.Parsing(c, e1);
			}
		}
	}

	/**
	 * Note: this is a global JVM wide setting, intended for debugging.
	 * @param on
	 *            true to activate {@link #getAPIUsageStats()}. false to switch
	 *            stats off. false by default
	 */
	static public void setTrackAPIUsage(boolean on) {
		if (!on) {
			usage = null;
			return;
		}
		if (usage != null)
			return;
		usage = new ConcurrentHashMap<String, Long>();
	}
	
	private static final Charset UTF_8 = Charset.forName("UTF-8");

	/**
	 * Use a bufferred reader (preferably UTF-8) to extract the contents of the
	 * given stream. A convenience method for
	 * {@link InternalUtils#toString(Reader)}.
	 */
	protected static String toString(InputStream inputStream) {
		try {
			Reader reader = new InputStreamReader(inputStream, UTF_8.newDecoder());
			reader = new BufferedReader(reader);
			StringBuilder output = new StringBuilder();
			while (true) {
				int c = reader.read();
				if (c == -1) {
					break;
				}
				output.append((char) c);
			}
			return output.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			close(inputStream);
		}
	}

	/**
	 * Twitter html encodes some entities: ", ', <, >, &
	 * 
	 * @param text
	 *            Can be null (which returns null)
	 * @return normal-ish text
	 */
	static String unencode(String text) {
		if (text == null)
			return null;
		// TODO use Jakarta to handle all html entities?
		text = text.replace("&quot;", "\"");
		text = text.replace("&apos;", "'");
		text = text.replace("&nbsp;", " ");
		text = text.replace("&amp;", "&");
		text = text.replace("&gt;", ">");
		text = text.replace("&lt;", "<");
		// zero-byte chars are a rare but annoying occurrence
		if (text.indexOf(0) != -1) {
			text = text.replace((char) 0, ' ').trim();
		}
		// if (Pattern.compile("&\\w+;").matcher(text).find()) {
		// System.out.print(text);
		// }
		return text;
	}

	/**
	 * Convert to a URI, or return null if this is badly formatted
	 */
	static URI URI(String uri) {
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			return null; // Bad syntax
		}
	}

	static User user(String json) {
		try {
			JSONObject obj = new JSONObject(json);
			User u = new User(obj, null);
			return u;
		} catch (JSONException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * Remove xml and html tags, e.g. to safeguard against javascript 
	 * injection attacks, or to get plain text for NLP.
	 * @param xml can be null, in which case null will be returned
	 * @return the text contents - ie input with all tags removed
	 */
	public static String stripTags(String xml) {
		if (xml==null) return null;
		// short cut if there are no tags
		if (xml.indexOf('<')==-1) return xml;
		// first all the scripts (cos we must remove the tag contents too)
		Matcher m4 = pScriptOrStyle.matcher(xml);
		xml = m4.replaceAll("");
		// comments
		Matcher m2 = pComment.matcher(xml);
		String txt = m2.replaceAll("");
		// now the tags
		Matcher m = TAG_REGEX.matcher(txt);
		String txt2 = m.replaceAll("");
		Matcher m3 = pDocType.matcher(txt2);
		String txt3 = m3.replaceAll("");		
		return txt3;
	}

	
	/**
	 * Matches an xml comment - including some bad versions
	 */
	public static final Pattern pComment = Pattern.compile("<!-*.*?-+>", Pattern.DOTALL);
	
	/**
	 * Used in strip tags to get rid of scripts and css style blocks altogether.
	 */
	public static final Pattern pScriptOrStyle = Pattern.compile("<(script|style)[^<>]*>.+?</(script|style)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	
	/**
	 * Matches a doctype element.
	 */
	public static final Pattern pDocType = Pattern.compile("<!DOCTYPE.*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	public static void sleep(long msecs) {
		try {
			Thread.sleep(msecs);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}		
	}


	/**
	 * Several methods require authorisation in API v1.1, but not in v1.0
	 * @param jtwit
	 * @return true if jtwit can authorise, or if the API v is 1.1
	 */
	static boolean authoriseIn11(Twitter jtwit) {
		return jtwit.getHttpClient().canAuthenticate() 
				|| jtwit.TWITTER_URL.endsWith("1.1");
	}

}
