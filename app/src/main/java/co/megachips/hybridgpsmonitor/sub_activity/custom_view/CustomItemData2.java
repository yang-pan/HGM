/*******************************************************************
 * @file	CustomItemData2.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
*******************************************************************/

package co.megachips.hybridgpsmonitor.sub_activity.custom_view;

/**@brief Item for customized ListView
 */
public class CustomItemData2 {

	public int interval = 0;
	private String textData1_;
	private String textData2_;

	public void setTextData1(String text) {
		textData1_ = text;
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
