package co.megachips.hybridgpsmonitor.fileexplorer;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class FileSelectionActivity extends Activity
		implements OnItemClickListener, OnClickListener
{
	// layout parameter
	private static final int		WC					= LinearLayout.LayoutParams.WRAP_CONTENT;
	private static final int		FP					= LinearLayout.LayoutParams.FILL_PARENT;
	// botton tag
	private static final int		BUTTONTAG_CANCEL	= 0;

	private ListView				m_listview;		// List view
	private FileInfoArrayAdapter	m_fileinfoarrayadapter;			// Array adapter of file infomation
	private String[]				m_astrExt;						// Filter extention array

	// In case of activity start, onCreate is called
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		// initialization of return value
		setResult( Activity.RESULT_CANCELED );

		// Get parameter from the caller
		String strInitialDir = null;
		String strExt = null;
		Bundle extras = getIntent().getExtras();
		if( null != extras )
		{
			strInitialDir = extras.getString( "initialdir" );
			strExt = extras.getString( "ext" );
		}
		// Initializing the folder
		if( null == strInitialDir || false == new File( strInitialDir ).isDirectory() )
		{
			strInitialDir = "/";
		}
		// filter of filename extension
		if( null != strExt )
		{
			StringTokenizer tokenizer = new StringTokenizer( strExt, "; " );
			int iCountToken = 0;
			while( tokenizer.hasMoreTokens() )
			{
				tokenizer.nextToken();
				iCountToken++;
			}
			if( 0 != iCountToken )
			{
				m_astrExt = new String[iCountToken];
				tokenizer = new StringTokenizer( strExt, "; " );
				iCountToken = 0;
				while( tokenizer.hasMoreTokens() )
				{
					m_astrExt[iCountToken] = tokenizer.nextToken();
					iCountToken++;
				}
			}
		}

		// layout
		LinearLayout layout = new LinearLayout( this );
		layout.setOrientation( LinearLayout.VERTICAL );
		setContentView( layout );

		// listview
		m_listview = new ListView( this );
		m_listview.setScrollingCacheEnabled( false );
		m_listview.setOnItemClickListener( this );
		LinearLayout.LayoutParams layoutparams = new LinearLayout.LayoutParams( FP, 0 );
		layoutparams.weight = 1;
		m_listview.setLayoutParams( layoutparams );
		layout.addView( m_listview );

		// button
		Button button = new Button( this );
		button.setText( "Cancel" );
		button.setTag( BUTTONTAG_CANCEL );
		button.setOnClickListener( this );
		layoutparams = new LinearLayout.LayoutParams( FP, WC );
		button.setLayoutParams( layoutparams );
		button.setPadding( 10, 10, 10, 10 );
		layout.addView( button );

		fill( new File( strInitialDir ) );
	}

	// Make contents on the activity
	private void fill( File fileDirectory )
	{
		// title
		setTitle( fileDirectory.getAbsolutePath() );

		// file list
		File[] aFile = fileDirectory.listFiles( getFileFilter() );
		List<FileInfo> listFileInfo = new ArrayList<FileInfo>();
		if( null != aFile )
		{
			for( File fileTemp : aFile )
			{
				listFileInfo.add( new FileInfo( fileTemp.getName(), fileTemp ) );
			}
			Collections.sort( listFileInfo );
		}
		// Add peth to back to parent folder
		if( null != fileDirectory.getParent() )
		{
			listFileInfo.add( 0, new FileInfo( "..", new File( fileDirectory.getParent() ) ) );
		}

		m_fileinfoarrayadapter = new FileInfoArrayAdapter( this, listFileInfo );
		m_listview.setAdapter( m_fileinfoarrayadapter );
	}

	// Make FileFilter's object
	private FileFilter getFileFilter()
	{
		return new FileFilter()
		{
			public boolean accept( File arg0 )
			{
				if( null == m_astrExt )
				{ // not filtering
					return true;
				}
				if( arg0.isDirectory() )
				{ // In case of directory, it's true
					return true;
				}
				for( String strTemp : m_astrExt )
				{
					if( arg0.getName().toLowerCase().endsWith( "." + strTemp ) )
					{
						return true;
					}
				}
				return false;
			}
		};
	}

	// management of clicking contents on the ListView
	public void onItemClick(	AdapterView<?> l,
								View v,
								int position,
								long id )
	{
		FileInfo fileinfo = m_fileinfoarrayadapter.getItem( position );

		if( true == fileinfo.getFile().isDirectory() )
		{
			fill( fileinfo.getFile() );
		}
		else
		{
			// Set parameter to caller
			Intent intent = new Intent();
			intent.putExtra( "file", fileinfo.getFile() );
			// Set return value of activity
			setResult( Activity.RESULT_OK, intent );

			// End activity
			finish();
		}
	}

	// management of clicking button
	public void onClick( View arg0 )
	{
		final int iTag = (Integer)arg0.getTag();
		switch( iTag )
		{
		case BUTTONTAG_CANCEL:
			// End activity
			finish();
			break;
		}
	}
}
