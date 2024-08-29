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
package com.gip.xyna.xmcp.xfcli.scriptentry.support;



import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gip.xyna.FileUtils;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable.TriggerInstanceState;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.DataProvider;
import com.gip.xyna.xfmg.xfctrl.appmgmt.DataProvider.ActivationTriggerProvider;
import com.gip.xyna.xfmg.xfctrl.appmgmt.DataProvider.XynaDispatcherProvider;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMapping;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_MONITORING_TYPE;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.Dependencies;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspensionBackupMode;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

import xmcp.gitintegration.ApplicationDefinition;
import xmcp.gitintegration.ContentEntry;
import xmcp.gitintegration.DispatcherDestination;
import xmcp.gitintegration.FactoryCapacity;
import xmcp.gitintegration.FactoryContent;
import xmcp.gitintegration.FactoryContentItem;
import xmcp.gitintegration.OrderType;
import xmcp.gitintegration.RuntimeContextDependency;
import xmcp.gitintegration.TriggerInstance;
import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceContentItem;
import xmcp.gitintegration.XMOMStorable;
import xmcp.gitintegration.impl.processing.OrderTypeProcessor;
import xmcp.gitintegration.impl.processing.RuntimeContextDependencyProcessor;



public class ApplicationManagement extends ApplicationManagementImpl {
  
  private static final Long revisionToBuild = -2l;
  private String factoryVersion;

  public ApplicationManagement(String factoryVersion) throws XynaException {
    this.factoryVersion = factoryVersion;
  }


  @Override
  protected void init() throws XynaException {
    //init without running xynafactory
    
    excludedSubtypesOfProperties = new HashMap<>();
    excludedSubtypesOfProperties.put(revisionToBuild, new XynaPropertyString("", ""));
    excludedSubtypesOfProperties.put(null, new XynaPropertyString("", ""));
    excludedSubtypesOfProperties.put(-3l, new XynaPropertyString("", "")); 
    for(long i=0; i<500; i++) {
      excludedSubtypesOfProperties.put(i, new XynaPropertyString("", ""));
    }
  }

  @Override
  protected ApplicationXmlEntry createApplicationXmlEntry(String applicationName, String versionName, boolean createStub) throws XynaException {
    ApplicationXmlEntry result = new ApplicationXmlEntry(applicationName, versionName, null);
    result.setFactoryVersion(factoryVersion);
    return result;
  }
  
  public ApplicationXmlEntry createApplicationDefinitionXml(CreateApplicationDefinitionXmlParameter parameter) throws XynaException {
    WorkspaceContent content = parameter.getContent();
    String applicationName = parameter.getApplicationName();
    String versionName = parameter.getVersionName();
    boolean createStub = parameter.isCreateStub();
    
    DataProvider provider = createDataProviderFromWorkspaceContent(parameter);
    return createApplicationXml(applicationName, versionName, content.getWorkspaceName(), createStub, provider);
  }


  private DataProvider createDataProviderFromWorkspaceContent(CreateApplicationDefinitionXmlParameter parameter) {
    DataProvider result = new DataProvider();
    WorkspaceContent content = parameter.getContent(); 
    String basePath = Path.of(parameter.getBasePath()).getParent().toString();
    boolean fromSaved = parameter.isFromSaved();
    String applicationName = parameter.getApplicationName();
    String versionName = parameter.getVersionName();
    List<RuntimeContextDependency> deps = content.getWorkspaceContentItems().stream().filter(x -> x instanceof RuntimeContextDependency).map(x -> (RuntimeContextDependency)x).collect(Collectors.toList());
    addRTCsFromStubs(deps, content);
    RuntimeContextDependency toBuild = new RuntimeContextDependency();
    toBuild.unversionedSetDepName(applicationName);
    toBuild.unversionedSetDepAddition(versionName);
    toBuild.unversionedSetDepType(RuntimeContextDependencyProcessor.DEP_TYPE_APPLICATION);
    Dispatcher dispatcher = new Dispatcher(parameter);
    RevMgmt revMgmt = new RevMgmt(deps, toBuild, parameter.getContent().getWorkspaceName());
    LocalFileActivationTriggerProvider activationTrigger = new LocalFileActivationTriggerProvider(parameter, revMgmt);
    CapMapping capMapping = new CapMapping(parameter);
    ObjectMgmt objectMgmt = new ObjectMgmt(content, basePath, fromSaved, dispatcher, revMgmt, applicationName, activationTrigger, capMapping);
    PrioManagement prioMgmt = new PrioManagement(parameter);
    Monitoring monitoring = new Monitoring(parameter);
    ParamInheritanceMgmt pimgmt = new ParamInheritanceMgmt(parameter);
    RtcRequirementsMgmt rtcMgmt = new RtcRequirementsMgmt(parameter);
    XmomOdsMgmt odsMgmt = new XmomOdsMgmt(parameter);
    FactoryConfigProvider factoryConfigProvider = new FactoryConfigProvider(parameter);
    
    
    result.setGetRevision(revMgmt::getRevision);
    result.setGetRuntimeContext(revMgmt::getRuntimeContext);
    result.setQueryAppDefStorables(objectMgmt::queryAppDefStorables);
    result.setFindDependenciesForStub(objectMgmt::findDependenciesForStub);
    result.setActivationTriggerProvider(activationTrigger);
    result.setGetPriority(prioMgmt::getPriority);
    result.setGetCapacities(capMapping::getCapacities);
    result.setGetMonitoringLevel(monitoring::getMonitoringLevel);
    result.setPlanningDispatcher(dispatcher.getDispatcherProvider(OrderTypeProcessor.DISPATCHERNAME_PLANNING));
    result.setExecutionDispatcher(dispatcher.getDispatcherProvider(OrderTypeProcessor.DISPATCHERNAME_EXECUTION));
    result.setCleanupDispatcher(dispatcher.getDispatcherProvider(OrderTypeProcessor.DISPATCHERNAME_CLEANUP));
    result.setGetListInheritanceRules(pimgmt::listInheritanceRules);
    result.setGetListApplicationDetails(objectMgmt::listApplicationDetails);
    result.setGetRequirements(rtcMgmt::getRequirements);
    result.setGetCheckDeploymentItemState(this::emptyCheck);
    result.setGetAllMappingsForRootType(odsMgmt::getAllMappingsForRootType);
    result.setGetIsDestinationKeyConfiguredForOrderContextMapping(this::emptyDestinationKeyContextMappingCheck);
    result.setGetGlobalCapacities(factoryConfigProvider::getCapacity);
    result.setGlobalRuntimeAppStorablesProvider(this::emtpyQueryAllRuntimeApplicationStorables);
    result.validate();
    return result;
  }
  
  private List<ApplicationEntryStorable> emtpyQueryAllRuntimeApplicationStorables(String application, String version) throws PersistenceLayerException {
    //only used for building application.xml of runtime applications.
    //we only create application.xmls from application definitions here
    return null;
  }
  
  private void addRTCsFromStubs(List<RuntimeContextDependency> deps, WorkspaceContent content) {
    for(WorkspaceContentItem item : content.getWorkspaceContentItems()) {
      if(!(item instanceof ApplicationDefinition)) {
        continue;
      }
      ApplicationDefinition appDef = (ApplicationDefinition) item;
      List<? extends RuntimeContextDependency> additionalDeps = appDef.getStubDependencies();
      additionalDeps = additionalDeps == null ? new ArrayList<>() : additionalDeps; 
      for(RuntimeContextDependency rcd : additionalDeps) {
        if(!deps.stream().anyMatch(x -> matchRuntimeContextDependency(x, rcd))) {
          deps.add(rcd);
        }
      }
    }
  }
  

  private boolean matchRuntimeContextDependency(RuntimeContextDependency rcd1, RuntimeContextDependency rcd2) {
    return Objects.equals(rcd1.getDepName(), rcd2.getDepName()) && Objects.equals(rcd1.getDepAddition(), rcd2.getDepAddition());
  }


  private boolean emptyDestinationKeyContextMappingCheck(DestinationKey dk, boolean ignoreGlobalProperty) {
    return false;
  }
  
  private void emptyCheck(String uniqueName, XMOMType xmomType, Long revision, boolean throwExceptionIfInvalid) {
    
  }
  
  private static class XmomOdsMgmt {
    private Map<String, List<XMOMODSMapping>> mappings;
    
    public XmomOdsMgmt(CreateApplicationDefinitionXmlParameter parameter) {
      mappings = new HashMap<String, List<XMOMODSMapping>>();
      
      for(WorkspaceContentItem entry : parameter.getContent().getWorkspaceContentItems()) {
        if(!(entry instanceof XMOMStorable)) {
          continue;
        }
        
        XMOMStorable item = (XMOMStorable) entry;
        XMOMODSMapping mapping = new XMOMODSMapping();
        mapping.setFqpath(item.getFQPath());
        mapping.setColumnname(item.getColumnName());
        mapping.setFqxmlname(item.getXMLName());
        mapping.setPath(item.getPath());
        mapping.setTablename(item.getODSName());
        mapping.setFqpath(item.getFQPath());
        mappings.putIfAbsent(item.getXMLName(), new ArrayList<XMOMODSMapping>());
        mappings.get(item.getXMLName()).add(mapping);
      }
    }
    
    public Collection<XMOMODSMapping> getAllMappingsForRootType(String name, Long revision) throws PersistenceLayerException {
      return mappings.getOrDefault(name, Collections.emptyList());
    }
  }
  
  
  private static class FactoryConfigProvider {
    
    private HashMap<String, CapacityInformation> capacities;
    
    public FactoryConfigProvider(CreateApplicationDefinitionXmlParameter parameter) {
      fillCapacitiesMap(parameter.getFactoryContent());
    }
    

    private void fillCapacitiesMap(FactoryContent content) {
      capacities = new HashMap<String, CapacityInformation>();
      Stream<? extends FactoryContentItem> stream = content.getFactoryContentItems().stream().filter(x -> x instanceof FactoryCapacity);
      List<FactoryCapacity> factoryCapacities = stream.map(x -> (FactoryCapacity) x).collect(Collectors.toList());
      for (FactoryCapacity capacity : factoryCapacities) {
        String name = capacity.getCapacityName();
        State state = State.valueOf(capacity.getState());
        CapacityInformation capacityInformation = new CapacityInformation(name, capacity.getCardinality(), 0, state);
        capacities.put(name, capacityInformation);
      }
    }
    
    
    public CapacityInformation getCapacity(String name) {
      return capacities.get(name);
    }
  }
  
  private static class RtcRequirementsMgmt {
    
    private List<RuntimeDependencyContext> requirements;
    
    public RtcRequirementsMgmt(CreateApplicationDefinitionXmlParameter parameter) {
      requirements = new ArrayList<RuntimeDependencyContext>();
      ApplicationDefinition appDef = getApplicationDefinition(parameter);
      for(RuntimeContextDependency req : appDef.getRuntimeContextDependencies()) {
        RuntimeDependencyContext item = req.getDepType().equals(RuntimeContextDependencyProcessor.DEP_TYPE_WORSPACE) ?
            new Workspace(req.getDepName()) : new Application(req.getDepName(), req.getDepAddition());
            requirements.add(item);
      }
    }
    
    public Collection<RuntimeDependencyContext> getRequirements(RuntimeDependencyContext owner) {
      return requirements;
    }
    
    
    private ApplicationDefinition getApplicationDefinition(CreateApplicationDefinitionXmlParameter parameter) {
      Stream<? extends WorkspaceContentItem> stream = parameter.getContent().getWorkspaceContentItems().stream();
      List<ApplicationDefinition> appDefs = stream.filter(x -> x instanceof ApplicationDefinition).map(x -> (ApplicationDefinition)x).collect(Collectors.toList());
      String appName = parameter.getApplicationName();
      try {
        return appDefs.stream().filter(x -> ((ApplicationDefinition)x).getName().equals(appName)).findFirst().get();
      } catch(NoSuchElementException e) {
        String appDefNames = String.join(", ", appDefs.stream().map(x -> ((ApplicationDefinition)x).getName()).collect(Collectors.toList()));
        throw new RuntimeException("Could not find ApplicationDefinition '" + appName + "'. ApplicationDefinitions: " + appDefNames);
      }
    }
  }
  
  private static class ParamInheritanceMgmt {
    
    private Map<String, Map<ParameterType, List<InheritanceRule>>> map;
    
    public ParamInheritanceMgmt(CreateApplicationDefinitionXmlParameter parameter) {
      map = new HashMap<String, Map<ParameterType,List<InheritanceRule>>>();
      
      List<OrderType> orderTypes = getOrderTypes(parameter.getContent());
      for(OrderType ot: orderTypes) {
        List<? extends xmcp.gitintegration.InheritanceRule> inheritanceRules = ot.getInheritanceRules();
        inheritanceRules = inheritanceRules == null ? new ArrayList<>() : inheritanceRules;
        Map<ParameterType, List<InheritanceRule>> innermap = new HashMap<>();
        for(xmcp.gitintegration.InheritanceRule rule : inheritanceRules) {
          addToMap(innermap, rule);
        }
        map.put(ot.getName(), innermap);
      }
      
    }
    
    private void addToMap(Map<ParameterType, List<InheritanceRule>> map, xmcp.gitintegration.InheritanceRule rule) {
      InheritanceRule r = convert(rule);
      ParameterType type = ParameterType.valueOf(rule.getParameterType());
      map.putIfAbsent(type, new ArrayList<InheritanceRule>());
      map.get(type).add(r);
    }
    
    private InheritanceRule convert(xmcp.gitintegration.InheritanceRule rule) {
      InheritanceRule.Builder builder = null; //InheritanceRule.create
      ParameterType type = ParameterType.valueOf(rule.getParameterType());
      switch(type) {
        case BackupWhenRemoteCall :
          builder = InheritanceRule.createBackupWhenRemoteCallRule(SuspensionBackupMode.valueOf(rule.getValue()));
          break;
        case MonitoringLevel :
          try {
            builder = InheritanceRule.createMonitoringLevelRule(rule.getValue());
          } catch (XPRC_INVALID_MONITORING_TYPE e) {
            throw new RuntimeException(e);
          }
          break;
        case SuspensionBackupMode :
          builder = InheritanceRule.createSuspensionBackupRule(SuspensionBackupMode.valueOf(rule.getValue()));
          break;
        default :
          throw new RuntimeException("Unsupported Type: ");
        
      }
      
      
      builder.childFilter(rule.getChildFilter());
      builder.precedence(Integer.valueOf(rule.getPrecedence()));
      return builder.build();
    }
    
    public Map<ParameterType, List<InheritanceRule>> listInheritanceRules(DestinationKey destinationKey) {
      return map.get(destinationKey.getOrderType());
    }
  }
  
  
  public static List<OrderType> getOrderTypes(WorkspaceContent content) {
    return content.getWorkspaceContentItems().stream().filter(x -> x instanceof OrderType).map(x -> (OrderType)x).collect(Collectors.toList());
  }
  
  private static class Dispatcher {
    
    private Map<String, String> plannings;
    private Map<String, String> executions;
    private Map<String, String> cleanups;
    
    private MapBasedDispatcher planningDispatcher;
    private MapBasedDispatcher executionDispatcher;
    private MapBasedDispatcher cleanupDispatcher;

    private Map<String, Map<String, String>> dataMaps;
    private Map<String, MapBasedDispatcher> maps;
    
    public Dispatcher(CreateApplicationDefinitionXmlParameter parameter) {
      plannings = new HashMap<String, String>();
      executions = new HashMap<String, String>();
      cleanups = new HashMap<String, String>();

      planningDispatcher = new MapBasedDispatcher(plannings);
      executionDispatcher = new MapBasedDispatcher(executions);
      cleanupDispatcher = new MapBasedDispatcher(cleanups);
      maps = new HashMap<String, MapBasedDispatcher>();
      dataMaps = new HashMap<String, Map<String, String>>();
      dataMaps.put(OrderTypeProcessor.DISPATCHERNAME_PLANNING, plannings);
      dataMaps.put(OrderTypeProcessor.DISPATCHERNAME_EXECUTION, executions);
      dataMaps.put(OrderTypeProcessor.DISPATCHERNAME_CLEANUP, cleanups);
      maps.put(OrderTypeProcessor.DISPATCHERNAME_PLANNING, planningDispatcher);
      maps.put(OrderTypeProcessor.DISPATCHERNAME_EXECUTION, executionDispatcher);
      maps.put(OrderTypeProcessor.DISPATCHERNAME_CLEANUP, cleanupDispatcher);
      
      
      List<OrderType> data = getOrderTypes(parameter.getContent());
      for(OrderType entry : data) {
        List<? extends DispatcherDestination> destinations = entry.getDispatcherDestinations();
        for(DispatcherDestination destination : destinations) {
          
          if(filterOutDestination(destination, entry.getName())) {
            continue;
          }
          dataMaps.get(destination.getDispatcherName()).put(entry.getName(), destination.getDestinationValue());
        }
      }
      

    }
    
    private boolean filterOutDestination(DispatcherDestination destination, String orderType) {
      switch(destination.getDispatcherName()) {
        case OrderTypeProcessor.DISPATCHERNAME_PLANNING: return destination.getDestinationValue().equals("DefaultPlanning");
        case OrderTypeProcessor.DISPATCHERNAME_EXECUTION: return destination.getDestinationValue().equals(orderType);
        case OrderTypeProcessor.DISPATCHERNAME_CLEANUP: return destination.getDestinationValue().equals("Empty");
      }
      return false;
    }
    
    public XynaDispatcherProvider getDispatcherProvider(String type) {
      return maps.get(type);
    }
  }
  
  private static class MapBasedDispatcher implements XynaDispatcherProvider {

    private Map<String, String> data;
    
    public MapBasedDispatcher(Map<String, String> map) {
      this.data = map;
    }
    
    @Override
    public DestinationValue getDestination(DestinationKey dk) throws XPRC_DESTINATION_NOT_FOUND {
      String fqn = data.get(dk.getOrderType());
      return new FractalWorkflowDestination(fqn); //Type is not present in final application.xml
    }

    @Override
    public boolean isCustom(DestinationKey key) {
      return data.containsKey(key.getOrderType());
    }
    
  }
  
  private static class Monitoring {
    
    private Map<String, Integer> monitoringLevels;
    
    public Monitoring (CreateApplicationDefinitionXmlParameter parameter) {
      monitoringLevels = new HashMap<String, Integer>();
      getOrderTypes(parameter.getContent()).stream().forEach(x -> monitoringLevels.put(x.getName(), x.getMonitoringLevel()));
    }
    
    public Integer getMonitoringLevel(final DestinationKey dk) {
      return monitoringLevels.get(dk.getOrderType());
    }
  }
  

  private static class CapMapping {
    
    private Map<String, List<Capacity>> map;
    
    public CapMapping(CreateApplicationDefinitionXmlParameter parameter) {
      map = new HashMap<String, List<Capacity>>();
      List<OrderType> orderTypes = getOrderTypes(parameter.getContent());
      for(OrderType ot: orderTypes) {
        List<? extends xmcp.gitintegration.Capacity> capacities = ot.getCapacities();
        List<Capacity> capList = new ArrayList<Capacity>();
        capacities = capacities == null ? new ArrayList<>() : capacities;
        for(xmcp.gitintegration.Capacity capacity: capacities) {
          Capacity cap = convert(capacity);
          capList.add(cap);
        }
        map.put(ot.getName(), capList);
      }
    }
    
    private Capacity convert(xmcp.gitintegration.Capacity capacity) {
      Capacity result = new Capacity();
      result.unversionedSetCapName(capacity.getCapacityName());
      result.unversionedSetCardinality(Integer.valueOf(capacity.getCardinality()));
      return  result;
    }
    
    public List<Capacity> getCapacities(DestinationKey key) {
      return map.get(key.getOrderType());
    }
  }
  
  
  private static class PrioManagement {
    
    private Map<String, Integer> map;
    
    public PrioManagement(CreateApplicationDefinitionXmlParameter parameter) {
      map = new HashMap<String, Integer>();
      getOrderTypes(parameter.getContent()).stream().forEach(x -> map.put(x.getName(), (x.getPrioritySetting() != null ? Integer.valueOf(x.getPrioritySetting().getPriority()) : null)));
    }
    
    public Integer getPriority(String orderType, Long revision) {
      return map.get(orderType);
    }
    
  }
  
  private static class LocalFileActivationTriggerProvider implements ActivationTriggerProvider {
    Map<String, Filter> filtersByName = new HashMap<String, Filter>();
    Map<String, FilterInstanceInformation> filterInstancesByName = new HashMap<String, FilterInstanceInformation>();
    Map<String, TriggerInstanceInformation> triggerInstancesByName = new HashMap<String, TriggerInstanceInformation>();
    Map<String, Trigger> triggerByName = new HashMap<String, Trigger>();
    Map<String, Pair<Long, Boolean>> maxEventConfig = new HashMap<String, Pair<Long, Boolean>>();
    RevMgmt revMgmt;
    
    public LocalFileActivationTriggerProvider(CreateApplicationDefinitionXmlParameter parameter, RevMgmt revMgmt) {
      this.revMgmt = revMgmt;
      parameter.getContent().getWorkspaceContentItems().stream().forEach(x -> fillMaps(x));
    }
    
    private void fillMaps(WorkspaceContentItem item) {
      if(item instanceof xmcp.gitintegration.Filter) {
        fillFilterMap((xmcp.gitintegration.Filter) item);
      }
      else if(item instanceof xmcp.gitintegration.FilterInstance) {
        fillFilterInstanceMap((xmcp.gitintegration.FilterInstance) item);
      }
      else if(item instanceof xmcp.gitintegration.TriggerInstance) {
        fillTriggerInstanceMaps((xmcp.gitintegration.TriggerInstance) item);
      }
      else if(item instanceof xmcp.gitintegration.Trigger) {
        fillTriggerMap((xmcp.gitintegration.Trigger)item);
      }
    }
    

    private void fillTriggerMap(xmcp.gitintegration.Trigger item) {
      TriggerStorable storable = new TriggerStorable();
      storable.setFqTriggerClassName(item.getFQTriggerClassName());
      storable.setJarFiles(item.getJarfiles());
      storable.setRevision(revisionToBuild);
      storable.setSharedLibs(item.getSharedlibs());
      storable.setTriggerName(item.getTriggerName());
      Trigger trigger = new Trigger(storable);
      triggerByName.put(item.getTriggerName(), trigger);
    }

    private void fillTriggerInstanceMaps(TriggerInstance item) {
      StringSerializableList<String> ssl = StringSerializableList.autoSeparator(String.class, ":|/;\\@-_.+#=[]?§$%&!", ':');
      List<String> startParameters = ssl.deserializeFromString(item.getStartParameter());
      TriggerInstanceInformation info;
      info = new TriggerInstanceInformation(item.getTriggerInstanceName(), item.getTriggerName(), "", //description
                                            TriggerInstanceState.ENABLED, 
                                            startParameters, 
                                            item.getStartParameter(), 
                                            null,
                                            revMgmt.getRuntimeContext(revisionToBuild), 
                                            revisionToBuild);
      triggerInstancesByName.put(item.getTriggerInstanceName(), info);
      
      //both are null if there is no limit configured
      Long maxReceives = item.getMaxReceives();
      Boolean reject = item.getRejectAfterMaxReceives();
      Pair<Long, Boolean> readConfig = new Pair<>(maxReceives, reject);
      maxEventConfig.put(item.getTriggerInstanceName(), readConfig);
    }

    private void fillFilterInstanceMap(xmcp.gitintegration.FilterInstance item) {
      FilterInstanceStorable storable = new FilterInstanceStorable();
      storable.setFilterName(item.getFilterName());
      storable.setFilterInstanceName(item.getFilterInstanceName());
      storable.setRevision(revisionToBuild);
      storable.setTriggerInstanceName(item.getTriggerInstanceName());
      RuntimeContext rtc = revMgmt.getRuntimeContext(revisionToBuild);
      FilterInstanceInformation info = new FilterInstanceInformation(storable, rtc);
      filterInstancesByName.put(item.getFilterInstanceName(), info);
      
    }

    private void fillFilterMap(xmcp.gitintegration.Filter filter) {
      StringSerializableList<String> ssl = StringSerializableList.autoSeparator(String.class, ":|/;\\@-_.+#=[]?§$%&!", ':');
      String[] jarFilesAsStrings = ssl.deserializeFromString(filter.getJarfiles()).toArray(new String[0]);
      File[] jarFiles = new File[jarFilesAsStrings.length];
      for (int i = 0; i < jarFilesAsStrings.length; i++) {
        jarFilesAsStrings[i] = jarFilesAsStrings[i].substring(6); //remove "filter"
        jarFiles[i] = new File(filter.getFilterName() + jarFilesAsStrings[i]);
      }
      String[] sharedLibs = ssl.deserializeFromString(filter.getSharedlibs()).toArray(new String[0]);
      Filter filterObject = new Filter(filter.getFilterName(), revisionToBuild, jarFiles, filter.getFQFilterClassName(), null,
                                       filter.getTriggerName(), sharedLibs, null); //missing: description
      filtersByName.put(filter.getFilterName(), filterObject);
    }

    @Override
    public Filter getFilter(Long revision, String filterName, boolean followRuntimeContextDependencies)
        throws PersistenceLayerException, XACT_FilterNotFound {
      return filtersByName.get(filterName);
    }

    @Override
    public FilterInstanceInformation getFilterInstanceInformation(String name, Long revision) throws PersistenceLayerException {
      return filterInstancesByName.get(name);
    }

    @Override
    public Pair<Long, Boolean> getTriggerConfiguration(TriggerInstanceInformation triggerinstance) throws PersistenceLayerException {
      return maxEventConfig.get(triggerinstance.getTriggerInstanceName());
    }

    @Override
    public TriggerInstanceInformation getTriggerInstanceInformation(String name, Long revision) throws PersistenceLayerException {
      return triggerInstancesByName.get(name);
    }

    @Override
    public Trigger getTrigger(Long revision, String name, boolean b) throws PersistenceLayerException, XACT_TriggerNotFound {
      return triggerByName.get(name);
    }
    
  }
  
  private static class ObjectMgmt {
    private Map<String, List<ContentEntry>> explicitObjectsByAppDefName;
    private Map<String, GenerationBase> objectsInWorkspace;
    private Map<String, List<String>> hierarchy;
    private File basePath;
    private GenerationBaseCache cache;
    private SingleRevisionFileSystemXMLSource source;
    private boolean fromSaved;
    private Dispatcher dispatcher;
    private Map<String, List<? extends RuntimeContextDependency>> stubDependencies;
    private RevMgmt revMgmt;
    private String applicationName;
    private LocalFileActivationTriggerProvider activation;
    private CapMapping capMapping;
    
    public ObjectMgmt(WorkspaceContent content, String basePath, boolean fromSaved, Dispatcher dispatcher, RevMgmt revMgmt, String applicationName, LocalFileActivationTriggerProvider activation, CapMapping capMapping) {
      this.basePath = new File(basePath);
      this.cache = new GenerationBaseCache();
      this.fromSaved = fromSaved;
      this.dispatcher = dispatcher;
      this.revMgmt = revMgmt;
      this.applicationName = applicationName;
      this.source = new SingleRevisionFileSystemXMLSource(basePath, revisionToBuild, -3l, fqNamesInRevision(basePath));
      this.activation = activation;
      this.capMapping = capMapping;
      explicitObjectsByAppDefName = new HashMap<String, List<ContentEntry>>();
      objectsInWorkspace = new HashMap<String, GenerationBase>();
      hierarchy = new HashMap<String, List<String>>();
      stubDependencies = new HashMap<String, List<? extends RuntimeContextDependency>>();
      List<ApplicationDefinition> appDefs = content.getWorkspaceContentItems().stream().filter(x -> x instanceof ApplicationDefinition).map(x -> (ApplicationDefinition)x).collect(Collectors.toList());
      
      for(ApplicationDefinition appDef : appDefs) {
        List<? extends ContentEntry> entries = appDef.getContentEntries();
        explicitObjectsByAppDefName.put(appDef.getName(), entries == null ? new ArrayList<>() : new ArrayList<>(entries));
        stubDependencies.put(appDef.getName(), appDef.getStubDependencies());
      }
      
      
      loadXMOMObjects(basePath, fromSaved);
    }
    
    private List<String> fqNamesInRevision(String basePath) {
      List<String> result;
      try (Stream<Path> files = Files.walk(Path.of(basePath + (fromSaved? "/saved" : "")))) {
        result = files.filter(x -> x.toString().endsWith(".xml")).map(this::fqNameFromPath).collect(Collectors.toList());
      } catch(Exception e) {
        throw new RuntimeException(e);
      }
      return result;
    }
    
    private void loadXMOMObjects(String basePath, boolean fromSaved) {
      //find all .XML files in save (or XMOM) folder under basePath
      //add them to objectsInWorkspace. key is FQN
      //see other scriptEntry BuildDatatypeJarFromSource
      //can we build dependency register from GenerationBase?
      //GenerationBase has dependencies object, but can we fill it without all other GenerationBases?
      //maybe pretend they are there, without further dependencies?
      List<GenerationBase> objects = null;
      try (Stream<Path> files = Files.walk(Path.of(basePath + (fromSaved? "/saved" : "")))) {
        objects = files.filter(x -> x.toString().endsWith(".xml")).map(this::toXmom).collect(Collectors.toList());
      } catch(Exception e) {
        throw new RuntimeException(e);
      }
      
      objects.stream().forEach(this::addToMaps);
      
      try {
        GenerationBase.deploy(objects, DeploymentMode.reload, false, WorkflowProtectionMode.BREAK_ON_USAGE);
      } catch (MDMParallelDeploymentException | XPRC_DeploymentDuringUndeploymentException e) {
        throw new RuntimeException(e);
      }
    }
    
    private void addToMaps(GenerationBase gb) {
      objectsInWorkspace.put(gb.getFqClassName(), gb);
      if(gb instanceof DomOrExceptionGenerationBase) {
        DomOrExceptionGenerationBase superGb = ((DomOrExceptionGenerationBase)gb).getSuperClassGenerationObject();
        if(superGb == null) {
          return;
        }
        hierarchy.putIfAbsent(superGb.getFqClassName(), new ArrayList<String>());
        hierarchy.get(superGb.getFqClassName()).add(gb.getFqClassName());
      }
    }
    
    private String fqNameFromPath(Path p) {
      String fqName =  FileUtils.deriveFqNameFromPath(basePath, p);
      if(fromSaved) {
        fqName = fqName.substring("saved.".length());
      }
      
      fqName = fqName.substring("XMOM.".length());     
      return fqName;
    }
    
    private GenerationBase toXmom(Path p) {
      String fqName = fqNameFromPath(p);

      try {
        XMOMType type = XMOMType.getXMOMTypeByFile(p.toFile());
        switch (type) {
        case DATATYPE:
          return DOM.getOrCreateInstance(fqName, cache, revisionToBuild, source);
        case EXCEPTION:
          return ExceptionGeneration.getOrCreateInstance(fqName, cache, revisionToBuild, source);
        case WORKFLOW:
          return WF.getOrCreateInstance(fqName, cache, revisionToBuild, source);
        default:
          throw new RuntimeException("Not of an accepted type: " + type);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    
    public List<ApplicationEntryStorable> queryAppDefStorables(String appName, Long parentRev) throws PersistenceLayerException {
      return explicitObjectsByAppDefName.get(appName).stream().map(this::convertToAppEntryStorable).collect(Collectors.toList());
    }
    
    private ApplicationEntryStorable convertToAppEntryStorable(ContentEntry entry) {
      ApplicationEntryStorable result = new ApplicationEntryStorable();
      result.setName(entry.getFQName());
      result.setType(entry.getType());
      return result;
    }
    
    public Collection<ApplicationEntryStorable> findDependenciesForStub(List<? extends ApplicationEntryStorable> appEntries, Long revision, Set<Long> revisionsToKeep, Application app) {
      List<ApplicationEntryStorable> result = new ArrayList<>();
      for(ApplicationEntryStorable entry : appEntries) {
        //we are interested in workFlows and orderTypes
        //for workFlows, add input/output/exceptions + dependencies
        //for orderTypes, only add input/output/exceptions of execution ... and execution itself?
        switch(entry.getTypeAsEnum()) {
          case ORDERTYPE: processOrderTypeForStub(entry, result); break;
          case WORKFLOW: processWorkflowForStub(entry.getName(), result); break;
          default: break; //nothing to be done ... if there is a dataType, we do not add the members? - we do not even add the dataType.
        }
      }
      

      List<? extends RuntimeContextDependency> deps = stubDependencies.get(applicationName);
      deps = deps == null ? new ArrayList<RuntimeContextDependency>() : deps;
      for(RuntimeContextDependency rtcd : deps) {
        //does not support workspaces
        Long r = revMgmt.getRevision(rtcd.getDepName(), rtcd.getDepAddition(), null);
        revisionsToKeep.add(r);
      }
      
      return result;
    }
    
    private void processWorkflowForStub(String entry, List<ApplicationEntryStorable> result) {
      //add input/output/exceptions + their dependencies - and workflow itself?
      GenerationBase gb = objectsInWorkspace.get(entry);
      Set<String> toResolve = new HashSet<String>();
      if(gb instanceof WF) {
        WF wf = (WF) gb;
        toResolve.addAll(wf.getInputVars().stream().map(x -> x.getFQClassName()).collect(Collectors.toSet()));
        toResolve.addAll(wf.getOutputVars().stream().map(x -> x.getFQClassName()).collect(Collectors.toSet()));
        toResolve.addAll(wf.getExceptionVars().stream().map(x -> x.getFQClassName()).collect(Collectors.toSet()));
        Set<GenerationBase> does = collectMemberAndHierarchy(toResolve);
        for(GenerationBase doe: does) {
          ApplicationEntryType type = doe instanceof DOM ? ApplicationEntryType.DATATYPE : ApplicationEntryType.EXCEPTION;
          result.add(ApplicationEntryStorable.create(null, null, null, doe.getOriginalFqName(),type));
        }
        ApplicationEntryStorable aes = new ApplicationEntryStorable();
        aes.setType(ApplicationEntryType.WORKFLOW.toString());
        aes.setName(gb.getFqClassName());
        result.add(aes);
      }
    }
    
    private void processOrderTypeForStub(ApplicationEntryStorable entry, List<ApplicationEntryStorable> result) {
      //add execution workflow
      try {
        DestinationValue fqn = dispatcher.getDispatcherProvider(OrderTypeProcessor.DISPATCHERNAME_EXECUTION).getDestination(new DestinationKey(entry.getName()));
        String s = fqn.getFQName() == null ? entry.getName() : fqn.getFQName();
        processWorkflowForStub(s, result);
        result.add(entry); //add explicit orderType
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        throw new RuntimeException(e);
      }
    }
    
    public List<ApplicationEntryStorable> listApplicationDetails(String applicationName, String version, boolean includingDependencies,
                                                                 List<String> excludeSubtypesOf, Long parentRevision) {
      List<ApplicationEntryStorable> result = new ArrayList<ApplicationEntryStorable>();
      
      //explicit
      result.addAll(explicitObjectsByAppDefName.get(applicationName).stream().map(this::convert).collect(Collectors.toList()));
      
      //implicit
      if(includingDependencies) {
        result = addImplicitDependencies(result);
        Set<GenerationBase> outputVarSubtypes = getSubTypesOfOutputVars(result);
        for(GenerationBase gb: outputVarSubtypes) {
          ApplicationEntryStorable appEntry =
              ApplicationEntryStorable.create(applicationName, version, parentRevision, gb.getOriginalFqName(), ApplicationEntryType.DATATYPE);
          result.add(appEntry);
          Dependencies deps = gb.getDependenciesRecursively();
          Set<GenerationBase> dependencies = deps.getDependencies(true);
          Set<ApplicationEntryStorable> toAdd = dependencies.stream().map(this::convert).collect(Collectors.toSet());
          toAdd.removeIf(x -> !objectsInWorkspace.containsKey(x.getName()));
          
          result.addAll(toAdd);
        }
      }
      
      ArrayList<ApplicationEntryStorable> clone = new ArrayList<ApplicationEntryStorable>(result);
      for(ApplicationEntryStorable entry : clone) {
        if(entry.getType().equals(ApplicationEntryType.ORDERTYPE.toString())) {
          DestinationKey dk = new DestinationKey(entry.getName());
          List<Capacity> capacityList = capMapping.getCapacities(dk);
          for(Capacity cap: capacityList) {
            ApplicationEntryStorable capacityEntry = new ApplicationEntryStorable();
            capacityEntry.setType(ApplicationEntryType.CAPACITY.toString());
            capacityEntry.setName(cap.getCapName());
            result.add(capacityEntry);
          }
        }
      }

      return result;
    }
    
    private Set<GenerationBase> getSubTypesOfOutputVars(List<ApplicationEntryStorable> entries) {
      List<String> dts = entries.stream().filter(x -> x.getTypeAsEnum() == ApplicationEntryType.DATATYPE).map(x -> x.getName()).collect(Collectors.toList());
      Set<String> toResolve = new HashSet<String>(); //FQNs of DataTypes and Exceptions that need to be added
      for(String dt : dts) {
        GenerationBase gb = objectsInWorkspace.get(dt);
        if(gb == null || !(gb instanceof DOM)) {
          continue;
        }
        toResolve.add(gb.getOriginalFqName());
        List<Operation> ops = ((DOM)gb).getOperations();
        for(Operation op : ops) {
          List<AVariable> outputs = op.getOutputVars();
          for(AVariable output: outputs) {
            GenerationBase outputGb = objectsInWorkspace.get(output.getFQClassName());
            if(outputGb == null) {
              continue;
            }
            toResolve.add(outputGb.getOriginalFqName());
          }
        }
      }
      
      return collectMemberAndHierarchy(toResolve);
    }
    
    
    private Set<GenerationBase> collectMemberAndHierarchy(Set<String> toResolve) {
      Set<GenerationBase> result = new HashSet<GenerationBase>();
      Set<String> resolved = new HashSet<String>();
      Set<String> toResolveNextRound = new HashSet<String>();
      while(toResolve.size() > 0) {
        for(String s : toResolve) {
          resolved.add(s);
          GenerationBase gb = objectsInWorkspace.get(s);
          if(gb == null || !(gb instanceof DomOrExceptionGenerationBase)) {
            continue; //object not in workspace (or not a DoE)
          }
          DomOrExceptionGenerationBase doe = (DomOrExceptionGenerationBase)gb;
          List<AVariable> members = doe.getAllMemberVarsIncludingInherited();
          for(AVariable member : members) {
            if(toResolve.contains(member.getFQClassName())) {
              continue;
            }
            //we found a new DoE that is not in toResolve, but needs to be resolved.
            toResolveNextRound.add(member.getFQClassName());
          }
          result.add(gb);
          toResolveNextRound.addAll(getSubtypesRecursive(s));
        }
        toResolve = new HashSet<String>(toResolveNextRound);
        toResolveNextRound.clear();
      }
      return result;
    }
    
    private Set<String> getSubtypesRecursive(String fqn) {
      Set<String> result = new HashSet<>(hierarchy.getOrDefault(fqn, new ArrayList<String>()));
      for(String s: hierarchy.getOrDefault(fqn, new ArrayList<String>())) {
        result.addAll(getSubtypesRecursive(s));
      }
      return result;
    }
    
    private List<ApplicationEntryStorable> addImplicitDependencies(List<ApplicationEntryStorable> explicitDependencies) {
      Set<ApplicationEntryStorable> result = new HashSet<ApplicationEntryStorable>(explicitDependencies);
      
      for(ApplicationEntryStorable entry : explicitDependencies) {
        addDependencies(entry, result);
      }
      
      //remove entries from other RTCs
      result.removeIf(x -> isXmom(x) && !objectsInWorkspace.containsKey(x.getName()));
      
      return new ArrayList<ApplicationEntryStorable>(result);
    }
    

    private boolean isXmom(ApplicationEntryStorable entry) {
      return entry.getTypeAsEnum() == ApplicationEntryType.WORKFLOW || entry.getTypeAsEnum() == ApplicationEntryType.DATATYPE
          || entry.getTypeAsEnum() == ApplicationEntryType.EXCEPTION;
    }
    
    private void addDependencies(ApplicationEntryStorable entry, Set<ApplicationEntryStorable> result) {
      
      Set<ApplicationEntryStorable> implicits = new HashSet<ApplicationEntryStorable>();
      if(entry.getTypeAsEnum() == ApplicationEntryType.ORDERTYPE) {
        addOrderTypeDependencies(entry.getName(), implicits);
        result.addAll(implicits);
        return;
      }
      
      if(entry.getTypeAsEnum() == ApplicationEntryType.FILTERINSTANCE) {
        addFilterInstanceImplicits(entry, result);
        return;
      }
      
      if(entry.getTypeAsEnum() == ApplicationEntryType.TRIGGERINSTANCE) {
        addTriggerInstanceImplicits(entry, result);
        return;
      }
      
      Optional<GenerationBase> obj = getObjectToEntry(entry);
      if(!obj.isPresent()) {
        return;
      }
      GenerationBase o = obj.get();
      if(o instanceof WF) {
        addOrderTypeDependencies(o.getFqClassName(), implicits);
        result.addAll(implicits);
      }
    }
    
    // if the trigger is in this revision as well, then add it
    private void addTriggerInstanceImplicits(ApplicationEntryStorable entry, Set<ApplicationEntryStorable> result) {
      String triggerName = null;
      try {
        triggerName = activation.getTriggerInstanceInformation(entry.getName(), entry.getParentRevision()).getTriggerName();
        if(activation.getTrigger(revisionToBuild, triggerName, false) == null) {
          return;
        }
      } catch (Exception e1) {
        throw new RuntimeException(e1);
      }
        
      ApplicationEntryStorable e = new ApplicationEntryStorable();
      e.setType(ApplicationEntryType.TRIGGER.toString());
      e.setName(triggerName);
      e.setParentRevision(entry.getParentRevision());
      result.add(e);
    }

    private void addFilterInstanceImplicits(ApplicationEntryStorable entry, Set<ApplicationEntryStorable> result) {
      ApplicationEntryStorable e = new ApplicationEntryStorable();
      e.setType(ApplicationEntryType.FILTER.toString());
      String filterName = null;
      try {
        filterName = activation.getFilterInstanceInformation(entry.getName(), revisionToBuild).getFilterName();
      } catch (PersistenceLayerException e1) {
        throw new RuntimeException(e1);
      }
      e.setName(filterName);
      e.setParentRevision(entry.getParentRevision());
      result.add(e);
      
      e = new ApplicationEntryStorable();
      e.setType(ApplicationEntryType.TRIGGERINSTANCE.toString());
      String triggerInstanceName = null;
      try {
        triggerInstanceName = activation.getFilterInstanceInformation(entry.getName(), revisionToBuild).getTriggerInstanceName();
      } catch (PersistenceLayerException e1) {
        throw new RuntimeException(e1);
      }
      e.setName(triggerInstanceName);
      e.setParentRevision(entry.getParentRevision());
      result.add(e);
    }
    
    private boolean compareAppEntryStorable(ApplicationEntryStorable aes1, ApplicationEntryStorable aes2) {
      return aes1.getName().equals(aes2.getName()) && aes1.getType().equals(aes2.getType());
    }
    
    private void addOrderTypeDependencies(String fqn, Set<ApplicationEntryStorable> result) {
      ApplicationEntryStorable e = new ApplicationEntryStorable();
      e.setType(ApplicationEntryType.ORDERTYPE.toString());
      e.setName(fqn);
      if(!result.stream().anyMatch(x -> compareAppEntryStorable(x, e))) {
        result.add(e);
        //check planning
        XynaDispatcherProvider planningDispatcher = dispatcher.getDispatcherProvider(OrderTypeProcessor.DISPATCHERNAME_PLANNING);
        if(planningDispatcher.isCustom(new DestinationKey(fqn))) {
          //ensure that the planning workflow is added to result
            String planningFqName = getFqName(planningDispatcher, fqn);
            GenerationBase planningGb = objectsInWorkspace.get(planningFqName);
            addToResult(planningGb, result);
        }
        
        //if execution destination is not part of result, add it together with dependencies
        XynaDispatcherProvider executionDispatcher = dispatcher.getDispatcherProvider(OrderTypeProcessor.DISPATCHERNAME_EXECUTION);
        String executionFqName = getFqName(executionDispatcher, fqn);
        GenerationBase executionGb = objectsInWorkspace.get(executionFqName == null ? fqn : executionDispatcher);
        addToResult(executionGb, result);
        
        //check cleanup
      }
    }
    
    private String getFqName(XynaDispatcherProvider dispatcher, String fqn) {
      try {
        return dispatcher.getDestination(new DestinationKey(fqn)).getFQName();
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        throw new RuntimeException(e);
      }
    }
    
    private void addToResult(GenerationBase gb, Set<ApplicationEntryStorable> result) {
      if(gb == null) {
        return;
      }
      Dependencies deps = gb.getDependenciesRecursively();
      Set<GenerationBase> dependencies = deps.getDependencies(true);
      Set<ApplicationEntryStorable> toAdd = dependencies.stream().map(this::convert).collect(Collectors.toSet());
      result.addAll(toAdd);

      ApplicationEntryStorable executionWf = new ApplicationEntryStorable();
      executionWf.setType(ApplicationEntryType.WORKFLOW.toString());
      executionWf.setName(gb.getFqClassName());
      result.add(executionWf);
      addOrderTypeDependencies(gb.getFqClassName(), result);
    }
    
    
    private Optional<GenerationBase> getObjectToEntry(ApplicationEntryStorable entry) {
      String fqn = entry.getName();
      GenerationBase result = objectsInWorkspace.get(fqn);
      return result == null ? Optional.empty() : Optional.of(result);
    }
    
    private ApplicationEntryStorable convert(GenerationBase gb) {
      ApplicationEntryStorable result = new ApplicationEntryStorable();
      result.setName(gb.getOriginalFqName());
      result.setParentRevision(revisionToBuild);
      result.setType(gb.getTypeAsString().toUpperCase());
      return result;
    }
    
    private ApplicationEntryStorable convert(ContentEntry entry) {
      ApplicationEntryStorable result = new ApplicationEntryStorable();
      result.setName(entry.getFQName());
      result.setParentRevision(revisionToBuild);
      result.setType(entry.getType());
      return result;
    }
    
  }
  
  private static class RevMgmt {
    
    private Map<String, Long> getRevisionMap;
    private Map<Long, RuntimeContext> getRtcMap;
    
    public RevMgmt(List<RuntimeContextDependency> deps, RuntimeContextDependency toBuild, String workspaceName) {
      getRevisionMap = new HashMap<String, Long>();
      getRtcMap = new HashMap<Long, RuntimeContext>();
      
      getRevisionMap.put(createKey(toBuild), revisionToBuild);
      getRtcMap.put(revisionToBuild, createRuntimeContext(toBuild));
      
      long nextId = 1;
      for(RuntimeContextDependency dep : deps) {
        getRevisionMap.put(createKey(dep), nextId);
        getRtcMap.put(nextId, createRuntimeContext(dep));
        nextId++;
      }
      //should this be the same as the application definition?
      getRevisionMap.put(createKey(null, null, workspaceName), nextId);
      getRtcMap.put(nextId, new Workspace(workspaceName));
    }
    
    public Long getRevision(String applicationName, String versionName, String workspaceName) {
      return getRevisionMap.get(createKey(applicationName, versionName, workspaceName));
    }
    
    public RuntimeContext getRuntimeContext(Long revision) {
      return getRtcMap.get(revision);
    }
    
    private RuntimeContext createRuntimeContext(RuntimeContextDependency dep) {
      if(dep.getDepType().equals(RuntimeContextDependencyProcessor.DEP_TYPE_WORSPACE)) {
        return new Workspace(dep.getDepName());
      } else if(dep.getDepType().equals(RuntimeContextDependencyProcessor.DEP_TYPE_APPLICATION)) {
        return new Application(dep.getDepName(), dep.getDepAddition());
      }
      throw new RuntimeException("unknown dep Type: " + dep.getDepType());      
    }
    
    private String createKey(RuntimeContextDependency dep) {
      if(dep.getDepType().equals(RuntimeContextDependencyProcessor.DEP_TYPE_WORSPACE)) {
        return createKey(null, null, dep.getDepName());
      } else if(dep.getDepType().equals(RuntimeContextDependencyProcessor.DEP_TYPE_APPLICATION)) {
        return createKey(dep.getDepName(), dep.getDepAddition(), null);
      }
      throw new RuntimeException("unknown dep Type: " + dep.getDepType());
    }
    
    private String createKey(String applicationName, String versionName, String workspaceName) {
      return applicationName + ":" + versionName + ":" + workspaceName;
    }
  }
  
  public static class CreateApplicationDefinitionXmlParameter {
    private String applicationName; 
    private String versionName;
    private boolean createStub; 
    private WorkspaceContent content;
    private FactoryContent factoryContent;
    private String basePath;
    private boolean fromSaved;
    
    public String getApplicationName() {
      return applicationName;
    }
    
    public void setApplicationName(String applicationName) {
      this.applicationName = applicationName;
    }
    
    public String getVersionName() {
      return versionName;
    }
    
    public void setVersionName(String versionName) {
      this.versionName = versionName;
    }
    
    public boolean isCreateStub() {
      return createStub;
    }
    
    public void setCreateStub(boolean createStub) {
      this.createStub = createStub;
    }
    
    public WorkspaceContent getContent() {
      return content;
    }
    
    public void setContent(WorkspaceContent content) {
      this.content = content;
    }
    
    
    public FactoryContent getFactoryContent() {
      return factoryContent;
    }

    
    public void setFactoryContent(FactoryContent factoryContent) {
      this.factoryContent = factoryContent;
    }

    public String getBasePath() {
      return basePath;
    }
    
    public void setBasePath(String basePath) {
      this.basePath = basePath;
    }

    
    public boolean isFromSaved() {
      return fromSaved;
    }

    
    public void setFromSaved(boolean fromSaved) {
      this.fromSaved = fromSaved;
    }

  }
}
