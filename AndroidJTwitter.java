package winterwell.jtwitter;


import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import winterwell.jtwitter.OAuthSignpostClient;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 
 * @author Daniel, Marko from Marakana.com
 */
public class TwitterActivity extends Activity {
	
  private static final String TAG = "OAuthDemo";
  private static final String OAUTH_KEY = "YOUR_KEY_GOES_HERE";
  private static final String OAUTH_SECRET = "YOUR_SECRET_GOES_HERE";
  private static final String OAUTH_CALLBACK_SCHEME = "x-oauth-twitter";
  private static final String OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME
      + "://callback";
  private static final String TWITTER_USER = "YOUR_EMAIL_GOES_HERE";

  private OAuthSignpostClient oauthClient;
  private OAuthConsumer mConsumer;
  private OAuthProvider mProvider;
  private Twitter twitter;

  /* Callback once we are done with the authorization of this app with Twitter. */
  @Override
  public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    Log.d(TAG, "intent: " + intent);
    // Check if this is a callback from OAuth
    Uri uri = intent.getData();
    if (uri != null && uri.getScheme().equals(OAUTH_CALLBACK_SCHEME)) {
      Log.d(TAG, "callback: " + uri.getPath());

      String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
      Log.d(TAG, "verifier: " + verifier);

      new RetrieveAccessTokenTask().execute(verifier);
    }
  }

  /* Responsible for starting the Twitter authorization */
  class OAuthAuthorizeTask extends AsyncTask<Void, Void, String> {

    @Override
    protected String doInBackground(Void... params) {
      String authUrl;
      String message = null;
      try {
        authUrl = mProvider.retrieveRequestToken(mConsumer, OAUTH_CALLBACK_URL);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        startActivity(intent);
      } catch (OAuthMessageSignerException e) {
        message = "OAuthMessageSignerException";
        e.printStackTrace();
      } catch (OAuthNotAuthorizedException e) {
        message = "OAuthNotAuthorizedException";
        e.printStackTrace();
      } catch (OAuthExpectationFailedException e) {
        message = "OAuthExpectationFailedException";
        e.printStackTrace();
      } catch (OAuthCommunicationException e) {
        message = "OAuthCommunicationException";
        e.printStackTrace();
      }
      return message;
    }
  }

  /* Responsible for retrieving access tokens from twitter */
  class RetrieveAccessTokenTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
      String message = null;
      String verifier = params[0];
      try {
        // Get the token
        Log.d(TAG, "mConsumer: " + mConsumer);
        Log.d(TAG, "mProvider: " + mProvider);
        mProvider.retrieveAccessToken(mConsumer, verifier);
        String token = mConsumer.getToken();
        String tokenSecret = mConsumer.getTokenSecret();
        mConsumer.setTokenWithSecret(token, tokenSecret);

        Log.d(TAG, String.format("verifier: %s, token: %s, tokenSecret: %s",
            verifier, token, tokenSecret));

        // Store token in prefs
        prefs.edit().putString("token", token).putString("tokenSecret",
            tokenSecret).commit();

        // Make a Twitter object
        oauthClient = new OAuthSignpostClient(OAUTH_KEY, OAUTH_SECRET, token,
            tokenSecret);
        twitter = new Twitter("MarkoGargenta", oauthClient);

        Log.d(TAG, "token: " + token);
      } catch (OAuthMessageSignerException e) {
        message = "OAuthMessageSignerException";
        e.printStackTrace();
      } catch (OAuthNotAuthorizedException e) {
        message = "OAuthNotAuthorizedException";
        e.printStackTrace();
      } catch (OAuthExpectationFailedException e) {
        message = "OAuthExpectationFailedException";
        e.printStackTrace();
      } catch (OAuthCommunicationException e) {
        message = "OAuthCommunicationException";
        e.printStackTrace();
      }
      return message;
    }
  }
   
}
