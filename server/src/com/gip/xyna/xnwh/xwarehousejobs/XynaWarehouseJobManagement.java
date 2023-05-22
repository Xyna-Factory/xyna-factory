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

package com.gip.xyna.xnwh.xwarehousejobs;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;



public class XynaWarehouseJobManagement extends Section {

  public static final String DEFAULT_NAME = "XynaWarehouseJobManagement";

  public static final int FUTURE_EXECUTION_ID = XynaFactory.getInstance().getFutureExecution().nextId();
  static {
    addDependencies(XynaWarehouseJobManagement.class, new ArrayList<XynaFactoryPath>(Arrays
                    .asList(new XynaFactoryPath[] {
                                    new XynaFactoryPath(XynaProcessing.class, XynaScheduler.class,
                                                        CronLikeScheduler.class),
                                    new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryManagementODS.class,
                                                        Configuration.class)})));
  }


  private AtomicLong nextJobID;
  private HashMap<Long, WarehouseJob> jobs;
  private ReentrantLock jobsLock;

  private volatile boolean isInitialized;

  private ODS ods;


  public XynaWarehouseJobManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  public void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask(XynaWarehouseJobManagement.class, DEFAULT_NAME)
    //FIXME this is too little, we need to wait for every component with a FutureExecution wherein a Storable is registered
    // Reason: WarehouseJobRunnable.getRunnableByActionType -> ODSImpl.getInstance().getStorableByTableName(tableName);
      .after("CronLikeScheduler.startTimerThread")
      .after(OrderArchive.FUTURE_EXECUTION_ID)
      .after(IDGenerator.class).
      execAsync(new Runnable() { public void run() { initXynaWarehouseJobManagement(); }});
  }

  
  

  private void initXynaWarehouseJobManagement() {
    try {
    if (jobsLock == null) {
      jobsLock = new ReentrantLock();
    }
    ods = ODSImpl.getInstance();
    ods.registerStorable(WarehouseJob.class);

    // FIXME possible deadlocks!
    jobsLock.lock();
    try {

      jobs = new HashMap<Long, WarehouseJob>();
      Collection<WarehouseJob> loadedJobs = null;

      ODSConnection con = ods.openConnection();
      try {
        loadedJobs = con.loadCollection(WarehouseJob.class);
      } finally {
        con.closeConnection();
      }

      long maxId = 1;
      if (loadedJobs != null && loadedJobs.size() > 0) {
        for (WarehouseJob job : loadedJobs) {
          // FIXME somehow find out whether to activate the job or not. maybe store that information within the job
          addJobInternally(job, true);
          if (job.getId() > maxId) {
            maxId = job.getId();
          }
        }
      }
      nextJobID = new AtomicLong(maxId+1);
    } finally {
      jobsLock.unlock();
    }

    isInitialized = true;
    } catch( XynaException e ) {
      throw new RuntimeException(e);
    }
  }


  @Override
  protected void shutdown() throws XynaException {
    super.shutdown();
    isInitialized = false;
  }


  /**
   * Adds a job but does not necessarily register it, that is, it wont be executed if it is not register
   * @throws PersistenceLayerException if the registration of the underlying {@link WarehouseJobSchedule} causes an error
   */
  public void addJob(WarehouseJob job, boolean register) throws PersistenceLayerException {
    checkState();
    addJobInternally(job, register);
  }


  private void addJobInternally(WarehouseJob job, boolean register) throws PersistenceLayerException {

    // FIXME the following code can result in connection pool deadlocks
    jobsLock.lock();
    try {
      if (!jobs.values().contains(job)) {
        if (job.getId() == -1) {
          jobs.put(nextJobID.getAndIncrement(), job);
        } else {
          jobs.put(job.getId(), job);
        }
      }

      if (register) {
        job.getJobSchedule().register();
      }

      //persist data after registration to prevent persistation of illegal params
      ODSConnection con = ods.openConnection();
      try {
        con.persistObject(job);
        con.commit();
      } finally {
        con.closeConnection();
      }

    } finally {
      jobsLock.unlock();
    }

  }


  /**
   * Returns a list of all jobs that are currently available but not necessarily active
   * @return {@link Map}
   */
  public Map<Long, WarehouseJob> getJobs() {
    checkState();
    jobsLock.lock();
    try {
      return Collections.unmodifiableMap(jobs);
    } finally {
      jobsLock.unlock();
    }
  }


  /**
   * Removes a job by object reference
   * 
   * @param job the job to be removed
   * @return true, if the job could be removed or false otherwise
   */
  
  public boolean removeJob(WarehouseJob job) throws PersistenceLayerException {

    checkState();

    if (job == null) {
      throw new IllegalArgumentException("Job to be removed may not be null");
    }

    // FIXME potential connection pool deadlock
    jobsLock.lock();
    try {
      Iterator<Entry<Long, WarehouseJob>> iter = jobs.entrySet().iterator();
      while (iter.hasNext()) {
        if (iter.next().getValue() == job) {
          iter.remove();
          job.getJobSchedule().unregister();

          ODSConnection con = ods.openConnection();
          try {
            con.deleteOneRow(job);
            con.commit();
          } finally {
            con.closeConnection();
          }

          return true;
        }
      }
      return false;
    } finally {
      jobsLock.unlock();
    }

  }


  /**
   * Removes a job by internal id (this has to be retrieved by "getJobs()" first
   * @param id the job to be removed
   * @return true, if the job could be removed or false otherwise
   */
  public boolean removeJob(Long id) throws PersistenceLayerException {

    checkState();

    if (id == null) {
      throw new IllegalArgumentException("ID of job to be removed may not be null");
    }

    // FIXME potential connection pool deadlock
    jobsLock.lock();
    try {
      if (jobs.containsKey(id)) {
        WarehouseJob job = jobs.remove(id);
        job.getJobSchedule().unregister();
        ODSConnection con = ods.openConnection();
        try {
          con.deleteOneRow(job);
          con.commit();
        } finally {
          con.closeConnection();
        }
        return true;
      } else {
        return false;
      }
    } finally {
      jobsLock.unlock();
    }

  }


  /**
   * Activates or deactivates the provided job (by job object reference)
   * 
   * @param job the job to be modified
   * @param b
   */
  public void setJobActive(WarehouseJob job, boolean b) {

    checkState();

    if (job == null)
      throw new IllegalArgumentException("Job to be removed may not be null");

    jobsLock.lock();
    try {

      Iterator<Entry<Long, WarehouseJob>> iter = jobs.entrySet().iterator();
      while (iter.hasNext()) {
        WarehouseJob next = iter.next().getValue();
        if (next == job) {
          if (b) {
            next.getJobSchedule().register();
          } else {
            next.getJobSchedule().unregister();
          }
        }
      }

    } finally {
      jobsLock.unlock();
    }

  }


  /**
   * Activates or deactivates the provided job (by job object id)
   * @param id the job to be modified
   * @param b
   */
  public void setJobActive(long id, boolean b) {

    checkState();

    jobsLock.lock();
    try {
      WarehouseJob job = jobs.get(id);
      if (job != null) {
        if (b) {
          job.getJobSchedule().register();
        } else {
          job.getJobSchedule().unregister();
        }
      }
    } finally {
      jobsLock.unlock();
    }

  }


  private void checkState() {
    if (!isInitialized) {
      throw new IllegalStateException(getDefaultName() + " has not been initialized");
    }
  }

  
  public long getNextJobId() {
    return nextJobID.getAndIncrement();
  }

}
