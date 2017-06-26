package com.araes.rimagenext;
import android.graphics.Rect;
import android.view.MotionEvent;

// A simple class to hold a bounding box
public class BBox {
	GameRenderer mRenderer;
	String mName;
	Rect mBox;
	BBoxAction mAct;

	public BBox( GameRenderer renderer, String name ){
		mRenderer = renderer;
		mName = name;
	}

	public BBox( GameRenderer renderer, String name, BBoxAction Act ){
		mRenderer = renderer;
		mName = name;
		mAct  = Act;
	}

	public BBox( GameRenderer renderer, String name, int right, int top, int left, int bottom, BBoxAction Act ){
		mRenderer = renderer;
		mName = name;
		setRect( right, top, left, bottom );
		mAct = Act;
	}

	public boolean checkHit( MotionEvent e ){
		if( mBox.left < e.getX() && e.getX() < mBox.right ){
			// annoying backward coords make this logic reversed
			if( mBox.top < e.getY() && e.getY() < mBox.bottom ){
				onTouch();
				return true;
			}
		}
		return false;
	}

	public void onTouch(){
		mRenderer.getMap().processButtonInput(mAct);
	}

	public void setRect( int right, int top, int left, int bottom ){
		mBox = new Rect( right, top, left, bottom );
	}

	public void setAct( BBoxAction Act ){
		mAct = Act;
	}
}
