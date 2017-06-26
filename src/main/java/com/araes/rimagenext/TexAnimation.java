package com.araes.rimagenext;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TexAnimation
{
	protected MainActivity mActivityContext;
	protected GameRenderer mRenderer;
	protected int mBytesPerFloat = 4;
	public long mTimeLastUpdate = 0;
	public float FPS = 1.0f;
	public float Interval = 1.0f / FPS;
	public float AnimLength = 1; // Length in seconds
	public float AnimProg = 0;
	public float mLERP    = 0;
	public int TexHandle = 0;
	public int TexW = 1;
	public int TexH = 1;
	public int iSt = 0;
	public int jSt = 0;
	public int iCur = 0;
	public int jCur = 0;
	public int iEnd = 0;
	public int jEnd = 0;
	public int frmCnt = 0;
	public FloatBuffer[] Frames = null;
	public FloatBuffer fbCur = null;
	public FloatBuffer fbNxt = null;
	public boolean isLoop = false;
	
	private LogPoster Logger;
	
	public TexAnimation( MainActivity context, GameRenderer renderer, int handle, int w, int h, int ist, int jst, int iend, int jend, float fps, boolean loop ){
		mActivityContext = context;
		mRenderer = renderer;
		TexHandle = handle;
		TexW = w;
		TexH = h;
		iSt = ist;
		jSt = jst;
		iEnd = iend;
		jEnd = jend;
		FPS = fps;
		isLoop = loop;
		
		Logger = new LogPoster();
		Logger.addLogListener( mActivityContext.GameLog );
		
		CalcFrames();
		
		Logger.post( "fbCur reads as " + fbCur );
	}
	
	public void CalcFrames(){
		Interval = 1.0f / FPS;
		iCur = iSt;
		jCur = jSt;
		// Frames are assumed to flow left to right on textures
		// If j > 1, its assume to be a multi-line texture
		//[ 0,0 1,0 2,0 3,0 ] walk down
		//[ 0,1 1,1 2,1 3,1 ] walk right
		//[ 0,2 1,2 2,2 3,2 ] walk left
		//[ 0,3 1,3 2,3 3,3 ] walk up
		frmCnt = (iEnd - iSt + 1) * (jEnd - jSt + 1);
		Logger.post("Calculated frame count is " + Integer.toString(frmCnt) );
		
		Frames = new FloatBuffer[frmCnt];
		AnimLength = Interval * frmCnt;
		int f = 0;
		for( int j = jSt; j <= jEnd; j++ ){
			for( int i = iSt; i <= iEnd; i++ ){
				float[] tFrame = new float[6];
				tFrame = DisplayLayer.ChangeUVToIJofWH( tFrame, i, j, TexW, TexH );
				Logger.post("Initializing buffer for Frame " + Integer.toString(f) );
				Frames[f] = InitializeBuffer( tFrame );
				Logger.post( "Frames[" + Integer.toString(f) + "] reads as " + Frames[f] );
				f++;
			}
		}
		fbCur = Frames[0];
		fbNxt = Frames[0];
		mLERP = 0.0f;
	}
	
	public void update(){
		long TimeCur = mRenderer.mTimeCur;
		if( mTimeLastUpdate > 0 ){
			float Delta = ( TimeCur - mTimeLastUpdate )/1000.0f;
			//Logger.post( "Updating anim, TimeCur is " + Float.toString(TimeCur) + " and Delta is " + Float.toString(Delta) + " and AnimLength is " + Float.toString(AnimLength) + " and AnimProg is " + Float.toString(AnimProg) );
			
			// Figure out how far through our animation we are
			AnimProg += Delta;
			if( AnimProg > AnimLength ){
				if( isLoop ){ AnimProg %= AnimLength; }
				else        { AnimProg  = AnimLength; }
			}
			//Logger.post( "After += Delta, AnimProg is " + Float.toString(AnimProg) );
			
			// Find the current display indices
			int frmIndCur = (int)( AnimProg / AnimLength * frmCnt * 0.99f );
			int frmIndNxt = frmIndCur + 1;
			if( frmIndNxt >= frmCnt ){
				if( isLoop ){ frmIndNxt = 0; }
				else        { frmIndNxt = frmCnt - 1; }
			}
			
			// Assign our texture locations
			fbCur = Frames[frmIndCur];
			fbNxt = Frames[frmIndNxt];
			
			// Calculate our LERP factor between them
			mLERP = ( AnimProg / Interval ) - frmIndCur;mTimeLastUpdate = TimeCur;
		} else {
			mTimeLastUpdate = TimeCur;
		}
	}
	
	private FloatBuffer InitializeBuffer( float[] data )
	{
		Logger.post( "Data[0] is " + data[0] + " prior to buffer init" );
		Logger.post("Allocating the ByteBuffer");
		FloatBuffer fbo = ByteBuffer.allocateDirect(data.length * mBytesPerFloat)
			.order(ByteOrder.nativeOrder()).asFloatBuffer();							
		Logger.post("Putting data in the ByteBuffer");
		fbo.put(data).position(0);
		Logger.post( "fbo reads as " + fbo );
		return fbo;
	}
}
