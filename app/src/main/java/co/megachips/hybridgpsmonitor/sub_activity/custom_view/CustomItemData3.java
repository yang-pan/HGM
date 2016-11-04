/*******************************************************************
 * @file	CustomItemData3.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
*******************************************************************/

package co.megachips.hybridgpsmonitor.sub_activity.custom_view;

/**@brief Item for customized ListView
 */
public class CustomItemData3 {

	public String sensor_id = "00";
	public int interval = 0;
	private String textData1_;
	private String textData2_;

	public void setInterval_sensor_id(String i){
		sensor_id = i;
		if(i == "00")
		{
			textData1_ ="Gyro/Accel/Magnet Sensor";
		} else if (i == "01")
		{
			textData1_ ="Magnet Calib/DR Sensor";
		} else if (i == "02")
		{
			textData1_ ="Gravity/Rot_vec";
		} else if(i == "FF")
		{
			textData1_ ="PPG";
		}
		else
		{
			textData1_ ="0x" +  sensor_id;
		}
	}
	public String getTextData1() {
		return textData1_;
	}

	public void setInterval(int i){
		interval = i;
		textData2_ = Integer.toString(interval) + "ms";
	}

	public String getTextData2() {
		return textData2_;
	}

}
