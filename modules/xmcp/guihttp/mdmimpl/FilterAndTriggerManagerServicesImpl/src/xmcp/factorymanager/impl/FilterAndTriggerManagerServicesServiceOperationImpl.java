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
package xmcp.factorymanager.impl;


import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterWithEnum;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.DeployFilterParameter;
import com.gip.xyna.xact.trigger.FilterInformation;
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xact.trigger.TriggerInformation;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext.RuntimeDependencyContextType;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xmcp.xfcli.StringParameterFormatter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import xact.http.URLPath;
import xact.http.enums.httpmethods.HTTPMethod;
import xact.templates.Document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import xmcp.factorymanager.filtermanager.DeployFilterRequest;
import xmcp.factorymanager.filtermanager.DeployTriggerRequest;
import xmcp.factorymanager.filtermanager.Filter;
import xmcp.factorymanager.filtermanager.FilterDetails;
import xmcp.factorymanager.filtermanager.FilterInstance;
import xmcp.factorymanager.filtermanager.FilterInstanceDetails;
import xmcp.factorymanager.filtermanager.GetFilterDetailRequest;
import xmcp.factorymanager.filtermanager.GetTriggerRequest;
import xmcp.factorymanager.filtermanager.StartParameterDetails;
import xmcp.factorymanager.filtermanager.Trigger;
import xmcp.factorymanager.filtermanager.TriggerDetail;
import xmcp.factorymanager.filtermanager.TriggerInstance;
import xmcp.factorymanager.filtermanager.TriggerInstanceDetail;
import xmcp.factorymanager.rtcmanager.RuntimeApplication;
import xmcp.factorymanager.rtcmanager.RuntimeContext;
import xmcp.factorymanager.rtcmanager.Workspace;
import xmcp.factorymanager.FilterAndTriggerManagerServicesServiceOperation;


public class FilterAndTriggerManagerServicesServiceOperationImpl implements ExtendedDeploymentTask, FilterAndTriggerManagerServicesServiceOperation {
  
  private static final XynaActivationTrigger activationTrigger = 
      XynaFactory.getInstance().getActivation().getActivationTrigger();
  
  
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
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }

  public void deployFilterInstance(DeployFilterRequest deployFilterRequest10) {
    String[] parameter;
    if (deployFilterRequest10.getConfigurationParameter() == null) {
      parameter = new String[0];
    } else {
      parameter = deployFilterRequest10.getConfigurationParameter().split(",");
    }
    DeployFilterParameter deployFilterParameter = 
        new DeployFilterParameter.Builder().
        filterName(deployFilterRequest10.getFilterName()).
        instanceName(deployFilterRequest10.getFilterInstanceName()).
        triggerInstanceName(deployFilterRequest10.getTriggerInstanceName()).
        description(deployFilterRequest10.getDocumentation()).
        optional(deployFilterRequest10.getOptional()).
        configuration(parameter).
        revision(getRevision(deployFilterRequest10.getRuntimeContext())).build();
    try {
      activationTrigger.deployFilter(deployFilterParameter);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void deployTriggerInstance(DeployTriggerRequest deployTriggerRequest16) {
    try {
      String[] parameter;
      if (deployTriggerRequest16.getStartParameter() == null) {
        parameter = new String[0];
      } else {
        parameter = deployTriggerRequest16.getStartParameter().split(",");
      }
      activationTrigger.deployTrigger(
        deployTriggerRequest16.getTriggerName(),
        deployTriggerRequest16.getTriggerInstanceName(),
        parameter,
        deployTriggerRequest16.getDocumentation(),
        getRevision(deployTriggerRequest16.getRuntimeContext()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void disableFilterInstance(FilterInstance filterInstance8) {
    try {
      activationTrigger.disableFilterInstance(
        filterInstance8.getFilterInstance(), getRevision(filterInstance8.getRuntimeContext()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void disableTriggerInstance(TriggerInstance triggerInstance12) {
    try {
      activationTrigger.disableTriggerInstance(
        triggerInstance12.getTriggerInstance(), getRevision(triggerInstance12.getRuntimeContext()), true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void enableFilterInstance(FilterInstance filterInstance7) {
    try {
      activationTrigger.enableFilterInstance(
        filterInstance7.getFilterInstance(), getRevision(filterInstance7.getRuntimeContext()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void enableTriggerInstance(TriggerInstance triggerInstance13) {
    try {
      activationTrigger.enableTriggerInstance(
        triggerInstance13.getTriggerInstance(), getRevision(triggerInstance13.getRuntimeContext()), true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public FilterDetails getFilterDetail(GetFilterDetailRequest getFilterDetailRequest25) {
    FilterInformation filterinfo;
    try {
      filterinfo = activationTrigger.getFilterInformation(
        getFilterDetailRequest25.getFilter(), getRevision(getFilterDetailRequest25.getRuntimeContext()), false);
    } catch (PersistenceLayerException | XACT_FilterNotFound e) {
      throw new RuntimeException(e);
    }
    return getDetail(filterinfo);
  }

  public FilterInstanceDetails getFilterInstanceDetail(FilterInstance filterInstance22) {
    FilterInstanceInformation info;
    TriggerInstanceInformation triggerInfo;
    try {
      info = activationTrigger.getFilterInstanceInformation
        (filterInstance22.getFilterInstance(), getRevision(filterInstance22.getRuntimeContext()));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }    
    FilterInstanceDetails result = getDetail(info);
    try {
      triggerInfo = activationTrigger.getTriggerInstanceInformation
        (info.getTriggerInstanceName(), info.getRevision(), true);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }    
    result.setTriggerInstanceDetail(getDetail(triggerInfo));
    return result;
  }

  public List<? extends Filter> getFilterOverview() {
    List<FilterInformation> filterinfo;
    try {
      filterinfo = activationTrigger.listFilterInformation();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    return filterinfo.stream().
        map(info -> convertToXMOM(info)).
        collect(Collectors.toList());
  }

  public TriggerDetail getTriggerDetails(GetTriggerRequest getTriggerRequest26) {
       
    TriggerInformation triggerinfo;
    try {
      triggerinfo = activationTrigger.getTriggerInformation(
        getTriggerRequest26.getTrigger(), getRevision(getTriggerRequest26.getRuntimeContext()), false);
    } catch (PersistenceLayerException | XACT_TriggerNotFound e) {
      throw new RuntimeException(e);
    }
    return getDetail(triggerinfo);
  }

  public TriggerInstanceDetail getTriggerInstanceDetail(TriggerInstance triggerInstance20) {
    TriggerInstanceInformation info;
    try {
      info = activationTrigger.getTriggerInstanceInformation(
        triggerInstance20.getTriggerInstance(), getRevision(triggerInstance20.getRuntimeContext()));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    TriggerInstanceDetail result = getDetail(info);
    
    List<FilterInstance> filterInstances;
    try {
      filterInstances = activationTrigger.getFilterInstancesForTriggerInstance
        (triggerInstance20.getTriggerInstance(), getRevision(triggerInstance20.getRuntimeContext()), true).stream().
        map(instance -> {
          try {
            return activationTrigger.getFilterInstanceInformation(instance.getFilterInstanceName(), instance.getRevision());
          } catch (PersistenceLayerException e) {
            throw new RuntimeException(e);
          }
        }).
        map(filterinstanceinfo -> convertToXMOM(filterinstanceinfo)).
        collect(Collectors.toList());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    result.setFilterInstance(filterInstances);
    return result;
  }

  public List<? extends Trigger> getTriggerOverview() {
    List<TriggerInformation> triggerinfo;
    try {
      triggerinfo = activationTrigger.listTriggerInformation();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    return triggerinfo.stream().
        map(info -> convertToXMOM(info)).
        collect(Collectors.toList());
  }

  public void undeployFilterInstance(FilterInstance filterInstance9) {
    try {
      activationTrigger.undeployFilter(
        filterInstance9.getFilterInstance(), getRevision(filterInstance9.getRuntimeContext()));
    } catch (PersistenceLayerException | XACT_FilterNotFound e) {
      throw new RuntimeException(e);
    }
  }

  public void undeployTriggerInstance(TriggerInstance triggerInstance15) {
    try {
      activationTrigger.undeployTrigger(
        triggerInstance15.getTrigger(), triggerInstance15.getTriggerInstance(), getRevision(triggerInstance15.getRuntimeContext()));
    } catch (XACT_TriggerNotFound | PersistenceLayerException | XACT_TriggerInstanceNotFound e) {
      throw new RuntimeException(e);
    }
  }
  
  public List<? extends RuntimeContext> getPossibleRTCForFilterDeployment0(Filter filter31) {
    long revision = getRevision(filter31.getRuntimeContext());
    HashSet<Long> revisions = new HashSet<Long>();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
      .getParentRevisionsRecursivly(revision, revisions);
    revisions.add(revision);
    return revisions.stream().
      map(rev -> convert(rev)).
      collect(Collectors.toList());
  }
  
  public List<? extends RuntimeContext> getPossibleRTCForTriggerDeployment0(Trigger trigger32) {
    long revision = getRevision(trigger32.getRuntimeContext());
    HashSet<Long> revisions = new HashSet<Long>();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
      .getParentRevisionsRecursivly(revision, revisions);
    revisions.add(revision);
    return revisions.stream().
      map(rev -> convert(rev)).
      collect(Collectors.toList());
  }
  
  public List<? extends StartParameterDetails> getPossibleStartParameterForTriggerDeployment(Trigger trigger43) {
    TriggerInformation triggerinfo;
    try {
      triggerinfo = activationTrigger.getTriggerInformation(trigger43.getName(), getRevision(trigger43.getRuntimeContext()), true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    if (triggerinfo.getEnhancedStartParameter() != null) {
      return triggerinfo.getEnhancedStartParameter().stream().
      map(para -> new StartParameterDetails.Builder().
          name(para.getName()).
          documentation(para.documentation(DocumentationLanguage.EN)).
          optional(para.isOptional()).
          mandatory(getMandatoryFromStringParameter(para)).
          type(getTypeFromStringParameter(para)).
          instance()).
      collect(Collectors.toList());
    } else if (triggerinfo.getStartParameterDocumentation() != null) {
      return Arrays.asList(triggerinfo.getStartParameterDocumentation()).stream().
      map(docu -> new StartParameterDetails.Builder().
          lagacyParameterCombination(Arrays.asList(docu)).
          instance()).
      collect(Collectors.toList());
    }
    return List.of();
  }

  
  public List<? extends TriggerInstance> getPossibleTriggerInstanceForFilterDeployment(Filter filter37, RuntimeContext runtimeContext36) {
    TriggerInformation triggerinfo;
    try {
      FilterInformation filterinfo = activationTrigger.getFilterInformation(filter37.getName(), getRevision(filter37.getRuntimeContext()), false);
      triggerinfo = activationTrigger.getTriggerInformation(filterinfo.getTriggerName(), getRevision(runtimeContext36), true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    List<TriggerInstanceInformation> instanceinfo = triggerinfo.getTriggerInstances();

    HashSet<Long> revisions = new HashSet<Long>();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().
    getDependenciesRecursivly(getRevision(runtimeContext36), revisions);
    revisions.add(getRevision(runtimeContext36));

    return instanceinfo.stream().
      filter(info -> revisions.contains(getRevision(info.getRuntimeContext()))).
      map(info -> convertToXMOM(info)).
      collect(Collectors.toList());
  }
  
  @Override
  public GeneralXynaObject invokeGuiHttpEndpoint(XynaOrderServerExtension order, URLPath url, HTTPMethod method, Document payload) {
    try {
      RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
      Object result = runnable.execute(url, method, payload.getText());
      if(result != null) {
        if(result instanceof GeneralXynaObject) {
          return (GeneralXynaObject) result;
        } else {
          return new Document.Builder().text(result.toString()).instance();
        }
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }
  
  private Trigger convertToXMOM(TriggerInformation info) {
    return new Trigger.Builder().
      name(info.getTriggerName()).
      state(info.getTriggerState().serializeToString()).
      triggerInstance(info.getTriggerInstances().stream().
        map(inInfo -> convertToXMOM(inInfo)).
        collect(Collectors.toList())).
      runtimeContext(convert(info.getRuntimeContext())).
      instance();
  }
  
  private TriggerInstance convertToXMOM(TriggerInstanceInformation info) {
    return new TriggerInstance.Builder().
      trigger(info.getTriggerName()).
      triggerInstance(info.getTriggerInstanceName()).
      status(info.getState().serializeToString()).
      runtimeContext(convert(info.getRuntimeContext())).
      instance();
  }
  
  private Filter convertToXMOM(FilterInformation info) {
    return new Filter.Builder().
      name(info.getFilterName()).
      state(info.getFilterState().serializeToString()).
      filterInstance(info.getFilterInstances().stream().
        map(inInfo -> convertToXMOM(inInfo)).
        collect(Collectors.toList())).
      runtimeContext(convert(info.getRuntimeContext())).
      instance();
  }
  
  private FilterInstance convertToXMOM(FilterInstanceInformation info) {
    return new FilterInstance.Builder().
      filter(info.getFilterName()).
      filterInstance(info.getFilterInstanceName()).
      state(info.getState().serializeToString()).
      runtimeContext(convert(info.getRuntimeContext())).
      instance();
  }
  
  private TriggerDetail getDetail(TriggerInformation info) {
    return new TriggerDetail.Builder().
      trigger(info.getFqTriggerClassName()).
      name(info.getTriggerName()).
      status(info.getTriggerState().serializeToString()).
      description(info.getDescription()).
      runtimeContext(convert(info.getRuntimeContext())).
      instance();
  }

  private TriggerInstanceDetail getDetail(TriggerInstanceInformation info) {
    return new TriggerInstanceDetail.Builder().
      triggerInstance(info.getTriggerInstanceName()).
      trigger(info.getTriggerName()).
      status(info.getState().serializeToString()).
      description(info.getDescription()).
      runtimeContext(convert(info.getRuntimeContext())).
      startParameter(info.getStartParameterAsString()).
      instance();
  }

  private FilterDetails getDetail(FilterInformation info) {
    return new FilterDetails.Builder().
      name(info.getFilterName()).
      status(info.getFilterState().serializeToString()).
      description(info.getDescription()).
      runtimeContext(convert(info.getRuntimeContext())).
      instance();
  }

  private FilterInstanceDetails getDetail(FilterInstanceInformation info) {
    return new FilterInstanceDetails.Builder().
      instance(info.getFilterInstanceName()).
      filter(info.getFilterName()).
      status(info.getState().serializeToString()).
      description(info.getDescription()).
      runtimeContext(convert(info.getRuntimeContext())).
      configurationParameter(String.join(", ", info.getConfiguration())).
      instance();
  }
  
  private RuntimeContext convert(com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext runtimeContext) {
    RuntimeContext result;
    RuntimeDependencyContextType type = runtimeContext.getRuntimeDependencyContextType();
    if (type.equals(RuntimeDependencyContextType.Workspace)) {
      result = new Workspace.Builder().
        name(runtimeContext.getName()).
        instance();
    } else if (type.equals(RuntimeDependencyContextType.Application)) {
      result = new RuntimeApplication.Builder().
        name(runtimeContext.getName()).
        version(((Application)runtimeContext).getVersionName()).
        instance();
    } else {
      throw new RuntimeException("not supported");
    }
    return result;
  }
  
  private RuntimeContext convert(long revision) {
    com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext runtimeContext;
    try {
      runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
        .getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }

    return convert(runtimeContext);
  }
  
  private long getRevision(RuntimeContext rc) {
    try {
      if (rc instanceof RuntimeApplication) {
        return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRevision( rc.getName() , ((RuntimeApplication) rc).getVersion(), null);
      } else if (rc instanceof Workspace) {
        return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRevision(null, null, rc.getName());
      } else {
        throw new RuntimeException("Not Supported runtime context");
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
  }
  
  private long getRevision(com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext rc) {
    
    RuntimeDependencyContextType type = rc.getRuntimeDependencyContextType();
    try {
      if (type.equals(RuntimeDependencyContextType.Application)) {
        return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRevision( rc.getName() , ((Application) rc).getVersionName(), null);
      } else if (type.equals(RuntimeDependencyContextType.Workspace)) {
        return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRevision(null, null, rc.getName());
      } else {
        throw new RuntimeException("Not Supported runtime context");
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
  }
  
  private static String getTypeFromStringParameter(StringParameter<?> sp) {
    
    String typename = sp.getSimpleTypeString();
    if (sp instanceof StringParameterWithEnum) {
      StringParameterWithEnum<?,?> spwe = (StringParameterWithEnum<?,?>) sp;
      if (spwe.hasDocumentedEnum()) {
        typename = spwe.getSimpleTypeString();
      } else {
        String enums = spwe.getEnumConstantsAsStrings().toString();
        typename = spwe.getSimpleTypeString()+"("+ enums.substring(1,enums.length()-1) +")";
      }
    }
    return typename;
  }
  
  private static String getMandatoryFromStringParameter(StringParameter<?> sp) {
    
    if (sp.isMandatory()) {
      return "mandatory";
    } else if (sp.isMandatoryFor()) {
      StringBuilder sb = new StringBuilder();
      sb.append("mandatory for [");
      String sep = "";
      List<SerializablePair<String,Serializable>> mandatoryFor = sp.getMandatoryFor();
      for (SerializablePair<String,Serializable> pair : mandatoryFor) {
        sb.append(sep).append(pair.getFirst());
        if (pair.getSecond() != null) {
          if (pair.getSecond() instanceof String) {
            sb.append("=\"").append(pair.getSecond()).append("\"");
          } else {
            sb.append("=").append(pair.getSecond());
          }
        }
        sep = ", ";
      }
      sb.append("]");
      return sb.toString();
    } else {
      return "";
    }
  }
}
