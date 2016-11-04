/*******************************************************************
 * @file	BleService.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
*******************************************************************/

package co.megachips.hybridgpsmonitor.ble_service;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import co.megachips.hybridgpsmonitor.BroadcastCommand;
import co.megachips.hybridgpsmonitor.BroadcastData;
import co.megachips.hybridgpsmonitor.DataPacket;
import android.os.Handler;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BleService extends Service {

	private BleDataHandler bleDataHandler = new BleDataHandler();

	private final static String TAG = BleService.class.getSimpleName();

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;
	private int mConnectionState = STATE_DISCONNECTED;

	public static final int STATE_DISCONNECTED = 0;
	public static final int STATE_CONNECTING = 1;
	public static final int STATE_CONNECTED = 2;

	public static final UUID TX_POWER_UUID = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
	public static final UUID TX_POWER_LEVEL_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
	public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	public static final UUID FIRMWARE_REVISON_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
	public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
	public static final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

	private static final byte RESPONSE_ACK = 0;
	private static final byte RESPONSE_RECEIVE_SUCCESS = 1;
	private static final byte RESPONSE_RECEIVE_FAIL = 2;
	private static final byte DATA_RECEIVE = (byte) 255;

	private static final int SEND_PACKET_SIZE = 20;

	private static final int FREE = 0;
	private static final int SENDING = 1;
	private static final int RECEIVING = 2;

	private int ble_status = FREE;
	private int packet_counter = 0;
	private int send_data_pointer = 0;
	private byte[] send_data = null;
	private boolean first_packet = false;
	private boolean final_packet = false;

	private boolean packet_send = false;

	private Timer mTimer;
	private int time_out_counter = 0;
	//private int TIMER_INTERVAL = 1000;		// Timer interval for time out
	//private int TIME_OUT_LIMIT = 10;
	private int TIMER_INTERVAL = 100;		// Timer interval for time out
	private int TIME_OUT_LIMIT = 100;

	public ArrayList<byte[]> data_queue = new ArrayList<byte[]>();
	boolean sendingStoredData = false;


	Handler start_handler = new Handler();

	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

			String intentAction;

			// Attempts to discover services after successful connection.
			if( newState == BluetoothProfile.STATE_CONNECTED ){
				intentAction = BroadcastCommand.ACTION_GATT_CONNECTED;
				mConnectionState = STATE_CONNECTED;
				broadcastUpdate(intentAction);
				Log.i(TAG, "Connected to GATT server.");
				Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
			}else if( newState == BluetoothProfile.STATE_DISCONNECTED ){
				intentAction = BroadcastCommand.ACTION_GATT_DISCONNECTED;
				mConnectionState = STATE_DISCONNECTED;
				broadcastUpdate(intentAction);
				Log.i(TAG, "Disconnected from GATT server.");
			}

		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if( status == BluetoothGatt.GATT_SUCCESS ){
				Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt);
			    broadcastUpdate(BroadcastCommand.ACTION_GATT_SERVICES_DISCOVERED);
			}else{
			    Log.w(TAG, "onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		    if( status == BluetoothGatt.GATT_SUCCESS ){
		        broadcastUpdate(BroadcastCommand.ACTION_DATA_AVAILABLE, characteristic);
		    }
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			//Log.d(TAG, "onCharacteristicWrite: " + status);
			switch (status) {
				case BluetoothGatt.GATT_SUCCESS:
					//Log.e(TAG, "onCharacteristicWrite: GATT_SUCCESS");
					packet_send = true;
					break;
				case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
					//Log.e(TAG, "onCharacteristicWrite: GATT_WRITE_NOT_PERMITTED");
					break;

				case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
					//Log.e(TAG, "onCharacteristicWrite: GATT_REQUEST_NOT_SUPPORTED");
					break;

				case BluetoothGatt.GATT_FAILURE:
					//Log.e(TAG, "onCharacteristicWrite: GATT_FAILURE");
					break;

				case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
					break;

				case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
					break;

				case BluetoothGatt.GATT_INVALID_OFFSET:
					break;
			}
		}

		// Server -> Client(Android) notification
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			final byte[] received = characteristic.getValue();
			String temp_text="";
			for(int i = 0;i < received.length; i++)
			{
				temp_text = (temp_text + Integer.toHexString(received[i] & 0xff)+ " ");
			}
			Log.d("BLE_receive_data", "" + temp_text);
		    broadcastUpdate(BroadcastCommand.ACTION_DATA_AVAILABLE, characteristic);
		}

	};


	/**@brief Receiver for message from each activity on intent.
	 */
	private final BroadcastReceiver DataFromActivityReceiver = new BroadcastReceiver() {
		public void onReceive( Context context, Intent intent ){

			String action = intent.getAction();
			if( action.equals( BroadcastCommand.DATA_RECEIVED_FROM_ACTIVITY ) ){

				BroadcastData bData = (BroadcastData)intent.getSerializableExtra(BroadcastData.keyword);

				/* BLE_STATUS_CHECK */
				if(bData.commandID == BroadcastCommand.BLE_STATUS_CHECK){
					BroadCastConnectionStatus();

				/* BLE_SEND_DATA */
				}else if(bData.commandID == BroadcastCommand.BLE_SEND_DATA){
					byte[] send_data = (byte[])bData.data;
					BLE_send_data_set(send_data, false);
				}
			}

		}
	};


	/**@brief Initializing bluetooth function.
	 */
	@SuppressLint("InlinedApi")
	public boolean initialize() {

		if( mBluetoothManager == null ){
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if(mBluetoothManager == null ){
				Log.e(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if( mBluetoothAdapter == null ){
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}

		LocalBroadcastManager.getInstance(this).registerReceiver(DataFromActivityReceiver, makeGattUpdateIntentFilter());

		return true;
	}


	/**@brief Send Broadcast intent.
	 * @param[in] action string of intent action.
	 */
	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		//sendBroadcast(intent);
	}


	private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {

	    final Intent intent = new Intent(action);

		if( TX_CHAR_UUID.equals(characteristic.getUuid()) ){

			if(ble_status == FREE || ble_status == RECEIVING){

				ble_status = RECEIVING;
				final byte[] received = characteristic.getValue();
				boolean result = bleDataHandler.add_data(received);
				if(result==true){

					int packet_status = bleDataHandler.check_packet();
					if(packet_status == BleDataHandler.PACKET_COMPLETE){

						DataPacket dataPacket = bleDataHandler.get_packet();
						if(dataPacket != null){

							BroadcastData bData;
							switch (dataPacket.commandID){

								case BleCommand.ReceivedDataType.SENSOR_RAW_DATA:
									bData = new BroadcastData(BroadcastCommand.BLE_SEND_DATA);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.SENSOR_TEST_DATA:
									bData = new BroadcastData(BroadcastCommand.BLE_SEND_DATA);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.CALIBRATION_END:
									bData = new BroadcastData(BroadcastCommand.CALIBRATION_END);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.STATE_STARTED:
									bData = new BroadcastData(BroadcastCommand.STATE_STARTED);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.STATE_STOPPED:
									bData = new BroadcastData(BroadcastCommand.STATE_STOPPED);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.ERASE_END:
									bData = new BroadcastData(BroadcastCommand.ERASE_END);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.LOGSEND_END:
									bData = new BroadcastData(BroadcastCommand.LOGSEND_END);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.RESPONSE_GEOFENCING_PARAMETER_SUCCESS:
									bData = new BroadcastData(BroadcastCommand.RESPONSE_GEOFENCING_PARAMETER_SUCCESS);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.RESPONSE_GEOFENCING_PARAMETER_FAIL:
									bData = new BroadcastData(BroadcastCommand.RESPONSE_GEOFENCING_PARAMETER_FAIL);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.RESPONSE_GEOFENCING_RESET_SUCCESS:
									bData = new BroadcastData(BroadcastCommand.RESPONSE_GEOFENCING_RESET_SUCCESS);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.RESPONSE_GEOFENCING_RESET_FAIL:
									bData = new BroadcastData(BroadcastCommand.RESPONSE_GEOFENCING_RESET_FAIL);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.SWING_CALI_END:
									bData = new BroadcastData(BroadcastCommand.SWING_CALI_END);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.PULSE_SETTING_DATA:
									bData = new BroadcastData(BroadcastCommand.SETITNG_DATA);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									String test = "dataPacket = ";

									for(int j=0;j<dataPacket.length;j++)
									{
										test = test + String.format("%02x ", dataPacket.data.get(j));
									}
									Log.d("TEST Code",test);

									break;
								case BleCommand.ReceivedDataType.LOG_DETAIL:
									bData = new BroadcastData(BroadcastCommand.LOG_DETAIL);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.CONNECT_TYPE:
									bData = new BroadcastData(BroadcastCommand.CONNECT_TYPE);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;
								case BleCommand.ReceivedDataType.RESULT_ACK:
									bData = new BroadcastData(BroadcastCommand.RESULT_ACK);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.RESULT_NAK:
									bData = new BroadcastData(BroadcastCommand.RESULT_NAK);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.SEND_FW_OK:
									bData = new BroadcastData(BroadcastCommand.SEND_FW_OK);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;

								case BleCommand.ReceivedDataType.SEND_FW_NG:
									bData = new BroadcastData(BroadcastCommand.SEND_FW_NG);
									bData.data = dataPacket;
									intent.putExtra(BroadcastData.keyword, bData);
									LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
									break;
								default:
									break;
							}
						}

						ble_status = FREE;

					}else if(packet_status == BleDataHandler.PACKET_INCOMPLETE){
						;
					}else if(packet_status == BleDataHandler.PACKET_DATA_OVERSIZE){
						ble_status = FREE;
					}
				}else{
					bleDataHandler.clear_packet();
					ble_status = FREE;
				}

			}else if(ble_status == SENDING){

				if(final_packet == true){
					final_packet = false;
					ble_status = FREE;
				}

				final byte[] received = characteristic.getValue();
				if(received.length==1){
					ble_status = FREE;
				}else{
					ble_status = FREE;
				}

			}

		}

	}


	/**@brief Set data into internal buffer for sending the data on BLE.
	 * @param[in] data sending data.
	 */
	private void BLE_send_data_set(byte[] data, boolean retry_status){
		boolean f_data_def = false;
		if(ble_status != FREE || mConnectionState != STATE_CONNECTED) {

			if(sendingStoredData == true){

				if( retry_status == true) {
					//non
				}
				else
				{
					data_queue.add(data);
				}
				return;
			}
			else
			{
				data_queue.add(data);
				start_timer();
			}

			final Intent intent = new Intent(BroadcastCommand.ACTION_BLE_SEND_REQUEST_DENIED);
			BroadcastData bData = new BroadcastData();
			bData.data = null;
			intent.putExtra(BroadcastData.keyword, bData);
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		}else{

			ble_status = SENDING;

			if( data_queue.size() != 0)
			{
				send_data = data_queue.get(0);
				sendingStoredData = false;
			}
			else
			{
				send_data = data;
			}
			packet_counter = 0;
			send_data_pointer = 0;
			first_packet = true;

			BLE_data_send();

			if( data_queue.size() != 0) {
				data_queue.remove(0);
			}

			if( data_queue.size() == 0) {
				if (mTimer != null) {
					mTimer.cancel();
				}
			}
		}

	}

	/**@brief Send data using BLE.
	 */
	private void BLE_data_send(){
		int		err_count = 0;
		int		send_data_pointer_save;
		int		wait_counter;
		boolean	first_packet_save;
		while( final_packet == false )
		{
			byte[] temp_buffer = null;
			send_data_pointer_save = send_data_pointer;
			first_packet_save = first_packet;
			if( first_packet == true )
			{
				if((send_data.length - send_data_pointer) > (SEND_PACKET_SIZE - 3))
				{
					temp_buffer = new byte[SEND_PACKET_SIZE];
					temp_buffer[0] = DATA_RECEIVE;
					temp_buffer[1] = (byte)((send_data.length&(0xFFFF))>>8);
					temp_buffer[2] = (byte)((send_data.length&(0xFF)));

					for(int i=3; i<SEND_PACKET_SIZE; i++)
					{
						temp_buffer[i] = send_data[send_data_pointer];
						send_data_pointer++;
					}
				}
				else
				{
					temp_buffer = new byte[send_data.length - send_data_pointer + 3];
					temp_buffer[0] = DATA_RECEIVE;
					temp_buffer[1] = (byte)((send_data.length&(0xFFFF))>>8);
					temp_buffer[2] = (byte)((send_data.length&(0xFF)));

					for(int i=3; i<temp_buffer.length; i++)
					{
						temp_buffer[i] = send_data[send_data_pointer];
						send_data_pointer++;
					}
					final_packet = true;
				}
				first_packet = false;
			}
			else
			{
				if(send_data.length - send_data_pointer >= SEND_PACKET_SIZE){
					temp_buffer = new byte[SEND_PACKET_SIZE];
					temp_buffer[0] = (byte)packet_counter;
					for(int i=1; i<SEND_PACKET_SIZE; i++){
						temp_buffer[i] = send_data[send_data_pointer];
						send_data_pointer++;
					}
				}else{
					final_packet = true;
					temp_buffer = new byte[send_data.length - send_data_pointer + 1];
					temp_buffer[0] = (byte)packet_counter;
					for(int i=1; i<temp_buffer.length; i++){
						temp_buffer[i] = send_data[send_data_pointer];
						send_data_pointer++;
					}
				}
				packet_counter++;
			}
			packet_send = false;
			boolean status = writeRXCharacteristic(temp_buffer);
			if( (status == false) && (err_count < 3) )
			{
				err_count++;
				//@@@@@
				try{
					Thread.sleep(50); //50[msec]Sleep
				}catch(InterruptedException e){}
				Log.e(TAG, "writeRXCharacteristic false");
				send_data_pointer = send_data_pointer_save;
				first_packet = first_packet_save;
				packet_counter--;
			}
			// Send Wait
			for( wait_counter = 0 ; wait_counter < 5 ; wait_counter++ ) {
				if (packet_send == true) {
					break;
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {}
			}
			//Log.i(TAG, String.format("wait_counter = %d",wait_counter));
		}
		final_packet = false;
		ble_status = FREE;
	}

	public class LocalBinder extends Binder{
		public BleService getService() {
			return BleService.this;
		}
	}


	@Override
	public IBinder onBind(Intent intent){
		return mBinder;
	}


	@Override
	public boolean onUnbind(Intent intent){
		close();
		return super.onUnbind(intent);
	}

	private final IBinder mBinder = new LocalBinder();


	/**@brief connect
	 *	Connects to the GATT server hosted on the Bluetooth LE device.
	 *	@param address The device address of the destination device.
	 *	@return Return true if the connection is initiated successfully. The connection result
	 *		is reported asynchronously through the
	 *		{@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *				callback.
	 */
	public boolean connect(final String address){

		if( mBluetoothAdapter == null || address == null) {
			Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		// Previously connected device.  Try to reconnect.
		if( mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress ) && mBluetoothGatt != null) {
			Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
			if( mBluetoothGatt.connect() ){
				mConnectionState = STATE_CONNECTING;
				return true;
			} else {
				return false;
			}
		}

		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if( device == null ){
			Log.w(TAG, "Device not found.  Unable to connect.");
			return false;
		}

		// We want to directly connect to the device, so we are setting the autoConnect parameter to false.
		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		Log.d(TAG, "Trying to create a new connection.");
		mBluetoothDeviceAddress = address;
		mConnectionState = STATE_CONNECTING;
		return true;
	}

	/**@brief Start Timer.
	 */
	private void start_timer(){
		sendingStoredData = true;
		if(mTimer != null)
		{
			mTimer.cancel();
		}
		mTimer = new Timer(true);
		mTimer.schedule( new TimerTask(){
			@Override
			public void run() {
				timer_Tick();
			}
		}, 100, TIMER_INTERVAL);
	}

	/**@brief Interval timer function.
	 */
	private void timer_Tick(){

		if(data_queue.size() !=0 ){
			sendingStoredData = true;
			BLE_send_data_set(data_queue.get(0), true);
		}

		if(time_out_counter < TIME_OUT_LIMIT){
			time_out_counter++;
		}else{
			ble_status = FREE;
			time_out_counter = 0;
		}
		return;
	}


	/**@brief disconnect
	 * Disconnects an existing connection or cancel a pending connection. The disconnection result
	 * is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect() {

		if( mBluetoothAdapter == null || mBluetoothGatt == null ){
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}

		mBluetoothGatt.disconnect();

	}


	/**@brief close
	 * After using a given BLE device, the app must call this method to ensure resources are
	 * released properly.
	 */
	public void close() {

		if (mBluetoothGatt == null) {
			return;
		}

		Log.w(TAG, "mBluetoothGatt closed");

		mBluetoothDeviceAddress = null;
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}


	/**@brief readCharacteristic
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
	 * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.
	 * BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)} callback.
	 *
	 * @param characteristic The characteristic to read from.
	 */
	public void readCharacteristic( BluetoothGattCharacteristic characteristic ){

		if( mBluetoothAdapter == null || mBluetoothGatt == null ){
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}

		mBluetoothGatt.readCharacteristic(characteristic);
	}


	/**@brief enableTXNotification
	 *
	 */
	@SuppressLint("InlinedApi")
	public void enableTXNotification(){

		BluetoothGattService RxService = mBluetoothGatt.getService( RX_SERVICE_UUID );
		if( RxService == null){
			showMessage("Rx service not found!");
			broadcastUpdate(BroadcastCommand.DEVICE_DOES_NOT_SUPPORT_UART);
			return;
		}

		BluetoothGattCharacteristic TxChar = RxService.getCharacteristic( TX_CHAR_UUID );
		if( TxChar == null ){
			showMessage("Tx charateristic not found!");
			broadcastUpdate(BroadcastCommand.DEVICE_DOES_NOT_SUPPORT_UART);
			return;
		}

		mBluetoothGatt.setCharacteristicNotification( TxChar,true );
		BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		mBluetoothGatt.writeDescriptor(descriptor);
	}


	/**@brief writeRXCharacteristic
	 *
	 */
	public boolean writeRXCharacteristic( byte[] value ){

		BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
		//showMessage("mBluetoothGatt null"+ mBluetoothGatt);
		if( RxService == null ){
			showMessage("Rx service not found!");
			broadcastUpdate(BroadcastCommand.DEVICE_DOES_NOT_SUPPORT_UART);
			return false;
		}

		BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
		if( RxChar == null ){
			showMessage("Rx charateristic not found!");
			broadcastUpdate(BroadcastCommand.DEVICE_DOES_NOT_SUPPORT_UART);
			return false;
		}

		RxChar.setValue(value);
		boolean status = mBluetoothGatt.writeCharacteristic(RxChar);

		//Log.d(TAG, "write TXchar - status=" + status);
		return status;
	}


	/**@brief showMessage
	 *
	 */
	private void showMessage(String msg) {
		Log.e(TAG, msg);
	}


	/**@brief getSupportedGattServices
	 * 	Retrieves a list of supported GATT services on the connected device. This should be
	 * 	invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
	 *
	 * 	@return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices(){
		if (mBluetoothGatt == null) return null;
		return mBluetoothGatt.getServices();
	}


	/**@brief Broadcast connection status of BLE.
	 *
	 */
	private void BroadCastConnectionStatus(){
		final Intent intent = new Intent(BroadcastCommand.ACTION_BLE_STATUS_RESULT);
		BroadcastData bData = new BroadcastData();
		bData.data = mConnectionState;
		intent.putExtra(BroadcastData.keyword, bData);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}


	private static IntentFilter makeGattUpdateIntentFilter(){
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BroadcastCommand.DATA_RECEIVED_FROM_ACTIVITY);
		return intentFilter;
	}
}
