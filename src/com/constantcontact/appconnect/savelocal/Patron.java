package com.constantcontact.appconnect.savelocal;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Patron implements Serializable
{
	@JsonIgnore
	private static final long serialVersionUID = 1L;
	
	public String id;
    public boolean facebook_share;
    public boolean twitter_share;
    public boolean share_emails_sent;
}
