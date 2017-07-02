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
import android.os.SystemClock;
import org.apache.http.conn.util.*;
import java.lang.reflect.Field;
import android.graphics.drawable.Drawable;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import android.graphics.*;
import java.io.*;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class GameRenderer implements GLSurfaceView.Renderer 
{	
	/** Used for debug logs. */
	private static final String TAG = "GameRenderCube";
	public LogPoster Logger;

	private final MainActivity mActivityContext;
	
	//private TexturedCube mCube;
	public GameWorld mWorld;
	//private TexturedSpriteArrayMobile mSpriteArray;
	private PointLight mLight;
	//private HeadsUpDisplay mHUD;

	// Properties of the viewport and view
	public int mWidth;
	public int mHeight;
	
	/**
	 * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
	 * it positions things relative to our eye.
	 */
	public float[] mViewMatrix = new float[16];

	/** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
	public float[] mProjectionMatrix = new float[16];
	public float[] mProjection3D = new float[16];
	public float[] mProjection2D = new float[16];
	
	/** This is a handle to our shading programs. */
	private int mProgramHandle;
	private int mPointProgramHandle;
	private int mHUDProgramHandle;

	/** This is a handle to our texture data. */
	private int mTextureDataHandle;
	
	// Clear color values for changing the background
	private float mRed = 0.0f;
    private float mGreen = 0.0f;
    private float mBlue = 0.0f;

	public long mTimeCur = 0;
	
	public long mTimeLast = 0;
	public long mTimeDelta = 0;
	
	public long mTimeLastRender = 0;
	public long mTimeSinceRender = 0;
	
	public long mTimeLastMove = 0;
	public long mTimeSinceMove = 0;
	
	public long mTimeLastAction = 0;
	public long mTimeSinceAction = 0;
	
	public long mDelta30fps = 1000/30;
	public long mDelta60fps = 1000 / 60;

	private int mPointPlateHandle;

	private boolean takeScreenshot = true;

	private int screenShotCnt = 0;
	
	/**
	 * Initialize the model data.
	 */
	public GameRenderer(final MainActivity activityContext)
	{	
		mActivityContext = activityContext;

		// Create our cubes
		//mCube = new TexturedCube( mActivityContext, this );
		mWorld = new GameWorld( mActivityContext, this );
		mWorld.BuildDisplayLayers();
		//mSpriteArray = new TexturedSpriteArrayMobile( mActivityContext, this );
		mLight = new PointLight( this );
		//mHUD = new HeadsUpDisplay( mActivityContext, this );
		Logger = new LogPoster();
		Logger.addLogListener( mActivityContext.GameLog );
		Logger.post( "Finished GameRenderer creation" );
	}
	
	public GameWorld getMap(){
		return mWorld;
	}

	public float[] getLightPos()
	{
		return mLight.getLightPos();
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) 
	{
		// Set the background clear color to black.
		GLES20.glClearColor(mRed, mGreen, mBlue, 1.0f);

		// Use culling to remove back faces.
		GLES20.glEnable(GLES20.GL_CULL_FACE);

		// Enable depth testing
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		// The below glEnable() call is a holdover from OpenGL ES 1, and is not needed in OpenGL ES 2.
		// Enable texture mapping
		// GLES20.glEnable(GLES20.GL_TEXTURE_2D);

		// Position the eye in front of the origin.
		final float eyeX = 0.0f;
		final float eyeY = 0.0f;
		final float eyeZ = 5.0f;

		// We are looking toward the distance
		final float lookX = 0.0f;
		final float lookY = 0.0f;
		final float lookZ = 0.0f;

		// Set our up vector. This is where our head would be pointing were we holding the camera.
		final float upX = 0.0f;
		final float upY = 1.0f;
		final float upZ = 0.0f;

		// Set the view matrix. This matrix can be said to represent the camera position.
		// NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
		// view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
		Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);		

		//final String vertexShader = RawResourceReader.readTextFileFromRawResource( mActivityContext, R.raw.per_pixel_vertex_shader); 		
 		//final String fragmentShader = RawResourceReader.readTextFileFromRawResource( mActivityContext, R.raw.per_pixel_fragment_shader);			
		
		String[] shaderParts = new String[]{ "3dbase" }; //, "tex" }; //, "light" };
		mProgramHandle = BuildShadersFromParts( shaderParts );//, new String[]{ vertexShader, fragmentShader } );
		//mCube.setShadingProgram( mProgramHandle );
		mWorld.layerByName.get("Ground").setShadingProgram( mProgramHandle );
		mWorld.layerByName.get("Grass").setShadingProgram( mProgramHandle );
		
		shaderParts = new String[]{ "3dbase", "tex", "anim", "light" };
		mWorld.layerByName.get("Character").setShadingProgram( BuildShadersFromParts( shaderParts ) );//, new String[]{ vertexShader, fragmentShader } );
		
		// Define a simple shader program for our point.
        final String pointVertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_vertex_shader);        	       
        final String pointFragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_fragment_shader);

        final int pointVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
        mPointProgramHandle = ShaderHelper.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle, 
																new String[] {"a_Position"}); 

		mLight.setShadingProgram( mPointProgramHandle );
		
		// Build shader for continent points in world map
		shaderParts = new String[]{ "3dbase", "point" };
		mPointPlateHandle = BuildShadersFromParts( shaderParts );
		mWorld.layerByName.get("Ground").mPointProgramHandle = mPointPlateHandle;

		//final String vertexShaderHUD = RawResourceReader.readTextFileFromRawResource( mActivityContext, R.raw.button_vertex_shader); 		
 		//final String fragmentShaderHUD = RawResourceReader.readTextFileFromRawResource( mActivityContext, R.raw.button_fragment_shader);			
		
		shaderParts = new String[]{ "2dbase", "tex" };
		//mHUD.setShadingProgram( BuildShadersFromParts( shaderParts ) );//, new String[]{ vertexShaderHUD, fragmentShaderHUD } );
		
		//mHUD.setShadingProgram( mHUDProgramHandle );
        
		// Load the texture
        //mTextureDataHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.bumpy_bricks_public_domain);
		mTextureDataHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.bumpy_bricks_public_domain);
		mWorld.layerByName.get("Ground").setTexture( mTextureDataHandle );

		mTextureDataHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.grass );
		mWorld.layerByName.get("Grass").setTexture( mTextureDataHandle );

		mTextureDataHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.herospritesheet );
		mWorld.layerByName.get("Character").setTexture( mTextureDataHandle );
	}
	
	protected int BuildShadersFromParts( String[] parts ){//, String[] fallbackShader ){
		int outProgram;

		Logger.post("Starting BuildShadersFromPart-----------");
		// The original shader in one file
		//final String vertexShader = fallbackShader[0];
		//final String fragmentShader = fallbackShader[1];
		
		
		// The new vert/frag shader built the parts with subfiles way
		//Logger.post("Starting BuildShaderParts for vert------------");
		//String vertShader = BuildShaderParts( "vert", parts );
		//Logger.post("Starting BuildShaderParts for frag------------");
		//String fragShader = BuildShaderParts( "frag", parts );
		
		for( int i = 0; i < parts.length; i++ ){
			if( parts[i].contains("2dbase" )){
				parts[i] = "base2d";
			}
			if( parts[i].contains("3dbase" )){
				parts[i] = "base3d";
			}
		}
		
		// new vert/frag shaders built the GG config file way
		Logger.post("Starting BuildShaderPartsGG-----------");
		String[] shaders = new String[]{ "", "" };
		BuildShaderPartsGG( parts, shaders );
		
		Logger.post( "vert" + " shader prior to creation is \n--------------------\n" + "NOTBUILT\n" );//vertShader + "\n" );
		Logger.post( "vertex" + " shader prior to creation is \n--------------------\n" + "NOTBUILT\n" );//vertexShader + "\n" );
		Logger.post( "shaders[0] {vert}" + " shader prior to creation is \n--------------------\n" + shaders[0] + "\n" );
		Logger.post( "frag" + " shader prior to creation is \n--------------------\n" + "NOTBUILT\n" );//fragShader + "\n" );
		Logger.post( "fragment" + " shader prior to creation is \n--------------------\n" + "NOTBUILT\n" );//fragmentShader + "\n" );
		Logger.post( "shaders[1] {frag}" + " shader prior to creation is \n--------------------\n" + shaders[1] + "\n" );
		
		final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, shaders[0]);		
		final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, shaders[1]);		
		
		Logger.post( "vert and frag compiled" );
		
		outProgram = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, 
													   new String[] {"a_Position",  "a_Color", "a_Normal", "a_TexCoordinate"});
		Logger.post( "outProgram compiled" );
		return outProgram;
	}
	
	private void BuildShaderPartsGG(String[] parts, String[] outPutShaders )
	{
		HashMap<String,String> outLines = new HashMap<String,String>();
		HashMap<String,HashMap<String,Integer>> inputs = new HashMap<String,HashMap<String,Integer>>();
		
		String[] zones = new String[]{
			"uniform",
			"attribute",
			"varying",
			"body",
			"end"
		};
		
		for(int i = 0; i < parts.length; i++ ){
			ReadShaderGG( parts[i], zones, outLines, inputs );
		}
		
		Logger.post( "Building output strings" );
		int s = 0;
		int zLen = zones.length;
		String type = "vert";
		String zone = "uniform";
		String type_zone = type + "_" + zone;
		for( int i = 0; i < 2; i++ ){
			// Put in the shared variables both use
			outPutShaders[s] += outLines.get("share_varying");
			for( int j = 0; j < zLen; j++ ){
				zone = zones[j];
				type_zone = type + "_" + zone;
				if( zone == "body" ){
					String beginStr = RawResToString( type + "_body_begin_default" ).trim();
					outPutShaders[s] += beginStr;
					outPutShaders[s] += outLines.get(type_zone);
				} else if( zone == "end" ){
					String endStr   = RawResToString( type + "_end_final" ).trim();
					outPutShaders[s] += outLines.get(type_zone);
					outPutShaders[s] += endStr;
				} else {
					outPutShaders[s] += outLines.get(type_zone);
				}
			}
			type = "frag";
			s++;
		}
	}

	private void ReadShaderGG( String resName, String[] zones, HashMap<String,String> outLines, HashMap<String,HashMap<String,Integer>> inputs ){
		Logger.post( "Entered ReadShaderGG" );
		Logger.post( "Getting text for resource " + resName );
		String[] shaderGG = RawResToString( resName ).trim().split( "\n" );
		int lines = shaderGG.length;
		Logger.post( "Found " + Integer.toString(lines) + " lines in ShaderGG" );
		
		int zLen = zones.length;
		
		String type = "vert";
		String zone = "uniform";
		String type_zone = type + "_" + zone;
		for( int i = 0; i < lines; i++ ){
			String line = shaderGG[ i ];

			Logger.post( "New line is $$" + line + "$$");
			
			String[] word = line.split( "\\s+" );
			if( word.length > 0 ){
				if( word[0].contains("REPLACE") ){
					Logger.post( "---# Entering replacement routine with type_zone " + type_zone );
					// Get next two lines
					// Search for first line in outLines and inputs
					// Replace found instances with second line
					i++;
					String lineTarget = shaderGG[i++];
					String lineReplace = shaderGG[i];
					Logger.post( "Found target line " + lineTarget );
					Logger.post( "Found replace line " + lineReplace );
					String tzLines = outLines.get(type_zone);
					Logger.post( "Found existing output lines " + tzLines );
					tzLines = tzLines.replace( lineTarget, lineReplace );
					Logger.post( "Output line after replace " + tzLines );
					outLines.put( type_zone, tzLines );
				} else if( word.length >= 2 ){
					if( word[0].contains("##") ){
						Logger.post( "## Trigger change type and type_zone" );
						// High level heading or comment that changes our shader group
						if( word[1].contains("Vertex")){
							type = "vert";  type_zone = type + "_" + zone;
						} else if( word[1].contains("Fragment") ){
							type = "frag";  type_zone = type + "_" + zone;
						} else if( word[1].contains("Shared") ){
							type = "share"; type_zone = type + "_" + zone;
						}
						if( inputs.get( type_zone ) == null ){
							inputs.put( type_zone, new HashMap<String,Integer>() );
						}
						Logger.post( "## Changed type and type_zone to " + type_zone );
					} else if( word[0].contains("#") ){
						// Zone heading to place variables
						for( int j = 0; j < zLen; j++ ){
							if( word[1].contains(zones[j]) ){
								Logger.post( "# Trigger change zone to " + zones[j] + " and type_zone to " + type + "_" + zones[j] );

								zone = zones[j];
								type_zone = type + "_" + zone;
								if( inputs.get(type_zone) == null ){
									inputs.put(type_zone, new HashMap<String,Integer>() );
								}
								if( outLines.get(type_zone) == null ){
									outLines.put(type_zone, "" );
								}
								Logger.post( "# Changed zone and type_zone to " + type_zone );
								break;
							}

						}
					} else {
						Logger.post( "Reading input line with type_zone set to " + type_zone );
						Logger.post( "Line is @@" + line + "@@" );
						// Actual uniform, attribute, varying, bodytext, or end equation data
						// If this is a data piece that needs an input, remember that
						String existString = outLines.get(type_zone);
						Logger.post( "Existing output data for " + type_zone + " reads as " + existString );
						outLines.put( type_zone, existString + "\n" + line );
						if(( zone.contains("uniform") || zone.contains("attribute") )|| zone.contains("varying") ){
							if( word.length >= 4 ){
								inputs.get(type_zone).put( word[3], 1 );
							}
						}
					}
				}
			}
		}
		Logger.post( "Finished ReadShaderGG" );
	}

	private String BuildShaderParts(String type, String[] parts)
	{
		Logger.post("Entering build " + type + " from parts" );
		
		int partsInd = 0;
		if( type == "frag" ){
			partsInd = 1;
		}
		
		Map<String,String> shaderText = new HashMap<String,String>();
		Map<String,List<String>> shaderInputs = new HashMap<String,List<String>>();
		for( int i = 0; i < parts.length; i++ ){
			// Find out what types of data are needed for this component (uniform,attribute,varying,body,end)
			Logger.post("Finding supParts for " + parts[i] );
			String subPartFile = "shader_parts_" + parts[i];
			int resourceID = getResId( subPartFile, R.raw.class );
			String[] vert_frag = ReadTextFileRes( resourceID ).trim().split( "\n" );
			String[] keys = vert_frag[partsInd].trim().split( ", " ); 
			
			// Access each of this components available data sections
			// { uniform, attribute, varying, body, end }
			for( int j = 0; j < keys.length; j++ ){
				keys[j].trim();
				String partFileName = type + "_" + keys[j] + "_" + parts[i];
				Logger.post("Adding resources for " + partFileName );
				int subResourceID = getResId( partFileName, R.raw.class );
				String resText   = ReadTextFileRes( subResourceID ).trim();
				String blockText = ( shaderText.get(keys[j]) == null ) ? "" : shaderText.get(keys[j] );
				String gap = "\n";
				Logger.post( "keys[" + Integer.toString(j) + "] equal " + keys[j] );
				if( keys[j].contains( "end" ) ){
					Logger.post( "keys equal end, setting gap empty for " + resText );
					gap = "";
				}
				
				blockText += gap + resText;
				shaderText.put(keys[j], blockText );
				Logger.post("Completed resources for " + type + "_" + keys[j] + "_" + parts[i] );
			}
		}

		String shader = "";
		
		if( type == "frag" ){
			int resID = getResId( type + "_begin_default", R.raw.class );
			String shaderHeader = ReadTextFileRes( resID ).trim();
			shader += shaderHeader;
		}

		String[] blocks = new String[]{ "uniform", "attribute", "varying", "body", "end" };
		for( int i = 0; i < blocks.length; i++ ){
			Logger.post("Finding inputs from " + blocks[i] );

			String blockText = shaderText.get( blocks[i] );
			if( blockText == null ){ blockText = ""; }
			else                   { blockText.trim(); }
			
			if( blocks[i] == "body" ){
				Logger.post( "Adding body start text" );
				int resIDSt  = getResId( "vert_body_begin_default", R.raw.class );
				String bodyStart = ReadTextFileRes( resIDSt ).trim();
				blockText = "\n" + bodyStart + "\n" + blockText;      
			} else if( blocks[i] == "end" ){
				String Final = type + "_end_final";
				Logger.post( "Adding ending footer text name " + Final + " and blocktext " + blockText );
				int resIDFinal = getResId( Final, R.raw.class );
				String endFinal = ReadTextFileRes( resIDFinal ).trim();
				blockText = blockText + endFinal;
				Logger.post( "Finished ending footer text" );
			} else {
				shaderInputs.put( blocks[i], new ArrayList<String>() );
				ScanBlockForInputs( blocks[i], blockText, shaderInputs );
			}
			shader += blockText;
		}
		
		return shader;
	}
	
	private void ScanBlockForInputs(String block, String blockText, Map<String, List<String>> inputs)
	{
		Logger.post("Scanning block " + block + " for inputs" );
		String[] lines = blockText.split("\n");
		Logger.post("Found " + Integer.toString( lines.length ) + " lines in blockText" );
		for( int i = 0; i < lines.length; i++ ){
			Logger.post( "Line " + Integer.toString(i) + "is\n" + lines[i] );
			String[] words = lines[i].trim().split( "\\s+" );
			if( words.length >= 3 ){
				if( words[2].contains(";") ){
					words[2].replace(";","");
					Logger.post("Found words[2] is " + words[2] );
					
					boolean isExist = false;
					for( int j = 0; j < inputs.get(block).size(); j++ ){
						if( inputs.get(block).get(j) == words[2] ){
							isExist = true;
						}
					}
					
					if( !isExist ){
						inputs.get(block).add( words[2] );
					}
				}
			}
		}
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
	
	public String ReadTextFileRes( int resID ){
		return RawResourceReader.readTextFileFromRawResource( mActivityContext, resID );
	}
	
	public String RawResToString( String fName ){
		return ReadTextFileRes( getResId( fName, R.raw.class ) );
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) 
	{
		mWidth = width;
		mHeight = height;

		// Set the OpenGL viewport to the same size as the surface.
		GLES20.glViewport(0, 0, mWidth, mHeight);

		// Create a new 3D perspective projection matrix. The height will stay the same
		// while the width will vary as per aspect ratio.
		final float ratio = (float) mWidth / mHeight*1.0f; // was 1.0f
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1.0f; // was 1.0f
		final float top = 1.0f;  // was 1.0f
		final float near = 3.0f; // was 1.0f
		final float far = 10.0f;

		Matrix.frustumM(mProjection3D, 0, left, right, bottom, top, near, far);
		
		// create a new 2D ortho projectio matrix
		Matrix.orthoM( mProjection2D, 0, -mWidth / 2, mWidth / 2, -mHeight / 2, mHeight / 2, -10, 10 );
		
		//mSpriteArray.BuildSpriteArray( mSpriteArray.getSprites() );
		mWorld.layerByName.get("Ground").boolDirty = true;
		//mHUD.boolDirty = true;
	}	

	@Override
	public void onDrawFrame(GL10 glUnused) 
	{
		mTimeCur = SystemClock.uptimeMillis();
		mTimeDelta       = mTimeCur - mTimeLast;
		mTimeSinceRender = mTimeCur - mTimeLastRender;
		mTimeSinceMove   = mTimeCur - mTimeLastMove;
		mTimeSinceAction = mTimeCur - mTimeLastAction;
		
		// Update the game objects and HUD
		updateScene();
		
		if( mTimeSinceRender > mDelta60fps ){
			// Draw the scene - first clear
			GLES20.glClearColor(mRed, mGreen, mBlue, 1.0f);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);			        
			drawScene();
			mTimeLastRender = mTimeCur;
			mTimeSinceRender = 0;
		}
	}

	private void updateScene()
	{
		// Do a complete rotation every 10 seconds.
        long timeLoop = mTimeCur % 10000L;        
        float angleInDegrees = (360.0f / 10000.0f) * ((int) timeLoop);

		if( mTimeSinceRender > mDelta60fps ){
			//mHUD.update();
		}
		
		// Update light
		mLight.update( angleInDegrees );

		// Update cubes
		//mCube.update( angleInDegrees );

		// Update sprite
		if( mTimeSinceRender > mDelta60fps ){
			mWorld.update();
		}
		
		if( mTimeSinceMove > mDelta30fps ){
			mWorld.processMoves();
			mTimeLastMove = mTimeCur;
			mTimeSinceMove = 0;
		}
		
		if( mTimeSinceAction > mDelta30fps ){
			mWorld.processActions();
			mTimeLastAction = mTimeCur;
			mTimeSinceAction = 0;
		}
		
		mTimeLast = mTimeCur;
	}

	private void drawScene()
	{
		// Draw3D elements
		mProjectionMatrix = mProjection3D;
		draw3D();
		
		// Draw2D elements like the HUD
		mProjectionMatrix = mProjection2D;
		draw2D();

		// https://stackoverflow.com/questions/26808518/screenshot-on-android-opengl-es-application
		if( takeScreenshot ) {
			Logger.post("screenshot");
			int screenshotSize = mWidth * mHeight;
			ByteBuffer bb = ByteBuffer.allocateDirect( screenshotSize * 4 );
			bb.order(ByteOrder.nativeOrder());
			GLES20.glReadPixels( 0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb );
			int pixelsBuffer[] = new int[screenshotSize];
			bb.asIntBuffer().get(pixelsBuffer);
			bb = null;

			for (int i = 0; i < screenshotSize; ++i) {
				// The alpha and green channels' positions are preserved while the red and blue are swapped
				pixelsBuffer[i] = ((pixelsBuffer[i] & 0xff00ff00)) | ((pixelsBuffer[i] & 0x000000ff) << 16) | ((pixelsBuffer[i] & 0x00ff0000) >> 16);
			}

			Bitmap bitmap = Bitmap.createBitmap( mWidth, mHeight, Bitmap.Config.ARGB_8888);
			bitmap.setPixels( pixelsBuffer, screenshotSize-mWidth, -mWidth, 0, 0, mWidth, mHeight);
			
			File LogPath = mActivityContext.getExternalFilesDir("gamelogs");
			LogPath.mkdirs();
			String screenCntFrmt = String.format("%04d", screenShotCnt );
			File OutFile = new File( LogPath, "GameScreen" + screenCntFrmt + ".png" );
			FileOutputStream fOut = null;
			try {
				fOut = new FileOutputStream(OutFile);
				bitmap.compress( Bitmap.CompressFormat.PNG, 85, fOut );
			} catch (FileNotFoundException e){}
			
			try {
				fOut.flush();
				fOut.close();
			} catch (IOException e){}
			
			screenShotCnt++;
		}
	}

	private void draw3D()
	{
		mWorld.draw();
		//mSpriteArray.draw();
		//mCube.draw();
		//mLight.draw();
	}

	private void draw2D()
	{
		//mHUD.draw();
	}
     
	public void buttonA(){ /* Do nothing */ }
	public void buttonX(){ /* Do nothing */ }
	public void buttonY(){ /* Do nothing */ }
	public void buttonB(){ /* Do nothing */ }
	
	public void setColor(float r, float g, float b) {
        mRed = r;
        mGreen = g;
        mBlue = b;
    }
}
