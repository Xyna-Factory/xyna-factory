/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.appmgmt;



import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.exceptions.XFMG_ObjectUnkownInDeploymentItemStateManagement;
import com.gip.xyna.xnwh.exceptions.XFMG_WrongDeploymentState;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMapping;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;



public class DataProvider {

  private GetRevision getRevision;
  private GetPriority getPriority;
  private GetCapacities getCapacities;
  private GetRequirements getRequirements;
  private GetRuntimeContext getRuntimeContext;
  private GetMonitoringLevel getMonitoringLevel;
  private QueryAppDefStorables queryAppDefStorables;
  private FindDependenciesForStub findDependenciesForStub;
  private ActivationTriggerProvider activationTriggerProvider;
  private XynaDispatcherProvider planningDispatcher;
  private XynaDispatcherProvider executionDispatcher;
  private XynaDispatcherProvider cleanupDispatcher;
  private GetListInheritanceRules getListInheritanceRules;
  private GetlistApplicationDetails getListApplicationDetails;
  private GetAllMappingsForRootType getAllMappingsForRootType;
  private GetCheckDeploymentItemState getCheckDeploymentItemState;
  private GetIsDestinationKeyConfiguredForOrderContextMapping getIsDestinationKeyConfiguredForOrderContextMapping;
  private GetGlobalCapacities globaleCapacities;
  private QueryRuntimeAppStorablesProvider queryRuntimeAppStorablesProvider;
  private XynaPropertiesProvider xynaPropertiesProvider;


  public void validate() {
    if (getRevision == null || getPriority == null || getCapacities == null || getRequirements == null || getRuntimeContext == null
        || getMonitoringLevel == null || queryAppDefStorables == null || findDependenciesForStub == null || planningDispatcher == null
        || executionDispatcher == null || cleanupDispatcher == null || getListInheritanceRules == null || getListApplicationDetails == null
        || getAllMappingsForRootType == null || getCheckDeploymentItemState == null
        || getIsDestinationKeyConfiguredForOrderContextMapping == null || activationTriggerProvider == null || globaleCapacities == null 
        || queryRuntimeAppStorablesProvider == null || xynaPropertiesProvider == null) {
      throw new RuntimeException();
    }
  }

  public String getXynaPropertyValue(String propertyName) {
    return xynaPropertiesProvider.getPropertyValue(propertyName);
  }
  
  public void setXynaPropertyProvider(XynaPropertiesProvider xynaPropertiesProvider) {
    this.xynaPropertiesProvider = xynaPropertiesProvider;
  }
  
  public void setGlobalRuntimeAppStorablesProvider(QueryRuntimeAppStorablesProvider queryRuntimeAppStorablesProvider) {
    this.queryRuntimeAppStorablesProvider = queryRuntimeAppStorablesProvider;
  }
  
  public List<ApplicationEntryStorable> queryAllRuntimeApplicationStorables(String application, String version) throws PersistenceLayerException {
    return queryRuntimeAppStorablesProvider.queryAllRuntimeApplicationStorables(application, version);
  }

  public GetGlobalCapacities getGetGlobalCapacities() {
    return globaleCapacities;
  }

  public CapacityInformation getCapacity(String capacityName) {
    return globaleCapacities.getCapacity(capacityName);
  }

  public void setGetGlobalCapacities(GetGlobalCapacities globaleCapacities) {
    this.globaleCapacities = globaleCapacities;
  }


  public DataProvider() {
  }


  public QueryAppDefStorables getQueryAppDefStorables() {
    return queryAppDefStorables;
  }


  public void setQueryAppDefStorables(QueryAppDefStorables queryAppDefStorables) {
    this.queryAppDefStorables = queryAppDefStorables;
  }
  
  public List<ApplicationEntryStorable> queryAppDefStorables(String applicationName, Long parentRevision) throws PersistenceLayerException {
    return queryAppDefStorables.apply(applicationName, parentRevision);
  }


  public FindDependenciesForStub getFindDependenciesForStub() {
    return findDependenciesForStub;
  }


  public void setFindDependenciesForStub(FindDependenciesForStub findDependenciesForStub) {
    this.findDependenciesForStub = findDependenciesForStub;
  }
  

  public Collection<ApplicationEntryStorable> findDependenciesForStub(List<? extends ApplicationEntryStorable> appEntries, Long revision,
                                                                      Set<Long> revisionsToKeep, Application app) {
    return findDependenciesForStub.apply(appEntries, revision, revisionsToKeep, app);
  }


  public GetRevision getGetRevision() {
    return getRevision;
  }


  public void setGetRevision(GetRevision getRevision) {
    this.getRevision = getRevision;
  }
  

  public Long getRevision(String applicationName, String versionName, String workspaceName) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return getRevision.apply(applicationName, versionName, workspaceName);
  }


  public GetRuntimeContext getGetRuntimeContext() {
    return getRuntimeContext;
  }
  
  public RuntimeContext getRuntimeContext(Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return getRuntimeContext.apply(revision);
  }


  public void setGetRuntimeContext(GetRuntimeContext getRuntimeContext) {
    this.getRuntimeContext = getRuntimeContext;
  }


  public ActivationTriggerProvider getActivationTriggerProvider() {
    return activationTriggerProvider;
  }


  public void setActivationTriggerProvider(ActivationTriggerProvider activationTriggerProvider) {
    this.activationTriggerProvider = activationTriggerProvider;
  }


  public GetPriority getGetPriority() {
    return getPriority;
  }


  public void setGetPriority(GetPriority getPriority) {
    this.getPriority = getPriority;
  }


  public GetCapacities getGetCapacities() {
    return getCapacities;
  }


  public void setGetCapacities(GetCapacities getCapacities) {
    this.getCapacities = getCapacities;
  }


  public GetMonitoringLevel getGetMonitoringLevel() {
    return getMonitoringLevel;
  }


  public void setGetMonitoringLevel(GetMonitoringLevel getMonitoringLevel) {
    this.getMonitoringLevel = getMonitoringLevel;
  }


  public XynaDispatcherProvider getPlanningDispatcher() {
    return planningDispatcher;
  }


  public void setPlanningDispatcher(XynaDispatcherProvider planningDispatcher) {
    this.planningDispatcher = planningDispatcher;
  }


  public XynaDispatcherProvider getExecutionDispatcher() {
    return executionDispatcher;
  }


  public void setExecutionDispatcher(XynaDispatcherProvider executionDispatcher) {
    this.executionDispatcher = executionDispatcher;
  }


  public XynaDispatcherProvider getCleanupDispatcher() {
    return cleanupDispatcher;
  }


  public void setCleanupDispatcher(XynaDispatcherProvider cleanupDispatcher) {
    this.cleanupDispatcher = cleanupDispatcher;
  }


  public GetListInheritanceRules getGetListInheritanceRules() {
    return getListInheritanceRules;
  }


  public void setGetListInheritanceRules(GetListInheritanceRules getListInheritanceRules) {
    this.getListInheritanceRules = getListInheritanceRules;
  }

  public List<ApplicationEntryStorable> listApplicationDetails(String appName, String version, boolean includingDependencies,
                                                               List<String> excludeSubtypesOf, Long parentRev) {
    return getListApplicationDetails.listApplicationDetails(appName, version, includingDependencies, excludeSubtypesOf, parentRev);
  }


  public void setGetListApplicationDetails(GetlistApplicationDetails getListApplicationDetails) {
    this.getListApplicationDetails = getListApplicationDetails;
  }


  public GetRequirements getGetRequirements() {
    return getRequirements;
  }


  public void setGetRequirements(GetRequirements getRequirements) {
    this.getRequirements = getRequirements;
  }

  public Collection<RuntimeDependencyContext> getRequirements(RuntimeDependencyContext owner) {
    return getRequirements.getRequirements(owner);
  }

  public GetCheckDeploymentItemState getGetCheckDeploymentItemState() {
    return getCheckDeploymentItemState;
  }


  public void setGetCheckDeploymentItemState(GetCheckDeploymentItemState getCheckDeploymentItemState) {
    this.getCheckDeploymentItemState = getCheckDeploymentItemState;
  }


  public GetAllMappingsForRootType getGetAllMappingsForRootType() {
    return getAllMappingsForRootType;
  }


  public void setGetAllMappingsForRootType(GetAllMappingsForRootType getAllMappingsForRootType) {
    this.getAllMappingsForRootType = getAllMappingsForRootType;
  }


  public GetIsDestinationKeyConfiguredForOrderContextMapping getGetIsDestinationKeyConfiguredForOrderContextMapping() {
    return getIsDestinationKeyConfiguredForOrderContextMapping;
  }


  public void setGetIsDestinationKeyConfiguredForOrderContextMapping(GetIsDestinationKeyConfiguredForOrderContextMapping getIsDestinationKeyConfiguredForOrderContextMapping) {
    this.getIsDestinationKeyConfiguredForOrderContextMapping = getIsDestinationKeyConfiguredForOrderContextMapping;
  }


  @FunctionalInterface
  public interface QueryAppDefStorables {

    List<ApplicationEntryStorable> apply(String applicationName, Long parentRevision) throws PersistenceLayerException;
  }
  
  @FunctionalInterface
  public interface QueryRuntimeAppStorables {

    List<ApplicationEntryStorable> apply(String applicationName, String versionName) throws PersistenceLayerException;
  }

  @FunctionalInterface
  public interface FindDependenciesForStub {

    Collection<ApplicationEntryStorable> apply(List<? extends ApplicationEntryStorable> appEntries, Long revision,
                                               Set<Long> revisionsToKeep, Application app);
  }

  @FunctionalInterface
  public interface GetRuntimeContext {

    RuntimeContext apply(Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
  }

  @FunctionalInterface
  public interface GetRevision {

    Long apply(String applicationName, String versionName, String workspaceName) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
  }

  @FunctionalInterface
  public interface GetPriority {

    Integer getPriority(String orderType, Long revision);
  }

  @FunctionalInterface
  public interface GetCapacities {

    List<Capacity> getCapacities(DestinationKey dk);
  }

  @FunctionalInterface
  public interface GetMonitoringLevel {

    Integer getMonitoringLevel(final DestinationKey dk);
  }

  @FunctionalInterface
  public interface GetListInheritanceRules {

    Map<ParameterType, List<InheritanceRule>> listInheritanceRules(DestinationKey destinationKey);
  }

  @FunctionalInterface
  public interface GetRequirements {

    Collection<RuntimeDependencyContext> getRequirements(RuntimeDependencyContext owner);
  }

  @FunctionalInterface
  public interface GetlistApplicationDetails {

    List<ApplicationEntryStorable> listApplicationDetails(String applicationName, String version, boolean includingDependencies,
                                                          List<String> excludeSubtypesOf, Long parentRevision);
  }

  @FunctionalInterface
  public interface GetCheckDeploymentItemState {

    void checkDeploymentItemState(String uniqueName, XMOMType xmomType, Long revision, boolean throwExceptionIfInvalid)
        throws XFMG_WrongDeploymentState, XFMG_ObjectUnkownInDeploymentItemStateManagement;
  }

  @FunctionalInterface
  public interface GetAllMappingsForRootType {

    Collection<XMOMODSMapping> getAllMappingsForRootType(String name, Long revision) throws PersistenceLayerException;
  }

  @FunctionalInterface
  public interface GetIsDestinationKeyConfiguredForOrderContextMapping {

    boolean isDestinationKeyConfiguredForOrderContextMapping(DestinationKey dk, boolean ignoreGlobalProperty);
  }

  @FunctionalInterface
  public interface GetGlobalCapacities {

    CapacityInformation getCapacity(String name);
  }
  
  @FunctionalInterface
  public interface QueryRuntimeAppStorablesProvider {

    List<ApplicationEntryStorable> queryAllRuntimeApplicationStorables(String application, String version) throws PersistenceLayerException;
  }

  @FunctionalInterface
  public interface XynaPropertiesProvider {
    String getPropertyValue(String propertyName);
  }

  public interface ActivationTriggerProvider {

    Filter getFilter(Long revision, String filterName, boolean followRuntimeContextDependencies)
        throws PersistenceLayerException, XACT_FilterNotFound;


    FilterInstanceInformation getFilterInstanceInformation(String name, Long revision) throws PersistenceLayerException;


    Pair<Long, Boolean> getTriggerConfiguration(TriggerInstanceInformation triggerinstance) throws PersistenceLayerException;


    TriggerInstanceInformation getTriggerInstanceInformation(String name, Long revision) throws PersistenceLayerException;


    Trigger getTrigger(Long revision, String name, boolean b) throws PersistenceLayerException, XACT_TriggerNotFound;

  }

  public interface XynaDispatcherProvider {

    DestinationValue getDestination(DestinationKey dk) throws XPRC_DESTINATION_NOT_FOUND;


    boolean isCustom(DestinationKey key);
  }
  
  
  public static class DefaultXynaActivationTriggerProvider implements ActivationTriggerProvider {
    
    private XynaActivationTrigger xynaActivationTrigger;
    
    public DefaultXynaActivationTriggerProvider() {
      xynaActivationTrigger = XynaFactory.getInstance().getActivation().getActivationTrigger();
    }
    
    @Override
    public TriggerInstanceInformation getTriggerInstanceInformation(String name, Long revision) throws PersistenceLayerException {
      return xynaActivationTrigger.getTriggerInstanceInformation(name, revision);
    }
    
    
    @Override
    public Pair<Long, Boolean> getTriggerConfiguration(TriggerInstanceInformation triggerinstance) throws PersistenceLayerException {
      return xynaActivationTrigger.getTriggerConfiguration(triggerinstance);
    }
    
    
    @Override
    public Trigger getTrigger(Long revision, String name, boolean b) throws PersistenceLayerException, XACT_TriggerNotFound {
      return xynaActivationTrigger.getTrigger(revision, name, b);
    }
    
    
    @Override
    public FilterInstanceInformation getFilterInstanceInformation(String name, Long revision) throws PersistenceLayerException {
      return xynaActivationTrigger.getFilterInstanceInformation(name, revision);
    }
    
    
    @Override
    public Filter getFilter(Long revision, String filterName, boolean followRuntimeContextDependencies)
        throws PersistenceLayerException, XACT_FilterNotFound {
      return xynaActivationTrigger.getFilter(revision, filterName, followRuntimeContextDependencies);
    }
  }
}
