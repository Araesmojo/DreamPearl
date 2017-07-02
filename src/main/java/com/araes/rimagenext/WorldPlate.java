package com.araes.rimagenext;
import java.util.List;
import java.util.ArrayList;
import android.util.*;

public class WorldPlate extends WorldVert
{
	DisplayLayerSphereMap world = null;
	public float angleOffAxis = 0.0f;

	public List<List<WorldTri>> mTris;
	public int mRings = 0;
	public int mFilledRings = -1;
	public float mPctSurfaceTile = 0;
	public float mVelMag = 0;
	public float mElevAvgTarget = 1;
	public Vec3 mColor = new Vec3( 1.0f, 1.0f, 1.0f );
	public boolean noAvailTiles = false;
	
	LogPoster Logger;

	public float area;

	public Vec3 mMoveDir = null;
	
	public WorldPlate( DisplayLayerSphereMap wrld, int indIn, float pctSurfIn ){
		super( indIn, pctSurfIn );
		world = wrld;
		mElevAvgTarget = world.mElevAvgTarget;
		mVelMag = world.mVelPlate;
		mTris = new ArrayList<List<WorldTri>>();
		Logger = new LogPoster();
		Logger.addLogListener( world.mActivityContext.GameLog );
		Logger.post( "Finished plate creation" );
	}
	
	public WorldPlate( DisplayLayerSphereMap wrld, WorldVert vert ){
		super( vert.ind, vert.pctSurface );
		world = wrld;
		mElevAvgTarget = world.mElevAvgTarget;
		mVelMag = world.mVelPlate;
		pos = vert.pos;
		vel = vert.vel;
		acc = vert.acc;
		vel2D = vert.vel2D;
		mTris = new ArrayList<List<WorldTri>>();
		Logger = new LogPoster();
		Logger.addLogListener( world.mActivityContext.GameLog );
		Logger.post( "Finished plate creation" );
	}

	public void updateParticles( float ts )
	{
		// for each ring layer of tiles
		//Logger.post( "ring cnt " + mTris.size() );
		for( int r = 0; r < mTris.size(); r++ ){
			List<WorldTri> ring = mTris.get(r);
			// for each tile in cur ring
			//Logger.post( "tris size " + ring.size() );
			for( int t = 0; t < ring.size(); t++ ){
				WorldTri tTri = ring.get(t);
				tTri.updateParticles( ts );
			} // end for each tile
		} // end for each ring
	}

	public void accumulateProps()
	{
		// for each ring layer of tiles
		for( int r = 0; r < mTris.size(); r++ ){
			List<WorldTri> ring = mTris.get(r);
			// for each tile in cur ring
			for( int t = 0; t < ring.size(); t++ ){
				WorldTri tTri = ring.get(t);
				//Logger.post( "Accumulate props in p, r, t " + p + ", " + r + "," + t + " of pm,rm,tm " + plates.size() + "," + plate.mTris.size() + "," + ring.size() );
				// compute per-tile properties from particles
				// still in this cell - elevation/density, pressure
				// goal: X prt = "filled" tri
				// area of cell (m2)
				//  * targetflowdensity (kg/m3)
				//  * plate velmag (m/s)
				//  = kg/s flow rate of all borders
				// add properties to vertices nearby
				// rel to dist (mass, mom)
				List<Particle> prt = tTri.particles;
				tTri.accumulateProps();
			} // end for each tile
		} // end for each ring
	}
	
	public void computeBulkProps()
	{
		// after all tiles have accumulated
		// compute final properties (vel, elev, pressure)
		// for each ring layer of tiles
		for( int r = 0; r < mRings; r++ ){
			List<WorldTri> ring = mTris.get(r);
			// for each tile in cur ring
			for( int t = 0; t < ring.size(); t++ ){
				//Logger.post( "compute bulk props in p, r, t " + p + ", " + r + "," + t );
				WorldTri tTri = ring.get(t);
				tTri.computeBulkProps();
			}
		}
	}
	
	public int getTriCnt(){
		int cnt = 0;
		for( List<WorldTri> ring : mTris ){
			cnt += ring.size();
		}
		return cnt;
	}
}
