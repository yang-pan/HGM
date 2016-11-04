/*******************************************************************
 * @file	CustomItemData1.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
*******************************************************************/

package co.megachips.hybridgpsmonitor.sub_activity.custom_view;

import android.graphics.Bitmap;

/**@brief Item for customized ListView
 */
public class CustomItemData1 {

	private Bitmap imageData_;
	private String textData_;

	public void setImagaData(Bitmap image) {
		imageData_ = image;
	}

	public Bitmap getImageData() {
		return imageData_;
	}

	public void setTextData(String text) {
		textData_ = text;
	}

	public String getTextData() {
		return textData_;
	}

}
