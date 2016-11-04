package co.megachips.hybridgpsmonitor.fileexplorer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class FileRW {
	
	OutputStreamWriter logFile;
	BufferedReader reader;
	
	public void open(String fileName) {
		
		File file = new File(fileName);
		
		if(file.exists() == false) {
			if(logFile == null) {
				try {
					try {
						logFile =  new OutputStreamWriter(
								new FileOutputStream(fileName), "UTF-8");
				 		} catch (UnsupportedEncodingException e) {
				 			// TODO Auto-generated catch block
				 			e.printStackTrace();
				 		}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (logFile != null) {
				//Log.d("debug", "not null");
			}
		}else {
			FileInputStream in;
			try {
				in = new FileInputStream(fileName);
				reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void writePdrData(long timestamp, float x, float y) {
		if(logFile != null) {
    		try {
    			logFile.write(timestamp + " ");
    			logFile.write(x + " ");
				logFile.write(y  + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
	}
	
	public void writeGPSData(long timestamp, double lon, double lat, int satelliteNum) {
		if(logFile != null) {
    		try {
    			logFile.write(timestamp + " ");
    			logFile.write(lon + " ");
				logFile.write(lat  + " ");
				logFile.write(satelliteNum + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
	}
	
	public void writeNmea(long timestamp, String str) {
		if(logFile != null) {
    		try {
    			logFile.write(timestamp + " ");
				logFile.write(str  + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
	}
	
	
	public void writeRoute(int lon, int lat) {
		if(logFile != null) {
    		try {
    			logFile.write(lon + " ");
				logFile.write(lat  + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
	}
	
	public String readStr() {
		String str ="";
		
		 try {
				str = reader.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 
		 if(str == null) {
			 return null;
		 }
		 
		return str;
	}
	
	public void close() {
		if(logFile != null) {
			try {
				logFile.flush();
				logFile.close();		
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		logFile = null;
	}
	
}