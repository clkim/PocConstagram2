package com.constantcontact.oauth;

import java.io.IOException;
import java.util.ArrayList;

import roboguice.util.Ln;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class OAuthPreferences {
	private static final String LAST_USERNAME_KEY = "last_username";
	private static final String ACCOUNTS_KEY = "accounts";

	@Inject
	private SharedPreferences _prefs;

	private ObjectMapper _om;

	@Inject
	private OAuthPreferences(ObjectMapper om) {
		_om = om;
		_om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		_om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	}

	public String getLastUsername() {
		return _prefs.getString(LAST_USERNAME_KEY, "");
	}

	public void setLastUsername(String lastUsername) {
		Editor editor = _prefs.edit();
		editor.putString(LAST_USERNAME_KEY, lastUsername);
		editor.commit();
	}

	public ArrayList<Account> getAccounts() {
		ArrayList<Account> accounts = null;
		try {
			final String accountsPref = _prefs.getString(ACCOUNTS_KEY, "");
			accounts = _om.readValue(accountsPref, new TypeReference<ArrayList<Account>>() {});
		} catch (JsonParseException e) {
			Ln.e(e);
		} catch (JsonMappingException e) {
			Ln.e(e);
		} catch (IOException e) {
			Ln.e(e);
		}
		if (accounts == null) {
			return new ArrayList<Account>();
		}

		return accounts;
	}

	public void setAccounts(ArrayList<Account> accountsList) {
		try {
			Editor editor = _prefs.edit();
			editor.putString(ACCOUNTS_KEY, _om.writeValueAsString(accountsList));
			editor.commit();
		} catch (JsonGenerationException e) {
			Ln.e(e);
		} catch (JsonMappingException e) {
			Ln.e(e);
		} catch (IOException e) {
			Ln.e(e);
		}
	}
}
