package dk.a04.android.httplib;

import java.io.Serializable;
import java.util.Date;

public abstract class FetchedObject extends Object implements Serializable {
	public static final int STATUS_OK = 1001;
	public static final int STATUS_ERROR = 1002;
	public static final int STATUS_UNKNOWN = 1003;
	
	public int status = STATUS_UNKNOWN;
	public String contentType = "text/html";
	public String encoding = "UTF-16";
	

	public long epoch = System.currentTimeMillis() / 1000;
	
	public boolean isOK() {
		return status == STATUS_OK ? true : false;
	}
	
	public void setEpoch() {
		this.epoch = System.currentTimeMillis() / 1000;
	}
	
	public void setEpochZero() {
		this.epoch = 0;
	}
	
	public long getEpoch() {
		return epoch;
	}
	
	public long getAge() {
		return System.currentTimeMillis() / 1000 - epoch;
	}
}
