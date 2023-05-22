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
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.parallel.ParallelTask;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.Step;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;


/**
 *
 */
public class SuspendableParallelTask<S extends Step> implements ParallelTask, Serializable {
  
  private static final long serialVersionUID = 1L;
  private static Logger logger = CentralFactoryLogging.getLogger(SuspendableParallelTask.class);
  
  
  public static final int PRIORITY_NORMAL = 0;
  public static final int PRIORITY_RESUME = 1000;
  public static final int PRIORITY_RESUME_BLOCKED = 2000;

  public enum State { Planned, Started, Suspended, Succeeded, Failed}
  
  private String laneId;
  private transient S step;
  private transient SuspensionCause suspensionCause;
  private FractalWorkflowParallelExecutor<S> exceptionHandler;
  private int priority;
  private volatile State state = State.Planned;
  private boolean compensation = false;
  
  public SuspendableParallelTask(String laneId, S step, FractalWorkflowParallelExecutor<S> exceptionHandler) {
    this.laneId = laneId;
    this.step = step;
    this.exceptionHandler = exceptionHandler;
    this.priority = PRIORITY_NORMAL;
  }
  
  @Override
  public String toString() {
    return laneId+":"+state;
  }
  
  public String getLaneId() {
    return laneId;
  }

  public int getPriority() {
    return priority;
  }
  
  public void setPriority(int priority) {
    this.priority = priority;
  }

  public S getStep() {
    return step;
  }
  
  
  public void execute() {
    if( !( state == State.Planned || state == State.Suspended ) ) {
      logger.warn( "unexpected state "+state +" in lane " + laneId +" in order "+exceptionHandler.getOrderId() );
    } else if (logger.isDebugEnabled()) {
      logger.debug("starting task " + this);
    }
    try {
      state = State.Started;
      suspensionCause = null;
      if( compensation ) {
        if (logger.isDebugEnabled()) {
          logger.debug("Compensate in laneId "+laneId+" in step " + step);
        }
        step.compensate();
      } else {
        step.execute();
      }
      state = State.Succeeded;
    } catch (ProcessSuspendedException e) {
      suspensionCause = exceptionHandler.handleProcessSuspendedException(this, e);
      state = State.Suspended;
    } catch (XynaException e) {
      exceptionHandler.handleXynaExceptions(this, e);
      state = State.Failed;
    } catch (Throwable t) {
      exceptionHandler.handleThrowable(this, t);
      state = State.Failed;
    }
  }
  

  
  @SuppressWarnings("unchecked")
  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    step = (S)((SerializableClassloadedObject) s.readObject()).getObject();
    if( state == State.Suspended ) {
      suspensionCause = (SuspensionCause)((SerializableClassloadedObject) s.readObject()).getObject();
    }
    priority = PRIORITY_NORMAL; //nach der Ausf�hrung zur�cksetzen
  }

  private void writeObject(ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
    s.writeObject(new SerializableClassloadedObject(step));
    if( state == State.Suspended ) {
      s.writeObject(new SerializableClassloadedObject(suspensionCause));
    }
  }

  /**
   * @return
   */
  public boolean hasFinished() {
    return state == State.Succeeded || state == State.Failed;
  }
  
  public boolean hasStarted () {
    return state == State.Started;
  }

  public boolean isSuspended() {
    return state == State.Suspended;
  }
  
  public State getState() {
    return state;
  }

  public SuspensionCause getSuspensionCause() {
    return suspensionCause;
  }

  public void compensate() {
    compensation = true;
    state = State.Planned;
  }

}
