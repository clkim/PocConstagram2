package com.constantcontact.oauth;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class Account implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String _username;
	
	private String _token;
	
	@JsonInclude(Include.NON_EMPTY)
	private String _environment;

	private long _expiration;

	protected Account() {
	}
	
	public Account(String username, String token, String environment, long expiration) {
		_username = username;
		_token = token;
		_environment = environment;
		_expiration = expiration;
	}

	public void setUsername(String username) {
		_username = username;
	}
	
	public String getUsername() {
		return _username;
	}

	public void setToken(String token) {
		_token = token;
	}
	
	public String getToken() {
		return _token;
	}

	public void setEnvironment(String environment) {
		_environment = environment;
	}
	
	public String getEnvironment() {
		return _environment;
	}

	public void setExpiration(long expiration) {
		_expiration = expiration;
	}
	
	public long getExpiration() {
		return _expiration;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Account) {
			Account account = (Account) o;
			return account.getUsername().equals(_username) && account.getEnvironment().equals(_environment);
		}
		
		return false;
	}
}
