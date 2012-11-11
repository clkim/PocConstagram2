package com.constantcontact.oauth;

import roboguice.RoboGuice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.inject.Inject;

public class AccountArrayAdapter extends ArrayAdapter<Account> {

	@Inject
	private LayoutInflater _inflater;
	private boolean _multiSelectionMode;

	public AccountArrayAdapter(Context context, Account[] accounts) {
		super(context, 0, accounts);
		RoboGuice.getInjector(context).injectMembers(this);
	}

	void setMultiSelectionMode(boolean multiSelectionMode) {
		_multiSelectionMode = multiSelectionMode;
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			if (_multiSelectionMode) {
				convertView = _inflater.inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
				convertView.setTag(false);
			} else {
				convertView = _inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
				convertView.setTag(true);
			}
		} else {
			Boolean isSingleSelect = (Boolean) convertView.getTag();
			if (_multiSelectionMode && isSingleSelect) {
				convertView = _inflater.inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
				convertView.setTag(false);
			} else if (!_multiSelectionMode && !isSingleSelect) {
				convertView = _inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
				convertView.setTag(true);				
			}
		}

		TextView view = (TextView) convertView;
		Account account = getItem(position);

		view.setText(account.getUsername());

		return convertView;
	}

	private class Holder {

	}
}
