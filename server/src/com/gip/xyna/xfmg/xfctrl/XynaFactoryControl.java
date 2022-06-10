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

package com.gip.xyna.xfmg.xfctrl;



import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.classloading.AutomaticUnDeploymentHandlerManager;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcherFactory;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelManagement;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarkerManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagementImpl;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyManagement;
import com.gip.xyna.xfmg.xfctrl.netconfmgmt.NetworkConfigurationManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.NodeManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationManagement;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyManagement;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.rtctxmgmt.RuntimeContextManagement;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.InfrastructureAlgorithmExecutionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xods.components.Components;



public class XynaFactoryControl extends Section {

  public static final String DEFAULT_NAME = "Xyna Factory Control";

  private ClassLoaderDispatcher classLoaderDispatcher;
  private AutomaticUnDeploymentHandlerManager undeploymentHandlerManager;
  private DependencyRegister dependencyRegister;
  private XMOMDatabase xmomDatabase;
  private QueueManagement qManagement;
  private RMIManagement rmiManagement;
  private ApplicationManagementImpl applicationManagement;
  private VersionManagement versionManagement;
  private RevisionManagement revisionManagement;
  private WorkspaceManagement workspaceManagement;
  private NetworkConfigurationManagement networkConfManagement;
  private NodeManagement nodeManagement;
  private DataModelManagement dataModelManagement;
  private FileManagement fileManagement;
  private DeploymentItemStateManagementImpl deploymentItemStateManagement;
  private DeploymentMarkerManagement deploymentMarkerManagement;
  private RuntimeContextDependencyManagement rcDependencyManagement;
  private RemoteDestinationManagement remoteDestinationManagement;
  private ProxyManagement proxyManagement;
  private KeyManagement keyManagement;
  private RuntimeContextManagement runtimeContextManagement;
  private InfrastructureAlgorithmExecutionManagement threadManagement;
  
  public XynaFactoryControl() throws XynaException {
    super();
  }


  @Override
  public void init() throws XynaException {
    deployFunctionGroup(new Components());
    
    classLoaderDispatcher = ClassLoaderDispatcherFactory.getInstance().getImpl();
    deployFunctionGroup(classLoaderDispatcher);

    undeploymentHandlerManager = new AutomaticUnDeploymentHandlerManager();
    deployFunctionGroup(undeploymentHandlerManager);
    
    dependencyRegister = new DependencyRegister();
    deployFunctionGroup(dependencyRegister);
    XynaFactory.getInstance().getFutureExecution().execAsync(new FutureExecutionTask(DependencyRegister.ID_FUTURE_EXECUTION) {

      @Override
      public void execute() {
        //nur dummy für abhängigkeiten
      }
      
    });
    
    xmomDatabase = new XMOMDatabase();
    deployFunctionGroup(xmomDatabase);

    qManagement = new QueueManagement();
    deployFunctionGroup(qManagement);
    
    rmiManagement = new RMIManagement();
    deployFunctionGroup(rmiManagement);
    
    versionManagement = new VersionManagement();
    deployFunctionGroup(versionManagement);
    
    revisionManagement = new RevisionManagement();
    deployFunctionGroup(revisionManagement);
    
    applicationManagement = new ApplicationManagementImpl();
    deployFunctionGroup(applicationManagement);

    workspaceManagement = new WorkspaceManagement();
    deployFunctionGroup(workspaceManagement);

    networkConfManagement = new NetworkConfigurationManagement();
    deployFunctionGroup(networkConfManagement);
    
    nodeManagement = new NodeManagement();
    deployFunctionGroup(nodeManagement);
    
    dataModelManagement = new DataModelManagement();
    deployFunctionGroup(dataModelManagement);
    
    fileManagement = new FileManagement();
    deployFunctionGroup(fileManagement);

    deploymentItemStateManagement = new DeploymentItemStateManagementImpl();
    deployFunctionGroup(deploymentItemStateManagement);

    deploymentMarkerManagement = new DeploymentMarkerManagement();
    deployFunctionGroup(deploymentMarkerManagement);
    
    rcDependencyManagement = new RuntimeContextDependencyManagement();
    deployFunctionGroup(rcDependencyManagement);
    
    remoteDestinationManagement = new RemoteDestinationManagement();
    deployFunctionGroup(remoteDestinationManagement);
    
    proxyManagement = new ProxyManagement();
    deployFunctionGroup(proxyManagement);
    
    keyManagement = new KeyManagement();
    deployFunctionGroup(keyManagement);
    
    runtimeContextManagement = new RuntimeContextManagement();
    deployFunctionGroup(runtimeContextManagement);
    
    threadManagement = new InfrastructureAlgorithmExecutionManagement();
    deployFunctionGroup(threadManagement);
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }
  
  public RMIManagement getRMIManagement() {
    return rmiManagement;
  }
  
  public ClassLoaderDispatcher getClassLoaderDispatcher() {
    return classLoaderDispatcher;
  }

  public AutomaticUnDeploymentHandlerManager getAutomaticUnDeploymentHandlerManager() {
    return undeploymentHandlerManager;
  }

  public DependencyRegister getDependencyRegister() {
    return dependencyRegister;
  }
  
  public XMOMDatabase getXMOMDatabase() {
    return xmomDatabase;
  }

  public QueueManagement getQueueManagement() {
    return qManagement;
  }
  
  public VersionManagement getVersionManagement() {
    return versionManagement;
  }

  public RevisionManagement getRevisionManagement() {
    return revisionManagement;
  }
  
  public ApplicationManagement getApplicationManagement() {
    return applicationManagement;
  }
  
  public WorkspaceManagement getWorkspaceManagement() {
    return workspaceManagement;
  }

  public NetworkConfigurationManagement getNetworkConfigurationManagement() {
    return networkConfManagement;
  }

  public NodeManagement getNodeManagement() {
    return nodeManagement;
  }
  
  public DataModelManagement getDataModelManagement() {
    return dataModelManagement;
  }
  
  public FileManagement getFileManagement() {
    return fileManagement;
  }

  public DeploymentItemStateManagement getDeploymentItemStateManagement() {
    return deploymentItemStateManagement;
  }
  
  public DeploymentMarkerManagement getDeploymentMarkerManagement() {
    return deploymentMarkerManagement;
  }
  
  public RuntimeContextDependencyManagement getRuntimeContextDependencyManagement() {
    return rcDependencyManagement;
  }
  
  public RemoteDestinationManagement getRemoteDestinationManagement() {
    return remoteDestinationManagement;
  }
  
  public ProxyManagement getProxyManagement() {
    return proxyManagement;
  }
  
  public KeyManagement getKeyManagement() {
    return keyManagement;
  }
  
  public RuntimeContextManagement getRuntimeContextManagement() {
    return runtimeContextManagement;
  }
  
  public InfrastructureAlgorithmExecutionManagement getInfrastructureAlgorithmExecutionManagement() {
    return threadManagement;
  }
  
}
