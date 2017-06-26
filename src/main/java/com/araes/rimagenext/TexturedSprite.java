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
import java.lang.reflect.Field;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class TexturedSprite
{	
	/** Used for debug logs. */
	private static final String TAG = "TexturedSprite";

	protected MainActivity mActivityContext;
	protected GameRenderer mRenderer;

	/**
	 * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
	 * of being located at the center of the universe) to world space.
	 */
	public float[] mModelMatrix = new float[16];

	/** Allocate storage for the final combined matrix. This will be passed into the shader program. */
	protected float[] mMVPMatrix = new float[16];
	
	protected float[] SpritePositionData = null;
	protected float[] SpriteNormalData = null;
	protected float[] SpriteColorData = null;
	protected float[] SpriteTexCoorData = null;
	
	/** Store our model data in a float buffer. */
	protected FloatBuffer mPositions;
	protected FloatBuffer mColors;
	protected FloatBuffer mNormals;
	protected FloatBuffer mTextureCoordinates;

	/** This will be used to pass in the transformation matrix. */
	protected int mMVMatrixHandle;
	protected int mMVPMatrixHandle;

	/** This will be used to pass in the light position. */
	protected int mLightPosHandle;

	/** This will be used to pass in the texture. */
	protected int mTextureUniformHandle;

	/** This will be used to pass in model position information. */
	protected int mPositionHandle;

	/** This will be used to pass in model color information. */
	protected int mColorHandle;

	/** This will be used to pass in model normal information. */
	protected int mNormalHandle;

	/** This will be used to pass in model texture coordinate information. */
	protected int mTextureCoordinateHandle;

	/** How many bytes per float. */
	public final int mBytesPerFloat = 4;
	
	//How many vertices per tile
	public final int mTileVerts = 6;

    public int mTris = 0;	

	/** Size of the position data in elements. */
	public final int mPositionDataSize = 3;	

	/** Size of the color data in elements. */
	public final int mColorDataSize = 4;	

	/** Size of the normal data in elements. */
	public final int mNormalDataSize = 3;

	/** Size of the texture coordinate data in elements. */
	public final int mTextureCoordinateDataSize = 2;

	/** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
	protected float[] mLightPosInEyeSpace = new float[4];

	/** This is a handle to our Sprite shading program. */
	protected int mProgramHandle;

	/** This is a handle to our texture data. */
	protected int mTextureDataHandle;
	
	protected boolean boolDirty = false;
	protected boolean mVisible = false;

	public TexturedSprite(final MainActivity activityContext, GameRenderer Renderer )
	{	
		mActivityContext = activityContext;
		mRenderer        = Renderer;

		boolDirty = true;
	}
	/**
	 * Initialize the model data.
	 */
	public TexturedSprite(final MainActivity activityContext, GameRenderer Renderer, boolean buildNow )
	{	
		mActivityContext = activityContext;
		mRenderer        = Renderer;
		
		if( buildNow ){
			BuildSprite();
		}
	}
	
	/**
	 * Copy constructor.
	 */
	public TexturedSprite(TexturedSprite aSprite) {
		this(aSprite.mActivityContext, aSprite.mRenderer );
		//no defensive copies are created here, since 
		//there are no mutable object fields (String is immutable)
		
		BuildSprite();
		setTexture( aSprite.mTextureDataHandle );
		setShadingProgram( aSprite.mProgramHandle );
		setLightPos( aSprite.mLightPosInEyeSpace );
	}
	
	public void BuildSprite()
	{
		// Define points for a Sprite.		

		// X, Y, Z
		SpritePositionData = new float[]
		{
			// In OpenGL counter-clockwise winding is default. This means that when we look at a triangle, 
			// if the points are counter-clockwise we are looking at the "front". If not we are looking at
			// the back. OpenGL has an optimization where all back-facing triangles are culled, since they
			// usually represent the backside of an object and aren't visible anyways.

			// Front face
			-0.5f, 0.5f, 0.0f,				
			-0.5f, -0.5f, 0.0f,
			0.5f, 0.5f, 0.0f, 
			-0.5f, -0.5f, 0.0f, 				
			0.5f, -0.5f, 0.0f,
			0.5f, 0.5f, 0.0f
		};	

		// R, G, B, A
		SpriteColorData = new float[]
		{				
			// Front face
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
		};

		// X, Y, Z
		// The normal is used in light calculations and is a vector which points
		// orthogonal to the plane of the surface. For a Sprite model, the normals
		// should be orthogonal to the points of each face.
		SpriteNormalData = new float[]
		{												
			// Front face
			0.0f, 0.0f, 1.0f,				
			0.0f, 0.0f, 1.0f,
			0.0f, 0.0f, 1.0f,
			0.0f, 0.0f, 1.0f,				
			0.0f, 0.0f, 1.0f,
			0.0f, 0.0f, 1.0f
		};

		// S, T (or X, Y)
		// Texture coordinate data.
		// Because images have a Y axis pointing downward (values increase as you move down the image) while
		// OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
		// What's more is that the texture coordinates are the same for every face.
		SpriteTexCoorData = new float[]
		{												
			// Front face
			0.0f, 0.0f, 				
			0.0f, 1.0f,
			1.0f, 0.0f,
			0.0f, 1.0f,
			1.0f, 1.0f,
			1.0f, 0.0f
		};
		
		setSpritePosition( SpritePositionData );
		setSpriteColor( SpriteColorData );
		setSpriteNormal( SpriteNormalData );
		setSpriteTexCoor( SpriteTexCoorData );
		
		mTris = SpritePositionData.length / mPositionDataSize;
		
		Matrix.setIdentityM(mModelMatrix, 0);
		
		boolDirty = false;
		mVisible  = true;
	}

	public void setSpritePosition(float[] posData)
	{
		mPositions = InitializeBuffer( mPositions, posData );
	}

	public void setSpriteColor(float[] colorData)
	{
		mColors = InitializeBuffer( mColors, colorData );
	}

	public void setSpriteNormal(float[] normData)
	{
		mNormals = InitializeBuffer( mNormals, normData );
	}

	public void setSpriteTexCoor(float[] texData)
	{
		mTextureCoordinates = InitializeBuffer( mTextureCoordinates, texData );
	}

	protected FloatBuffer[] InitializeBuffers(float[] posData, float[] colorData, float[] normalData, float[] texCoorData, FloatBuffer fbPos, FloatBuffer fbColor, FloatBuffer fbNormal, FloatBuffer fbTexCoor )
	{
		LogPoster Logger = new LogPoster();
		Logger.addLogListener( mActivityContext.GameLog );
		
		// Initialize the buffers.
		if( posData     != null ){ fbPos     = InitializeBuffer( fbPos, posData ); } else { Logger.post( "posData is null" ); }
		if( colorData   != null ){ fbColor   = InitializeBuffer( fbColor, colorData ); } else { Logger.post( "colorData is null" ); }
		if( normalData  != null ){ fbNormal  = InitializeBuffer( fbNormal, normalData ); } else { Logger.post( "normalData is null" ); }
		if( texCoorData != null ){ fbTexCoor = InitializeBuffer( fbTexCoor, texCoorData ); } else { Logger.post( "texCoorData is null" ); }
		
		return new FloatBuffer[]{ fbPos, fbColor, fbNormal, fbTexCoor };
	}

	protected FloatBuffer InitializeBuffer(FloatBuffer fbo, float[] data )
	{
		fbo = ByteBuffer.allocateDirect(data.length * mBytesPerFloat)
			.order(ByteOrder.nativeOrder()).asFloatBuffer();							
		fbo.put(data).position(0);
		return fbo;
	}

	public void update()
	{
		setLightPos( mRenderer.getLightPos() );
		
		if( boolDirty ){
			BuildSprite();
		}
	}

	public void draw() 
	{
		if( mVisible ){
			setupDraw();
			executeDraw();
		}
	}

	public void setupDraw() 
	{
		setupHandles();
		loadVertices();
	}

	protected void setupHandles()
	{
		// Set our per-vertex lighting program.
        GLES20.glUseProgram(mProgramHandle);

        // Set program handles for Sprite drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix"); 
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal"); 
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
	}
		
	protected void loadVertices()
	{
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

        // Pass in the texture coordinate information
        mTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 
									 0, mTextureCoordinates);

        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

        // Pass in the light position in eye space.        
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
	}
		
	public void executeDraw(){
        SetupTextures();
		BuildMVPMatrices();
		
        // Draw the Sprite.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mTris);                               
	}

	protected void SetupTextures()
	{
		// Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);        
	}

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

	public void setTexture(int texDataHandle)
	{
		mTextureDataHandle = texDataHandle;
	}

	public void setShadingProgram( int programHandle )
	{
		mProgramHandle = programHandle;
	}

	public void setLightPos(float[] lightPos)
	{
		mLightPosInEyeSpace = lightPos;
	}	
	
	public static int getResId(String resName, Class<?> c) {
		try {
			Field idField = c.getDeclaredField(resName);
			return idField.getInt(idField);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} 
	}
	
}
