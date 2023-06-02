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

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;


public class UpdateGrantNewRights extends UpdateJustVersion {
  
  public UpdateGrantNewRights(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
  }
  
  
  @Override
  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(Role.class);
    try {
      ODSConnection c = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        //neu benötigte Rechte den bereits vorhanden Rollen zuweisen
        Collection<Role> roles = c.loadCollection(Role.class);
        for (Role role : roles) {
          if (role.getAlias() != null && !role.getAlias().equals("")) {
            continue; //Externe Rolle, daher keine Rechte zuweisen
          }
          
          Set<String> newScopedRights = new HashSet<String>();
          newScopedRights.addAll(role.getScopedRights());
          
          //vorhandene Rechte ermitteln
          boolean editMdmRight = role.hasRight(Rights.EDIT_MDM.toString());
          boolean deploymentMdmRight = role.hasRight(Rights.DEPLOYMENT_MDM.toString());
          boolean startOrderRight = role.hasRight(Rights.START_ORDER.toString());
          boolean orderArchiveViewRight = role.hasRight(Rights.ORDERARCHIVE_VIEW.toString());
          boolean orderTypesRight = false;
          boolean capacitiesRight = false;
          boolean applicationRight = false;
          
          if (role.hasRight(Rights.APPLICATION_MANAGEMENT.toString())
               || role.hasRight(Rights.APPLICATION_ADMINISTRATION.toString())
               || role.hasRight(Rights.WORKINGSET_MANAGEMENT.toString())) {
            applicationRight = true;
          }

          for (String scopedRight : role.getScopedRights()) {
            if (scopedRight.startsWith(ScopedRight.APPLICATION.getKey())) {
              applicationRight = true;
            }
            if (scopedRight.startsWith(ScopedRight.START_ORDER.getKey())) {
              startOrderRight = true;
            }
            if (scopedRight.startsWith(ScopedRight.ORDER_TYPE.getKey())) {
              orderTypesRight = true;
            }
            if (scopedRight.startsWith(ScopedRight.CAPACITY.getKey())) {
              capacitiesRight = true;
            }
          }
          
          //Datenmodell-Rechte zu Rollen hinzufügen, die vorher die Rechte EDIT_MDM
          //oder ein ApplicationManagement-Recht hatten.
          if (editMdmRight || applicationRight) {
            newScopedRights.add(ScopedRight.DATA_MODEL.allAccess());
          }
          
          //DeploymentMarker-Rechte zu Rollen hinzufügen, die vorher das Recht EDIT_MDM hatten
          if (editMdmRight) {
            newScopedRights.add(ScopedRight.DEPLOYMENT_MARKER.allAccess());
          }
          
          //DeploymentItem-Rechte zu Rollen hinzufügen, die vorher die Rechte EDIT_MDM,
          //DEPLOYMENT_MDM oder ein Application-Management-Recht hatten
          if (editMdmRight || deploymentMdmRight || applicationRight) {
            newScopedRights.add(ScopedRight.DEPLOYMENT_ITEM.allAccess());
          }
          
          //OrderInputSource-Rechte zu Rollen hinzufügen, die vorher das Recht EDIT_MDM hatten
          if (editMdmRight) {
            newScopedRights.add(ScopedRight.ORDER_INPUT_SOURCE.allAccess());
          }
          
          //CronLikeOrders-Rechte zu Rollen hinzufügen, die vorher das Recht EDIT_MDM hatten
          if (startOrderRight) {
            newScopedRights.add(ScopedRight.CRON_LIKE_ORDER.allAccess());
          }
          
          //Read-OrderTypes-Recht zu allen Rollen hinzufügen, die nicht bereits ein Ordertypes-Recht besitzen
          if (!orderTypesRight) {
            newScopedRights.add(ScopedRight.ORDER_TYPE.getKey() + ":" + Action.read + ":*:*:*");
          }
          
          //Read-Capacities-Recht zu allen Rollen hinzufügen, die nicht bereits ein Capacities-Recht besitzen
          if (orderArchiveViewRight && !capacitiesRight) {
            newScopedRights.add(ScopedRight.CAPACITY.getKey() + ":" + Action.read + ":*");
          }
          
          role.setScopedRights(newScopedRights);
          
          //Sichtbarkeitsrechte für GUI zu allen Rollen hinzufügen
          role.grantRight(GuiRight.FACTORY_MANAGER.getKey());
          role.grantRight(GuiRight.PROCESS_MODELLER.getKey());
          role.grantRight(GuiRight.PROCESS_MONITOR.getKey());
          role.grantRight(GuiRight.TEST_FACTORY.getKey());
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
