/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.sharedresources;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;



/**
 * A thread that allows multiple factories to cooperate on shared resources.<br><br>
 * 
 * It keeps track of two entries for a factory:<br>
 *   The nextId-entry is a counter that provides unique keys for the factory-entry.<br>
 *   The factory-entry contains resource specific data<br><br>
 * 
 * The shared resource table is checked at regular intervals to determine which factory is responsible
 * for executing work. There can be multiple kinds of work and this class manages cleanup work
 * (deleting factory-entries for disconnected factories) automatically. After a factory executes work,
 * it updates its factory-entry with a new id.
 * 
 */
public class SharedResourceWorkManagementThread<T extends SharedResourceDefinition<R>, R> extends Thread {

  private static final Logger logger = CentralFactoryLogging.getLogger(SharedResourceWorkManagementThread.class);

  public static final String NEXT_ID_KEY = "nextId";

  private boolean running;
  private final ShareResourceEntryManagement<R> nextIdAccessor;
  private final SharedResourceWorkManagement workMgmt;
  private final SharedResourceDefinition<R> resourceDef;
  private final SharedResourceManagement srm;
  private final long heartBeatIntervalMs;
  private final long staleThresholdMs;
  private final long workPerRoundMs;
  private long ourId;


  public SharedResourceWorkManagementThread(SharedResourceWorkManagementThreadConfig<T, R> config) {
    super(config.name);
    this.nextIdAccessor = config.nextIdProcessor;
    this.workMgmt = config.workMgmt;
    this.resourceDef = config.sharedResourceDef;
    this.heartBeatIntervalMs = config.heartbeatIntervalMs;
    staleThresholdMs = config.heartbeatIntervalMs * 5;
    workPerRoundMs = config.heartbeatIntervalMs * 2;
    srm = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
    ourId = -1l;
    running = true;
    setDaemon(true);
  }


  private void processWork() {
    List<SharedResourceWork> work = queryWork();
    int numberOfProcessedItems = 0;
    if (work.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug("No work to do.");
      }
      return;
    }
    long now = System.currentTimeMillis();
    if (logger.isDebugEnabled()) {
      logger.debug("Found " + work.size() + " work items to process. Starting now: " + now);
    }
    for (SharedResourceWork workItem : work) {
      if (System.currentTimeMillis() - now > workPerRoundMs) {
        if (logger.isDebugEnabled()) {
          logger.debug("Work this round exceeded limit, leaving remaining work items for the next round");
        }
        break;
      }
      workItem.execute();
      numberOfProcessedItems++;
      if (logger.isTraceEnabled()) {
        logger.trace("Finished work item " + workItem + " - total work time " + (System.currentTimeMillis() - now) + "ms");
      }
      if (System.currentTimeMillis() - now > heartBeatIntervalMs) {
        refreshEntry();
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Updating entry after processing " + numberOfProcessedItems + " work items.");
    }
    updateEntry();
  }


  public void run() {
    updateEntry();

    while (running) {
      try {
        if (isOurTurnToWork()) {
          processWork();
        }
        Thread.sleep(heartBeatIntervalMs);
        refreshEntry();
      } catch (InterruptedException e) {

      }
    }
  }


  private List<SharedResourceWork> queryStaleEntryWork() {
    long now = System.currentTimeMillis();
    SharedResourceRequestResult<R> resources = srm.readAll(resourceDef);
    if (!resources.isSuccess() || resources.getResources() == null) {
      return Collections.emptyList();
    }
    List<SharedResourceWork> result = new ArrayList<>();
    for (SharedResourceInstance<R> resource : resources.getResources()) {
      if (Objects.equals(NEXT_ID_KEY, resource.getId())) {
        continue;
      }
      long lastUpdate = resource.getCreated();
      if (lastUpdate + staleThresholdMs < now) {
        if (logger.isDebugEnabled()) {
          logger.debug("Identified Stale Entry Deletion work for entry of type " + resourceDef.getPath() + " with id " + resource.getId()
              + " lastUpdate: " + lastUpdate);
        }
        result.add(new DeleteStaleEntryWork(resource.getId(), resourceDef));
      }
    }
    return result;
  }


  private List<SharedResourceWork> queryWork() {
    List<SharedResourceWork> result = new ArrayList<>();
    result.addAll(queryStaleEntryWork());
    result.addAll(workMgmt.queryWork(ourId));
    return result;
  }


  private boolean isOurTurnToWork() {
    if (ourId == -1l) {
      if (logger.isDebugEnabled()) {
        logger.debug("OurId is not set. It cannot be our turn to work");
      }
      return false;
    }
    long now = System.currentTimeMillis();
    SharedResourceRequestResult<R> result = srm.readAll(resourceDef);
    if (!result.isSuccess() || result.getResources() == null) {
      return false;
    }
    boolean ourEntryFound = false;
    List<SharedResourceInstance<R>> resources = result.getResources();
    for (SharedResourceInstance<R> resource : resources) {
      if (Objects.equals(NEXT_ID_KEY, resource.getId())) {
        continue;
      }
      long id = Long.valueOf(resource.getId());
      if (id < ourId && resource.getCreated() + staleThresholdMs > now) {
        if (logger.isDebugEnabled()) {
          logger.debug("Not out turn to work. Active lower id found: " + id);
        }
        return false;
      }
      if (id == ourId) {
        ourEntryFound = true;
      }
    }

    if (!ourEntryFound) {
      if (logger.isWarnEnabled()) {
        logger.warn("No other working node found with a lower id than us (" + ourId + ") but our entry is missing.");
      }
      ourId = -1l;
      updateEntry();
      return false;
    }

    //ourId is the smallest, it is our turn to work
    if (logger.isDebugEnabled()) {
      logger.debug("Our turn to work");
    }
    return true;
  }


  private void refreshEntry() {
    if (ourId == -1l) {
      updateEntry();
      return;
    }
    long now = System.currentTimeMillis();
    SharedResourceRequestResult<R> result = srm.update(resourceDef, List.of(String.valueOf(ourId)), x -> {
      return new SharedResourceInstance<>(x.getId(), now, x.getValue());
    });
    if (!result.isSuccess()) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not refresh entry", result.getException());
      }
      ourId = -1l;
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Successfully refreshed entry (" + ourId + ") to " + now);
      }
    }
  }


  private void updateEntry() {
    long oldId = ourId;
    IdContainer container = new IdContainer();
    SharedResourceRequestResult<R> result = srm.update(resourceDef, List.of(NEXT_ID_KEY), (x) -> {
      container.id = nextIdAccessor.readId(x.getValue()) + 1;
      nextIdAccessor.updateNextIdEntry(x.getValue(), container.id);
      return x;
    });

    if (!result.isSuccess() || container.id == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Could not get id from nextId entry. Creating it.", result.getException());
      }
      createNextIdEntry();
    } else {
      ourId = container.id;
      if (logger.isDebugEnabled()) {
        logger.debug("Got new Id for SharedResource: " + ourId);
      }
    }
    R oldResource = nextIdAccessor.createNewEntry();
    if (oldId != -1l) {
      SharedResourceRequestResult<R> readResult = srm.read(resourceDef, List.of(String.valueOf(oldId)));
      if (readResult.isSuccess() && readResult.getResources() != null && readResult.getResources().size() == 1) {
        oldResource = readResult.getResources().get(0).getValue();
      }
    }

    long now = System.currentTimeMillis();
    if (ourId != -1l) {
      srm.create(resourceDef, List.of(new SharedResourceInstance<>(String.valueOf(ourId), now, oldResource)));
    }

    removeEntry(oldId);
  }


  private void removeEntry(long oldId) {
    if (oldId == -1l) {
      if (logger.isDebugEnabled()) {
        logger.debug("No Entry to remove. oldId is not set");
      }
      return;
    }
    SharedResourceRequestResult<R> result = srm.delete(resourceDef, List.of(String.valueOf(oldId)));
    if (!result.isSuccess()) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not delete our entry with id " + oldId, result.getException());
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Successfully deleted our entry with id " + oldId);
      }
    }

  }


  private void createNextIdEntry() {
    long now = System.currentTimeMillis();
    SharedResourceRequestResult<R> result =
        srm.create(resourceDef, List.of(new SharedResourceInstance<>(NEXT_ID_KEY, now, nextIdAccessor.createNextIdEntry(0))));
    if (!result.isSuccess()) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not create initial NextId entry.", result.getException());
      }
      return;
    }
    ourId = 0;
    if (logger.isDebugEnabled()) {
      logger.debug("Initialized NextId entry. Using id: " + ourId);
    }
  }


  public long getOurId() {
    return ourId;
  }


  public static interface ShareResourceEntryManagement<R> {

    long readId(R nextEntry);


    R createNextIdEntry(long value);


    R createNewEntry();


    void updateNextIdEntry(R nextIdEntry, long value);
  }

  public static interface SharedResourceWork {

    void execute();
  }


  private static class DeleteStaleEntryWork implements SharedResourceWork {

    private final String id;
    private final SharedResourceDefinition<?> resourceDef;


    public DeleteStaleEntryWork(String id, SharedResourceDefinition<?> resourceDef) {
      this.id = id;
      this.resourceDef = resourceDef;
    }


    @Override
    public void execute() {
      if (logger.isDebugEnabled()) {
        logger.debug("Deleting State entry with id " + id + " from " + resourceDef.getPath());
      }
      SharedResourceRequestResult<?> result =
          XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement().delete(resourceDef, List.of(id));
      if (!result.isSuccess()) {
        if (logger.isWarnEnabled()) {
          logger.warn("Failed to delete stale entry with id " + id, result.getException());
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Sucessfully deleted stale entry with id " + id);
        }
      }
    }
  }


  public static interface SharedResourceWorkManagement {

    List<SharedResourceWork> queryWork(long ourId);
  }

  public static class SharedResourceWorkManagementThreadConfig<T extends SharedResourceDefinition<R>, R> {

    private final String name;
    private final T sharedResourceDef;
    private final ShareResourceEntryManagement<R> nextIdProcessor;
    private final SharedResourceWorkManagement workMgmt;
    private final long heartbeatIntervalMs;


    public SharedResourceWorkManagementThreadConfig(String name, T sharedResourceDef, ShareResourceEntryManagement<R> nextIdProcessor,
                                                    SharedResourceWorkManagement workMgmt, long heartbeatIntervalMs) {
      this.name = name;
      this.sharedResourceDef = sharedResourceDef;
      this.nextIdProcessor = nextIdProcessor;
      this.workMgmt = workMgmt;
      this.heartbeatIntervalMs = heartbeatIntervalMs;
    }
  }

  private static class IdContainer {

    private Long id = null;
  }


  public void end() {
    running = false;
  }


  public void removeOurEntry() {
    removeEntry(ourId);
  }
}
