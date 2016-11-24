/*******************************************************************
 * @file	HybridGPS.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2016/05/18
 * @author	MegaChips
 * @date	2014/02/26
*******************************************************************/

package co.megachips.hybridgpsmonitor.sub_activity;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.FragmentActivity;
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
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import co.megachips.hybridgpsmonitor.BroadcastCommand;
import co.megachips.hybridgpsmonitor.BroadcastData;
import co.megachips.hybridgpsmonitor.DataPacket;
import co.megachips.hybridgpsmonitor.R;
import co.megachips.hybridgpsmonitor.OpenStreetMapsEvent;
import co.megachips.hybridgpsmonitor.ble_service.BleCommand;
import co.megachips.hybridgpsmonitor.ble_service.BleService;
import co.megachips.hybridgpsmonitor.hybridgps.GPS_converter;
import co.megachips.hybridgpsmonitor.hybridgps.HybridGPS_Command;
import co.megachips.hybridgpsmonitor.hybridgps.HybridGPS_Command.device_hand_mode;
import co.megachips.hybridgpsmonitor.hybridgps.HybridGPS_Command.user_default_param;
import co.megachips.hybridgpsmonitor.sub_activity.custom_view.HybridGPS_CustomDraw;
import co.megachips.hybridgpsmonitor.OpenStreetMapsEvent.MapEventListener;

/*
 * @brief HybridGPS activity
 */
public class HybridGPS extends FragmentActivity implements MapEventListener, LocationListener {

	//---------------------------------------
	// Member Valiables
	//---------------------------------------

	//--[Button number]---------------------------------------------
	private final int MENU_BUTTON = 0;
	private final int RESET_BUTTON = 1;
	private final int CALIBRATION_BUTTON = 2;
	private final int HOST_START_STOP_BUTTON = 3;
	private final int ANDROID_START_STOP_BUTTON = 4;
	private final int LOG_BUTTON = 5;
	//--[Button status]---------------------------------------------
	private final boolean START_ENABLE = true;
	private final boolean STOP_ENABLE = false;
	//--[Debug]-----------------------------------------------------
	private final String TAG = "nRF_BLE";
	private final String TAB = null;

	private final int LOG_INTERVAL = 500;

	//Hybrid data
	private final byte B_CORRDINATION = 0x06;
	private final byte B_CORRDINATION_HYBRID = 0x02;
	private final byte B_CORRDINATION_GPS = 0x04;

	private final boolean GPS_SENSOR_OUTPUT = false; //false:not activate GPS sensro true:Activate GPS sensor

	private Writer_log_to_file writeHybridGPSLog;	// HybridGPS Log writer
	private Writer_log_to_file writeGPSLog;	// GPS Log writer
	private Writer_log_to_file writeAngleLog;
	// Not Final
	//Button----------------------------------------------------------
	private ImageButton menuButton;
	private ImageButton resetButton;
	private ImageButton calibrationButton;
	private ImageButton host_startstopButton;
	private ImageButton android_startstopButton;
	private ImageButton logButton;
	private Button reset_button;
	//SeekBar-- for user setting -------------------------------------
	private SeekBar seekbar_activity;
	private SeekBar seekbar_sleep;
	private SeekBar seekbar_activity_indoor;
	private SeekBar seekbar_sleep_indoor;
	private SeekBar seekbar_detect_timer;
	private SeekBar seekbar_gps_sate_h;
	private SeekBar seekbar_gps_sate_m;
	private SeekBar seekbar_gps_hdop_h;
	private SeekBar seekbar_gps_hdop_m;
	private SeekBar seekbar_gps_accuracy_h;
	private SeekBar seekbar_gps_accuracy_m;
	private SeekBar seekbar_stride;
	private SeekBar seekbar_angle_offset;
	private SeekBar seekbar_dr_plot;
	//TextView-- for user setting -------------------------------------
	private TextView textview_activity_time;
	private TextView textview_sleep_time;
	private TextView textview_activity_time_indoor;
	private TextView textview_sleep_time_indoor;
	private TextView textview_detect_timer;
	private TextView textview_gps_sate_h;
	private TextView textview_gps_sate_m;
	private TextView textview_gps_hdop_h;
	private TextView textview_gps_accuracy_h;
	private TextView textview_gps_accuracy_m;
	private TextView textview_gps_hdop_m;
	private TextView textview_stride;
	private TextView textview_angle_offset;
	private TextView textview_dr_plot;
	//RadioGroup-- for user setting -------------------------------------
	RadioGroup radio_group_hand;
	RadioGroup radio_group_output;
	RadioGroup radio_group_gps_type;

	//---------------------------------------
	// User setting
	//---------------------------------------
	//GPS setting-------------------------------------------------------------
	private int input_activity_time = user_default_param.DEFAULT_ACTIVITY_TIME;
	private int input_sleep_time 	= user_default_param.DEFAULT_SLEEP_TIME;
	private int input_activity_time_indoor = user_default_param.DEFAULT_ACTIVITY_TIME_INDOOR;
	private int input_sleep_time_indoor 	= user_default_param.DEFAULT_SLEEP_TIME_INDOOR;
	private int input_detect_timer 	= user_default_param.DEFAULT_DETECT_TIME;
	private int input_gps_type	 	= user_default_param.DEFAULT_TYPE_SIMPLE;
	private int input_gps_sate_h 	= user_default_param.DEFAULT_SATE_H;
	private int input_gps_sate_m 	= user_default_param.DEFAULT_SATE_M;
	private int input_gps_hdop_h 	= user_default_param.DEFAULT_HDOP_H;  // HDOP=value/10
	private int input_gps_hdop_m 	= user_default_param.DEFAULT_HDOP_M; 	// HDOP=value/10
	private int input_gps_accuracy_h	 	= user_default_param.DEFAULT_ACCURACY_H;
	private int input_gps_accuracy_m	 	= user_default_param.DEFAULT_ACCURACY_M;
	//DR setting--------------------------------------------------------------
	private int input_stride 		= user_default_param.DEFAULT_STRIDE;
	private int input_hand_mode		= user_default_param.DEFAULT_DETECT_HANDMODE;
	private int input_angle_average	= user_default_param.DEFAULT_ANGLE_AVERAGE;
	private int input_angle_offset	= user_default_param.DEFAULT_ANGLE_OFFSET;
	private int input_dr_plot	= user_default_param.DEFAULT_DR_PLOT;
	//Output setting--------------------------------------------------------------
	private int input_output_mode	= user_default_param.DEFAULT_OUTPUT;
	//AlertDialog---------------------------------------------------------
	private AlertDialog.Builder builder;
	//View----------------------------------------------------------------
	private View customDialogView;
	//LayoutInflater------------------------------------------------------
	private LayoutInflater inflater;
	//Packet--------------------------------------------------------------
	private HybridGPS_packet hybridgps_Packet;
	private Magnet_cali_raw_packet magnet_cali_raw_Packet;
	private GPS_raw_packet gps_raw_Packet;
	private LOG_packet	log_Packet;
	private GPS_converter.Android_GPS_packet android_gps_packet;
	//Other---------------------------------------------------------------
	private int pre_time_status = HybridGPS_Command.hybridgps_timer_status.NO_DATA;
	private float pre_lon = 135.50171f;
	private float	pre_lat = 34.73562f;
	private float pre_gps_lon = 0.0f;
	private float pre_gps_lat = 0.0f;
	private float initial_gps_data_lon = 0.0f;
	private float initial_gps_data_lat = 0.0f;
	private float Compensation_angle_a = 0.0f;
	private float Compensation_angle_b = 0.0f;
	private float compensation_angle = 0.0f;

	private int output_cnt = 0;
	private float gps_accuracy;
	private boolean get_initial_gps_data = false;
	private boolean angle_compensation_ready = false;

	//color
	public static final int LUCENT_RED    = 0x44FF0000;
	public static final int LUCENT_GREEN  = 0x4400FF00;
	public static final int LUCENT_BLUE   = 0x440000FF;

	//Android GPS---------------------------------------------------------
	private GpsStatus.NmeaListener GpsNmeaListener = null;
	LocationManager mLocationManager;
	private boolean f_android_gps = false;


	//-------------------------------------------------
	// Define class
	//-------------------------------------------------
	static private HybridGPS_CustomDraw hybridgps_CustomDraw;	// View class
	static private OpenStreetMapsEvent OSMEvent;	//Map event class
	static private GPS_converter gps_converter;

	//---------------------------------------
	// Other parameter
	//---------------------------------------
	private int sample_num = input_angle_average / 10;
	private boolean host_startstopStatus = START_ENABLE;
	private boolean android_startstopStatus = START_ENABLE;
	private ArrayList<Double> MagnetOrientationArray = new ArrayList<Double>(sample_num);
	private ProgressDialog dialog;

	WakeLock cpuWakeLock;

	private static int	BLE_CONNECT = 0;
	private static int	BLE_STATUS = 0;

	private Timer mTimer;

	//---------------------------------------
	// Function
	//---------------------------------------
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_HOME);
		getActionBar().setTitle(R.string.hybridgps);
		getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0090FF")));
		getActionBar().setSubtitle("");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.hybridgps);

		broadcast(BroadcastCommand.BLE_STATUS_CHECK);

		inflater = LayoutInflater.from(this);
		builder = new AlertDialog.Builder(this);

		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();

		LinearLayout linearlayout2 = (LinearLayout)findViewById(R.id.linearLayout2);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(linearlayout2.getLayoutParams().width, linearlayout2.getLayoutParams().height);
		params.setMargins(0,0,0,0);

		hybridgps_CustomDraw = new HybridGPS_CustomDraw(this);
		gps_converter  = new GPS_converter();

		linearlayout2.addView(hybridgps_CustomDraw);

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
		float scale = (float) (disp.getWidth()/6.0f)/image_w;
		params = new LinearLayout.LayoutParams((int) (disp.getWidth()/6), (int)(image_h * scale));
		params.setMargins(2, 2, 2, 2);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    cpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"gps_service");
	    cpuWakeLock.acquire();

		radio_group_hand = new RadioGroup(this);
		radio_group_gps_type = new RadioGroup(this);
		hybridgps_CustomDraw.set_gps_output(GPS_SENSOR_OUTPUT);

		android_gps_packet = new GPS_converter(). new Android_GPS_packet();

		// *** set plot 0x03 ***
		int plot[] = new int[1];
		plot[0] = input_dr_plot;
		byte [] data = BleCommand.Set_SensorControl_Param(0x03, 0x00, BleCommand.SensorID.SENSOR_ID_HYBRIDGPS, 0x01, plot);
		broadcastData(data);

		// *** set stride mode 0x00 ***
		int stride[] = new int[1];
		stride[0] = input_stride;
		data = BleCommand.Set_SensorControl_Param(0x00, 0x00, BleCommand.SensorID.SENSOR_ID_HYBRIDGPS, 0x01, stride);
		broadcastData(data);

		// *** set direction offset 0x02 ***
		int direction_offset[] = new int[1];
		direction_offset[0] = input_angle_offset;
		data = BleCommand.Set_SensorControl_Param(0x02, 0x00, BleCommand.SensorID.SENSOR_ID_HYBRIDGPS, 0x01, direction_offset);
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

		/*--------------------------*/
		/*	Calibration button		*/
		/*--------------------------*/
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

		/*------------------------------*/
		/*		(host)StartStop button		*/
		/*------------------------------*/
		host_startstopButton = new ImageButton(this);
		host_startstopButton.setBackground(null);
		host_startstopButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
		host_startstopButton.setPadding(0, 0, 0, 0);
		host_startstopButton.setImageResource(R.drawable.host_start_button_off);
		host_startstopButton.setLayoutParams(params);
		linearlayout3.addView(host_startstopButton);
		host_startstopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				button_click_event(HOST_START_STOP_BUTTON);
			}
		});
		host_startstopButton.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch (action) {
					case MotionEvent.ACTION_DOWN:
						if (host_startstopStatus == START_ENABLE) {
							host_startstopButton.setImageResource(R.drawable.host_start_button_on);
						} else {
							host_startstopButton.setImageResource(R.drawable.host_stop_button_on);
						}
						break;
					case MotionEvent.ACTION_UP:
						if (host_startstopStatus == START_ENABLE) {
							host_startstopButton.setImageResource(R.drawable.host_start_button_off);
						} else {
							host_startstopButton.setImageResource(R.drawable.host_stop_button_off);
						}
						break;
				}
				return false;
			}
		});

		/*------------------------------*/
		/*		(android)StartStop button		*/
		/*------------------------------*/
		android_startstopButton = new ImageButton(this);
		android_startstopButton.setBackground(null);
		android_startstopButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
		android_startstopButton.setPadding(0, 0, 0, 0);
		android_startstopButton.setImageResource(R.drawable.android_start_button_off);
		android_startstopButton.setLayoutParams(params);
		linearlayout3.addView(android_startstopButton);
		android_startstopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				button_click_event(ANDROID_START_STOP_BUTTON);
			}
		});
		android_startstopButton.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				switch (action) {
					case MotionEvent.ACTION_DOWN:
						if (android_startstopStatus == START_ENABLE) {
							android_startstopButton.setImageResource(R.drawable.android_start_button_on);
						} else {
							android_startstopButton.setImageResource(R.drawable.android_stop_button_on);
						}
						break;
					case MotionEvent.ACTION_UP:
						if (android_startstopStatus == START_ENABLE) {
							android_startstopButton.setImageResource(R.drawable.android_start_button_off);
						} else {
							android_startstopButton.setImageResource(R.drawable.android_stop_button_off);
						}
						break;
				}
				return false;
			}
		});

	/*----------------------*/
	/*		LOG button		*/
	/*----------------------*/

	logButton = new ImageButton(this);
	logButton.setBackground(null);
	logButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
	logButton.setPadding(0, 0, 0, 0);
	logButton.setImageResource(R.drawable.ble_button_off);
	logButton.setLayoutParams(params);
	linearlayout3.addView(logButton);
	logButton.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
			button_click_event(LOG_BUTTON);
		}
	});
	logButton.setOnTouchListener(new View.OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch(action){
				case MotionEvent.ACTION_DOWN:
					logButton.setImageResource(R.drawable.ble_button_on);
					break;
				case MotionEvent.ACTION_UP:
					logButton.setImageResource(R.drawable.ble_button_off);
					break;
			}
			return false;
		}
	});
		mTimer = new Timer(true);
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				timer_Tick();
			}
		}, 1000, 1000);

		GpsNmeaListener = new GpsStatus.NmeaListener()
		{
			@Override
			public void onNmeaReceived(long timestamp, String nmea) {
				String[] data = nmea.split(",");
				String gpa_logging_data;
				// Make Log File
				if (data[0].equals("$GPGSA")) {
					//non
				} else if (data[0].equals("$GPRMC")) {
					//non
				} else if (data[0].equals("$GPVTG")) {
					//non
				} else if (data[0].equals("$GPGGA")) {
					android_gps_packet = gps_converter.GPGGA_converter(nmea.trim());
				} else if (data[0].equals("$GPGSV")) {
					//non
				}
			}

		};

	}


	@Override
	public void onLocationChanged(Location location) {
		gps_accuracy = location.getAccuracy();
		android_gps_packet.direction_lat = (float)location.getLatitude();
		android_gps_packet.direction_lon = (float)location.getLongitude();
		String str = "AndroidGPS"
				+ "," + String.valueOf(android_gps_packet.time)
				+ "," + String.valueOf(android_gps_packet.satellites_used)
				+ "," + String.valueOf(android_gps_packet.direction_lat)
				+ "," + String.valueOf(android_gps_packet.direction_lon)
				+ "," + String.valueOf(android_gps_packet.gpgga_hdop)
				+ "," + String.valueOf(android_gps_packet.gpgga_altitude)
				+ "," + String.valueOf(gps_accuracy) + "\n";
		writeGPSLog.write_log(str);
		hybridgps_CustomDraw.update_GPS_data(android_gps_packet.satellites_used, android_gps_packet.gpgga_hdop);

		// *** set location  mode0x05 ***
		int gps_location[] = new int[6];
		gps_location[0] = 0;
		gps_location[1] = (int) android_gps_packet.satellites_used;
		gps_location[2] = Float.floatToIntBits(android_gps_packet.direction_lat);
		gps_location[3] = Float.floatToIntBits(android_gps_packet.direction_lon);
		gps_location[4] = Float.floatToIntBits(android_gps_packet.gpgga_hdop);
		gps_location[5] = Float.floatToIntBits(android_gps_packet.gpgga_altitude);
		byte[] gps_data = BleCommand.Set_SensorControl_Param(0x05, 0x00, BleCommand.SensorID.SENSOR_ID_HYBRIDGPS, 0x06, gps_location);
		broadcastData(gps_data);
	}

	public static byte[] float2Byte(float input) {
		byte[] output = new byte[4];
		ByteBuffer byte_buffer = ByteBuffer.wrap(output, 0, 4);
		byte_buffer.putFloat(input);
		return byte_buffer.array();
	}

	/**@brief Interval timer function for checking BLE connection.
	 */
	public void timer_Tick(){
		broadcast(BroadcastCommand.BLE_STATUS_CHECK);
	}

	public void start_gps()
	{
		Log.d(TAG, "**Start GPS**");
		//GPS init
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.addNmeaListener(GpsNmeaListener);
		// Set LocationListener(GPS)
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
		// Set LocationListener(Network)
		//mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
		hybridgps_CustomDraw.start_GPS();
		f_android_gps = true;
	}

	/**@brief Stop GPS
	 */
	public void stop_gps()
	{
		Log.d(TAG, "**Stop GPS**");
		if(mLocationManager != null)
		{
			mLocationManager.removeUpdates(this);
		}
		hybridgps_CustomDraw.end_GPS();
		f_android_gps = false;
	}

	/**@brief Button click event handler.
	 * @param[in] clicked_button clicked_button ID of clicked button number.
	 */
	private void button_click_event(int clicked_button){

		//----------------------------------//
		//		MENU button pressed			//
		//----------------------------------//
		if( clicked_button == MENU_BUTTON ){
			//Deactivate sensor
			byte[] data = BleCommand.unregisterListener(BleCommand.SensorID.SENSOR_ID_HYBRIDGPS);
			broadcastData(data);
			data = BleCommand.unregisterListener(BleCommand.SensorID.SENSOR_ID_HYBRID_WRAPPER);
			broadcastData(data);
			if(GPS_SENSOR_OUTPUT == true) {
				data = BleCommand.unregisterListener(BleCommand.SensorID.SENSOR_ID_GPS_RAW);
				broadcastData(data);
			}
			data = new BleCommand(BleCommand.RequestType.SPI_FLASH_GPS_SET_SAMPLING_STOP).getData();
			data[1] = (byte) BleCommand.SensorID.SENSOR_ID_MAGNET_UNCALIB;
			broadcastDataSPIflash(data);
			// Stop Logging data to file
			if(writeHybridGPSLog != null) {
				// Stop Logging data to file
				writeHybridGPSLog.stop_logging();
			}
			if(writeGPSLog != null) {
				// Stop Logging data to file
				writeGPSLog.stop_logging();
			}
			OSMEvent.ClearLocationArray();
			this.finish();

		//----------------------------------//
		//		RESET button pressed		//
		//----------------------------------//
		}else if( clicked_button == RESET_BUTTON ){
			OSMEvent.resetOffset();
			hybridgps_CustomDraw.resetOffset();
			hybridgps_CustomDraw.end_GPS();
			OSMEvent.ClearLocationArray();
			stop_gps();
			get_initial_gps_data = false;
			angle_compensation_ready = false;
			//Deactivate sensor
			byte[] data = BleCommand.unregisterListener(BleCommand.SensorID.SENSOR_ID_HYBRIDGPS);
			broadcastData(data);
			data = BleCommand.unregisterListener(BleCommand.SensorID.SENSOR_ID_HYBRID_WRAPPER);
			broadcastData(data);
			data = BleCommand.unregisterListener(BleCommand.SensorID.SENSOR_ID_MAGNET_CALIB_RAW);
			broadcastData(data);
			if(GPS_SENSOR_OUTPUT == true) {
				data = BleCommand.unregisterListener(BleCommand.SensorID.SENSOR_ID_GPS_RAW);
				broadcastData(data);
			}
			data = new BleCommand(BleCommand.RequestType.SPI_FLASH_GPS_SET_SAMPLING_STOP).getData();
			data[1] = (byte) BleCommand.SensorID.SENSOR_ID_MAGNET_UNCALIB;
			broadcastDataSPIflash(data);
		//----------------------------------//
		//		CALIBRATION button pressed		//
		//----------------------------------//
		}else if( clicked_button == CALIBRATION_BUTTON ){
			//Show dialog
			dialog = new ProgressDialog(this);
			dialog.setIndeterminate(true);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setMessage(getResources().getString(R.string.magnet_calibration));
			dialog.show();
			//Activate Magnet calib raw sensor
			byte[] data = BleCommand.registerListener(BleCommand.SensorID.SENSOR_ID_MAGNET_CALIB_RAW);
			broadcastData(data);

		//--------------------------------------//
		//		host START STOP button pressed		//
		//--------------------------------------//
		}else if( clicked_button == HOST_START_STOP_BUTTON ){

			// START_ENABLE: START button is shown.
			// STOP_ENABLE : STOP  button is shown.
			gps_accuracy = 0;
			if(host_startstopStatus == START_ENABLE){
				byte[] data;
				OSMEvent.resetOffset();
				hybridgps_CustomDraw.resetOffset();
				OSMEvent.ClearLocationArray();
				if(GPS_SENSOR_OUTPUT == true) {
					//Activate GPS sensor
					data = BleCommand.registerListener(BleCommand.SensorID.SENSOR_ID_GPS_RAW);
					broadcastData(data);
				}

				data = new BleCommand(BleCommand.RequestType.SPI_FLASH_GPS_SET_SAMPLING_START).getData();
				data[1] = (byte) ((LOG_INTERVAL & 0xFF000000)>>24);
				data[2] = (byte) ((LOG_INTERVAL & 0x00FF0000)>>16);
				data[3] = (byte) ((LOG_INTERVAL & 0x0000FF00)>>8);
				data[4] = (byte) ((LOG_INTERVAL & 0x000000FF));
				broadcastDataSPIflash(data);

				//Show dialog
				dialog = new ProgressDialog(this);
				dialog.setIndeterminate(true);
				dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				dialog.setMessage(getResources().getString(R.string.get_gps_data));
				dialog.show();
				host_startstopStatus = STOP_ENABLE;
				host_startstopButton.setImageResource(R.drawable.host_stop_button_off);

				//Activate HybridGPS sensor
				data = BleCommand.registerListener(BleCommand.SensorID.SENSOR_ID_HYBRID_WRAPPER);
				broadcastData(data);

				// Start Logging data to file
				writeGPSLog =  new Writer_log_to_file();	// GPS Log writer
				writeGPSLog.start_logging(BleCommand.SensorID.SENSOR_ID_GPS_RAW);
				writeHybridGPSLog =  new Writer_log_to_file();	// HybridGPS Log writer
				writeHybridGPSLog.start_logging(BleCommand.SensorID.SENSOR_ID_HYBRID_WRAPPER);
				pre_time_status = HybridGPS_Command.hybridgps_timer_status.NO_DATA;
				android_gps_packet = gps_converter.GPGGA_init();

				hybridgps_CustomDraw.set_gps_output(false);
			}else{
				hybridgps_CustomDraw.end_GPS();
				//Deactivate sensor
				byte[] data = BleCommand.unregisterListener(BleCommand.SensorID.SENSOR_ID_HYBRID_WRAPPER);
				broadcastData(data);
				data = BleCommand.unregisterListener(BleCommand.SensorID.SENSOR_ID_GPS_RAW);
				broadcastData(data);
				host_startstopStatus = START_ENABLE;
				host_startstopButton.setImageResource(R.drawable.host_start_button_off);

				data = new BleCommand(BleCommand.RequestType.SPI_FLASH_GPS_SET_SAMPLING_STOP).getData();
				data[1] = (byte) BleCommand.SensorID.SENSOR_ID_MAGNET_UNCALIB;
				broadcastDataSPIflash(data);
				// Stop Logging data to file
				if(writeHybridGPSLog != null) {
					// Stop Logging data to file
					writeHybridGPSLog.stop_logging();
				}
				if(writeGPSLog != null) {
					// Stop Logging data to file
					writeGPSLog.stop_logging();
				}
			}

			//--------------------------------------//
			//		android START STOP button pressed		//
			//--------------------------------------//
		}else if( clicked_button == ANDROID_START_STOP_BUTTON ){

			// START_ENABLE: START button is shown.
			// STOP_ENABLE : STOP  button is shown.
			if(android_startstopStatus == START_ENABLE){
				OSMEvent.resetOffset();
				hybridgps_CustomDraw.resetOffset();
				OSMEvent.ClearLocationArray();
				get_initial_gps_data = false;
				angle_compensation_ready = false;
				//Activate HybridGPS sensor
				byte[] data = new BleCommand(BleCommand.RequestType.SPI_FLASH_GPS_SET_SAMPLING_START).getData();
				data[1] = (byte) ((LOG_INTERVAL & 0xFF000000)>>24);
				data[2] = (byte) ((LOG_INTERVAL & 0x00FF0000)>>16);
				data[3] = (byte) ((LOG_INTERVAL & 0x0000FF00)>>8);
				data[4] = (byte) ((LOG_INTERVAL & 0x000000FF));
				broadcastDataSPIflash(data);

				//Show dialog
				dialog = new ProgressDialog(this);
				dialog.setIndeterminate(true);
				dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				dialog.setMessage(getResources().getString(R.string.get_gps_data));
				dialog.show();
				android_startstopStatus = STOP_ENABLE;
				android_startstopButton.setImageResource(R.drawable.android_stop_button_off);
				f_android_gps = false;

				data = BleCommand.registerListener(BleCommand.SensorID.SENSOR_ID_HYBRIDGPS);
				broadcastData(data);

				// Start Logging data to file
				writeGPSLog =  new Writer_log_to_file();	// GPS Log writer
				writeGPSLog.start_logging(BleCommand.SensorID.SENSOR_ID_GPS_RAW);
				writeHybridGPSLog =  new Writer_log_to_file();	// HybridGPS Log writer
				writeHybridGPSLog.start_logging(BleCommand.SensorID.SENSOR_ID_HYBRID_WRAPPER);
				pre_time_status = HybridGPS_Command.hybridgps_timer_status.NO_DATA;


				hybridgps_CustomDraw.set_gps_output(true);
			}else{
				hybridgps_CustomDraw.end_GPS();
				//Deactivate sensor
				byte[] data = BleCommand.unregisterListener(BleCommand.SensorID.SENSOR_ID_HYBRIDGPS);
				broadcastData(data);
				data = BleCommand.unregisterListener(BleCommand.SensorID.SENSOR_ID_GPS_RAW);
				broadcastData(data);
				android_startstopStatus = START_ENABLE;
				android_startstopButton.setImageResource(R.drawable.android_start_button_off);

				data = new BleCommand(BleCommand.RequestType.SPI_FLASH_GPS_SET_SAMPLING_STOP).getData();
				data[1] = (byte) BleCommand.SensorID.SENSOR_ID_MAGNET_UNCALIB;
				broadcastDataSPIflash(data);

				stop_gps();
				// Stop Logging data to file
				if(writeHybridGPSLog != null) {
					// Stop Logging data to file
					writeHybridGPSLog.stop_logging();
				}
				if(writeGPSLog != null) {
					// Stop Logging data to file
					writeGPSLog.stop_logging();
				}
			}
		}else if( clicked_button == LOG_BUTTON ){
			int dummy[] = new int[1];
			//for ( int i = 0; i<10; i++) 
			{
				byte[] data = BleCommand.Set_SensorControl_Param(0x08, 0x00, BleCommand.SensorID.SENSOR_ID_HYBRIDGPS, 0x00, dummy);
				broadcastData(data);
				//try {Thread.sleep(100);} catch (InterruptedException e) {}
				//OSMEvent.draw_point(pre_lat, pre_lon, R.drawable.blue_point);
				//pre_lat = pre_lat +0.0002f;
				//pre_lon = pre_lon +0.0002f;
			}
			//
			//OSMEvent.MapPosition_center(pre_lat, pre_lon);
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

					if(bData.commandID == BroadcastCommand.BLE_SEND_DATA){
						DataPacket dataPacket = (DataPacket)bData.data;
						// If sensorID is SENSOR_ID_PDR(0x98), it use PDR class.
						// If sensorID is SENSOR_ID_STAIR_DETECTOR, it use stair_detector_packet class.
						hybridgps_Packet = new HybridGPS.HybridGPS_packet(dataPacket);
						magnet_cali_raw_Packet = new HybridGPS.Magnet_cali_raw_packet(dataPacket);
						gps_raw_Packet = new HybridGPS.GPS_raw_packet(dataPacket);
						log_Packet = new LOG_packet(dataPacket);


						if(hybridgps_Packet.dataStatus == true){
							// Write HybridGPS packet data to file
							//writeHybridGPSLog.write_log(hybridgps_Packet.getHybridGPSParamAsString());

							if((hybridgps_Packet.timer_status != HybridGPS_Command.hybridgps_timer_status.NO_DATA)&&
									(hybridgps_Packet.timer_status != HybridGPS_Command.hybridgps_timer_status.GPS_ACTIVE_TIME_INIT)) {
								dialog.dismiss();
							}

							int coordination_i = B_CORRDINATION & hybridgps_Packet.data_kind;
							hybridgps_CustomDraw.addPacket(hybridgps_Packet);

							//Android GPS ON/OFF
							if((host_startstopStatus == START_ENABLE) && (android_startstopStatus == STOP_ENABLE) &&
									((hybridgps_Packet.timer_status == HybridGPS_Command.hybridgps_timer_status.GPS_ACTIVE_TIME_INIT)||
											(hybridgps_Packet.timer_status == HybridGPS_Command.hybridgps_timer_status.GPS_ACTIVE_TIME)||
											(hybridgps_Packet.timer_status == HybridGPS_Command.hybridgps_timer_status.GPS_ACTIVE_INDOOR_TIME))) {
								if(	f_android_gps == false) {
									start_gps();
								}
							} else {
								if(	f_android_gps == true) {
									stop_gps();
								}
							}


							if (coordination_i != 0) {
								writeHybridGPSLog.write_log(hybridgps_Packet.getHybridGPSParamAsString());
								if (coordination_i == B_CORRDINATION_HYBRID) {
									if(input_output_mode == HybridGPS_Command.output_mode.OUTPUT_DEMO)  {
										OSMEvent.ClearLocationArray();
										OSMEvent.resetOffset();
									}
									if((hybridgps_Packet.timer_status == HybridGPS_Command.hybridgps_timer_status.GPS_ACTIVE_INDOOR_TIME) ||
											(hybridgps_Packet.timer_status == HybridGPS_Command.hybridgps_timer_status.GPS_SLEEP_INDOOR_TIME)) {
										//OSMEvent.draw_circle(pre_lat, pre_lon, hybridgps_Packet.error_radius, LUCENT_GREEN);
										//OSMEvent.draw_point(pre_lat, pre_lon, R.drawable.green_point);
									} else {
										if(angle_compensation_ready == true){
											if(compensation_angle<0){
												pre_lon = (float)((Math.cos(-compensation_angle)*(hybridgps_Packet.coordination_longitude-pre_gps_lon)) - Math.sin(-compensation_angle)*(hybridgps_Packet.coordination_latitude-pre_gps_lat) + pre_gps_lon);
												pre_lat = (float)((Math.sin(-compensation_angle)*(hybridgps_Packet.coordination_longitude-pre_gps_lon)) + Math.cos(-compensation_angle)*(hybridgps_Packet.coordination_latitude-pre_gps_lat) + pre_gps_lat);
											}else{
												pre_lon = (float)((Math.cos(compensation_angle)*(hybridgps_Packet.coordination_longitude-pre_gps_lon)) + Math.sin(compensation_angle)*(hybridgps_Packet.coordination_latitude-pre_gps_lat) + pre_gps_lon);
												pre_lat = (float)(-(Math.sin(compensation_angle)*(hybridgps_Packet.coordination_longitude-pre_gps_lon)) + Math.cos(compensation_angle)*(hybridgps_Packet.coordination_latitude-pre_gps_lat) + pre_gps_lat);
											}
											OSMEvent.draw_point(pre_lat, pre_lon, R.drawable.purple_point);
											OSMEvent.draw_point(hybridgps_Packet.coordination_latitude, hybridgps_Packet.coordination_longitude, R.drawable.green_point);
										}else{
											//OSMEvent.draw_circle(hybridgps_Packet.coordination_latitude, hybridgps_Packet.coordination_longitude, hybridgps_Packet.error_radius, LUCENT_RED);
											OSMEvent.draw_point(hybridgps_Packet.coordination_latitude, hybridgps_Packet.coordination_longitude, R.drawable.red_point);
											pre_lat = hybridgps_Packet.coordination_latitude;
											pre_lon = hybridgps_Packet.coordination_longitude;
										}
										//pre_lat = hybridgps_Packet.coordination_latitude;
										//pre_lon = hybridgps_Packet.coordination_longitude;

									}
								} else if(((coordination_i == B_CORRDINATION_GPS) && (hybridgps_Packet.gps_accuracy == HybridGPS_Command.gps_status.HIGH_QUARITY))||
										(coordination_i == B_CORRDINATION_GPS) && (pre_time_status == HybridGPS_Command.hybridgps_timer_status.GPS_ACTIVE_TIME_INIT) &&
												hybridgps_Packet.timer_status != HybridGPS_Command.hybridgps_timer_status.GPS_ACTIVE_TIME_INIT) {
									if(input_output_mode == HybridGPS_Command.output_mode.OUTPUT_DEMO)  {
										OSMEvent.ClearLocationArray();
										OSMEvent.resetOffset();
									}
									OSMEvent.draw_circle(hybridgps_Packet.coordination_latitude, hybridgps_Packet.coordination_longitude, hybridgps_Packet.error_radius, LUCENT_BLUE);
									OSMEvent.MapPosition_center(hybridgps_Packet.coordination_latitude, hybridgps_Packet.coordination_longitude);
									if(get_initial_gps_data == false) {
										OSMEvent.draw_point(hybridgps_Packet.coordination_latitude, hybridgps_Packet.coordination_longitude, R.drawable.purple_point);
										get_initial_gps_data = true;
										initial_gps_data_lon = hybridgps_Packet.coordination_longitude;
										initial_gps_data_lat = hybridgps_Packet.coordination_latitude;
									}else {
										pre_gps_lat = hybridgps_Packet.coordination_latitude;
										pre_gps_lon = hybridgps_Packet.coordination_longitude;
										if(angle_compensation_ready == false){
											angle_compensation_ready = true;
											Compensation_angle_a = (float)(Math.atan2((pre_lat-initial_gps_data_lat),(pre_lon-initial_gps_data_lon)));
											Compensation_angle_b = (float)(Math.atan2((pre_gps_lat-initial_gps_data_lat),(pre_gps_lon-initial_gps_data_lon)));
											compensation_angle = Compensation_angle_a-Compensation_angle_b;
										}
										OSMEvent.draw_point(hybridgps_Packet.coordination_latitude, hybridgps_Packet.coordination_longitude, R.drawable.blue_point);
									}
									//pre_lat = hybridgps_Packet.coordination_latitude;
									//pre_lon = hybridgps_Packet.coordination_longitude;
								} else {
									//non
								}
							}
							pre_time_status = hybridgps_Packet.timer_status;
						}

						if(magnet_cali_raw_Packet.dataStatus == true){
							hybridgps_CustomDraw.add_Magnet_calib_raw_Packet(magnet_cali_raw_Packet);
							if(magnet_cali_raw_Packet.quality == HybridGPS_Command.magnet_cali_status.HIGH_QUARITY) {
								dialog.dismiss();
								//Deactivate sensor
								byte[] data = BleCommand.unregisterListener(BleCommand.SensorID.SENSOR_ID_MAGNET_CALIB_RAW);
								broadcastData(data);
							}
						}
						if(gps_raw_Packet.dataStatus == true) {
							// Write GPS packet data to file
							writeGPSLog.write_log(gps_raw_Packet.getGPSParamAsString());
							hybridgps_CustomDraw.update_GPS_data(gps_raw_Packet);
						}
						if(log_Packet.dataStatus == true){

						}
					}


				}catch (Exception e){
					return;
				}
				hybridgps_CustomDraw.timer_Tick();
			}else if( action.equals( BroadcastCommand.ACTION_GATT_CONNECTED ) ){
				getActionBar().setSubtitle(R.string.ble_connected);
				if( BLE_CONNECT == 0 )
				{
					hybridgps_CustomDraw.timer_Tick();
				}
				BLE_CONNECT = 1;
			}else if( action.equals( BroadcastCommand.ACTION_GATT_DISCONNECTED ) ){
				getActionBar().setSubtitle("");
				host_startstopStatus = START_ENABLE;
				android_startstopStatus = START_ENABLE;
				host_startstopButton.setImageResource(R.drawable.host_start_button_off);
				android_startstopButton.setImageResource(R.drawable.android_start_button_off);
				if( BLE_CONNECT != 0 )
				{
					hybridgps_CustomDraw.timer_Tick();
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
						hybridgps_CustomDraw.timer_Tick();
					}
					BLE_STATUS = 1;
				}else{
					getActionBar().setSubtitle("");
					if( BLE_STATUS != 0 )
					{
						hybridgps_CustomDraw.timer_Tick();
					}
					BLE_STATUS = 0;
				}
			}
		}
	};



	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

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
		stop_gps();
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
		getMenuInflater().inflate(R.menu.main_menu_hybrid_gps, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		//set pdr mode
		if(item.getItemId() == R.id.gps_setting)
		{

			customDialogView = inflater.inflate(R.layout.hybridgps_gps_dialog,
				        (ViewGroup)findViewById(R.id.hybridgps_layout));

			seekbar_activity = (SeekBar)customDialogView.findViewById(R.id.activity_time_seekbar);
			textview_activity_time = (TextView)customDialogView.findViewById(R.id.activity_time_textview);
			seekbar_activity.setProgress(input_activity_time);
			textview_activity_time.setText("GPS activity time [1-600 sec] : " + String.valueOf(input_activity_time) + "sec");
			seekbar_activity.setOnSeekBarChangeListener(
					new OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							if(progress == 0) {
								progress = 1;
							}
							textview_activity_time.setText("GPS activity time [1-600 sec] : " + String.valueOf(progress) + "sec");
							input_activity_time = progress;
						}
						public void onStartTrackingTouch(SeekBar seekBar) {
						}
						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
				);

			seekbar_sleep = (SeekBar)customDialogView.findViewById(R.id.sleep_time_seekbar);
			textview_sleep_time = (TextView)customDialogView.findViewById(R.id.sleep_time_textview);
			seekbar_sleep.setProgress(input_sleep_time);
			textview_sleep_time.setText("GPS sleep time [0-600 sec] : " + String.valueOf(input_sleep_time) + "sec");
			seekbar_sleep.setOnSeekBarChangeListener(
					new OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							input_sleep_time = progress;
							textview_sleep_time.setText("GPS sleep time [0-600 sec] : " + String.valueOf(progress) + "sec");
						}
						public void onStartTrackingTouch(SeekBar seekBar) {
						}
						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
				);

			seekbar_activity_indoor = (SeekBar)customDialogView.findViewById(R.id.activity_time_indoor_seekbar);
			textview_activity_time_indoor = (TextView)customDialogView.findViewById(R.id.activity_time_indoor_textview);
			seekbar_activity_indoor.setProgress(input_activity_time_indoor);
			textview_activity_time_indoor.setText("GPS activity time (indoor) [1-600 sec] : " + String.valueOf(input_activity_time_indoor) + "sec");
			seekbar_activity_indoor.setOnSeekBarChangeListener(
					new OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							if(progress == 0) {
								progress = 1;
							}
							textview_activity_time_indoor.setText("GPS activity time (indoor) [1-600 sec] : " + String.valueOf(progress) + "sec");
							input_activity_time_indoor = progress;
						}

						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
			);

			seekbar_sleep_indoor = (SeekBar)customDialogView.findViewById(R.id.sleep_time_indoor_seekbar);
			textview_sleep_time_indoor = (TextView)customDialogView.findViewById(R.id.sleep_time_indoor_textview);
			seekbar_sleep_indoor.setProgress(input_sleep_time_indoor);
			textview_sleep_time_indoor.setText("GPS sleep time (indoor) [0-600 sec] : " + String.valueOf(input_sleep_time_indoor) + "sec");
			seekbar_sleep_indoor.setOnSeekBarChangeListener(
					new OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							input_sleep_time_indoor = progress;
							textview_sleep_time_indoor.setText("GPS sleep time (indoor) [0-600 sec] : " + String.valueOf(progress) + "sec");
						}

						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
			);

			seekbar_detect_timer = (SeekBar)customDialogView.findViewById(R.id.detect_timer_seekbar);
			textview_detect_timer = (TextView)customDialogView.findViewById(R.id.detect_timer_textview);
			seekbar_detect_timer.setProgress(input_detect_timer);
			textview_detect_timer.setText("Detect time (indoor) [1-600 sec] : " + String.valueOf(input_detect_timer) + "sec");
			seekbar_detect_timer.setOnSeekBarChangeListener(
					new OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							if(progress == 0) {
								progress = 1;
							}
							input_detect_timer = progress;
							textview_detect_timer.setText("Detect time (indoor) [1-600 sec] : " + String.valueOf(progress) + "sec");
						}

						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
			);

			seekbar_gps_sate_h = (SeekBar)customDialogView.findViewById(R.id.gps_sate_h_seekbar);
			textview_gps_sate_h = (TextView)customDialogView.findViewById(R.id.gps_sate_h_textview);
			seekbar_gps_sate_h.setProgress(input_gps_sate_h);
			textview_gps_sate_h.setText("\nSatellite number (High) [(low)2-24(high)] : " + String.valueOf(input_gps_sate_h));
			seekbar_gps_sate_h.setOnSeekBarChangeListener(
					new OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							if((progress == 0)||(progress == 1)) {
								progress = 2;
								seekbar_gps_sate_h.setProgress(progress);
							}
							input_gps_sate_h = progress;
							textview_gps_sate_h.setText("\nSatellite number (High) [(low)2-24(high)] : " + String.valueOf(progress));

							if(input_gps_sate_h <= input_gps_sate_m){
								seekbar_gps_sate_m.setProgress(input_gps_sate_h - 1);
								input_gps_sate_m = input_gps_sate_h - 1;
								textview_gps_sate_m.setText("Satellite number (Middle) [(low)1-23(high)] : " + String.valueOf(input_gps_sate_m));
							}
						}

						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
			);

			seekbar_gps_sate_m = (SeekBar)customDialogView.findViewById(R.id.gps_sate_m_seekbar);
			textview_gps_sate_m = (TextView)customDialogView.findViewById(R.id.gps_sate_m_textview);
			seekbar_gps_sate_m.setProgress(input_gps_sate_m);
			textview_gps_sate_m.setText("Satellite number (Middle) [(low)1-23(high)] : " + String.valueOf(input_gps_sate_m));
			seekbar_gps_sate_m.setOnSeekBarChangeListener(
					new OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							if(progress == 0) {
								progress = 1;
							}
							if(input_gps_sate_h <= progress){
								seekbar_gps_sate_m.setProgress(input_gps_sate_h - 1);
								progress = input_gps_sate_h - 1;
							}
							input_gps_sate_m = progress;
							textview_gps_sate_m.setText("Satellite number (Middle) [(low)1-23(high)] : " + String.valueOf(progress));
						}

						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
			);

			seekbar_gps_hdop_h = (SeekBar)customDialogView.findViewById(R.id.gps_hdop_h_seekbar);
			textview_gps_hdop_h = (TextView)customDialogView.findViewById(R.id.gps_hdop_h_textview);
			seekbar_gps_hdop_h.setProgress(input_gps_hdop_h);
			textview_gps_hdop_h.setText("HDOP (High) [(high)0.1-7.9(low)] : " + String.valueOf((float)input_gps_hdop_h/10.0));
			seekbar_gps_hdop_h.setOnSeekBarChangeListener(
					new OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							if(progress == 0) {
								progress = 1;
							}
							if(progress == 80) {
								progress = 79;
								seekbar_gps_hdop_h.setProgress(progress);
							}
							input_gps_hdop_h = progress;
							textview_gps_hdop_h.setText("HDOP (High) [(high)0.1-7.9(low)] : " + String.valueOf((float)progress/10.0));

							if(input_gps_hdop_m <= input_gps_hdop_h){
								seekbar_gps_hdop_m.setProgress(input_gps_hdop_h + 1);
								input_gps_hdop_m = input_gps_hdop_h + 1;
								textview_gps_hdop_m.setText("HDOP (Middle) [(high)0.2-8.0(low)] : " + String.valueOf((float)input_gps_hdop_m/10.0));
							}
						}

						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
			);

			seekbar_gps_hdop_m = (SeekBar)customDialogView.findViewById(R.id.gps_hdop_m_seekbar);
			textview_gps_hdop_m = (TextView)customDialogView.findViewById(R.id.gps_hdop_m_textview);
			seekbar_gps_hdop_m.setProgress(input_gps_hdop_m);
			textview_gps_hdop_m.setText("HDOP (Middle) [(high)0.2-8.0(low)] : " + String.valueOf((float)input_gps_hdop_m/10.0));
			seekbar_gps_hdop_m.setOnSeekBarChangeListener(
					new OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							if(progress == 0) {
								progress = 2;
							}
							if(input_gps_hdop_h >= progress){
								seekbar_gps_hdop_m.setProgress(input_gps_hdop_h + 1);
								progress = input_gps_hdop_h + 1;
							}
							input_gps_hdop_m = progress;
							textview_gps_hdop_m.setText("HDOP (Middle) [(high)0.2-8.0(low)] : " + String.valueOf((float)progress/10.0));
						}

						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
			);

			seekbar_gps_accuracy_h = (SeekBar)customDialogView.findViewById(R.id.gps_accuracy_h_seekbar);
			textview_gps_accuracy_h = (TextView)customDialogView.findViewById(R.id.gps_accuracy_h_textview);
			seekbar_gps_accuracy_h.setProgress(input_gps_accuracy_h - 10);
			textview_gps_accuracy_h.setText("GPS accuracy (High) [(high)10-49(low)[m]] : " + String.valueOf(input_gps_accuracy_h));
			seekbar_gps_accuracy_h.setOnSeekBarChangeListener(
					new OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							if (progress == 40) {
								progress = 39;
							}
							if (input_gps_accuracy_m <= (progress + 10)) {
								seekbar_gps_accuracy_m.setProgress(progress + 1);
								input_gps_accuracy_m = progress + 10 + 1;
							}

							input_gps_accuracy_h = progress + 10;
							textview_gps_accuracy_h.setText("GPS accuracy (High) [(high)10-49(low)[m]] : " + String.valueOf(input_gps_accuracy_h));
						}

						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
			);

			seekbar_gps_accuracy_m = (SeekBar)customDialogView.findViewById(R.id.gps_accuracy_m_seekbar);
			textview_gps_accuracy_m = (TextView)customDialogView.findViewById(R.id.gps_accuracy_m_textview);
			seekbar_gps_accuracy_m.setProgress(input_gps_accuracy_m - 10);
			textview_gps_accuracy_m.setText("GPS accuracy (Middle) [(high)11-50(low)[m]] : " + String.valueOf(input_gps_accuracy_m));
			seekbar_gps_accuracy_m.setOnSeekBarChangeListener(
					new OnSeekBarChangeListener() {
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							if ((progress + 10) == 10) {
								progress = 1;
							}
							if (input_gps_accuracy_h >= (progress + 10)) {
								seekbar_gps_accuracy_m.setProgress(input_gps_accuracy_h - 10 + 1);
								progress = input_gps_accuracy_h - 10 + 1;
							}
							input_gps_accuracy_m = progress + 10;
							textview_gps_accuracy_m.setText("GPS accuracy (Middle) [(high)11-50(low)[m]] : " + String.valueOf(input_gps_accuracy_m));
						}

						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					}
			);

			radio_group_gps_type = (RadioGroup)customDialogView.findViewById(R.id.mode_RadioGroup);
			radio_group_gps_type.check(input_gps_type);
			if (input_gps_type == HybridGPS_Command.gps_type.DEFAULT_TYPE_SIMPLE) {
				seekbar_gps_sate_h.setEnabled(false);
				seekbar_gps_sate_m.setEnabled(false);
				seekbar_gps_hdop_h.setEnabled(false);
				seekbar_gps_hdop_m.setEnabled(false);
				seekbar_gps_accuracy_h.setEnabled(true);
				seekbar_gps_accuracy_m.setEnabled(true);
			} else {
				seekbar_gps_sate_h.setEnabled(true);
				seekbar_gps_sate_m.setEnabled(true);
				seekbar_gps_hdop_h.setEnabled(true);
				seekbar_gps_hdop_m.setEnabled(true);
				seekbar_gps_accuracy_h.setEnabled(false);
				seekbar_gps_accuracy_m.setEnabled(false);
			}
			radio_group_gps_type.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					input_gps_type = checkedId;

					if (input_gps_type == HybridGPS_Command.gps_type.DEFAULT_TYPE_SIMPLE) {
						seekbar_gps_sate_h.setEnabled(false);
						seekbar_gps_sate_m.setEnabled(false);
						seekbar_gps_hdop_h.setEnabled(false);
						seekbar_gps_hdop_m.setEnabled(false);
						seekbar_gps_accuracy_h.setEnabled(true);
						seekbar_gps_accuracy_m.setEnabled(true);
					} else {
						seekbar_gps_sate_h.setEnabled(true);
						seekbar_gps_sate_m.setEnabled(true);
						seekbar_gps_hdop_h.setEnabled(true);
						seekbar_gps_hdop_m.setEnabled(true);
						seekbar_gps_accuracy_h.setEnabled(false);
						seekbar_gps_accuracy_m.setEnabled(false);
					}
				}
			});

			reset_button = (Button) customDialogView.findViewById(R.id.reset_button);
			reset_button.setOnClickListener(new View.OnClickListener() {
		            @Override
		            public void onClick(View v) {
		            	input_activity_time = user_default_param.DEFAULT_ACTIVITY_TIME;
		            	input_sleep_time = user_default_param.DEFAULT_SLEEP_TIME;
						input_activity_time_indoor = user_default_param.DEFAULT_ACTIVITY_TIME_INDOOR;
						input_sleep_time_indoor = user_default_param.DEFAULT_SLEEP_TIME_INDOOR;
						input_detect_timer = user_default_param.DEFAULT_DETECT_TIME;
						input_gps_sate_h = user_default_param.DEFAULT_SATE_H;
						input_gps_sate_m = user_default_param.DEFAULT_SATE_M;
						input_gps_hdop_h = user_default_param.DEFAULT_HDOP_H;
						input_gps_hdop_m = user_default_param.DEFAULT_HDOP_M;
						input_gps_accuracy_h = user_default_param.DEFAULT_ACCURACY_H;
						input_gps_accuracy_m = user_default_param.DEFAULT_ACCURACY_M;

		    			seekbar_activity.setProgress(user_default_param.DEFAULT_ACTIVITY_TIME);
						seekbar_sleep.setProgress(user_default_param.DEFAULT_SLEEP_TIME);
						seekbar_activity_indoor.setProgress(user_default_param.DEFAULT_ACTIVITY_TIME_INDOOR);
						seekbar_sleep_indoor.setProgress(user_default_param.DEFAULT_SLEEP_TIME_INDOOR);
						seekbar_detect_timer.setProgress(user_default_param.DEFAULT_DETECT_TIME);
						seekbar_gps_sate_h.setProgress(user_default_param.DEFAULT_SATE_H);
						seekbar_gps_sate_m.setProgress(user_default_param.DEFAULT_SATE_M);
						seekbar_gps_hdop_h.setProgress(user_default_param.DEFAULT_HDOP_H);
						seekbar_gps_hdop_m.setProgress(user_default_param.DEFAULT_HDOP_M);
						seekbar_gps_accuracy_h.setProgress(user_default_param.DEFAULT_ACCURACY_H);
						seekbar_gps_accuracy_m.setProgress( user_default_param.DEFAULT_ACCURACY_M);
		            }
			});

			builder.setView(customDialogView)
			.setPositiveButton("OK", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// *** set timer mode0x06 ***
					int timer[] = new int[5];
					timer[0] = input_activity_time;
					timer[1] = input_sleep_time;
					timer[2] = input_activity_time_indoor;
					timer[3] = input_sleep_time_indoor;
					timer[4] = input_detect_timer;
					byte[] data = BleCommand.Set_SensorControl_Param(0x06, 0x00, BleCommand.SensorID.SENSOR_ID_HYBRIDGPS, 0x05, timer);
					broadcastData(data);

					// *** set NMEA th mode0x07 ***
					int nmea[] = new int[7];
					if(input_gps_type == HybridGPS_Command.gps_type.DEFAULT_TYPE_SIMPLE) {
						nmea[0] = HybridGPS_Command.gps_type.COMMAND_TYPE_SIMPLE;
					} else {
						nmea[0] = HybridGPS_Command.gps_type.COMMAND_TYPE_DETAIL;
					}
					nmea[1] = input_gps_sate_h;
					nmea[2] = input_gps_sate_m;
					nmea[3] = Float.floatToIntBits((float)(input_gps_hdop_h / 10.0));
					nmea[4] = Float.floatToIntBits((float)(input_gps_hdop_m / 10.0));
					nmea[5] = input_gps_accuracy_h;
					nmea[6] = input_gps_accuracy_m;
					byte[] mnea_data = BleCommand.Set_SensorControl_Param(0x07, 0x00, BleCommand.SensorID.SENSOR_ID_HYBRIDGPS, 0x07, nmea);
					broadcastData(mnea_data);
				}
			});

			builder.show();
			return true;

		} else if (item.getItemId() == R.id.dr_setting) {
			customDialogView = inflater.inflate(R.layout.hybridgps_dr_dialog,
			        (ViewGroup)findViewById(R.id.hybridgps_layout));

			seekbar_stride = (SeekBar)customDialogView.findViewById(R.id.stride_seekbar);
			textview_stride = (TextView)customDialogView.findViewById(R.id.stride_textview);
			seekbar_stride.setProgress(input_stride);
			textview_stride.setText("Stride [1-200 cm] : " + String.valueOf(input_stride) + "cm");
			seekbar_stride.setOnSeekBarChangeListener(
					new OnSeekBarChangeListener() {
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
					new OnSeekBarChangeListener() {
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
					new OnSeekBarChangeListener() {
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
					input_stride = user_default_param.DEFAULT_STRIDE;
					input_angle_offset = user_default_param.DEFAULT_ANGLE_OFFSET;
					input_dr_plot = user_default_param.DEFAULT_DR_PLOT;
					input_hand_mode = device_hand_mode.LEFT_HAND_MODE;
					seekbar_stride.setProgress(user_default_param.DEFAULT_STRIDE);
					seekbar_dr_plot.setProgress(user_default_param.DEFAULT_DR_PLOT);
					seekbar_angle_offset.setProgress(user_default_param.DEFAULT_ANGLE_OFFSET + 90);
					radio_group_hand.check(device_hand_mode.LEFT_HAND_MODE);
				}
			});

			builder.setView(customDialogView)
			.setPositiveButton("OK", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sample_num = input_angle_average / 10;
					MagnetOrientationArray.clear();
					MagnetOrientationArray = new ArrayList<Double>(sample_num);


					// *** set stride mode 0x00 ***
					int stride[] = new int[1];
					stride[0] = input_stride;
					byte[] data = BleCommand.Set_SensorControl_Param(0x00, 0x00, BleCommand.SensorID.SENSOR_ID_HYBRIDGPS, 0x01, stride);
					broadcastData(data);


					// *** set hand mode 0x01 ***
					int hand_mode[] = new int[1];
					if (input_hand_mode == device_hand_mode.LEFT_HAND_MODE) {
						hand_mode[0] = device_hand_mode.COMMAND_LEFT_HAND_MODE;
					} else {
						hand_mode[0] = device_hand_mode.COMMAND_RIGHT_HAND_MODE;
					}
					data = BleCommand.Set_SensorControl_Param(0x01, 0x00, BleCommand.SensorID.SENSOR_ID_HYBRIDGPS, 0x01, hand_mode);
					broadcastData(data);
					hybridgps_CustomDraw.set_hand_mode(hand_mode[0]);


					// *** set direction offset 0x02 ***
					int direction_offset[] = new int[1];
					direction_offset[0] = input_angle_offset;
					data = BleCommand.Set_SensorControl_Param(0x02, 0x00, BleCommand.SensorID.SENSOR_ID_HYBRIDGPS, 0x01, direction_offset);
					broadcastData(data);

					// *** set plot 0x03 ***
					int plot[] = new int[1];
					plot[0] = input_dr_plot;
					data = BleCommand.Set_SensorControl_Param(0x03, 0x00, BleCommand.SensorID.SENSOR_ID_HYBRIDGPS, 0x01, plot);
					broadcastData(data);

				}
			});

			builder.show();
			return true;
		} else if (item.getItemId() == R.id.output_setting) {

			customDialogView = inflater.inflate(R.layout.hybridgps_output_dialog,
					(ViewGroup)findViewById(R.id.hybridgps_layout));

			radio_group_output = (RadioGroup)customDialogView.findViewById(R.id.outputmode_RadioButton);
			radio_group_output.check(input_output_mode);
			radio_group_output.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					input_output_mode = checkedId;
				}
			});

			reset_button = (Button) customDialogView.findViewById(R.id.reset_button);
			reset_button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					input_output_mode = user_default_param.DEFAULT_OUTPUT;
					radio_group_output.check(input_output_mode);
				}
			});

			builder.setView(customDialogView)
					.setPositiveButton("OK", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});

			builder.show();
			return true;
		}
		return false;
	}


	public void deleteRoute() {
		// TODO Auto-generated method stub

	}


	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		MenuItem menu0 = menu.findItem(R.id.gps_setting);
		MenuItem menu1 = menu.findItem(R.id.dr_setting);
    	menu0.setVisible(true);
    	menu1.setVisible(true);
        return true;
	}

	public class HybridGPS_packet{

		public byte header_direction;
		public byte sensorID;
		public byte length;

		public long timeStamp;

		public byte data_kind;
		public byte location_status;
		public byte timer_status;
		public byte gps_accuracy;

		public long step_count;

		public float coordination_longitude;
		public float coordination_latitude;
		public float direction;
		public float distance_traveled;
		public float error_radius;

		public boolean dataStatus;

		public HybridGPS_packet(DataPacket packet){

			byte b1, b2, b3, b4;

			if(packet.length == 36){

				header_direction = packet.data.get(1);
				sensorID = packet.data.get(2);

				length = packet.data.get(3);

				if((sensorID == -46)||(sensorID == -47)){//HybridGPS wrapper or HybridGPS
					b1 = packet.data.get(4);
					b2 = packet.data.get(5);
					b3 = packet.data.get(6);
					b4 = packet.data.get(7);
					timeStamp = combineByte_long(b1, b2, b3, b4);

					gps_accuracy = packet.data.get(8);
					timer_status = packet.data.get(9);
					location_status = packet.data.get(10);
					data_kind = packet.data.get(11);

					b1 = packet.data.get(12);
					b2 = packet.data.get(13);
					b3 = packet.data.get(14);
					b4 = packet.data.get(15);
					step_count = combineByte_long(b1, b2, b3, b4);
					b1 = packet.data.get(16);
					b2 = packet.data.get(17);
					b3 = packet.data.get(18);
					b4 = packet.data.get(19);
					coordination_latitude = combineByte_float(b1, b2, b3, b4);
					b1 = packet.data.get(20);
					b2 = packet.data.get(21);
					b3 = packet.data.get(22);
					b4 = packet.data.get(23);
					coordination_longitude = combineByte_float(b1, b2, b3, b4);
					b1 = packet.data.get(24);
					b2 = packet.data.get(25);
					b3 = packet.data.get(26);
					b4 = packet.data.get(27);
					direction = combineByte_float(b1, b2, b3, b4);
					b1 = packet.data.get(28);
					b2 = packet.data.get(29);
					b3 = packet.data.get(30);
					b4 = packet.data.get(31);
					distance_traveled = combineByte_float(b1, b2, b3, b4);
					b1 = packet.data.get(32);
					b2 = packet.data.get(33);
					b3 = packet.data.get(34);
					b4 = packet.data.get(35);
					error_radius = combineByte_float(b1, b2, b3, b4);

					dataStatus = true;

				}else{

					dataStatus = false;

				}

			}

		}

		private float combineByte_float(byte b1, byte b2, byte b3, byte b4){

			byte[] bArray = new byte[4];
			bArray[0] = b1;
			bArray[1] = b2;
			bArray[2] = b3;
			bArray[3] = b4;
			ByteBuffer buffer = ByteBuffer.wrap(bArray);

			return buffer.getFloat();
		}
		private long combineByte_long(byte b1, byte b2, byte b3, byte b4){
			int t1 = ((int)b1<<24) & 0xFF000000;
			int t2 = ((int)b2<<16) & 0x00FF0000;
			int t3 = ((int)b3<<8)  & 0x0000FF00;
			int t4 = ((int)b4)     & 0x000000FF;
			long temp = (t1|t2|t3|t4) & (0xFFFFFFFFL);
			return temp;
		}

		/**
		 *
		 * @brief Convert sensor parameter to String
		 * @return String
		 */
		public String getHybridGPSParamAsString(){
			String str = String.valueOf(timeStamp)
					+ "," + String.valueOf(Compensation_angle_a)
					+ "," + String.valueOf(Compensation_angle_b)
					+ "," + String.valueOf(compensation_angle)
					+ "," + String.valueOf(gps_accuracy)
					+ "," + String.valueOf(timer_status)
					+ "," + String.valueOf(location_status)
					+ "," + String.valueOf(data_kind)
					+ "," + String.valueOf(step_count)
					+ "," + String.valueOf(coordination_latitude)
					+ "," + String.valueOf(coordination_longitude)
					+ "," + String.valueOf(direction)
					+ "," + String.valueOf(distance_traveled)
					+ "," + String.valueOf(error_radius);
			str += "\n";
			return str;
		}
	}

	public class Magnet_cali_raw_packet{

		public int result_size;

		public byte header_direction;
		public byte sensorID;
		public byte length;

		public long timeStamp;
		public long size;

		byte[] result = new byte[64];

		public byte bDoneInit;
		public byte quality;
		public byte bMagnvalue;
		public byte bCount;

		public boolean dataStatus;

		public Magnet_cali_raw_packet(DataPacket packet){

			byte b1, b2, b3, b4;

			header_direction = packet.data.get(1);
			sensorID = packet.data.get(2);
			length = packet.data.get(3);

			if(sensorID == -85) {

				result_size = 64;

				b1 = packet.data.get(4);
				b2 = packet.data.get(5);
				b3 = packet.data.get(6);
				b4 = packet.data.get(7);
				timeStamp = combineByte_long(b1, b2, b3, b4);

				b1 = packet.data.get(8);
				b2 = packet.data.get(9);
				b3 = packet.data.get(10);
				b4 = packet.data.get(11);
				size = combineByte_long(b1, b2, b3, b4);

				for (int i = 0; i < result_size; i++) {
					result[i] = packet.data.get(i + 12);
				}

				bCount = packet.data.get(result_size + 12);
				bMagnvalue = packet.data.get(result_size + 1 + 12);
				quality = packet.data.get(result_size + 2 + 12);
				bDoneInit = packet.data.get(result_size + 3 + 12);

				dataStatus = true;
			} else {
				dataStatus = false;
			}
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

	public class GPS_raw_packet{

		public byte header_direction;
		public byte sensorID;
		public byte length;

		public long timeStamp;
		public long size;

		public long utc;
		public long satellites_used;
		public float direction_lat;
		public float direction_lon;
		public float gpgga_hdop;
		public float gpgga_altitude;

		public boolean dataStatus;

		public GPS_raw_packet(DataPacket packet){

			byte b1, b2, b3, b4;
			header_direction = packet.data.get(1);
			sensorID = packet.data.get(2);
			length = packet.data.get(3);

			if(sensorID == -49) {
				b1 = packet.data.get(4);
				b2 = packet.data.get(5);
				b3 = packet.data.get(6);
				b4 = packet.data.get(7);
				timeStamp = combineByte_long(b1, b2, b3, b4);

				b1 = packet.data.get(8);
				b2 = packet.data.get(9);
				b3 = packet.data.get(10);
				b4 = packet.data.get(11);
				utc = combineByte_long(b1, b2, b3, b4);

				b1 = packet.data.get(12);
				b2 = packet.data.get(13);
				b3 = packet.data.get(14);
				b4 = packet.data.get(15);
				satellites_used = combineByte_long(b1, b2, b3, b4);

				b1 = packet.data.get(16);
				b2 = packet.data.get(17);
				b3 = packet.data.get(18);
				b4 = packet.data.get(19);
				direction_lat = combineByte_float(b1, b2, b3, b4);

				b1 = packet.data.get(20);
				b2 = packet.data.get(21);
				b3 = packet.data.get(22);
				b4 = packet.data.get(23);
				direction_lon = combineByte_float(b1, b2, b3, b4);

				b1 = packet.data.get(24);
				b2 = packet.data.get(25);
				b3 = packet.data.get(26);
				b4 = packet.data.get(27);
				gpgga_hdop = combineByte_float(b1, b2, b3, b4);

				b1 = packet.data.get(28);
				b2 = packet.data.get(29);
				b3 = packet.data.get(30);
				b4 = packet.data.get(31);
				gpgga_altitude = combineByte_float(b1, b2, b3, b4);
				dataStatus = true;
			} else {
				dataStatus = false;
			}
		}

		private long combineByte_long(byte b1, byte b2, byte b3, byte b4){
			int t1 = ((int)b1<<24) & 0xFF000000;
			int t2 = ((int)b2<<16) & 0x00FF0000;
			int t3 = ((int)b3<<8)  & 0x0000FF00;
			int t4 = ((int)b4)     & 0x000000FF;
			long temp = (t1|t2|t3|t4) & (0xFFFFFFFFL);
			return temp;
		}

		private float combineByte_float(byte b1, byte b2, byte b3, byte b4){

			byte[] bArray = new byte[4];
			bArray[0] = b1;
			bArray[1] = b2;
			bArray[2] = b3;
			bArray[3] = b4;
			ByteBuffer buffer = ByteBuffer.wrap(bArray);

			return buffer.getFloat();
		}


		/**
		 *
		 * @brief Convert sensor parameter to String
		 * @return String
		 */
		public String getGPSParamAsString(){
			String str = String.valueOf(timeStamp)
					+ "," + String.valueOf(utc)
					+ "," + String.valueOf(satellites_used)
					+ "," + String.valueOf(direction_lat)
					+ "," + String.valueOf(direction_lon)
					+ "," + String.valueOf(gpgga_hdop)
					+ "," + String.valueOf(gpgga_altitude);
			str += "\n";
			return str;
		}

	}

	public class LOG_packet{
		public byte header_direction;
		public byte sensorID;
		public byte length;
		public boolean dataStatus;

		public LOG_packet(DataPacket packet){
			
			if(packet.length == 24) {
				byte b1, b2, b3, b4;
				header_direction = packet.data.get(1);
				sensorID = packet.data.get(2);
				length = packet.data.get(3);
				/*
				if((sensorID == -47) && (output_cnt<10)){
					int dummy[] = new int[1];
					byte[] data = BleCommand.Set_SensorControl_Param(0x08, 0x00, BleCommand.SensorID.SENSOR_ID_HYBRIDGPS, 0x00, dummy);
					broadcastData(data);
					output_cnt ++;
					b1 = packet.data.get(16);
					b2 = packet.data.get(17);
					b3 = packet.data.get(18);
					b4 = packet.data.get(19);
					pre_lat = combineByte_float(b1, b2, b3, b4);
					b1 = packet.data.get(20);
					b2 = packet.data.get(21);
					b3 = packet.data.get(22);
					b4 = packet.data.get(23);
					pre_lon = combineByte_float(b1, b2, b3, b4);
					OSMEvent.draw_point(pre_lat, pre_lon, R.drawable.blue_point);
					dataStatus = true;
				*/
				if(sensorID == -47){
					pre_lat = 34.742146f;
					pre_lon = 135.50887f;
					OSMEvent.draw_point(pre_lat, pre_lon, R.drawable.purple_point);
					pre_lat = 34.74111f;
					pre_lon = 135.50888f;
					OSMEvent.draw_point(pre_lat, pre_lon, R.drawable.blue_point);
					pre_lat = 34.74078f;
					pre_lon = 135.5098f;
					OSMEvent.draw_point(pre_lat, pre_lon, R.drawable.red_point);
					pre_lat = 34.741291046f;
					pre_lon = 135.509445190f;
					OSMEvent.draw_point(pre_lat, pre_lon, R.drawable.green_point);
					dataStatus = true;
				}
				else{
					dataStatus = false;
				}
			}
		}
		private float combineByte_float(byte b1, byte b2, byte b3, byte b4){

			byte[] bArray = new byte[4];
			bArray[0] = b1;
			bArray[1] = b2;
			bArray[2] = b3;
			bArray[3] = b4;
			ByteBuffer buffer = ByteBuffer.wrap(bArray);

			return buffer.getFloat();
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

	/**@brief Show toast message.
	 * @param[in] msg Show message with toast.
	 */
	private void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	/**
	 * @brief Save PDR/stair_detector log to file in Strage.
	 */

	public class Writer_log_to_file {
		//---------------------------------------
		// Member Valiables(final)
		//---------------------------------------
		// Final
		private final String wrtrTAG = "LOG_WRITER";
		private final String LogFolder = "/megachips/HybridGPS/";
		// Not Final
		private String sensor_data_file_name;
		private FileOutputStream fout;

		/**
		 * @brief Constructor
		 */
		public Writer_log_to_file(){
			sensor_data_file_name = "";
			fout = null;
		}

		/**
		 * @brief Make Log folder and file. Log file name format is "YMDHMS.txt"
		 * @n     If file the same name is exist, writing log from top of file.
		 */
		public void start_logging(int sensor_id){
			// Make Log Folder(/megachips/HybridGPS/)
			File dir = new File(Environment.getExternalStorageDirectory() + LogFolder);
			if (dir.exists() == false) {
				dir.mkdirs();	// Make Log Folder Recursively
				dir.setReadable(true);
				dir.setWritable(true);
			}

			// Make Log File
			Calendar now = Calendar.getInstance();
			Date date = now.getTime();
			SimpleDateFormat sdf_date = new SimpleDateFormat("yyyyMMdd", Locale.US);
			SimpleDateFormat sdf_time = new SimpleDateFormat("HHmmss", Locale.US);

			if(sensor_id == BleCommand.SensorID.SENSOR_ID_HYBRID_WRAPPER) {
				sensor_data_file_name = dir.getPath()
						+ "/" + "hybridgps_" + sdf_date.format(date)
						+ "_" + sdf_time.format(date);
			} else if (sensor_id == BleCommand.SensorID.SENSOR_ID_GPS_RAW){
				sensor_data_file_name = dir.getPath()
						+ "/" + "gps_" + sdf_date.format(date)
						+ "_" + sdf_time.format(date);
			}

			try
			{
				// File
				fout = new FileOutputStream(sensor_data_file_name + ".csv", false);
			}
			catch(Exception e)
			{
				//Error
				fout = null;
			}
			return;
		}
		/**
		 * @brief Stop logging
		 */
		public void stop_logging(){
//			StackTraceElement throwableStackTraceElement = new Throwable().getStackTrace()[0];
//			Log.d(wrtrTAG, throwableStackTraceElement.getMethodName() + "(" + throwableStackTraceElement.getLineNumber() + ")");

			if(fout == null){
				return;
			}
			try{
				fout.close();
				fout = null;
			}
			catch(Exception e){
				// Error
			}

			try
			{
				String[] paths = new String[1];
				paths[0] = sensor_data_file_name + ".csv";
				MediaScannerConnection.scanFile(getApplicationContext(), paths, null,
						new MediaScannerConnection.OnScanCompletedListener() {
							//@Override
							public void onScanCompleted(String path,Uri uri) {
								//
							}
						});
			}
			catch(Exception e)
			{
				//error
			}
		}
		/**
		 * @brief write log_str to log file.
		 * @param log_str [in] log written
		 */
		public void write_log(String log_str){
			//StackTraceElement throwableStackTraceElement = new Throwable().getStackTrace()[0];
			//Log.d(wrtrTAG, throwableStackTraceElement.getMethodName() + "(" + throwableStackTraceElement.getLineNumber() + ")");

			if(fout == null){
				return;
			}
			try{
				fout.write(log_str.getBytes());
			}
			catch(Exception e){
				// Error
			}
			return;
		}
	}
}
