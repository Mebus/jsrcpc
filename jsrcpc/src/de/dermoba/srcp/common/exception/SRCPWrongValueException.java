/*
 * $RCSfile: SRCPWrongValueException.java,v $
 *
 * History
 $Log: not supported by cvs2svn $
 Revision 1.1  2005/10/02 17:49:41  fork_ch
 - implemented the basic devicegroups
 - uses now the SocketReader / SocketWriter

 Revision 1.1  2005/07/09 13:11:58  harders
 balkon050709

 Revision 1.1  2005/06/30 14:41:31  harders
 Aufger�umte erste Version

 Revision 1.1.1.1  2002/01/08 18:21:54  osc3
 import of jsrcpd


 */

package de.dermoba.srcp.common.exception;


/**
 *
 * @author  osc
 * @version $Revision: 1.2 $
  */

public class SRCPWrongValueException extends SRCPCommandException {

    public SRCPWrongValueException () {
        super(412,"wrong value");
    }

    public SRCPException cloneExc () {
    	return new SRCPWrongValueException();
    }
}
