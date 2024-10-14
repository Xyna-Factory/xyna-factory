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
package com.gip.xyna.xprc.xbatchmgmt;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.ExecutionTimeoutConfiguration;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaExecutor;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaRunnable;
import com.gip.xyna.xprc.exceptions.XPRC_InputGeneratorInitializationException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessStatus;
import com.gip.xyna.xprc.xbatchmgmt.input.ConstantInputGenerator;
import com.gip.xyna.xprc.xbatchmgmt.input.InputGenerator;
import com.gip.xyna.xprc.xbatchmgmt.input.InputGeneratorData;
import com.gip.xyna.xprc.xbatchmgmt.storables.ArchiveBatchProcess;
import com.gip.xyna.xprc.xbatchmgmt.storables.ArchiveUpdater;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessArchiveStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessCustomizationStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRestartInformationStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRuntimeInformationStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRuntimeInformationStorable.BatchProcessState;
import com.gip.xyna.xprc.xbatchmgmt.storables.CustomizationUpdater;
import com.gip.xyna.xprc.xbatchmgmt.storables.RuntimeInformationUpdater;
import com.gip.xyna.xprc.xbatchmgmt.storables.SelectStorables;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.capacities.TransferCapacities;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean.CANCEL_RESULT;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeWindowDefinition;


public class BatchProcess {

  private static Logger logger = CentralFactoryLogging.getLogger(BatchProcess.class);

  private Long batchProcessId;
  private BatchProcessArchiveStorable batchProcessArchiveData;
  private BatchProcessRuntimeInformationStorable runtimeInformation;
  private BatchProcessCustomizationStorable customizationData;
  private BatchProcessRestartInformationStorable restartInformation;
  private InputGenerator inputGenerator;
  private XynaOrderServerExtension masterOrder; //Master-Auftrag
  private ReentrantLock startSlavesLock = new ReentrantLock(); //Lock, damit Slaves nur von einem Thread gleichzeitig gestartet werden
  private ReentrantLock managementOperationLock = new ReentrantLock(); //Lock, damit nur eine Operation, wie z.B. Cancel, Pause oder Continue, gleichzeitig ausgeführt wird
  private AtomicInteger started = new AtomicInteger(0);
  private boolean masterInCleanup = false; //true, wenn sich der Master im Cleanup befindet
  private BatchProcessScheduling batchProcessScheduling;
  private RemoteXynaOrderCreationParameter masterOrderCreationParameter;
  private ConcurrentHashMap<Long, OrderInstanceStatus> currentSlaves = new ConcurrentHashMap<Long, OrderInstanceStatus>();   //Map der XynaOrderIds der gerade laufenden Slaves mit ihrem Status
  private AtomicBoolean allSlavesStarted = new AtomicBoolean(false); //true, wenn alle Slaves gestartet wurden 
  
  
  public enum PauseCause {
    MANUALLY("manually paused"), 
    MIGRATE("migrate batch process"),
    MODIFY("modify batch process"),
    DISABLED("created as paused"),
    ORDER_ENTRANCE_CLOSED("order entrance closed");
    
    private String cause;
    
    private PauseCause(String cause) {
      this.cause = cause;
    }
    
    public String getCause() {
      return cause;
    }
  }
  
  public BatchProcess(BatchProcessInput input) throws XynaException {
    //überprüfen, ob die Application existiert und nicht im Zustand AUDIT_MODE ist
    checkApplication(input.getMasterOrder().getDestinationKey().getApplicationName(),
                     input.getMasterOrder().getDestinationKey().getVersionName());

    this.batchProcessId = createMasterOrder(input.getMasterOrder());
    this.batchProcessScheduling = new BatchProcessScheduling(this,masterOrder);
    
    TimeConstraint tc = batchProcessScheduling.prepareTimeContraint(batchProcessId,input.getMasterOrder().getTimeConstraint(),input.getTimeWindowDefinition());
    masterOrderCreationParameter.setTimeConstraint(tc);

    //Storables anlegen (werden erst persistiert, wenn Master gebackupt wurde)
    createStorables(batchProcessId,input);
    
    createInputGenerator(input.getInputGeneratorData(), false);
    
    //ParallelExecutor anlegen und Limitierung prüfen
    batchProcessScheduling.createParallelExecutorAndCheckLimitation(
        createSlaveOrderTypeInfo(), restartInformation.getSlaveExecutionPeriod(), 
        restartInformation.getTotal(), getMaxParallelism() );
    
    if (input.isPaused()) {
      //BatchProcessState auf "Paused" und SchedulingState auf "Remove" setzen
      runtimeInformation.setState(BatchProcessState.PAUSED);
      runtimeInformation.setPauseCause(PauseCause.DISABLED.getCause());
      batchProcessScheduling.pause(false);
    }
    
    //Application (wieder) auf Running setzen
    setApplicationRunning();
  }

  public BatchProcess(Long batchProcessId, SchedulingOrder masterSchedulingOrder, XynaOrderServerExtension masterOrder) {
    this.batchProcessId = batchProcessId;
    this.runtimeInformation = new BatchProcessRuntimeInformationStorable(batchProcessId);
    this.batchProcessArchiveData = new BatchProcessArchiveStorable(batchProcessId);
    this.customizationData = new BatchProcessCustomizationStorable(batchProcessId);
    this.restartInformation = new BatchProcessRestartInformationStorable(batchProcessId);
    this.batchProcessScheduling = new BatchProcessScheduling(this,masterSchedulingOrder,masterOrder);
    this.masterOrder = masterOrder;
  }

  public Long getBatchProcessId() {
    return batchProcessId;
  }

  public XynaOrderServerExtension getMasterOrder() {
    return masterOrder;
  }

  public int getTerminated() {
    return runtimeInformation.getFinished() + runtimeInformation.getFailed();
  }

  public void setMasterInCleanup (boolean masterInCleanup) {
    this.masterInCleanup = masterInCleanup;
  }

  public int getStarted() {
    return started.get();
  }
  
  public void incrementStarted() {
    started.incrementAndGet();
  }
  
  public BatchProcessScheduling getBatchProcessScheduling() {
    return batchProcessScheduling;
  }
  

  public OrderInstanceStatus removeCurrentSlave(Long slaveOrderId) {
    return currentSlaves.remove(slaveOrderId);
  }
  
  public boolean hasRunningSlaves() {
    return currentSlaves.size() > 0;
  }

  public void setSlaveState(Long slaveOrderId, OrderInstanceStatus state) {
    currentSlaves.put(slaveOrderId, state);
  }
  
  /**
   * Legt eine XynaOrder für den Master an.
   * @param masterOrderCreationParameter
   * @return batchProcessId
   * @throws XynaException
   */
  private Long createMasterOrder(RemoteXynaOrderCreationParameter masterOrderCreationParameter) throws XynaException{
    this.masterOrderCreationParameter = masterOrderCreationParameter;
    
    //XynaObject für InputPayload aus dem XML erstellen
    masterOrderCreationParameter.convertInputPayload();
    
    //XynaOrder anlegen
    XynaOrderServerExtension xo = new XynaOrderServerExtension(masterOrderCreationParameter);
    
    //XynaObject-InputPayload wieder entfernen, damit die masterOrderCreationParameter
    //auch wieder außerhalb der Factory deserialisiert werden können
    masterOrderCreationParameter.removeXynaObjectInputPayload();
    
    //BatchProcessMarker erstellen
    BatchProcessMarker batchProcessMarker = new BatchProcessMarker(xo.getId(), true);
    xo.setBatchProcessMarker(batchProcessMarker);
    
    //Priorität für den Master setzen
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS()
      .getPriorityManagement().discoverPriority(xo);
    
    this.masterOrder = xo;
    
    return xo.getId();
  }
  
  public ResponseListener createMasterResponseListener() {
    return new BatchProcessMasterResponseListener(batchProcessId);
  }

  public OrderContext createMasterOrderContext() {
    MasterBackupAck acknowledgableObject = new MasterBackupAck(this);

    OrderContextServerExtension ctx = new OrderContextServerExtension(masterOrder);
    ctx.set(OrderContextServerExtension.ACKNOWLEDGABLE_OBJECT_KEY, acknowledgableObject);
    
    return ctx;
  }


  /**
   * Legt eine XynaOrder für einen Slave an.
   */
  public XynaOrderServerExtension createSlaveOrder(GeneralXynaObject inputPayload) {  
    XynaOrderServerExtension xo = new XynaOrderServerExtension(batchProcessArchiveData.getDestinationKey(), inputPayload);
    
    //markieren, dass zu BatchProcess gehörend
    BatchProcessMarker batchProcessMarker = new BatchProcessMarker(batchProcessId, false);
    xo.setBatchProcessMarker(batchProcessMarker);
    
    //ExecutionTimeouts und TimeConstraints für die Slaves setzen, falls vorhanden
    xo.setWorkflowExecutionTimeout(absRelTimeToExecutionTimeout(restartInformation.getSlaveWorkflowExecTimeout()));
    xo.setOrderExecutionTimeout(absRelTimeToExecutionTimeout(restartInformation.getSlaveOrderExecTimeout()));
    xo.getSchedulingData().setTimeConstraint(restartInformation.getSlaveTimeConstraint());
    
    //Custom-Felder vom Master auf den Slave übertragen
    xo.setCustom0(masterOrder.getCustom0());
    xo.setCustom1(masterOrder.getCustom1());
    xo.setCustom2(masterOrder.getCustom2());
    xo.setCustom3(masterOrder.getCustom3());
    
    return xo;
  }

  private ExecutionTimeoutConfiguration absRelTimeToExecutionTimeout(AbsRelTime absRelTime) {
    if( absRelTime == null ) {
      return null;
    }
    if (absRelTime.isRelative()) {
      return new ExecutionTimeoutConfiguration.ExecutionTimeoutConfigurationRelative(absRelTime.getTime(), TimeUnit.MILLISECONDS);
    } else {
      return new ExecutionTimeoutConfiguration.ExecutionTimeoutConfigurationAbsolute(absRelTime.getTime(), TimeUnit.MILLISECONDS);
    }
  }

  public ResponseListener createSlaveResponseListener() {
    return new BatchProcessSlaveResponseListener(this);
  }
  
  public OrderContext createSlaveOrderContext(XynaOrderServerExtension xo, String currentInputId) {
    //AcknowledgableObject, damit die Anzahl der laufenden Slaves und der vergebene Input
    //erst persistiert werden, wenn der Slave gebackupt wird
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    RuntimeInformationUpdater updater = updateRuntimeInformation();
    SlaveBackupAck acknowledgableObject = new SlaveBackupAck(con, updater, currentInputId);
    
    OrderContextServerExtension ctx = new OrderContextServerExtension(xo);
    ctx.set(OrderContextServerExtension.ACKNOWLEDGABLE_OBJECT_KEY, acknowledgableObject);
    
    return ctx;
  }


  public void createInputGenerator(InputGeneratorData inputGeneratorData, boolean restart) throws XPRC_InputGeneratorInitializationException {
    if( inputGeneratorData != null ) {
      inputGenerator = inputGeneratorData.createInputGenerator(batchProcessArchiveData.getRevision() );
    } else {
      //Default kein Input, wenn nichts übergeben wurde
      inputGenerator = new ConstantInputGenerator("", 0, batchProcessArchiveData.getRevision() );
    }
    if( restart ) {
      //Bestimmt, welche Inputs der InputGenerator noch ausgeben darf. Dies sind alle 
      //schon einmal ausgegebene, aber noch nicht verwendete und alle mit InputId > LastInputId
      inputGenerator.copyReusableInputIds(runtimeInformation.getOpenDataPlanning());
      inputGenerator.setLastInputId(runtimeInformation.getLastInputGeneratorID());
      inputGenerator.setAlreadyStarted(runtimeInformation.getStarted());
      started.set(runtimeInformation.getStarted());
    }
  }  
  
  /**
   * Liefert das Minimum der maximalen Parallelität aus restartInformation und der 
   * XynaProperty "xyna.xprc.xbatchmgmt.max.parallelism".
   * @return
   */
  public int getMaxParallelism() {
    int ret = 0;
    //wenn der Wert in restartInformation negativ oder Null ist, wird der Wert aus der XynaProperty genommen
    if (restartInformation.getMaxParallelism() <= 0) { 
      ret = XynaProperty.BATCH_MAX_PARALLELISM.get();
    } else { //sonst das Minimum der beiden Werte
      ret = Math.min(restartInformation.getMaxParallelism(), XynaProperty.BATCH_MAX_PARALLELISM.get());
    }
    return ret;
  }
  
  
  /**
   * Bestimmt die Anzahl an nicht mehr gestarteten Slaves und
   * speichert sie als canceled im BatchProcessArchive.
   * @throws PersistenceLayerException 
   */
  public void countCanceled() throws PersistenceLayerException {
    int canceled = 0;
    if (inputGenerator != null) {
      canceled = inputGenerator.getRemainingInputs();
    }
    if (runtimeInformation.getOpenDataPlanning() != null) {
      canceled += runtimeInformation.getOpenDataPlanning().size();
    }
    
    ArchiveUpdater updater = updateArchive();
    updater.canceled(canceled);
    updater.update();
  }
  
 
  public SlaveTask createSlaveTask(TransferCapacities transferCapacities) {
    //es darf immer nur ein Thread gleichzeitig den nächsten Input holen
    startSlavesLock.lock();
    try {
      //Können und dürfen weitere Slaves gestartet werden?
      if( ! batchProcessScheduling.canSlaveBeStarted(inputGenerator.hasNext()) ) {
        return null;
      }
      
      //es gibt noch Inputs, also den nächsten holen
      Pair<String,GeneralXynaObject> input = inputGenerator.next();
      //vergebenen Input persistieren
      RuntimeInformationUpdater updater = updateRuntimeInformation();
      updater.setCurrentInput(inputGenerator.getLastInputId(), input.getFirst());
      updater.update();
      
      incrementStarted();
      
      //XynaOrder anlegen
      XynaOrderServerExtension xynaOrder = createSlaveOrder(input.getSecond());
      
      return new SlaveTask(this, xynaOrder, input.getFirst(), transferCapacities);
    } catch (Throwable t) {
      //beim Bestimmen des nächsten Inputs ist ein Fehler aufgetreten
      //-> Batch Process pausieren, damit er später wieder fortgesetzt werden kann
      logger.warn("Exception on creating SlaveTasks -> pause batch process", t);
      handleThrowable(t);
      return null;
    } finally {
      startSlavesLock.unlock();
    }
  }
  
  /**
   * überprüfen, ob beim nächsten Scheduling überhaupt noch weitere Slaves
   * gestartet werden sollen, ansonsten wird der SchedulingState jetzt schon
   * auf Ignore gesetzt
   */
  public void checkNextSlavesCanBeStarted() {
    startSlavesLock.lock();
    try {
      batchProcessScheduling.canSlaveBeStarted(inputGenerator.hasNext());
    }
    catch (Exception e) {
      //nichts machen
      //beim nächsten Scheduling wird dann erneut versucht Slaves zu starten,
      //tritt hierbei die Exception wieder auf, wird sie durch createSlaveTask(...) behandelt
      logger.warn("Exception on creating SlaveTasks", e);
    } finally {
      startSlavesLock.unlock();
    }
  }
  
  /**
   * Legt eine XynaOrder mit dem übergebenen Input für einen Slave an,
   * überträgt die Kapazitäten vom Master auf den Slave und startet diesen.
   * @param xynaOrder
   * @param inputId
   * @param transferCapacities
   */
  public void startSlaveOrder(XynaOrderServerExtension xynaOrder, String inputId, TransferCapacities transferCapacities) {
    //ResponseListener und OrderContext anlegen
    ResponseListener rl = createSlaveResponseListener();
    OrderContext ctx = createSlaveOrderContext(xynaOrder, inputId);
    
    //Übertragen der vom Master reservierten Kapazitäten auf den Slave
    batchProcessScheduling.transferCapacities(xynaOrder,transferCapacities); 
    
    currentSlaves.put(xynaOrder.getId(), OrderInstanceStatus.RUNNING_PLANNING);
    
    //XynaOrder für Slave starten
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrder(xynaOrder, rl, ctx);
  }
  
  
  /**
   * Wartet bis alle gestarteten Slaves fertig sind oder das Timeout abgelaufen ist
   */
  public void waitForSlaves() {
    long end;
    AbsRelTime timeout = restartInformation.getSlaveOrderExecTimeout();
    if (timeout == null) {
      end =  System.currentTimeMillis() + XynaProperty.BATCH_CANCEL_WAIT_TIMEOUT.getMillis();
    } else if (timeout.isRelative()) {
      end =  System.currentTimeMillis() + timeout.getTime();
    } else {
      end = timeout.getTime();
    }
    
    long sleepTime = (end - System.currentTimeMillis()) / 20;
    while (runtimeInformation.getRunning() > 0 && System.currentTimeMillis() < end) {
      try {
        Thread.sleep(sleepTime);
      }
      catch (InterruptedException e) {
        // weiter warten
      } 
    }
  }
  
  
  /**
   * Bestimmt, ob alle Slaves gestartet worden sind und auch schon beendet wurden.
   * @return
   * @throws XynaException 
   */
  public boolean allSlavesFinished() throws XynaException {
    startSlavesLock.lock();
    try {
      if(inputGenerator.hasNext()) {
        return false; //es werden noch weitere Slaves gestartet
      }
      
      if (getTerminated() == started.get()) {
        return true; //alle gestarteten Slaves sind auch fertig
      }
      
      return false;
    } finally {
      startSlavesLock.unlock();  
    }
  }
  
  
  public static class BatchProcessMasterResponseListener extends ResponseListener {

    private static final long serialVersionUID = 1L;
    private Long batchProcessId;

    public BatchProcessMasterResponseListener(Long batchProcessId) {
      this.batchProcessId = batchProcessId;
    }

    @Override
    public void onResponse(GeneralXynaObject response, OrderContext ctx) throws XNWH_RetryTransactionException {
      terminateMaster(true);
    }

    @Override
    public void onError(XynaException[] e, OrderContext ctx) throws XNWH_RetryTransactionException {
      terminateMaster(false);
    }

    private void terminateMaster(final boolean success) {
      XynaRunnable r = new XynaRunnable() {

        public void run() {
          try {
            BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
            bpm.finishBatchProcess(batchProcessId, success);
          } catch (Throwable t) {
            Department.handleThrowable(t);
            logger.warn("Exception during execution of batch master response listener. batchProcessId=" + batchProcessId, t);
          }
        }

      };
      int retries = 0;
      while (true) {
        try {
          XynaExecutor.getInstance().executeRunnableWithCleanupThreadpool(r);
          return;
        } catch (RejectedExecutionException e) {
          if (++ retries > 10) {
            logger.warn("Could not execute batch master response listener. batchProcessId=" + batchProcessId, e);          
            return;
          }
          try {
            Thread.sleep(500 + new Random().nextInt(500));
          } catch (InterruptedException e1) {
            return;
          }
        }
      }
    }
  }
  
  
  public static class BatchProcessSlaveResponseListener extends ResponseListener {
    
    private static final long serialVersionUID = 1L;
    private Long batchProcessId;
    
    public BatchProcessSlaveResponseListener(BatchProcess batchProcess) {
      this.batchProcessId = batchProcess.getBatchProcessId();
    }
    
    @Override
    public void onResponse(GeneralXynaObject response, OrderContext ctx) throws XNWH_RetryTransactionException {
      terminateSlaves(ctx.getOrderId(), true);
    }
    
    @Override
    public void onError(XynaException[] e, OrderContext ctx) throws XNWH_RetryTransactionException {
      terminateSlaves(ctx.getOrderId(), false);
    }
    
    /**
     * Aktualisiert die Counter und stellt den Master wieder ein, falls alle Slaves fertig sind.
     * @param orderId
     * @param success
     */
    private void terminateSlaves(final long orderId, final boolean success) {
      XynaRunnable r = new XynaRunnable() {

        public void run() {
          try {
            BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
            bpm.terminateSlaves(batchProcessId, orderId, success);
          } catch (Throwable t) {
            Department.handleThrowable(t);
            logger.warn("Exception during execution of batch slave response listener. orderId=" + orderId + ", batchProcessId=" + batchProcessId, t);
          }
        }

      };
      int retries = 0;
      while (true) {
        try {
          XynaExecutor.getInstance().executeRunnableWithCleanupThreadpool(r);
          return;
        } catch (RejectedExecutionException e) {
          if (++ retries > 10) {
            logger.warn("Could not execute batch slave response listener. orderId=" + orderId + ", batchProcessId=" + batchProcessId, e);          
            return;
          }
          try {
            Thread.sleep(500 + new Random().nextInt(500));
          } catch (InterruptedException e1) {
            return;
          }
        }
      }
    }
  }

  /**
   * Anzeige von sämtlichen Informationen zu dem BatchProcess.
   * Von den Storables werden Kopien angelegt, damit die interen Storables geschützt gegen Änderungen sind
   * Daten, die noch nicht in den Storables abgelegt sind, werden ergänzt. 
   * @return
   */
  public BatchProcessInformation getBatchProcessInformation() {
    BatchProcessInformation bpi = new BatchProcessInformation(batchProcessArchiveData,false);
    bpi.setRuntimeInformation( new BatchProcessRuntimeInformationStorable(runtimeInformation) );
    bpi.setCustomization( new BatchProcessCustomizationStorable(customizationData) );
    bpi.setRestartInformation(new BatchProcessRestartInformationStorable(restartInformation));
    bpi.setMasterOrderCreationParameter(masterOrderCreationParameter);
    
    //Ergänzungen
    OrderInstanceStatus currentStatus = batchProcessScheduling.getOrderStatus(masterInCleanup);
    bpi.setBatchProcessStatus( getBatchProcessStatus() );
    bpi.setSchedulingState( batchProcessScheduling.getSchedulingState() );
    bpi.getArchive().setOrderStatus(currentStatus);
    //falls Zeitfenster nicht speziell für BatchProcess angelegt wurde, soll Definition trotzdem angezeigt werden 
    bpi.getRestartInformation().setTimeWindowDefinition(batchProcessScheduling.getTimeWindowDefinition());
    
    return bpi;
  }

  public BatchProcessStatus getBatchProcessStatus() {
    OrderInstanceStatus currentStatus = batchProcessScheduling.getOrderStatus(masterInCleanup);
    return BatchProcessStatus.from(batchProcessArchiveData.getOrderStatus(), currentStatus );
  }
  
  public void createStorables(Long batchProcessId, BatchProcessInput input) {
    this.batchProcessArchiveData = new BatchProcessArchiveStorable(batchProcessId, input);
    this.runtimeInformation = new BatchProcessRuntimeInformationStorable(batchProcessId);
    this.customizationData = new BatchProcessCustomizationStorable(batchProcessId);
    this.restartInformation = new BatchProcessRestartInformationStorable(batchProcessId, input);
    restartInformation.setMasterSchedulingData(masterOrder.getSchedulingData());
  }

  public CustomizationUpdater updateCustomFields() {
    return new CustomizationUpdater(customizationData,batchProcessArchiveData);
  }
  
  public RuntimeInformationUpdater updateRuntimeInformation() {
    return new RuntimeInformationUpdater(runtimeInformation);
  }
  
  public ArchiveUpdater updateArchive() {
    return new ArchiveUpdater(batchProcessArchiveData);
  }
  
  public void archiveStorables(boolean success) throws PersistenceLayerException {
    OrderInstanceStatus orderStatus;
    
    if (isCanceled()) {
      orderStatus = OrderInstanceStatus.CANCELED;
    } else if (isTimedout()) {
      orderStatus = OrderInstanceStatus.SCHEDULING_TIME_OUT;
    } else {
      orderStatus = success ? OrderInstanceStatus.FINISHED : OrderInstanceStatus.XYNA_ERROR;
    }
    //Zeitfenster noch ermitteln, damit später nachlesbar ist, welche TimeWindowDefinition verwendet wurde
    restartInformation.setTimeWindowDefinition(batchProcessScheduling.getTimeWindowDefinition());

    ArchiveBatchProcess abp = new ArchiveBatchProcess(batchProcessArchiveData, runtimeInformation, customizationData, restartInformation, orderStatus);
    
    WarehouseRetryExecutor.buildCriticalExecutor().
      storables(abp.getStorableClassList()).
      execute(abp);
  }
  
  /**
   * Stellt den BatchProcess mit den Informationen aus der Datenbank wieder her.
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY 
   * @throws PersistenceLayerException 
   * @throws XPRC_InputGeneratorInitializationException 
   */
  public void restoreBatchProcess() throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XPRC_InputGeneratorInitializationException {
    //Storables neu auslesen
    SelectStorables select = new SelectStorables(runtimeInformation, batchProcessArchiveData, customizationData, restartInformation);
    
    WarehouseRetryExecutor.buildCriticalExecutor().
    storables(select.getStorableClassList()).
    execute(select);
    
    batchProcessScheduling.schedulerMasterWith(restartInformation.getMasterSchedulingData());
    
    //InputGenerator anlegen und auf richtigen Stand bringen
    InputGeneratorData inputGeneratorData = new InputGeneratorData(restartInformation);
    createInputGenerator(inputGeneratorData, true);
    
    //ParallelExecutor wieder anlegen und Limitierung prüfen
    batchProcessScheduling.createParallelExecutorAndCheckLimitation(
        createSlaveOrderTypeInfo(), restartInformation.getSlaveExecutionPeriod(), 
        restartInformation.getTotal(), getMaxParallelism() );
  }
  
  private void restoreMasterOrderCreationParameter() {
    masterOrderCreationParameter = new RemoteXynaOrderCreationParameter(masterOrder.getDestinationKey(), masterOrder.getInputPayload() );
    masterOrderCreationParameter.setPriority(masterOrder.getPriority());
    masterOrderCreationParameter.setTimeConstraint(masterOrder.getSchedulingData().getTimeConstraint());
    masterOrderCreationParameter.setCustom0(masterOrder.getCustom0());
    masterOrderCreationParameter.setCustom1(masterOrder.getCustom1());
    masterOrderCreationParameter.setCustom2(masterOrder.getCustom2());
    masterOrderCreationParameter.setCustom3(masterOrder.getCustom3());
  }

  
  /**
   * Stellt einen BatchProcess wieder her und stellt ihn neu in den Scheduler ein.
   * @param waitForLock CountDownLatch, mit dem der Erhalt des Locks gemeldet wird
   */
  public void restartBatchProcess(CountDownLatch waitForLock) {
    managementOperationLock.lock();
    try {
      waitForLock.countDown();
      
      //BatchProcess wieder herstellen
      restoreBatchProcess();
      restoreMasterOrderCreationParameter();
      
      TimeConstraint tc = batchProcessScheduling.prepareTimeContraint(batchProcessId, masterOrder.getSchedulingData().getTimeConstraint(), restartInformation.getTimeWindowDefinition() );
      masterOrderCreationParameter.setTimeConstraint(tc);
      
      batchProcessScheduling.restart(restartInformation.getMasterSchedulingData());
      
    } catch (Throwable t) {
      handleThrowable(t);
    } finally {
      managementOperationLock.unlock();
    }
  }


  public void switchToRunningSlave(long slaveOrderId) {
    currentSlaves.put(slaveOrderId, OrderInstanceStatus.RUNNING);
  }
  
  
  /**
   * Bricht alle laufenden Slaves ab
   */
  public void cancelSlaves(boolean onlyScheduling, long callerOrderId) {
    //FIXME direkt nach einem Neustart der Factory sind evtl. noch nicht
    //alle Slaves wieder in die Map aufgenommen worden. Dies ist erst nach 
    //einem vollständigen Scheduler-Durchlauf der Fall.
    for (Long id : currentSlaves.keySet()) {
      if (id.equals(callerOrderId)) {
        continue; //nicht versuchen den slave abzubrechen, der das cancel selbst aufruft.
      }
      OrderInstanceStatus slaveStatus = currentSlaves.get(id);
      boolean kill = false;
      
      //falls sich der Slave noch im Planning oder Scheduling befindet -> cancel
      boolean cancel = slaveStatus == OrderInstanceStatus.RUNNING_PLANNING
          || slaveStatus == OrderInstanceStatus.SCHEDULING;
      
      if( cancel ) {
        try {
          CancelBean cancelResult = XynaFactory.getInstance().getProcessing().cancelOrder(id, Constants.DEFAULT_CANCEL_TIMEOUT);
          if( cancelResult.getResult() == CANCEL_RESULT.FAILED ) {
            kill = true; //cancel hat nicht geklappt, evtl. hilft kill
          }
        } catch (XynaException e) {
          logger.warn("could not cancel slave " + id ,e);
        }
      }
      
      if( onlyScheduling ) {
        continue;
      } else {
        //falls sich der Slave schon in der Execution befindet -> kill
        kill = kill || slaveStatus == OrderInstanceStatus.RUNNING;
        if (kill) {
          try {
            KillStuckProcessBean input = new KillStuckProcessBean(id, true, AbortionCause.MANUALLY_ISSUED);
            @SuppressWarnings("unused")
            KillStuckProcessBean killResult = XynaFactory.getInstance().getProcessing().killStuckProcess(input);
            //TODO wie result auswerten?
          } catch (XynaException e) {
            logger.warn("could not kill slave " + id ,e);
          }
        }
      }
    }
  }
  
  
  public boolean isPaused() {
    return runtimeInformation.getState() == BatchProcessState.PAUSED;
  }

  public boolean isCanceled() {
    return runtimeInformation.getState() == BatchProcessState.CANCELED;
  }
  
  public boolean isTimedout() {
    return runtimeInformation.getState() == BatchProcessState.TIMEOUT;
  }

  /**
   * Überprüft, der batchProcess zur angegebenen Revision gehört
   * @param revision
   * @return
   */
  public boolean isInRevision(Long revision) {
    return batchProcessArchiveData.getRevision().equals(revision);
  }
  
  
  /**
   * Pausiert einen Batch Process.
   * @param pauseCause
   * @throws PersistenceLayerException 
   */
  public boolean pauseBatchProcess(String pauseCause) throws PersistenceLayerException {
    managementOperationLock.lock();
    try{
      if( batchProcessScheduling.pause(true) ) {
        RuntimeInformationUpdater updaterR = updateRuntimeInformation();
        updaterR.pauseBatchProcess(pauseCause);
        updaterR.update();
        
        ArchiveUpdater updaterA = updateArchive();
        updaterA.pauseBatchProcess();
        updaterA.update();
        
        return true;
      }
    } catch (PersistenceLayerException e) {
      handleManagementOperationException(e, "pauseBatchProcess");
      throw e;
    } catch (RuntimeException e) {
      handleManagementOperationException(e, "pauseBatchProcess");
      throw e;
    } finally {
      managementOperationLock.unlock();
    }
    
    return false;
  }
  
  /**
   * Pausiert den Batch Process, falls er zur angegebenen Revision gehört und nicht
   * bereits pausiert ist
   * @param revision
   */
  public void pauseBatchProcess(Long revision, PauseCause pauseCause) throws PersistenceLayerException {
    if (isInRevision(revision) && !isPaused()) {
      pauseBatchProcess(pauseCause.getCause());
    }
  }
  
  /**
   * Startet der Batch Prozess zur Zeit neue Slaves in der Revision?
   * D.h. er gehört zur Revision, ist nicht pausiert und hat noch nicht alle Slaves gestartet.
   * @param revision
   * @return
   */
  public boolean startsCurrentlySlaves(Long revision) {
    if (!isInRevision(revision)) {
      return false; //BatchProcess gehört nicht zur Revision
    }
    
    if (isPaused()) {
      return false; //BatchProcess ist pausiert
    }
    
    if (allSlavesStarted.get()) {
      return false; //es wurden bereits alle Slaves gestartet
    }
    
    return true;
  }
  
  
  /**
   * Setzt den Status der Application des Batch Prozesses auf Running
   */
  private void setApplicationRunning(){
    ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    if (batchProcessArchiveData.getApplication() != null) {
      applicationManagement.changeApplicationState(batchProcessArchiveData.getApplication(), batchProcessArchiveData.getVersion(), ApplicationState.RUNNING);
    }
  }

  /**
   * Überprüft, ob die Application existiert und nicht im Zustand AUDIT_MODE ist.
   * @param applicationName
   * @param versionName
   */
  private void checkApplication(String applicationName, String versionName) throws PersistenceLayerException{
    ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    if (applicationName != null) {
      ApplicationState state = applicationManagement.getApplicationState(applicationName, versionName);
      if (state == null) {
        throw new IllegalArgumentException("version '" + versionName + "' not found in application '" + applicationName +"'");
      }
      if (state == ApplicationState.AUDIT_MODE) {
        throw new IllegalStateException("could not create or migrate batch process in AUDIT_MODE application '" + applicationName + "' '" + versionName + "'");
      }
    }
  }
  
  /**
   * Setzt einen pausierten Batch Process wieder fort.
   * @return
   * @throws PersistenceLayerException
   */
  public boolean continueBatchProcess() throws PersistenceLayerException {
    managementOperationLock.lock();
    try{
      if (isPaused()) { //BatchProcess ist noch unterbrochen
        if ( batchProcessScheduling.canBeContinued() ) {
          //Status wieder auf Running setzen
          RuntimeInformationUpdater updaterR = updateRuntimeInformation();
          updaterR.continueBatchProcess();
          updaterR.update();
          
          ArchiveUpdater updaterA = updateArchive();
          updaterA.continueBatchProcess();
          updaterA.update();
         
          batchProcessScheduling.rescheduleMaster(false, false);
          
          //Application (wieder) auf Running setzen
          setApplicationRunning();
          
          return true;
        } else {
          logger.info("BatchProcess "+batchProcessId+" cannot be continued in state "+batchProcessScheduling.getSchedulingState() );
        }
      }
    } catch (PersistenceLayerException e) {
      handleManagementOperationException(e, "continueBatchProcess");
      throw e;
    } catch (RuntimeException e) {
      handleManagementOperationException(e, "continueBatchProcess");
      throw e;
    } finally {
      managementOperationLock.unlock();
    }
    
    return false;
  }
  
  /**
   * Setzt den Batch Process wieder fort, falls er zur angegebenen Revision gehört
   * @param revision
   */
  public void continueBatchProcess(Long revision) throws PersistenceLayerException {
    if (batchProcessArchiveData.getRevision().equals(revision)) {
      continueBatchProcess();
    }
  }
  
  /**
   * Überprüft, ob der BatchProcess mit dem erwarteten PauseCause pausiert wurde.
   * @param expectedPauseCause
   * @return true, wenn der aktuelle PauseCause aus der RuntimeInformation mit 
   *   expectedPauseCause übereinstimmt
   */
  public boolean pausedWith(PauseCause expectedPauseCause) {
    if (runtimeInformation.getPauseCause() == null) {
      return false;
    }
    
    return runtimeInformation.getPauseCause().equals(expectedPauseCause.getCause());
  }
  
  
  /**
   * Ändert die Werte des BatchProcesses, die im übergebenen Input ungleich null sind.
   * @param input
   * @throws PersistenceLayerException 
   */
  public boolean modifyBatchProcess(BatchProcessInput input) throws PersistenceLayerException {
    managementOperationLock.lock(); //Warten bis andere Operationen fertig sind
    try {
      //Batch Process pausieren, falls er noch läuft
      if (!isPaused()) {
        if (!pauseBatchProcess(PauseCause.MODIFY.getCause())) {
          return false;
        }
      }
      
      changeSchedulingData(input);
      
      //MasterOrder ändern
      changeMasterOrder(input.getMasterOrder());
      
      //Daten im BatchProcessArchive ändern
      changeArchiveData(input);
      
      //TODO weitere Änderungen ermöglichen (z.B. InputDaten für Slaves)
      //changeInputGenerator, changeSlave
      
      //Batch Process fortsetzen, falls er nicht schon vorher pausiert war
      if (pausedWith(PauseCause.MODIFY)) {
        continueBatchProcess();
      }
    } catch (PersistenceLayerException e) {
      handleManagementOperationException(e, "modifyBatchProcess");
      throw e;
    } catch (RuntimeException e) {
      handleManagementOperationException(e, "modifyBatchProcess");
      throw e;
    } finally {
      managementOperationLock.unlock();
    }
    
    return true;
  }
  
  
  private void changeSchedulingData(BatchProcessInput input) throws PersistenceLayerException {
    TimeConstraint newTC = null;
    if( input.getMasterOrder() != null ) {
      newTC = input.getMasterOrder().getTimeConstraint();
    }
    TimeWindowDefinition newTWD = input.getTimeWindowDefinition();
    
    TimeConstraint tc = batchProcessScheduling.changeMasterTimeConstraint(newTC, newTWD);
    masterOrderCreationParameter.setTimeConstraint(tc);
    
    restartInformation.setTimeWindowDefinition(input.getTimeWindowDefinition());
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {
      //restartInformation persistieren
      con.persistObject(restartInformation);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }
  
  /**
   * Ändert die MasterOrder
   * TODO weitere Änderungen, im Moment werden nur die CustomFelder geändert
   * @param newMasterOrder
   * @throws PersistenceLayerException
   */
  private void changeMasterOrder(RemoteXynaOrderCreationParameter newMasterOrder) throws PersistenceLayerException {
    if (newMasterOrder != null) {
      //Custom-Felder ändern
      changeCustomFields(newMasterOrder);
    }
    
    //Backup
    masterOrder.setHasBeenBackuppedAfterChange(false);
    XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().backup(masterOrder, BackupCause.ACKNOWLEDGED);
  }
  
  /**
   * Ändert die Custom-Felder in der MasterOrder
   * @param newMasterOrder
   */
  private void changeCustomFields(RemoteXynaOrderCreationParameter newMasterOrder) {
    if (newMasterOrder.getCustom0() != null) {
      masterOrderCreationParameter.setCustom0(newMasterOrder.getCustom0());
      masterOrder.setCustom0(newMasterOrder.getCustom0());
    }
    if (newMasterOrder.getCustom1() != null) {
      masterOrderCreationParameter.setCustom1(newMasterOrder.getCustom1());
      masterOrder.setCustom1(newMasterOrder.getCustom1());
    }
    if (newMasterOrder.getCustom2() != null) {
      masterOrderCreationParameter.setCustom2(newMasterOrder.getCustom2());
      masterOrder.setCustom2(newMasterOrder.getCustom2());
    }
    if (newMasterOrder.getCustom3() != null) {
      masterOrderCreationParameter.setCustom3(newMasterOrder.getCustom3());
      masterOrder.setCustom3(newMasterOrder.getCustom3());
    }
  }
  

  /**
   * Ändert Label und Component im BatchProcessArchive
   * @param BatchProcessInput
   * @throws PersistenceLayerException 
   */
  private void changeArchiveData(BatchProcessInput newData) throws PersistenceLayerException {
    ArchiveUpdater updaterA = updateArchive();
    updaterA.labelAndComponent( newData.getLabel(), newData.getComponent() );
    updaterA.update();
  }
  
  /**
   * Bricht einen Batch Process ab. D.h. es werden keine neuen Slaves mehr gestartet und die
   * laufenden Slaves werden abgebrochen. Bei CancelMode == WAIT wird vor dem Abbrechen gewartet,
   * ob die Slaves innerhalb ihres OrderExecutionTimeouts fertig werden.
   * Anschließend geht der Master in die Execution-Phase über. Das ist ein entscheidender Unterschied zum Cancel im Scheduler.
   * @param cancelMode
   * @param ignoreTimeConstraintsOfMaster falls true, werden die master timeconstraints auf "immediately" umgestellt werden?
   * @return true, falls der Batch Process abgebrochen wurde
   * @throws PersistenceLayerException 
   */
  public boolean cancelBatchProcess(CancelMode cancelMode, boolean ignoreTimeConstraintsOfMaster, long callerOrderId) throws PersistenceLayerException {
    managementOperationLock.lock(); //Warten bis andere Operationen fertig sind
    try {
      if( ! batchProcessScheduling.canBeCanceled() ) {
        return false;
      }
      boolean wasPaused = isPaused(); 
      String oldPauseCause = runtimeInformation.getPauseCause();
      
      //Status auf canceled setzen
      RuntimeInformationUpdater updater = updateRuntimeInformation();
      updater.cancelBatchProcess();
      updater.update();

      if( batchProcessScheduling.cancel() ) {
        //schedulende Slaves canceln 
        cancelSlaves(true, -1L);

        if (cancelMode == CancelMode.WAIT || cancelMode == CancelMode.WAIT_KEEP_PAUSED ) {
          //warten bis alle gestarteten Slaves fertig sind oder ExecutionTimeout abgelaufen ist
          waitForSlaves();
        }

        //restliche laufende Slaves abbrechen
        cancelSlaves(false, callerOrderId);
      }

      //Anzahl an nicht mehr gestarteten Slaves bestimmen
      countCanceled();

      if( wasPaused && (cancelMode == CancelMode.WAIT_KEEP_PAUSED || cancelMode == CancelMode.KILL_SLAVES_KEEP_PAUSED) ) {
        pauseBatchProcess(oldPauseCause);
      } else {
        batchProcessScheduling.rescheduleMaster(true, ignoreTimeConstraintsOfMaster);
      }
      
      
      return true;

    } catch (PersistenceLayerException e) {
      handleManagementOperationException(e, "cancelBatchProcess");
      throw e;
    } catch (RuntimeException e) {
      handleManagementOperationException(e, "cancelBatchProcess");
      throw e;
    } finally {
      managementOperationLock.unlock();
    }
  }
  
  
  /**
   * Migriert den Batch Process auf eine andere Application-Version
   * @param application
   * @param version
   * @return
   * @throws XynaException
   */
  public boolean migrateBatchProcess(String application, String version) throws XynaException {
    return migrateBatchProcess(new Application(application, version));
  }
  
  
  public boolean migrateBatchProcess(RuntimeContext to) throws XynaException {
    if (to instanceof Application) {
      //überprüfen, ob die Application existiert und nicht im Zustand AUDIT_MODE ist
      checkApplication(((Application)to).getName(), ((Application)to).getVersionName());
    }
    
    managementOperationLock.lock();
    try{
      //Batch Process pausieren, falls er noch läuft
      if (!isPaused()) {
        if (!pauseBatchProcess(PauseCause.MIGRATE.getCause())) {
          return false;
        }
      }
      
      //es dürfen keine Slaves mehr laufen
      if (runtimeInformation.getRunning() > 0) {
        if (pausedWith(PauseCause.MIGRATE)) {
          continueBatchProcess();
        }
        return false;
      }
      
      if (to instanceof Application) {
        //Application und Version im BatchProcessArchive ändern
        batchProcessArchiveData.setApplication(((Application)to).getName());
        batchProcessArchiveData.setVersion(((Application)to).getVersionName());
        batchProcessArchiveData.setWorkspace(null);
      } else if (to instanceof Workspace) {
        batchProcessArchiveData.setApplication(null);
        batchProcessArchiveData.setVersion(null);
        batchProcessArchiveData.setWorkspace(((Workspace)to).getName());
      }
      Long revision = batchProcessArchiveData.getRevision();
      
      ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
      try {
        //BatchProcessArchive persistieren
        con.persistObject(batchProcessArchiveData);
        
        //Master migrieren
        migrateMaster(con, to, revision);
        con.commit();
      } finally {
        finallyClose(con);
      }
      
      //Application und Version in SlaveOrderTypeInfo ändern
      batchProcessScheduling.getSlaveOrderTypeInfo().fillFromBatchProcessArchive(batchProcessArchiveData);
      
      //InputGenerator aktualisieren, damit Objekte mit dem richtigen ClassLoader geladen werden
      inputGenerator.changeRevision(revision);

      //Batch Process fortsetzen, falls er nicht schon vorher pausiert war
      if (pausedWith(PauseCause.MIGRATE)) {
        continueBatchProcess();
      }
    } catch (XynaException e) {
      handleManagementOperationException(e, "migrateBatchProcess");
      throw e;
    } catch (RuntimeException e) {
      handleManagementOperationException(e, "migrateBatchProcess");
      throw e;
    } finally {
      managementOperationLock.unlock();
    }
    
    return true;
  }
  
  /**
   * InputGenerator aktualisieren, damit Objekte mit dem richtigen ClassLoader geladen werden
   */
  public void refreshInputGenerator()  {
    try {
      Long revision = batchProcessArchiveData.getRevision();
      inputGenerator.changeRevision(revision);
    } catch (XynaException e) {
      handleManagementOperationException(e, "migrateBatchProcess");
    } catch (RuntimeException e) {
      handleManagementOperationException(e, "migrateBatchProcess");
    }
  }
  
  /**
   * Migriert den Master auf eine neue Application-Version
   * @param con
   * @param application
   * @param version
   * @param revision
   * @throws XynaException
   */
  private void migrateMaster(ODSConnection con, RuntimeContext to, Long revision) throws XynaException {
    masterOrder.getDestinationKey().setRuntimeContext(to);
    masterOrder.setRevision(revision);
    
    //InputDaten neu laden
    GeneralXynaObject gxo = masterOrder.getInputPayload();
    if (gxo != null) {
      String asXML = gxo.toXml();
      if (asXML.length() > 0) {
        gxo = XynaObject.generalFromXml(asXML, revision);
        masterOrder.setInputPayload(gxo);    
      }
      //else: leerer container
    }
    
    //Application und Version im OrderArchive anpassen und XynaOrder backuppen
    XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().updateApplicationVersion(masterOrder, to, con);
  }
  
  
  
  /**
   * Pausiert einen BatchProcess nach einer Exception und speichert die
   * Message als PauseCause ab
   */
  public void handleThrowable(Throwable t) {
    Department.handleThrowable(t);
    String pauseCause = t.getClass().getSimpleName() + ": " + t.getMessage();
    try {
      pauseBatchProcess(pauseCause);
    }
    catch (Throwable t2) {
      Department.handleThrowable(t2);
      logger.warn("Could not pause batch process", t2);
    }
  }

  private void handleManagementOperationException(Exception e, String method) {
    try {
      String pauseCause = e.getClass().getSimpleName() + " in " + method +": " + e.getMessage();
      RuntimeInformationUpdater updater = updateRuntimeInformation();
      updater.pauseBatchProcess(pauseCause);
      updater.update();
      if (logger.isDebugEnabled()) {
        logger.debug("Exception in " + method + " for batch process " + batchProcessId, e);
      }
    } catch (Exception e2) {
      logger.warn("Could not update pauseCause for batch process", e2);
    }
  }
  
  public SlaveOrderTypeInfo createSlaveOrderTypeInfo() {
    SlaveOrderTypeInfo slaveOrderTypeInfo = new SlaveOrderTypeInfo(batchProcessArchiveData.getSlaveOrdertype());
    slaveOrderTypeInfo.setMasterOrder(masterOrder);
    slaveOrderTypeInfo.fillFromBatchProcessArchive(batchProcessArchiveData);
    return slaveOrderTypeInfo;
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


  public void removeTimeWindow() {
    batchProcessScheduling.removeTimeWindow(batchProcessId);
  }

  public void persistStorables(ODSConnection connection) throws PersistenceLayerException {
    connection.persistObject(runtimeInformation);
    connection.persistObject(batchProcessArchiveData);
    connection.persistObject(customizationData);
    connection.persistObject(restartInformation);
  }

  public void acknowledgeMasterBackup() {
    managementOperationLock.lock();
    try {
      //nun kann sicher auf isPaused() zugegriffen werden 
      //TODO Wenn startBatchProcess per managementOperationLock geschützt wird, darf hier Lock nicht verwendet werden
      batchProcessScheduling.acknowledgeMasterBackup(isPaused());
    } finally {
      managementOperationLock.unlock();
    }
  }
  
  
  public void setAllSlavesStarted() {
    this.allSlavesStarted.set(true);
  }

  public void storeTimeoutStateIfNecessary() throws PersistenceLayerException {
    if (runtimeInformation.getState() == BatchProcessState.TIMEOUT) {
      RuntimeInformationUpdater updater = updateRuntimeInformation();
      updater.timeoutBatchProcess();
      updater.update();
    }
  }

  public void setTimeoutTransiently() {
    runtimeInformation.setState(BatchProcessState.TIMEOUT);
  }

}
