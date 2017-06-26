package com.araes.rimagenext;
import android.graphics.Bitmap;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import android.graphics.Color;

public class DisplayLayerMap extends DisplayLayer
{
	public Map<Integer,Tile> Atlas = new HashMap<Integer,Tile>();
	public Map<Integer,List<Tile>>[] Connects = new HashMap[4];
	int DSqCrit = 16;
	
	// few special tiles for reference
	Tile Tile00;
	Tile TileMaxMax;
	
	
	public DisplayLayerMap( MainActivity context, GameRenderer renderer ){
		super( context, renderer, "Map", "Map" );
		for( int i = 0; i < 4; i++ ){
			Connects[i] = new HashMap<Integer,List<Tile>>();
		}
	}

	@Override
	protected TexturedSprite[] PlaceSprites()
	{
		Logger.post( "------Starting PlaceSprites in DisplayLayerMap" );
		
		BuildTileAtlas();
		// Start in corner
		// place edging sprite
		// for touching edges:
		//  place sprites that match edge conditions
		//   edge of map is a condition
		//  if multi choices, random draw
		//  in beginning, don't even care about map edge
		
		return super.PlaceSprites();
	}
	
	protected void BuildTileAtlas(){
		Logger.post( "----Starting build tile atlas" );
		// Load the image resource
		int bmpResID = getResId( "tileset_biomes", R.drawable.class );
		Bitmap tileset = TextureHelper.LoadBitmapFromResource( mActivityContext, bmpResID );
		Logger.post( "--Finished loading tileset bitmap" );
		Logger.post( "tileset resID is " + bmpResID + " and tileset is " + tileset );
		Logger.post( "tileset has w,h " + Integer.toString(tileset.getWidth()) + ", " + Integer.toString(tileset.getHeight()) );
		
		// 7 x 2 biomes
		// red (3,0) and sky(5,1) not usable
		int secW = 7;
		int secH = 2;
		
		// Tiles are 16x16
		// Each subsection 8 x 20 tiles
		int TileWH = 16;
		int subW = 8;
		int subH = 20;
		
		int imgW = tileset.getWidth() / TileWH;
		int imgH = tileset.getHeight() / TileWH;
		
		Logger.post( "--Finding univ tile connections" );
		// Find the universal tile connectors
		int[][] UnivIndices = new int[][]{
			{ 5, 16 }, { 8, 16 }, { 15, 9 }, { 23, 8 },
			{ 25, 8 }, { 46, 8 }, { 50, 9 }, { 0, 27 },
			{ 14, 29 }, { 18, 30 }, { 24, 36 }, { 40, 36 },
			{ 48, 37 }
		};

		int[] UnivColor = new int[ UnivIndices.length ];
		for( int m = 0; m < UnivIndices.length; m++ ){
			int i = UnivIndices[m][0];
			int j = UnivIndices[m][1];
			Tile tTile = BuildTile( tileset, i, j, TileWH, imgW, imgH );
			UnivColor[ m ] = tTile.mColor;
		}
		
		Logger.post( "--Building atlas lookup for each tile" );
		// Build a tile atlas lookup for each
		//  tile on the tileset
		
		int secJ = 0;
		for( int bj = 0; bj < secH; bj++ ){
			int secI = 0;
			for( int bi = 0; bi < secW; bi++ ){
				// check if its the sections we can't use
				if( bi == 3 && bj == 0 ){
					Logger.post(bi + ", " + bj + " WARNING: Tiles unusable in this section" );
				} else if( bi == 5 && bj == 1 ){
					Logger.post(bi + ", " + bj + " WARNING: Tiles unusable in this section" );
				} else {
					// If its a good section, scan all the tiles into the atlas
					for( int j = secJ; j < secJ + subH; j++ ){
						for( int i = secI; i < secI + subW; i++ ){
							Tile tTile = BuildTile( tileset, i, j, TileWH, imgW, imgH );
							CheckUnivColorEdges( tTile, UnivColor );
							Logger.post(bi + ", " + bj + " Tile " + i + ", " + j + " color is " + tTile.mColor + " edges are " + tTile.mEdgeColor[0] + ", " + tTile.mEdgeColor[1] + ", " + tTile.mEdgeColor[2] + ", " + tTile.mEdgeColor[3] );
							Atlas.put( tTile.mColor, tTile );
							if( i == 0 && j == 0 ){
								Logger.post( "Setting Tile00" );
								Tile00 = new Tile( tTile );
							}
							if( i == (imgW - 1) && j == (imgH - 1) ){
								Logger.post( "Setting TileMaxMax" );
								TileMaxMax = new Tile( tTile );
							}
						}
					}			
				}
				secI += subW;
			}
			secJ += subH;
		}
		
		// Search through the available tiles
		// Note all edge colors available
		//  and on what sides
		// Note membership of edges in color
		//  groups 
		//  - Build hash entry for each
		//    color/edge combo
		//  - Some should exist from the
		//    universal matching set
		//  - Check for color nearness
		//    and assign very close
		//    matches to the same color
		// Solution for keys
		// https://stackoverflow.com/questions/16203880/get-array-of-maps-keys
		Logger.post( "--Building edge color groups" );
		Set<Integer> keySet = Atlas.keySet();
		Logger.post("Atlas key set length " + keySet.size() );
		int index = 0;
		for(Integer key : keySet){
			//int color = key.intValue();
			//Logger.post( "Tile color is " + color + " for key " + key );
			Tile tTile = Atlas.get(key);
			for( int i = 0; i < 4; i++ ){
				// Get the edge color
				// use as lookup into edge[i]<edgeColor>List<Tile>
				int edgeColor = tTile.mEdgeColor[i];
				int searchColor = SearchColorsByNearness( Connects[i].keySet(), edgeColor );
				if( searchColor != -1 ){
					//Logger.post("Changing edgeColor");
					edgeColor = searchColor;
					tTile.mEdgeColor[i] = searchColor;
				}
			
				// if no list, start
				if( Connects[i].get(edgeColor) == null ){
					Connects[i].put(edgeColor, new ArrayList<Tile>() );
				}
				
				//Logger.post( "Adding tile " + tTile.mColor + " w edge color " + edgeColor + " to Connects[" + i +"]" );
				
				// Add the tile to the correct list
				Connects[i].get(edgeColor).add( tTile );
			}
		}
	}

	private void CheckUnivColorEdges(Tile tTile, int[] univColor)
	{
		for( int i = 0; i < 4; i++ ){
			for( int j = 0; j < univColor.length; j++ ){
				// If they exactly equal, make the edge
				// the main universal match color
				if( tTile.mEdgeColor[i] == univColor[j] ){
					tTile.mEdgeColor[i] = univColor[0];
				} else {
					// If not exact match, check if the
					// colors are near enough for a match
					if( RGBADistSq( tTile.mEdgeColor[i], univColor[j] ) < DSqCrit ){
						tTile.mEdgeColor[i] = univColor[0];
					}
				}
			}
		}
	}
	
	private Tile BuildTile( Bitmap tileset, int i, int j, int TileWH, int imgW, int imgH ){
		//Logger.post( "--Starting BuildTile");
		//  - Find 1x1 color
		Bitmap tile = Bitmap.createBitmap( tileset, i, j, TileWH, TileWH );
		int color = Bitmap.createScaledBitmap( tile, 1, 1, true ).getPixel(0,0);
		//Logger.post( "tile is " + tile + " and color is " + color );
		
		//  - Find edge colors
		//  R, U, L, D
		Bitmap[] edges = new Bitmap[4];
		edges[0] = Bitmap.createBitmap( tile, TileWH - 1, 0, 1, TileWH );
		edges[1] = Bitmap.createBitmap( tile, 0, TileWH-1, TileWH, 1 );
		edges[2] = Bitmap.createBitmap( tile, 0, 0, 1, TileWH );
		edges[3] = Bitmap.createBitmap( tile, 0, 0, TileWH, 1 );

		int[] colorEdge = new int[4];
		colorEdge[0] = Bitmap.createScaledBitmap( edges[0], 1, 1, true ).getPixel(0,0);
		colorEdge[1] = Bitmap.createScaledBitmap( edges[1], 1, 1, true ).getPixel(0,0);
		colorEdge[2] = Bitmap.createScaledBitmap( edges[2], 1, 1, true ).getPixel(0,0);
		colorEdge[3] = Bitmap.createScaledBitmap( edges[3], 1, 1, true ).getPixel(0,0);
		//Logger.post( "Edge colors are " + colorEdge[0] + ", " + colorEdge[1] + ", " + colorEdge[2] + ", " + colorEdge[3] );
		
		//  - Find UV coords
		float[] TileUV = new float[12];
		TileUV = ChangeUVToIJofWH( TileUV, i, j, imgW, imgH );

		// Save to tile atlas as new Tile object
		Tile tTile = new Tile( mActivityContext, mRenderer );
		tTile.setTexture( mTextureDataHandle );
		tTile.setEdgeColors( colorEdge );
		tTile.setColor( color );
		tTile.setUV( TileUV );
		
		return tTile;
	}

	private int SearchColorsByNearness(Set<Integer> keySet, int edgeColor)
	{
		for( int color : keySet ){
			int DistSq = RGBADistSq( edgeColor, color );
			//Logger.post("For " + edgeColor + " and " + color + " DistSq is " + DistSq );
			if( DistSq < DSqCrit ){
				return color;
			}
		}
		//Logger.post("Returning -1" );
		return -1;
	}

	private int RGBADistSq(int colorA, int colorB)
	{
		int[] rgbA = ColorToRGB( colorA );
		int[] rgbB = ColorToRGB( colorB );
		//int rA = Color.red(colorA);
		//int gA = Color.green(colorA);
		//int bA = Color.blue(colorA);
		//int rB = Color.red(colorB);
		//int gB = Color.green(colorB);
		//int bB = Color.blue(colorB);
		int rDelta = rgbA[0] - rgbB[0];
		int gDelta = rgbA[1] - rgbB[1];
		int bDelta = rgbA[2] - rgbB[2];
		return rDelta*rDelta + gDelta*gDelta + bDelta*bDelta;
	}

	private int[] ColorToRGB(int color)
	{
		int r = Color.red(color);
		int g = Color.green(color);
		int b = Color.blue(color);
		return new int[]{ r, g, b };
	}

	@Override
	protected Tile ChooseTile(Tile[][] arrOut, int i, int j, int arrW, int arrH)
	{
		Logger.post( "--Choosing tile with i,j " + i + ", " + j + " and arr w,h " + arrW + ", " + arrH );
		Tile[] tTiles;
		
		float[] redWarn = new float[]
		{				
			// Front face
			1.0f, 0.5f, 0.5f, 1.0f,				
			1.0f, 0.5f, 0.5f, 1.0f,				
			1.0f, 0.5f, 0.5f, 1.0f,				
			1.0f, 0.5f, 0.5f, 1.0f,				
			1.0f, 0.5f, 0.5f, 1.0f,				
			1.0f, 0.5f, 0.5f, 1.0f				
		};
		
		float[] blueWarn = new float[]
		{				
			// Front face
			0.5f, 0.5f, 1.0f, 1.0f,				
			0.5f, 0.5f, 1.0f, 1.0f,
			0.5f, 0.5f, 1.0f, 1.0f,
			0.5f, 0.5f, 1.0f, 1.0f,				
			0.5f, 0.5f, 1.0f, 1.0f,
			0.5f, 0.5f, 1.0f, 1.0f
		};
		// Check for edge constraints left
		  // Filter available tile choices by color
		// Check for edge constraints up
		  // Further filter
		// If multiple remain, filter by current biome
		// If multiple remain, random
		// NOTE: Opengl renders Y axis reverse of most programs (Y goes up)
		int colorL, colorD;
		if( i == 0 ){
			colorL = 0;
			if( j == 0 ){
				colorD = 0;
				// Choose a random tile to return
				int ALen = Atlas.values().size();
				tTiles = new Tile[ALen];
				tTiles = Atlas.values().toArray(tTiles);
			} else {
				// Find the edge color on the tile below[3] us (j-1) pointing up[1]
				// This is the color on our lower side we have to match
				colorD = arrOut[i][j-1].mEdgeColor[1];
				//int[] rgb = ColorToRGB(colorD);
				//for( int key: Connects[3].keySet() ){
				//	rgb = ColorToRGB(key);
				//	Logger.post( "color: " + key + " and rgb " + rgb[0] + ", " + rgb[1] + ", " + rgb[2] );
				//}
				if( Connects[3].get( colorD ) != null ){
					int ConnectLen = Connects[3].get( colorD ).size();
					Logger.post( "size of edge connection array is " + ConnectLen );
					tTiles = new Tile[ ConnectLen ];
					tTiles = Connects[3].get( colorD ).toArray(tTiles);
				} else {
					Logger.post( "--------No available color match" );
					Set keys = Connects[3].keySet();
					Logger.post( "Connects[3] key set size is " + keys.size() );
					Logger.post( "Connects[3]<> avail colors are:" );
					
					tTiles = new Tile[1];
					tTiles = Atlas.values().toArray(tTiles);
					arrOut[i][j-1].setSpriteColor( redWarn );
				}
				
			}
		} else {
			// Find the edge color on the tile left[2] of us (i-1) pointing right[0]
			// This is the color on our left side we have to match
			colorL = arrOut[i-1][j].mEdgeColor[0];
			if( Connects[2].get( colorL ) != null ){
				tTiles = new Tile[ Connects[2].get( colorL ).size() ];
				tTiles = Connects[2].get( colorL ).toArray(tTiles);
			} else {
				Logger.post( "--------No available color match" );
				Set keys = Connects[2].keySet();
				Logger.post( "Connects[2] key set size is " + keys.size() );
				Logger.post( "Connects[2]<> avail colors are:" );
				
				tTiles = new Tile[1];
				tTiles = Atlas.values().toArray(tTiles);
				arrOut[i-1][j].setSpriteColor( blueWarn );
			}
			// if j > 0 filter this list further by down matches
			if( j != 0 ){
				colorD = arrOut[i][j-1].mEdgeColor[1];
				List<Tile> TList = new ArrayList<Tile>();
				for( int m = 0; m < tTiles.length; m++ ){
					if( tTiles[m].mEdgeColor[3] == colorD ){
						TList.add(tTiles[m]);
					}
				}
				if( TList.size() > 0 ){
					tTiles = new Tile[TList.size()];
					tTiles = TList.toArray(tTiles);
				}
			}
		}
		
		int outInd = (int)(Math.random()*(tTiles.length-1));
		Tile outTile = new Tile( tTiles[ outInd ] );
		
		float fW = (float)arrW;
		float fH = (float)arrH; 
		
		// Quick experiment to see which way OpenGL counts x,y coord
		/*float[] ColorData = new float[]
		{				
			// Front face
			i/fW, j/fH, 1.0f, 1.0f,				
			i/fW, j/fH, 1.0f, 1.0f,
			i/fW, j/fH, 1.0f, 1.0f,
			i/fW, j/fH, 1.0f, 1.0f,				
			i/fW, j/fH, 1.0f, 1.0f,
			i/fW, j/fH, 1.0f, 1.0f,
		};
		outTile.setSpriteColor( ColorData );*/
		
		return outTile;
		//Logger.post( "TileMaxMax is " + Tile00 );
		//return TileMaxMax;
	}
}
