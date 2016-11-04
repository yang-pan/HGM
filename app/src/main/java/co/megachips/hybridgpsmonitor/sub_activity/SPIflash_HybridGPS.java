/*******************************************************************
 * @file	SPIflash.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
*******************************************************************/

package co.megachips.hybridgpsmonitor.sub_activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import co.megachips.hybridgpsmonitor.BroadcastCommand;
import co.megachips.hybridgpsmonitor.BroadcastData;
import co.megachips.hybridgpsmonitor.DataPacket;
import co.megachips.hybridgpsmonitor.R;
import co.megachips.hybridgpsmonitor.ble_service.BleCommand;
import co.megachips.hybridgpsmonitor.ble_service.BleDataHandler;
import co.megachips.hybridgpsmonitor.ble_service.BleService;
import co.megachips.hybridgpsmonitor.hybridgps.HybridGPS_Command;
import co.megachips.hybridgpsmonitor.sub_activity.custom_view.CustomAdapter3;
import co.megachips.hybridgpsmonitor.sub_activity.custom_view.CustomItemData3;

/**@brief SensorRaw HybridGPS activity
 */
public class SPIflash_HybridGPS extends Activity {

	ImageButton menuButton;
	ImageButton uartButton;
	ImageButton startstopButton;

	BleDataHandler bleDataHandler;

	private final int UART_BUTTON = 0;
	private final int START_STOP_BUTTON = 1;
	private final int MENU_BUTTON = 2;

	private final int SAMPLING_FREQ_MAX = 60000;
	private final int SAMPLING_FREQ_MIN = 10;

	private final int OUTPUT_MODE_MAGNET = R.id.mode_RadioButton1;
	private final int OUTPUT_MODE_GRAVITY = R.id.mode_RadioButton2;
	private final int DEFAULT_OUTPUT_MODE = OUTPUT_MODE_MAGNET;

	private ListView listView;
	private CustomAdapter3 customAdapater;
	private CustomItemData3 item;
	CustomItemData3 item1, item2;

	private TextView title_left;
	private TextView title_right;

	private TextView debugOut;
	private ScrollView scrollView;

	private final boolean START_ENABLE = true;
	private final boolean STOP_ENABLE = false;
	private boolean startstopStatus = START_ENABLE;

	public static final String TAG = "nRF_BLE";
	static public final String save_directory = "hybridgps_log";

	//View----------------------------------------------------------------
	private View customDialogView;
	//LayoutInflater------------------------------------------------------
	private LayoutInflater inflater;
	//RadioGroup-- for user setting -------------------------------------
	RadioGroup radio_output_type;
	//AlertDialog---------------------------------------------------------
	private AlertDialog.Builder builder;
	//Mode setting--------------------------------------------------------------
	private int input_output_mode  = DEFAULT_OUTPUT_MODE;

	public static int interval_time = 0;
	public static int	hybridgps_interval_time = 500;
	public static int	gravity_interval_time = 10;

	private Timer mTimer;										// Timer for interval timer

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.spi_flash_hybridgps);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title_bar);
		title_left = (TextView)findViewById(R.id.title_left_text);
		title_left.setText(R.string.spi_flash_hybridgps);

		title_right = (TextView)findViewById(R.id.title_right_text);
		broadcast(BroadcastCommand.BLE_STATUS_CHECK);
		title_right.setText("");

		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();

		LinearLayout linearlayout2 = (LinearLayout)findViewById(R.id.linearLayout2);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(linearlayout2.getLayoutParams().width, linearlayout2.getLayoutParams().height);
		params.setMargins(0,0,0,0);

		scrollView = new ScrollView(this);
		debugOut = new TextView(this);
		debugOut.setTextSize(14);
		debugOut.setTextColor(Color.GRAY);
		debugOut.setBackgroundColor(Color.WHITE);
		debugOut.setPadding(10, 10, 10, 10);

		scrollView.addView(debugOut);
		linearlayout2.addView(scrollView);

		LinearLayout linearlayout3 = (LinearLayout)findViewById(R.id.linearLayout3);
		params = new LinearLayout.LayoutParams((int)(disp.getWidth())/3, 200);
		params.setMargins(2, 2, 2, 2);

		inflater = LayoutInflater.from(this);
		builder = new AlertDialog.Builder(this);

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

		/*----------------------*/
		/*		Uart button		*/
		/*----------------------*/
		uartButton = new ImageButton(this);
		uartButton.setBackground(null);
		uartButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
		uartButton.setPadding(0, 0, 0, 0);
		uartButton.setImageResource(R.drawable.uart_button_off);
		uartButton.setLayoutParams(params);
		linearlayout3.addView(uartButton);
		uartButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
		    	button_click_event(UART_BUTTON);
			}
		});
		uartButton.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch(action){
				case MotionEvent.ACTION_DOWN:
					uartButton.setImageResource(R.drawable.uart_button_on);
					break;
				case MotionEvent.ACTION_UP:
					uartButton.setImageResource(R.drawable.uart_button_off);
					break;
			}
				return false;
			}
		});

		mTimer = new Timer(true);
		mTimer.schedule( new TimerTask(){
			@Override
			public void run() {
				timer_Tick();
			}
		}, 1000, 1000);


		/*--------------------------*/
		/*		Add menu items		*/
		/*--------------------------*/
		List<CustomItemData3> objects = new ArrayList<CustomItemData3>();

		item1 = new CustomItemData3();
		item1.setInterval_sensor_id("01");
		item1.setInterval(hybridgps_interval_time);
		objects.add(item1);
		item2 = new CustomItemData3();
		item2.setInterval_sensor_id("02");
		item2.setInterval(gravity_interval_time);
		objects.add(item2);

		customAdapater = new CustomAdapter3(this, 0, objects);

		listView = (ListView)findViewById(R.id.listView2);		// Get ListView
		listView.setAdapter(customAdapater);      			// Make a adapter to output on ListView

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				getValue(position);
			}

		});

	/*------------------------------*/
	/*		StartStop button		*/
	/*------------------------------*/
	startstopButton = new ImageButton(this);
	startstopButton.setBackground(null);
	startstopButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
	startstopButton.setPadding(0, 0, 0, 0);
	startstopButton.setImageResource(R.drawable.start_button_off);
	startstopButton.setLayoutParams(params);
	linearlayout3.addView(startstopButton);
	startstopButton.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
			button_click_event(START_STOP_BUTTON);
		}
	});
	startstopButton.setOnTouchListener(new View.OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch(action){
			case MotionEvent.ACTION_DOWN:
				if(startstopStatus == START_ENABLE){
					startstopButton.setImageResource(R.drawable.start_button_on);
				}else{
					startstopButton.setImageResource(R.drawable.stop_button_on);
				}
				break;
			case MotionEvent.ACTION_UP:
				if(startstopStatus == START_ENABLE){
					startstopButton.setImageResource(R.drawable.start_button_off);
				}else{
					startstopButton.setImageResource(R.drawable.stop_button_off);
				}
				break;
		}
			return false;
		}
	});
	}

	/**@brief Get value of selected item for setting sampling frequency
	 * @param[in] selected_num selected item number
	 */
	private int getValue(int selected_num){

		if(selected_num == 0) {
			item = item1;
		}else if(selected_num == 1){
			item = item2;
		}else{
			return 0;
		}

		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		LinearLayout layout = new LinearLayout(this);
		final EditText text1 = new EditText(this);

		text1.setText(Integer.toString(item.interval));			//Set cycle

		layout.addView(make_TextView(getResources().getString(R.string.set_cycle)), new LinearLayout.LayoutParams( 300, 40));
		layout.addView(text1, new LinearLayout.LayoutParams( 300,90));

		dialog.setView(layout);
		layout.setOrientation(LinearLayout.VERTICAL);

		dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {
	        	int sampling_freq = Integer.valueOf((text1.getText().toString()));

				if((sampling_freq >= SAMPLING_FREQ_MIN) && (sampling_freq < SAMPLING_FREQ_MAX)){
					item.setInterval(sampling_freq);
					customAdapater.notifyDataSetChanged();
					interval_time = sampling_freq;
				}else
				{
					showMessage("You cannot use value of frequency. 10ms set.");
					item.setInterval(SAMPLING_FREQ_MIN);
					customAdapater.notifyDataSetChanged();
					interval_time = SAMPLING_FREQ_MIN;
				}
		    }
		});

		if(selected_num == 0) {
			hybridgps_interval_time = interval_time;
		}else if(selected_num == 1){
			gravity_interval_time = interval_time;
		}else{
			return 0;
		}

		dialog.setNegativeButton("Cancle",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		dialog.show();
		return 0;

	}

    private TextView make_TextView(String sMessage){
		//Make textview
		TextView tv = new TextView(getApplicationContext());
		//set massage
		tv.setText(sMessage);
		return tv;
	}

	/**@brief Show toast message.
	 * @param[in] msg Show message with toast.
	 */
	private void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	/**@brief Send sampling frequency on BLE as byte deviced data.
	 */
	public void sendSamplingFrequency(){

		if(item!=null){
			byte[] data = new BleCommand(BleCommand.RequestType.SET_SAMPLING_FREQUENCY).getData();
			data[1] = (byte) ((item.interval & 0xFF000000)>>24);
			data[2] = (byte) ((item.interval & 0x00FF0000)>>16);
			data[3] = (byte) ((item.interval & 0x0000FF00)>>8);
			data[4] = (byte) ((item.interval & 0x000000FF));
			broadcastDataSPIflash(data);
		}

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
		if( clicked_button == MENU_BUTTON ){
			byte[] data = new BleCommand(BleCommand.RequestType.SPI_FLASH_GPS_SET_SAMPLING_STOP).getData();
			broadcastDataSPIflash(data);
			this.finish();

		//----------------------------------//
		//		UART button pressed			//
		//----------------------------------//
		}else if( clicked_button == UART_BUTTON ){

			customDialogView = inflater.inflate(R.layout.spi_hybridgps_dialog,
					(ViewGroup)findViewById(R.id.spi_hybridgps_layout));
			radio_output_type = (RadioGroup)customDialogView.findViewById(R.id.mode_RadioGroup);
			radio_output_type.check(input_output_mode);
			radio_output_type.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					input_output_mode = checkedId;
				}
				});

			builder.setView(customDialogView)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(input_output_mode == R.id.mode_RadioButton1) {
								byte[] data = new BleCommand(BleCommand.RequestType.SPI_FLASH_GPS_SET_UART).getData();
								broadcastDataSPIflash(data);
							} else if(input_output_mode == R.id.mode_RadioButton2){	//Gravity
								byte[] data = new BleCommand(BleCommand.RequestType.SPI_FLASH_GRAVITY_SET_UART).getData();
								broadcastDataSPIflash(data);
							} else {	//Gravity
								//non
							}
						}
					})
					.setNegativeButton("Cancel", null);
			builder.show();

		//--------------------------------------//
		//		START_STOP button pressed		//
		//--------------------------------------//
		}else if( clicked_button == START_STOP_BUTTON ){
			if(startstopStatus == START_ENABLE){

				customDialogView = inflater.inflate(R.layout.spi_hybridgps_dialog,
						(ViewGroup)findViewById(R.id.spi_hybridgps_layout));
				radio_output_type = (RadioGroup)customDialogView.findViewById(R.id.mode_RadioGroup);
				radio_output_type.check(input_output_mode);
				radio_output_type.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						input_output_mode = checkedId;
					}
				});

				builder.setView(customDialogView)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (input_output_mode == R.id.mode_RadioButton1) {    //HybridGPS
									if(item1 != null) {
										byte[] data = new BleCommand(BleCommand.RequestType.SPI_FLASH_GPS_SET_SAMPLING_START).getData();
										data[1] = (byte) ((item1.interval & 0xFF000000) >> 24);
										data[2] = (byte) ((item1.interval & 0x00FF0000) >> 16);
										data[3] = (byte) ((item1.interval & 0x0000FF00) >> 8);
										data[4] = (byte) ((item1.interval & 0x000000FF));
										broadcastDataSPIflash(data);
									}
								} else if (input_output_mode == R.id.mode_RadioButton2) {    //Gravity
									if(item2 != null) {
										byte[] data = new BleCommand(BleCommand.RequestType.SPI_FLASH_GRAVITY_SET_SAMPLING_START).getData();
										data[1] = (byte) ((item2.interval & 0xFF000000) >> 24);
										data[2] = (byte) ((item2.interval & 0x00FF0000) >> 16);
										data[3] = (byte) ((item2.interval & 0x0000FF00) >> 8);
										data[4] = (byte) ((item2.interval & 0x000000FF));
										broadcastDataSPIflash(data);
									}
								} else {
									//non
								}
								startstopStatus = STOP_ENABLE;
								startstopButton.setImageResource(R.drawable.stop_button_off);
							}
						})
						.setNegativeButton("Cancel", null);
				builder.show();
			}else{
				byte[] data = new BleCommand(BleCommand.RequestType.SPI_FLASH_GPS_SET_SAMPLING_STOP).getData();
				data[1] = 0x00; //dummy
				broadcastDataSPIflash(data);
				data = new BleCommand(BleCommand.RequestType.SPI_FLASH_GRAVITY_SET_SAMPLING_STOP).getData();
				data[1] = 0x00; //dummy
				broadcastDataSPIflash(data);
				startstopStatus = START_ENABLE;
				startstopButton.setImageResource(R.drawable.start_button_off);
			}
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

	/**@brief Broadcast intent with pointed data.
	 * @param[in] data Array of byte to send on BLE.
	 */
	public void broadcastDataSPIflash(byte[] data)
	{
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

				title_right.setText(R.string.ble_connected);
				BroadcastData bData = (BroadcastData)intent.getSerializableExtra(BroadcastData.keyword);
				DataPacket dataPacket = (DataPacket)bData.data;

				if(bData.commandID == BroadcastCommand.STATE_STARTED){
					startstopStatus = STOP_ENABLE;
					startstopButton.setImageResource(R.drawable.stop_button_off);
				}else if(bData.commandID == BroadcastCommand.STATE_STOPPED) {
					startstopStatus = START_ENABLE;
					startstopButton.setImageResource(R.drawable.start_button_off);
				} else {
					//non
				}
			}else if( action.equals( BroadcastCommand.ACTION_GATT_CONNECTED ) ){
				title_right.setText(R.string.ble_connected);
			}else if( action.equals( BroadcastCommand.ACTION_GATT_DISCONNECTED ) ){
				title_right.setText("");
				startstopStatus = STOP_ENABLE;
			}else if( action.equals( BroadcastCommand.ACTION_BLE_STATUS_RESULT ) ){
				BroadcastData bData = (BroadcastData)intent.getSerializableExtra(BroadcastData.keyword);
				String objStr = ((Object)bData.data).toString();
				int result = new Integer(objStr).intValue();
				if(result == BleService.STATE_CONNECTED){
					title_right.setText(R.string.ble_connected);
				}else{
					title_right.setText("");
					startstopStatus = STOP_ENABLE;
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

		byte[] data = new BleCommand(BleCommand.RequestType.SPI_FLASH_SET_SAMPLING_STOP).getData();
		broadcastDataSPIflash(data);

		try{
			LocalBroadcastManager.getInstance(this).unregisterReceiver(BLEStatusChangeReceiver);
		}catch (Exception ignore){
			Log.e(TAG, ignore.toString());
		}

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
	}


	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		LocalBroadcastManager.getInstance(this).registerReceiver(BLEStatusChangeReceiver, makeGattUpdateIntentFilter());
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
}


