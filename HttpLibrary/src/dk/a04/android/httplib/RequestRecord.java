package dk.a04.android.httplib;

import java.io.File;
import java.util.HashMap;

public class RequestRecord {
	public static final int METHOD_GET    = 1;
	public static final int METHOD_POST   = 2;
	public static final int METHOD_PUT    = 3;
	public static final int METHOD_DELETE = 4;
	
	public int requestId                   = -1;
	public int method                      =  0;
	public String url                      = null;
	public String postData                 = null;
	public File file                       = null;
	public HashMap<String, String> headers = null;
	
	public RequestRecord(int requestId, 
			int method, 
			String url, 
			String postData, 
			String charset, 
			File file, 
			HashMap<String, String> headers) {
		this.requestId = requestId;
		this.method = method;
		this.url = url;
		this.postData = postData;
		this.file = file;
		this.headers = headers;
	}
	
	
}
