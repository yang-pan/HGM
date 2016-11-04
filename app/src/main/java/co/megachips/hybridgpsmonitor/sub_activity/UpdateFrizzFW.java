/*******************************************************************
 * @file	FallDown.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
 * @author	Takeshi Matsumoto
*******************************************************************/

package co.megachips.hybridgpsmonitor.sub_activity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import co.megachips.hybridgpsmonitor.BroadcastCommand;
import co.megachips.hybridgpsmonitor.BroadcastData;
import co.megachips.hybridgpsmonitor.DataPacket;
import co.megachips.hybridgpsmonitor.R;
import co.megachips.hybridgpsmonitor.ble_service.BleCommand;
import co.megachips.hybridgpsmonitor.ble_service.BleCommand.SensorID;
import co.megachips.hybridgpsmonitor.ble_service.BleDataHandler;
import co.megachips.hybridgpsmonitor.ble_service.BleService;
import co.megachips.hybridgpsmonitor.fileexplorer.FileSelectionActivity;

	/**@brief FallDown activity
	 */
public class UpdateFrizzFW extends Activity {

	Intent intent;
	Bundle extras;

	ImageButton menuButton;
	ImageButton startstopButton;

	Button selectfileButton;

	BleDataHandler bleDataHandler;
	UpdateFrizzFW.pedometer_packet pedometerPacket;

	ProgressBar progressBar;

	private final int MENU_BUTTON = 0;
	private final int START_STOP_BUTTON = 1;
	private final int SELECT_FILE_BUTTON = 2;

	private final int  DATA_SIZE = 256;		 //The number of data to be transferred at a time

	private TextView title_left;
	private TextView title_right;
	private Timer mTimer;

	private TextView debugOut;
	private ScrollView scrollView;

	private final boolean START_ENABLE = true;
	private final boolean STOP_ENABLE = false;

	private boolean transfer_status = false;
	private	WakeLock g_wake_lock;
	private	boolean g_wake_lock_flag = false;

	// request code
	private static final int	REQUEST_FILESELECT	= 0;

	private boolean startstopStatus = START_ENABLE;

	public static final String TAG = "nRF_BLE";
	static public final String save_directory = "megachips";

	public int step_cnt = 0;
	public int next_send_timer_cnt;
	public int retry_count;
	private final int retry_count_max = 3;
	public int seg;
	public int partition_function = 0;
	long file_size;
	public byte[] readBinary;

	Handler progressbar_handler = new Handler();

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.update_frizz_fw);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title_bar);
		title_left = (TextView)findViewById(R.id.title_left_text);
		title_left.setText(R.string.update_frizz_fw);

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
		params = new LinearLayout.LayoutParams((int)(disp.getWidth())/2, 200);
		params.setMargins(2,2,2,2);

		progressBar = (ProgressBar) findViewById(R.id.progressBar);

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

		/*------------------------------*/
		/*		Select File button		*/
		/*------------------------------*/
		selectfileButton = (Button)findViewById(R.id.selectfilebutton);
		selectfileButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
		    	button_click_event(SELECT_FILE_BUTTON);
			}
		});
		selectfileButton.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch(action){
				case MotionEvent.ACTION_DOWN:
					//non
					break;
				case MotionEvent.ACTION_UP:
		    		Intent fileIntent = new Intent( UpdateFrizzFW.this, FileSelectionActivity.class );
		    		String loggingDirectoryName ="/sdcard/Download";
		    		fileIntent.putExtra( "initialdir", loggingDirectoryName);
					fileIntent.putExtra( "ext", "bin" );
					startActivityForResult( fileIntent, REQUEST_FILESELECT );
					break;
			}
				return false;
			}
		});
		next_send_timer_cnt = -1;
		retry_count = 0;
		mTimer = new Timer(true);
		mTimer.schedule( new TimerTask(){
			@Override
			public void run() {
				timer_Tick();
			}
		}, 1000, 1000);
		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		g_wake_lock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "UpdateFrizzFW");
		g_wake_lock_flag = false;
	}

	@Override
	protected void onActivityResult(	int requestCode,
										int resultCode,
										Intent fileIntent )
	{
		if( REQUEST_FILESELECT == requestCode && RESULT_OK == resultCode )
		{
			extras = fileIntent.getExtras();
			if( null != extras )
			{
				File file = (File)extras.getSerializable( "file" );
				TextView filenameText = (TextView) findViewById(R.id.filenameText);
				filenameText.setText(file.getName());
				TextView filepathText = (TextView) findViewById(R.id.filepathText);
				filepathText.setText(file.getPath());
				file_size = file.length();
				TextView filesizeText = (TextView) findViewById(R.id.filesizeText);
				filesizeText.setText(String.format("%1$,3d", file_size) + " [Byte]\n");

				readBinary = new byte[(int)file_size];

				try {
					BufferedInputStream bis;
					FileInputStream fis;
					fis = new FileInputStream(file);
					bis = new BufferedInputStream(fis);
					try {
						bis.read(readBinary);
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

	/**@brief Interval timer function for checking BLE connection.
	 */
	public void timer_Tick(){
		if(transfer_status == false)
		{
			broadcast(BroadcastCommand.BLE_STATUS_CHECK);
		}
		if( next_send_timer_cnt >= 0 )
		{
			next_send_timer_cnt++;
			if( next_send_timer_cnt > 30 ) {
				// flash 256Kbyte erase about 10 seconds, at the margin
				next_send_timer_cnt = -1;
				seg--;
				send_frizzFW();
			}
		}
	}


	/**@brief Button click event handler.
	 * @param[in] clicked_button clicked_button ID of clicked button number.
	 */
	private void button_click_event(int clicked_button){

		//----------------------------------//
		//		MENU button pressed			//
		//----------------------------------//
		if( clicked_button == MENU_BUTTON ){
			this.finish();

		//--------------------------------------//
		//		START_STOP button pressed		//
		//--------------------------------------//
		}else if( clicked_button == START_STOP_BUTTON ){

			if(startstopStatus == START_ENABLE){
				//startstopStatus = START_ENABLE;
				if( null != extras )
				{
					partition_function = (int) (file_size / DATA_SIZE);
					if(file_size % DATA_SIZE != 0)
					{
						partition_function++;
					}

					progressBar.setProgress(0);
					progressBar.setMax(partition_function);
					transfer_status = true;
					seg = 1;
					next_send_timer_cnt = 0;
					retry_count = 0;
					if( g_wake_lock_flag == false ) {
						g_wake_lock.acquire();
						g_wake_lock_flag = true;
					}
					send_frizzFW();

					//Set text
					TextView filesizeText = (TextView) findViewById(R.id.transfer_status);
					filesizeText.setText(R.string.data_trandfer);
					//Disabled button
					startstopButton.setEnabled(false);
					startstopButton.setColorFilter(0xaa808080);
				}
				else
				{
					Toast.makeText(this, "Select file", Toast.LENGTH_LONG).show();
				}


			}else{
				//
			}
		}
		return;

	}

	private void send_frizzFW() {
		long check_sum = 0;
		long check_data;
		int length, i;
		int time_over;
		byte[] param;

		//data size
		if(file_size - (seg * DATA_SIZE) < 0)
		{
			length = (int) (file_size - ((seg - 1) * DATA_SIZE));
		}
		else
		{
			length = DATA_SIZE;
		}

		param = new byte[(int)length];
		for(i = 0; i < length; i++)
		{
			param[i] = readBinary[i + (seg - 1) * DATA_SIZE];
			check_data = (readBinary[i + (seg - 1) * DATA_SIZE] & 0x000000FF);
			check_sum += check_data;
		}
		if( next_send_timer_cnt >= 0 ) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			retry_count++;
		}
		//Make sending data
		byte[] data = BleCommand.setfrizzFW(BleCommand.RequestType.SEND_FRIZZFW, seg, partition_function, length, param, check_sum);
		broadcastData(data);
		progressBar.setProgress(seg);

		seg++;
		if( next_send_timer_cnt >= 0 ) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		next_send_timer_cnt = 0;
		if(seg > partition_function)
		{
			if( g_wake_lock_flag == true ) {
				g_wake_lock.release();
				g_wake_lock_flag = false;
			}
			next_send_timer_cnt = -1;
			Toast.makeText(this, "Success", Toast.LENGTH_LONG).show();
			transfer_status = false;
			//Set text
			TextView filesizeText = (TextView) findViewById(R.id.transfer_status);
			filesizeText.setText(R.string.success);
			//Enabled button
			startstopButton.setEnabled(true);
			startstopButton.setColorFilter(null);
		}
		else if( (retry_count >= retry_count_max) || (startstopStatus == STOP_ENABLE) ) {
			next_send_timer_cnt = -1;
			retry_count = retry_count_max;
		}
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
		try {Thread.sleep(5);} catch (InterruptedException e) {}				 //Sleep
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
				}else if(bData.commandID == BroadcastCommand.STATE_STOPPED){
					startstopStatus = START_ENABLE;
					startstopButton.setImageResource(R.drawable.start_button_off);
				}else if(bData.commandID ==  BroadcastCommand.SEND_FW_OK){
					if(transfer_status == true)
					{
						next_send_timer_cnt = 0;
						retry_count = 0;
						send_frizzFW();
					}
				}else if(bData.commandID ==  BroadcastCommand.SEND_FW_NG){
					next_send_timer_cnt = -1;
					if( g_wake_lock_flag == true ) {
						g_wake_lock.release();
						g_wake_lock_flag = false;
					}
					Toast.makeText(getApplicationContext(), "failed! Please try again", Toast.LENGTH_LONG).show();
					transfer_status = false;
					//Set text
					TextView filesizeText = (TextView) findViewById(R.id.transfer_status);
					filesizeText.setText(R.string.transfer_failure);
					//Enabled button
					startstopButton.setEnabled(true);
					startstopButton.setColorFilter(null);
				}else if(bData.commandID == BroadcastCommand.BLE_SEND_DATA){
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
			if( (retry_count >= retry_count_max) && (next_send_timer_cnt == -1) && (transfer_status == true)) {
				if( g_wake_lock_flag == true ) {
					g_wake_lock.release();
					g_wake_lock_flag = false;
				}
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);	// Display Backlight ON Event!!
				Toast.makeText(getApplicationContext(), "faild! Please try again", Toast.LENGTH_LONG).show();
				transfer_status = false;
				//Set text
				TextView filesizeText = (TextView) findViewById(R.id.transfer_status);
				filesizeText.setText(R.string.transfer_failure);
				//Enabled button
				startstopButton.setEnabled(true);
				startstopButton.setColorFilter(null);
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

		if( g_wake_lock_flag == true ) {
			g_wake_lock.release();
			g_wake_lock_flag = false;
		}

		Log.d(TAG, "onDestroy()");
		
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

	public class pedometer_packet{

		public byte direction;
		public byte sensorID;
		public byte length;
		public long timeStamp;
		public boolean dataStatus;
		public long step_count;
		public int sensor_id_int;

		public pedometer_packet(DataPacket packet){

			byte b1, b2, b3, b4;

			direction = packet.data.get(1);
			sensorID = packet.data.get(2);
			sensor_id_int = combineBytehex_int(sensorID);
			length = packet.data.get(3);

			if(sensor_id_int == SensorID.SENSOR_ID_ACCEL_PEDOMETER){

				b1 = packet.data.get(4);
				b2 = packet.data.get(5);
				b3 = packet.data.get(6);
				b4 = packet.data.get(7);
				timeStamp = combineByte_long(b1, b2, b3, b4);

				b1 = packet.data.get(8);
				b2 = packet.data.get(9);
				b3 = packet.data.get(10);
				b4 = packet.data.get(11);
				step_count = combineByte_long(b1, b2, b3, b4);

				dataStatus = true;
			}else{

				dataStatus = false;
			}
		}

		private int combineBytehex_int(byte data){
			String hex_data;
			int int_data;
			hex_data = Integer.toHexString(data);
			hex_data = hex_data.substring(6);
			int_data = Integer.parseInt(hex_data,16);
			return int_data;
		}

		private long combineByte_long(byte b1, byte b2, byte b3, byte b4){
			int t1 = ((int)b1<<24) & 0xFF000000;
			int t2 = ((int)b2<<16) & 0x00FF0000;
			int t3 = ((int)b3<<8)  & 0x0000FF00;
			int t4 = ((int)b4)     & 0x000000FF;
			long temp = (t1|t2|t3|t4) & (0xFFFFFFFFL);
			return temp;
		}
	}
}
