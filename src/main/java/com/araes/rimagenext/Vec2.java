package com.araes.rimagenext;

public class Vec2
{
	private float x = 0.0f;
	private float y = 0.0f;
	
	float len = 0.0f;
	float lenSq = 0.0f;
	
	boolean isDirty = true;
	
	public Vec2(){
		x = 0.0f;
		y = 0.0f;
	}
	
	public Vec2( float xIn, float yIn ){
		x = xIn;
		y = yIn;
	}
	
	public float gX(){ return x; }
	public float gY(){ return y; }

	public void sX( float xIn ){ x = xIn; isDirty = true; }
	public void sY( float yIn ){ y = yIn; isDirty = true; }

	public void eq(float xIn, float yIn )
	{
		x = xIn; y = yIn; isDirty = true;
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
			lenSq = x*x+y*y;
		}
		return lenSq;
	}

	public Vec2 add( Vec2 v ){
		Vec2 out = new Vec2( x + v.x, y + v.y );
		return out;
	}
	
	public void addEq( Vec2 v ){
		x += v.x;
		y += v.y;
	}

	public Vec2 sub( Vec2 v ){
		Vec2 out = new Vec2( x - v.x, y - v.y );
		return out;
	}
	
	public void subEq( Vec2 v ){
		x -= v.x;
		y -= v.y;
	}

	public Vec2 mult( float f ){
		Vec2 out = new Vec2( x * f, y * f );
		return out;
	}

	public Vec2 mult( Vec2 v ){
		Vec2 out = new Vec2( x * v.x, y * v.y );
		return out;
	}
	
	public void multEq( float f ){
		x *= f;
		y *= f;
	}
	
	public void multEq( Vec2 v ){
		x *= v.x;
		y *= v.y;
	}

	public Vec2 div( float f ){
		Vec2 out = new Vec2( x / f, y / f );
		return out;
	}

	public Vec2 div( Vec2 v ){
		Vec2 out = new Vec2( x / v.x, y / v.y );
		return out;
	}
	
	public void divEq( float f ){
		x /= f;
		y /= f;
	}

	public void divEq( Vec2 v ){
		x /= v.x;
		y /= v.y;
	}

	public String toS(){
		return Float.toString(x) + ", " + Float.toString(y); 
	}

	public Vec2 norm(){
		if( this.length() > 0 ){
			return this.div( this.length() );
		} else {
			return new Vec2( 0.0f, 0.0f );
		}
	}

	public float dot( Vec2 v ){
		return x*v.x + y*v.y;
	}
	
	public float cross( Vec2 v ){
		return x*v.y - y*v.x;
	}
}
