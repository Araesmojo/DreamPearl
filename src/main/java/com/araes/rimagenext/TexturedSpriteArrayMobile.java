package com.araes.rimagenext;

import android.opengl.Matrix;
import android.opengl.*;

public class TexturedSpriteArrayMobile extends TexturedSpriteArray
{
	Vec3 mPos = new Vec3( 0.0f, 0.0f, 0.0f );
	float mMoveRate = 5.0f;
	
	int Dirs = 4;
	boolean[] mButtonStates = new boolean[]{ false, false, false, false, false, false, false, false  };
	
	ActionStack stackMove = new ActionStack();
	ActionStack stackAction = new ActionStack();
	
	/**
	 * Initialize the class object.
	 */
	public TexturedSpriteArrayMobile( MainActivity activityContext, GameRenderer Renderer )
	{
		super( activityContext, Renderer );
	}

	@Override
	public void BuildSpriteArray()
	{
		super.BuildSpriteArray();
	}
	
	public void MoveTo( Vec3 pos ){
		Logger.post( "Entered MoveTo with pos " + Float.toString(pos.gX()) + ", " + Float.toString(pos.gY()) + ", " + Float.toString(pos.gZ()) );
		mPos = pos;
		Matrix.setIdentityM(mModelMatrix,0);
		Matrix.translateM(mModelMatrix, 0, pos.gX(), pos.gY(), pos.gZ() );
	}
	
	public void processMoves(){
		//Logger.post( "Starting mobile array move" );
		if( !stackMove.isEmpty() ){
			Logger.post( "Found non-empty move stack-process last move" );
			Vec3 Delta = stackMove.pop().Move;
			stackMove.clear();
			Logger.post( "Found move " + Float.toString( Delta.gX() ) + ", " + Float.toString( Delta.gY() ) );
			mPos.sX( mPos.gX() + Delta.gX() );
			mPos.sY( mPos.gY() + Delta.gY() );
			mPos.sZ( mPos.gZ() + Delta.gZ() );
			Matrix.translateM(mModelMatrix, 0, Delta.gX(), Delta.gY(), Delta.gZ() );
		}
		//Logger.post( "Finished move" );
	}
	
	public void processActions(){
		int length = stackAction.length();
		String names = "";
		for( int i = 0; i < length; i++ ){
			names = names + " " + stackAction.pop().mName;
		}
		//Logger.post( "Found " + Integer.toString(length) + " button presses: " + names );
	}
	
	/*
	public float[] buildMoveDelta(){
		//Logger.post( "Starting buildMoveDelta" );
		float[] deltaOut = new float[]{ 0.0f, 0.0f, 0.0f };
		
		// If we have started having successful render timesteps
		if( mRenderer.mTimeLast != 0 && mRenderer.mTimeDelta < 1000 ){
			int deltaInd = 0;
			boolean isMoving = false;
			for( int i = 0; i < 4; i++ ){
				if( mButtonStates[i] ){
					deltaOut[0] += mDirDeltas[deltaInd  ];
					deltaOut[1] += mDirDeltas[deltaInd+1];
					Logger.post( "Successful move requested posted" );
					isMoving = true;
				}
				mButtonStates[i] = false;
				deltaInd += 2;
			}
			if( isMoving ){
				deltaOut[0] *= mRenderer.mTimeDelta * mMoveRate;
				deltaOut[1] *= mRenderer.mTimeDelta * mMoveRate;
			}
		}
		
		//Logger.post( "Finished buildMoveDelta" );
		return deltaOut;
	}
	*/
	public void processButtonInput( BBoxAction act ){
		if( act.isMove ){
			Logger.post( "Trying to add action " + act.mName + " to the stack" );
			stackMove.push( act );
		} else {
			stackAction.push( act );
		}
		Logger.post( "TexSpriteArrayMobile added action " + act.mName + " to the stack" );
	}

	@Override
	public void update()
	{
		//Logger.post( "Starting mobile array update" );
		super.update();
		//Logger.post( "Finished update" );
	}

	@Override
	protected void BuildMVPMatrices()
	{
		// This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
		Matrix.setIdentityM(mMVPMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mRenderer.mViewMatrix, 0, mModelMatrix, 0);   

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);                

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mRenderer.mProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
	}
	
	
}
