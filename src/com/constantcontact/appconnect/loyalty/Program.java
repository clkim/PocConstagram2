package com.constantcontact.appconnect.loyalty;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Program implements Serializable {
	@JsonIgnore
	private static final long serialVersionUID = 1L;
	
	public String id;
	public String color;
	public Reward[] rewards;
	public ProgramType type;
	public String org_name;
}
