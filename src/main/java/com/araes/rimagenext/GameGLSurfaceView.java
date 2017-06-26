package com.araes.rimagenext;

import android.opengl.GLSurfaceView;
import android.content.Context;
import android.view.MotionEvent;
import android.opengl.Matrix;

public class GameGLSurfaceView extends GLSurfaceView {
	private final MainActivity mActivityContext;
	public LogPoster Logger;
	
    public GameGLSurfaceView(MainActivity context) {
        super(context);
		mActivityContext = context;
		
		setEGLContextClientVersion(2);
        mRenderer = new GameRenderer( context );
        setRenderer(mRenderer);
		//setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		Logger = new LogPoster();
		Logger.addLogListener( mActivityContext.GameLog );
		Logger.post( "Finished GameGLSurfaceView creation" );
    }
	
	private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
	private float mPreviousX = 0.0f;
	private float mPreviousY = 0.0f;

    public boolean onTouchEvent(final MotionEvent e) {
		float x = e.getX();
		float y = e.getY();
		
        /*queueEvent(new Runnable(){
				public void run() {
					mRenderer.setColor(e.getX() / getWidth(),
									   e.getY() / getHeight(), 1.0f);
				}});*/

		switch (e.getAction()) {
			case MotionEvent.ACTION_MOVE:

				if( mPreviousX != 0.0f ){
					float dx = x - mPreviousX;
					float dy = y - mPreviousY;

					Vec3 rotAxis = new Vec3( dy,dx, 0 ).norm();
					float[] rAxis = new float[]{ rotAxis.gX(), rotAxis.gY(), rotAxis.gZ(), 1 };
					float rotLen = (float)(Math.sqrt( dx * dx + dy * dy ) );
					float[] rotation = new float[16];
					float[] model    = new float[16];
					
					for( int i = 0; i < 16; i++ ){
						model[i] = mRenderer.mWorld.layerByName.get("Ground").mModelMatrix[i];
					}
					
					float[] modelInv = new float[16];
					Matrix.invertM( modelInv, 0, model, 0 );
					
					Matrix.multiplyMV( rAxis, 0, modelInv, 0, rAxis, 0 );
					rotAxis.sX( rAxis[0] );
					rotAxis.sY( rAxis[1] );
					rotAxis.sZ( rAxis[2] );
					//Logger.post( "rotAxis " + rotAxis.toS() );
					//Logger.post( "rotLen " + rotLen );
					if( rotLen > 0 ){
						//Matrix.setRotateM( rotation, 0, rotLen, rotAxis.gY(), rotAxis.gX(), rotAxis.gZ() );
						//Matrix.multiplyMM( model, 0, rotation, 0, model, 0 );
						Matrix.rotateM( model, 0, rotLen, rotAxis.gX(), rotAxis.gY(), rotAxis.gZ() );
						mRenderer.mWorld.layerByName.get("Ground").mModelMatrix = model;
						//Matrix.setRotateM( rotation, 0, rotLen, rotAxis.gX(), rotAxis.gY(), rotAxis.gZ() );
						//Matrix.multiplyMM( model, 0, rotation, 0, model, 0 );
					}
					// reverse direction of rotation above the mid-line
					if (y > getHeight() / 2) {
						dx = dx * -1 ;
					}

					// reverse direction of rotation to left of the mid-line
					if (x < getWidth() / 2) {
						dy = dy * -1 ;
					}

					/*mRenderer.setAngle(
					 mRenderer.getAngle() +
					 ((dx + dy) * TOUCH_SCALE_FACTOR));*/
				}
		}
		
		//Logger.post( "Responded to touch event at " + Float.toString(x) + ", " + Float.toString(y) );

		mPreviousX = x;
		mPreviousY = y;
		return true;
	}

	GameRenderer mRenderer;
}
