package com.constantcontact.appconnect.loyalty;

import java.util.Comparator;


public class CouponDateComparator implements Comparator<Coupon> {
	private boolean _descending = true;
	
	public CouponDateComparator() {
	}

	@Override
	public int compare(Coupon lhs, Coupon rhs) {
		if (_descending) {
			return rhs.created_date.compareTo(lhs.created_date);
		}
		return lhs.created_date.compareTo(rhs.created_date);
	}
}
