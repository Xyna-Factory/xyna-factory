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
package xmcp.gitintegration.cli.impl;



import java.io.OutputStream;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xmcp.gitintegration.FactoryXmlIgnoreEntry;
import xmcp.gitintegration.cli.generated.Validatefactoryxmlignoreentries;
import xmcp.gitintegration.impl.processing.FactoryContentProcessingPortal;



public class ValidatefactoryxmlignoreentriesImpl extends XynaCommandImplementation<Validatefactoryxmlignoreentries> {

  public void execute(OutputStream statusOutputStream, Validatefactoryxmlignoreentries payload) throws XynaException {
    FactoryContentProcessingPortal portal = new FactoryContentProcessingPortal();
    List<FactoryXmlIgnoreEntry> entryList = portal.listInvalidateFactoryXmlIgnoreEntries(payload.getRemove());

    StringBuilder sb = new StringBuilder();
    if ((entryList != null) && (entryList.size() > 0)) {
      if (payload.getRemove()) {
        sb.append("The following entries were invalid and have been removed:\n");
      } else {
        sb.append("The following entries are invalid:\n");
      }
    }
    writeToCommandLine(statusOutputStream, sb.toString());
  }

}
