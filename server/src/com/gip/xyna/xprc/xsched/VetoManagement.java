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
package com.gip.xyna.xprc.xsched;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.statistics.XynaStatistics;
import com.gip.xyna.xfmg.statistics.XynaStatistics.StatisticsReportEntry;
import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyEnum;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_VetonameMustNotBeEmpty;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.gip.xyna.xprc.xsched.vetos.AdministrativeVeto;
import com.gip.xyna.xprc.xsched.vetos.VetoAllocationResult;
import com.gip.xyna.xprc.xsched.vetos.VetoInformation;
import com.gip.xyna.xprc.xsched.vetos.VetoManagementAlgorithmType;
import com.gip.xyna.xprc.xsched.vetos.VetoManagementAlgorithmType.ClusterMode;
import com.gip.xyna.xprc.xsched.vetos.VetoManagementInterface;
import com.gip.xyna.xprc.xsched.vetos.VetoStatistics;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoHistory;



public class VetoManagement extends FunctionGroup implements VetoManagementInterface, Clustered {

  private static Logger logger = CentralFactoryLogging.getLogger(VetoManagement.class);
  public static final String DEFAULT_NAME = "Veto Management";
  
  private VetoManagementInterface vmAlgorithm;
  private ClusterContext rmiClusterContext;
  private RMIClusterStateChangeHandler rmiClusterStateChangeHandler;
  private final AtomicBoolean initializing;
  
  static {
    ArrayList<XynaFactoryPath> dependencies = new ArrayList<XynaFactoryPath>();
    // wait for the configuration class to be loaded to be able to read properties for the default configuration
    dependencies.add(new XynaFactoryPath(XynaProcessing.class, XynaScheduler.class));
    addDependencies(VetoManagement.class, dependencies);
    addDependencies(VetoManagement.class,
                    new ArrayList<XynaFactoryPath>(Arrays
                        .asList(new XynaFactoryPath[] {new XynaFactoryPath(XynaFactoryManagement.class,
                                                                           XynaFactoryControl.class,
                                                                           DependencyRegister.class)})));
  }

  public static XynaPropertyEnum<VetoManagementAlgorithmType> VM_ALGORITHM_TYPE = 
      new XynaPropertyEnum<VetoManagementAlgorithmType>("xprc.veto.algorithm", VetoManagementAlgorithmType.class, 
          VetoManagementAlgorithmType.SeparateThread  )
      .setDefaultDocumentation(DocumentationLanguage.EN, "VetoManagementAlgorithm: "+ VetoManagementAlgorithmType.documentation(DocumentationLanguage.EN))
      .setDefaultDocumentation(DocumentationLanguage.DE, "VetoManagementAlgorithm: "+ VetoManagementAlgorithmType.documentation(DocumentationLanguage.DE));
  

  private class RMIClusterStateChangeHandler implements ClusterStateChangeHandler {

    private ClusterState clusterState;
    private VetoManagementAlgorithmType vetoManagementAlgorithmType;
    
    public boolean isReadyForChange(ClusterState newState) {
      return true; //immer bereit
    }

    public synchronized void onChange(ClusterState newState) {
      logger.info("VetoManagement.RMIClusterStateChangeHandler.onChange " + newState );
      //VetoManagement ist nur �ber RMI geclustert.
      this.clusterState = newState;
      if( vetoManagementAlgorithmType != null && vetoManagementAlgorithmType.isClusterable() ) {
        switchAlgorithmClusteredLocal();
      }
    }
    
    public synchronized void setVetoManagementAlgorithmType(VetoManagementAlgorithmType vmat) {
      logger.info("VetoManagement.RMIClusterStateChangeHandler.setVetoManagementAlgorithmType " +vmat );
      this.vetoManagementAlgorithmType = vmat;
      if( clusterState != null && vetoManagementAlgorithmType.isClusterable() ) {
        switchAlgorithmClusteredLocal();
      }
    }
    
    private void switchAlgorithmClusteredLocal() {
      logger.info("VetoManagement.RMIClusterStateChangeHandler.switchAlgorithmClusteredLocal "+ clusterState );
      
      RMIClusterProvider rmi = (RMIClusterProvider)rmiClusterContext.getClusterInstance();

      ClusterMode cm = null;
      switch( clusterState ) {
      case CONNECTED:
        cm = ClusterMode.Clustered;
        break;
      case DISCONNECTED_MASTER:
      case NO_CLUSTER:
      case SINGLE:
      case SYNC_MASTER:
        cm = ClusterMode.Local;
        break;
      case DISCONNECTED:
      case NEVER_CONNECTED:
        //TODO Sinnvollerweise konfigurierbar Local oder Unsupported, da hier beide 
        //Clusterknoten laufen k�nnen und damit Vetos doppelt vergeben k�nnten
        cm = ClusterMode.Local; //ClusterMode.Unsupported;
        break;
      default: //INIT, SHUTDOWN, STARTING, SYNC_PARTNER, SYNC_SLAVE
        cm = ClusterMode.Unsupported;
        break;
      }
      logger.info("VetoManagement.RMIClusterStateChangeHandler.switchAlgorithmClusteredLocal "+ cm );
      
      vetoManagementAlgorithmType.switchClusteredLocal(vmAlgorithm, rmi, cm);
    }
    
  }
  
  
  
  public VetoManagement() throws XynaException {
    super();
    initializing = new AtomicBoolean(false);
  }
  
  public VetoManagementInterface getVMAlgorithm() {
    return vmAlgorithm;
  }

  @Override
  public void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(VetoManagement.class, "VetoManagement.initClusterContext").
          before(XynaClusteringServicesManagement.class).
          execAsync( new Runnable() {public void run() { initClusterContext(); }});
    fExec.addTask(VetoManagement.class, "VetoManagement").
      after(XynaClusteringServicesManagement.class, XynaProperty.class, PersistenceLayerInstances.class).
      before(WorkflowDatabase.FUTURE_EXECUTION_ID).
      execAsync( new Runnable() {public void run() { initVetoManagement(); }});
  }
  
  
  private void initClusterContext() {
    rmiClusterStateChangeHandler = new RMIClusterStateChangeHandler();
    rmiClusterContext = new ClusterContext();
    rmiClusterContext.addClusterStateChangeHandler( rmiClusterStateChangeHandler );
    try {
      XynaClusteringServicesManagement.getInstance().registerClusterableComponent(this);
    } catch (XFMG_ClusterComponentConfigurationException e) {
      logger.warn("Failed to register " + VetoManagement.class.getName() + " as clusterable component.");
    }
  }
  
  private void initVetoManagement() {
    logger.debug("initVetoManagement");
    if (initializing.compareAndSet(false, true)) {
      VM_ALGORITHM_TYPE.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
      VetoHistory.HISTORY_SIZE.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
      try {
        VetoManagementAlgorithmType vmat = VM_ALGORITHM_TYPE.get();
        vmAlgorithm = vmat.instantiate( isClustered() ? ClusterMode.Unsupported : ClusterMode.Local);
        rmiClusterStateChangeHandler.setVetoManagementAlgorithmType( vmat );
      } catch (XynaException e) {
        throw new RuntimeException(e);
      }
      registerStatistics();
    }
  }
 
  
  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void shutdown() throws XynaException {
    XynaFactoryManagementBase fm = XynaFactory.getInstance().getFactoryManagement();
    if (fm != null) {
      XynaStatistics xs = fm.getXynaStatistics();
      if (xs != null) {
        xs.unregisterStatistics( "XPRC.XSched.CC.Vetos" );
      }
    }
  }

  
  public Collection<VetoInformationStorable> listVetosAsStorables() {
    return CollectionUtils.transform(vmAlgorithm.listVetos(), VetoInformationStorable.fromVetoInformation );
  }

  public void registerStatistics() {
  }

  public StatisticsReportEntry[] getStatisticsReport() {
    return VetoStatistics.createReport(listVetos());
  }

  public boolean freeVetos(final XynaOrderServerExtension xo) {
    return vmAlgorithm.freeVetos(new OrderInformation(xo));
  }
  
  public boolean forceFreeVetos(long orderId) {
    return vmAlgorithm.freeVetosForced(orderId);
  }

  /////////////////////////////////////////////////////////////
  //
  // Implementierung des Interface Clustered
  //
  /////////////////////////////////////////////////////////////
  
  public boolean isClustered() {
    return rmiClusterContext.isClustered();
  }

  public long getClusterInstanceId() {
    return rmiClusterContext.getClusterInstanceId();
  }

  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
      XFMG_ClusterComponentConfigurationException {
    rmiClusterContext.enableClustering(clusterInstanceId);
    initVetoManagement();
  }
  
  public String getName() {
    return getDefaultName();
  }

  public void disableClustering() {
    rmiClusterContext.disableClustering();
  }


  
  /////////////////////////////////////////////////////////////
  //
  // Implementierung des Interface VetoManagementInterface
  //
  /////////////////////////////////////////////////////////////
  
  public VetoAllocationResult allocateVetos(OrderInformation orderInformation, List<String> vetos, long urgency) {
    
    if (vetos.isEmpty() ) {
      return VetoAllocationResult.SUCCESS;
    }
    
    for( String v : vetos ) {
      if( v == null || v.length() == 0 ) {
        return new VetoAllocationResult(new XPRC_VetonameMustNotBeEmpty());
      }
    }
    
    return vmAlgorithm.allocateVetos(orderInformation, vetos, urgency);
  }

  public void undoAllocation(OrderInformation orderInformation, List<String> vetos) {
    vmAlgorithm.undoAllocation(orderInformation,vetos);
  }
  
  public void finalizeAllocation(OrderInformation orderInformation, List<String> vetos) {
    vmAlgorithm.finalizeAllocation(orderInformation, vetos);
  }
  
  public boolean freeVetos(OrderInformation orderInformation) {
    return vmAlgorithm.freeVetos(orderInformation);
  }
  
  public boolean freeVetosForced(long orderId) {
    return vmAlgorithm.freeVetosForced(orderId);
  }
 
  public Collection<VetoInformation> listVetos() {
    return vmAlgorithm.listVetos();
  }
  
  public void allocateAdministrativeVeto(AdministrativeVeto administrativeVeto) throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {
    vmAlgorithm.allocateAdministrativeVeto(administrativeVeto);
  }
  
  public synchronized VetoInformation freeAdministrativeVeto(AdministrativeVeto administrativeVeto) throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException {
    VetoInformation vi = vmAlgorithm.freeAdministrativeVeto(administrativeVeto);
    
    XynaScheduler scheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler();
    scheduler.notifyScheduler();
    
    if (scheduler instanceof ClusteredScheduler) {
      ((ClusteredScheduler) scheduler).notifyRemoteScheduler();
    }
    
    return vi;
  }
  
  public synchronized String setDocumentationOfAdministrativeVeto(AdministrativeVeto administrativeVeto) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return vmAlgorithm.setDocumentationOfAdministrativeVeto(administrativeVeto);
  }
  
  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
    return vmAlgorithm.searchVetos(select, maxRows);
  }

  @Override
  public VetoManagementAlgorithmType getAlgorithmType() {
    return vmAlgorithm.getAlgorithmType();
  }

  @Override
  public String showInformation() {
    return vmAlgorithm.showInformation();
  }


}
