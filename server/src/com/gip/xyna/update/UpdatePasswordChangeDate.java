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
import java.util.List;

import com.gip.xyna.update.utils.StorableUpdater;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.PasswordHistoryStorable;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User.ChangeReason;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class UpdatePasswordChangeDate extends UpdateJustVersion {

  public UpdatePasswordChangeDate(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }
  
  @Override
  protected void update() throws XynaException {
    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_6_1_2_0.User.class,
                           User.class,
                           new SetPasswordChangeDate(),
                           ODSConnectionType.DEFAULT);
    
    ODS ods = ODSImpl.getInstance();
    boolean areHistoryAndDefaultTheSameForUsers =
                    ods.isSamePhysicalTable(User.TABLENAME, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY);
    if (!areHistoryAndDefaultTheSameForUsers) {
      StorableUpdater.update(com.gip.xyna.update.outdatedclasses_6_1_2_0.User.class,
                             User.class,
                             new SetPasswordChangeDate(),
                             ODSConnectionType.HISTORY);
    }

    addPasswordsToHistory(ODSConnectionType.HISTORY);
  }
  
  
  private static class SetPasswordChangeDate implements Transformation<com.gip.xyna.update.outdatedclasses_6_1_2_0.User, User> {
    
    public User transform(com.gip.xyna.update.outdatedclasses_6_1_2_0.User from) {
      User to = new User();
      to.setName(from.getName());
      to.setRole(from.getRole());
      to.setPasswordHash(from.getPassword());
      to.setCreationDate(from.getCreationDate());
      to.setLocked(from.isLocked());
      to.setDomains(from.getDomains());
      to.setFailedLogins(from.getFailedLogins());
      to.setPasswordChangeDate(System.currentTimeMillis());
      to.setPasswordChangeReason(ChangeReason.CHANGE_PASSWORD.toString());
      
      return to;
    }
  }
  

  public static void addPasswordsToHistory(ODSConnectionType connectionType) throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    
    ODSConnection con = ods.openConnection(connectionType);
    
    try{
      Collection<User> users;
      ods.registerStorable(User.class);
      try {
        users = con.loadCollection(User.class);
      } finally {
        ods.unregisterStorable(User.class);
      }
      
      List<PasswordHistoryStorable> history = new ArrayList<PasswordHistoryStorable>();
      for (User user : users) {
        history.add(new PasswordHistoryStorable(user.getName(), user.getPassword(), user.getPasswordChangeDate(), 1));
      }
      
      ods.registerStorable(PasswordHistoryStorable.class);
      try {
        con.persistCollection(history);
        con.commit();
      } finally {
        ods.unregisterStorable(PasswordHistoryStorable.class);
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
  }
}
