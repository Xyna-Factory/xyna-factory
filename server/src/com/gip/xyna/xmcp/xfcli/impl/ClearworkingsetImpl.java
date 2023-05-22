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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ClearWorkingSet;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl.OrderEntryInterfacesCouldNotBeClosedException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkingSetBlackListXynaProperties;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl.Operation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Clearworkingset;



public class ClearworkingsetImpl extends XynaCommandImplementation<Clearworkingset> {

  //ACHTUNG klasse wird auch in rmichannelimpl verwendet
  
  public void execute(OutputStream statusOutputStream, Clearworkingset payload) throws XynaException {
    ClearWorkingSet cws = new ClearWorkingSet(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    WorkingSetBlackListXynaProperties blackList = new WorkingSetBlackListXynaProperties();
    cws.setBlackList(blackList);
    
    List<String> removeSubtypesOf = new ArrayList<String>();
    if (payload.getRemoveSubtypesOf() != null) {
      removeSubtypesOf.addAll(Arrays.asList(payload.getRemoveSubtypesOf()));
    }
    
    CommandControl.tryLock(Operation.APPLICATION_CLEAR_WORKINGSET);
    CommandControl.unlock(Operation.APPLICATION_CLEAR_WORKINGSET); //kurz danach wird writelock geholt, das kann nicht upgegraded werden
    try {
      cws.clear(payload.getForce(), removeSubtypesOf);
    } catch (OrderEntryInterfacesCouldNotBeClosedException e) {
      throw new RuntimeException(e);
    }
  }

}
