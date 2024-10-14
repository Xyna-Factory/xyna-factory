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

package com.gip.xyna.xdev.xdelivery;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XMOM.base.IP;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.update.Updater;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_AdditionalDependencyDeploymentException;
import com.gip.xyna.xact.exceptions.XACT_DuplicateFilterDefinitionException;
import com.gip.xyna.xact.exceptions.XACT_ErrorDuringFilterAdditionRollback;
import com.gip.xyna.xact.exceptions.XACT_FilterImplClassNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_IncompatibleFilterImplException;
import com.gip.xyna.xact.exceptions.XACT_IncompatibleTriggerImplException;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterException;
import com.gip.xyna.xact.exceptions.XACT_JarFileUnzipProblem;
import com.gip.xyna.xact.exceptions.XACT_LibOfFilterImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_LibOfTriggerImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_StartParameterDefinitionsChangedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerImplClassNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xdev.exceptions.XDEV_ExistingTriggerHasIncompatibleStartParametersException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_IllegalPropertyValueException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCapacityCardinality;
import com.gip.xyna.xfmg.exceptions.XFMG_NameContainsInvalidCharacter;
import com.gip.xyna.xfmg.exceptions.XFMG_NamingConventionException;
import com.gip.xyna.xfmg.exceptions.XFMG_PasswordRestrictionViolation;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RightDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.exceptions.XFMG_UserDoesNotExistException;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xopctrl.radius.PresharedKey;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSDomainSpecificData;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSServer;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSServerPort;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.exceptions.XPRC_CAPACITY_ALREADY_DEFINED;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_ExclusiveDeploymentInProgress;
import com.gip.xyna.xprc.exceptions.XPRC_ExecutionDestinationMissingException;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_MONITORING_TYPE;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.exceptions.XPRC_WorkflowProtectionModeViolationException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.Presence;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.WorkflowRevision;
import com.gip.xyna.xprc.xfractwfe.DeploymentProcess.OrderFilterDeployment;
import com.gip.xyna.xprc.xfractwfe.OrderFilterAlgorithmsImpl.OrderFilter;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xsched.CapacityManagement;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;



public class ImportDeliveryItem {

  private static Logger logger = CentralFactoryLogging.getLogger(ImportDeliveryItem.class);

  private boolean verboseOutput = false;

  private InputStream inputFile;

  // Used for output of console information
  private PrintStream logStream;


  /**
   * @param packageFile xyna package
   * @param logStream the stream to which the status/log information is written
   * @throws FileNotFoundException
   */
  public ImportDeliveryItem(File packageFile, OutputStream logStream) throws FileNotFoundException {
    this(new FileInputStream(packageFile), logStream);
    this.logStream.println("Reading package file: " + packageFile.getAbsolutePath());
  }


  /**
   * @param packageStream the stream from which the xyna package is read
   * @param logStream the stream to which the status/log information is written
   */
  public ImportDeliveryItem(InputStream packageStream, OutputStream logStream) {
    this.inputFile = packageStream;
    try {
      this.logStream = new PrintStream(logStream, true, Constants.DEFAULT_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Check the xyna factory version number from delivery.xml against the version number of the current xyna factory.
   * Only prints warning if both version numbers don't match.
   * @param versionNumberElement xml element storing the version number of the xyna package
   */
  private void importVersionNumber(Element versionNumberElement) throws XPRC_VERSION_DETECTION_PROBLEM,
      PersistenceLayerException {
    String versionNumber = Updater.getInstance().getFactoryVersion().getString();
    if (versionNumberElement.getTextContent().trim().equals(versionNumber)) {
      if (verboseOutput)
        logStream.println("Reading version number " + versionNumber);
    } else {
      logStream.println("Warning: Version numbers differ");
      logStream.println("Version number of package: " + versionNumberElement.getTextContent().trim());
      logStream.println("Version number of xyna factory: " + versionNumber);
    }
  }


  /**
   * Deploy every data type from the list if its status is deployed. Data types in status saved are ignored.
   * @param dataTypeElements list of xml elements representing data types
   */
  private void importDataTypes(List<Element> dataTypeElements) throws XPRC_DeploymentDuringUndeploymentException,
      XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH {

    for (Element dataType : dataTypeElements) {
      if (dataType.getTagName().equals(DeliveryItemConstants.DATA_TYPE_ELEMENT)) {
        // deploy data type
        if (verboseOutput) {
          logStream.print("Deploying data type " + dataType.getTextContent().trim() + "... ");
        }

        try {
          XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal()
              .deployMDM(dataType.getTextContent().trim(), WorkflowProtectionMode.FORCE_DEPLOYMENT, null, null);
        } catch (DOMException e) {
          throw new RuntimeException(e);
        } catch (XynaException e) {
          throw new RuntimeException(e);
        }

        if (verboseOutput) {
          logStream.println("done");
        }
      }
    }
  }


  /**
   * Deploy every exception from the list if its status is deployed. Exceptions in status saved are ignored.
   * @param exceptionElements list of xml elements representing exceptions
   */
  private void importExceptions(List<Element> exceptionElements) throws XPRC_DeploymentDuringUndeploymentException,
      XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH {

    for (Element exception : exceptionElements) {
      if (exception.getTagName().equals(DeliveryItemConstants.EXCEPTION_ELEMENT)) {
        // deploy exception
        if (verboseOutput) {
          logStream.print("Deploying exception " + exception.getTextContent().trim() + "... ");
        }

        try {
          XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal()
              .deployException(exception.getTextContent().trim(), WorkflowProtectionMode.FORCE_DEPLOYMENT);
        } catch (DOMException e) {
          throw new RuntimeException(e);
        } catch (XynaException e) {
          throw new RuntimeException(e);
        }

        if (verboseOutput) {
          logStream.println("done");
        }
      }
    }
  }

  
  /**
   * Deploy every workflow from the list if its status is deployed. Workflows in status saved are ignored.
   * @param workflowElements list of xml elements representing workflows
   */
  private void importWorkflows(List<Element> workflowElements) throws XPRC_DeploymentDuringUndeploymentException,
      XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH {

    GenerationBase.removeFromCache = false;
    try {
      for (Element workflow : workflowElements) {
        if (workflow.getTagName().equals(DeliveryItemConstants.WORKFLOW_ELEMENT)) {
          if (workflow.getAttribute(DeliveryItemConstants.WORKFLOW_DEPLOY_STATE_ATTR).trim()
              .equals(DeliveryItemConstants.WORKFLOW_DEPLOYED)) {
            // deploy workflow
            if (verboseOutput) {
              logStream.print("Deploying workflow " + workflow.getTextContent().trim() + "... ");
            }
            try {
              XynaFactory.getInstance().getProcessing().getWorkflowEngine()
                  .deployWorkflowAndDependants(workflow.getTextContent().trim());
            } catch (XPRC_InvalidPackageNameException e1) {
              throw new RuntimeException(e1);
            }
            if (verboseOutput) {
              logStream.println("done");
            }
          }
          // Unzipping saved workflows is sufficient. Nothing to do.
        } else {
          logStream.println("Warning: attribute '" + DeliveryItemConstants.WORKFLOW_DEPLOY_STATE_ATTR + "' is set to "
              + workflow.getAttribute(DeliveryItemConstants.WORKFLOW_DEPLOY_STATE_ATTR).trim() + " for workflow '"
              + workflow.getTextContent().trim() + "'");
          logger.warn("Delivery item xml file contains unknown elements");
        }
      }
    } finally {
      GenerationBase.removeFromCache = true;
      GenerationBase.clearGlobalCache();
    }

  }


  /**
   * Start all cron like orders from the list
   * @param cronLikeOrderElements list of xml elements representing cron like orders
   */
  private void importCronLikeOrders(List<Element> cronLikeOrderElements) throws XynaException {

    for (Element cronLikeOrder : cronLikeOrderElements) {
      if (cronLikeOrder.getTagName().equals(DeliveryItemConstants.CRONLS_ELEMENT)) {
        Long interval = Long.valueOf(cronLikeOrder.getAttribute(DeliveryItemConstants.CRONLS_INTERVAL_ATTR).trim());
        Long startTime = Long.valueOf(cronLikeOrder.getAttribute(DeliveryItemConstants.CRONLS_STARTTIME_ATTR).trim());
        String orderType = cronLikeOrder.getAttribute(DeliveryItemConstants.CRONLS_ORDER_ATTR).trim();
        GeneralXynaObject payloadXynaObject = null;
        if (cronLikeOrder.getElementsByTagName(DeliveryItemConstants.CRONLS_PAYLOAD_ELEMENT).getLength() > 0) {
          String payload =
              cronLikeOrder.getElementsByTagName(DeliveryItemConstants.CRONLS_PAYLOAD_ELEMENT).item(0).getNodeValue();
          if (payload != null) {
            payloadXynaObject = XynaObject.generalFromXml(payload, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
          }
        }
        if (payloadXynaObject == null) {
          payloadXynaObject = new Container();
        }
        String label = cronLikeOrder.getAttribute(DeliveryItemConstants.CRONLS_LABEL_ATTR).trim();
        OnErrorAction onError =
            OnErrorAction.valueOf(cronLikeOrder.getAttribute(DeliveryItemConstants.CRONLS_ONERROR_ATTR).toUpperCase()
                .trim());
        Boolean enabled = Boolean.valueOf(cronLikeOrder.getAttribute(DeliveryItemConstants.CRONLS_ENABLED_ATTR).trim());

        if (verboseOutput)
          logStream.print("Starting cron like order " + orderType + "... ");

        if (startTime < System.currentTimeMillis())
          startTime = System.currentTimeMillis();

        boolean failed = false;
        try {
          XynaFactory
              .getInstance()
              .getProcessing()
              .startCronLikeOrder(new CronLikeOrderCreationParameter(label, orderType, startTime, interval, enabled,
                                                                     onError, payloadXynaObject));
        } catch (XPRC_CronLikeSchedulerException e1) {
          logger.warn("Failed to start cron like order", e1);
          logStream.println("failed! See log for more information.");
          failed = true;
        }
        if (!failed && verboseOutput) {
          logStream.println("done.");
        }

      } else {
        logger.warn("Delivery item xml file contains unknown elements");
      }
    }

  }


  /**
   * Store all xyna properties from the list
   * @param propertyElements list of xml elements representing xyna properties
   * @throws XFMG_IllegalPropertyValueException 
   */
  private void importProperties(List<Element> propertyElements) throws PersistenceLayerException, XFMG_IllegalPropertyValueException {
    for (Element property : propertyElements) {
      if (property.getTagName().equals(DeliveryItemConstants.PROPERTY_ELEMENT)) {
        String key = property.getAttribute(DeliveryItemConstants.PROPERTY_KEY).trim();
        String value = property.getAttribute(DeliveryItemConstants.PROPERTY_VALUE).trim();
        if (key == null || key.length() == 0 || value == null || value.length() == 0) {
          logger.warn("Invalid property");
          logStream.println("Warning: Invalid property found (null or empty name)");
          continue;
        }
        logger.debug("Property: " + key + " = " + value);
        if (verboseOutput)
          logStream.println("Restoring property: '" + key + "' = '" + value + "'");
        XynaFactory.getPortalInstance().getFactoryManagementPortal().setProperty(key, value);
      }
    }
  }


  /**
   * Deploy all triggers and their instances.
   * @param triggerElements list of xml elements representing trigger
   */
  private void importTriggers(List<Element> triggerElements) throws XynaException {
    for (Element trigger : triggerElements) {
      if (trigger.getTagName().equals(DeliveryItemConstants.TRIGGER_ELEMENT)) {
        importTrigger(trigger);
      }
    }
  }


  /**
   * Deploy a trigger and all of its instances.
   * @param triggerElement xml element representing a trigger
   */
  private void importTrigger(Element triggerElement) throws XynaException {

    String triggerName = triggerElement.getAttribute(DeliveryItemConstants.TRIGGER_NAME_ATTR).trim();
    String fqClassName = triggerElement.getAttribute(DeliveryItemConstants.TRIGGER_FQCLASSNAME_ATTR).trim();

    if (verboseOutput) {
      logStream.println("Restoring trigger " + triggerName + " (fqClassName: " + fqClassName + ")");
    }

    List<Element> childElements = XMLUtils.getChildElements(triggerElement);

    ArrayList<File> jarFiles = new ArrayList<File>();
    ArrayList<String> sharedLibs = new ArrayList<String>();

    // first find sharedlibs and jar to add the trigger ...
    for (Element e : childElements) {
      if (e.getTagName().equals(DeliveryItemConstants.TRIGGER_SHAREDLIB_ELEMENT)) {
        String sl = e.getAttribute(DeliveryItemConstants.TRIGGER_SHAREDLIB_NAME_ATTR).trim();
        sharedLibs.add(sl);
        if (verboseOutput) {
          logStream.println("\tSharedLib: " + sl);
        }
      }

      if (e.getTagName().equals(DeliveryItemConstants.TRIGGER_JAR_ELEMENT)) {
        String jar = e.getAttribute(DeliveryItemConstants.TRIGGER_JAR_NAME_ATTR).trim();
        if(jar.startsWith("trigger")) {
          jar = jar.substring("trigger".length());
        }
        jarFiles.add(new File(RevisionManagement.getPathForRevision(PathType.TRIGGER, RevisionManagement.REVISION_DEFAULT_WORKSPACE) + jar));
        if (verboseOutput) {
          logStream.println("\tJar: " + jar);
        }
      }
    }

    // add the trigger
    if (verboseOutput) {
      logStream.print("Adding trigger...");
    }
    try {
      XynaFactory.getInstance().getActivation()
          .addTrigger(triggerName, jarFiles.toArray(new File[0]), fqClassName, sharedLibs.toArray(new String[0]));
    } catch (XACT_IncompatibleTriggerImplException e1) {
      // wenn das nicht klappt, war wohl das archiv kaputt oder es wird versucht auf einem system zu importieren, wo die
      // schnittstelle von triggern sich geändert hat
      throw new RuntimeException(e1);
    } catch (XACT_TriggerImplClassNotFoundException e1) {
      // wenn das nicht klappt, war wohl das archiv kaputt
      throw new RuntimeException(e1);
    } catch (XACT_StartParameterDefinitionsChangedException e1) {
      // wenn das nicht klappt, existierte der trigger bereits mit einer anderen startparameter implementierung
      throw new XDEV_ExistingTriggerHasIncompatibleStartParametersException(triggerName, e1);
    } catch (XACT_LibOfTriggerImplNotFoundException e) {
      // archiv kaputt
      throw new RuntimeException(e);
    } catch (Ex_FileAccessException e) {
      // archiv kaputt
      throw new RuntimeException(e);
    } catch (XPRC_XmlParsingException e) {
      // archiv kaputt
      throw new RuntimeException(e);
    } catch (XPRC_InvalidXmlMissingRequiredElementException e) {
      // archiv kaputt
      throw new RuntimeException(e);
    }
    if (verboseOutput) {
      logStream.println("done");
    }

    // ... then deploy all instances
    // find all instances
    for (Element e : childElements) {
      if (e.getTagName().equals(DeliveryItemConstants.TRIGGER_INSTANCE_ELEMENT)) {
        String instanceName = e.getAttribute(DeliveryItemConstants.TRIGGER_INSTANCE_INSTANCENAME_ATTR).trim();
        triggerName = e.getAttribute(DeliveryItemConstants.TRIGGER_INSTANCE_NAME_ATTR).trim();

        if (verboseOutput) {
          logStream.println("Restoring trigger instance " + instanceName + " for trigger " + triggerName);
        }

        // find all StartParameters
        List<Element> startParameterList = XMLUtils.getChildElements(e);
        ArrayList<String> startParameterStringList = new ArrayList<String>();
        if (verboseOutput) {
          logStream.print("  Startparameter ");
        }
        for (Element startParameter : startParameterList) {
          if (startParameter.getTagName().equals(DeliveryItemConstants.TRIGGER_INSTANCE_STARTPARAM_ELEMENT)) {
            String value =
                startParameter.getAttribute(DeliveryItemConstants.TRIGGER_INSTANCE_STARTPARAM_VALUE_ATTR).trim();
            startParameterStringList.add(value);
            if (verboseOutput) {
              logStream.print(value + " ");
            }
          }
        }
        if (verboseOutput) {
          logStream.println("");
          logStream.print("Deploying trigger...");
        }
        // deploying trigger
        try {
          XynaFactory.getPortalInstance().getActivationPortal()
              .deployTrigger(triggerName, instanceName, startParameterStringList.toArray(new String[0]), "", RevisionManagement.REVISION_DEFAULT_WORKSPACE);
        } catch (XACT_IncompatibleTriggerImplException e1) {
          // hat vor export ja auch funktioniert... evtl andere server-version?
          throw new RuntimeException(e1);
        } catch (XACT_TriggerImplClassNotFoundException e1) {
          // archiv kaputt
          throw new RuntimeException(e1);
        } catch (XACT_TriggerNotFound e1) {
          // archiv kaputt
          throw new RuntimeException(e1);
        } catch (XACT_InvalidStartParameterException e1) {
          // startparameter waren beim export doch auch ok => runtime ex
          throw new RuntimeException(e1);
        } catch (XFMG_SHARED_LIB_NOT_FOUND e1) {
          // archiv kaputt
          throw new RuntimeException(e1);
        } catch (XACT_LibOfTriggerImplNotFoundException e1) {
          // die muss ja wohl mit im archiv sein, oder der trigger muss vorher schon deployed worden sein
          throw new RuntimeException(e1);
        }
        if (verboseOutput) {
          logStream.println("done");
        }
      }
    }
  }


  private void importFilters(List<Element> filterElements) throws PersistenceLayerException,
      XACT_AdditionalDependencyDeploymentException, XACT_DuplicateFilterDefinitionException,
      XACT_ErrorDuringFilterAdditionRollback {
    for (Element e : filterElements) {
      if (e.getTagName().equals(DeliveryItemConstants.FILTER_ELEMENT)) {
        importFilter(e);
      }
    }
  }


  /**
   * Deploy a filter and all of its instances.
   */
  private void importFilter(Element filterElement) throws PersistenceLayerException,
      XACT_AdditionalDependencyDeploymentException, XACT_DuplicateFilterDefinitionException,
      XACT_ErrorDuringFilterAdditionRollback {

    String filterName = filterElement.getAttribute(DeliveryItemConstants.FILTER_NAME_ATTR).trim();
    String triggerName = filterElement.getAttribute(DeliveryItemConstants.FILTER_TRIGGERNAME_ATTR).trim();
    String fqClassName = filterElement.getAttribute(DeliveryItemConstants.FILTER_FQCLASSNAME_ATTR).trim();

    if (verboseOutput) {
      logStream.println("Restoring filter " + filterName + "(fqClassName: " + fqClassName + ", triggerName: "
          + triggerName + ")");
    }

    List<Element> childElements = XMLUtils.getChildElements(filterElement);

    ArrayList<File> jarFiles = new ArrayList<File>();
    ArrayList<String> sharedLibs = new ArrayList<String>();

    // first find sharedlibs and jar to add the trigger ...
    for (Element e : childElements) {
      if (e.getTagName().equals(DeliveryItemConstants.FILTER_SHAREDLIB_ELEMENT)) {
        String sl = e.getAttribute(DeliveryItemConstants.FILTER_SHAREDLIB_NAME_ATTR).trim();
        sharedLibs.add(sl);
        if (verboseOutput)
          logStream.println("\tSharedLib: " + sl);
      }

      if (e.getTagName().equals(DeliveryItemConstants.FILTER_JAR_ELEMENT)) {
        String jar = e.getAttribute(DeliveryItemConstants.FILTER_JAR_NAME_ATTR).trim();
        if(jar.startsWith("filter")) {
          jar = jar.substring("filter".length());
        }
        jarFiles.add(new File(RevisionManagement.getPathForRevision(PathType.FILTER, RevisionManagement.REVISION_DEFAULT_WORKSPACE) + jar));
        if (verboseOutput)
          logStream.println("\tJar: " + jar);
      }
    }

    // add the filter
    if (verboseOutput) {
      logStream.print("Adding filter...");
    }
    try {
      XynaFactory.getInstance().getActivation().getActivationTrigger().addFilter(filterName, jarFiles.toArray(new File[0]), fqClassName, triggerName,
                                      sharedLibs.toArray(new String[0]), "", RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    } catch (XACT_TriggerNotFound e1) {
      // wenn das nicht klappt, war wohl das archiv kaputt
      throw new RuntimeException(e1);
    } catch (Ex_FileAccessException e) {
      // wenn das nicht klappt, war wohl das archiv kaputt
      throw new RuntimeException(e);
    } catch (XPRC_XmlParsingException e) {
      // wenn das nicht klappt, war wohl das archiv kaputt
      throw new RuntimeException(e);
    } catch (XPRC_InvalidXmlMissingRequiredElementException e) {
      // wenn das nicht klappt, war wohl das archiv kaputt
      throw new RuntimeException(e);
    } catch (XACT_FilterImplClassNotFoundException e) {
      // wenn das nicht klappt, war wohl das archiv kaputt
      throw new RuntimeException(e);
    } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
      // wenn das nicht klappt, war wohl das archiv kaputt
      throw new RuntimeException(e);
    } catch (XACT_LibOfFilterImplNotFoundException e) {
      // wenn das nicht klappt, war wohl das archiv kaputt
      throw new RuntimeException(e);
    } catch (XPRC_ExclusiveDeploymentInProgress e) {
      // wenn das nicht klappt, war wohl das archiv kaputt
      throw new RuntimeException(e);
    } catch (XACT_JarFileUnzipProblem e) {
      throw new RuntimeException(e);
    }
    if (verboseOutput) {
      logStream.println("done");
    }


    // ... then deploy all instances
    // find all instances
    for (Element e : childElements) {
      if (e.getTagName().equals(DeliveryItemConstants.FILTER_INSTANCE_ELEMENT)) {

        String instanceName = e.getAttribute(DeliveryItemConstants.FILTER_INSTANCE_INSTANCENAME_ATTR).trim();
        filterName = e.getAttribute(DeliveryItemConstants.FILTER_INSTANCE_NAME_ATTR).trim();
        String triggerInstanceName =
            e.getAttribute(DeliveryItemConstants.FILTER_INSTANCE_TRIGGERINSTANCENAME_ATTR).trim();

        if (verboseOutput) {
          logStream.println("Restoring filter instance " + instanceName + " for filter " + filterName
              + ", triggerInstanceName: " + triggerInstanceName);
          logStream.print("Deploying filter...");
        }

        // deploying filter
        try {
          XynaFactory.getPortalInstance().getActivationPortal()
              .deployFilter(filterName, instanceName, triggerInstanceName, "", RevisionManagement.REVISION_DEFAULT_WORKSPACE);
        } catch (XACT_FilterImplClassNotFoundException e1) {
          // wenn das nicht klappt, war wohl das archiv kaputt
          throw new RuntimeException(e1);
        } catch (XACT_FilterNotFound e1) {
          // wenn das nicht klappt, war wohl das archiv kaputt
          throw new RuntimeException(e1);
        } catch (XACT_IncompatibleFilterImplException e1) {
          // wenn das nicht klappt, war wohl das archiv kaputt oder es wird versucht auf einem system zu importieren, wo
          // die schnittstelle von filtern sich geändert hat
          throw new RuntimeException(e1);
        } catch (XFMG_SHARED_LIB_NOT_FOUND e1) {
          // archiv kaputt oder vorher vorhandener trigger fehlerhaft?
          throw new RuntimeException(e1);
        } catch (XACT_LibOfFilterImplNotFoundException e1) {
          // archiv kaputt
          throw new RuntimeException(e1);
        }
        if (verboseOutput) {
          logStream.println("done");
        }

      }
    }
  }


  private void importCapacities(List<Element> list) throws PersistenceLayerException {

    for (Element e : list) {
      if (e.getTagName().equals(DeliveryItemConstants.CAPACITY_ELEMENT)) {
        String name = e.getAttribute(DeliveryItemConstants.CAPACITY_NAME_ATTR).trim();
        int cardinality = Integer.parseInt(e.getAttribute(DeliveryItemConstants.CAPACITY_CARDINALITY_ATTR).trim());

        if (verboseOutput)
          logStream.println("Restoring Capacity: name=" + name + " cardinality=" + cardinality);

        XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal().removeCapacity(name);
        try {
          XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal()
              .addCapacity(name, cardinality, CapacityManagement.State.ACTIVE);
        } catch (XPRC_CAPACITY_ALREADY_DEFINED e1) {
          logger.warn(null, e1);
          logStream.println("WARN: capacity " + name
              + " is already defined. keeping old configuration. import tried to change state to "
              + CapacityManagement.State.ACTIVE.toString() + " and cardinality to " + cardinality + ".");
        }
      }
    }

  }


  private void importCapacityMapping(List<Element> list) throws Ex_FileWriteException {

    for (Element e : list) {
      if (e.getTagName().equals(DeliveryItemConstants.CAPACITYMAPPING_ELEMENT)) {
        String capacityName = e.getAttribute(DeliveryItemConstants.CAPACITYMAPPING_CAPACITYNAME_ATTR).trim();
        int cardinality =
            Integer.parseInt(e.getAttribute(DeliveryItemConstants.CAPACITYMAPPING_CARDINALITY_ATTR).trim());
        String orderType = e.getAttribute(DeliveryItemConstants.CAPACITYMAPPING_ORDERTYPE_ATTR).trim();
        List<String> orderTypes = null;
        if (!orderType.equals("")) {
          orderTypes = new ArrayList<String>();
          orderTypes.add(orderType);
        } else {
          String workflowName = e.getAttribute(DeliveryItemConstants.CAPACITYMAPPING_WORKFLOW_ATTR).trim();
          orderTypes = XynaFactory.getInstance().getProcessing().listOrderTypesForWorkflow(workflowName);
        }
        for (String current : orderTypes) {
          if (verboseOutput)
            logStream.println("Restoring Capacity for order type " + current + " name=" + capacityName
                + " cardinality=" + cardinality);
          try {
            XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal()
                .requireCapacityForOrderType(current, capacityName, cardinality);
          } catch (PersistenceLayerException e1) {
            throw new RuntimeException(e1);
          } catch (XFMG_InvalidCapacityCardinality e1) {
            throw new RuntimeException("Inconsistent package content", e1);
          }
        }
      }
    }
  }


  private void importRights(List<Element> list) throws PersistenceLayerException {

    for (Element e : list) {
      if (e.getTagName().equals(DeliveryItemConstants.RIGHTS_ELEMENT)) {
        String right = e.getAttribute(DeliveryItemConstants.RIGHTS_NAME_ATTR).trim();

        if (verboseOutput)
          logStream.println("Restoring right " + right);

        try {
          if (XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
              .createRight(right)) {
            String rightDescription = e.getAttribute(DeliveryItemConstants.RIGHTS_DESCRIPTION_ATTR).trim();
            try {
              XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
                  .modifyRightFieldDescription(right, rightDescription, null);
            } catch (XFMG_RightDoesNotExistException excep) {
              throw new RuntimeException(excep); // we just created it
            } catch (XFMG_PredefinedXynaObjectException excep) {
              throw new RuntimeException(excep); // we did not export predefined Objects
            } catch (XynaException excep) {
              throw new RuntimeException(excep); // could not generate id for localization
            }
          }
        } catch (XFMG_NamingConventionException xe) {
          logger.warn(new StringBuilder().append("Right ").append(right).append(" could not be imported").toString(),
                      xe);
        } catch (XFMG_NameContainsInvalidCharacter xe) {
          logger.warn(new StringBuilder().append("Right ").append(right).append(" could not be imported").toString(),
                      xe);
        }
      }
    }
  }


  private void importDomains(List<Element> list) throws PersistenceLayerException {
    for (Element e : list) {
      if (e.getTagName().equals(DeliveryItemConstants.DOMAINS_ELEMENT)) {
        String domain = e.getAttribute(DeliveryItemConstants.DOMAINS_NAME_ATTR).trim();
        DomainType type = DomainType.valueOfNiceString(e.getAttribute(DeliveryItemConstants.DOMAINS_TYPE_ATTR).trim());
        int maxRetries = Integer.parseInt(e.getAttribute(DeliveryItemConstants.DOMAINS_MAXRETRIES_ATTR).trim());
        int connectionTimeout =
            Integer.parseInt(e.getAttribute(DeliveryItemConstants.DOMAINS_CONNECTIONTIMEOUT_ATTR).trim());

        if (verboseOutput)
          logStream.println("Restoring domain " + domain);

        try {
          if (XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
              .createDomain(domain, type, maxRetries, connectionTimeout)) {
            switch (type) {
              case LOCAL : // this should not happen atm, as the one and only LOCAL-Domain will not get exported
                break;
              case RADIUS :
                RADIUSDomainSpecificData data = new RADIUSDomainSpecificData();
                String associatedOrdertype =
                    e.getAttribute(DeliveryItemConstants.DOMAINS_RADIUSDATA_ASSOCIATEDORDERTYPE_ATTR).trim();
                data.setAssociatedOrdertype(associatedOrdertype);
                List<Element> serverElementList = XMLUtils.getChildElements(e);
                List<RADIUSServer> serverList = new ArrayList<RADIUSServer>();
                for (Element serverElement : serverElementList) {
                  if (serverElement.getTagName().equals(DeliveryItemConstants.DOMAINS_RADIUSSERVER_ELEMENT)) {
                    String ipValue =
                        serverElement.getAttribute(DeliveryItemConstants.DOMAINS_RADIUSSERVER_IP_ATTR).trim();
                    int portValue =
                        Integer.parseInt(serverElement
                            .getAttribute(DeliveryItemConstants.DOMAINS_RADIUSSERVER_PORT_ATTR).trim());
                    String presharedKey =
                        serverElement.getAttribute(DeliveryItemConstants.DOMAINS_RADIUSSERVER_PRESHAREDKEY_ATTR).trim();
                    serverList.add(new RADIUSServer(IP.generateIPFromString(ipValue), new RADIUSServerPort(portValue),
                                                    new PresharedKey(presharedKey)));
                  }
                }
                data.setServerList(serverList);
                XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
                    .modifyDomainFieldDomainTypeSpecificData(domain, data);
                break;
            }
            String description = e.getAttribute(DeliveryItemConstants.DOMAINS_DESCRIPTION_ATTR).trim();
            XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
                .modifyDomainFieldDescription(domain, description);
          }
        } catch (XFMG_DomainDoesNotExistException excep) {
          throw new RuntimeException(excep); // we just created it
        } catch (XFMG_PredefinedXynaObjectException excep) {
          throw new RuntimeException(excep); // we did not export predefined Objects
        } catch (XFMG_NameContainsInvalidCharacter ex) {
          logger.warn("could not import domain '" + domain + "'", ex);
        }
      }
    }
  }


  private void importRoles(List<Element> list) throws PersistenceLayerException {

    for (Element e : list) {
      if (e.getTagName().equals(DeliveryItemConstants.ROLE_ELEMENT)) {
        String roleName = e.getAttribute(DeliveryItemConstants.ROLE_NAME_ATTR).trim();
        String domainName = e.getAttribute(DeliveryItemConstants.ROLE_DOMAIN_ATTR).trim();
        if (domainName == null || domainName.equals("")) {
          domainName = UserManagement.PREDEFINED_LOCALDOMAIN_NAME;
        }

        if (verboseOutput)
          logStream.println("Restoring role " + roleName);
        try {
          if (XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
              .createRole(roleName, domainName)) {
            String alias = e.getAttribute(DeliveryItemConstants.ROLE_ALIAS_ATTR).trim();
            if (alias != null && !alias.equals("")) {
              try {
                XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
                    .modifyRoleFieldAlias(roleName, domainName, alias);
              } catch (XFMG_RoleDoesNotExistException e1) {
                throw new RuntimeException(e1); // we just created it
              } catch (XFMG_PredefinedXynaObjectException e1) {
                throw new RuntimeException(e1);// we did not export thse
              }
            }
            String description = e.getAttribute(DeliveryItemConstants.ROLE_DESCRIPTION_ATTR).trim();
            try {
              XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
                  .modifyRoleFieldDescription(roleName, domainName, description);
            } catch (XFMG_RoleDoesNotExistException e1) {
              throw new RuntimeException(e1); // we just created it
            } catch (XFMG_PredefinedXynaObjectException e1) {
              throw new RuntimeException(e1);// we did not export thse
            }

            // get rights which are to be granted
            List<Element> rightList = XMLUtils.getChildElements(e);

            for (Element rightElement : rightList) {

              if (rightElement.getTagName().equals(DeliveryItemConstants.RIGHTS_ELEMENT)) {
                String right = rightElement.getAttribute(DeliveryItemConstants.ROLE_RIGHTNAME_ATTR).trim();

                if (verboseOutput)
                  logStream.println("\tGranting right " + right + " for role " + roleName);

                try {
                  XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
                      .grantRightToRole(roleName, right);
                } catch (XFMG_RightDoesNotExistException xe) {
                  logger.warn(new StringBuilder().append("Right ").append(right)
                                  .append(" could not be granted to role ").append(roleName).toString(), xe);
                } catch (XFMG_RoleDoesNotExistException xe) {
                  logger.warn(new StringBuilder().append("Right ").append(right)
                                  .append(" could not be granted to role ").append(roleName).toString(), xe);
                }
              }
            }
          }
        } catch (XFMG_DomainDoesNotExistException e1) {
          throw new RuntimeException(e1); // Domains are imported before roles, this should never be able to happen
        } catch (XFMG_NameContainsInvalidCharacter ex) {
          logger.warn("could not import role '" + roleName + "'", ex);
        }
      }
    }
  }


  private void importUsers(List<Element> list) throws PersistenceLayerException {
    for (Element e : list) {
      if (e.getTagName().equals(DeliveryItemConstants.USER_ELEMENT)) {
        String userId = e.getAttribute(DeliveryItemConstants.USER_USERID_ATTR).trim();
        String roleName = e.getAttribute(DeliveryItemConstants.USER_ROLENAME_ATTR).trim();
        String password = e.getAttribute(DeliveryItemConstants.USER_PASSWORD_ATTR).trim();


        if (verboseOutput)
          logStream.println("Restoring user " + userId + " with role " + roleName);

        try {
          if (XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
              .createUser(userId, roleName, password, true)) {

            boolean lockedState = Boolean.parseBoolean(e.getAttribute(DeliveryItemConstants.USER_PASSWORD_ATTR).trim());
            try {
              XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
                  .modifyUserFieldLocked(userId, lockedState);
            } catch (XFMG_UserDoesNotExistException e1) {
              throw new RuntimeException(e1); // we just created it
            } catch (XFMG_PredefinedXynaObjectException e1) {
              throw new RuntimeException(e1); // we did not export those
            }

            // get domains
            List<Element> possibleDomainList = XMLUtils.getChildElements(e);
            List<String> domainListForSure = new ArrayList<String>();

            for (Element domainElement : possibleDomainList) {

              if (domainElement.getTagName().equals(DeliveryItemConstants.DOMAIN_ELEMENT)) {
                String domainName = domainElement.getAttribute(DeliveryItemConstants.USER_DOMAIN_ATTR).trim();
                domainListForSure.add(domainName);
              }
            }

            if (domainListForSure != null && domainListForSure.size() > 0) {
              if (verboseOutput)
                logStream.println("\tAssigning domains " + domainListForSure.toString() + " to user " + userId);

              try {
                XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
                    .modifyUserFieldDomains(userId, domainListForSure);
              } catch (XFMG_UserDoesNotExistException xe) {
                throw new RuntimeException(xe); // we just created it
              } catch (XFMG_DomainDoesNotExistException xe) {
                throw new RuntimeException(xe); // we just created it
              }
            }
          }
        } catch (XFMG_RoleDoesNotExistException xe) {
          logger.warn(new StringBuilder().append("User ").append(userId).append(" could not be created, role ")
                          .append(roleName).append(" does not exist").toString(), xe);
        } catch (XFMG_PasswordRestrictionViolation xe) {
          logger.warn(new StringBuilder().append("User ").append(userId).append(" could not be created.").toString(), xe);
        } catch (XFMG_NameContainsInvalidCharacter ex) {
          logger.warn("Could not import user '" + userId + "'", ex);
        }
      }
    }
  }


  private void importMonitoring(List<Element> list) throws PersistenceLayerException {
    for (Element e : list) {
      if (e.getTagName().equals(DeliveryItemConstants.MONITORING_ELEMENT)) {
        String workflow = e.getAttribute(DeliveryItemConstants.MONITORING_WF_ATTR).trim();
        Integer level = Integer.valueOf(e.getAttribute(DeliveryItemConstants.MONITORING_LEVEL_ATTR).trim());

        if (verboseOutput) {
          logStream.println("Restoring monitoring level " + level + " for workflow " + workflow);
        }

        try {
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher()
              .setMonitoringLevel(new DestinationKey(workflow), level);
        } catch (XPRC_INVALID_MONITORING_TYPE e1) {
          // archiv kaputt?
          throw new RuntimeException(e1);
        } catch (XPRC_ExecutionDestinationMissingException e1) {
          // archiv kaputt?
          throw new RuntimeException(e1);
        }
      }
    }
  }


  /**
   * doImport does the actual import of the delivery item. First, the delivery item zip file is unzipped at the right
   * folder location except the delivery.xml file. Existing file will only be overwritten if forceOverwrite is true.
   * Then the xml file is parsed, in order to determine resulting actions for deploying workflows, setting properites.
   */
  public void doRestore(boolean forceOverwrite, boolean dontUpdateMDM) throws XynaException, IOException {
    // first unzip all files into temporary location
    File tempDir = FileUtils.makeTemporaryDirectory();
    Map<File, File> unzippedFiles = new TreeMap<File, File>();
    int restoredServerFiles = doUnzip(tempDir, unzippedFiles);

    File tmp = new File(tempDir.getPath(), DeliveryItemConstants.XML_FILE);
    Document deliveryXMLDOM = XMLUtils.parse(tmp.getAbsolutePath());

    // acquire all workflows, data types and exceptions that depend on the objects to be installed
    // not all types are in deployment.xml therefore we rely on the extracted files
    // TODO : make deployment.xml smarter by adding more info about what needs to be halted because it is going to be
    //        deployed and what is just saved and does not need to be blocked.
    Set<WorkflowRevision> dependentWorkflows = new HashSet<WorkflowRevision>();
    Set<DependencyNode> processedDependants = new HashSet<DependencyNode>();

    for (File f : unzippedFiles.values()) {
      if (doesFileQualifyForRegistration(f.getPath())) {
        String fqXMLName = convertPathToOriginalFqName(f.getPath());
        String fqName = GenerationBase.transformNameForJava(fqXMLName);
        dependentWorkflows.add(new WorkflowRevision(fqName, RevisionManagement.REVISION_DEFAULT_WORKSPACE));

        Set<DependencyNode> dependantDataTypes =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister()
                .getDependencies(fqXMLName, DependencyRegister.DependencySourceType.DATATYPE);
        Set<DependencyNode> dependantExceptions =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister()
                .getDependencies(fqXMLName, DependencyRegister.DependencySourceType.XYNAEXCEPTION);
        Set<DependencyNode> dependantWorkflows =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister()
                .getDependencies(fqXMLName, DependencyRegister.DependencySourceType.WORKFLOW);

        Queue<DependencyNode> queue = new LinkedList<DependencyNode>();
        queue.addAll(dependantDataTypes);
        queue.addAll(dependantExceptions);
        queue.addAll(dependantWorkflows);

        while (!queue.isEmpty()) {
          DependencyNode node = queue.poll();

          if (!processedDependants.contains(node)) {
            processedDependants.add(node);
            queue.addAll(node.getDependentNodes());
            dependentWorkflows.add(new WorkflowRevision(node.getUniqueName(), node.getRevision()));
          }
        }
      }
    }

    // block out all dependant types
    OrderFilter of = new OrderFilterDeployment(dependentWorkflows);
    DeploymentManagement.getInstance().blockWorkflowProcessingForDeployment(of);
    long myID = DeploymentManagement.getInstance().propagateDeployment();

    try {
      logStream.print("Waiting for unreachable orders to finish ... ");
      DeploymentManagement.getInstance().waitForUnreachableOrders();
      logStream.println("done");
      
      long startTime = System.currentTimeMillis();
      logStream.print("Waiting for dependent orders to finish ... ");
      logStream.flush();
      
      while (foundDependantsInSystem(dependentWorkflows).size() > 0) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        
        if (System.currentTimeMillis() - startTime > 60000) {
          logger.error("Could not perform installation of package because there were depending orders still running in the system after one minute.");
          logStream.println("failed");
          logStream.println("Installation cannot be performed as there are orders still running that depend on the package contents:");
          Set<WorkflowRevision> dependentOrdersStillInUse = foundDependantsInSystem(dependentWorkflows);
          logger.error(dependentOrdersStillInUse);
          
          for (WorkflowRevision orderType : dependentOrdersStillInUse) {
            logStream.println(orderType);
          }
          
          return;
        }
      }
      
      logStream.println("done");

      // perform installation
      for (Entry<File, File> f : unzippedFiles.entrySet()) {
        if (!f.getValue().getName().equals(DeliveryItemConstants.XML_FILE)) {
          if (!f.getValue().exists()) {
            f.getValue().getParentFile().mkdirs();
            f.getValue().createNewFile();
            FileUtils.copyFile(f.getKey(), f.getValue());
            
            if (verboseOutput) {
              logStream.println("Installing file: " + f.getValue().getAbsolutePath());
            }
          } else {
            if (!forceOverwrite) {
              if (logger.isDebugEnabled()) {
                logger.debug("File already exists and will not be overwritten: " + f.getValue().getCanonicalPath());
              }
              logStream.println("Warning: File already exists and will not be overwritten: "
                  + f.getValue().getCanonicalPath());
              continue;
            } else {
              FileUtils.copyFile(f.getKey(), f.getValue());
              
              if (verboseOutput) {
                logStream.println("Installing file: " + f.getValue().getAbsolutePath());
              }
            }
          }
        }

        registerUnzippedMOMObject(f.getValue());
      }
      
      if (!dontUpdateMDM) {
        Updater.getInstance().checkUpdateMdm();
      }

      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
          .reloadAllSharedLibs(RevisionManagement.REVISION_DEFAULT_WORKSPACE);

      importDelivery(deliveryXMLDOM);
      
      // rescan the saved workflows from hard disc
      XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase().updateSavedProcessFQs();

      logStream.println("Restore/package installation completed");
      if (restoredServerFiles > 0) {
        logStream
            .println("Changes in the files log4j2.xml and server.policy require a server restart to become effective.");
      }

    } finally {
      DeploymentManagement.getInstance().unblockWorkflowProcessingForDeployment(of, myID);
      FileUtils.deleteDirectoryRecursively(tempDir);
    }
  }

  
  private void importDelivery(Document deliveryXMLDOM) throws XynaException {
      Element rootElement = deliveryXMLDOM.getDocumentElement();

      if (rootElement.getTagName().equals(DeliveryItemConstants.CONTAINER_ELEMENT)) {

        Element versionNumberElement =
            XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.VERSION_NUMBER_ELEMENT);
        if (versionNumberElement != null) {
          importVersionNumber(versionNumberElement);
        }

        Element propertyElements = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.PROPERTY_SECTION);
        if (propertyElements != null) {
          importProperties(XMLUtils.getChildElements(propertyElements));
        }

        Element dataTypeElements = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.DATA_TYPE_SECTION);
        if (dataTypeElements != null) {
          importDataTypes(XMLUtils.getChildElements(dataTypeElements));
        }

        Element exceptionElements =
            XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.EXCEPTION_SECTION);
        if (exceptionElements != null) {
          importExceptions(XMLUtils.getChildElements(exceptionElements));
        }

        Element workflowElements = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.WORKFLOW_SECTION);
        if (workflowElements != null) {
          importWorkflows(XMLUtils.getChildElements(workflowElements));
        }

        Element monitoringElements =
            XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.MONITORING_SECTION);
        if (monitoringElements != null) {
          importMonitoring(XMLUtils.getChildElements(monitoringElements));
        }

        Element capacityElements = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.CAPACITY_SECTION);
        if (capacityElements != null) {
          importCapacities(XMLUtils.getChildElements(capacityElements));
        }

        Element capacityMappingElements =
            XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.CAPACITYMAPPING_SECTION);
        if (capacityMappingElements != null) {
          importCapacityMapping(XMLUtils.getChildElements(capacityMappingElements));
        }

        Element triggerElements = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.TRIGGER_SECTION);
        if (triggerElements != null) {
          importTriggers(XMLUtils.getChildElements(triggerElements));
        }

        Element filterElements = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.FILTER_SECTION);
        if (filterElements != null) {
          importFilters(XMLUtils.getChildElements(filterElements));
        }

        Element rightsElements = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.RIGHTS_SECTION);
        if (rightsElements != null) {
          importRights(XMLUtils.getChildElements(rightsElements));
        }

        Element domainsElements = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.DOMAINS_SECTION);
        if (domainsElements != null) {
          importDomains(XMLUtils.getChildElements(domainsElements));
        }

        Element roleElements = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.ROLE_SECTION);
        if (roleElements != null) {
          importRoles(XMLUtils.getChildElements(roleElements));
        }

        Element userElements = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.USER_SECTION);
        if (userElements != null) {
          importUsers(XMLUtils.getChildElements(userElements));
        }

        Element cronLsElements = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.CRONLS_SECTION);
        if (cronLsElements != null) {
          importCronLikeOrders(XMLUtils.getChildElements(cronLsElements));
        }

      }
  }


  private Set<WorkflowRevision> foundDependantsInSystem(Set<WorkflowRevision> dependants) throws XPRC_WorkflowProtectionModeViolationException, XPRC_DESTINATION_NOT_FOUND {
    Set<WorkflowRevision> dependentPresentOrders = new HashSet<WorkflowRevision>();
    Pair<Map<Presence, Set<WorkflowRevision>>, Map<WorkflowRevision, List<Long>>> pair = DeploymentManagement.getInstance().getWorkflowsPresentInSystem();
    Map<Presence, Set<WorkflowRevision>> stillPresentOrderTypesMap = pair.getFirst();
    
    HashSet<WorkflowRevision> stillPresentOrderTypes = new HashSet<WorkflowRevision>();
    for( Set<WorkflowRevision> set : stillPresentOrderTypesMap.values() ) {
      stillPresentOrderTypes.addAll(set); 
    }
    /*
    Map<WorkflowRevision, List<Long>> suspendedOrders = pair.getSecond();
    Set<Long> orderIds = new HashSet<Long>();
    */
    for (WorkflowRevision orderType : stillPresentOrderTypes ) {
      if (dependants.contains(orderType)) {
        dependentPresentOrders.add(orderType);
        /*
        List<Long> list = suspendedOrders.get(orderType);
        if( list != null ) {
          orderIds.addAll(list);
        }*/
      }
    }
    //return Pair.of(dependentPresentOrders, orderIds); Aufträge in Suspendierung werden nicht benötigt
    
    return dependentPresentOrders; 
  }
  
  
  private Set<String> elementsToBeInstalled(Document doc) {
    Set<String> toBeInstalled = new HashSet<String>();
    Element rootElement = doc.getDocumentElement();

    if (rootElement.getTagName().equals(DeliveryItemConstants.CONTAINER_ELEMENT)) {
//      Element dataTypeElement = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.WORKFLOW_SECTION);
//
//      if (dataTypeElements != null) {
//        List<Element> dataTypeElementList = XMLUtils.getChildElements(dataTypeElement);
//
//        for (Element dataType : dataTypeElementList) {
//          if (dataType.getTagName().equals(DeliveryItemConstants.WORKFLOW_ELEMENT)) {
//            if (dataType.getAttribute(DeliveryItemConstants.WORKFLOW_DEPLOY_STATE_ATTR).trim()
//                .equals(DeliveryItemConstants.WORKFLOW_DEPLOYED)) {
//
//              toBeInstalled.add(dataType.getTextContent().trim());
//            }
//          }
//        }
//      }
//
//      Element workflowElements = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.WORKFLOW_SECTION);
//
//      if (workflowElements != null) {
//        List<Element> workflowElementList = XMLUtils.getChildElements(workflowElements);
//
//        for (Element workflow : workflowElementList) {
//          if (workflow.getTagName().equals(DeliveryItemConstants.WORKFLOW_ELEMENT)) {
//            if (workflow.getAttribute(DeliveryItemConstants.WORKFLOW_DEPLOY_STATE_ATTR).trim()
//                .equals(DeliveryItemConstants.WORKFLOW_DEPLOYED)) {
//
//              toBeInstalled.add(workflow.getTextContent().trim());
//            }
//          }
//        }
//      }

      Element workflowElements = XMLUtils.getChildElementByName(rootElement, DeliveryItemConstants.WORKFLOW_SECTION);

      if (workflowElements != null) {
        List<Element> workflowElementList = XMLUtils.getChildElements(workflowElements);

        for (Element workflow : workflowElementList) {
          if (workflow.getTagName().equals(DeliveryItemConstants.WORKFLOW_ELEMENT)) {
            if (workflow.getAttribute(DeliveryItemConstants.WORKFLOW_DEPLOY_STATE_ATTR).trim()
                .equals(DeliveryItemConstants.WORKFLOW_DEPLOYED)) {

              toBeInstalled.add(workflow.getTextContent().trim());
            }
          }
        }
      }
    }

    return toBeInstalled;
  }
  

  /**
   * @return true, if the zip file contained log4j.property and server.policy or false otherwise
   */
  private int doUnzip(File tempDir, Map<File, File> inOutFileMap) throws Ex_FileAccessException, IOException {
    int serverBaseFilesRestored = 0;
    ZipInputStream zis = new ZipInputStream(inputFile);

    try {
      ZipEntry entry;

      // loop over all zip entries
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }

        String entryName = entry.getName();
        
        entryName = entryName.replaceAll(CreateDeliveryItem.PACKAGE_DIR_SAVED,
                             Constants.PREFIX_SAVED + Constants.fileSeparator + Constants.SUBDIR_XMOM);
        entryName = entryName.replaceAll(CreateDeliveryItem.PACKAGE_DIR_SERVICES,
                             Constants.REVISION_PATH + Constants.fileSeparator + Constants.PREFIX_REVISION + 
                             Constants.SUFFIX_REVISION_WORKINGSET + Constants.fileSeparator + Constants.SUBDIR_SERVICES);
        entryName = entryName.replaceAll(CreateDeliveryItem.PACKAGE_DIR_SHAREDLIBS,
                             Constants.REVISION_PATH + Constants.fileSeparator + Constants.PREFIX_REVISION + 
                             Constants.SUFFIX_REVISION_WORKINGSET + Constants.fileSeparator + Constants.SUBDIR_SHAREDLIBS);
        entryName = entryName.replaceAll(CreateDeliveryItem.PACKAGE_DIR_FILTER,
                             Constants.REVISION_PATH + Constants.fileSeparator + Constants.PREFIX_REVISION + 
                             Constants.SUFFIX_REVISION_WORKINGSET + Constants.fileSeparator + Constants.SUBDIR_FILTER);
        entryName = entryName.replaceAll(CreateDeliveryItem.PACKAGE_DIR_TRIGGER,
                             Constants.REVISION_PATH + Constants.fileSeparator + Constants.PREFIX_REVISION + 
                             Constants.SUFFIX_REVISION_WORKINGSET + Constants.fileSeparator + Constants.SUBDIR_TRIGGER);
        
        File f = new File(tempDir.getPath(), entryName);
        File fileDestination = new File(DeliveryItemConstants.BASEDIR, entryName);
        
        if (!f.exists()) {
          // create file and folder structure if not existent
          if (f.getParentFile() != null) {
            f.getParentFile().mkdirs();
          }

          f.createNewFile();
        }

        if (logger.isTraceEnabled()) {
          logger.trace("Unzipping file: " + f.getAbsolutePath());
        }

        if (verboseOutput) {
          logStream.println("Unzipping file: " + f.getAbsolutePath());
        }

        if (f.getName().equals("log4j2.xml")) {
          serverBaseFilesRestored++;
        } else if (f.getName().equals("server.policy")) {
          serverBaseFilesRestored++;
        }

        FileOutputStream out = new FileOutputStream(f);
        try {
          // copy bytes from zip input stream to the output stream: i.e. do the unzipping
          byte[] data = new byte[2048];
          int length;

          while ((length = zis.read(data)) != -1) {
            out.write(data, 0, length);
          }
          out.flush();
        } finally {
          out.close();
        }

        inOutFileMap.put(f, fileDestination);
      }
    } finally {
      zis.close();
    }

    return serverBaseFilesRestored;
  }

  
  private void registerUnzippedMOMObject(File f) {
    String filePath = f.getPath();
    if (doesFileQualifyForRegistration(filePath)) {
      String fqname = convertPathToOriginalFqName(filePath);
      try {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase()
            .registerMOMObject(fqname, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
      } catch (XynaException e) {
        logger.debug("Error while trying to register MOM-Object from package, continuing with next", e);
      }
    }
  }


  private boolean doesFileQualifyForRegistration(String filePath) {
    String savedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, RevisionManagement.REVISION_DEFAULT_WORKSPACE, false);
    return filePath.startsWith(savedMdmDir);
  }
  

  private String convertPathToOriginalFqName(String filePath) {
    String savedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, RevisionManagement.REVISION_DEFAULT_WORKSPACE, false);
    String conversion = filePath.substring(savedMdmDir.length() + 1);
    if (conversion.endsWith(".xml")) {
      conversion = conversion.substring(0, conversion.length() - ".xml".length());
    }
    return conversion.replaceAll(Constants.fileSeparator, ".");
  }


  public void setVerboseOutput(boolean b) {
    verboseOutput = b;
  }

}
