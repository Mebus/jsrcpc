/*------------------------------------------------------------------------
 * 
 * copyright : (C) 2008 by Benjamin Mueller 
 * email     : news@fork.ch
 * website   : http://sourceforge.net/projects/adhocrailway
 * version   : $Id: SRCPLocomotiveControl.java,v 1.9 2012-03-15 06:22:50 fork_ch Exp $
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.dermoba.srcp.client.SRCPSession;
import de.dermoba.srcp.common.exception.SRCPDeviceLockedException;
import de.dermoba.srcp.common.exception.SRCPException;
import de.dermoba.srcp.devices.listener.GLInfoListener;
import de.dermoba.srcp.model.Constants;
import de.dermoba.srcp.model.InvalidAddressException;
import de.dermoba.srcp.model.NoSessionException;
import de.dermoba.srcp.model.SRCPAddress;
import de.dermoba.srcp.model.SRCPModelException;
import de.dermoba.srcp.model.locking.SRCPLockChangeListener;
import de.dermoba.srcp.model.locking.SRCPLockControl;

/**
 * Controls all actions which can be performed on Locomotives.
 * 
 * @author fork
 * 
 */
public class SRCPLocomotiveControl implements GLInfoListener, Constants {
	private static Logger logger = Logger
			.getLogger(SRCPLocomotiveControl.class);

	private static SRCPLocomotiveControl instance;
	private final List<SRCPLocomotiveChangeListener> listeners;
	private final SRCPLockControl lockControl = SRCPLockControl.getInstance();

	private final List<SRCPLocomotive> srcpLocomotives;
	private final Map<SRCPAddress, SRCPLocomotive> addressLocomotiveCache;
	private SRCPSession session;

	@SuppressWarnings("rawtypes")
	private static final Map<Class, LocomotiveStrategy> locomotiveStrategies = new HashMap<Class, LocomotiveStrategy>();
	static {
		final DefaultLocomotiveStrategy defaultStrategy = new DefaultLocomotiveStrategy();
		final SimulatedMFXLocomotiveStrategy simulatedMFXStrategy = new SimulatedMFXLocomotiveStrategy();
		locomotiveStrategies.put(MMDigitalLocomotive.class, defaultStrategy);
		locomotiveStrategies.put(MMDeltaLocomotive.class, defaultStrategy);
		locomotiveStrategies.put(DoubleMMDigitalLocomotive.class,
				simulatedMFXStrategy);
	}

	private SRCPLocomotiveControl() {
		logger.info("SRCPLocomotiveControl loaded");
		listeners = new ArrayList<SRCPLocomotiveChangeListener>();
		srcpLocomotives = new ArrayList<SRCPLocomotive>();
		addressLocomotiveCache = new HashMap<SRCPAddress, SRCPLocomotive>();
	}

	public static SRCPLocomotiveControl getInstance() {
		if (instance == null) {
			instance = new SRCPLocomotiveControl();
		}
		return instance;
	}

	public void setSession(final SRCPSession session) {
		this.session = session;
		if (session != null) {
			session.getInfoChannel().addGLInfoListener(this);
		}
		for (final SRCPLocomotive l : srcpLocomotives) {
			l.setSession(session);
		}

	}

	public void toggleDirection(final SRCPLocomotive locomotive)
			throws SRCPLocomotiveException, SRCPModelException {
		checkLocomotive(locomotive);
		switch (locomotive.direction) {
		case REVERSE:
			locomotive.setDirection(SRCPLocomotiveDirection.FORWARD);
			break;
		case FORWARD:
		case UNDEF:
		default:
			locomotive.setDirection(SRCPLocomotiveDirection.REVERSE);
			break;

		}
		locomotive.setPreventDirectionToggle(true);
		setSpeed(locomotive, 0, locomotive.getFunctions());
		setSpeed(locomotive, 0, locomotive.getFunctions());
		informListeners(locomotive);
	}

	public SRCPLocomotiveDirection getDirection(final SRCPLocomotive locomotive) {
		if (locomotive == null) {
			return SRCPLocomotiveDirection.UNDEF;
		}
		return locomotive.getDirection();
	}

	public int getCurrentSpeed(final SRCPLocomotive locomotive) {
		if (locomotive == null) {
			return 0;
		}
		return locomotive.getCurrentSpeed();
	}

	public void setSpeed(final SRCPLocomotive locomotive, final int speed,
			final boolean[] functions) throws SRCPLocomotiveException,
			SRCPModelException {

		checkLocomotive(locomotive);
		try {
			final LocomotiveStrategy strategy = locomotiveStrategies
					.get(locomotive.getClass());
			strategy.setSpeed(locomotive, speed, functions);
			informListeners(locomotive);
		} catch (final SRCPDeviceLockedException x) {
			throw new SRCPLocomotiveLockedException(ERR_LOCKED);
		} catch (final SRCPException x) {
			throw new SRCPLocomotiveException(ERR_FAILED, x);
		}

	}

	public void increaseSpeed(final SRCPLocomotive locomotive)
			throws SRCPLocomotiveException, SRCPModelException {
		checkLocomotive(locomotive);
		final int newSpeed = locomotive.getCurrentSpeed() + 1;

		setSpeed(locomotive, newSpeed, locomotive.getFunctions());

	}

	public void decreaseSpeed(final SRCPLocomotive locomotive)
			throws SRCPLocomotiveException, SRCPModelException {
		checkLocomotive(locomotive);
		final int newSpeed = locomotive.getCurrentSpeed() - 1;

		setSpeed(locomotive, newSpeed, locomotive.getFunctions());
	}

	public void increaseSpeedStep(final SRCPLocomotive locomotive)
			throws SRCPLocomotiveException, SRCPModelException {
		checkLocomotive(locomotive);
		final int newSpeed = locomotive.getCurrentSpeed() + 1;

		setSpeed(locomotive, newSpeed, locomotive.getFunctions());
	}

	public void decreaseSpeedStep(final SRCPLocomotive locomotive)
			throws SRCPLocomotiveException, SRCPModelException {
		checkLocomotive(locomotive);
		final int newSpeed = locomotive.getCurrentSpeed() - 1;
		setSpeed(locomotive, newSpeed, locomotive.getFunctions());
	}

	public void setFunctions(final SRCPLocomotive locomotive,
			final boolean[] functions) throws SRCPLocomotiveException,
			SRCPModelException {
		checkLocomotive(locomotive);
		setSpeed(locomotive, locomotive.getCurrentSpeed(), functions);
	}

	public boolean[] getFunctions(final SRCPLocomotive locomotive) {
		if (locomotive == null) {
			return new boolean[0];
		}

		return locomotive.getFunctions();
	}

	public void emergencyStop(final SRCPLocomotive locomotive,
			final int emergencyStopFunction) throws SRCPLocomotiveException,
			SRCPModelException {
		checkLocomotive(locomotive);
		final LocomotiveStrategy locomotiveStrategy = locomotiveStrategies
				.get(locomotive.getClass());
		final boolean[] functions = locomotiveStrategy
				.getEmergencyStopFunctions(locomotive, emergencyStopFunction);

		setSpeed(locomotive, 0, functions);
	}

	public void GLinit(final double timestamp, final int bus,
			final int address, final String protocol, final String[] params) {
		logger.debug("GLinit( " + bus + " , " + address + " , " + protocol
				+ " , " + Arrays.toString(params) + " )");
		final SRCPLocomotive locomotive = addressLocomotiveCache
				.get(new SRCPAddress(bus, address));
		if (locomotive == null) {
			// ignore unknown locomotive
			return;
		}
		try {
			locomotive.setInitialized(true);
			checkLocomotive(locomotive);

			// informListeners(locomotive);
		} catch (final SRCPModelException e1) {
		}
	}

	public void GLset(final double timestamp, final int bus, final int address,
			final SRCPLocomotiveDirection drivemode, final int v,
			final int vMax, final boolean[] functions) {

		logger.debug("GLset( " + bus + " , " + address + " , " + drivemode
				+ " , " + v + " , " + vMax + " , " + Arrays.toString(functions)
				+ " )");
		final SRCPLocomotive locomotive = addressLocomotiveCache
				.get(new SRCPAddress(bus, address));
		try {
			checkLocomotive(locomotive);
		} catch (final SRCPModelException e1) {
			// ignore unknown locomotive
		}
		// Update locomotive if known and if info is newer than our own.
		if (locomotive != null
				&& timestamp > locomotive.getLastCommandAcknowledge()) {
			locomotive.setDirection(drivemode);
			locomotive.setCurrentSpeed(v);
			locomotive.setFunctions(functions);
			// informListeners(locomotive);
		}
	}

	public void GLterm(final double timestamp, final int bus, final int address) {
		logger.debug("GLterm( " + bus + " , " + address + " )");

		final SRCPLocomotive locomotive = addressLocomotiveCache
				.get(new SRCPAddress(bus, address));
		try {
			checkLocomotive(locomotive);
		} catch (final SRCPModelException e1) {
			// ignore unknown locomotive
		}
		if (locomotive != null) {
			locomotive.setGL(null);
			lockControl.unregisterControlObject("GL", new SRCPAddress(bus,
					address));
			locomotive.setInitialized(false);
		}
	}

	public void addLocomotiveChangeListener(
			final SRCPLocomotiveChangeListener l,
			final SRCPLockChangeListener lockListener) {
		listeners.add(l);
		lockControl.addLockChangeListener(lockListener);
	}

	public void removeLocomotiveChangeListener(
			final SRCPLocomotiveChangeListener l,
			final SRCPLockChangeListener lockListener) {
		listeners.remove(l);
		lockControl.removeLockChangeListener(lockListener);
	}

	public void removeAllLocomotiveChangeListener() {
		listeners.clear();
	}

	private void informListeners(final SRCPLocomotive changedLocomotive) {
		for (final SRCPLocomotiveChangeListener l : listeners) {
			l.locomotiveChanged(changedLocomotive);
		}

	}

	private void checkLocomotive(final SRCPLocomotive locomotive)
			throws SRCPLocomotiveException, SRCPModelException {
		if (locomotive == null) {
			return;
		}
		if (!locomotive.checkAddress()) {
			throw new InvalidAddressException();
		}

		if (locomotive.getSession() == null && session == null) {
			throw new NoSessionException();
		}
		if (locomotive.getSession() == null && session != null) {
			locomotive.setSession(session);
		}

		final LocomotiveStrategy strategy = locomotiveStrategies.get(locomotive
				.getClass());
		strategy.initLocomotive(locomotive, session, lockControl);

	}

	public boolean acquireLock(final SRCPLocomotive locomotive)
			throws SRCPLocomotiveLockedException, SRCPModelException {
		checkLocomotive(locomotive);

		try {
			return lockControl.acquireLock(
					"GL",
					new SRCPAddress(locomotive.getBus(), locomotive
							.getAddress()));
		} catch (final SRCPDeviceLockedException e) {
			throw new SRCPLocomotiveLockedException(Constants.ERR_LOCKED, e);
		}
	}

	public boolean releaseLock(final SRCPLocomotive locomotive)
			throws SRCPLocomotiveLockedException, SRCPModelException {

		checkLocomotive(locomotive);
		try {
			return lockControl.releaseLock(
					"GL",
					new SRCPAddress(locomotive.getBus(), locomotive
							.getAddress()));
		} catch (final SRCPDeviceLockedException e) {
			throw new SRCPLocomotiveLockedException(Constants.ERR_LOCKED, e);
		}
	}

	public boolean isLocked(final SRCPLocomotive locomotive)
			throws SRCPModelException {
		checkLocomotive(locomotive);

		return lockControl.isLocked("GL", new SRCPAddress(locomotive.getBus(),
				locomotive.getAddress()));
	}

	public boolean isLockedByMe(final SRCPLocomotive locomotive)
			throws SRCPModelException {
		checkLocomotive(locomotive);

		final int sessionID = lockControl.getLockingSessionID("GL",
				new SRCPAddress(locomotive.getBus(), locomotive.getAddress()));
		if (sessionID == session.getCommandChannelID()) {
			return true;
		} else {
			return false;
		}
	}

	public SRCPSession getSession() {
		return session;
	}

}
