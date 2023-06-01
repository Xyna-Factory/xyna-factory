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
package com.gip.xyna.xdev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;



// TODO make this functionality part of an already existing component?
public class ProjectCreationOrChangeProvider {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(ProjectCreationOrChangeProvider.class);
  
  public static enum EventType {
    TRIGGER_CREATION,
    FILTER_CREATION,
    XMOM_MODIFICATION,
    XMOM_MOVE,
    XMOM_DELETE,
    APPLICATION_DEFINE,
    APPLICATION_DEFINITION_CHANGE,
    APPLICATION_REMOVE,
    TRIGGER_ADD,
    TRIGGER_REMOVE,
    FILTER_ADD,
    FILTER_REMOVE,
    SERVICE_DEPLOY,
    SHAREDLIB_DEPLOY,
    SHAREDLIB_REMOVE,
    RUNTIMECONTEXT_DEPENDENCY_MODIFICATION;
  }
  
  
  private static volatile ProjectCreationOrChangeProvider instance;
  
  private final ExecutorService threadpool;
  
  public static ProjectCreationOrChangeProvider getInstance() {
    if (instance == null) {
      synchronized (ProjectCreationOrChangeProvider.class) {
        if (instance == null) {
          ProjectCreationOrChangeProvider newInstance = new ProjectCreationOrChangeProvider();
          instance = newInstance;
        }
      }
    }
    return instance;
  }
  
  
  private final ConcurrentMap<String, ProjectCreationOrChangeListener> listeners;
  
  
  public ProjectCreationOrChangeProvider() {
    listeners = new ConcurrentHashMap<String, ProjectCreationOrChangeListener>();
    threadpool = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
      
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r);

        if (t.getPriority() != Constants.DEFAULT_THREAD_PRIORITY) {
          t.setPriority(Constants.DEFAULT_THREAD_PRIORITY);
        }

        t.setDaemon(true);
        t.setName("ProjectCreationOrChange Notifier");
        return t;
      }
    });
  }
  
  
  public void addListener(String id, ProjectCreationOrChangeListener listener) {
    listeners.put(id, listener);
  }
  
  
  public void removeListener(String id) {
    listeners.remove(id);
  }
  
  public void notify(Collection<? extends ProjectCreationOrChangeEvent> event, Long revision, boolean executeSync) {
    notify(event, revision, null, executeSync);
  }
  
  public void notify(Collection<? extends ProjectCreationOrChangeEvent> event, Long revision, String commitMsg, boolean executeSync) {
    Future<?> future = threadpool.submit(new NotificationRunnable(event, revision, commitMsg, XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.get()));
    if (executeSync) {
      try {
        future.get();
      } catch (InterruptedException e) {
        // TODO warn log
      } catch (ExecutionException e) {
        // TODO warn log
      }
    }
  }
  
  
  public void notify(ProjectCreationOrChangeEvent event, Long revision, boolean executeSync) {
    notify(Collections.singleton(event), revision, executeSync);
  }
  
  
  public boolean listeneresPresent() {
    return listeners.size() > 0;
  }
  
  
  public static abstract class ProjectCreationOrChangeEvent {
    
    private final EventType type;
    
    public ProjectCreationOrChangeEvent(EventType type) {
      this.type = type;
    }
    
    public EventType getType() {
      return type;
    }

  }
  
  
  public static class BasicProjectCreationOrChangeEvent extends ProjectCreationOrChangeEvent {

    private final String objectIdentifier;
    
    public BasicProjectCreationOrChangeEvent(EventType type, String objectIdentifier) {
      super(type);
      this.objectIdentifier = objectIdentifier;
    }
    
    public String getObjectIdentifier() {
      return objectIdentifier;
    }
    
  }
  
  
  public static class XMOMChangeEvent extends BasicProjectCreationOrChangeEvent {

    private final XMOMType xmomtype;
    
    protected XMOMChangeEvent(EventType type, String objectIdentifier, XMOMType xmomtype) {
      super(type, objectIdentifier);
      this.xmomtype = xmomtype;
    }
    
    public XMOMChangeEvent(String objectIdentifier, XMOMType xmomtype) {
      this(EventType.XMOM_MODIFICATION, objectIdentifier, xmomtype);
    }
    
    public XMOMType getXMOMType() {
      return xmomtype;
    }
    
  }
  
  
  public static class FilterCreationEvent extends BasicProjectCreationOrChangeEvent {
    
    private final String triggerName;

    public FilterCreationEvent(String filterName, String triggerName) {
      super(EventType.FILTER_CREATION, filterName);
      this. triggerName = triggerName;
    }
    
    public String getTriggerName() {
      return triggerName;
    }
    
  }
  
  public static class FilterChangeEvent extends BasicProjectCreationOrChangeEvent {
    
    private final String simpleFqClassName;
    
    public FilterChangeEvent(EventType type, String filterName, String simpleFqClassName) {
      super(type, filterName);
      this. simpleFqClassName = simpleFqClassName;
    }
    
    
    public String getSimpleFqClassName() {
      return simpleFqClassName;
    }
    
  }

  public static class TriggerChangeEvent extends BasicProjectCreationOrChangeEvent {
    
    private final String simpleFqClassName;
    
    public TriggerChangeEvent(EventType type, String triggerName, String simpleFqClassName) {
      super(type, triggerName);
      this. simpleFqClassName = simpleFqClassName;
    }
    
    
    public String getSimpleFqClassName() {
      return simpleFqClassName;
    }
    
  }
  
  public static class XMOMChangeEventWithRevision extends XMOMChangeEvent {
    
    private final long revision;
    
    public XMOMChangeEventWithRevision(EventType event, String fqName, XMOMType xmomtype, long revision) {
      super(event, fqName, xmomtype);
      this.revision = revision;
    }

    public long getRevision() {
      return revision;
    }
    
  }
  
  
  public static class XMOMMovementEvent extends XMOMChangeEventWithRevision {
    
    private final String oldFqName;

    public XMOMMovementEvent(String newFqName, String oldFqName, XMOMType xmomtype, long revision) {
      super(EventType.XMOM_MOVE, newFqName, xmomtype, revision);
      this.oldFqName = oldFqName;
    }
    
    public String getOldFqName() {
      return oldFqName;
    }
    
  }

  public static class XMOMDeleteEvent extends XMOMChangeEventWithRevision {

    public XMOMDeleteEvent(String oldFqName, XMOMType xmomtype, Long revision) {
      super(EventType.XMOM_DELETE, oldFqName, xmomtype, revision);
    }
    
  }

  public static class RuntimeContextDependencyChangeEvent extends ProjectCreationOrChangeEvent {
    
    public RuntimeContextDependencyChangeEvent() {
      super(EventType.RUNTIMECONTEXT_DEPENDENCY_MODIFICATION);
    }
  }
  
  
  public interface ProjectCreationOrChangeListener {
    
    public void projectCreatedOrModified(Collection<? extends ProjectCreationOrChangeEvent> event, Long revision, String commitMsg);
    
  }
  
  
  private class NotificationRunnable implements Runnable {

    private final Collection<? extends ProjectCreationOrChangeEvent> events;
    private final Long revision;
    private final String commitMsg;
    private final XynaMultiChannelPortal.Identity identity;
    
    public NotificationRunnable(Collection<? extends ProjectCreationOrChangeEvent> events, Long revision, String commitMsg, XynaMultiChannelPortal.Identity identity) {
      this.events = events;
      this.revision = revision;
      this.commitMsg = commitMsg;
      this.identity = identity;
    }

    public void run() {
      try {
        XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(identity);
        try {
          for (ProjectCreationOrChangeListener listener : listeners.values()) {
            listener.projectCreatedOrModified(events, revision, commitMsg);
          }
        } finally {
          XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
        }
      } catch (Throwable t) {
        logger.warn("Exception during " + ProjectCreationOrChangeListener.class.getSimpleName() + " execution.", t);
      }
    }
    
  }
  
  public static abstract class RepositoryEvent {
    
    protected boolean executeSync = false;
    
    public abstract void addEvent(ProjectCreationOrChangeEvent event);

    public void setExecuteSynchron(boolean executeSync) {
      this.executeSync = executeSync;
    }

  }

  
  public static class SingleRepositoryEvent extends RepositoryEvent{

    private final Long revision;
    
    public SingleRepositoryEvent(Long revision) {
      this.revision = revision;
    }

    public void addEvent(ProjectCreationOrChangeEvent event) {
      ProjectCreationOrChangeProvider.getInstance().notify(event, revision, executeSync);
    }

  }

  public static class BatchRepositoryEvent extends RepositoryEvent{
    
    private final Long revision;
    private final Collection<ProjectCreationOrChangeEvent> events;
    
    public BatchRepositoryEvent(Long revision) {
      this.revision = revision;
      this.events = new ArrayList<ProjectCreationOrChangeEvent>();
    }
    
    public void addEvent(ProjectCreationOrChangeEvent event) {
      this.events.add(event);
    }
    
    public void execute(String commitMsg) {
      ProjectCreationOrChangeProvider.getInstance().notify(events, revision, commitMsg, executeSync);
    }

  }

  
  public static class EmptyRepositoryEvent extends RepositoryEvent{
    
    public void addEvent(ProjectCreationOrChangeEvent event) {
      //nichts tun
    }
    
  }

}
