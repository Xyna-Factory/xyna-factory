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

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.update.outdatedclasses_5_1_4_6.ApplicationEntryStorable;
import com.gip.xyna.update.outdatedclasses_5_1_4_6.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;

/**
 * entfernt alle applicationentries, die vom typ filter sind, weil sie wegen bugz 14551 
 * einen falschen namen haben und wegen bugz 14670 eh nicht mehr benötigt werden
 */
public class UpdateRemoveIncorrectApplicationEntries extends Update {

  private final Version allowedForUpdate;
  private final Version afterUpdate;
  private final boolean mustRegenerate;


  UpdateRemoveIncorrectApplicationEntries(Version allowedForUpdate, Version afterUpdate, boolean mustRegenerate) {
    this.allowedForUpdate = allowedForUpdate;
    this.afterUpdate = afterUpdate;
    this.mustRegenerate = mustRegenerate;
  }


  @Override
  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(ApplicationEntryStorable.class);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<ApplicationEntryStorable> coll = con.loadCollection(ApplicationEntryStorable.class);
      for (ApplicationEntryStorable entry : coll) {
        if (entry.getTypeAsEnum() == ApplicationEntryType.FILTER) {
          con.deleteOneRow(entry);
        }
      }
      con.commit();
    } finally {
      con.closeConnection();
      ods.unregisterStorable(ApplicationEntryStorable.class);
    }
  }


  @Override
  protected Version getAllowedVersionForUpdate() {
    return allowedForUpdate;
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return afterUpdate;
  }


  @Override
  public boolean mustUpdateGeneratedClasses() {
    return mustRegenerate;
  }
}
