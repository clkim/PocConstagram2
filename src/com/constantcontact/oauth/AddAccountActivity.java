package com.constantcontact.oauth;

import java.util.Calendar;

import roboguice.inject.InjectView;
import roboguice.util.Ln;
import roboguice.util.SafeAsyncTask;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.constantcontact.appconnect.AppConnectApi;
import com.constantcontact.appconnect.AppConnectWrapper;
import com.examplectct.pocconstagram2.BuildConfig;
import com.examplectct.pocconstagram2.R;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockActivity;
import com.google.inject.Inject;

public class AddAccountActivity extends RoboSherlockActivity {
	@Inject
	private AppConnectWrapper _appConnect;

	@Inject
	private AccountManager _accountManager;

	@InjectView(tag = "webview")
	private WebView _webView;

	@InjectView(tag = "progress_frame")
	private View _progressFrame;

	@InjectView(tag = "error")
	private View _error;

	/** Keep track of the login task so can cancel it if requested */
	private GetUserNameTask _authTask = null;

	private String _redirectUrl;

	private String _correctedRedirectUrl;

	private String _selectedEnvironment = "";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.act_authenticator);

		final ActionBar actionBar = getSupportActionBar();
		if (BuildConfig.DEBUG) {
			Context context = getSupportActionBar().getThemedContext();
			actionBar.setDisplayShowTitleEnabled(false);
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			final String[] serverEntries = getResources().getStringArray(R.array.servers_entries);
			final SpinnerAdapter adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, serverEntries);
			actionBar.setListNavigationCallbacks(adapter, new OnNavigationListener() {
				@Override
				public boolean onNavigationItemSelected(int itemPosition, long itemId) {
					_selectedEnvironment = serverEntries[itemPosition].toLowerCase();
					if ("public".equals(_selectedEnvironment)) {
						_selectedEnvironment = "";
					}
					loadAuthenticationUrl();
					return true;
				}
			});
		}

		_webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				super.onReceivedError(view, errorCode, description, failingUrl);
				_error.setVisibility(View.VISIBLE);
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				showProgress();
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				if (_authTask == null) {
					hideProgress();
				}
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Ln.d("Loading url: " + url);
				if (url.startsWith(_redirectUrl) || url.startsWith(_correctedRedirectUrl)) {

					// extract OAuth2 access_token appended in url
					Uri uri = Uri.parse(url.replace("#", "?"));
					String accessToken = uri.getQueryParameter("access_token");
					String expiresIn = uri.getQueryParameter("expires_in");
					String tokenType = uri.getQueryParameter("token_type");
					if (TextUtils.isEmpty(accessToken)) {
						// TODO error condition
					} else {
						long expiration;
						try {
							int expiresInValue = Integer.parseInt(expiresIn);
							Calendar cal = Calendar.getInstance();
							cal.add(Calendar.SECOND, expiresInValue);
							expiration = cal.getTimeInMillis();
						} catch (NumberFormatException e) {
							expiration = -1;
						}

						showProgress();
						_authTask = new GetUserNameTask(accessToken, tokenType, expiration);
						_authTask.execute();
					}

					// don't go to redirectUri
					return true;
				}

				// load the webpage from url (login and grant access)
				return super.shouldOverrideUrlLoading(view, url); // return false;
			}
		});

		// Clear cookies
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();

		// Clear browser cache
		_webView.clearCache(true);

		// Disable caching, password saving and more.
		WebSettings settings = _webView.getSettings();
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		settings.setSavePassword(false);
		settings.setSaveFormData(false);
		settings.setLoadWithOverviewMode(false);
		settings.setSupportZoom(false);

		loadAuthenticationUrl();
	}

	private void loadAuthenticationUrl() {
		showProgress();

		_redirectUrl = _correctedRedirectUrl = _appConnect.getRedirectUrl(_selectedEnvironment);
		int schemeDividerIndex = _redirectUrl.indexOf(':');
		int miscColonIndex = _redirectUrl.indexOf(':', schemeDividerIndex + 1);
		if (miscColonIndex > -1) {
			// NOTE: Hack for screwy redirect url set up for CTCT apps (http://x-myapp://oauth). ICS+ Android browser removes the
			// second colon on the redirect url.
			_correctedRedirectUrl = _redirectUrl.substring(0, miscColonIndex) + _redirectUrl.substring(miscColonIndex + 1);
		}
		Ln.d("redirect: " + _redirectUrl + " corrected: " + _correctedRedirectUrl);

		_webView.loadUrl(_appConnect.getTokenAuthenticationUrl(_selectedEnvironment));
	}

	/**
	 * Called when the authentication process completes (see attemptLogin()).
	 * 
	 * @param authToken
	 *            the authentication token returned by the server, or NULL if authentication failed.
	 * @param expiration
	 */
	public void onAuthenticationResult(String username, String authToken, String tokenType, long expiration) {

		boolean success = ((authToken != null) && (authToken.length() > 0));
		Ln.i("onAuthenticationResult(" + success + ")");

		// Our task is complete, so clear it out
		_authTask = null;

		// Hide the progress dialog
		hideProgress();

		if (success) {
			if (BuildConfig.DEBUG && _selectedEnvironment.length() > 0) {
				username = username + " - " + _selectedEnvironment;
			}
			final Account account = new Account(username, authToken, _selectedEnvironment, expiration);
			_accountManager.addAccount(account);

			final Intent intent = new Intent();
			intent.putExtra(OAuthConstants.KEY_ACCOUNT, account);
			setResult(RESULT_OK, intent);
			finish();
		} else {
			Ln.e("onAuthenticationResult: failed to authenticate");
			// "Please enter a valid username/password.
//				mMessage.setText(getText(R.string.login_activity_loginfail_text_both));
		}
	}

	public void onAuthenticationCancel() {
		Ln.i("onAuthenticationCancel()");

		// Our task is complete, so clear it out
		_authTask = null;

		// Hide the progress dialog
		hideProgress();
	}

	/**
	 * Shows the progress UI for a lengthy operation.
	 */
	private void showProgress() {
		_error.setVisibility(View.GONE);
		if (_progressFrame.getVisibility() != View.VISIBLE) {
			_progressFrame.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Hides the progress UI for a lengthy operation.
	 */
	private void hideProgress() {
		_progressFrame.setVisibility(View.GONE);
	}

	public class GetUserNameTask extends SafeAsyncTask<Boolean> {
		private final String _authToken;
		private String _username;
		private final long _expiration;
		private final String _tokenType;

		public GetUserNameTask(String authToken, String tokenType, long expiration) {
			_authToken = authToken;
			_expiration = expiration;
			_tokenType = tokenType;
		}

		@Override
		protected void onPreExecute() throws Exception {
			showProgress();
		}

		@Override
		public Boolean call() throws Exception {
			_username = AppConnectApi.fetchUserNameForToken(_selectedEnvironment, _authToken);
			return true;
		}

		@Override
		protected void onSuccess(Boolean success) throws Exception {
			if (success) {
				onAuthenticationResult(_username, _authToken, _tokenType, _expiration);
			}
		}

		@Override
		protected void onFinally() throws RuntimeException {
			hideProgress();
		}
	}
}
