package com.araes.rimagenext;

import java.util.EventObject;

/**
 * An event that represents the Sun rising or setting
 */
public class LogEvent extends EventObject {
	private String data;

	public LogEvent(Object source, String data ) {
		super(source);
		this.data = data;
	}
	
	public String getData(){ return data; }
}
