package com.constantcontact.appconnect;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;

import roboguice.util.Ln;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.constantcontact.appconnect.campaigns.Campaign;
import com.constantcontact.appconnect.campaigns.CampaignStatus;
import com.constantcontact.appconnect.campaigns.Schedule;
import com.constantcontact.appconnect.contacts.Contact;
import com.constantcontact.appconnect.contacts.EmailList;
import com.constantcontact.appconnect.loyalty.Consumer;
import com.constantcontact.appconnect.loyalty.Member;
import com.constantcontact.appconnect.loyalty.Points;
import com.constantcontact.appconnect.loyalty.PointsAdded;
import com.constantcontact.appconnect.loyalty.Program;
import com.constantcontact.appconnect.loyalty.Reward;
import com.constantcontact.appconnect.savelocal.Coupon;
import com.constantcontact.oauth.Account;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class AppConnectApi {
	private static final String GET = "GET";
	private static final String POST = "POST";
	private static final String PUT = "PUT";

	private static final int REQUEST_TIMEOUT_MS = 5000;
	private static final int RESPONSE_TIMEOUT_MS = 10000;

	private static String AUTHORIZE_URL = "https://oauth2.%sconstantcontact.com/oauth2/oauth/siteowner/authorize";
	private static String TOKEN_INFO_URL = "https://oauth2.%sconstantcontact.com/oauth2/tokeninfo.htm";
	private static String BASE_API_URL = "https://api.%sconstantcontact.com/v2";

	public static String getTokenAuthenticationUrl(String environment, String clientId, String redirectUri) {
		if (!TextUtils.isEmpty(environment)) {
			environment += '.';
		}
		Uri.Builder b = Uri.parse(String.format(AUTHORIZE_URL, environment)).buildUpon();
		b.appendQueryParameter("response_type", "token");
		b.appendQueryParameter("client_id", clientId);
		b.appendQueryParameter("redirect_uri", redirectUri);
		return b.toString();
	}

	public static String getCodeAuthenticationUrl(String environment, String clientId, String redirectUri) {
		if (!TextUtils.isEmpty(environment)) {
			environment += '.';
		}
		Uri.Builder b = Uri.parse(String.format(AUTHORIZE_URL, environment)).buildUpon();
		b.appendQueryParameter("response_type", "code");
		b.appendQueryParameter("client_id", clientId);
		b.appendQueryParameter("redirect_uri", redirectUri);
		return b.toString();
	}

	/**
	 * Fetches the username associated with the oAuth token. Must be called on a background thread.
	 * 
	 * @return The username
	 * @throws ConstantContactApiException
	 *             Thrown when an error occurs.
	 */
	public static String fetchUserNameForToken(String environment, String token) throws ConstantContactApiException {
		try {
			if (!TextUtils.isEmpty(environment)) {
				environment += '.';
			}
			URL url = new URL(String.format(TOKEN_INFO_URL, environment));
			HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
			c.addRequestProperty("Accept", "application/json");
			c.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			// Send oAuth token as a post
			c.setDoOutput(true);
			OutputStream outputStream = c.getOutputStream();
			final StringReader outputReader = new StringReader("access_token=" + Uri.encode(token));
			IOUtils.copy(outputReader, outputStream);
			IOUtils.closeQuietly(outputStream);
			IOUtils.closeQuietly(outputReader);

			try {
				InputStream inputStream = c.getInputStream();
				JsonFactory f = new JsonFactory();
				final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
				JsonParser parser = f.createJsonParser(bufferedInputStream);
				try {
					String error = null;
					String errorDescription = null;
					parser.nextToken();
					while (parser.nextToken() != JsonToken.END_OBJECT) {
						String name = parser.getCurrentName();
						parser.nextToken();
						if ("user_name".equals(name)) {
							return parser.getText();
						} else if ("error".equals(name)) {
							error = parser.getText();
						} else if ("error_description".equals(name)) {
							errorDescription = parser.getText();
						} else {
						}
					}

					throw new ConstantContactApiException(error + ":" + errorDescription);
				} finally {
					IOUtils.closeQuietly(bufferedInputStream);
					IOUtils.closeQuietly(inputStream);
				}
			} catch (IOException e) {
				Ln.d("Response code: " + c.getResponseCode());
				throw new ConstantContactApiException(e);
			}
		} catch (MalformedURLException e) {
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			throw new ConstantContactApiException(e);
		}
	}

	private Account _account;
	private final String _environment;
	private Uri _baseApiUri;
	private ObjectMapper _om;

	public AppConnectApi(String environment) {
		_environment = TextUtils.isEmpty(environment) ? "" : environment + ".";
		_baseApiUri = Uri.parse(String.format(BASE_API_URL, _environment));

		_om = new ObjectMapper();
		_om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		_om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	}

	public void setAccount(Account account) {
		_account = account;
	}

	public Account getAccount() {
		return _account;
	}

	/******************** Loyalty API Calls *******************/

	public Result<Program[]> getPrograms() throws ConstantContactApiException {
		try {
			Response response = doApiRequest(GET, "loyalty/programs");
			Program[] result = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				result = _om.readValue(inputStream, Program[].class);
			}

			return new Result<Program[]>(response, result);
		} catch (JsonMappingException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		}
	}

	public Result<Program> getProgramDetails(String programId) throws ConstantContactApiException {
		try {
			Response response = doApiRequest(GET, "loyalty/programs/" + programId);
			Program result = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				result = _om.readValue(inputStream, Program.class);
			}

			return new Result<Program>(response, result);
		} catch (JsonMappingException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		}
	}

	public Result<Member> getMemberDetails(String programId, String memberId) throws ConstantContactApiException {
		try {
			Response response = doApiRequest(GET, "loyalty/programs/" + programId + "/members/" + memberId);
			Member result = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				result = _om.readValue(inputStream, Member.class);
			}

			return new Result<Member>(response, result);
		} catch (JsonMappingException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		}
	}

	public Result<PointsAdded> addPoints(String programId, String memberId, Points points) throws ConstantContactApiException {
		try {
			String requestJson = _om.writeValueAsString(points);
			Response response = doApiRequestWithJson(PUT, "loyalty/programs/" + programId + "/members/" + memberId + "/points",
					requestJson);
			PointsAdded result = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				result = _om.readValue(inputStream, PointsAdded.class);
			} else if (response.responseCode == HttpStatus.SC_BAD_REQUEST) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				result = _om.readValue(inputStream, PointsAdded.class);
			}

			return new Result<PointsAdded>(response, result);
		} catch (JsonMappingException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		}
	}

	public Result<Reward> redeemReward(String programId, String memberId, String couponCode) throws ConstantContactApiException {
		try {
			String requestJson = "{\"status\":\"redeemed\"}";
			Response response = doApiRequestWithJson(PUT, "loyalty/programs/" + programId + "/members/" + memberId + "/rewards/"
					+ couponCode, requestJson);
			Reward result = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				result = _om.readValue(inputStream, Reward.class);
//			} else if (response.responseCode == HttpStatus.SC_BAD_REQUEST) {
//				InputStream inputStream = new BufferedInputStream(response.inputStream);
//				return new Result<Reward>(response, _om.readValue(inputStream, ApiError.class));
			}

			return new Result<Reward>(response, result);
		} catch (JsonMappingException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		}
	}

	public Result<Consumer> createConsumer(String email) throws ConstantContactApiException {
		try {
			String requestJson = "{\"email_address\":\"" + email + "\"}";
			Response response = doApiRequestWithJson(POST, "loyalty/consumers/", requestJson);
			Consumer result = null;
			if (response.responseCode == HttpStatus.SC_OK || response.responseCode == HttpStatus.SC_CREATED) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				result = _om.readValue(inputStream, Consumer.class);
			}

			return new Result<Consumer>(response, result);
		} catch (JsonMappingException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		}
	}

	public Result<Member> addMember(String programId, String consumerId, String firstName, String lastName)
			throws ConstantContactApiException {
		try {
			final LinkedHashMap<String, String> request = new LinkedHashMap<String, String>();
			request.put("consumer_id", consumerId);
			request.put("first_name", firstName);
			request.put("last_name", lastName);

			String requestJson = _om.writeValueAsString(request);
			Response response = doApiRequestWithJson(POST, "loyalty/programs/" + programId + "/members", requestJson);
			Member result = null;
			if (response.responseCode == HttpStatus.SC_OK || response.responseCode == HttpStatus.SC_CREATED) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				result = _om.readValue(inputStream, Member.class);
			}

			return new Result<Member>(response, result);
		} catch (JsonMappingException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		}
	}

	/****************** SaveLocal API Calls ********************/
	public Result<Coupon> getCoupon(String couponId) throws ConstantContactApiException {
		try {
			Response response = doApiRequest(GET, "savelocal/coupons/codes/" + couponId);
			Coupon result = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				result = _om.readValue(inputStream, Coupon.class);
			}

			return new Result<Coupon>(response, result);
		} catch (JsonMappingException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		}
	}

	public Result<Coupon> redeemCoupon(String couponId, float revenue) throws ConstantContactApiException {
		try {
			String requestJson = "{\"redeemed\":true,\"revenue\":" + revenue + "}";
			Response response = doApiRequestWithJson(PUT, "savelocal/coupons/codes/" + couponId, requestJson);
			Coupon result = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				result = _om.readValue(inputStream, Coupon.class);
			}

			return new Result<Coupon>(response, result);
		} catch (JsonMappingException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		}
	}

	/******************** Loyalty API Calls *******************/

	/**
	 * Gets an array of contacts. If email supplied, searches based on email address.
	 * 
	 * @param email
	 *            Search for contacts by email address. Optional.
	 * @param limit
	 *            Limit the number of contacts to return, maximum 500. Optional (null to use default)
	 * @param offset
	 *            Offset to first result from result set. Optional (null to use default)
	 * 
	 * @return The result of the api call
	 * 
	 * @throws ConstantContactApiException
	 *             Thrown when an unrecoverable error occurs
	 */
	public Result<Contact[]> getContacts() throws ConstantContactApiException {
		return getContacts(null, null, null);
	}

	public Result<Contact[]> getContacts(String email, Integer limit, Integer offset) throws ConstantContactApiException {
		try {
			Response response = doApiRequest(GET, "contacts", "email", email, "limit", limit, "offset", offset);
			Contact[] contacts = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				contacts = _om.readValue(inputStream, Contact[].class);
			} else if (response.responseCode == HttpStatus.SC_BAD_REQUEST) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				return new Result<Contact[]>(response, _om.readValue(inputStream, ApiError.class));
			}

			return new Result<Contact[]>(response, contacts);
		} catch (JsonMappingException e) {
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			throw new ConstantContactApiException(e);
		}
	}

	/**
	 * Gets a contact
	 * 
	 * @param contactId
	 *            the id of the contact to retrieve
	 *            
	 * @return The result of the api call
	 * 
	 * @throws ConstantContactApiException
	 *             Thrown when an unrecoverable error occurs
	 */
	public Result<Contact> getContact(int contactId) throws ConstantContactApiException {
		try {
			Response response = doApiRequest(GET, "contacts/" + contactId);
			Contact contact = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				contact = _om.readValue(inputStream, Contact.class);
			} else if (response.responseCode == HttpStatus.SC_BAD_REQUEST) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				return new Result<Contact>(response, _om.readValue(inputStream, ApiError.class));
			}

			return new Result<Contact>(response, contact);
		} catch (JsonMappingException e) {
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			throw new ConstantContactApiException(e);
		}
	}

	public Result<EmailList[]> getLists() throws ConstantContactApiException {
		try {
			Response response = doApiRequest(GET, "lists");
			EmailList[] lists = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				lists = _om.readValue(inputStream, EmailList[].class);
			} else if (response.responseCode == HttpStatus.SC_BAD_REQUEST) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				return new Result<EmailList[]>(response, _om.readValue(inputStream, ApiError.class));
			}

			return new Result<EmailList[]>(response, lists);
		} catch (JsonMappingException e) {
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			throw new ConstantContactApiException(e);
		}
	}

	public Result<EmailList> getList(int listId) throws ConstantContactApiException {
		try {
			Response response = doApiRequest(GET, "lists/" + listId);
			EmailList list = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				list = _om.readValue(inputStream, EmailList.class);
			} else if (response.responseCode == HttpStatus.SC_BAD_REQUEST) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				return new Result<EmailList>(response, _om.readValue(inputStream, ApiError.class));
			}

			return new Result<EmailList>(response, list);
		} catch (JsonMappingException e) {
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			throw new ConstantContactApiException(e);
		}
	}

	public Result<Contact[]> getListContacts(int listId) throws ConstantContactApiException {
		return getListContacts(listId, null, null);
	}

	public Result<Contact[]> getListContacts(int listId, Integer limit, Integer offset) throws ConstantContactApiException {
		try {
			Response response = doApiRequest(GET, "lists/" + listId + "/contacts", "limit", limit, "offset", offset);
			Contact[] contacts = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				contacts = _om.readValue(inputStream, Contact[].class);
			} else if (response.responseCode == HttpStatus.SC_BAD_REQUEST) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				return new Result<Contact[]>(response, _om.readValue(inputStream, ApiError.class));
			}

			return new Result<Contact[]>(response, contacts);
		} catch (JsonMappingException e) {
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			throw new ConstantContactApiException(e);
		}
	}

	public Result<Campaign[]> getCampaigns(CampaignStatus status) throws ConstantContactApiException {
		return getCampaigns(status, null, null);
	}

	public Result<Campaign[]> getCampaigns(CampaignStatus status, Integer limit, Integer offset) throws ConstantContactApiException {
		try {
			Response response = doApiRequest(GET, "campaigns", "status", status, "limit", limit, "offset", offset);
			Campaign[] campaigns = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				campaigns = _om.readValue(inputStream, Campaign[].class);
			} else if (response.responseCode == HttpStatus.SC_BAD_REQUEST) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				return new Result<Campaign[]>(response, _om.readValue(inputStream, ApiError.class));
			}

			return new Result<Campaign[]>(response, campaigns);
		} catch (JsonMappingException e) {
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			throw new ConstantContactApiException(e);
		}
	}

	public Result<Campaign> getCampaign(int campaignId) throws ConstantContactApiException {
		try {
			Response response = doApiRequest(GET, "campaigns/" + campaignId);
			Campaign campaign = null;
			if (response.responseCode == HttpStatus.SC_OK) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				campaign = _om.readValue(inputStream, Campaign.class);
			} else if (response.responseCode == HttpStatus.SC_BAD_REQUEST) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				return new Result<Campaign>(response, _om.readValue(inputStream, ApiError.class));
			}

			return new Result<Campaign>(response, campaign);
		} catch (JsonMappingException e) {
			throw new ConstantContactApiException(e);
		} catch (JsonParseException e) {
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			throw new ConstantContactApiException(e);
		}
	}
	
// clk: new scheduleCampaign() overloaded methods
// clk: also need new Schedule class in campaigns package
	public Result<Schedule> scheduleCampaign(long campaignId, Date scheduledDate, Locale locale)
			throws ConstantContactApiException {
		// format date to utc time string
		String format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
		SimpleDateFormat sdf = new SimpleDateFormat(format, locale);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String scheduledDateFormattedString = sdf.format(scheduledDate);
		
		return scheduleCampaign(campaignId, scheduledDateFormattedString);
	}
	
	public Result<Schedule> scheduleCampaign(long campaignId, String scheduledDateString) throws ConstantContactApiException {
		try {
			final LinkedHashMap<String, String> request = new LinkedHashMap<String, String>();
			request.put("scheduled_date", scheduledDateString);

			String requestJson;
			requestJson = _om.writeValueAsString(request);
			Response response = doApiRequestWithJson(POST, "campaigns/" + campaignId + "/schedules", requestJson);
			Schedule result = null;
			if (response.responseCode == HttpStatus.SC_OK || response.responseCode == HttpStatus.SC_CREATED) {
				InputStream inputStream = new BufferedInputStream(response.inputStream);
				result = _om.readValue(inputStream, Schedule.class);
			}
			
			return new Result<Schedule>(response, result);
		} catch (JsonGenerationException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (JsonMappingException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		} catch (IOException e) {
			Ln.e(e);
			throw new ConstantContactApiException(e);
		}
	}


	private Response doApiRequest(String method, String path, Object... params) {
		final String url = getApiRequestUrl(path, params);
		return sendRequest(url, method, null);
	}

	private Response doApiRequestWithJson(String method, String path, String requestJson, Object... params) {
		final String url = getApiRequestUrl(path, params);
		return sendRequest(url, method, requestJson);
	}

	private Response sendRequest(String url, String method, String requestJson) {
		int responseCode = HttpStatus.SC_OK;

		try {
			Ln.d(method + " " + url);
			Ln.d("Bearer " + _account.getToken());

			URL theUrl = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) theUrl.openConnection();
			connection.setConnectTimeout(REQUEST_TIMEOUT_MS);
			connection.setReadTimeout(RESPONSE_TIMEOUT_MS);
			connection.addRequestProperty("Accept", "application/json");
			connection.addRequestProperty("Authorization", "Bearer " + _account.getToken());
			connection.setRequestMethod(method);

			if ((POST.equals(method) || PUT.equals(method)) && requestJson != null) {
				Ln.d("Request: " + requestJson);
				connection.addRequestProperty("Content-Type", "application/json");
				connection.setDoOutput(true);
				StringReader reader = new StringReader(requestJson);
				OutputStream outputStream = connection.getOutputStream();
				IOUtils.copy(reader, outputStream);
			}
			connection.connect();

			responseCode = connection.getResponseCode();
			if (responseCode == HttpStatus.SC_OK || responseCode == HttpStatus.SC_CREATED) {
				return new Response(connection.getInputStream(), responseCode, connection.getResponseMessage());
			}

			return new Response(null, responseCode, connection.getResponseMessage());

		} catch (MalformedURLException e) {
			Ln.d(e);
			return new Response(null, HttpStatus.SC_BAD_REQUEST, "Malformed URL: " + url);
		} catch (SocketTimeoutException e) {
			Ln.d(e, "Connection timeout");
			return new Response(null, HttpStatus.SC_REQUEST_TIMEOUT, e.getLocalizedMessage());
		} catch (IOException e) {
			Ln.e(e);
			return new Response(null, HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
		}
	}

	private String getApiRequestUrl(String path, Object... params) {
		Uri.Builder b = _baseApiUri.buildUpon();
		b.appendEncodedPath(path);
		if (params.length > 0) {
			for (int index = 0; index < params.length; index += 2) {
				if (params[index] != null && params[index + 1] != null) {
					b.appendQueryParameter(params[index].toString(), params[index + 1].toString());
				}
			}
		}
		return b.toString();
	}

	public static class Result<T> {
		private final int _responseCode;
		private final String _responseMessage;
		private final ApiError _responseError;
		private T _result;

		public Result(Response response, T result) {
			_responseCode = response.responseCode;
			_responseMessage = response.responseMessage;
			_responseError = null;
			_result = result;
		}

		public Result(Result<?> result) {
			_responseCode = result._responseCode;
			_responseMessage = result._responseMessage;
			_responseError = result._responseError;
		}

		public Result(Response response, ApiError error) {
			_responseCode = response.responseCode;
			_responseMessage = response.responseMessage;
			_responseError = error;
		}

		public T getResult() {
			return _result;
		}

		public boolean isResponseOk() {
			return _responseCode < 400;
		}

		public int getResponseCode() {
			return _responseCode;
		}

		public String getResponseMessage() {
			return _responseMessage;
		}

		public ApiError getResponseError() {
			return _responseError;
		}

		@Override
		public String toString() {
			return "Response - code: " + _responseCode + " message: " + _responseMessage + " result: " + _result;
		}
	}

	private static class Response {

		public final InputStream inputStream;
		public final int responseCode;
		public final String responseMessage;

		public Response(InputStream inputStream, int responseCode, String responseMessage) {
			this.inputStream = inputStream;
			this.responseCode = responseCode;
			this.responseMessage = responseMessage;
		}
	}
}
