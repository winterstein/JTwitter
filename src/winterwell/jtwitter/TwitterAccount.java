package winterwell.jtwitter;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import winterwell.jtwitter.Twitter.User;

/**
 * <p><b>Status: sketch!</b></p>
 * 
 * Access the account methods: e.g. change your profile colours.
 * 
 * <p>
 * These methods are here, rather than in Twitter, at least for now, because:<br>
 * (a) the Twitter class is getting crowded,<br>
 * (b) I'm undecided on how best to support them, so keeping them
 * in a separate "subject to change" class.
 * 
 * @author Daniel Winterstein
 *
 */
public class TwitterAccount {

	final Twitter jtwit;
	
	public TwitterAccount(Twitter jtwit) {
		assert jtwit.getHttpClient().canAuthenticate();
		this.jtwit = jtwit;
	}
	
	@Override
	public String toString() {
		return "TwitterAccount["+jtwit.getScreenName()+"]";
	}
	
	public static String COLOR_BG = "profile_background_color";
	public static String COLOR_TEXT = "profile_text_color";
	public static String COLOR_LINK = "profile_link_color";
	public static String COLOR_SIDEBAR_FILL = "profile_sidebar_fill_color";
	public static String COLOR_SIDEBAR_BORDER = "profile_sidebar_border_color";
	
	/**
	 * Update profile. 
	 * @param name Can be null for no change. Full name associated with the profile. Maximum of 20 characters.
	  @param url Can be null for no change. URL associated with the profile. Will be prepended with "http://" if not present. Maximum of 100 characters.
	  @param location Can be null for no change. The city or country describing where the user of the account is located. The contents are not normalized or geocoded in any way. Maximum of 30 characters.
	  @param description Can be null for no change. A description of the user. Maximum of 160 characters.
	 * @return updated User object
	 */
	public User setProfile(String name, String url, String location, String description) 
	{		
		Map<String, String> vars = Twitter.asMap("name", name, "url", url, "location", location, "description", description);
		String apiUrl = jtwit.TWITTER_URL+"/account/update_profile.json";
		String json = jtwit.getHttpClient().post(apiUrl, vars, true);
		return user(json);
	}
	
	/**
	 * Set the authenticating user's colors.
	 * @param colorName2hexCode Use the COLOR_XXX constants as keys, and
	 * 3 or 6 letter hex-codes as values (e.g. 0f0 or 00ff00 both code
	 * for green). You can set as many colors as you like (but at least one).
	 * @return updated User object
	 */
	public User setProfileColors(Map<String, String> colorName2hexCode) {		
		assert ! colorName2hexCode.isEmpty();
		String url = jtwit.TWITTER_URL+"/account/update_profile_colors.json";
		String json = jtwit.getHttpClient().post(url, colorName2hexCode, true);
		return user(json);
	}
	
	private User user(String json) {
		try {
			JSONObject obj = new JSONObject(json);
			User u = new User(obj, null);
			return u;
		} catch (JSONException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * Test the login credentials -- and get some user info.
	 * @return a representation of the requesting user if authentication 
	 * was successful
	 * @throws TwitterException.E401 thrown if the authorisation credentials fail.
	 * 
	 * @see Twitter#isValidLogin()
	 */
	public User verifyCredentials() throws TwitterException.E401 {
		String url = jtwit.TWITTER_URL+"/account/verify_credentials.json";
		String json = jtwit.getHttpClient().getPage(url, null, true); 
		return user(json);
	}
	
}
