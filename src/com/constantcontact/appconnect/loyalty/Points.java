package com.constantcontact.appconnect.loyalty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class Points {
	public int points = 1;
	
	public int bonus = 0;
	
	public Float revenue = 0f;

	public String msg = "";

	@JsonInclude(Include.NON_EMPTY)
	public String type = "pos";
		
	@JsonInclude(Include.NON_EMPTY)
	public String device_name;
}
