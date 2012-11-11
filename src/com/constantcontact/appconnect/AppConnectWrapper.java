package com.constantcontact.appconnect;

import com.constantcontact.oauth.Account;


public interface AppConnectWrapper {

	public abstract void setAccount(Account account);
	
	public abstract Account getAccount();
	
	public abstract String getRedirectUrl(String environment);
	
	public abstract String getTokenAuthenticationUrl(String environment);

	public abstract AppConnectApi getApi();

}