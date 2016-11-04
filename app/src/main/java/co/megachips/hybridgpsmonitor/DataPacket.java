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
import java.util.ArrayList;

public class DataPacket implements Serializable{

	private static final long serialVersionUID = 1L;

	public int commandID;
	public int length;
	public ArrayList<Byte> data = new ArrayList<Byte>();

	public DataPacket(int id, int len) {
		commandID = id;
		length = len;
	}

	public void add(byte b) {
		data.add(b);
	}

}
