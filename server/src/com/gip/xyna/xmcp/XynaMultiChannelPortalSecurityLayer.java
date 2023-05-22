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
package com.gip.xyna.xmcp;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.FilterInformation;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.TriggerInformation;
import com.gip.xyna.xdev.exceptions.XDEV_CodeAccessInitializationException;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilterInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringActionParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringResult;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement.Path;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.parameters.InstantiateRepositoryAccessParameters;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCreationOfExistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidModificationOfUnexistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchDataModelTypeException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownSessionIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterInformation;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.BuildApplicationVersionParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.CopyApplicationIntoWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassProvider;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ImportDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ModifyDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.RemoveDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarker;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNodeStorable;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.CreateWorkspaceResult;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.RemoveWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xfmg.xfmon.XynaFactoryMonitoring;
import com.gip.xyna.xfmg.xfmon.processmonitoring.ProcessMonitoring;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.PropertyMap;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement.OptionalOISGenerateMetaInformation;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.SearchOrdertypeParameter;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.managedsessions.ASessionPrivilege;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionBasedUserContextValue;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionDetails;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.ANotificationConnection;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.PasswordExpiration;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaUserCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSearchResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSelect;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageRetrievalResult;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageSubscriptionParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSearchResult;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSelectImpl;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSearchResult;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSelect;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DependentObjectMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ExtendedManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionSelect;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xpce.statustracking.IStatusChangeListener;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.SearchMode;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchiveStatisticsStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceResult;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowInformation;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSearchResult;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSelectImpl;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;



/*
 * TODO - should be generated!
 *      - do not extend xynamultichannelportal 
 * 
 * could be generated as follows
 * - take every declared and public method
 * - add 'Role role' to it's parameters
 * - inside the method body call super.<methodName>(oldParametersWithoutRole) <- why super, he could call his own, he extends and therefore inherits them
 *     |_> if it's a static method (do we want to control em?) call XynaMultiChannelPortal.<methodName>(oldParametersWithoutRole) instead 
 * - if a it's annotated with AccessControlled insert a hasRights-Check around the call
 *                                                |       |=> with methodName = method.getName() & role = the incoming role
 *                                                |=> and add a 'throws AuthentificationException' to method        
 */
public class XynaMultiChannelPortalSecurityLayer extends XynaMultiChannelPortal {

  // FIXME externalize all the method name strings

  public XynaMultiChannelPortalSecurityLayer() throws XynaException {
    super();
  }

  private void checkRights(String operation, Role role) throws PersistenceLayerException, XFMG_ACCESS_VIOLATION {
    if ( ! hasRight(resolveFunctionToRight(operation), role)) {
      throw new XFMG_ACCESS_VIOLATION(operation, role.getName());
    }
  }

  /**
   * Rechte�berpr�fung f�r Rechtebereiche.
   */
  private void checkScopedRights(String scopedRight, Role role) throws PersistenceLayerException, XFMG_ACCESS_VIOLATION {
    if (!hasRight(scopedRight, role)) {
      throw new XFMG_ACCESS_VIOLATION(scopedRight, role.getName());
    }
  }

  private UserManagement getUserManagement() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement();
  }

  @Override
  // kind of cheap we have to do this
  public boolean hasRight(String methodName, Role role) throws PersistenceLayerException {
    return XynaFactory.getInstance().getFactoryManagementPortal().hasRight(methodName, role);
  }


  public String getCrossDomainXML(Role role) throws XynaException {
    return super.getCrossDomainXML();
  }


  public String getMinimalCrossDomainXML(String ip, String hostname, String port, Role role) {
    return super.getMinimalCrossDomainXML(ip, hostname, port);
  }

  @Deprecated
  public void deployWF(String fqXmlName, WorkflowProtectionMode mode, Role role) throws XynaException {
    deployWF(fqXmlName, mode, RevisionManagement.REVISION_DEFAULT_WORKSPACE, role);
  }
  
  public void deployWF(String fqXmlName, WorkflowProtectionMode mode, Long revision, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("deployWF"), role)) {
      super.deployWF(fqXmlName, mode, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deployWF", role.getName());
    }
  }


  public void undeployWF(String fqXmlName, boolean disableChecks, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("undeployWF"), role)) {
      super.undeployWF(fqXmlName, disableChecks);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("undeployWF", role.getName());
    }
  }

  @Deprecated
  public void undeployWF(String originalFqName, boolean undeploydDependent, boolean disableChecks, Role role)
                  throws XynaException {
    undeployWF(originalFqName, undeploydDependent, disableChecks, RevisionManagement.REVISION_DEFAULT_WORKSPACE, role);
  }

  public void undeployWF(String originalFqName, boolean undeploydDependent, boolean disableChecks, Long revision, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("undeployWF"), role)) {
      super.undeployWF(originalFqName, undeploydDependent, disableChecks, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("undeployWF", role.getName());
    }
  }

  public void undeployXMOMObject(String originalFqName, XMOMType type, DependentObjectMode dependentObjectMode, boolean disableChecks, Long revision, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("undeployXMOMObject"), role)) {
      super.undeployXMOMObject(originalFqName, type, dependentObjectMode, disableChecks, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("undeployXMOMObject", role.getName());
    }
  }

  @Deprecated
  public String saveMDM(String xml, Role role) throws XynaException {
    return saveMDM(xml, RevisionManagement.REVISION_DEFAULT_WORKSPACE, role);
  }

  public String saveMDM(String xml, Long revision, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("saveMDM"), role)) {
      return super.saveMDM(xml, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("saveMDM", role.getName());
    }
  }
  
  @Deprecated
  public String saveMDM(String xml, boolean override, String user, String sessionId, Role role) throws XynaException {
    return saveMDM(xml, override, user, sessionId, RevisionManagement.REVISION_DEFAULT_WORKSPACE, role);
  }
  
  
  public String saveMDM(String xml, boolean override, String user, String sessionId, Long revision, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("saveMDM"), role)) {
      return super.saveMDM(xml, override, user, sessionId, revision, null);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("saveMDM", role.getName());
    }
  }


  public void registerSavedWorkflow(String fqXmlName, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("registerSavedWorkflow"), role)) {
      super.registerSavedWorkflow(fqXmlName);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("registerSavedWorkflow", role.getName());
    }
  }


  public void deployMultiple(Map<XMOMType, List<String>> deploymentItems, WorkflowProtectionMode mode, Long revision, Role role) 
                  throws XynaException{
    if (hasRight(resolveFunctionToRight("deployMultiple"), role)) {
      super.deployMultiple(deploymentItems, mode, revision);
    } else {
      throw new XFMG_ACCESS_VIOLATION("deployMultiple", role.getName());
    }
  }
  
  public void deployMDM(String fqXmlName, WorkflowProtectionMode mode, String fileName, InputStream inputStream,
                        Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("deployMDM"), role)) {
      super.deployMDM(fqXmlName, mode, fileName, inputStream);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deployMDM", role.getName());
    }
  }

  @Deprecated
  public void deployDatatype(String fqXmlName, WorkflowProtectionMode mode, Map<String, InputStream> jars, Role role)
                  throws XynaException {
    deployDatatype(fqXmlName, mode, jars, RevisionManagement.REVISION_DEFAULT_WORKSPACE, role);
  }
  
  public void deployDatatype(String fqXmlName, WorkflowProtectionMode mode, Map<String, InputStream> jars, Long revision, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("deployDatatype"), role)) {
      super.deployDatatype(fqXmlName, mode, jars, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deployDatatype", role.getName());
    }
  }

  @Deprecated
  public void deployException(String fqXmlName, WorkflowProtectionMode mode, Role role) throws XynaException {
    deployException(fqXmlName, mode, RevisionManagement.REVISION_DEFAULT_WORKSPACE, role);
  }
  
  public void deployException(String fqXmlName, WorkflowProtectionMode mode, Long revision, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("deployDatatype"), role)) {
      super.deployException(fqXmlName, mode, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deployException", role.getName());
    }
  }

  @Deprecated
  public void undeployMDM(String fqXmlName, boolean undeployDependendObjects, boolean disableChecks, Role role)
                  throws XynaException {
    undeployMDM(fqXmlName, undeployDependendObjects, disableChecks, RevisionManagement.REVISION_DEFAULT_WORKSPACE, role);
  }

  public void undeployMDM(String fqXmlName, boolean undeployDependendObjects, boolean disableChecks, Long revision, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("undeployMDM"), role)) {
      super.undeployMDM(fqXmlName, undeployDependendObjects, disableChecks, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("undeployMDM", role.getName());
    }
  }

  @Deprecated
  public void undeployException(String originalFqName, boolean undeployDependendObjects, boolean disableChecks,
                                Role role) throws XynaException {
    undeployException(originalFqName, undeployDependendObjects, disableChecks, RevisionManagement.REVISION_DEFAULT_WORKSPACE, role);
  }

  public void undeployException(String originalFqName, boolean undeployDependendObjects, boolean disableChecks,
                                Long revision, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("undeployException"), role)) {
      super.undeployException(originalFqName, undeployDependendObjects, disableChecks, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("undeployException", role.getName());
    }
  }


  public void addTrigger(String name, ZipInputStream jarFiles, String fqTriggerClassName, String[] sharedLibs,
                         String description, String startParameterDocumentation, Role role, long revision) throws XynaException {
    if (hasRight(resolveFunctionToRight("addTrigger"), role)) {
      super.addTrigger(name, jarFiles, fqTriggerClassName, sharedLibs, description, startParameterDocumentation, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("addTrigger", role.getName());
    }
  }


  public void removeTrigger(String nameOfTrigger, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("removeTrigger"), role)) {
      super.removeTrigger(nameOfTrigger);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("removeTrigger", role.getName());
    }
  }


  public void deployTrigger(String nameOfTrigger, String nameOfTriggerInstance, String[] startParameter,
                            String description, Role role, long revision) throws XynaException {
    if (hasRight(resolveFunctionToRight("deployTrigger"), role)) {
      super.deployTrigger(nameOfTrigger, nameOfTriggerInstance, startParameter, description, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deployTrigger", role.getName());
    }
  }


  public void undeployTrigger(String nameOfTrigger, String nameOfTriggerInstance, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("undeployTrigger"), role)) {
      super.undeployTrigger(nameOfTrigger, nameOfTriggerInstance);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("undeployTrigger", role.getName());
    }
  }


  public void addFilter(String filterName, ZipInputStream jarFiles, String fqFilterClassName, String triggerName,
                        String[] sharedLibs, String description, Role role, long revision) throws XynaException {
    if (hasRight(resolveFunctionToRight("addFilter"), role)) {
      super.addFilter(filterName, jarFiles, fqFilterClassName, triggerName, sharedLibs, description, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("addFilter", role.getName());
    }
  }


  public void removeFilter(String nameOfFilter, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("removeFilter"), role)) {
      super.removeFilter(nameOfFilter);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("removeFilter", role.getName());
    }
  }


  public void deployFilter(String filtername, String nameOfFilterInstance, String nameOfTriggerInstance,
                           String description, Role role, long revision) throws XynaException {
    if (hasRight(resolveFunctionToRight("deployFilter"), role)) {
      super.deployFilter(filtername, nameOfFilterInstance, nameOfTriggerInstance, description, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deployFilter", role.getName());
    }
  }


  public void undeployFilter(String filterName, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("undeployFilter"), role)) {
      super.undeployFilter(filterName);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("undeployFilter", role.getName());
    }
  }


  public void addStatusChangeListener(ClassProvider cp, IStatusChangeListener listener, Role role) {
    super.addStatusChangeListener(cp, listener);
  }


  @Override
  public void removeStatusChangeListener(IStatusChangeListener listener) {
    super.removeStatusChangeListener(listener);
  }


  public InputStream getServiceImplTemplate(String baseDir, String fqClassNameDOM,
                                            boolean deleteServiceImplAfterStreamClose, boolean deleteBaseDir, Role role)
                  throws XynaException {
    return super.getServiceImplTemplate(baseDir, fqClassNameDOM, deleteServiceImplAfterStreamClose, deleteBaseDir);

  }


  public InputStream getServiceImplTemplate(String fqClassNameDOM, boolean deleteServiceImplAfterStreamClose, Role role)
                  throws XynaException {
    return super.getServiceImplTemplate(fqClassNameDOM, deleteServiceImplAfterStreamClose);
  }


  public InputStream getTriggerImplTemplate(String baseDir, String triggerName,
                                            boolean deleteTriggerImplAfterStreamClose, boolean deleteBaseDir, Role role)
                  throws XynaException {
    return super.getTriggerImplTemplate(baseDir, triggerName, deleteTriggerImplAfterStreamClose, deleteBaseDir);
  }


  public InputStream getTriggerImplTemplate(String triggerName, boolean deleteTriggerImplAfterStreamClose, Role role)
                  throws XynaException {
    return super.getTriggerImplTemplate(triggerName, deleteTriggerImplAfterStreamClose);
  }


  public InputStream getFilterImplTemplate(String baseDir, String filterName, String fqTriggerClassName,
                                           boolean deleteFilterImplAfterStreamClose, boolean deleteBaseDir, Role role)
                  throws XynaException {
    return super.getFilterImplTemplate(baseDir, filterName, fqTriggerClassName, deleteFilterImplAfterStreamClose,
                                       deleteBaseDir);
  }


  public InputStream getFilterImplTemplate(String filterName, String fqTriggerClassName,
                                           boolean deleteFilterImplAfterStreamClose, Role role) throws XynaException {
    return super.getFilterImplTemplate(filterName, fqTriggerClassName, deleteFilterImplAfterStreamClose);
  }


  public boolean removeCronLikeOrder(Long id, Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.CRON_LIKE_ORDER, Action.delete);
    checkScopedRights(scopedRight, role);
    return super.removeCronLikeOrder(id);
  }


  public CronLikeOrder startCronLikeOrder(CronLikeOrderCreationParameter clocp, Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.CRON_LIKE_ORDER, Action.insert);
    checkScopedRights(scopedRight, role);

    //Start-Order-Recht wird zus�tzlich ben�tigt
    if (!hasRight(resolveFunctionToRight("startOrder"), role)) {
      String startOrderRight = getUserManagement().getStartOrderRight(clocp.getDestinationKey());
      checkScopedRights(startOrderRight, role);
    }
    
    return super.startCronLikeOrder(clocp);
  }
  

  @Deprecated
  public CronLikeOrder modifyCronLikeOrder(Long id, String label, String orderType, GeneralXynaObject payload,
                                           Long firstStartupTime, Long interval, Boolean enabled,
                                           OnErrorAction onError, Role role) throws XynaException {
    return modifyCronLikeOrder(id, label, new DestinationKey(orderType), payload, firstStartupTime, Constants.DEFAULT_TIMEZONE, interval,
                               false, enabled, onError, null, null, null, null, role);
  }


  public CronLikeOrder modifyCronLikeOrder(Long id, String label, DestinationKey destination, GeneralXynaObject payload,
                                           Long firstStartupTime, String timeZoneID, Long interval, Boolean useDST,
                                           Boolean enabled, OnErrorAction onError, String cloCustom0,
                                           String cloCustom1, String cloCustom2, String cloCustom3, Role role)
      throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.CRON_LIKE_ORDER, Action.write);
    checkScopedRights(scopedRight, role);

    //Start-Order-Recht f�r neue Destination wird ben�tigt
    if (!hasRight(resolveFunctionToRight("startOrder"), role)) {
      if (destination != null) {
        String scopedRightDestination = getUserManagement().getStartOrderRight(destination);
        checkScopedRights(scopedRightDestination, role);
      }
    }

    return super.modifyCronLikeOrder(id, label, destination, payload, firstStartupTime, timeZoneID, interval, useDST,
                                     enabled, onError, cloCustom0, cloCustom1, cloCustom2, cloCustom3);
  }

  
  public CronLikeOrder modifyCronLikeOrder(Long id, String label, DestinationKey destination, GeneralXynaObject payload,
                                           Calendar firstStartupTimeWithTimeZone, Long interval, Boolean useDST,
                                           Boolean enabled, OnErrorAction onError, String cloCustom0,
                                           String cloCustom1, String cloCustom2, String cloCustom3, Role role)
      throws XynaException {
    return modifyCronLikeOrder(id, label, destination, payload, firstStartupTimeWithTimeZone.getTimeInMillis(),
                               firstStartupTimeWithTimeZone.getTimeZone().getID(), interval, useDST, enabled, onError,
                               cloCustom0, cloCustom1, cloCustom2, cloCustom3, role);
  }
  
  public CronLikeOrder modifyTimeControlledOrder(Long id, CronLikeOrderCreationParameter clocp, Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.CRON_LIKE_ORDER, Action.write);
    checkScopedRights(scopedRight, role);
    
    //Start-Order-Recht f�r neue Destination wird ben�tigt
    if (!hasRight(resolveFunctionToRight("startOrder"), role)) {
      String startOrderRight = getUserManagement().getStartOrderRight(clocp.getDestinationKey());
      checkScopedRights(startOrderRight, role);
    }
    return super.modifyTimeControlledOrder(id, clocp);
  }
  

  public Long startOrder(XynaOrderCreationParameter xocp, Role role) throws XynaException {
    if (!hasRight(resolveFunctionToRight("startOrder"), role)) {
      String scopedRight = getUserManagement().getStartOrderRight(xocp.getDestinationKey());
      checkScopedRights(scopedRight, role);
    }
    
    return super.startOrder(xocp);
  }


  public GeneralXynaObject startOrderSynchronously(XynaOrderCreationParameter xocp, Role role) throws XynaException {
    if (!hasRight(resolveFunctionToRight("startOrderSynchronously"), role)) {
      String scopedRight = getUserManagement().getStartOrderRight(xocp.getDestinationKey());
      checkScopedRights(scopedRight, role);
    }
    return super.startOrderSynchronously(xocp);
  }


  public XynaOrderServerExtension startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp, Role role)
                  throws XynaException {
    if (!hasRight(resolveFunctionToRight("startOrderSynchronouslyAndReturnOrder"), role)) {
      String scopedRight = getUserManagement().getStartOrderRight(xocp.getDestinationKey());
      checkScopedRights(scopedRight, role);
    }
    return super.startOrderSynchronouslyAndReturnOrder(xocp);
  }

  public OrderExecutionResponse startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp, Role role, ResultController resultController)
                  throws XynaException {
    if (!hasRight(resolveFunctionToRight("startOrderSynchronouslyAndReturnOrder"), role)) {
      String scopedRight = getUserManagement().getStartOrderRight(xocp.getDestinationKey());
      checkScopedRights(scopedRight, role);
    }
    return super.startOrderSynchronouslyAndReturnOrder(xocp, resultController);
  }


  public Long startBatchProcess(BatchProcessInput input, Role role) throws XynaException {
    String scopedRight = getUserManagement().getManageTCORight(Action.insert,input);
    checkScopedRights(scopedRight, role);
    return super.startBatchProcess(input);
  }

  public BatchProcessInformation startBatchProcessSynchronous(BatchProcessInput input, Role role) throws XynaException {
    String scopedRight = getUserManagement().getManageTCORight(Action.insert,input);
    checkScopedRights(scopedRight, role);
    return super.startBatchProcessSynchronous(input);
  }
  
  public BatchProcessInformation getBatchProcessInformation(Long batchProcessId, Role role) throws XynaException {
    BatchProcessInformation info = super.getBatchProcessInformation(batchProcessId);
    if (info != null) {
      //ist das Read-Recht f�r diese TCO vorhanden?
      String scopedRight = getUserManagement().getReadTCORight(info);
      checkScopedRights(scopedRight, role);
    }
    return info;
  }
  
  public BatchProcessSearchResult searchBatchProcesses(BatchProcessSelectImpl select, int maxRows, Role role) throws XynaException {
    List<String> scopedRights = getUserManagement().getReadTCORights(role);
    if (scopedRights.size() == 0) {
      //es ist kein Read-Recht vorhanden
      throw new XFMG_ACCESS_VIOLATION("searchBatchProcesses", role.getName());
    }
    //vorhandene Read-Rechte der SelectImpl �bergeben, damit nur die TCOs
    //selektiert werden, f�r die ein passendes Recht vorhanden ist
    select.setRightWhereClauses(scopedRights);
    
    return super.searchBatchProcesses(select,maxRows);
  }

  public boolean cancelBatchProcess(Long batchProcessId, CancelMode cancelMode, Role role) throws XynaException{
    String scopedRight = getUserManagement().getManageTCORight(Action.kill, batchProcessId);
    checkScopedRights(scopedRight, role);
    return super.cancelBatchProcess(batchProcessId, cancelMode);
  }

  public boolean pauseBatchProcess(Long batchProcessId, Role role) throws XynaException {
    String scopedRight = getUserManagement().getManageTCORight(Action.disable, batchProcessId);
    checkScopedRights(scopedRight, role);
    return super.pauseBatchProcess(batchProcessId);
  }

  public boolean continueBatchProcess(Long batchProcessId, Role role) throws XynaException {
    String scopedRight = getUserManagement().getManageTCORight(Action.enable, batchProcessId);
    checkScopedRights(scopedRight, role);
    return super.continueBatchProcess(batchProcessId);
  }

  public boolean modifyBatchProcess(Long batchProcessId, BatchProcessInput input, Role role) throws XynaException {
    String scopedRight = getUserManagement().getManageTCORight(Action.write, batchProcessId);
    checkScopedRights(scopedRight, role);
    return super.modifyBatchProcess(batchProcessId, input);
  }

  public CancelBean cancelOrder(Long id, Long timeout, Role role) throws XynaException {
    return super.cancelOrder(id, timeout);
  }


  public String getProperty(String key, Role role) throws XynaException {
    String scopedRight = getUserManagement().getXynaPropertyRight(key, Action.read);
    checkScopedRights(scopedRight, role);
    return super.getProperty(key);
  }

  public XynaPropertyWithDefaultValue getPropertyWithDefaultValue(String key, Role role) throws XynaException {
    String scopedRight = getUserManagement().getXynaPropertyRight(key, Action.read);
    checkScopedRights(scopedRight, role);
    return super.getPropertyWithDefaultValue(key);
  }


  public PropertyMap<String, String> getPropertiesReadOnly(Role role) throws XynaException {
    //�berpr�fen, ob �berhaupt ein read-Recht vorhanden ist
    String scopedRight = getUserManagement().getXynaPropertyRight(null, Action.read);
    checkScopedRights(scopedRight, role);
    
    //Properties bestimmen
    PropertyMap<String, String> properties = super.getPropertiesReadOnly();
    
    //nur die Properties zur�ckgeben, f�r die ein read-Recht vorhanden ist
    PropertyMap<String, String> allowedProperties = new PropertyMap<String, String>();
    for (String key : properties.keySet()) {
      scopedRight = getUserManagement().getXynaPropertyRight(key, Action.read);
      if (hasRight(scopedRight, role)) {
        allowedProperties.put(key, properties.get(key));
      }
    }
    
    return allowedProperties;
  }
  
  public Collection<XynaPropertyWithDefaultValue> getPropertiesWithDefaultValuesReadOnly(Role role) throws XynaException {
    //�berpr�fen, ob �berhaupt ein read-Recht vorhanden ist
    String scopedRight = getUserManagement().getXynaPropertyRight(null, Action.read);
    checkScopedRights(scopedRight, role);
    
    //Properties bestimmen
    Collection<XynaPropertyWithDefaultValue> properties = super.getPropertiesWithDefaultValuesReadOnly();
    
    //nur die Properties zur�ckgeben, f�r die ein read-Recht vorhanden ist
    Iterator<XynaPropertyWithDefaultValue> it = properties.iterator();
    while (it.hasNext()) {
      scopedRight = getUserManagement().getXynaPropertyRight(it.next().getName(), Action.read);
      if (!hasRight(scopedRight, role)) {
        it.remove();
      }
    }
    
    return properties;
  }


  public void setProperty(String key, String value, Role role) throws XynaException {
    String scopedRight = getUserManagement().getXynaPropertyRight(key, Action.write);
    checkScopedRights(scopedRight, role);
    super.setProperty(key, value);
  }

  public void setProperty(XynaPropertyWithDefaultValue property, Role role) throws XynaException {
    String scopedRight = getUserManagement().getXynaPropertyRight(property.getName(), Action.write);
    checkScopedRights(scopedRight, role);
    super.setProperty(property);
  }


  public void removeProperty(String key, Role role) throws XynaException {
    String scopedRight = getUserManagement().getXynaPropertyRight(key, Action.delete);
    checkScopedRights(scopedRight, role);
    super.removeProperty(key);
  }


  public void setDefaultMonitoringLevel(Integer code, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("setDefaultMonitoringLevel"), role)) {
      super.setDefaultMonitoringLevel(code);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setDefaultMonitoringLevel", role.getName());
    }
  }


  public void setMonitoringLevel(String orderType, Integer code, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("setMonitoringLevel"), role)) {
      super.setMonitoringLevel(orderType, code);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setMonitoringLevel", role.getName());
    }
  }


  public Integer getMonitoringLevel(String orderType, Role role) {
    return super.getMonitoringLevel(orderType);
  }


  public void removeMonitoringLevel(String orderType, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("removeMonitoringLevel"), role)) {
      super.removeMonitoringLevel(orderType);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("removeMonitoringLevel", role.getName());
    }
  }


  public Collection<CapacityInformation> listCapacityInformation(Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.CAPACITY, Action.read);
    checkScopedRights(scopedRight, role);
    return super.listCapacityInformation();
  }


  public ExtendedCapacityUsageInformation listExtendedCapacityUsageInformation(Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.CAPACITY, Action.read);
    checkScopedRights(scopedRight, role);
    return super.listExtendedCapacityInformation();
  }


  public Map<ApplicationEntryType, Map<String, DeploymentStatus>> listDeploymentStatuses(Role role, Long revision) {
    return super.listDeploymentStatuses(revision);
  }

  public List<WorkflowInformation> listWorkflows(Role role) throws XynaException {
    return super.listWorkflows();
  }

  public boolean addCapacity(String name, int cardinality, CapacityManagement.State state, Role role)
                  throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.CAPACITY, Action.insert);
    checkScopedRights(scopedRight, role);
    return super.addCapacity(name, cardinality, state);
  }


  public boolean removeCapacity(String name, Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.CAPACITY, Action.delete);
    checkScopedRights(scopedRight, role);
    return super.removeCapacity(name);
  }


  public boolean changeCapacityName(String name, String newName, Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.CAPACITY, Action.write);
    checkScopedRights(scopedRight, role);
    return super.changeCapacityName(name, newName);
  }


  public boolean changeCapacityCardinality(String name, int cardinality, Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.CAPACITY, Action.write);
    checkScopedRights(scopedRight, role);
    return super.changeCapacityCardinality(name, cardinality);
  }


  public boolean changeCapacityState(String name, CapacityManagement.State state, Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.CAPACITY, Action.write);
    checkScopedRights(scopedRight, role);
    return super.changeCapacityState(name, state);
  }


  @Deprecated
  public boolean requireCapacityForWorkflow(String workflowFqName, String capacityName, int capacityCardinality,
                                            Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.ORDER_TYPE, Action.write);
    checkScopedRights(scopedRight, role);
    return super.requireCapacityForWorkflow(workflowFqName, capacityName, capacityCardinality);
  }


  @Deprecated
  public boolean removeCapacityForWorkflow(String workflowFqName, String capacityName, Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.ORDER_TYPE, Action.write);
    checkScopedRights(scopedRight, role);
    return super.removeCapacityForWorkflow(workflowFqName, capacityName);
  }


  public boolean requireCapacityForOrderType(String orderType, String capacityName, int capacityCardinality, Role role)
                  throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.ORDER_TYPE, Action.write);
    checkScopedRights(scopedRight, role);
    return super.requireCapacityForOrderType(orderType, capacityName, capacityCardinality);
  }


  public boolean removeCapacityForOrderType(String orderType, String capacityName, Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.ORDER_TYPE, Action.write);
    checkScopedRights(scopedRight, role);
    return super.removeCapacityForOrderType(orderType, capacityName);
  }


  public List<Capacity> listCapacitiesForOrderType(DestinationKey destination, Role role) throws XynaException {
    return super.listCapacitiesForOrderType(destination);
  }


  public OrderInstanceDetails getOrderInstanceDetails(Long id, Role role) throws XynaException {
    return super.getOrderInstanceDetails(id);
  }


  public Map<String, OrderArchiveStatisticsStorable> getStatisticsMap(Role role) {
    return super.getCompleteCallStatistics();
  }


  public Map<Long, CronLikeOrderInformation> getAllCronLikeOrders(long maxRows, Role role)
                  throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.CRON_LIKE_ORDER, Action.read);
    checkScopedRights(scopedRight, role);
    return super.getAllCronLikeOrders(maxRows);
  }
  

  public CronLikeOrderSearchResult searchCronLikeOrders(CronLikeOrderSelectImpl selectCron, int maxRows, Role role)
                                                    throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.CRON_LIKE_ORDER, Action.read);
    checkScopedRights(scopedRight, role);
    return super.searchCronLikeOrders(selectCron, maxRows);
  }


  public Map<Long, OrderInstance> getAllRunningProcesses(long offset, int count, Role role) throws XynaException {
    return super.getAllRunningProcesses(offset, count);
  }


  public OrderInstanceDetails getRunningProcessDetails(Long id, Role role) throws XynaException {
    return super.getRunningProcessDetails(id);
  }


  public String getRunningProcessDetailsXML(Long id, Role role) throws XynaException {
    return super.getRunningProcessDetailsXML(id);
  }


  public Map<Long, ManualInteractionEntry> listManualInteractionEntries(Role role) throws PersistenceLayerException {
    return super.listManualInteractionEntries();
  }


  public ManualInteractionResult searchManualInteractionEntries(ManualInteractionSelect select, int maxRows, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("searchManualInteractionEntries"), role)) {
      return super.searchManualInteractionEntries(select, maxRows);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("searchManualInteractionEntries", role.getName());
    }
  }
  
  
  public ExtendedManualInteractionResult searchExtendedManualInteractionEntries(ManualInteractionSelect select,
                                                                                int maxRows, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("searchExtendedManualInteractionEntries"), role)) {
      return super.searchExtendedManualInteractionEntries(select, maxRows);
    } else {
      throw new XFMG_ACCESS_VIOLATION("searchExtendedManualInteractionEntries", role.getName());
    }
  }


  public void processManualInteractionEntry(Long id, GeneralXynaObject response, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("processManualInteractionEntry"), role)) {
      super.processManualInteraction(id, response);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("processManualInteractionEntry", role.getName());
    }
  }


  public GeneralXynaObject waitForMI(XynaOrderServerExtension xo, String reason, String type, String userGroup,
                                     String todo, GeneralXynaObject payload, Role role) throws XynaException {
    return super.waitForMI(xo, reason, type, userGroup, todo, payload);
  }


  public ConnectionFilterInstance[] getFilterInstances(String filterName, Role role) throws XynaException {
    return super.getFilterInstances(filterName);
  }


  public Filter[] getFilters(String triggerName, Role role) throws XynaException {
    return super.getFilters(triggerName);
  }


  public EventListenerInstance[] getTriggerInstances(String triggerName, Role role) throws XynaException {
    return super.getTriggerInstances(triggerName);
  }


  public Trigger[] getTriggers(Role role) throws XynaException {
    return super.getTriggers();
  }


  public static void exportMDM(TemporaryFileHandler tfh, Role role) throws Exception {
    XynaMultiChannelPortal.exportMDM(tfh);
  }


  public static void importMDM(ZipInputStream zis, Role role) throws XynaException {
    XynaMultiChannelPortal.importMDM(zis);
  }


  public ProcessMonitoring getProcessMonitoring(Role role) {
    return super.getProcessMonitoring();
  }


  public XynaFactoryManagementODS getXynaFactoryManagementODS(Role role) {
    return super.getXynaFactoryManagementODS();
  }


  public XynaFactoryMonitoring getXynaFactoryMonitoring(Role role) {
    return super.getXynaFactoryMonitoring();
  }


  public XynaFactoryControl getXynaFactoryControl(Role role) {
    return super.getXynaFactoryControl();
  }


  public FactoryWarehouseCursor<OrderInstanceBackup> listSuspendedOrders(ODSConnection defaultConnection, Role role)
      throws XynaException {
    return super.listSuspendedOrders(defaultConnection);
  }

  
  @Deprecated
  public SessionCredentials getNewSession(User user, boolean force, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("getNewSession"), role)) {
      return super.getNewSession(user, force);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("getNewSession", role.getName());
    }
  }
  
  
  public SessionCredentials createSession(XynaUserCredentials credentials, Optional<String> roleName, boolean force, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("createSession"), role)) {
      return super.createSession(credentials, roleName, force);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("createSession", role.getName());
    }
  }


  public boolean authorizeSession(String sessionId, String token, String roleName, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("authorizeSession"), role)) {
      return super.authorizeSession(sessionId, token, roleName);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("authorizeSession", role.getName());
    }
  }


  public SessionDetails getSessionDetails(String sessionId, Role role) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XFMG_UnknownSessionIDException {
    return super.getSessionDetails(sessionId);
  }


  @Override
  public SessionDetails getSessionDetails(String sessionId) throws PersistenceLayerException,
                  XFMG_UnknownSessionIDException {
    return super.getSessionDetails(sessionId);
  }


  @Override
  public void quitSession(String sessionId) throws PersistenceLayerException {
    super.quitSession(sessionId);
  }


  public void quitSession(String sessionId, Role role) throws XynaException {
    super.quitSession(sessionId);
  }

  
  public boolean isSessionAlive(String sessionId, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("isSessionAlive"), role)) {
      return super.isSessionAlive(sessionId);
    } else {
      throw new XFMG_ACCESS_VIOLATION("isSessionAlive", role.getName());
    }
  }
  

  public boolean releaseAllSessionPriviliges(String sessionId, Role role) throws PersistenceLayerException {
    return super.releaseAllSessionPriviliges(sessionId);
  }


  public boolean releaseSessionPrivilige(String sessionId, ASessionPrivilege privilige, Role role)
                  throws PersistenceLayerException {
    return super.releaseSessionPrivilige(sessionId, privilige);
  }


  public boolean requestSessionPriviliges(String sessionId, ASessionPrivilege privilige, Role role)
                  throws PersistenceLayerException {
    return super.requestSessionPriviliges(sessionId, privilige);
  }


  public boolean keepSessionAlive(String sessionId, Role role) throws PersistenceLayerException {
    return super.keepSessionAlive(sessionId);
  }


  public boolean createUser(String id, String roleName, String password, boolean isPassHashed, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("createUser"), role)) {
      return super.createUser(id, roleName, password, isPassHashed);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("createUser", role.getName());
    }
  }


  public boolean importUser(String id, String roleName, String passwordhash, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("importUser"), role)) {
      return super.importUser(id, roleName, passwordhash);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("importUser", role.getName());
    }
  }


  public boolean deleteUser(String id, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("deleteUser"), role)) {
      return super.deleteUser(id);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deleteUser", role.getName());
    }
  }


  public String listUsers(Role role) throws PersistenceLayerException {
    return super.listUsers();
  }


  /**
   * ACHTUNG: Hier wird �berpr�ft, ob die Rolle des Aufrufers das Recht USER_MANAGEMENT oder
   * USER_MANAGEMENT_EDIT_OWN hat. Es wird jedoch nicht �berpr�ft, ob bei USER_MANAGEMENT_EDIT_OWN 
   * auch "Aufrufer = zu �ndernder User" gilt (da an dieser Stelle nur noch die Rolle und nicht
   * mehr der Aufrufer bekannt ist).
   * D.h. es ist m�glich mit dieser Methode das Passwort eines andern Benutzers
   * zu �ndern, obwohl man nur das Recht USER_MANAGEMENT_EDIT_OWN hat.
   * Aus dem XynaBlackEditionWebService wird die Methode im Moment nur mit "Aufrufer = zu �ndernder User" 
   * aufgerufen.
   */
  public boolean changePassword(String id, String oldPassword, String newPassword, boolean isNewPasswordHashed, Role role) throws XynaException {
    if (!hasRight(Rights.USER_MANAGEMENT.toString(), role)) {
      checkRights("changePassword", role);
    }
    return super.changePassword(id, oldPassword, newPassword, isNewPasswordHashed);
  }


  // this is actually no useCase, we call this to get our role, we need to call it without role
  public User authenticate(String id, String password, Role role) throws XynaException {
    return super.authenticate(id, password);
  }


  public boolean resetPassword(String id, String newPassword, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("resetPassword"), role)) {
      return super.resetPassword(id, newPassword);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("resetPassword", role.getName());
    }
  }


  public boolean setPassword(String id, String newPassword, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("setPassword"), role)) {
      return super.setPassword(id, newPassword);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setPassword", role.getName());
    }
  }


  public boolean setPasswordHash(String id, String newPassword, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("setPasswordHash"), role)) {
      return super.setPasswordHash(id, newPassword);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setPasswordHash", role.getName());
    }
  }


  public boolean changeRole(String id, String name, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("changeRole"), role)) {
      return super.changeRole(id, name);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("changeRole", role.getName());
    }
  }


  public boolean createRole(String name, String domain, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("createRole"), role)) {
      return super.createRole(name, domain);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("createRole", role.getName());
    }
  }


  public boolean deleteRole(String name, String domain, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("deleteRole"), role)) {
      return super.deleteRole(name, domain);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deleteRole", role.getName());
    }
  }


  public boolean grantRightToRole(String roleName, String right, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("grantRightToRole"), role)) {
      return super.grantRightToRole(roleName, right);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("grantRightToRole", role.getName());
    }
  }


  public boolean revokeRightFromRole(String roleName, String right, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("revokeRightFromRole"), role)) {
      return super.revokeRightFromRole(roleName, right);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("revokeRightFromRole", role.getName());
    }
  }


  public boolean createRight(String rightName, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("createRight"), role)) {
      return super.createRight(rightName);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("createRight", role.getName());
    }
  }


  public boolean deleteRight(String rightName, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("deleteRight"), role)) {
      return super.deleteRight(rightName);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deleteRight", role.getName());
    }
  }


  public Collection<Right> getRights(Role role, String language) throws XynaException {
    if (hasRight(resolveFunctionToRight("getRights"), role)) {
      return super.getRights(language);
    } else {
      throw new XFMG_ACCESS_VIOLATION("getRights", role.getName());
    }
  }
  
  
  public Collection<RightScope> getRightScopes(Role role, String language) throws XynaException {
    if (hasRight(resolveFunctionToRight("getRightScopes"), role)) {
      return super.getRightScopes(language);
    } else {
      throw new XFMG_ACCESS_VIOLATION("getRightScopes", role.getName());
    }
  }


  public Collection<Role> getRoles(Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("getRoles"), role)) {
      return super.getRoles();
    } else {
      throw new XFMG_ACCESS_VIOLATION("getRoles", role.getName());
    }
  }


  public Collection<User> getUser(Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("getUser"), role)) {
      return super.getUser();
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("getUser", role.getName());
    }
  }


  public void listenToMdmModifications(String sessionId, ANotificationConnection con, Role role)
                  throws PersistenceLayerException {
    super.listenToMdmModifications(sessionId, con);
  }


  public void listenToProcessProgress(String sessionId, ANotificationConnection con, Long orderId, Role role)
                  throws PersistenceLayerException {
    super.listenToProcessProgress(sessionId, con, orderId);
  }


  public FrequencyControlledTaskSearchResult searchFrequencyControlledTask(FrequencyControlledTaskSelect select,
                                                                           int maxRows, Role authenticate)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("searchFrequencyControlledTask"), authenticate)) {
      return super.searchFrequencyControlledTasks(select, maxRows);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("searchFrequencyControlledTask", authenticate.getName());
    }
  }


  public OrderInstanceResult search(OrderInstanceSelect select, int maxRows, Role authenticate) throws XynaException {
    if (hasRight(resolveFunctionToRight("search"), authenticate)) {
      return super.search(select, maxRows);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("search", authenticate.getName());
    }
  }
  
  
  public OrderInstanceResult searchOrderInstances(OrderInstanceSelect select, int maxRows, SearchMode searchMode, Role authenticate) throws XynaException {
    if (hasRight(resolveFunctionToRight("searchOrderInstances"), authenticate)) {
      return super.searchOrderInstances(select, maxRows, searchMode);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("searchOrderInstances", authenticate.getName());
    }
  }


  /*public OrderInstanceResult searchAndAppendChildren(OrderInstanceSelect select, int maxRows, Role authenticate)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("searchAndAppendChildren"), authenticate)) {
      return super.searchAndAppendChildren(select, maxRows);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("searchAndAppendChildren", authenticate.getName());
    }
  }


  public OrderInstanceResult searchAndAppendHierarchy(OrderInstanceSelect select, int maxRows, Role authenticate)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("searchAndAppendHierarchy"), authenticate)) {
      return super.searchAndAppendHierarchy(select, maxRows);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("searchAndAppendHierarchy", authenticate.getName());
    }
  }*/


  public OrderInstanceDetails getCompleteOrder(long id, Role authenticate) throws XynaException {
    if (hasRight(resolveFunctionToRight("getCompleteOrder"), authenticate)) {
      return super.getCompleteOrder(id);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("getCompleteOrder", authenticate.getName());
    }
  }


  public Map<DestinationKey, DestinationValue> getDestinations(DispatcherIdentification dispatcherId, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("getDestinations"), role)) {
      return super.getDestinations(dispatcherId);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("getDestinations", role.getName());
    }
  }


  public List<DispatcherEntry> listDestinations(DispatcherIdentification dispatcherId, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("listDestinations"), role)) {
      return super.listDestinations(dispatcherId);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("listDestinations", role.getName());
    }
  }


  public DispatcherEntry getDestination(DispatcherIdentification dispatcherId, DestinationKey dk, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("getDestination"), role)) {
      return super.getDestination(dispatcherId, dk);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("getDestination", role.getName());
    }
  }


  public void removeDestination(DispatcherIdentification dispatcherId, DestinationKey dk, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("removeDestination"), role)) {
      super.removeDestination(dispatcherId, dk);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("removeDestination", role.getName());
    }
  }


  public void setDestination(DispatcherIdentification dispatcherId, DestinationKey dk, DestinationValue dv, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("setDestination"), role)) {
      super.setDestination(dispatcherId, dk, dv);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setDestination", role.getName());
    }
  }


  public String[] scanLogForLinesOfOrder(long orderId, int lineOffset, int maxNumberOfLines, Role role,
                                         String... excludes) throws XynaException {
    return super.scanLogForLinesOfOrder(orderId, lineOffset, maxNumberOfLines, excludes);
  }


  public String retrieveLogForOrder(long orderId, int lineOffset, int maxNumberOfLines, Role role, String... excludes)
                  throws XynaException {
    return super.retrieveLogForOrder(orderId, lineOffset, maxNumberOfLines, excludes);
  }


  public long startFrequencyControlledTask(FrequencyControlledTaskCreationParameter creationParameter, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("startFrequencyControlledTask"), role)) {
      return super.startFrequencyControlledTask(creationParameter);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("startFrequencyControlledTask", role.getName());
    }
  }


  public boolean cancelFrequencyControlledTask(long taskId, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("cancelFrequencyControlledTask"), role)) {
      return super.cancelFrequencyControlledTask(taskId);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("cancelFrequencyControlledTask", role.getName());
    }
  }


  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(long taskId,
                                                                                  String[] selectedStatistics, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("getFrequencyControlledTaskInformation"), role)) {
      return super.getFrequencyControlledTaskInformation(taskId, selectedStatistics);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("getFrequencyControlledTaskInformation", role.getName());
    }
  }


  public User getUser(String useridentifier, Role role) throws XynaException {
    return super.getUser(useridentifier);
  }


  public Role getRole(String rolename, String domainname, Role role) throws XynaException {
    return super.getRole(rolename, domainname);
  }


  public Right getRight(String rightidentifier, Role role, String language) throws XynaException {
    return super.getRight(rightidentifier, language);
  }


  public Domain getDomain(String domainidentifier, Role role) throws XynaException {
    return super.getDomain(domainidentifier);
  }


  public boolean setLockedStateOfUser(String useridentifier, boolean newState, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("setLockedStateOfUser"), role)) {
      return super.setLockedStateOfUser(useridentifier, newState);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setLockedStateOfUser", role.getName());
    }
  }


  public boolean setDomainsOfUser(String useridentifier, List<String> domains, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("setDomainsOfUser"), role)) {
      return super.setDomainsOfUser(useridentifier, domains);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setDomainsOfUser", role.getName());
    }
  }


  public boolean setDescriptionOfRole(String rolename, String domainname, String newDescription, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("setDescriptionOfRole"), role)) {
      return super.setDescriptionOfRole(rolename, domainname, newDescription);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setDescriptionOfRole", role.getName());
    }
  }


  public boolean setAliasOfRole(String rolename, String domainname, String newAlias, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("setAliasOfRole"), role)) {
      return super.setAliasOfRole(rolename, domainname, newAlias);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setAliasOfRole", role.getName());
    }
  }


  public UserSearchResult searchUsers(UserSelect selection, int maxRows, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("searchUsers"), role)) {
      return super.searchUsers(selection, maxRows);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("searchUsers", role.getName());
    }
  }


  public Collection<Domain> getDomains(Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("getDomains"), role)) {
      return super.getDomains();
    } else {
      throw new XFMG_ACCESS_VIOLATION("getDomains", role.getName());
    }
  }


  public boolean createDomain(String domainidentifier, DomainType type, int maxRetries, int connectionTimeout, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("createDomain"), role)) {
      return super.createDomain(domainidentifier, type, maxRetries, connectionTimeout);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("createDomain", role.getName());
    }
  }


  public boolean setDomainSpecificDataOfDomain(String domainidentifier, DomainTypeSpecificData specificData, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("setDomainSpecificDataOfDomain"), role)) {
      return super.setDomainSpecificDataOfDomain(domainidentifier, specificData);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setDomainSpecificDataOfDomain", role.getName());
    }
  }


  public boolean setDescriptionOfRight(String rightidentifier, String description, Role role, String language) throws XynaException {
    if (hasRight(resolveFunctionToRight("setDescriptionOfRight"), role)) {
      return super.setDescriptionOfRight(rightidentifier, description, language);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setDescriptionOfRight", role.getName());
    }
  }


  public boolean setMaxRetriesOfDomain(String domainidentifier, int maxRetries, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("setMaxRetriesOfDomain"), role)) {
      return super.setMaxRetriesOfDomain(domainidentifier, maxRetries);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setMaxRetriesOfDomain", role.getName());
    }
  }


  public boolean setConnectionTimeoutOfDomain(String domainidentifier, int connectionTimeout, Role role)
                  throws XynaException {
    if (hasRight(resolveFunctionToRight("setConnectionTimeoutOfDomain"), role)) {
      return super.setConnectionTimeoutOfDomain(domainidentifier, connectionTimeout);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setConnectionTimeoutOfDomain", role.getName());
    }
  }


  public boolean setDescriptionOfDomain(String domainidentifier, String description, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("setDescriptionOfDomain"), role)) {
      return super.setDescriptionOfDomain(domainidentifier, description);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("setDescriptionOfDomain", role.getName());
    }
  }


  public boolean deleteDomain(String domainidentifier, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("deleteDomain"), role)) {
      return super.deleteDomain(domainidentifier);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deleteDomain", role.getName());
    }
  }


  public String listDomains(Role role) throws XynaException {
    return super.listDomains();
  }


  public void installDeliveryItem(InputStream deliveryItem, OutputStream out, boolean forceOverwrite,
                                  boolean dontUpdateMdm, boolean verboseOutput, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("installDeliveryItem"), role)) {
      try {
        super.installDeliveryItem(deliveryItem, out, forceOverwrite, dontUpdateMdm, verboseOutput);
      }
      catch (IOException e) {
        throw new Ex_FileAccessException("unknown", e);
      }
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("installDeliveryItem", role.getName());
    }
  }


  public void createDeliveryItem(InputStream packageDefininition, OutputStream deliveryItem, OutputStream out,
                                 boolean verboseOutput, boolean includeXynaComponents, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("createDeliveryItem"), role)) {
      try {
        super.createDeliveryItem(packageDefininition, deliveryItem, out, verboseOutput, includeXynaComponents);
      }
      catch (IOException e) {
        throw new Ex_FileAccessException("unknown", e);
      }
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("createDeliveryItem", role.getName());
    }
  }

  @Deprecated
  public void deleteWorkflow(String originalFqName, boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                             boolean deleteDependencies, Role role) throws XynaException {
    deleteWorkflow(originalFqName, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, RevisionManagement.REVISION_DEFAULT_WORKSPACE, role);
  }
  
  public void deleteWorkflow(String originalFqName, boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                             boolean deleteDependencies, Long revision, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("deleteWorkflow"), role)) {
      super.deleteWorkflow(originalFqName, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deleteWorkflow", role.getName());
    }
  }

  @Deprecated
  public void deleteDatatype(String originalFqName, boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                             boolean deleteDependencies, Role role) throws XynaException {
    deleteDatatype(originalFqName, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, RevisionManagement.REVISION_DEFAULT_WORKSPACE, role);
  }
  
  public void deleteDatatype(String originalFqName, boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                             boolean deleteDependencies, Long revision, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("deleteDatatype"), role)) {
      super.deleteDatatype(originalFqName, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deleteDatatype", role.getName());
    }
  }

  @Deprecated
  public void deleteException(String originalFqName, boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                              boolean deleteDependencies, Role role) throws XynaException {
    deleteException(originalFqName, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, RevisionManagement.REVISION_DEFAULT_WORKSPACE, role);
  }
  
  public void deleteException(String originalFqName, boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                              boolean deleteDependencies, Long revision, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("deleteDatatype"), role)) {
      super.deleteException(originalFqName, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deleteDatatype", role.getName());
    }
  }
  
  @Deprecated
  public void deleteXMOMObject(XMOMType type, String originalFqName,
                               boolean recursivlyUndeploy, boolean recursivlyDelete, String user, String sessionId, Role role) throws XynaException {
    deleteXMOMObject(type, originalFqName, recursivlyUndeploy, recursivlyDelete, user, sessionId, RevisionManagement.REVISION_DEFAULT_WORKSPACE, role);
  }
  
  public void deleteXMOMObject(XMOMType type, String originalFqName,
                               boolean recursivlyUndeploy, boolean recursivlyDelete, String user,
                               String sessionId, Long revision, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("deleteXMOMObject"), role)) {
      super.deleteXMOMObject(type, originalFqName, recursivlyUndeploy, recursivlyDelete, user, sessionId, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deleteXMOMObject", role.getName());
    }
  }

  public void deleteXMOMObject(XMOMType type, String originalFqName,
                               DependentObjectMode dependentObjectMode, String user,
                               String sessionId, Long revision, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("deleteXMOMObject"), role)) {
      super.deleteXMOMObject(originalFqName, type, dependentObjectMode, user, sessionId, revision);
    }
    else {
      throw new XFMG_ACCESS_VIOLATION("deleteXMOMObject", role.getName());
    }
  }

  @Deprecated
  public XMOMDatabaseSearchResult searchXMOMDatabase(List<XMOMDatabaseSelect> selects, int maxRows, Role role)
                  throws XynaException {
    return searchXMOMDatabase(selects, maxRows, RevisionManagement.REVISION_DEFAULT_WORKSPACE, role);
  }
  
  public XMOMDatabaseSearchResult searchXMOMDatabase(List<XMOMDatabaseSelect> selects, int maxRows, Long revision, Role role)
                  throws XynaException {
    return super.searchXMOMDatabase(selects, maxRows, revision);
  }


  public List<CapacityMappingStorable> getAllCapacityMappings(Role role) {
    return super.getAllCapacityMappings();
  }


  public CapacityInformation getCapacityInformation(String capacityName, Role role) {
    return super.getCapacityInformation(capacityName);
  }


  public Collection<VetoInformationStorable> listVetoInformation(Role role) throws PersistenceLayerException {
    return super.listVetoInformation();
  }


  public void createOrdertype(OrdertypeParameter ordertypeParameter, Role role) throws XynaException,
                  XFMG_InvalidCreationOfExistingOrdertype {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.ORDER_TYPE, Action.insert);
    checkScopedRights(scopedRight, role);
    super.createOrdertype(ordertypeParameter);
  }


  public void modifyOrdertype(OrdertypeParameter ordertypeParameter, Role role) throws XynaException,
                  XFMG_InvalidModificationOfUnexistingOrdertype {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.ORDER_TYPE, Action.write);
    checkScopedRights(scopedRight, role);
    super.modifyOrdertype(ordertypeParameter);
  }


  public void deleteOrdertype(OrdertypeParameter ordertypeParameter, Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.ORDER_TYPE, Action.delete);
    checkScopedRights(scopedRight, role);
    super.deleteOrdertype(ordertypeParameter);
  }


  public List<OrdertypeParameter> listOrdertypes(Role role, RuntimeContext runtimeContext) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.ORDER_TYPE, Action.read);
    checkScopedRights(scopedRight, role);
    return super.listOrdertypes(runtimeContext);
  }
  
  
  public List<OrdertypeParameter> listOrdertypes(Role role, SearchOrdertypeParameter sop) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.ORDER_TYPE, Action.read);
    checkScopedRights(scopedRight, role);
    return super.listOrdertypes(sop);
  }
  
  
  public void allocateAdministrativeVeto(String vetoName, String documentation, Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.VETO, Action.insert);
    checkScopedRights(scopedRight, role);
    super.allocateAdministrativeVeto(vetoName, documentation);
  }
  
  public void freeAdministrativeVeto(String vetoName, Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.VETO, Action.delete);
    checkScopedRights(scopedRight, role);
    super.freeAdministrativeVeto(vetoName);
  }
  
  public void setDocumentationOfAdministrativeVeto(String vetoName, String documentation, Role role) 
      throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.VETO, Action.write);
    checkScopedRights(scopedRight, role);
    super.setDocumentationOfAdministrativeVeto(vetoName, documentation);
  }
  
  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows, Role role) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.VETO, Action.read);
    checkScopedRights(scopedRight, role);
    return super.searchVetos(select, maxRows);
  }

  public Map<Long, ClusterInformation> listClusterInstances(Role role) throws XynaException {
    return super.listClusterInstances();
  }
  
  public Collection<ApplicationInformation> listApplications(Role role)  throws XynaException {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement().listApplications();
  }
  
  
  public KillStuckProcessBean killStuckProcess(Long orderId, boolean forceKill, AbortionCause reason, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("killStuckProcess"), role)) {
      return super.killStuckProcess(orderId, forceKill, reason);
    } else {
      throw new XFMG_ACCESS_VIOLATION("killStuckProcess", role.getName());
    }
  }


  public RefactoringResult refactorXMOM(RefactoringActionParameter action, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("refactorXMOM"), role)) {
      return super.refactorXMOM(action);
    } else {
      throw new XFMG_ACCESS_VIOLATION("refactorXMOM", role.getName());
    }
  }
  
  
  public Long publish(MessageInputParameter message, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("publish"), role)) {
      return super.publish(message);
    } else {
      throw new XFMG_ACCESS_VIOLATION("publish", role.getName());
    }
  }

  
  public boolean addSubscription(String subscriptionSessionId, MessageSubscriptionParameter subscription, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("addSubscription"), role)) {
      return super.addSubscription(subscriptionSessionId, subscription);
    } else {
      throw new XFMG_ACCESS_VIOLATION("addSubscription", role.getName());
    }
  }

  
  public boolean cancelSubscription(String subscriptionSessionId, Long subscriptionId, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("cancelSubscription"), role)) {
      return super.cancelSubscription(subscriptionSessionId, subscriptionId);
    } else {
      throw new XFMG_ACCESS_VIOLATION("cancelSubscription", role.getName());
    }
  }

  
  public MessageRetrievalResult fetchMessages(String subscriptionSessionId, Long lastReceivedId, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("fetchMessages"), role)) {
      return super.fetchMessages(subscriptionSessionId, lastReceivedId);
    } else {
      throw new XFMG_ACCESS_VIOLATION("fetchMessages", role.getName());
    }
  }

  @Deprecated
  public boolean lockXMOM(String sessionId, String creator, String path, String type, Role role) throws XynaException {
    return lockXMOM(sessionId, creator, new Path(path, RevisionManagement.REVISION_DEFAULT_WORKSPACE), type, role);
  }
  
  public boolean lockXMOM(String sessionId, String creator, Path path, String type, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("lockXMOM"), role)) {
      return super.lockXMOM(sessionId, creator, path, type);
    } else {
      throw new XFMG_ACCESS_VIOLATION("lockXMOM", role.getName());
    }
  }

  @Deprecated
  public boolean unlockXMOM(String sessionId, String creator, String path, String type, Role role) throws XynaException {
    return unlockXMOM(sessionId, creator, new Path(path, RevisionManagement.REVISION_DEFAULT_WORKSPACE), type, role);
  }
  
  
  public boolean unlockXMOM(String sessionId, String creator, Path path, String type, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("unlockXMOM"), role)) {
      return super.unlockXMOM(sessionId, creator, path, type);
    } else {
      throw new XFMG_ACCESS_VIOLATION("unlockXMOM", role.getName());
    }
  }

  @Deprecated
  public void publishXMOM(String sessionId, String creator, String path, String type, String payload,
                          Long autosaveCounter, Role role) throws XynaException {
    publishXMOM(sessionId, creator, new Path(path, RevisionManagement.REVISION_DEFAULT_WORKSPACE), type, payload, autosaveCounter, role);
  }
  
  public void publishXMOM(String sessionId, String creator, Path path, String type, String payload,
                          Long autosaveCounter, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("publishXMOM"), role)) {
      super.publishXMOM(sessionId, creator, path, type, payload, autosaveCounter);
    } else {
      throw new XFMG_ACCESS_VIOLATION("publishXMOM", role.getName());
    }
  }


  public void addTimeWindow(TimeConstraintWindowDefinition definition, Role authenticate) throws XynaException {
    super.addTimeWindow(definition);
  }

  public void removeTimeWindow(String name, boolean force, Role authenticate) throws XynaException {
    super.removeTimeWindow(name,force);
  }

  public void changeTimeWindow(TimeConstraintWindowDefinition definition, Role authenticate) throws XynaException {
    super.changeTimeWindow(definition);
  }
  
  
  public void buildApplicationVersion(String applicationName, String versionName, String comment, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("buildApplicationVersion"), role)) {
      super.buildApplicationVersion(applicationName, versionName, comment);
    } else {
      throw new XFMG_ACCESS_VIOLATION("buildApplicationVersion", role.getName());
    }
  }

  public void buildApplicationVersion(String applicationName, String versionName, BuildApplicationVersionParameters params, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("buildApplicationVersion"), role)) {
      super.buildApplicationVersion(applicationName, versionName, params);
    } else {
      throw new XFMG_ACCESS_VIOLATION("buildApplicationVersion", role.getName());
    }
  }
  
  
  @Deprecated
  public void copyApplicationIntoWorkingSet(String applicationName, String versionName, String comment, boolean overrideChanges, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("copyApplicationIntoWorkingSet"), role)) {
      super.copyApplicationIntoWorkingSet(applicationName, versionName, comment, overrideChanges);
    } else {
      throw new XFMG_ACCESS_VIOLATION("copyApplicationIntoWorkingSet", role.getName());
    }
  }

  public void copyApplicationIntoWorkspace(String applicationName, String versionName, CopyApplicationIntoWorkspaceParameters params, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("copyApplicationIntoWorkspace"), role)) {
      super.copyApplicationIntoWorkspace(applicationName, versionName, params);
    } else {
      throw new XFMG_ACCESS_VIOLATION("copyApplicationIntoWorkspace", role.getName());
    }
  }
  
  

  public List<FactoryNodeStorable> getAllFactoryNodes(Role authenticate) {
    return super.getAllFactoryNodes();
  }
  
  
  public List<SharedLib> listAllSharedLibs(Role authenticate) {
    return super.listAllSharedLibs();
  }

  public List<SharedLib> listSharedLibs(Long revision, boolean withContent, Role authenticate) {
    return super.listSharedLibs(revision, withContent);
  }

  public List<WorkspaceInformation> listWorkspaces(Role role, boolean includeProblems) throws PersistenceLayerException {
    UserManagement um = getUserManagement();
    List<WorkspaceInformation> workspaces = super.listWorkspaces(includeProblems);
    if (hasRight(ScopedRight.WORKSPACE.getKey() + ":" + Action.list + ":*", role)) {
      return workspaces;
    }
    List<WorkspaceInformation> allowedWorkspaces = new ArrayList<WorkspaceInformation>();
    for (WorkspaceInformation ws : workspaces) {
      if (hasRight(um.getScopedRight(ScopedRight.WORKSPACE,  Action.list, ws.getWorkspace().getName()), role)) {
        allowedWorkspaces.add(ws);
      }
    }
    return allowedWorkspaces;
  }

  public List<ApplicationDefinitionInformation> listApplicationDefinitions(Role authenticate, boolean includeProblems) {
    return super.listApplicationDefinitions(includeProblems);
  }

  public CreateWorkspaceResult createWorkspace(Workspace workspace, Role role) throws XynaException {
    return createWorkspace(workspace, null, role);
  }

  public CreateWorkspaceResult createWorkspace(Workspace workspace, String user, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("createWorkspace"), role)) {
      return super.createWorkspace(workspace, user);
    } else {
      throw new XFMG_ACCESS_VIOLATION("createWorkspace", role.getName());
    }
  }

  public void removeWorkspace(Workspace workspace, RemoveWorkspaceParameters params, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("removeWorkspace"), role)) {
      super.removeWorkspace(workspace, params);
    } else {
      throw new XFMG_ACCESS_VIOLATION("removeWorkspace", role.getName());
    }
  }

  public void instantiateRepositoryAccessInstance(InstantiateRepositoryAccessParameters parameters, Long revision, Role authenticate) throws XDEV_CodeAccessInitializationException {
    super.instantiateRepositoryAccessInstance(parameters, revision);
  }

  public DataModelResult importDataModel(ImportDataModelParameters parameters, Role authenticate) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.DATA_MODEL, Action.insert);
    checkScopedRights(scopedRight, authenticate);
    return super.importDataModel(parameters);
  }

  public DataModelResult removeDataModel(RemoveDataModelParameters parameters, Role authenticate) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.DATA_MODEL, Action.delete);
    checkScopedRights(scopedRight, authenticate);
    return super.removeDataModel(parameters);
  }
  
  public DataModelResult modifyDataModel(ModifyDataModelParameters parameters, Role authenticate) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.DATA_MODEL, Action.write);
    checkScopedRights(scopedRight, authenticate);
    return super.modifyDataModel(parameters);
  }

  public SearchResult<?> search(SearchRequestBean searchRequest) throws XynaException {
    return super.search(searchRequest);
  }

  public SearchResult<?> search(SearchRequestBean searchRequest, Role role) throws XynaException {
    ScopedRight scopedRight = null;
    switch (searchRequest.getArchiveIdentifier()) {
      case datamodel:
        scopedRight = ScopedRight.DATA_MODEL;
        break;
      case deploymentitem:
        scopedRight = ScopedRight.DEPLOYMENT_ITEM;
        break;
      case orderInputSource:
        scopedRight = ScopedRight.ORDER_INPUT_SOURCE;
        break;
      default :
        scopedRight = null;
    }
    
    if (scopedRight != null) {
      String scopedRightString = getUserManagement().getScopedRight(scopedRight, Action.read);
      checkScopedRights(scopedRightString, role);
    }
    //TODO Ergebnis anhand der Rechte filtern
    return super.search(searchRequest);
  }
  
  
  public boolean enableTriggerInstance(String triggerInstanceName, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("enableTriggerInstance"), role)) {
      return super.enableTriggerInstance(triggerInstanceName);
    } else {
      throw new XFMG_ACCESS_VIOLATION("enableTriggerInstance", role.getName());
    }
  }
  
  
  public boolean disableTriggerInstance(String triggerInstanceName, Role role) throws XynaException {
    if (hasRight(resolveFunctionToRight("disableTriggerInstance"), role)) {
      return super.disableTriggerInstance(triggerInstanceName);
    } else {
      throw new XFMG_ACCESS_VIOLATION("disableTriggerInstance", role.getName());
    }
  }
  
  
  public List<FilterInformation> listFilterInformation(Role role) throws XynaException {
    return super.listFilterInformation();
  }
  
  
  public List<TriggerInformation> listTriggerInformation(Role role) throws XynaException {
    return super.listTriggerInformation();
  }


  public DeploymentMarker createDeploymentMarker(Role authenticate, DeploymentMarker marker) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.DEPLOYMENT_MARKER, Action.insert);
    checkScopedRights(scopedRight, authenticate);
    return super.createDeploymentMarker(marker);
  }

  public void deleteDeploymentMarker(Role authenticate, DeploymentMarker marker) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.DEPLOYMENT_MARKER, Action.delete);
    checkScopedRights(scopedRight, authenticate);
    super.deleteDeploymentMarker(marker);
  }
  
  public void modifyDeploymentMarker(Role authenticate, DeploymentMarker marker) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.DEPLOYMENT_MARKER, Action.write);
    checkScopedRights(scopedRight, authenticate);
    super.modifyDeploymentMarker(marker);
  }

  public void createOrderInputSource(Role authenticate, OrderInputSourceStorable inputSource) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.ORDER_INPUT_SOURCE, Action.insert);
    checkScopedRights(scopedRight, authenticate);
    super.createOrderInputSource(inputSource);
  }

  public XynaOrderCreationParameter generateOrderInput(Role authenticate, long inputSourceId) throws XynaException {
    return generateOrderInput(authenticate, inputSourceId, new OptionalOISGenerateMetaInformation());
  }

  public XynaOrderCreationParameter generateOrderInput(Role authenticate, long inputSourceId,
                                                       OptionalOISGenerateMetaInformation parameters) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.ORDER_INPUT_SOURCE, Action.generate);
    checkScopedRights(scopedRight, authenticate);
    return super.generateOrderInput(inputSourceId, parameters);
  }
  
  public List<PluginDescription> listPluginDescriptions(PluginType type, Role authenticate) {
    return super.listPluginDescriptions(type);
  }

  public void modifyOrderInputSource(Role authenticate, OrderInputSourceStorable inputSource) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.ORDER_INPUT_SOURCE, Action.write);
    checkScopedRights(scopedRight, authenticate);
    super.modifyOrderInputSource(inputSource);
  }

  public void deleteOrderInputSource(Role authenticate, long inputSourceId) throws XynaException {
    String scopedRight = getUserManagement().getScopedRight(ScopedRight.ORDER_INPUT_SOURCE, Action.delete);
    checkScopedRights(scopedRight, authenticate);
    super.deleteOrderInputSource(inputSourceId);
  }

  public PasswordExpiration getPasswordExpiration(Role authenticate, String userName) throws XynaException {
    return super.getPasswordExpiration(userName);
  }

  public void modifyRuntimeContextDependencies(Role role, RuntimeDependencyContext owner, List<RuntimeDependencyContext> newDependencies, boolean force, String user) throws XynaException {
    UserManagement um = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement();
    String right;
    if (owner instanceof Application) {
      right = um.getApplicationRight(owner.getName(), ((Application) owner).getVersionName(), Action.write);
    } else if (owner instanceof Workspace) {
      right = UserManagement.Rights.WORKINGSET_MANAGEMENT.name();
    } else if (owner instanceof ApplicationDefinition) {
      right = UserManagement.Rights.WORKINGSET_MANAGEMENT.name();
    } else {
      throw new RuntimeException("Unsupported runtime context: " + owner.getClass().getName());
    }
    if (!um.hasRight(right, role)) {
      throw new XFMG_ACCESS_VIOLATION(right, role.getName());
    }
    super.modifyRuntimeContextDependencies(owner, newDependencies, force, user);
  }


  public void setUserContextValue(String sessionId, String key, String value) throws PersistenceLayerException {
    XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement()
        .setUserContextValueBySession(sessionId, key, value);
  }


  public void resetUserContextValues(String sessionId) throws PersistenceLayerException {
    XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement()
        .resetUserContextValuesBySession(sessionId);
  }


  public List<SessionBasedUserContextValue> getUserContextValues(String sessionId) throws PersistenceLayerException {
    return XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement()
        .getUserContextValuesBySession(sessionId);
  }
}
