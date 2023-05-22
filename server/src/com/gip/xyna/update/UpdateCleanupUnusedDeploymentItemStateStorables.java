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
package com.gip.xyna.update;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateStorable;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;



public class UpdateCleanupUnusedDeploymentItemStateStorables extends UpdateJustVersion {

  public UpdateCleanupUnusedDeploymentItemStateStorables(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion, false);
    setExecutionTime(ExecutionTime.endOfUpdate);
  }


  @Override
  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    XynaFactoryBase oldInstance = XynaFactory.getInstance();
    try {
      UpdateGeneratedClasses.mockFactory();
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      List<Long> revs = rm.getAllRevisions();
      ods.registerStorable(DeploymentItemStateStorable.class);
      try {
        ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
        try {
          Collection<DeploymentItemStateStorable> coll = new ArrayList<DeploymentItemStateStorable>(con.loadCollection(DeploymentItemStateStorable.class));
          Iterator<DeploymentItemStateStorable> it = coll.iterator();
          while (it.hasNext()) {
            DeploymentItemStateStorable diss = it.next();
            if (isUnused(diss, revs)) {
              it.remove();
            }
          }
          con.deleteAll(DeploymentItemStateStorable.class);
          con.persistCollection(coll);
          con.commit();
        } finally {
          con.closeConnection();
        }
      } finally {
        ods.unregisterStorable(DeploymentItemStateStorable.class);
      }
    } finally {
      XynaFactory.setInstance(oldInstance);
    }
  }


  private boolean isUnused(DeploymentItemStateStorable diss, List<Long> revs) {
    return !revs.contains(diss.getRevision());
  }

}
