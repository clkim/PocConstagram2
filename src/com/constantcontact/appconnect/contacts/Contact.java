package com.constantcontact.appconnect.contacts;

import java.util.List;

public class Contact
{
    public int id;
    public ContactStatus status;
    public String fax;
    public List<Address> addresses;
    public List<Note> notes;
    public boolean confirmed;
    public List<EmailList> lists;
    public String source;
    public List<EmailAddress> email_addresses;
    public String prefix_name;
    public String first_name;
    public String middle_name;
    public String last_name;
    public String job_title;
    public String department_name;
    public String company_name;
    public List<CustomField> custom_fields;
    public String source_details;
    public ContactActionBy action_by;
}