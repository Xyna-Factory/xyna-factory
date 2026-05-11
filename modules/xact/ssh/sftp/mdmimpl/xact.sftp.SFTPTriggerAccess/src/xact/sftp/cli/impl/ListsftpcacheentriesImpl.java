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

import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;

import xact.sftp.cli.generated.Listsftpcacheentries;
import xact.sftp.impl.SFTPTriggerAccessServiceOperationImpl;



public class ListsftpcacheentriesImpl extends XynaCommandImplementation<Listsftpcacheentries> {
  
  private final static String LIST_CACHE_METHOD_NAME = "listCacheKeysAsTable";

  public void execute(OutputStream statusOutputStream, Listsftpcacheentries payload) throws XynaException {
    try {
      EventListener el = SFTPTriggerAccessServiceOperationImpl.getFirstEnabledTriggerInstanceInSameRevisionOrAbove();
      Method m = el.getClass().getDeclaredMethod(LIST_CACHE_METHOD_NAME);
      TableFormatter table = (TableFormatter) m.invoke(el);
      StringBuilder output = new StringBuilder();
      table.writeTableHeader(output);
      table.writeTableRows(output);
      writeLineToCommandLine(statusOutputStream, output.toString());
    } catch (NoSuchMethodException e) {
      writeLineToCommandLine(statusOutputStream, "System not properly setup to support methd: " + e.getMessage());
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      writeLineToCommandLine(statusOutputStream, "System not properly setup to support methd: " + e.getMessage());
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      writeLineToCommandLine(statusOutputStream, "System not properly setup to support methd: " + e.getMessage());
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      writeLineToCommandLine(statusOutputStream, "System not properly setup to support methd: " + e.getMessage());
      e.printStackTrace();
    }
  }

}
