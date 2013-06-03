package dk.a04.android.httplib;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * 
 * @author Piotr Czarny
 * 
 * <p>This class is a wrapper for HttpHelper for accessing Asp.NET based (and perhaps other) WebServices
 * The ASP.NET web service must be JSON-enabled for this wrapper to work</p>
 * 
 * <b>There is no SOAP support in here and probably never will be</b>
 *
 */
public class WebServiceCommunicator {

	public static final String LOGTAG = "HTTPLIB.WEBSERVICECOMMUNICATOR";
	
	private Handler mParentHandler = null;
	private Handler mCommHandler   = null;
	private String mServiceUrl     = null;
	private HttpHelper mHttpHelper = null;
	private String mCharset = "utf-8";
	
	public static final int STATUS_OK    = 10;
	public static final int STATUS_ERROR = 20;
	
	public static final int ERR_HTTP_LAYER  = 1;
	public static final int ERR_JSON_PARSER = 2;
	
	public WebServiceCommunicator(Handler parentHandler, String serviceUrl, Context context, String charset) {
		mParentHandler = parentHandler;
		mServiceUrl    = serviceUrl;
		if(charset != null)
			mCharset = charset;
		
		mCommHandler = new Handler() {

			public void handleMessage(Message mo) {
                final Message m = new Message();

                int status    = mo.what;
                int requestId = mo.arg2;
                
                m.what = requestId;
                debug( "requestId=" + requestId + " status=" + status);
                if(status == HttpHelper.MSG_RESPONSE_OK) {
                	String jsonString = (String)mo.obj;
                	if(jsonString == null)
                		jsonString = "";
                	debug("Received JSON String " + jsonString);
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
                	m.arg2 = ERR_HTTP_LAYER;
                	m.obj  = null;
                }
                mParentHandler.sendMessage(m);
			}
		};
		
		mHttpHelper = new HttpHelper(mCommHandler, context);
		
	}
	
	public int callMethod(String methodName, JSONObject jo) {
		String url      = mServiceUrl + "/" + methodName;
		String postData = jo.toString(); 
		return mHttpHelper.post(url, "application/json", postData, mCharset);
	}
	
	public int callMethod(String methodName, JSONObject jo, HashMap<String, String> headers) {
		String url      = mServiceUrl + "/" + methodName;
		String postData = jo.toString(); 
		return mHttpHelper.post(url, "application/json", postData, headers);
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
