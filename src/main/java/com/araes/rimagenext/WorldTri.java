package com.araes.rimagenext;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.*;

public class WorldTri
{
	public WorldTri parent = null;
	public WorldTri Up     = null;
	public WorldTri Down   = null;

	public List<WorldVert> corner = null;
	public List<WorldTri> child = null;
	public List<WorldEdge> edge = null;
	
	public List<Vec2> corner2D = null;
	public List<Particle> particles = null;

	public WorldPlate mPlate = null;

	public Vec3 pos = new Vec3(0, 0, 0);

	public Vec3 xLocal_3D;
	public Vec3 yLocal_3D;
	public Vec3 zLocal_3D;

	public Vec2 mFlowDir_2D;
	public float mVelMag;
	public float P;
	public float mFlowPerPres;

	private float mAngleOffAxis = 0;
	public float pctSurface = 0;
	public float area = 0;  // m2
	public float scale = 1; // m
	public float accGrav = 10; // m/s2
	public float height  = 0; // if 3d, how tall

	public float surfaceAreaEarth = 5.1e14f; // m2
	public float mDensityAvgEarth = 1500; // kg/m3
	public float R = 8.31447f; // J/mol-K

	public float elevPerMass; // m
	
	public LogPoster Logger;

	public DisplayLayerSphereMap displayLayer;

	private boolean mVisible = true;

	private float QRate;

	public WorldTri( DisplayLayerSphereMap disp, WorldTri par, float pct, WorldVert vrt0, WorldVert vrt1, WorldVert vrt2 ){
		parent = par;
		displayLayer = disp;
		pctSurface = pct;
		
		Logger = new LogPoster();
		Logger.addLogListener( displayLayer.mActivityContext.GameLog );
		/*Logger.post( "Tri Creation" );
		Logger.post( "c0 pos " + vrt0.pos.toS() );
		Logger.post( "c1 pos " + vrt1.pos.toS() );
		Logger.post( "c2 pos " + vrt2.pos.toS() );*/
		
		corner = new ArrayList<WorldVert>();
		corner.add( vrt0 );
		corner.add( vrt1 );
		corner.add( vrt2 );
		update();
	}

	public void updateParticles(float ts)
	{
		for( int e = 0; e < 3; e++ ){
			if( edge.get(e).BC != null ){
				edge.get(e).BC.flowThisStep = 0.0f;
			}
		}
		
		if( particles != null ){
			// for each particle in tile, update motion
			for( int pt = 0; pt < particles.size(); pt++ ){
				Particle prt = particles.get(pt);
				/*if( particles.size() > 0 ){
					//Logger.post( prt.size() + " prt to update in r, t " + r + "," + t );
					//Logger.post( "pos " + prt.pos2D + "," + prt.vel2D + " tTri corner P, vel2d " + tTri.corner.get(0).pressure + "," + tTri.corner.get(0).vel2D );
				}
				if( pt == 0 ){
					//Logger.post( "prt B4 update pos " + "," + prt.pos2D.toS() + ", vel " + prt.vel2D.toS() + ", acc " + prt.acc2D.toS() + ", mass " + prt.mass + ", mom " + prt.momentum2D.toS() + ", E " + prt.energy + ", KE " + prt.KE + ", elevLoc " + prt.elevLoc + ", areaXSec " + prt.areaXSec );
					/.WorldVert c0 = corner.get(0);
					//Logger.post( "corner 0 elev " + c0.elevation + ", prtCnt " + c0.prtAccumlated + ", mass " + c0.mass + " , mom2d " + c0.momentum2D.toS() + ", pres " + c0.pressure + ", E " + c0.energy + ", PE " + c0.PE + ", KE " + c0.KE + ", vel2D " + c0.vel2D.toS() + ", flowdir2D " + tTri.mFlowDir_2D.toS() );
				}*/
				prt.update( ts );
				/*if( pt == 0 ){
					//Logger.post( "prt Aft update pos " + "," + prt.pos2D.toS() + ", vel " + prt.vel2D.toS() + ", acc " + prt.acc2D.toS() + ", mom " + prt.momentum2D.toS() + ", E " + prt.energy + ", KE " + prt.KE + ", elevLoc " + prt.elevLoc );
				}*/
			}
		}
		//Logger.post( "activating inflow" );
		activateInflowBC( ts );
	}

	public void extrudeUp( List<WorldVert> verts, List<WorldTri> layerUp, float layerH )
	{
		height = layerH;
		
		// Check edges for which neighbors are already extruded
		List<WorldTri> needExtrude = new ArrayList<WorldTri>();
		boolean[] isUp = new boolean[]{ false, false, false };
		
		// save the tri list and edge positions
		for( int i = 0; i < 3; i++ ){
			if( edge.get(i).side.Up != null ){
				isUp[i] = true;
			} else {
				needExtrude.add( edge.get(i).side );
			}
		}
		
		// using neighbor list, raise each edge of this tri
		WorldVert[] corNew = new WorldVert[ 3 ];
		if( isUp[0] ){
			// Use both corners for our new tri
			WorldTri side0Up = edge.get(0).side.Up;
			int t = edge.get(0).twin.Up.ind;
			int tp1 = (t+1)%3;
			corNew[0] = side0Up.corner.get(tp1);
			corNew[1] = side0Up.corner.get(t);
			if( isUp[1] ){
				// Use the second good edhe and we're done
				WorldTri side1Up = edge.get(1).side.Up;
				t = edge.get(1).twin.Up.ind;
				corNew[2] = side1Up.corner.get(t);
			} else if( isUp[2] ){
				// if !1, but 2, use the corner from edge2
				WorldTri side2Up = edge.get(2).side.Up;
				t = edge.get(2).twin.Up.ind;
				tp1 = (t+1)%3;
				corNew[2] = side2Up.corner.get(tp1);
			} else {
				// make a new corner, offset our existing one
				Vec3 c2pos = corner.get(2).pos;
				Vec3 posNew = c2pos.mult( 1 + (layerH)/c2pos.length() );
				corNew[2] = new WorldVert( verts.size(), corner.get(2).pctSurface, posNew );
				verts.add( corNew[2] );
			}
		} else if( isUp[1] ){
			// Use both corners for our new tri
			WorldTri side1Up = edge.get(1).side.Up;
			int t = edge.get(1).twin.Up.ind;
			int tp1 = (t+1)%3;
			corNew[1] = side1Up.corner.get(tp1);
			corNew[2] = side1Up.corner.get(t);
			if( isUp[2] ){
				// if !0, but 2, use the corner 0 from edge2
				WorldTri side2Up = edge.get(2).side.Up;
				t = edge.get(2).twin.Up.ind;
				corNew[0] = side2Up.corner.get(t);
			} else {
				// make corner 0
				Vec3 c0pos = corner.get(0).pos;
				Vec3 posNew = c0pos.mult( 1 + (layerH)/c0pos.length() );
				corNew[0] = new WorldVert( verts.size(), corner.get(0).pctSurface, posNew );
				verts.add( corNew[0] );
			}
		} else if( isUp[2] ){
			// Use both corners for our new tri
			WorldTri side2Up = edge.get(2).side.Up;
			int t = edge.get(2).twin.Up.ind;
			int tp1 = (t+1)%3;
			corNew[2] = side2Up.corner.get(tp1);
			corNew[0] = side2Up.corner.get(t);
			
			// we know the other edges are !, so build corner 1
			Vec3 c1pos = corner.get(1).pos;
			Vec3 posNew = c1pos.mult( 1 + (layerH)/c1pos.length() );
			corNew[1] = new WorldVert( verts.size(), corner.get(1).pctSurface, posNew );
			verts.add( corNew[1] );
		} else {
			// no good edges, so build them all
			for( int c = 0; c < 3; c++ ){
				Vec3 cPos = corner.get(c).pos;
				Vec3 posNew = cPos.mult( 1 + (layerH)/cPos.length() );
				corNew[c] = new WorldVert( verts.size(), corner.get(c).pctSurface, posNew );
				verts.add( corNew[c] );
			}
		}
		
		// build Up tri out of verts
		Up = new WorldTri( this.displayLayer, this, this.pctSurface, corNew[0], corNew[1], corNew[2] );
		for( int e = 0; e < 3; e++ ){
			// assign corners, edges, sides, edge twins
			corner.get(e).Up = Up.corner.get(e);
			edge.get(e).Up = Up.edge.get(e);
			
			Up.corner.get(e).elevation = corner.get(e).elevation + height;
			
			if( isUp[e] ){
				Up.edge.get(e).side = edge.get(e).side.Up;
				Up.edge.get(e).twin = edge.get(e).twin.Up;
				Up.edge.get(e).twin.twin = Up.edge.get(e);
				Up.edge.get(e).twin.side = Up;
			}
		}
		
		// add to the skyLayer
		layerUp.add( Up );
		Up.mPlate = mPlate;
		Up.Down = this;
		
		// Assign parent if base tri is a child
		if( parent != null ){
			Up.parent = parent.Up;
		}
	}
	
	public void update()
	{
		//Logger.post( "Update" );
		WorldVert c0 = corner.get(0);
		WorldVert c1 = corner.get(1);
		WorldVert c2 = corner.get(2);

		/*Logger.post( "c0 pos " + c0.pos.toS() );
		 Logger.post( "c1 pos " + c1.pos.toS() );
		 Logger.post( "c2 pos " + c2.pos.toS() );*/

		if( edge == null ){
			edge = new ArrayList<WorldEdge>();
			edge.add( new WorldEdge( 0, this ) );
			edge.add( new WorldEdge( 1, this ) );
			edge.add( new WorldEdge( 2, this ) );
		}

		WorldEdge e0 = edge.get(0);
		WorldEdge e1 = edge.get(1);
		WorldEdge e2 = edge.get(2);
		e0.vec = c1.pos.sub( c0.pos );
		e1.vec = c2.pos.sub( c1.pos );
		e2.vec = c0.pos.sub( c2.pos );

		/*Logger.post( "e0 vec " + e0.vec.toS() );
		 Logger.post( "e1 vec " + e1.vec.toS() );
		 Logger.post( "e2 vec " + e2.vec.toS() );*/

		pos = ( c0.pos.add( c1.pos ).add( c2.pos ) ).div( 3 );

		// aligns w surface up
		// https://math.stackexchange.com/questions/305642/how-to-find-surface-normal-of-a-triangle
		// V = P1 - P0, W = P2 - P0;  VxW  V = e0, W = -e2
		zLocal_3D = e0.vec.cross( ( e2.vec ).mult( -1 ) );
		zLocal_3D = zLocal_3D.norm();
	}
	
	public void splitEdge( List<WorldVert> verts, List<WorldTri> surface )
	{
		//Logger.post( "Entered split edge" );

		if( child != null ){
			for( WorldTri chld : child ){
				chld.splitEdge( verts, surface );

			}
			if( mVisible ){
				surface.remove( this);
				mVisible = false;
			}
		} else{
			//Logger.post( "adding children" );

			int vLen = verts.size();

			// foreach edge, find out
			// if neighbors are split, use existing midpt
			// else, make new midpoint
			for( int i = 0; i < 3; i++ ){
				if( edge.get(i).twin.midpt != null ){
					edge.get(i).midpt = edge.get(i).twin.midpt;
				} else {
					int ip1 = (i+1)%3;
					WorldVert cI = corner.get(i);
					WorldVert cIp1 = corner.get(ip1);
					Vec3 cPos = ( ( cI.pos.add( cIp1.pos ).mult(0.5f) ).norm() ).mult( displayLayer.mRadius );
					float pct = ( cI.pctSurface + cIp1.pctSurface ) * 0.5f;
					edge.get(i).midpt = new WorldVert( vLen, pct, cPos );
					edge.get(i).midpt.elevation = ( cI.elevation + cIp1.elevation ) * 0.5f;
					verts.add( edge.get(i).midpt );
					vLen = verts.size();
				}
			}

			float pctSplit = pctSurface / 4.0f;

			child = new ArrayList<WorldTri>();
			//Logger.post( "adding child 0,1,2: " + corner.get(0).pos.toS() + " | " + edge.get(0).midpt.pos.toS() + " | " + edge.get(2).midpt.pos.toS() );
			child.add( new WorldTri( this.displayLayer, this, pctSplit, corner.get(0), edge.get(0).midpt, edge.get(2).midpt ) );
			//Logger.post( "adding child 0,1,2: " + edge.get(0).midpt.pos.toS() + " | " + corner.get(1).pos.toS() + " | " + edge.get(1).midpt.pos.toS() );
			child.add( new WorldTri( this.displayLayer, this, pctSplit, edge.get(0).midpt, corner.get(1), edge.get(1).midpt ) );
			//Logger.post( "adding child 0,1,2: " + edge.get(2).midpt.pos.toS() + " | " + edge.get(1).midpt.pos.toS() + " | " + corner.get(2).pos.toS() );
			child.add( new WorldTri( this.displayLayer, this, pctSplit, edge.get(2).midpt, edge.get(1).midpt, corner.get(2) ) );
			//Logger.post( "adding child 0,1,2: " + edge.get(0).midpt.pos.toS() + " | " + edge.get(1).midpt.pos.toS() + " | " + edge.get(2).midpt.pos.toS() );
			child.add( new WorldTri( this.displayLayer, this, pctSplit, edge.get(1).midpt, edge.get(2).midpt, edge.get(0).midpt ) );

			//Logger.post( "assigning sides and edges" );

			// assign the sides for our child tris
			int e = 0;
			for( WorldEdge edg: edge ){
				int ep1 = (e+1)%3;
				if( edg.side.child == null ){
					// add ind 0 side to tr 0 & 1
					child.get(e).edge.get(e).side   = edg.side;
					child.get(ep1).edge.get(e).side = edg.side;
					child.get(e).edge.get(e).twin   = edg.twin;
					child.get(ep1).edge.get(e).twin = edg.twin;
				} else {
					// find out which side of edg.side this tri is on
					int t = edg.twin.ind;
					int tp1 = (t+1)%3;
					// add ind X side children to tr s & sp1
					// s0: 1>0,0>1, s1: 2>0, 1>1, s2: 0>0, 2>1
					child.get(e).edge.get(e).side = edg.side.child.get(tp1);
					child.get(ep1).edge.get(e).side = edg.side.child.get(t);

					child.get(e).edge.get(e).twin = edg.side.child.get(tp1).edge.get(t);
					child.get(ep1).edge.get(e).twin = edg.side.child.get(t).edge.get(t);

					// add new children to existing 
					edg.side.child.get(t).edge.get(t).side = child.get(ep1);
					edg.side.child.get(tp1).edge.get(t).side = child.get(e);

					edg.side.child.get(t).edge.get(t).twin = child.get(ep1).edge.get(e);
					edg.side.child.get(tp1).edge.get(t).twin = child.get(e).edge.get(e);
				}
				e++;
			}

			//Logger.post( "infacing tri" );
			// set all infacing sides equal to central tri
			for( int i = 0; i < 3; i++ ){
				int ip1 = (i+1)%3;
				child.get(i).edge.get(ip1).side = child.get(3);
				child.get(3).edge.get(ip1).side = child.get(i);
				child.get(i).edge.get(ip1).twin = child.get(3).edge.get(ip1);
				child.get(3).edge.get(ip1).twin = child.get(i).edge.get(ip1);
			}
		}

		for( int c = 0; c < 4; c++ ){
			child.get(c).mPlate = mPlate;
		}

		surface.add( child.get(0) );
		surface.add( child.get(1) );
		surface.add( child.get(2) );
		surface.add( child.get(3) );
	}

	public void set2DCFDEnv( float angle, float velMag, Vec3 upVecWorld ){
		particles = new ArrayList<Particle>();

		mAngleOffAxis = angle;

		area = edge.get(0).vec.cross( edge.get(1).vec ).length()/2.0f;
		mDensityAvgEarth = mPlate.world.mDensityAvgEarth;

		scale = (float)Math.sqrt( area );

		// Compute component of source sink dir(a) in "out" (pos norm) dir(b)
		// sub component a . b / |b|
		//float outFlowMag = upVecWorld.dot( pos ) / ( pos.length() );

		// find up (+y) and (+x) in worldCoor
		xLocal_3D = ( edge.get(0).vec ).norm();
		yLocal_3D = ( zLocal_3D.cross(xLocal_3D) ).norm();

		// rewrite properties as 2D
		// corner pos rel to 0 center
		corner2D = new ArrayList<Vec2>();
		for( int i = 0; i < 3; i++ ){
			Vec3 cVec = corner.get(i).pos.sub( pos );
			corner2D.add( new Vec2( cVec.dot( xLocal_3D ), cVec.dot( yLocal_3D ) ) );
		}

		for( int i = 0; i < 3; i++ ){
			int ip1 = (i+1)%3;
			edge.get(i).vec2D = corner2D.get(ip1).sub( corner2D.get(i) );
			edge.get(i).BC = null;
		}

		if( mPlate.mMoveDir == null ){
			// start as negative of upVec (source to sink)
			// rotate by angleOffAxis
			// https://stackoverflow.com/questions/4780119/2d-euclidean-vector-rotations
			float flowX = upVecWorld.dot( xLocal_3D );
			float flowY = upVecWorld.dot( yLocal_3D );
			float cs = (float)Math.cos( mAngleOffAxis );
			float sn = (float)Math.sin( mAngleOffAxis );
			float px = flowX * cs - flowY * sn; 
			float py = flowX * sn + flowY * cs;
			mFlowDir_2D = ( new Vec2( px, py ) ).norm();
			mFlowDir_2D = mFlowDir_2D.mult( velMag );
			mVelMag = velMag;
		} else {
			float flowX = mPlate.mMoveDir.dot( xLocal_3D );
			float flowY = mPlate.mMoveDir.dot( yLocal_3D );
			mFlowDir_2D = ( new Vec2( flowX, flowY ) ).norm();
			mFlowDir_2D = mFlowDir_2D.mult( velMag );
			mVelMag = velMag;
		}
	}
	
	public void set3DCFDEnv(){
		particles = new ArrayList<Particle>();

		area = edge.get(0).vec.cross( edge.get(1).vec ).length()/2.0f;
		scale = (float)Math.sqrt( area );
		
		// find up (+y) and (+x) in worldCoor
		xLocal_3D = ( edge.get(0).vec ).norm();
		yLocal_3D = ( zLocal_3D.cross(xLocal_3D) ).norm();
		//Logger.post( "x,y, z local: x" + xLocal_3D.toS() + ", y " + yLocal_3D.toS() + ", z " + zLocal_3D.toS() );

		// rewrite properties as 2D
		// corner pos rel to 0 center
		corner2D = new ArrayList<Vec2>();
		for( int i = 0; i < 3; i++ ){
			Vec3 cVec = corner.get(i).pos.sub( pos );
			corner2D.add( new Vec2( cVec.dot( xLocal_3D ), cVec.dot( yLocal_3D ) ) );
		}

		for( int i = 0; i < 3; i++ ){
			int ip1 = (i+1)%3;
			edge.get(i).vec2D = corner2D.get(ip1).sub( corner2D.get(i) );
			edge.get(i).BC = null;
		}
	}

	public WorldTri findNearestChild(Vec3 pos)
	{
		if( child != null ){
			WorldTri nearest = null;
			float distMin = Float.MAX_VALUE;
			for( WorldTri chld : child ){
				WorldTri nearChild = chld.findNearestChild( pos );
				float dist = pos.sub( nearChild.pos ).lenSq();
				if( distMin > dist ){
					nearest = nearChild;
					distMin = dist;
				}
			}
			return nearest;
		} else {
			return this;
		}
	}
	
	public void checkBCs( float ts, Particle prt)
	{
		prt.triggeredSideBC = false;
		prt.triggeredTriShift = false;
		// checks a particle against each edge
		for( int i = 0; i < edge.size(); i++ ){
			int im1 = (i+2)%3; // i-1+3
			int ip1 = (i+1)%3;
			// has it crossed
			
			// find parallel magnitude (P-ci) . (cip1-ci).norm
			// P-ci = particle local vector relative to near edge corner
			Vec2 vecPrt = prt.pos2D.sub( corner2D.get(i) );
			Vec2 edgePara = edge.get(i).vec2D.norm();
			Vec2 midPt = ( corner2D.get(ip1).add( corner2D.get(i) ) ).mult(0.5f);
			Vec2 edgePerp = new Vec2( -edgePara.gY(), edgePara.gX() );
			Vec2 vecIn = midPt.mult(-1).norm();
			float inVsRotate = vecIn.dot( edgePerp );
			//Logger.post( "In vs rotate perp edge vectors, dot: " + inVsRotate );
			
			float paraLen = vecPrt.dot( edgePara );
			Vec2 vecPara  = edgePara.mult( paraLen );
			
			// find perp portion  (P-c0) [vec] - ( (P-c0) . (c1-c0).norm ) * (c1-c0).norm [para vec]
			Vec2 vecPerp = vecPrt.sub( vecPara );
			
			// find if perp vec and other corner point in same dir
			float inComp = vecPerp.dot( edgePerp );
			
			if( inComp < 0 ){
				float perpLen = vecPerp.length();
				// if crossed edge
				if( edge.get(i).BC != null ){
					edge.get(i).actBC( ts, prt, paraLen, perpLen );
					if( prt.parent == null ){
						break;
					}
				} else {
					//edge.get(i).prtToSide( prt, paraLen, perpLen );
					prt.triggeredTriShift = true;
				}
			}
			//Logger.post("Leaving loop " + i );
		}
		//Logger.post( "Leaving check bc" );
	}
	
	public void activateInflowBC( float ts )
	{
		// for any inflow bcs, add particles of mass
		for( int i = 0; i < edge.size(); i++ ){
			WorldEdge edg = edge.get(i);
			if( edg.BC != null ){
				if( edg.BC.mType == CFDBnd.IN ){
					CFDBnd tBC = edg.BC;
					/*Logger.post( "edge " + i + " executing BC " + tBC.mType + " w args " + tBC.mArgs[0] + "," + tBC.mArgs[1] + "," + tBC.mArgs[2] );
					Logger.post( "edge flowThisStep, vec2d " + tBC.flowThisStep + ", " + edg.vec2D.toS() );
					Logger.post( "corner2d " + corner2D + " particles " + particles );
					Logger.post( "Elev Target " + mPlate.mElevAvgTarget + ", accG " + accGrav );
					*/
					//Logger.post( "edge ind " + edge.get(i).ind +  );
					edg.actBC( ts );
					//Logger.post( "Inflow: timestep " + ts + ", flowTarget " + tBC.mArgs[0] + " flowThisStep " + tBC.flowThisStep + " prtCnt " + particles.size() );
				}
			}
		}
	}

	
	public void accumulateProps()
	{
		/*if( particles != null ){
			Logger.post( "prtCnt in accumulate " + particles.size() );
		}*/
		for( Particle prt: particles ){
			// Gather properties from this particle
			// apply them to the mesh corners
			// based on distance
			if( prt.pos3D != null ){
				accumulateProps3D( prt );
			} else {
				float dTot = 0;
				float[] cDist = new float[3];
				float[] cPct = new float[3];
				//Logger.post( "prt pos " + prt.pos2D.toS() + " crn2d " + corner2D );
				for( int i = 0; i < 3; i++ ){
					cDist[i] = ( prt.pos2D.sub( corner2D.get(i) ) ).length();
					//cDist[i] *= cDist[i];
					dTot += cDist[i];
				}

				//Logger.post( "disttot " + dTot + " cDist[0,1,2] " + cDist[0] + "," + cDist[1] + "," + cDist[2] );
				for( int i = 0; i < 3; i++ ){
					WorldVert cI = corner.get(i);
					cPct[i] = cDist[i] / dTot;
					cI.mass += prt.mass * cPct[i];
					cI.momentum2D.addEq( prt.momentum2D.mult( cPct[i] ) );
					cI.energy += prt.energy * cPct[i];
					cI.prtAccumlated++;
				}

				/*if( Math.abs( prt.momentum2D.gX() ) > 0 ){
				 Logger.post( "prt w > 0 momentum, " + prt.momentum2D.toS() );
				 Logger.post( " corners after: c0 " + corner.get(0).momentum2D.toS() + ", c1 " + corner.get(1).momentum2D.toS() + ", c2 " + corner.get(2).momentum2D.toS() );
				 }*/
			}
		}
	}

	private void accumulateProps3D( Particle prt )
	{
		// same as 2D but w more distance
		float dTot = 0;
		float[] cDist = new float[3];
		float hDelta = Up.height - height;
		//Logger.post( "prt pos " + prt.pos2D.toS() + " crn2d " + corner2D );
		for( int i = 0; i < 3; i++ ){
			cDist[i] = ( prt.pos3D.gXY().sub( corner2D.get(i) ) ).length();
			dTot += cDist[i];
		}

		float zPctBot, zPctTop;
		float[] cPct = new float[3];
		float[] cPctTop = new float[3];
		float[] cPctBot = new float[3];
		zPctBot = ( hDelta - prt.pos3D.gZ() ) / hDelta;
		zPctTop = 1.0f - zPctBot;
		
		//Logger.post( "disttot " + dTot + " cDist[0,1,2] " + cDist[0] + "," + cDist[1] + "," + cDist[2] );
		// accumulate properties from all prt between
		// cI (base) and cU (upper tri)
		for( int i = 0; i < 3; i++ ){
			WorldVert cI = corner.get(i);
			WorldVert cU = Up.corner.get(i);
			cPct[i] = cDist[i] / dTot;
			cPctBot[i] = cPct[i] * zPctBot;
			cPctTop[i] = cPct[i] * zPctTop;
			
			cI.mass += prt.mass * cPctBot[i];
			cI.momentum3D.addEq( prt.momentum3D.mult( cPctBot[i] ) );
			cI.energy += prt.energy * cPctBot[i];
			cI.Cv += prt.Cv * cPctBot[i];
			cI.molM += prt.molM * cPctBot[i];
			cI.prtAccumlated++;
			
			cU.mass += prt.mass * cPctTop[i];
			cU.momentum3D.addEq( prt.momentum3D.mult( cPctTop[i] ) );
			cU.energy += prt.energy * cPctTop[i];
			cU.Cv += prt.Cv * cPctTop[i];
			cU.molM += prt.molM * cPctTop[i];
			cU.prtAccumlated++;
		}
	}

	public void computeBulkProps()
	{
		// take accumulated properties
		// calculate bulk properties like
		// elevation, velocity
		if( Up != null ){
			computeBulkProps3D();
		} else {
			// corners occupy same area as tri
			//float elevTarget = mPlate.world.mElevAvgTarget;
			float cArea = 2 * area;
			elevPerMass = 1 / ( cArea * mDensityAvgEarth );
			for( int i = 0; i < 3; i++ ){
				WorldVert cI = corner.get(i);
				if( cI.mass > 0 ){
					cI.vel2D = cI.momentum2D.div( cI.mass );
				} else {
					cI.vel2D.eq( 0.0f, 0.0f );
				}
				cI.vol = cI.mass / mDensityAvgEarth;
				cI.elevation = cI.vol / cArea;
				cI.KE = 0.5f * cI.mass * cI.vel2D.dot( cI.vel2D );
				cI.PE = cI.mass * accGrav * cI.elevation * 0.5f;
				cI.energy = cI.PE + cI.KE;
				//Logger.post( "prt " + cI.prtAccumlated + " mass " + cI.mass + " elev " + cI.elevation + ", PE " + cI.PE + ", KE " + cI.KE + ", E " + cI.energy + ", G " + accGrav + ", rho " + mDensityAvgEarth );
				cI.pressure = ( cI.PE ) / cI.vol;  // avg press (kg m/s2)/m2 from kg-m2/s2
				//cI.density = cI.mass / ( area * elevTarget ); // density w reference elev
			}
		}
	}

	private void computeBulkProps3D()
	{
		// similar to 2D, but now we have a prism
		// made of two stacked tris
		//float elevTarget = mPlate.world.mElevAvgTarget;
		float cArea = 2 * area;
		for( int i = 0; i < 3; i++ ){
			WorldVert cI = corner.get(i);
			
			if( cI.mass > 0 ){ cI.vel3D = cI.momentum3D.div( cI.mass ); }
			else {             cI.vel3D.eq( 0.0f, 0.0f, 0.0f ); }
			
			// find the mass above our tile pressurizing us
			float massAbove = 0.0f;
			WorldTri triCur = this;
			while( triCur != null ){
				massAbove += triCur.corner.get(i).mass;
				triCur = triCur.Up;
			}
			
			cI.mol = cI.mass / cI.molM;
			cI.pressure = massAbove * accGrav / cArea;
			cI.KE = 0.5f * cI.mass * cI.vel3D.dot( cI.vel3D );
			cI.PE = cI.energy - cI.KE;
			cI.temp = cI.PE / (cI.mol*cI.Cv);
			cI.density = cI.pressure * cI.molM / (R * cI.temp);
		}
	}

	public WorldTri[] splitCenter(WorldVert cN)
	{
		// split tris like
		//     c2
		//   / cN \
		// c0  --  c1
		float weightSplit = pctSurface / 3.0f;
		child = new ArrayList<WorldTri>();
		int tp1, tm1;
		for( int t = 0; t < 3; t++ ){
			tm1 = (t+2)%3;  // -1+3 for %
			tp1 = (t+1)%3;
			child.add( new WorldTri( this.displayLayer, this, weightSplit, corner.get(t), corner.get(tp1), cN ) );
		}
		
		for( int t = 0; t < 3; t++ ){
			// edge t = parent edge t
			// other edge 0,1,2 = child 0,1,2
			tm1 = (t+2)%3;  // -1+3 for %
			tp1 = (t+1)%3;
			child.get(t).edge.set( t, edge.get(t) );
			child.get(t).edge.get( tm1 ).side = child.get(tm1);
			child.get(t).edge.get( tp1 ).side = child.get(tp1);
			// make sure neighbor that changed knows about change
			child.get(t).edge.get(t).twin.side = child.get(t);
			child.get(t).edge.get(t).twin.twin = child.get(t).edge.get(t);
			// setup twin references for other edges
			child.get(t).edge.get(tm1).twin = child.get(tm1).edge.get(t);
			child.get(t).edge.get(tp1).twin = child.get(tp1).edge.get(t);
		}
		
		return new WorldTri[]{ child.get(0), child.get(1), child.get(2) };
	}
	
	public String toS(){
		String out = "Properties of; " + this;
		out += "\ncorners: ";
		out += "\n" + corner.get(0);
		out += "\n pos " + corner.get(0).pos.toS();
		out += "\n" + corner.get(1);
		out += "\n pos " + corner.get(1).pos.toS();
		out += "\n" + corner.get(2);
		out += "\n pos " + corner.get(2).pos.toS();
		out += "\nedges: ";
		out += "\n" + edge.get(0);
		out += "\n vec " + edge.get(0).vec.toS();
		out += "\n side " + edge.get(0).side;
		out += "\n twin " + edge.get(0).twin;
		out += "\n" + edge.get(1);
		out += "\n vec " + edge.get(1).vec.toS();
		out += "\n side " + edge.get(1).side;
		out += "\n twin " + edge.get(1).twin;
		out += "\n" + edge.get(2);
		out += "\n vec " + edge.get(2).vec.toS();
		out += "\n side " + edge.get(2).side;
		out += "\n twin " + edge.get(2).twin;
		out += "\nzlocal: " + zLocal_3D.toS();
		return out;
	}
}
