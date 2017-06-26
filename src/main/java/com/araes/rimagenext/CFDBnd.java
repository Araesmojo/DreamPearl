package com.araes.rimagenext;

public class CFDBnd
{
	static public int IN = 1;
	static public int INRESTRICT = 2;
	static public int OUT = 3;
	static public int OUTRESTRICT = 4;
	static public int SIDE = 5;
	
	public int mType = 0;
	public float[] mArgs = null;

	public float flowThisStep = 0;

	
	
	public CFDBnd( int type, float[] args ){
		mType = type;
		mArgs = args;
	}
}
