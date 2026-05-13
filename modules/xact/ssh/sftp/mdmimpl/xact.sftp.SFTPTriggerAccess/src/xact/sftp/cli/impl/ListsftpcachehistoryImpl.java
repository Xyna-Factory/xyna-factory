/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
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
