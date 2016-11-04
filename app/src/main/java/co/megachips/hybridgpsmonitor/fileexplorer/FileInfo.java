package co.megachips.hybridgpsmonitor.fileexplorer;

import java.io.File;

public class FileInfo
		implements Comparable<FileInfo>
{
	private String	m_strName;	// title
	private File	m_file;	// file object

	// contractor
	public FileInfo(	String strName,
						File file )
	{
		m_strName = strName;
		m_file = file;
	}

	public String getName()
	{
		return m_strName;
	}

	public File getFile()
	{
		return m_file;
	}

	// comparison
	public int compareTo( FileInfo another )
	{
		// Directory < order of files
		if( true == m_file.isDirectory() && false == another.getFile().isDirectory() )
		{
			return -1;
		}
		if( false == m_file.isDirectory() && true == another.getFile().isDirectory() )
		{
			return 1;
		}

		// In each other files of directories, order of file name (directory name) without difference between Uppercase and Lowercase
		return m_file.getName().toLowerCase().compareTo( another.getFile().getName().toLowerCase() );
	}
}
