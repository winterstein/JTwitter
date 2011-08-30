package winterwell.jtwitter;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import winterwell.jtwitter.Twitter.User;
import winterwell.jtwitter.TwitterException.Parsing;
import winterwell.utils.web.WebUtilsTest;

/**
 * Utility methods used in Twitter.
 * This class is public in case anyone else wants to use these methods.
 * WARNING: they don't really form part of the JTwitter API, and may be changed or 
 * reorganised in future versions.
 * <p>
 * NB: Some of these are copies (sometimes simplified) of methods in winterwell.utils.Utils
 * @author daniel
 *
 */
public class InternalUtils {

	static ConcurrentHashMap<String, Long> usage;
	
	/**
	 * @return a map of API endpoint to count-of-calls.
	 * null if switched off (which is the default).
	 * 
	 * @see #setTrackAPIUsage(boolean)
	 */
	static public ConcurrentHashMap<String, Long> getAPIUsageStats() {
		return usage;
	}
	
	/**
	 * @param on true to activate {@link #getAPIUsageStats()}. false to
	 * switch stats off. 
	 * false by default
	 */
	static public void setTrackAPIUsage(boolean on) {
		if ( ! on) {
			usage = null;
			return;
		}
		if (usage != null) return;
		usage = new ConcurrentHashMap<String, Long>();
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

	static Date parseDate(String c) {
		if (InternalUtils.REGEX_JUST_DIGITS.matcher(c).matches()) {
			return new Date(Long.valueOf(c));
		}
		try {
			Date _createdAt = new Date(c);
			return _createdAt;
		} catch (Exception e) { // Bug reported by Marakana with *some* Status.Net sites
			try {
				Date _createdAt = InternalUtils.dfMarko.parse(c);
				return _createdAt;
			} catch (ParseException e1) {
				throw new TwitterException.Parsing(c, e1);
			}
		}
	}

	/**
	 * Join a slice of the list
	 * @param screenNamesOrIds
	 * @param first Inclusive
	 * @param last Exclusive. Can be > list.size (will be truncated).
	 * @return
	 */
	static String join(List screenNamesOrIds, int first, int last) {
		StringBuilder names = new StringBuilder();
		for (int si = first, n = Math.min(last, screenNamesOrIds.size()); 
			si < n; si++) 
		{
			names.append(screenNamesOrIds.get(si));
			names.append(",");
		}
		// pop final ,
		names.delete(names.length() - 1, names.length());
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
	 * Create a map from a list of key, value pairs. An easy way to make small
	 * maps, basically the equivalent of {@link Arrays#asList(Object...)}.
	 * If the value is null, the key will not be included.
	 */
	@SuppressWarnings("unchecked")
	static <K, V> Map<K, V> asMap(Object... keyValuePairs) {
		assert keyValuePairs.length % 2 == 0;
		Map m = new HashMap(keyValuePairs.length / 2);
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			Object v = keyValuePairs[i + 1];
			if (v==null) continue;
			m.put(keyValuePairs[i], v);
		}
		return m;
	}

	public static final Pattern REGEX_JUST_DIGITS = Pattern.compile("\\d+");
	
	/**
	 * Matches urls. 
	 * Note: Excludes any trailing .
	 * @testedy {@link WebUtilsTest#testUrlRegex()}
	 */
	static final Pattern URL_REGEX = Pattern.compile(
			"[hf]tt?ps?://[a-zA-Z0-9_%\\-\\.,\\?&\\/=\\+'~#!\\*:]+[a-zA-Z0-9_%\\-&\\/=\\+]");
	/**
	 * Matches latitude, longitude, including with the UberTwitter UT: prefix
	 * Group 2 = latitude, Group 3 = longitude.
	 * <p>
	 * Weird: I saw this as an address - "ÜT: 25.324488,55.376224t"
	 * Is it just a one-off typo?
	 * Should we match N/S/E/W markers?
	 */
	public static final Pattern latLongLocn = Pattern.compile(
			"(\\S+:)?\\s*(-?[\\d\\.]+)\\s*,\\s*(-?[\\d\\.]+)");
	static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	/**
	 * The date format used by Marko from Marakana.
	 * This is needed for *some* installs of Status.Net, though not for Identi.ca.
	 */
	static final DateFormat dfMarko = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy");
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
		// zero-byte chars are a rare but annoying occurence
		if (text.indexOf(0) != -1) {
			text = text.replace((char)0, ' ').trim();
		}
		// if (Pattern.compile("&\\w+;").matcher(text).find()) {
		// System.out.print(text);
		// }
		return text;
	}

	/**
	 * Use a buffered reader to extract the contents of the given reader.
	 *
	 * @param reader
	 * @return The contents of this reader.
	 */
	static String toString(Reader reader) throws RuntimeException {
		try {
			// Buffer if not already buffered
			reader = reader instanceof BufferedReader ? (BufferedReader) reader
					: new BufferedReader(reader);
			StringBuilder output = new StringBuilder();
			while (true) {
				int c = reader.read();
				if (c == -1) {
					break;
				}
				output.append((char) c);
			}
			return output.toString();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} finally {
			URLConnectionHttpClient.close(reader);
		}
	}
	

	/**
	 * Use a bufferred reader (preferably UTF-8) to extract the contents of the
	 * given stream. A convenience method for {@link InternalUtils#toString(Reader)}.
	 */
	protected static String toString(InputStream inputStream) {
		InputStreamReader reader;
		try {
			reader = new InputStreamReader(inputStream, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			reader = new InputStreamReader(inputStream);
		}
		return InternalUtils.toString(reader);
	}

	static String encode(Object x) {
		try {
			return URLEncoder.encode(String.valueOf(x), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// This shouldn't happen as UTF-8 is standard
			return URLEncoder.encode(String.valueOf(x));
		}
	}

	/**
	 * Count API usage for api usage stats.
	 * @param url
	 */
	static void count(String url) {
		if (usage==null) return;
		// ignore parameters
		int i = url.indexOf("?");
		if (i!=-1) url = url.substring(0, i);		
		// for clarity
		i = url.indexOf("/1/");
		if (i!=-1) url = url.substring(i+3);
		// some calls - eg statuses/show - include the tweet id
		url = url.replaceAll("\\d+", "");
		// non-blocking
		for(int j = 0; j<100; j++) {
			Long v = usage.get(url);
			boolean done;
			if (v==null) {
				Long old = usage.putIfAbsent(url, 1L);
				done = old == null;
			} else {
				long nv = v+1;
				done = usage.replace(url, v, nv);
			}
			if (done) break;
		}
	}

	static Boolean getOptBoolean(JSONObject obj, String key) throws JSONException {
		Object o = obj.opt(key);
		if (o==null || o.equals(JSONObject.NULL)) return null;
		if (o.equals(Boolean.FALSE) ||
                (o instanceof String &&
                ((String)o).equalsIgnoreCase("false"))) {
            return false;
        } else if (o.equals(Boolean.TRUE) ||
                (o instanceof String &&
                ((String)o).equalsIgnoreCase("true"))) {
            return true;
        }
		throw new JSONException(o+" ("+key+") is not boolean");
	}

	public static void close(Closeable strm) {
		try {
			strm.close();
		} catch (IOException e) {
			// ignore
		}
	}

	
}
