/*
*  [File]  MainManu.java
*  Copyright (C) 2014-2015 MegaChips Corporation
*
* onActivityResult()
* showMessage()
* onBackPressed() Function is
*
* Copyright (c) 2015, Nordic Semiconductor
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* Redistributions of source code must retain the above copyright notice, this
* list of conditions and the following disclaimer.
*
* Redistributions in binary form must reproduce the above copyright notice,
* this list of conditions and the following disclaimer in the documentation
* and/or other materials provided with the distribution.
*
* Neither the name of nRF UART nor the names of its
* contributors may be used to endorse or promote products derived from
* this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
* FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
* DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
* SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
* CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
* OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package co.megachips.hybridgpsmonitor;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import co.megachips.hybridgpsmonitor.ble_service.BleCommand;
import co.megachips.hybridgpsmonitor.ble_service.BleDataHandler;
import co.megachips.hybridgpsmonitor.ble_service.BleService;
import co.megachips.hybridgpsmonitor.sub_activity.HybridGPS;
import co.megachips.hybridgpsmonitor.sub_activity.MapMatching;
import co.megachips.hybridgpsmonitor.sub_activity.SPIflash_HybridGPS;
import co.megachips.hybridgpsmonitor.sub_activity.UpdateFrizzFW;
import co.megachips.hybridgpsmonitor.sub_activity.DR;

/**@brief Main activity of application
 */
public class MainMenu extends Activity implements Runnable
{

	ImageButton calibrationButton;
	ImageButton connectButton;

	boolean connected;

	BleDataHandler bleDataHandler;
	private BleService mService = null;
	private BluetoothDevice mDevice = null;
	private BluetoothAdapter mBtAdapter = null;

	private int mState = UART_PROFILE_DISCONNECTED;

	private final int RESET_BUTTON = 0;
	private final int CALIBRATION_BUTTON = 1;
	private final int CONNECT_BUTTON = 2;

	private static final int REQUEST_SELECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	private static final int UART_PROFILE_CONNECTED = 20;
	private static final int UART_PROFILE_DISCONNECTED = 21;

	private boolean service_bined = false;
	static private boolean	 heart_rate_mode = false;
	private Timer mTimer;
	private String[] items_DEMO_MODE_save = null;

	private View customDialogView;
	private LayoutInflater inflater;
	private AlertDialog.Builder builder;

	boolean calibrating = false;

	Calibration calibration;

	private final Handler handler = new Handler();

	private ProgressDialog dialog;

	public static final String TAG = "nRF_BLE";
	Calibration_packet calibrationPacket;

	//ArrayAdapter<String> adapt = null;
	CustomArrayAdapter adapt = null;
	ListView menuList = null;
	private int		 list_disable_position = 0;

	private long	g_customer_type;
	private long	g_frizz_version_ex;
	private long	g_nordic_version_ex;
	private int		g_frizz_source_device;
	private int		g_board_type;
	private int		g_DipSw;

	private boolean		f_gyro_lpf_receive  = false;
	private Timer gps_lpf_Timer;	//Timer for gps_lpf
	private Handler gyro_lpf_handler = new Handler();
	String ver_info = "";

	/**@brief Initializing values and create buttons, set handler, start timer.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_HOME);
		String title = getString(R.string.app_name);
		getActionBar().setTitle(title);
		getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0090FF")));
		getActionBar().setSubtitle("");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);

		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();

		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBtAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		inflater = LayoutInflater.from(this);
		builder = new AlertDialog.Builder(this);

		LinearLayout linearlayout3 = (LinearLayout)findViewById(R.id.linearLayout3);
		@SuppressWarnings("deprecation")
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int)(disp.getWidth())/2, 200);
		params.setMargins(2,2,2,2);

		/*------------------------------*/
		/*		Calibration button 		*/
		/*------------------------------*/
		calibrationButton = new ImageButton(this);
		calibrationButton.setBackground(null);
		calibrationButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
		calibrationButton.setPadding(0, 0, 0, 0);
		calibrationButton.setImageResource(R.drawable.calibration_button_off);
		calibrationButton.setLayoutParams(params);
		linearlayout3.addView(calibrationButton);
		calibrationButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				button_click_event(CALIBRATION_BUTTON);
			}
		});
		calibrationButton.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch(action){
				case MotionEvent.ACTION_DOWN:
					calibrationButton.setImageResource(R.drawable.calibration_button_on);
					break;
				case MotionEvent.ACTION_UP:
					calibrationButton.setImageResource(R.drawable.calibration_button_off);
					break;
			}
				return false;
			}
		});

		/*--------------------------*/
		/*		Connect button 		*/
		/*--------------------------*/
		connectButton = new ImageButton(this);
		connectButton.setBackground(null);
		connectButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
		connectButton.setPadding(0, 0, 0, 0);
		connectButton.setImageResource(R.drawable.connect_button_off);
		connectButton.setLayoutParams(params);
		linearlayout3.addView(connectButton);
		connectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				button_click_event(CONNECT_BUTTON);
			}
		});
		connected = false;
		connectButton.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch(action){
				case MotionEvent.ACTION_DOWN:
					if( connected == true ){
						connectButton.setImageResource(R.drawable.disconnect_button_on);
					}else{
						connectButton.setImageResource(R.drawable.connect_button_on);
					}
					break;
				case MotionEvent.ACTION_UP:
					if( connected == true ){
						connectButton.setImageResource(R.drawable.disconnect_button_off);
					}else{
						connectButton.setImageResource(R.drawable.connect_button_off);
					}
					break;
			}
				return false;
			}
		});

		/*--------------------------------------------------*/
		/*		Initializing service for BLE connection		*/
		/*--------------------------------------------------*/
		service_init();

		/*--------------------------*/
		/*		Add menu items		*/
		/*--------------------------*/
		String[] items_DEMO_MODE = {
				getResources().getString(R.string.dr),
				getResources().getString(R.string.hybridgps),
				getResources().getString(R.string.spi_flash_hybridgps),
				getResources().getString(R.string.update_frizz_fw),
				getResources().getString(R.string.mapmatching)
		};

		items_DEMO_MODE_save = items_DEMO_MODE;

		menuList = (ListView) findViewById(R.id.listView1);
		adapt = new CustomArrayAdapter(this, R.layout.menu_item);
		for( int loop = 0 ; loop < items_DEMO_MODE_save.length ; loop++ ) {
			adapt.add(items_DEMO_MODE_save[loop]);
		}

		//
		menuList.setAdapter(adapt);

		menuList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

				/*----------------------------------*/
				/*		Add menu item events		*/
				/*----------------------------------*/
				TextView textView = (TextView) arg1;
				String strText = textView.getText().toString();

				if(connected == true)
				{
					//needless calibration item
					if (strText.equalsIgnoreCase(getResources().getString(R.string.update_frizz_fw))) {
						Intent objIntent = new Intent(MainMenu.this, UpdateFrizzFW.class);
						startActivity(objIntent);
					} else if (strText.equalsIgnoreCase(getResources().getString(R.string.dr))) {
						Intent objIntent = new Intent(MainMenu.this, DR.class);
						startActivity(objIntent);
					} else if (strText.equalsIgnoreCase(getResources().getString(R.string.hybridgps))) {
						Intent objIntent = new Intent(MainMenu.this, HybridGPS.class);
						startActivity(objIntent);
					} else if (strText.equalsIgnoreCase(getResources().getString(R.string.mapmatching))) {
						Intent objIntent = new Intent(MainMenu.this, MapMatching.class);
						startActivity(objIntent);
					} else if (strText.equalsIgnoreCase(getResources().getString(R.string.spi_flash_hybridgps))) {
						Intent objIntent = new Intent(MainMenu.this, SPIflash_HybridGPS.class);
						startActivity(objIntent);
					}
				} else {
					if (strText.equalsIgnoreCase(getResources().getString(R.string.mapmatching))) {
						Intent objIntent = new Intent(MainMenu.this, MapMatching.class);
						startActivity(objIntent);
					}else {
						showMessage(getResources().getString(R.string.connect_device));
					}
				}

			}

		});

	    mTimer = new Timer(true);
	    mTimer.schedule( new TimerTask(){
			@Override
			public void run() {
				timer_Tick();
			}
		}, 1000, 1000);

	}


	/**@brief Interval timer function for checking BLE connection.
	 */
	public void timer_Tick(){
		broadcast(BroadcastCommand.BLE_STATUS_CHECK);
	}


	/**@brief Button click event handler.
	 * @param[in] clicked_button clicked_button  ID of clicked button number.
	 */
	private void button_click_event(int clicked_button){

		//----------------------------------//
		//		Connect button pressed		//
		//----------------------------------//
		if( clicked_button == CONNECT_BUTTON ){

			if( !mBtAdapter.isEnabled() ){
				Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			}else{

				if( connected == false ){
					// open DeviceListActivity class for listing scanned devices.
					Intent newIntent = new Intent(this, DeviceListActivity.class);
					startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
				} else {
					if( mDevice != null ){ mService.disconnect(); }
				}

			}

		//--------------------------------------//
		//		Calibration button pressed		//
		//--------------------------------------//
		}else if( clicked_button == CALIBRATION_BUTTON ) {

			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			alertDialogBuilder.setMessage(getResources().getString(R.string.calibration_frizz));

			alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (gps_lpf_Timer != null) {
								gps_lpf_Timer.cancel();
							}
							gps_lpf_Timer = new Timer(true);
							gps_lpf_Timer.schedule(
									new TimerTask() {
										@Override
										public void run() {
											gyro_lpf_handler.post(new Runnable() {
												@Override
												public void run() {
													f_gyro_lpf_receive = true;
												}
											});
										}
									},
									2000);
							f_gyro_lpf_receive = false;
							//Activate gyro lpf sensor
							byte[] data = BleCommand.registerListener(BleCommand.SensorID.SENSOR_ID_GYRO_LPF);
							broadcastData(data);
							execute_calibration();
						}
					});

			alertDialogBuilder.setNegativeButton(getResources().getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});

			alertDialogBuilder.setCancelable(true);
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		}
		return;
	}

	/**@brief Send calibration command and waito until calibration is done.
	 */
	private void execute_calibration()
	{
		calibrating = true;

		dialog = new ProgressDialog(this);
		dialog.setIndeterminate(true);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setMessage(getResources().getString(R.string.now_calibrating));
		dialog.show();

		calibration = new Calibration(handler, this);
		calibration.main_context = this;
		calibration.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch(item.getItemId()) {
			case R.id.menu_license :
				customDialogView = inflater.inflate(R.layout.mainmenu_message_dialog, null);
				builder.setView(customDialogView)
						.setPositiveButton("Close", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
							}
						});
				builder.show();
				return true;
			case R.id.fw_version :

				customDialogView = inflater.inflate(R.layout.fw_version_dialog, null);
				TextView version_textView = (TextView)customDialogView.findViewById(R.id.fw_version_textview);
				version_textView.setText("apk ver=" + getString(R.string.app_name) + "\n"+ ver_info);
				builder.setView(customDialogView)
						.setPositiveButton("Close", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
							}
						});
				builder.show();
				return true;
		}
		return false;
	}

	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		MenuItem menu0 = menu.findItem(R.id.menu_license);
		MenuItem menu1 = menu.findItem(R.id.fw_version);
		menu0.setVisible(true);
		menu1.setVisible(true);
		return true;
	}

	/**@brief Closing calibration dialog.
	 */
	public void run()
	{
		dialog.dismiss();
	}


	/**@brief Binding BLE service.
	 */
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder rawBinder) {

			mService = ((BleService.LocalBinder) rawBinder).getService();

			Log.d(TAG, "onServiceConnected mService= " + mService);
			if( !mService.initialize() ){
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}

		}

		public void onServiceDisconnected( ComponentName classname ){
			mService = null;
		}

	};


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


	/**@brief Broadcast intent with pointed data.
	 * @param[in] data Array of byte to send on BLE.
	 */
	public void broadcastData(byte[] data){

		BroadcastData bData = new BroadcastData(BroadcastCommand.BLE_SEND_DATA);
		bData.data = data;
		final Intent intent = new Intent(BroadcastCommand.DATA_RECEIVED_FROM_ACTIVITY);
		intent.putExtra(BroadcastData.keyword, bData);

		try{
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		}catch(Exception e){
			Log.e(TAG, e.toString());
		}
		try {Thread.sleep(100);} catch (InterruptedException e) {}				 //Sleep 100msec
	}


	/**@brief Broadcast receiver for receiving data from BLE service.
	 */
	private final BroadcastReceiver BLEStatusChangeReceiver = new BroadcastReceiver() {

		@SuppressLint("UseValueOf")
		public void onReceive( Context context, Intent intent ){

			String action = intent.getAction();

			if( action.equals( BroadcastCommand.ACTION_DATA_AVAILABLE ) ){

				getActionBar().setSubtitle(R.string.ble_connected);
				try{

					BroadcastData bData = (BroadcastData)intent.getSerializableExtra(BroadcastData.keyword);
					if(bData.commandID == BroadcastCommand.BLE_SEND_DATA) {
						DataPacket dataPacket = (DataPacket) bData.data;
						calibrationPacket = new MainMenu.Calibration_packet(dataPacket);
						if (calibrationPacket.dataStatus == true) {
							if (f_gyro_lpf_receive == true) {
								calibrating = false;
								//Deactivate guro lpf sensor
								byte[] data = BleCommand.unregisterListener(BleCommand.SensorID.SENSOR_ID_GYRO_LPF);
								broadcastData(data);
								showMessage(getResources().getString(R.string.success));
							}
						}
					}
					else if(bData.commandID == BroadcastCommand.CONNECT_TYPE) {
						byte	b_frizz_version1, b_frizz_version2, b_frizz_version3, b_frizz_version4;
						byte	b_nordic_version1, b_nordic_version2, b_nordic_version3, b_nordic_version4;
						byte	b_customer_type1, b_customer_type2, b_customer_type3, b_customer_type4;
						long	frizz_version_detail,nordic_version_detail;
						String Log_Text = "";
						DataPacket dataPacket = (DataPacket)bData.data;
						int type = dataPacket.data.get(0);

						if( dataPacket.length >= 16 ) {
							g_board_type = type;
							g_frizz_source_device = dataPacket.data.get(1);
							g_DipSw = dataPacket.data.get(2);
							//
							b_nordic_version1 = dataPacket.data.get(4);
							b_nordic_version2 = dataPacket.data.get(5);
							b_nordic_version3 = dataPacket.data.get(6);
							b_nordic_version4 = dataPacket.data.get(7);
							g_nordic_version_ex = combineByte_long(b_nordic_version4, b_nordic_version3, b_nordic_version2, b_nordic_version1);
							nordic_version_detail = combineByte_long((byte) 0, (byte) 0, b_nordic_version2, b_nordic_version1);
							b_frizz_version1 = dataPacket.data.get(8);
							b_frizz_version2 = dataPacket.data.get(9);
							b_frizz_version3 = dataPacket.data.get(10);
							b_frizz_version4 = dataPacket.data.get(11);
							g_frizz_version_ex = combineByte_long(b_frizz_version4, b_frizz_version3, b_frizz_version2, b_frizz_version1);
							frizz_version_detail = combineByte_long((byte) 0,(byte)  0, b_frizz_version2, b_frizz_version1);
							b_customer_type1 = dataPacket.data.get(12);
							b_customer_type2 = dataPacket.data.get(13);
							b_customer_type3 = dataPacket.data.get(14);
							b_customer_type4 = dataPacket.data.get(15);
							g_customer_type = combineByte_long(b_customer_type4, b_customer_type3, b_customer_type2, b_customer_type1);

							if( g_frizz_version_ex != 0 ) {
								if (g_board_type == 0) {
									Log_Text = Log_Text + String.format("board=Chignon\n");
								} else if (g_board_type == 1) {
									Log_Text = Log_Text + String.format("board=Mullet\n");
								} else {
									Log_Text = Log_Text + String.format("board=Unknown\n");
								}
								Log_Text = Log_Text + String.format("nordic_version=%d.%d.%04d\n", b_nordic_version4, b_nordic_version3, nordic_version_detail);
								Log_Text = Log_Text + String.format("dip_sw=%d\n", g_DipSw);
								if (g_frizz_source_device == 0) {
									Log_Text = Log_Text + String.format("frizz_source=Internal Flash\n");
								} else {
									Log_Text = Log_Text + String.format("frizz_source=External SPI-%d\n", g_frizz_source_device);
								}
								Log_Text = Log_Text + String.format("frizz_version=%d.%d.%04d\n", b_frizz_version4, b_frizz_version3, frizz_version_detail);
								Log_Text = Log_Text + String.format("firmware code=%08X", g_customer_type);
								ver_info = Log_Text;
							} else {
								Log_Text = Log_Text + String.format("nordic_version=%d.%d.%04d\n", b_nordic_version4, b_nordic_version3, nordic_version_detail);
								Log_Text = Log_Text + String.format("dip_sw=%d\n", g_DipSw);
								ver_info = Log_Text;
								Log_Text = Log_Text + String.format("frizz does not start (failed!!)");
							}

						} else {
							g_board_type = type;
							g_frizz_source_device = 0;
							g_frizz_version_ex = 0;
							g_customer_type = 0;
							Log_Text = Log_Text + String.format("Not support nordic version");
						}
						Toast.makeText(getApplicationContext(), Log_Text , Toast.LENGTH_LONG).show();
						Log.d(TAG, Log_Text);
						//
					}
				}catch (Exception e){
					return;
				}

			}else if( action.equals( BroadcastCommand.ACTION_GATT_CONNECTED ) ){

				runOnUiThread( new Runnable(){
					public void run() {
						Log.d(TAG, "BLE_CONNECT_MSG");
						connectButton.setImageResource(R.drawable.disconnect_button_off);
						getActionBar().setSubtitle(R.string.ble_connected);
						connected = true;
						mState = UART_PROFILE_CONNECTED;
					}
				} );

			}else if( action.equals( BroadcastCommand.ACTION_GATT_DISCONNECTED ) ){

				runOnUiThread( new Runnable() {
					public void run() {
						Log.d(TAG, "UART_DISCONNECT_MSG");
						connectButton.setImageResource(R.drawable.connect_button_off);
						getActionBar().setSubtitle("");
						connected = false;
						mState = UART_PROFILE_DISCONNECTED;
						mService.close();
						ver_info = "";
					}
				});

			}else if( action.equals( BroadcastCommand.ACTION_GATT_SERVICES_DISCOVERED ) ){

				mService.enableTXNotification();

			}else if( action.equals( BroadcastCommand.DEVICE_DOES_NOT_SUPPORT_UART ) ){

				showMessage("Device doesn't support this application. Disconnecting");
				mService.disconnect();

			}else if( action.equals( BroadcastCommand.ACTION_BLE_STATUS_RESULT ) ){

				BroadcastData bData = (BroadcastData)intent.getSerializableExtra(BroadcastData.keyword);
				String objStr = ((Object)bData.data).toString();
				int result = new Integer(objStr).intValue();
				if(result == BleService.STATE_DISCONNECTED){
					Log.d(TAG, "UART_DISCONNECT_MSG");
					connectButton.setImageResource(R.drawable.connect_button_off);
					getActionBar().setSubtitle("");
					connected = false;
					mState = UART_PROFILE_DISCONNECTED;
					mService.close();
				}

			}

		}

	};


	/**@brief Intent filter for receiving data from BLE service.
	 */
	private static IntentFilter makeGattUpdateIntentFilter() {

		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BroadcastCommand.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BroadcastCommand.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BroadcastCommand.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BroadcastCommand.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(BroadcastCommand.DEVICE_DOES_NOT_SUPPORT_UART);
		intentFilter.addAction(BroadcastCommand.ACTION_BLE_STATUS_RESULT);
		return intentFilter;

	}


	/**@brief Initializing BLE service and binding with the service.
	 */
	private void service_init() {

		if (service_bined == false){
			service_bined = true;
			Intent bindIntent = new Intent(this, BleService.class);
			bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
			LocalBroadcastManager.getInstance(this).registerReceiver(BLEStatusChangeReceiver, makeGattUpdateIntentFilter());
		}

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

		if(gps_lpf_Timer != null) {
			gps_lpf_Timer.cancel();
		}

		try{
			LocalBroadcastManager.getInstance(this).unregisterReceiver(BLEStatusChangeReceiver);
		}catch (Exception ignore){
			Log.e(TAG, ignore.toString());
		}

		unbindService(mServiceConnection);
		mService.stopSelf();
		mService= null;

	}


	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
	}


	/**@brief Unregister broadcast receiver at the time of exiting this activity.
	 */
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
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
		broadcast(BroadcastCommand.BLE_STATUS_CHECK);
	}


	@Override
	public void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		if (!mBtAdapter.isEnabled()) {
			Log.i(TAG, "onResume - BT not enabled yet");
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
		LocalBroadcastManager.getInstance(this).registerReceiver(BLEStatusChangeReceiver, makeGattUpdateIntentFilter());
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}


	/**@brief Handler of DeviceListActivity of listing connectable devices.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {

		case REQUEST_SELECT_DEVICE:

			/*--------------------------------------------------*/
			/*		When the DeviceListActivity return,			*/
			/*		with the selected device addressq			*/
			/*--------------------------------------------------*/
			if (resultCode == Activity.RESULT_OK && data != null) {
				String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
				mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
				mService.connect(deviceAddress);
			}

			break;

		case REQUEST_ENABLE_BT:

			/*----------------------------------------------------------*/
			/*		When the request to enable Bluetooth returns		*/
			/*----------------------------------------------------------*/
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
			} else {

				/*--------------------------------------------------------------*/
				/*		User did not enable Bluetooth or an error occurred		*/
				/*--------------------------------------------------------------*/
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
				finish();
			}

			break;

		default:

			Log.e(TAG, "wrong request code");
			break;

		}

	}


	/**@brief Show toast message.
	 * @param[in] msg Show message with toast.
	 */
	private void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	/**@brief Event handler of pressing backward button.
	 */
	@Override
	public void onBackPressed() {

		if (mState == UART_PROFILE_CONNECTED) {

			Intent startMain = new Intent(Intent.ACTION_MAIN);

			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			startActivity(startMain);
			showMessage("nRFUART's running in background.\n Disconnect to exit");

		} else {

			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

			alertDialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
			alertDialogBuilder.setTitle(R.string.popup_title);
			alertDialogBuilder.setMessage(R.string.popup_message);
			alertDialogBuilder.setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});

			alertDialogBuilder.setNegativeButton(R.string.popup_no, null);
			alertDialogBuilder.show();

		}
	}


	/**@brief Dealing calibrating procedure class.
	 */
	public class Calibration extends Thread
	{
		private final int TIME_OUT_COUNT = 10000;
		private int timeout=0;
		private Handler handler;
		private final Runnable listener;
		Context main_context;
		private Timer mTimer;

		public Calibration(Handler _handler, Runnable _listener)
		{
			this.handler = _handler;
			this.listener = _listener;

			mTimer = new Timer(true);
			mTimer.schedule( new TimerTask(){
				@Override
				public void run() {
					timer_Tick();
				}
			}, 1000, 1000);
		}

		/**@brief Interval timer for watching time out.
	 	*/
		public void timer_Tick(){
			timeout++;
		}

		@Override
		public void run()
		{
			try{
				MainMenu tempMainMenu = (MainMenu)main_context;
				while(tempMainMenu.calibrating == true && timeout<TIME_OUT_COUNT){}
			}catch(Exception e){
			}
			handler.post(listener);
		}
	}

	public class Calibration_packet{

		public byte direction;
		public byte sensorID;
		public byte length;
		public long timeStamp;
		public boolean dataStatus;
		public long calib_param1;
		public long calib_param2;
		public long calib_param3;
		public long calib_param4;

		public Calibration_packet(DataPacket packet){

			byte b1, b2, b3, b4;
			byte[] ver_array = new byte[50];
			int		i;

			direction = packet.data.get(1);
			sensorID = packet.data.get(2);
			length = packet.data.get(3);

			if (direction != -128) {
				dataStatus = false;
			} else {

				// direction dec:-67 = hex:0xFFFFFFFFFFFFFF8F
				// direction 0x8F means "frizz -> nordic"
				if(sensorID == -113) {
					b1 = packet.data.get(4);
					b2 = packet.data.get(5);
					b3 = packet.data.get(6);
					b4 = packet.data.get(7);
					timeStamp = combineByte_long(b1, b2, b3, b4);

					b1 = packet.data.get(8);
					b2 = packet.data.get(9);
					b3 = packet.data.get(10);
					b4 = packet.data.get(11);
					calib_param1 = combineByte_long(b1, b2, b3, b4);

					b1 = packet.data.get(12);
					b2 = packet.data.get(13);
					b3 = packet.data.get(14);
					b4 = packet.data.get(15);
					calib_param2 = combineByte_long(b1, b2, b3, b4);

					b1 = packet.data.get(16);
					b2 = packet.data.get(17);
					b3 = packet.data.get(18);
					b4 = packet.data.get(19);
					calib_param3 = combineByte_long(b1, b2, b3, b4);

					b1 = packet.data.get(20);
					b2 = packet.data.get(21);
					b3 = packet.data.get(22);
					b4 = packet.data.get(23);
					calib_param4 = combineByte_long(b1, b2, b3, b4);

					dataStatus = true;
				} else {
					dataStatus = false;
				}
			}
		}
	}

	private long combineByte_long(byte b1, byte b2, byte b3, byte b4){
		int t1 = ((int)b1<<24)    & 0xFF000000;
		int t2 = ((int)b2<<16)    & 0x00FF0000;
		int t3 = ((int)b3<<8)     & 0x0000FF00;
		int t4 = ((int)b4)        & 0x000000FF;
		long temp = (t1|t2|t3|t4) & (0xFFFFFFFFL);
		return temp;
	}
	//
	private class CustomArrayAdapter extends ArrayAdapter<String>
	{
		public CustomArrayAdapter(Context context,int textViewResId) {
    	   super(context,textViewResId);
       }

		public boolean isEnabled(int position)
		{
			if( (position == list_disable_position) && (0 != list_disable_position) )
			{
				return false;
			}
			return true;
		}
	}
}
