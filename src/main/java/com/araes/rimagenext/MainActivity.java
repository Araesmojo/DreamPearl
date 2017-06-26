package com.araes.rimagenext;

import android.opengl.GLSurfaceView;

import android.app.Activity;
import android.os.Bundle;
import java.io.File;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.text.SimpleDateFormat;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
	// The surface we view
	public GLSurfaceView mGLView;
	public GameLogger GameLog;
	public LogPoster Logger;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		StartGameLogging();
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		String dateStr = df.format( Calendar.getInstance().getTime() );
		Logger.post( "Started logging on " + dateStr );
		Logger.post( "Finished MainActivity creation" );
		
        mGLView = new GameGLSurfaceView(this);
        setContentView(mGLView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
    }
	
	private void StartGameLogging()
	{
		File LogPath = this.getExternalFilesDir("gamelogs");
		LogPath.mkdirs();
		File OutFile = new File( LogPath, "GameLog.txt" );
		//ShowAlert( OutFile.getAbsolutePath() );
		GameLog = new GameLogger( this, OutFile );
		Logger = new LogPoster();
		Logger.addLogListener( GameLog );
	}
	
	private void ShowAlert(String p0)
	{
		AlertDialog.Builder builder1 = new AlertDialog.Builder( this );
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
}

