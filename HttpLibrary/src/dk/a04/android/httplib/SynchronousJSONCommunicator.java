package dk.a04.android.httplib;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Message;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class SynchronousJSONCommunicator {
public static final String LOGTAG = "HTTPLIB.JSONCOMMUNICATOR";
	
	private SynchronousHttpHelper mHttpHelper = null;
	private String mCharset = "utf-8";
	
	public static final int STATUS_OK = 1, STATUS_ERROR = 2;
	public static final int ERR_JSON_PARSER = 1000, ERR_HTTP_LAYER = 2000, ERR_NO_NETWORK = 3000;
	
	
	public SynchronousJSONCommunicator( Context context, String charset) {
		if(charset != null)
			mCharset = charset;
		
		mHttpHelper = new SynchronousHttpHelper( context);
	}
	
	
	
	public Message GET( String url, Map<String, String> params) {
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
		Message msg = mHttpHelper.get(sb.toString());
		return handleMessage(msg);
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
	 * @return
	 */
	protected Message handleMessage( Message httpMessage ) {
        final Message m = new Message();

        if(httpMessage == null) {
        	m.what = 0;
        	m.arg1 = STATUS_ERROR;
        	m.arg2 = ERR_HTTP_LAYER;
        	return m;
        }
        
        int status    = httpMessage.what;
        int requestId = httpMessage.arg2;
        
        m.what = requestId;
        debug( "requestId=" + requestId + " status=" + status);
        if(status == HttpHelper.MSG_RESPONSE_OK) {
        	String jsonString = (String)httpMessage.obj;
        	if(jsonString == null)
        		jsonString = "";
        	debug( "Received JSON String " + jsonString);
        	try {
				JSONObject jsonObject = new JSONObject(jsonString);
            	m.arg1 = STATUS_OK;
            	m.arg2 = 0;
            	m.obj  = jsonObject;
			} catch (JSONException e) {
				m.arg1 = STATUS_ERROR;
            	m.arg2 = ERR_JSON_PARSER;
            	m.obj  = null;
			}
        }
        else {
        	m.arg1 = STATUS_ERROR;
        	if(status == HttpHelper.MSG_NO_NETWORK_CONNECTION)
        		m.arg2 = ERR_NO_NETWORK;
        	else 
        		m.arg2 = ERR_HTTP_LAYER;
        	m.obj  = null;
        }
        return m;
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
			Log.d(LOGTAG, message);
	}
	
	private static void debug(String message, Throwable tr) {
		if(Config.DEBUG)
			Log.d(LOGTAG, message, tr);
	}
}
