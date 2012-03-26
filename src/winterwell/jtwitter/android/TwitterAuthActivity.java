package winterwell.jtwitter.android;

import java.net.URI;

import winterwell.jtwitter.OAuthSignpostClient;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * An activity for easily getting Twitter authorisation. This can also be used
 * for doing login-by-Twitter.
 * <p>
 * On success, this returns an Intent which can be used as follows:
 * <pre><code>
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (resultCode == TwitterAuthActivity.TWITTER_RESULT) {
							
			String accessToken = intent.getStringExtra("accessToken");
			String accessTokenSecret = intent.getStringExtra("accessTokenSecret");
			// Note: Best practice is to store these tokens for future use.

			OAuthSignpostClient client = new OAuthSignpostClient(MY_APP_KEY, MY_APP_SECRET, accessToken, accessTokenSecret);
			
			// Ready to go!
			Twitter twitter = new Twitter(null, client);
		}
	}
	</code></pre>
 * @author John Turner, Daniel Winterstein
 */
public class TwitterAuthActivity extends Activity {
	
	/**
	 * An arbitrary number. Use this in 
	 * onActivityResult() to detect that it's TwitterAuthActivity returning.
	 */
	public static final int TWITTER_RESULT_CODE = 872;
	
	private String callbackUrl;

	/**
	 * Create an Intent for launching this using myActivity.startActivityForResult().
		@param oauthAppSecret 
	 * @param calbackUrl 
	 * @param msg The message that is shown to the user before
	 * directing them off to Twitter. Can be null for the default, which is 
	 * "Please authorize with Twitter"
	 */
	 public static Intent newIntent(Activity myActivity, 
			 String oauthAppKey, String oauthAppSecret, String calbackUrl, 
			 String msg)
	 {
		Intent taa = new Intent(myActivity, TwitterAuthActivity.class);
		Bundle bundle = taa.getExtras();
		bundle.putString("consumerKey", oauthAppKey);
		bundle.putString("consumerSecret", oauthAppSecret);
		bundle.putString("callbackUrl", calbackUrl);			
		if (msg!=null) {
			bundle.putString("authoriseMessage", msg);
		}
		return taa;
	}
	
	public TwitterAuthActivity() {		
	}
	
	public TwitterAuthActivity(String oauthKey, String oauthSecret, String callbackUrl) {
		this.callbackUrl = callbackUrl;
		client = new OAuthSignpostClient(oauthKey, oauthSecret, callbackUrl);
	}
	
	OAuthSignpostClient client;

	private String authoriseMessage = "Please authorize with Twitter";

	private String consumerSecret;

	private String consumerKey;
	
	
	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		setup(state);
	}			
	
	@Override
	protected void onResume() {
		super.onResume();
		Bundle b = this.getIntent().getExtras();
		setup(b);
		
		WebView webview = new WebView(this);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setVisibility(View.VISIBLE);
		setContentView(webview);
		
		webview.setWebViewClient(new WebViewClient() {
			@Override public void onPageFinished(WebView view, String url) {
				Uri uri = Uri.parse(url);
				if (url.contains(callbackUrl)) {
					uri.getQueryParameter("oauth_token");
        			String verifier= uri.getQueryParameter("oauth_verifier");
        			client.setAuthorizationCode(verifier);
        			String[] tokens = client.getAccessToken();

        			Intent result = new Intent();
        			result.putExtra("accessToken", tokens[0]);
        			result.putExtra("accessTokenSecret", tokens[1]);
        			setResult(TWITTER_RESULT_CODE, result);
        			finish();
				}
			}
		});
		try {
			URI authUrl =  client.authorizeUrl();
			Toast.makeText(this, authoriseMessage, Toast.LENGTH_SHORT).show();
			webview.loadUrl(authUrl.toString());
		} catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	private void setup(Bundle b) {
		callbackUrl = b.getString("callbackUrl");		
		String msg = b.getString("authoriseMessage");
		if (msg!=null) {
			authoriseMessage = msg;
		}
		consumerKey = b.getString("consumerKey");
		consumerSecret = b.getString("consumerSecret");
		client = new OAuthSignpostClient(consumerKey, consumerSecret, callbackUrl);
	}
}
