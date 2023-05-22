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
package com.gip.xyna.xprc.xfractwfe.base.parallel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.parallel.ParallelExecutor;
import com.gip.xyna.utils.parallel.ParallelTask;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xprc.xfractwfe.base.parallel.SuspendableParallelTask.State;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.Step;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_Multiple;


/**
 *
 */
public class FractalWorkflowParallelExecutorData<S extends Step> implements Serializable {
  
  private static final long serialVersionUID = 1L;
  public static final String ALL_UNTREATED = "allUntreated";
  public static final String ALL_SUSPENDED = "allSuspended";
  
  
  private final List<SuspendableParallelTask<S>> tasks;
  private volatile ConcurrentHashMap<String, SuspendableParallelTask<S>> suspendedTasks;
  private transient volatile ConcurrentLinkedQueue<XynaException> xynaExceptions;
  private transient volatile ConcurrentLinkedQueue<Throwable> throwables;
  private HashSet<String> untreatedLaneIds;
  
  
  public FractalWorkflowParallelExecutorData(List<SuspendableParallelTask<S>> tasks) {
    this.tasks = tasks;
  }


  public void addTasksToParallelExecutor(ParallelExecutor parallelExecutor, Set<String> laneIds) {
    /*
     * f�lle:
     * 1. suspended laneids starten (PE war vollst�ndig suspendiert und dann kam resume)
     * 2. initial alle starten
     * 3. resume von allem (z.b. serverstart, oder resume auf CLI) => (alles verbleibende starten -> laneIds=all_suspended)
     */
    if (laneIds.contains(ALL_UNTREATED)) {
      if (untreatedLaneIds == null) {
        //2
        //Initiale F�llung
        parallelExecutor.addTasks(tasks);
      } else {
        for (SuspendableParallelTask<S> task : tasks) {
          if (untreatedLaneIds.contains(task.getLaneId())) {
            parallelExecutor.addTask(task);
          }
        }
      }
    }

    if (laneIds.contains(ALL_SUSPENDED)) {
      //3 nur die suspendierten
      if (suspendedTasks != null) {
        parallelExecutor.addTasks(suspendedTasks.values());
      }
      //3 planned wurde noch nicht ausgef�hrt und gilt deshalb auch als "all_suspended"
      for (SuspendableParallelTask<S> task : tasks) {
        if (task.getState() == State.Planned) {
          parallelExecutor.addTask(task);
        }
      }
    } else if (suspendedTasks != null) {
      //1
      for (String laneId : laneIds) {
        SuspendableParallelTask<S> task = suspendedTasks.get(laneId);
        if (task != null) {
          parallelExecutor.addTask(task);
        }
      }
    }
    removeSuspendedTasks(laneIds);
  }


  /**
   * @param parallelExecutor
   * @param priorityThreshold 
   */
  @SuppressWarnings("unchecked")
  public void changeLowPriorityTasksToUntreated(ParallelExecutor parallelExecutor, int priorityThreshold) {
    ArrayList<ParallelTask> unworkedTasks = new ArrayList<ParallelTask>();
    parallelExecutor.drainTasksTo( unworkedTasks );
    untreatedLaneIds = new HashSet<String>();
    for( ParallelTask pt : unworkedTasks ) {
      if( pt instanceof SuspendableParallelTask ) {
        if( pt.getPriority() < priorityThreshold ) {
          untreatedLaneIds.add( ((SuspendableParallelTask<S>)pt).getLaneId() ); 
          continue;
        }
      }
      //unerwarteten ParallelTask wieder dem ParallelExecutor geben
      parallelExecutor.addTask(pt);
    }    
  }
   
  
  @Override
  public String toString() {
    return "FractalWorkflowParallelExecutorData("+
        "suspendedLanes="+(suspendedTasks==null?"[]":suspendedTasks.keySet())
        + ", #exceptions=" + ((xynaExceptions == null ? 0 : xynaExceptions.size()) + (throwables == null ? 0 : throwables.size()))  
        + ", #untreated=" + (untreatedLaneIds == null ? 0 : untreatedLaneIds.size()) 
        + ", #tasks=" + tasks.size() + ")";
  }

  /**
   * @return
   */
  public boolean hasSuspendedTasks() {
    if (suspendedTasks == null) {
      return false;
    } else {
      return ! suspendedTasks.isEmpty();
    }
  }
  
  /**
   * @return
   */
  public SuspensionCause combinedSuspensionCauses() {
    if( suspendedTasks == null || suspendedTasks.size() == 0 ) {
      throw new IllegalStateException("combinedSuspensionCauses sholud be called only if suspendedTasks exist");
    }
    if( suspendedTasks.size() == 1 ) {
      SuspendableParallelTask<S> spt = suspendedTasks.values().iterator().next();
      return spt.getSuspensionCause();
    } else {
      SuspensionCause_Multiple scm = new SuspensionCause_Multiple();
      for( SuspendableParallelTask<S> spt : suspendedTasks.values() ) {
        scm.addCause(spt.getSuspensionCause());
      }
      return scm;
    }
  }

  /**
   * @return
   */
  public boolean hasThrowables() {
    return throwables != null && ! throwables.isEmpty();
  }


  /**
   * @return
   */
  public boolean hasXynaExceptions() {
    return xynaExceptions != null && ! xynaExceptions.isEmpty();
  }


  /**
   * @return
   */
  public List<Throwable> getThrowables() {
    ArrayList<Throwable> list = new ArrayList<Throwable>();
    if( hasThrowables() ) {
      synchronized (throwables) {
        list.addAll(throwables);
      }
    }
    return list;
  }

  /**
   * @return
   */
  public List<XynaException> getXynaExceptions() {
    ArrayList<XynaException> list = new ArrayList<XynaException>();
    if( hasXynaExceptions() ) {
      synchronized (xynaExceptions) {
        list.addAll(xynaExceptions);
      }
    }
    return list;
  }

  /**
   * @param laneId
   * @param suspendableParallelTask
   * @param suspendedException
   */
  public void addSuspension(String laneId, SuspendableParallelTask<S> suspendableParallelTask,
                           ProcessSuspendedException suspendedException) {
    assureSuspendedTasksQueueExists();
    suspendedTasks.put( suspendableParallelTask.getLaneId(), suspendableParallelTask );
  }


  /**
   * @param laneId
   * @param xynaException
   */
  public void addXynaException(String laneId, XynaException xynaException) {
    assureXynaExceptionsQueueExists();
    xynaExceptions.add(xynaException);
  }


  /**
   * @param laneId
   * @param throwable
   */
  public void addThrowable(String laneId, Throwable throwable) {
    assureThrowablesQueueExists();
    throwables.add(throwable);
  }

  /**
   * @return
   */
  public boolean checkAllTasksHasFinished() {
    for( SuspendableParallelTask<S> task : tasks ) {
      if( ! task.hasFinished() ) {
        return false;
      }
    }
    return true;
  }

  
  

  private void assureSuspendedTasksQueueExists() {
    if (suspendedTasks == null) {
      synchronized (this) {
        if (suspendedTasks == null) {
          suspendedTasks = new ConcurrentHashMap<String, SuspendableParallelTask<S>>();
        }
      }
    }
  }
  

  private void assureXynaExceptionsQueueExists() {
    if (xynaExceptions == null) {
      synchronized (this) {
        if (xynaExceptions == null) {
          xynaExceptions = new ConcurrentLinkedQueue<XynaException>();
        }
      }
    }
  }


  private void assureThrowablesQueueExists() {
    if (throwables == null) {
      synchronized (this) {
        if (throwables == null) {
          throwables = new ConcurrentLinkedQueue<Throwable>();
        }
      }
    }
  }

  
  /**
   * @param laneId
   * @return
   */
  public SuspendableParallelTask<S> removeSuspendedTask(String laneId) {
    if( suspendedTasks == null ) {
      return null;
    }
    SuspendableParallelTask<S> task = suspendedTasks.remove(laneId);
    return task;
  }

  /**
   * @param laneIds
   */
  private void removeSuspendedTasks(Set<String> laneIds) {
    if( suspendedTasks == null ) {
      return;
    }
    if( laneIds.contains(ALL_SUSPENDED)) {
      suspendedTasks.clear();
      return;
    }
    for( String laneId : laneIds ) {
      suspendedTasks.remove(laneId);
    }
  }

  public List<SuspendableParallelTask<S>> getTasks() {
    return tasks;
  }
  
  
  private void writeObject(ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
    writeExceptions(s, xynaExceptions );
    writeExceptions(s, throwables );
  }
  
  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    xynaExceptions = readExceptions(s, xynaExceptions);
    throwables = readExceptions(s, throwables);
  }

  private <E extends Throwable> void writeExceptions(ObjectOutputStream s, ConcurrentLinkedQueue<E> exceptions) throws IOException {
    if( exceptions == null || exceptions.size() == 0 ) {
      s.writeInt(-1);
    } else {
      s.writeInt(exceptions.size());
      for (E e : exceptions)  {
        s.writeObject(new SerializableClassloadedException(e));
      }
    }
  }
  
  @SuppressWarnings("unchecked")
  private <E extends Throwable> ConcurrentLinkedQueue<E> readExceptions(ObjectInputStream s,
                                                                        ConcurrentLinkedQueue<E> dummyForTypeInference) throws IOException, ClassNotFoundException {
    int size = s.readInt();
    if (size <= 0) {
      return null;
    } else {
      ConcurrentLinkedQueue<E> exceptions = new ConcurrentLinkedQueue<E>();
      for (int i = 0; i < size; i++) {
        exceptions.add( (E) ((SerializableClassloadedException) s.readObject()).getThrowable());
      }
      return exceptions;
    }
  }

  public void compensate() {
    for( SuspendableParallelTask<S> spt : tasks ) {
      spt.compensate();
    }
    if( xynaExceptions != null ) {
      xynaExceptions.clear();
    }
    if( throwables != null ) {
      throwables.clear();
    }
    untreatedLaneIds = null;
  }

}
