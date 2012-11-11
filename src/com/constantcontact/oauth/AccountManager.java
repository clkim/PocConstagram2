package com.constantcontact.oauth;

import java.util.ArrayList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AccountManager {
	public static final String KEY_ACCOUNT_NAME = "account_name";

	private OAuthPreferences _prefs;

	private ArrayList<Account> _accounts;

	@Inject
	private AccountManager(OAuthPreferences prefs) {
		_prefs = prefs;
		_accounts = _prefs.getAccounts();
	}

	/**
	 * Gets the list of accounts saved for the app.
	 * 
	 * @return An array of Accounts
	 */
	public Account[] getAccounts() {
		return _accounts.toArray(new Account[_accounts.size()]);
	}

	/**
	 * Adds an account. If the account already exists, its information is updated.
	 * 
	 * @param account An Account instance
	 */
	public void addAccount(Account account) {
		int index = _accounts.indexOf(account);
		if (index == -1) {
			_accounts.add(account);
		} else {
			_accounts.set(index, account);
		}
		_prefs.setAccounts(_accounts);
	}
	
	/**
	 * Deletes the specified account. If account unknown, ignored.
	 * 
	 * @param account The account to delete.
	 */
	public void deleteAccount(Account account) {
		int index = _accounts.indexOf(account);
		if (index != -1) {
			_accounts.remove(index);
			_prefs.setAccounts(_accounts);
		}
	}

	/**
	 * Gets the last used account.
	 * 
	 * @return The last used Account, or null if none.
	 */
	public Account getLastAccount() {
		String lastUsername = _prefs.getLastUsername();
		for (int index = 0; index < _accounts.size(); ++index) {
			Account account = _accounts.get(index);
			if (account.getUsername().equals(lastUsername)) {
				return account;
			}
		}
		
		return null;
	}
	
	/**
	 * Sets the last used account.
	 * 
	 * @param account The last used account.
	 */
	public void setLastAccount(Account account) {
		_prefs.setLastUsername(account.getUsername());
	}
}
