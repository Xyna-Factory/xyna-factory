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

package com.gip.xyna.xprc.xpce;



import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_JarFileUnzipProblem;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;
import com.gip.xyna.xprc.exceptions.XPRC_ExclusiveDeploymentInProgress;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InternalObjectMayNotBeUndeployedException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DependentObjectMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.SpecialPurposeHelper;
import com.gip.xyna.xprc.xpce.monitoring.EngineSpecificStepHandlerManager;



public interface WorkflowEngine {

  public String getDefaultName();


  public void deployMultiple(Map<XMOMType, List<String>> deploymentItems, WorkflowProtectionMode mode, Long revision) 
                  throws MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException,
                  XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException;
  
  public void deployDatatype(String fqXmlName, WorkflowProtectionMode mode, String fileName, InputStream inputStream)
                  throws Ex_FileAccessException, XACT_JarFileUnzipProblem,
                  XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT, XPRC_InvalidPackageNameException,
                  XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_DeploymentDuringUndeploymentException,
                  XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException;
  
  public void deployDatatype(String fqXmlName, WorkflowProtectionMode mode, String fileName, InputStream inputStream, Long revision)
                  throws Ex_FileAccessException, XACT_JarFileUnzipProblem,
                  XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT, XPRC_InvalidPackageNameException,
                  XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_DeploymentDuringUndeploymentException,
                  XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException;


  /**
   * Deploys the datatype and all dependent datatypes and workflows with codeChanged mode
   */
  public void deployDatatypeAndDependants(String fqXmlName, String fileName, InputStream inputStream)
                  throws Ex_FileAccessException, XACT_JarFileUnzipProblem,
                  XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT, XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT,
                  XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException;


  @Deprecated
  public void deployDatatype(String fqXmlName, WorkflowProtectionMode mode, Map<String, InputStream> jars) throws Ex_FileAccessException,
                  XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException;
  
  public void deployDatatype(String fqXmlName, WorkflowProtectionMode mode, Map<String, InputStream> jars, Long revision) throws Ex_FileAccessException,
                  XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException;

  @Deprecated
  public void undeployDatatype(String fqXmlName, boolean undeployDependendObjects, boolean disableChecks)
                  throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                  XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;
  
  public void undeployDatatype(String fqXmlName, boolean undeployDependendObjects, boolean disableChecks, Long revision)
                  throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                  XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;

  @Deprecated
  public void deployWorkflow(String fqXmlName, WorkflowProtectionMode mode) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_InvalidPackageNameException, XPRC_DeploymentDuringUndeploymentException,
                  XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException;
  

  public void deployWorkflow(String fqXmlName, WorkflowProtectionMode mode, Long revision)
                  throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException;


  /**
   * Deploys the workflow and all dependent datatypes and workflows with codeChanged mode
   */
  @Deprecated
  public void deployWorkflowAndDependants(String fqXmlName) throws XPRC_DeploymentDuringUndeploymentException,
                  XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException,
                  XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException;
  
  @Deprecated
  public void undeployWorkflow(String originalFqName, boolean undeployDependentObjects, boolean disableChecks) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException,
                  XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;
  

  public void undeployWorkflow(String originalFqName, boolean undeployDependentObjects, boolean disableChecks,
                               Long revision) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException,
                  XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;

  public void undeployXMOMObject(String originalFqName, XMOMType type, DependentObjectMode dependentObjectMode, boolean disableChecks,
                                 Long revision) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException,
                  XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;

  @Deprecated
  public void deleteWorkflow(String originalFqName, boolean disableChecks,
                             boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies)
                  throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;

  public void deleteWorkflow(String originalFqName, boolean disableChecks,
                             boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies, boolean checkDeploymentLock,
                             Long revision)
                  throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;

  @Deprecated
  public void deleteDatatype(String originalFqName, boolean disableChecks,
                             boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies)
                  throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;

  public void deleteDatatype(String originalFqName, boolean disableChecks,
                             boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies, boolean checkDeploymentLock,
                             Long revision)
                  throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;

  @Deprecated
  public void deleteException(String originalFqName, boolean disableChecks,
                              boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies)
                  throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;

  public void deleteException(String originalFqName, boolean disableChecks,
                              boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies, boolean checkDeploymentLock,
                              Long revision)
                  throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;

  /**
   * Gibt aus der übergebenen Liste alle WORKFLOWs, EXCEPTIONs und DATATYPEs zurück, die bereits in der Revision vorliegen
   * @param xmomObjects
   * @param revision
   * @return
   */
  public Map<XMOMType,List<String>> existsInRevision( Map<XMOMType,List<String>> xmomObjects, Long revision);

  /**
   * Kopiert aus der übergebenen Liste alle WORKFLOWs, EXCEPTIONs und DATATYPEs aus einer Revision 
   * in die andere und deploy anschließend.
   * @param xmomObjects
   * @param fromRevision
   * @param toRevision
   * @throws Ex_FileAccessException 
   * @throws XPRC_InvalidPackageNameException 
   * @throws XPRC_DeploymentDuringUndeploymentException 
   * @throws MDMParallelDeploymentException 
   */
  public void copyToRevisionAndDeploy( Map<XMOMType,List<String>> xmomObjects, Long fromRevision, Long toRevision,
                                       DeploymentMode deploymentMode, WorkflowProtectionMode wpm, boolean inheritCodeChanged ) 
                                           throws Ex_FileAccessException, XPRC_InvalidPackageNameException, 
                                           MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException;
  
  public void copyToRevisionAndDeploy( Map<XMOMType,List<String>> xmomObjects, Long fromRevision, Long toRevision,
                                       DeploymentMode deploymentMode, WorkflowProtectionMode wpm, boolean inheritCodeChanged,
                                       String username, String sessionId, String comment) 
                                           throws Ex_FileAccessException, XPRC_InvalidPackageNameException, 
                                           MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException;

  public void deleteXMOMObjects( Map<XMOMType,List<String>> xmomObjects, boolean disableChecks,
                                 DependentObjectMode dependentObjectMode, boolean checkDeploymentLock, Long revision ) 
                                     throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;

  
  public void deleteXMOMObject(String fullXmlName, XMOMType type, boolean disableChecks,
                               DependentObjectMode dependentObjectMode, boolean checkDeploymentLock,
                               Long revision)
                  throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;


  public EngineSpecificWorkflowProcessor getPlanningProcessor();


  public EngineSpecificWorkflowProcessor getExecutionProcessor();


  public EngineSpecificWorkflowProcessor getCleanupProcessor();

  
  public SpecialPurposeHelper getSpecialPurposeHelper();
  

  public DeploymentHandling getDeploymentHandling();


  public EngineSpecificStepHandlerManager getStepHandlerManager();


  public int getNumberOfRunningProcesses();


  public void deployException(String fqClassName, WorkflowProtectionMode mode)
                  throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException;
  
  public void deployExceptionAndDependants(String fqXmlName) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_InvalidPackageNameException, XPRC_DeploymentDuringUndeploymentException,
                  XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException;


  public void undeployException(String originalFqName, boolean undeployDependentObjects, boolean disableChecks)
                  throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                  XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;


  public void deployException(String fqClassName, WorkflowProtectionMode mode, Long revision)
                  throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException;


  public void undeployException(String originalFqName, boolean undeployDependentObjects, boolean disableChecks,
                                Long revision) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException,
                  XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;


  public OrderContext setOrderContext(OrderContextServerExtension ctx);


  public OrderContext removeOrderContext();


  public OrderContext getOrderContext();
  
  
  public boolean checkForActiveOrders() throws XynaException;

}
