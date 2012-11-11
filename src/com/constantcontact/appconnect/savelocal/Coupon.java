package com.constantcontact.appconnect.savelocal;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Coupon implements Serializable
{
	@JsonIgnore
	private static final long serialVersionUID = 1L;
	
	public String id;
    public String patron_id;
    public String coupon_template_id;
    public String earned_how;
    public int purchased_price;
    public boolean redeemed;
    public String code;
    public String redeemed_on;
    public boolean refunded;
    public boolean already_redeemed;
    public boolean already_refunded;
    public String pay_key;
    public String created_at;
    public String updated_at;
    public Patron patron;
    public Template template;
}