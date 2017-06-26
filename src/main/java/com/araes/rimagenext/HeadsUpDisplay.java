package com.araes.rimagenext;
import android.opengl.Matrix;
import java.nio.FloatBuffer;
import android.opengl.GLES20;
import android.view.MotionEvent;
import android.graphics.Rect;
import android.text.method.*;
import android.view.*;
import android.view.View.*;

public class HeadsUpDisplay extends TexturedSpriteArray implements OnTouchListener
{
	int utf16Dn = 0x21E9;
	int utf16Rt = 0x21E8;
	int utf16Up = 0x21E7;
	int utf16Lt = 0x21E6;
	
	int utf16A = 0x0041;
	int utf16X = 0x0058;
	int utf16Y = 0x0059;
	int utf16B = 0x0042;

	String[] DirButtons;
	String[] ActButtons;
	
	BBox[] BoundingBoxes;
	
	boolean mTouching = false;
	MotionEvent lastMotionEvent = null;
	
	private View.OnTouchListener handleTouch = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent e) {
			mTouching = true;
			lastMotionEvent = e;
			return false;
		}
	};
	
	// Main class declaration
	public HeadsUpDisplay( MainActivity activityContext, GameRenderer Renderer )
	{
		super( activityContext, Renderer );
		
		Logger.post( "Entered HeadsUpDisplay" );
		
		String strDn = Character.toString( (char)utf16Dn );
		String strRt = Character.toString( (char)utf16Rt );
		String strUp = Character.toString( (char)utf16Up );
		String strLt = Character.toString( (char)utf16Lt );

		DirButtons = new String[]{ strRt, strUp, strLt, strDn };

		String strA = Character.toString( (char)utf16A );
		String strX = Character.toString( (char)utf16X );
		String strY = Character.toString( (char)utf16Y );
		String strB = Character.toString( (char)utf16B );

		ActButtons = new String[]{ strA, strX, strY, strB };
	
		BoundingBoxes = new BBox[]{
			new BBox( mRenderer, strRt, new BBoxAction( "RIGHT", true, new Vec3(  1,  0, 0 ) ) ),
			new BBox( mRenderer, strUp, new BBoxAction( "UP",    true, new Vec3(  0,  1, 0 ) ) ),
			new BBox( mRenderer, strLt, new BBoxAction( "LEFT",  true, new Vec3( -1,  0, 0 ) ) ),
			new BBox( mRenderer, strDn, new BBoxAction( "DOWN",  true, new Vec3(  0, -1, 0 ) ) ),
			new BBox( mRenderer, strDn, new BBoxAction( "A", true ) ),
			new BBox( mRenderer, strDn, new BBoxAction( "X", true ) ),
			new BBox( mRenderer, strDn, new BBoxAction( "Y", true ) ),
			new BBox( mRenderer, strDn, new BBoxAction( "B", true ) )
		};
		
		Logger.post( "Finished TexturedSprite creation" );
	}

	private void BuildHUD( String[] dirButtons, String[] actButtons )
	{
		// Build and set our HUD texture
		int texLettersHandle=0;
		String[] AllButtons = GameUtils.ConcatStringArrays( DirButtons, ActButtons );
		TexUV[] texUV = new TexUV[ AllButtons.length ];
		texLettersHandle = TextureHelper.createTextureFromLetters(mActivityContext, AllButtons, 32, texUV );
		setTexture( texLettersHandle );
		
		// Build our display mesh
		TexturedSprite[] Sprites = CreateButtons( AllButtons, texUV );
		AppendSpritesToBulkMesh( Sprites );

		// Initialize the buffers
		FloatBuffer[] arrFB;
		arrFB = InitializeBuffers( SpritePositionData, SpriteColorData, SpriteNormalData, SpriteTexCoorData,
								  mPositions, mColors, mNormals, mTextureCoordinates );
		mPositions          = arrFB[0];
		mColors             = arrFB[1];
		mNormals            = arrFB[2];
		mTextureCoordinates = arrFB[3];

		// Calculate the number of triangles to draw with this call
		mTris = SpritePositionData.length / mPositionDataSize;

		Matrix.setIdentityM(mModelMatrix, 0);
		
		boolDirty = false;
		
		mActivityContext.mGLView.setOnTouchListener(handleTouch);

		Logger.post("Finished TexturedSpriteArray BuildHUD");
	}

	private TexturedSprite[] CreateButtons(String[] allButtons, TexUV[] texUV)
	{
		int w = mRenderer.mWidth;
		int h = mRenderer.mHeight;
		int wd2 = w/2;
		int hd2 = h/2;
		
		Logger.post( "Entered CreateButtons" );
		
		TexturedSprite[] tSprites = new TexturedSprite[ allButtons.length ];
		
		int Border = 8;
		int BoxWH = Math.min( w, h ) / 2;
		int BtnWH = BoxWH / 3 - 2*Border;
		int BtnHalfWH = BtnWH / 2;
		Logger.post( "w, h, BoxWH, and BtnWH have values of " + Integer.toString(w) + ", " + Integer.toString(h) + " and " + Integer.toString(BoxWH)  + ", " + Integer.toString(BtnWH) );
		
		float pctDelta = -0.4f; // negative is downscreen
		float[] DirBoxCen = new float[]{ BoxWH/2,     hd2*(1.0f+pctDelta) };
		float[] ActBoxCen = new float[]{ w-(BoxWH/2), hd2*(1.0f+pctDelta) };
		Logger.post( "DirboxCen and ActBoxCen have values of " + Float.toString(DirBoxCen[0]) + ", " + Float.toString(DirBoxCen[1]) + " and " + Float.toString(ActBoxCen[0])  + ", " + Float.toString(ActBoxCen[1]) );
		
		Logger.post( "start button creation-display" );
		
		// Create direction buttons
		double Angle = 0;
		int GridRad = ( BoxWH - BtnWH - Border ) / 2;
		for( int i = 0; i < DirButtons.length; i++ ){
			float[] BtnCen = new float[]{
				DirBoxCen[0] + (float)( Math.cos( Angle )*GridRad ),
				DirBoxCen[1] + (float)( Math.sin( Angle )*GridRad )
			};
			tSprites[i] = CreateButton( allButtons[i], texUV[i], BtnCen, BtnHalfWH, wd2, hd2, BoundingBoxes[i] );
			Angle += Math.PI / 2;
		}

		Logger.post( "next button group-action" );
		
		// Create direction buttons
		Angle = 0;
		for( int i = DirButtons.length; i < allButtons.length; i++ ){
			float[] BtnCen = new float[]{
				ActBoxCen[0] + (float)( Math.cos( Angle )*GridRad ),
				ActBoxCen[1] + (float)( Math.sin( Angle )*GridRad )
			};
			tSprites[i] = CreateButton( allButtons[i], texUV[i], BtnCen, BtnHalfWH, wd2, hd2, BoundingBoxes[i] );
			Angle += Math.PI / 2;
		}

		Logger.post( "Finished CreateButtons" );
		
		return tSprites;
	}

	private TexturedSprite CreateButton(String btn, TexUV uv, float[] cen, int halfWH, int scrWd2, int scrHd2, BBox box )
	{
		Logger.post( "Entered CreateButton" );
		Logger.post( "cen[0], cen[1], halfWH, scrWd2, and scrHd2 have values of " + Float.toString(cen[0]) + ", " + Float.toString(cen[1]) + ", " + Integer.toString(halfWH) + ", " + Integer.toString(scrWd2) + ", " + Integer.toString(scrHd2) );
		
		TexturedSprite tSprite = new TexturedSprite( mActivityContext, mRenderer, true );
		
		int xlo = ( Math.round(cen[0]) - halfWH ); 
		int xhi = ( Math.round(cen[0]) + halfWH );
		int ylo = ( Math.round(cen[1]) - halfWH );
		int yhi = ( Math.round(cen[1]) + halfWH );
		
		// Maps to ortho screen (-1,1)
		float xloOrth = ( xlo )/(float)scrWd2 - 1.0f; 
		float xhiOrth = ( xhi )/(float)scrWd2 - 1.0f;
		float yloOrth = ( ylo )/(float)scrHd2 - 1.0f;
		float yhiOrth = ( yhi )/(float)scrHd2 - 1.0f;
		
		Logger.post( "xlo, xhi, ylo, yhi have values of " + Float.toString(xlo) + ", " + Float.toString(xhi) + ", " + Float.toString(ylo)  + ", " + Float.toString(yhi) );
		
		tSprite.SpritePositionData = new float[]
		{
			xloOrth, yhiOrth, 0.0f,				
			xloOrth, yloOrth, 0.0f,
			xhiOrth, yhiOrth, 0.0f, 
			xloOrth, yloOrth, 0.0f, 				
			xhiOrth, yloOrth, 0.0f,
			xhiOrth, yhiOrth, 0.0f
		};
		
		tSprite.SpriteTexCoorData = new float[]
		{
			uv.xmin, uv.ymin, 				
			uv.xmin, uv.ymax,
			uv.xmax, uv.ymin,
			uv.xmin, uv.ymax,
			uv.xmax, uv.ymax,
			uv.xmax, uv.ymin
		};
		
		int scrH = scrHd2 + scrHd2;
		
		// set the bounding rect for the button onTouch event
		box.setRect( xlo, scrH - yhi, xhi, scrH - ylo );
		
		Logger.post( "Finished CreateButton" );
		return tSprite;
	}
	
	public boolean onTouchEvent(final MotionEvent e) {
		mTouching = true;
		lastMotionEvent = e;
		return false;
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent e)
	{
		mTouching = true;
		lastMotionEvent = e;
		return false;
	}
	
	public boolean CheckHits(){
		// for each bounding box, check if we have a hit
		// they perform their own logic on what to do if hit
		int x = (int)lastMotionEvent.getX();
		int y = (int)lastMotionEvent.getY();
		Logger.post( "Checking hits with x,y " + Integer.toString(x) + ", " + Integer.toString(y) + " of last MotionEvent" );
		
		boolean hit = false;
		int i = 0;
		while( i < BoundingBoxes.length && !hit ){
			String right = Integer.toString( BoundingBoxes[i].mBox.right );
			String top = Integer.toString( BoundingBoxes[i].mBox.top );
			String left = Integer.toString( BoundingBoxes[i].mBox.left );
			String bottom = Integer.toString( BoundingBoxes[i].mBox.bottom );
			Logger.post( "Checking hits of " + BoundingBoxes[i].mName + " with bbox rect of [r,t,l,b] " + right + ", " + top + ", " + left + ", " + bottom );
			
			// Checking for hit. On success, BBoxes perform their onTouch action
			hit = BoundingBoxes[i].checkHit(lastMotionEvent);
			if( hit ){
				Logger.post( "HUD button " + BoundingBoxes[i].mName + " responding to touch event" );
			}
			i++;
		}
		Logger.post( "Ran searchv of button hits" );
		return hit;
	}

	@Override
	public void update()
	{
		if( boolDirty ){
			BuildHUD( DirButtons, ActButtons );
		}
		if( mTouching){
			CheckHits();
			mTouching = false;
		}
	}

	@Override
	public void draw()
	{
		setupDraw();
		executeDraw();
	}
	
	@Override
	public void setupDraw() 
	{
		// Use simple ortho no light program.
        GLES20.glUseProgram(mProgramHandle);

        // Set program handles for Sprite drawing.
		//mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
		
		//GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mRenderer.mProjectionMatrix, 0);

		// Pass in the position information
		mPositions.position(0);		
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
									 0, mPositions);        

        GLES20.glEnableVertexAttribArray(mPositionHandle);        

        // Pass in the color information
        mColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
									 0, mColors);        

        GLES20.glEnableVertexAttribArray(mColorHandle);

        // Pass in the texture coordinate information
        mTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 
									 0, mTextureCoordinates);

        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
	}

	@Override
	public void executeDraw(){
        SetupTextures();
		
        // Draw the Sprite.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mTris);                               
	}

	@Override
	protected void SetupTextures()
	{
		// Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);        
	}
}
