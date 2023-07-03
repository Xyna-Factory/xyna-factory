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
package xmcp.gitintegration.cli.impl;

import java.io.OutputStream;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import xmcp.gitintegration.cli.generated.ListReferences;
import xmcp.gitintegration.storage.ReferenceStorable;
import xmcp.gitintegration.storage.ReferenceStorage;



public class ListReferencesImpl extends XynaCommandImplementation<ListReferences> {

  public void execute(OutputStream statusOutputStream, ListReferences payload) throws XynaException {
    
    ReferenceStorage storage = new ReferenceStorage();
    String workspaceName = payload.getWorkspaceName();
    List<ReferenceStorable> references = null;
    if(workspaceName == null) {
      references = storage.getAllReferences();
    } else {
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision = revMgmt.getRevision(new Workspace(workspaceName));
      references = storage.getAllReferencesForWorkspace(revision);
    }

    String storableString;
    for(ReferenceStorable storable : references) {
      storableString = storable.getObjectName() + ", " + storable.getPath() + ", " + storable.getReftype() + ", " + storable.getObjecttype() + ", "+ storable.getWorkspace() + "\n";
      writeToCommandLine(statusOutputStream, storableString);
    }
  }

}
