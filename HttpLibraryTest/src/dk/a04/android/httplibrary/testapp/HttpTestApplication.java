package dk.a04.android.httplibrary.testapp;

import android.app.Application;
import android.util.Log;


public class HttpTestApplication extends Application {

	public static final String LOGTAG = "HTTPTEST.HTTPTESTAPP";
	
	@Override
	public void onCreate() {
		Log.d(LOGTAG, "Creating application instance");
	}
	
}
