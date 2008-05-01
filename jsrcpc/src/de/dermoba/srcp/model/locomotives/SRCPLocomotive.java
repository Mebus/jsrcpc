/*------------------------------------------------------------------------
 * 
 * copyright : (C) 2008 by Benjamin Mueller 
 * email     : news@fork.ch
 * website   : http://sourceforge.net/projects/adhocrailway
 * version   : $Id: SRCPLocomotive.java,v 1.1 2008-04-24 06:19:06 fork_ch Exp $
 * 
 *----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 *----------------------------------------------------------------------*/

package de.dermoba.srcp.model.locomotives;

import de.dermoba.srcp.client.SRCPSession;
import de.dermoba.srcp.devices.GL;

public abstract class SRCPLocomotive {

	public static final String			FORWARD_DIRECTION	= "1";

	public static final String			REVERSE_DIRECTION	= "0";

	protected boolean					initialized			= false;
	
	protected SRCPLocomotiveDirection	direction			= SRCPLocomotiveDirection.UNDEF;

	protected int						currentSpeed		= 0;

	private GL							gl;

	private SRCPSession					session;

	protected boolean[]					functions;

	protected String[]					params;

	protected String					protocol;

	protected int						drivingSteps;

	protected int						bus;

	protected int						address;

	public SRCPLocomotive() {
		this(0,0);
	}
	
	public SRCPLocomotive(int bus, int address) {
		super();
		this.bus = bus;
		this.address = address;
	}

	public abstract boolean checkAddress();

	public boolean checkBus() {
		return bus > 0;
	}

	public boolean checkBusAddress() {
		return (checkBus() && checkAddress());
	}

	public int getBus() {
		return bus;
	}

	public void setBus(int bus) {
		this.bus = bus;
	}

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}

	public int getCurrentSpeed() {
		return currentSpeed;
	}

	protected void setCurrentSpeed(int currentSpeed) {
		this.currentSpeed = currentSpeed;
	}

	public SRCPLocomotiveDirection getDirection() {
		return direction;
	}

	protected void setDirection(SRCPLocomotiveDirection direction) {
		this.direction = direction;
	}

	public boolean[] getFunctions() {
		return functions;
	}

	public void setFunctions(boolean[] functions) {
		this.functions = functions;
	}

	public GL getGL() {
		return this.gl;
	}

	protected void setGL(GL gl) {
		this.gl = gl;
	}

	public SRCPSession getSession() {
		return this.session;
	}

	protected void setSession(SRCPSession session) {
		this.session = session;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean init) {
		initialized = init;
	}

	public String[] getParams() {
		return params;
	}

	public String getProtocol() {
		return protocol;
	}

	public int getDrivingSteps() {
		return drivingSteps;
	}
}