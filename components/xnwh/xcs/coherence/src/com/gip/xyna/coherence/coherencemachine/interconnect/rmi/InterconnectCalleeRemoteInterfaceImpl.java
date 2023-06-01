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
package com.gip.xyna.coherence.coherencemachine.interconnect.rmi;



import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

import com.gip.xyna.coherence.coherencemachine.CoherenceAction;
import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.interconnect.InitialConnectionData;
import com.gip.xyna.coherence.coherencemachine.interconnect.ThreadType;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockAwaitResponse;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject.LockRequestResponse;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.management.NodeInformation;
import com.gip.xyna.coherence.utils.logging.LoggerFactory;



/**
 * "Server" - Implementierung des RMI-spezifischen Interconnects. Wird als Instanz innerhalb von
 *  {@link InterconnectCalleeRMI} verwendet, nämlich per Reflection instanziiert. 
 *  Deshalb ist der Konstruktor nirgends referenziert.
 */
public class InterconnectCalleeRemoteInterfaceImpl implements InterconnectProtocolRemoteInterface {

  private static final Logger logger = LoggerFactory.getLogger(InterconnectCalleeRemoteInterfaceImpl.class);

  private InterconnectCalleeRMI impl;
  private String rmiBindingName;
  private int port;


  public InterconnectCalleeRemoteInterfaceImpl(InterconnectCalleeRMI impl, String rmiBindingName, int port) {
    this.impl = impl;
    this.rmiBindingName = rmiBindingName;
    this.port = port;
  }


  public void init() {

    //FIXME extrahieren in rmi utils klasse, weil nochmal in cachecotrnollerremote.... benutzt
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new RMISecurityManager());
    }
    Registry registry = null;
    try {
      registry = LocateRegistry.createRegistry(port);
    } catch (RemoteException e) {
      try {
        registry = LocateRegistry.getRegistry(port);
      } catch (RemoteException e1) {
        //TODO prio4: implement RMI node connection
        throw new RuntimeException(e1);
      }
    }

    try {
      InterconnectProtocolRemoteInterface stub =
          (InterconnectProtocolRemoteInterface) UnicastRemoteObject.exportObject(this, port);
      registry.rebind(rmiBindingName, stub);
      if (logger.isDebugEnabled()) {
        logger.debug(rmiBindingName + " bound to registry on port " + port);
      }
    } catch (Exception e) {
      //TODO prio4: implement RMI node connection
      throw new RuntimeException(e);
    }
  }


  public void shutdown() {
    Registry registry = null;
    try {
      registry = LocateRegistry.getRegistry(port);
    } catch (RemoteException e) {
      //TODO prio4: implement RMI node connection
      throw new RuntimeException(e);
    }
    try {
      // wenn man das nicht macht, haengt der server fuer eine bestimmte zeit, wenn man den main thread versucht zu
      // beenden:
      UnicastRemoteObject.unexportObject(this, false);
      // schadet nichts, ich habe aber auch nichts gefunden, wofuer es zwingend notwendig waere:
      registry.unbind(rmiBindingName);
      if (logger.isDebugEnabled()) {
        logger.debug(rmiBindingName + " unbound from registry");
        if (logger.isTraceEnabled()) {
          String[] boundObjects = registry.list();
          logger.trace("rmi objects still bound: " + boundObjects.length);
          for (int i = 0; i < boundObjects.length; i++) {
            logger.trace("- " + boundObjects[i]);
          }
        }
      }
    } catch (RemoteException e) {
      //TODO prio4: implement RMI node connection
      logger.warn("rmi could not be unbound, because it was not bound.", e);
    } catch (NotBoundException e) {
      logger.warn("rmi could not be unbound, because it was not bound.", e);
    }
  }


  public LockAwaitResponse awaitLock(long objectId, long priority, boolean tryLock, long nanoTimeout) throws RemoteException,
      ObjectNotInCacheException, InterruptedException {
    //rücktransformation von timeout, siehe NodeConnectionRMI
    return impl.awaitLock(objectId, priority, tryLock, System.nanoTime() + nanoTimeout);
  }


  public CoherencePayload executeActions(CoherenceAction actions) throws RemoteException {
    return impl.executeActions(actions);
  }


  public void releaseLock(long objectId, long priorityToRelease) throws RemoteException, ObjectNotInCacheException {
    impl.releaseLock(objectId, priorityToRelease);
  }


  public LockRequestResponse requestLock(long objectId, long priority, boolean tryLock, long nanoTimeout)
      throws RemoteException, ObjectNotInCacheException, InterruptedException {
    //rücktransformation von timeout, siehe NodeConnectionRMI
    return impl.requestLock(objectId, priority, tryLock, System.nanoTime() + nanoTimeout);
  }


  public InitialConnectionData connectToCluster(NodeInformation nodeInformation) throws RemoteException {
    return impl.connectToClusterRemotely(nodeInformation);
  }


  public void waitForActiveThreads(ThreadType type) throws RemoteException {
    impl.waitForActiveThreads(type);
  }


  public void recallLockRequest(long objectId, long priority, long priorityOfLockInOldLockCircle,
                                boolean countedInLockCircle, int lockCircleSize) throws RemoteException, ObjectNotInCacheException {
    impl.recallLockRequest(objectId, priority, priorityOfLockInOldLockCircle, countedInLockCircle, lockCircleSize);
  }

}
