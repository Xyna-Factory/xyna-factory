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
import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.XMOMVersionStorable;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.update.outdatedclasses_8_2_1_0.XMOMODSConfig;


public class CleanXMOMOdsConfig extends UpdateJustVersion {

  public CleanXMOMOdsConfig(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }
  
  
  @Override
  protected void update() throws XynaException {
    super.update();
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(XMOMVersionStorable.class);
    ods.registerStorable(XMOMODSConfig.class);
    
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<XMOMVersionStorable> versions = con.loadCollection(XMOMVersionStorable.class);
      Set<Long> allRevisions = new HashSet<Long>();
      for (XMOMVersionStorable version : versions) {
        allRevisions.add(version.getRevision());
      }
      Collection<XMOMODSConfig> configs = con.loadCollection(XMOMODSConfig.class);
      Collection<XMOMODSConfig> configsToDelete = new ArrayList<XMOMODSConfig>();
      for (XMOMODSConfig config : configs) {
        if (!allRevisions.contains(config.getRevision())) {
          logger.debug("CleanXMOMOdsConfig found entry with invalid revision " + config.getRevision() + ", cleaning entry " + config.getFqXmlName() + " -> " + config.getOdsName());
          configsToDelete.add(config);
        }
      }
      con.delete(configsToDelete);
      con.commit();
    } finally {
      con.closeConnection();
      ods.unregisterStorable(XMOMVersionStorable.class);
      ods.unregisterStorable(XMOMODSConfig.class);
    }
  }

}
