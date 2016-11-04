/*******************************************************************
 * @file	CustomAdapter2.java
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
import android.widget.TextView;
import co.megachips.hybridgpsmonitor.R;

/**@brief Customized ArrayAdapter for customized ListView
 */
public class CustomAdapter3 extends ArrayAdapter<CustomItemData3>{

	private LayoutInflater layoutInflater_;

	public CustomAdapter3(Context context, int textViewResourceId, List<CustomItemData3> objects) {
		super(context, textViewResourceId, objects);
		layoutInflater_ = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		CustomItemData3 item = (CustomItemData3)getItem(position);

		if (null == convertView) {
			convertView = layoutInflater_.inflate(R.layout.menu_item3, null);
		}

		TextView textView1;
		textView1 = (TextView)convertView.findViewById(R.id.item3_text1);
		textView1.setText(item.getTextData1());

		TextView textView2;
		textView2 = (TextView)convertView.findViewById(R.id.item3_text2);
		textView2.setText(item.getTextData2());

		return convertView;

	}

}
