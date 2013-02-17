package winterwell.jtwitter.android;

import java.net.URI;

import winterwell.jtwitter.OAuthSignpostClient;
import winterwell.jtwitter.Twitter;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * A View for easily getting Twitter authorisation or doing login-by-Twitter.
 * 
 * <h3>Example</h3>
 * <code><pre>
 * AndroidTwitterLogin atl = new AndroidTwitterLogin(myApp, 
				MY_TWITTER_KEY,MY_TWITTER_SECRET,MY_TWITTER_CALLBACK) {					

	protected void onSuccess(Twitter jtwitter, String[] tokens) {
		jtwitter.setStatus("I can now post to Twitter!");
		// Recommended: store tokens in your app for future use
		// with the constructor OAuthSignpostClient(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret)
	}
 * };
 * atl.run();
 *	</pre></code>
 * @author Daniel Winterstein, John Turner
 */
public abstract class AndroidTwitterLogin {
		
	private String callbackUrl;

	private Activity context;

	/**
	 * The message that is shown to the user before
	 * directing them off to Twitter. Default is 
	 * "Please authorize with Twitter"
	 */
	public void setAuthoriseMessage(String authoriseMessage) {
		this.authoriseMessage = authoriseMessage;
	}
	
	/**
	 * @param myActivity This will have its conten view set, then reset.
	 * @param oauthAppKey 
	 * @param oauthAppSecret 
	 * @param calbackUrl Not important
	 */
	 public AndroidTwitterLogin(Activity myActivity, 
			 String oauthAppKey, String oauthAppSecret, String calbackUrl)
	 {
		this.context = myActivity;
		consumerKey =  oauthAppKey;
		consumerSecret = oauthAppSecret;
		this.callbackUrl = calbackUrl;					
		client = new OAuthSignpostClient(consumerKey, consumerSecret, callbackUrl);		
	}
	 
	OAuthSignpostClient client;

	private String authoriseMessage = "Please authorize with Twitter";

	private String consumerSecret;

	private String consumerKey;
			
	public final void run() {
		Log.i("jtwitter","TwitterAuth run!");		
		final WebView webview = new WebView(context);
		webview.setBackgroundColor(Color.BLACK);
		webview.setVisibility(View.VISIBLE);
		final Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		dialog.setContentView(webview);
		dialog.show();
		
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setWebViewClient(new WebViewClient() {
			
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				Log.d("jtwitter","url: "+url);				
				if ( ! url.contains(callbackUrl)) return;
				Uri uri = Uri.parse(url);
    			String verifier= uri.getQueryParameter("oauth_verifier");
    			if (verifier==null) {
    				// denied!
    				Log.i("jtwitter","Auth-fail: "+url);  			
    				dialog.dismiss();
    				onFail(new Exception(url));
    				return;
    			}    			
    			client.setAuthorizationCode(verifier);
    			String[] tokens = client.getAccessToken();
    			Twitter jtwitter = new Twitter(null, client);
    			Log.i("jtwitter","Authorised :)");
    			dialog.dismiss();
    			onSuccess(jtwitter, tokens);						
			}
			
			@Override public void onPageFinished(WebView view, String url) {
				Log.i("jtwitter","url finished: "+url);
			}
		});
		// Workaround for http://code.google.com/p/android/issues/detail?id=7189
		webview.requestFocus(View.FOCUS_DOWN);
		webview.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent e) {
				if (e.getAction()==MotionEvent.ACTION_DOWN
					|| e.getAction()== MotionEvent.ACTION_UP) {
					if (!v.hasFocus()) {
						v.requestFocus();
					}
		        }
		        return false;
			}
		});
		
		// getting the url to load involves a web call -- let the UI update first
		Toast.makeText(context, authoriseMessage, Toast.LENGTH_SHORT).show();
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				try {			
					URI authUrl =  client.authorizeUrl();
					webview.loadUrl(authUrl.toString());
				} catch (Exception e) {
					onFail(e);
				}				
			}			
		},10);
	}

	protected abstract void onSuccess(Twitter jtwitter, String[] tokens);

	protected void onFail(Exception e) {
		Toast.makeText(context, "Twitter authorisation failed?!", Toast.LENGTH_LONG).show();
		Log.w("jtwitter", e.toString());
	}

}
