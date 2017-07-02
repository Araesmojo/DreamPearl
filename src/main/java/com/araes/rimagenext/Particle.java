package com.araes.rimagenext;
import java.util.List;
import java.util.ArrayList;

public class Particle
{
	public WorldTri parent = null;
	public WorldTri nearest = null;
	
	public Vec3 pos = new Vec3( 0.0f, 0.0f, 0.0f );
	public Vec3 vel = new Vec3( 0.0f, 0.0f, 0.0f );
	
	public Vec2 pos2D = new Vec2( 0.0f, 0.0f );
	public Vec2 vel2D = new Vec2( 0.0f, 0.0f );
	public Vec2 acc2D = new Vec2( 0.0f, 0.0f );
	public Vec2 pos2Dpr = new Vec2( 0.0f, 0.0f );
	public Vec2 momentum2D = new Vec2( 0.0f, 0.0f );
	
	public Vec3 pos3D      = null;
	public Vec3 vel3D      = new Vec3( 0.0f, 0.0f, 0.0f );
	private Vec3 acc3D     = new Vec3( 0.0f, 0.0f, 0.0f );
	private Vec3 pos3Dpr   = new Vec3( 0.0f, 0.0f, 0.0f );
	public Vec3 momentum3D = new Vec3( 0.0f, 0.0f, 0.0f );

	public float mass = 1.0f;
	public float Cv;
	public float temp;
	public float rho;
	public float mol = 1.0f;
	
	public float energy = 0;
	public float KE = 0;
	public float PE = 0;
	
	public float molM = 1.0f;  //0.0289644 kg/mol for air
	public float R = 8.31447f; // J/mol-K
	
	public Vec2 FVec2D = new Vec2( 0.0f, 0.0f );
	
	public float Cd = 0.5f;  // sphere drag
	public float density = 1000; // kg/ m3 default water;
	public float radius;
	public float areaXSec;
	public float areaSurf;
	public float volume;

	public float elevLoc = 0.0f;
	
	private float timeCur = 0.0f;

	public boolean triggeredSideBC = false;
	public boolean triggeredTriShift = false;
	
	public Particle( WorldTri par, float massIn, float densityIn ){
		parent = par;
		mass = massIn;
		density = densityIn;
		InitParticle();
	}
	
	public Particle( WorldTri par, float massIn, float densityIn, Vec2 pos2DIn ){
		parent = par;
		mass = massIn;
		pos2D = pos2DIn;
		density = densityIn;
		InitParticle();
	}
	
	public Particle( WorldTri par, float massIn, float densityIn, Vec3 pos3DIn ){
		parent = par;
		mass = massIn;
		pos3D = pos3DIn;
		density = densityIn;
		InitParticle();
	}
	
	private void InitParticle(){
		// V = 4/3*PI*r3
		// SA = 4*PI*r2
		volume = mass / density;
		float rQ = volume * 0.75f / (float)Math.PI;
		radius = (float)Math.cbrt( rQ );
		areaSurf = 4 * (float)Math.PI * radius * radius;
		areaXSec = areaSurf / 4;
		//energy = parent.accGrav * mass * parent.mPlate.mElevAvgTarget;
	}
	
	public void delete()
	{
		// WARNING; this is a hack.
		// todo; figure why parent == null can occur
		//if( this.parent != null ){
			this.parent.particles.remove( this );
			parent = null;
			nearest = null;
		//}
	}
	
	public void pos2DTo3D()
	{
		if( pos3D != null ){
			Vec3 x = parent.xLocal_3D.mult( pos3D.gX() );  // /parent.scale
			Vec3 y = parent.yLocal_3D.mult( pos3D.gY() );  // /parent.scale
			Vec3 z = parent.zLocal_3D.mult( pos3D.gZ() );  // /parent.scale
			Vec3 vX = parent.xLocal_3D.mult( vel2D.gX() ); // /parent.scale
			Vec3 vY = parent.yLocal_3D.mult( vel2D.gY() ); // /parent.scale
			Vec3 vZ = parent.zLocal_3D.mult( vel3D.gZ() ); // /parent.scale

			pos = parent.pos.add( x ).add( y ).add( z );
			if( pos.length() > 1.7f * parent.displayLayer.mRadius ){
				parent.Logger.post( "prt pos " + pos.toS() );
			}
			vel = vX.add( vY ).add( vZ );
		} else {
			Vec3 x = parent.xLocal_3D.mult( pos2D.gX() );  // /parent.scale
			Vec3 y = parent.yLocal_3D.mult( pos2D.gY() );  // /parent.scale
			Vec3 vX = parent.xLocal_3D.mult( vel2D.gX() ); // /parent.scale
			Vec3 vY = parent.yLocal_3D.mult( vel2D.gY() ); // /parent.scale

			pos = parent.pos.add( x ).add( y );
			if( pos.length() > 1.7f * parent.displayLayer.mRadius ){
				parent.Logger.post( "prt pos " + pos.toS() );
			}
			vel = vX.add( vY );
		}
	}

	public void findNearestTri( float ts )
	{
		nearest = parent;
		float distMin = ( pos.sub( nearest.pos ) ).lenSq();
		boolean foundNearTri = false;

		while( !foundNearTri ){
			foundNearTri = true;
			if( pos3D != null ){
				if( pos3D.gZ() > parent.height ){
					if( parent.Up != null ){
						if( parent.Up.height > 0.0f ){
							nearest = parent.Up;
							foundNearTri = false;
						} else {
							pos3D.sZ( parent.height * 0.999f );
						}
					} else {
						pos3D.sZ( parent.height * 0.999f );
					}
				}
				if( pos3D.gZ() < 0.0f ){
					if( parent.Down != null ){
						nearest = parent.Up;
						foundNearTri = false;
					} else {
						pos3D.sZ( 0.0f );
					}
				}
			}
			
			if( foundNearTri ){
				for( WorldEdge edge: nearest.edge ){
					WorldTri side = edge.side;
					float dist = ( pos.sub( side.pos ) ).lenSq();
					if( distMin > dist ){
						distMin = dist;
						nearest = side;
						foundNearTri = false;
					}
				}
			}
		}
		
		if( parent != nearest ){
			parent.checkBCs( ts, this );
			if( pos3D != null ){
				pos3D.sX( pos2D.gX() );
				pos3D.sY( pos2D.gY() );
			}
		}
		parent = nearest;
	}

	public void pos3DTo2D()
	{
		// Transform the position to 2D
		// check we would end up in same spot (reversible)
		Vec3 posLoc = pos.sub( parent.pos );
		float pZ = posLoc.dot( parent.zLocal_3D );
		Vec3 posNoZ = posLoc.sub( parent.zLocal_3D.mult( pZ ) );
		float pX = posNoZ.dot( parent.xLocal_3D );
		float pY = posNoZ.dot( parent.yLocal_3D );
		Vec2 pos2Dnew = ( new Vec2( pX, pY ) ); //.mult( parent.scale );

		//parent.Logger.post( "tri scale " + parent.scale + " x,y,z comp of posLoc " + pX + ", " + pY + ", " + pZ );
		pos2D = pos2Dnew;

		// same for vel
		float vZ = vel.dot( parent.zLocal_3D );
		Vec3 vNoZ = vel.sub( parent.zLocal_3D.mult( vZ ) );
		float vX = vNoZ.dot( parent.xLocal_3D );
		float vY = vNoZ.dot( parent.yLocal_3D );
		vel2D = ( new Vec2( vX, vY ) ); //.mult( parent.scale );
		//parent.Logger.post( "prt pos2d, orig " + pos2D.toS() + " in 3d " + pos.toS() + " after 2D-3D-2D, " + pos2Dnew.toS() + " vel3D " + vel.toS() + " vel2D " + vel2D.toS() );
		if( pos3D != null ){
			pos3D = new Vec3( pos2D.gX(), pos2D.gY(), pZ );
			vel3D = new Vec3( vel2D.gX(), vel2D.gY(), vZ );
		}
	}

	public void update( float timestep )
	{
		if( pos3D != null ){
			update3D( timestep );
		} else {
			update2D( timestep );
		}
	}

	private void update3D(float timestep)
	{
		timeCur += timestep;
		
		// find distance to bottom corners
		float[] cPctBot    = new float[3];
		Vec2[]  cVecBot2D  = new Vec2[3];
		float[] cPctTop    = new float[3];
		Vec2[]  cVecTop2D  = new Vec2[3];
		find2DCornerDist( parent, pos2D, cPctBot, cVecBot2D );
		
		// find distance to top corners
		find2DCornerDist( parent.Up, pos2D, cPctTop, cVecTop2D );
		
		// find distance between top - bottom
		float distBot = pos3D.gZ();
		float distTop = parent.height - pos3D.gZ();
		
		// find the 3D pct and vectors
		Vec3[] cVecBot = new Vec3[ 3 ];
		Vec3[] cVecTop = new Vec3[ 3 ];
		for( int i = 0; i < 3; i++ ){
			cVecBot[i] = new Vec3( cVecBot2D[i].gX(), cVecBot2D[i].gY(), distBot ).norm();
			cVecTop[i] = new Vec3( cVecTop2D[i].gX(), cVecTop2D[i].gY(), distTop ).norm();
			cPctBot[i] *= (1 - distBot/parent.height);
			cPctTop[i] *= distTop/parent.height;
		}
		
		// accumulate P, v top and bottom
		elevLoc = 0.0f;
		float PLoc = 0.0f;
		float densityLoc = 0.0f;
		float TLoc = 0.0f;
		Vec2[] PGradBot = new Vec2[3];
		Vec2[] PGradTop = new Vec2[3];
		Vec3 PGradLoc = new Vec3( 0, 0, 0 );
		Vec3 vLoc = new Vec3( 0, 0, 0 );
		for( int i = 0; i < 3; i++ ){
			WorldVert cI = parent.corner.get(i);
			WorldVert cIp1 = parent.corner.get((i+1)%3);
			Vec2 vecP = parent.edge.get(i).vec2D.norm();
			float eLen = parent.edge.get(i).vec2D.length();
			PGradBot[i] = vecP.mult( ( cIp1.pressure - cI.pressure )/eLen );
			
			vLoc.addEq(( cI.vel3D ).mult( cPctBot[i] ));
			TLoc += cI.temp * cPctBot[i];
			densityLoc += cI.density * cPctBot[i];
			
			WorldVert cU = parent.Up.corner.get(i);
			WorldVert cUp1 = parent.Up.corner.get((i+1)%3);
			vecP = parent.Up.edge.get(i).vec2D.norm();
			eLen = parent.Up.edge.get(i).vec2D.length();
			PGradTop[i] = vecP.mult( ( cUp1.pressure - cU.pressure )/eLen );
			
			vLoc.addEq(( cU.vel3D ).mult( cPctTop[i] ));
			TLoc += cU.temp * cPctTop[i];
			densityLoc += cU.density * cPctTop[i];
			
			elevLoc += cI.elevation * cPctBot[i];
			elevLoc += cU.elevation * cPctTop[i];
			
			Vec2 PGlo = PGradBot[(i+1)%3].mult( cPctBot[i] );
			Vec2 PGhi = PGradTop[(i+1)%3].mult( cPctTop[i] );
			PGradLoc.addEq( new Vec3( PGlo.gX(), PGlo.gY(), 0.0f ) );
			PGradLoc.addEq( new Vec3( PGhi.gX(), PGhi.gY(), 0.0f ) );
		}

		Vec3 FPres = PGradLoc.mult( -2.0f * radius * areaXSec );
		
		// radiation down from above
		// radiation vector at space.dot( zlocal )
		Vec3 vecSun = parent.displayLayer.mSun.mult( 1361 );
		float wattMax = vecSun.dot( parent.zLocal_3D );
		
		// vary from max by time of day
		float raSun = wattMax * (float)Math.cos( ( timeCur % 86400.0f ) / 43200.0f * Math.PI );
		
		// 35% reflect, 14% to air, 51% to ground
		float raSunToAir = raSun * 0.14f;
		
		// new energy from ground is phase delayed (heats up)
		float wattGround = wattMax * 0.51f * (float)Math.cos( ( ( timeCur % 86400.0f ) / 43200.0f - 0.25f ) * Math.PI );
		float raGroundAir = wattGround * 0.1176f; // 6% wattMax
		float conGroundAir = wattGround * 0.1765f; // 9% wattMax, energy only goes in 1st layer particles
		float evapGroundWater = wattGround * 0.3725f / ( parent.displayLayer.mPctLand * 0.75f ); // 19% wattMax, energy only goes in 1st layer particles
		float raAirToSpace = raSunToAir + raGroundAir - conGroundAir;
		
		// All particles absorb raSunToAir, raGroundAir
		// Particles on layer 0 also get convection and evap
		// Ground tiles evap 50% as much as full water for now
		// Approx 11000 kg absorb 1 m^2 worth of energy
		float energyParticle = (raSunToAir + raGroundAir - raAirToSpace ) * mass / 11000.0f;
		if( parent.height == parent.displayLayer.skyLayerInitH ){
			energyParticle += conGroundAir * 4;
		}
		
		// Now,P = PLoc
		// U = n*Cv*T, T = U/n*Cv
		// rho = P * M / (R*T);  M = 0.0289644 lg/mol
		// need mol of matter, mass stays same but density, V change
		// exchange energy
		energyParticle -= wattGround * 0.941f;
		energyParticle *= mass / 11000.0f;
		PE += energyParticle * timestep;
		PE -= 10*( temp - TLoc ) * areaSurf * timestep;
		
		// now calculate the new temp
		mol = mass / molM;
		temp = PE / (mol*Cv);
		
		// density changes based on temp
		density = PLoc * molM / (R * temp);
		volume = mass / density;
		
		// Fbouy = (rho_local - rho)*volume*G
		Vec3 FBouy = new Vec3( 0.0f, 0.0f, (densityLoc - density)*volume*parent.accGrav );

		// calc F drag
		// 1/2 * rho * v2 * Cd * areaXSec
		Vec3 vReduce = vel3D.sub( vLoc );
		float vMagSq = vReduce.lenSq();
		vReduce = vReduce.norm();
		Vec3 FDrag = vReduce.mult( -0.5f * Cd * parent.mDensityAvgEarth * vMagSq * areaXSec );

		// sum the forces
		Vec3 FSum = FPres.add( FDrag ).add( FBouy );
		//parent.Logger.post( "vLoc " + vLoc.toS() + " Forces: sum, " + FSum.toS() + ", FPLoc " + FPLoc.toS() + ", FDrag " + FDrag.toS() + ", Fric " + FFricPlate.toS() );

		// change acc -> vel -> pos based on forces
		pos3Dpr = new Vec3( pos3D.gX(), pos3D.gY(), pos3D.gZ() );
		Vec3 vOld = new Vec3( vel3D.gX(), vel3D.gY(), vel3D.gZ() );
		acc3D = FSum.div( mass );
		float accLen = acc3D.length();
		/*if( acc2D.length() > 1 ){
		 parent.Logger.post( " acc " + accLen + "vel " + vel2D.toS() + ", vloc " + vLoc.toS() + " sum, P, drag, fric " + FSum.toS() + ", " + FPLoc.toS() + ", " + FDrag.toS() + ", " + FFricPlate.toS() ); 
		 }*/
		vel3D.addEq( acc3D.mult( timestep ) );
		pos3D.addEq( ( vel3D.add( vOld ) ).mult( 0.5f * timestep ) );
		pos2D = new Vec2( pos3D.gX(), pos3D.gY() );
		
		// calculate new momentum (mv), energies (0.5mv2)
		momentum3D = vel3D.mult( mass );
		KE = 0.5f * momentum3D.dot( vel3D );
		energy = PE + KE;
	}

	private void find2DCornerDist(WorldTri tri, Vec2 pos2D, float[] cPct, Vec2[] cVec )
	{
		// find distance to corners
		float dTot = 0;
		float[] cDist = new float[3];
		cPct  = new float[3];
		cVec  = new Vec2[3];
		List<Vec2> cPos2D = tri.corner2D;
		for( int i = 0; i < 3; i++ ){
			cVec[i]  = pos2D.sub( cPos2D.get( i ) );
			cDist[i] = cVec[i].length();
			//cDist[i] *= cDist[i];
			dTot += cDist[i];
		}
	}
	
	private void update2D(float timestep)
	{
		//parent.Logger.post( "particle update() " );
		List<Vec2> cPos2D = parent.corner2D;
		triggeredSideBC = false;
		triggeredTriShift = false;

		// find distance to corners
		float dTot = 0;
		float[] cDist = new float[3];
		float[] cPct  = new float[3];
		Vec2[]  cVec  = new Vec2[3];
		Vec2[]  PGrad  = new Vec2[3];
		for( int i = 0; i < 3; i++ ){
			cVec[i]  = pos2D.sub( cPos2D.get( i ) );
			cDist[i] = cVec[i].length();
			//cDist[i] *= cDist[i];
			dTot += cDist[i];
			cVec[i] = cVec[i].norm();
			
			WorldVert cI   = parent.corner.get(i);
			WorldVert cIp1 = parent.corner.get((i+1)%3);
			Vec2 vecP = parent.edge.get(i).vec2D.norm();
			float eLen = parent.edge.get(i).vec2D.length();
			PGrad[i] = vecP.mult( ( cIp1.pressure - cI.pressure )/eLen );
		}

		// accumulate local P and v, calc Fp
		elevLoc = 0.0f;
		Vec2 PGradLoc = new Vec2( 0, 0 );
		Vec2 vLoc = new Vec2( 0, 0 );
		for( int i = 0; i < 3; i++ ){
			WorldVert cI = parent.corner.get(i);
			cPct[i] = cDist[i] / dTot;

			elevLoc += cI.elevation * cPct[i];
			PGradLoc.addEq( PGrad[(i+1)%3].mult( cPct[i] ) );
			vLoc.addEq(( cI.vel2D ).mult( cPct[i] ));
		}
		
		//Vec2 FPres = PGradLoc.mult( (10000000) * -2.0f * radius * areaXSec );
		Vec2 FPres = PGradLoc.mult( (20) * -2.0f * radius * areaXSec );

		// calc F drag
		// 1/2 * rho * v2 * Cd * areaXSec
		Vec2 vReduce = vel2D.sub( vLoc );
		float vMagSq = vReduce.lenSq();
		float vLen = (float)Math.sqrt(vMagSq);
		vReduce = vReduce.norm();
		Vec2 FDrag = vReduce.mult( -0.5f * Cd * parent.mDensityAvgEarth * vLen * areaXSec );
		//Vec2 FDrag = new Vec2( 0.0f, 0.0f );
		
		// apply plate friction as pull
		Vec2 vPltReduce = vel2D.sub( parent.mFlowDir_2D );
		float G = parent.accGrav;
		Vec2 FFricPlate = ( parent.mFlowDir_2D.norm() ).mult( 0.02f * G * mass );
		//FFricPlate = new Vec2( 0.0f, 0.0f );
		
		// sum the forces
		Vec2 FSum = FPres.add( FDrag ).add( FFricPlate );
		//parent.Logger.post( "vLoc " + vLoc.toS() + " Forces: sum, " + FSum.toS() + ", FPres " + FPres.toS() + ", FDrag " + FDrag.toS() + ", Fric " + FFricPlate.toS() );

		// change acc -> vel -> pos based on forces
		pos2Dpr = new Vec2( pos2D.gX(), pos2D.gY() );
		Vec2 vOld = new Vec2( vel2D.gX(), vel2D.gY() );
		acc2D = FSum.div( mass );
		float accLen = acc2D.length();
		/*if( acc2D.length() > parent.displayLayer.mRadius ){
			parent.Logger.post( " acc " + accLen + "vel " + vel2D.toS() + ", vloc " + vLoc.toS() + " Sum " + FSum.toS() + ", P " + FPres.toS() + ", Drag " + FDrag.toS() + ", Fric " + FFricPlate.toS() );
			parent.Logger.post( "mass " + mass + ", areaXC " + areaXSec + ", radius " + radius + ", c0 mass " + parent.corner.get(0).mass + ", c1 mass " + parent.corner.get(1).mass + ", c2 mass " + parent.corner.get(2).mass );
		}*/
		vel2D.addEq( acc2D.mult( timestep ) );
		pos2D.addEq( ( vel2D.add( vOld ) ).mult( 0.5f * timestep ) );

		// calculate new momentum (mv), energies (0.5mv2)
		momentum2D = vel2D.mult( mass );
		KE = 0.5f * momentum2D.dot( vel2D );
		energy = elevLoc * parent.accGrav * density - KE;
	}
}
