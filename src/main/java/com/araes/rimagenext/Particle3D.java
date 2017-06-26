package com.araes.rimagenext;

public class Particle3D
{
	public WorldTri parent;
	public Vec3 pos;
	public Vec3 vel;
	
	public float mass;
	public float energy;
	public float KE;
	public float Cd;
	public float density;
	public float radius;
	public float areaXSec;
	public float areaSurf;
	public float volume;
	public float elevLoc;
	
	public boolean triggeredTriShift;
	public boolean triggeredSideBC;

	public Particle3D( Vec3 pIn, boolean trigShift, boolean trigSide ){
		pos = pIn;
		triggeredTriShift = trigShift;
		triggeredSideBC = trigSide;
	}
	
	public Particle3D( Particle prt ){
		WorldTri tTri = prt.parent;
		Vec3 x = tTri.xLocal_3D.mult( prt.pos2D.gX()/tTri.scale );
		Vec3 y = tTri.yLocal_3D.mult( prt.pos2D.gY()/tTri.scale );
		Vec3 vX = tTri.xLocal_3D.mult( prt.vel2D.gX()/tTri.scale );
		Vec3 vY = tTri.yLocal_3D.mult( prt.vel2D.gY()/tTri.scale );
		
		pos = tTri.pos.add( x ).add( y );
		//pos = pos.norm();
		vel = vX.add( vY );
		
		parent = tTri;
		mass = prt.mass;
		energy = prt.energy;
		KE = prt.KE;
		Cd = prt.Cd;  // sphere drag
		density = prt.density; // kg/ m3 default water;
		radius = prt.radius;
		areaXSec = prt.areaXSec;
		areaSurf = prt.areaSurf;
		volume = prt.volume;
		elevLoc = prt.elevLoc;
		triggeredSideBC = prt.triggeredSideBC;
		triggeredTriShift = prt.triggeredTriShift;
	}

	public void findNearestTri()
	{
		WorldTri nearest = parent;
		float distMin = ( pos.sub( nearest.pos ) ).lenSq();
		boolean foundNearTri = true;
		
		while( foundNearTri ){
			foundNearTri = false;
			for( WorldEdge edge: nearest.edge ){
				WorldTri side = edge.side;
				float dist = ( pos.sub( side.pos ) ).lenSq();
				if( distMin > dist ){
					distMin = dist;
					nearest = side;
					foundNearTri = true;
		}}}
		
		parent = nearest;
	}
}
