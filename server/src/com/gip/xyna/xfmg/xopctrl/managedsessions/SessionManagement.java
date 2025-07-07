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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateSessionException;
import com.gip.xyna.xfmg.exceptions.XFMG_SESSION_AUTHENTICATION_FAILED;
import com.gip.xyna.xfmg.exceptions.XFMG_SessionTimedOutException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownSessionIDException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xopctrl.managedsessions.ManagedSession.GUI_SESSION_STATE;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.AChangeEvent;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.AChangeNotificationListener;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaUserCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.PasswordCreationUtils.EncryptionPhase;
import com.gip.xyna.xfmg.xopctrl.usermanagement.usercontext.UserContextEntryStorable;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;



public class SessionManagement extends FunctionGroup implements IPropertyChangeListener {

  static {
    addDependencies(SessionManagement.class,
                    new ArrayList<XynaFactoryPath>(Arrays.asList(new XynaFactoryPath[] {
                        new XynaFactoryPath(XynaProcessing.class, XynaProcessingODS.class),
                        new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryControl.class,
                                            DependencyRegister.class),
                        new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryManagementODS.class,
                                            Configuration.class)})));
  }

  public static final String DEFAULT_NAME = "GuiSessionManagement";
  private static final Logger logger = CentralFactoryLogging.getLogger(SessionManagement.class);
  private static final Long NO_ID = Long.valueOf(-1);

  private static final String SESSION_TIMEOUT_THREAD_NAME = "Session Timeout Thread";

  /*
   * ACHTUNG: bei änderungen auch in der dokumentation anpassen
   */
  public final static Long DEFAULT_GUI_SESSION_TIMEOUT_SECONDS = new Long(15 * 60);
  public final static Long DEFAULT_SESSION_DELETION_INTERVAL = new Long(30 * 60);

  private volatile boolean isShuttingDown = false;

  private PreparedQuery<ManagedSession> selectTimedoutSessions;
  private SessionDeletionRunnable deletionRunnable;

  private final Random random;

  private final AtomicLong changeNotificationListenerIdGenerator = new AtomicLong(1);
  private final Map<String, Map<Long, AChangeNotificationListener>> changeNotificationListeners =
      new HashMap<String, Map<Long, AChangeNotificationListener>>();

  private final ReentrantLock notificationListenerLock = new ReentrantLock();
  
  private final ConcurrentMap<String, List<SessionFinalizationHandler>> finalizationHandlers = new ConcurrentHashMap<String, List<SessionFinalizationHandler>>();

  //cached Properties
  private static Long sessionTimeout;
  private static Long deletionInterval;

  private ODS ods;

  //Zuordnung zwischen Sessions und Users, damit für gesperrte Benutzer die Session
  //beendet werden kann
  private SessionUserMap sessionUserMap = new SessionUserMap(); 


  public SessionManagement() throws XynaException {

    super();
    random = new Random();

  }


  @Override
  protected void init() throws XynaException {

    ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ods.registerStorable(ManagedSession.class);

    ODSConnection defaultConnection = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      selectTimedoutSessions =
          defaultConnection.prepareQuery(new Query<ManagedSession>("select * from " + ManagedSession.TABLE_NAME + " where "
              + ManagedSession.COL_ABSOLUTE_SESSION_TIMEOUT + " < ?", new ManagedSession().getReader()), true);
    } finally {
      defaultConnection.closeConnection();
    }

    deletionRunnable = new SessionDeletionRunnable(this);
    Thread deletionThread = new Thread(deletionRunnable);
    deletionThread.setName(SESSION_TIMEOUT_THREAD_NAME);
    deletionThread.start();

    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(SessionManagement.class, "SessionManagement XynaProperty init").
      after(DependencyRegister.class, XynaProperty.class).
      execAsync(new Runnable() { public void run() {
        
        XynaProperty.GUI_SESSION_TIMEOUT_SECONDS.registerDependency(DEFAULT_NAME);
        XynaProperty.GUI_SESSION_DELETION_INTERVAL.registerDependency(DEFAULT_NAME);
        
        sessionTimeout = getSessionTimeout();
        deletionInterval = getDeletionInterval();
        
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
                        .addPropertyChangeListener(SessionManagement.this);
        }
      });

  }


  @Override
  protected void shutdown() throws XynaException {
    isShuttingDown = true;
    deletionRunnable.shutdown();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  /**
   * nur ausgeben, wenn compile-schalter gesetzt ist
   * @return
   */
  private static String printSessionIdSafely(String sessionId) {
    if (Constants.SHOW_SESSION_ID_IN_DEBUG) {
      return sessionId + " ";
    } else {
      return "";
    }
  }

  
  @Deprecated
  public SessionCredentials getNewSession(User user, boolean force) throws PersistenceLayerException, XFMG_DuplicateSessionException {
    return createSession(new XynaUserCredentials(user.getName(), user.getPassword()), new Optional<String>(user.getRole()), force);
  }
  
  
  public SessionCredentials createSession(XynaUserCredentials credentials, Optional<String> roleName, boolean force) throws PersistenceLayerException, XFMG_DuplicateSessionException {
    Role role;
    if (roleName.isPresent()) {
      role = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getRole(roleName.get());
    } else {
      User user = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getUser(credentials.getUserName());
      role = (user != null) ? XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getRole(user.getRole()) : null;
      if (role == null) {
        // session for external user to be authorized later via authorizeSession(...)
        return createSession(credentials, roleName, force, true);
      }
    }

    return createSession(credentials, roleName, force, areMultipleSessionsAllowed(role));
  }

  public SessionCredentials createSession(XynaUserCredentials credentials, Optional<String> roleName, boolean force, boolean multipleSessionsAllowed) throws PersistenceLayerException, XFMG_DuplicateSessionException {
    
    if (!force && !multipleSessionsAllowed) {
      List<String> activeSessions = sessionUserMap.getSessionIds(credentials.getUserName());
      if (!activeSessions.isEmpty()) {
        throw new XFMG_DuplicateSessionException(credentials.getUserName());
      }
    }

    String randomId = getRandomSessionIdString();

    ODSConnection defaultCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      while (defaultCon.containsObject(new ManagedSession(randomId, null, null))) {
        randomId = getRandomSessionIdString();
      }
    } finally {
      defaultCon.closeConnection();
    }

    ManagedSession newSession;
    if (roleName.isPresent()) {
      newSession = new ManagedSession(randomId, roleName.get(), generateToken(credentials.getPassword(), randomId), multipleSessionsAllowed, force);
    } else {
      newSession = new ManagedSession(randomId, generateToken(credentials.getPassword(), randomId), multipleSessionsAllowed, force);
    }
    

    if (logger.isDebugEnabled()) {
      logger.debug("Creating new " + ManagedSession.class.getSimpleName() + " "
          + printSessionIdSafely(newSession.getID()) + "with timeout: " + newSession.getAbsoluteSessionTimeout());
    }
    defaultCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      defaultCon.persistObject(newSession);
      defaultCon.commit();
    } finally {
      defaultCon.closeConnection();
    }

    try {
      if (!multipleSessionsAllowed) {
        List<String> activeSessions = sessionUserMap.getSessionIds(credentials.getUserName());
        for (String sessionId : activeSessions) {
          quitSession(sessionId);
        }
      }
      sessionUserMap.addSession(randomId, credentials.getUserName());
    } catch (XFMG_UserIsLockedException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("could not create session ", e);
      }
      //sollte nur vorkommen, wenn sich ein Benutzer gerade einloggt, während er gesperrt wird
      quitSession(randomId);
    }
    
    return new SessionCredentials(newSession.getID(), newSession.getToken());
  }


  private boolean areMultipleSessionsAllowed(Role role) {
    boolean hasMultipleSessionRight = role.hasRight(Rights.MULTIPLE_SESSION_CREATION);
    return XynaProperty.CREATE_MULTIPLE_SESSIONS_IF_ALLOWED.get() && hasMultipleSessionRight;
  }


  // TODO save guard to ensure the creating user authorizes the session?
  public boolean authorizeSession(String sessionId, String token, String roleName) throws PersistenceLayerException, XFMG_UnknownSessionIDException, XFMG_SESSION_AUTHENTICATION_FAILED, XFMG_DuplicateSessionException {
    ManagedSession session = new ManagedSession(sessionId, null, null);
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      con.queryOneRow(session);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_UnknownSessionIDException(sessionId);
    } finally {
      con.closeConnection();
    }
    try {
      if (!session.authenticate(token)) {
        throw new XFMG_SESSION_AUTHENTICATION_FAILED(sessionId);
      }
    } catch (XFMG_SessionTimedOutException e) {
      quitSession(sessionId);
      throw new XFMG_SESSION_AUTHENTICATION_FAILED(sessionId);
    }
    if (session.getRole() != null) {
      if (session.getRole().equals(roleName)) {
        return true;
      } else {
        throw new XFMG_SESSION_AUTHENTICATION_FAILED(sessionId);
      }
    }
    session.setRole(roleName);

    // now that role is known, it can be decided whether session is going to be discarded, be unique for the user or exists among others for the same user
    Role role = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getRole(roleName);
    if (!areMultipleSessionsAllowed(role)) {
      session.setMultipleSessionsAllowed(false);
      String userName = sessionUserMap.getUser(sessionId);
      List<String> activeSessions = sessionUserMap.getSessionIds(userName);

      if (!session.isForced()) {
        if (activeSessions.size() > 1) {
          // session must be discarded, because it was created while another session for the user already existed, no multisession was allowed and no force was used to switch to a new one
          quitSession(sessionId);
          throw new XFMG_DuplicateSessionException(userName);
        }
      } else {
        // new session replaces the ones that are currently existing for the user
        for (String curSessionId : activeSessions) {
          if (!Objects.equals(sessionId, curSessionId)) {
            quitSession(curSessionId);
          }
        }
      }
    }

    con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      con.persistObject(session);
      con.commit();
    } finally {
      con.closeConnection();
    }

    return true;
  }

  public Role authenticateSession(String sessionId, String token) throws PersistenceLayerException,
    XFMG_UnknownSessionIDException, XFMG_SESSION_AUTHENTICATION_FAILED {
    return authenticateSession(sessionId, token, true);
  }

  public Role authenticateSession(String sessionId, String token, boolean countsAsInteraction) throws PersistenceLayerException,
                  XFMG_UnknownSessionIDException, XFMG_SESSION_AUTHENTICATION_FAILED {

    ManagedSession session = new ManagedSession(sessionId, null, null);
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      con.queryOneRow(session);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_UnknownSessionIDException(sessionId);
    } finally {
      con.closeConnection();
    }

    try {
      if (session.authenticate(token, countsAsInteraction)) {
        // store session in case the authenticate changed states
        con = ods.openConnection(ODSConnectionType.DEFAULT);
        try {
          con.persistObject(session);
          con.commit();
        } finally {
          con.closeConnection();
        }
        if (session.getRole() == null) {
          throw new XFMG_UnknownSessionIDException(sessionId);
        }
        return this.getRole(session.getID());
      } else {
        if (session.getState().equals(ManagedSession.GUI_SESSION_STATE.TIMEOUT.toString())) {
          quitSession(session.getID());
        }
        throw new XFMG_UnknownSessionIDException(sessionId);
      }
    } catch (XFMG_SessionTimedOutException e) {
      quitSession(sessionId);
      throw new XFMG_SESSION_AUTHENTICATION_FAILED(sessionId);
    }
  }


  boolean isShuttingDown() {
    return isShuttingDown;
  }


  public void quitSession(String sessionId) throws PersistenceLayerException {

    if (logger.isDebugEnabled()) {
      logger.debug("Quitting session " + printSessionIdSafely(sessionId));
    }
    ODSConnection defaultConnection = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      ManagedSession session = new ManagedSession(sessionId, null, null);

      if (!defaultConnection.containsObject(session)) {
        if (logger.isDebugEnabled()) {
          logger.debug("Session " + printSessionIdSafely(sessionId)
                          + "could not be found while trying to quit, doing nothing");
        }
      } else {
        Collection<ManagedSession> col = new ArrayList<ManagedSession>();
        col.add(session);
        try {
          defaultConnection.delete(col);
          defaultConnection.commit();
        } finally {
          handleSessionFinalization(col);
        }
      }
      
      sessionUserMap.removeSession(sessionId);
      
      notificationListenerLock.lock();
      try {
        changeNotificationListeners.remove(session.getID());
      } finally {
        notificationListenerLock.unlock();
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Successfully quit session " + printSessionIdSafely(sessionId));
      }
    } finally {
      defaultConnection.closeConnection();
    }
    
    

  }

  /**
   * Beendet alle (bekannten) Sessions für einen User. <br>
   * 
   * TODO In folgenden Fällen kann es vorkommen, dass nicht alle Sessions beendet werden:
   * - nach einem Factory-Neustart, falls die Sessions persistiert werden
   * - im Cluster-Betrieb (wenn der User auf einem andern Knoten gesperrt wird, als er eingeloggt ist)
   * 
   * @param userName
   * @throws PersistenceLayerException
   */
  public void quitSessionsForUser(String userName) throws PersistenceLayerException {
    for (String sessionId : sessionUserMap.getSessionIds(userName)) {
      quitSession(sessionId);
    }
  }

  /**
   * Markiert den User als gesperrt bzw. hebt die Markierung auf,
   * falls der User entsperrt wird.
   */
  public void lockUser(User user) {
    sessionUserMap.lockUser(user);
  }
  
  /**
   * @return true if the session was kept alive and false otherwise
   * @throws PersistenceLayerException
   */
  public boolean keepAlive(String sessionId) throws PersistenceLayerException {

    ManagedSession session = new ManagedSession(sessionId, null, null);

    ODSConnection con = ods.openConnection();
    try {
      con.queryOneRow(session);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Cannot keep " + ManagedSession.class.getSimpleName() + " alive, SessionID "
                        + printSessionIdSafely(sessionId) + "does not exist");
      }
      return false;
    } finally {
      con.closeConnection();
    }
    try {
      session.keepAlive();
    } catch (XFMG_SessionTimedOutException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Cannot keep " + ManagedSession.class.getSimpleName() + " alive, Session "
                        + printSessionIdSafely(sessionId) + "timed out.");
      }
      return false;
    }

    con = ods.openConnection();
    try {
      con.persistObject(session);
      con.commit();
    } finally {
      con.closeConnection();
    }

    return true;

  }


  public SessionDetails getSessionDetails(String sessionId) throws PersistenceLayerException,
                  XFMG_UnknownSessionIDException {
    ManagedSession session = new ManagedSession(sessionId, null, null);

    ODSConnection con = ods.openConnection();
    try {
      con.queryOneRow(session);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_UnknownSessionIDException(sessionId, e);
    } finally {
      con.closeConnection();
    }

    Role role = this.getRole(sessionId);
    if (role == null) {
      throw new RuntimeException("Role for session " + printSessionIdSafely(sessionId) + "could not be resolved");
    }
    List<String> allRights = new ArrayList<String>();
    allRights.addAll(role.getRightsAsList());
    if (role.getScopedRights() != null) {
      allRights.addAll(role.getScopedRights());
    }
    return new SessionDetails(session.getID(), role.getName(), allRights, session.getStartDate(),
                              session.getLastInteraction(), session.getMultipleSessionsAllowed(), session.isForced());
  }


  public boolean releaseSessionPriviliges(String sessionId) throws PersistenceLayerException {
    return releaseSessionPriviliges(sessionId, null);
  }


  public boolean releaseSessionPriviliges(String sessionId, ASessionPrivilege requestedPriviliges)
                  throws PersistenceLayerException {

    ODSConnection con = ods.openConnection();
    try {
      ManagedSession targetSession = new ManagedSession(sessionId, null, null);
      con.queryOneRow(targetSession);
      if (requestedPriviliges != null) {
        if (logger.isDebugEnabled()) {
          logger.debug("Successfully released session privilige for SessionID " + printSessionIdSafely(sessionId));
        }
        targetSession.priviligeRemoved(requestedPriviliges);
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Successfully released all session priviliges for SessionID " + printSessionIdSafely(sessionId));
        }
        targetSession.allPriviligesRemoved();
      }
      con.persistObject(targetSession);
      return true;
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      logger.warn("Cannot release session priviliges because SessionID " + printSessionIdSafely(sessionId)
                      + "is unkonwn");
      return false;
    } finally {
      con.closeConnection();
    }
  }


  public boolean requestSessionPriviliges(String sessionId, ASessionPrivilege requestedPriviliges)
                  throws PersistenceLayerException {

    ODSConnection con = ods.openConnection();
    try {

      ManagedSession targetSession = new ManagedSession(sessionId, null, null);
      con.queryOneRow(targetSession);

      for (ManagedSession session : con.loadCollection(ManagedSession.class)) {

        if (session.getID().equals(targetSession.getID())) {
          continue;
        }

        List<ASessionPrivilege> priviliges = session.getCurrentPriviliges();
        if (priviliges == null || priviliges.size() == 0) {
          continue;
        }

        for (ASessionPrivilege currentPrivilige : priviliges) {
          if (currentPrivilige.isInConflictWith(requestedPriviliges)) {
            return false;
          }
        }


      }

      targetSession.priviligeGranted(requestedPriviliges);
      con.persistObject(targetSession);
      con.commit();
      return true;
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      logger.warn("Cannot request session priviliges because session does not exist");
      return false;
    } finally {
      con.closeConnection();
    }

  }
  

  /**
   * @return The internal ID by which the listener can be removed later or -1 if no session corresponding to the
   *         specified ID has been registered before.
   * @throws PersistenceLayerException
   */
  public Long signupSessionForNotification(String sessionId, AChangeNotificationListener listener)
                  throws PersistenceLayerException {

    //if (sessionId == null)
    //  throw new IllegalArgumentException("Cannot sign up a listener for ID 'null'");
    if (listener == null) {
      throw new IllegalArgumentException("Cannot sign up listener 'null'");
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Signing up " + listener.getClass().getSimpleName());
    }
    ODSConnection con = ods.openConnection();
    try {
      ManagedSession session = new ManagedSession(sessionId, null, null);
      if (con.containsObject(session)) {
        if (logger.isDebugEnabled()) {
          logger.debug("Found session " + printSessionIdSafely(sessionId) + ", adding listener");
        }
        notificationListenerLock.lock();
        try {
          Map<Long, AChangeNotificationListener> entry = null;
          if (!changeNotificationListeners.containsKey(sessionId)) {
            changeNotificationListeners.put(sessionId, new HashMap<Long, AChangeNotificationListener>());
          }
          entry = changeNotificationListeners.get(sessionId);

          if (entry == null) {
            if (logger.isDebugEnabled()) {
              logger.debug("Could not find specified session " + printSessionIdSafely(sessionId) + ", doing nothing");
            }
            return NO_ID;
          }
          long usedListenerID = changeNotificationListenerIdGenerator.getAndIncrement();
          entry.put(usedListenerID, listener);
          return usedListenerID;
        } finally {
          notificationListenerLock.unlock();
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Could not find specified session " + printSessionIdSafely(sessionId) + ", doing nothing");
        }
        return NO_ID;
      }
    } finally {
      con.closeConnection();
    }
  }


  public void removeNotificationListener(String sessionId, Long listenerId) throws PersistenceLayerException {

    if (listenerId == null)
      throw new IllegalArgumentException("Cannot remove listener with ID 'null'");

    ODSConnection con = ods.openConnection();
    try {
      ManagedSession session = new ManagedSession(sessionId, null, null);
      if (con.containsObject(session)) {
        if (logger.isDebugEnabled()) {
          logger.debug("Found session for sessionID " + printSessionIdSafely(sessionId)
                          + ", removing listener with ID " + listenerId);
        }
        notificationListenerLock.lock();
        try {
          if (!changeNotificationListeners.containsKey(sessionId)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Could not find a session with listener for sessionID " + printSessionIdSafely(sessionId)
                              + ", doing nothing");
            }
            return;
          }
          Map<Long, AChangeNotificationListener> entry = changeNotificationListeners.get(sessionId);
          if (!entry.containsKey(listenerId)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Could not find a listener for ID " + listenerId + ", doing nothing");
            }
            return;
          }
          entry.remove(listenerId);
        } finally {
          notificationListenerLock.unlock();
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Could not find session for SessionID " + printSessionIdSafely(sessionId) + ", doing nothing");
        }
      }
    } finally {
      con.closeConnection();
    }

  }


  public void performChangeNotification(AChangeEvent event) throws PersistenceLayerException {
    notificationListenerLock.lock();
    try {
      // FIXME deadlock-gefahr! connection außerhalb des locks holen und lowlevel connectivity sicherstellen!
      ODSConnection con = ods.openConnection();
      try {
        for (ManagedSession session : con.loadCollection(ManagedSession.class)) {
          if (changeNotificationListeners.containsKey(session.getID())) {
            Iterator<AChangeNotificationListener> iter = changeNotificationListeners.get(session.getID()).values()
                            .iterator();
            while (iter.hasNext()) {
              AChangeNotificationListener next = iter.next();
              if (next.matches(event)) {
                // try {
                next.onChange(event);
                //  } catch (XynaException e) {
                //    logger.error("Could not notify change listener for SessionID " + printSessionIdSafely(session.getID()), e);
                //  }
                iter.remove(); // you need to sign up again for that change for further notification
              }
            }
          }
        }
      } finally {
        con.closeConnection();
      }
    } finally {
      notificationListenerLock.unlock();
    }
  }


  public Role getRole(String sessionId) throws PersistenceLayerException, XFMG_UnknownSessionIDException {
    ODSConnection con = ods.openConnection();
    try {
      con.ensurePersistenceLayerConnectivity(new StorableClassList(Role.class, ManagedSession.class));
      ManagedSession session = new ManagedSession(sessionId, null, null);
      con.queryOneRow(session);

      return XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
                      .resolveRole(session.getRole(), UserManagement.PREDEFINED_LOCALDOMAIN_NAME, con);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_UnknownSessionIDException(sessionId);
    } finally {
      con.closeConnection();
    }
  }



  /**
   * Access to this should be synchronized TODO wieso? should = must? wieso nicht methode synchronized machen?
   */
  private String getRandomSessionIdString() {
    StringBuilder temp = new StringBuilder();
    StringBuilder idBuffer = new StringBuilder();
    //36.15 | 36: random int ; 15 SystemTime with leading zeros 
    for (int i = 0; i < 36; i++) {
       int randomNumber = random.nextInt(10); // creates a Number between 0 and 9
       temp.append(randomNumber);
    }
    idBuffer.append(temp.toString());
    idBuffer.append(".");
    temp = new StringBuilder();
    temp.append(System.currentTimeMillis());
    while (temp.length() < 15) {
      temp.insert(0, "0");
    }
    idBuffer.append(temp.toString());
    return idBuffer.toString();
  }


  public static Long getSessionTimeout() {
    if (sessionTimeout != null) {
      return sessionTimeout;
    }
    
    sessionTimeout = XynaProperty.GUI_SESSION_TIMEOUT_SECONDS.getMillis();
    return sessionTimeout;
  }


  public static Long getDeletionInterval() {
    if (deletionInterval != null) {
      return deletionInterval;
    }
    
    deletionInterval = XynaProperty.GUI_SESSION_DELETION_INTERVAL.getMillis();
    return deletionInterval;
  }


  private String generateToken(String... args) {
    StringBuilder temp = new StringBuilder();
    for (String string : args) {
      temp.append(string);
    }
    return User.generateHash(temp.toString(), EncryptionPhase.LOGIN);
  }


  void deleteTimedoutSessions() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      List<ManagedSession> timedoutSessions = con.query(selectTimedoutSessions, new Parameter(System.currentTimeMillis()), -1);
      con.delete(timedoutSessions);
      con.commit();
      if (timedoutSessions.size() > 0) {
        for (ManagedSession session : timedoutSessions) {
          sessionUserMap.removeSession(session.getID());
        }
        handleSessionFinalization(con);
      }
    } finally {
      con.closeConnection();
    }
  }


  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> watched = new ArrayList<String>();
    watched.add(XynaProperty.GUI_SESSION_TIMEOUT_SECONDS.getPropertyName());
    watched.add(XynaProperty.GUI_SESSION_DELETION_INTERVAL.getPropertyName());
    return watched;
  }


  public void propertyChanged() {
    logger.debug("propertyChanged is called");
    
    long millis = XynaProperty.GUI_SESSION_TIMEOUT_SECONDS.getMillis();
    if (millis != sessionTimeout) {
      logger.debug("change to " + XynaProperty.GUI_SESSION_TIMEOUT_SECONDS + " detected.");
      try {
        updateSessionTimeout(millis);
        return;
      } catch (PersistenceLayerException e) {
        logger.warn("Error while changing session timeout", e);
      }
    }


    millis = XynaProperty.GUI_SESSION_DELETION_INTERVAL.getMillis();
    updateDeletionInterval(millis);
  }


  private void updateSessionTimeout(long newTimeoutInMillis) throws PersistenceLayerException {
    sessionTimeout = newTimeoutInMillis;
    // load everyone and set absoluteTimeout to lastInteraction + newTimeout    
    Collection<ManagedSession> loaded = null;
    ODSConnection con = ods.openConnection();
    try {
      loaded = con.loadCollection(ManagedSession.class);
    } finally {
      con.closeConnection();
    }

    if (loaded == null || loaded.size() == 0) {
      return;
    }

    // check if timedOut and gather 2 Collections, 1 to persist 1 to delete
    Collection<ManagedSession> toPersist = new ArrayList<ManagedSession>();
    Collection<ManagedSession> toDelete = new ArrayList<ManagedSession>();
    for (ManagedSession session : loaded) {
      session.setAbsoluteSessionTimeout(session.getLastInteraction() + newTimeoutInMillis);
      try {
        if (session.timeout()) {
          toDelete.add(session);
          sessionUserMap.removeSession(session.getID());
        } else {
          toPersist.add(session);
        }
      } catch (IllegalStateException ise) {
        toDelete.add(session);
      }
    }

    con = ods.openConnection();
    try {
      con.delete(toDelete);
      con.persistCollection(toPersist);
      con.commit();
    } finally {
      con.closeConnection();
    }
    handleSessionFinalization(toDelete);
  }


  private void updateDeletionInterval(long newInterval) {
    //should we interrupt the old one?
    deletionInterval = newInterval;
    deletionRunnable.setInterval(newInterval);
  }
  
  
  public boolean isSessionAlive(String sessionId) throws PersistenceLayerException {
    ManagedSession session = new ManagedSession(sessionId, null, null);

    ODSConnection con = ods.openConnection();
    try {
      con.queryOneRow(session);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Cannot keep " + ManagedSession.class.getSimpleName() + " alive, SessionID "
                        + printSessionIdSafely(sessionId) + "does not exist");
      }
      return false;
    } finally {
      con.closeConnection();
    }
    
    if (GUI_SESSION_STATE.ACTIVE.toString().equals(session.getState()) &&
        session.getAbsoluteSessionTimeout() > System.currentTimeMillis()) {
      return true;
    } else {
      return false;
    }
  }
  
  
  public String resolveSessionToUser(String sessionId) {
    return sessionUserMap.getUser(sessionId);
  }
  
  
  public void removeSessionTerminationHandler(String sessionId, SessionFinalizationHandler handler) {
    try {
      if (sessionId == null) {
        return;
      }
      while (true) {
        List<SessionFinalizationHandler> oldHandlers = finalizationHandlers.get(sessionId);
        if (oldHandlers == null) {
          logger.trace("no finalization handlers found for session " + sessionId);
          return;
        }
        List<SessionFinalizationHandler> newHandlers = new ArrayList<SessionFinalizationHandler>(oldHandlers);  
        boolean didRemove = newHandlers.remove(handler);
        if (!didRemove) {
          logger.trace("removed no finalization handler for session " + sessionId);
          return;
        }
        logger.trace("removed finalization handler for session " + sessionId);
        
        while (didRemove) {
          didRemove = newHandlers.remove(handler);
        }
  
        boolean sessionHandlersUnchangedByOtherThread = finalizationHandlers.replace(sessionId, oldHandlers, newHandlers);
        if (sessionHandlersUnchangedByOtherThread) {
          return;
        }
      }
    } catch (Throwable t) {
      logger.error("Error trying to remove SessionTerminationHandler: ", t);
    }
  }
  
  
  public void addSessionTerminationHandler(String sessionId, SessionFinalizationHandler handler) {
    while (true) {
      List<SessionFinalizationHandler> handlers = finalizationHandlers.get(sessionId);
      List<SessionFinalizationHandler> newHandlers;
      if (handlers == null) {
        newHandlers = new ArrayList<SessionFinalizationHandler>();
      } else {
        newHandlers = new ArrayList<SessionFinalizationHandler>(handlers);
      }
      newHandlers.add(handler);
      if (handlers == null) {
        if (finalizationHandlers.putIfAbsent(sessionId, newHandlers) == null) {
          return;
        }
      } else {
        if (finalizationHandlers.replace(sessionId, handlers, newHandlers)) {
          return;
        }
      }
    }
  }


  private void handleSessionFinalization(ODSConnection con) throws PersistenceLayerException {
    ManagedSession session = new ManagedSession();
    for (Entry<String, List<SessionFinalizationHandler>> entry : finalizationHandlers.entrySet()) {
      session.setID(entry.getKey());
      if (!con.containsObject(session)) {
        for (SessionFinalizationHandler handler : entry.getValue()) {
          handler.handleSessionFinalization(entry.getKey());
        }
        finalizationHandlers.remove(entry.getKey());
      }
    }
  }
  
  
  private void handleSessionFinalization(Collection<ManagedSession> sessionForFinalization) {
    for (ManagedSession managedSession : sessionForFinalization) {
      List<SessionFinalizationHandler> handlers = finalizationHandlers.get(managedSession.getID());
      if (handlers != null) {
        for (SessionFinalizationHandler handler : handlers) {
          handler.handleSessionFinalization(managedSession.getID());
        }
        finalizationHandlers.remove(managedSession.getID());
      }
    }
  }


  private static class SessionDeletionRunnable implements Runnable {

    private Long deletionInterval = getDeletionInterval();
    private volatile boolean running;

    private final SessionManagement sessionManagement;

    private final Object waitObject = new Object();


    public SessionDeletionRunnable(SessionManagement sessionManagement) {
      this.sessionManagement = sessionManagement;
    }


    public void shutdown() {
      synchronized(waitObject) {
        this.running = false;
        waitObject.notify();
      }
    }


    public void setInterval(long deletionInterval) {
      synchronized (waitObject) {
        this.deletionInterval = deletionInterval;
        waitObject.notify();
      }
    }


    public void run() {

      synchronized (waitObject) {
        running = true;
        while (running) {
          try {
            sessionManagement.deleteTimedoutSessions();
          } catch (Throwable e) {
            Department.handleThrowable(e);
            logger.warn("Some sessions could not be deleted", e);
          }
          try {
            // if this should happen to wake up too early it does not really matter as those events are
            // expected to be very rare (if they even exist)
            waitObject.wait(deletionInterval);
          } catch (InterruptedException e) {
            logger.warn("Deletion-Thread interrupted with Thread-interrupt and running = " + running, e);
          }
        }
      }

    }

  }


  public List<SessionBasedUserContextValue> getUserContextValuesBySession(String sessionId)
      throws PersistenceLayerException {
    String userName = resolveSessionToUser(sessionId);
    if (userName == null) {
      throw new RuntimeException("Failed to determine user for current session.");
    }
    List<UserContextEntryStorable> resultAsObjects =
        XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
            .getUserContextValues(userName);
    List<SessionBasedUserContextValue> result = new ArrayList<SessionBasedUserContextValue>();
    if (resultAsObjects != null) {
      for (UserContextEntryStorable uces : resultAsObjects) {
        result.add(new SessionBasedUserContextValue(uces));
      }
    }
    return result;
  }


  public void setUserContextValueBySession(String sessionId, String key, String value) throws PersistenceLayerException {
    String userName = resolveSessionToUser(sessionId);
    if (userName == null) {
      throw new RuntimeException("Failed to determine user for current session.");
    }
    XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
        .setUserContextValue(userName, key, value);
  }


  public void resetUserContextValuesBySession(String sessionId) throws PersistenceLayerException {
    String userName = resolveSessionToUser(sessionId);
    if (userName == null) {
      throw new RuntimeException("Failed to determine user for current session.");
    }
    XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
        .resetUserContextValues(userName);
  }


  public static interface SessionFinalizationHandler {
    
    public void handleSessionFinalization(String sessionId); // TODO reason? (logout | timeout)
    
  }

}
