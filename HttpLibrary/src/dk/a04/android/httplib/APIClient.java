package dk.a04.android.httplib;

import android.content.Context;

public interface APIClient {
	public Context getContext();
	public void deliverObject(int id, int status, FetchedObject o);
	
	public final int STATUS_OK                 = 1111;
	public final int STATUS_HTTP_ERROR         = 2222;
	public final int STATUS_NO_NETWORK         = 3333;
	public final int STATUS_NETWORK_TIMEOUT    = 4444;
	public final int STATUS_XML_ERROR          = 5555;
	public final int STATUS_STATUS_ERROR       = 6666;
	public final int STATUS_UNKNOWN_ERROR      = 99999;

}
