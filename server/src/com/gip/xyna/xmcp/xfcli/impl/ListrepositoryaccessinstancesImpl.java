/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.utils.exceptions.XynaException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.xmcp.xfcli.generated.Listrepositoryaccessinstances;



public class ListrepositoryaccessinstancesImpl extends XynaCommandImplementation<Listrepositoryaccessinstances> {

  public void execute(OutputStream statusOutputStream, Listrepositoryaccessinstances payload) throws XynaException {
    Map<String, RepositoryAccess> repositoryAccesses = 
      XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryAccessManagement().listRepositoryAccessInstances();
    writeLineToCommandLine(statusOutputStream, repositoryAccesses.size() + " repositoryAccessInstances defined"
                           + (repositoryAccesses.size() > 0 ? ":" : "."));
    for (RepositoryAccess repositoryAccessInstance : repositoryAccesses.values()) {
      writeLineToCommandLine(statusOutputStream, repositoryAccessInstance.getName() + " - " + repositoryAccessInstance);
    }
  }

}
