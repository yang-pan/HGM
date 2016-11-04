/*******************************************************************
 * @file	CustomAdapter1.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
*******************************************************************/

package co.megachips.hybridgpsmonitor.sub_activity.custom_view;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import co.megachips.hybridgpsmonitor.R;

/**@brief Customized ArrayAdapter for customized ListView
 */
public class CustomAdapter1 extends ArrayAdapter<CustomItemData1> {

	private LayoutInflater layoutInflater_;
	private int mTextColor;
	private int mBgColor;

	public CustomAdapter1(Context context, int textViewResourceId, List<CustomItemData1> objects, int textColor, int bgColor ) {
		super(context, textViewResourceId, objects);
		layoutInflater_ = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mTextColor = textColor;
		mBgColor = bgColor;
	}

	public void setTextColor( int bg, int fg ) {
		mTextColor = fg;
		mBgColor = bg;
	}
		
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		CustomItemData1 item = (CustomItemData1)getItem(position);

		if (null == convertView) {
			 convertView = layoutInflater_.inflate(R.layout.menu_item2, null);
		}

		ImageView imageView;
		imageView = (ImageView)convertView.findViewById(R.id.item2_image);
		imageView.setImageBitmap(item.getImageData());

		TextView textView;
		textView = (TextView)convertView.findViewById(R.id.item2_text);
		textView.setText(item.getTextData());

		return convertView;

	}

}
