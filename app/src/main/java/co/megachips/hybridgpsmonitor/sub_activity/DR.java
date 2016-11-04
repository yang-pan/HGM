/*******************************************************************
 * @file	PDR.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
*******************************************************************/

package co.megachips.hybridgpsmonitor.sub_activity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import co.megachips.hybridgpsmonitor.BroadcastCommand;
import co.megachips.hybridgpsmonitor.BroadcastData;
import co.megachips.hybridgpsmonitor.DataPacket;
import co.megachips.hybridgpsmonitor.R;
import co.megachips.hybridgpsmonitor.ble_service.BleCommand;
import co.megachips.hybridgpsmonitor.ble_service.BleService;
import co.megachips.hybridgpsmonitor.hybridgps.HybridGPS_Command;
import co.megachips.hybridgpsmonitor.sub_activity.custom_view.DR_CustomDraw;

/**@brief DR activity
 */
public class DR extends Activity {

	//---------------------------------------
	// Member Valiables
	//---------------------------------------
	// Final
	private final int MENU_BUTTON = 0;
	private final int RESET_BUTTON = 1;
	private final int START_STOP_BUTTON = 2;
	private final boolean START_ENABLE = true;
	private final boolean STOP_ENABLE = false;
	public static final String TAG = "nRF_BLE";

	// Not Final
	private View customDialogView, messageDialogView;
	private LayoutInflater inflater;
	private DR_CustomDraw dr_CustomDraw;	// View
	private Timer mTimer;
	private ImageButton menuButton;
	private ImageButton resetButton;
	private ImageButton startstopButton;;
	private int inputStride;
	private AlertDialog.Builder builder;

	private DR_packet drPacket;
	//private stair_detector_packet stair_detectorPacket;
	private boolean startstopStatus = START_ENABLE;
	private static int	BLE_CONNECT = 0;
	private static int	BLE_STATUS = 0;

	//RadioGroup-- for user setting -------------------------------------
	RadioGroup radio_group_hand;
	//SeekBar-- for user setting -------------------------------------
	private Button reset_button;
	private SeekBar seekbar_stride;
	private SeekBar seekbar_angle_offset;
	private SeekBar seekbar_dr_plot;
	//TextView-- for user setting -------------------------------------
	private TextView textview_stride;
	private TextView textview_angle_offset;
	private TextView textview_dr_plot;
	//DR setting--------------------------------------------------------------
	private int input_stride 		= HybridGPS_Command.user_default_param.DEFAULT_STRIDE;
	private int input_hand_mode		= HybridGPS_Command.user_default_param.DEFAULT_DETECT_HANDMODE;
	private int input_angle_offset	= HybridGPS_Command.user_default_param.DEFAULT_ANGLE_OFFSET;
	private int input_dr_plot	= HybridGPS_Command.user_default_param.DEFAULT_DR_PLOT;

	//---------------------------------------
	// Function
	//---------------------------------------
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_HOME);
		getActionBar().setTitle(R.string.dr);
		getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0090FF")));

		getActionBar().setSubtitle("");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.dr);

		broadcast(BroadcastCommand.BLE_STATUS_CHECK);

		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();

		LinearLayout linearlayout2 = (LinearLayout)findViewById(R.id.linearLayout2);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(linearlayout2.getLayoutParams().width, linearlayout2.getLayoutParams().height);
		params.setMargins(0,0,0,0);
		dr_CustomDraw = new DR_CustomDraw(this);
		linearlayout2.addView(dr_CustomDraw);

		LinearLayout linearlayout3 = (LinearLayout)findViewById(R.id.linearLayout3);
		params = new LinearLayout.LayoutParams((int)(disp.getWidth()) /3, 200);
		params.setMargins(2,2,2,2);

		inputStride = 80;
		inflater = LayoutInflater.from(this);
		builder = new AlertDialog.Builder(this);

		radio_group_hand = new RadioGroup(this);

		// *** set plot 0x03 ***
		int plot[] = new int[1];
		plot[0] = input_dr_plot;
		byte [] data = BleCommand.Set_SensorControl_Param(0x03, 0x00, BleCommand.SensorID.SENSOR_ID_DR, 0x01, plot);
		broadcastData(data);

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

		/*--------------------------*/
		/*		Reset button		*/
		/*--------------------------*/
		resetButton = new ImageButton(this);
		resetButton.setBackground(null);
		resetButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
		resetButton.setPadding(0, 0, 0, 0);
		resetButton.setImageResource(R.drawable.reset_button_off);
		resetButton.setLayoutParams(params);
		linearlayout3.addView(resetButton);
		resetButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				button_click_event(RESET_BUTTON);
			}
		});
		resetButton.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch(action){
				case MotionEvent.ACTION_DOWN:
					resetButton.setImageResource(R.drawable.reset_button_on);
					break;
				case MotionEvent.ACTION_UP:
					resetButton.setImageResource(R.drawable.reset_button_off);
					break;
			}
				return false;
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
	 * @param[in] clicked_button clicked_button ID of clicked button number.
	 */
	private void button_click_event(int clicked_button){

		//----------------------------------//
		//		MENU button pressed			//
		//----------------------------------//
		if( clicked_button == MENU_BUTTON ){
			//deactivateDR sensor
			byte[] data = BleCommand.unregisterListener(0xD0);
			broadcastData(data);
			this.finish();

		//----------------------------------//
		//		RESET button pressed		//
		//----------------------------------//
		}else if( clicked_button == RESET_BUTTON ){
			//deactivate DR sensor
			byte[] data = BleCommand.unregisterListener(0xD0);
			broadcastData(data);
			dr_CustomDraw.resetOffset();

		//--------------------------------------//
		//		START STOP button pressed		//
		//--------------------------------------//
		}else if( clicked_button == START_STOP_BUTTON ){

			Intent intent = getIntent();
	        int mode = intent.getIntExtra("int mode",0);

			// START_ENABLE: START button is shown.
			// STOP_ENABLE : STOP  button is shown.
			if(startstopStatus == START_ENABLE){

				//Start logging mode
				byte[] data = BleCommand.registerListener(0xD0);
				broadcastData(data);

				int stride[] = new int[1];
				stride[0] = inputStride;

			}else{
				//deactivate DR sensor
				byte[] data = BleCommand.unregisterListener(0xD0);
				broadcastData(data);
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
	public void broadcastData(byte[] data)
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
			if( action.equals( BroadcastCommand.ACTION_DATA_AVAILABLE )){

				try{
					BroadcastData bData = (BroadcastData)intent.getSerializableExtra(BroadcastData.keyword);

					// Recieve DR Packet
					if(bData.commandID == BroadcastCommand.BLE_SEND_DATA){
						DataPacket dataPacket = (DataPacket)bData.data;

						// If sensorID is SENSOR_ID_DR(0xD0), it use DR class.
						drPacket = new DR_packet(dataPacket);
						if(drPacket.dataStatus == true){
							dr_CustomDraw.addPacket(drPacket);
						}
					// After revieved STATE_STARTED command, StartStop button changed to START button.
					}else if(bData.commandID == BroadcastCommand.STATE_STARTED){

						startstopStatus = STOP_ENABLE;
						startstopButton.setImageResource(R.drawable.stop_button_off);

					// After revieved STATE_STOPPED command, StartStop button changed to STOP button.
					}else if(bData.commandID == BroadcastCommand.STATE_STOPPED){

						startstopStatus = START_ENABLE;
						startstopButton.setImageResource(R.drawable.start_button_off);

					}

				}catch (Exception e){
					return;
				}
				dr_CustomDraw.timer_Tick();
			}else if( action.equals( BroadcastCommand.ACTION_GATT_CONNECTED ) ){
				getActionBar().setSubtitle(R.string.ble_connected);
				if( BLE_CONNECT == 0 )
				{
					dr_CustomDraw.timer_Tick();
				}
				BLE_CONNECT = 1;
			}else if( action.equals( BroadcastCommand.ACTION_GATT_DISCONNECTED ) ){
				getActionBar().setSubtitle("");
				startstopStatus = START_ENABLE;
				startstopButton.setImageResource(R.drawable.start_button_off);
				if( BLE_CONNECT != 0 )
				{
					dr_CustomDraw.timer_Tick();
				}
				BLE_CONNECT = 0;
			}else if( action.equals( BroadcastCommand.ACTION_BLE_STATUS_RESULT ) ){
				BroadcastData bData = (BroadcastData)intent.getSerializableExtra(BroadcastData.keyword);
				String objStr = ((Object)bData.data).toString();
				int result = new Integer(objStr).intValue();
				if(result == BleService.STATE_CONNECTED){
					getActionBar().setSubtitle(R.string.ble_connected);
					if( BLE_STATUS == 0 )
					{
						dr_CustomDraw.timer_Tick();
					}
					BLE_STATUS = 1;
				}else{
					getActionBar().setSubtitle("");
					if( BLE_STATUS != 0 )
					{
						dr_CustomDraw.timer_Tick();
					}
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

		//deactivate DR sensor
		byte[] data = BleCommand.unregisterListener(0xD0);
		broadcastData(data);
		data = new BleCommand(BleCommand.RequestType.SET_LOGGING_MODE).getData();
		data[1] = BleCommand.Loging_mode.DISABLE_LOGGING;
		broadcastData(data);
		try{
			LocalBroadcastManager.getInstance(this).unregisterReceiver(BLEStatusChangeReceiver);
		}catch (Exception ignore){
			Log.e(TAG, ignore.toString());
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
		getMenuInflater().inflate(R.menu.main_menu_hybrid_gps, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.dr_setting) {
			customDialogView = inflater.inflate(R.layout.hybridgps_dr_dialog,
					(ViewGroup)findViewById(R.id.hybridgps_layout));

			seekbar_stride = (SeekBar)customDialogView.findViewById(R.id.stride_seekbar);
			textview_stride = (TextView)customDialogView.findViewById(R.id.stride_textview);
			seekbar_stride.setProgress(input_stride);
			textview_stride.setText("Stride [1-200 cm] : " + String.valueOf(input_stride) + "cm");
			seekbar_stride.setOnSeekBarChangeListener(
					new SeekBar.OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							if(progress == 0) {
								progress = 1;
							}
							textview_stride.setText("Stride [1-200 cm] : " + String.valueOf(progress) + "cm");
							input_stride = progress;
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
			);

			seekbar_angle_offset = (SeekBar)customDialogView.findViewById(R.id.angle_offset_seekbar);
			textview_angle_offset = (TextView)customDialogView.findViewById(R.id.angle_offset_textview);
			seekbar_angle_offset.setProgress(input_angle_offset + 90);
			textview_angle_offset.setText("Angle offset [-90~90] : " + String.valueOf(input_angle_offset) + "°");
			seekbar_angle_offset.setOnSeekBarChangeListener(
					new SeekBar.OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							textview_angle_offset.setText("Angle offset [-90~90] : " + String.valueOf(progress - 90) + "°");
							input_angle_offset = progress - 90;
						}

						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
			);

			seekbar_dr_plot = (SeekBar)customDialogView.findViewById(R.id.dr_plot_seekbar);
			textview_dr_plot = (TextView)customDialogView.findViewById(R.id.dr_plot_textview);
			seekbar_dr_plot.setProgress(input_dr_plot);
			textview_dr_plot.setText("DR plot [0~100] : " + String.valueOf(input_dr_plot));
			seekbar_dr_plot.setOnSeekBarChangeListener(
					new SeekBar.OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							textview_dr_plot.setText("DR plot [0~100] : " + String.valueOf(progress));
							input_dr_plot = progress;
						}

						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
			);

			radio_group_hand = (RadioGroup)customDialogView.findViewById(R.id.mode2_RadioGroup);
			radio_group_hand.check(input_hand_mode);
			radio_group_hand.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					input_hand_mode = checkedId;
				}
			});

			reset_button = (Button) customDialogView.findViewById(R.id.reset_button);
			reset_button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					input_stride = HybridGPS_Command.user_default_param.DEFAULT_STRIDE;
					input_angle_offset = HybridGPS_Command.user_default_param.DEFAULT_ANGLE_OFFSET;
					input_dr_plot = HybridGPS_Command.user_default_param.DEFAULT_DR_PLOT;
					input_hand_mode = HybridGPS_Command.device_hand_mode.LEFT_HAND_MODE;
					seekbar_stride.setProgress(HybridGPS_Command.user_default_param.DEFAULT_STRIDE);
					seekbar_dr_plot.setProgress(HybridGPS_Command.user_default_param.DEFAULT_DR_PLOT);
					seekbar_angle_offset.setProgress(HybridGPS_Command.user_default_param.DEFAULT_ANGLE_OFFSET + 90);
					radio_group_hand.check(HybridGPS_Command.device_hand_mode.LEFT_HAND_MODE);
				}
			});

			builder.setView(customDialogView)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

							// *** set stride mode 0x00 ***
							int stride[] = new int[1];
							stride[0] = input_stride;
							byte[] data = BleCommand.Set_SensorControl_Param(0x00, 0x00, BleCommand.SensorID.SENSOR_ID_DR, 0x01, stride);
							broadcastData(data);


							// *** set hand mode 0x01 ***
							int hand_mode[] = new int[1];
							if (input_hand_mode == HybridGPS_Command.device_hand_mode.LEFT_HAND_MODE) {
								hand_mode[0] = HybridGPS_Command.device_hand_mode.COMMAND_LEFT_HAND_MODE;
							} else {
								hand_mode[0] = HybridGPS_Command.device_hand_mode.COMMAND_RIGHT_HAND_MODE;
							}
							data = BleCommand.Set_SensorControl_Param(0x01, 0x00, BleCommand.SensorID.SENSOR_ID_DR, 0x01, hand_mode);
							broadcastData(data);


							// *** set direction offset 0x02 ***
							int direction_offset[] = new int[1];
							direction_offset[0] = input_angle_offset;
							data = BleCommand.Set_SensorControl_Param(0x02, 0x00, BleCommand.SensorID.SENSOR_ID_DR, 0x01, direction_offset);
							broadcastData(data);

							// *** set prot 0x03 ***
							int plot[] = new int[1];
							plot[0] = input_dr_plot;
							data = BleCommand.Set_SensorControl_Param(0x03, 0x00, BleCommand.SensorID.SENSOR_ID_DR, 0x01, plot);
							broadcastData(data);
						}
					});

			builder.show();
			return true;
		}
		return false;
	}

    private TextView make_TextView(String sMessage){
		//Make textview
		TextView tv = new TextView(getApplicationContext());
		//set massage
		tv.setText(sMessage);
		return tv;
	}

	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		MenuItem menu0 = menu.findItem(R.id.gps_setting);
		MenuItem menu1 = menu.findItem(R.id.dr_setting);
		menu0.setVisible(false);
		menu1.setVisible(true);
		return true;
	}

	/**
	 *	@brief Data packet class for DR custom view class.
	 */
	public class DR_packet{

		public byte direction;		// Direction of command between frizz and nordic
									//  0x80: sensor data(frizz -> nordic)
		public byte sensorID;
		public byte length;
		public long timeStamp;
		public boolean dataStatus;
		public long num_of_steps;
		public float relativeX;
		public float relativeY;
		public float oddMeter;
		public float angle;

		/**
		 * @brief Constructor
		 * @param packet [in] DR pakcket recieved from Chignon via BLE
		 */
		public DR_packet(DataPacket packet){

			byte b1, b2, b3, b4;

			direction = packet.data.get(1);
			sensorID = packet.data.get(2);
			length = packet.data.get(3);
			// direction dec:-128 = hex:0xFFFFFFFFFFFFFF80
			// direction 0x80 means "frizz -> nordic"
			if(direction != -128){
				dataStatus = false;
			}else{
				// sensorID dec:-104 = hex:0xFFFFFFFFFFFFFFD0
				// 0xFFFFFFFFFFFFFF98 means SENSOR_ID_DR(D0)
				if(sensorID == -48){

					b1 = packet.data.get(4);
					b2 = packet.data.get(5);
					b3 = packet.data.get(6);
					b4 = packet.data.get(7);
					timeStamp = combineByte_long(b1, b2, b3, b4);

					b1 = packet.data.get(8);
					b2 = packet.data.get(9);
					b3 = packet.data.get(10);
					b4 = packet.data.get(11);
					num_of_steps = combineByte_long(b1, b2, b3, b4);

					b1 = packet.data.get(12);
					b2 = packet.data.get(13);
					b3 = packet.data.get(14);
					b4 = packet.data.get(15);
					relativeX = combineByte_float(b1, b2, b3, b4);
					b1 = packet.data.get(16);
					b2 = packet.data.get(17);
					b3 = packet.data.get(18);
					b4 = packet.data.get(19);
					relativeY = combineByte_float(b1, b2, b3, b4);
					b1 = packet.data.get(20);
					b2 = packet.data.get(21);
					b3 = packet.data.get(22);
					b4 = packet.data.get(23);
					oddMeter = combineByte_float(b1, b2, b3, b4);

					b1 = packet.data.get(24);
					b2 = packet.data.get(25);
					b3 = packet.data.get(26);
					b4 = packet.data.get(27);
					angle = combineByte_float(b1, b2, b3, b4);

					dataStatus = true;
				}else{

					dataStatus = false;
				}
			}
		}

		/**
		 *
		 * @brief Convert sensor parameter to String
		 * @return String
		 */
		public String getParamAsString(){
			String str = String.valueOf(timeStamp)
					+ "," + String.valueOf(num_of_steps)
					+ "," + String.valueOf(relativeX)
					+ "," + String.valueOf(relativeY)
					+ "," + String.valueOf(oddMeter)
					+ "," + String.valueOf(angle);
			str += "\n";
			return str;
		}

		/**@brief Combine 4byte data as float data.
		 *
		 * @param[in] b1 combine target data(byte)
		 * @param[in] b2 combine target data(byte)
		 * @param[in] b3 combine target data(byte)
		 * @param[in] b4 combine target data(byte)
	 	*/
		private float combineByte_float(byte b1, byte b2, byte b3, byte b4){

			byte[] bArray = new byte[4];
			bArray[0] = b1;
			bArray[1] = b2;
			bArray[2] = b3;
			bArray[3] = b4;
			ByteBuffer buffer = ByteBuffer.wrap(bArray);

			return buffer.getFloat();
		}


		/**@brief Combine 4byte data as long data.
		 *
		 * @param[in] b1 combine target data(byte)
		 * @param[in] b2 combine target data(byte)
		 * @param[in] b3 combine target data(byte)
		 * @param[in] b4 combine target data(byte)
	 	*/
		private long combineByte_long(byte b1, byte b2, byte b3, byte b4){
			int t1 = ((int)b1<<24)    & 0xFF000000;
			int t2 = ((int)b2<<16)    & 0x00FF0000;
			int t3 = ((int)b3<<8)     & 0x0000FF00;
			int t4 = ((int)b4)        & 0x000000FF;
			long temp = (t1|t2|t3|t4) & (0xFFFFFFFFL);
			return temp;
		}

	}
}
