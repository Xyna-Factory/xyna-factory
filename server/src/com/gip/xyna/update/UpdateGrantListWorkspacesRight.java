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
import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;

public class UpdateGrantListWorkspacesRight extends UpdateJustVersion {

  public UpdateGrantListWorkspacesRight(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
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
          if (role.getAlias() != null && !role.getAlias().equals("")) {
            continue; //Externe Rolle, daher keine Rechte zuweisen
          }
          
          Set<String> newScopedRights = new HashSet<String>();
          newScopedRights.addAll(role.getScopedRights());
          newScopedRights.add(ScopedRight.WORKSPACE.getKey() + ":" + Action.list + ":*");
          role.setScopedRights(newScopedRights);
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
