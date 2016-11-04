/*******************************************************************
 * @file	MapMatching.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2016/05/18
 * @author	MegaChips
 * @date	2014/02/26
*******************************************************************/

package co.megachips.hybridgpsmonitor.sub_activity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import co.megachips.hybridgpsmonitor.BroadcastCommand;
import co.megachips.hybridgpsmonitor.BroadcastData;
import co.megachips.hybridgpsmonitor.DataPacket;
import co.megachips.hybridgpsmonitor.OpenStreetMapsEvent;
import co.megachips.hybridgpsmonitor.OpenStreetMapsEvent.MapEventListener;
import co.megachips.hybridgpsmonitor.R;
import co.megachips.hybridgpsmonitor.ble_service.BleService;
import co.megachips.hybridgpsmonitor.fileexplorer.FileSelectionActivity;
import co.megachips.hybridgpsmonitor.hybridgps.Network_converter;

/*
 * @brief MapMatching activity
 */
public class MapMatching extends FragmentActivity implements MapEventListener {

	//---------------------------------------
	// Member Valiables
	//---------------------------------------

	//--[Button number]---------------------------------------------
	private final int MENU_BUTTON = 0;
	//--[Debug]-----------------------------------------------------
	private final String TAG = "nRF_BLE";
	private final String TAB = null;
	//--[request code]----------------------------------------------
	Bundle extras;
	private static final int	REQUEST_FILESELECT	= 0;
	// Not Final
	//Button----------------------------------------------------------
	private ImageButton menuButton;

	//-------------------------------------------------
	// Define class
	//-------------------------------------------------
	static private OpenStreetMapsEvent OSMEvent;	//Map event class
	static private Network_converter network_converter;

	WakeLock cpuWakeLock;

	private static int	BLE_CONNECT = 0;
	private static int	BLE_STATUS = 0;

	//---------------------------------------
	// Function
	//---------------------------------------
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_HOME);
		getActionBar().setTitle(R.string.mapmatching);
		getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0090FF")));
		getActionBar().setSubtitle("");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapmatching);

		broadcast(BroadcastCommand.BLE_STATUS_CHECK);

		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();

		LinearLayout linearlayout2 = (LinearLayout)findViewById(R.id.linearLayout2);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(linearlayout2.getLayoutParams().width, linearlayout2.getLayoutParams().height);
		params.setMargins(0,0,0,0);

		network_converter = new Network_converter();

		MapView map = new MapView(this, 256);
		map.setBuiltInZoomControls(true);
		map.setTileSource(TileSourceFactory.MAPNIK);
		map.setMultiTouchControls(true);
		MapView.LayoutParams mapParams = new MapView.LayoutParams(
				MapView.LayoutParams.MATCH_PARENT,
				MapView.LayoutParams.MATCH_PARENT, null,
				0, 0, MapView.LayoutParams.BOTTOM_CENTER);
		LinearLayout map_layout = (LinearLayout) findViewById(R.id.mapview);
		OSMEvent = OpenStreetMapsEvent.getInstance();
		OSMEvent.setContext(this);
		OSMEvent.setMap(map);
		OSMEvent.enableRouteEvent(this, this);	//Set Android location
		OSMEvent.setListener(this);
		OSMEvent.MapPosition_init();
		map_layout.addView(map, mapParams);

		LinearLayout linearlayout3 = (LinearLayout)findViewById(R.id.linearLayout3);
		Bitmap image = BitmapFactory.decodeResource(this.getResources(), R.drawable.menu_button_on);
		int image_h = image.getHeight();
		int image_w = image.getWidth();
		float scale = (float) (disp.getWidth()/5.0f)/image_w;
		params = new LinearLayout.LayoutParams((int) (disp.getWidth()/1), (int)(image_h * scale));
		params.setMargins(2, 2, 2, 2);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    cpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"gps_service");
	    cpuWakeLock.acquire();

		/*----------------------*/
		/*		Menu button		*/
		/*----------------------*/
		menuButton = new ImageButton(this);
		menuButton.setBackground(null);
		menuButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
		menuButton.setPadding(0, 0, 0, 0);
		menuButton.setImageResource(R.drawable.menu_button_off);
		menuButton.setLayoutParams(params);
		linearlayout3.addView(menuButton);
		menuButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
		    	button_click_event(MENU_BUTTON);
			}
		});
		menuButton.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch(action){
				case MotionEvent.ACTION_DOWN:
					menuButton.setImageResource(R.drawable.menu_button_on);
					break;
				case MotionEvent.ACTION_UP:
					menuButton.setImageResource(R.drawable.menu_button_off);
					break;
			}
				return false;
			}
		});
	}

	/**@brief Interval timer function for checking BLE connection.
	 */
	public void timer_Tick(){
		broadcast(BroadcastCommand.BLE_STATUS_CHECK);
	}

	/**@brief Button click event handler.
	 * @param[in] clicked_button clicked_button ID of clicked button number.
	 */
	private void button_click_event(int clicked_button){

		//----------------------------------//
		//		MENU button pressed			//
		//----------------------------------//
		if( clicked_button == MENU_BUTTON ) {
			OSMEvent.ClearLocationArray();
			this.finish();
		}
		return;
	}

	/**@brief Broadcast intent with pointed id.
	 * @param[in] id A value defined in BroadcastCommand class.
	 */
	public void broadcast(int id){
		BroadcastData bData = new BroadcastData(id);
		final Intent intent = new Intent(BroadcastCommand.DATA_RECEIVED_FROM_ACTIVITY);
		intent.putExtra(BroadcastData.keyword, bData);
		try{
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		}catch(Exception e){
			Log.e(TAG, e.toString());
		}
	}


	/**@brief Broadcast receiver for receiving data from BLE service.
	 */
	private final BroadcastReceiver BLEStatusChangeReceiver = new BroadcastReceiver() {

		@SuppressLint("UseValueOf")
		public void onReceive( Context context, Intent intent ){

			String action = intent.getAction();
			if( action.equals( BroadcastCommand.ACTION_DATA_AVAILABLE )){
				try{
					BroadcastData bData = (BroadcastData)intent.getSerializableExtra(BroadcastData.keyword);

					if(bData.commandID == BroadcastCommand.BLE_SEND_DATA){
						DataPacket dataPacket = (DataPacket)bData.data;
					}
				}catch (Exception e){
					return;
				}
			}else if( action.equals( BroadcastCommand.ACTION_GATT_CONNECTED ) ){
				getActionBar().setSubtitle(R.string.ble_connected);
				BLE_CONNECT = 1;
			}else if( action.equals( BroadcastCommand.ACTION_GATT_DISCONNECTED ) ){
				getActionBar().setSubtitle("");
				BLE_CONNECT = 0;
			}else if( action.equals( BroadcastCommand.ACTION_BLE_STATUS_RESULT ) ){
				BroadcastData bData = (BroadcastData)intent.getSerializableExtra(BroadcastData.keyword);
				String objStr = ((Object)bData.data).toString();
				int result = new Integer(objStr).intValue();
				if(result == BleService.STATE_CONNECTED){
					getActionBar().setSubtitle(R.string.ble_connected);
					BLE_STATUS = 1;
				}else{
					getActionBar().setSubtitle("");
					BLE_STATUS = 0;
				}
			}
		}
	};

	/**@brief Intent filter for receiving data from BLE service.
	 */
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BroadcastCommand.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(BroadcastCommand.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BroadcastCommand.ACTION_BLE_STATUS_RESULT);
		return intentFilter;
	}

	@Override
	public void onStart() {
		super.onStart();
	}


	/**@brief Unregister broadcast receiver at the time of exiting this activity.
	 */
	@Override
	public void onDestroy() {

		super.onDestroy();
		Log.d(TAG, "onDestroy()");
		//Deactivate sensor

		if (cpuWakeLock.isHeld())
		{
			cpuWakeLock.release();
		}
	}


	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop");
	}


	/**@brief Unregister broadcast receiver at the time of exiting this activity.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
		try{
			LocalBroadcastManager.getInstance(this).unregisterReceiver(BLEStatusChangeReceiver);
		}catch (Exception ignore){
			Log.e(TAG, ignore.toString());
		}
	}


	/**@brief Broadcast checking BLE connection status command at the time of restarting this activity.
	 */
	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(TAG, "onRestart");
	}


	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		try{
			LocalBroadcastManager.getInstance(this).registerReceiver(BLEStatusChangeReceiver, makeGattUpdateIntentFilter());
		}catch (Exception e){
			Log.e(TAG, e.toString());
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu_map_matching, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.plot_highway) {
			Intent fileIntent = new Intent( MapMatching.this, FileSelectionActivity.class );
			String loggingDirectoryName ="/sdcard/Download";
			fileIntent.putExtra( "initialdir", loggingDirectoryName);
			fileIntent.putExtra( "ext", "txt" );
			startActivityForResult( fileIntent, REQUEST_FILESELECT );
			return true;
		}
		return false;
	}

	@Override
	protected void onActivityResult(	int requestCode,
										int resultCode,
										Intent fileIntent )
	{
		TextView textview = (TextView) findViewById(R.id.textView);
		String output_text = "";

		if( REQUEST_FILESELECT == requestCode && RESULT_OK == resultCode )
		{
			long file_size;
			extras = fileIntent.getExtras();
			Network_converter.Highway_data highway_data;

			if( null != extras )
			{
				File file = (File)extras.getSerializable("file");
				output_text = "File name : " + file.getName() + "\n";
				output_text += "File path : " + file.getPath() + "\n";
				file_size = file.length();
				output_text += "File size : " + String.format("%1$,3d", file.length()) + "[Byte]\n";

				byte[] readBinary = new byte[(int)file_size];

				try {
					BufferedInputStream bis;
					FileInputStream fis;
					fis = new FileInputStream(file);
					bis = new BufferedInputStream(fis);
					try {
						OSMEvent.ClearLocationArray();
						bis.read(readBinary);
						ArrayList<Network_converter.Highway_data> highway_array = network_converter.network_converter(readBinary);
						if(highway_array.size() != 0) {
							highway_data = highway_array.get(0);
							OSMEvent.MapPosition_center((float) highway_data.start_lat, (float) highway_data.start_lon);
							for (int i = 0; i < highway_array.size(); i++) {
								highway_data = highway_array.get(i);
								OSMEvent.draw_line(highway_data.start_lat, highway_data.start_lon, highway_data.stop_lat, highway_data.stop_lon, Color.RED);
							}
						}
						output_text += "Highway data num : " + highway_array.size() + "\n";
						textview.setText(output_text);
						bis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public void deleteRoute() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		MenuItem menu0 = menu.findItem(R.id.plot_highway);
    	menu0.setVisible(true);
        return true;
	}

	/**@brief Show toast message.
	 * @param[in] msg Show message with toast.
	 */
	private void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
}
