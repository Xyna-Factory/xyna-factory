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
package com.gip.xyna.xprc.xfractwfe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_ResumeFailedException;
import com.gip.xyna.xprc.exceptions.XPRC_WorkflowProtectionModeViolationException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.DispatcherType;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.WorkflowRevision;
import com.gip.xyna.xprc.xfractwfe.OrderFilterAlgorithmsImpl.OrderFilter;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xsched.AllOrdersList;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;


/**
 * This class encapsulates all simultaneous running deployments
 * 
 * - sammelt alle gleichzeitigen Deployments
 * - in der Regel gibt es nur einen aktiven DeploymentProcess und ggf. einen Wartenden (wenn ein
 *   Deployment gerade läuft und gleichzeitig ein weiteres angestoßen wird)
 * - falls ein Objekt von unterschiedlichen Deployments mit unterschiedlichen Deploymentmodes deployt
 *   werden soll, gewinnt der stärkste Deploymentmode
 * - ein Deployment im DeploymentProcess übernimmt die Führerschaft und prüft, ob die betroffene Workflows
 *   in Benutzung sind. Ebenfalls werden Algorithmen ausgetauscht, um ein weiteres Einstellen von Aufträgen verhindern zu können
 * - die anderen Deployments im DeploymentProcess warten, bis das führende Deployment die Prüfung beendet hat
 * - falls das führende Deployment bei der Prüfung feststellt, dass das Deployment aufgrund des Deploymentmodes
 *   abgebrochen werden muss, übernimmt ein anderes Deployment die Führerschaft
 * - anschließend werden die Deployments parallel durchgeführt
 * - nach Beendigung aller Deployments werden die Algorithmen wieder durch die Defaultalgorithmen von einem führendem Deployment
 *   erstetzt - hierbei warten die restlichen Deployments, bis dieser Schritt abgeschlossen ist
 * - aufgehaltene Aufträge/Crons werden eingestellt
 * 
 */
public class DeploymentProcess {

  private static Logger logger = CentralFactoryLogging.getLogger(DeploymentProcess.class);

  private final Map<Long, Set<WorkflowRevision>> affectedWorkflowsPerDeployment; //affectedWorkflows per single Deployment
  private final Map<Long, GenerationBase.WorkflowProtectionMode> workflowProtModePerDeployment;
  private final List<Long> pausedFqCtrlTasks = new ArrayList<Long>(); //all FrequencyControlledTasks paused for this deployment process
  private final List<Long> pausedResumes = new ArrayList<Long>(); //all Resumes paused for this deployment process
  private final List<Long> pausedScheduling = new ArrayList<Long>(); //all entries in scheduler paused for this deployment process
  
  private boolean caresForInterfaceChanges;
  private final Map<Long, Set<WorkflowRevision>> interfaceDependentWorkflows; //this is the subset of affectedWorkflowsPerDeployment that is directly using the changed object
  private final List<ResumeTarget> resumeTargets = new ArrayList<ResumeTarget>(); //all orders that were suspended for this DeploymentProcess (excluding the ones from the PreSchedulerAlgorithm)
  
  private final Map<Long, Object> pillows;
  private volatile Set<Long> failedDeployments = new HashSet<Long>();
  private WorkflowRevision workflowIdentifierOfProtectedWF;

  private final List<OrderFilterDeployment> ofs = new ArrayList<OrderFilterDeployment>();
  
  private Long idOfLeader;
  final AtomicBoolean struggelingForLeadership = new AtomicBoolean(false);
  final AtomicBoolean someoneWantsToCleanUp = new AtomicBoolean(false);
  private CyclicBarrier cleanupBarrier;
  final Set<Long> participatingThreads;
  

  public static enum DeploymentProcessState {
    PRE_DEPLOYMENT, DEPLOYMENT
  }


  private DeploymentProcessState processState;

  
  
  DeploymentProcess(Set<WorkflowRevision> myAffectedWorkflows, GenerationBase.WorkflowProtectionMode myProtMode) {
    this(myAffectedWorkflows, myProtMode, null);    
  }


  DeploymentProcess(Set<WorkflowRevision> myAffectedWorkflows, GenerationBase.WorkflowProtectionMode myProtMode,
                    Set<WorkflowRevision> usingWorkflows) {
    affectedWorkflowsPerDeployment = new ConcurrentHashMap<Long, Set<WorkflowRevision>>();
    interfaceDependentWorkflows = new ConcurrentHashMap<Long, Set<WorkflowRevision>>();
    workflowProtModePerDeployment = new ConcurrentHashMap<Long, GenerationBase.WorkflowProtectionMode>();
    pillows = new HashMap<Long, Object>();
    
    Set<WorkflowRevision> transformXMLNameSetToJavaNames = myAffectedWorkflows;//DeploymentProcess.transformXMLNameSetToJavaNames(myAffectedWorkflows, revision);
    OrderFilterDeployment of = new OrderFilterDeployment(transformXMLNameSetToJavaNames);
    ofs.add(of);
    DeploymentManagement.getInstance().blockWorkflowProcessingForDeployment(of);
    Long myId = DeploymentManagement.getInstance().propagateDeployment();
    affectedWorkflowsPerDeployment.put(myId, myAffectedWorkflows);
    workflowProtModePerDeployment.put(myId, myProtMode);
    processState = DeploymentProcessState.PRE_DEPLOYMENT;
    if (usingWorkflows != null) {
      caresForInterfaceChanges = true;
      interfaceDependentWorkflows.put(myId, usingWorkflows);
    }
    idOfLeader = myId;
    participatingThreads = new HashSet<Long>();
    participatingThreads.add(Thread.currentThread().getId());
  }


  private void cleanFromMaps(Long id) {
    affectedWorkflowsPerDeployment.remove(id);
    workflowProtModePerDeployment.remove(id);
    if (interfaceDependentWorkflows != null) {
      interfaceDependentWorkflows.remove(id);
      if (interfaceDependentWorkflows.size() == 0) {
        caresForInterfaceChanges = false;
      }
    }
  }


  public synchronized void progressState() {
    if (this.processState == DeploymentProcessState.PRE_DEPLOYMENT) {
      this.processState = DeploymentProcessState.DEPLOYMENT;
    }
  }


  public synchronized DeploymentProcessState getState() {
    return processState;
  }


  public void resumeDeployment() {
    //set that stuff up before waking the others
    logger.debug("creating barrier for cleanup with size: " + affectedWorkflowsPerDeployment.size());
    //initialize the cleanUp-Barrier with the size of participated deployments
    cleanupBarrier = new CyclicBarrier(affectedWorkflowsPerDeployment.size());
    someoneWantsToCleanUp.set(false); //we'll need a new leader for cleanup

    synchronized (pillows) {
      for (Object pillow : pillows.values()) { //wake up everyone and continue the actual Deployment
        synchronized (pillow) {
          pillow.notify();
        }
      }
    }
  }


  public boolean caresForInterfaceChanges() {
    return caresForInterfaceChanges;
  }


  public Collection<Long> getParticipatedDeploymentIds() {
    Set<Long> setCopy = new HashSet<Long>();
    setCopy.addAll(affectedWorkflowsPerDeployment.keySet());
    return setCopy;
  }


  public synchronized Map<GenerationBase.WorkflowProtectionMode, Set<WorkflowRevision>> generateMapping() {

    Map<GenerationBase.WorkflowProtectionMode, Set<WorkflowRevision>> generatedMapping = new HashMap<GenerationBase.WorkflowProtectionMode, Set<WorkflowRevision>>();

    Set<Long> allDeployments = affectedWorkflowsPerDeployment.keySet();
    for (Long deploymentId : allDeployments) {
      GenerationBase.WorkflowProtectionMode myProtectionMode = workflowProtModePerDeployment.get(deploymentId);
      Set<WorkflowRevision> myAffectedWorkflows = new HashSet<WorkflowRevision>();
      myAffectedWorkflows.addAll(affectedWorkflowsPerDeployment.get(deploymentId));
      if (generatedMapping.containsKey(myProtectionMode)) {
        Set<WorkflowRevision> oldAffectedWorkflows = generatedMapping.get(myProtectionMode);
        if (oldAffectedWorkflows != null) {
          oldAffectedWorkflows.addAll(myAffectedWorkflows);
        } else {
          generatedMapping.put(myProtectionMode, myAffectedWorkflows);
        }
      } else {
        generatedMapping.put(myProtectionMode, myAffectedWorkflows);
      }
    }

    Set<WorkflowRevision> doubleChecks = generatedMapping.get(WorkflowProtectionMode.BREAK_ON_USAGE);
    if (doubleChecks != null && doubleChecks.size() > 0) {
      Iterator<WorkflowRevision> checkingIter = doubleChecks.iterator();
      while (checkingIter.hasNext()) {
        WorkflowRevision ordertype = checkingIter.next();
        Set<WorkflowRevision> subSet = getAllHigherModes(generatedMapping, WorkflowProtectionMode.BREAK_ON_USAGE);
        if (subSet != null && subSet.size() > 0) {
          if (subSet.contains(ordertype)) {
            checkingIter.remove();
          }
        }
      }
    }

    doubleChecks = generatedMapping.get(WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES);
    if (doubleChecks != null && doubleChecks.size() > 0) {
      Iterator<WorkflowRevision> checkingIter = doubleChecks.iterator();
      while (checkingIter.hasNext()) {
        WorkflowRevision ordertype = checkingIter.next();
        Set<WorkflowRevision> subSet = getAllHigherModes(generatedMapping, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES);
        if (subSet != null && subSet.size() > 0) {
          if (subSet.contains(ordertype)) {
            checkingIter.remove();
          }
        }
      }
    }

    doubleChecks = generatedMapping.get(WorkflowProtectionMode.FORCE_DEPLOYMENT);
    if (doubleChecks != null && doubleChecks.size() > 0) {
      Iterator<WorkflowRevision> checkingIter = doubleChecks.iterator();
      while (checkingIter.hasNext()) {
        WorkflowRevision ordertype = checkingIter.next();
        Set<WorkflowRevision> subSet = getAllHigherModes(generatedMapping, WorkflowProtectionMode.FORCE_DEPLOYMENT);
        if (subSet != null && subSet.size() > 0) {
          if (subSet.contains(ordertype)) {
            checkingIter.remove();
          }
        }
      }
    }
    return generatedMapping;
  }


  private Set<WorkflowRevision> getAllHigherModes(Map<GenerationBase.WorkflowProtectionMode, Set<WorkflowRevision>> generatedMapping,
                                        WorkflowProtectionMode mode) {
    Set<WorkflowRevision> ret = new HashSet<WorkflowRevision>();
    switch (mode) {
      case BREAK_ON_USAGE :
        if (generatedMapping.containsKey(WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES)) {
          ret.addAll(generatedMapping.get(WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES));
        }
      case BREAK_ON_INTERFACE_CHANGES :
        if (generatedMapping.containsKey(WorkflowProtectionMode.FORCE_DEPLOYMENT)) {
          ret.addAll(generatedMapping.get(WorkflowProtectionMode.FORCE_DEPLOYMENT));
        }
      case FORCE_DEPLOYMENT :
        if (generatedMapping.containsKey(WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT)) {
          ret.addAll(generatedMapping.get(WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT));
        }
        break;
    }
    return ret;
  }


  public void addDeployment(Set<WorkflowRevision> myAffectedWorkflows, GenerationBase.WorkflowProtectionMode myProtMode) {
    addDeployment(myAffectedWorkflows, myProtMode, null);
  }


  public static class OrderFilterDeployment implements OrderFilter {
    
    private final Set<WorkflowRevision> affectedWorkflows;
    
    public OrderFilterDeployment(Set<WorkflowRevision> affectedWorkflows) {
      this.affectedWorkflows = new HashSet<WorkflowRevision>(affectedWorkflows);
    }
    
    public boolean filterForAddOrderToScheduler(XynaOrderServerExtension xo) {
      try {
        Set<WorkflowRevision> resolved = WorkflowRevision.construct(DispatcherType.Execution, xo.getDestinationKey());
        for (WorkflowRevision workflowRevision : resolved) {
          if (affectedWorkflows.contains(workflowRevision)) {
            return true;
          }
        }
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        return false;
      }
      return false;
    }


    public boolean filterForCheckOrderReadyForProcessing(XynaOrderServerExtension xo, DispatcherType type) {
      try {
        Set<WorkflowRevision> resolved = WorkflowRevision.construct(DispatcherType.Execution, xo.getDestinationKey());
        for (WorkflowRevision workflowRevision : resolved) {
          if (affectedWorkflows.contains(workflowRevision)) {
            if (logger.isDebugEnabled()) {
              logger.debug(new StringBuilder().append("Order ").append(xo.getId())
                  .append(" is affected by the active deployment and is held at a processor"));
            }
            return true;
          }
        }
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        return false;
      }
      return false;
    }


    public boolean startUnderlyingOrder(CronLikeOrder cronLikeOrder, CronLikeOrderCreationParameter clocp, ResponseListener rl) {
      try {
        Set<WorkflowRevision> resolved = WorkflowRevision.construct(DispatcherType.Execution, clocp.getDestinationKey());
        for (WorkflowRevision workflowRevision : resolved) {
          if (affectedWorkflows.contains(workflowRevision)) {
            if (logger.isDebugEnabled()) {
              logger.debug(new StringBuilder().append("Cron like order with creation parameters for ").append(clocp.getOrderType())
                  .append(" is affected by the active deployment and is saved until after the deployment"));
            }
  
            return true;
          }
        }
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        return false;
      }
      return false;
    }


    public void continueOrderReadyForProcessing(XynaOrderServerExtension xo) {
      if (logger.isDebugEnabled()) {
        logger.debug(new StringBuilder().append("Order ").append(xo.getId())
            .append(" has been awoken at a processor"));
      }
    }
    
  }
  
  public long addDeployment(Set<WorkflowRevision> myAffectedWorkflows, GenerationBase.WorkflowProtectionMode myProtMode,
                            Set<WorkflowRevision> usingWorkflows) {
    participatingThreads.add(Thread.currentThread().getId());
    // propagate into Algorithm
    Set<WorkflowRevision> transformXMLNameSetToJavaNames = myAffectedWorkflows; //DeploymentProcess.transformXMLNameSetToJavaNames(myAffectedWorkflows, revision);
    OrderFilterDeployment of = new OrderFilterDeployment(transformXMLNameSetToJavaNames);
    ofs.add(of);
    DeploymentManagement.getInstance().blockWorkflowProcessingForDeployment(of);

    Long myId = DeploymentManagement.getInstance().propagateDeployment();

    // add to everyMap
    Set<WorkflowRevision> tmpSet = new HashSet<DeploymentManagement.WorkflowRevision>(myAffectedWorkflows);
    
    affectedWorkflowsPerDeployment.put(myId,tmpSet);
    workflowProtModePerDeployment.put(myId, myProtMode);
    if (usingWorkflows != null) {
      tmpSet = new HashSet<WorkflowRevision>(usingWorkflows);
      interfaceDependentWorkflows.put(myId, tmpSet);
      caresForInterfaceChanges = true;
    }
    Object pillow = new Object();
    synchronized (pillows) {
      pillows.put(myId, pillow);
    }

    return myId;
  }


  public void putToSleep(long deploymentId, AtomicBoolean entranceLockLocked) throws XPRC_WorkflowProtectionModeViolationException {
    try {
      Object pillow = pillows.get(deploymentId);
      synchronized (pillow) {
        DeploymentManagement.getInstance().unlockEntrance(entranceLockLocked);
        pillow.wait();
        logger.debug("A sleeping deployment has been awoken");
      }
    } catch (InterruptedException e) {
      participatingThreads.remove(Thread.currentThread().getId());
      // if this happens neither condition should be true and the deployment is continued, might be a valid alternative
      throw new RuntimeException("Sleeping deployment-thread got interrupted while waiting to continue");
    }

    //if wokenUp check for Leadership & Failure, if newLeader continue checks else exit and continue deployment
    if (failedDeployments.contains(deploymentId)) {
      participatingThreads.remove(Thread.currentThread().getId());
      throw new XPRC_WorkflowProtectionModeViolationException(workflowIdentifierOfProtectedWF.wfFqClassName);
    }

    if (struggelingForLeadership.compareAndSet(true, false)) {
      if (logger.isDebugEnabled()) {
        logger.debug(new StringBuilder().append("deployment was struggelingForLeadership, #").append(deploymentId)
                        .append(" took over that role.").toString());
      }
      idOfLeader = deploymentId;
      synchronized (pillows) {
        pillows.remove(idOfLeader);
      }
      DeploymentManagement.getInstance().leaderWaitsTillDeploymentPropagated();
    } 
  }
  
  
  public void addPausedTaskIds(Collection<Long> pausedTasksId) {
    pausedFqCtrlTasks.addAll(pausedTasksId);
  }
  
  
  public Collection<Long> getPausedTaskIds() {
    return pausedFqCtrlTasks;
  }
  
  public void addPausedResumes(Collection<Long> pausedResumes) {
    this.pausedResumes.addAll(pausedResumes);
  }
  
  
  public Collection<Long> getPausedResumes() {
    return pausedResumes;
  }

  public void addPausedScheduling(Collection<Long> pausedScheduling) {
    this.pausedScheduling.addAll(pausedScheduling);
  }

  public boolean isLeader(Long id) {
    return id.equals(idOfLeader);
  }


  public void protectionModeViolationForWorkflow(WorkflowRevision workflowIdentifier)
                  throws XPRC_WorkflowProtectionModeViolationException {

    logger.debug("protectionModeViolationForWorkflow");

    workflowIdentifierOfProtectedWF = workflowIdentifier;
    failedDeployments = getAllRelevantDeployments(workflowIdentifier);
    //check if all Deployments are participating
    if (failedDeployments.size() == affectedWorkflowsPerDeployment.size()) {
      logger.debug("all deployments are affected");
      //I can wake all up and cleanUp myself
      abortDeployment(null);
    } else if (failedDeployments.contains(idOfLeader)) { // if I'm participating, set struggelingForLeadership, wakeOne, throw exception
      logger.debug("not all deployments are affected, but leader is");
      if (!struggelingForLeadership.compareAndSet(false, true)) {
        //throw new XynaException("Leadership error"); this should never happen        
      }
      synchronized (pillows) {
        //everything is set up, let's run the other deployments into their demise
        for (Long failedDeploy : failedDeployments) {
          Object pillow = pillows.get(failedDeploy);
          if (pillow != null) { //that would be the failed deployment of the leader
            if (logger.isDebugEnabled()) {
              logger.debug(new StringBuilder().append("notifying ").append(failedDeploy)
                              .append(" of it's failed status").toString());
            }
            synchronized (pillow) {
              pillow.notify();
            }
            pillows.remove(failedDeploy);
          }
          cleanFromMaps(failedDeploy);
        }

        //benachrichtige einen anderen thread und werfe fehler. es muss mindestens einen anderen thread geben, ansonsten wäre man oben in das erste if gegangen
        Entry<Long, Object> pillowEntry = pillows.entrySet().iterator().next();
        Object pillow = pillowEntry.getValue();
        synchronized (pillow) {
          pillow.notify();
        }
        pillows.remove(pillowEntry.getKey());
        // no need to continue the search, let's throw our own exception       
        participatingThreads.remove(Thread.currentThread().getId());
        throw new XPRC_WorkflowProtectionModeViolationException(workflowIdentifier.wfFqClassName);
      }
    }
    logger.debug("leader is not affected from violation");
    // not all are participated and I'm not, so I can continue with the procedure
    synchronized (pillows) {
      for (Long failedDeploy : failedDeployments) { //let's run the other deployments into their demise
        if (logger.isDebugEnabled()) {
          logger.debug(new StringBuilder().append("notifying ").append(failedDeploy).append(" of it's failed status")
                          .toString());
        }
        Object pillow = pillows.get(failedDeploy);
        synchronized (pillow) {
          pillow.notify();
        }
        pillows.remove(failedDeploy);
        cleanFromMaps(failedDeploy);
      }
    }
  }


  public boolean isWorkflowInterfaceDependent(WorkflowRevision workflowIdentifier) {
    //that workflow is present in the system, and is somehow related to our changed Interface, but is he directly using it?
    for (Set<WorkflowRevision> singleSet : interfaceDependentWorkflows.values()) {
      if (singleSet.contains(workflowIdentifier)) {
        return true;
      }
    }
    return false;
  }


  private synchronized Set<Long> getAllRelevantDeployments(WorkflowRevision workflowIdentifier) {
    Set<Long> relevantDeployments = new HashSet<Long>();
    for (Long deploymentId : affectedWorkflowsPerDeployment.keySet()) {
      if (affectedWorkflowsPerDeployment.get(deploymentId).contains(workflowIdentifier)) {
        relevantDeployments.add(deploymentId);
      }
    }
    return relevantDeployments;
  }

  void abortDeploymentProcess(Exception e, WorkflowRevision workflowIdentifier)
                  throws XPRC_WorkflowProtectionModeViolationException {
    failedDeployments = pillows.keySet();
    workflowIdentifierOfProtectedWF = workflowIdentifier;
    abortDeployment(e);
  }
  
  //muss nicht unbedingt fqclassname sein, workflowIdentifierOfProtectedWF wird nur in fehlermeldung verwendet
  void abortDeploymentProcess(Throwable e, String wfFqClassName) 
                  throws XPRC_WorkflowProtectionModeViolationException {
    failedDeployments = pillows.keySet();
    workflowIdentifierOfProtectedWF = new WorkflowRevision(wfFqClassName, -1L);
    abortDeployment(e);
  }


  private void abortDeployment(Throwable e) throws XPRC_WorkflowProtectionModeViolationException {
    logger.debug("aborting deployment");
    //wake all
    for (Object pillow : pillows.values()) {
      synchronized (pillow) {
        pillow.notify();
      }
    }
    DeploymentManagement.getInstance().cleanup(ofs);
    if (e != null) {
      if (e instanceof XPRC_WorkflowProtectionModeViolationException) {
        throw (XPRC_WorkflowProtectionModeViolationException)e;
      } else {
        throw new XPRC_WorkflowProtectionModeViolationException(workflowIdentifierOfProtectedWF.wfFqClassName, e);
      }
    } else {
      throw new XPRC_WorkflowProtectionModeViolationException(workflowIdentifierOfProtectedWF.wfFqClassName);
    }
  }


  private final static XynaPropertyDuration maximumWaitTimeForOtherDeploymentsToFinish =
      new XynaPropertyDuration("xprc.xfractwfe.deployment.reloadcleanup.concurrentdeployments.wait.time", new Duration(3, TimeUnit.MINUTES))
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "The maximum time a deployment waits for other concurrent deployments to finish before it starts order reloading.");


  public boolean isDesignatedToCleanup() throws InterruptedException, BrokenBarrierException {
    boolean timeout = false;
    try {
      cleanupBarrier.await(maximumWaitTimeForOtherDeploymentsToFinish.getMillis(), TimeUnit.MILLISECONDS); //this barrier will trip once all deployments are gathered
    } catch (TimeoutException e) {
      // could happen if a severe error prevents one of the deployment-threads from calling the cleanup, let's start to race
      timeout = true;
    }

    if (someoneWantsToCleanUp.compareAndSet(false, true)) {
      if (timeout) {
        logger.warn("Timeout while waiting for other deployment thread. Continuing with order reloading and cleanup. Other deployment threads: " + participatingThreads);
      }
      return true;
    }
    //TODO hier müsste man eigtl warten, dass der designierte cleanup-thread fertig wird
    return false;
  }

  public void cleanup() {
    resumeScheduler();
    resumeResumes();
    resumeSuspendedOrders();
    resumeFQTasks();
  }
  
  void resumeFQTasks() {
    logger.debug("Resuming "+pausedFqCtrlTasks.size()+" fqTasks");
    for (Long taskId : pausedFqCtrlTasks) {
      if (logger.isDebugEnabled()) {
        logger.debug("resuming: " + taskId);
      }
      try {
        XynaFactory.getInstance().getProcessing().getFrequencyControl().resumeFrequencyControlledTasks(taskId);
      } catch (RuntimeException e) {
        logger.warn("Could not resume FrequencyControlledTask: " + taskId, e);
      }
    }
  }
  
  void resumeResumes() {
    logger.debug("Resuming "+pausedResumes.size()+" SRM-resumes");
    try {
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().removeUnresumableOrders(pausedResumes);
    } catch (RuntimeException e) {
      logger.warn("Could not resume Resumes: " + pausedResumes, e);
    }
  }

  void resumeScheduler() {
    logger.debug("Resuming "+pausedScheduling.size()+" orders in scheduler");
    AllOrdersList allOrders = XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList();
    for( Long orderId : pausedScheduling ) {
      try {
        allOrders.deploymentFinished(orderId);
      } catch (RuntimeException e) {
        logger.warn("Could not resume order from Scheduling: " + orderId, e); 
      }
    }
    try {
      XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler();
    } catch (RuntimeException e) {
      logger.warn("Failed to notify Scheduler.", e);
    }
  }



  /*private static Set<WorkflowRevision> transformXMLNameSetToJavaNames(Set<String> xmlNames, Long revision) {
    Set<WorkflowRevision> javaNames = new HashSet<WorkflowRevision>();
    for (String xmlName : xmlNames) {
      try {
        javaNames.add(new WorkflowRevision(GenerationBase.transformNameForJava(xmlName), revision));
      } catch (XPRC_InvalidPackageNameException e) {
        // if we received an invalidPackage name it should not concern us, it just won't be considered during our checks, but it shouldn't be running either
        logger.warn("Received invalid PackageName: " + xmlName, e);
      }
    }
    return javaNames;
  }*/


  public List<OrderFilterDeployment> getOrderFilters() {
    return ofs;
  }


  void resumeSuspendedOrders() {
    logger.debug("Resuming "+resumeTargets.size()+" suspendedOrders");

    if (resumeTargets.size() > 0) {
      try {
        Set<Long> unlocksForSuspended = new HashSet<Long>();
        for (ResumeTarget rt : resumeTargets) {
          unlocksForSuspended.add(rt.getRootId());
        }
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().removeUnresumableOrders(unlocksForSuspended);
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().resumeMultipleOrders(resumeTargets, false);
      } catch (XPRC_ResumeFailedException e) {
        //we still need to continue the cleanup and don't throw this
        logger.error("Failed to resume all suspend orders during DeploymentCleanup.", e);
      }
    }
  }

  
  public void addResumeTargets(List<ResumeTarget> targets) {
    resumeTargets.addAll(targets);
  }
  
  
  public WorkflowProtectionMode getLowestDeploymentMode() {
    Set<WorkflowProtectionMode> allModes = new HashSet<GenerationBase.WorkflowProtectionMode>(workflowProtModePerDeployment.values());
    for (WorkflowProtectionMode lowestToHighestMode : WorkflowProtectionMode.getWorkflowProtectionModesOrderdByForce(true)) {
      if (allModes.contains(lowestToHighestMode)) {
        return lowestToHighestMode;
      }
    }
    return WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT;
  }
  
}
