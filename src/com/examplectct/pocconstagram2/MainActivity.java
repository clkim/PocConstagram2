package com.examplectct.pocconstagram2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import roboguice.util.Ln;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.constantcontact.appconnect.AppConnectApi;
import com.constantcontact.appconnect.AppConnectApi.Result;
import com.constantcontact.appconnect.ConstantContactApiException;
import com.constantcontact.appconnect.campaigns.Campaign;
import com.constantcontact.appconnect.campaigns.MessageFooter;
import com.constantcontact.appconnect.campaigns.Schedule;
import com.constantcontact.appconnect.campaigns.SentToContactList;
import com.constantcontact.appconnect.contacts.EmailList;
import com.constantcontact.oauth.Account;

public class MainActivity extends SherlockFragmentActivity {
	private static final String USERNAME		= "ckim201211";
	private static final String TOKEN			= "7257c21e-461e-451e-af2d-e826951f2481";
	private static final String ENVIRONMENT		= "l1";
	private static final long EXPIRATION		= 0l; // not used?
	
	private static final String TAG_LOG						= "MainActivity";
	private static final int ACTION_PICK_PHOTO_GALLERY		= 1;	// there is a bug, see
	private static final int ACTION_PICK_PHOTO_GALLERY_BUG	= 65537;// https://groups.google.com/forum/?fromgroups=#!topic/android-developers/NiM_dAOtXQU
	
	private static MainActivity mainActivity;
	private static View templatesView;
	private static View audienceView;
	private static RadioGroup templatesRadioGroup;
	private static WebView templatesWebview;
	private static ImageView campaignImageView;
	private static Bitmap mImageBitmap;
	private static EditText titleSubject;
	private static EditText content;
	private static EditText webSite;
	
	private static AppConnectApi acApi;
	private static Account account;
	private static Handler handler;
	private static String emailContent;
	
	private static ArrayList<EmailList> emailLists;
//	private static ArrayList<Campaign> draftCampaigns;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * sections. We use a {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will
     * keep every loaded fragment in memory. If this becomes too memory intensive, it may be best
     * to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG_LOG, "**Starting activity onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());


        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        
        mViewPager.setOnPageChangeListener(new SimpleOnPageChangeListener() {
        	
        	@Override
        	public void onPageScrolled (int position, float positionOffset, int positionOffsetPixels) {
        		if (position == 0 && positionOffset > 0) {
        			templatesWebview.loadDataWithBaseURL("constantcontact.com", jReadEmailContentFromFile(R.raw.invtempl1),
        					"text/html", "UTF-8", null);
        		}
        	}
        });
        
        
        // set up handler for worker thread to do stuff on this UI thread
        handler = new Handler();
        // save activity object for use by helper method
        mainActivity = this;
        
        // API call in AsyncTask worker thread to get email lists
        AsyncTask<Void, Void, Void> getEmailListsAsyncTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... v) {
				acApi = new AppConnectApi(ENVIRONMENT);
				account = new Account(USERNAME, TOKEN, ENVIRONMENT, EXPIRATION);
				acApi.setAccount(account);
				
				EmailList[] elArray = null;
//				Campaign[] draftcampaignArray = null;
				try {
					Result<EmailList[]> result = acApi.getLists();
					elArray = result.getResult();
					
//					Result<Campaign[]> result2 = acApi.getCampaigns(CampaignStatus.DRAFT);
//					draftcampaignArray = result2.getResult();
				} catch (ConstantContactApiException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// store email lists
				emailLists = new ArrayList<EmailList>();
				if (elArray!=null && elArray.length>0) {
					for (int i = 0; i < elArray.length; i++) {
			        	emailLists.add(elArray[i]);
			        }
				}
				
//				// store draft campaigns 
//				draftCampaigns = new ArrayList<Campaign>();
//				if (draftcampaignArray!=null && draftcampaignArray.length>0) {
//					for (int i = 0; i < draftcampaignArray.length; i++) {
//						draftCampaigns.add(draftcampaignArray[i]);
//					}
//				}
				
				Log.d( TAG_LOG, "emailLists size "+(emailLists==null ? 0 : emailLists.size()) );
//				Log.d(TAG_LOG, "draftCampaigns size "+draftCampaigns.size());
				
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
//				templatesView = createRadioGroupInTemplatesFragment(templatesView);
				audienceView = createRadioGroupInAudienceFragment(audienceView);
				mSectionsPagerAdapter.notifyDataSetChanged();
				Log.d(TAG_LOG, "**Ending AsyncTask onPostExecute()");
			}
		};
		getEmailListsAsyncTask.execute(null, null, null);
        
//        // Temp fake the email lists array which would be returned in api call getLists()
//		AsyncTask<Void, Void, Void> getFakeEmailListsAsyncTask = new AsyncTask<Void, Void, Void>() {
//		
//			@Override
//			protected Void doInBackground(Void... v) {
//				//String[] fakeEmailListNames = {};
//				String[] fakeEmailListNames = {"Coupon of the Day List", "Specials List", "Xtras List", "should not appear"};
//				int numberEmailLists = fakeEmailListNames.length;
//				EmailList[] elArray = new EmailList[numberEmailLists];
//				for (int i = 0; i<numberEmailLists; i++) {
//					elArray[i] = new EmailList();
//				}
//				for (int i = 0; i<numberEmailLists; i++) {
//					elArray[i].name = fakeEmailListNames[i];
//					elArray[i].status = EmailListStatus.ACTIVE;
//				}
//				// wait to simulate api call delay
//				try {
//					Thread.sleep(3000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				// Get the email lists
//				for (int i = 0; i<numberEmailLists; i++) {
//					emailLists.add(elArray[i]);
//				}
//				Log.d(TAG_LOG, "emailLists size "+emailLists.size() +", first listname "+emailLists.get(0).name);
//				
//				return null;
//			}
//			
//			@Override
//			protected void onPostExecute(Void result) {
//				templatesView = doCreateTemplatesView(templatesView);
//				mSectionsPagerAdapter.notifyDataSetChanged();
//				Log.d(TAG_LOG, "**Ending AsyncTask onPostExecute()");
//			}
//		};
//		getFakeEmailListsAsyncTask.execute(null, null, null);
		
		
        Log.d(TAG_LOG, "**Ending activity onCreate()");
    }

    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		switch (requestCode) {
		case ACTION_PICK_PHOTO_GALLERY_BUG: {
			if (resultCode == RESULT_OK){  
	            Uri selectedImageUri = intent.getData();
	            String mCurrentPhotoPath = mParseUriToFilepath(selectedImageUri);
	            setPicFromExifThumbnail(mCurrentPhotoPath);
	        }
		}
		}
	}
    
	private String mParseUriToFilepath(Uri uri) {
		// copied over from UploadMLP app
		// if from Gallery app after picking a DCIM/Camera image, uri is 'content://media/external/images/media/1'
		// if from Gallery app after picking a Pictures folder image, uri is 'content://media/external/images/media/2'
		String[] projection = { MediaStore.Images.Media.DATA }; // value is '_data'
		Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
		if (cursor != null) {
			// could be null if uri is not from Gallery app
			//  e.g. if you used OI file manager for picking the media
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			String selectedImagePath = cursor.getString(column_index);
			cursor.close();
			if (selectedImagePath != null) {
				return selectedImagePath;
			}
		}
		// if uri is not from Gallery app, e.g. from OI file manager
		String filemanagerPath = uri.getPath();
		if (filemanagerPath != null) {
			return filemanagerPath;
		}

		return null;
	}
	
	private void setPicFromExifThumbnail(String mCurrentPhotoPath) {
		try {
			ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
			mImageBitmap = null;
			if (exif.hasThumbnail()) { //TODO a picture cropped within Gallery app seems does not have exif so will not display
				byte[] data = exif.getThumbnail();
				mImageBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
				// rotate if portrait
				if (ExifInterface.ORIENTATION_ROTATE_90 == exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
					Matrix matrix = new Matrix();
					matrix.preRotate(90f);
					mImageBitmap = Bitmap.createBitmap(mImageBitmap, 0, 0, mImageBitmap.getWidth(), mImageBitmap.getHeight(), matrix, true);
				}
			}
			// associate the bitmap to the imageView
			if (mImageBitmap!=null) campaignImageView.setImageBitmap(mImageBitmap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
        	switch (i) {
        	// if section position 0 return Campaign fragment
        	case 0: return new CampaignFragment();
        	// if section position 1 return Templates fragment
        	case 1: return new TemplatesFragment();
        	// if section position 2 return Audience fragment
        	case 2: return new AudienceFragment();
        	}
        	return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getString(R.string.title_section1).toUpperCase();
                case 1: return getString(R.string.title_section2).toUpperCase();
                case 2: return getString(R.string.title_section3).toUpperCase();
            }
            return null;
        }
    }

    /**
     * The Campaign fragment in first section of the app
     */
    public static class CampaignFragment extends SherlockFragment {
    	public CampaignFragment() {
    	}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			Log.d(TAG_LOG, "**Starting/Ending CampaignFragment's onCreateView()");
			View view = inflater.inflate(R.layout.activity_fragment_campaign, container, false);
			
			// get title text view
			titleSubject = (EditText) view.findViewById(R.id.editTextTitleSubject);
			content = (EditText) view.findViewById(R.id.editTextContent);
			webSite = (EditText) view.findViewById(R.id.editTextWebsite);
			
			// set image view
			campaignImageView = (ImageView) view.findViewById(R.id.imageview);
			if (mImageBitmap!=null) campaignImageView.setImageBitmap(mImageBitmap);
			
			// set button listeners
			ImageButton cameraButton = (ImageButton) view.findViewById(R.id.imageButton1Camera);
			ImageButton galleryButton = (ImageButton) view.findViewById(R.id.imageButton2Gallery);
			ImageButton getLibraryButton = (ImageButton) view.findViewById(R.id.imageButton3Get);
			cameraButton.setOnClickListener(jOnClickListener);
			galleryButton.setOnClickListener(jOnClickListener);
			getLibraryButton.setOnClickListener(jOnClickListener);
			
			return view;
		}
		
		private OnClickListener jOnClickListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent gIntent = new Intent(Intent.ACTION_PICK,
						android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(gIntent, ACTION_PICK_PHOTO_GALLERY);
			}
		};
    }
    
    /**
     * The Templates fragment in second section of the app
     */
    public static class TemplatesFragment extends SherlockFragment {
    	public TemplatesFragment() {
    	}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			Log.d(TAG_LOG, "**Starting TemplatesFragment's onCreateView()");
			templatesView = inflater.inflate(R.layout.activity_fragment_templates, container, false);
			
			templatesRadioGroup = (RadioGroup) templatesView.findViewById(R.id.radioGroup1);
			templatesRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					templatesWebview.loadDataWithBaseURL("constantcontact.com", jReadEmailContentFromFile(R.raw.invtempl1),
        					"text/html", "UTF-8", null);
				}
			});
			
			templatesWebview = (WebView) templatesView.findViewById(R.id.webView1);
			templatesWebview.loadDataWithBaseURL("constantcontact.com", jReadEmailContentFromFile(R.raw.invtempl1),
					"text/html", "UTF-8", null);

			Log.d(TAG_LOG, "**Ending TemplatesFragment's onCreateView()");
			return templatesView;
		}
    }
    
    /**
     * The Audience fragment in third section of the app
     */
    public static class AudienceFragment extends SherlockFragment {
    	public AudienceFragment() {
    	}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			Log.d(TAG_LOG, "**Starting AudienceFragment's onCreateView()");
			audienceView = inflater.inflate(R.layout.activity_fragment_audience, container, false);
			audienceView = createRadioGroupInAudienceFragment(audienceView);
			
			final View view = audienceView;
			
			// set listener for Send button
			ImageButton sendButton = (ImageButton) view.findViewById(R.id.imageButton1Send);
			sendButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					CheckBox cbEmail = (CheckBox) view.findViewById(R.id.checkBox1SendEmail);
					if (cbEmail.isChecked()) doSendEmailCampaign();
					
					CheckBox cbFacebook = (CheckBox) view.findViewById(R.id.checkBox2SendFacebook);
					if (cbFacebook.isChecked()) doSendFacebook();
					
					CheckBox cbTwitter = (CheckBox) view.findViewById(R.id.checkBox3SendTwitter);
					if (cbTwitter.isChecked()) doSendTwitter();
				}
			});
			
			// set listener for Email checkbox
			CheckBox emailCheckBox = (CheckBox) view.findViewById(R.id.checkBox1SendEmail);
			final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup1Send);
			emailCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (!isChecked) {
						radioGroup.setVisibility(View.GONE);
					} else {
						radioGroup.setVisibility(View.VISIBLE);
					}
				}
			});
			
			Log.d(TAG_LOG, "**Ending AudienceFragment's onCreateView()");
			return audienceView;
		}
		
		private void doSendEmailCampaign() {
			Thread scheduleCampaignThread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						// create a new email campaign
						long campaignId = Long.parseLong(createNewDraftCampaign());
						
						// get time now
						Calendar cTime = Calendar.getInstance();
						cTime.add(Calendar.MINUTE, 15); // arbitrarily add 15 minute delay
						// get soonest time to schedule email campaign as a Date
						Date soonestAllowed = cTime.getTime();
						
						int checkedRadioButtonId = MainActivity.templatesRadioGroup.getCheckedRadioButtonId();
						RadioButton checkedRadioButton = (RadioButton) MainActivity.templatesRadioGroup.findViewById(checkedRadioButtonId);
//						long campaignId = Long.parseLong((String)checkedRadioButton.getTag()); // "1100392652031"  or  "1100392008919"
						
						Result<Schedule> result = acApi.scheduleCampaign(campaignId, soonestAllowed, Locale.US);
						final StringBuffer sb = new StringBuffer();
						if (result.isResponseOk()) {
							sb.append("Scheduled ");
							sb.append(result.getResult().scheduled_date.toString());
						} else {
							sb.append("Not scheduled ");
							sb.append(result.getResponseCode() + " " + result.getResponseMessage());
						}
						// do a Toast to show scheduled date, must do in main UI thread 
						handler.post(new Runnable() {
							
							@Override
							public void run() {
								jShowToast(sb.toString());
							}
						});
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ConstantContactApiException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			scheduleCampaignThread.start();
			
			jShowToast("Sending Campaign");
		}
		
		private void doSendFacebook() {
			//TODO
			jShowToast("Sending Facebook");
		}
		
		private void doSendTwitter() {
			//TODO
			jShowToast("Sending Twitter");
		}
		
		private void jShowToast(String str) {
			Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
		}
    }
    
	private static View createRadioGroupInAudienceFragment(View view) {
		if (view == null) return view; // audiencefragment view not yet created
		View rgView = view.findViewById(R.id.radioGroup1Send);
		if (rgView == null) return view; // radiogroup is GONE
    	RadioGroup radioGroup = (RadioGroup) rgView;
		RadioButton rb;
		EmailList elist;
		// limit to show maximum of 3 radio buttons
		for (int i=0; i<3; i++) {
			rb = (RadioButton) radioGroup.getChildAt(i);
			if (emailLists != null  &&  i < MainActivity.emailLists.size()) {
				elist = MainActivity.emailLists.get(i);
				if (elist != null) { //TODO can't test for status ACTIVE; second list in test account has status HIDDEN for some reason
					rb.setText(elist.name);
					rb.setTag(elist.id); // store listId for use when creating a campaign
					rb.setVisibility(View.VISIBLE);
					if (radioGroup.getCheckedRadioButtonId()==-1 && i==0) {
						// no email lists previously but now there is at least one so checkmark first radiobutton
						radioGroup.check(rb.getId());
						CheckBox cb = (CheckBox) view.findViewById(R.id.checkBox1SendEmail);
						cb.setChecked(true);
					}
				} else {
					rb.setVisibility(View.GONE);
				}
			} else {
				if (i != 0) {
					rb.setVisibility(View.GONE);
				} else {
					// leave first radio button there
					radioGroup.clearCheck();
					rb.setText("You have no email lists!");
					CheckBox cb = (CheckBox) view.findViewById(R.id.checkBox1SendEmail);
					cb.setChecked(false);
				}
			}
		}
		return view;
    }
	
	private static String createNewDraftCampaign() throws ConstantContactApiException {
		try {
			Campaign campaign = jSetupNewCampaign();
			Result<Campaign> result = acApi.createCampaign(campaign);
			
			return result.getResult().id;
			
		} catch (ConstantContactApiException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		}
	}
	
	private static Campaign jSetupNewCampaign() {
		Campaign c = new Campaign();
		c.name = "EasyCampaign"+(new Date().getTime());
		c.subject= titleSubject.getText().toString();
		c.from_name = "ctct";
		c.from_email = "ckim@constantcontact.com";
		c.reply_to_email = "ckim@constantcontact.com";
//		c.email_content = jReadEmailContentFromFile(R.raw.invtempl1);
		c.email_content = "<html lang=\"en\" xml:lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:cctd=\"http://www.constantcontact.com/cctd\">\n\n\n<body>Testing...</body></html>";
		c.text_content = "<Text><Greeting/>" + content.getText().toString() + "</Text>";
		c.email_content_format = "XHTML";
		//set sent_to_contact_list
		SentToContactList stContactList = new SentToContactList();
		stContactList.id = 1738066246; //TODO
		List<SentToContactList> list = new ArrayList<SentToContactList>();
		list.add(stContactList);
		c.sent_to_contact_lists = list;
		//set message footer
		MessageFooter mf = new MessageFooter();
		mf.city = "Waltham";
		mf.state = "MA";
		mf.country = "US";
		mf.organization_name = "ctct";
		mf.address_line_1 = "1601 Trapelo Road";
		mf.address_line_2 = "";
		mf.address_line_3 = "";
		mf.international_state = "";
		mf.postal_code = "02451";
		mf.include_forward_email = false;
		mf.forward_email_link_text = "Forward email";
		mf.include_subscribe_link = false;
		mf.subscribe_link_text = "Subscribe me!";
		c.message_footer = mf;
		
		return c;
	}
	
	private static String jReadEmailContentFromFile(int R_raw_file) {
        // set email content
        // read static template file
        InputStream inputStream = mainActivity.getResources().openRawResource(R_raw_file);
        // get content into a string
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
			while ((line = r.readLine()) != null) {
			    sb.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        emailContent = sb.toString();
        emailContent = emailContent.replace("title~here", titleSubject.getText().toString());
        emailContent = emailContent.replace("content~here", content.getText().toString());
        
        int rgbStartIndex = emailContent.indexOf("color: rgb(");
        int rgbEndIndex = emailContent.indexOf("); font-family:", rgbStartIndex);
        int colorButtonIdChosen = 0;
        if (templatesRadioGroup!=null) colorButtonIdChosen = templatesRadioGroup.getCheckedRadioButtonId();
        String colorRGB = "color: rgb(0, 0, 0)";
        switch (colorButtonIdChosen) {
        	case R.id.radio0Red: {
        		colorRGB = "color: rgb(255, 0, 0)";
        		break;
        	}
        	case R.id.radio1Green: {
        		colorRGB = "color: rgb(0, 255, 0)";
        		break;
        	}
        	case R.id.radio2Blue: {
        		colorRGB = "color: rgb(0, 0, 255)";
        		break;
        	}
        }
		emailContent = emailContent.substring(0, rgbStartIndex) + colorRGB + emailContent.substring(rgbEndIndex+1);
		
		int srcStartIndex = emailContent.indexOf("src='https");
		int srcEndIndex = emailContent.indexOf(".jpg' /></td>");
		
		String dummyImgSrc = "src='" + "https://imgssl.l1.constantcontact.com/ui/stock1/skyscrapers_clouds.jpg"; //TODO
		emailContent = emailContent.substring(0, srcStartIndex) + dummyImgSrc + emailContent.substring(srcEndIndex+4);

        return emailContent;
	}
	
	/** Some lifecycle callbacks so that the image can survive orientation change */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}
    
}
