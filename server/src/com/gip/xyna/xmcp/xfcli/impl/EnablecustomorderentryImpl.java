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



import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Enablecustomorderentry;



public class EnablecustomorderentryImpl extends XynaCommandImplementation<Enablecustomorderentry> {

  public void execute(OutputStream statusOutputStream, Enablecustomorderentry payload) throws XynaException {

    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revisionToOpen = rm.getRevision(payload.getApplicationName(), payload.getVersionName(), null);
    Long definingRevision = rm.getRevision(payload.getDefiningApplicationName(), payload.getDefiningApplicationVersion(), null);
    String orderEntryName = payload.getOrderEntryName();

    RevisionOrderControl.enableCustomOrderEntry(revisionToOpen, definingRevision, orderEntryName);
  }

}
