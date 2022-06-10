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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Requirecapacityforordertype;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xsched.XynaScheduler;


public class RequirecapacityforordertypeImpl extends XynaCommandImplementation<Requirecapacityforordertype> {

  @Override
  public void execute(OutputStream statusOutputStream, Requirecapacityforordertype payload) throws XynaException {

    String orderType = payload.getOrderType();
    String capName = payload.getCapacityName();

    Integer cardinality = null;
    try {
      cardinality = Integer.valueOf(payload.getCardinality());
    } catch (NumberFormatException e) {
      writeLineToCommandLine(statusOutputStream,
                             "Could not parse parameter 'cardinality' ('" + payload.getCardinality() + "')");
      return;
    }

    // Prüfung, ob Application/Version bzw. Workspace existiert
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    RuntimeContext runtimeContext = RevisionManagement.getRuntimeContext(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    Long revision = revisionManagement.getRevision(runtimeContext);

    CommandControl.tryLock(CommandControl.Operation.ORDERTYPE_CAPACITY_MAPPING_CREATE, revision);
    boolean success = false;
    try {
      success = factory.getXynaMultiChannelPortalPortal().requireCapacityForOrderType(orderType, capName, cardinality, runtimeContext);
    } finally {
      CommandControl.unlock(CommandControl.Operation.ORDERTYPE_CAPACITY_MAPPING_CREATE, revision);
    }

    writeLineToCommandLine(statusOutputStream, "Successfully changed capacity '" + capName + "' requirement "
        + "for order type '" + orderType + "' to '" + cardinality + "'");

    if( success && payload.getUpdateschedulingorders() ) {
      DestinationKey destinationKey = new DestinationKey(orderType, runtimeContext);
      XynaScheduler xynaScheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler();
      int modified = xynaScheduler.getAllOrdersList().changeCapacityRequirement(destinationKey, true, capName, cardinality);
      writeLineToCommandLine(statusOutputStream,
                             (modified == 0? "No affected":("Successfully updated "+modified)) +
                             " orders waiting in scheduler."); 
    }
  }

}
