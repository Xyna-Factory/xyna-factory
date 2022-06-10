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

import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface.AlreadyUnlockedException;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface.LockFailedException;


/**
 *
 * Verwendung:
 * ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
 * databaseLock.lock(con);
 * try {
 *   ....
 *   
 *   databaseLock.commit();
 * } catch( Exception e ) {
 *   databaseLock.rollback();
 * } finally {
 *   databaseLock.unlock(); //schließt con
 * }
 * 
 * 
 *
 */
public class DatabaseLockExternalConnection extends AbstractDatabaseLock {

  private ODSConnection lockingCon;
  private CommitRollbackGuard commitGuard;
  private CommitRollbackGuard rollbackGuard;
  private boolean closeOnUnlock = true;

  public DatabaseLockExternalConnection(ODS ods, ClusterContext clusterContext, String name) throws PersistenceLayerException, LockFailedException {
    super(ods, clusterContext, name);
  }

  @Override
  protected void closeConnectionForShutdown() {
    lockingCon = null;
  }

  @Override
  protected void closeConnectionForUnlock() {
    if( closeOnUnlock ) {
      closeConnectionWithoutException(lockingCon);
    }
    lockingCon = null;
  }

  @Override
  protected ODSConnection getConnectionForLock() {
    if( lockingCon == null ) {
      throw new LockFailedException("no connection set");
    }
    return lockingCon;
  }

  @Override
  protected ODSConnection getConnectionForRetry(PersistenceLayerException cause) throws LockFailedException {
    //Retries erfordern Neubau der Connection, dies ist nicht erlaubt, daher Abbruch
    throw new LockFailedException(cause);
  }

  @Override
  protected ODSConnection getConnectionForUnlock() {
    return lockingCon;
  }
  
  public void lock() throws LockFailedException {
    if( lockingCon == null ) {
      throw new LockFailedException("lock with Connection can not be used to set internal connection");
    } else {
      localLock.lock();
    }
  }

  public void lock( ODSConnection con ) throws LockFailedException {
    localLock.lock(); //hier schon lokales Lock holen, damit die Connection sicher ist 
    boolean lockSuccessful = false;
    try {
      if( lockingCon != null ) {
        throw new LockFailedException("Connection already set");
      }
      lockingCon = con;
      commitGuard = new CommitRollbackGuard();
      rollbackGuard = new CommitRollbackGuard();

      lockingCon.executeAfterCommit( commitGuard );
      lockingCon.executeAfterRollback( rollbackGuard );

      lockInDB();
      lockSuccessful = true;
    }
    finally {
      if( ! lockSuccessful ) {
        localLock.unlock();
      }
    }
  }
  
  
  private static class CommitRollbackGuard implements Runnable {
    private long firstCall = 0;
    public void run() {
      if( firstCall == 0 ) {
        firstCall = System.currentTimeMillis();
      }
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xnwh.xclusteringservices.lockinginterface.AbstractDatabaseLock#unlock()
   */
  @Override
  public void unlock() {
    try {
      if( commitGuard.firstCall != 0 || rollbackGuard.firstCall != 0 ) {
        if( commitGuard.firstCall != 0 ) {
          throw new AlreadyUnlockedException("Connection was committed "+(System.currentTimeMillis()-commitGuard.firstCall)+" ms ago");
        }
        if( rollbackGuard.firstCall != 0 ) {
          throw new AlreadyUnlockedException("Connection was rollbacked "+(System.currentTimeMillis()-rollbackGuard.firstCall)+" ms ago");
        }
      }
    } finally {
      //auf jeden Fall Reentrant-Lock zurückgeben
      super.unlock();
    }
  }
  
}
