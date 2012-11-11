package com.constantcontact.appconnect.loyalty;

import java.util.Comparator;



public class TransactionDateComparator implements Comparator<Transaction> {
	private boolean _descending = true;
	
	public TransactionDateComparator() {
	}

	@Override
	public int compare(Transaction lhs, Transaction rhs) {
		if (_descending) {
			return rhs.created_date.compareTo(lhs.created_date);
		}
		return lhs.created_date.compareTo(rhs.created_date);
	}

}
