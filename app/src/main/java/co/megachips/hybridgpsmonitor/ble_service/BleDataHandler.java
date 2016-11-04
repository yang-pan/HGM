/*******************************************************************
 * @file	BleDataHandler.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
*******************************************************************/

package co.megachips.hybridgpsmonitor.ble_service;

import java.io.Serializable;
import java.util.Timer;

import co.megachips.hybridgpsmonitor.DataPacket;

public class BleDataHandler implements Serializable{

	private static final long serialVersionUID = 1L;

	public String dialog_message = "";

	private DataPacket dataPacket;
	private Timer mTimer;										// Timer for interval timer
	private int timeoutCounter = 0;
	private int packetCounter = 0;
	private static final int TIMER_INTERVAL		= 500;			// Internal time(msec)
	private static final int TIMER_START_DELAY	= 500;			// Timer will start after TIMER_START_DELAY(msec)
	private static final int TIMEOUT_COUNT = 20;				// Interval timer reaches this VALUE then timeout event occurs.

	public static final int PACKET_COMPLETE = 0;
	public static final int PACKET_INCOMPLETE = 1;
	public static final int PACKET_DATA_OVERSIZE = 2;

	public BleDataHandler(){
/*
		mTimer = new Timer(true);
		mTimer.schedule( new TimerTask(){
			@Override
			public void run() {
				timerTick();
			}
	    }, TIMER_START_DELAY, TIMER_INTERVAL );
*/
		return;

	}


	/**@brief Interval timer function.
	 */
	private void timerTick(){
		timeoutCounter++;
		if(timeoutCounter > TIMEOUT_COUNT){
			//timeout counter reaches TIMEOUT value.
			//clear_packet();
		}
		return;
	}


	/**@brief Add data received from BLE interruption.
	 */
	public boolean add_data(byte[] received){

		timeoutCounter = 0;

		if(dataPacket == null){
				int id	= (int)received[0];
				byte b1 = received[1];
				byte b2 = received[2];
				int length = (int)( ( ((int)b1<<8) & 0xFF00 ) | (((int)b2)& 0xFF) );
				dataPacket = new DataPacket(id, length);
				for(int i=3; i<received.length; i++){
					dataPacket.add(received[i]);
				}
				return true;

		}else{

			if(packetCounter == (int)received[0]){
				packetCounter++;
				for(int i=1; i<received.length; i++){
					dataPacket.add(received[i]);
				}
				return true;
			}else{
				clear_packet();
				return false;
			}

		}

	}


	/**@brief Clear internal data buffer.
	 */
	public void clear_packet(){
		dataPacket = null;
		packetCounter = 0;
	}


	/**@brief Check received data packet completion.
	 */
	public int check_packet(){

		if(dataPacket.length == dataPacket.data.size()){
			return PACKET_COMPLETE;
		}else if(dataPacket.length > dataPacket.data.size()){
			return PACKET_INCOMPLETE;
		}else{
			clear_packet();
			return PACKET_DATA_OVERSIZE;
		}

	}


	/**@brief Get data as DataPacket.
	 */
	public DataPacket get_packet(){
		DataPacket temp_packet = dataPacket;
		clear_packet();
		return temp_packet;
	}

}
