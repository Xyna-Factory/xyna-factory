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

import xmcp.gitintegration.FactoryXmlEntryType;
import xmcp.gitintegration.cli.generated.Listfactoryxmlentrytypes;
import xmcp.gitintegration.impl.processing.FactoryContentProcessingPortal;



public class ListfactoryxmlentrytypesImpl extends XynaCommandImplementation<Listfactoryxmlentrytypes> {

  public void execute(OutputStream statusOutputStream, Listfactoryxmlentrytypes payload) throws XynaException {
    FactoryContentProcessingPortal portal = new FactoryContentProcessingPortal();
    StringBuffer sb = new StringBuffer();
    List<FactoryXmlEntryType> entryTypeList = portal.listFactoryXmlEntrytypes();
    String exampleCall=null;
    if ((entryTypeList != null) && (entryTypeList.size() > 0)) {
      for (FactoryXmlEntryType entryType : entryTypeList) {
        sb.append(entryType.getName());
        for (String ignoreEntryType : entryType.getIgnoreEntryTypes()) {
          sb.append("\n").append(" ").append(ignoreEntryType);
          if(exampleCall == null) {
            exampleCall="./xynafactory.sh addfactoryxmlignoreentry -type " + entryType.getName() + " -value " + ignoreEntryType;
          }
        }
        sb.append("\n");
      }
    }
    if(exampleCall != null) {
      sb.append("\nExample: ").append(exampleCall).append("\n");
    }  
    writeToCommandLine(statusOutputStream, sb.toString());
  }

}
