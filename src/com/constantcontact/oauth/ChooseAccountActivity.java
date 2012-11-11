package com.constantcontact.oauth;

import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.examplectct.pocconstagram2.R;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockListActivity;
import com.google.inject.Inject;

public class ChooseAccountActivity extends RoboSherlockListActivity {
	private static final int ADD_ACCOUNT_REQUEST = 0;

	@Inject
	private AccountManager _am;

	private AccountArrayAdapter _adapter;

	private ActionMode _actionMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_accounts);

//		ActionBar actionBar = getSupportActionBar();
//		actionBar.setDisplayShowTitleEnabled(false);

		Account[] accounts = _am.getAccounts();

		if (accounts.length == 0) {
			addAccount();
		} else {
			getListView().setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					if (_actionMode == null) {
						Intent data = new Intent();
						data.putExtra(OAuthConstants.KEY_ACCOUNT, _adapter.getItem(position));
						setResult(RESULT_OK, data);
						finish();
					} else {
						SparseBooleanArray checkedItemPositions = getListView().getCheckedItemPositions();
						if (checkedItemPositions.size() == 0) {
							if (_actionMode != null) {
								_actionMode.finish();
							}
						} else {
							getListView().setItemChecked(position, checkedItemPositions.get(position, true));
						}
					}
				}
			});
			getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					if (_actionMode != null) {
						return false;
					}

					_adapter.setMultiSelectionMode(true);
					_actionMode = startActionMode(_actionModeCallback);
					getListView().setItemChecked(position, true);

					return true;
				}
			});
		}
	}

	private void initializeList() {
		Account[] accounts = _am.getAccounts();
		_adapter = new AccountArrayAdapter(this, accounts);
		getListView().setAdapter(_adapter);
		getListView().clearChoices();
		getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
		getListView().setLongClickable(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		initializeList();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.act_choose_account, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_add_account) {
			addAccount();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case ADD_ACCOUNT_REQUEST:
				if (resultCode == RESULT_OK) {
					Account account = (Account) data.getSerializableExtra(OAuthConstants.KEY_ACCOUNT);

					Intent result = new Intent();
					result.putExtra(OAuthConstants.KEY_ACCOUNT, account);
					setResult(RESULT_OK, result);
					finish();
				}

				break;

			default:
				break;
		}
	}

	private void addAccount() {
		Intent intent = new Intent(this, AddAccountActivity.class);
		startActivityForResult(intent, ADD_ACCOUNT_REQUEST);
	}

	private ActionMode.Callback _actionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.getMenuInflater().inflate(R.menu.ctx_delete_account, menu);
			getListView().setLongClickable(false);
			getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (item.getItemId() == R.id.delete) {
				SparseBooleanArray checkedItemPositions = getListView().getCheckedItemPositions();

				_adapter.setNotifyOnChange(false);
				for (int index = 0; index < checkedItemPositions.size(); ++index) {
					int position = checkedItemPositions.keyAt(index);
					Account account = _adapter.getItem(position);
					_am.deleteAccount(account);
				}
				_adapter.notifyDataSetChanged();

				mode.finish();

				return true;
			}
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			_actionMode = null;
			initializeList();
		}
	};
}
