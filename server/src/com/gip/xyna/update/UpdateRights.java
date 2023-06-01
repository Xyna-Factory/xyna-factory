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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;


public class UpdateRights extends UpdateJustVersion {

  public UpdateRights(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
  }
  
  private static enum OldScopedRights {
    MANAGE_TCO("xprc.xpce.ManageTimeControlledOrder"),
    READ_TCO("xprc.xpce.ReadTimeControlledOrder");
    
    private String key;

    private OldScopedRights(String key) {
      this.key = key;
    }
  }
  
  @Override
  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(Right.class);
    ods.registerStorable(RightScope.class);
    ods.registerStorable(Role.class);
    try {
      ODSConnection c = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        //die neue Rechte werden automatisch bei der UserManagement-Initialisierung angelegt
        
        //Rechtezuweisung der Rollen ändern
        Collection<Role> roles = c.loadCollection(Role.class);
        for (Role role : roles) {
          Set<String> newScopedRights = new HashSet<String>();
          
          //simple Rechte auf neue komplexe Rechte mappen
          boolean bpView = role.revokeRight("BATCH_PROCESS_VIEW");
          boolean bpMgmt = role.revokeRight("BATCH_PROCESS_MANAGEMENT");
          if (bpView && bpMgmt) {
            newScopedRights.add(ScopedRight.TIME_CONTROLLED_ORDER.getKey() + ":*:*:*:*");
          } else if (bpView) {
            newScopedRights.add(ScopedRight.TIME_CONTROLLED_ORDER.getKey() + ":" + Action.read + ":*:*:*");
          } else if (bpMgmt) {
            newScopedRights.add(ScopedRight.TIME_CONTROLLED_ORDER.getKey() + ":" + Action.write + ":*:*:*");
            newScopedRights.add(ScopedRight.TIME_CONTROLLED_ORDER.getKey() + ":" + Action.insert + ":*:*:*");
            newScopedRights.add(ScopedRight.TIME_CONTROLLED_ORDER.getKey() + ":" + Action.enable + ":*:*:*");
            newScopedRights.add(ScopedRight.TIME_CONTROLLED_ORDER.getKey() + ":" + Action.disable + ":*:*:*");
            newScopedRights.add(ScopedRight.TIME_CONTROLLED_ORDER.getKey() + ":" + Action.kill + ":*:*:*");
          }
          
          if (role.revokeRight("ORDERTYPE_MANAGEMENT")) {
            newScopedRights.add(ScopedRight.ORDER_TYPE.getKey() + ":*:*:*:*");
          }
          if (role.revokeRight("CAPACITY_MANAGEMENT")) {
            newScopedRights.add(ScopedRight.CAPACITY.getKey() + ":*:*");
          }
          if (role.revokeRight("ADMINISTRATIVE_VETO_MANAGEMENT")) {
            newScopedRights.add(ScopedRight.VETO.getKey() + ":*:*");
          }
          
          //komplexe Rechte mappen
          for (String right : role.getScopedRights()) {
            if (right.startsWith(OldScopedRights.READ_TCO.key)) {
              String suffix = right.substring(OldScopedRights.READ_TCO.key.length());
              newScopedRights.add(ScopedRight.TIME_CONTROLLED_ORDER.getKey() + ":" + Action.read + suffix);
            } else if (right.startsWith(OldScopedRights.MANAGE_TCO.key)) {
              String suffix = right.substring(OldScopedRights.MANAGE_TCO.key.length());
              newScopedRights.add(ScopedRight.TIME_CONTROLLED_ORDER.getKey() + ":" + Action.write + suffix);
              newScopedRights.add(ScopedRight.TIME_CONTROLLED_ORDER.getKey() + ":" + Action.insert + suffix);
              newScopedRights.add(ScopedRight.TIME_CONTROLLED_ORDER.getKey() + ":" + Action.enable + suffix);
              newScopedRights.add(ScopedRight.TIME_CONTROLLED_ORDER.getKey() + ":" + Action.disable + suffix);
              newScopedRights.add(ScopedRight.TIME_CONTROLLED_ORDER.getKey() + ":" + Action.kill + suffix);
            } else {
              newScopedRights.add(right);
            }
          }
          role.setScopedRights(newScopedRights);
        }
        c.persistCollection(roles);
        
        //alte Rechte löschen
        List<Right> rightsToDelete = new ArrayList<Right>();
        rightsToDelete.add(new Right("BATCH_PROCESS_VIEW"));
        rightsToDelete.add(new Right("BATCH_PROCESS_MANAGEMENT"));
        rightsToDelete.add(new Right("ORDERTYPE_MANAGEMENT"));
        rightsToDelete.add(new Right("CAPACITY_MANAGEMENT"));
        rightsToDelete.add(new Right("ADMINISTRATIVE_VETO_MANAGEMENT"));

        List<RightScope> rightScopesToDelete = new ArrayList<RightScope>();
        rightScopesToDelete.add(new RightScope("xprc.xpce.ManageTimeControlledOrder"));
        rightScopesToDelete.add(new RightScope("xprc.xpce.ReadTimeControlledOrder"));
        
        c.delete(rightsToDelete);
        c.delete(rightScopesToDelete);
        
        c.commit();
      } finally {
        c.closeConnection();
      }
    } finally {
      ods.unregisterStorable(Right.class);
      ods.unregisterStorable(RightScope.class);
      ods.unregisterStorable(Role.class);
    }
  }
}
