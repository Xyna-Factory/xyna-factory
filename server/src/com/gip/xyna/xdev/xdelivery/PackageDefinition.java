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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionFileInvalidRootException;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionItemNotFoundException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
import com.gip.xyna.xprc.XynaProcessingPortal.DispatcherIdentification;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeOrderStorageException;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;



public class PackageDefinition {

  private static Logger logger = CentralFactoryLogging.getLogger(UninstallPackage.class);

  private boolean verboseOutput = false;
  private boolean includeXynaComponents = false;
  /**
   * Filenames of all deployed mdm elements which should be stored in Xyna Package
   */
  private HashSet<String> mdmDeployedFilesToBeStored = new HashSet<String>();
  /**
   * Filenames of all saved mdm elements which should be stored in Xyna Package
   */
  private HashSet<String> mdmSavedFilesToBeStored = new HashSet<String>();
  /**
   * Full qualified names for all mdm elements which should be stored in Xyna Package
   */
  private HashSet<String> mdmFqClassesToBeStored = new HashSet<String>();
  /**
   * Diverse other files e.g configuration files
   */
  private HashSet<String> otherFilesToBeStored = new HashSet<String>();

  /**
   * redundanzfreie menge der workflows, die bei installpackage explizit deployed werden müssen.
   */
  private Set<String> mainWorkflowsToBeDeployedOnInstallPackage = new HashSet<String>();
  
  private Set<String> dataTypesToBeStored = new HashSet<String>();
  private Set<String> exceptionsToBeStored = new HashSet<String>();

  private HashSet<CronLikeOrderInformation> cronLikeOrderInformationToBeStored = new HashSet<CronLikeOrderInformation>();
  private HashSet<CapacityInformation> capacityInformationToBeStored = new HashSet<CapacityInformation>();
  private HashSet<String> propertiesToBeStored = new HashSet<String>();
  private HashSet<Filter> filtersToBeStored = new HashSet<Filter>();
  private HashSet<String> sharedLibsToBeStored = new HashSet<String>();
  private HashSet<TriggerPackageRepresentation> triggersToBeStored = new HashSet<TriggerPackageRepresentation>();
  /**
   * All mdm elements detected by dependency analysis
   */
  private Set<DependencyNode> allObjects = new HashSet<DependencyNode>();

  private final InputStream packageDefinitionStream;
  private final File packageFile;

  /**
   * Used for output of console information
   */
  private PrintStream statusStream;


  /**
   * @param packageDefinitionStream package description file
   * @param logOutputStream Stream for log/status information
   */
  public PackageDefinition(InputStream packageDefinitionStream, OutputStream logOutputStream) {
    // packagefile darf null sein.
    this.packageDefinitionStream = packageDefinitionStream;
    this.packageFile = null;
    this.statusStream = new PrintStream(logOutputStream);
  }


  public PackageDefinition(File packageFile, OutputStream os) {
    // packagefile darf null sein.
    this.packageFile = packageFile;
    this.packageDefinitionStream = null;
    this.statusStream = new PrintStream(os);
  }


  /**
   * Parse the package definition file into a dom document.
   * 
   * @return dom document
   * @throws Ex_FileAccessException
   * @throws XPRC_XmlParsingException
   */
  private Document getParsedDocument() throws Ex_FileAccessException, XPRC_XmlParsingException {

    Document parsedDocument;

    if (this.packageFile != null) {
      return XMLUtils.parse(this.packageFile);
    }
    else if (this.packageDefinitionStream != null) {
      File tempFile = new File("." + Constants.fileSeparator + "packageTemp" + System.currentTimeMillis() + ".xml");
      try {
        synchronized (PackageDefinition.class) {
          while (tempFile.exists()) {
            tempFile = new File("." + Constants.fileSeparator + "packageTemp" + System.currentTimeMillis() + ".xml");
          }
          tempFile.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(tempFile);
        try {
          while (this.packageDefinitionStream.available() > 0) {
            fos.write(this.packageDefinitionStream.read());
          }
          fos.flush();
        } finally {
          fos.close();
        }

        parsedDocument = XMLUtils.parse(tempFile.getAbsolutePath());

      }
      catch (IOException e) {
        throw new Ex_FileAccessException(tempFile.getAbsolutePath());
      }
      finally {
        synchronized (PackageDefinition.class) {
          if (tempFile.exists()) {
            tempFile.delete();
          }
        }
      }

      return parsedDocument;
    }
    else {
      throw new RuntimeException("Either file or a stream have to be specified.");
    }

  }


  /**
   * Read information from package definition file
   * 
   * @throws XDEV_PackageDefinitionItemNotFoundException
   * @throws XDEV_PackageDefinitionFileInvalidRootException
   * @throws PersistenceLayerException
   * @throws Ex_FileAccessException
   * @throws XPRC_XmlParsingException
   * @throws XPRC_DESTINATION_NOT_FOUND
   */
  public void parsePackageDefinitionFile() throws XDEV_PackageDefinitionItemNotFoundException,
                  XDEV_PackageDefinitionFileInvalidRootException, PersistenceLayerException, Ex_FileAccessException,
                  XPRC_XmlParsingException, XPRC_DESTINATION_NOT_FOUND {

    Document parsedDocument = getParsedDocument();

    Element rootElement = parsedDocument.getDocumentElement();
    // check root
    if (!rootElement.getTagName().equals(DeliveryItemConstants.PACKAGE_FILE_ROOT))
      throw new XDEV_PackageDefinitionFileInvalidRootException();

    writeStandardInfoToOutputStream("Parsing package information...");
    processWorkflowEntries(rootElement);
    processDatatypeEntries(rootElement);
    processExceptionEntries(rootElement);
    processTriggerEntries(rootElement);
    processFilterEntries(rootElement);
    processCapacityEntries(rootElement);
    processCronLSEntries(rootElement);
    processXynaPropertyEntries(rootElement);
    processFileEntries(rootElement);
    writeStandardInfoToOutputStream("Package information successfully read");
  }

  /**
   * Store filenames and full qualified names of workflows. Also detect dependent objects.
   * 
   * @param element root element of package definition file
   * @throws XDEV_PackageDefinitionItemNotFoundException
   * @throws PersistenceLayerException
   * @throws Ex_FileAccessException
   * @throws XPRC_XmlParsingException
   */
  private void processWorkflowEntries(Element element) throws XDEV_PackageDefinitionItemNotFoundException,
                  PersistenceLayerException, Ex_FileAccessException, XPRC_XmlParsingException {
    writeVerboseToOutputStream("Parsing workflows to be included...");
    // get all saved and all deployed workflows
    Map<String, DeploymentStatus> deploymentStatuses =
        XynaFactory.getPortalInstance().getProcessingPortal().listDeploymentStatuses(RevisionManagement.REVISION_DEFAULT_WORKSPACE).get(ApplicationEntryType.WORKFLOW);
    Set<String> workflowsToBeDeployedOnInstallPackageNoDependenciesIncluded = new HashSet<String>();

    for (Element e : XMLUtils.getChildElementsByName(element, DeliveryItemConstants.WORKFLOW_ELEMENT)) {

      String wfName = XMLUtils.getTextContent(e).trim();
      DeploymentStatus wfStatus = deploymentStatuses.get(wfName);
      if (wfStatus == null || wfStatus != DeploymentStatus.DEPLOYED) {
        writeWarningToOutputStream("Warning: " + wfName
            + " is not deployed, its dependencies have not been collected and wont be exported");
      }

      if (wfStatus != null) {
        String wfNameFile;
        if (wfStatus == DeploymentStatus.SAVED) {
          wfNameFile = GenerationBase.getFileLocationOfXmlNameForSaving(wfName) + ".xml";
        } else if (wfStatus == DeploymentStatus.DEPLOYED) {
          wfNameFile = GenerationBase.getFileLocationOfXmlNameForDeployment(wfName) + ".xml";
        } else {
          throw new RuntimeException("wfStatus " + wfStatus + " not supported");
        }
        
        if (!new File(wfNameFile).exists()) {
          logger.warn("did not find file " + wfNameFile);
          throw new XDEV_PackageDefinitionItemNotFoundException(GenerationBase.EL.DEPENDENCY_WORKFLOW, wfName);
        }
        if (logger.isDebugEnabled()) {
          logger.debug("Found relevant root file: " + wfNameFile);
        }

        if (wfStatus == DeploymentStatus.SAVED) {
          mdmSavedFilesToBeStored.add(wfNameFile);
        } else {
          if (checkIgnoreDependencies(e)) {
            workflowsToBeDeployedOnInstallPackageNoDependenciesIncluded.add(wfName);
          } else {
            mainWorkflowsToBeDeployedOnInstallPackage.add(wfName);
          }
          mdmDeployedFilesToBeStored.add(wfNameFile);
        }

        mdmFqClassesToBeStored.add(wfName);

      }
    }
    
    Set<String> redundantElements = new HashSet<String>();
    
    for (String wfNameDeployed : mainWorkflowsToBeDeployedOnInstallPackage) {

      // get all objects used by the workflow (other workflows, datatypes, xyna properties, shared libs, exceptions)
      // including itself
      Set<DependencyNode> relevantObjects =
          getDependencyRegister().getAllUsedNodes(wfNameDeployed, DependencySourceType.WORKFLOW, true, true);

      //redundanzen ermitteln
      for (DependencyNode dn : relevantObjects) {
        if (!dn.getUniqueName().equals(wfNameDeployed)) { //den eigenen wf ausklammern
          if (mainWorkflowsToBeDeployedOnInstallPackage.contains(dn.getUniqueName())) {
            redundantElements.add(dn.getUniqueName());
          }
          if (workflowsToBeDeployedOnInstallPackageNoDependenciesIncluded.contains(dn.getUniqueName())) {
            redundantElements.add(dn.getUniqueName());
          }
        }
      }
      
      allObjects.addAll(relevantObjects);

      for (DependencyNode usedObject : relevantObjects) {
        handleDetectedDependency(usedObject);
      }
    }

    mainWorkflowsToBeDeployedOnInstallPackage.addAll(workflowsToBeDeployedOnInstallPackageNoDependenciesIncluded);
    mainWorkflowsToBeDeployedOnInstallPackage.removeAll(redundantElements);
  }


  private void processDatatypeEntries(Element element) throws XDEV_PackageDefinitionItemNotFoundException,
                  Ex_FileAccessException, XPRC_XmlParsingException, PersistenceLayerException {
    writeVerboseToOutputStream("Parsing data types to be included...");
    for (Element e : XMLUtils.getChildElementsByName(element, GenerationBase.EL.DEPENDENCY_DATATYPE)) {

      String datatypeOriginalFqName = XMLUtils.getTextContent(e).trim();
      tryToAddMdmType(datatypeOriginalFqName);
      Set<DependencyNode> relevantObjects = getDependencyRegister().getAllUsedNodes(datatypeOriginalFqName,
                                                                                    DependencySourceType.DATATYPE,
                                                                                    true, true);
      allObjects.addAll(relevantObjects);
      dataTypesToBeStored.add(datatypeOriginalFqName);

      if (checkIgnoreDependencies(e)) {
        continue;
      }

      for (DependencyNode usedObject : relevantObjects) {
        handleDetectedDependency(usedObject);
      }

    }
  }


  private void processExceptionEntries(Element element) throws XDEV_PackageDefinitionItemNotFoundException,
                  Ex_FileAccessException, XPRC_XmlParsingException, PersistenceLayerException {
    writeVerboseToOutputStream("Parsing exceptions to be included...");
    for (Element e : XMLUtils.getChildElementsByName(element, GenerationBase.EL.DEPENDENCY_EXCEPTION)) {

      String exceptionName = XMLUtils.getTextContent(e).trim();
      tryToAddMdmType(exceptionName);
      Set<DependencyNode> relevantObjects = getDependencyRegister().getAllUsedNodes(exceptionName,
                                                                                    DependencySourceType.XYNAEXCEPTION,
                                                                                    true, true);
      allObjects.addAll(relevantObjects);
      exceptionsToBeStored.add(exceptionName);

      if (checkIgnoreDependencies(e)) {
        continue;
      }

      for (DependencyNode usedObject : relevantObjects) {
        handleDetectedDependency(usedObject);
      }

    }
  }


  private void processTriggerEntries(Element element) throws Ex_FileAccessException, XPRC_XmlParsingException,
                  XDEV_PackageDefinitionItemNotFoundException, PersistenceLayerException {
    writeVerboseToOutputStream("Parsing triggers to be included...");
    for (Element e : XMLUtils.getChildElementsByName(element, GenerationBase.EL.DEPENDENCY_TRIGGER)) {

      String triggerName = XMLUtils.getTextContent(e).trim();
      tryToAddTrigger(triggerName, true);

      Set<DependencyNode> relevantObjects = getDependencyRegister().getAllUsedNodes(triggerName,
                                                                                    DependencySourceType.TRIGGER, true,
                                                                                    true);
      allObjects.addAll(relevantObjects);

      if (checkIgnoreDependencies(e))
        continue;

      for (DependencyNode usedObject : relevantObjects) {
        handleDetectedDependency(usedObject);
      }

    }
  }


  private void processFilterEntries(Element element) throws XDEV_PackageDefinitionItemNotFoundException,
                  Ex_FileAccessException, XPRC_XmlParsingException, PersistenceLayerException {
    writeVerboseToOutputStream("Parsing filters to be included...");
    for (Element e : XMLUtils.getChildElementsByName(element, GenerationBase.EL.DEPENDENCY_FILTER)) {

      String filterName = XMLUtils.getTextContent(e).trim();
      tryToAddFilter(filterName);

      Set<DependencyNode> relevantObjects = getDependencyRegister().getAllUsedNodes(filterName,
                                                                                    DependencySourceType.FILTER, true,
                                                                                    true);
      allObjects.addAll(relevantObjects);

      if (checkIgnoreDependencies(e))
        continue;

      for (DependencyNode usedObject : relevantObjects) {
        handleDetectedDependency(usedObject);
      }

    }
  }


  private void processCapacityEntries(Element element) {
    writeVerboseToOutputStream("Parsing capacities to be included...");
    Collection<CapacityInformation> capInfoList = XynaFactory.getPortalInstance().getProcessingPortal()
                    .listCapacityInformation();
    for (Element e : XMLUtils.getChildElementsByName(element, DeliveryItemConstants.CAPACITY_ELEMENT)) {

      String capacityName = XMLUtils.getTextContent(e).trim();
      boolean found = false;
      for (CapacityInformation capInfo : capInfoList) {
        if (capInfo.getName().equals(capacityName)) {
          capacityInformationToBeStored.add(capInfo);
          writeVerboseToOutputStream("\tFound valid capacity entry '" + capacityName + "'");
          found = true;
          break;
        }
      }

      if (!found) {
        writeWarningToOutputStream("Warning: Capacity '" + capacityName + "' could not be found on the server, skipping");
      }
    }
  }


  private void processCronLSEntries(Element element) throws XDEV_PackageDefinitionItemNotFoundException,
                  PersistenceLayerException, Ex_FileAccessException, XPRC_XmlParsingException,
                  XPRC_DESTINATION_NOT_FOUND {
    writeVerboseToOutputStream("Parsing cron like orders to be included...");
    for (Element e : XMLUtils.getChildElementsByName(element, DeliveryItemConstants.CRONLS_ELEMENT)) {

      String cronLsIDString = XMLUtils.getTextContent(e).trim();
      Long cronLsId = null;
      try {
        cronLsId = Long.valueOf(cronLsIDString);
      }
      catch (NumberFormatException e2) {
        writeWarningToOutputStream("Warning: Could not parse cron like order ID '" + cronLsIDString + "', skipping");
        continue;
      }

      CronLikeOrderInformation info;
      try {
        info = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
                        .getOrderInformation(cronLsId);
      }
      catch (XPRC_CronLikeOrderStorageException e1) {
        writeWarningToOutputStream("Warning: Could not get cron like order information for id '" + cronLsId + "', skipping.");
        info = null;
      }

      if (info != null) {
        cronLikeOrderInformationToBeStored.add(info);
        DispatcherEntry entry = XynaFactory.getInstance().getProcessingPortal()
                        .getDestination(DispatcherIdentification.Execution,
                                        new DestinationKey(info.getTargetOrdertype()));
        // include workflow that is started by cronls in the package definition
        Set<DependencyNode> relevantObjects = getDependencyRegister()
                        .getAllUsedNodes(GenerationBase.lookupXMLNameByJavaClassName(entry.getValue().getFqName(), RevisionManagement.REVISION_DEFAULT_WORKSPACE, false),
                                         DependencySourceType.WORKFLOW, true, true);

        allObjects.addAll(relevantObjects);

        if (checkIgnoreDependencies(e)) {
          continue;
        }

        for (DependencyNode usedObject : relevantObjects) {
          handleDetectedDependency(usedObject);
        }
      }
      else {
        writeWarningToOutputStream("Warning: Could not find cron like order <" + cronLsId + ">, skipping");
      }
    }

  }


  private void processXynaPropertyEntries(Element element) {
    writeVerboseToOutputStream("Parsing XynaProperties orders to be included...");
    for (Element e : XMLUtils.getChildElementsByName(element, DeliveryItemConstants.PROPERTY_ELEMENT)) {
      String propertyName = XMLUtils.getTextContent(e).trim();
      propertiesToBeStored.add(propertyName);
      Set<DependencyNode> relevantObjects = getDependencyRegister().getAllUsedNodes(propertyName,
                                                                                    DependencySourceType.XYNAPROPERTY,
                                                                                    true, true);
      allObjects.addAll(relevantObjects);
    }
  }


  private void processFileEntries(Element element) {
    writeVerboseToOutputStream("Parsing other files to be included...");
    for (Element e : XMLUtils.getChildElementsByName(element, DeliveryItemConstants.FILE_ELEMENT)) {
      String fileName = XMLUtils.getTextContent(e).trim();
      otherFilesToBeStored.add(fileName);
    }
  }


  /**
   * @return true, falls das element das attribut {@link DeliveryItemConstants#IGNORE_DEPENDENCIES} mit dem 
   * wert <code>true</code> enthält. 
   */
  private boolean checkIgnoreDependencies(Element e) {
    String s = e.getAttribute(DeliveryItemConstants.IGNORE_DEPENDENCIES).trim();
    if (s != null && s.equals("true")) {
      return true;
    } else {
      return false;
    }
  }


  private void handleDetectedDependency(DependencyNode node) throws XDEV_PackageDefinitionItemNotFoundException,
                  PersistenceLayerException, Ex_FileAccessException, XPRC_XmlParsingException {

    if (node.getType() == DependencySourceType.WORKFLOW) {
      String filename = GenerationBase.getFileLocationOfXmlNameForDeployment(node.getUniqueName()) + ".xml";
      if (!qualifiesForInclusion(containsXynaComponentMarker(filename))) {
        // is a xyna component and not explicitly included
        return;
      }
      Map<String, DeploymentStatus> deploymentStatuses =
          XynaFactory.getPortalInstance().getProcessingPortal().listDeploymentStatuses(RevisionManagement.REVISION_DEFAULT_WORKSPACE).get(ApplicationEntryType.WORKFLOW);

      DeploymentStatus wfStatus = deploymentStatuses.get(node.getUniqueName());

      if (wfStatus != null && wfStatus == DeploymentStatus.DEPLOYED) {
        String wfNameFile = GenerationBase.getFileLocationOfXmlNameForDeployment(node.getUniqueName()) + ".xml";
        if (!new File(wfNameFile).exists()) {
          throw new XDEV_PackageDefinitionItemNotFoundException(GenerationBase.EL.DEPENDENCY_WORKFLOW, node
                          .getUniqueName());
        }
        if (logger.isDebugEnabled()) {
          logger.debug("Found relevant root file: " + wfNameFile);
        }
        mdmDeployedFilesToBeStored.add(wfNameFile);
        mdmFqClassesToBeStored.add(node.getUniqueName());
      }

      if (wfStatus != null && wfStatus == DeploymentStatus.SAVED) {
        String wfNameFile = GenerationBase.getFileLocationOfXmlNameForSaving(node.getUniqueName()) + ".xml";
        if (!new File(wfNameFile).exists()) {
          throw new XDEV_PackageDefinitionItemNotFoundException(GenerationBase.EL.DEPENDENCY_WORKFLOW, node
              .getUniqueName());
        }
        if (logger.isDebugEnabled()) {
          logger.debug("Found relevant root file: " + wfNameFile);
        }
        mdmSavedFilesToBeStored.add(wfNameFile);
        mdmFqClassesToBeStored.add(node.getUniqueName());
      }

    }
    else if (node.getType() == DependencySourceType.DATATYPE) {
      tryToAddMdmType(node.getUniqueName());
    }
    else if (node.getType() == DependencySourceType.XYNAPROPERTY) {
      boolean isFactoryComponent = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS()
                      .getConfiguration().isPropertyFactoryComponent(node.getUniqueName());
      if (qualifiesForInclusion(isFactoryComponent)) {
        propertiesToBeStored.add(node.getUniqueName());
      }
    }
    else if (node.getType() == DependencySourceType.FILTER) {
      tryToAddFilter(node.getUniqueName());
    }
    else if (node.getType() == DependencySourceType.TRIGGER) {
      tryToAddTrigger(node.getUniqueName(), false);
    }
    else if (node.getType() == DependencySourceType.XYNAEXCEPTION) {
      tryToAddMdmType(node.getUniqueName());
    }
    else if (node.getType() == DependencySourceType.SHAREDLIB) {
      File sharedLibFile = new File(Constants.SHAREDLIB_BASEDIR + Constants.fileSeparator + node.getUniqueName());
      if (!sharedLibFile.exists()) {
        writeWarningToOutputStream("Warning: Could not find shared library: " + sharedLibFile.getPath());
      }
      else {
        writeVerboseToOutputStream("Found dependency on shared library: " + sharedLibFile.getPath());
        sharedLibsToBeStored.add(node.getUniqueName());
      }
    }
    else {
      writeWarningToOutputStream("Warning: Found dependency " + node.getUniqueName() + " with unhandled type " + node
                      .getType() + ", ignoring");
    }

  }


  /**
   * Add a trigger to the trigger store list if it is not already on it. The trigger is only added if the include xyna
   * components flag allows it.
   * 
   * @param triggerName name of the trigger to add
   * @param includeAllInstances if true, add all instances of the named trigger, else add only the trigger instance that
   *          causes the dependency analysis
   * @throws Ex_FileAccessException
   * @throws XPRC_XmlParsingException
   */
  private void tryToAddTrigger(String triggerName, boolean includeAllInstances) throws Ex_FileAccessException,
                  XPRC_XmlParsingException {

    // trigger is used by a filter. Only backup relevant instance

    for (Trigger trigger : XynaFactory.getPortalInstance().getActivationPortal().getTriggers()) {
      if (trigger.getTriggerName().equals(triggerName)) {
        // found matching trigger
        if (qualifiesForInclusion(containsXynaComponentMarker(XynaActivationTrigger
                        .getTriggerXmlLocationByTriggerFqClassName(triggerName, VersionManagement.REVISION_WORKINGSET)))) {
          // trigger should be added
          if (includeAllInstances) {
            if (!triggersToBeStored.add(new TriggerPackageRepresentation(trigger, includeAllInstances))) {
              // trigger has already been added
              writeWarningToOutputStream("Warning Duplicate entry detected: Trigger " + triggerName);
            }
          }
          else {
            if (triggersToBeStored.contains(new TriggerPackageRepresentation(trigger, includeAllInstances))) {
              writeWarningToOutputStream("Warning Duplicate entry detected: Trigger " + triggerName);
            }
            else {
              triggersToBeStored.add(new TriggerPackageRepresentation(trigger, includeAllInstances));
            }
          }
        }
        break;
      }
    }
  }


  /**
   * Add a filter to the filter store list if it is not already on it. The filter is only added if the include xyna
   * components flag allows it.
   * 
   * @param filterName name of the filter to add
   * @throws XDEV_PackageDefinitionItemNotFoundException
   * @throws Ex_FileAccessException
   * @throws XPRC_XmlParsingException
   */
  private void tryToAddFilter(String filterName) throws XDEV_PackageDefinitionItemNotFoundException,
                  Ex_FileAccessException, XPRC_XmlParsingException {
    // first check trigger store list
    for (TriggerPackageRepresentation relevantTrigger : triggersToBeStored) {
      for (Filter filter : XynaFactory.getInstance().getActivation().getFilters(relevantTrigger.getTriggerName())) {
        if (filter.getName().equals(filterName)) {
          // found filter
          if (qualifiesForInclusion(containsXynaComponentMarker(XynaActivationTrigger
                          .getFilterXmlLocationByFqFilterClassName(filter.getFQFilterClassName(), VersionManagement.REVISION_WORKINGSET)))) {
            filtersToBeStored.add(filter);
          }
          return;
        }
      }
    }
    // check all registered triggers
    for (Trigger relevantTrigger : XynaFactory.getInstance().getActivation().getTriggers()) {
      for (Filter filter : XynaFactory.getInstance().getActivation().getFilters(relevantTrigger.getTriggerName())) {
        if (filter.getName().equals(filterName)) {
          // filter found
          if (qualifiesForInclusion(containsXynaComponentMarker(XynaActivationTrigger
                          .getFilterXmlLocationByFqFilterClassName(filter.getFQFilterClassName(), VersionManagement.REVISION_WORKINGSET)))) {
            filtersToBeStored.add(filter);
          }
          return;
        }
      }
    }
    throw new XDEV_PackageDefinitionItemNotFoundException(GenerationBase.EL.DEPENDENCY_FILTER, filterName);
  }


  /**
   * Add the full qualified name of a datatype to the mdm fq classes list and the name of the belonging xml file to
   * the mdm deployed files list. The names are only added if the include xyna
   * components flag allows it.
   * 
   * @param fqDatatypeName
   * @throws XDEV_PackageDefinitionItemNotFoundException
   * @throws Ex_FileAccessException
   * @throws XPRC_XmlParsingException
   */
  private void tryToAddMdmType(String fqDatatypeName) throws XDEV_PackageDefinitionItemNotFoundException,
                  Ex_FileAccessException, XPRC_XmlParsingException {
    String filename = GenerationBase.getFileLocationOfXmlNameForDeployment(fqDatatypeName) + ".xml";
    if (!new File(filename).exists()) {
      if (GenerationBase.isReservedServerObjectByFqOriginalName(fqDatatypeName)) {
        writeVerboseToOutputStream(fqDatatypeName + " is a reserved server object, ignoring");
        return;
      }
      else {
        throw new XDEV_PackageDefinitionItemNotFoundException(GenerationBase.EL.DEPENDENCY_DATATYPE, fqDatatypeName);
      }
    }
    if (qualifiesForInclusion(containsXynaComponentMarker(filename))) {
      if (logger.isDebugEnabled()) {
        logger.debug("Found relevant dependent file: " + filename);
      }
      mdmDeployedFilesToBeStored.add(filename);
      mdmFqClassesToBeStored.add(fqDatatypeName);
    }
    else {
      writeVerboseToOutputStream("Found relevant dependent file: " + filename + " - will not be included since it is a Xyna component");
    }
  }


  private boolean qualifiesForInclusion(boolean isXynaComponent) {
    return includeXynaComponents || !isXynaComponent;
  }


  /**
   * Check if the given component description file contains the xyna component flag in its meta section.
   * 
   * @param filename name of a trigger/filter description file
   * @return true, if the file contains a xyna component marker, else false. also return false if file not exists.
   * @throws XPRC_XmlParsingException
   * @throws Ex_FileAccessException
   */
  private boolean containsXynaComponentMarker(String filename) throws Ex_FileAccessException, XPRC_XmlParsingException {
    if (!new File(filename).exists()) {
      return false;
    }
    Element doc = XMLUtils.parse(filename).getDocumentElement();
    Element metaElement = XMLUtils.getChildElementByName(doc, GenerationBase.EL.META);
    if (metaElement == null) {
      return false;
    }
    Element xynaComponentElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.ISXYNACOMPONENT);
    return XMLUtils.getTextContent(xynaComponentElement).equals("true");
  }


  private DependencyRegister getDependencyRegister() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister();
  }


  private void writeStandardInfoToOutputStream(String s) {
    if (verboseOutput)
      statusStream.println(s);
  }


  private void writeVerboseToOutputStream(String s) {
    if (verboseOutput)
      statusStream.println(s);
  }


  private void writeWarningToOutputStream(String s) {
    statusStream.println(s);
  }


  public void setVerboseOutput(boolean b) {
    verboseOutput = b;
  }


  public boolean getVerboseOutput() {
    return verboseOutput;
  }


  public void setIncludeXynaComponents(boolean b) {
    includeXynaComponents = b;
  }


  public boolean isIncludeXynaComponents() {
    return includeXynaComponents;
  }


  public HashSet<String> getDeployedMdmFilesToBeStored() {
    return mdmDeployedFilesToBeStored;
  }


  public HashSet<String> getSavedMdmFilesToBeStored() {
    return mdmSavedFilesToBeStored;
  }
  
  
  public Set<String> getMainWorkflowsToBeDeployedOnInstallPackage() {
    return mainWorkflowsToBeDeployedOnInstallPackage;
  }

  
  public Set<String> getDataTypesToBeStored() {
    return dataTypesToBeStored;
  }
  
  
  public Set<String> getExceptionsToBeStored() {
    return exceptionsToBeStored;
  }
  

  public HashSet<String> getMdmFqClassesToBeStored() {
    return mdmFqClassesToBeStored;
  }


  public HashSet<CronLikeOrderInformation> getCronLikeOrderInformationToBeStored() {
    return cronLikeOrderInformationToBeStored;
  }


  public HashSet<CapacityInformation> getCapacityInformationToBeStored() {
    return capacityInformationToBeStored;
  }


  public HashSet<String> getPropertiesToBeStored() {
    return propertiesToBeStored;
  }


  public HashSet<Filter> getFiltersToBeStored() {
    return filtersToBeStored;
  }


  public HashSet<String> getSharedLibsToBeStored() {
    return sharedLibsToBeStored;
  }


  public HashSet<TriggerPackageRepresentation> getTriggersToBeStored() {
    return triggersToBeStored;
  }


  public Set<DependencyNode> getAllObjects() {
    return allObjects;
  }


  public Set<String> getOtherFilesToBeStored() {
    return otherFilesToBeStored;
  }


  public Collection<File> retrieveOtherFiles(String basedir) {
    Collection<File> files = new ArrayList<File>();
    StringBuilder filePathBuilder = new StringBuilder();
    for (String pathRelativToBaseDir : otherFilesToBeStored) {
      filePathBuilder.append(basedir).append(Constants.fileSeparator).append(pathRelativToBaseDir);
      File file = new File(filePathBuilder.toString());
      if (file != null && file.exists() && file.isFile()) {
        files.add(file);
      }
      else {
        writeVerboseToOutputStream("PackageDefinition did include a invalid file-element: " + filePathBuilder
                        .toString());
      }
      filePathBuilder = new StringBuilder();
    }
    return files;
  }
}
