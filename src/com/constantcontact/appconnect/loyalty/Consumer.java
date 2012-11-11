package com.constantcontact.appconnect.loyalty;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Consumer implements Serializable {
	@JsonIgnore
	private static final long serialVersionUID = 1L;
	
	public String id;
	public String email_address;
	public Date created_date;
	public Date updated_date;
}