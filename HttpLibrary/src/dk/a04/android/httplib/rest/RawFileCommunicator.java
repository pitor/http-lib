package dk.a04.android.httplib.rest;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.client.BasicCookieStore;
import org.json.JSONException;
import org.json.JSONObject;

import dk.a04.android.httplib.Config;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class RawFileCommunicator {
public static final String LOGTAG = "HTTPLIB.JSONCOMMUNICATOR";
	
	private RestHttpHelper mHttpHelper = null;
	private String mCharset = "utf-8";
	
	private Map<String, String> headers = null;
	
	public static final int ERR_JSON_PARSER = 1000, ERR_HTTP_LAYER = 2000, ERR_NO_NETWORK = 3000;
	
	private enum METHOD { GET, POST, PUT, DELETE };
	
	public RawFileCommunicator( Context context, String charset) {
		if(charset != null)
			mCharset = charset;
		
		mHttpHelper = new RestHttpHelper( context );
		
	}
	
	public RawFileCommunicator( Context context, String charset, String ntlmUserName, String ntlmPassword, String ntlmDeviceIP, String ntlmDomain) {
		if(charset != null)
			mCharset = charset;
		
		mHttpHelper = RestHttpHelper.helperWithNTLM(context, ntlmUserName, ntlmPassword, ntlmDeviceIP, ntlmDomain);
	}
		
	
	public void useHeader(String key, String value) {
		if(headers == null)
			headers = new HashMap<String, String>(5);
		if(headers.containsKey(key))
			headers.remove(key);
		headers.put(key, value);
	}
	
	public RestResponse __METHOD( METHOD method, String url, Map<String, String> params, File file, String contentType) {
		String postData    = null;

		
		if(params != null) {
			try {
				postData = urlEncode(params);
				url = url + "?" + postData;
				debug("postData:" + postData);
			} catch (UnsupportedEncodingException e) {
				debug("Got exception from urlEncode", e);
				return null;
			}
		}
		
		Bundle b = null;
		switch(method) {
		case POST:
			debug( "POST file to url: " + url );
			b = mHttpHelper.post( url, contentType, file);
			break;
		default:
			throw new InvalidParameterException("Invalid method parameter");
		}

		return handleBundle(b);
	}
	
	
	public RestResponse POST( String url, Map<String, String> params, File file, String contentType) {
		return __METHOD( METHOD.POST, url, params, file, contentType );
	}
	
	public RestResponse PUT( String url, Map<String, String> params, File file, String contentType) {
		return __METHOD( METHOD.PUT, url, params, file, contentType );
	}
	
	public void clearCookies() {
		mHttpHelper.clearCookies();
	}
	
	
	public BasicCookieStore getCookieStore() {
		return mHttpHelper.getCookieStore();
	}
	
	public void setCookieStore(BasicCookieStore cookieStore) {
		mHttpHelper.setCookieStore(cookieStore);
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
		for (Map.Entry<String, String> e : params.entrySet()) {
			    if(!is_first)
			    	sb.append('&');
				sb.append(URLEncoder.encode( e.getKey(), mCharset) );
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
