package com.araes.rimagenext;

public class BBoxAction
{
	public boolean isMove = false;
	public String mName = "";
	public Vec3 Move;
	
	public BBoxAction( String name, boolean isMove_in ){
		mName = name;
		isMove = isMove_in;
		Move = null;
	}
	
	public BBoxAction( String name, boolean isMove_in, Vec3 Move_in ){
		mName = name;
		isMove = isMove_in;
		Move = Move_in;
	}
}
