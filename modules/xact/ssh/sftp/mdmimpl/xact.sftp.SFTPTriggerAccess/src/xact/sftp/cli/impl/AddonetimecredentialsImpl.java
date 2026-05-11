/*----------------------------------------------------
* Xyna 5.1 (Black Edition)
* Xyna Multi-Channel Portal
*----------------------------------------------------
* Copyright GIP AG 2013
* (http://www.gip.com)
* Hechtsheimer Str. 35-37
* 55131 Mainz
*----------------------------------------------------
* $Revision: 221780 $
* $Date: 2018-04-09 09:32:14 +0200 (Mo, 09 Apr 2018) $
*----------------------------------------------------
*/
package xact.sftp.cli.impl;

import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import java.io.OutputStream;
import com.gip.xyna.utils.exceptions.XynaException;
import xact.sftp.cli.generated.Addonetimecredentials;
import xact.sftp.impl.SFTPTriggerAccessServiceOperationImpl;



public class AddonetimecredentialsImpl extends XynaCommandImplementation<Addonetimecredentials> {

  public void execute(OutputStream statusOutputStream, Addonetimecredentials payload) throws XynaException {
    SFTPTriggerAccessServiceOperationImpl.addOneTimeCredentials(payload.getUsername(), payload.getPassword(), payload.getIp(), payload.getPort());
    writeLineToCommandLine(statusOutputStream, "One time crediantials added succesfully.");
  }

}
