/*******************************************************************
 * @file	MainMenu.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
*******************************************************************/

package co.megachips.hybridgpsmonitor;

import java.io.Serializable;

public class BroadcastData implements Serializable{

	private static final long serialVersionUID = 1L;
	public static final String keyword = "command";
	public int commandID = 0;
	public Object data = null;

	public BroadcastData(){
		return;
	}

	public BroadcastData(int id){
		commandID = id;
		return;
	}

	public BroadcastData(int id, Object obj){
		commandID = id;
		data = obj;
		return;
	}

}
