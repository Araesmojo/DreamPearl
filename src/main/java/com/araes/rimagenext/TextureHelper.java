package com.araes.rimagenext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.graphics.Rect;

public class TextureHelper
{
	public static int loadTexture(final MainActivity context, final int resourceId)
	{
		final Bitmap bitmap = LoadBitmapFromResource( context, resourceId );
		return createTexture( context, bitmap );
	}
	
	public static int createTextureFromLetters(final MainActivity context, String[] letters, int textSize, TexUV[] texUV )
	{
		final Bitmap bitmap = RenderLettersToBitmap( context, letters, textSize, texUV );
		return createTexture( context, bitmap );
	}
	
	public static int createTexture(final MainActivity context, final Bitmap bitmap )
	{
		final int[] texHandle = new int[1];

		GLES20.glGenTextures(1, texHandle, 0);

		if (texHandle[0] != 0){ BitmapToTexture( bitmap, texHandle ); }
		else                  { throw new RuntimeException("Error loading texture."); }

		return texHandle[0];
	}

	public static Bitmap LoadBitmapFromResource( final MainActivity context, int resourceId)
	{
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;	// No pre-scaling

		// Read in the resource
		final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
		
		return bitmap;
	}

	private static void BitmapToTexture(Bitmap bitmap, int[] textureHandle)
	{
		// Bind to the texture in OpenGL
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

		// Set filtering
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

		// Load the bitmap into the bound texture.
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

		// Recycle the bitmap, since its data has been loaded into OpenGLES20.
		bitmap.recycle();			
	}
	
	public static Bitmap RenderLettersToBitmap( MainActivity context, String[] letters, int textSize, TexUV[] texUV )
	{
		LogPoster Logger = new LogPoster();
		Logger = new LogPoster();
		Logger.addLogListener( context.GameLog );
		Logger.post( "Entered RenderLettersToTexture" );
		
		// Setup the painter
		Paint textPaint = new Paint();
		textPaint.setTextSize(textSize);
		textPaint.setAntiAlias(true);
		textPaint.setARGB(0xff, 0x00, 0x00, 0x00);

		// find out how big our text is
		int tw = textSize; //textBounds.width();
		int th = textSize; //textBounds.height();
		int tA = tw*th;
		Logger.post( "tw and th have values " + Integer.toString(tw) + " and " + Integer.toString(th) );
		
		// ensure power of 2 bitmap that can hold our data
		double areaLogBase2 = Math.log( letters.length * tA )/Math.log(2);
		Logger.post( "areaLogBase2 has value " + Double.toString(areaLogBase2) );
		int bitmapLogArea = (int)( Math.ceil( areaLogBase2 / 2.0 ) )* 2;
		int bitmapWH = (int)Math.round( Math.pow( 2, bitmapLogArea/2 ) );
		Logger.post( "bitmapLogArea has value " + Integer.toString(bitmapLogArea) );
		Logger.post( "bitmapWH has value " + Integer.toString(bitmapWH) );
		
		// Create an empty, mutable bitmap
		Bitmap bitmap = Bitmap.createBitmap(bitmapWH, bitmapWH, Bitmap.Config.ARGB_4444);
		// get a canvas to paint over the bitmap
		Canvas canvas = new Canvas(bitmap);
		bitmap.eraseColor(android.graphics.Color.WHITE);

		// get a background image from resources
		// note the image format must match the bitmap format
		//Drawable background = context.getResources().getDrawable(R.drawable.background);
		//background.setBounds(0, 0, bitmapWH, bitmapWH);
		//background.draw(canvas); // draw the background to our bitmap

		int lCnt = 0;
		int xD = 0;
		int xIndMax = bitmapWH / tw;
		int yIndMax = bitmapWH / th;
		Logger.post( "xIndMax and yIndMax have values " + Integer.toString(xIndMax) + " and " + Integer.toString(yIndMax) );
		
		for( int i = 0; i < xIndMax; i++ ){
			int yD = 0;
			for( int j = 0; j < yIndMax; j++ ){
				// draw the text centered and offset
				Rect textBounds = new Rect();
				textPaint.getTextBounds( letters[lCnt], 0, letters[lCnt].length(), textBounds );
				float textW = textPaint.measureText( letters[lCnt] );
				int xDLet = (textSize - (int)textW)/2;
				int yDLet = (textSize + textBounds.height())/2;
				
				Logger.post( "drawing " + Integer.toString(lCnt) + " letter " + letters[lCnt] + " to canvas" );
				canvas.drawText( letters[lCnt], xD + xDLet, yD + yDLet, textPaint );
				Logger.post( "calculating uv for " + Integer.toString(lCnt) + " letter " + letters[lCnt] + " to canvas" );
				// calculate uv coords for the letter
				texUV[lCnt] = new TexUV();
				// Min values
				texUV[lCnt].xmin = xD/(float)bitmapWH;
				texUV[lCnt].xmax = (xD+tw)/(float)bitmapWH;
				// Max values
				texUV[lCnt].ymin = yD/(float)bitmapWH;
				texUV[lCnt].ymax = (yD+th)/(float)bitmapWH;
				Logger.post( "For lCnt " + Integer.toString(lCnt) + " uv x min max and y min max are " + Float.toString(texUV[lCnt].xmin) +", " + Float.toString(texUV[lCnt].xmax) + " and " + Float.toString(texUV[lCnt].ymin) +", " + Float.toString(texUV[lCnt].ymax) );
				yD += th;
				
				lCnt++;
				if( lCnt == letters.length ){
					break;
				}
			}
			xD += tw;
			if( lCnt == letters.length ){
				break;
			}
		}
		
		Logger.post( "Finished RenderLettersToTexture" );
		
		return bitmap;
	}
}
	
