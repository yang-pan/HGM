/*******************************************************************
 * @file	BleCommand.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
*******************************************************************/

package co.megachips.hybridgpsmonitor.ble_service;
public class BleCommand {

	private byte id;

	public BleCommand(byte b){
		id = b;
	}

	public BleCommand(byte b, byte[] data){
		id = b;
	}

	public byte[] getData(){
		byte[] data = makeData();
		return data;
	}

	public byte[] makeData(){
		byte[] data = null;
		if(id == RequestType.FRIZZ_RESET){
			data = new byte[1];
			data[0] = id;
		}else if(id == RequestType.SET_LOGGING_MODE){
			data = new byte[2];
			data[0] = id;
		}else if(id == RequestType.SET_BLE_TIMER){
			data = new byte[2];
			data[0] = id;
		}else if(id == RequestType.SET_SAMPLING_FREQUENCY){
			data = new byte[5];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_SET_SAMPLING_START){
			data = new byte[5];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_SET_SAMPLING_STOP){
			data = new byte[2];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_SET_UART){
			data = new byte[1];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_PPG_SET_UART){
			data = new byte[1];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_SET_BLE){
			data = new byte[1];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_PPG_SET_SAMPLING_START){
			data = new byte[5];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_PPG_SET_SAMPLING_STOP){
			data = new byte[2];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_PPG_SET_BLE){
			data = new byte[1];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_PPG_SET_DETAIL){
			data = new byte[2];
			data[0] = id;
		}else if(id == RequestType.START_ERASE){
			data = new byte[1];
			data[0] = id;
		}else if(id == RequestType.START_ALL_ERASE){
			data = new byte[1];
			data[0] = id;
		}else if(id == RequestType.START_HEART_RATE){
			data = new byte[1];
			data[0] = id;
		}else if(id == RequestType.SET_HEART_RATE){
			data = new byte[44+1];
			data[0] = id;
		}else if(id == RequestType.STOP_HEART_RATE){
			data = new byte[1];
			data[0] = id;
		}else if(id == RequestType.GET_SENSOR_VERSION){
			data = new byte[1];
			data[0] = id;
		}else if(id == RequestType.SWING_CALIBRATION_START){
			data = new byte[1];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_PPG_GET_DETAIL){
			data = new byte[2];
			data[0] = id;
		}else if(id == RequestType.SEND_CMD){
			data = new byte[7];
			data[0] = id;
		}else if(id == RequestType.ERASE_SPIFLASH){
			data = new byte[9];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_GPS_SET_SAMPLING_START){
			data = new byte[5];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_GPS_SET_SAMPLING_STOP){
			data = new byte[2];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_GRAVITY_SET_SAMPLING_START){
			data = new byte[5];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_GRAVITY_SET_SAMPLING_STOP){
			data = new byte[2];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_GPS_SET_UART){
			data = new byte[1];
			data[0] = id;
		}else if(id == RequestType.SPI_FLASH_GRAVITY_SET_UART){
			data = new byte[1];
			data[0] = id;
		}
		return data;
	}

	//Make data to control sensor
	public static byte[] Set_SensorControl_Param(int cmd, int sensor_id, int sensor_id2, int param_length, int param[]){
		byte[] data = new byte[7 + param_length * 4];
		data[0] = BleCommand.RequestType.SENSOR_CONTROL;
		data[1] = (byte) cmd;
		data[2] = (byte) sensor_id;
		data[3] = 0x00; //dummy
		data[4] = BleCommand.GPIO_mode.GPIO_DISABLE;
		data[5] = (byte) sensor_id2;
		data[6] = (byte) param_length; //parameter num
		for(int i = 0; i < param_length; i++)
		{
			data[7 + i * 4] = (byte) ((param[i] & 0xFF000000)>>24);
			data[8 + i * 4] = (byte) ((param[i] & 0x00FF0000)>>16);
			data[9 + i * 4] = (byte) ((param[i] & 0x0000FF00)>>8);
			data[10 + i * 4] = (byte) ((param[i] & 0x000000FF));
		}
		return data;
	}

	//Make data to activate sensor
	public static byte[] registerListener(int sensor_id){
		byte[] data = new byte[7];
		data[0] = BleCommand.RequestType.SENSOR_CONTROL;
		data[1] = BleCommand.HUB_mng_cmd.HUB_MGR_CMD_SET_SENSOR_ACTIVATE;
		data[2] = (byte) sensor_id;
		data[3] = 0x00; //dummy
		data[4] = BleCommand.GPIO_mode.GPIO_ENABLE;
		data[5] = (byte) BleCommand.SensorID.HUB_MGR_ID;
		data[6] = 0x00; //parameter number
		return data;
	}

	//Make data to deactivate sensor
	public static byte[] unregisterListener(int sensor_id){
		byte[] data = new byte[7];
		data[0] = BleCommand.RequestType.SENSOR_CONTROL;
		data[1] = BleCommand.HUB_mng_cmd.HUB_MGR_CMD_DEACTIVATE_SENSOR;
		data[2] = (byte) sensor_id;
		data[3] = 0x00; //dummy
		data[4] = BleCommand.GPIO_mode.GPIO_DISABLE;
		data[5] = (byte) BleCommand.SensorID.HUB_MGR_ID;
		data[6] = 0x00; //parameter number
		return data;
	}

	//Make data to set sensor interval
	public static byte[] setinterval(int sensor_id, int interval) {
		byte[] data = new byte[7 + 4];
		data[0] = BleCommand.RequestType.SENSOR_CONTROL;
		data[1] = BleCommand.HUB_mng_cmd.HUB_MGR_CMD_SET_SENSOR_INTERVAL;
		data[2] = (byte) sensor_id;
		data[3] = 0x00; //dummy
		data[4] = BleCommand.GPIO_mode.GPIO_DISABLE;
		data[5] = (byte) BleCommand.SensorID.HUB_MGR_ID;
		data[6] = 0x01; //parameter number 0x01 = 4byte
		data[7] = (byte) ((interval & 0xFF000000)>>24);
		data[8] = (byte) ((interval & 0x00FF0000)>>16);
		data[9] = (byte) ((interval & 0x0000FF00)>>8);
		data[10] = (byte) ((interval & 0x000000FF));
		return data;
	}

	//Make data to send frizz FW
	public static byte[] setfrizzFW(byte id,int seg, int total, int length, byte param[], long check_sum) {
		byte[] data = new byte[9 + length];
		data[0] = id;
		data[1] = (byte) ((seg & 0x0000FF00)>>8);
		data[2] = (byte) ((seg & 0x000000FF));
		data[3] = (byte) ((total & 0x0000FF00)>>8);
		data[4] = (byte) ((total & 0x000000FF));
		data[5] = (byte) ((length & 0x0000FF00)>>8);
		data[6] = (byte) ((length & 0x000000FF));
		for(int i = 0; i < length; i++)
		{
			data[7 + i] = param[i];
		}

		data[9 + length - 2] = (byte) ((check_sum & 0x0000FF00)>>8);
		data[9 + length - 1] = (byte) ((check_sum & 0x000000FF));
		return data;
	}

	//Request type from android to host
	public class RequestType{
		public static final byte SENSOR_CONTROL					= 0x0;
		public static final byte FRIZZ_RESET					= 0x1;
		public static final byte SET_LOGGING_MODE				= 0x2;
		public static final byte SET_BLE_TIMER					= 0x3;
		public static final byte START_SENSOR_DUMP				= 0x4;
		public static final byte STOP_SENSOR_DUMP				= 0x5;
		public static final byte EXIT_SENSOR_DUMP				= 0x6;
		public static final byte SET_SAMPLING_FREQUENCY			= 0x7;
		public static final byte SPI_FLASH_SET_SAMPLING_START	= 0x8;
		public static final byte SPI_FLASH_SET_SAMPLING_STOP	= 0x9;
		public static final byte SPI_FLASH_SET_UART				= 0xA;
		public static final byte SPI_FLASH_SET_BLE				= 0xB;
		public static final byte START_ERASE					= 0xC;
		public static final byte START_ALL_ERASE				= 0xD;
		public static final byte SWING_CALIBRATION_START		= 0xE;
		public static final byte GET_SENSOR_VERSION				= 0xF;
		public static final byte SEND_CMD						= 0x10;
		public static final byte SEND_FRIZZFW					= 0x11;
		public static final byte ERASE_SPIFLASH					= 0x12;
		public static final byte SEND_CHIGNONFW					= 0x13;

		public static final byte SPI_FLASH_GPS_SET_SAMPLING_START	= 0x14;
		public static final byte SPI_FLASH_GPS_SET_SAMPLING_STOP	= 0x15;
		public static final byte SPI_FLASH_GPS_SET_UART				= 0x16;
		public static final byte SPI_FLASH_GRAVITY_SET_SAMPLING_START	= 0x17;
		public static final byte SPI_FLASH_GRAVITY_SET_SAMPLING_STOP	= 0x18;
		public static final byte SPI_FLASH_GRAVITY_SET_UART				= 0x19;

		public static final byte START_HEART_RATE				= 0x40;
		public static final byte SET_HEART_RATE					= 0x41;
		public static final byte STOP_HEART_RATE				= 0x42;

		public static final byte SPI_FLASH_PPG_SET_SAMPLING_START	= 0x43;
		public static final byte SPI_FLASH_PPG_SET_SAMPLING_STOP	= 0x44;
		public static final byte SPI_FLASH_PPG_SET_BLE				= 0x45;
		public static final byte SPI_FLASH_PPG_SET_DETAIL			= 0x46;
		public static final byte SPI_FLASH_PPG_GET_DETAIL			= 0x47;
		public static final byte SPI_FLASH_PPG_SET_UART				= 0x48;
	}

	//Receive type from host to android
	public class ReceivedDataType{
		public static final int RESPONSE_ACK 					= 0x0;
		public static final int RESPONSE_RECEIVE_SUCCESS 			= 0x1;
		public static final int RESPONSE_RECEIVE_FAIL 				= 0x2;
		public static final int SENSOR_RAW_DATA					= 0x4;
		public static final int SENSOR_TEST_DATA				= 0x5;
		public static final int CALIBRATION_END					= 0x6;
		public static final int STATE_STARTED					= 0x7;
		public static final int STATE_STOPPED					= 0x8;
		public static final int ERASE_END						= 0x9;
		public static final int LOGSEND_END						= 0xA;
		public static final int SWING_CALI_END					= 0xB;
		public static final int PULSE_SETTING_DATA				= 0xC;
		public static final int LOG_DETAIL						= 0xD;
		public static final int RESPONSE_GEOFENCING_PARAMETER_SUCCESS	= 0x10;
		public static final int RESPONSE_GEOFENCING_PARAMETER_FAIL	= 0x11;
		public static final int RESPONSE_GEOFENCING_RESET_SUCCESS	= 0x12;
		public static final int RESPONSE_GEOFENCING_RESET_FAIL		= 0x13;
		public static final int RESULT_ACK							= 0x14;
		public static final int RESULT_NAK							= 0x15;
		public static final int SEND_FW_OK							= 0x16;
		public static final int SEND_FW_NG							= 0x17;
		public static final int CONNECT_TYPE					= 0x7F;
		public static final int DATA_RECEIVE 					= 0xFF;
	}

	//frizz sensor ID
	public class SensorID{
		public static final int SENSOR_ID_PRESSURE_ALPS_RAW		= 0x00;
		/* Physical Sensors */
		public static final int SENSOR_ID_ACCEL_RAW				= 0x80;
		public static final int SENSOR_ID_MAGNET_RAW			= 0x81;
		public static final int SENSOR_ID_GYRO_RAW	    		= 0x82;
		public static final int SENSOR_ID_PRESSURE_RAW			= 0x83;
		public static final int SENSOR_ID_LIGHT_RAW				= 0xA9;
		public static final int SENSOR_ID_PROXIMITY_RAW			= 0xAA;
		/* Application Sensors */
		// Accelerometer
		public static final int SENSOR_ID_ACCEL_POWER			= 0x84;
		public static final int SENSOR_ID_ACCEL_LPF	    		= 0x85;
		public static final int SENSOR_ID_ACCEL_HPF	    		= 0x86;
		public static final int SENSOR_ID_ACCEL_STEP_DETECTOR	= 0x87;
		public static final int SENSOR_ID_ACCEL_PEDOMETER		= 0x88;
		public static final int SENSOR_ID_ACCEL_LINEAR			= 0x89;
		// Magnetometer
		public static final int SENSOR_ID_MAGNET_PARAMETER		= 0x8A;
		public static final int SENSOR_ID_MAGNET_CALIB_SOFT		= 0x8B;
		public static final int SENSOR_ID_MAGNET_CALIB_HARD		= 0x8C;
		public static final int SENSOR_ID_MAGNET_LPF			= 0x8D;
		public static final int SENSOR_ID_MAGNET_UNCALIB		= 0x8E;
		// Gyroscope
		public static final int SENSOR_ID_GYRO_LPF				= 0x8F;
		public static final int SENSOR_ID_GYRO_HPF				= 0x90;
		public static final int SENSOR_ID_GYRO_UNCALIB			= 0x91;
		// Fusion 6D
		public static final int SENSOR_ID_GRAVITY				= 0x92;
		public static final int SENSOR_ID_DIRECTION				= 0x93;
		// Fusion 9D
		public static final int SENSOR_ID_POSTURE				= 0x94;
		public static final int SENSOR_ID_ROTATION_MATRIX		= 0x95;
		public static final int SENSOR_ID_ORIENTATION			= 0x96;
		public static final int SENSOR_ID_ROTATION_VECTOR		= 0x97;
		// PDR
		public static final int SENSOR_ID_PDR					= 0x98;
		public static final int SENSOR_ID_VELOCITY				= 0x99;
		public static final int SENSOR_ID_RELATIVE_POSITION		= 0x9A;
		public static final int SENSOR_ID_MIGRATION_LENGTH		= 0x9B;
		// Util
		public static final int SENSOR_ID_CYCLIC_TIMER			= 0x9C;
		public static final int SENSOR_ID_DEBUG_QUEUE_IN		= 0x9D;
		public static final int SENSOR_ID_DEBUG_STD_IN			= 0x9E;
		// Accelerometer
		public static final int SENSOR_ID_ACCEL_MOVE			= 0x9F;
		// Libs
		public static final int SENSOR_ID_ISP					= 0xA0;
		// Rotation
		public static final int SENSOR_ID_ROTATION_GRAVITY_VECTOR		= 0xA1;
		public static final int SENSOR_ID_ROTATION_LPF_VECTOR			= 0xA2;
		// Fall down detection
		public static final int SENSOR_ID_ACCEL_FALL_DOWN		= 0xA3;
		public static final int SENSOR_ID_ACCEL_POS_DET			= 0xA4;
		// Geofencing
		public static final int SENSOR_ID_PDR_GEOFENCING		= 0xA5;
		// Gesture
		public static final int SENSOR_ID_GESTURE				= 0xA6;
		// Pressure
		public static final int SENSOR_ID_STAIR_DETECTOR		= 0xA8;
		// magnet calibration
		public static final int SENSOR_ID_MAGNET_CALIB_RAW 		= 0xAB;
		// blood_pressure
		public static final int SENSOR_ID_BLOOD_PRESSURE 		= 0xAE;
		public static final int SENSOR_ID_BLOOD_PRESSURE_LEARN	= 0xAF;
		// Activity Detection
		public static final int SENSOR_ID_ACTIVITY_DETECTOR		= 0xBA;
		// Motion Detection
		public static final int SENSOR_ID_MOTION_DETECTOR		= 0xBC;
		public static final int SENSOR_ID_MOTION_SENSING		= 0xBD;
		// BikeDetection
		public static final int SENSOR_ID_BIKE_DETECTOR			= 0xBE;
		// Calorie
		public static final int SENSOR_ID_CALORIE				= 0xBF;
		// Stress Measure
		public static final int SENSOR_ID_STRESS_MEASURE		= 0xC9;
		// ppg data quality judgement
		public static final int SENSOR_ID_PPG_QUALITY_CHECK		= 0xCE;
		// HybridGPS
		public static final int SENSOR_ID_GPS_RAW				= 0xCF;
		public static final int SENSOR_ID_DR					= 0xD0;
		public static final int SENSOR_ID_HYBRIDGPS				= 0xD1;
		public static final int SENSOR_ID_HYBRID_WRAPPER		= 0xD2;
		// HUB manager
		public static final int HUB_MGR_ID						= 0xFF;
	}

	//HUB manager command
	public class HUB_mng_cmd{
		public static final int HUB_MGR_CMD_DEACTIVATE_SENSOR = 0x00;
		public static final int HUB_MGR_CMD_SET_SENSOR_ACTIVATE = 0x01;
		public static final int HUB_MGR_CMD_SET_SENSOR_INTERVAL = 0x02;
	}

	//GPIO event handler mode of host
	public class GPIO_mode{
		public static final int GPIO_DISABLE 	= 0x00;
		public static final int GPIO_ENABLE 	= 0x01;
	}

	//Logging mode of host
	public class Loging_mode{
		public static final int ENABLE_LOGGING 	= 0x00;
		public static final int DISABLE_LOGGING = 0x01;
	}

	//Packet transmission interval from host to android
	public class BLE_timer_mode{
		public static final int HIGH_SPEED_MODE 	= 0x00;
		public static final int LOW_SPEED_MODE 		= 0x01;
	}

	//kind of data
	public class kind_data{
		public static final int OUTPUT		= 0x80;
		public static final int RESPONCE	= 0x84;
	}
}
