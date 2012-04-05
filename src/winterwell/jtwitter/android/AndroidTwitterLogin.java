package winterwell.jtwitter.android;

import java.net.URI;

import winterwell.jtwitter.OAuthSignpostClient;
import winterwell.jtwitter.Twitter;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * A View for easily getting Twitter authorisation or doing login-by-Twitter.
 *	
 * @author John Turner, Daniel Winterstein
 */
public abstract class AndroidTwitterLogin {
		
	private String callbackUrl;

	private Activity context;

	/**
		@param oauthAppSecret 
	 * @param calbackUrl 
	 * @param msg The message that is shown to the user before
	 * directing them off to Twitter. Can be null for the default, which is 
	 * "Please authorize with Twitter"
	 */
	 public AndroidTwitterLogin(Activity myActivity, 
			 String oauthAppKey, String oauthAppSecret, String calbackUrl, 
			 String msg)
	 {
		this.context = myActivity;
		consumerKey =  oauthAppKey;
		consumerSecret = oauthAppSecret;
		this.callbackUrl = calbackUrl;			
		if (msg!=null) {
			authoriseMessage = msg;
		}
		client = new OAuthSignpostClient(consumerKey, consumerSecret, callbackUrl);
	}
	 
	OAuthSignpostClient client;

	private String authoriseMessage = "Please authorize with Twitter";

	private String consumerSecret;

	private String consumerKey;
	
	private boolean resetView = true;

	private View oldView;
			
	public final void run() {
		WebView webview = new WebView(context);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setVisibility(View.VISIBLE);
		if (resetView) {
			oldView = ((ViewGroup)context.findViewById(android.R.id.content)).getChildAt(0);
		}		
		context.setContentView(webview);
				
		webview.setWebViewClient(new WebViewClient() {
			@Override public void onPageFinished(WebView view, String url) {
				Log.i("jtwitter","TwitterAuth url: "+url);
				Uri uri = Uri.parse(url);
				if (url.contains(callbackUrl)) {
					uri.getQueryParameter("oauth_token");
        			String verifier= uri.getQueryParameter("oauth_verifier");
        			client.setAuthorizationCode(verifier);
        			String[] tokens = client.getAccessToken();
        			Twitter jtwitter = new Twitter(null, client);
        			if (resetView) {
        				context.setContentView(oldView);
        			}
        			onSuccess(jtwitter, tokens);
        			return;
				}
				// TODO onFail
				System.out.println("Fail?"+url);
			}
		});
		// Workaround for http://code.google.com/p/android/issues/detail?id=7189
		webview.requestFocus(View.FOCUS_DOWN);
		webview.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent e) {
				if (e.getAction()==MotionEvent.ACTION_DOWN
					|| e.getAction()==MotionEvent.ACTION_UP) {
					if ( ! v.hasFocus()) {
						v.requestFocus();
					}
		        }
		        return false;
			}
		});
		
		try {
			URI authUrl =  client.authorizeUrl();
			Toast.makeText(context, authoriseMessage, Toast.LENGTH_SHORT).show();
			webview.loadUrl(authUrl.toString());
		} catch (Exception e) {
			onFail(e);
		}
	}

	protected abstract void onSuccess(Twitter jtwitter, String[] tokens);

	protected void onFail(Exception e) {
		Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
		Log.e("jtwitter", e.toString());
	}

}
