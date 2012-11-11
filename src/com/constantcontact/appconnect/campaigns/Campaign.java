package com.constantcontact.appconnect.campaigns;

import java.util.Date;
import java.util.List;

public class Campaign {
	public String id;
    public String name;
    public String subject;
    public CampaignStatus status;
    public String from_name;
    public String from_email;
    public String reply_to_email;
    public String campaign_type;
    public Date created_date;
    public String last_edit_date;
    public String last_run_date;
    public String share_page_url;
    public boolean is_permission_reminder_enabled;
    public String permission_reminder_text;
    public boolean is_view_as_webpage_enabled;
    public String view_as_web_page_text;
    public String view_as_web_page_link_text;
    public String greeting_salutations;
    public String greeting_name;
    public String greeting_String;
    public MessageFooter message_footer;
    public TrackingSummary tracking_summary;
    public String archive_status;
    public String archive_url;
    public boolean is_visible_in_ui;
    public List<SentToContactList> sent_to_contact_lists;
    public List<ClickThroughDetail> click_through_details;
}
