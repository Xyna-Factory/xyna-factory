/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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


package xmcp.oas.fman.codedservice;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;

import base.KeyValue;


public class CSCreateWorkspaceInputDownloadData {

  public List<? extends KeyValue> execute() {
    List<KeyValue> result = new ArrayList<KeyValue>();
    result.add(new KeyValue.Builder().key("<Application>").value(" ").instance());
    WorkspaceManagement wsMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    wsMgmt.listWorkspaces(false).forEach(
      ws -> result.add(new KeyValue.Builder().key(ws.getWorkspace().getName()).value(ws.getWorkspace().getName()).instance())
    );
    return result;
  }
  
}
