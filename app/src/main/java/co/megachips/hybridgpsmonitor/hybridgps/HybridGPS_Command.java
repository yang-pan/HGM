/*******************************************************************
 * @file	HybridGPS_Command.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2016/05/18
 * @author	MegaChips
 * @brief	Define value for HybridGPS
*******************************************************************/

package co.megachips.hybridgpsmonitor.hybridgps;
import co.megachips.hybridgpsmonitor.R;

public class HybridGPS_Command {

	/**
	 * @brief Magnet accuracy status
	 */
	public class magnet_cali_status{
		public static final int LOW_QUARITY  = 1;		//Low accuracy
		public static final int MIDDLE_QUARITY  = 2;	//Middle accuracy
		public static final int HIGH_QUARITY = 3;		//High accuracy
	}

	/**
	 * @brief GPS accuracy status
	 */
	public class gps_status{
		public static final int LOW_QUARITY  = 0;		//Low accuracy
		public static final int MIDDLE_QUARITY  = 1;	//Middle accuracy
		public static final int HIGH_QUARITY = 2;		//High accuracy
	}

	/**
	 * @brief Location status
	 */
	public class location_status {
		public static final int  UNKNOWN  = 0;	//Unknown
		public static final int  OUTDOOR  = 1;	//Outdoor
		public static final int  INDOOR  = 2;	//Indoor
	}

	/**
	 * @brief Radio data for "hand mode"
	 */
	public class device_hand_mode {
		public static final int  LEFT_HAND_MODE  = R.id.mode2_RadioButton1;		//Left hand
		public static final int  RIGHT_HAND_MODE  = R.id.mode2_RadioButton2;	//Right hand
		public static final int  COMMAND_LEFT_HAND_MODE  = 0;		//Left hand
		public static final int  COMMAND_RIGHT_HAND_MODE  = 1;	//Right hand
	}

	/**
	 * @brief Radio data for "output mode"
	 */
	public class output_mode {
		public static final int  OUTPUT_DEMO  = R.id.outputmode_RadioButton1;
		public static final int  OUTPUT_DEBUG  = R.id.outputmode_RadioButton2;
	}


	/**
	 * @brief Radio data for "gps type"
	 */
	public class gps_type {
		public static final int  DEFAULT_TYPE_DETAIL  = R.id.mode_RadioButton1;
		public static final int  DEFAULT_TYPE_SIMPLE  = R.id.mode_RadioButton2;
		public static final int  COMMAND_TYPE_DETAIL  = 0;
		public static final int  COMMAND_TYPE_SIMPLE  = 1;
	}

	/**
	 * @brief Default parameter (User setting parameter)"
	 */
	public class user_default_param {
		//GPS setting-------------------------------------------------------------
		public static  final int DEFAULT_ACTIVITY_TIME = 60;
		public static  final int DEFAULT_SLEEP_TIME = 120;
		public static  final int DEFAULT_ACTIVITY_TIME_INDOOR = 60;
		public static  final int DEFAULT_SLEEP_TIME_INDOOR = 180;
		public static  final int DEFAULT_DETECT_TIME = 180;
		public static  final int DEFAULT_TYPE_SIMPLE = gps_type.DEFAULT_TYPE_SIMPLE;
		public static  final int DEFAULT_SATE_H = 10;
		public static  final int DEFAULT_SATE_M = 8;
		public static  final int DEFAULT_HDOP_H = 8; 	// HDOP=value/10
		public static  final int DEFAULT_HDOP_M = 10; 	// HDOP=value/10
		public static  final int DEFAULT_ACCURACY_H = 10;
		public static  final int DEFAULT_ACCURACY_M = 15;

		//DR setting--------------------------------------------------------------
		public static  final int DEFAULT_STRIDE = 67;
		public static  final int DEFAULT_ANGLE_AVERAGE = 1000;
		public static  final int DEFAULT_ANGLE_OFFSET = 0;
		public static  final int DEFAULT_DR_PLOT = 0;
		public static  final int DEFAULT_DETECT_HANDMODE = device_hand_mode.LEFT_HAND_MODE;
		//output setting--------------------------------------------------------------
		public static  final int DEFAULT_OUTPUT = output_mode.OUTPUT_DEMO;
	}


	/**
	 * @brief HybridGPS timer_status"
	 */
	public class hybridgps_timer_status {
		public static  final int NO_DATA = 0;
		public static  final int GPS_ACTIVE_TIME_INIT = 1;
		public static  final int GPS_ACTIVE_TIME = 2;
		public static  final int GPS_SLEEP_TIME = 3;
		public static  final int GPS_ACTIVE_INDOOR_TIME = 4;
		public static  final int GPS_SLEEP_INDOOR_TIME = 5;
	}
}
