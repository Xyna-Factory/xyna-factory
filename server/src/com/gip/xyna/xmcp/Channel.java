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
package com.gip.xyna.xmcp;



import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.XynaActivationPortal;
import com.gip.xyna.xact.exceptions.XACT_JarFileUnzipProblem;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.RepositoryEvent;
import com.gip.xyna.xdev.XynaDevelopmentPortal;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.XynaFactoryManagementPortal;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassProvider;
import com.gip.xyna.xmcp.xguisupport.XGUISupportPortal;
import com.gip.xyna.xnwh.XynaFactoryWarehousePortal;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessingPortal;
import com.gip.xyna.xprc.exceptions.MiProcessingRejected;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_ExecutionDestinationMissingException;
import com.gip.xyna.xprc.exceptions.XPRC_FACTORY_IS_SHUTTING_DOWN;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_MONITORING_TYPE;
import com.gip.xyna.xprc.exceptions.XPRC_IllegalManualInteractionResponse;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_ResumeFailedException;
import com.gip.xyna.xprc.exceptions.XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xpce.manualinteraction.IManualInteraction.ProcessManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xpce.statustracking.IStatusChangeListener;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchiveStatisticsStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;



public interface Channel
                extends
                  XynaActivationPortal,
                  XynaProcessingPortal,
                  XynaFactoryManagementPortal,
                  XynaDevelopmentPortal,
                  XynaFactoryWarehousePortal,
                  XGUISupportPortal {

  public void deployWF(String fqXmlName, WorkflowProtectionMode mode) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_InvalidPackageNameException, XPRC_DeploymentDuringUndeploymentException,
                  XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException;
  
  
  
  
  public void undeployWF(String fqXMLName, boolean disableChecks) throws XynaException;
  
  public void undeployWF(String originalFqName, boolean undeployDependentObjects, boolean disableChecks) throws XynaException;


  public void deployMDM(String fqXmlName, WorkflowProtectionMode mode, String fileName, InputStream inputStream) throws 
                  XACT_JarFileUnzipProblem, XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT,
                  XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException, Ex_FileAccessException;


  public void deployDatatype(String fqXmlName, WorkflowProtectionMode mode, Map<String, InputStream> jars) throws XynaException;


  public void undeployMDM(String fqXmlName, boolean undeployDependendObjects, boolean disableChecks) throws XynaException;


  public void deployException(String fqXmlName, WorkflowProtectionMode mode) throws XynaException;


  public void undeployException(String originalFqName, boolean undeployDependendObjects, boolean disableChecks) throws XynaException;
  
  public void undeployException(String fqXmlName, boolean disableChecks) throws XynaException;


  public Map<String, OrderArchiveStatisticsStorable> getCompleteCallStatistics();


  public OrderInstanceDetails getOrderInstanceDetails(Long id) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


  public Map<Long, ManualInteractionEntry> listManualInteractionEntries() throws PersistenceLayerException;


  public void addStatusChangeListener(ClassProvider c, IStatusChangeListener listener);


  public void removeStatusChangeListener(IStatusChangeListener listener);


  @Deprecated
  public ProcessManualInteractionResult processManualInteractionEntry(Long id, GeneralXynaObject response)
      throws XPRC_FACTORY_IS_SHUTTING_DOWN, PersistenceLayerException, XPRC_ResumeFailedException,
      XPRC_IllegalManualInteractionResponse;


  public ProcessManualInteractionResult processManualInteraction(Long id, GeneralXynaObject response)
      throws PersistenceLayerException, XPRC_ResumeFailedException, XPRC_IllegalManualInteractionResponse,
      MiProcessingRejected;


  public void setDefaultMonitoringLevel(Integer code) throws PersistenceLayerException;


  public void setMonitoringLevel(String string, Integer code) throws XPRC_INVALID_MONITORING_TYPE,
      PersistenceLayerException, XPRC_ExecutionDestinationMissingException;


  public Integer getMonitoringLevel(String string);


  @Deprecated
  public InputStream getFilterImplTemplate(String baseDir, String filterName, String fqTriggerClassName,
                                           boolean deleteFilterImplAfterStreamClose, boolean deleteBaseDir)
      throws XynaException;

  public InputStream getFilterImplTemplate(String baseDir, String filterName, String fqTriggerClassName, Long revision,
                                           boolean deleteFilterImplAfterStreamClose, boolean deleteBaseDir)
                                                           throws XynaException;

  @Deprecated
  public InputStream getServiceImplTemplate(String baseDir, String implementationString,
                                            boolean deleteServiceImplAfterStreamClose, boolean deleteBaseDir)
      throws XynaException;

  public InputStream getServiceImplTemplate(String baseDir, String implementationString, Long revision,
                                            boolean deleteServiceImplAfterStreamClose, boolean deleteBaseDir)
                                                            throws XynaException;

  @Deprecated
  public InputStream getServiceImplTemplate(String fqClassNameDOM, boolean deleteServiceImplAfterStreamClose)
      throws XynaException;

  public InputStream getServiceImplTemplate(String fqClassNameDOM, Long revision, boolean deleteServiceImplAfterStreamClose)
      throws XynaException;


  public InputStream getPythonServiceImplTemplate(String fqClassNameDOM, Long revision, boolean deleteServiceImplAfterStreamClose)
          throws XynaException;


  public InputStream getTriggerImplTemplate(String triggerName, boolean deleteTriggerImplAfterStreamClose)
      throws XynaException;


  public InputStream getFilterImplTemplate(String filterName, String triggerName, boolean service)
      throws XynaException;

  @Deprecated
  public InputStream getTriggerImplTemplate(String baseDir, String implementationString,
                                            boolean deleteTriggerImplAfterStreamClose, boolean deleteBaseDir)
      throws XynaException;

  public InputStream getTriggerImplTemplate(String baseDir, String implementationString, Long revision,
                                            boolean deleteTriggerImplAfterStreamClose, boolean deleteBaseDir)
                                                            throws XynaException;


  public String getCrossDomainXML();


  public String getMinimalCrossDomainXML(String ip, String hostname, String port);

  @Deprecated
  public String saveMDM(String xml) throws XynaException;

  public String saveMDM(String xml, Long revision) throws XynaException;

  public String saveMDM(String xml, Long revision, RepositoryEvent repositoryEvent) throws XynaException;


  public void unsecureDeleteSavedMDM(String fqClassName) throws XynaException;


  public void removeMonitoringLevel(String orderType) throws PersistenceLayerException, XPRC_DESTINATION_NOT_FOUND;


  public String getRunningProcessDetailsXML(Long id) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


  public Map<Long, ManualInteractionEntry> listManualInteractionEntries(int maxRows) throws PersistenceLayerException;

  
  public Map<String, Long> getCallStatistics();


  public Map<String, Long> getFinishedStatistics();


  public Map<String, Long> getErrorStatistics();


  public Map<String, Long> getTimeoutStatistics();
  
  
  public List<SharedLib> listSharedLibs(long revision);
  
  public List<SharedLib> listAllSharedLibs();

}
