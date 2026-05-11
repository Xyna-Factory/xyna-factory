/*----------------------------------------------------
* Xyna 5.1 (Black Edition)
* Xyna Multi-Channel Portal
*----------------------------------------------------
* Copyright GIP AG 2013
* (http://www.gip.com)
* Hechtsheimer Str. 35-37
* 55131 Mainz
*----------------------------------------------------
* $Revision: 224418 $
* $Date: 2018-05-07 10:17:05 +0200 (Mo, 07 Mai 2018) $
*----------------------------------------------------
*/
package xact.sftp.cli.impl;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xact.sftp.cli.generated.Listsftpcachehistory;
import xact.sftp.impl.SFTPTriggerAccessServiceOperationImpl;



public class ListsftpcachehistoryImpl extends XynaCommandImplementation<Listsftpcachehistory> {

  private final static String LIST_ACCESS_LOG_METHOD_NAME = "getCacheAccessHistoryAsTable";

  @Override
  public void execute(OutputStream statusOutputStream, Listsftpcachehistory payload) throws XynaException {
    try {
      EventListener el = SFTPTriggerAccessServiceOperationImpl.getFirstEnabledTriggerInstanceInSameRevisionOrAbove();
      if (el == null) {
        writeLineToCommandLine(statusOutputStream, "SFTPTrigger instance not found or not enabled.");
        return;
      }
      Method m = el.getClass().getDeclaredMethod(LIST_ACCESS_LOG_METHOD_NAME);
      TableFormatter table = (TableFormatter) m.invoke(el);
      StringBuilder output = new StringBuilder();
      table.writeTableHeader(output);
      table.writeTableRows(output);
      writeLineToCommandLine(statusOutputStream, output.toString());
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
      writeLineToCommandLine(statusOutputStream, "System not properly setup to support methd: " + e.getMessage());
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      writeLineToCommandLine(statusOutputStream, "System not properly setup to support methd: " + e.getMessage());
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      writeLineToCommandLine(statusOutputStream, "System not properly setup to support methd: " + e.getMessage());
    } catch (InvocationTargetException e) {
      e.printStackTrace();
      writeLineToCommandLine(statusOutputStream, "System not properly setup to support methd: " + e.getMessage());
    }
  }


}
