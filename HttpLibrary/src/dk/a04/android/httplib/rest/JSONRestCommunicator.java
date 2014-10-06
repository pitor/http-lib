package dk.a04.android.httplib.rest;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import dk.a04.android.httplib.Config;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class JSONRestCommunicator {
public static final String LOGTAG = "HTTPLIB.JSONCOMMUNICATOR";
	
	private RestHttpHelper mHttpHelper = null;
	private String mCharset = "utf-8";
	
	private Map<String, String> headers = null;
	
	private boolean sendAsFormData = false;
	private boolean sendAsJson     = false;
	
	public static final int ERR_JSON_PARSER = 1000, ERR_HTTP_LAYER = 2000, ERR_NO_NETWORK = 3000;
	
	
	public JSONRestCommunicator( Context context, String charset) {
		if(charset != null)
			mCharset = charset;
		
		mHttpHelper = new RestHttpHelper( context );
		
	}
	
	public JSONRestCommunicator( Context context, String charset, String ntlmUserName, String ntlmPassword, String ntlmDeviceIP, String ntlmDomain) {
		if(charset != null)
			mCharset = charset;
		
		mHttpHelper = RestHttpHelper.helperWithNTLM(context, ntlmUserName, ntlmPassword, ntlmDeviceIP, ntlmDomain);
	}
	
	public void sendAsFormData() {
		sendAsJson     = false;
		sendAsFormData = true;
	}
	
	public void sendAsJSON() {
		sendAsJson     = true;
		sendAsFormData = false;
				
	}
		
	
	public void useHeader(String key, String value) {
		if(headers == null)
			headers = new HashMap<String, String>(5);
		if(headers.containsKey(key))
			headers.remove(key);
		headers.put(key, value);
	}
	
	
	public RestResponse GET( String url) {
		return GET(url, null);
	}
	
	public RestResponse GET( String url, Map<String, String> params) {
		String paramString = null;
		if(params != null) {
			try {
				paramString = urlEncode(params);
			} catch (UnsupportedEncodingException e) {
				debug("Got exception from urlEncode", e);
				return null;
			}
		}
		StringBuilder sb = new StringBuilder( url );
		if(paramString != null && paramString.length() > 0)
			sb.append('?').append(paramString);
		
		debug( "GET " + sb.toString() );
		Bundle b = mHttpHelper.get( sb.toString() );
		return handleBundle(b);
	}

	
	public RestResponse DELETE( String url, Map<String, String> params) {
		String paramString = null;
		
		if(params != null) {
			try {
				paramString = urlEncode(params);
			} catch (UnsupportedEncodingException e) {
				debug("Got exception from urlEncode", e);
				return null;
			}
		}
		StringBuilder sb = new StringBuilder( url );
		if(paramString != null && paramString.length() > 0)
			sb.append('?').append(paramString);
		
		debug("GET " + sb.toString());
		Bundle b = mHttpHelper.delete(sb.toString());
		return handleBundle(b);
	}
	

	public RestResponse POST( String url, Map<String, String> params) {
		String postData    = null;
		String contentType = null;
		
		if(params != null) {
			try {
				postData = urlEncode(params);
				debug("postData:" + postData);
			} catch (UnsupportedEncodingException e) {
				debug("Got exception from urlEncode", e);
				return null;
			}
		}
		contentType = "application/x-www-form-urlencoded";
		
		debug("POST url: " + url + " data: " + postData );
		Bundle b = mHttpHelper.post( url, contentType, postData, "utf-8");
		return handleBundle(b);
	}
	
	
	public RestResponse POST( String url, JSONObject json) {
		String postData = json != null ? 
				json.toString() : "{}";
		String contentType = "application/json";
		debug("POST url: " + url + " data: " + postData );
		Bundle b = mHttpHelper.post( url, contentType, postData, "utf-8");
		return handleBundle(b);
	}
	
	
	public RestResponse PUT( String url, Map<String, String> params) {
		String postData = "";
		if(params != null) {
			try {
				postData = urlEncode(params);
			} catch (UnsupportedEncodingException e) {
				debug("Got exception from urlEncode", e);
				return null;
			}
		}
	
		String contentType = "application/x-www-form-urlencoded";		
		debug("PUT url: " + url + " data: " + postData );
		Bundle b = mHttpHelper.put( url, contentType, postData, "utf-8");
		return handleBundle(b);
	}
	
	public RestResponse PUT( String url, JSONObject json) {
		String postData = json.toString();
		String contentType = "application/json; charset=utf-8";		
		debug("PUT url: " + url + " data: " + postData );
		Bundle b = mHttpHelper.put( url, contentType, postData, "utf-8");
		return handleBundle(b);
	}
	
	public void clearCookies() {
		mHttpHelper.clearCookies();
	}
	
	/**
	 * url encode dictionary so that it can be used with POST and GET methods
	 * @param params: Map<String, String> of params to be encoded
	 * @return url encoded string
	 * @throws UnsupportedEncodingException 
	 */
	private String urlEncode(Map<String, String> params) throws UnsupportedEncodingException {
		StringBuffer sb = new StringBuffer(200);
		boolean is_first = true;
		for (Map.Entry<String, String> e: params.entrySet()) {
			    if(!is_first)
			    	sb.append('&');
				sb.append(URLEncoder.encode(e.getKey(), mCharset));
				sb.append('=');
				if(e.getValue() != null)
					sb.append(URLEncoder.encode(e.getValue(), mCharset));
				is_first = false;
		}
		return sb.toString();
	}
	
	/**
	 * 
	 * @param httpMessage
	 * @return RestResponse object contain
	 */
	protected RestResponse handleBundle( Bundle bundle ) {
        final RestResponse rr = new RestResponse();

        if( bundle == null) {
        	rr.httpLayerStatus = RestResponse.STATUS_HTTP_LAYER_ERROR;
        	rr.httpCode = 0;
        	rr.json = null;
        	return rr;
        }
        
        int what = bundle.getInt(RestHttpHelper.KEY_HTTP_WHAT_IS_IT);
        
        rr.noNetworkFlag = (what == RestHttpHelper.WHAT_NO_NETWORK_CONNECTION);
        
        if(what == RestHttpHelper.WHAT_NETWORK_TIMEOUT || 
           what == RestHttpHelper.WHAT_HTTP_LAYER_ERROR ||
           what == RestHttpHelper.WHAT_NO_NETWORK_CONNECTION) {
        	rr.httpLayerStatus = RestResponse.STATUS_HTTP_LAYER_ERROR;
        	rr.httpCode = 0;
        	rr.json = null;
        	return rr;
        }
        	       
        rr.httpLayerStatus = RestResponse.STATUS_HTTP_LAYER_OK;
        int httpCode = bundle.getInt(RestHttpHelper.KEY_HTTP_CODE, 0);
        String contentType = bundle.getString(RestHttpHelper.KEY_RESPONSE_CONTENT_TYPE);
        
        String responseString = bundle.getString(RestHttpHelper.KEY_RESPONSE_STRING);
    	if(responseString == null) {
    		rr.httpLayerStatus = RestResponse.STATUS_HTTP_LAYER_ERROR;
        	rr.httpCode = 0;
        	rr.json = null;
        	return rr;
    	}
    		
        try {
        	if(contentType != null && contentType.startsWith("application/json")) {
        		JSONObject jsonObject = new JSONObject(responseString);
        		rr.json  = jsonObject;
        	}
        	rr.httpLayerStatus = RestResponse.STATUS_HTTP_LAYER_OK;
        	rr.httpCode        = httpCode;
        	rr.contentType     = contentType;
        	rr.content         = responseString;
        	return rr;
		} 
        catch (JSONException e) {
    		rr.httpLayerStatus = RestResponse.STATUS_HTTP_LAYER_OK;
        	rr.httpCode = 0;
        	rr.json = null;
        	return rr;
		}
        
	}
	
	/**
	 * Set TCP/IP socket timeout
	 * @param milliseconds
	 * @see HttpHelper.setSocketTimeout(int)
	 */
	public void setSocketTimeout(int milliseconds) {
		mHttpHelper.setSocketTimeout(milliseconds);
	}
	
	/**
	 * Get TCP/IP connection socket timeout
	 * @return the timeout in milliseconds
	 * @see HttpHelper.getSocketTimeout(int)
	 */
	public int getSocketTimeout() {
		return mHttpHelper.getSocketTimeout();
	}
	
	/**
	 * Set connection timeout
	 * @param milliseconds
	 * @see HttpHelper.setConnectionTimeout(int)
	 */
	public void setConnectionTimeout(int milliseconds) {
		mHttpHelper.setConnectionTimeout(milliseconds);
	}
	
	/**
	 * 
	 * @return connection timeout in milliseconds
	 * @see HttpHelper.getConnectionTimeout()
	 */
	public int getConnectionTimeout() {
		return mHttpHelper.getConnectionTimeout();
	}
	
	private static void debug(String message) {
		if(Config.DEBUG)
			Log.i(LOGTAG, message);
	}
	
	private static void debug(String message, Throwable tr) {
		if(Config.DEBUG)
			Log.i(LOGTAG, message, tr);
	}
	
}
