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
package com.gip.xyna.xprc.xbatchmgmt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaRunnable;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcess.PauseCause;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessManagementInformation;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSearch;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSearchResult;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSelectImpl;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessArchiveStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessCustomizationStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRestartInformationStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRuntimeInformationStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.CustomizationUpdater;
import com.gip.xyna.xprc.xbatchmgmt.storables.RuntimeInformationUpdater;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.AllOrdersList;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;
import com.gip.xyna.xprc.xsched.scheduling.TrySchedule.TryScheduleResult;
import com.gip.xyna.xprc.xsched.scheduling.XynaOrderExecutor;


public class BatchProcessManagement extends Section {

  public static final String DEFAULT_NAME = "Batch Process Management";
  
  private ConcurrentHashMap<Long,BatchProcess> batchProcesses;
  private ODS ods;

  
  public enum RuntimeInformationUpdateTask {
    Started, Finished, Failed, UpdateState;
  }
  
  
  public enum CancelMode {
    DEFAULT, // = KILL_SLAVES
    KILL_SLAVES, //Slaves werden sofort abgebrochen, pausierter BatchProcess wird fortgesetzt
    KILL_SLAVES_KEEP_PAUSED, //Slaves werden sofort abgebrochen, pausierter BatchProcess bleibt pausiert
    WAIT, //es wird auf die Slaves gewartet, pausierter BatchProcess wird fortgesetzt
    WAIT_KEEP_PAUSED; //es wird auf die Slaves gewartet, pausierter BatchProcess bleibt pausiert
  }
  
  public enum MissingLimitationReaction {
    SlowDown,     //Slaves werden über eine SlaveExecutionPeriod verzögert
    Fail,         //BatchProcess wird abgewiesen
    Unlimited;    //trotzdem startem

    public static String documentation(DocumentationLanguage lang) {
      switch( lang ) {
        case DE:
          return "'SlowDown': Slaves werden über eine SlaveExecutionPeriod verzögert; "
          +"'Fail': BatchProcess wird mit einer Exception abgewiesen; "
          +"'Unlimited': BatchProcess wird unlimitiert gestartet";
        case EN:
        default:
          return "'SlowDown': Slaves will be slowed down via a SlaveExecutionPeriod; "
          +"'Fail': Batch Process will be rejectected with an exception; "
          +"'Unlimited': Batch Process will be started unlimited";
      }
    }

  }
  
  
  
  
  public BatchProcessManagement() throws XynaException {
    super();
    batchProcesses = new ConcurrentHashMap<Long,BatchProcess>();
  }
  
  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    //benutzte XynaProperties
    XynaProperty.BATCH_MAX_PARALLELISM.registerDependency(DEFAULT_NAME);
    XynaProperty.BATCH_INPUT_MAX_ROWS.registerDependency(DEFAULT_NAME); 
    XynaProperty.BATCH_CANCEL_WAIT_TIMEOUT.registerDependency(DEFAULT_NAME); 
    XynaProperty.BATCH_DEFAULT_MASTER.registerDependency(DEFAULT_NAME); 
    XynaProperty.BATCH_NO_LIMITATION_REACTION.registerDependency(DEFAULT_NAME);
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(BatchProcessManagement.class, "BatchProcessManagement.initStorablesAndDeployment").
          after(PersistenceLayerInstances.class, DeploymentHandling.class ).
          execAsync( new Runnable() { public void run() { initStorablesAndDeployment();} });
  }
  
  private class OnTheFlyInputGeneratorCacheClearer implements DeploymentHandler {

    public void exec(GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {
      //nichts zu tun
    }

    public void finish(boolean success) throws XPRC_DeploymentHandlerException {
      //TODO das muss nicht für alle batchprocesses gemacht werden. optimierungsmöglichkeit
      for( BatchProcess bp : batchProcesses.values() ) {
        bp.refreshInputGenerator();
      }
    }

    @Override
    public void begin() throws XPRC_DeploymentHandlerException {
    }
  }
  
  
  private void initStorablesAndDeployment() {
    try {
      ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
      ods.registerStorable(BatchProcessRuntimeInformationStorable.class);
      ods.registerStorable(BatchProcessArchiveStorable.class);
      ods.registerStorable(BatchProcessCustomizationStorable.class);
      ods.registerStorable(BatchProcessRestartInformationStorable.class);
    }
    catch (PersistenceLayerException e) {
      //FIXME bessere Behandlung?
      throw new RuntimeException(e);
    }
    
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
    .addDeploymentHandler(DeploymentHandling.PRIORITY_XPRC, new OnTheFlyInputGeneratorCacheClearer() );

  }
  
  
  public Long startBatchProcess(BatchProcessInput input) throws XynaException {
    BatchProcess batchProcess = new BatchProcess(input);
    if (batchProcesses.put(batchProcess.getBatchProcessId(), batchProcess) != null) {
      throw new RuntimeException("there is already a batch process with the same id " + batchProcess.getBatchProcessId());
    }
    
    XynaOrderServerExtension xo = batchProcess.getMasterOrder();
    ResponseListener rl = batchProcess.createMasterResponseListener();
    OrderContext ctx = batchProcess.createMasterOrderContext();
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrder(xo, rl, ctx);
    
    return batchProcess.getBatchProcessId();
  }
  
  public BatchProcessInformation startBatchProcessSynchronous(BatchProcessInput input) throws XynaException {
    BatchProcess batchProcess = new BatchProcess(input);
    if (batchProcesses.put(batchProcess.getBatchProcessId(), batchProcess) != null) {
      throw new RuntimeException("there is already a batch process with the same id " + batchProcess.getBatchProcessId());
    }
    
    XynaOrderServerExtension xo = batchProcess.getMasterOrder();
    xo.setOrderContext(batchProcess.createMasterOrderContext());
    xo.setResponseListener(batchProcess.createMasterResponseListener());
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrderSynchronous(xo);
    
    return batchProcess.getBatchProcessInformation();
  }

  
  private void finallyClose(ODSConnection con) {
    if( con != null ) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }
  }

  public BatchProcess getBatchProcess (Long batchProcessId) {
    return batchProcesses.get(batchProcessId);
  }

  
  
  /**
   * Pausiert einen Batch Process.
   * @param batchProcessId
   * @throws PersistenceLayerException 
   */
  public boolean pauseBatchProcess (Long batchProcessId) throws PersistenceLayerException {
    BatchProcess batchProcess = batchProcesses.get(batchProcessId);
    if (batchProcess == null) { //Batch Process existiert nicht mehr
      return false;
    }
    
    return batchProcess.pauseBatchProcess(PauseCause.MANUALLY.getCause());
  }
  
  
  /**
   * Schließt die Auftragseingangsschnittstelle der Batch Prozesse für eine Application,
   * indem alle laufenden Prozesse pausiert werden
   * @param revision
   */
  public void closeBatchProcessEntrance (Long revision) throws PersistenceLayerException {
    for (Long batchProcessId : batchProcesses.keySet()) {
      getBatchProcess(batchProcessId).pauseBatchProcess(revision, PauseCause.ORDER_ENTRANCE_CLOSED);
    }
  }
  
  
  /**
   * Überprüft, ob die Auftragseingangsschnittstelle der Batch Prozesse für eine Application
   * geschlossen ist, d.h. ob alle Batch Prozesse pausiert sind oder
   * keine weiteren Slaves starten
   * @param revision
   * @return
   */
  public boolean batchProcessEntranceClosed (Long revision) {
    for (Long batchProcessId : batchProcesses.keySet()) {
      BatchProcess bp = getBatchProcess(batchProcessId);
      if (bp.startsCurrentlySlaves(revision)) {
        return false; //Batch Process startet Slaves
      }
    }
    
    return true;
  }

  public List<BatchProcess> getBatchProcesses (Long revision) {
    List<BatchProcess> result = new ArrayList<BatchProcess>();
    for (Long batchProcessId : batchProcesses.keySet()) {
      BatchProcess bp = getBatchProcess(batchProcessId);
      if (bp.isInRevision(revision)) {
        result.add(bp);
      }
    }
    
    return result;
  }
  
  /**
   * Setzt einen manuell pausierten Batch Process wieder fort.
   * @param batchProcessId
   * @return
   * @throws PersistenceLayerException
   */
  public boolean continueBatchProcess(Long batchProcessId) throws PersistenceLayerException{
    return continueBatchProcess(batchProcessId, false);
  }

  /**
   * Setzt einen pausierten Batch Process wieder fort.
   * @param batchProcessId
   * @param force true, wenn der PauseCause nicht beachtet werden soll
   * @return
   * @throws PersistenceLayerException
   */
  public boolean continueBatchProcess(Long batchProcessId, boolean force) throws PersistenceLayerException{
    BatchProcess batchProcess = batchProcesses.get(batchProcessId);
    if (batchProcess == null) { //Batch Process existiert nicht mehr
      return false;
    }
    
    if (!batchProcess.isPaused()) {
      return false; //BatchProcess ist nicht unterbrochen
    }
    
    //BatchProcess fortsetzen
    if (force) {
      //pauseCause ist egal
      return batchProcess.continueBatchProcess();
    } else {
      //nur manuell pausierte oder pausiert gestartete Batch Prozesse fortsetzen
      if (batchProcess.pausedWith(PauseCause.MANUALLY)
                      || batchProcess.pausedWith(PauseCause.DISABLED)
                      || batchProcess.pausedWith(PauseCause.ORDER_ENTRANCE_CLOSED)) {
        return batchProcess.continueBatchProcess();
      }
      
      return false;
    }
  }
  
  /**
   * Aktiviert die Auftragseingansschnittstelle der Batch Prozesse für eine Revision wieder,
   * indem alle Prozesse wieder fortgesetzt werden, die durch das Schließen der
   * Schnittstelle pausiert worden sind.
   * @param revision
   */
  public void openBatchProcessEntrance(Long revision) throws PersistenceLayerException {
    for (Long batchProcessId : batchProcesses.keySet()) {
      BatchProcess batchProcess = getBatchProcess(batchProcessId);
      if (batchProcess.pausedWith(PauseCause.ORDER_ENTRANCE_CLOSED)) {
        batchProcess.continueBatchProcess(revision);
      }
    }
  }
  
  /**
   * Bricht einen Batch Process ab. D.h. es werden keine neuen Slaves mehr gestartet und die
   * laufenden Slaves werden abgebrochen. Bei CancelMode == WAIT wird vor dem Abbrechen gewartet,
   * ob die Slaves innerhalb ihres OrderExecutionTimeouts fertig werden.
   * Anschließend geht der Master in die Execution-Phase über.
   * @param batchProcessId
   * @param cancelMode
   * @param callerOrderId Id des Auftrags der das Cancel aufruft. Falls das selbst ein Slaveauftrag ist, wird nicht
   *        versucht sich selbst zu canceln. -1 kann angegeben werden, wenn nicht von Auftrag aufgerufen.
   * @return
   * @throws PersistenceLayerException 
   */
  public boolean cancelBatchProcess (Long batchProcessId, CancelMode cancelMode, long callerOrderId) throws PersistenceLayerException {
    BatchProcess batchProcess = batchProcesses.get(batchProcessId);
    if (batchProcess == null) { //Batch Process existiert nicht mehr
      return false;
    }
    
    //TODO falls true Übergabe weitergegeben werden soll, sollte man die schnittstelle hier nochmal überdenken,
    //ob man statt booleanflag lieber ein enum-set baut oder das enum erweitert
    return batchProcess.cancelBatchProcess(cancelMode, true, callerOrderId);
  }
  
  /**
   * Ändert die Werte eines BatchProcesses, die im übergebenen Input ungleich null sind.
   * @param batchProcessId
   * @param input
   * @throws PersistenceLayerException 
   */
  public boolean modifyBatchProcess(Long batchProcessId, BatchProcessInput input) throws PersistenceLayerException {
    BatchProcess batchProcess = batchProcesses.get(batchProcessId);
    if (batchProcess == null) { //Batch Process existiert nicht
      return false;
    }
    
    return batchProcess.modifyBatchProcess(input);
  }
  
  /**
   * Aktualisiert die Scheduling Data der Slaves
   * @param batchProcessId
   * @return
   */
  public boolean refreshBatchProcess (Long batchProcessId) {
    BatchProcess batchProcess = batchProcesses.get(batchProcessId);
    if (batchProcess == null) { //Batch Process existiert nicht
      return false;
    }
    
    batchProcess.getBatchProcessScheduling().refreshSlaveSchedulingData();
    
    //TODO die Priorität des ParallelExecutors muss angepasst werden
    
    return true;
  }

  /**
   * Der Batch Process wird von einer Application-Version auf eine andere
   * migriert, falls keine Slaves laufen.
   * @return
   * @throws XynaException 
   */
  public boolean migrateBatchProcess(Long batchProcessId, String application, String version) throws XynaException {
    BatchProcess batchProcess = batchProcesses.get(batchProcessId);
    if (batchProcess == null) { //Batch Process existiert nicht
      return false;
    }
    
    return batchProcess.migrateBatchProcess(application, version);
  }
  
  
  public boolean migrateBatchProcess(Long batchProcessId, RuntimeContext to) throws XynaException {
    BatchProcess batchProcess = batchProcesses.get(batchProcessId);
    if (batchProcess == null) { //Batch Process existiert nicht
      return false;
    }
    
    return batchProcess.migrateBatchProcess(to);
  }
    
  /**
   * Auftrag wird zum ersten Mal in den Scheduler eingestellt (entweder nach Neuanlage 
   * des Auftrags oder nach Restart der Factory).
   * Falls dies ein BatchProcess-Master ist, muss der BatchProcess wiederhergestellt 
   * werden, wenn er noch nicht existiert oder die SchedulingOrder im BatchProcess gespeichert werden.
   * @param so
   */
  public void addWaitingOrder(SchedulingOrder so) {
    BatchProcessMarker marker = so.getBatchProcessMarker();
    if( marker == null ) {
      return; //kein BatchProcess -> nichts zu tun
    }
    if( ! marker.isBatchProcessMaster() ) {
      //FIXME Slaves speichern, bis Master vorhanden oder anderer Clusterknoten ansprechbar ist 
      return; //kein BatchProcess-Master -> nichts zu tun
    }
    BatchProcess batchProcess = getBatchProcess(marker.getBatchProcessId());
    if( batchProcess == null ) {
      Long batchProcessId = marker.getBatchProcessId();
      BatchProcessRestarter bpr = new BatchProcessRestarter(batchProcessId, so);
      XynaOrderExecutor.restartBatchProcess( bpr );
      batchProcesses.put(batchProcessId, bpr.getBatchProcess() );
    } else {
      batchProcess.getBatchProcessScheduling().addSchedulingOrder(so);
    }
  }
  
  /**
   * Falls BatchProcess noch nicht angelegt wurde, müssen die Slaves, deren BatchProcess
   * fehlt, warten:
   * entweder bis der Master geschedult wurde und der BatchProcess angelegt wurde,
   * oder bis bekannt ist, dass der andere Knoten den BatchProcessMaster ausfÜhrt. //FIXME nicht implementiert!
   * Weitere Behandlung geschieht in {@link BatchProcessScheduling#tryScheduleBatch(SchedulingOrder,boolean)}
   * @param so
   * @return
   */
  public TryScheduleResult tryScheduleBatch(SchedulingOrder so) {
    BatchProcessMarker marker = so.getBatchProcessMarker();
    BatchProcess batchProcess = getBatchProcess(marker.getBatchProcessId());
    if( batchProcess == null ) {
      //FIXME Slaves speichern, bis Master vorhanden oder anderer Clusterknoten ansprechbar ist 
      return TryScheduleResult.CONTINUE;
    }
    TryScheduleResult tsr = batchProcess.getBatchProcessScheduling().tryScheduleBatch(so, marker.isBatchProcessMaster());
    if (logger.isTraceEnabled()) {
      logger.trace("bp " + marker.getBatchProcessId() + " in state: " + batchProcess.getBatchProcessScheduling().getSchedulingState()
          + " ismaster=" + marker.isBatchProcessMaster());
    }
    return tsr;
  }
  
  /**
   * BatchProcess kann gestartet werden:
   * entweder Slaves erzeugen und TryScheduleResult zurückgeben
   * oder null zurückgeben, so dass Master in die Execution gelangt
   * @param so
   * @return
   */
  public TryScheduleResult trySchedule(SchedulingOrder so) {
    BatchProcess batchProcess = getBatchProcess(so.getBatchProcessMarker().getBatchProcessId());
    return batchProcess.getBatchProcessScheduling().trySchedule(so, batchProcess.isCanceled());
  }

  /**
   * BatchProcessRestarter ist ein XynaRunnable, mit dem die Initialisierung des kompletten BatchProcess
   * außerhalb des SchedulerThread durchgeführt werden kann. Der BatchProcess wird aber gleich sichtbar im
   * BatchProcessManagement, jedoch geschätzt durch das ManagementOperationLock.
   */
  public static class BatchProcessRestarter extends XynaRunnable {

    private CountDownLatch waitForManagementOperationLock;
    private BatchProcess batchProcess;
    private int priority;
    
    public BatchProcessRestarter(Long batchProcessId, SchedulingOrder master) {
      AllOrdersList allOrders = XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList();
      this.batchProcess = new BatchProcess(batchProcessId, master, allOrders.getXynaOrder(master));
      this.waitForManagementOperationLock = new CountDownLatch(1);
      this.priority = master.getSchedulingData().getPriority(); //könnte Prio vom Slave sein
    }

    public void run() {
      batchProcess.restartBatchProcess(waitForManagementOperationLock);
    }
    
    /**
     * Liefert den neuen BatchProzess, wartet blockierend, bis managementOperationLock erhalten wurde
     * @return
     */
    public BatchProcess getBatchProcess() {
      try {
        waitForManagementOperationLock.await();
      } catch (InterruptedException e) {
        Logger logger = CentralFactoryLogging.getLogger(BatchProcessRestarter.class);
        logger.warn( "Interrupted while waiting to get managementOperationLock", e);
      }
      return batchProcess;
    }

    public int getPriority() {
      return priority;
    }
    
  }
  

  /**
   * Setzt das Flag, dass der Master sich jetzt im Cleanup befindet
   * @param batchProcessId
   */
  public void masterInCleanup(Long batchProcessId){
    BatchProcess batchProcess = batchProcesses.get(batchProcessId);
    batchProcess.setMasterInCleanup(true);
  }
  
  
  /**
   * Führt die Aufräumarbeiten aus:
   * 1) Austragen aus laufenden BatchProcessen
   * 2) Löschen der Runtime-Storables und archivieren des BatchProcessArchive
   * 3) Entfernen des zugehörigen Zeitfensters
   * @param batchProcessId
   * @throws PersistenceLayerException
   */
  public void finishBatchProcess(Long batchProcessId, boolean success) throws PersistenceLayerException{
    BatchProcess batchProcess = batchProcesses.remove(batchProcessId);
    batchProcess.archiveStorables(success);
    batchProcess.removeTimeWindow();
  }

  public BatchProcessManagementInformation getBatchProcessManagementInformation() {
    BatchProcessManagementInformation bpmi = new BatchProcessManagementInformation();
    for( BatchProcess bp : batchProcesses.values() ) {
      bpmi.addBatchProcessInformation( bp.getBatchProcessInformation() );
    }
    return bpmi;
  }

  public BatchProcessSearchResult searchBatchProcesses(BatchProcessSelectImpl select, int maxRows) throws PersistenceLayerException {
    BatchProcessSearch search = new BatchProcessSearch();

    return search.searchBatchProcesses(select, maxRows);
  }
  
  
  public BatchProcessInformation getBatchProcessInformation(Long batchProcessId) throws XynaException {
    BatchProcess bp = getBatchProcess(batchProcessId);
    if( bp != null ) {
      return bp.getBatchProcessInformation();
    } else {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        BatchProcessArchiveStorable bpas = new BatchProcessArchiveStorable(batchProcessId);
        con.queryOneRow(bpas);
        BatchProcessInformation bpi = new BatchProcessInformation(bpas, true);
        try {
          BatchProcessRestartInformationStorable bpris = new BatchProcessRestartInformationStorable(batchProcessId);
          con.queryOneRow(bpris);
          bpi.setRestartInformation(bpris);
        } catch( XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e ) {
          logger.info( "BatchProcessRestartInformationStorable could not be found for batchProcessId "+ batchProcessId);
        }
        return bpi;
      } finally {
        finallyClose(con);
      }
    }
  }
  
  
  public void addCounterFields(Long batchProcessId, List<Double> counter ) {
    BatchProcess bp = getBatchProcess(batchProcessId);
    if( bp != null ) {
      CustomizationUpdater cu = bp.updateCustomFields();
      cu.addCounter(counter);
      cu.update();
    } else {
      throw new IllegalArgumentException("invalid batchProcessId"); //FIXME
    }
  }
  
  public void setCounterFields(Long batchProcessId, List<Double> counter) {
    BatchProcess bp = getBatchProcess(batchProcessId);
    if( bp != null ) {
      CustomizationUpdater cu = bp.updateCustomFields();
      cu.setCounter(counter);
      cu.update();
    } else {
      throw new IllegalArgumentException("invalid batchProcessId"); //FIXME
    }
  }
    
  public void setCustomFields(Long batchProcessId, List<String> custom) {
    BatchProcess bp = getBatchProcess(batchProcessId);
    if( bp != null ) {
      CustomizationUpdater cu = bp.updateCustomFields();
      cu.setCustom(custom);
      cu.update();
    } else {
      throw new IllegalArgumentException("invalid batchProcessId"); //FIXME
    }
  }

  public void terminateSlaves(Long batchProcessId, long orderId, boolean success) {
    BatchProcess batchProcess = getBatchProcess(batchProcessId);
    if (batchProcess != null) { //BatchProcess könnte inzwischen abgebrochen worden sein
      try {
        //Slave aus Map der aktuellen Slaves austragen
        OrderInstanceStatus slaveState = batchProcess.removeCurrentSlave(orderId);
        
        //Counter aktualisieren
        RuntimeInformationUpdater updater = batchProcess.updateRuntimeInformation();
        updater.terminateSlave(slaveState, success);
        updater.update();
        
        //Master wieder einstellen, falls bereits alle Slaves fertig sind
        if (batchProcess.allSlavesFinished()) {
          batchProcess.getBatchProcessScheduling().terminateSlaves();
        }
      }
      catch (XynaException ple) {
        logger.error("Exception during execution for Slave ResponseListener.", ple);
      }
    }
    
  }

  /*
   * falls master noch am slave-scheduling ist, dieses unterbrechen und TryScheduleResult.ReSchedule zurückgeben
   * 
   * ansonsten null zurückgeben (-> master wird abgebrochen)
   */
  public TryScheduleResult tryScheduleMasterFailed(SchedulingOrder so) {
    BatchProcess batchProcess = batchProcesses.get(so.getBatchProcessMarker().getBatchProcessId());
    if (batchProcess == null) {
      return null;
    }
    if (!so.getBatchProcessMarker().isBatchProcessMaster()) {
      return null;
    }
    TryScheduleResult tsr = batchProcess.getBatchProcessScheduling().tryScheduleMasterFailed();
    if (logger.isTraceEnabled()) {
      logger.trace("bp " + so.getBatchProcessMarker().getBatchProcessId() + " failed in state: "
          + batchProcess.getBatchProcessScheduling().getSchedulingState() + ". so-state=" + so.getExtendedStatus());
    }
    return tsr;

  }

  public void updateMasterAfterScheduling(BatchProcessMarker bpm) throws PersistenceLayerException {
    // falls batchprocess wegen timeout aus dem scheduler geflogen ist, speichern
    BatchProcess batchProcess = batchProcesses.get(bpm.getBatchProcessId());
    if (batchProcess == null) {
      return;
    }
    batchProcess.storeTimeoutStateIfNecessary();
  }
  
}
