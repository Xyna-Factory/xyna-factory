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

import java.util.Collection;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;


public class UpdateGrantZetaMonitorRights extends UpdateJustVersion {
  
  public UpdateGrantZetaMonitorRights(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
  }
  
  
  @Override
  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(Role.class);
    try {
      ODSConnection c = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        //neu ben�tigte Rechte den bereits vorhanden Rollen zuweisen
        Collection<Role> roles = c.loadCollection(Role.class);
        for (Role role : roles) {
          //Rechte f�r Ordermonitor, Mi-Monitor und Live-Reporting in neuer GUI den Rollen ADMIN und MODELLER zuweisen
          if (UserManagement.ADMIN_ROLE_NAME.equals(role.getName()) ||
              UserManagement.MODELLER_ROLE_NAME.equals(role.getName())) {
            role.grantRight(GuiRight.ZETA_PROCESS_MONITOR_ORDERMONITOR.getKey());
            role.grantRight(GuiRight.ZETA_PROCESS_MONITOR_MIMONITOR.getKey());
            role.grantRight(GuiRight.ZETA_PROCESS_MONITOR_LIVEREPORTING.getKey());
          }
        }
        c.persistCollection(roles);
        
        c.commit();
      } finally {
        c.closeConnection();
      }
    } finally {
      ods.unregisterStorable(Role.class);
    }
  }
}
