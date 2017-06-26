package com.araes.rimagenext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.util.logging.*;
import java.io.IOException;
import android.view.*;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class TexturedSpriteArray extends TexturedSprite
{	
	/** Used for debug logs. */
	private static final String TAG = "TexturedSpriteArray";
	public LogPoster Logger;
	
	private TexturedSprite[] m_arrSprites = null;
	
	public float[] SpritePositionData = null;
	public float[] SpriteNormalData = null;
	public float[] SpriteColorData = null;
	public float[] SpriteTexCoorData = null;
	
	public boolean boolDirty = false;
	
	public int arrW = 0;
	public int arrH = 0;

	//private Logger logger = Logger.getLogger("MyLog");  
	
	//private float[] SpritePositionData;
	
	/**
	 * Initialize the class object
	 */
	public TexturedSpriteArray( MainActivity activityContext, GameRenderer Renderer )
	{
		super( activityContext, Renderer );

		Logger = new LogPoster();
		Logger.addLogListener( mActivityContext.GameLog );
		Logger.post( "Finished TexturedSprite creation" );
	}

	public void LogMotionEvent(MotionEvent e)
	{
		// TODO: Implement this method
	}
	
	public TexturedSprite[] getSprites(){
		return m_arrSprites;
	}
	
	public void BuildSpriteArray()
	{
		Logger.post("Entered TexturedSpriteArray BuildSpriteArray");
		TexturedSprite[] tSprites = PlaceSprites();
		
		Logger.post( "tSprites has " + Integer.toString(tSprites.length) + " entries" );
		Logger.post( "tSprites[0].SpritePositionData[0] has value of " + Float.toString(tSprites[0].SpritePositionData[0]) );
		Logger.post( "tSprites[0].SpritePositionData.length has value of " + Integer.toString(tSprites[0].SpritePositionData.length) );
		
		AppendSpritesToBulkMesh( tSprites );
		
		Logger.post( "SpritePositionData.length has value of " + Integer.toString(SpritePositionData.length) );
		
		if( mPositions != null ){
			Logger.post( "mPositions has value of " + mPositions );
		} else {
			Logger.post( "mPositions is null" );
		}
		
		FloatBuffer[] arrFB;
		arrFB = InitializeBuffers( SpritePositionData, SpriteColorData, SpriteNormalData, SpriteTexCoorData,
								  mPositions, mColors, mNormals, mTextureCoordinates );
		mPositions          = arrFB[0];
		mColors             = arrFB[1];
		mNormals            = arrFB[2];
		mTextureCoordinates = arrFB[3];		
		
		Logger.post( "SpritePositionData.length has value of " + Integer.toString(SpritePositionData.length) );
		if( mPositions != null ){
			Logger.post( "mPositions has value of " + mPositions );
		} else {
			Logger.post( "mPositions is null" );
		}
		
		mTris = SpritePositionData.length / mPositionDataSize;
		
		boolDirty = false;
		mVisible = true;
		
		Logger.post("Finished TexturedSpriteArray BuildSpriteArray");
	}
	
	protected TexturedSprite[] PlaceSprites()
	{
		int viewWidth = mRenderer.mWidth;
		int viewHeight = mRenderer.mHeight;

		int SpriteSize = 16;

		arrW = viewWidth / SpriteSize + 2;
		if( arrW % 2 != 0 ){ arrW++; }
		arrH = viewHeight / SpriteSize + 2;
		if( arrH % 2 != 0 ){ arrH++; }
		arrW = 6;
		arrH = 6;
		
		Tile[][] arrTemp = new Tile[ arrW ][ arrH ];
		Tile[] arrOut = new Tile[ arrW * arrH ];
		
		float xPosStart = -arrW / 2 + 0.5f;
		float yPosStart = -arrH / 2 + 0.5f;
		
		int a = 0;
		float zDelta=  0.0f;
		float xDelta = xPosStart;
		for( int i = 0; i < arrW; i++ ){
			float yDelta = yPosStart;
			for( int j = 0; j < arrH; j++ ){
				arrTemp[i][j] = ChooseTile( arrTemp, i, j, arrW, arrH );
				arrOut[a] = arrTemp[i][j];
				TranslateVec3Array( arrOut[a].SpritePositionData, xDelta, yDelta, zDelta );
				yDelta += 1.0f;
				a++;
			}
			xDelta += 1.0f;
		}
		int aLen = arrOut.length;
		Logger.post( "Created arrOut with " + Integer.toString(aLen) + " entries" );
		return arrOut;
	}

	protected Tile ChooseTile(Tile[][] arrOut, int i, int j, int arrW, int arrH)
	{
		return new Tile( mActivityContext, mRenderer, true );
	}

	protected void AppendSpritesToBulkMesh(TexturedSprite[] Sprites )
	{
		SpritePositionData = new float[0];
		SpriteNormalData = new float[0];
		SpriteColorData = new float[0];
		SpriteTexCoorData = new float[0];
		
		int w = Sprites.length;
		Logger.post( "Data in Sprites[0].SpritePositionData[0-5] is "
					+ Float.toString( Sprites[0].SpritePositionData[0] ) + ","
					+ Float.toString( Sprites[0].SpritePositionData[1] ) + ","
					+ Float.toString( Sprites[0].SpritePositionData[2] ) + ","
					+ Float.toString( Sprites[0].SpritePositionData[3] ) + ","
					+ Float.toString( Sprites[0].SpritePositionData[4] ) + ","
					+ Float.toString( Sprites[0].SpritePositionData[5] ) );
		for( int i = 0; i < w; i++ ){
				Logger.post( "Appending sprite data from sprite " + Integer.toString(i) );
				SpritePositionData = GameUtils.ConcatFloatArrays( SpritePositionData, Sprites[i].SpritePositionData );
				SpriteNormalData   = GameUtils.ConcatFloatArrays( SpriteNormalData, Sprites[i].SpriteNormalData );
				SpriteColorData    = GameUtils.ConcatFloatArrays( SpriteColorData, Sprites[i].SpriteColorData );
				SpriteTexCoorData  = GameUtils.ConcatFloatArrays( SpriteTexCoorData, Sprites[i].SpriteTexCoorData );
		}

		int pLen = SpritePositionData.length;
		Logger.post( "Created new SpritePositionData with " + Integer.toString(pLen) + " entries" );
		Logger.post( "Data in SpritePositionData[0-5] is "
					+ Float.toString( SpritePositionData[0] ) + ","
					+ Float.toString( SpritePositionData[1] ) + ","
					+ Float.toString( SpritePositionData[2] ) + ","
					+ Float.toString( SpritePositionData[3] ) + ","
					+ Float.toString( SpritePositionData[4] ) + ","
					+ Float.toString( SpritePositionData[5] ) );
	}

	protected void TranslateVec3Array(float[] arr, float xDelta, float yDelta, float zDelta)
	{
		//float[] arrOut = new float[ arr.length];
		//System.arraycopy(arr, 0, arrOut, 0, arr.length);
		
		int Len = arr.length / 3;
		Logger.post( "arr has " + Integer.toString(Len) + " entries" );
		int j = 0;
		for( int i = 0; i < Len; i++ ){
			arr[ j ]   += xDelta;
			arr[ j+1 ] += yDelta;
			arr[ j+2 ] += zDelta;
			j += 3;
		}
	}

	@Override
	public void update()
	{
		if( boolDirty ){
			BuildSpriteArray();
		}
		
		setLightPos( mRenderer.getLightPos() );
		
		//mSprite.setLightPos( mRenderer.getLightPos() );		
		//m_arrSprites[0].setLightPos( mRenderer.getLightPos() );
		
		/*int ind = 0;
		for( int i=0; i < arrW; i++ ){
			for( int j=0; j < arrH; j++ ){
				m_arrSprites[ind++].setLightPos( mRenderer.getLightPos() );
			}
		}*/
	}
}
