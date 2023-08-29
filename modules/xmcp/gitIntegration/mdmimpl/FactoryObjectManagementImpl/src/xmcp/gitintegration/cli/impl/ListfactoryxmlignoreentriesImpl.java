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
import java.util.Collections;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xmcp.gitintegration.FactoryObjectManagement;
import xmcp.gitintegration.FactoryXmlIgnoreEntry;
import xmcp.gitintegration.cli.generated.Listfactoryxmlignoreentries;



public class ListfactoryxmlignoreentriesImpl extends XynaCommandImplementation<Listfactoryxmlignoreentries> {

  public void execute(OutputStream statusOutputStream, Listfactoryxmlignoreentries payload) throws XynaException {
    List<? extends FactoryXmlIgnoreEntry> entryList = FactoryObjectManagement.listFactoryXmlIgnoreEntries();
    Collections.sort(entryList, new FactoryXmlIgnoreEntryComparator());
    StringBuilder sb = new StringBuilder();
    if (entryList != null) {
      for (FactoryXmlIgnoreEntry entry : entryList) {
        sb.append(entry.getConfigType()).append(" ").append(entry.getValue()).append("\n");
      }
    }
    writeToCommandLine(statusOutputStream, sb.toString());
  }


  class FactoryXmlIgnoreEntryComparator implements java.util.Comparator<FactoryXmlIgnoreEntry> {

    @Override
    public int compare(FactoryXmlIgnoreEntry a, FactoryXmlIgnoreEntry b) {
      return a.getConfigType().compareTo(b.getConfigType());
    }
  }

}
