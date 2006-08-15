package de.dermoba.srcp.common.exception;

public class SRCPIOException extends SRCPServerException {

    public final static int NUMBER = 603;

    
	public SRCPIOException (Throwable cause) {
        super(NUMBER,"io exception", cause);
    }
	
	public SRCPIOException () {
        super(NUMBER,"io exception");
    }

	public SRCPException cloneExc() {
		return new SRCPIOException();
	}

}