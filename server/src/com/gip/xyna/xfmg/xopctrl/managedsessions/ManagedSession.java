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

package com.gip.xyna.xfmg.xopctrl.managedsessions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.exceptions.XFMG_SessionTimedOutException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey="id", tableName=ManagedSession.TABLE_NAME)
public class ManagedSession extends Storable<ManagedSession> {

  private static final long serialVersionUID = -2648467009092432785L;

  private static final Logger logger = CentralFactoryLogging.getLogger(ManagedSession.class);

  public static final String TABLE_NAME = "sessionmanagement";
  public static final String COL_ABSOLUTE_SESSION_TIMEOUT = "absoluteSessionTimeout";


  @Column(name = "id", size = 100)
  private String id;
  @Column(name = "token", size = 200)
  private String token;
  @Column(name = "multipleSessionsAllowed")
  private boolean multipleSessionsAllowed;
  @Column(name = "forced")
  private boolean forced;
  @Column(name = "startDate")
  private Long startDate;
  @Column(name = "lastInteraction")
  private Long lastInteraction;
  @Column(name = COL_ABSOLUTE_SESSION_TIMEOUT)
  private Long absoluteSessionTimeout;
  
  @Column(name="currentPriviliges", type=ColumnType.BLOBBED_JAVAOBJECT)
  private List<ASessionPrivilege> currentPriviliges = new ArrayList<ASessionPrivilege>();

  private ReentrantLock priviligesLock;
  private ReentrantLock stateLock;
  
  @Column(name="role", size=100)
  private String role;

  @Column(name="state", size=20)
  private String state;


  public enum GUI_SESSION_STATE {
    TIMEOUT, ACTIVE
  }


  public ManagedSession() {
    priviligesLock = new ReentrantLock();
    stateLock = new ReentrantLock();
  }


  public ManagedSession(String id, String role, String token) {
    this(id, role, token, false, false);
  }

  public ManagedSession(String id, String role, String token, boolean multipleSessionsAllowed, boolean forced) {
    this(id, token, multipleSessionsAllowed, forced);
    this.role = role;
  }
  
  
  // TODO reduced timeout for unauthorized sessions
  public ManagedSession(String id, String token, boolean multipleSessionsAllowed, boolean forced) {
    this();
    
    if (id == null || id.length() == 0)
      throw new IllegalArgumentException("ID may not be null or empty");

    this.id = id;
        
    state = GUI_SESSION_STATE.ACTIVE.toString();
    
    this.startDate = System.currentTimeMillis();
    this.lastInteraction = System.currentTimeMillis();
    this.token = token;
    this.multipleSessionsAllowed = multipleSessionsAllowed;
    this.forced = forced;
    
    resetPointOfTimeout();
  }


  public String getID() {
    return id;
  }

  
  public void setID(String id) {
    this.id = id;
  }

  
  public String getToken() {
    return token;
  }

  
  public void setToken(String token) {
    this.token = token;
  }


  public boolean getMultipleSessionsAllowed() {
    return multipleSessionsAllowed;
  }


  public void setMultipleSessionsAllowed(boolean multipleSessionsAllowed) {
    this.multipleSessionsAllowed = multipleSessionsAllowed;
  }


  public boolean isForced() {
    return forced;
  }


  public void setForced(boolean forced) {
    this.forced = forced;
  }


  public Long getStartDate() {
    return startDate;
  }

  
  public void setStartDate(Long startDate) {
    this.startDate = startDate;
  }

  
  public Long getLastInteraction() {
    return lastInteraction;
  }

  
  public void setLastInteraction(Long lastInteraction) {
    this.lastInteraction = lastInteraction;
  }
  
  
  public Long getAbsoluteSessionTimeout() {
    return absoluteSessionTimeout;
  }

  
  public void setAbsoluteSessionTimeout(Long absoluteSessionTimeout) {
    this.absoluteSessionTimeout = absoluteSessionTimeout;
  }
  
  
  public String getRole() {
    return role;
  }

  
  public void setRole(String role) {
    this.role = role;
  }

  
  public void setState(String state) {
    this.state = state;
  }
  

  public boolean authenticate(String token) throws XFMG_SessionTimedOutException {
    return authenticate(token, true);
  }
  
  public boolean authenticate(String token, boolean countsAsInteraction) throws XFMG_SessionTimedOutException {
    try {
      if (!timeout()) {
        if (this.token.equals(token)) {
          if (countsAsInteraction) {
            this.resetPointOfTimeout();
          }
          return true;
        } else {
          return false; 
        }
      }
      throw new XFMG_SessionTimedOutException(id);
    } catch (IllegalStateException e) {
      //logger.error(e);
      throw new XFMG_SessionTimedOutException(id); //FIXME sollte hier nicht einfach die illegalstate exception weitergeworfen werden?
    } 
  }


  final void keepAlive() throws XFMG_SessionTimedOutException {
    logger.debug("keeping alive session");
    stateLock.lock();
    try {
      if (state.equals(GUI_SESSION_STATE.TIMEOUT.toString())) {
        throw new XFMG_SessionTimedOutException(id);
      }
      resetPointOfTimeout();
    } finally {
      stateLock.unlock();
    }
  }


  private void resetPointOfTimeout() {
    this.lastInteraction = System.currentTimeMillis();
    this.absoluteSessionTimeout = getLastInteraction() + SessionManagement.getSessionTimeout();
  }
  

  public final List<ASessionPrivilege> getCurrentPriviliges() {
    return currentPriviliges;
  }


  final void priviligeGranted(ASessionPrivilege privilige) {
    priviligesLock.lock();
    try {
      currentPriviliges.add(privilige);
    } finally {
      priviligesLock.unlock();
    }
  }


  final void priviligeRemoved(ASessionPrivilege privilige) {
    priviligesLock.lock();
    try {
      currentPriviliges.remove(privilige);
    } finally {
      priviligesLock.unlock();
    }
  }


  final void allPriviligesRemoved() {
    priviligesLock.lock();
    try {
      currentPriviliges.clear();
    } finally {
      priviligesLock.unlock();
    }
  }


  final boolean timeout() {
    logger.trace("time out request");
    stateLock.lock();
    try {
      if (state.equals(GUI_SESSION_STATE.TIMEOUT.toString())) {
        throw new IllegalStateException("Session is already timedout");
      }
      if (getAbsoluteSessionTimeout() > System.currentTimeMillis()) {
        return false;
      }
      state = GUI_SESSION_STATE.TIMEOUT.toString();
      currentPriviliges.clear();
      return true;
    } finally {
      stateLock.unlock();
    }
  }


  public final String getState() {
    stateLock.lock();
    try {
      return state;
    } finally {
      stateLock.unlock();
    }
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  private static class ManagedSessionReader implements ResultSetReader<ManagedSession> {

    public ManagedSession read(ResultSet rs) throws SQLException {
      ManagedSession session = new ManagedSession();
      fillByResultSet(session, rs);
      return session;
    }
  }

  private static ManagedSessionReader reader = new ManagedSessionReader();

  @Override
  public ResultSetReader<? extends ManagedSession> getReader() {
    return reader;
  }
 

  @Override
  public <U extends ManagedSession> void setAllFieldsFromData(U data) {
    ManagedSession cast = data;
    id = cast.id;
    startDate = cast.startDate;
    token = cast.token;
    lastInteraction = cast.lastInteraction;
    absoluteSessionTimeout = cast.absoluteSessionTimeout;
    state = cast.state;
    role = cast.role;
    currentPriviliges = cast.currentPriviliges;
    priviligesLock = new ReentrantLock(); // FIXME potentiell b�se!
    stateLock = new ReentrantLock();
    multipleSessionsAllowed = cast.multipleSessionsAllowed;
    forced = cast.forced;
  }


  private static final void fillByResultSet(ManagedSession newEntry, ResultSet rs) throws SQLException {
    newEntry.id = rs.getString("id");
    newEntry.token = rs.getString("token");
    newEntry.startDate = rs.getLong("startDate");
    newEntry.lastInteraction = rs.getLong("lastInteraction");
    newEntry.absoluteSessionTimeout = rs.getLong("absoluteSessionTimeout");
    newEntry.state = rs.getString("state");
    newEntry.role = rs.getString("role");
    newEntry.currentPriviliges =
        (List<ASessionPrivilege>) newEntry.readBlobbedJavaObjectFromResultSet(rs, "currentPriviliges");
    newEntry.priviligesLock = new ReentrantLock(); // FIXME potentiell b�se!
    newEntry.stateLock = new ReentrantLock();
    newEntry.multipleSessionsAllowed = rs.getBoolean("multipleSessionsAllowed");
    newEntry.forced = rs.getBoolean("forced");
  }


}
