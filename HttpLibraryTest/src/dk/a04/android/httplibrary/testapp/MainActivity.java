package dk.a04.android.httplibrary.testapp;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity {

	public static final String LOGTAG="HTTPTEST.MAINACT";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d(LOGTAG, "onCreate called");
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(LOGTAG, "onCreateOptionsMenu called");
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
