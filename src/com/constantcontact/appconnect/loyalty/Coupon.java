package com.constantcontact.appconnect.loyalty;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Coupon implements Serializable {
	@JsonIgnore
	private static final long serialVersionUID = 1L;
	
	public String id;
	public Date expires;
	public CouponStatus status;
	public String member_id;
	public String reward_id;
	public String coupon_code;
	public String coupon_pass_url;
	public Date created_date;
	public Date updated_date;
}