/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.SharedLib;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listsharedlibs;



public class ListsharedlibsImpl extends XynaCommandImplementation<Listsharedlibs> {

  public void execute(OutputStream statusOutputStream, Listsharedlibs payload) throws XynaException {
    Long revision = factory.getFactoryManagementPortal().getXynaFactoryControl().getRevisionManagement()
                           .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    
    Collection<SharedLib> libs = factory.getXynaMultiChannelPortalPortal().listSharedLibs(revision);
    String formatLine = "%-30s  %-25s  %-30s";
    writeLineToCommandLine(statusOutputStream, String.format(formatLine, "Shared Library name", "Used by deployed objects", "Jar file content"));
    for (SharedLib sharedLib : libs) {
      boolean firstLine = true;
      List<String> libContent = sharedLib.getContent();
      if (libContent.size() > 0) {
        for (String content : libContent) {
          if (firstLine) {
            writeLineToCommandLine(statusOutputStream, String.format(formatLine, sharedLib.getName(), sharedLib.isInUse(), content));
          } else {
            writeLineToCommandLine(statusOutputStream, String.format(formatLine, "", "", content));
          }
          firstLine = false;
        }
      } else {
        writeLineToCommandLine(statusOutputStream, String.format(formatLine, sharedLib.getName(), sharedLib.isInUse(), "-"));
      }
    }
  }

}
