package dk.a04.android.httplib.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import dk.a04.android.httplib.Config;
import dk.a04.android.httplib.ntlm.NTLMSchemeFactory;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class RestHttpHelper {
	
	private ConnectivityManager mConnectivyManager;
	
	private static final String LOGTAG   = "HTTPLIB.RESTHTTPHELPER";
	
	public static final int WHAT_RESPONSE_OK                 = 1001;
	public static final int WHAT_HTTP_LAYER_ERROR            = 1002;
	public static final int WHAT_NO_NETWORK_CONNECTION       = 1003;
	public static final int WHAT_NETWORK_TIMEOUT             = 1004;
	
	public static final int METHOD_GET    = 10001;
	public static final int METHOD_POST   = 10002;
	public static final int METHOD_PUT    = 10003;
	public static final int METHOD_DELETE = 10004;
	
	public static final int SOCKET_TIMEOUT_DEFAULT     = 20000;
	public static final int CONNECTION_TIMEOUT_DEFAULT = 20000;
	
	public static final String DEFAULT_REQUEST_CHARSET = "utf-8";
	
	
	private static int curRequestId = 1;
	
	private int mSocketTimeout     = SOCKET_TIMEOUT_DEFAULT;
	private int mConnectionTimeout = CONNECTION_TIMEOUT_DEFAULT;
	
	private BasicCookieStore mCookieStore = null;
	private boolean useNTLM = false;
	private String ntlmUserName  = "";
	private String ntlmPassword  = "";
	private String ntlmDeviceIP  = "";
	private String ntlmDomain    = "";
	
	public RestHttpHelper(Context context) {
		mConnectivyManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	}
	
	public static RestHttpHelper helperWithNTLM( Context context, String username, String password, String deviceIP, String domain ) {
		RestHttpHelper helper = new RestHttpHelper(context);
		helper.useNTLM = true;
		
		helper.ntlmUserName = username;
		helper.ntlmPassword = password;
		helper.ntlmDeviceIP = deviceIP;
		helper.ntlmDomain   = domain;
		
		return helper;
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
	
	public void clearCookies() {
		if(mCookieStore != null)
			mCookieStore.clear();
	}
	
	
	public Bundle get(final String url, String charset) {
		return doRequest(METHOD_GET, url, charset, null, null, null, null);
	}
	
	public Bundle get(final String url) {
		return doRequest(METHOD_GET, url, null, null, null, null, null);
	}
	
	public Bundle get(final String url, Map<String, String> headers ) {
		return doRequest(METHOD_GET, url, null, null, null, null, headers );
	}
	
	
	
	public Bundle post(final String url, final String postData) {
		return doRequest(METHOD_POST, url, null, postData, null, null, null);
	}
	
	public Bundle post(final String url, final String contentType, final String postData, String charset) {
		return doRequest(METHOD_POST, url, charset, postData, contentType, null, null);
	}
	
	public Bundle post(final String url, final String contentType, final File file) {
		return doRequest(METHOD_POST, url, null, null, contentType, file, null);
	}
	
	public Bundle post(final String url, final String contentType, final String postData, final HashMap<String, String> headers) {
		return doRequest(METHOD_POST, url, null, postData, contentType, null, headers);
	}
	
	public Bundle put(final String url, final String contentType, final String postData, String charset) {
		return doRequest(METHOD_PUT, url, charset, postData, contentType, null, null);
	}
	
	public Bundle put(final String url, final String contentType, final String postData, final HashMap<String, String> headers) {
		return doRequest(METHOD_PUT, url, null, postData, contentType, null, headers);
	}
	
	public Bundle delete(final String url) {
		return doRequest(METHOD_DELETE, url, null, null, null, null, null);
	}
	
	public Bundle delete(final String url, Map<String, String> headers) {
		return doRequest(METHOD_DELETE, url, null, null, null, null, headers);
	}
	
	
	/** internal method that does the actual request
	 * 
	 * @param method  integer value. Use POST or GET static fields.
	 * @param url String containing url
	 * @param charset String containing charset for the request
	 * @param postData
	 * @param contentType
	 * @param file
	 * @param headers Additional Headers
	 * @return unique request id that will be used to identify the response
	 */
	private Bundle doRequest( final int method, 
						   final String url, 
						   final String charset, 
						   final String postData, 
						   final String contentType, 
						   final File file,
						   final Map<String, String> headers) {
			debug( "Calling doRequest. method=" + method );
		
		NetworkInfo netinfo = mConnectivyManager.getActiveNetworkInfo();
    	if(netinfo == null)
    		debug( "network info is null" );
    	else
    		debug( "obtained netinfo");
    	
    	if(netinfo == null || !netinfo.isConnected()) {
    		debug( "No network connection... sending message" );
    		return noNetworkConnection();
    	}

        try {
        	
        	HttpParams httpParameters = new BasicHttpParams();
        	HttpConnectionParams.setConnectionTimeout(httpParameters, mConnectionTimeout);
        	HttpConnectionParams.setSoTimeout(httpParameters, mSocketTimeout);
        	
        	String contentTypeHeader = null;
        	String usedCharset = (charset == null) ?  DEFAULT_REQUEST_CHARSET : charset;
        	
        	if(contentType != null)
        		contentTypeHeader = contentType + "; charset=" + usedCharset;
        				
      	
            DefaultHttpClient client = new DefaultHttpClient( httpParameters );
            
            if(useNTLM)
            	addNTLMToClient( client );
            
            if(mCookieStore == null)
                 mCookieStore = new BasicCookieStore();
            client.setCookieStore(mCookieStore);
            
            
            HttpResponse httpResponse = null;
            if( method == METHOD_GET || method == METHOD_DELETE ) {
            	debug( "Using method GET/DELETE method=" + method);
            	//HttpGet http_method = new HttpGet( url );
            	HttpRequestBase http_method = null;
            	switch( method ) {
            	case METHOD_GET: http_method = new HttpGet( url ); debug("HttpGet"); break;
            	case METHOD_DELETE: http_method = new HttpDelete( url ); debug("HttpDelete"); break;
            	}
            	if(useNTLM)
            		http_method.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, true);
            	if(contentTypeHeader != null)
            		http_method.addHeader("Content-Type", contentTypeHeader);
            	if(headers != null) {
            		Set<String> keys = headers.keySet();
            		for(String key : keys) {
            			http_method.addHeader( key, headers.get(key));
            		}
            	}
            	
            	httpResponse = client.execute( http_method );
            }
            else 
            if ( method == METHOD_POST || method == METHOD_PUT ) {
            	debug( "Using method POST/PUT method=" + method);

            	HttpEntityEnclosingRequest http_method = null;
            	switch(method) {
            	case METHOD_POST: http_method = new HttpPost( url ); debug("HttpPost"); break;
            	case METHOD_PUT: http_method = new HttpPut( url ); debug("HttpPut"); break;
            	}
            	
            	//if(useNTLM)
            	//	http_method.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, true);
            	
            	if(contentTypeHeader != null)
            		http_method.addHeader("Content-Type", contentTypeHeader);
            	
            	if(postData != null)
            		http_method.setEntity(new StringEntity(postData, usedCharset));

            	else if(file != null) {
            		http_method.setEntity(new FileEntity(file, contentType));
            	}
            	
            	//Add headers to request if headers are present
            	if(headers != null) {
            		for(String key : headers.keySet()) {
            			debug("Adding header: " + key + ":" + headers.get(key));
            			http_method.addHeader( key, headers.get(key));
            		}
            	}
            	httpResponse = client.execute( (HttpRequestBase)http_method );

            }
            else
            	debug("Illegal method!");
            
            return handleResponse(charset, httpResponse);
        } 
        catch (ClientProtocolException e) {
        	debug( "HttpRequest Failed with exception", e);
        	return httpLayerError();
        } 
        catch(ConnectTimeoutException e) { 
        	debug( "HttpRequest Failed with connecttimeoutexception" + e, e);
        	return networkTimeOutError();
        } 
        catch(SocketTimeoutException e) { 
        	debug( "HttpRequest Failed with sockettimeoutexception" + e, e);
        	return networkTimeOutError();
        } 
        catch (IOException e) {
        	debug( "HttpRequest Failed with exception " + e, e);
        	return httpLayerError();
        }
        catch(Exception e) {
        	debug( "HttpRequest Failed with exception " + e , e);
        	return httpLayerError();
        }
    }
	
	private void addNTLMToClient( DefaultHttpClient client) {
		
		if(!useNTLM)
			return;
		
		Log.d(LOGTAG, "Adding ntlm to client");
		
		client.getAuthSchemes().register("ntlm", new NTLMSchemeFactory());
		final NTCredentials creds = new NTCredentials( ntlmUserName, ntlmPassword, ntlmDeviceIP, ntlmDomain );
		final AuthScope scope = new AuthScope(null, -1);
		client.getCredentialsProvider().setCredentials( scope , creds );
	}

	private Bundle noNetworkConnection() {
		Bundle b = new Bundle();
		b.putInt(KEY_HTTP_WHAT_IS_IT, WHAT_NO_NETWORK_CONNECTION);
		b.putString(KEY_HTTP_WHAT_DESCRIPTION, "No network connection");
		return b;
	}
	
	private Bundle httpLayerError() {
		Bundle b = new Bundle();
		b.putInt(KEY_HTTP_WHAT_IS_IT, WHAT_HTTP_LAYER_ERROR);
		b.putString(KEY_HTTP_WHAT_DESCRIPTION, "Http layer error");
		return b;
	}
	
	private Bundle networkTimeOutError() {
		Bundle b = new Bundle();
		b.putInt(KEY_HTTP_WHAT_IS_IT, WHAT_NETWORK_TIMEOUT);
		b.putString(KEY_HTTP_WHAT_DESCRIPTION, "Http layer error");
		return b;
	}
	
	public static final String KEY_HTTP_CODE = "HTTP_CODE";
	public static final String KEY_HTTP_WHAT_IS_IT = "WHAT_IS_IT";
	public static final String KEY_HTTP_WHAT_DESCRIPTION = "WHAT_DESCRIPTION";
	public static final String KEY_RESPONSE_STRING ="RESPONSE_STRING";
	public static final String KEY_RESPONSE_CONTENT_TYPE = "CONTENT_TYPE";
	
	private Bundle handleResponse(String charset, HttpResponse response) {
		debug( "handleResponse called");
		int  httpResponseCode = 0;
		
		StatusLine status = response.getStatusLine();
		httpResponseCode = status.getStatusCode();
		HttpEntity entity = response.getEntity();
		String usedCharset = getContentCharSet(entity);
		if(usedCharset == null)
			usedCharset = charset;
		
		Header contentTypeHeader = response.getLastHeader("content-type");
		String contentType = null;
		if(contentTypeHeader != null)
		   contentType = contentTypeHeader.getValue();
		
		debug( "handleResponse charset=" + charset + " usedCharset=" + usedCharset + " content-type: " + contentType);
		
		
		try {
			int statuscode = status.getStatusCode();
			debug( "Http status code=" + statuscode);
			httpResponseCode = statuscode;
			String resultString = inputStreamToString( entity.getContent(), usedCharset );
			debug( "Result string:" + resultString);
			
			Bundle b = new Bundle();
			b.putInt(KEY_HTTP_WHAT_IS_IT, WHAT_RESPONSE_OK);
			b.putInt(KEY_HTTP_CODE, httpResponseCode);
			b.putString(KEY_RESPONSE_STRING, resultString);
			b.putString(KEY_RESPONSE_CONTENT_TYPE, contentType);
			return b;
			
		} catch (IOException e) {
			debug("Got http exception", e);
			return httpLayerError();
		}
		


	}
	

	public static String inputStreamToString(final InputStream stream, String charset) throws IOException {
		if(charset == null)
			charset = "UTF-8";
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

