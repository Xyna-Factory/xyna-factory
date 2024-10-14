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

package com.gip.xyna.update;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.update.outdatedclasses_8_2_1_0.XMOMODSConfig;

public class UpdateRemoveRelics extends UpdateJustVersion {

  public UpdateRemoveRelics(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
    setExecutionTime(ExecutionTime.endOfFactoryStart);
  }

  @Override
  public void update() throws XynaException {
    Set<Long> allRevisions = new HashSet<>(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getAllRevisions());
    ODSImpl.getInstance().registerStorable(XMOMODSConfig.class);
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<XMOMODSConfig> allODSConfigs = con.loadCollection(XMOMODSConfig.class);

      Set<XMOMODSConfig> toDelete = new HashSet<>();
      for (XMOMODSConfig cfg : allODSConfigs) {
        if (!allRevisions.contains(cfg.getRevision())) {
          toDelete.add(cfg);
        }
      }

      if (!toDelete.isEmpty()) {
        con.delete(toDelete);
        con.commit();
      }
    } finally {
      con.closeConnection();

    }
  }

}
