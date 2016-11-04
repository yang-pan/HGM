/*******************************************************************
 * @file	HybridGPS_CustomDraw.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
 *******************************************************************/

package co.megachips.hybridgpsmonitor.sub_activity.custom_view;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import co.megachips.hybridgpsmonitor.R;
import co.megachips.hybridgpsmonitor.hybridgps.HybridGPS_Command.location_status;
import co.megachips.hybridgpsmonitor.hybridgps.HybridGPS_Command.magnet_cali_status;
import co.megachips.hybridgpsmonitor.hybridgps.HybridGPS_Command.gps_status;
import co.megachips.hybridgpsmonitor.hybridgps.HybridGPS_Command.device_hand_mode;
import co.megachips.hybridgpsmonitor.hybridgps.HybridGPS_Command.hybridgps_timer_status;
import co.megachips.hybridgpsmonitor.sub_activity.HybridGPS;

/**
 * @brief CustomView for CompassStepTracker activity
 */
@SuppressLint("DrawAllocation")
public class HybridGPS_CustomDraw extends View {
	public static final String TAG = "HybridGPS_CustomDraw";
	Context main_context;
	private Timer mTimer;

	private int TIMER_STARTVAL = 100; // mTimer first start time
	private int TIMER_INTERVAL = 300; // Timer interval for draw

	private ScaleGestureDetector _gestureDetector;
	private float _scaleFactor = 1.0f;
	private float _scaleFactor_temp = 1.0f;
	private float FLOAT_SCALE_FACTOR_MAX_CLIP = 2.50f;
	private float FLOAT_SCALE_FACTOR_MIN_CLIP = 0.50f;

	private float PAI = 3.14159265358979323846f;

	boolean touch_flag = false;
	boolean pinch_flag = false;

	public int offset_x = 0;
	public int offset_y = 0;

	private int touch_base_x = 0;
	private int touch_base_y = 0;

	private long gps_sate = 0;
	private float gps_hdop = 0;

	private int hand_mode = device_hand_mode.COMMAND_LEFT_HAND_MODE;
	private double distance_data = 0;

	private float magnet_calib_packet_quarity = 0;
	private int step_num = 0;
	private int location = location_status.UNKNOWN;
	private int pre_location = location_status.UNKNOWN;
	private int timer_status = hybridgps_timer_status.NO_DATA;

	private double real_time_orientation = 0;
	private double real_time_gps_accuracy = 0;

	public Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	public Paint paint_circle = new Paint(Paint.ANTI_ALIAS_FLAG);

	public boolean GPS_data_flag = false;
	public boolean GPS_output_flag = false;

	/**
	 * @brief Constructor of this class.
	 */
	public HybridGPS_CustomDraw(Context context) {

		super(context);
		main_context = context;

		_gestureDetector = new ScaleGestureDetector(main_context, _simpleListener);

		mTimer = new Timer(true);
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				timer_Tick();
			}
		}, TIMER_STARTVAL, TIMER_INTERVAL);

	}

	/**
	 * @brief Reset internal parameter
	 */
	public void resetOffset() {
		distance_data = 0;
		_scaleFactor = 1.0f;
		offset_x = 0;
		offset_y = 0;
		real_time_gps_accuracy = 0;
		step_num = 0;
		gps_sate = 0;
		gps_hdop = 0;
		redraw("test");
	}

	public void timer_Tick() {
		redraw("test");
	}

	/**
	 * @brief set Orientation data.
	 */
	public void set_orientation(double orientation) {
		if(orientation < 0) {
			orientation += 360;
		}

		real_time_orientation = orientation;
	}

	private void redraw(String str) {
		Message valueMsg = new Message();
		valueMsg.obj = str;
		mHandler.sendMessage(valueMsg);
		return;
	}

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			invalidate();
		}
	};

	@SuppressLint("DrawAllocation")
	protected void onDraw(Canvas canvas) {
		Bitmap bitmap;
		Resources r = this.getResources();

		// ---------------------------------------
		int BUTTON_AREA_RATIO = 50;
		int BUTTON_AREA_MARGIN_VERTICAL = 2;
		int BUTTON_AREA_MARGIN_HORIZONTAL = 2;
		// ---------------------------------------

		bitmap = BitmapFactory.decodeResource(r, R.drawable.distance2);
		float image_h = bitmap.getHeight();
		float image_w = bitmap.getWidth();

		int canvas_width = canvas.getWidth();
		int canvas_height = canvas.getHeight();
		int CANVAS_WIDTH = canvas.getWidth();

		int BUTTON_AREA_HEIGHT = (int)((canvas_height * BUTTON_AREA_RATIO) / 100);
		int BUTTON_MARGIN_VERTICAL =  (int)((canvas_height * BUTTON_AREA_MARGIN_VERTICAL) / 80);
		int BUTTON_MARGIN_HORIZONTAL = (canvas_width * BUTTON_AREA_MARGIN_HORIZONTAL) / 100;

		int BUTTON_HEIGHT = (int) (canvas_height / 4);
		int BUTTON_WIDTH = (int) (BUTTON_HEIGHT * ((float)(image_w / image_h)));
		int setTextSize = BUTTON_HEIGHT / 3;

		int BUTTON1_BASE_X1 = BUTTON_MARGIN_HORIZONTAL;
		int BUTTON1_BASE_X2 = BUTTON_MARGIN_HORIZONTAL + BUTTON_WIDTH;
		int BUTTON1_BASE_Y1 = BUTTON_MARGIN_VERTICAL;
		int BUTTON1_BASE_Y2 = BUTTON_MARGIN_VERTICAL * 2 + BUTTON_HEIGHT;

		int CONTEX_BASE_X1 = BUTTON1_BASE_X1;
		int CONTEX_BASE_Y1 = BUTTON_MARGIN_VERTICAL * 6 + BUTTON_HEIGHT * 2;

		// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		bitmap = BitmapFactory.decodeResource(r, R.drawable.distance2);
		bitmap = Bitmap.createScaledBitmap(bitmap, BUTTON_WIDTH, BUTTON_HEIGHT, false);
		canvas.drawBitmap(bitmap, BUTTON1_BASE_X1, BUTTON1_BASE_Y1, null);
		// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		bitmap = BitmapFactory.decodeResource(r, R.drawable.step2);
		bitmap = Bitmap.createScaledBitmap(bitmap, BUTTON_WIDTH, BUTTON_HEIGHT, false);
		canvas.drawBitmap(bitmap, BUTTON1_BASE_X1, BUTTON1_BASE_Y2, null);
		// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

		int TEXT_BOTTOM_MARGIN = 20;
		int TEXT_RIGHT_MARGIN = 10;

		paint.setColor(Color.argb(255, 255, 255, 255));

		float textWidth;
		float unitWidth;
		String disp_string;

		paint.setTextSize(setTextSize);
		disp_string = " m";
		unitWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, BUTTON1_BASE_X2 - unitWidth - TEXT_RIGHT_MARGIN, BUTTON1_BASE_Y1 + BUTTON_HEIGHT - TEXT_BOTTOM_MARGIN, paint);

		disp_string = String.format("%.2f", distance_data);
		textWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, BUTTON1_BASE_X2 - textWidth - unitWidth - TEXT_RIGHT_MARGIN, BUTTON1_BASE_Y1 + BUTTON_HEIGHT  - TEXT_BOTTOM_MARGIN, paint);

		// --------------------------------------------------------------------------------------------

		disp_string = " step";
		unitWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, BUTTON1_BASE_X2 - unitWidth - TEXT_RIGHT_MARGIN, BUTTON1_BASE_Y2 + BUTTON_HEIGHT - TEXT_BOTTOM_MARGIN, paint);

		disp_string = String.format("%d", step_num);
		textWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, BUTTON1_BASE_X2 - textWidth - unitWidth - TEXT_RIGHT_MARGIN, BUTTON1_BASE_Y2 + BUTTON_HEIGHT - TEXT_BOTTOM_MARGIN, paint);

		// --------------------------------------------------------------------------------------------

		paint.setColor(Color.argb(255, 0, 0, 0));
		if(hand_mode == device_hand_mode.COMMAND_LEFT_HAND_MODE) {
			disp_string = String.format("Left hand mode");
		} else {
			disp_string = String.format("Right hand mode");
		}
		textWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, CONTEX_BASE_X1, CONTEX_BASE_Y1 + 5, paint);

		// --------------------------------------------------------------------------------------------

		if(magnet_calib_packet_quarity == magnet_cali_status.HIGH_QUARITY) {
			paint.setColor(Color.argb(255, 255, 0, 0));
			disp_string = String.format("Mag Calibration : High");
		} else if (magnet_calib_packet_quarity == magnet_cali_status.MIDDLE_QUARITY){
			disp_string = String.format("Mag Calibration : Middle");
		} else {
			disp_string = String.format("Mag Calibration : LOW");
		}
		textWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, CONTEX_BASE_X1, CONTEX_BASE_Y1 + paint.getTextSize() + 5, paint);

		// --------------------------------------------------------------------------------------------
		paint.setColor(Color.argb(255, 0, 0, 0));
		paint.setTextSize(setTextSize);
		if(real_time_gps_accuracy == gps_status.HIGH_QUARITY) {
			paint.setColor(Color.argb(255, 255, 0, 0));
			disp_string = String.format("GPS Accuracy : High");
		} else if (real_time_gps_accuracy == gps_status.MIDDLE_QUARITY){
			disp_string = String.format("GPS Accuracy : Middle");
		} else {
			disp_string = String.format("GPS Accuracy : LOW");
		}
		canvas.drawText(disp_string, CONTEX_BASE_X1, CONTEX_BASE_Y1 + paint.getTextSize() * 2 + 5, paint);

		if(GPS_output_flag == true) {
			if((pre_location != location_status.UNKNOWN) && (location == location_status.UNKNOWN)) {
				gps_sate = 0;
				gps_hdop = 0;
			}
			paint.setColor(Color.argb(255, 0, 0, 0));
			disp_string = String.format("Satellite : " + gps_sate + " HDOP : " + gps_hdop);
			canvas.drawText(disp_string, CONTEX_BASE_X1, CONTEX_BASE_Y1 + paint.getTextSize() * 3 + 5, paint);
			pre_location = location;
		}
		// --------------------------------------------------------------------------------------------

		//circle centor
		float x_0 = canvas_width * 3 / 4;
		float y_0 = canvas_height / 2;
		float x_1 = 0;
		float y_1 = BUTTON_WIDTH / 4 + 10;
		float x_2, y_2;
		float orientation_circle;

		paint.setColor(Color.argb(255, 0, 0, 0));
		paint.setTextSize(setTextSize);
		disp_string = String.format("Orientation : %.2f°", real_time_orientation);
		textWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, x_0 - textWidth / 2, canvas_height / 2  - BUTTON_WIDTH / 4 - paint.getTextSize(), paint);

		paint_circle.setColor(Color.argb(255, 150, 150, 0));
		paint_circle.setStyle(Paint.Style.STROKE);
		canvas.drawCircle(x_0, canvas_height / 2, BUTTON_WIDTH / 4, paint_circle);

		for(int i = 0; i < 360; i++) {
			orientation_circle = (float) ((real_time_orientation + i) * Math.PI / 180);
			x_2 = (float) ( x_1 * Math.cos(orientation_circle) - y_1 * Math.sin(orientation_circle)) + x_0;
			y_2 = (float) (-1 * ( x_1 * Math.sin(orientation_circle) + y_1 * Math.cos(orientation_circle))) + y_0;
			if(i % 10 == 0) {
				paint_circle.setColor(Color.argb(255, 0, 0, 0));
				paint_circle.setStrokeWidth(1);
				canvas.drawLine(x_0, y_0, x_2, y_2, paint_circle);
			}
			if(i % 30 == 0) {
				paint_circle.setColor(Color.argb(255, 0, 0, 0));
				paint_circle.setStrokeWidth(3);
				canvas.drawLine(x_0, y_0, x_2, y_2, paint_circle);
			}
			if(i % 90 == 0)	{
				paint_circle.setColor(Color.argb(255, 0, 0, 0));
				paint_circle.setStrokeWidth(5);
				canvas.drawLine(x_0, y_0, x_2, y_2, paint_circle);
			}
			if(i % 360 == 0)	{
				paint_circle.setColor(Color.argb(255, 255, 0, 0));
				paint_circle.setStrokeWidth(5);
				canvas.drawLine(x_0, y_0, x_2, y_2, paint_circle);
			}
		}
		paint.setColor(Color.argb(255, 255, 255, 255));
		paint.setStrokeWidth(15);
		canvas.drawCircle(x_0, canvas_height / 2, BUTTON_WIDTH / 5, paint);

		paint_circle.setColor(Color.argb(255, 200, 200, 200));
		paint_circle.setStrokeWidth(1);
		canvas.drawLine(x_0 - BUTTON_WIDTH / 4, canvas_height / 2,
				x_0 + BUTTON_WIDTH / 4, canvas_height / 2, paint_circle);
		canvas.drawLine(x_0, canvas_height / 2  - BUTTON_WIDTH / 4,
				x_0, canvas_height / 2 + BUTTON_WIDTH / 4, paint_circle);

		paint_circle.setColor(Color.argb(255, 255, 0, 0));
		paint_circle.setStrokeWidth(5);
		canvas.drawLine(x_0, canvas_height / 2  - BUTTON_WIDTH / 4,
				x_0, canvas_height / 2, paint_circle);

		// --------------------------------------------------------------------------------------------

		paint.setColor(Color.argb(255, 0, 0, 0));
		paint.setTextSize(setTextSize);
		if(location == location_status.INDOOR) {
			paint.setColor(Color.argb(255, 0, 0, 255));
			disp_string = String.format("Location : INDOOR");
		} else if (location == location_status.OUTDOOR) {
			disp_string = String.format("Location : OUTDOOR");
		} else {
			disp_string = String.format("Location : UNKNOWN");
		}
		textWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, CONTEX_BASE_X1, CONTEX_BASE_Y1 + paint.getTextSize() * 4 + 5 , paint);

		// --------------------------------------------------------------------------------------------

		if(GPS_data_flag == true) {
			paint.setTextSize(setTextSize);
			paint.setColor(Color.argb(255, 255, 0, 0));
			disp_string = String.format("Getting GPS ");
			textWidth = paint.measureText(disp_string, 0, disp_string.length());
			canvas.drawText(disp_string, canvas_width - textWidth, canvas.getHeight() - paint.getTextSize(), paint);
		} else {
			//non
		}
	}

	/**@brief add HybridGPS packet data.
	 */
	public void addPacket(HybridGPS.HybridGPS_packet packet){
		step_num = (int)packet.step_count;
		distance_data = packet.distance_traveled;
		real_time_gps_accuracy = packet.gps_accuracy;
		real_time_orientation = packet.direction * 180 / PAI;
		location = packet.location_status;
		timer_status = packet.timer_status;

		if((timer_status == hybridgps_timer_status.GPS_ACTIVE_INDOOR_TIME) ||
				(timer_status == hybridgps_timer_status.GPS_ACTIVE_TIME) ||
				(timer_status == hybridgps_timer_status.GPS_ACTIVE_TIME_INIT)) {
			GPS_data_flag = true;
		} else {
			GPS_data_flag = false;
		}
	}

	/**@brief add MagnetCalibraw packet data.
	 */
	public void add_Magnet_calib_raw_Packet(HybridGPS.Magnet_cali_raw_packet packet){
		magnet_calib_packet_quarity = packet.quality;
	}

	/**@brief Update GPS data.
	 */
	public void update_GPS_data(HybridGPS.GPS_raw_packet packet) {
		gps_sate = packet.satellites_used;
		gps_hdop = packet.gpgga_hdop;
	}

	/**@brief Update GPS data.
	 */
	public void update_GPS_data(long satellites_used, float hdop) {
		gps_sate = satellites_used;
		gps_hdop = hdop;
	}

	/**@brief set hand mode data.
	 */
	public void set_hand_mode(int handmode) {
		hand_mode = handmode;
	}


	private SimpleOnScaleGestureListener _simpleListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			pinch_flag = true;
			redraw("test");
			return super.onScaleBegin(detector);
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			_scaleFactor_temp = detector.getScaleFactor();
			_scaleFactor *= _scaleFactor_temp;
			if (_scaleFactor < FLOAT_SCALE_FACTOR_MIN_CLIP) {
				_scaleFactor = FLOAT_SCALE_FACTOR_MIN_CLIP;
			} else if (_scaleFactor > FLOAT_SCALE_FACTOR_MAX_CLIP) {
				_scaleFactor = FLOAT_SCALE_FACTOR_MAX_CLIP;
			}
			redraw("test");
			super.onScaleEnd(detector);
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			pinch_flag = true;
			_scaleFactor_temp = detector.getScaleFactor();
			_scaleFactor *= _scaleFactor_temp;
			if (_scaleFactor < FLOAT_SCALE_FACTOR_MIN_CLIP) {
				_scaleFactor = FLOAT_SCALE_FACTOR_MIN_CLIP;
			} else if (_scaleFactor > FLOAT_SCALE_FACTOR_MAX_CLIP) {
				_scaleFactor = FLOAT_SCALE_FACTOR_MAX_CLIP;
			}
			redraw("test");
			return true;
		};
	};

	public boolean onTouchEvent(MotionEvent e) {

		int temp_x;
		int temp_y;

		_gestureDetector.onTouchEvent(e);

		switch (e.getAction()) {

			case MotionEvent.ACTION_DOWN:

				touch_flag = true;
				touch_base_x = (int) e.getX();
				touch_base_y = (int) e.getY();

				invalidate();
				break;

			case MotionEvent.ACTION_MOVE:

				if (touch_flag == true && pinch_flag != true) {
					temp_x = (int) e.getX();
					temp_y = (int) e.getY();
					offset_x = offset_x + (temp_x - touch_base_x);
					offset_y = offset_y + (temp_y - touch_base_y);
					touch_base_x = temp_x;
					touch_base_y = temp_y;
				}

				invalidate();
				break;

			case MotionEvent.ACTION_UP:

				pinch_flag = false;
				touch_flag = false;

				invalidate();
				break;

			default:
				break;

		}

		return true;

	}

	public void set_distance(double distance) {
		distance_data = distance;
	}

	/*
	public void add_coordinate(CompassStepTracker.Coordinate_packet coordinate_Packet) {
		Coordinate_packet.add(coordinate_Packet);
	}
	*/

	public void set_calib_status(float accuracy) {
		magnet_calib_packet_quarity = (int)accuracy;
	}

	public void start_GPS() {
		GPS_data_flag = true;
	}

	public void end_GPS() {
		GPS_data_flag = false;
	}

	public void set_gps_accuracy(float accuracy) {
		real_time_gps_accuracy = accuracy;
	}

	public void set_gps_output(boolean f_gps_output) {
		GPS_output_flag = f_gps_output;
	}

}
