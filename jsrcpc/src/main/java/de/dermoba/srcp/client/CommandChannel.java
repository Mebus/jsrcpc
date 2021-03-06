/*
 * Created on 26.09.2005
 *
 */
package de.dermoba.srcp.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import de.dermoba.srcp.common.SocketReader;
import de.dermoba.srcp.common.SocketWriter;
import de.dermoba.srcp.common.exception.SRCPException;
import de.dermoba.srcp.common.exception.SRCPHostNotFoundException;
import de.dermoba.srcp.common.exception.SRCPIOException;

public class CommandChannel {

	private int id;

	String serverName = null;

	int serverPort;

	private Socket socket = null;

	private SocketWriter out = null;

	private SocketReader in = null;

	private final Set<CommandDataListener> listeners;

	/**
	 * creates a new SRCP connection on the command channel to handle all
	 * command communication.
	 * 
	 * @param pServerName
	 *            name or IP-Address of server
	 * @param pServerPort
	 *            TCP port number
	 * @throws SRCPException
	 */
	public CommandChannel(final String pServerName, final int pServerPort)
			throws SRCPException {
		serverName = pServerName;
		serverPort = pServerPort;
		listeners = new HashSet<CommandDataListener>();
	}

	public void connect() throws SRCPException {
		try {
			socket = new Socket();
			socket.connect(new InetSocketAddress(serverName, serverPort), 5000);
			out = new SocketWriter(socket);
			in = new SocketReader(socket);
			final String incoming = in.read();
			informListenersReceived(incoming);
		} catch (final UnknownHostException e) {
			throw new SRCPHostNotFoundException();
		} catch (final IOException e) {
			throw new SRCPIOException(e);
		}
		send("SET CONNECTIONMODE SRCP COMMAND");
		final String output = sendReceive("GO");
		final String[] outputSplitted = output.split(" ");

		if (outputSplitted.length >= 5) {
			id = Integer.parseInt(outputSplitted[4]);
		}
	}

	public void disconnect() throws SRCPException {
		try {
			// sendReceive("SESSION 0 TERM");
			if (socket != null) {
				socket.close();
				socket = null;
			}
		} catch (final IOException e) {
			throw new SRCPIOException(e);
		}
	}

	public String sendReceive(String output) throws SRCPException {
		if (out == null) {
			throw new SRCPIOException();
		}
		String s = "";
		try {
			if (output != null) {
				informListenersSent(output);
				output += "\n";
				out.write(output);
			}
			s = in.read();
			if (s == null) {
				throw new SRCPIOException();
			}
		} catch (final IOException e) {
			throw new SRCPIOException();
		}
		informListenersReceived(s);
		return s;
	}

	/**
	 * send a command to the server
	 * 
	 * @param pCommand
	 *            the command to send
	 * @return the ervers reply
	 * @throws SRCPException
	 */
	public String send(final String pCommand) throws SRCPException {
		final String response = sendReceive(pCommand);
		final SRCPException ex = ReceivedExceptionFactory.parseResponse(
				pCommand, response);
		if (ex != null) {
			throw ex;
		}
		return response;
	}

	private void informListenersReceived(final String s) {
		for (final CommandDataListener l : listeners) {
			l.commandDataReceived(s);
		}
	}

	private void informListenersSent(final String s) {
		for (final CommandDataListener l : listeners) {
			l.commandDataSent(s);
		}
	}

	public void addCommandDataListener(final CommandDataListener l) {
		listeners.add(l);
	}

	public void removeCommandDataListener(final CommandDataListener l) {
		listeners.remove(l);
	}

	public int getID() {
		return id;
	}
}
