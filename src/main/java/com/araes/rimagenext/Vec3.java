package com.araes.rimagenext;

public class Vec3
{
	private float x = 0.0f;
	private float y = 0.0f;
	private float z = 0.0f;
	private float len = 0.0f;
	private float lenSq = 0.0f;
	
	boolean isDirty = true;
	
	public Vec3(){
		x = 0.0f;
		y = 0.0f;
		z = 0.0f;
	}
	
	public Vec3( float xn, float yn, float zn ){
		x = xn;
		y = yn;
		z = zn;
	}
	
	public float gX(){ return x; }
	public float gY(){ return y; }
	public float gZ(){ return z; }
	
	public void sX( float xIn ){ x = xIn; isDirty = true; }
	public void sY( float yIn ){ y = yIn; isDirty = true; }
	public void sZ( float zIn ){ z = zIn; isDirty = true; }
	
	public void eq( float xIn, float yIn, float zIn ){
		x = xIn; y = yIn; z = zIn; isDirty = true;
	}
	
	public float length(){
		if( isDirty ){
			len = (float)Math.sqrt(lenSq());
			isDirty = false;
		}
		return len;
	}
	
	public float lenSq(){
		if( isDirty ){
			lenSq = x*x+y*y+z*z;
		}
		return lenSq;
	}
	
	public Vec3 add( Vec3 v ){
		Vec3 out = new Vec3( x + v.x, y + v.y, z + v.z );
		return out;
	}
	
	public void addEq( Vec3 v ){
		x += v.x;
		y += v.y;
		z += v.z;
	}
	
	public Vec3 sub( Vec3 v ){
		Vec3 out = new Vec3( x - v.x, y - v.y, z - v.z );
		return out;
	}
	
	public Vec3 mult( float f ){
		Vec3 out = new Vec3( x * f, y * f, z * f );
		return out;
	}
	
	public Vec3 mult( Vec3 v ){
		Vec3 out = new Vec3( x * v.x, y * v.y, z * v.z );
		return out;
	}
	
	public Vec3 div( float f ){
		Vec3 out = new Vec3( x / f, y / f, z / f );
		return out;
	}
	
	public Vec3 div( Vec3 v ){
		Vec3 out = new Vec3( x / v.x, y / v.y, z / v.z );
		return out;
	}
	
	public String toS(){
		return Float.toString(x) + ", " + Float.toString(y) + ", " + Float.toString(z); 
	}
	
	public Vec3 norm(){
		if( this.length() > 0 ){
			return this.div( this.length() );
		} else {
			return new Vec3( 0.0f, 0.0f, 0.0f );
		}
	}
	
	public float dot( Vec3 b ){
		return x*b.x + y*b.y + z*b.z;
	}
	
	public Vec3 cross( Vec3 b ){
		Vec3 out = new Vec3();
		out.x = y*b.z - z*b.y;
		out.y = z*b.x - x*b.z;
		out.z = x*b.y - y*b.x;
		return out;
	}
}
