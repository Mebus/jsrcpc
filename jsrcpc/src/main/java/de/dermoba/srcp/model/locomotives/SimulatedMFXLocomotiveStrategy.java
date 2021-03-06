package de.dermoba.srcp.model.locomotives;

import java.util.Arrays;

import de.dermoba.srcp.client.SRCPSession;
import de.dermoba.srcp.common.Response;
import de.dermoba.srcp.common.exception.SRCPException;
import de.dermoba.srcp.devices.GL;
import de.dermoba.srcp.model.Constants;
import de.dermoba.srcp.model.SRCPAddress;
import de.dermoba.srcp.model.locking.SRCPLockControl;

public class SimulatedMFXLocomotiveStrategy extends LocomotiveStrategy {

	@Override
	public void setSpeed(final SRCPLocomotive locomotive, final int speed,
			boolean[] functions) throws SRCPException {
		final DoubleMMDigitalLocomotive doubleMM = (DoubleMMDigitalLocomotive) locomotive;
		if (functions == null) {
			functions = locomotive.getFunctions();
		}

		final boolean[] functions1 = Arrays.copyOfRange(functions, 0, 5);
		final boolean[] functions2 = Arrays.copyOfRange(functions, 5, 10);

		String resp = setSpeedOnGl(doubleMM.getGL(), doubleMM, speed,
				functions1);

		if (resp == null || resp.equals("")) {
			return;
		}
		Response r = new Response(resp);
		locomotive.setLastCommandAcknowledge(r.getTimestamp());

		doubleMM.setCurrentSpeed(speed);
		doubleMM.setFunctions(functions);
		resp = setSpeedOnGl(doubleMM.getGL2(), doubleMM, speed, functions2);

		r = new Response(resp);
		locomotive.setLastCommandAcknowledge(r.getTimestamp());
	}

	@Override
	public void initLocomotive(final SRCPLocomotive locomotive,
			final SRCPSession session, final SRCPLockControl lockControl)
			throws SRCPLocomotiveException {

		final DoubleMMDigitalLocomotive doubleMM = (DoubleMMDigitalLocomotive) locomotive;
		if (doubleMM.getGL() == null) {
			final GL gl = new GL(session, locomotive.getBus());
			gl.setAddress(locomotive.getAddress());
			locomotive.setGL(gl);
			lockControl.registerControlObject(
					"GL",
					new SRCPAddress(locomotive.getBus(), locomotive
							.getAddress()), locomotive);
		}
		if (doubleMM.getGL2() == null) {
			final GL gl2 = new GL(session, locomotive.getBus());
			gl2.setAddress(doubleMM.getAddress2());
			doubleMM.setGL2(gl2);
			lockControl
					.registerControlObject(
							"GL",
							new SRCPAddress(locomotive.getBus(), doubleMM
									.getAddress2()), locomotive);
		}
		if (!locomotive.isInitialized()) {
			initLocomotive(doubleMM);
		}
	}

	private void initLocomotive(final DoubleMMDigitalLocomotive locomotive)
			throws SRCPLocomotiveException {
		try {
			final String[] params = locomotive.getParams();
			final String[] params2 = locomotive.getParams2();
			locomotive.getGL().init(locomotive.getAddress(),
					locomotive.getProtocol(), params);
			locomotive.getGL2().init(locomotive.getAddress2(),
					locomotive.getProtocol(), params2);
			locomotive.setInitialized(true);
		} catch (final SRCPException x) {
			throw new SRCPLocomotiveException(Constants.ERR_INIT_FAILED, x);
		}
	}

	@Override
	public boolean[] getEmergencyStopFunctions(final SRCPLocomotive locomotive,
			final int emergencyStopFunction) {
		final boolean[] functions = new boolean[] { false, false, false, false,
				false, false, false, false, false, false };
		if (emergencyStopFunction != -1) {
			functions[emergencyStopFunction] = true;
		}
		return functions;
	}

}
