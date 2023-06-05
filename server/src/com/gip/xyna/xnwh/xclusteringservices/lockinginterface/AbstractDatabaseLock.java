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
package com.gip.xyna.xnwh.xclusteringservices.lockinginterface;



import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.Department;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface.AlreadyUnlockedException;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface.LockFailedException;



public abstract class AbstractDatabaseLock implements DatabaseLock {

  protected static Logger logger = Logger.getLogger(AbstractDatabaseLock.class);

  protected boolean doCommit = false;
  protected boolean useConnection = false;
  
  protected ClusteringServicesLockStorable targetStorable;

  protected ODS ods;

  protected final ReentrantLock localLock = new ReentrantLock();

  protected long lockTimestamp;

  protected volatile boolean shutdown = false;

  protected ClusterContext clusterContext;

  protected boolean lockedInDB = false;


  public AbstractDatabaseLock(ODS ods, ClusterContext clusterContext, String name) throws PersistenceLayerException {
    this.ods = ods;
    this.clusterContext = clusterContext;
    this.targetStorable = new ClusteringServicesLockStorable(name);

    this.useConnection = clusterContext.isClustered();
    if (useConnection) {
      ODSConnection connection = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        connection.persistObject(targetStorable);
        connection.commit();
      } finally {
        connection.closeConnection();
      }
    }
  }


  /**
   * Welche ODSConnection soll zum Lock verwendet werden?
   */
  protected abstract ODSConnection getConnectionForLock();


  /**
   * Welche ODSConnection soll zum Unlock verwendet werden?
   */
  protected abstract ODSConnection getConnectionForUnlock();


  /**
   * Neubau der Connection wegen der aufgetretenen Exception cause
   * @throws LockFailedException falls Connection nicht neu geöffnet werden kann
   */
  protected abstract ODSConnection getConnectionForRetry(PersistenceLayerException cause) throws LockFailedException;


  /**
   * Schließen der Connection nach dem Unlock
   */
  protected abstract void closeConnectionForUnlock();


  /**
   * Schließen der Connection beim Shutdown
   */
  protected abstract void closeConnectionForShutdown();


  public void commit() {
    doCommit = true;
  }


  public void rollback() {
    doCommit = false;
  }


  /**
   * Persistiert das ClusteringServicesLockStorable. Falls dies nicht klappt, werden Retries durchgeführt, solange der
   * ClusterState != DISCONNECTED_SLAVE ist Ansonsten wird abgebrochen mit der zuletzt erhaltenen
   * PersistenceLayerException oder einer InterruptedException
   */
  protected void persistStorable() throws PersistenceLayerException, InterruptedException, LockFailedException {
    int retry = 0;
    PersistenceLayerException retryException;
    do {
      ODSConnection con = getConnectionForLock();
      try {
        con.persistObject(targetStorable);
        con.commit();
        return; //normaler Ausstieg hier
      } catch (XNWH_RetryTransactionException e) {
        // do not use WarehouseRetryExecutor as we are retrying forever with special exception handling
        retryException = e;
      } catch (PersistenceLayerException e) {
        if (e.getCause() instanceof NoConnectionAvailableException) {
          retryException = e;
        } else {
          throw e;
        }
      } finally {
        closeConnectionForUnlock();
      }

      if (retry == 0) {
        logger.error("Could not persist ClusteringServicesLockStorable-entry \"" + targetStorable.getName()
            + "\" -> retry: " + retryException.getMessage());
      }

      ClusterState clusterState = clusterContext.getClusterState();
      if (clusterState == ClusterState.DISCONNECTED_SLAVE) {
        logger.warn("ClusterState is now " + clusterState + ", giving up to retry after " + (retry - 1) + " retries");
        throw retryException;
      } else if (shutdown) {
        throw new LockFailedException("shutdown");
      } else {
        //Eintrag in DB sollte eigentlich schreibbar sein, d.h. weiter Retries
        Thread.sleep((long) (Math.random() * 1000));
        ++retry;
      }
    } while (true);
  }


  public void setUseConnection(boolean useConnection) throws LockFailedException {
    if (shutdown) {
      return;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("ClusterLockingInterface.setUseConnection(" + useConnection + ")");
    }
    if (this.useConnection == useConnection) {
      return;
    }
    localLock.lock();
    try {
      this.useConnection = useConnection;
      if (this.useConnection) {
        try {
          persistStorable();
        } catch (LockFailedException e) {
          throw e;
        } catch (Exception e) {
          //Fehler tritt nur in Ausnahmefällen im ClusterState DISCONNECTED_SLAVE auf,
          //daher Fehler einfach weiterwerfen
          throw new LockFailedException(e);
        }
      }
    } finally {
      localLock.unlock();
    }
  }


  protected void closeConnectionWithoutException(ODSConnection con) {
    if (con == null) {
      return; //nichts zu tun
    }
    try {
      con.closeConnection();
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to close connection", e);
    }
  }


  protected void lockInDB() throws LockFailedException {
    if (shutdown) {
      throw new LockFailedException("shutdown");
    }
    try {

      if (lockedInDB) {
        //nichts mehr zu tun
        return;
      }

      if (useConnection) {
        lockStorable();
        lockedInDB = true;
      } else {
        if (clusterContext.isClustered()) {
          logger.warn("Using ClusterLockingInterface-Lock \"" + targetStorable.getName()
              + "\" without database even though clusterState is " + clusterContext.getClusterState());
        }
      }
      lockTimestamp = System.currentTimeMillis();

    } catch (Throwable t) {
      Department.handleThrowable(t);
      if (t instanceof LockFailedException) {
        throw (LockFailedException) t;
      } else {
        throw new LockFailedException(t);
      }
    }
  }


  /**
   * Lockt das ClusteringServicesLockStorable durch ein SelectForUpdate. Falls dies nicht klappt, werden Retries
   * durchgeführt, solange der ClusterState != DISCONNECTED_SLAVE ist Ansonsten wird abgebrochen mit der zuletzt
   * erhaltenen PersistenceLayerException oder einer InterruptedException
   * @throws PersistenceLayerException
   * @throws InterruptedException
   * @throws LockFailedException
   */
  private void lockStorable() throws PersistenceLayerException, InterruptedException, LockFailedException {
    int retry = 0;
    PersistenceLayerException retryException = null;
    ODSConnection lockingCon = getConnectionForLock();

    do {
      try {
        lockingCon.queryOneRowForUpdate(targetStorable);
        return; //normaler Ausstieg hier
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.error("Could not lock ClusteringServicesLockStorable-entry \"" + targetStorable.getName()
            + "\"; not found in database -> trying to recreate");
        persistStorable(); //wirft Exceptions, daher Austieg, falls persistStorable auch auf Fehler stößt
        retryException = null;
      } catch (PersistenceLayerException e) {
        lockingCon = getConnectionForRetry(e);
        retryException = e;
      }

      if (retryException != null) {
        if (retry++ % 120 == 0) { //ca jede 1 min, weil unten durchschnittlich 500ms gewartet wird
          logger.error("Could not lock ClusteringServicesLockStorable-entry \"" + targetStorable.getName()
              + "\" -> retry: " + retryException.getMessage(), retryException);
        } else {
          if (logger.isTraceEnabled()) {
            logger.trace("could not get lock again", retryException);
          }
        }

        ClusterState clusterState = clusterContext.getClusterState();
        if (clusterState == ClusterState.DISCONNECTED_SLAVE) {
          logger.warn("ClusterState is now " + clusterState + ", giving up to retry after " + (retry - 1) + " retries");
          closeConnectionForUnlock();
          throw retryException;
        } else if (shutdown) {
          closeConnectionForUnlock();
          throw new LockFailedException("shutdown");
        } else {
          //Eintrag in DB sollte eigentlich schreibbar sein, d.h. weiter Retries
          Thread.sleep( (long)(Math.random()*1000) );
        }
      }
    } while( true );
  }

  public void unlock() {

    if (!localLock.isHeldByCurrentThread()) {
      throw new IllegalMonitorStateException();
    }
    
    try {
      if (localLock.getHoldCount() > 1) {
        // DB-Lock noch nicht freigeben
        return;
      }
      ODSConnection lockingCon = getConnectionForUnlock();
      lockedInDB = false;
      if( lockingCon != null ) {
        unlockStorable(lockingCon);
      } else {
        if( useConnection ) {
          logger.warn( "connection is null even though useConnection is true!");
        }
      }
    } finally {
      localLock.unlock();
    }
  }

  /**
   * @param con
   */
  private void unlockStorable(ODSConnection con) {
    try {
      if (doCommit) {
        con.commit();
      } else {
        con.rollback();
      }
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.warn("Failed to release lock in the database.");
      logger.info("The lock may have been released before while it was expected to be hold since "
          + (System.currentTimeMillis() - lockTimestamp) + " ms.");
      ClusterState clusterState = clusterContext.getClusterState();
      if (clusterState == ClusterState.DISCONNECTED_SLAVE) {
        logger.info("Cause can be the lost connection to the database: clusterState is DISCONNECTED_SLAVE.");
      } else {
        logger.info("Cause is unknown, since clusterState is " + clusterState + ".");
      }
      logger.info("Exception was " + t.getMessage(), t);
      throw new AlreadyUnlockedException(t);
    } finally {
      closeConnectionForUnlock();
    }
  }
  
  public void shutdown() {
    logger.info( "ClusterLockingInterface.shutdown()" );
    shutdown = true;
    closeConnectionForShutdown();
    logger.info( "ClusterLockingInterface.shutdown() finished" );
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(100);
    sb.append(this.getClass().getSimpleName()).append("(");
    sb.append("localLock is ");
    if( localLock.isLocked() ) {
      String thread = localLock.toString();
      int pos = thread.indexOf("thread");
      if( pos != -1 ) {
        sb.append("locked by ").append(thread.substring(pos, thread.length()-1) );
      }
      if( localLock.getHoldCount() > 1 ) {
        sb.append(" ").append(localLock.getHoldCount()).append( " times");
      }
    } else {
      sb.append("unlocked");
    }
    sb.append(", db is ");
    if( useConnection ) {
      sb.append(lockedInDB?" locked":"not locked");
    } else {
      sb.append(" not used"); 
    }
    sb.append(")");
    return sb.toString();
  }

  
  
}
