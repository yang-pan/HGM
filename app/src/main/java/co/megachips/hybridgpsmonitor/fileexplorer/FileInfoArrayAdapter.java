package co.megachips.hybridgpsmonitor.fileexplorer;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FileInfoArrayAdapter extends ArrayAdapter<FileInfo>
{
	private List<FileInfo>	m_listFileInfo; // list of file infomation
	// constructor
	public FileInfoArrayAdapter(	Context context,
									List<FileInfo> objects )
	{
		super( context, -1, objects );

		m_listFileInfo = objects;
	}

	// Get part of m_listFileInfo
	@Override
	public FileInfo getItem( int position )
	{
		return m_listFileInfo.get( position );
	}

	// Make part of view
	@Override
	public View getView(	int position,
							View convertView,
							ViewGroup parent )
	{
		// Make layout
		if( null == convertView )
		{
			Context context = getContext();
			// layout
			LinearLayout layout = new LinearLayout( context );
			layout.setPadding( 10, 10, 10, 10 );
			layout.setBackgroundColor( Color.WHITE );
			convertView = layout;
			// text
			TextView textview = new TextView( context );
			textview.setTag( "text" );
			textview.setTextColor( Color.BLACK );
			textview.setPadding( 10, 10, 10, 10 );
			layout.addView( textview );
		}

		// Set value
		FileInfo fileinfo = m_listFileInfo.get( position );
		TextView textview = (TextView)convertView.findViewWithTag( "text" );
		if( fileinfo.getFile().isDirectory() )
		{ // In cese of directory, set "/"
			textview.setText( fileinfo.getName() + "/" );
		}
		else
		{
			textview.setText( fileinfo.getName() );
		}

		return convertView;
	}
}
