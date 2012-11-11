package com.constantcontact.appconnect.savelocal;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Template implements Serializable
{
	@JsonIgnore
	private static final long serialVersionUID = 1L;

	public String id;
    public String deal_id;
    public String prerequisite_coupon_id;
    public int requires;
    public String coupon_type;
    public int price;
    public int value;
    public int order;
    public int limit;
    public int user_limit;
    public String fine_print;
    public String thank_you_message;
    public String valid_from;
    public String valid_to;
    public String headline;
    public int number_of_coupons;
    public int coupons;
}