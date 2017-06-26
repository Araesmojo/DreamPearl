package com.araes.rimagenext;
import java.util.*;

public class GameWorld extends DisplayLayer
{
	public Map<String, DisplayLayer> layerByName = new HashMap<String, DisplayLayer>();
	public String[] layerData = new String[]{
		"Ground",     "-0.5", "-0.5", "-0.5", "Map",
		"Grass",      "0", "0", "0", "Simple",
		"Character",  "0.5", "0.5", "0.5", "Character",
		"Collidable", "1", "1", "1", "Collidable",
		"Mobile",     "1.5", "1.5", "1.5", "Mobile",
		"Aerial",     "1.5", "1.5", "1.5", "Mobile",
		"Sky",        "2", "2", "2", "Sky"
	};
	public int layerDataSride = 4;
	
	List<String> layerNames;
	
	GameWorld( MainActivity activityContext, GameRenderer renderer ){
		super( activityContext, renderer, "world", "world" );
		Logger.post( "Finished GameWorld creation" );
	}
	
	public void BuildDisplayLayers(){
			Logger.post( "Starting build display layers" );
			int i = 0;
			int layerNum = 0;
			String layerName;
			String layerType;
			Logger.post( "layer data length " + Integer.toString(layerData.length) );
			while( i < layerData.length ){
				Logger.post( "Builing layer " + Integer.toString(layerNum) + " at index i " + Integer.toString(i) );
				layerName = layerData[i++];
				float x = Float.parseFloat(layerData[i++]);
				float y = Float.parseFloat(layerData[i++]);
				float z = Float.parseFloat(layerData[i++]);
				layerType = layerData[i++];
				Logger.post( "Building layer " + layerName + " num " + Integer.toString(layerNum) + " with pos " + Float.toString(x) + ", " + Float.toString(y) + ", " + Float.toString(z) );
				
				DisplayLayer layerNew;
				if( layerType == "Map" ){
					layerNew  = new DisplayLayerSphereMap( mActivityContext, mRenderer );
				} else {
					layerNew  = new DisplayLayer( mActivityContext, mRenderer, layerName, layerType );
				}
				layerByName.put( layerName, layerNew );
				layerNew.MoveTo( new Vec3( x, y, z ) );
				layerNew.boolDirty = false;
				layerNew.mVisible  = false;
				layerNum++;
			}
			layerByName.get( "Ground" ).boolDirty = true;
			layerByName.get( "Grass" ).boolDirty = true;
			layerByName.get( "Character" ).boolDirty = true;
			updateLayerNames();
			
			Logger.post("Finished build display layers");
			boolDirty = false;
	}
	
	public void updateLayerNames() {
		layerNames = new ArrayList<String>(layerByName.keySet());
	}

	@Override
	public void draw()
	{
		// DEBUG, turning off grass layer
		layerByName.get( "Grass" ).mVisible = false;
		layerByName.get( "Ground" ).mVisible = true;
		layerByName.get( "Character" ).mVisible = false;
		for( int i = 0; i < layerNames.size(); i++ ){
			DisplayLayer layer = layerByName.get(layerNames.get(i));
			if( layer.mVisible ){
				//Logger.post( "Drawing layer " + layer.mName + " with pos " + layer.mPos.toS() + " and model " + GameUtils.MatToS(layer.mModelMatrix) );
				layer.draw();
			}
		}
	}

	@Override

	public void processMoves(){
		//Logger.post( "Starting mobile array move" );
		if( !stackMove.isEmpty() ){
			Logger.post( "Found non-empty move stack-process last move" );
			Vec3 Delta = stackMove.pop().Move;
			stackMove.clear();
			Logger.post( "Found move " + Float.toString( Delta.gX() ) + ", " + Float.toString( Delta.gY() ) + ", " + Float.toString( Delta.gZ() ) );
			mPos.sX( mPos.gX() + Delta.gX() );
			mPos.sY( mPos.gY() + Delta.gY() );
			mPos.sZ( mPos.gZ() + Delta.gZ() );
			
			for( int i = 0; i < layerNames.size(); i++ ){
				Vec3 posNew = layerByName.get(layerNames.get(i)).mPos.add(Delta);
				layerByName.get(layerNames.get(i)).MoveTo( posNew );
			}
		}
		//Logger.post( "Finished move" );
	}

	@Override
	public void update()
	{
		if( boolDirty ){
			BuildDisplayLayers();
		}
		
		for( int i = 0; i < layerNames.size(); i++ ){
			layerByName.get(layerNames.get(i)).update();
		}
	}
}
