package com.araes.rimagenext;

public class WorldVert
{
	int ind = -1;
	
	public WorldVert Up = null;
	
	public Vec3 pos = new Vec3( 0.0f, 0.0f, 0.0f );
	public Vec3 vel = new Vec3( 0.0f, 0.0f, 0.0f );
	public Vec3 acc = new Vec3( 0.0f, 0.0f, 0.0f );
	
	public Vec2 vel2D      = new Vec2( 0.0f, 0.0f );
	public Vec2 momentum2D = new Vec2( 0.0f, 0.0f );
	
	public Vec3 vel3D      = new Vec3( 0.0f, 0.0f, 0.0f );
	public Vec3 momentum3D = new Vec3( 0.0f, 0.0f, 0.0f );
	
	public float pctSurface;
	
	public float mass = 0.0f;
	public float mol  = 0.0f;
	public float molM = 1;
	public float Cv   = 1;
	
	public float energy = 0.0f;
	public float PE = 0.0f;
	public float KE = 0.0f;
	public float pressure = 0.0f;
	public float temp = 288.15f;  // K
	public float density;
	
	public float elevPerMass;
	public float elevation = 1.0f;
	public float vol;

	public boolean hasTriggered = false;

	public int prtAccumlated = 0;

	private float QRate;

	public WorldVert( int indIn, float pctSurfaceIn ){
		ind = indIn;
		pctSurface = pctSurfaceIn;
	}
	
	public WorldVert( int indIn, float pctSurfaceIn, Vec3 posIn ){
		ind = indIn;
		pctSurface = pctSurfaceIn;
		pos = posIn;
	}
	
	public void solveSolarEnergy( float Qs )
	{
		// for land area this tile, based on latitude
		// find usable sun hours per day at 1000 kW/m2
		// apply in oscillating sin wave
		// w time to heat land / air
		QRate = Qs * ( 0.66f * ( 1 - pos.gZ() * pos.gZ() ) + 0.34f );
	}
}
