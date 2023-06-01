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
package com.gip.xyna.xfmg.xopctrl.managedsessions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


/**
 * Zuordnung zwischen Sessions und Users
 *
 */
public class SessionUserMap {

  private Map<String, String> sessionToUser = new HashMap<String, String>();
  private Map<String, List<String>> userToSessions = new HashMap<String, List<String>>();
  //TODO user-Eintrag aus userToSessions entfernen, wenn der user gelöscht wird
  
  private Set<String> lockedUser = new HashSet<String>();
  
  private ReentrantLock lock = new ReentrantLock();
  
  
  /**
   * Trägt eine Session mit zugehörigem User in die Maps ein.
   * @param sessionId
   * @param user
   * @throws PersistenceLayerException
   * @throws XFMG_UserIsLockedException
   */
  public void addSession(String sessionId, String user) throws PersistenceLayerException, XFMG_UserIsLockedException {
    lock.lock();
    try{
      if (lockedUser.contains(user)) {
        throw new XFMG_UserIsLockedException(user);
      }
      
      List<String> sessionIds = userToSessions.get(user);
      if (sessionIds == null) {
        sessionIds = new ArrayList<String>();
      }
      sessionIds.add(sessionId);
      userToSessions.put(user, sessionIds);
      
      sessionToUser.put(sessionId, user);
    } finally {
      lock.unlock();
    }
  }
  
  
  /**
   * Entfernt eine Session aus den Maps.
   * @param sessionId
   */
  public void removeSession(String sessionId) {
    lock.lock();
    try{
      String user = sessionToUser.remove(sessionId);
      
      if (user != null && userToSessions.get(user) != null) {
        userToSessions.get(user).remove(sessionId);
      }
    } finally {
      lock.unlock();
    }
  }
  
  
  /**
   * Liefert die SessionIds für einen User
   * @param user
   * @return
   */
  public List<String> getSessionIds(String user) {
    List<String> sessionIds = new ArrayList<String>();
    
    lock.lock();
    try{
      if(userToSessions.get(user) != null) {
        sessionIds.addAll(userToSessions.get(user));
      }
      return sessionIds;
    } finally {
      lock.unlock();
    }
  }
  
  
  public String getUser(String sessionId) {
    lock.lock();
    try{
      return sessionToUser.get(sessionId);
    } finally {
      lock.unlock();
    }
  }
  
  /**
   * Trägt gesperrte User in die lockedUser-Liste bzw. entfernt nicht gesperrte User.
   * @param user
   */
  public void lockUser(User user) {
    lock.lock();
    try{
      if (user.isLocked()) {
        lockedUser.add(user.getName());
      } else {
        lockedUser.remove(user.getName());
      }
    } finally {
      lock.unlock();
    }
  }
}
