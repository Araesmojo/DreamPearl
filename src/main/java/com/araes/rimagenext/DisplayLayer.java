package com.araes.rimagenext;
import android.opengl.GLES20;
import java.nio.FloatBuffer;

public class DisplayLayer extends FractalTexturedSpriteArrayMobile
{
	public String mName;
	public String mType;
	
	/** This will be used to pass in model texture coordinate information. */
	protected int mTextureCoordinateAnimHandle;
	protected int mLERPHandle;
	protected float[] mAnimEndCoordinates;
	protected TexAnimation mCharacterAnim;
	
	// for maps where we need to draw floating dots
	public int mPointProgramHandle;
	
	public Vec3 mSun = new Vec3( 0, 1, 0 );
	
	public DisplayLayer( MainActivity activityContext, GameRenderer Renderer, String name, String type )
	{
		super( activityContext, Renderer );
		mName = name;
		mType = type;
	}

	@Override
	public void BuildSpriteArray()
	{
		super.BuildSpriteArray();
		if( mType.contains("Character") ){
			Logger.post( "Reached BuildCharacterAnim" );
			BuildCharacterAnim();
			mCharacterAnim.update();
		}
	}

	@Override
	protected TexturedSprite[] PlaceSprites()
	{
		if( mType != "Character" ){
			return super.PlaceSprites();
		} else {
			return PlaceCharacterSprites();
		}
	}

	private TexturedSprite[] PlaceCharacterSprites()
	{
		int viewWidth = mRenderer.mWidth;
		int viewHeight = mRenderer.mHeight;

		int SpriteSize = 16;

		arrW = viewWidth / SpriteSize + 2;
		if( arrW % 2 != 0 ){ arrW++; }
		arrH = viewHeight / SpriteSize + 2;
		if( arrH % 2 != 0 ){ arrH++; }
		arrW = 1;
		arrH = 1;

		TexturedSprite[] arrOut = new TexturedSprite[ arrW * arrH ];

		float xPosStart = -arrW / 2 + 0.5f;
		float yPosStart = -arrH / 2 + 0.5f;

		int a = 0;
		float zDelta=  0.0f;
		float xDelta = xPosStart;
		for( int i = 0; i < arrW; i++ ){
			float yDelta = yPosStart;
			for( int j = 0; j < arrH; j++ ){
				arrOut[a] = new Tile( mActivityContext, mRenderer, true );
				TranslateVec3Array( arrOut[a].SpritePositionData, xDelta, yDelta, zDelta );
				ChangeUVToCharStart( arrOut[a] );
				yDelta += 1.0f;
				a++;
			}
			xDelta += 1.0f;
		}
		
		return arrOut;
	}
	
	protected void BuildCharacterAnim(){
		mCharacterAnim = new TexAnimation( mActivityContext, mRenderer, mTextureDataHandle, 4, 4, 0, 1, 3, 1, 4, true );
	}
		
	private void ChangeUVToCharStart(TexturedSprite arr)
	{
		arr.SpriteTexCoorData = new float[]
		{												
			// Front face
			0.0f,  0.0f, 				
			0.0f,  0.25f,
			0.25f, 0.0f,
			0.0f,  0.25f,
			0.25f, 0.25f,
			0.25f, 0.0f
		};
	}
	
	public static float[] ChangeUVToIJofWH(float[] arr, int i, int j, int w, int  h )
	{
		float deltaX = 1.0f / (float)w;
		float deltaY = 1.0f / (float)h;
		
		float xLo = i * deltaX;
		float xHi = xLo + deltaX;
		float yLo = j * deltaY;
		float yHi = yLo + deltaY;
		
		arr = new float[]
		{												
			// Front face
			xLo, yLo, 				
			xLo, yHi,
			xHi, yLo,
			xLo, yHi,
			xHi, yHi,
			xHi, yLo
		};
		
		return arr;
	}

	@Override
	public void update()
	{
		super.update();
		if( mType.contains("Character") ){
			mCharacterAnim.update();
		}
	}

	@Override
	public void draw()
	{
		if( mType.contains("Character") ){
			setupCharacterDraw();
			executeDraw();
		} else {
			super.draw();
		}
	}
	
	public void setupCharacterDraw() 
	{
		setupCharacterHandles();
		loadCharacterVertices();
	}
	
	protected void setupCharacterHandles()
	{
		// Set our per-vertex lighting program.
        GLES20.glUseProgram(mProgramHandle);

        // Set program handles for Sprite drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix"); 
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
        mLERPHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LERP");
		mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
        mTextureCoordinateAnimHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate_Anim");
    }

	protected void loadCharacterVertices()
	{
		float tLERP = mCharacterAnim.mLERP;
		float AProg = mCharacterAnim.AnimProg;
		//Logger.post("Draw: AnimProg at " + Float.toString(AProg) + " and LERP at " + Float.toString(tLERP) );
		
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

        // Pass in the normal information
        mNormals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 
									 0, mNormals);

        GLES20.glEnableVertexAttribArray(mNormalHandle);

		//Logger.post("Draw call for Display with char tex 1 fb " + mCharacterAnim.fbCur );
        // Pass in the texture coordinate information
        mCharacterAnim.fbCur.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 
									 0, mCharacterAnim.fbCur);

        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

		// Pass in the second animation texture coordinate information
        mCharacterAnim.fbNxt.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateAnimHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 
									 0, mCharacterAnim.fbNxt);

        GLES20.glEnableVertexAttribArray(mTextureCoordinateAnimHandle);
		
        // Pass in the light position in eye space.        
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
		
		// Pass in the LERP factor
		GLES20.glUniform1f(mLERPHandle,mCharacterAnim.mLERP );
	}
}
