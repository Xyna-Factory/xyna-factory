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
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listcodeaccessinstances;



public class ListcodeaccessinstancesImpl extends XynaCommandImplementation<Listcodeaccessinstances> {

  public void execute(OutputStream statusOutputStream, Listcodeaccessinstances payload) throws XynaException {
    Map<Long, CodeAccess> codeAccessInstances =
        XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getCodeAccessManagement()
            .listCodeAccessInstances();
    writeLineToCommandLine(statusOutputStream, codeAccessInstances.size() + " codeAccessInstances defined"
        + (codeAccessInstances.size() > 0 ? ":" : "."));
    for (Entry<Long, CodeAccess> codeAccessInstance : codeAccessInstances.entrySet()) {
      //FIXME support f�r andere workingsets
      Long revision = codeAccessInstance.getKey();

      writeLineToCommandLine(statusOutputStream, " - workingset (rev=" + (revision == -1 ? "rev_workingset" : revision)
          + "): " + codeAccessInstance.getValue());
      //TODO mehr informationen: startparameter
    }
  }

}
