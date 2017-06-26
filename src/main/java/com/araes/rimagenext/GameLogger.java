package com.araes.rimagenext;

import android.content.Context;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;

public class GameLogger implements LogListener
{
	Context mActivityContext;
	FileOutputStream OutputStream;
	OutputStreamWriter osw;
	
	public GameLogger( Context activityContext, File outputFile )
	{
		mActivityContext = activityContext;
		openOutputFile( outputFile ); 
	}

	private void openOutputFile( File outputFile )
	{
		try {
			OutputStream = new FileOutputStream( outputFile );
			osw = new OutputStreamWriter( OutputStream );
			osw.write( "opened output file" );
			osw.flush();
		}
		catch (IOException e){
			ShowAlert( "Could not open outputfile" );
		} 
	}
	
	public void print( String data ){
		if( isExternalStorageWritable() ){
			try { osw.write( data ); osw.flush(); }
			catch(IOException e){}
		}
	}
	
	private void closeOutputFile(String outputFile)
	{
		try { OutputStream.close(); }
		catch (IOException e){}
	}

	@Override
	public void onLogEvent(LogEvent e)
	{
		print( e.getData() );
	}

	private void ShowAlert(String p0)
	{
		AlertDialog.Builder builder1 = new AlertDialog.Builder( mActivityContext );
		builder1.setMessage( p0 );
		builder1.setCancelable(true);

		builder1.setPositiveButton( "Yes",
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});

		builder1.setNegativeButton( "No",
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});

		AlertDialog alert11 = builder1.create();
		alert11.show();
	}
	
	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	/* Checks if external storage is available to at least read */
	public boolean isExternalStorageReadable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state) ||
			Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return true;
		}
		return false;
	}
}

