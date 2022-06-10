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

package com.gip.xyna.xprc.xfractwfe.base;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;



/**
 */
public class DeploymentHandling extends FunctionGroup {


  public static final String DEFAULT_NAME = DeploymentHandling.class.getSimpleName();

  /*
   * The following constants are supposed to be used and extended when adding a new deployment handler to ensure the
   * correct order on which the handlers are being called. Example: The class loading has to be updated before the step
   * handling can attach the new step handlers to the process instance.
   * deployment: small priorities are executed first
   * undeployment: small priorities are executed last
   */
  public static final Integer PRIORITY_DEPENDENCY_CREATION = 1;
  public static final Integer PRIORITY_CLASS_LOADER_UNDEPLOY_OLD = 2;
  public static final Integer PRIORITY_EXCHANGE_ADDITIONAL_LIBS = 3;
  public static final Integer PRIORITY_CLASS_LOADER_RECREATE_CLASSLOADERS = 4;
  
  //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Achtung: enum "GenerationBase.DeploymentState" anpassen, wenn neue Prios definiert werden !!!!!!!!!!!!!!!!!!!
  //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  
  public static final Integer PRIORITY_PRE_ONDEPLOYMENT = 5;
  public static final Integer PRIORITY_CLASS_LOADER_DEPLOY_NEW = 6;
  public static final Integer PRIORITY_PROCESS_MANAGER = 24;
  public static final Integer PRIORITY_EXCEPTION_DATABASE = 25;
  public static final Integer PRIORITY_WORKFLOW_DATABASE = 26;
  public static final Integer PRIORITY_XPRC = 27;
  public static final Integer PRIORITY_REMOTESERIALIZATION = 28;

  public static final Integer[] allPriorities = new Integer[] {
     PRIORITY_DEPENDENCY_CREATION,
     PRIORITY_CLASS_LOADER_UNDEPLOY_OLD,
     PRIORITY_EXCHANGE_ADDITIONAL_LIBS,
     PRIORITY_CLASS_LOADER_RECREATE_CLASSLOADERS,
     PRIORITY_PRE_ONDEPLOYMENT,
     PRIORITY_CLASS_LOADER_DEPLOY_NEW,
     PRIORITY_PROCESS_MANAGER,
     PRIORITY_EXCEPTION_DATABASE,
     PRIORITY_WORKFLOW_DATABASE,
     PRIORITY_XPRC,
     PRIORITY_REMOTESERIALIZATION
   };


  public static interface DeploymentHandler {
    
    public void begin() throws XPRC_DeploymentHandlerException;

    public void exec(GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException;
    
    public void finish(boolean success) throws XPRC_DeploymentHandlerException;

  }

  public static interface UndeploymentHandler {

    //FIXME: objekte sind im gegensatz zum deployment hier in einem status, wo sie fast leer sind.
    //nur fqclassname ist gesetzt, es wurde evtl nicht das xml geparst!
    public void exec(GenerationBase object) throws XPRC_UnDeploymentHandlerException;

    public void exec(FilterInstanceStorable object);
    
    public void exec(TriggerInstanceStorable object);
    
    public void exec(FilterStorable object);
    
    public void exec(TriggerStorable object);
    
    public void exec(Capacity object);
    
    public void exec(DestinationKey object);

    /**
     * wird nur für generationbase objekte ausgeführt
     */
    public void finish() throws XPRC_UnDeploymentHandlerException;
    
    /**
     * soll der UndeploymentHandler auch für reservedServerObjects ausgeführt werden?
     */
    public boolean executeForReservedServerObjects();
  }

  private final ConcurrentMap<Integer, List<DeploymentHandler>> deploymentHandlers =
      new ConcurrentHashMap<Integer, List<DeploymentHandler>>(16, 0.75f, 2);

  private final ConcurrentMap<Integer, List<UndeploymentHandler>> undeploymentHandlers =
      new ConcurrentHashMap<Integer, List<UndeploymentHandler>>(16, 0.75f, 2);


  public DeploymentHandling() throws XynaException {
    super();
  }

  public void executeDeploymentHandler(Integer handlerPriority, GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {
    for (DeploymentHandler d : getDeploymentHandlers(handlerPriority)) {
      if (logger.isTraceEnabled()) {
        logger.trace("Executing deployment handler " + d + " for " + object.getOriginalFqName());
      }
      d.exec(object, mode);
    }
  }


  private List<DeploymentHandler> getDeploymentHandlers(Integer p) {
    List<DeploymentHandler> l = deploymentHandlers.get(p);
    if (l == null) {
      return Collections.emptyList();
    }
    synchronized (l) {
      return new ArrayList<DeploymentHandler>(l);
    }
  }
  
  private List<UndeploymentHandler> getUnDeploymentHandlers(Integer p) {
    List<UndeploymentHandler> l = undeploymentHandlers.get(p);
    if (l == null) {
      return Collections.emptyList();
    }
    synchronized (l) {
      return new ArrayList<UndeploymentHandler>(l);
    }
  }

  public void notifyDeploymentHandlerFinish(Integer handlerPriority, boolean success) throws XPRC_DeploymentHandlerException {
      List<Throwable> exceptions = null;
      for (DeploymentHandler d : getDeploymentHandlers(handlerPriority)) {
        if (logger.isTraceEnabled()) {
          logger.trace("Executing deployment handler " + d + " finish.");
        }
        try {
          d.finish(success);
        } catch (Throwable t) {
          Department.handleThrowable(t);
          if (exceptions == null) {
            exceptions = new ArrayList<Throwable>();
          }
          exceptions.add(t);
        }
      }
      if (exceptions != null) {
        if (exceptions.size() == 1) {
          Throwable t = exceptions.get(0);
          if (t instanceof XPRC_DeploymentHandlerException) {
            throw (XPRC_DeploymentHandlerException) t;
          } else if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
          } else if (t instanceof Error) {
            throw (Error) t;
          } else {
            throw new RuntimeException(t);
          }
        } else {
          throw (XPRC_DeploymentHandlerException) new XPRC_DeploymentHandlerException("unknown", "more than one").initCauses(exceptions
              .toArray(new Throwable[exceptions.size()]));
        }
      }
  }

  public void notifyDeploymentHandlerBegin(Integer handlerPriority) throws XPRC_DeploymentHandlerException {
    List<Throwable> exceptions = null;
    for (DeploymentHandler d : getDeploymentHandlers(handlerPriority)) {
      if (logger.isTraceEnabled()) {
        logger.trace("Executing deployment handler " + d + " finish.");
      }
      try {
        d.begin();
      } catch (Throwable t) {
        Department.handleThrowable(t);
        if (exceptions == null) {
          exceptions = new ArrayList<Throwable>();
        }
        exceptions.add(t);
      }
    }
    if (exceptions != null) {
      if (exceptions.size() == 1) {
        Throwable t = exceptions.get(0);
        if (t instanceof XPRC_DeploymentHandlerException) {
          throw (XPRC_DeploymentHandlerException) t;
        } else if (t instanceof RuntimeException) {
          throw (RuntimeException) t;
        } else if (t instanceof Error) {
          throw (Error) t;
        } else {
          throw new RuntimeException(t);
        }
      } else {
        throw (XPRC_DeploymentHandlerException) new XPRC_DeploymentHandlerException("unknown", "more than one").initCauses(exceptions
            .toArray(new Throwable[exceptions.size()]));
      }
    }
  }

  public void executeUndeploymentHandler(Integer handlerPriority, GenerationBase object) throws XPRC_UnDeploymentHandlerException {
      for (UndeploymentHandler d : getUnDeploymentHandlers(handlerPriority)) {
        //für ReservedServerObjects dürfen nur spezielle UndeploymentHandler ausgeführt werden
        if (!object.isReservedServerObject() || d.executeForReservedServerObjects()) {
          if (logger.isTraceEnabled()) {
            logger.trace("Executing undeployment handler " + d);
          }
          d.exec(object);
        }
      }
  }


  public void finishUndeploymentHandler(Integer handlerPriority) throws XPRC_UnDeploymentHandlerException {
      for (UndeploymentHandler d : getUnDeploymentHandlers(handlerPriority)) {
        if (logger.isTraceEnabled()) {
          logger.trace("Executing undeployment handler finish for " + d);
        }
        d.finish();
      }
  }

  public void executeUndeploymentHandler(Integer handlerPriority, FilterInstanceStorable object) throws XPRC_UnDeploymentHandlerException {
      for (UndeploymentHandler d : getUnDeploymentHandlers(handlerPriority)) {
        if (logger.isTraceEnabled()) {
          logger.trace("Executing undeployment handler " + d);
        }
        d.exec(object);
      }
  }

  
  public void executeUndeploymentHandler(Integer handlerPriority, TriggerInstanceStorable object) throws XPRC_UnDeploymentHandlerException {
      for (UndeploymentHandler d : getUnDeploymentHandlers(handlerPriority)) {
        if (logger.isTraceEnabled()) {
          logger.trace("Executing undeployment handler " + d);
        }
        d.exec(object);
      }
  }

  public void executeUndeploymentHandler(Integer handlerPriority, FilterStorable object) throws XPRC_UnDeploymentHandlerException {
      for (UndeploymentHandler d : getUnDeploymentHandlers(handlerPriority)) {
        if (logger.isTraceEnabled()) {
          logger.trace("Executing undeployment handler " + d);
        }
        d.exec(object);
      }
  }

  
  public void executeUndeploymentHandler(Integer handlerPriority, TriggerStorable object) throws XPRC_UnDeploymentHandlerException {
      for (UndeploymentHandler d : getUnDeploymentHandlers(handlerPriority)) {
        if (logger.isTraceEnabled()) {
          logger.trace("Executing undeployment handler " + d);
        }
        d.exec(object);
      }
  }
  
  public void executeUndeploymentHandler(Integer handlerPriority, Capacity object) throws XPRC_UnDeploymentHandlerException {
      for (UndeploymentHandler d : getUnDeploymentHandlers(handlerPriority)) {
        if (logger.isTraceEnabled()) {
          logger.trace("Executing undeployment handler " + d);
        }
        d.exec(object);
      }
  }
  

  public void executeUndeploymentHandler(Integer handlerPriority, DestinationKey object) throws XPRC_UnDeploymentHandlerException {
      for (UndeploymentHandler d : getUnDeploymentHandlers(handlerPriority)) {
        if (logger.isTraceEnabled()) {
          logger.trace("Executing undeployment handler " + d);
        }
        d.exec(object);
      }
  }
  

  public void addDeploymentHandler(Integer priority, DeploymentHandler handler) {
    List<DeploymentHandler> relevantList = deploymentHandlers.get(priority);
    if (relevantList == null) {
      relevantList = new ArrayList<DeploymentHandler>();
      List<DeploymentHandler> prev = deploymentHandlers.putIfAbsent(priority, relevantList);
      if (prev != null) {
        relevantList = prev;
      }
    }
    synchronized (relevantList) {
      relevantList.add(handler);
      if (logger.isDebugEnabled()) {
        logger.debug("added deployment handler (prio " + priority + "): " + relevantList.size());
      }
    }
  }
  

  public void addUndeploymentHandler(Integer priority, UndeploymentHandler handler) {
    List<UndeploymentHandler> relevantList = undeploymentHandlers.get(priority);
    if (relevantList == null) {
      relevantList = new ArrayList<UndeploymentHandler>();
      List<UndeploymentHandler> prev = undeploymentHandlers.putIfAbsent(priority, relevantList);
      if (prev != null) {
        relevantList = prev;
      }
    }
    synchronized (relevantList) {
      relevantList.add(handler);
      if (logger.isDebugEnabled()) {
        logger.debug("added undeployment handler (prio " + priority + "): " + relevantList.size());
      }
    }
  }


  public void removeDeploymentHandler(DeploymentHandler handler) {
    for (List<DeploymentHandler> handlerList: deploymentHandlers.values()) {
      synchronized (handlerList) {
        handlerList.remove(handler);
      }
    }
  }


  public void removeUndeploymentHandler(UndeploymentHandler handler) {
    for (List<UndeploymentHandler> handlerList: undeploymentHandlers.values()) {
      synchronized (handlerList) {
        handlerList.remove(handler);
      }
    }
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  public void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(DeploymentHandling.class, "DeploymentHandling").execAsync(); //nur Marker
  }

  @Override
  public void shutdown() throws XynaException {
  }


}
