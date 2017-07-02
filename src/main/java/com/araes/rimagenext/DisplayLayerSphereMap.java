package com.araes.rimagenext;
import java.util.List;
import java.util.ArrayList;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import java.nio.FloatBuffer;
import java.util.zip.*;
import java.nio.*;
import android.app.*;
import java.util.*;

public class DisplayLayerSphereMap extends DisplayLayerMap
{
	private List<WorldPlate> mPlates;
	private List<WorldVert> mPlateVerts;
	private List<WorldVert> mVert;
	private List<List<WorldTri>> mSurface;
	private List<Particle> particles;
	
	private List<WorldVert> skyVerts;;
	private List<List<List<WorldTri>>> mSkyLayer;
	private List<Particle> particlesAir;
	
	public int displayMode = DisplayMode.DEFAULT;

	int mPlateCnt = 14;
	float mMaxPlateAngleVar = (float)Math.PI / 2; // 45 deg
	float mTimeStepPlate = 0.01f;
	float mTimeStepIco = 0.01f;
	float mTimestepEarth = 100.0f; // 100 million years
	float mTimestepWind = 1;

	// Note: always add the .#f to floats
	// Java is stupid and will do math on them like int if you don't
	public float mRadius           = 6371000.1f; // 6371000.0f m
	public float mRadiusInv        = 1.0f/mRadius;
	public float mAreaSurfaceEarth = 4.0f*(float)Math.PI*mRadius*mRadius;//5.10e14f; // m2
	public float mDensityAvgEarth = 1500.1f;//1500.0f; // kg/m3
	public float mDensityAvgWater = 1000.1f; // kg/m3
	public float mDensityAvgAir = 1.21f; // kg/m3
	public float mElevAvgTarget = 10000.1f; // 1cm //10000.0f; // m
	public float mVelPlate = 1000.1f; // 0.1 cm/s -> 1 km/yr
	public float mPctLand = 0.6f;
	public float mDragCoefPlate = 0.5f;
	public float accGrav = 10.0f;
	public float FRepelPntMag = 8.0f;
	public float FRepelCoreMag = 0.5f;

	private float ptSlvLimitSlope = 0.005f*mRadius*mRadius*mRadius;
	private float ptSlvAddSlope = 0.025f*mRadius*mRadius*mRadius;

	private float mTilt;

	// booleans for stepping through phases of world creation
	// using small segments of working time
	// structure step bools
	private boolean initLists      = false;
	private boolean doneStructure  = false;
	private boolean doneWeights    = false;
	private boolean doneInitPtSlv  = false;
	private boolean donePlates     = false;
	private boolean doneSurfaces   = false;
	private boolean doneInitSurfFB = false;
	// earth step bools
	private boolean doneAssignPlateFlow   = false;
	private boolean doneRefineSurface     = false;
	private boolean donePlateSurfaces     = false;
	private boolean doneBuildEarthCFD     = false;
	private boolean doneSetPlateCenters   = false;
	private boolean doneAssignSurfaceFlow = false;
	private boolean doneEarth             = false;
	private boolean doneCrustFlow         = false;
	private boolean doneNormalizeEarth    = false;
	private boolean doneFire              = false;
	private boolean doneInitSkyLayers     = false;
	private boolean doneWindVolumes       = false;
	private boolean doneSetupWind         = false;
	private boolean doneInitSkyBuffers    = false;
	private boolean doneWind              = false;
	private boolean doneWater             = false;
	private boolean doneBiomes            = false;
	private boolean doneLife              = false;
	private boolean doneWorld             = false;

	public float[] weightPlates;
	public float[] weightIco;
	public float[] elevLines;

	private long timeStart;
	private long timeUpdate;

	private float ptSlvCurMvmt;
	private float ptSlvLastMvmt;
	private float ptSlvCurSlope;
	private int ptSlvTotSurface;
	
	public int crustSolveCnt = 0;
	private int crustSolveCntMax = 100;
	private int windSlvCnt = 0;
	private int windSlvCntMax = 100;
	
	protected FloatBuffer mPlatePositions;
	protected FloatBuffer mPlateColors;
	protected FloatBuffer mPlatePointSize;
	
	protected FloatBuffer mIcoPositions;
	protected FloatBuffer mIcoColors;
	protected FloatBuffer mIcoPointSize;
	
	protected FloatBuffer mSurfacePositions;
	protected FloatBuffer mSurfaceNormals;
	protected FloatBuffer mSurfaceColors;
	protected FloatBuffer mSurfaceTexCoords;
	protected ShortBuffer mSurfaceIndexBuffer;
	
	protected FloatBuffer mSkyPositions;
	protected FloatBuffer mSkyNormals;
	protected FloatBuffer mSkyColors;
	protected FloatBuffer mSkyTexCoords;
	protected ShortBuffer mSkyIndexBuffer;

	protected FloatBuffer mSkyPrtPositions;
	protected FloatBuffer mSkyPrtColors;
	protected FloatBuffer mSkyPrtPointSize;

	protected int mPointSizeDataSize = 1;
	protected int BYTES_PER_SHORT = 2;

	private int breakCycle = 0;

	private float prtNumDensity = 4.2f;
	
	private FloatBuffer mPrtPositions;
	private FloatBuffer mPrtColors;
	private FloatBuffer mPrtPointSize;

	private int mAirNumDensity = 10;

	private int skyResCnt = 0;
	private int skyResMax = 1;
	private int skyLayerCnt = 0;
	private int skyLayerCntMax = 5;
	
	public float skyLayerInitH = 2000.0f;

	
	
	public DisplayLayerSphereMap( MainActivity context, GameRenderer renderer ){
		super( context, renderer );
	}

	@Override
	public void update()
	{
		// first, build world
		if( !doneWorld ){
			BuildWorld();
		} else {
			// if world creation is done, deafult to normal update
			super.update();
		}

	}

	private boolean checkUpdate(){
		long timeCur = SystemClock.uptimeMillis();
		if( ( timeCur - timeStart) < timeUpdate ){
			return true;
		} else { return false; }
	}

	public void BuildWorld(){
		//Logger.post( "entered BuildWorld" );

		// get time avail for update
		timeStart = SystemClock.uptimeMillis();
		timeUpdate = mRenderer.mDelta60fps;

		if( !initLists ){
			initializeLists(); initLists = true;
		}

		// find the wireframe shape and dirt coating of planet
		if( !doneStructure ){
			doneStructure = SolveStructure( mPlates, mVert, mSurface.get(0) );
		}

		if( doneStructure && !doneEarth ){
			doneEarth = SolveEarth( mPlates, mVert, mSurface );
		}

		if( doneEarth && !doneFire ){
			displayMode = DisplayMode.ELEV;
			initSurfaceColorFB( mSurface.get( mSurface.size()-1 ) );
			/*// solve heating and resulting wind
			 float tTilt = (float)Math.PI / 18; // 10 deg
		 	SolveFire( mSurface, tTilt );*/
			doneFire = true;
		}

		if( doneFire && !doneWind ){
		 	doneWind = SolveWind( mVert, mSurface, mSkyLayer, particlesAir, skyResMax, skyLayerCntMax, skyLayerInitH );
		}

		// solve water evap/transport and biomes
		if( doneWind && !doneWater ){
			doneWater = SolveWater( mSurface );
		}
		
		if( doneWater && !doneBiomes ){
			doneBiomes = SolveBiomes( mSurface );
		}

		// solve fauna and life coverage, types
		if( doneBiomes && !doneLife ){
			doneLife = SolveLife( mSurface );
		}
	}

	private void initializeLists()
	{
		mPlates = new ArrayList<WorldPlate>();
		mPlateVerts = new ArrayList<WorldVert>();
		mVert = new ArrayList<WorldVert>();
		mSurface = new ArrayList<List<WorldTri>>();
		particles = new ArrayList<Particle>();
		
		mSkyLayer = new ArrayList<List<List<WorldTri>>>();

		List<WorldTri> surface0 = new ArrayList<WorldTri>();
		mSurface.add( surface0 );

		Matrix.setIdentityM( mModelMatrix, 0 );
	}

	private boolean SolveStructure( List<WorldPlate> plates, List<WorldVert> verts, List<WorldTri> surface )
	{
		//Logger.post( "entered SolveStructure" );
		if( !doneWeights ){
			solvePlateIcoWeights();
		}

		if( !donePlates ){
			donePlates = SolvePointsOnSphere( weightPlates, mPlateVerts, null, mTimeStepPlate );
			if( donePlates ){
				Logger.post( "------triggered donePlates" );
				for( WorldVert vert : mPlateVerts ){
					WorldPlate plateNew = new WorldPlate( this, vert );
					plateNew.world = this;
					plates.add( plateNew );
					plateNew.pos.mult( mRadius );
				}
			}
		//} else if( !doneIco ){
			// creat icosahedron verts and surfaces
			//Logger.post( "entering ico solve pointonsphere" );
			//doneIco = SolvePointsOnSphere( weightIco, verts, surface, mTimeStepIco );
		}

		if( donePlates && !doneSurfaces ){
			Logger.post( "enter solve iso surface" );
			doneSurfaces = SolveIsoSurfaces2( weightIco, verts, surface );
		}
		
		// add whatever points are done to our renderable objects
		if( mVert.size() > 0 ){
			Logger.post( "initPointFB" );
			initPointFB( mVert, surface );
		}

		if( doneSurfaces && !doneInitSurfFB ){
			Logger.post( "done surface, initFB" );
			displayMode = DisplayMode.DEFAULT;
			initSurfaceFB( surface );
			doneInitSurfFB = true;
		}
		
		if( doneInitSurfFB ){ return true;  }
		else                { return false; }
	}

	private void solvePlateIcoWeights()
	{
		// Create surface area partitions in plates
		// Avg area +- rand variance
		weightPlates = new float[ mPlateCnt ];
		float weightAvg = 1.0f / (float)mPlateCnt;
		float weightVarMax = 0.25f;
		float weightVarOld = 0.0f;
		int lastCnt = mPlateCnt - 1;
		for( int i = 0; i < mPlateCnt; i++ ){
			float weightVar = ((float)Math.random()-0.5f)*weightVarMax;
			weightPlates[i] = weightAvg*(1+weightVar-weightVarOld);
			if( i == lastCnt ){
				weightPlates[0] -= weightAvg*weightVar;
			}
			weightVarOld = weightVar;
		}

		// create even partitions for an icosahedron
		weightIco = new float[ 12 ];
		weightAvg = 1.0f / 12.0f;
		for( int i = 0; i < 12; i++ ){
			weightIco[i] = weightAvg;
		}
	}

	private boolean SolvePointsOnSphere( float[] weights, List<WorldVert> points, List<WorldTri> surface, float timestep )
	{
		//Logger.post( "entered SolvePointsOnSphere" );
		boolean donePointSolve = false;

		// Solve Repeling Point Positions on r=1 sphere
		// weights list should add to 1
		if( !doneInitPtSlv ){
			initPointSolve();
		}

		while( checkUpdate() ){
			int pntCnt = points.size();
			if( ( ( ptSlvCurSlope > ptSlvLimitSlope ) || ( pntCnt < weights.length ) ) || !points.get(pntCnt-1).hasTriggered ){
				//Logger.post( "Cnt: " + ptSlvCnt + " points " + pntCnt + " cur mvmt " + ptSlvCurMvmt + " slope " + ptSlvCurSlope );
				if( ( pntCnt < weights.length ) && ( ptSlvCurSlope < ptSlvAddSlope ) ){
					if( pntCnt == 0 ){
						WorldVert vertNew = addPointToSphere( points, weights, ptSlvAddSlope );
						ptSlvTotSurface += vertNew.pctSurface;
					} else if( points.get(pntCnt-1).hasTriggered ){
						WorldVert vertNew = addPointToSphere( points, weights, ptSlvAddSlope );
						ptSlvTotSurface += vertNew.pctSurface;
					}
				}

				// for each point, Calculate movement
				// for each point pair, Calculate pnt to pnt repulsion
				solveBodyForces( points );

				// add pos and vel based forces, 
				solveSurfaceForces( points );

				// update vel, update pos
				// Calculate movement avg
				ptSlvCurMvmt = solvePointMovement( points, surface );
				ptSlvCurSlope = (float)Math.abs(1 - ptSlvLastMvmt/ptSlvCurMvmt);
				ptSlvLastMvmt = ptSlvCurMvmt;

				// check tp see if close enough to surface to create new triangles
				checkReachSurface( points, surface );
			} else {
				donePointSolve = true;
				doneInitPtSlv = false;
			}
		}

		// add whatever points are done to our renderable objects
		if( points !=  null ){
			Logger.post( "initPointFB" );
			initPointFB( points, surface );
		}

		return donePointSolve;
	}

	private void initPointSolve()
	{
		ptSlvLastMvmt = 0.0f;
		ptSlvCurSlope = 0.02f*mRadius;
		ptSlvTotSurface = 0;
		doneInitPtSlv = true;
	}

	private WorldVert addPointToSphere(List<WorldVert> points, float[] weights, float ptSlvAddSlope)
	{
		Logger.post("adding point");
		// Generate new vertex w weight
		int p = points.size();
		WorldVert pnt = new WorldVert( p, weights[p] );
		float phi = ((p/3) * 60) / 180.0f * (float)Math.PI;
		float theta = ((p%3) * 120 + (p/3)%2 * 180) / 180.0f * (float)Math.PI;
		float xSt = (float)Math.cos(theta);
		float ySt = (float)Math.sin(theta);
		float zSt = (float)( -Math.cos(phi) );
		pnt.pos = new Vec3( xSt, ySt, zSt );
		pnt.pos = pnt.pos.norm().mult( 0.1f*mRadius );
		pnt.vel = pnt.pos.norm().mult( 0.1f*mRadius );
		points.add( pnt );
		// although vert have 
		pnt.pctSurface = weights[p];
		Logger.post("p; " + p + " pnt.pos; " + pnt.pos.toS() + " mass " + pnt.mass );
		String weightStr = "";
		for( float weight : weights ){
			weightStr += weight + " ";
		}
		Logger.post("weights: " + weightStr );
		return pnt;
	}

	private void solveBodyForces(List<WorldVert> points)
	{
		float mRInvSq = mRadiusInv * mRadiusInv;
		for( int i = 0; i < points.size(); i++ ){
			// for each point, look at each other point
			// calculate reverse gravity based on mass pair
			for( int j = i+1; j < points.size(); j++ ){
				WorldVert iPnt = points.get(i);
				WorldVert jPnt = points.get(j);

				// vector between pnts
				Vec3 delta = iPnt.pos.sub(jPnt.pos);
				float dist = delta.length()*mRadiusInv;
				Vec3 norm = delta.div( dist );

				// repulsion like gravity
				float repelF = FRepelPntMag * iPnt.pctSurface * jPnt.pctSurface / (dist*dist) * mRadius * mRadius * 0.25f;
				Vec3 repelVec = norm.mult( repelF );
				
				//Logger.post( "repelF i,j,F " + i + ", " + j + ", " + repelF );

				// change acc oppositely
				iPnt.acc = iPnt.acc.add( repelVec.div(iPnt.pctSurface) );
				jPnt.acc = jPnt.acc.sub( repelVec.div(jPnt.pctSurface) );
			}
		}
	}

	private void solveSurfaceForces(List<WorldVert> points)
	{
		float mRInvSq = mRadiusInv * mRadiusInv;
		for( int i = 0; i < points.size(); i++ ){
			WorldVert iPnt = points.get(i);

			// Apply bouyancy based on dist from center
			// if small dist, large bouyancy 0-1
			float dist = iPnt.pos.length();
			float coreRepelF = FRepelCoreMag / iPnt.pctSurface * (1-dist)*(1-dist) * mRadiusInv * 0.25f;
			Vec3 coreRepelVec = iPnt.pos.norm().mult( coreRepelF );
			//Logger.post("i; " + i + " i.pos; " + iPnt.pos.toS() + "dist; " + dist + " coreRepelVec; " + coreRepelVec.toS() );

			// apply drag near sphere boundary
			// small v, small drag
			// small distance, large drag
			// much larger perpendicular to plane
			float ptVel = iPnt.vel.length();
			//Vec3 velDampVec = ( iPnt.vel.mult( ptVel * ptVel ) ).mult( -mDragCoefPlate );

			//Logger.post( "CoreRepelVec " + coreRepelVec.toS() );//+ ", dampVec " + velDampVec.toS() );
			
			// update acc
			iPnt.acc = iPnt.acc.add( coreRepelVec.div( iPnt.pctSurface ) );
			//iPnt.acc = iPnt.acc.add( velDampVec.div( iPnt.pctSurface ) );
			//Logger.post("i; " + i + " i.acc; " + iPnt.acc.toS() );
		}
	}

	private float solvePointMovement(List<WorldVert> points, List<WorldTri> surface)
	{
		Vec3 vecMvmt = new Vec3( 0.0f, 0.0f, 0.0f );

		for( int i = 0; i < points.size(); i++ ){
			WorldVert iPnt = points.get(i);

			// update vel
			Vec3 velOld = iPnt.vel;
			iPnt.vel = velOld.add( iPnt.acc.mult( mTimeStepPlate ) );
			//Logger.post("i; " + i + " i.vel; " + iPnt.vel.toS() );

			// add vel to mvmt total
			vecMvmt.addEq( iPnt.vel );

			// update pos
			Vec3 posDelta = iPnt.vel.add( velOld ).mult( 0.5f * mTimeStepPlate );
			iPnt.pos = iPnt.pos.add( posDelta );
			//Logger.post("i; " + i + " i.pos; " + iPnt.pos.toS() );

			// zero out acc again
			iPnt.acc.eq( 0, 0, 0 );
		}

		//Logger.post( "vecMvmt " + vecMvmt.toS() );

		return vecMvmt.length();
	}

	private void checkReachSurface(List<WorldVert> points, List<WorldTri> surface)
	{
		for( int i = 0; i < points.size(); i++ ){
			WorldVert iPnt = points.get(i);

			// if pos outside sphere
			float dist = iPnt.pos.length();
			if( dist > mRadius ){
				// return to boundary
				iPnt.pos = iPnt.pos.div( dist ).mult( mRadius );
				iPnt.hasTriggered = true;
			}// end dist check
		}
	}

	
	
	private boolean SolveIsoSurfaces2( float[] weights, List<WorldVert> verts, List<WorldTri> surface ){
		float X = 0.525731112119133606f;
		float Z = 0.850650808352039932f;
		
		float[][] vData = new float[][]{
			{-X, 0.0f, Z}, {X, 0.0f, Z}, {-X, 0.0f, -Z}, {X, 0.0f, -Z},    
			{0.0f, Z, X}, {0.0f, Z, -X}, {0.0f, -Z, X}, {0.0f, -Z, -X},    
			{Z, X, 0.0f}, {-Z, X, 0.0f}, {Z, -X, 0.0f}, {-Z, -X, 0.0f} 
		};
		
		short[][] tri = new short[][]{ 
			{0,4,1}, {0,9,4}, {9,5,4}, {4,5,8}, {4,8,1},    
			{8,10,1}, {8,3,10}, {5,3,8}, {5,2,3}, {2,7,3},    
			{7,10,3}, {7,6,10}, {7,11,6}, {11,0,6}, {0,1,6}, 
			{6,1,10}, {9,0,11}, {9,11,2}, {9,2,5}, {7,2,11}
		};
		
		for( int p = 0; p < vData.length; p++ ){
			WorldVert pnt = new WorldVert( p, 0.0833333f );
			pnt.pos = new Vec3( vData[p][0], vData[p][1], vData[p][2] ).mult( mRadius );
			verts.add( pnt );
		}
		
		for( int t= 0; t < tri.length; t++ ){
			surface.add( new WorldTri( this, null, 0.05f, verts.get( tri[t][0] ), verts.get( tri[t][1] ), verts.get( tri[t][2] ) ) );
		}
		
		// cycle thru corners
		// compare w other tri corners
		// if 2 match, assign those sides as twins
		// 0 & 1 -> 0, 1 & 2 -> 1, 2 & 0 -> 2
		boolean[] matchI = new boolean[3];
		boolean[] matchJ = new boolean[3];
		int matchCnt;
		for( int i = 0; i < tri.length; i++ ){
			for( int j = i+1; j < tri.length; j++ ){
				matchI[0] = false; matchI[1] = false; matchI[2] = false;
				matchJ[0] = false; matchJ[1] = false; matchJ[2] = false;
				matchCnt = 0;
				
				// scan sides vs all other sides
				for( int si = 0; si < 3; si++ ){
					for( int sj = 0; sj < 3; sj++ ){
						if( tri[i][si] == tri[j][sj] ){
							matchI[si] = true;
							matchJ[sj] = true;
							matchCnt++;
						}
					}
				}
				
				// takes edges that match and pair them
				// edge.side == side, edge.twin == edge
				if( matchCnt == 2 ){
					int eI = -1;
					int eJ = -1;
					for( int m = 0; m < 3; m++ ){
						int mp1 = (m+1)%3;
						if( matchI[m] && matchI[mp1] ){ eI = m; }
						if( matchJ[m] && matchJ[mp1] ){ eJ = m; }
					}
					
					surface.get(i).edge.get(eI).side = surface.get(j);
					surface.get(j).edge.get(eJ).side = surface.get(i);
					
					surface.get(i).edge.get(eI).twin = surface.get(j).edge.get(eJ);
					surface.get(j).edge.get(eJ).twin = surface.get(i).edge.get(eI);
				}
			}
		}
		
		return true;
	}

	private void initPointFB(List<WorldVert> points, List<WorldTri> surface)
	{
		FloatBuffer fboP = null, fboC = null, fboPS = null;

		int pntsDraw = points.size();
		
		int d = 0;
		int c = 0;
		int ps = 0;
		float[] pointPos = new float[ pntsDraw * mPositionDataSize ];
		float[] pointColor = new float[ pntsDraw * mColorDataSize ];
		float[] pointSize = new float[ pntsDraw * mPointSizeDataSize ];
		for( int i = 0; i < pntsDraw; i++ ){
			WorldVert point = points.get(i);
			pointPos[ d++ ] = point.pos.gX()*mRadiusInv;
			pointPos[ d++ ] = point.pos.gY()*mRadiusInv;
			pointPos[ d++ ] = point.pos.gZ()*mRadiusInv;
			pointColor[ c++ ] = point.pos.gX()*mRadius*0.4f + 0.6f;
			pointColor[ c++ ] = point.pos.gY()*mRadius*0.4f + 0.6f;
			pointColor[ c++ ] = point.pos.gZ()*mRadius*0.4f + 0.6f;
			pointColor[ c++ ] = 1.0f;
			pointSize[ ps++ ] = ps*3.0f/(float)pntsDraw+6.0f;//point.pos.gZ()*3.0f + 6.0f;
		}
		//Logger.post( "pos 0, 1, & 2 " + pointPos[0] + ", " + pointPos[1] + ", " + pointPos[2] );
		fboP  = InitializeBuffer( fboP, pointPos );
		fboC  = InitializeBuffer( fboC, pointColor );
		fboPS = InitializeBuffer( fboPS, pointSize);

		if( surface == null ){ mPlatePositions = fboP; mPlateColors = fboC; mPlatePointSize = fboPS; }
		else                 { mIcoPositions   = fboP; mIcoColors   = fboC; mIcoPointSize   = fboPS; }
	}
	
	private void initSurfaceFB( List<WorldTri> surface )
	{
		FloatBuffer fboP = null, fboN = null, fboC = null, fboT = null;
		
		mTris = surface.size();

		int p = 0;
		int n = 0;
		int t = 0;
		float[] TexCoorData = new float[] {												
			0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f
		};

		float[] triPosData = new float[ mTris * 3 * mPositionDataSize ];
		float[] triNormalData = new float[ mTris * 3 * mNormalDataSize ];
		float[] triTexData = new float[ mTris * 3 * mTextureCoordinateDataSize ];
		for( WorldTri tri : surface ){
			Vec3 posNorm = tri.pos.norm();
			for( int i = 0; i < 3; i++ ){
				String out = "";
				//Logger.post( "normals, pos, color, tex " );
				triNormalData[ n ] = posNorm.gX(); n++; //tri.zLocal_3D.gX(); n++; out += " " + tri.zLocal_3D.gX();
				triNormalData[ n ] = posNorm.gY(); n++; //tri.zLocal_3D.gY(); n++; out += " " + tri.zLocal_3D.gY();
				triNormalData[ n ] = posNorm.gZ(); n++; //tri.zLocal_3D.gZ(); n++; out += " " + tri.zLocal_3D.gZ();
				triPosData[ p ]    = tri.corner.get(i).pos.gX()*mRadiusInv; p++; out += " || " + tri.corner.get(i).pos.gX();
				triPosData[ p ]    = tri.corner.get(i).pos.gY()*mRadiusInv; p++; out += " " + tri.corner.get(i).pos.gY();
				triPosData[ p ]    = tri.corner.get(i).pos.gZ()*mRadiusInv; p++; out += " " + tri.corner.get(i).pos.gZ();
				triTexData[ t ]    = TexCoorData[ t%12 ]; out += " || " + TexCoorData[ t%12 ]; t++;
				triTexData[ t ]    = TexCoorData[ t%12 ]; out += " " + TexCoorData[ t%12 ]; t++;
				//Logger.post( out );
			}
		}
		
		initSurfaceColorFB( surface );
		
		int vLen = mTris*3;
		short[] drawOrder = new short[ mTris*3 ];
		for( short i = 0; i < drawOrder.length; i++ ){
			drawOrder[i] = (short)(drawOrder.length - 1 - i);
		}
		
		// initialize byte buffer for the draw list
		mSurfaceIndexBuffer = ByteBuffer
			.allocateDirect(drawOrder.length * BYTES_PER_SHORT).order(ByteOrder.nativeOrder())
			.asShortBuffer();
		mSurfaceIndexBuffer.put(drawOrder).position(0);
		
		//Logger.post( "pos 0, 1, & 2 " + pointPos[0] + ", " + pointPos[1] + ", " + pointPos[2] );
		fboP = InitializeBuffer( fboP, triPosData );
		mSurfacePositions = fboP;

		fboN = InitializeBuffer( fboN, triNormalData );
		mSurfaceNormals = fboN;

		fboT = InitializeBuffer( fboT, triTexData );
		mSurfaceTexCoords = fboT;
	}
	
	private void initSurfaceColorFB( List<WorldTri> surface )
	{
		FloatBuffer fboC = null;

		mTris = surface.size();
		
		if( elevLines != null ){
			Logger.post( "Elevation lines" + ", 6 " + elevLines[6] + ", " + elevLines[9] );
		}

		int c = 0;
		float r = 0.0f, g = 0.0f, b = 0.0f;
		float[] triColorData = new float[ mTris * 3 * mColorDataSize ];
		for( WorldTri tri : surface ){
			if( displayMode == DisplayMode.RANDOM ){
				r = (float)Math.random();
				g = (float)Math.random();
				b = (float)Math.random();
			}
			for( int i = 0; i < 3; i++ ){
				String out = "";
				if( displayMode == DisplayMode.DEFAULT ){
					triColorData[ c ]  = tri.corner.get(i).pos.gX()*mRadius*0.4f + 0.6f; c++;
					triColorData[ c ]  = tri.corner.get(i).pos.gY()*mRadius*0.4f + 0.6f; c++;
					triColorData[ c ]  = tri.corner.get(i).pos.gZ()*mRadius*0.4f + 0.6f; c++;
				} else if( displayMode == DisplayMode.RANDOM ){
					triColorData[ c ]  = r; c++;
					triColorData[ c ]  = g; c++;
					triColorData[ c ]  = b; c++;
				} else if( displayMode == DisplayMode.PLATE ){
					if( tri.mPlate != null ){
						triColorData[ c ]  = tri.mPlate.mColor.gX(); c++;
						triColorData[ c ]  = tri.mPlate.mColor.gY(); c++;
						triColorData[ c ]  = tri.mPlate.mColor.gZ(); c++;
					} else {
						triColorData[ c ]  = 1.0f; c++;
						triColorData[ c ]  = 1.0f; c++;
						triColorData[ c ]  = 1.0f; c++;
					}
				} else if( displayMode == DisplayMode.WIRE_VEL || displayMode == DisplayMode.WIRE_BNC ){
					triColorData[ c ]  = 1.0f; c++;
					triColorData[ c ]  = 1.0f; c++;
					triColorData[ c ]  = 1.0f; c++;
				} else if( displayMode == DisplayMode.ELEV ){
					//Logger.post( "corner " + i + " elev " + tri.corner.get(i).elevation + " scaled " + ( tri.corner.get(i).elevation - elevMin ) / elevScale );
					Vec3 tColor = findElevColor( tri.corner.get(i).elevation, elevLines );
					triColorData[ c ]= tColor.gX(); c++;
					triColorData[ c ]= tColor.gY(); c++;
					triColorData[ c ]= tColor.gZ(); c++;
				} else if( displayMode == DisplayMode.FLAT ){
					triColorData[ c ]  = 1.0f; c++;
					triColorData[ c ]  = 1.0f; c++;
					triColorData[ c ]  = 1.0f; c++;
				} else {
					triColorData[ c ]  = tri.pos.gX()*mRadius*0.4f + 0.6f; c++;
					triColorData[ c ]  = tri.pos.gY()*mRadius*0.4f + 0.6f; c++;
					triColorData[ c ]  = tri.pos.gZ()*mRadius*0.4f + 0.6f; c++;
				}
				triColorData[ c ]  = 1.0f; out += " " + triColorData[ c ]; c++;
				//Logger.post( out );
			}
		}

		fboC = InitializeBuffer( fboC, triColorData );
		mSurfaceColors = fboC;
	}
	
	private Vec3 findElevColor( float elevation, float[] elevLines )
	{
		// elev should be 0 - 1
		float outRatio;
		//Logger.post( "elevation " + elevation );
		Vec3 out = new Vec3();
		if( elevation < elevLines[0] ){
			outRatio = elevation / elevLines[0];
			out = new Vec3( 0.0f, 0.0f, 0.5f ).mult(outRatio);
		} else if( elevation < elevLines[1] ){
			outRatio = ( elevation - elevLines[0] )/( elevLines[1] - elevLines[0] );
			out = ( new Vec3( 0.0f, 0.0f, 0.75f ).mult(outRatio) ).add( new Vec3( 0.0f, 0.0f, 0.5f ).mult(1-outRatio) );
		} else if( elevation < elevLines[3] ){
			outRatio = ( elevation - elevLines[1] )/( elevLines[3] - elevLines[1] );
			out = ( new Vec3( 0.0f, 0.0f, 1.0f ).mult(outRatio) ).add( new Vec3( 0.0f, 0.0f, 0.75f ).mult(1-outRatio) );
		} else if( elevation < elevLines[4] ){
			outRatio = ( elevation - elevLines[3] )/( elevLines[4] - elevLines[3] );
			out = ( new Vec3( 0.25f, 0.25f, 1.0f ).mult(outRatio) ).add( new Vec3( 0.0f, 0.0f, 1.0f ).mult(1-outRatio) );
		} else if( elevation < elevLines[5] ){
			outRatio = ( elevation - elevLines[4] )/( elevLines[5] - elevLines[4] );
			out = ( new Vec3( 0.0f, 1.0f, 1.0f ).mult(outRatio) ).add( new Vec3( 0.25f, 0.25f, 1.0f ).mult(1-outRatio) );;
		} else if( elevation < elevLines[6] ){
			outRatio = ( elevation - elevLines[5] )/( elevLines[6] - elevLines[5] );
			out = ( new Vec3( 0.0f, 1.0f, 0.0f ).mult(outRatio) ).add( new Vec3( 0.0f, 1.0f, 1.0f ).mult(1-outRatio) );
		} else if( elevation < elevLines[7] ){
			outRatio = ( elevation - elevLines[6] )/( elevLines[7] - elevLines[6] );
			out = ( new Vec3( 0.0f, 0.5f, 0.0f ).mult(outRatio) ).add( new Vec3( 0.0f, 1.0f, 0.0f ).mult(1-outRatio) );
			out = new Vec3( 0.0f, 0.5f, 0.0f );
		} else if( elevation < elevLines[8] ){
			outRatio = ( elevation - elevLines[7] )/( elevLines[8] - elevLines[7] );
			out = ( new Vec3( 0.5f, 0.5f, 0.5f ).mult(outRatio) ).add( new Vec3( 0.0f, 0.5f, 0.0f ).mult(1-outRatio) );
		} else {
			outRatio = ( elevation - elevLines[8] )/( elevLines[9] - elevLines[8] );
			out = ( new Vec3( 1.0f, 1.0f, 1.0f ).mult(outRatio) ).add( new Vec3( 0.5f, 0.5f, 0.5f ).mult(1-outRatio) );
		}

		return out;
	}
	
	private void initSurfacePosFBwElev(List<WorldTri> surface )
	{
		//Logger.post( "Reinitialoze surface w weights");
		FloatBuffer fboP = null;

		mTris = surface.size();

		int p = 0;

		float[] triPosData = new float[ mTris * 3 * mPositionDataSize ];
		for( WorldTri tri : surface ){
			for( int i = 0; i < 3; i++ ){
				WorldVert cI = tri.corner.get(i);
				triPosData[ p ] = cI.pos.gX() * mRadiusInv; p++;
				triPosData[ p ] = cI.pos.gY() * mRadiusInv; p++;
				triPosData[ p ] = cI.pos.gZ() * mRadiusInv; p++;
			}
		}

		//Logger.post( "pos 0, 1, & 2 " + pointPos[0] + ", " + pointPos[1] + ", " + pointPos[2] );
		fboP = InitializeBuffer( fboP, triPosData );
		mSurfacePositions = fboP;
	}

	private void initParticleFB()
	{
		FloatBuffer fboP = null, fboC = null, fboPS = null;

		int d = 0;
		int c = 0;
		int ps = 0;

		float[] pointPos = new float[ particles.size() * mPositionDataSize ];
		float[] pointColor = new float[ particles.size() * mColorDataSize ];
		float[] pointSize = new float[ particles.size() * mPointSizeDataSize ];

		for( int i = 0; i < particles.size(); i++ ){
			Particle point = particles.get(i);
			pointPos[ d++ ] = point.pos.gX()*mRadiusInv;
			pointPos[ d++ ] = point.pos.gY()*mRadiusInv;
			pointPos[ d++ ] = point.pos.gZ()*mRadiusInv;
			if( displayMode == DisplayMode.WIRE_VEL ){
				Vec3 velPrt = ( ( point.parent.xLocal_3D.mult( point.parent.mFlowDir_2D.gX() ) ).add( point.parent.yLocal_3D.mult( point.parent.mFlowDir_2D.gY() ) ) ).norm();
				pointColor[ c++ ] = velPrt.gX();
				pointColor[ c++ ] = velPrt.gY();
				pointColor[ c++ ] = velPrt.gZ();
				pointColor[ c++ ] = 1.0f;
			} else if( displayMode == DisplayMode.WIRE_BNC ){
				pointColor[ c++ ] = 1.0f; //point.pos.gX()*0.4f + 0.6f;
				pointColor[ c++ ] = ( point.triggeredTriShift ? 1.0f : 0.0f );//point.pos.gY()*0.4f + 0.6f;
				pointColor[ c++ ] = ( point.triggeredSideBC ? 1.0f : 0.0f );//point.pos.gZ()*0.4f + 0.6f;
				pointColor[ c++ ] = 1.0f;
			} else if( displayMode == DisplayMode.WIRE_SKY ){
				pointColor[ c++ ] = 1.0f;
				pointColor[ c++ ] = 1.0f;
				pointColor[ c++ ] = 1.0f;
				pointColor[ c++ ] = 1.0f;
			} else {
				pointColor[ c++ ] = 1.0f;
				pointColor[ c++ ] = 0.0f;
				pointColor[ c++ ] = 0.0f;
				pointColor[ c++ ] = 1.0f;
			}

			pointSize[ ps++ ] = 8.0f;//point.pos.gZ()*3.0f + 6.0f;
		}
		//Logger.post( "pos 0, 1, & 2 " + pointPos[0] + ", " + pointPos[1] + ", " + pointPos[2] );
		fboP  = InitializeBuffer( fboP, pointPos );
		fboC  = InitializeBuffer( fboC, pointColor );
		fboPS = InitializeBuffer( fboPS, pointSize);

		mPrtPositions = fboP;
		mPrtColors = fboC;
		mPrtPointSize = fboPS;
	}
	
	private void initSkyLayerFB( List<List<WorldTri>> skyLayerRes )
	{
		Logger.post( "Entered initSkyLayerFB" );
		//Logger.post( "Reinitialoze surface w weights");
		FloatBuffer fboP = null, fboN = null, fboC = null, fboT = null;

		int layers = skyLayerRes.size();
		mTris = skyLayerRes.get(0).size() * layers;
		int dLen = mTris * 3;
		
		Logger.post( "layers, " + layers + " mTris, " + mTris );

		int p = 0;
		int n = 0;
		int c = 0;
		int t = 0;
		float[] TexCoorData = new float[] {												
			0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f
		};

		float[] triNormalData = new float[ dLen * mNormalDataSize ];
		float[] triPosData = new float[ dLen * mPositionDataSize ];
		float[] triColorData = new float[ dLen * mColorDataSize ];
		float[] triTexData = new float[ dLen * mTextureCoordinateDataSize ];
		for( List<WorldTri> layer : skyLayerRes ){
			for( WorldTri tri : layer ){
				Vec3 posNorm = tri.pos.norm();
				for( int i = 0; i < 3; i++ ){
					WorldVert cI = tri.corner.get(i);
					Vec3 tColor = findElevColor( cI.elevation, elevLines );
					triColorData[ c ]= tColor.gX(); c++;
					triColorData[ c ]= tColor.gY(); c++;
					triColorData[ c ]= tColor.gZ(); c++;
					triColorData[ c ]= 1.0f; c++;
					triNormalData[ n ] = posNorm.gX(); n++;
					triNormalData[ n ] = posNorm.gY(); n++;
					triNormalData[ n ] = posNorm.gZ(); n++;
					triPosData[ p ]    = tri.corner.get(i).pos.gX()*mRadiusInv; p++;
					triPosData[ p ]    = tri.corner.get(i).pos.gY()*mRadiusInv; p++;
					triPosData[ p ]    = tri.corner.get(i).pos.gZ()*mRadiusInv; p++;
					triTexData[ t ]    = TexCoorData[ t%12 ]; t++;
					triTexData[ t ]    = TexCoorData[ t%12 ]; t++;
				}
			}
		}

		
		short[] drawOrder = new short[ dLen ];
		for( short i = 0; i < dLen; i++ ){
			drawOrder[i] = (short)(dLen - 1 - i);
		}

		// initialize byte buffer for the draw list
		mSkyIndexBuffer = ByteBuffer
			.allocateDirect(drawOrder.length * BYTES_PER_SHORT).order(ByteOrder.nativeOrder())
			.asShortBuffer();
		mSkyIndexBuffer.put(drawOrder).position(0);

		//Logger.post( "pos 0, 1, & 2 " + pointPos[0] + ", " + pointPos[1] + ", " + pointPos[2] );
		fboP = InitializeBuffer( fboP, triPosData );
		mSkyPositions = fboP;

		fboN = InitializeBuffer( fboN, triNormalData );
		mSkyNormals = fboN;

		fboT = InitializeBuffer( fboT, triTexData );
		mSkyTexCoords = fboT;
		
		fboC = InitializeBuffer( fboC, triColorData );
		mSkyColors = fboC;
	}
	
	private void initSkyPointFB( List<List<List<WorldTri>>> skyLayer )
	{
		Logger.post( "Entered initSkyPointFB" );
		FloatBuffer fboP = null, fboC = null, fboPS = null;

		int d = 0;
		int c = 0;
		int ps = 0;

		int prtCnt = 0;
		for( List<List<WorldTri>> res : skyLayer ){
			int layerQty = res.size()-1;
			Logger.post( "layerQty " + layerQty );
			int triQty   = res.get(0).size();
			Logger.post( "triQty " + triQty );
			int prtQty   = res.get(0).get(0).particles.size();
			Logger.post( "prtQty " + prtQty );
			prtCnt += layerQty * triQty * prtQty;
		}
		
		float[] pointPos   = new float[ prtCnt * mPositionDataSize ];
		float[] pointColor = new float[ prtCnt * mColorDataSize ];
		float[] pointSize  = new float[ prtCnt * mPointSizeDataSize ];

		for( List<List<WorldTri>> res : skyLayer ){
			for( List<WorldTri> layer : res ){
				for( WorldTri tri : layer ){
					for( Particle point : tri.particles ){
						// Position
						pointPos[ d++ ] = point.pos.gX()*mRadiusInv;
						pointPos[ d++ ] = point.pos.gY()*mRadiusInv;
						pointPos[ d++ ] = point.pos.gZ()*mRadiusInv;
						// Color
						if( displayMode == DisplayMode.WIRE_VEL ){
							Vec3 velPrt = point.vel3D.norm();
							pointColor[ c++ ] = velPrt.gX();
							pointColor[ c++ ] = velPrt.gY();
							pointColor[ c++ ] = velPrt.gZ();
							pointColor[ c++ ] = 1.0f;
						} else if( displayMode == DisplayMode.WIRE_BNC ){
							pointColor[ c++ ] = 1.0f; //point.pos.gX()*0.4f + 0.6f;
							pointColor[ c++ ] = ( point.triggeredTriShift ? 1.0f : 0.0f );//point.pos.gY()*0.4f + 0.6f;
							pointColor[ c++ ] = ( point.triggeredSideBC ? 1.0f : 0.0f );//point.pos.gZ()*0.4f + 0.6f;
							pointColor[ c++ ] = 1.0f;
						} else if( displayMode == DisplayMode.WIRE_SKY ){
							pointColor[ c++ ] = 1.0f;
							pointColor[ c++ ] = 1.0f;
							pointColor[ c++ ] = 1.0f;
							pointColor[ c++ ] = 1.0f;
						} else {
							pointColor[ c++ ] = 1.0f;
							pointColor[ c++ ] = 0.0f;
							pointColor[ c++ ] = 0.0f;
							pointColor[ c++ ] = 1.0f;
						}
						// Size
						pointSize[ ps++ ] = 4.0f; //point.pos.gZ()*3.0f + 6.0f;
					}
				}
			}
		}
		
		//Logger.post( "pos 0, 1, & 2 " + pointPos[0] + ", " + pointPos[1] + ", " + pointPos[2] );
		fboP  = InitializeBuffer( fboP, pointPos );
		fboC  = InitializeBuffer( fboC, pointColor );
		fboPS = InitializeBuffer( fboPS, pointSize);

		mSkyPrtPositions = fboP;
		mSkyPrtColors    = fboC;
		mSkyPrtPointSize = fboPS;
		
		Logger.post( "PrtPos 6 & 9 " + mSkyPrtPositions.get(6) + ", " + mSkyPrtPositions.get(9) );
		mSkyPrtPositions.position(0);
		Logger.post("Leaving initSkyPointFB" );
	}

	private boolean SolveEarth( List<WorldPlate> plates, List<WorldVert> verts, List<List<WorldTri>> surface )
	{
		Logger.post( "Entered solve earth");
		if( !doneRefineSurface ){
			// Divide mesh to size necessary for earth
			// Break until tris > plates*4
			// Guarantee unique plate - tri sets
			// Save each layer for multires tree
			doneRefineSurface = RefineSurface( plates, verts, surface, plates.size() * 32 );
		}

		if( doneRefineSurface && !doneSetPlateCenters ){
			// Assign plates membership to
			// nearest tile in current layer
			// descend tree
			doneSetPlateCenters = SetPlateCenters( plates, surface );
		}
		
		// Assign flow directions and magnitude to plates
		if( doneSetPlateCenters && !doneAssignPlateFlow ){
			doneAssignPlateFlow = AssignPlateFlow( plates );
		}
		
		if( doneAssignPlateFlow && !donePlateSurfaces ){
			// Play tile grabbing game to build the plate surface tri groups
			displayMode = DisplayMode.PLATE;
			donePlateSurfaces = BuildPlateSurfaces( plates, surface.get(surface.size()-1) );
		}
		
		if( donePlateSurfaces && !doneAssignSurfaceFlow){
			// assign plate flow to all member tris
			doneAssignSurfaceFlow = AssignSurfaceFlow( surface );
		}

		if( doneAssignSurfaceFlow && !doneBuildEarthCFD ){
			doneBuildEarthCFD =  BuildCFDDomains( plates );
		}

		if( doneBuildEarthCFD && !doneCrustFlow ){
			// Flow dirt across tiles
			displayMode = DisplayMode.WIRE_VEL;
			doneCrustFlow = SolveCrustFlow( plates, verts );
		}
		
		if( !doneNormalizeEarth && doneCrustFlow ){
			NormalizeEarth( verts );
			CalcElevLines( verts );
			doneNormalizeEarth = true;
		}
		
		if( doneNormalizeEarth && !doneEarth ){
			// Another mesh divide to make it look nice
			doneEarth = RefineSurface( plates, verts, surface, plates.size() * 128 );
			initSurfacePosFBwElev( surface.get(surface.size()-1) );
		}
		
		return doneEarth;
	}

	private void CalcElevLines(List<WorldVert> verts)
	{
		List<Float> elevSort = new ArrayList<Float>();
		int bands = 10;
		elevLines = new float[bands];
		
		for( WorldVert vert : mVert ){
			elevSort.add( vert.elevation );
		}

		Collections.sort( elevSort );

		// sort into groups w cutoffs based on elev
		int vPerBand = elevSort.size() / bands;
		for( int i = 0; i < bands; i++ ){
			int ind = vPerBand * (i+1);
			elevLines[ i ] = elevSort.get(ind);
		}
	} 
	
	private boolean RefineSurface( List<WorldPlate> plates, List<WorldVert> verts, List<List<WorldTri>> surface, int breakLimit )
	{
		Logger.post( "Refining surface");
		List<WorldTri> surfaceCur = surface.get( surface.size()-1);
		while( checkUpdate() ){
			if( surfaceCur.size() < breakLimit ){
				List<WorldTri> surfaceNew = new ArrayList<WorldTri>();
				for( int i = 0; i < surfaceCur.size(); i++ ){
					//Logger.post( "breakCycle " + breakCycle + " i " + i + " target " + breakLimit + " cur size " + surfaceCur.size() );
					if( surfaceCur.get(i).child == null ){
						surfaceCur.get(i).splitEdge( verts, surfaceNew );
					}
				}

				surfaceCur = surfaceNew;
				surface.add( surfaceNew );
				initSurfaceFB( surfaceNew );
				breakCycle++;
			} else {
				doneRefineSurface = true;
			}
		}
		
		return doneRefineSurface;
	}
	
	private boolean SetPlateCenters(List<WorldPlate> plates, List<List<WorldTri>> surface)
	{
		Logger.post( "Setting plate centers");
		for( int i = 0; i < plates.size(); i++ ){
			WorldPlate plate = plates.get(i);
			plate.mColor = new Vec3( (float)Math.random()*0.4f+0.6f,
									 (float)Math.random()*0.4f+0.6f,
									 (float)Math.random()*0.4f+0.6f );
			float distMin = Float.MAX_VALUE;
			List<WorldTri> surfBase = surface.get(0);
			WorldTri nearest = null;
			for( WorldTri tTri : surfBase ){
				WorldTri nearChild = tTri.findNearestChild( plate.pos );
				float dist = plate.pos.sub( nearChild.pos ).lenSq();
				if( distMin > dist ){
					nearest = nearChild;
					distMin = dist;
				}
			}
			
			plate.mTris.add( new ArrayList<WorldTri>() );
			plate.mTris.get(0).add( nearest );
			plate.mFilledRings = 0;
			plate.mRings = 1;
			nearest.mPlate = plate;
			//Logger.post( "Assigned plate " + i + " to tri " + nearest );
		}

		return true;
	}
	
	private boolean AssignPlateFlow( List<WorldPlate> plates )
	{
		float volCrust = mAreaSurfaceEarth * mElevAvgTarget * mPctLand;
		float massCrust = volCrust * mDensityAvgEarth;
		Logger.post( "Assigning plate flow" );
		for( int i = 0; i < plates.size(); i++ ){
			WorldPlate plate = plates.get(i);
			plate.angleOffAxis = ( (float)Math.random()-0.5f ) * 2 * mMaxPlateAngleVar;
			plate.mass = plate.pctSurface * massCrust;
			
			WorldTri cTri = plate.mTris.get(0).get(0);
			cTri.set2DCFDEnv( plate.angleOffAxis, plate.mVelMag, new Vec3( 0,0,1 ) );
			
			Vec3 plateVec = ( cTri.xLocal_3D.mult( cTri.mFlowDir_2D.gX() ) ).add( cTri.yLocal_3D.mult( cTri.mFlowDir_2D.gY() ) ).norm();
			plate.mMoveDir = plateVec;
		}
		return true;
	}
	
	private boolean BuildPlateSurfaces(List<WorldPlate> plates, List<WorldTri> surface )
	{
		boolean tileRemain = true;
		while( checkUpdate() ){
			if( tileRemain ){
				// Each plate grabs tiles in round robin
				// Grab until over target weight
				int trisAvailTot = 0;
				for( int p = 0; p < plates.size(); p++ ){
					// start in max filled ring
					WorldPlate plate = plates.get(p);

					//Logger.post( "Plate " + p + " turn" );

					// buy till hit target plate size
					if( !plate.noAvailTiles ){
						List<WorldTri> trisCur = plate.mTris.get(plate.mFilledRings);

						// look in neighbors for possible connections
						List<WorldTri> trisAvail = new ArrayList<WorldTri>();
						for( int t = 0; t < trisCur.size(); t++ ){
							List<WorldEdge> edges = trisCur.get(t).edge;
							for( int s = 0; s < edges.size(); s++ ){
								if( edges.get(s).side.mPlate == null ){
									trisAvail.add( edges.get(s).side );
								}
							}
						}

						//Logger.post( "Found " + trisAvail.size() + " avail tris" );

						// if free tiles, grab nearest
						// add shape effects here if want
						WorldTri triNear = null;
						int fillRingp1 = plate.mFilledRings+1;
						
						if( trisAvail.size() > 0 ){
							// keep lists of layers of tiles
							// 6 dof chain of who knows who
							// work on inner layers first
							// till full, like electrons (use quanta)
							// if this is the first addition to a layer
							// make the layer
							if( plate.mTris.size() == fillRingp1 ){
								plate.mTris.add( new ArrayList<WorldTri>() );
								plate.mRings++;
							}
							
							if( trisAvail.size() > 1 ){
								float distMin = Float.MAX_VALUE;
								for( int t = 0; t < trisAvail.size(); t++ ){
									WorldTri tTri = trisAvail.get(t);
									float dist = ( plate.pos.sub( tTri.pos ) ).lenSq();
									if( distMin > dist ){
										distMin = dist;
										triNear = tTri;
									}
								}

								//Logger.post( "fill and fill+1 " + plate.mFilledRings + ", " + fillRingp1 + " trinear " + triNear );
								plate.mTris.get(fillRingp1).add( triNear );
								plate.mPctSurfaceTile += triNear.pctSurface;
							} else {
								triNear = trisAvail.get(0);
								//Logger.post( "fill and fill+1 " + plate.mFilledRings + ", " + fillRingp1 + " trinear " + triNear );
								plate.mTris.get(fillRingp1).add( triNear );
								plate.mPctSurfaceTile += triNear.pctSurface;
								plate.mFilledRings++;
							}

							triNear.mPlate = plate;
						} else {
							if( plate.mTris.size() > fillRingp1 ){
								plate.mFilledRings++;
							} else {
								Logger.post( "---No tiles - stopping" );
								plate.noAvailTiles = true;
							}
						}

						trisAvailTot += trisAvail.size();
					} // end if

					// All plates keep buying till tiles gone
				} // end for

				if( trisAvailTot == 0 ){
					tileRemain = false;
					for( WorldPlate plate : plates ){
						plate.area = plate.mPctSurfaceTile * mAreaSurfaceEarth;
					}
					
				}
			} // end if
		}
		
		initSurfaceColorFB( surface );

		return !tileRemain;
	}
	
	private boolean AssignSurfaceFlow(List<List<WorldTri>> surface)
	{
		Logger.post( "Entered AssignSurfaceFlow" );
		List<WorldTri> surfaceCur = surface.get( surface.size() - 1 );
		// Assign flow dir to tiles
		for( int t = 0; t < surfaceCur.size(); t++ ){
			// parallel to each tile surface, x/y axis pair created
			// rotated to match source / sink dir, 0,0 cen
			// tri states updated for 2D rep
			WorldTri tTri = surfaceCur.get(t);
			float tAngle = tTri.mPlate.angleOffAxis;
			float tVelMag = tTri.mPlate.mVelMag;

			//Logger.post( "Setting 2D CFD for tri " + t + " of " + surfaceCur.size() + ", particles " + tTri.particles);
			// add ic: uni flow to each tile
			//tTri.particles = new ArrayList<Particle>();
			tTri.set2DCFDEnv( tAngle, tVelMag, new Vec3( 0,0,1 ) );
			//Vec3 flowXYZ = ( tTri.xLocal_3D.mult( tTri.mFlowDir_2D.gX() ) ).add( tTri.yLocal_3D.mult( tTri.mFlowDir_2D.gY() ) );
			//Logger.post( "plate " + tTri.mPlate.ind + " angle " + tTri.mPlate.angleOffAxis + " tri flowXYZ " + flowXYZ.toS() );
		}
		return true;
	}
	
	private boolean BuildCFDDomains(List<WorldPlate> plates)
	{
		Logger.post( "Entered BuildCFDDomains" );
		// for each plate
		// turn plate into cfd domain
		for( int p = 0; p < plates.size(); p++ ){
			WorldPlate plate = plates.get(p);
			//Logger.post( "Finding edge tiles for plate " + p + " w mTris size " + plate.mTris.size() + " and filledRings " + plate.mFilledRings);
			List<WorldTri> trisEdge = new ArrayList<WorldTri>();
			for( int i = 0; i < plate.mTris.size(); i++ ){
				List<WorldTri> trisCur = plate.mTris.get(i);
				//Logger.post( "Checking ring " + i );
				for( int t = 0; t < trisCur.size(); t++ ){
					WorldTri tTri = trisCur.get(t);
					for( int s = 0; s < tTri.edge.size(); s++ ){
						if( tTri.edge.get(s).side.mPlate != tTri.mPlate ){
							trisEdge.add( tTri );
							break;
						}
					}
				}

				//Logger.post( "Currently " + trisEdge.size() + " edge tris" );
			}

			// for each border tile
			// tiles in last ring(s)
			for( int t = 0; t < trisEdge.size(); t++ ){
				// assign open edges bcs:
				// in, out, outRestrict, reflect, ect..
				WorldTri tTri = trisEdge.get(t);
				for( int s = 0; s < tTri.edge.size(); s++ ){
					WorldTri side = tTri.edge.get(s).side;
					if( side.mPlate != tTri.mPlate ){
						// take edge.normal.dot( flow dir)
						Vec3 delta = side.pos.sub( tTri.pos );
						Vec3 delNorm = delta.norm();

						// find components in tri 2D x,y
						float xComp = delNorm.dot( tTri.xLocal_3D );
						float yComp = delNorm.dot( tTri.yLocal_3D );

						// find flow dir component
						float flowX = tTri.mFlowDir_2D.gX()/tTri.mVelMag;
						float flowY = tTri.mFlowDir_2D.gY()/tTri.mVelMag;
						float flowComp = xComp*flowX + yComp*flowY;

						/*float triCnt = plate.mPctSurfaceTile / tTri.pctSurface;
						float triEdge = Math.round( Math.sqrt( triCnt / 18.0f ) ) * 2.0f; // assume plate is close to a circle
						float triIn = triEdge / 4;  // assumme about a quarter in
						float areaPlate = plate.mPctSurfaceTile * mAreaSurfaceEarth;
						float areaLand  = areaPlate * mPctLand;*/
						float massPlateFull = tTri.mPlate.area * plate.mElevAvgTarget * mPctLand * mDensityAvgEarth;
						float prtPerPlate   = plate.getTriCnt()*prtNumDensity;
						float massPrtGoal   = massPlateFull / prtPerPlate;
						float radiusPlate   = (float)Math.sqrt(tTri.mPlate.area / 3.14159f);
						float circPlateQtr  = 3.14159f * radiusPlate * (2.0f / 4.0f);
						float edgeTris      = circPlateQtr / tTri.scale;
						float timeTot       = mTimestepEarth * crustSolveCntMax;
						float flowTarget    = massPlateFull / ( edgeTris * crustSolveCntMax );
						//Logger.post("plate; vel, elevTarg, area, tris " + plate.mVelMag + ", " + plate.mElevAvgTarget + ", " + plate.area + ", " + plate.getTriCnt() + " tri; scale " + tTri.scale );
						Logger.post("massGoal " + massPrtGoal + " flowTarget " + flowTarget + ", prtPerPlate " + prtPerPlate + ", edgeTri" + edgeTris );
						// choose bc
						// bcs are static type ID + array config floats
						//if( flowComp > 0.5f ){
							//tTri.edge.get(s).BC = new CFDBnd( CFDBnd.OUT, new float[]{} );
						float coefRebound = 0.75f;  // very elastic
						if( flowComp > 0.5f ){
							// straight ahead, should be restricted outflow
							// backpressure set to hit target density
							// rolls off toward zero as density increases
							// pressure = depth * gravitational_acceleration * water_density
							// pressure_avg = 0.5 * total_depth * gravitational_acceleration * water_density
							// Reference https://www.physicsforums.com/threads/lateral-force-of-water-on-the-walls-of-a-tank.581539/
							// flowTarget = plate area (m2)* velMag (m/s) * %land goal * target_land_density (kg/m3) = kg/s flow rate
							// P/P0 * flowTarget = flowReal
							float P0 = mElevAvgTarget * accGrav * mDensityAvgEarth;
							float flowPerPres = flowTarget / P0; 
							tTri.edge.get(s).BC = new CFDBnd( CFDBnd.OUTRESTRICT, new float[]{ flowPerPres } );
						} else if( flowComp < -0.5f ){
							// behind us (against flow), should be inflow edge
							// add particles w vel = %flowRate
							tTri.edge.get(s).BC = new CFDBnd( CFDBnd.IN, new float[]{ coefRebound, flowTarget, massPrtGoal, mDensityAvgEarth } );
							//Logger.post( "New IN BC w flowTarget, mass, density " + tTri.edge.get(s).BC.mArgs[0] + ", " + tTri.edge.get(s).BC.mArgs[1] + ", " + tTri.edge.get(s).BC.mArgs[2] );
							//Logger.post( "particles " + tTri.particles );
						} else {
							// this is a side boundary
							tTri.edge.get(s).BC = new CFDBnd( CFDBnd.SIDE, new float[]{ coefRebound } );
						} // endif
					}// endif
				} // end for each edge side
			} // end for each edge
		} // end for each plate

		return true;
	}
	
	private boolean SolveCrustFlow(List<WorldPlate> plates, List<WorldVert> verts )
	{
		Logger.post( "Entered SolveCrustFlow" );
		
		while( checkUpdate() ){
			// while sim time less than X perform iteration
			if( crustSolveCnt < crustSolveCntMax ){
				Logger.post( "crust solve cnt." + crustSolveCnt ); 
				
				// for each plate
				//Logger.post( "plates size " + plates.size() );
				for( int p = 0; p < plates.size(); p++ ){
					WorldPlate plate = plates.get(p);
					plate.updateParticles( mTimestepEarth );
				} // end for each plate
				
				// transform particles back to 3d space
				// for plottimg and fimd nearest tris
				shiftParticleParents( mTimestepEarth );
				
				// zero out all verts before accumulating
				for( int v = 0; v < verts.size(); v++ ){
					WorldVert vv = verts.get(v);
					vv.mass = 0.0f;
					vv.momentum2D.eq( 0.0f, 0.0f );
					vv.energy = 0.0f;
					vv.PE = 0.0f;
					vv.KE = 0.0f;
					vv.elevation = 0.0f;
					vv.prtAccumlated = 0;
				}
				
				// Once all particles have moved and been tile shifted
				// for each plate
				for( int p = 0; p < plates.size(); p++ ){
					WorldPlate plate = plates.get(p);
					plate.accumulateProps();
					plate.computeBulkProps();
				} // end for each plate
				
				crustSolveCnt++;
			} else {
				return true;
			}// end solver if
		}// end while loop
		
		// load any existing particles to be drawn
		initParticleFB();

		return false;
	}

	private void shiftParticleParents( float ts )
	{
		particles = new ArrayList<Particle>();

		for( WorldPlate plate : mPlates ){
			for( List<WorldTri> ring : plate.mTris ){
				for( WorldTri tri : ring ){
					for( Particle prt : tri.particles ){
						prt.pos2DTo3D();
						prt.findNearestTri( ts );
						//Logger.post( "prt parent " + prt.parent + " tri " + tri );
						/*if( prt.triggeredSideBC && prt.triggeredTriShift ){
							prt.parent = null;
						}*/
						if( prt.parent != null ){
							prt.pos3DTo2D();
							particles.add( prt );
						}
						if( prt.vel2D.length() > mRadius*0.1 ){
							//Logger.post( "Fast prt: vel " + prt.vel2D.length() + " acc " + prt.acc2D.length() );
						}
					}
					tri.particles = new ArrayList<Particle>();
		}}}
		
		// Reassign particles to the new lists
		for( Particle prt : particles ){
			prt.parent.particles.add( prt );
		}
	}
	
	private boolean NormalizeEarth(List<WorldVert> verts)
	{
		float ElevMax = 0.0f;
		for( WorldVert vert : verts ){
			if( ElevMax < vert.elevation ){
				ElevMax = vert.elevation;
			}
		}
		
		Logger.post( "Normalize: ElevMax " + ElevMax );
		
		for( WorldVert vert : verts ){
			//Logger.post( "elev before mult " + vert.elevation );
			vert.elevation *= mElevAvgTarget / ElevMax;
			//Logger.post( "elev after mult " + vert.elevation );
			Vec3 vPosNorm = vert.pos.norm();
			vert.pos = vPosNorm.mult( mRadius + vert.elevation );
		}
		
		return true;
	}

	private void SolveFire( List<List<WorldTri>> surface, float tilt )
	{
		// Set orientation angle of planet
		mTilt = tilt;

		// find out max surface detail level
		int lastSurfInd = surface.size() - 1;
		List<WorldTri> surfaceLast = surface.get( lastSurfInd );

		// Calculate heat per tile based on sol avg
		// Simple version, make arctic-equator avg smooth transition
		// falls off from 9 to 3 hours of 1000 kW/m2
		// annual avg pct max / latitude
		// 0:100%, 25:98%, 40:63%, 45:57%, 48:54%, 90:34%
		
		// Follow this for upgraded version w seasons
		// https://earthscience.stackexchange.com/questions/5301/equation-for-solar-radiation-at-a-given-latitude
		for( WorldVert tVert: mVert ){
			tVert.solveSolarEnergy( 1000f );
			// Modify heat for elevation
			// 3.3 F / 1000 ft or 6.4 C / 1000 m
			//tTri.solveElevTemp();
		}
	}

	private boolean SolveWind( List<WorldVert> verts, List<List<WorldTri>> surface, List<List<List<WorldTri>>> skyLayer, List<Particle> air, int resMax, float layerCnt, float layerH )
	{
		Logger.post("------Entered SolveWind");
		// Soln2: 3D CFD w heating, bouyancy
		// make many joined 3D volumes
		// take surface, scale out x times
		if( !doneInitSkyLayers ){
			doneInitSkyLayers = InitSkyLayers( surface, skyLayer, resMax, layerCnt );
		}
		
		if( !doneWindVolumes && doneInitSkyLayers ){
			doneWindVolumes = BuildAirCFDDomain( verts, skyLayer, layerH );
		}
		
		// Setup particles in volume
		// Set starting energy and temp as something reasonable
		// Start w evenly distributed amount everywhere
		if( doneWindVolumes && !doneSetupWind ){
			doneSetupWind = SetupWindParticles( skyLayer, air );
		}
		
		/*if( doneSetupWind && !doneInitSkyBuffers ){
			displayMode = DisplayMode.WIRE_SKY;
			initSkyLayerFB( skyLayer.get( skyLayer.size()-1 ) );
			initSkyPointFB( skyLayer );
			doneInitSkyBuffers = true;
		}*/
		
		// Slowly spin Earth w solar heat on half
		if( doneSetupWind && !doneWind ){
			//doneWind = SolveWindCFD( skyLayer, air );
		}
		
		return doneWind;
	}

	private boolean InitSkyLayers(List<List<WorldTri>> surface, List<List<List<WorldTri>>> skyLayer, int resMax, float layerMax )
	{
		Logger.post("----Entered InitSkyLayers");
		for( int s = 0; s <= resMax; s++ ){
			Logger.post( "Adding skyLayer list for resolution " + s );
			skyLayer.add( new ArrayList<List<WorldTri>>() );
			
			List<List<WorldTri>> resCur = skyLayer.get(s);
			
			// base layer of each resolution group is surface
			resCur.add( surface.get(s) );
			WorldTri tri0 = surface.get(s).get(0);
			Logger.post( "res " + s + " tri 0 c0 pos " + tri0.corner.get(0).pos.toS() + ", c1 " + tri0.corner.get(1).pos.toS() + ", c2 " + tri0.corner.get(2).pos.toS()  );
			Logger.post( "edge 0 " + tri0.edge.get(0) );
			Logger.post( "edge 0 vec " + tri0.edge.get(0).vec.toS() );
			for( int L = 0; L < layerMax; L++ ){
				Logger.post( "Adding ceiling to layer " + L );
				// Add ceiling layers till we hit air max
				resCur.add( new ArrayList<WorldTri>() );
			}
		}
		
		return true;
	}

	private boolean BuildAirCFDDomain( List<WorldVert> verts, List<List<List<WorldTri>>> skyLayer, float layerH)
	{
		Logger.post("----Entered BuildAirCFDDomain");
		boolean doneVolume = false;
		
		// First, find all the verts and surfaces of the last layer
		   // HashMap of verts, check each tri, if vert no exist, add, copy to list
		   // straight copy surface(size-1) = skylayer(0)
		// Copy them to new lists (they can remain the same objects)
		
		while( checkUpdate() ){
			// for each resolution
			if( skyResCnt < skyLayer.size() ){
				List<List<WorldTri>> resCur = skyLayer.get( skyResCnt );

				// if we have a remaining layer to build
				if( skyLayerCnt+1 < resCur.size() ){
					// define the base and ceiling layer
					List<WorldTri> layerCur =  resCur.get( skyLayerCnt );
					List<WorldTri> layerUp = resCur.get( skyLayerCnt+1 );

					WorldTri triCur = null;

					// find next tile not extruded of current layer
					for( WorldTri tTri : layerCur ){
						if( tTri.Up == null ){
							triCur = tTri;
							break;
					}}
					
					if( triCur != null ){
						//Logger.post("triCur != null, extruding " + triCur + "in resCnt " + skyResCnt + " in layer " + skyLayerCnt );
						// extrude tile into 3D volume
						// check if neighbors extruded
						// if yes, mark to join to existing neighbors bndry
						float height = layerH * (float)Math.pow( 2.0f, skyLayerCnt );
						//Logger.post( "resCnt " + skyResCnt + " in layer " + skyLayerCnt + " height " + height );
						//Logger.post( "edge 2 vec base before extrude" + triCur.edge.get(2).vec.toS() );
						triCur.extrudeUp( verts, layerUp, height );
						//Logger.post( "edge 2 vec Up " + triCur.Up.edge.get(2).vec.toS() );
						triCur.set3DCFDEnv();
						// if on last layer, setup ceiling
						if( skyLayerCnt+2  == resCur.size() ){
							triCur.Up.set3DCFDEnv();
						}
						//Logger.post( "edge 2 vec after set CFD" + triCur.Up.edge.get(2).vec.toS() + ", vec2D" + triCur.edge.get(2).vec2D.toS() );
					} else {
						// if no more tris w tri.Up unextruded, finished
						skyLayerCnt++;
						//Logger.post( "no tris, skyLayerCnt++ = " + skyLayerCnt );
					}
				} else {
					// done w resolution
					skyResCnt++;
					skyLayerCnt = 0;
					//Logger.post( "skyLayerCnt+1 " + (skyLayerCnt+1) + " > resCur.size " + resCur.size() + ", skyResCnt++ = " + skyResCnt );
				}
			} else {
				// done w all resolutions
				doneVolume = true;
			}
		}
		
		return doneVolume;
	}
	
	private boolean SetupWindParticles( List<List<List<WorldTri>>> skyLayer, List<Particle> air)
	{
		Logger.post("Entered SetupWindParticles");
		// for each tri prism of each layer
		// add mPrtPerWindPrism particles to the volume
		// randomly distributed
		float heightCur = 0;
		for( int r = 0; r < skyLayer.size(); r++ ){
			List<List<WorldTri>> res = skyLayer.get(r);
			
			int Im1 = res.size() - 1;
			for( int L = 0; L <  Im1; L++ ){
				List<WorldTri> layer = res.get(L);
				WorldTri       First = layer.get(0);
				
				int prtCnt   = 0;
				float height = First.height;
				Logger.post( "First height " + height + " heightCur " + heightCur );
				if( First.height > 0 ){
					Vec3 propsBase = CalcAirStartDensityTempPres( heightCur );
					Vec3 propsUp   = CalcAirStartDensityTempPres( heightCur + height );
					Vec3 propsAvg  = ( propsBase.add( propsUp ) ).mult( 0.5f );
					Logger.post( "Props: base " + propsBase.toS() + ", Up " + propsUp.toS() + ", avg " + propsAvg.toS() );
					for( WorldTri prism : layer ){
						//Logger.post( "Prism " + prism );
						float vol = prism.area * prism.height;
						float airMassPrism = vol * propsAvg.gRho();
						float airPrtMass = airMassPrism / mAirNumDensity;

						for( int p = 0; p < mAirNumDensity; p++ ){
							//Logger.post( "prt " + p );
							// prt pos as 0-1 in each dir
							float e0rand = (float)Math.random();
							float e1rand = (float)Math.random();
							Vec2 e0seg = prism.edge.get(0).vec2D.mult( e0rand );
							Vec2 e1seg = prism.edge.get(1).vec2D.mult( e0rand*e1rand );
							//Logger.post( "Found edge segs" );
							Vec2 posPrtxy = ( prism.corner2D.get(0).add( e0seg ) ).add( e1seg );
							float posPrtz = (float)Math.random();
					
							float prtDensity = propsBase.gRho() * (1-posPrtz) + propsUp.gRho() * posPrtz;

							posPrtz *= height;
							Vec3 posPrt = new Vec3( posPrtxy.gX(), posPrtxy.gY(), posPrtz );
							//Logger.post( "Found prt pos " + posPrt.toS() );
							
							Particle newPrt = new Particle( prism, airPrtMass, prtDensity, posPrt );
							prism.particles.add( newPrt );
							newPrt.pos2DTo3D();
							
							prtCnt++;
						}
					}
					heightCur += height;
				}
				// end current layer, how many prt?
				Logger.post("res " + r + " layer " + L + " prtCnt " + prtCnt );
			}
			// end layer loop, reset height
			heightCur = 0;
		}
		// end res loop
		Logger.post("Leaving SetupWindParticles");
		return true;
	}

	private Vec3 CalcAirStartDensityTempPres(float heightCur)
	{
		// calc T = T0-LH
		// P = P0(1-(LH)/T0)^(gM/RL)
		// rho = PM/RT
		float P0 = 101325.0f;  // Pa
		float T0 = 288.15f;  // K
		float G = accGrav;
		float L0_10 = 0.0065f; // K/m
		float L10_20 = 0.0f;
		float L20_40 = -0.00075f;
		float L40_80 = -0.0009f;
		float R = 8.31447f; // J/mol-K
		float M = 0.0289644f; // kg/mol
		float H = heightCur; // m
		float T=0, P=0;
		float H0_10 = H, H10_20, H20_40, H40_80;
		// calculate the lower troposphere
		if( H < 10000.0f ){
			H0_10 = H;
			T = T0-(L0_10*H0_10);
			P = P0 * (float)Math.pow( (T/T0), (G*M/(R*L0_10)) );
		} else {
			H0_10 = 10000;
			T = T0-(L0_10*H0_10);
			P = P0 * (float)Math.pow( (T/T0), (G*M/(R*L0_10)) );
			T0 = T;
			P0 = P;
			// calculate the upper troposphere
			if( H < 20000.0f ){
				H10_20 = H - 10000.0f;
				T = T0-(L10_20*H10_20);
				P = P0 * (float)Math.pow( (T/T0), (G*M/(R*L10_20)) );
			} else {
				H10_20 = 10000.0f;
				T = T0-(L10_20*H10_20);
				P = P0 * (float)Math.pow( (T/T0), (G*M/(R*L10_20)) );
				T0 = T;
				P0 = P;
				// calculate the lower stratosphere
				if( H < 40000.0f ){
					H20_40 = H - 20000.0f;
					T = T0-(L20_40*H20_40);
					P = P0 * (float)Math.pow( (T/T0), (G*M/(R*L20_40)) );
				} else {
					H20_40 = 20000.0f;
					T = T0-(L20_40*H20_40);
					P = P0 * (float)Math.pow( (T/T0), (G*M/(R*L20_40)) );
					T0 = T;
					P0 = P;
					
					// calculate the upper stratosphere
					H40_80 = H - 40000.0f;
					T = T0-(L40_80*H40_80);
					P = P0 * (float)Math.pow( (T/T0), (G*M/(R*L40_80)) );
				}
			}
		}
		
		float rho = P*M/(R*T);
		
		Vec3 out = new Vec3( rho, T, P );
		
		return out;
	}

	private boolean SolveWindCFD(List<List<List<WorldTri>>> skyLayer, List<Particle> air)
	{
		Logger.post("Entered SolveWindCFD");
		while( checkUpdate() ){
			if( windSlvCnt < windSlvCntMax ){
				Logger.post( "wind solve cnt." + windSlvCnt ); 

				// for each layer and tri
				for( List<List<WorldTri>> res : skyLayer ){
					for( List<WorldTri> layer : res ){
						for( WorldTri tri : layer ){
							tri.updateParticles( mTimestepWind );
				}}}
					
				 // end for each layer

				// transform particles back to 3d space
				// for plottimg and fimd nearest tris
				shiftParticleParents( mTimestepWind );

				// zero out all verts before accumulating
				for( int v = 0; v < skyVerts.size(); v++ ){
					WorldVert vv = skyVerts.get(v);
					vv.mass = 0.0f;
					vv.momentum3D.eq( 0.0f, 0.0f, 0.0f );
					vv.energy = 0.0f;
					vv.PE = 0.0f;
					vv.KE = 0.0f;
					vv.prtAccumlated = 0;
				}

				// Once all particles have moved and been tile shifted
				// for each layer
				for( List<List<WorldTri>> res : skyLayer ){
					for( List<WorldTri> layer : res ){
						for( WorldTri tri : layer ){
							tri.accumulateProps();
							tri.computeBulkProps();
				}}}

				windSlvCnt++;
			} else {
				return true;
			}// end solver if
		}// end while loop

		// load any existing particles to be drawn
		initSkyPointFB( skyLayer );

		return false;
	}

	private boolean SolveWater(List<List<WorldTri>> surface)
	{
		// TODO: Implement this method
		return false;
	}

	private boolean SolveBiomes(List<List<WorldTri>> surface)
	{
		// TODO: Implement this method
		return false;
	}

	private boolean SolveLife(List<List<WorldTri>> surface)
	{
		// TODO: Implement this method
		return false;
	}

	@Override
	public void draw()
	{
		//Logger.post( "Entered draw() for DisplayLayerMapSphere" );
		//Logger.post( "doneInitPtSlv " + doneInitPtSlv + " plate size " + mPlates.size() + " vert size " + mVert.size() );
		if( doneSurfaces ){
			if( displayMode == DisplayMode.WIRE_SKY ){
				displayMode = DisplayMode.ELEV;
				drawSurfaceTris( mSurfacePositions, mSurfaceNormals, mSurfaceColors, mSurfaceTexCoords, mSurfaceIndexBuffer );
				displayMode = DisplayMode.WIRE_SKY;
			} else {
				drawSurfaceTris( mSurfacePositions, mSurfaceNormals, mSurfaceColors, mSurfaceTexCoords, mSurfaceIndexBuffer );
			}
		}
		/*if( doneInitSkyBuffers ){
			drawSurfaceTris( mSkyPositions, mSkyNormals, mSkyColors, mSkyTexCoords, mSkyIndexBuffer );
			drawPoints( mSkyPrtPositions, mSkyPrtColors, mSkyPrtPointSize );
		}*/
		if( mPlateVerts.size() > 0 ){
			drawPoints( mPlatePositions, mPlateColors, mPlatePointSize ); }
		if( mVert.size() > 0 ){
			drawPoints( mIcoPositions, mIcoColors, mIcoPointSize ); }
		if( particles.size() > 0 && ( displayMode == DisplayMode.WIRE_VEL || ( displayMode == DisplayMode.WIRE_BNC ) ) ){
			Logger.post( "particle cnt " + particles.size() );
			drawPoints( mPrtPositions, mPrtColors, mPrtPointSize ); }
	}

	private void drawSurfaceTris(FloatBuffer positions, FloatBuffer normals, FloatBuffer colors, FloatBuffer texCoords, ShortBuffer indexBuffer  ){
		//Logger.post( "Entered draw surface tris");
		mPositions = positions;
		mNormals = normals;
		mColors = colors;
		mTextureCoordinates = texCoords;
		ShortBuffer mIndexBuffer = indexBuffer;
		/*for( int i = 0; i < mPositions.capacity(); i+=3 ){
			float p0 = mPositions.get(i+0);
			float p1 = mPositions.get(i+1);
			float p2 = mPositions.get(i+2);
			Logger.post( "posData pos[" + i + "] " + p0 + "," + p1 + "," + p2 );
		}*/
		/*Logger.post( "nrmData " + mNormals );
		Logger.post( "colorData " + mColors );
		Logger.post( "texData " + mTextureCoordinates );
		Logger.post( "posDataLen " + mPositions.capacity() );
		Logger.post( "nrmDataLen " + mNormals.capacity() );
		Logger.post( "colorDataLen " + mColors.capacity() );
		Logger.post( "texDataLen " + mTextureCoordinates.capacity() );*/

		setupHandles();
		//Logger.post( "Finished setup handles");
		//Logger.post( "pos, nrm, col, tex handles " + mPositionHandle + ", " + mNormalHandle + ", " + mColorHandle + ", " + mTextureCoordinateHandle );

		// Pass in the position information
		mPositions.position(0);	
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
									 0, mPositions);        
        GLES20.glEnableVertexAttribArray(mPositionHandle);        

		//Logger.post( "Finished fill position");
        // Pass in the color information
        mColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
									 0, mColors);
        GLES20.glEnableVertexAttribArray(mColorHandle);

		//GLES20.glVertexAttrib4f(mColorHandle, 1.0f, 1.0f, 1.0f, 1.0f );
		//GLES20.glDisableVertexAttribArray(mColorHandle);

		//Logger.post( "Finished color");

        // Pass in the normal information
        mNormals.position(0);
		GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 
									 0, mNormals);

		 GLES20.glEnableVertexAttribArray(mNormalHandle);

		//Logger.post( "Finished fill normal");
        // Pass in the texture coordinate information
        mTextureCoordinates.position(0);
		GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 
									 0, mTextureCoordinates);

		 GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

		//Logger.post( "Finished fill tex");
        // Pass in the light position in eye space.        
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
	
        SetupTextures();

		float[] mm = mModelMatrix;
		/*Logger.post( "model matrix " );
		Logger.post( mm[0] + ", " + mm[1] + ", " + mm[2] + ", " + mm[3] );
		Logger.post( mm[4] + ", " + mm[5] + ", " + mm[6] + ", " + mm[7] );
		Logger.post( mm[8] + ", " + mm[9] + ", " + mm[10] + ", " + mm[11] );
		Logger.post( mm[12] + ", " + mm[13] + ", " + mm[14] + ", " + mm[15] );
		*/
		BuildMVPMatrices();
		//Logger.post( "Finished build mvp");
		//Logger.post( "drawing " + mTris + " tris" );

		//GLES20.glDisable(GLES20.GL_CULL_FACE);
        // Draw the Sprite.
        //GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mTris);
		if( displayMode == DisplayMode.WIRE_VEL || ( displayMode == DisplayMode.WIRE_BNC || displayMode == DisplayMode.WIRE_SKY ) ){
			GLES20.glDrawElements(GLES20.GL_LINES, mTris*3, GLES20.GL_UNSIGNED_SHORT, mIndexBuffer );
		} else {
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, mTris*3, GLES20.GL_UNSIGNED_SHORT, mIndexBuffer );
		}
		//Logger.post( "done drawing" );
		
	}

	/*@Override
	 protected void BuildMVPMatrices()
	 {
	 // Pass in the transformation matrix.
	 Matrix.multiplyMM(mMVPMatrix, 0, mRenderer.mProjectionMatrix, 0, mRenderer.mViewMatrix, 0);
	 GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
	 }*/

	public void drawPoints( FloatBuffer pointPositions, FloatBuffer pointColors, FloatBuffer pointSizes ){
		// Draw points to indicate the structure positions
		GLES20.glUseProgram(mPointProgramHandle);

		//GLES20.glCullFace(GLES20.GL_FRONT_AND_BACK);
		//Logger.post( "mPointProgramHandle " + mPointProgramHandle );
		//Logger.post( "plates size " + mPlates.size() );

		final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");
		final int pointColorHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Color");
		final int pointSizeHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_PointSize");

		int pntCnt = pointPositions.capacity() / mPositionDataSize;

		// Pass in the positions
		//Logger.post( "pointpositions " + pointPositions );
		
		pointPositions.position(0);
        GLES20.glVertexAttribPointer(pointPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
									 0, pointPositions);        

        GLES20.glEnableVertexAttribArray(pointPositionHandle);        

		// Pass in the color information
        pointColors.position(0);
        GLES20.glVertexAttribPointer(pointColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
									 0, pointColors);
        GLES20.glEnableVertexAttribArray(pointColorHandle);

		// Pass in the pointsize
        pointSizes.position(0);
        GLES20.glVertexAttribPointer(pointSizeHandle, mPointSizeDataSize, GLES20.GL_FLOAT, false,
									 0, pointSizes );
        GLES20.glEnableVertexAttribArray(pointSizeHandle);


		// Pass in the transformation matrix.
		Matrix.multiplyMM(mMVPMatrix, 0, mRenderer.mViewMatrix, 0, mModelMatrix, 0 );
		Matrix.multiplyMM(mMVPMatrix, 0, mRenderer.mProjectionMatrix, 0, mMVPMatrix, 0);
		GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Draw the points
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pntCnt );
	}
}

