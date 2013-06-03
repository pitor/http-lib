package dk.a04.android.httplib;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class HttpHelper {
	
	private Handler mCallerHandler = null;
	private ConnectivityManager mConnectivyManager;
	
	private static final String LOGTAG   = "HTTPLIB.HTTPHELPER";
	
	public static final int MSG_RESPONSE_OK           = 1001;
	public static final int MSG_RESPONSE_HTTP_ERROR   = 1002;
	public static final int MSG_NO_NETWORK_CONNECTION = 1003;
	public static final int MSG_NETWORK_TIMEOUT       = 1004;

	
	public static final int SOCKET_TIMEOUT_DEFAULT     = 20000;
	public static final int CONNECTION_TIMEOUT_DEFAULT = 20000;
	
	public static final String DEFAULT_REQUEST_CHARSET="utf-8";
	
	
	private static int curRequestId = 1;
	
	private int mSocketTimeout     = SOCKET_TIMEOUT_DEFAULT;
	private int mConnectionTimeout = CONNECTION_TIMEOUT_DEFAULT;
	
	public static long GET(Handler h, Context context, String url) {
		HttpHelper helper = new HttpHelper(h, context);
		return helper.get(url);
	}
	
	public HttpHelper(Handler callerHandler, Context context) {
		this.mCallerHandler = callerHandler;
		mConnectivyManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	}
	
	/** Set the socket timeout for the http request.
	 * 
	 * Socket timeout is the time it can take to create the raw TCP/IP socket to 
	 * the server
	 * 
	 * @param
	 *     milliseconds the timeout in milliseconds
	 * 
	 */
	public void setSocketTimeout(int milliseconds) {
		mSocketTimeout = milliseconds;
	}
	
	/**
	 * Get the socket timeout
	 * @return socket timeout in milliseconds
	 */
	public int getSocketTimeout() {
		return mSocketTimeout;
	}
	
	/** Set the timeout for the connection
	 * 
	 * Connection timeout is the time it can take to get a http response from
	 * the server after the TCP/IP connection has been established
	 * 
	 * @param
	 *     milliseconds the timeout in milliseconds
	 * 
	 */
	public void setConnectionTimeout(int milliseconds) {
		mConnectionTimeout = milliseconds;
	}
	
	/**
	 * Get the connection timeout
	 * @return the timeout in milliseconds
	 */
	public int getConnectionTimeout() {
		return mConnectionTimeout;
	}
	
	
	public int get(final String url, String charset) {
		return doRequest(RequestRecord.METHOD_GET, url, charset, null, null, null, null);
	}
	
	public int get(final String url) {
		return doRequest(RequestRecord.METHOD_GET, url, null, null, null, null, null);
	}
	
	public int post(final String url, final String postData) {
		return doRequest(RequestRecord.METHOD_POST, url, null, postData, null, null, null);
	}
	
	public int post(final String url, final String contentType, final String postData, String charset) {
		return doRequest(RequestRecord.METHOD_POST, url, charset, postData, contentType, null, null);
	}
	
	public int post(final String url, final String contentType, final File file) {
		return doRequest(RequestRecord.METHOD_POST, url, null, null, contentType, file, null);
	}
	
	public int post(final String url, final String contentType, final String postData, final HashMap<String, String> headers) {
		return doRequest(RequestRecord.METHOD_POST, url, null, postData, contentType, null, headers);
	}
	
	/** internal method that does the actual request
	 * 
	 * @param method  integer value. Use POST or GET static fields.
	 * @param url String containing url
	 * @param charset String containing charset for the request
	 * @param postData
	 * @param contentType
	 * @param file
	 * @param headers
	 * @return unique request id that will be used to identify the response
	 */
	private int doRequest( final int method, 
						   final String url, 
						   final String charset, 
						   final String postData, 
						   final String contentType, 
						   final File file,
						   final HashMap<String, String> headers) {
			debug( "Calling doRequest. method=" + method );
		
		NetworkInfo netinfo = mConnectivyManager.getActiveNetworkInfo();
    	if(netinfo == null)
    		debug( "network info is null" );
    	else
    		debug( "obtained netinfo");
    	
    	final int currentRequestId = obtainRequestId();
    	if(netinfo == null || !netinfo.isConnected()) {
    		debug( "No network connection... sending message" );
    		Message message = mCallerHandler.obtainMessage(MSG_NO_NETWORK_CONNECTION);
    		message.arg2 = currentRequestId;
    		mCallerHandler.sendMessageDelayed(message, 100);
    		return currentRequestId;
    	}
		

    	
	    final ResponseHandler<String> response_handler = new ResponseHandler<String>() {
	    	
	    	public String handleResponse(HttpResponse response) {
	    		debug( "handleResponse called");
    			int what = 0;
    			int http_response_code = 0;  			
	    		StatusLine status = response.getStatusLine();
	    		HttpEntity entity = response.getEntity();
	    		String usedCharset = getContentCharSet(entity);
	    		if(usedCharset == null)
	    			usedCharset = charset;
	    		
	    		debug( "handleResponse charset=" + charset + " usedCharset=" + usedCharset );
	    		
	    		String result = null;
	    		try {
	    			int statuscode = status.getStatusCode();
	    			debug( "Http status code=" + statuscode);
	    			what = statuscode == HttpStatus.SC_OK ? MSG_RESPONSE_OK : MSG_RESPONSE_HTTP_ERROR;
	    			http_response_code = statuscode;
	    			result = inputStreamToString( entity.getContent(), usedCharset );
	    			debug( "Result string:" + result);
	    		} catch (IOException e) {
	    			debug("Got http exception", e);
	    			what = MSG_RESPONSE_HTTP_ERROR;
	    			http_response_code = status.getStatusCode();
	    		}
    			Message message = mCallerHandler.obtainMessage(what, http_response_code, currentRequestId, result);
    			mCallerHandler.sendMessage( message );
	    		return result;
	    	}
	    };

	    // do the HTTP request in a separate thread (the responseHandler will fire when complete)
	    Thread httpthread = new Thread() {

	            @Override
	            public void run() {           		
	                try {
	                	debug("HttpThread run charset=" + charset);
	                	
	                	HttpParams httpParameters = new BasicHttpParams();
	                	HttpConnectionParams.setConnectionTimeout(httpParameters, mConnectionTimeout);
	                	HttpConnectionParams.setSoTimeout(httpParameters, mSocketTimeout);
	                	
	                	String contentTypeHeader = null;
	                	String usedCharset = (charset == null) ?  DEFAULT_REQUEST_CHARSET : charset;
	                	
	                	if(contentType != null)
	                		contentTypeHeader = contentType + "; charset=" + usedCharset;
	                				

	                	
	                    DefaultHttpClient client = new DefaultHttpClient(httpParameters);
	                    if(method == RequestRecord.METHOD_GET) {
	                    	debug( "Using method GET");
	                    	HttpGet http_method = new HttpGet( url );
	                    	if(contentTypeHeader != null)
	                    		http_method.addHeader("Content-Type", contentTypeHeader);
	                    	if(headers != null) {
	                    		Set<String> keys = headers.keySet();
	                    		for(String key : keys) {
	                    			http_method.addHeader( key, headers.get(key));
	                    		}
	                    	}
	                    	client.execute( http_method, response_handler );
	                    }
	                    else if(method == RequestRecord.METHOD_POST) {
	                    	debug( "Using method POST");
	                    	HttpPost http_method = new HttpPost(url);
	                    	if(contentTypeHeader != null)
	                    		http_method.addHeader("Content-Type", contentTypeHeader);
	                    	if(postData != null)
	                    		http_method.setEntity(new StringEntity(postData, usedCharset));

	                    	else if(file != null) {
	                    		http_method.setEntity(new FileEntity(file, contentType));
	                    	}
	                    	if(headers != null) {
	                    		debug( "Adding headers");
	                    		for(String key : headers.keySet()) {
	                    			debug("Adding header: " + key + ":" + headers.get(key));
	                    			http_method.addHeader( key, headers.get(key));
	                    		}
	                    		debug( "Done with headers");
	                    	}
	                    	client.execute( http_method, response_handler );
	                    }
	                    else
	                    	debug("Illegal method!");
	                    return;
	                } catch (ClientProtocolException e) {
	                	debug( "HttpRequest Failed with exception", e);
	                	Message message = mCallerHandler.obtainMessage(MSG_RESPONSE_HTTP_ERROR, 0, currentRequestId);
	                	mCallerHandler.sendMessage( message );
	                } catch(ConnectTimeoutException e) { 
	                	debug( "HttpRequest Failed with connecttimeoutexception" + e, e);
	                	Message message = mCallerHandler.obtainMessage(MSG_NETWORK_TIMEOUT, 0 ,currentRequestId);
	                	mCallerHandler.sendMessage( message );
	                } catch(SocketTimeoutException e) { 
	                	debug( "HttpRequest Failed with sockettimeoutexception" + e, e);
	                	Message message = mCallerHandler.obtainMessage(MSG_NETWORK_TIMEOUT, 0, currentRequestId);
	                	mCallerHandler.sendMessage( message );
	                } catch (IOException e) {
	                	debug( "HttpRequest Failed with exception " + e, e);
	                	Message message = mCallerHandler.obtainMessage(MSG_RESPONSE_HTTP_ERROR, 0, currentRequestId);
	                	mCallerHandler.sendMessage( message );
	                }  catch(Exception e) {
	                	debug( "HttpRequest Failed with exception " + e , e);
	                	Message message = mCallerHandler.obtainMessage(MSG_RESPONSE_HTTP_ERROR, 0, currentRequestId);
	                	mCallerHandler.sendMessage( message );
	                }
	            }
	        };
	        httpthread.start();
	        
	        return currentRequestId;
	}
	

	public static String inputStreamToString(final InputStream stream, String charset) throws IOException {
		if(charset == null)
			charset = "UTF-8";
		InputStreamReader isr = new InputStreamReader(stream);
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, charset), 10000);
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();
        return sb.toString();
    }
	
	private synchronized static int obtainRequestId() {
		return curRequestId++;
	}
	
	public String getContentCharSet(final HttpEntity entity) {

		if (entity == null)
			return null;
		String charset = null;

		if (entity.getContentType() != null) {
			HeaderElement values[] = entity.getContentType().getElements();

			if (values.length > 0) {
				NameValuePair param = values[0].getParameterByName("charset");
				if (param != null)
					charset = param.getValue();
			}
		}

		return charset;

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

