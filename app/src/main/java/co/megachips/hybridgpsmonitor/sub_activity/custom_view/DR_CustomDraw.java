/*******************************************************************
 * @file	DR_CustomDraw.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
 *******************************************************************/

package co.megachips.hybridgpsmonitor.sub_activity.custom_view;

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

import java.util.ArrayList;
import java.util.Timer;

import co.megachips.hybridgpsmonitor.R;
import co.megachips.hybridgpsmonitor.sub_activity.DR;


/**@brief CustomView for DR activity
 */
@SuppressLint("DrawAllocation")
public class DR_CustomDraw extends View{

	public ArrayList<DR.DR_packet> drPackets = new ArrayList<DR.DR_packet>();

	Context main_context;
	private Timer mTimer;

	private ScaleGestureDetector _gestureDetector;
	private float _scaleFactor = 1.0f;
	private float _scaleFactor_temp = 1.0f;
	private float FLOAT_SCALE_FACTOR_MAX_CLIP = 2.50f;
	private float FLOAT_SCALE_FACTOR_MIN_CLIP = 0.50f;

	boolean touch_flag = false;
	boolean pinch_flag = false;

	public int offset_x = 0;
	public int offset_y = 0;

	private int touch_base_x = 0;
	private int touch_base_y = 0;

	public Paint paint = new Paint( Paint.ANTI_ALIAS_FLAG);

	/**@brief Constructor of this class.
	 */
	public DR_CustomDraw(Context context){

		super(context);
		main_context = context;

		_gestureDetector = new ScaleGestureDetector(main_context, _simpleListener);
	}

	/**@brief Reset internal parameter
	 */
	public void resetOffset(){
		_scaleFactor = 1.0f;
		offset_x = 0;
		offset_y = 0;
		drPackets.clear();
		redraw("test");
	}

	public void timer_Tick(){
		redraw("test");
	}

	/**@brief add DR packet data.
	 */
	public void addPacket(DR.DR_packet packet){
		drPackets.add(packet);
	}

	private void redraw(String str){
		Message valueMsg = new Message();
		valueMsg.obj = str;
		mHandler.sendMessage(valueMsg);
		return;
	}

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//invalidate();
			postInvalidate();
		}
	};

	@SuppressLint("DrawAllocation")
	protected void onDraw(Canvas canvas){

		Bitmap bitmap;
		Resources r = this.getResources();

		//---------------------------------------
		int BUTTON_AREA_RATIO = 30;
		int DR_AREA_RATIO = 100 - BUTTON_AREA_RATIO;
		int DR_AREA_MARGIN_RATIO_VERTICAL = 2;
		int DR_AREA_MARGIN_RATIO_HORIZONTAL = 3;
		int DR_BUTTON_AREA_MARGIN_VERTICAL = 2;
		int DR_BUTTON_AREA_MARGIN_HORIZONTAL = 2;
		//---------------------------------------

		int canvas_width = canvas.getWidth();
		int canvas_height = canvas.getHeight();

		int CANVAS_WIDTH = canvas.getWidth();

		int DR_MARGIN_VERTICAL = (canvas_height * DR_AREA_MARGIN_RATIO_VERTICAL) / 100;
		int DR_MARGIN_HORIZONTAL = (canvas_width * DR_AREA_MARGIN_RATIO_HORIZONTAL) / 100;

		int DR_AREA_HEIGHT = (canvas_height * DR_AREA_RATIO) /100;
		int BUTTON_AREA_HEIGHT = (canvas_height * BUTTON_AREA_RATIO) / 100;

		int DR_HEIGHT = DR_AREA_HEIGHT - (DR_MARGIN_VERTICAL * 2);
		int DR_WIDTH = CANVAS_WIDTH - (DR_MARGIN_HORIZONTAL * 2);

		int DR_BASE_X1 = DR_MARGIN_HORIZONTAL;
		int DR_BASE_X2 = DR_MARGIN_HORIZONTAL + DR_WIDTH;
		int DR_BASE_Y1 = BUTTON_AREA_HEIGHT + DR_MARGIN_VERTICAL;
		int DR_BASE_Y2 = BUTTON_AREA_HEIGHT + DR_MARGIN_VERTICAL + DR_HEIGHT;

		//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		bitmap  = BitmapFactory.decodeResource(r, R.drawable.graph_background);
		bitmap = Bitmap.createScaledBitmap(bitmap, DR_WIDTH, DR_HEIGHT, false);
		canvas.drawBitmap(bitmap, DR_BASE_X1, DR_BASE_Y1, null);
		//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

		int BUTTON_MARGIN_VERTICAL = (canvas_height * DR_BUTTON_AREA_MARGIN_VERTICAL) / 100;
		int BUTTON_MARGIN_HORIZONTAL = (canvas_width * DR_BUTTON_AREA_MARGIN_HORIZONTAL) / 100;

		int BUTTON_HEIGHT = (BUTTON_AREA_HEIGHT - (BUTTON_MARGIN_VERTICAL * 2)) / 2;
		int BUTTON_WIDTH = ( CANVAS_WIDTH - (BUTTON_MARGIN_HORIZONTAL * 3) ) / 2;

		int BUTTON1_BASE_X1 = BUTTON_MARGIN_HORIZONTAL;
		int BUTTON1_BASE_X2 = BUTTON_MARGIN_HORIZONTAL * 2 + BUTTON_WIDTH;
		int BUTTON1_BASE_Y1 = BUTTON_MARGIN_VERTICAL;
		int BUTTON1_BASE_Y2 = BUTTON_MARGIN_VERTICAL * 2 + BUTTON_HEIGHT;

		int BUTTON2_BASE_X1 = (BUTTON_MARGIN_HORIZONTAL * 2) + BUTTON_WIDTH;
		int BUTTON2_BASE_X2 = (BUTTON_MARGIN_HORIZONTAL * 2) + (BUTTON_WIDTH * 2);
		int BUTTON2_BASE_Y1 = BUTTON_MARGIN_VERTICAL;
		int BUTTON2_BASE_Y2 = BUTTON_MARGIN_VERTICAL + BUTTON_HEIGHT;

		// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		bitmap = BitmapFactory.decodeResource(r, R.drawable.distance2);
		bitmap = Bitmap.createScaledBitmap(bitmap, BUTTON_WIDTH, BUTTON_HEIGHT, false);
		canvas.drawBitmap(bitmap, BUTTON1_BASE_X1, BUTTON1_BASE_Y1, null);
		// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		bitmap = BitmapFactory.decodeResource(r, R.drawable.step2);
		bitmap = Bitmap.createScaledBitmap(bitmap, BUTTON_WIDTH, BUTTON_HEIGHT, false);
		canvas.drawBitmap(bitmap, BUTTON1_BASE_X1, BUTTON1_BASE_Y2, null);
		// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		bitmap  = BitmapFactory.decodeResource(r, R.drawable.degree);
		bitmap = Bitmap.createScaledBitmap(bitmap, BUTTON_WIDTH, BUTTON_HEIGHT, false);
		canvas.drawBitmap(bitmap, BUTTON1_BASE_X2, BUTTON1_BASE_Y1, null);
		//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

		int TEXT_BOTTOM_MARGIN = 20;
		int TEXT_RIGHT_MARGIN = 20;

		paint.setColor(Color.argb(255, 255, 255, 255));

		float textWidth;
		float unitWidth;
		String disp_string;

		paint.setTextSize(20);
		disp_string = "m";
		unitWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, BUTTON1_BASE_X2 - unitWidth - TEXT_RIGHT_MARGIN * 2, BUTTON2_BASE_Y2 - TEXT_BOTTOM_MARGIN, paint);

		paint.setTextSize(65);
		if(drPackets.size()>0){
			DR.DR_packet last_packet = drPackets.get(drPackets.size()-1);
			disp_string = String.format("%.2f", last_packet.oddMeter);
		}else{
			disp_string = "";
		}
		textWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, BUTTON1_BASE_X2 - textWidth - unitWidth - TEXT_RIGHT_MARGIN * 2, BUTTON2_BASE_Y2 - TEXT_BOTTOM_MARGIN, paint);

		//---------------------------------------------------------
		paint.setTextSize(20);
		disp_string = "degree";
		unitWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, BUTTON2_BASE_X2 - unitWidth - TEXT_RIGHT_MARGIN, BUTTON2_BASE_Y2 - TEXT_BOTTOM_MARGIN, paint);

		paint.setTextSize(65);
		if(drPackets.size()>=1){
			DR.DR_packet last_packet = drPackets.get(drPackets.size()-1);
			disp_string = String.format("%.2f", last_packet.angle * 180/3.14);
		}else{
			disp_string = "";
		}
		textWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, BUTTON2_BASE_X2 - unitWidth - textWidth - TEXT_RIGHT_MARGIN, BUTTON2_BASE_Y2 - TEXT_BOTTOM_MARGIN, paint);

		//--------------------------------------------------------------------------------------------

		paint.setTextSize(20);
		disp_string = "step";
		unitWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, BUTTON1_BASE_X2 - unitWidth - TEXT_RIGHT_MARGIN * 2, BUTTON2_BASE_Y2 + BUTTON_HEIGHT , paint);

		paint.setTextSize(65);
		if(drPackets.size()>0){
			DR.DR_packet last_packet = drPackets.get(drPackets.size()-1);
			disp_string = String.format("%d", last_packet.num_of_steps);
		}else{
			disp_string = "";
		}
		textWidth = paint.measureText(disp_string, 0, disp_string.length());
		canvas.drawText(disp_string, BUTTON1_BASE_X2 - textWidth - unitWidth - TEXT_RIGHT_MARGIN * 2, BUTTON2_BASE_Y2 + BUTTON_HEIGHT, paint);


		int DEVIDE_MIN = 4;
		int DEVIDE_MAX = 6;
		int SCALE_FACTOR_MIN = 100;
		int SCALE_FACTOR_MAX = 500;
		int SCALE_FACTOR_MIN_CLIP = 345;
		int SCALE_FACTOR_MAX_CLIP = 490;
		int SCALE_OFFSET = SCALE_FACTOR_MIN_CLIP - SCALE_FACTOR_MIN;
		int SCALE_FACTOR_UNIT = ((DR_HEIGHT/DEVIDE_MIN) - (DR_HEIGHT/DEVIDE_MAX))/(SCALE_FACTOR_MAX - SCALE_FACTOR_MIN);
		if(SCALE_FACTOR_UNIT <= 1){
			SCALE_FACTOR_UNIT = 1;
		}

		float scale_factor_temp = (FLOAT_SCALE_FACTOR_MAX_CLIP+FLOAT_SCALE_FACTOR_MIN_CLIP) - _scaleFactor;
		int scale = (int)(scale_factor_temp * 100) + SCALE_OFFSET;
		if(scale<SCALE_FACTOR_MIN_CLIP){
			scale = SCALE_FACTOR_MIN_CLIP;
		}else if(scale > SCALE_FACTOR_MAX_CLIP){
			scale = SCALE_FACTOR_MAX_CLIP;
		}

		paint.setColor(Color.argb(255, 220, 220, 220));
		paint.setStrokeWidth(3);
		int center_x = DR_BASE_X1+(DR_BASE_X2-DR_BASE_X1)/2 + offset_x;
		int center_y = DR_BASE_Y1+(DR_BASE_Y2-DR_BASE_Y1)/2 + offset_y;

		if(center_x>DR_BASE_X1 && center_x<DR_BASE_X2){
			canvas.drawLine(center_x, DR_BASE_Y1, center_x, DR_BASE_Y2, paint);
		}

		if(center_y>DR_BASE_Y1 && center_y<DR_BASE_Y2){
			canvas.drawLine(DR_BASE_X1, center_y, DR_BASE_X2, center_y, paint);
		}

		paint.setColor(Color.argb(255, 220, 220, 220));
		paint.setStrokeWidth(1);

		int reversed_scale = (SCALE_FACTOR_MAX-SCALE_FACTOR_MIN)-(scale-SCALE_FACTOR_MIN);
		if(reversed_scale<=0){reversed_scale = 1;}
		for(int i=1; center_x + (i*reversed_scale*SCALE_FACTOR_UNIT) < DR_BASE_X2; i++){
			int temp_pos = center_x + i*reversed_scale*SCALE_FACTOR_UNIT;
			if(temp_pos > DR_BASE_X1){
				canvas.drawLine(temp_pos, DR_BASE_Y1, temp_pos, DR_BASE_Y2, paint);
			}
		}

		for(int i=1; center_x - (i*reversed_scale*SCALE_FACTOR_UNIT) > DR_BASE_X1; i++){
			int temp_pos = center_x - i*reversed_scale*SCALE_FACTOR_UNIT;
			if(temp_pos < DR_BASE_X2){
				canvas.drawLine(temp_pos, DR_BASE_Y1, temp_pos, DR_BASE_Y2, paint);
			}
		}

		for(int i=1; center_y + (i*reversed_scale*SCALE_FACTOR_UNIT) < DR_BASE_Y2; i++){
			int temp_pos = center_y + i*reversed_scale*SCALE_FACTOR_UNIT;
			if(temp_pos > DR_BASE_Y1){
				canvas.drawLine(DR_BASE_X1, temp_pos, DR_BASE_X2, temp_pos, paint);
			}
		}

		for(int i=1; center_y - (i*reversed_scale*SCALE_FACTOR_UNIT) > DR_BASE_Y1; i++){
			int temp_pos = center_y - i*reversed_scale*SCALE_FACTOR_UNIT;
			if(temp_pos < DR_BASE_Y2){
				canvas.drawLine(DR_BASE_X1, temp_pos, DR_BASE_X2, temp_pos, paint);
			}
		}
		//--------------------------------------------------------------------------------------------

		canvas.clipRect(DR_BASE_X1, DR_BASE_Y1, DR_BASE_X2, DR_BASE_Y2);

		if(drPackets.size()>1){

			paint.setStrokeWidth(12);
			paint.setColor(Color.argb(255, 255, 0, 0));

			int plotX1 = 0;
			int plotY1 = 0;
			int plotX2 = 0;
			int plotY2 = 0;

			DR.DR_packet packet;
			for(int i=0; i<drPackets.size()-1; i++){

				packet = drPackets.get(i);
				plotX1 = center_x + (int)((packet.relativeX * 100.0f ) * reversed_scale * SCALE_FACTOR_UNIT ) / 100;
				plotY1 = center_y - (int)((packet.relativeY * 100.0f ) * reversed_scale * SCALE_FACTOR_UNIT ) / 100;

				packet = drPackets.get(i+1);
				plotX2 = center_x + (int)((packet.relativeX * 100.0f ) * reversed_scale * SCALE_FACTOR_UNIT ) / 100;
				plotY2 = center_y - (int)((packet.relativeY * 100.0f ) * reversed_scale * SCALE_FACTOR_UNIT ) / 100;

				canvas.drawLine(plotX1, plotY1, plotX2, plotY2, paint);
				canvas.drawCircle(plotX2, plotY2, 5, paint);
			}

			canvas.drawCircle(plotX2, plotY2, 9, paint);

		}else if(drPackets.size()==1){
			paint.setColor(Color.argb(255, 255, 0, 0));
			int plotX1 = 0;
			int plotY1 = 0;
			DR.DR_packet packet;
			packet = drPackets.get(drPackets.size()-1);
			plotX1 = center_x + (int)((packet.relativeX * 100.0f ) * reversed_scale * SCALE_FACTOR_UNIT ) / 100;
			plotY1 = center_y - (int)((packet.relativeY * 100.0f ) * reversed_scale * SCALE_FACTOR_UNIT ) / 100;
			canvas.drawCircle(plotX1, plotY1, 9, paint);

		}

		//--------------------------------------------------------------------------------------------
		paint.setTextSize(20);
		paint.setColor(Color.argb(255, 0, 0, 0));
		String grid_string = "1grid = 1m";
		unitWidth = paint.measureText(grid_string, 0, grid_string.length());
		canvas.drawText(grid_string, DR_BASE_X1 + 20, DR_BASE_Y2 - 20, paint);
		//--------------------------------------------------------------------------------------------

	}

	private SimpleOnScaleGestureListener _simpleListener
			= new SimpleOnScaleGestureListener() {
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
			if(_scaleFactor<FLOAT_SCALE_FACTOR_MIN_CLIP){
				_scaleFactor=FLOAT_SCALE_FACTOR_MIN_CLIP;
			}else if(_scaleFactor>FLOAT_SCALE_FACTOR_MAX_CLIP){
				_scaleFactor=FLOAT_SCALE_FACTOR_MAX_CLIP;
			}
			redraw("test");
			super.onScaleEnd(detector);
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			pinch_flag = true;
			_scaleFactor_temp = detector.getScaleFactor();
			_scaleFactor *= _scaleFactor_temp;
			if(_scaleFactor<FLOAT_SCALE_FACTOR_MIN_CLIP){
				_scaleFactor=FLOAT_SCALE_FACTOR_MIN_CLIP;
			}else if(_scaleFactor>FLOAT_SCALE_FACTOR_MAX_CLIP){
				_scaleFactor=FLOAT_SCALE_FACTOR_MAX_CLIP;
			}
			redraw("test");
			return true;
		};
	};

	public boolean onTouchEvent(MotionEvent e){

		int temp_x;
		int temp_y;

		_gestureDetector.onTouchEvent(e);

		switch(e.getAction()){

			case MotionEvent.ACTION_DOWN:

				touch_flag = true;
				touch_base_x = (int)e.getX();
				touch_base_y = (int)e.getY();

				invalidate();
				break;

			case MotionEvent.ACTION_MOVE:

				if(touch_flag == true && pinch_flag != true){
					temp_x = (int)e.getX();
					temp_y = (int)e.getY();
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

}
