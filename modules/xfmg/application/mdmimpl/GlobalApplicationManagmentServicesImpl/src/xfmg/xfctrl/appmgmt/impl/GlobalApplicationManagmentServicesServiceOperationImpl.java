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
package xfmg.xfctrl.appmgmt.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeRemoteException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ApplicationPartImportMode;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ExportApplicationBuildParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoader;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.CredentialsCache;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InfrastructureLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.NodeManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt.RemoteFileManagementLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt.RemoteFileManagementUtils;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.ListRuntimeDependencyContextParameter;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.LocalRuntimeContextManagementSecurity;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.RuntimeContextManagementLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.RuntimeDependencyContextInformation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext.RuntimeDependencyContextType;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceState;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xmcp.PluginInformation;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.securestorage.SecureStorage;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_ResumeFailedException;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_Manual;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext.ActiveOrderType;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext.MigrationResult;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendRevisionsBean;

import base.DetailedDescription;
import base.KeyValue;
import xfmg.xfctrl.FactoryNode;
import xfmg.xfctrl.RemoteOperationResult;
import xfmg.xfctrl.appmgmt.ApplicationDefinitionDetails;
import xfmg.xfctrl.appmgmt.ApplicationDetails;
import xfmg.xfctrl.appmgmt.ApplicationState;
import xfmg.xfctrl.appmgmt.AuditMode;
import xfmg.xfctrl.appmgmt.CLI;
import xfmg.xfctrl.appmgmt.Error;
import xfmg.xfctrl.appmgmt.GlobalApplicationManagmentServicesServiceOperation;
import xfmg.xfctrl.appmgmt.ImportCapacitiesOnly;
import xfmg.xfctrl.appmgmt.ImportSettings;
import xfmg.xfctrl.appmgmt.ImportXynaPropertiesOnly;
import xfmg.xfctrl.appmgmt.IncludeCapacities;
import xfmg.xfctrl.appmgmt.IncludeXynaProperties;
import xfmg.xfctrl.appmgmt.ListApplicationParameter;
import xfmg.xfctrl.appmgmt.ListRuntimeDependencyContextsParameter;
import xfmg.xfctrl.appmgmt.MigrateRuntimeContextDependencyParameter;
import xfmg.xfctrl.appmgmt.OrderEntranceType;
import xfmg.xfctrl.appmgmt.RMI;
import xfmg.xfctrl.appmgmt.RemoveApplicationParameter;
import xfmg.xfctrl.appmgmt.RepositoryAccess;
import xfmg.xfctrl.appmgmt.Running;
import xfmg.xfctrl.appmgmt.RuntimeDependencyContextDetails;
import xfmg.xfctrl.appmgmt.StartApplicationParameter;
import xfmg.xfctrl.appmgmt.StopApplicationParameter;
import xfmg.xfctrl.appmgmt.Stopped;
import xfmg.xfctrl.appmgmt.TriggerInstance;
import xfmg.xfctrl.appmgmt.Warning;
import xfmg.xfctrl.appmgmt.Workingcopy;
import xfmg.xfctrl.appmgmt.WorkspaceDetails;
import xfmg.xfctrl.filemgmt.ManagedFileId;
import xfmg.xfctrl.nodemgmt.ConnectException;
import xfmg.xfctrl.nodemgmt.RemoteException;
import xprc.xpce.Application;
import xprc.xpce.ApplicationDefinition;
import xprc.xpce.RuntimeContext;
import xprc.xpce.Workspace;


public class GlobalApplicationManagmentServicesServiceOperationImpl implements ExtendedDeploymentTask, GlobalApplicationManagmentServicesServiceOperation {

  private static CredentialsCache cache;
  private static LocalRuntimeContextManagementSecurity localLrcms;
  private static Logger logger = CentralFactoryLogging.getLogger(GlobalApplicationManagmentServicesServiceOperationImpl.class);
  
  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    cache = CredentialsCache.getInstance();
    localLrcms = new LocalRuntimeContextManagementSecurity();
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

  /**
   * Exportiert lokal eine Application.
   */
  public ManagedFileId exportApplication(XynaOrderServerExtension correlatedOrder, FactoryNode factoryNode, Application application) throws RemoteException {
    try {
      ExportApplicationBuildParameter eabp = ExportApplicationBuildParameter.local();
      String localFileId;
      if (factoryNode.getLocal()) {
        localFileId = localLrcms.exportApplication(correlatedOrder.getCreationRole(), convertApplication(application), eabp);
      } else {
        RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(factoryNode.getName());
        String remoteFileId = remoteAccess.exportApplication(getCredentials(factoryNode), convertApplication(application), eabp);
        RemoteFileManagementLinkProfile remoteFileMgmt = getRemoteFileMgmt(factoryNode.getName());
        localFileId = RemoteFileManagementUtils.download(factoryNode.getName(), remoteFileId, remoteFileMgmt, getCredentials(factoryNode));
      }
      return new ManagedFileId(localFileId);
    } catch (Exception e) {
      throw new RemoteException(factoryNode.getName(), "exportApplication", e);
    }
  }

  
  /**
   * Importiert eine Application. 
   */
  public void importApplication(XynaOrderServerExtension correlatedOrder, FactoryNode factoryNode, ManagedFileId fileId, ImportSettings importSettings) throws RemoteException, ConnectException {
    try {
      ImportApplicationParameter iap = convertImportSettings(importSettings);
      if (factoryNode.getLocal()) {
        localLrcms.importApplication(correlatedOrder.getCreationRole(), iap, fileId.getId());
      } else {
        RemoteFileManagementLinkProfile remoteFileMgmt = getRemoteFileMgmt(factoryNode.getName());
        String remoteFileId = RemoteFileManagementUtils.upload(factoryNode.getName(), fileId.getId(), remoteFileMgmt, getCredentials(factoryNode));
        RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(factoryNode.getName());
        remoteAccess.importApplication(getCredentials(factoryNode), iap, remoteFileId);
      }
    } catch (XynaException e) {
      handleXynaException(factoryNode.getName(), "importApplication", e);
    } catch (Exception e) {
      throw new RemoteException(factoryNode.getName(), "importApplication", e);
    } finally {
      // localFileMgmt.remove(fileId.getId()); ?
    }
  }

  /**
   * Mappt die ImportSettings auf ImportApplicationParameters
   * @param importSettings
   * @return
   */
  private ImportApplicationParameter convertImportSettings(ImportSettings importSettings) {
    ApplicationPartImportMode properties = ApplicationPartImportMode.EXCLUDE;
    if (importSettings.getXynaPropertyImportSettings() instanceof IncludeXynaProperties) {
      properties = ApplicationPartImportMode.INCLUDE;
    } else if (importSettings.getXynaPropertyImportSettings() instanceof ImportXynaPropertiesOnly) {
      properties = ApplicationPartImportMode.ONLY;
    }
    ApplicationPartImportMode capacities = ApplicationPartImportMode.EXCLUDE;
    if (importSettings.getCapacityImportSettings() instanceof IncludeCapacities) {
      capacities = ApplicationPartImportMode.INCLUDE;
    } else if (importSettings.getCapacityImportSettings() instanceof ImportCapacitiesOnly) {
      capacities = ApplicationPartImportMode.ONLY;
    }
    ImportApplicationParameter params = ImportApplicationParameter.with(properties,
                                                                        capacities,
                                                                        importSettings.getForceOverride(),
                                                                        true,
                                                                        importSettings.getUser());
    return params;
  }

  /**
   * Liefert eine Liste mit allen Applications auf dem Knoten. 
   */
  public List<? extends ApplicationDetails> listApplications(XynaOrderServerExtension correlatedOrder, FactoryNode factoryNode,
                                                             ListApplicationParameter parameter) throws ConnectException, RemoteException {
    List<ApplicationDetails> ret = new ArrayList<ApplicationDetails>();
    try {
      List<RuntimeDependencyContextInformation> result = 
        listRuntimeDependencyContexts(correlatedOrder.getCreationRole(), factoryNode, new ListRuntimeDependencyContextParameter(false, Collections.singleton(RuntimeDependencyContextType.Application)));
      
      
      for (RuntimeDependencyContextInformation rdci : result) {
        if (rdci instanceof ApplicationInformation) {
          ApplicationInformation appInfo = (ApplicationInformation) rdci;
          if (!parameter.getSelectProblems() && parameter.getReevaluateState()) {
            appInfo.setProblems(Collections.<RuntimeContextProblem>emptyList());
          }
          if (appInfo instanceof ApplicationDefinitionInformation) {
            continue; //Application Definitions rausfiltern
          }
          
          ApplicationDetails detail = new ApplicationDetails(appInfo.getName(),
                                                             convertState(appInfo.getState()),
                                                             convertRuntimeDependencyContexts(appInfo.getRequirements()),
                                                             convertProblems(appInfo.getProblems()),
                                                             appInfo.getVersion(),
                                                             appInfo.getComment(),
                                                             appInfo.getRemoteStub(),
                                                             convertOrderEntrances(appInfo.getOrderEntrances()));
          ret.add(detail);
        }
      }
    } catch (XynaException e) {
      handleXynaException(factoryNode.getName(), "listApplications", e);
    }
    return ret;
  }
  
  private List<RuntimeDependencyContextInformation> listRuntimeDependencyContexts(Role role, FactoryNode factoryNode, ListRuntimeDependencyContextParameter lrdcp) throws XynaException {
    List<RuntimeDependencyContextInformation> result;
    if (factoryNode.getLocal()) {
      result = localLrcms.listRuntimeDependencyContexts(role, lrdcp);
    } else {
      RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(factoryNode.getName());
      result = remoteAccess.listRuntimeDependencyContexts(getCredentials(factoryNode), lrdcp);
    }
    return result;
  }


  private List<OrderEntranceType> convertOrderEntrances(Map<OrderEntrance, SerializablePair<Boolean, String>> orderEntrances) {
    List<OrderEntranceType> conversion = new ArrayList<OrderEntranceType>();
    for (Entry<OrderEntrance, SerializablePair<Boolean, String>> entry : orderEntrances.entrySet()) {
      switch (entry.getKey().getType()) {
        case CLI :
          if (entry.getValue().getFirst()) {
            conversion.add(new CLI()); // FIXME add enabled flag to this modelled OrderEntranceTypes
          }
          break;
        case RMI :
          if (entry.getValue().getFirst()) {
            conversion.add(new RMI()); // FIXME add enabled flag to this modelled OrderEntranceTypes
          }
          break;
        case triggerInstance :
          conversion.add(new TriggerInstance(entry.getKey().getName(), entry.getValue().getFirst(), entry.getValue().getSecond()));
          break;
        default :
          // skip everything else
          break;
      }
    }
    return conversion;
  }


  private List<DetailedDescription> convertProblems(Collection<RuntimeContextProblem> problems) {
    List<DetailedDescription> l = new ArrayList<DetailedDescription>();
    if (problems != null) {
      for (RuntimeContextProblem p : problems) {
        List<KeyValue> keyValues = new ArrayList<KeyValue>();
        for (Pair<String, String> detail : p.getDetails()) {
          keyValues.add(new KeyValue(detail.getFirst(), detail.getSecond()));
        }
        l.add(new DetailedDescription(p.getId().name(), p.getId().getDescription(), keyValues));
      }
    }
    return l;
  }
  
  
  private RepositoryAccess convertRepositoryAccess(PluginInformation repositoryAccess) {
    RepositoryAccess ra = new RepositoryAccess();
    ra.unversionedSetName(repositoryAccess.getName());
    List<KeyValue> params = new ArrayList<KeyValue>();
    if (repositoryAccess.getParamMap() != null) {
      for (Entry<String, Object> param : repositoryAccess.getParamMap().entrySet()) {
        params.add(new KeyValue(param.getKey(), param.getValue() == null? null : param.getValue().toString()));
      }
    }
    ra.unversionedSetParameter(params);
    return ra;
  }

  /**
   * Mappt den ApplicationState
   * @param state
   * @return
   */
  private ApplicationState convertState(com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState state) {
    switch(state) {
      case RUNNING: 
        return new Running();
      case STOPPED:
        return new Stopped();
      case WORKINGCOPY:
        return new Workingcopy();
      case AUDIT_MODE:
        return new AuditMode();
      case WARNING:
        return new Warning();
      case ERROR:
        return new Error();
      default:
        return new ApplicationState();
    }
  }
  
  
  private ApplicationState convertState(WorkspaceState state) {
    switch(state) {
      case OK: 
        return new Running();
      case WARNING:
        return new Warning();
      case ERROR:
        return new Error();
      default:
        return new ApplicationState();
    }
  }
  
  
  /**
   * Entfernt die übergebene Application auf dem Knoten.
   */
  public void removeApplication(XynaOrderServerExtension correlatedOrder, FactoryNode factoryNode, Application application, RemoveApplicationParameter removeApplicationParameter) throws ConnectException, RemoteException {
    try{
      if (factoryNode.getLocal()) {
        localLrcms.deleteRuntimeDependencyContext(correlatedOrder.getCreationRole(), convertApplication(application));
      } else {
        //Remote-Aufruf
        RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(factoryNode.getName());
        remoteAccess.deleteRuntimeDependencyContext(getCredentials(factoryNode), convertApplication(application));
      }
    } catch (XynaException e) {
      handleXynaException(factoryNode.getName(), "removeApplication", e);
    }
  }

  /**
   * Startet die übergebene Application auf dem Knoten.
   */
  public void startApplication(XynaOrderServerExtension correlatedOrder, FactoryNode factoryNode, Application application, StartApplicationParameter startApplicationParameter) throws ConnectException, RemoteException {
    try{
      ApplicationInformation appInfo = getCurrentApplication(correlatedOrder.getCreationRole(), factoryNode, convertApplication(application));
      appInfo = adjustApplicationForStart(appInfo, startApplicationParameter);
      if (factoryNode.getLocal()) {
        localLrcms.modifyRuntimeDependencyContext(correlatedOrder.getCreationRole(), appInfo);
      } else {
        RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(factoryNode.getName());
        remoteAccess.modifyRuntimeDependencyContext(getCredentials(factoryNode), appInfo);
      }
    } catch (XynaException e) {
      handleXynaException(factoryNode.getName(), "startApplication", e);
    }
  }

  private ApplicationInformation getCurrentApplication(Role role, FactoryNode factoryNode, com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application application) throws XynaException {
    
    List<RuntimeDependencyContextInformation> rdcis = 
      listRuntimeDependencyContexts(role, factoryNode, new ListRuntimeDependencyContextParameter(false, Collections.singleton(RuntimeDependencyContextType.Application)));
    for (RuntimeDependencyContextInformation rdci : rdcis) {
      if (((ApplicationInformation)rdci).getName().equals(application.getName()) &&
          ((ApplicationInformation)rdci).getVersion().equals(application.getVersionName())) {
        return (ApplicationInformation) rdci;
      }
    }
    return null;
  }
  
  private ApplicationInformation adjustApplicationForStart(ApplicationInformation appInfo,
                                                           StartApplicationParameter startApplicationParameter) {

    if (startApplicationParameter.getOnlyEnableOrderEntrance() == null ||
        startApplicationParameter.getOnlyEnableOrderEntrance().size() <= 0) {
      // enable all
      for (SerializablePair<Boolean, String> entry : appInfo.getOrderEntrances().values()) {
        entry.setFirst(Boolean.TRUE);
      }
    } else {
      for (OrderEntranceType oet : startApplicationParameter.getOnlyEnableOrderEntrance()) {
        // TODO triggerInstances have an enabled field...should we have a look at it? Or is their containment in the list sufficient?
        OrderEntrance oe = convertOrderEntranceType(oet);
        appInfo.getOrderEntrances().get(oe).setFirst(Boolean.TRUE);
        // TODO should we enable every filterInstance?
      }
    }
    return appInfo;
  }
  

  /**
   * Stoppt die übergebene Application auf dem Knoten.
   */
  public void stopApplication(XynaOrderServerExtension correlatedOrder, FactoryNode factoryNode, Application application,
                              StopApplicationParameter stopApplicationParameter) throws ConnectException, RemoteException {
    try{
      ApplicationInformation appInfo = getCurrentApplication(correlatedOrder.getCreationRole(), factoryNode, convertApplication(application));
      appInfo = adjustApplicationForStop(appInfo, stopApplicationParameter);
      if (factoryNode.getLocal()) {
        localLrcms.modifyRuntimeDependencyContext(correlatedOrder.getCreationRole(), appInfo);
      } else {
        RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(factoryNode.getName());
        remoteAccess.modifyRuntimeDependencyContext(getCredentials(factoryNode), appInfo);
      }
    } catch (XynaException e) {
      handleXynaException(factoryNode.getName(), "stopApplication", e);
    }
  }
  
  
  private ApplicationInformation adjustApplicationForStop(ApplicationInformation appInfo,
                                                          StopApplicationParameter stopApplicationParameter) {
    if (stopApplicationParameter.getOnlyDisableOrderEntrance() == null ||
        stopApplicationParameter.getOnlyDisableOrderEntrance().size() <= 0) {
      for (SerializablePair<Boolean, String> entry : appInfo.getOrderEntrances().values()) {
        entry.setFirst(Boolean.FALSE);
      }
    } else {
      for (OrderEntranceType oet : stopApplicationParameter.getOnlyDisableOrderEntrance()) {
        // TODO triggerInstances have an enabled field...should we have a look at it? Or is their containment in the list sufficient?
        OrderEntrance oe = convertOrderEntranceType(oet);
        appInfo.getOrderEntrances().get(oe).setFirst(Boolean.FALSE);
      }
    }
    return appInfo;
  }

  private static NodeManagement getNodeManagement() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement();
  }
  
  
  private static void handleXynaException(String nodeName, String operationName, XynaException e) throws ConnectException, RemoteException{
    if (e instanceof ConnectException) {
      throw (ConnectException)e;
    } else if (e instanceof RemoteException) {
      throw (RemoteException)e;
    } else if (e instanceof XFMG_NodeConnectException) {
      XFMG_NodeConnectException nce = (XFMG_NodeConnectException)e;
      throw new ConnectException(nce.getNodeName(), nce);
    } else if (e instanceof XFMG_NodeRemoteException) {
      XFMG_NodeRemoteException nre = (XFMG_NodeRemoteException)e;
      throw new RemoteException(nre.getNodeName(), nre.getOperationName(), nre);
    } else {
      throw new RemoteException(nodeName, operationName, e);
    }
  }
  
  
  private RuntimeContextManagementLinkProfile getRemoteRtCtxMgmt(String nodeName) throws ConnectException {
    com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNode fn = getNodeManagement().getNodeByName(nodeName);
    if (fn == null) {
      throw new ConnectException(nodeName, new Exception("node does not exists"));
    }
    
    RuntimeContextManagementLinkProfile remoteAccess = fn.getInterFactoryLink().getProfile(InterFactoryLink.InterFactoryLinkProfileIdentifier.RuntimeContextManagement);
    if (remoteAccess == null) {
      throw new ConnectException(nodeName, new Exception("remoteAccess does not exists"));
    }
    
    return remoteAccess;
  }
  
  
  private FileManagement getLocalFileMgmt() throws ConnectException {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
  }
  
  
  private RemoteFileManagementLinkProfile getRemoteFileMgmt(String nodeName) throws ConnectException {
    com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNode fn = getNodeManagement().getNodeByName(nodeName);
    if (fn == null) {
      throw new ConnectException(nodeName, new Exception("node does not exists"));
    }
    
    RemoteFileManagementLinkProfile remoteAccess = fn.getInterFactoryLink().getProfile(InterFactoryLink.InterFactoryLinkProfileIdentifier.FileManagement);
    if (remoteAccess == null) {
      throw new ConnectException(nodeName, new Exception("remoteAccess does not exists"));
    }
    
    return remoteAccess;
  }
  
  
  private InfrastructureLinkProfile getInfrastructure(String nodeName) throws ConnectException {
    com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNode fn = getNodeManagement().getNodeByName(nodeName);
    if (fn == null) {
      throw new ConnectException(nodeName, new Exception("node does not exists"));
    }
    
    InfrastructureLinkProfile remoteAccess = fn.getInterFactoryLink().getProfile(InterFactoryLink.InterFactoryLinkProfileIdentifier.Infrastructure);
    if (remoteAccess == null) {
      throw new ConnectException(nodeName, new Exception("remoteAccess does not exists"));
    }
    
    return remoteAccess;
  }
  
  
  private com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application convertApplication(Application app) {
    return new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application(app.getName(), app.getVersion());
  }
  
  private OrderEntrance convertOrderEntranceType(OrderEntranceType oet) {
    if (oet instanceof RMI) {
      return new OrderEntrance(com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.RMI, 
                               com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.RMI.toString());
    } else if (oet instanceof CLI) {
      return new OrderEntrance(com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.CLI, 
                               com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.CLI.toString());
    } else if (oet instanceof TriggerInstance) {
      return new OrderEntrance(com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.triggerInstance, 
                               ((TriggerInstance) oet).getName());
    } else {
      return null;
    }
  }
  
  private XynaCredentials getCredentials(FactoryNode node) throws ConnectException, XFMG_NodeConnectException {
    return checkConnectivityAndAccess(node);
  }

  
  private XynaCredentials checkConnectivityAndAccess(FactoryNode node) throws XFMG_NodeConnectException, ConnectException  {
    XynaCredentials credentials = cache.getCredentials(node.getName(), getInfrastructure(node.getName()));
    InfrastructureLinkProfile ilp = getInfrastructure(node.getName());
    try {
      ilp.getExtendedStatus(credentials);
    } catch (XFMG_NodeConnectException e) {
      cache.clearSession(node.getName());
      credentials = cache.getCredentials(node.getName(), getInfrastructure(node.getName()));
      ilp.getExtendedStatus(credentials);
    }
    return credentials;
  }

  public void copyApplicationIntoWorkspace(XynaOrderServerExtension correlatedOrder, FactoryNode factoryNode, Application application, Workspace workspace) throws RemoteException, ConnectException {
    try{
      if (factoryNode.getLocal()) {
        localLrcms.copyApplicationIntoWorkspace(correlatedOrder.getCreationRole(), convertApplication(application), convertWorkspace(workspace));
      } else {
        RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(factoryNode.getName());
        remoteAccess.copyApplicationIntoWorkspace(getCredentials(factoryNode), convertApplication(application), convertWorkspace(workspace));
      }
    } catch (XynaException e) {
      handleXynaException(factoryNode.getName(), "copyApplicationIntoWorkspace", e);
    }
  }

  private com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace convertWorkspace(Workspace workspace) {
    return new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace(workspace.getName());
  }
  
  private Workspace convertWorkspace(com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace workspace) {
    return new Workspace(workspace.getName());
  }

  public void createRuntimeDependencyContext(XynaOrderServerExtension correlatedOrder, FactoryNode factoryNode, RuntimeContext rtCtx) throws RemoteException, ConnectException {
    try{
      if (factoryNode.getLocal()) {
        localLrcms.createRuntimeDependencyContext(correlatedOrder.getCreationRole(), convertRuntimeContext(rtCtx));
      } else {
        RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(factoryNode.getName());
        remoteAccess.createRuntimeDependencyContext(getCredentials(factoryNode), convertRuntimeContext(rtCtx));
      }
    } catch (XynaException e) {
      handleXynaException(factoryNode.getName(), "createRuntimeDependencyContext", e);
    }
  }


  public void deleteRuntimeDependencyContext(XynaOrderServerExtension correlatedOrder, FactoryNode factoryNode, RuntimeContext rtCtx) throws RemoteException, ConnectException {
    try{
      if (factoryNode.getLocal()) {
        localLrcms.deleteRuntimeDependencyContext(correlatedOrder.getCreationRole(), convertRuntimeContext(rtCtx));
      } else {
        RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(factoryNode.getName());
        remoteAccess.deleteRuntimeDependencyContext(getCredentials(factoryNode), convertRuntimeContext(rtCtx));
      }
    } catch (XynaException e) {
      handleXynaException(factoryNode.getName(), "deleteRuntimeDependencyContext", e);
    }
  }

  public List<? extends RuntimeDependencyContextDetails> listRuntimeDependencyContexts(XynaOrderServerExtension correlatedOrder, FactoryNode factoryNode, ListRuntimeDependencyContextsParameter lrdcp)
                  throws ConnectException, RemoteException {
    try{
      List<RuntimeDependencyContextInformation> infos;
      if (factoryNode.getLocal()) {
        infos = localLrcms.listRuntimeDependencyContexts(correlatedOrder.getCreationRole(), convertListRuntimeDependencyContextParameter(lrdcp));
      } else {
        RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(factoryNode.getName());
        infos = remoteAccess.listRuntimeDependencyContexts(getCredentials(factoryNode), convertListRuntimeDependencyContextParameter(lrdcp));
      }
      return convertRuntimeDependencyContextInformations(infos);
    } catch (XynaException e) {
      handleXynaException(factoryNode.getName(), "deleteRuntimeDependencyContext", e);
      return null;
    }
  }

  private com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.ListRuntimeDependencyContextParameter convertListRuntimeDependencyContextParameter(ListRuntimeDependencyContextsParameter lrdcp) {
    Set<RuntimeDependencyContextType> selectedTypes = new HashSet<RuntimeDependencyContextType>();
    if (lrdcp.getSelectRuntimeContext() == null ||
        lrdcp.getSelectRuntimeContext().size() <= 0) {
      selectedTypes.add(RuntimeDependencyContextType.Application);
      selectedTypes.add(RuntimeDependencyContextType.ApplicationDefinition);
      selectedTypes.add(RuntimeDependencyContextType.Workspace);
    } else {
      for (RuntimeContext rc : lrdcp.getSelectRuntimeContext()) {
        if (rc instanceof Application) {
          selectedTypes.add(RuntimeDependencyContextType.Application);
        } else if (rc instanceof ApplicationDefinition) {
          selectedTypes.add(RuntimeDependencyContextType.ApplicationDefinition);
        } else if (rc instanceof Workspace) {
          selectedTypes.add(RuntimeDependencyContextType.Workspace);
        }
      }
    }
    ListRuntimeDependencyContextParameter conversion = new ListRuntimeDependencyContextParameter(lrdcp.getSelectProblems(), selectedTypes);
    return conversion;
  }

  private List<? extends RuntimeDependencyContextDetails> convertRuntimeDependencyContextInformations(List<RuntimeDependencyContextInformation> infos) {
    List<RuntimeDependencyContextDetails> details = new ArrayList<RuntimeDependencyContextDetails>();
    for (RuntimeDependencyContextInformation rdci : infos) {
      details.add(convertRuntimeDependencyContextInformation(rdci));
    }
    return details;
  }


  private RuntimeDependencyContextDetails convertRuntimeDependencyContextInformation(RuntimeDependencyContextInformation rdci) {
    switch (rdci.getRuntimeDependencyContextType()) {
      case Application :
        return convertApplication((ApplicationInformation)rdci);
      case ApplicationDefinition :
        return convertApplicationDefinition((ApplicationDefinitionInformation)rdci);
      case Workspace :
        return convertWorkspace((WorkspaceInformation)rdci);

      default :
        throw new IllegalArgumentException("Not a valid RuntimeDependencyContextType: " + rdci.getRuntimeDependencyContextType());
    }
  }


  private WorkspaceDetails convertWorkspace(WorkspaceInformation wsInfo) {
    WorkspaceDetails wsDetails = new WorkspaceDetails();
    wsDetails.unversionedSetName(wsInfo.getWorkspace().getName());
    wsDetails.unversionedSetState(convertState(wsInfo.getState()));
    wsDetails.unversionedSetDependencies(convertRuntimeDependencyContexts(wsInfo.getRequirements()));
    wsDetails.unversionedSetProblems(convertProblems(wsInfo.getProblems()));
    wsDetails.unversionedSetOrderEntrances(convertOrderEntrances(wsInfo.getOrderEntrances()));
    wsDetails.unversionedSetRepositoryAccess(convertRepositoryAccess(wsInfo.getRepositoryAccess()));
    return wsDetails;
  }

  private ApplicationDetails convertApplication(ApplicationInformation appInfo) {
    ApplicationDetails appDetails = new ApplicationDetails();
    appDetails.unversionedSetName(appInfo.getName());
    appDetails.unversionedSetVersion(appInfo.getVersion());
    appDetails.unversionedSetComment(appInfo.getComment());
    appDetails.unversionedSetRemoteStub(appInfo.getRemoteStub());
    appDetails.unversionedSetState(convertState(appInfo.getState()));
    appDetails.unversionedSetOrderEntrances(convertOrderEntrances(appInfo.getOrderEntrances()));
    appDetails.unversionedSetDependencies(convertRuntimeDependencyContexts(appInfo.getRequirements()));
    appDetails.unversionedSetProblems(convertProblems(appInfo.getProblems()));
    return appDetails;
  }

  private ApplicationDefinitionDetails convertApplicationDefinition(ApplicationDefinitionInformation appDefInfo) {
    ApplicationDefinitionDetails appDefDetails = new ApplicationDefinitionDetails();
    appDefDetails.unversionedSetName(appDefInfo.getName());
    appDefDetails.unversionedSetParentWorkspace(convertWorkspace(appDefInfo.getParentWorkspace()));
    appDefDetails.unversionedSetState(convertState(appDefInfo.getState()));
    appDefDetails.unversionedSetDependencies(convertRuntimeDependencyContexts(appDefInfo.getRequirements()));
    appDefDetails.unversionedSetProblems(convertProblems(appDefInfo.getProblems()));
    return appDefDetails;
  }

  
  public RemoteOperationResult migrateRuntimeContextDependencies(final XynaOrderServerExtension correlatedOrder, final FactoryNode factoryNode, final RuntimeContext from, final RuntimeContext to, final MigrateRuntimeContextDependencyParameter mrcdp) throws RemoteException, ConnectException {
    try {
      MigrationResult result;
      if (factoryNode.getLocal()) {
        RemoteOperationResult ror = checkForPreviousResult(correlatedOrder, factoryNode);
        if (ror != null) {
          return ror;
        } else {
          boolean canNotRunSync = affectsCurrentRuntimeContext(from);
          if (canNotRunSync) {
            Runnable detached = new Runnable() {
              
              public void run() {
                try {
                  MigrationResult detachedResult = localLrcms.migrateRuntimeContextDependencies(correlatedOrder.getCreationRole(),
                                                               convertRuntimeContext(from),
                                                               convertRuntimeContext(to),
                                                               Arrays.asList(MigrateRuntimeContext.MigrationTargets.values()),
                                                               mrcdp.getForce());
                  try {
                    XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage().store("migrateRuntimeContextDependencies", String.valueOf(correlatedOrder.getId()), detachedResult);
                  } catch (PersistenceLayerException e1) {
                    logger.warn("Failed to store result of migrateRuntimeContextDependencies from detached thread", e1);
                  }
                } catch (XynaException e) {
                  try {
                    XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage().store("migrateRuntimeContextDependencies", String.valueOf(correlatedOrder.getId()), e);
                  } catch (PersistenceLayerException e1) {
                    logger.warn("Failed to store result of migrateRuntimeContextDependencies from detached thread", e1);
                  }
                } finally {
                  SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
                  try {
                    srm.resumeOrder(new ResumeTarget(correlatedOrder));
                  } catch (PersistenceLayerException e) {
                    logger.warn("Failed to resume migrateRuntimeContextDependencies main invocation from detached thread", e);
                  }
                }
              }
            };
            Thread detachedThread = new Thread(detached);
            detachedThread.start();
            throw new ProcessSuspendedException(new SuspensionCause_Manual());
          } else {
            result = localLrcms.migrateRuntimeContextDependencies(correlatedOrder.getCreationRole(),
                                                                  convertRuntimeContext(from),
                                                                  convertRuntimeContext(to),
                                                                  Arrays.asList(MigrateRuntimeContext.MigrationTargets.values()),
                                                                  mrcdp.getForce());
          }
        }
      } else {
        RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(factoryNode.getName());
        result = remoteAccess.migrateRuntimeContextDependencies(getCredentials(factoryNode),
                                                                convertRuntimeContext(from),
                                                                convertRuntimeContext(to),
                                                                Arrays.asList(MigrateRuntimeContext.MigrationTargets.values()),
                                                                mrcdp.getForce());
      }
      return convertResult(result, factoryNode);
    } catch (XynaException e) {
      handleXynaException(factoryNode.getName(), "migrateRuntimeContextDependencies", e);
      return failedOperation(e, factoryNode);
    }
  }

  
  private RemoteOperationResult checkForPreviousResult(XynaOrderServerExtension correlatedOrder, FactoryNode node) {
    SecureStorage secStore = XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage();
    Serializable result = secStore.retrieve("migrateRuntimeContextDependencies", String.valueOf(correlatedOrder.getId()));
    if (result != null) {
      if (result instanceof MigrationResult) {
        try {
          secStore.remove("migrateRuntimeContextDependencies", String.valueOf(correlatedOrder.getId()));
        } catch (PersistenceLayerException e) {
          // ntbd
        }
        return convertResult((MigrationResult)result, node);
      } else if (result instanceof XynaException) {
        try {
          secStore.remove("migrateRuntimeContextDependencies", String.valueOf(correlatedOrder.getId()));
        } catch (PersistenceLayerException e) {
          // ntbd
        }
        return failedOperation((XynaException)result, node);
      } else {
        return null;
      }
    } else {
      return null;
    }
  }


  private boolean affectsCurrentRuntimeContext(RuntimeContext from) {
    ClassLoader cl = GlobalApplicationManagmentServicesServiceOperationImpl.class.getClassLoader();
    if (cl instanceof MDMClassLoader) {
      RuntimeContextDependencyManagement rcdMgmt = 
                      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision = ((MDMClassLoader)cl).getRevision();
      Set<Long> allDepsOfThis = new HashSet<Long>();
      rcdMgmt.getDependenciesRecursivly(revision, allDepsOfThis);
      allDepsOfThis.add(revision);
      try {
        return allDepsOfThis.contains(rm.getRevision(convertRuntimeContext(from).asCorrespondingRuntimeContext()));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        return false;
      }
    } else {
      return false;
    }
  }

  private RemoteOperationResult failedOperation(XynaException e, FactoryNode node) {
    RemoteOperationResult ror = new RemoteOperationResult();
    ror.setFactoryNode(node);
    ror.setSuccess(false);
    ror.setErrorMessage(e.getMessage());
    return ror;
  }

  
  private RemoteOperationResult convertResult(MigrationResult result, FactoryNode factoryNode) {
    RemoteOperationResult ror = new RemoteOperationResult();
    StringBuilder msgBuilder = new StringBuilder();
    if (result.getMigrationAbortionReason() == null) {
      ror.setSuccess(true);
      appendMigrationDescription(msgBuilder, result);
    } else {
      ror.setSuccess(false);
      switch (result.getMigrationAbortionReason()) {
        case UNACCESSIBLE_ORDERS :
          msgBuilder.append("Unaccesible orders prevented a successfull migration.");
          break;
        case UNSPECIFIED :
          msgBuilder.append("Migration failed.");
          break;
        case EXCEPTION :
          msgBuilder.append("Migration failed");
          if (result.getMigrationAbortionCause() == null) {
            msgBuilder.append(".");
          } else {
            msgBuilder.append(": ").append(result.getMigrationAbortionCause().getMessage());
          }
          break;
        default :
          break;
      }
    }

    ror.setErrorMessage(msgBuilder.toString());
    ror.setFactoryNode(factoryNode);
    return ror;
  }
  

  private void appendMigrationDescription(StringBuilder msgBuilder, MigrationResult result) {
    if (result.activeOrdersFound()) {
      msgBuilder.append("Migration aborted.\nActive orders present, force the migration to proceed anyway.").append(Constants.LINE_SEPARATOR);
      if (result.getActiveOrderIds(ActiveOrderType.CRON).size() > 0) {
        msgBuilder.append(result.getActiveOrderIds(ActiveOrderType.CRON).size()).append(" affected Crons").append(Constants.LINE_SEPARATOR);
        msgBuilder.append(result.getActiveOrderIds(ActiveOrderType.CRON)).append(Constants.LINE_SEPARATOR);
      }
      if (result.getActiveOrderIds(ActiveOrderType.BATCH).size() > 0) {
        msgBuilder.append(result.getActiveOrderIds(ActiveOrderType.BATCH).size()).append(" affected BatchProcesses").append(Constants.LINE_SEPARATOR);
        msgBuilder.append(result.getActiveOrderIds(ActiveOrderType.BATCH)).append(Constants.LINE_SEPARATOR);
      }
      if (result.getActiveOrderIds(ActiveOrderType.ORDER).size() > 0) {
        msgBuilder.append(result.getActiveOrderIds(ActiveOrderType.ORDER).size()).append(" affected orders").append(Constants.LINE_SEPARATOR);
        msgBuilder.append(result.getActiveOrderIds(ActiveOrderType.ORDER)).append(Constants.LINE_SEPARATOR);
      }
    } else {
      msgBuilder.append("Migration finished").append(Constants.LINE_SEPARATOR);
      if (result.getAbortedOrderIds(ActiveOrderType.CRON).size() > 0) {
        msgBuilder.append(result.getAbortedOrderIds(ActiveOrderType.CRON).size()).append(" aborted Crons").append(Constants.LINE_SEPARATOR);
        msgBuilder.append(result.getAbortedOrderIds(ActiveOrderType.CRON)).append(Constants.LINE_SEPARATOR);
      }
      if (result.getAbortedOrderIds(ActiveOrderType.BATCH).size() > 0) {
        msgBuilder.append(result.getAbortedOrderIds(ActiveOrderType.BATCH).size()).append(" aborted BatchProccesses").append(Constants.LINE_SEPARATOR);
        msgBuilder.append(result.getAbortedOrderIds(ActiveOrderType.BATCH)).append(Constants.LINE_SEPARATOR);
      }
      if (result.getAbortedOrderIds(ActiveOrderType.ORDER).size() > 0) {
        msgBuilder.append(result.getAbortedOrderIds(ActiveOrderType.ORDER).size()).append(" aborted Orders").append(Constants.LINE_SEPARATOR);
        msgBuilder.append(result.getAbortedOrderIds(ActiveOrderType.ORDER)).append(Constants.LINE_SEPARATOR);
      }
      Pair<SuspendRevisionsBean, XPRC_ResumeFailedException> resumeInfo = result.getResumeInformation();
      if (resumeInfo.getFirst().getSuspendedRootOrderIds().size() > 0) {
        msgBuilder.append(resumeInfo.getFirst().getSuspendedRootOrderIds().size()).append(" RootOrders were suspended");
        if (resumeInfo.getSecond() == null) {
          msgBuilder.append(" and resumed.").append(Constants.LINE_SEPARATOR);
        } else {
          msgBuilder.append(".").append(Constants.LINE_SEPARATOR);
          msgBuilder.append(resumeInfo.getFirst().getSuspendedRootOrderIds()).append(Constants.LINE_SEPARATOR);
          msgBuilder.append("There has been an error during resume: ").append(resumeInfo.getSecond().getMessage());
        }
      }
    }
  }

  public void modifyRuntimeDependencyContext(XynaOrderServerExtension correlatedOrder, FactoryNode factoryNode, RuntimeDependencyContextDetails rtCtx) throws RemoteException, ConnectException {
    try{
      if (factoryNode.getLocal()) {
        localLrcms.modifyRuntimeDependencyContext(correlatedOrder.getCreationRole(), convertRuntimeDependencyContextDetail(rtCtx));
      } else {
        RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(factoryNode.getName());
        remoteAccess.modifyRuntimeDependencyContext(getCredentials(factoryNode), convertRuntimeDependencyContextDetail(rtCtx));
      }
    } catch (XynaException e) {
      handleXynaException(factoryNode.getName(), "deleteRuntimeDependencyContext", e);
    }
  }


  private List<RuntimeContext> convertRuntimeDependencyContexts(Collection<RuntimeDependencyContext> requirements) {
    List<RuntimeContext> rtCtxs = new ArrayList<RuntimeContext>();
    for (RuntimeDependencyContext rdc : requirements) {
      rtCtxs.add(convertRuntimeDependencyContext(rdc));
    }
    return rtCtxs;
  }
  

  private RuntimeContext convertRuntimeDependencyContext(RuntimeDependencyContext rdc) {
    switch (rdc.getRuntimeDependencyContextType()) {
      case Application :
        return new Application(rdc.getName(), ((com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application)rdc).getVersionName());
      case ApplicationDefinition :
        return new ApplicationDefinition(rdc.getName(), convertWorkspace(((com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition)rdc).getParentWorkspace()));
      case Workspace :
        return new Workspace(rdc.getName());

      default :
        throw new IllegalArgumentException("Not a valid RuntimeContext: " + rdc);
    }
  }
  
  private RuntimeDependencyContext convertRuntimeContext(RuntimeContext rtCtx) {
    if (rtCtx instanceof Application) {
      return new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application(((Application)rtCtx).getName(), ((Application)rtCtx).getVersion());
    } else if (rtCtx instanceof ApplicationDefinition) {
      return new com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition(((ApplicationDefinition)rtCtx).getName(), convertWorkspace(((ApplicationDefinition)rtCtx).getParentWorkspace()));
    } else if (rtCtx instanceof Workspace) {
      return new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace(((Workspace)rtCtx).getName());
    } else {
      throw new IllegalArgumentException("Not a valid RuntimeDependencyContext: " + rtCtx);
    }
  }
  
  private RuntimeDependencyContextInformation convertRuntimeDependencyContextDetail(RuntimeDependencyContextDetails rtCtx) {
    if (rtCtx instanceof ApplicationDetails) {
      return convertApplicationDetails((ApplicationDetails)rtCtx);
    } else if (rtCtx instanceof ApplicationDefinitionDetails) {
      return convertApplicationDefinitionDetails((ApplicationDefinitionDetails)rtCtx);
    } else if (rtCtx instanceof WorkspaceDetails) {
      return convertWorkspaceDetails((WorkspaceDetails)rtCtx);
    } else {
      throw new IllegalArgumentException("Not a valid RuntimeDependencyContextInformation: " + rtCtx);
    }
  }

  private ApplicationInformation convertApplicationDetails(ApplicationDetails appDetails) {
    ApplicationInformation appInfo = new ApplicationInformation(appDetails.getName(), appDetails.getVersion(), com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState.RUNNING, appDetails.getComment());
    appInfo.setOrderEntrances(convertOrderEntrances(appDetails.getOrderEntrances()));
    appInfo.setRequirements(convertRuntimeDependencyContexts(appDetails.getDependencies()));
    return appInfo;
  }


  private Collection<RuntimeDependencyContext> convertRuntimeDependencyContexts(List<? extends RuntimeContext> dependencies) {
    List<RuntimeDependencyContext> requierments = new ArrayList<RuntimeDependencyContext>();
    for (RuntimeContext dependency : dependencies) {
      requierments.add(convertRuntimeContext(dependency));
    }
    return requierments;
  }

  private ApplicationDefinitionInformation convertApplicationDefinitionDetails(ApplicationDefinitionDetails appDefDetails) {
    ApplicationDefinitionInformation appDefInfo = new ApplicationDefinitionInformation(appDefDetails.getName(), null, convertWorkspace(appDefDetails.getParentWorkspace()), appDefDetails.getComment());
    //appDefInfo.setApplicationEntries(covertApplicationEntries(appDefDetails.getPLACEHOLDER())); TODO
    appDefInfo.setRequirements(convertRuntimeDependencyContexts(appDefDetails.getDependencies()));
    return appDefInfo;
  }

  private WorkspaceInformation convertWorkspaceDetails(WorkspaceDetails wsDetails) {
    WorkspaceInformation wsInfo = new WorkspaceInformation(new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace(wsDetails.getName()));
    wsInfo.setRequirements(convertRuntimeDependencyContexts(wsDetails.getDependencies()));
    return wsInfo;
  }
  
  private Map<OrderEntrance, SerializablePair<Boolean, String>> convertOrderEntrances(List<? extends OrderEntranceType> orderEntrances) {
    OrderEntrance rmi = new OrderEntrance(com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.RMI,
                                          com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.RMI.toString());
    OrderEntrance cli = new OrderEntrance(com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.CLI,
                                          com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.CLI.toString());
    Map<OrderEntrance, SerializablePair<Boolean, String>> orderEntranceMap = new HashMap<OrderEntrance, SerializablePair<Boolean,String>>();
    for (OrderEntranceType oet : orderEntrances) {
      if (oet instanceof RMI) {
        orderEntranceMap.put(rmi, SerializablePair.<Boolean, String>of(Boolean.TRUE,""));
      } else if (oet instanceof CLI) {
        orderEntranceMap.put(cli, SerializablePair.<Boolean, String>of(Boolean.TRUE,""));
      } else if (oet instanceof TriggerInstance) {
        orderEntranceMap.put(new OrderEntrance(com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.triggerInstance,
                                               ((TriggerInstance)oet).getName()),
                             SerializablePair.<Boolean, String>of(((TriggerInstance)oet).getEnabled(),""));
      }
    }
    if (!orderEntranceMap.containsKey(rmi)) {
      orderEntranceMap.put(rmi, SerializablePair.<Boolean, String>of(Boolean.FALSE,""));
    }
    if (!orderEntranceMap.containsKey(cli)) {
      orderEntranceMap.put(cli, SerializablePair.<Boolean, String>of(Boolean.FALSE,""));
    }
    return orderEntranceMap;
  }


}
