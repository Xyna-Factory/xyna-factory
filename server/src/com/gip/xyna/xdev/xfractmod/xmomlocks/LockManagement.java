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
package com.gip.xyna.xdev.xfractmod.xmomlocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.concurrent.HashParallelReentrantLock;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_RefactoringConflict;
import com.gip.xyna.xdev.xfractmod.xmomlocks.PublicationInformation.PublicationEntry;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionDetails;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement.SessionFinalizationHandler;
import com.gip.xyna.xmcp.xguisupport.messagebus.Message;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBusImpl;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBusManagement;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBusSubscriptionSession;
import com.gip.xyna.xmcp.xguisupport.messagebus.PredefinedMessagePath;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


public class LockManagement extends FunctionGroup implements LockManagementPortal, SessionFinalizationHandler { // rename or separate Autosaves?

  private static Logger _logger = CentralFactoryLogging.getLogger(LockManagement.class);
  
  public final static String DEFAULT_NAME = "Lock Management";
  
  public final static String MESSAGE_PAYLOAD_KEY_DOCUMENT = "Document";
  public final static String MESSAGE_PAYLOAD_KEY_PUBLICATION_ID = "AutosaveCounter";
  
  private final static String AUTOSAVE_NOT_LOCKED_MSG = "Autosave object not locked";
  
  private final static List<SerializablePair<String, String>> emptyPayload = new ArrayList<SerializablePair<String,String>>();

  private final Map<Path, XMOMLock> locksPerPath = new HashMap<Path, XMOMLock>();
  private final Map<String, Map<Path, XMOMLock>> locksPerSession = new HashMap<String, Map<Path, XMOMLock>>();
  private final HashParallelReentrantLock<Path> locksLock = new HashParallelReentrantLock<Path>();
  
  private final ReentrantReadWriteLock autosaveGuardLock = new ReentrantReadWriteLock();
  private final Map<String, AutosaveGuard> autosaveGuards = new HashMap<String, LockManagement.AutosaveGuard>();
  
  
  public LockManagement() throws XynaException {
    super();
  }
  
  
  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  protected void init() throws XynaException {
    // ntbd
  }

  @Override
  protected void shutdown() throws XynaException {
    // ntbd
  }
  
  
  public boolean lockXMOM(String sessionId, String creator, Path path, String type) throws XynaException {
    locksLock.lock(path);
    try {
      XMOMLock lock = locksPerPath.get(path);
      if (lock == null) {
        lock = new XMOMLock(path, type, creator, sessionId);
        locksPerPath.put(path, lock);
        Map<Path, XMOMLock> sessionLocks = locksPerSession.get(sessionId);
        if (sessionLocks == null) {
          sessionLocks = new HashMap<Path, XMOMLock>();
          locksPerSession.put(sessionId, sessionLocks);
          registerAsListenerForSession(sessionId);
        }
        sessionLocks.put(path, lock);
        lock.setAssociatedLockMessage(sendLockMessage(sessionId, creator, path, type));
        return true;
      } else { // jemand versucht Lock zu holen und Lock existiert schon
        if (lock.getUser() == null || lock.getUser().equals(creator)) {
          if (lock.getSessionId().equals(sessionId)) {
            return true; // reentrant FIXME das führt zu fälschlichen Fehlermeldungen "Autosave not locked", wenn man zwei separate threads mit der gleichen sessionid hat
          } else {
            SessionDetails lockingSession = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement().getSessionDetails(lock.getSessionId());
            SessionDetails newSession = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement().getSessionDetails(sessionId);
            if (lockingSession.areMultipleSessionsAllowed() || newSession.areMultipleSessionsAllowed()) {
              return false;
            }

            transferLock(sessionId, lock);
            return true;
          }
        } else {
          return false;
        }
      }
    } finally {
      locksLock.unlock(path);
    }
  }
  
  
  public boolean unlockXMOM(String sessionId, String creator, Path path, String type) throws XynaException {
    locksLock.lock(path);
    try {
      XMOMLock lock = locksPerPath.get(path);
      if (lock == null || (lock.getUser() != null && !lock.getUser().equals(creator))) {
        return false;
      } else {
        locksPerPath.remove(path);
        Map<Path, XMOMLock> sessionLocks = locksPerSession.get(lock.getSessionId());
        if (sessionLocks != null) {
          sessionLocks.remove(path);
        }
        deleteLockAndInsertUnlockMessage(creator, path, type, lock.getAssociatedLockMessage());
        PublicationInformation publications = lock.getPublicationInformation();
        if (publications.hasPuplications()) {
          replaceAutosaveWithRollback(lock.getPath(), lock.getType(), lock.getUser(), publications.getCurrentMessageId());
          publications.clearPuplications();
        }
        return true;
      }
    } finally {
      locksLock.unlock(path);
    }
  }


  public String getLockingUser(Path path) {
    XMOMLock lock = locksPerPath.get(path);
    if (lock == null) {
      return null;
    }

    return lock.getUser();
  }


  public void publishXMOM(String sessionId, String creator, Path path, String type, String payload, Long autosaveCounter) throws XynaException {
    locksLock.lock(path);
    try {
      XMOMLock lock;
      try {
        lock = getLockIfHoldByUser(sessionId, path, creator);
        PublicationInformation publications = lock.getPublicationInformation();
        if (autosaveCounter < 0) {
          if (publications.hasPuplications()) {
            removePreviousAutosaveMessage(path, type, publications.getCurrentMessageId());
            publications.clearPuplications();
            publishRollback(path, type, creator);
          }
        } else {
          if (publications.hasPuplications()) {
            removePreviousAutosaveMessage(path, type, publications.getCurrentMessageId());
            if (publications.isRevert(autosaveCounter) || publications.isRedo(autosaveCounter)) {
              PublicationEntry previousPuplication = publications.getPublicationEntry(autosaveCounter);
              publishPublication(sessionId, path, type, creator, previousPuplication.getPayload(), previousPuplication.getPublicationId(), publications);
            } else {
              publishPublication(sessionId, path, type, creator, payload, autosaveCounter, publications);
            }
          } else {
            publishPublication(sessionId, path, type, creator, payload, autosaveCounter, publications);
          }
        }
      } catch (RuntimeException re) {
        // gui wishes for autosave rollbacks on unlocked objects to succeed and distribute
        if (!lockExists(path) &&
            autosaveCounter < 0) {
          publishRollback(path, type, creator);
        } else {
          throw re;
        }
      }
      
    } finally {
      locksLock.unlock(path);
    }
  }
  
  
  public void propagateXMOMSave(String sessionId, String creator, Path path, String type, String xml) throws XynaException {
    locksLock.lock(path);
    try {
      XMOMLock lock = getLockIfHoldByUser(sessionId, path, creator);
      PublicationInformation publications = lock.getPublicationInformation();
      if (publications.hasPuplications()) {
        removePreviousAutosaveMessage(path, type, publications.getCurrentMessageId());
        publications.clearPuplications();
      }
      PredefinedMessagePath saveMessagePath = PredefinedMessagePath.XYNA_MODELLER_SAVE;
      XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement()
                 .publish(new MessageInputParameter(saveMessagePath.getProduct(),
                                                    saveMessagePath.getContext(),
                                                    createCorrelation(path, type),
                                                    creator,
                                                    createMessagePayload(xml, null),
                                                    saveMessagePath.isPersistent()));
    } finally {
      locksLock.unlock(path);
    }
  }
  
  
  public void propagateXMOMDelete(String sessionId, String creator, Path path, String type) throws XynaException {
    locksLock.lock(path);
    try {
      XMOMLock lock = getLockIfHoldByUser(sessionId, path, creator);
      PublicationInformation publications = lock.getPublicationInformation();
      if (publications.hasPuplications()) {
        removePreviousAutosaveMessage(path, type, publications.getCurrentMessageId());
        publications.clearPuplications();
      }
      PredefinedMessagePath deleteMessagePath = PredefinedMessagePath.XYNA_MODELLER_DELETE;
      XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement()
                 .publish(new MessageInputParameter(deleteMessagePath.getProduct(),
                                                    deleteMessagePath.getContext(),
                                                    createCorrelation(path, type), creator,
                                                    emptyPayload, deleteMessagePath.isPersistent()));
    } finally {
      locksLock.unlock(path); 
    }
  }
  
  
  public boolean registerAutosaveGuard(String sessionId, AutosaveFilter filter) throws XynaException {
    AutosaveGuard guard = new AutosaveGuard(filter);
    autosaveGuardLock.writeLock().lock();
    try {
      autosaveGuards.put(sessionId, guard);
      boolean success = guard.activate();
      if (!success) {
        autosaveGuards.remove(sessionId);
      }
      return success;
    } finally {
      autosaveGuardLock.writeLock().unlock();
    }
  }
  
  
  public void unregisterAutosaveGuard(String sessionId) {
    autosaveGuardLock.writeLock().lock();
    try {
      autosaveGuards.remove(sessionId);
    } finally {
      autosaveGuardLock.writeLock().unlock();
    }
  }
  
  
  public synchronized void handleSessionFinalization(String sessionId) {
    Map<Path, XMOMLock> sessionLocks = locksPerSession.get(sessionId);
    if (sessionLocks != null) {
      for (XMOMLock lock : sessionLocks.values()) {
        locksLock.lock(lock.getPath());
        try {
          if (locksPerSession.get(sessionId) != null && locksPerSession.get(sessionId).containsValue(lock)) { // double check
            try {
              locksPerPath.remove(lock.getPath());
              deleteLockAndInsertUnlockMessage(lock.getUser(), lock.getPath(), lock.getType(), lock.getAssociatedLockMessage());
              PublicationInformation publications = lock.getPublicationInformation();
              if (publications.hasPuplications()) {
                replaceAutosaveWithRollback(lock.getPath(), lock.getType(), lock.getUser(), publications.getCurrentMessageId());
                publications.clearPuplications();
              }
            } catch (XynaException e) {
              Department.handleThrowable(e);
              logger.warn("Error during session finalization", e);
            }
          }
        } finally {
          locksLock.unlock(lock.getPath());
        }
      }
    }
    locksPerSession.remove(sessionId);
    unregisterAutosaveGuard(sessionId);
  }
  
  
  private XMOMLock getLockIfHoldByUser(String sessionId, Path path, String user) {
    XMOMLock lock = locksPerPath.get(path);
    if (lock == null) {
      throw new RuntimeException(AUTOSAVE_NOT_LOCKED_MSG);
    } else {
      if (!lock.getUser().equals(user)) {
        throw new RuntimeException("Object locked from different user: " + user);
      } else {
        if (!lock.getSessionId().equals(sessionId)) {
          transferLock(sessionId, lock);
        }
        return lock;
      }
    }
  }

  
  private boolean lockExists(Path path) {
    return locksPerPath.containsKey(path);
  }
  

  public static String createCorrelation(Path path, String type) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    RuntimeContext context = revisionManagement.getRuntimeContext(path.getRevision());
    if (context instanceof Application) { // special revisions are ok though
      // we expect this to throw
      revisionManagement.getWorkspace(path.getRevision());
    }
    //String contextname = context.getName().replaceAll("[^_a-zA-Z0-9]", "_");
    String contextname = context.getName(); //.replaceAll("[^_a-zA-Z0-9]", "_");
    _logger.warn("### contextname for correlationid: " + contextname);
    return "CORRID__" + type + "-" + path.getPath() + "-WS:\"" + context.getName()+"\"";
    //String ret = type + "-" + path.getPath() + "-WS:\"" + mask(contextname)+"\"";
    //_logger.warn("### created corr id: " + ret);
    //return ret;
  }
  
  public static String createMaskedCorrelation(Path path, String type) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    String ret = mask(createCorrelation(path, type));
    _logger.warn("### created masked corr id: " + ret);
    return ret;
  }
  
  public static String mask(String input) {
    String tmp = input.replace("\\", "\\\\");
    tmp = tmp.replaceAll("[(]", "\\\\(");
    tmp = tmp.replaceAll("[)]", "\\\\)");
    tmp = tmp.replaceAll("[.]", "\\\\.");
    return tmp;
  }
  
  public static String unmask(String input) {
    String[] parts = input.split("\\\\\\\\", -1);
    String ret = "";
    int i = 0;
    for (String str : parts) {
      ret += str.replace("\\", "");
      if (i < parts.length - 1) {
        ret += "\\";
      }
      i++;
    }
    return ret;
  }
  

  private static Pair<String, Path> splitCorrelation(String correlation) {
    String[] splitted = correlation.split("-", 3);
    String type = splitted[0];
    String path = splitted[1];
    RevisionManagement revisionManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    String ws = splitted[2].substring(4, splitted[2].length() - 1);
    Long revision;
    try {
      revision = revisionManagement.getRevision(new Workspace(ws));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException("Workspace \"" + ws + "\" unknown.", e);
    }

    return Pair.of(type, new Path(path, revision));
  }
  
  
  private void transferLock(String newSessionId, XMOMLock lock) {
    Map<Path, XMOMLock> oldSessionLocks = locksPerSession.get(lock.getSessionId());
    if (oldSessionLocks != null) {
      oldSessionLocks.remove(lock.getPath());
    }
    lock.setSessionId(newSessionId);
    registerAsListenerForSession(newSessionId);
    Map<Path, XMOMLock> sessionLocks = locksPerSession.get(newSessionId);
    if (sessionLocks == null) {
      sessionLocks = new HashMap<Path, XMOMLock>();
      locksPerSession.put(newSessionId, sessionLocks);
    }
    sessionLocks.put(lock.getPath(), lock);
  }
  
  
  private void removePreviousAutosaveMessage(Path path, String type, Long messageId) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    PredefinedMessagePath autosaveMessagePath = PredefinedMessagePath.XYNA_MODELLER_AUTOSAVES;
    XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement().removePersistentMessage(autosaveMessagePath.getProduct(),
                                                                                                            autosaveMessagePath.getContext(),
                                                                                                            createCorrelation(path, type), messageId);
  }
  
  
  private void publishPublication(String sessionId, Path path, String type, String creator, String payload, Long autosaveCounter, PublicationInformation publications) throws XynaException {
    PredefinedMessagePath autosaveMessagePath = PredefinedMessagePath.XYNA_MODELLER_AUTOSAVES;
    autosaveGuardLock.readLock().lock();
    try {
      for (AutosaveGuard guard : autosaveGuards.values()) {
        if (!guard.accept(path, type, payload)) {
          // reply to that session with previous autosave
          // if there is no autosave, publishRollback
          // reply goes directly to the calling session
          Message directReply;
          if (!publications.hasPuplications()) {
            directReply = new Message(IDGenerator.getInstance().getUniqueId(),
                                      autosaveMessagePath.getProduct(),
                                      autosaveMessagePath.getContext(),
                                      createCorrelation(path, type),
                                      creator,
                                      createMessagePayload(null, -1L),
                                      false);
          } else { //  TODO can we just replay the message or do we need to increment the id?
            PublicationEntry entry = publications.getPublicationEntry(publications.getCurrentPublicationId());
            directReply = new Message(publications.getCurrentMessageId(),
                                      autosaveMessagePath.getProduct(),
                                      autosaveMessagePath.getContext(),
                                      createCorrelation(path, type),
                                      creator,
                                      createMessagePayload(entry.getPayload(), entry.getPublicationId()),
                                      false);
          }
          MessageBusImpl messageBus = ((MessageBusImpl)((MessageBusManagement)
                          XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement()).getMessageBus());
          MessageBusSubscriptionSession session = messageBus.getSession(sessionId);
          session.directReply(directReply);
          messageBus.signal(sessionId);
          return;
        }
      }
    } finally {
      autosaveGuardLock.readLock().unlock();  
    }
    Long messageId = XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement()
                                              .publish(new MessageInputParameter(autosaveMessagePath.getProduct(),
                                                                                 autosaveMessagePath.getContext(),
                                                                                 createCorrelation(path, type), creator,
                                                                                 createMessagePayload(payload, autosaveCounter),
                                                                                 autosaveMessagePath.isPersistent()));
    publications.addPublication(autosaveCounter, payload, messageId);
  }
  
  
  private void publishRollback(Path path, String type, String creator) throws XynaException {
    PredefinedMessagePath autosaveMessagePath = PredefinedMessagePath.XYNA_MODELLER_AUTOSAVES;
    XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement()
                             .publish(new MessageInputParameter(autosaveMessagePath.getProduct(),
                                                                autosaveMessagePath.getContext(),
                                                                createCorrelation(path, type), creator,
                                                                createMessagePayload(null, -1L),
                                                                false));
  }
  
  
  private void replaceAutosaveWithRollback(Path path, String type, String creator, Long messageId) throws XynaException {
    removePreviousAutosaveMessage(path, type, messageId);
    publishRollback(path, type, creator);
  }
  
  
  private Long sendLockMessage(String sessionId, String creator, Path path, String type) throws XynaException {
    PredefinedMessagePath messagePath = PredefinedMessagePath.XYNA_MODELLER_LOCKS;
    MessageInputParameter message = new MessageInputParameter(messagePath.getProduct(),
                                                              messagePath.getContext(),
                                                              createCorrelation(path, type),
                                                              creator,
                                                              emptyPayload,
                                                              messagePath.isPersistent());
    return XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement().publish(message);
  }
  
  
  private void deleteLockAndInsertUnlockMessage(String creator, Path path, String type, Long messageId) throws XynaException {
    PredefinedMessagePath lockMessagePath = PredefinedMessagePath.XYNA_MODELLER_LOCKS;
    if (lockMessagePath.isPersistent()) {
      XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement().removePersistentMessage(lockMessagePath.getProduct(),
                                                                  lockMessagePath.getContext(),
                                                                  createCorrelation(path, type),
                                                                  messageId);
    }
    PredefinedMessagePath unlockMessagePath = PredefinedMessagePath.XYNA_MODELLER_UNLOCKS;
    MessageInputParameter message = new MessageInputParameter(unlockMessagePath.getProduct(),
                                                              unlockMessagePath.getContext(),
                                                              createCorrelation(path, type),
                                                              creator,
                                                              emptyPayload,
                                                              unlockMessagePath.isPersistent());
    XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement().publish(message);
  }
  
  
  private static List<SerializablePair<String, String>> createMessagePayload(String xml, Long autosavecounter) {
    List<SerializablePair<String, String>> payload = new ArrayList<SerializablePair<String,String>>();
    if (xml != null) {
      payload.add(new SerializablePair<String, String>(MESSAGE_PAYLOAD_KEY_DOCUMENT, xml));
    }
    if (autosavecounter != null) {
      payload.add(new SerializablePair<String, String>(MESSAGE_PAYLOAD_KEY_PUBLICATION_ID, autosavecounter.toString()));
    }
    return payload;
  }
  
  
  private void registerAsListenerForSession(String sessionId) {
    XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement().addSessionTerminationHandler(sessionId, this);
  }
  
  
  public static class AutosaveGuard {
    
    private AutosaveFilter filter;
    
    private AutosaveGuard(AutosaveFilter filter) {
      this.filter = filter;
    }
    
    
    public boolean activate() throws XynaException {
      Set<Message> messages = ((MessageBusManagement)XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement())
        .fetchPersistentMessages(PredefinedMessagePath.XYNA_MODELLER_AUTOSAVES);
      for (Message message : messages) {
        String xml = message.getPayloadAsMap().get(MESSAGE_PAYLOAD_KEY_DOCUMENT);
        Pair<String, Path> typePathSplit = splitCorrelation(message.getCorrelation());
        if (filter.detect(typePathSplit.getSecond(), typePathSplit.getFirst(), xml)) {
          throw new XDEV_RefactoringConflict(typePathSplit.getSecond().getPath(), typePathSplit.getFirst(),"unknown");
        }
      }
      return true;
    }
    
    
    public boolean accept(Path path, String type, String payload) {
      return !filter.detect(path, type, payload);
    }
    
  }
  
  
  public static interface AutosaveFilter {
    
    public boolean detect(Path path, String type, String payload);
    
  }
  

  public static class Path {
    
    private String path;
    private Long revision;
    
    public Path(String path, Long revision) {
      this.path = path;
      this.revision = revision;
    }
    
    public String getPath() {
      return path;
    }

    public Long getRevision() {
      return revision;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((path == null) ? 0 : path.hashCode());
      result = prime * result + ((revision == null) ? 0 : revision.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Path other = (Path) obj;
      if (path == null) {
        if (other.path != null)
          return false;
      } else if (!path.equals(other.path))
        return false;
      if (revision == null) {
        if (other.revision != null)
          return false;
      } else if (!revision.equals(other.revision))
        return false;
      return true;
    }
  }
}
