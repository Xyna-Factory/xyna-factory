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
package com.gip.xyna.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.FileUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ScopedRightUtils;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;


public class UpdateGrantFileAccessRights extends UpdateJustVersion {
  
  public UpdateGrantFileAccessRights(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
  }
  
  
  @Override
  protected void update() throws XynaException {
    String importRightStart = ScopedRightUtils.getScopedRight(ScopedRight.APPLICATION.getKey(), Arrays.asList(UserManagement.Action.deploy.toString()));
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(Role.class);
    try {
      ODSConnection c = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        Collection<Role> roles = c.loadCollection(Role.class);
        for (Role role : roles) {
          if (role.getAlias() != null && !role.getAlias().equals("")) {
            continue; //Externe Rolle
          }
          boolean hasRight = role.hasRight(Rights.APPLICATION_MANAGEMENT.toString()) ||
                             role.hasRight(Rights.APPLICATION_ADMINISTRATION.toString());
          if (!hasRight) {
            for (String scopedRight : role.getScopedRights()) {
              if (scopedRight.startsWith(importRightStart)) {
                hasRight = true;
                break;
              }
            }
          }
          if (hasRight) {
            Set<String> newScopedRights = new HashSet<String>();
            newScopedRights.addAll(role.getScopedRights());
            newScopedRights.add(ScopedRight.FILE_ACCESS.getKey() + ":*:" + FileUtils.getSystemTempDir() + "*");
            role.setScopedRights(newScopedRights);
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
