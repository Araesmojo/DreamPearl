package com.araes.rimagenext;

public class TexUV
{
	public float xmin = 0.0f;
	public float xmax = 0.0f;
	public float ymin = 0.0f;
	public float ymax = 0.0f;
	
	public TexUV(){
		// do nothing
	}
	
	public TexUV( float xminin, float xmaxin, float yminin, float ymaxin ){
		xmin = xminin;
		xmax = xmaxin;
		ymin = yminin;
		ymax = ymaxin;
	}
}
