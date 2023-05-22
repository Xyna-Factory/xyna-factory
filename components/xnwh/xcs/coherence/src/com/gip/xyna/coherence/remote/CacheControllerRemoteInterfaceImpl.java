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

package com.gip.xyna.coherence.remote;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

import com.gip.xyna.coherence.CacheController;
import com.gip.xyna.coherence.coherencemachine.CoherenceObjectCompleteState;
import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.utils.logging.LoggerFactory;



public class CacheControllerRemoteInterfaceImpl implements CacheControllerRemoteInterfaceWithInit {

  public static final String RMI_NAME = "CoherenceNodeRemoteInterfaceRMI";
  private static final Logger logger = LoggerFactory.getLogger(CacheControllerRemoteInterfaceImpl.class);

  private final CacheController controller;
  private int port;

  SessionBasedLockUnlock lockUnlockController = new SessionBasedLockUnlock();


  CacheControllerRemoteInterfaceImpl(CacheController controller, int port) {
    this.controller = controller;
    this.port = port;
  }


  public void init() {

    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new RMISecurityManager());
    }

    Registry registry = null;
    try {
      registry = LocateRegistry.createRegistry(getRMIPortForRegistry());
    } catch (RemoteException e) {
      try {
        registry = LocateRegistry.getRegistry(getRMIPortForRegistry());
      } catch (RemoteException e1) {
        throw new RuntimeException("Failed to get rmi registry" + RMI_NAME, e1);
      }
    }
    try {
      CacheControllerRemoteInterface stub =
          (CacheControllerRemoteInterface) UnicastRemoteObject.exportObject((CacheControllerRemoteInterface) this,
                                                                            getRMIPortForCommunication());
      registry.rebind(RMI_NAME, stub);
      if (logger.isDebugEnabled()) {
        logger.debug(RMI_NAME + " bound to registry on port " + port);
      }

    } catch (Exception e) {
      throw new RuntimeException(RMI_NAME, e);
    }
  }


  public void shutdown() {
    Registry registry = null;
    try {
      registry = LocateRegistry.getRegistry(getRMIPortForRegistry());
    } catch (RemoteException e) {
      throw new RuntimeException(RMI_NAME, e);
    }
    try {
      // wenn man das nicht macht, haengt der server fuer eine bestimmte zeit, wenn man den main thread versucht zu
      // beenden:
      //UnicastRemoteObject.unexportObject(this, false);
      UnicastRemoteObject.unexportObject(this, true);
      // schadet nichts, ich habe aber auch nichts gefunden, wofuer es zwingend notwendig waere:
      try {
        registry.unbind(RMI_NAME);
      } catch (ConnectException e) {
        logger.debug("Could not unbind XynaRMIChannel");
        // the RMIRegistry might have been started outside our VM, if so we might try to unbind from
        // a terminated Registry (getRegistry wouldn't notice). Registry will be recreated in our VM on init
      }
      if (logger.isDebugEnabled()) {
        logger.debug(RMI_NAME + " unbound from registry");
        if (logger.isTraceEnabled()) {
          String[] boundObjects = registry.list();
          logger.trace("rmi objects still bound: " + boundObjects.length);
          for (int i = 0; i < boundObjects.length; i++) {
            logger.trace("- " + boundObjects[i]);
          }
        }
      }
    } catch (RemoteException e) {
      throw new RuntimeException(RMI_NAME, e);
    } catch (NotBoundException e) {
      logger.warn("rmi could not be unbound, because it was not bound.", e);
    }
  }


  private int getRMIPortForRegistry() {
    return port;
  }


  private int getRMIPortForCommunication() {
    // TODO configurable
    return 0;
  }


  public long create(CoherencePayload payload) throws RemoteException {
    try {
      return controller.create(payload);
    } catch (Throwable e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }


  public CoherencePayload read(long objectId) throws RemoteException {
    try {
      return controller.read(objectId);
    } catch (ObjectNotInCacheException e) {
      throw new RemoteException(e.getMessage(), e);
    } catch (Throwable e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }


  public void update(long objectId, CoherencePayload payload) throws RemoteException {
    try {
      controller.update(objectId, payload);
    } catch (ObjectNotInCacheException e) {
      throw new RemoteException(e.getMessage(), e);
    } catch (Throwable e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }


  public void delete(long objectId) throws RemoteException {
    try {
      controller.delete(objectId);
    } catch (ObjectNotInCacheException e) {
      throw new RemoteException(e.getMessage(), e);
    } catch (Throwable e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }


  public void lock(long objectId, long sessionId) throws RemoteException {
    try {
      lockUnlockController.lock(controller, sessionId, objectId);
    } catch (ObjectNotInCacheException e) {
      throw new RemoteException(e.getMessage(), e);
    } catch (Throwable e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }


  public void unlock(long objectId, long sessionId) throws RemoteException {
    try {
      lockUnlockController.unlock(controller, sessionId, objectId);
    } catch (ObjectNotInCacheException e) {
      throw new RemoteException(e.getMessage(), e);
    } catch (Throwable e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }


  public void pauseCluster() throws RemoteException {
    try {
      throw new RuntimeException("unsupported: pause cluster");
    } catch (Throwable e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }


  public void unpauseCluster() throws RemoteException {
    try {
      throw new RuntimeException("unsupported: unpause cluster");
    } catch (Throwable e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }


  public CoherenceObjectCompleteState getCompleteObjectState(long objectId) throws RemoteException {
    try {
      throw new RuntimeException("unsupported: get complete object state");
    } catch (Throwable e) {
      throw new RemoteException(e.getMessage(), e);
    }
  }


  public void executeRemoteRunnable(RemoteRunnable r) throws RemoteException {
    r.run(controller);
  }

}
