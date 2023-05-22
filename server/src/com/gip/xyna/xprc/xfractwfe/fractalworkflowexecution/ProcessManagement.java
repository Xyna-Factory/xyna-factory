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
package com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_PROCESS_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.xfractwfe.WorkflowInstancePool;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.EmptyWorkflow;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.fractalplanning.DefaultPlanning;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCall;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xpce.planning.EmptyPlanning;



public class ProcessManagement extends FunctionGroup implements IPropertyChangeListener {

  private static final Logger logger = CentralFactoryLogging.getLogger(ProcessManagement.class);
  public static final String DEFAULT_NAME = "ProcessManager";
  
  private static class WorkflowInstancePoolKey {
    private final long definingRevision;
    private final String fqClassName;
    private final Set<Long> ownerRevisions = new HashSet<Long>();
    
    public WorkflowInstancePoolKey(String fqClassName, long definingRevision) {
      this.fqClassName = fqClassName;
      this.definingRevision = definingRevision;
    }
  }
  
  private static class WorkflowInstancePoolMgmt {
    
    private final Map<WorkflowInstancePoolKey, WorkflowInstancePool> pools = new HashMap<WorkflowInstancePoolKey, WorkflowInstancePool>();
    private final RuntimeContextDependencyManagement rcdm;
    
    private WorkflowInstancePoolMgmt() {
      this.rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    }
    
    private Pair<Long, String> key(DestinationValue dv, Long revision) {
      Long definingRevision = rcdm.getRevisionDefiningXMOMObject(dv.getOriginalFqName(dv.getFQName(), revision), revision);
      if (definingRevision == null) {
        throw new RuntimeException();
      }
      return Pair.of(definingRevision, dv.getFQName());
    }


    public boolean remove(DestinationValue dv) {
      // TODO Auto-generated method stub
      return false;
    }

    public void changeMaxSize(Integer poolsize) {
      // TODO Auto-generated method stub
      
    }

    public XynaProcess getFromLazyCreatedPool(DestinationValue dv) {
      WorkflowInstancePool wfPool;

 /*     final Lock readLock = rwLock.readLock();
      readLock.lock();
      try {
        wfPool = pools.get(dv);
      } finally {
        readLock.unlock();
      }

      if (wfPool != null) {
        process = wfPool.getProcessInstance();
        // store the id to be able to differentiate between pool versions
        dv.setPoolId(wfPool.getID());
        if (process != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("Got workflow instance from pool for " + dv.getFQName());
          }
          return process;
        }
      } else {

        if (poolsize == null) {
          poolsize = XynaProperty.XYNA_WORKFLOW_POOL_SIZE.get();
        }
        if (logger.isInfoEnabled()) {
          logger.info("Creating new workflow pool for " + dv.getFQName() + " (size " + poolsize + ")");
        }
        wfPool = new WorkflowInstancePool(poolsize);
        dv.setPoolId(wfPool.getID());
        // FIXME This should not add the original object to the map.
        //       Make DestinationValue implement Cloneable? Watch out for backward compatability since DestinvationValue may exist in serialized objects!

        final Lock writeLock = rwLock.writeLock();
        writeLock.lock();
        try {
          pools.put(dv, wfPool);
        } finally {
          writeLock.unlock();
        }

      }*/

      // TODO Auto-generated method stub
      return null;
    }

    public void returnProcessInstance(DestinationValue dv, XynaProcess instance) {
   /*   final Lock readLock = rwLock.readLock();
      readLock.lock();
      try {

        WorkflowInstancePool pool = pools.get(dv);

        if (pool != null) {
          if (pool.getID() == dv.getPoolId()) {
            logger.debug("Re-adding workflow instance to workflow pool");
            pool.returnProcessInstance(instance);
          } else {
            logger.info("Returned pool id does not match current pool id, discarding instance");
          }
        }

      } finally {
        readLock.unlock();
      }*/

    }
    
  }

  private WorkflowInstancePoolMgmt pools;
  private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
  private Integer poolsize = null;

  static {
    try {
      addDependencies(ProcessManagement.class,
                      new ArrayList<XynaFactoryPath>(Arrays.asList(new XynaFactoryPath[] {
                          new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryManagementODS.class,
                                              Configuration.class),
                          new XynaFactoryPath(XynaProcessing.class, XynaFractalWorkflowEngine.class,
                                              DeploymentHandling.class)})));
      //WorkflowDatabase darf erst nach der klasse geladen werden die einen deploymenthandler definiert! siehe WorkflowDatabase

    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error("", t);
    }
  }


  public ProcessManagement() throws XynaException {
    super();
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public void init() throws XynaException {
    pools = new WorkflowInstancePoolMgmt();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
        .addPropertyChangeListener(this);
    XynaProperty.XYNA_WORKFLOW_POOL_SIZE.registerDependency(DEFAULT_NAME);
    
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
        .addDeploymentHandler(DeploymentHandling.PRIORITY_PROCESS_MANAGER, new DeploymentHandler() {

          public void exec(GenerationBase o, DeploymentMode mode) {
            if (o instanceof WF) {
              clearInstancePool(new FractalWorkflowDestination(o.getFqClassName()), o.getRevision());
            } else if (o instanceof DOM) {
              //f�r nicht statische methoden merken, ob die methoden als workflow implementiert sind, oder nicht
              DOM dom = (DOM) o;
              ConcurrentMap<String, ConcurrentMap<String, Boolean>> map = XynaProcess.instanceMethodTypes.get(o.getRevision());
              if (map == null) {
                ConcurrentMap<String, ConcurrentMap<String, Boolean>> newMap =
                    new ConcurrentHashMap<String, ConcurrentMap<String, Boolean>>(16, 0.75f, 2);
                if (null == (map = XynaProcess.instanceMethodTypes.putIfAbsent(o.getRevision(), newMap))) {
                  map = newMap;
                }
              }
              ConcurrentMap<String, Boolean> map2 = map.get(dom.getFqClassName());
              if (map2 != null) {
                map2.clear();
              }
              
              for (Operation operation : dom.getOperations()) {
                if (!operation.isStatic()) {
                  boolean isWorkflow = operation instanceof WorkflowCall;
                  if (map2 == null) {
                    ConcurrentMap<String, Boolean> newMap2 = new ConcurrentHashMap<String, Boolean>(16, 0.75f, 1);
                    if (null == (map2 = map.putIfAbsent(dom.getFqClassName(), newMap2))) {
                      map2 = newMap2;
                    }
                  }
                  map2.put(operation.getName(), isWorkflow);
                }
              }
            }
          }

          public void finish(boolean success) throws XPRC_DeploymentHandlerException {
          }

          @Override
          public void begin() throws XPRC_DeploymentHandlerException {
          }

        });

    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
        .addUndeploymentHandler(DeploymentHandling.PRIORITY_PROCESS_MANAGER, new UndeploymentHandler() {

          public void exec(GenerationBase o) {
            if (o instanceof WF) {
              clearInstancePool(new FractalWorkflowDestination(o.getFqClassName()), o.getRevision());
            } else if (o instanceof DOM) {
              DOM dom = (DOM) o;
              ConcurrentMap<String, ConcurrentMap<String, Boolean>> map = XynaProcess.instanceMethodTypes.get(o.getRevision());
              if (map != null) {
                map.remove(dom.getFqClassName());
              }
            }
          }


          public void exec(FilterInstanceStorable object) {
          }

          public void exec(TriggerInstanceStorable object) {
          }

          public void exec(Capacity object) {
          }

          public void exec(DestinationKey object) {
          }

          public void finish() throws XPRC_UnDeploymentHandlerException {
          }
          
          public boolean executeForReservedServerObjects(){
            return false;
          }


          public void exec(FilterStorable object) {
          }


          public void exec(TriggerStorable object) {
          }
        });
  }


  public void shutdown() throws XynaException {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
        .removePropertyChangeListener(this);
  }


  public XynaProcess getProcess(DestinationValue dv, Long rootRevision) throws XPRC_PROCESS_NOT_FOUND {

    XynaProcess process;

    // check whether the destination value is allowed for pooling and if such a pool exists, try to get
    // an instance out of it. the use of read and write locks should guarantee that locking is only
    // required when creating a new pool which is considered a rare event
    if (dv.isPoolable()) {
      process = pools.getFromLazyCreatedPool(dv);
      if (process != null) {
        return process;
      }
    }

    if (XynaDispatcher.DESTINATION_EMPTY_PLANNING.getFQName().equals(dv.getFQName())) {
      return new EmptyPlanning();
    }
    if (XynaDispatcher.DESTINATION_DEFAULT_PLANNING.getFQName().equals(dv.getFQName())) {
      return new DefaultPlanning();
    }
    if (XynaDispatcher.DESTINATION_EMPTY_WORKFLOW.getFQName().equals(dv.getFQName())) {
      return new EmptyWorkflow();
    }

    // process mit entsprechendem classloader laden
    return instantiateWF(dv.getFQName(), rootRevision);

  }


  private XynaProcess instantiateWF(String fqClassName, Long revision) throws XPRC_PROCESS_NOT_FOUND {

    if (logger.isDebugEnabled()) {
      logger.debug(XynaFractalWorkflowEngine.DEFAULT_NAME + ": Instantiate process " + fqClassName);
    }

    Class<?> wfcl;
    try {
      wfcl =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
              .loadWFClass(fqClassName, revision);
    } catch (ClassNotFoundException e) {
      throw new XPRC_PROCESS_NOT_FOUND(fqClassName, e);
    }
    try {
      return (XynaProcess) wfcl.getConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }


  public void returnProcessInstances(DestinationValue dv, XynaProcess instance) {

    instance.setNeedsReinitialization();

    pools.returnProcessInstance(dv, instance);

  }


  public void clearInstancePool(DestinationValue dv, long revision) {
    if (pools.remove(dv)) {
      if (logger.isInfoEnabled()) {
        logger.info("Cleared workflow instance pool for " + dv.getFQName());
      }
    } else if (logger.isTraceEnabled()) {
      logger.trace("Workflow pool for '" + dv.getFQName() + "' is already empty and does not need to be cleared");
    }
  }


  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> watches = new ArrayList<String>();
    watches.add(XynaProperty.XYNA_WORKFLOW_POOL_SIZE.getPropertyName());
    return watches;
  }


  public void propertyChanged() {
    logger.debug("Workflow instance pool size parameter changed, setting pools' max sizes");
    
    poolsize = XynaProperty.XYNA_WORKFLOW_POOL_SIZE.read();
    pools.changeMaxSize(poolsize);
  }

}
