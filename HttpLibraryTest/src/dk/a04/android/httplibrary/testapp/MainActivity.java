package dk.a04.android.httplibrary.testapp;

import dk.a04.android.httplib.HttpHelper;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class MainActivity extends Activity {

	public static final String LOGTAG="HTTPTEST.MAINACT";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(LOGTAG, "onCreate called");
		
		showMainScreen();
		
		//android.app.FragmentManager fm = getFragmentManager();
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(LOGTAG, "onCreateOptionsMenu called");
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void showMainScreen() {
		setContentView(R.layout.activity_main);
		
		Spinner spinner = (Spinner) findViewById(R.id.spMethod);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.form_methods_array, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
	}

	public void goButtonClicked(View v) {
		Log.d(LOGTAG, "Go button has been clicked");
		final Handler h = new Handler() {
			public void handleMessage(Message m) {
				Log.d(LOGTAG, "I have a message to handle");
			}
		};
		
		HttpHelper hh = new HttpHelper(h, getApplicationContext());
		
		hh.get("http://blok4.dk");
		
	}
	
}
