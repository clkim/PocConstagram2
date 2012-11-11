package com.constantcontact.appconnect.loyalty;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Reward implements Serializable {
	@JsonIgnore
	private static final long serialVersionUID = 1L;
	
	public String title;
	public String terms;
	public CouponStatus status;
	public String reward_description;
	public Date created;
	public Date updated;
}