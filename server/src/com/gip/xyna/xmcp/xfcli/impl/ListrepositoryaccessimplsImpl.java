/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess;
import com.gip.xyna.xmcp.xfcli.StringParameterFormatter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listrepositoryaccessimpls;



public class ListrepositoryaccessimplsImpl extends XynaCommandImplementation<Listrepositoryaccessimpls> {
  
  

  public void execute(OutputStream statusOutputStream, Listrepositoryaccessimpls payload) throws XynaException {
    Map<String, Class<?>> registeredRepositoryAccesses =
      XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryAccessManagement().listRegisteredRepositoryAccesses();
    writeLineToCommandLine(statusOutputStream, registeredRepositoryAccesses.size()
        + " registered repository access implementations and initialization parameters"
        + (registeredRepositoryAccesses.size() > 0 ? ":" : "."));
    for (Entry<String, Class<?>> registeredCodeAccess : registeredRepositoryAccesses.entrySet()) {
      Class<?> clazz = registeredCodeAccess.getValue();
      RepositoryAccess ra;
      try {
        ra = (RepositoryAccess) clazz.getConstructor().newInstance();
        writeLineToCommandLine(statusOutputStream, " - " + registeredCodeAccess.getKey() + " -> " + clazz.getName());
        for (StringParameter<?> parameterInfo : ra.getParameterInformation()) {
          StringBuilder output = new StringBuilder();
          StringParameterFormatter.appendStringParameter(output, parameterInfo);
          writeLineToCommandLine(statusOutputStream, output.toString());
        }
      } catch (Exception e) {
        String msg =
            "Could not get parameter information for " + registeredCodeAccess.getKey() + " -> " + clazz.getName();
        throw new RuntimeException(msg, e);
      }
    }
  }
  

}
