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
package xprc.xbatchmgmt.impl;


import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.ExecutionPeriod.Type;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.ExecutionTimeoutConfiguration;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessMarker;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.SlaveExecutionPeriod;
import com.gip.xyna.xprc.xbatchmgmt.input.InputGeneratorData;
import com.gip.xyna.xprc.xbatchmgmt.input.InputGeneratorData.InputGeneratorType;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRuntimeInformationStorable;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage.ChildOrderStorageStack;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint.TimeConstraint_Start;
import com.gip.xyna.xprc.xsched.xynaobjects.AbsoluteDate;
import com.gip.xyna.xprc.xsched.xynaobjects.Date;
import com.gip.xyna.xprc.xsched.xynaobjects.RelativeDate;

import xprc.xbatchmgmt.BatchProcessCounterFields;
import xprc.xbatchmgmt.BatchProcessCustomFields;
import xprc.xbatchmgmt.BatchProcessCustomizationServiceServiceOperation;
import xprc.xbatchmgmt.BatchProcessId;
import xprc.xbatchmgmt.BatchProcessInput;
import xprc.xbatchmgmt.BatchProcessRuntimeInformation;
import xprc.xbatchmgmt.CancelResult;
import xprc.xbatchmgmt.NoBatchOrderException;
import xprc.xbatchmgmt.cancelmode.CancelKillRunningSlaves;
import xprc.xbatchmgmt.cancelmode.CancelMode;
import xprc.xbatchmgmt.cancelmode.CancelWaitForRunningSlaves;
import xprc.xbatchmgmt.slavestatus.SlaveSchedulingCancelled;
import xprc.xbatchmgmt.slavestatus.SlaveSchedulingFinished;
import xprc.xbatchmgmt.slavestatus.SlaveSchedulingHadTimeout;
import xprc.xbatchmgmt.slavestatus.SlaveSchedulingIsWaiting;
import xprc.xbatchmgmt.slavestatus.SlaveSchedulingNotStarted;
import xprc.xbatchmgmt.slavestatus.SlaveSchedulingPaused;
import xprc.xbatchmgmt.slavestatus.SlaveStatus;
import xprc.xbatchmgmt.slavestatus.SlavesAreRunning;
import xprc.xpce.timeconstraint.TimeWindow;


public class BatchProcessCustomizationServiceServiceOperationImpl implements ExtendedDeploymentTask, BatchProcessCustomizationServiceServiceOperation {

  Logger logger = CentralFactoryLogging.getLogger(BatchProcessCustomizationServiceServiceOperationImpl.class);
  
  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.;
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.;
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.;
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }
  
  private static BatchProcessMarker getBatchProcessMarker(XynaOrderServerExtension xynaOrder) throws NoBatchOrderException {
    BatchProcessMarker bpm = xynaOrder.getRootOrder().getBatchProcessMarker();
    if( bpm == null ) {
      throw new NoBatchOrderException();
    }
    return bpm;
  }

  private static BatchProcessManagement getBatchProcessManagement() {
    return XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
  }

  private BatchProcessInformation getBatchProcessInformation(BatchProcessMarker bpm) {
    BatchProcessInformation bpi;
    try {
      bpi = getBatchProcessManagement().getBatchProcessInformation(bpm.getBatchProcessId());
    } catch (XynaException e) {
      //XynaException ensteht, wenn batchProcessId ungï¿½ltig ist. Dies sollte hier nie der Fall sein
      throw new RuntimeException(e);
    }
    return bpi;
  }

  
  public BatchProcessId getBatchProcessId(XynaOrderServerExtension correlatedXynaOrder) throws NoBatchOrderException {
    BatchProcessMarker bpm = getBatchProcessMarker(correlatedXynaOrder);
    return new BatchProcessId(bpm.getBatchProcessId());
  }  

  public BatchProcessCustomFields getCustomFields(XynaOrderServerExtension correlatedXynaOrder)
      throws NoBatchOrderException {
    BatchProcessMarker bpm = getBatchProcessMarker(correlatedXynaOrder);
    BatchProcessInformation bpi = getBatchProcessInformation(bpm);
    return new BatchProcessCustomFields( bpi.getArchive().getCustomsAsList() );
  }

  
  public BatchProcessCounterFields getCounterFields(XynaOrderServerExtension correlatedXynaOrder) throws NoBatchOrderException {
    BatchProcessMarker bpm = getBatchProcessMarker(correlatedXynaOrder);
    BatchProcessInformation bpi = getBatchProcessInformation(bpm);
    List<Double> counter = bpi.getCustomization().getCountersAsList();
    return new BatchProcessCounterFields(counter);
  }

  public BatchProcessRuntimeInformation getRuntimeInformation(XynaOrderServerExtension correlatedXynaOrder)
      throws NoBatchOrderException {
    BatchProcessMarker bpm = getBatchProcessMarker(correlatedXynaOrder);
    BatchProcessInformation bpi = getBatchProcessInformation(bpm);
    BatchProcessRuntimeInformationStorable data = bpi.getRuntimeInformation();
    BatchProcessRuntimeInformation info = new BatchProcessRuntimeInformation();
    info.setTotal(bpi.getArchive().getTotal());
    info.setFinished(data.getFinished());
    info.setFailed(data.getFailed());
    info.setCanceled(bpi.getCanceled());
    return info;
  }
  
 
  public void setCustomFields(XynaOrderServerExtension correlatedXynaOrder, BatchProcessCustomFields customFields)
      throws NoBatchOrderException {
    BatchProcessMarker bpm = getBatchProcessMarker(correlatedXynaOrder);
    getBatchProcessManagement().setCustomFields(bpm.getBatchProcessId(), customFields.getCustom() );
  }
  
  public void addCounterFields(XynaOrderServerExtension correlatedXynaOrder, BatchProcessCounterFields counterFields)
      throws NoBatchOrderException {
    BatchProcessMarker bpm = getBatchProcessMarker(correlatedXynaOrder);
    getBatchProcessManagement().addCounterFields(bpm.getBatchProcessId(), counterFields.getCounter() );    
  }

  public void setCounterFields(XynaOrderServerExtension correlatedXynaOrder, BatchProcessCounterFields counterFields)
      throws NoBatchOrderException {
    BatchProcessMarker bpm = getBatchProcessMarker(correlatedXynaOrder);
    getBatchProcessManagement().setCounterFields(bpm.getBatchProcessId(), counterFields.getCounter() );    
  }

  public BatchProcessId startBatchProcess(BatchProcessInput batchProcessInput) throws XynaException {
    
    com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput bpi = new com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput();
    bpi.setLabel(batchProcessInput.getLabel());
    bpi.setComponent(batchProcessInput.getComponent());
    bpi.setMasterOrder( createXynaOrderCreationParameter(batchProcessInput.getMasterOrder() ) );
    bpi.setSlaveOrderType( batchProcessInput.getSlaveOrder().getOrderType().getOrderType() );
    if (batchProcessInput.getSlaveOrder().getExecutionTimeout() != null) {
      bpi.setSlaveOrderExecTimeout( batchProcessInput.getSlaveOrder().getExecutionTimeout().toAbsRelTime() );
    }
    //private AbsRelTime slaveWorkflowExecTimeout; //WorkflowExecutionTimeout der Slaves
    bpi.setSlaveTimeConstraint( createTimeConstraint( batchProcessInput.getSlaveOrder().getTimeConstraint() ) );
    bpi.setInputGeneratorData(createInputGeneratorData(batchProcessInput.getInputGeneratorData()) );
    bpi.setSlaveExecutionPeriod(createSlaveExecutionPeriod(batchProcessInput.getSlaveOrder().getExecutionPeriod()) );
    bpi.setMaxParallelism( batchProcessInput.getSlaveOrder().getMaxParallelism() );
    bpi.setGuiRepresentationData("UTC@false"); //derzeit gibt es keine möglichkeit im xmom der date/time typen auszudrücken, in welcher zeitzone man die zeiten angegeben haben möchte
    
    BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
   
    Long batchProcessId = bpm.startBatchProcess(bpi);
    return new BatchProcessId(batchProcessId);
  }


  private SlaveExecutionPeriod createSlaveExecutionPeriod(xprc.xbatchmgmt.SlaveExecutionPeriod executionPeriod) {
    if( executionPeriod instanceof xprc.xbatchmgmt.FixedInterval ) {
      long interval = ((xprc.xbatchmgmt.FixedInterval)executionPeriod).getInterval().toMillis();
      return new SlaveExecutionPeriod(Type.FixedInterval, interval);
    } else if ( executionPeriod instanceof xprc.xbatchmgmt.FixedDate ) {
      long interval = ((xprc.xbatchmgmt.FixedDate)executionPeriod).getInterval().toMillis();
      xprc.xbatchmgmt.MissedExecution me = ((xprc.xbatchmgmt.FixedDate)executionPeriod).getMissedExecution();
      if( me instanceof xprc.xbatchmgmt.CatchUpImmediately ) {
        return new SlaveExecutionPeriod(Type.FixedDate_CatchUpImmediately, interval);
      } else if( me instanceof xprc.xbatchmgmt.CatchUpInPast ) {
        return new SlaveExecutionPeriod(Type.FixedDate_CatchUpInPast, interval);
      } if( me instanceof xprc.xbatchmgmt.CatchUpNotTooLate ) {
        if( ((xprc.xbatchmgmt.CatchUpNotTooLate)me).getMaxTooLate() != null ) {
          long max = ((xprc.xbatchmgmt.CatchUpNotTooLate)me).getMaxTooLate().toMillis();
          return new SlaveExecutionPeriod(Type.FixedDate_CatchUpNotTooLate, interval, max);
        } else {
          return new SlaveExecutionPeriod(Type.FixedDate_CatchUpNotTooLate, interval);
        }
      } else {
        //TODO eigentlich Fehler?
        return new SlaveExecutionPeriod(Type.FixedDate_CatchUpImmediately, interval);
      }
    } else {
      return null;
    }
  }

  private InputGeneratorData createInputGeneratorData(xprc.xbatchmgmt.InputGeneratorData inputGeneratorData) {
    InputGeneratorData igd = null;
    if( inputGeneratorData instanceof xprc.xbatchmgmt.ConstantInputGeneratorData ) {
      xprc.xbatchmgmt.ConstantInputGeneratorData cigd = (xprc.xbatchmgmt.ConstantInputGeneratorData)inputGeneratorData;
      igd = new InputGeneratorData(InputGeneratorType.Constant);
      igd.setMaximumInputs(defaultInt(cigd.getMaximumInputs(), 0));
      igd.setConstantInput(createPayload(cigd.getConstantInput()));
    } else if( inputGeneratorData instanceof xprc.xbatchmgmt.OnTheFlyInputGeneratorData ) {
      xprc.xbatchmgmt.OnTheFlyInputGeneratorData otfigd = (xprc.xbatchmgmt.OnTheFlyInputGeneratorData)inputGeneratorData;
      igd = new InputGeneratorData(InputGeneratorType.OnTheFly);
      igd.setConstantInput(createPayload(otfigd.getConstantInput()));
      igd.setMaximumInputs( defaultInt(otfigd.getMaximumInputs(), 0) );
      igd.setQuery(otfigd.getQuery());
      igd.setStorable(otfigd.getStorable());
      igd.setSortCriteria(otfigd.getSortCriteria());
    } else {
      throw new RuntimeException("InputGenerator not set");
    }
    return igd;
  }

  private int defaultInt(Integer value, int defValue) {
    if( value != null ) {
      return value.intValue();
    }
    return defValue;
  }

  private static String createPayload(xprc.xpce.InputPayload inputPayload) {
    if( inputPayload instanceof xprc.xpce.XMLInputPayload ) {
      return ((xprc.xpce.XMLInputPayload)inputPayload).getInputPayload();
    } else if( inputPayload instanceof xprc.xpce.AnyInputPayload ) {
      Container c = new Container();
      for( GeneralXynaObject o : ((xprc.xpce.AnyInputPayload)inputPayload).getInputPayload() ) {
        c.add(o);
      }
      return "<Container>\n"+c.toXml()+"\n</Container>";
    } else {
      return null;
    }
  }

  private TimeConstraint createTimeConstraint(xprc.xpce.timeconstraint.TimeConstraint timeConstraint) {
    if( timeConstraint instanceof xprc.xpce.timeconstraint.Immediate ) {
      return TimeConstraint.immediately();
    } else if( timeConstraint instanceof xprc.xpce.timeconstraint.Relative ) {
      return TimeConstraint.delayed(((xprc.xpce.timeconstraint.Relative)timeConstraint).getRelativeDate().toMillis());
    } else if( timeConstraint instanceof xprc.xpce.timeconstraint.Absolute ) {
      return TimeConstraint.at(((xprc.xpce.timeconstraint.Absolute)timeConstraint).getAbsoluteDate().toMillis());
    } else if (timeConstraint instanceof TimeWindow) {
      TimeWindow tw = (TimeWindow) timeConstraint;
      TimeConstraint_Start tc;

      //start
      if (tw.getStart() != null) {
        long startTimeAbs;
        if (tw.getStart() instanceof RelativeDate) {
          long m = tw.getStart().toMillis();
          tc = TimeConstraint.delayed(m);
          startTimeAbs = System.currentTimeMillis() + m;
        } else if (tw.getStart() instanceof AbsoluteDate) {
          startTimeAbs = tw.getStart().toMillis();
          tc = TimeConstraint.at(startTimeAbs);
        } else {
          throw new RuntimeException("unexpected start time type: " + tw.getStart().getClass().getName());
        }
      } else {
        tc = TimeConstraint.immediately();
      }

      //end
      if (tw.getEnd() != null) {
        if (tw.getEnd() instanceof RelativeDate) {
          tc = tc.withSchedulingTimeout(tw.getEnd().toMillis());
        } else if (tw.getEnd() instanceof AbsoluteDate) {
          tc = tc.withAbsoluteSchedulingTimeout(tw.getEnd().toMillis());
        } else {
          throw new RuntimeException("unexpected end time type: " + tw.getEnd().getClass().getName());
        }
      }

      //timewindow
      if (tw.getTimeWindowName() != null) {
        if (tw.getTimeWindowOffset() != null) {
          return tc.withTimeWindow(tw.getTimeWindowName(), TimeConstraint.delayed(tw.getTimeWindowOffset().toMillis()));
        } else {
          return tc.withTimeWindow(tw.getTimeWindowName());
        }
      } else {
        return tc;
      }
    }
    return null;
  }

  private RemoteXynaOrderCreationParameter createXynaOrderCreationParameter(xprc.xpce.XynaOrderCreationParameter xocp) throws XFMG_NoSuchRevision {
    DestinationKey dk = createDestinationKey(xocp.getRuntimeContext(), xocp.getOrderType());
    RemoteXynaOrderCreationParameter rxocp = new RemoteXynaOrderCreationParameter(dk);
    if( xocp.getInputPayload() instanceof xprc.xpce.XMLInputPayload ) {
      rxocp.setInputPayload( ((xprc.xpce.XMLInputPayload)xocp.getInputPayload()).getInputPayload() );
    } else if( xocp.getInputPayload() instanceof xprc.xpce.AnyInputPayload ) {
      Container c = new Container();
      for( GeneralXynaObject o : ((xprc.xpce.AnyInputPayload)xocp.getInputPayload()).getInputPayload() ) {
        c.add(o);
      }
      rxocp.setInputPayload(c);
    }
    
    rxocp.setCustom0(xocp.getCustom0());
    rxocp.setCustom1(xocp.getCustom1());
    rxocp.setCustom2(xocp.getCustom2());
    rxocp.setCustom3(xocp.getCustom3());
    
    rxocp.setWorkflowExecutionTimeoutConfiguration( createExecutionTimeoutConfiguration(xocp.getExecutionTimeout()) );
    rxocp.setTimeConstraint(createTimeConstraint(xocp.getTimeConstraint()));
    return rxocp;
  }

  
  
  private ExecutionTimeoutConfiguration createExecutionTimeoutConfiguration(Date date) {
    if( date == null ) {
      return null;
    }
    AbsRelTime art = date.toAbsRelTime();
    if( art.isAbsolute() ) {
      return ExecutionTimeoutConfiguration.generateAbsoluteExecutionTimeout(art.getTime(), TimeUnit.MILLISECONDS);
    } else {
      return ExecutionTimeoutConfiguration.generateRelativeExecutionTimeout(art.getTime(), TimeUnit.MILLISECONDS);
    }
  }

  private DestinationKey createDestinationKey(xprc.xpce.RuntimeContext runtimeContext, xprc.xpce.OrderType orderType) throws XFMG_NoSuchRevision {
    RuntimeContext rc = null;
    if( runtimeContext instanceof xprc.xpce.OwnContext ) {
      rc = getOwnRuntimeContext();
    } else if ( runtimeContext instanceof xprc.xpce.Application ) {
      xprc.xpce.Application app = (xprc.xpce.Application)runtimeContext;
      rc = new Application(app.getName(), app.getVersion());
    } else if ( runtimeContext instanceof xprc.xpce.Workspace ) {
      xprc.xpce.Workspace wsp = (xprc.xpce.Workspace)runtimeContext;
      rc = new Workspace(wsp.getName());
    } else {
      //Fallback: eigener Context
      rc = getOwnRuntimeContext();
    }
    return new DestinationKey(orderType.getOrderType(), rc);
  }

  private RuntimeContext getOwnRuntimeContext() throws XFMG_NoSuchRevision {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    ChildOrderStorageStack coss = ChildOrderStorage.childOrderStorageStack.get();
    Long revision = coss.getCorrelatedXynaOrder().getRevision();
    try {
      return rm.getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_NoSuchRevision(String.valueOf(revision), e);
    }
  }


  public CancelResult cancelBatchProcess(BatchProcessId id, CancelMode cancelMode) {
    try {
      ChildOrderStorageStack coss = ChildOrderStorage.childOrderStorageStack.get();
      boolean success = getBatchProcessManagement().cancelBatchProcess(id.getBatchProcessId(), transformCancelMode(cancelMode), coss.getCorrelatedXynaOrder().getRootOrder().getId());
      return new CancelResult(success);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Could not cancel Batch Process", e);
    }
  }


  private com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode transformCancelMode(CancelMode cancelMode) {
    if (cancelMode == null) {
      return com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode.DEFAULT;
    }
    if (cancelMode instanceof CancelKillRunningSlaves) {
      return com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode.KILL_SLAVES;
    } else if (cancelMode instanceof CancelWaitForRunningSlaves) {
      return com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode.WAIT;
    }
    throw new RuntimeException("unexpected cancelmode: " + cancelMode.getClass().getName());
  }


  public SlaveStatus getSlaveStatus(BatchProcessId id) throws NoBatchOrderException {
    BatchProcessInformation bpi;
    try {
      bpi = getBatchProcessManagement().getBatchProcessInformation(id.getBatchProcessId());
    } catch (XynaException e) {
      throw new NoBatchOrderException(e);
    }
    if (bpi == null) {
      throw new NoBatchOrderException();
    }
    BatchProcessRuntimeInformationStorable info = bpi.getRuntimeInformation();
    switch (info.getState()) {
      case CANCELED :
        return new SlaveSchedulingCancelled();
      case PAUSED :
        return new SlaveSchedulingPaused(info.getPauseCause());
      case RUNNING :
        OrderInstanceStatus ois = bpi.getArchive().getOrderStatus();
        switch (ois.getStatusGroup()) {
          case Accepted :
          case Planning :
            return new SlaveSchedulingNotStarted();
          case Running :
          case Cleanup :
          case Failed :
          case Succeeded :
            return new SlaveSchedulingFinished();
          case Scheduling :
            return new SlavesAreRunning();
          case Waiting :
            return new SlaveSchedulingIsWaiting(ois.getName());
          default :
            throw new RuntimeException("unexpected status group: " + ois.getStatusGroup());
        }
      case TIMEOUT :
        return new SlaveSchedulingHadTimeout();
      default :
        throw new RuntimeException("unexpected state: " + info.getState());
    }
  }


  @Override
  public xprc.xbatchmgmt.BatchProcessInformation getExtendedInformation(BatchProcessId id) {
    BatchProcessInformation bpi;
    try {
      bpi = getBatchProcessManagement().getBatchProcessInformation(id.getBatchProcessId());
    } catch (XynaException e) {
      throw new RuntimeException("Unknown Batchprocess: " + id.getBatchProcessId());
    }
    String label = bpi.getLabel();
    String application = bpi.getApplication();
    String version = bpi.getVersion();
    String workspace = bpi.getWorkspace();
    String component = bpi.getComponent();
    int cancelled = bpi.getArchive().getCanceled();
    int failed = bpi.getArchive().getFailed();
    int finished = bpi.getArchive().getFinished();
    int total = bpi.getArchive().getTotal();
    String orderTypeSlave = bpi.getArchive().getSlaveOrdertype();
    String orderTypeMaster = bpi.getArchive().getDestinationKey().getOrderType();
    List<String> customFields = bpi.getArchive().getCustomsAsList();
    xprc.xpce.RuntimeContext rc;
    if (workspace != null) {
      rc = new xprc.xpce.Workspace(workspace);
    } else if (application != null && version != null) {
      rc = new xprc.xpce.Application(application, version);
    } else {
      throw new RuntimeException("Unknown RuntimeContext: " + workspace + ", " + application + ", " + version);
    }
    BatchProcessRuntimeInformation runtimeInfo = new BatchProcessRuntimeInformation(total, finished, failed, cancelled);
    return new xprc.xbatchmgmt.BatchProcessInformation.Builder().label(label).runtimeContext(rc).component(component)
        .batchProcessRuntimeInformation(runtimeInfo).orderTypeMaster(orderTypeMaster).orderTypeSlave(orderTypeSlave)
        .customFields(customFields).id(id.getBatchProcessId()).instance();
  }

}
