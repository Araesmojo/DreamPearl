package com.araes.rimagenext;
import java.util.*;

public class WorldEdge
{
	public WorldTri parent = null;
	public WorldTri side   = null;
	public WorldEdge twin  = null;
	public WorldEdge Up    = null;
	public WorldVert midpt = null;
	public CFDBnd BC       = null;
	public Vec3 vec        = null;
	public Vec2 vec2D      = null;
	public int ind;
	
	public WorldEdge(){
	}
	
	public WorldEdge( int i, WorldTri par ){
		ind = i;
		parent = par;
	}
	
	public WorldEdge( int i, WorldTri par, WorldTri sideIn ){
		ind = i;
		parent = par;
		side = sideIn;
	}

	public void actBC( float ts )
	{
		if( BC.mType == CFDBnd.IN ){
			// mArgs]0] = rebound coef
			// mArgs[1] = flowPerStep
			// mArgs[2] = prt mass
			// mArgs[3] = prt density
			while( BC.flowThisStep < BC.mArgs[1] ){
				Particle prt = new Particle( parent, BC.mArgs[2], BC.mArgs[3] );
				prt.pos2D = new Vec2( (float)Math.random(), (float)Math.random() ).mult( 0.25f * vec2D.length() );
				parent.particles.add( prt );
				BC.flowThisStep += prt.mass;
				//parent.Logger.post( "Added particle w mass " + prt.mass + " and pos " + prt.pos2D.toS() + " vec2d len " + vec2D.toS() );
			}
		} else if( BC.mType == CFDBnd.INRESTRICT ){
			// mArgs[0] = flowPerPres for this BC
			// mArgs[1] = prt mass
			// mArgs[2] = prt density
			// flowTarget = plate area (m2)* velMag (m/s) * %land goal * target_land_density (kg/m3) = kg/s flow rate
			// Use current pressure
			// P=0 flow=0, P0=target, flow=target, more=more
			// can be second order or log to be more stable
			// BC.mArgs[0] = flowPerPres for this BC
			float outFlowTarget = parent.P * BC.mArgs[0];
			while( BC.flowThisStep < outFlowTarget ){
				Particle prt = new Particle( parent, BC.mArgs[1], BC.mArgs[2] );
				prt.pos2D = (parent.corner2D.get(ind).add( vec2D.mult( (float)Math.random() ) )).mult(0.5f);
				parent.particles.add( prt );
				BC.flowThisStep += prt.mass;
			}
		}
	}

	public void actBC( float ts, Particle prt, float paraLen, float perpLen )
	{
		if( BC.mType == CFDBnd.OUT ){
			/*parent.Logger.post( "triggered out, prt " + prt );
			parent.Logger.post( "triggered out, prt.parent " + prt.mass );
			parent.Logger.post( "triggered out, prt.parent " + prt.parent );
			parent.Logger.post( "triggered out, prt.parent.particles " + prt.parent.particles );*/
			// remove the particle from the sim
			BC.flowThisStep += prt.mass;
			prt.parent = null;
			prt.nearest = null;
			//parent.Logger.post( "done w delete" );
		} else {
			if( BC.mType == CFDBnd.OUTRESTRICT ){
				// Use args[0] inflow/outflow rate
				// flowTarget = plate area (m2)* velMag (m/s) * %land goal * target_land_density (kg/m3) = kg/s flow rate
				// Use current pressure
				// P=0 flow=0, P0=target, flow=target, more=more
				// can be second order or log to be more stable
				// BC.mArgs[0] = flowPerPres for this BC
				float outFlowTarget = parent.P * BC.mArgs[0];
				if( BC.flowThisStep < outFlowTarget * ts ){
					BC.flowThisStep += prt.mass;
					prt.delete();
				} else {
					// Act like a low coeffecient of restitution (0.2) bounce edge
					ReboundParticle( prt, paraLen, perpLen, 0.2f );
				}
			} else if( BC.mType == CFDBnd.SIDE || BC.mType == CFDBnd.IN ){
				ReboundParticle( prt, paraLen, perpLen, BC.mArgs[0] );
			}
		}
	}

	private void ReboundParticle(Particle prt, float paraLen, float perpLen, float CR )
	{
		// calculate intersection w edge
		Vec2 C0 = parent.corner2D.get(ind);
		Vec2 P0 = prt.pos2Dpr;
		Vec2 C1 = C0.add( vec2D );
		Vec2 P1 = prt.pos2D;
		
		float CdelX = ( C0.gX()-C1.gX() );
		float CdelY = ( C0.gY()-C1.gY() );
		float PdelX = ( P0.gX()-P1.gX() );
		float PdelY = ( P0.gY()-P1.gY() );
		
		float Div = CdelX*PdelY - CdelY*PdelX;
		float INumPos = ( C0.gX()*C1.gY() - C0.gY()*C1.gX() );
		float INumNeg = ( P0.gX()*P1.gY() - P0.gY()*P1.gX() );
		float Ix = ( INumPos*PdelX - INumNeg*CdelX ) / Div;
		float Iy = ( INumPos*PdelY - INumNeg*CdelY ) / Div;
		Vec2 I = new Vec2( Ix, Iy );
		//parent.Logger.post( "P0 " + P0.toS() + ", C0 " + C0.toS() + ", P1 " + P1.toS() + ", C1 " + C1.toS() + ", I " + I.toS() );
		
		// get vectoe from intersect to projected point
		Vec2 prtVec = prt.pos2D.sub( new Vec2( Ix, Iy ) );
		Vec2 ePerp = new Vec2( -vec2D.gX(), vec2D.gY() );
		float prtPara = prtVec.dot( vec2D );
		float prtPerp = prtVec.dot( ePerp );
		
		prt.triggeredSideBC= true;
		
		prt.pos2D = I.add( new Vec2( prtPara, -prtPerp ) );
	}

	public void prtToSide(Particle prt, float paraLen, float perpLen )
	{
		// method for when no BC (equiv to open boundary)
		// place intersection on twin
		int twinInd = twin.ind;
		Vec2 twinCor = twin.parent.corner2D.get(twinInd);
		Vec2 twinVecNorm = twin.vec2D.norm();
		
		// take how far para up the edge we are and flip to mirror vector space
		Vec2 prtParaPos = twinCor.add( twinVecNorm.mult( twin.vec2D.length() - paraLen ) );
		
		// place particle perpendicular inward by perpLen
		// inward pointing vector (rotate edge norm 90)
		Vec2 inVec = new Vec2( twinVecNorm.gY(), -twinVecNorm.gX() );
		prt.pos2D = prtParaPos.add( inVec.mult( perpLen ) );
		
		// update particle ownership
		prt.parent = side;
		side.particles.add( prt );
		parent.particles.remove( prt );
	}
}
