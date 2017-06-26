package com.araes.rimagenext;

public class Tile extends TexturedSprite
{

	public int mColor;
	public int[] mEdgeColor;

	/**
	 * Constructors
	 */	
	public Tile( MainActivity context, GameRenderer renderer ){
		super( context, renderer );
		mEdgeColor = new int[4];
	}

	public Tile(final MainActivity context, GameRenderer renderer, boolean buildNow )
	{	
		super( context, renderer );
		mEdgeColor = new int[4];
		if( buildNow ){
			BuildSprite();
		}
	}
	
	public Tile( Tile tile )
	{	
		super( tile );
		mEdgeColor = new int[4];
		setUV( tile.SpriteTexCoorData );
		setColor( tile.mColor );
		setEdgeColors( tile.mEdgeColor );
	}
	
	/**
	 * Methods
	 */
	public void setUV(float[] tileUV)
	{
		SpriteTexCoorData = tileUV;
		mTextureCoordinates = InitializeBuffer( mTextureCoordinates, SpriteTexCoorData );
	}

	public void setColor(int color)
	{
		mColor = color;
	}

	public void setEdgeColors(int[] colorEdge)
	{
		for( int i = 0; i < 4; i++ ){
			mEdgeColor[i] = colorEdge[i];
		}
	}
}
