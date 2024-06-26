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

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationStorable;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;



public class UpdateSetCreationDateInApplications extends UpdateJustVersion {

  public UpdateSetCreationDateInApplications(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
  }


  @Override
  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(ApplicationStorable.class);
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        long currentTime = System.currentTimeMillis();
        Collection<ApplicationStorable> coll = con.loadCollection(ApplicationStorable.class);
        for (ApplicationStorable as : coll) {
          as.setCreationDate(currentTime);
        }
        con.persistCollection(coll);
        con.commit();
      } finally {
        con.closeConnection();
      }
    } finally {
      ods.unregisterStorable(ApplicationStorable.class);
    }
  }


}
