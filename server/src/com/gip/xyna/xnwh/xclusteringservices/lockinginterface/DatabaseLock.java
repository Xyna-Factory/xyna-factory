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
package com.gip.xyna.xnwh.xclusteringservices.lockinginterface;

import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface.AlreadyUnlockedException;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface.LockFailedException;


/**
 *
 */
public interface DatabaseLock {

  /**
   * Soll das Lock in der DB geholt werden?
   * @param useConnection
   */
  public void setUseConnection(boolean useConnection);

  /**
   * Holen des Locks
   * @throws LockFailedException
   */
  public void lock() throws LockFailedException;
  
  /**
   * Holen des Locks mit Übergabe der zum Lock zu verwendenden Connection
   * @throws LockFailedException
   */
  public void lock(ODSConnection con) throws LockFailedException;
  
  /**
   * Setzen der Unlock-Operation
   */
  public void commit();
  
  /**
   * Setzen der Unlock-Operation
   */
  public void rollback();
  
  /**
   * Rückgabe des Locks
   * @throws AlreadyUnlockedException falls erkannt werden konnte, dass das Lock bereits zurückgegeben wurde
   */
  public void unlock() throws AlreadyUnlockedException;

  /**
   * FIXME wird dies wirklich benötigt? Was soll es genau machen? 
   * Wartende Statements können nicht abgebrochen werden
   */
  public void shutdown();
  
}
