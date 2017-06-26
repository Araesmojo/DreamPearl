package com.araes.rimagenext;
import java.util.Vector;
import java.util.Enumeration;
import java.io.Serializable;
import java.net.*;

public class LogPoster implements Serializable
{
	private transient Vector listeners;

	/** Provide a simple GUI that triggers our SunEvents
	 */
	public LogPoster() {
		
	}

	/** Register a listener for SunEvents */
	synchronized public void addLogListener(LogListener l) {
		if (listeners == null)
			listeners = new Vector();
		listeners.addElement(l);
	}  

	/** Remove a listener for SunEvents */
	synchronized public void removeLogListener(LogListener l) {
		if (listeners == null)
			listeners = new Vector();
		listeners.removeElement(l);
	}

	/** Fire a LogEvent to all registered listeners */
	protected void createLogEvent(String data) {
		// if we have no listeners, do nothing...
		if (listeners != null && !listeners.isEmpty()) {
			// create the event object to send
			LogEvent event = 
				new LogEvent( this, data );

			// make a copy of the listener list in case
			//   anyone adds/removes listeners
			Vector targets;
			synchronized (this) {
				targets = (Vector) listeners.clone();
			}

			// walk through the listener list and
			//   call the sunMoved method in each
			Enumeration e = targets.elements();
			while (e.hasMoreElements()) {
				LogListener l = (LogListener) e.nextElement();
				l.onLogEvent(event);
			}
		}
	}
	
	protected void post( String data ){
		createLogEvent( data + "\n" );
	}
}
