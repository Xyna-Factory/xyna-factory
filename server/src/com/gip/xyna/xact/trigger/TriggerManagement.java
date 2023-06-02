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
package com.gip.xyna.xact.trigger;



import java.util.List;
import java.util.zip.ZipInputStream;

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
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilterInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_ExclusiveDeploymentInProgress;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;



public interface TriggerManagement {

  public void addTrigger(String triggerName, ZipInputStream jarFiles, String fqTriggerClassName, String[] sharedLibs,
                         String description, String startParameterDocumentation, long revision) throws XynaException;


  public void removeTrigger(String triggerName) throws XACT_TriggerNotFound,
                  XACT_TriggerMayNotBeRemovedIsDeployedException, PersistenceLayerException;

  public void removeTrigger(String triggerName, Long revision) throws XACT_TriggerNotFound,
                  XACT_TriggerMayNotBeRemovedIsDeployedException, PersistenceLayerException;


  public void deployTrigger(String triggerName, String nameOfTriggerInstance, String[] startParameter,
                            String description, long revision) throws XACT_TriggerNotFound, XACT_IncompatibleTriggerImplException,
      XACT_TriggerImplClassNotFoundException, XACT_InvalidStartParameterException, PersistenceLayerException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, 
      XACT_TriggerCouldNotBeStartedException, XACT_AdditionalDependencyDeploymentException,
      XACT_TriggerInstanceNeedsEnabledTriggerException;


  public void undeployTrigger(String nameOfTrigger, String nameOfTriggerInstance) throws XACT_TriggerNotFound,
                  PersistenceLayerException, XACT_TriggerInstanceNotFound;

  
  public void addFilter(String filterName, ZipInputStream jarFiles, String fqFilterClassName, String triggerName, String[] sharedLibs,
                        String description, long revision) throws XPRC_ExclusiveDeploymentInProgress, XACT_FilterImplClassNotFoundException,
                        XACT_IncompatibleFilterImplException, XACT_TriggerNotFound, PersistenceLayerException, Ex_FileAccessException,
                        XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException, XACT_AdditionalDependencyDeploymentException,
                        XACT_JarFileUnzipProblem, XACT_LibOfFilterImplNotFoundException, XACT_OldFilterVersionInstantiationException,
                        XFMG_SHARED_LIB_NOT_FOUND;


  public void removeFilter(String filterName) throws XACT_FilterNotFound, XACT_FilterMayNotBeRemovedIsDeployedException,
      PersistenceLayerException;

  public void removeFilter(String filterName, Long revision) throws XACT_FilterNotFound, XACT_FilterMayNotBeRemovedIsDeployedException,
      PersistenceLayerException;


  public void removeFilterWithUndeployingInstances(String filterName) throws XACT_FilterNotFound,
      PersistenceLayerException;

  public void deployFilter(DeployFilterParameter deployFilterParameter)
      throws XACT_FilterImplClassNotFoundException, XACT_IncompatibleFilterImplException, XACT_FilterNotFound, PersistenceLayerException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException, XACT_InvalidFilterConfigurationParameterValueException;
  
  @Deprecated
  public void deployFilter(String filtername, String nameOfFilterInstance, String nameOfTriggerInstance, String description, long revision)
      throws XACT_FilterImplClassNotFoundException, XACT_IncompatibleFilterImplException, XACT_FilterNotFound, PersistenceLayerException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException;


  public void undeployFilter(String filterName) throws XACT_FilterNotFound, PersistenceLayerException;


  public Trigger[] getTriggers();


  public Filter[] getFilters(String triggerName);
  
  
  public List<TriggerInformation> listTriggerInformation() throws PersistenceLayerException;
  
  
  public List<FilterInformation> listFilterInformation() throws PersistenceLayerException;


  public EventListenerInstance<?, ?>[] getTriggerInstances(String triggerName) throws XACT_TriggerNotFound;

  public EventListener<?, ?> getTriggerInstance(TriggerInstanceIdentification triggerInstanceId) throws XACT_TriggerNotFound;

  public ConnectionFilterInstance<?>[] getFilterInstances(String filterName);


  public void configureTriggerMaxEvents(String triggerInstanceName, long maxNumberEvents, boolean autoReject)
                  throws XACT_TriggerInstanceNotFound, PersistenceLayerException;
  
  
  public boolean disableTriggerInstance(String triggerInstanceName) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XACT_TriggerNotFound, XACT_TriggerInstanceNotFound;


  public boolean enableTriggerInstance(String triggerInstanceName) throws PersistenceLayerException,
      XACT_IncompatibleTriggerImplException,
      XACT_TriggerImplClassNotFoundException, XACT_TriggerNotFound, XACT_InvalidStartParameterException,
      XACT_LibOfTriggerImplNotFoundException,
      XACT_TriggerCouldNotBeStartedException, XACT_AdditionalDependencyDeploymentException, XFMG_SHARED_LIB_NOT_FOUND,
      XACT_TriggerInstanceNeedsEnabledTriggerException, XACT_TriggerInstanceNotFound;
  

  public boolean enableFilterInstance(String filterInstanceName) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XACT_FilterImplClassNotFoundException,
      XACT_IncompatibleFilterImplException, XACT_FilterNotFound, XACT_TriggerInstanceNotFound,
      XACT_LibOfFilterImplNotFoundException, XFMG_SHARED_LIB_NOT_FOUND,
      XACT_FilterInstanceNeedsEnabledFilterException;

  
  
}
