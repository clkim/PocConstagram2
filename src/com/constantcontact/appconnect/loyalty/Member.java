package com.constantcontact.appconnect.loyalty;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Member implements Serializable {
	@JsonIgnore
	private static final long serialVersionUID = 1L;

	public String status;

	public Consumer consumer;
	public long contacts_id;
	public String program_id;
	public String member_pass_url;
	public String first_name;
	public String last_name;
	public String loyalty_number;
	public Date created_date;
	public Date updated_date;
	public int points_current;
	public int points_lifetime;

	public Transaction[] transactions;
	public Coupon[] coupons;
}
