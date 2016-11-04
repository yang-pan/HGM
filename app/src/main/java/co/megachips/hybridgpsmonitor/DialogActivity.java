/*******************************************************************
 * @file	MainMenu.java
 *
 * @par		Copyright
 *			 (C) 2014-2015 MegaChips Corporation
 *
 * @date	2014/02/26
*******************************************************************/

package co.megachips.hybridgpsmonitor;

import co.megachips.hybridgpsmonitor.ble_service.BleDataHandler;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class DialogActivity extends Activity{
	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			BleDataHandler bleDataHandler = (BleDataHandler)getIntent().getSerializableExtra("dataHandler");

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("BLE message").setMessage(bleDataHandler.dialog_message).setNegativeButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						finish();
					}
				});
			AlertDialog alert = builder.create();
			alert.show();
		}
}
