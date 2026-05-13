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
