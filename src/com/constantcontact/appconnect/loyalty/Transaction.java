package com.constantcontact.appconnect.loyalty;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Transaction implements Serializable {
		@JsonIgnore
		private static final long serialVersionUID = 1L;
		
		public int points;
		public int bonus;
//		public String revenue;
//		public String type;
//		public String message;
//		public String device_name;
		public Date created_date;
		public Date updated_date;
	}