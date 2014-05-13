package dk.a04.android.httplib.rest;

import org.json.JSONObject;

public class RestResponse {
	
	public static final int STATUS_HTTP_LAYER_ERROR = 1;
	public static final int STATUS_HTTP_LAYER_OK    = 2;
	
	
	public int httpLayerStatus = 0;
	public int httpCode = 0;
	public String contentType = "";
	public String content = "";
	public JSONObject json = null;

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("RestResponse { ");
		sb.append("httpLayerStatus: " + httpLayerStatus + ", " );
		sb.append("httpCode: " + httpCode + ", ");
		sb.append("contentType: " + contentType + ", ");
		sb.append("content: " + content + ", ");
		
		return sb.toString();
	}
	
	public boolean hasHttpLayerError() {
		return httpLayerStatus != STATUS_HTTP_LAYER_OK;
	}
	
	public boolean hasJSON() {
		return json != null;
	}
}
