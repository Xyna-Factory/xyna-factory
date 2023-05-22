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

package com.gip.xyna.xact;



import java.io.File;
import java.util.List;
import java.util.zip.ZipInputStream;

import com.gip.xyna.Department;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_AdditionalDependencyDeploymentException;
import com.gip.xyna.xact.exceptions.XACT_FilterImplClassNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_FilterInstanceNeedsEnabledFilterException;
import com.gip.xyna.xact.exceptions.XACT_FilterMayNotBeRemovedIsDeployedException;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_IncompatibleFilterImplException;
import com.gip.xyna.xact.exceptions.XACT_IncompatibleTriggerImplException;
import com.gip.xyna.xact.exceptions.XACT_InvalidFilterConfigurationParameterValueException;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterException;
import com.gip.xyna.xact.exceptions.XACT_JarFileUnzipProblem;
import com.gip.xyna.xact.exceptions.XACT_LibOfFilterImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_LibOfTriggerImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_OldFilterVersionInstantiationException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerImplClassNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNeedsEnabledTriggerException;
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerMayNotBeRemovedIsDeployedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.DeployFilterParameter;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.FilterInformation;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.TriggerInformation;
import com.gip.xyna.xact.trigger.TriggerInstanceIdentification;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.SingleRepositoryEvent;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilterInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_ExclusiveDeploymentInProgress;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;



public abstract class XynaActivationBase extends Department implements XynaActivationPortal {

  public XynaActivationBase() throws XynaException {
    super();
  }


  public abstract XynaActivationTrigger getActivationTrigger();


  public void addFilter(String filterName, ZipInputStream jarFiles, String fqFilterClassName, String triggerName, String[] sharedLibs,
                        String description, long revision) throws XPRC_ExclusiveDeploymentInProgress, XACT_FilterImplClassNotFoundException,
      XACT_IncompatibleFilterImplException, XACT_TriggerNotFound, PersistenceLayerException, Ex_FileAccessException,
      XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException, XACT_AdditionalDependencyDeploymentException,
      XACT_JarFileUnzipProblem, XACT_LibOfFilterImplNotFoundException, XACT_OldFilterVersionInstantiationException,
      XFMG_SHARED_LIB_NOT_FOUND {
    getActivationTrigger().addFilter(filterName, jarFiles, fqFilterClassName, triggerName, sharedLibs, description, revision);
  }


  public void removeFilter(String filterName) throws XACT_FilterNotFound,
                  XACT_FilterMayNotBeRemovedIsDeployedException, PersistenceLayerException {
    getActivationTrigger().removeFilter(filterName);
  }

  public void removeFilter(String filterName, Long revision) throws XACT_FilterNotFound,
                  XACT_FilterMayNotBeRemovedIsDeployedException, PersistenceLayerException {
    getActivationTrigger().removeFilter(filterName, revision);
  }
  

  public void removeFilterWithUndeployingInstances(String filterName) throws XACT_FilterNotFound,
      PersistenceLayerException {
    getActivationTrigger().removeFilterWithUndeployingInstances(filterName);
  }


  
  public void addTrigger(String triggerName, ZipInputStream jarFiles, String fqTriggerClassName, String[] sharedLibs,
                         String description, String startParameterDocumentation, long revision)
      throws XynaException {
    getActivationTrigger().addTrigger(triggerName, jarFiles, fqTriggerClassName, sharedLibs, description,
                                      startParameterDocumentation, revision);
  }


  public void addTrigger(String name, File[] jarFiles, String fqTriggerClassName, String[] sharedLibs)
      throws XynaException {
    getActivationTrigger().addTrigger(name, jarFiles, fqTriggerClassName, sharedLibs);
  }

  public void addTrigger(String name, File[] jarFiles, String fqTriggerClassName, String[] sharedLibs, Long revision)
                  throws XynaException {
    getActivationTrigger().addTrigger(name, jarFiles, fqTriggerClassName, sharedLibs, revision, new SingleRepositoryEvent(revision));
  }


  public void removeTrigger(String triggerName) throws XACT_TriggerNotFound,
                  XACT_TriggerMayNotBeRemovedIsDeployedException, PersistenceLayerException {
    getActivationTrigger().removeTrigger(triggerName);
  }

  public void removeTrigger(String triggerName, Long revision) throws XACT_TriggerNotFound,
                  XACT_TriggerMayNotBeRemovedIsDeployedException, PersistenceLayerException {
    getActivationTrigger().removeTrigger(triggerName, revision);
  }


  public void deployFilter(DeployFilterParameter deployFilterParameter)
      throws XACT_FilterImplClassNotFoundException, XACT_IncompatibleFilterImplException, XACT_FilterNotFound, PersistenceLayerException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException, XACT_InvalidFilterConfigurationParameterValueException {
    getActivationTrigger().deployFilter(deployFilterParameter);
  }
  @Deprecated
  public void deployFilter(String filtername, String nameOfFilterInstance, String nameOfTriggerInstance, String description, long revision)
      throws XACT_FilterImplClassNotFoundException, XACT_IncompatibleFilterImplException, XACT_FilterNotFound, PersistenceLayerException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException {
    getActivationTrigger().deployFilter(filtername, nameOfFilterInstance, nameOfTriggerInstance, description, revision);
  }


  public void undeployFilter(String filterName) throws XACT_FilterNotFound, PersistenceLayerException {
    getActivationTrigger().undeployFilter(filterName);
  }


  public void deployTrigger(String triggerName, String nameOfTriggerInstance, String[] startParameter,
                            String description, long revision) throws XACT_IncompatibleTriggerImplException,
                  XACT_TriggerImplClassNotFoundException, XACT_TriggerNotFound, XACT_InvalidStartParameterException,
                  PersistenceLayerException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException,
                  XACT_TriggerCouldNotBeStartedException,
                  XACT_AdditionalDependencyDeploymentException, XACT_TriggerInstanceNeedsEnabledTriggerException {
    getActivationTrigger().deployTrigger(triggerName, nameOfTriggerInstance, startParameter, description, revision);
  }


  public void undeployTrigger(String nameOfTrigger, String nameOfTriggerInstance) throws XACT_TriggerNotFound,
                  PersistenceLayerException, XACT_TriggerInstanceNotFound {
    getActivationTrigger().undeployTrigger(nameOfTrigger, nameOfTriggerInstance);
  }


  public ConnectionFilterInstance[] getFilterInstances(String filterName) {
    return getActivationTrigger().getFilterInstances(filterName);
  }

  
  public Filter[] getFilters(String triggerName) {
    return getActivationTrigger().getFilters(triggerName);
  }


  public EventListenerInstance[] getTriggerInstances(String triggerName) throws XACT_TriggerNotFound {
    return getActivationTrigger().getTriggerInstances(triggerName);
  }


  public Trigger[] getTriggers() {
    return getActivationTrigger().getTriggers(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  public EventListener<?, ?> getTriggerInstance(TriggerInstanceIdentification triggerInstanceId) throws XACT_TriggerNotFound {
    return getActivationTrigger().getTriggerInstance(triggerInstanceId);
  }


  public abstract ODS getXynaActivationODS();


  public void configureTriggerMaxEvents(String triggerInstanceName, long maxNumberEvents, boolean autoReject)
                  throws XACT_TriggerInstanceNotFound, PersistenceLayerException {
    getActivationTrigger().configureTriggerMaxEvents(triggerInstanceName, maxNumberEvents, autoReject);
  }
  

  public boolean enableFilterInstance(String filterInstanceName) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XACT_FilterImplClassNotFoundException,
      XACT_IncompatibleFilterImplException, XACT_FilterNotFound, XACT_TriggerInstanceNotFound,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException, 
      XACT_FilterInstanceNeedsEnabledFilterException {
    return getActivationTrigger().enableFilterInstance(filterInstanceName);
  }
  

  public boolean disableTriggerInstance(String triggerInstanceName) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XACT_TriggerNotFound, XACT_TriggerInstanceNotFound {
    return getActivationTrigger().disableTriggerInstance(triggerInstanceName);
  };


  public boolean enableTriggerInstance(String triggerInstanceName) throws PersistenceLayerException,
      XACT_IncompatibleTriggerImplException,
      XACT_TriggerImplClassNotFoundException, XACT_TriggerNotFound, XACT_InvalidStartParameterException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, 
      XACT_TriggerCouldNotBeStartedException, XACT_AdditionalDependencyDeploymentException,
      XACT_TriggerInstanceNeedsEnabledTriggerException, XACT_TriggerInstanceNotFound {
    return getActivationTrigger().enableTriggerInstance(triggerInstanceName);
  };

  
  public List<TriggerInformation> listTriggerInformation() throws PersistenceLayerException {
    return getActivationTrigger().listTriggerInformation();
  }


  public List<FilterInformation> listFilterInformation() throws PersistenceLayerException {
    return getActivationTrigger().listFilterInformation();
  }
}
