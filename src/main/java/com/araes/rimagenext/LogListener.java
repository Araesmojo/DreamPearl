package com.araes.rimagenext;
import java.util.EventListener;

public interface LogListener extends EventListener
{
	void onLogEvent( LogEvent e );
}
