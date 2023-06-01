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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.update.Updater;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionFileInvalidRootException;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionFileNotFoundException;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionItemNotFoundException;
import com.gip.xyna.xdev.exceptions.XDEV_ZipFileCouldNotBeCreatedException;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilterInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xods.configuration.PropertyMap;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSDomainSpecificData;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSServer;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessingPortal.DispatcherIdentification;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingDatabase;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;



/**
 * Class for creating Xyna Packages. The method doBackup executes the package creation. The package is created depending
 * on a package definition file. In this file all items are listed which should be contained in the Xyna Package. Each
 * Xyna Package contains a delivery.xml file in which all elements from the package description are listed. Parameters
 * are added.
 */
public class CreateDeliveryItem {

  private static Logger logger = CentralFactoryLogging.getLogger(CreateDeliveryItem.class);

  public static final String COMMANDS_VERBOSE_OUTPUT = "-v";
  public static final String COMMANDS_INCLUDE_XYNA_COMP = "-i";

  private boolean verboseOutput = false;
  /**
   * Indicates if complete backup is requested
   */
  private boolean saveEverything = false;

  /**
   * Output stream for creating the zip file (aka delivery item)
   */
  private OutputStream packageOutputStream;

  /**
   * Used for output of console information
   */
  private PrintStream statusOutputStream;

  /**
   * Description of package content
   */
  private PackageDefinition packageDefinition;

  private FilenameFilter mdmDeployedObjectXmlFilter = new FilenameFilter() {

    public boolean accept(File dir, String name) {
      File f = new File(dir, name);
      Boolean isIncludedFileOrNonSvnDirectory = isIncludedFileOrNonSvnDirectory(f, name, ".xml");
      if (isIncludedFileOrNonSvnDirectory != null)
        return isIncludedFileOrNonSvnDirectory;
      String path = f.getPath();
      for (String s : packageDefinition.getDeployedMdmFilesToBeStored()) {
        if (s.startsWith(path))
          return true;
      }
      if (packageDefinition.getDeployedMdmFilesToBeStored().contains(f.getPath()))
        return true;
      return false;
    }

  };

  private FilenameFilter mdmSavedObjectXmlFilter = new FilenameFilter() {

    public boolean accept(File dir, String name) {
      File f = new File(dir, name);
      Boolean isIncludedFileOrNonSvnDirectory = isIncludedFileOrNonSvnDirectory(f, name, ".xml");
      if (isIncludedFileOrNonSvnDirectory != null)
        return isIncludedFileOrNonSvnDirectory;
      String path = f.getPath();
      for (String s : packageDefinition.getSavedMdmFilesToBeStored()) {
        if (s.startsWith(path))
          return true;
      }
      if (packageDefinition.getSavedMdmFilesToBeStored().contains(f.getPath()))
        return true;
      return false;
    }

  };


  private FilenameFilter serviceImplJarFilter = new FilenameFilter() {

    public boolean accept(File dir, String name) {
      File f = new File(dir, name);
      Boolean isIncludedFileOrNonSvnDirectory = isIncludedFileOrNonSvnDirectory(f, name, ".jar", ".xml");
      if (isIncludedFileOrNonSvnDirectory != null) {
        // the file could be identified. if it was decided that it should not be contained,
        // check that it's not an arbitrary part of a directory that is supposed to be exported.
        // if that is the case, export it, no matter what the file extension is.
        if (isIncludedFileOrNonSvnDirectory) {
          return true;
        }
        String possibleParentDirectory = dir.getAbsolutePath() + Constants.fileSeparator;
        for (String originalFqName : packageDefinition.getMdmFqClassesToBeStored()) {
          String sTransformed;
          try {
            // TODO should the paths really be equal to the java names? better write an update to rename
            // the directories and remove this transformation
            sTransformed = GenerationBase.transformNameForJava(originalFqName);
          } catch (XPRC_InvalidPackageNameException e) {
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
          }
          if (possibleParentDirectory.contains(Constants.fileSeparator + Constants.DEPLOYED_SERVICES_DIR
              + Constants.fileSeparator + sTransformed + Constants.fileSeparator)) {
            return true;
          }
        }
        return isIncludedFileOrNonSvnDirectory;
      }
      for (String originalFqName : packageDefinition.getMdmFqClassesToBeStored()) {
        String sTransformed;
        try {
          // TODO should the paths really be equal to the java names? better write an update to rename
          // the directories and remove this transformation
          sTransformed = GenerationBase.transformNameForJava(originalFqName);
        } catch (XPRC_InvalidPackageNameException e) {
          throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }
        if (dir.getAbsolutePath().contains(sTransformed) || name.equals(sTransformed)) {
          return true;
        }
      }
      return false;
    }

  };

  private FilenameFilter triggerJarFilter = new FilenameFilter() {

    public boolean accept(File dir, String name) {
      File f = new File(dir, name);
      Boolean isIncludedFileOrNonSvnDirectory = isIncludedFileOrNonSvnDirectory(f, name, ".jar", ".xml");
      if (isIncludedFileOrNonSvnDirectory != null) {
        return isIncludedFileOrNonSvnDirectory;
      }
      if (name.contains(".")) {
        name = name.substring(0, name.lastIndexOf("."));
      } else if (!f.isDirectory()) {
        return false;
      }
      for (TriggerPackageRepresentation trigger : packageDefinition.getTriggersToBeStored()) {
        String expectedString =
            trigger.getFqTriggerClassName().substring(trigger.getFqTriggerClassName().lastIndexOf(".") + 1);
        if (name.equals(expectedString))
          return true;
      }
      return false;
    }

  };

  private FilenameFilter filterJarFilter = new FilenameFilter() {

    public boolean accept(File dir, String name) {
      File f = new File(dir, name);
      Boolean isIncludedFileOrNonSvnDirectory = isIncludedFileOrNonSvnDirectory(f, name, ".jar", ".xml");
      if (isIncludedFileOrNonSvnDirectory != null) {
        return isIncludedFileOrNonSvnDirectory;
      }
      if (name.contains(".")) {
        name = name.substring(0, name.lastIndexOf("."));
      } else if (!f.isDirectory()) {
        return false;
      }
      for (Filter filter : packageDefinition.getFiltersToBeStored()) {
        String expectedString =
            filter.getFQFilterClassName().substring(filter.getFQFilterClassName().lastIndexOf(".") + 1);
        if (name.equals(expectedString))
          return true;
      }
      return false;
    }

  };

  private FilenameFilter sharedLibJarFilter = new FilenameFilter() {

    public boolean accept(File dir, String name) {
      File f = new File(dir, name);
      Boolean isIncludedFileOrNonSvnDirectory = isIncludedFileOrNonSvnDirectory(f, name, ".jar");
      if (isIncludedFileOrNonSvnDirectory != null)
        return isIncludedFileOrNonSvnDirectory;
      for (String s : packageDefinition.getSharedLibsToBeStored()) {
        String absolutePath = dir.getAbsolutePath();
        if (absolutePath.contains(s) || name.equals(s))
          return true;
      }
      return false;
    }

  };


  private Boolean isIncludedFileOrNonSvnDirectory(File f, String name, String suffix) {
    if (!name.endsWith(suffix) && !f.isDirectory()) {
      return false;
    }
    if (f.isDirectory() && f.getAbsolutePath().contains(".svn")) {
      return false;
    }
    if (saveEverything) {
      return true;
    }
    return null;
  }


  private Boolean isIncludedFileOrNonSvnDirectory(File f, String name, String suffixPossibility1,
                                                  String suffixPossibility2) {
    Boolean isIncludedFileOrNonSvnDirectory = isIncludedFileOrNonSvnDirectory(f, name, suffixPossibility1);
    if (isIncludedFileOrNonSvnDirectory != null) {
      if (!isIncludedFileOrNonSvnDirectory) {
        isIncludedFileOrNonSvnDirectory = isIncludedFileOrNonSvnDirectory(f, name, suffixPossibility2);
        if (isIncludedFileOrNonSvnDirectory != null)
          return isIncludedFileOrNonSvnDirectory;
      }
    }
    return isIncludedFileOrNonSvnDirectory;
  }


  /**
   * Constructor. Create a complete backup.
   * @param outputFile the zip file to create (Xyna Package)
   * @param logStream the stream to which the status/log information is written
   * @throws XDEV_PackageDefinitionFileNotFoundException
   * @throws IOException
   */
  public CreateDeliveryItem(File outputFile, OutputStream logStream)
      throws XDEV_PackageDefinitionFileNotFoundException, IOException {
    this(outputFile, logStream, null);
  }


  /**
   * Constructor. Create a package depending on the package definition.
   * @param outputFile the zip file to create (Xyna Package)
   * @param logStream the stream to which the status/log information is written
   * @param packageDefinitionFile the package definition which definies the content of the package
   * @throws XDEV_PackageDefinitionFileNotFoundException thrown if the package definition file could not be found
   * @throws IOException
   */
  public CreateDeliveryItem(File outputFile, OutputStream logStream, File packageDefinitionFile)
      throws XDEV_PackageDefinitionFileNotFoundException, IOException {
    this.packageOutputStream = getOutputStreamAndEnsureFileExistence(outputFile);
    this.statusOutputStream = new PrintStream(logStream);

    if (packageDefinitionFile == null) {
      saveEverything = true;
    } else {
      if (!packageDefinitionFile.exists()) {
        throw new XDEV_PackageDefinitionFileNotFoundException(packageDefinitionFile.getAbsolutePath());
      }
    }
    packageDefinition = new PackageDefinition(packageDefinitionFile, statusOutputStream);
  }


  /**
   * @param targetFile Output stream for creating the target file (aka delivery item)
   * @param logOutputStream Stream to write log/status information to
   * @param packageDefinitionFile XML file which defines the content of the target file
   * @throws XDEV_PackageDefinitionFileNotFoundException
   */
  public CreateDeliveryItem(OutputStream targetFile, OutputStream logOutputStream, InputStream packageDefinitionFile)
      throws XDEV_PackageDefinitionFileNotFoundException {

    this.packageOutputStream = targetFile;
    this.statusOutputStream = new PrintStream(logOutputStream);

    if (packageDefinitionFile == null) {
      saveEverything = true;
    }
    packageDefinition = new PackageDefinition(packageDefinitionFile, logOutputStream);
  }


  private static OutputStream getOutputStreamAndEnsureFileExistence(File outputFile) throws IOException {
    if (!outputFile.exists()) {
      if (outputFile.getParentFile() != null) {
        outputFile.getParentFile().mkdirs();
      }
      outputFile.createNewFile();
    }
    return new FileOutputStream(outputFile);
  }


  private void writeVerboseToOutputStream(String s) {
    if (verboseOutput) {
      writeToOutputStream(s);
    }
  }


  private void writeToOutputStream(String s) {
    try {
      statusOutputStream.write((s + "\n").getBytes(Constants.DEFAULT_ENCODING));
    } catch (Exception e) {
      logger.warn("could not write to provided output stream: " + s);
    }
  }


  /**
   * Create the delivery.xml file. Each package contains a delivery.xml file in which all information is stored which is
   * not available as file, e.g. start parameters for trigger instances and filter, definition of cron like orders ...
   * @throws Ex_FileAccessException
   * @throws XPRC_VERSION_DETECTION_PROBLEM
   * @throws PersistenceLayerException
   */
  private Document buildDeliveryXML() throws Ex_FileAccessException, XPRC_VERSION_DETECTION_PROBLEM,
      PersistenceLayerException {

    DocumentBuilder builder = null;
    try {
      builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      logger.error("Error building XML file: " + e);
      return null;
    }
    Document doc = builder.newDocument();
    Element root = doc.createElement(DeliveryItemConstants.CONTAINER_ELEMENT);
    doc.appendChild(root);

    // Exporting VersionNumber
    Element version = buildVersionNumberElement(doc);
    if (version != null) {
      root.appendChild(version);
    }
    
    // Exporting DataTypes
    Element dataTypes = buildDataTypesElement(doc);
    if (dataTypes != null) {
      root.appendChild(dataTypes);
    } 
    
    // Exporting Exceptions
    Element exceptions = buildExceptionsElement(doc);
    if (exceptions != null) {
      root.appendChild(exceptions);
    } 

    // Exporting Workflows
    Element workflows = buildWorkflowsElement(doc);
    if (workflows != null) {
      root.appendChild(workflows);
    } 

    // Exporting CronLikeOrders
    Element crons = buildCronLikeOrdersElement(doc);
    if (crons != null) {
      root.appendChild(crons);
    }

    // Exporting Properties
    Element properties = buildXynaPropertiesElement(doc);
    if (properties != null) {
      root.appendChild(properties);
    }

    // exporting triggers
    Element triggers = buildTriggersElement(doc);
    if (triggers != null) {
      root.appendChild(triggers);
    }

    // exporting filters
    Element filters = buildFiltersElement(doc);
    if (filters != null) {
      root.appendChild(filters);
    }

    // exporting capacities
    Element capacities = buildCapacitiesElement(doc);
    if (capacities != null) {
      root.appendChild(capacities);
    }

    // exporting capacityMapping
    Element capacityMappings = buildCapacityMappingElement(doc);
    if (capacityMappings != null) {
      root.appendChild(capacityMappings);
    }

    // exporting rights
    Element rights = buildRightsElement(doc);
    if (rights != null) {
      root.appendChild(rights);
    }
      
    // exporting domains
    Element domains = buildDomainsElement(doc);
    if (domains != null) {
      root.appendChild(domains);
    }

    // exporting roles
    Element roles = buildRolesElement(doc);
    if (roles != null) {
      root.appendChild(roles);
    }

    // exporting users
    Element users = buildUsersElement(doc);
    if (users != null) {
      root.appendChild(users);
    }

    // exporting monitoring properties
    Element monitoring = buildMonitoringElement(doc);
    if (monitoring != null) {
      root.appendChild(monitoring);
    }
      
    return doc;
  }


  /**
   * Create xml element for the current xyna server version number.
   * @param doc xml representation of delivery.xml
   * @return the version number node
   * @throws XPRC_VERSION_DETECTION_PROBLEM
   * @throws PersistenceLayerException
   */
  private Element buildVersionNumberElement(Document doc) throws XPRC_VERSION_DETECTION_PROBLEM,
      PersistenceLayerException {
    Element versionNumber = doc.createElement(DeliveryItemConstants.VERSION_NUMBER_ELEMENT);
    versionNumber.setTextContent(Updater.getInstance().getFactoryVersion().getString());
    return versionNumber;
  }

  
  /**
   * Create xml element for data types. In case of backup store all data types, else only store data types mentioned in
   * package defintion. For each data type its deployment status is stored.
   * @param doc xml representation of delivery.xml
   * @return the data types node
   */
  private Element buildDataTypesElement(Document doc) {
    Element dataTypesElement = doc.createElement(DeliveryItemConstants.DATA_TYPE_SECTION);

    Set<DependencyNode> doms =
        XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getDependencyRegister()
            .getDependencyNodesByType(DependencySourceType.DATATYPE);

    if (doms == null || doms.size() == 0) {
      return dataTypesElement;
    }

    for (DependencyNode depNode : doms) {
      String dataTypeOriginalFqName = depNode.getUniqueName();
      
      if (!saveEverything && !packageDefinition.getDataTypesToBeStored().contains(dataTypeOriginalFqName)) {
        continue;
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Creating XML entry for data type " + dataTypeOriginalFqName);
      }

      writeVerboseToOutputStream("Exporting data type '" + dataTypeOriginalFqName + "'");
      Element dataType = doc.createElement(DeliveryItemConstants.DATA_TYPE_ELEMENT);
      dataType.setTextContent(dataTypeOriginalFqName);
      dataTypesElement.appendChild(dataType);
    }

    if (dataTypesElement.getChildNodes().getLength() == 0) {
      return null;
    }

    return dataTypesElement;
  }

  
  /**
   * Create xml element for exceptions. In case of backup store all exceptions, else only store exceptions mentioned in
   * package defintion. For each exception its deployment status is stored.
   * @param doc xml representation of delivery.xml
   * @return the exceptions node
   */
  private Element buildExceptionsElement(Document doc) {
    Element exceptionsElement = doc.createElement(DeliveryItemConstants.EXCEPTION_SECTION);
    Set<DependencyNode> excepts =
        XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getDependencyRegister()
            .getDependencyNodesByType(DependencySourceType.XYNAEXCEPTION);

    if (excepts == null || excepts.size() == 0) {
      return exceptionsElement;
    }

    for (DependencyNode depNode : excepts) {
      String exceptionOriginalFqName = depNode.getUniqueName();
      
      if (!saveEverything && !packageDefinition.getExceptionsToBeStored().contains(exceptionOriginalFqName)) {
        continue;
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Creating XML entry for exception " + exceptionOriginalFqName);
      }

      writeVerboseToOutputStream("Exporting exception '" + exceptionOriginalFqName + "'");
      Element exception = doc.createElement(DeliveryItemConstants.EXCEPTION_ELEMENT);
      exception.setTextContent(exceptionOriginalFqName);
      exceptionsElement.appendChild(exception);
    }

    if (exceptionsElement.getChildNodes().getLength() == 0) {
      return null;
    }

    return exceptionsElement;
  }
  
  
  /**
   * Create xml element for workflows. In case of backup store all workflows, else only store workflows mentioned in
   * package defintion. For each workflow its deployment status is stored.
   * @param doc xml representation of delivery.xml
   * @return the workflows node
   */
  private Element buildWorkflowsElement(Document doc) {

    Element workflowsElement = doc.createElement(DeliveryItemConstants.WORKFLOW_SECTION);

    Map<String, DeploymentStatus> map =
        XynaFactory.getPortalInstance().getProcessingPortal().listDeploymentStatuses(RevisionManagement.REVISION_DEFAULT_WORKSPACE).get(ApplicationEntryType.WORKFLOW);

    for (String workflowOriginalFqName : map.keySet()) {

   /*   boolean packageDefContainsFile =
          packageDefinition.getDeployedMdmFilesToBeStored()
              .contains(GenerationBase.getFileLocationOfXmlNameForDeployment(workflowOriginalFqName) + ".xml");
      boolean packageDefContainsSavedFile =
          packageDefinition.getSavedMdmFilesToBeStored()
              .contains(GenerationBase.getFileLocationOfXmlNameForSaving(workflowOriginalFqName) + ".xml");*/
      if (!saveEverything && !packageDefinition.getMainWorkflowsToBeDeployedOnInstallPackage().contains(workflowOriginalFqName)) {
        continue;
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Creating XML entry for workflow " + workflowOriginalFqName);
      }
      writeVerboseToOutputStream("Exporting workflow '" + workflowOriginalFqName + "'");

      Element workflow = doc.createElement(DeliveryItemConstants.WORKFLOW_ELEMENT);
      workflow.setTextContent(workflowOriginalFqName);

      if (map.get(workflowOriginalFqName) == DeploymentStatus.DEPLOYED) {
        workflow
            .setAttribute(DeliveryItemConstants.WORKFLOW_DEPLOY_STATE_ATTR, DeliveryItemConstants.WORKFLOW_DEPLOYED);
        workflowsElement.appendChild(workflow);
      } else if (map.get(workflowOriginalFqName) == DeploymentStatus.SAVED) {
        workflow.setAttribute(DeliveryItemConstants.WORKFLOW_DEPLOY_STATE_ATTR, DeliveryItemConstants.WORKFLOW_SAVED);
        workflowsElement.appendChild(workflow);

      } else if (map.get(workflowOriginalFqName) == DeploymentStatus.DEPLOYMENT_ERROR) {
        logger.warn("Erroneous workflow " + workflowOriginalFqName + " will not be included in the delivery item.");
      } else {
        logger.warn("Workflow " + workflowOriginalFqName
            + " in unknown state will not be included in the delivery item.");
      }
    }
    if (workflowsElement.getChildNodes().getLength() == 0) {
      return null;
    }
    return workflowsElement;
  }


  private Element buildCronLikeOrdersElement(Document doc) {

    Element cronsElement = doc.createElement(DeliveryItemConstants.CRONLS_SECTION);

    Map<Long, CronLikeOrderInformation> map;
    try {
      map =
          XynaFactory.getInstance().getFactoryManagementPortal().getProcessMonitoring()
              .getAllCronLikeOrders(Long.MAX_VALUE);
    } catch (XPRC_CronLikeSchedulerException e) {
      writeToOutputStream("Could not obtain cron like order information, skipping.");
      return null;
    }

    for (Entry<Long, CronLikeOrderInformation> entry : map.entrySet()) {

      if (!saveEverything && !packageDefinition.getCronLikeOrderInformationToBeStored().contains(entry.getValue())) {
        writeVerboseToOutputStream("Skipping cron like order <" + entry.getKey() + ">");
        continue;
      }

      Element cron = doc.createElement(DeliveryItemConstants.CRONLS_ELEMENT);

      cron.setAttribute(DeliveryItemConstants.CRONLS_INTERVAL_ATTR, entry.getValue().getInterval().toString());
      cron.setAttribute(DeliveryItemConstants.CRONLS_STARTTIME_ATTR, entry.getValue().getStartTime().toString());
      cron.setAttribute(DeliveryItemConstants.CRONLS_ORDER_ATTR, entry.getValue().getTargetOrdertype());
      cron.setAttribute(DeliveryItemConstants.CRONLS_LABEL_ATTR, entry.getValue().getLabel());
      cron.setAttribute(DeliveryItemConstants.CRONLS_ONERROR_ATTR, entry.getValue().getOnError().name());
      cron.setAttribute(DeliveryItemConstants.CRONLS_ENABLED_ATTR, entry.getValue().isEnabled().toString());
      if (entry.getValue().getPayload() != null) {
        Element payload = doc.createElement(DeliveryItemConstants.CRONLS_PAYLOAD_ELEMENT);
        payload.setNodeValue(entry.getValue().getPayload());
      }

      writeVerboseToOutputStream("Backing up cron like order " + entry.getValue().getTargetOrdertype());
      cronsElement.appendChild(cron);

    }

    if (cronsElement.getChildNodes().getLength() == 0) {
      return null;
    }
    return cronsElement;

  }


  private Element buildXynaPropertiesElement(Document doc) {

    Element propertiesElement = doc.createElement(DeliveryItemConstants.PROPERTY_SECTION);

    // Exporting Properties
    PropertyMap<String, String> propertyMap =
        XynaFactory.getPortalInstance().getFactoryManagementPortal().getPropertiesReadOnly();

    for (String key : propertyMap.keySet()) {

      if (!saveEverything && !packageDefinition.getPropertiesToBeStored().contains(key)) {
        writeVerboseToOutputStream("Skipping property '" + key + "'");
        continue;
      }

      Element property = doc.createElement(DeliveryItemConstants.PROPERTY_ELEMENT);
      property.setAttribute(DeliveryItemConstants.PROPERTY_KEY, key);
      property.setAttribute(DeliveryItemConstants.PROPERTY_VALUE, propertyMap.get(key));

      writeVerboseToOutputStream("Backing up property " + key + " = " + propertyMap.get(key));
      propertiesElement.appendChild(property);
    }

    if (propertiesElement.getChildNodes().getLength() == 0)
      return null;
    return propertiesElement;
  }


  /**
   * Add trigger information to delivery.xml
   * @param doc
   * @return
   * @throws Ex_FileAccessException
   */
  private Element buildTriggersElement(Document doc) throws Ex_FileAccessException {

    // Exporting Triggers
    // //////////////////////////
    Element triggersElement = doc.createElement(DeliveryItemConstants.TRIGGER_SECTION);

    // Iterate over all triggers
    Trigger[] ts = XynaFactory.getPortalInstance().getActivationPortal().getTriggers();

    for (int i = 0; i < ts.length; i++) {

      TriggerPackageRepresentation currentTrigger = null;
      for (TriggerPackageRepresentation trigger : packageDefinition.getTriggersToBeStored()) {
        if (trigger.equals(ts[i])) {
          currentTrigger = trigger;
          break;
        }
      }
      if (!saveEverything && currentTrigger == null) {
        writeVerboseToOutputStream("Skipping trigger '" + ts[i].getTriggerName() + "'");
        continue;
      } else if (saveEverything && currentTrigger == null) {
        currentTrigger = new TriggerPackageRepresentation(ts[i], true);
      }


      writeVerboseToOutputStream("Backing up trigger " + ts[i].getTriggerName());

      // create for each trigger a separate Element
      Element trigger = doc.createElement(DeliveryItemConstants.TRIGGER_ELEMENT);

      trigger.setAttribute(DeliveryItemConstants.TRIGGER_FQCLASSNAME_ATTR, ts[i].getFQTriggerClassName());
      trigger.setAttribute(DeliveryItemConstants.TRIGGER_NAME_ATTR, ts[i].getTriggerName());


      // For each jar file a separate xml element is created
      File[] jars = ts[i].getJarFiles();
      if (jars != null) {
        for (int j = 0; j < jars.length; j++) {

          Element jar = doc.createElement(DeliveryItemConstants.TRIGGER_JAR_ELEMENT);
          String fileName;
          try {
            fileName =
                FileUtils
                    .getRelativePath(new File(RevisionManagement
                                         .getPathForRevision(PathType.ROOT, RevisionManagement.REVISION_DEFAULT_WORKSPACE))
                                         .getCanonicalPath(), jars[j].getCanonicalPath());
          } catch (IOException e) {
            throw new Ex_FileAccessException(jars[j].getAbsolutePath());
          }
          jar.setAttribute(DeliveryItemConstants.TRIGGER_JAR_NAME_ATTR, fileName);
          trigger.appendChild(jar);
        }
      }


      // For each shared lib a separate xml element is created
      String[] sharedLibs = ts[i].getSharedLibs();
      if (sharedLibs != null) {
        for (int j = 0; j < sharedLibs.length; j++) {
          Element sl = doc.createElement(DeliveryItemConstants.TRIGGER_SHAREDLIB_ELEMENT);
          sl.setAttribute(DeliveryItemConstants.TRIGGER_SHAREDLIB_NAME_ATTR, sharedLibs[j]);
          trigger.appendChild(sl);
        }
      }

      /*
       * don't
       */

      // Iterate over all trigger instances
      EventListenerInstance<?, ?>[] elis;
      try {
        elis = XynaFactory.getPortalInstance().getActivationPortal().getTriggerInstances(ts[i].getTriggerName());
      } catch (XACT_TriggerNotFound e) {
        throw new RuntimeException(e);
      }
      if (elis != null) {
        for (int j = 0; j < elis.length; j++) {
          // include all trigger instances
          if (currentTrigger != null && currentTrigger.isIncludeAllInstances()) {
            writeVerboseToOutputStream("Backing up trigger instance " + elis[j].getInstanceName());
            Element trigins = doc.createElement(DeliveryItemConstants.TRIGGER_INSTANCE_ELEMENT);
            trigins.setAttribute(DeliveryItemConstants.TRIGGER_INSTANCE_INSTANCENAME_ATTR, elis[j].getInstanceName());
            trigins.setAttribute(DeliveryItemConstants.TRIGGER_INSTANCE_NAME_ATTR, ts[i].getTriggerName());

            // for each start parameter an own xml element is created
            String[] startParams = elis[j].getStartParameterAsStringArray();
            if (startParams != null) {
              for (int k = 0; k < startParams.length; k++) {
                Element startParam = doc.createElement(DeliveryItemConstants.TRIGGER_INSTANCE_STARTPARAM_ELEMENT);
                startParam.setAttribute(DeliveryItemConstants.TRIGGER_INSTANCE_STARTPARAM_VALUE_ATTR, startParams[k]);
                trigins.appendChild(startParam);
              }
            }
            trigger.appendChild(trigins);
          } else {
            // only include instance that have filters bound to them that are contained in the packageDefinition
            for (Filter filterToBeStored : packageDefinition.getFiltersToBeStored()) {
              ConnectionFilterInstance<?>[] filterInstances =
                  XynaFactory.getPortalInstance().getActivationPortal().getFilterInstances(filterToBeStored.getName());
              for (ConnectionFilterInstance<?> filterInstance : filterInstances) {
                if (filterInstance.getTriggerInstanceName().equals(elis[j].getInstanceName())) {
                  writeVerboseToOutputStream("Backing up trigger instance " + elis[j].getInstanceName());
                  Element trigins = doc.createElement(DeliveryItemConstants.TRIGGER_INSTANCE_ELEMENT);
                  trigins.setAttribute(DeliveryItemConstants.TRIGGER_INSTANCE_INSTANCENAME_ATTR,
                                       elis[j].getInstanceName());
                  trigins.setAttribute(DeliveryItemConstants.TRIGGER_INSTANCE_NAME_ATTR, ts[i].getTriggerName());

                  // for each start parameter an own xml element is created
                  String[] startParams = elis[j].getStartParameterAsStringArray();
                  if (startParams != null) {
                    for (int k = 0; k < startParams.length; k++) {
                      Element startParam = doc.createElement(DeliveryItemConstants.TRIGGER_INSTANCE_STARTPARAM_ELEMENT);
                      startParam.setAttribute(DeliveryItemConstants.TRIGGER_INSTANCE_STARTPARAM_VALUE_ATTR,
                                              startParams[k]);
                      trigins.appendChild(startParam);
                    }
                  }
                  trigger.appendChild(trigins);
                }
              }
            }
          }
        }
      }
      // finally add the trigger to the triggers element
      triggersElement.appendChild(trigger);

    }
    if (triggersElement.getChildNodes().getLength() == 0)
      return null;
    return triggersElement;
  }


  private Element buildFiltersElement(Document doc) throws Ex_FileAccessException {

    // Exporting Filters
    // //////////////////////////
    Element filtersElement = doc.createElement(DeliveryItemConstants.FILTER_SECTION);

    // Iterate over all triggers to get all Filters
    Trigger[] ts = XynaFactory.getPortalInstance().getActivationPortal().getTriggers();
    for (int i = 0; i < ts.length; i++) {

      // Iterate over all filters corresponding to the trigger
      Filter[] fs = XynaFactory.getPortalInstance().getActivationPortal().getFilters(ts[i].getTriggerName());
      for (int j = 0; j < fs.length; j++) {

        if (!saveEverything && !packageDefinition.getFiltersToBeStored().contains(fs[j])) {
          writeVerboseToOutputStream("Skipping filter '" + fs[j].getName() + "'");
          continue;
        }

        TriggerPackageRepresentation currentTrigger = null;
        for (TriggerPackageRepresentation trigger : packageDefinition.getTriggersToBeStored()) {
          if (trigger.equals(ts[i])) {
            currentTrigger = trigger;
            break;
          }
        }

        if (!saveEverything && currentTrigger == null) {
          writeToOutputStream("Warning: Filter " + fs[j].getName() + " is associated with trigger "
              + ts[i].getFQTriggerClassName() + " which is not stored. Result may not be an installable package.");
        }


        // create for each filter a separate filter element
        Element filterElement = doc.createElement(DeliveryItemConstants.FILTER_ELEMENT);

        // setting attributes
        filterElement.setAttribute(DeliveryItemConstants.FILTER_FQCLASSNAME_ATTR, fs[j].getFQFilterClassName());
        filterElement.setAttribute(DeliveryItemConstants.FILTER_NAME_ATTR, fs[j].getName());
        filterElement.setAttribute(DeliveryItemConstants.FILTER_TRIGGERNAME_ATTR, ts[i].getTriggerName());

        writeVerboseToOutputStream("Backing up filter " + fs[j].getName());

        // adding jar files
        File[] jarFiles = fs[j].getJarFiles();
        if (jarFiles != null) {
          // convert File array to String array
          String[] jars = new String[jarFiles.length];
          for (int k = 0; k < jarFiles.length; k++) {
            try {
              jars[k] =
                  FileUtils.getRelativePath(new File(RevisionManagement
                                                .getPathForRevision(PathType.ROOT,
                                                                    RevisionManagement.REVISION_DEFAULT_WORKSPACE))
                                                .getCanonicalPath(), jarFiles[k].getCanonicalPath());
            } catch (IOException e) {
              throw new Ex_FileAccessException(jarFiles[k].getAbsolutePath());
            }
          }
          createElementsForArray(filterElement, jars, DeliveryItemConstants.FILTER_JAR_ELEMENT,
                                 DeliveryItemConstants.FILTER_JAR_NAME_ATTR);
        }

        // adding shared libraries
        createElementsForArray(filterElement, fs[j].getSharedLibs(), DeliveryItemConstants.FILTER_SHAREDLIB_ELEMENT,
                               DeliveryItemConstants.FILTER_SHAREDLIB_NAME_ATTR);

        // Iterate over all dependent filter instances
        ConnectionFilterInstance<?>[] cfis =
            XynaFactory.getPortalInstance().getActivationPortal().getFilterInstances(fs[j].getName());
        if (cfis != null) {
          for (int k = 0; k < cfis.length; k++) {

            // create new filter instance element
            Element filterInstanceElement = doc.createElement(DeliveryItemConstants.FILTER_INSTANCE_ELEMENT);

            // setting attributes
            filterInstanceElement.setAttribute(DeliveryItemConstants.FILTER_INSTANCE_INSTANCENAME_ATTR,
                                               cfis[k].getInstanceName());
            filterInstanceElement
                .setAttribute(DeliveryItemConstants.FILTER_INSTANCE_NAME_ATTR, cfis[k].getFilterName());
            filterInstanceElement.setAttribute(DeliveryItemConstants.FILTER_INSTANCE_TRIGGERINSTANCENAME_ATTR,
                                               cfis[k].getTriggerInstanceName());

            writeVerboseToOutputStream("Backing up filter instance " + cfis[k].getInstanceName());

            // appending to the filter element
            filterElement.appendChild(filterInstanceElement);
          }
        }

        // finally add the filter to the filters element
        filtersElement.appendChild(filterElement);

      }
    }
    if (filtersElement.getChildNodes().getLength() == 0)
      return null;
    return filtersElement;
  }


  private Element buildCapacitiesElement(Document doc) {


    // Exporting Capacities
    // //////////////////////////
    Element capacitiesElement = doc.createElement(DeliveryItemConstants.CAPACITY_SECTION);


    // export all capacities
    Collection<CapacityInformation> capInfoList =
        XynaFactory.getPortalInstance().getProcessingPortal().listCapacityInformation();

    for (CapacityInformation capInfo : capInfoList) {

      if (!saveEverything && !packageDefinition.getCapacityInformationToBeStored().contains(capInfo)) {
        writeVerboseToOutputStream("Skipping capacity '" + capInfo.getName() + "'");
        continue;
      }

      // create for each capacity a separate capacity element
      Element capacityElement = doc.createElement(DeliveryItemConstants.CAPACITY_ELEMENT);

      // setting attributes
      capacityElement.setAttribute(DeliveryItemConstants.CAPACITY_NAME_ATTR, capInfo.getName());
      capacityElement.setAttribute(DeliveryItemConstants.CAPACITY_CARDINALITY_ATTR,
                                   Integer.toString(capInfo.getCardinality()));
      // capacityElement.setAttribute(DeliveryItemConstants.CAPACITY_STATE_ATTR, Boolean.toString(capInfo.getState()));

      writeVerboseToOutputStream("Backing up capacity " + capInfo.getName() + " with cardinality "
          + capInfo.getCardinality());
      capacitiesElement.appendChild(capacityElement);
    }
    if (capacitiesElement.getChildNodes().getLength() == 0)
      return null;
    return capacitiesElement;
  }


  private Element buildCapacityMappingElement(Document doc) {

    Element capacityMappings = doc.createElement(DeliveryItemConstants.CAPACITYMAPPING_SECTION);

    // export capacity mapping to workflows

    CapacityMappingDatabase capDataBase =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase();

    Map<DestinationKey, DestinationValue> destinations =
        XynaFactory.getInstance().getProcessing().getDestinations(DispatcherIdentification.Execution);

    for (Entry<DestinationKey, DestinationValue> e : destinations.entrySet()) {

      if (saveEverything || packageDefinition.getMdmFqClassesToBeStored().contains(e.getValue().getFQName())) {

        for (Capacity cap : capDataBase.getCapacities(e.getKey())) {
          // create for each capacity a separate capacity element
          Element capacityMappingElement = doc.createElement(DeliveryItemConstants.CAPACITYMAPPING_ELEMENT);

          capacityMappingElement.setAttribute(DeliveryItemConstants.CAPACITYMAPPING_ORDERTYPE_ATTR, e.getKey()
              .getOrderType());
          capacityMappingElement
              .setAttribute(DeliveryItemConstants.CAPACITYMAPPING_CAPACITYNAME_ATTR, cap.getCapName());
          capacityMappingElement.setAttribute(DeliveryItemConstants.CAPACITYMAPPING_CARDINALITY_ATTR,
                                              Integer.toString(cap.getCardinality()));

          writeVerboseToOutputStream("Backing up capacity for order type " + e.getKey().getOrderType());

          capacityMappings.appendChild(capacityMappingElement);
        }

      } else {
        writeVerboseToOutputStream("Skipping capacity mapping for order type " + e.getKey().getOrderType());
      }

    }

    if (capacityMappings.getChildNodes().getLength() == 0)
      return null;
    return capacityMappings;
  }


  private Element buildRightsElement(Document doc) throws PersistenceLayerException {

    if (!saveEverything) {
      writeVerboseToOutputStream("Not exporting any rights");
      return null;
    }

    Element rightsElement = doc.createElement(DeliveryItemConstants.RIGHTS_SECTION);

    // Set<String> set =
    // XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getRights();
    Collection<Right> col =
        XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getRights(null);

    for (Right right : col) {
      if (!XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
          .isPredefined(UserManagement.PredefinedCategories.RIGHT, right.getName())) {
        Element rightElement = doc.createElement(DeliveryItemConstants.RIGHTS_ELEMENT);
        rightElement.setAttribute(DeliveryItemConstants.RIGHTS_NAME_ATTR, right.getName());
        rightElement.setAttribute(DeliveryItemConstants.RIGHTS_DESCRIPTION_ATTR, right.getDescription());

        writeVerboseToOutputStream("Backing up right " + right);

        rightsElement.appendChild(rightElement);
      }
    }

    if (rightsElement.getChildNodes().getLength() == 0)
      return null;
    return rightsElement;
  }


  private Element buildDomainsElement(Document doc) throws PersistenceLayerException {
    if (!saveEverything) {
      writeVerboseToOutputStream("Not exporting any domains");
      return null;
    }

    Element domainsElement = doc.createElement(DeliveryItemConstants.DOMAINS_SECTION);

    Collection<Domain> col =
        XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getDomains();

    for (Domain domain : col) {
      if (!XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
          .isPredefined(UserManagement.PredefinedCategories.DOMAIN, domain.getName())) {
        Element domainElement = doc.createElement(DeliveryItemConstants.DOMAIN_ELEMENT);
        domainElement.setAttribute(DeliveryItemConstants.DOMAINS_NAME_ATTR, domain.getName());
        domainElement.setAttribute(DeliveryItemConstants.DOMAINS_DESCRIPTION_ATTR, domain.getDescription());
        domainElement.setAttribute(DeliveryItemConstants.DOMAINS_MAXRETRIES_ATTR,
                                   Integer.toString(domain.getMaxRetries()));
        domainElement.setAttribute(DeliveryItemConstants.DOMAINS_CONNECTIONTIMEOUT_ATTR,
                                   Integer.toString(domain.getConnectionTimeout()));
        DomainType type = domain.getDomainTypeAsEnum();
        domainElement.setAttribute(DeliveryItemConstants.DOMAINS_TYPE_ATTR, type.toString());
        switch (type) {
          case LOCAL : // atm no local domains should be exported because there can be only one and it's predefined
            break;
          case RADIUS :
            if (domain.getDomainSpecificData() != null
                && (domain.getDomainSpecificData() instanceof RADIUSDomainSpecificData)) {
              RADIUSDomainSpecificData data = (RADIUSDomainSpecificData) domain.getDomainSpecificData();
              domainElement.setAttribute(DeliveryItemConstants.DOMAINS_RADIUSDATA_ASSOCIATEDORDERTYPE_ATTR,
                                         data.getAssociatedOrdertype());
              for (RADIUSServer server : data.getServerList()) {
                Element serverElement = doc.createElement(DeliveryItemConstants.DOMAINS_RADIUSSERVER_ELEMENT);
                serverElement.setAttribute(DeliveryItemConstants.DOMAINS_RADIUSSERVER_IP_ATTR, server.getIp()
                    .getValue());
                serverElement.setAttribute(DeliveryItemConstants.DOMAINS_RADIUSSERVER_PORT_ATTR,
                                           Integer.toString(server.getPort().getValue()));
                serverElement.setAttribute(DeliveryItemConstants.DOMAINS_RADIUSSERVER_PRESHAREDKEY_ATTR, server
                    .getPresharedKey().getKey());
                domainElement.appendChild(serverElement);
              }
            }
            break;
        }

        writeVerboseToOutputStream("Backing up domain " + domain.getName());

        domainsElement.appendChild(domainElement);
      }
    }

    if (domainsElement.getChildNodes().getLength() == 0)
      return null;
    return domainsElement;
  }


  private Element buildRolesElement(Document doc) throws PersistenceLayerException {

    if (!saveEverything) {
      writeVerboseToOutputStream("Not exporting any roles");
      return null;
    }

    Element rolesElement = doc.createElement(DeliveryItemConstants.ROLE_SECTION);

    // Map<String, Role> map =
    // XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getRoles();
    Collection<Role> col =
        XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getRoles();

    if (col != null) {
      for (Role r : col) {

        if (!XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
            .isPredefined(UserManagement.PredefinedCategories.ROLE, r.getName())) {
          Element roleElement = doc.createElement(DeliveryItemConstants.ROLE_ELEMENT);
          roleElement.setAttribute(DeliveryItemConstants.ROLE_NAME_ATTR, r.getName());
          roleElement.setAttribute(DeliveryItemConstants.ROLE_DESCRIPTION_ATTR, r.getDescription());
          roleElement.setAttribute(DeliveryItemConstants.ROLE_ALIAS_ATTR, r.getAlias());
          roleElement.setAttribute(DeliveryItemConstants.ROLE_DOMAIN_ATTR, r.getDomain());

          writeVerboseToOutputStream("Backing up role " + r.getName());

          for (String right : r.getRightsAsList()) {
            Element rightElement = doc.createElement(DeliveryItemConstants.RIGHTS_ELEMENT);
            rightElement.setAttribute(DeliveryItemConstants.ROLE_RIGHTNAME_ATTR, right);
            roleElement.appendChild(rightElement);
            writeVerboseToOutputStream("  Granted right " + right);
          }
          rolesElement.appendChild(roleElement);
        }
      }
    }
    if (rolesElement.getChildNodes().getLength() == 0)
      return null;
    return rolesElement;
  }


  private Element buildUsersElement(Document doc) throws PersistenceLayerException {

    if (!saveEverything) {
      writeVerboseToOutputStream("Not exporting any users");
      return null;
    }

    Element usersElement = doc.createElement(DeliveryItemConstants.USER_SECTION);

    Collection<User> col =
        XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getUsers();

    if (col != null) {
      for (User u : col) {
        Element userElement = doc.createElement(DeliveryItemConstants.USER_ELEMENT);

        writeVerboseToOutputStream("Backing up user " + u.getName() + " with role " + u.getRole());

        userElement.setAttribute(DeliveryItemConstants.USER_PASSWORD_ATTR, u.getPassword());
        userElement.setAttribute(DeliveryItemConstants.USER_ROLENAME_ATTR, u.getRole());
        userElement.setAttribute(DeliveryItemConstants.USER_USERID_ATTR, u.getName());
        userElement.setAttribute(DeliveryItemConstants.USER_LOCKED_ATTR, Boolean.toString(u.isLocked()));


        List<String> d = u.getDomainList();
        if (d != null) {
          for (String domain : d) {
            Element domainElement = doc.createElement(DeliveryItemConstants.DOMAIN_ELEMENT);
            domainElement.setAttribute(DeliveryItemConstants.USER_DOMAIN_ATTR, domain);
            userElement.appendChild(domainElement);
            writeVerboseToOutputStream("  Assigned domain " + domain);
          }
        }

        usersElement.appendChild(userElement);
      }
    }

    if (usersElement.getChildNodes().getLength() == 0)
      return null;

    return usersElement;

  }


  private Element buildMonitoringElement(Document doc) {

    Element monElement = doc.createElement(DeliveryItemConstants.MONITORING_SECTION);

    Map<DestinationKey, Integer> map =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher()
            .getAllMonitoringLevels();

    if (map != null) {
      for (DestinationKey dk : map.keySet()) {

        if (!saveEverything) {
          DestinationValue dv = null;
          try {
            dv =
                XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
                    .getExecutionDestination(dk);
          } catch (XynaException e) {
            // nothing to be done, workflow probably has been undeployed
          }
          if (dv == null || !packageDefinition.getMdmFqClassesToBeStored().contains(dv.getFQName())) {
            if (dv == null) {
              writeVerboseToOutputStream("Skipping monitoring settings configured for previously configured order type "
                  + dk.getOrderType());
            } else {
              writeVerboseToOutputStream("Skipping monitoring settings for workflow " + dv.getFQName());
            }
            continue;
          }
        }

        Element monpropElement = doc.createElement(DeliveryItemConstants.MONITORING_ELEMENT);

        monpropElement.setAttribute(DeliveryItemConstants.MONITORING_LEVEL_ATTR, map.get(dk).toString());
        monpropElement.setAttribute(DeliveryItemConstants.MONITORING_WF_ATTR, dk.getOrderType());

        writeVerboseToOutputStream("Backing up monitoring level " + map.get(dk).toString() + " for workflow "
            + dk.getOrderType());
        monElement.appendChild(monpropElement);
      }
    }

    if (monElement.getChildNodes().getLength() == 0)
      return null;

    return monElement;

  }


  private void createElementsForArray(Element e, String[] s, String elementName, String attrName) {
    Document doc = e.getOwnerDocument();
    Element subElement;
    if (s != null) {
      for (int i = 0; i < s.length; i++) {
        subElement = doc.createElement(elementName);
        subElement.setAttribute(attrName, s[i]);
        e.appendChild(subElement);
      }
    }
  }

  
  public final static String PACKAGE_DIR_SAVED = "MDM/saved";
  public final static String PACKAGE_DIR_SERVICES = "server/services";
  public final static String PACKAGE_DIR_SHAREDLIBS = "server/sharedLibs";
  public final static String PACKAGE_DIR_FILTER = "server/filter";
  public final static String PACKAGE_DIR_TRIGGER = "server/trigger";
  

  /**
   * Create a zip file which contains all components mentioned in the package description and their dependencies.
   * @throws XDEV_ZipFileCouldNotBeCreatedException
   * @throws Ex_FileAccessException
   * @throws XDEV_PackageDefinitionFileInvalidRootException
   * @throws XDEV_PackageDefinitionItemNotFoundException
   * @throws PersistenceLayerException
   * @throws XPRC_VERSION_DETECTION_PROBLEM
   * @throws XPRC_XmlParsingException
   * @throws IOException
   * @throws XPRC_DESTINATION_NOT_FOUND
   */
  public void doBackup() throws XDEV_ZipFileCouldNotBeCreatedException, Ex_FileAccessException,
      XDEV_PackageDefinitionFileInvalidRootException, XDEV_PackageDefinitionItemNotFoundException,
      PersistenceLayerException, XPRC_VERSION_DETECTION_PROBLEM, XPRC_XmlParsingException, IOException,
      XPRC_DESTINATION_NOT_FOUND {

    if (!saveEverything) {
      // read package definition file
      packageDefinition.parsePackageDefinitionFile();
    }
    writeVerboseToOutputStream("Creating package...");

    HashMap<File, String> destinationMapping = new HashMap<File, String>();

    ArrayList<File> files = new ArrayList<File>();

    // alle files in mdm-dirs
    String deployedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    findFilesRecursively(new File(deployedMdmDir), files, mdmDeployedObjectXmlFilter);
    // map all files in the deployed directory to the saved directory
    String savedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, RevisionManagement.REVISION_DEFAULT_WORKSPACE, false);
    for (File f : files) {
      File tmpfile = new File(f.getPath().replace(deployedMdmDir, savedMdmDir));
      String relativepath;
      try {
        String baseDirCanonical = new File(DeliveryItemConstants.BASEDIR).getCanonicalPath();
        String tmpFileCanonical = tmpfile.getCanonicalPath();
        relativepath = FileUtils.getRelativePath(baseDirCanonical, tmpFileCanonical);
      } catch (IOException e) {
        throw new Ex_FileAccessException(tmpfile.getAbsolutePath());
      }
      destinationMapping.put(f, relativepath);
    }

    findFilesRecursively(new File(savedMdmDir), files, mdmSavedObjectXmlFilter);

    // alle files in service-dirs
    String deployedServicesDir = RevisionManagement.getPathForRevision(PathType.SERVICE, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    findFilesRecursively(new File(deployedServicesDir), files, serviceImplJarFilter);

    // alle files in sharedlib-dirs
    String sharedLibDir = RevisionManagement.getPathForRevision(PathType.SHAREDLIB, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    findFilesRecursively(new File(sharedLibDir), files, sharedLibJarFilter);

    // alle files in filter dir
    String filterDir = RevisionManagement.getPathForRevision(PathType.FILTER, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    findFilesRecursively(new File(filterDir), files, filterJarFilter);

    // alle files in trigger dirs
    String triggerDir = RevisionManagement.getPathForRevision(PathType.TRIGGER, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    findFilesRecursively(new File(triggerDir), files, triggerJarFilter);

    files.addAll(packageDefinition.retrieveOtherFiles(Constants.BASEDIR));

    if (saveEverything) {
      files.add(new File(DeliveryItemConstants.LOG4J_PROPERTIES));
      files.add(new File(DeliveryItemConstants.SERVER_POLICY));
    }

    ZipOutputStream zos = new ZipOutputStream(packageOutputStream);

    for (File f : files) {

      if (!f.isDirectory()) {
        String path =
            FileUtils.getRelativePath(new File(DeliveryItemConstants.BASEDIR).getCanonicalPath(), f.getCanonicalPath());

        // if there is a mapping to that file ignore it.
        boolean found = false;
        for (File ff : destinationMapping.keySet()) {
          if (destinationMapping.get(ff).equals(path)) {
            found = true;
            break;
          }
        }

        if (found) {
          continue; // ignore this file
        }

        // if there is a mapping for this file use the new path
        if (destinationMapping.get(f) != null) {
          path = destinationMapping.get(f);
        }

        // the following line is just a workaround to make it work under windows
        path = path.replace(File.separatorChar, '/');
        if (logger.isTraceEnabled()) {
          logger.trace("Adding file to backup: " + path);
        }
        writeVerboseToOutputStream("Adding file to backup: " + f.getAbsolutePath());

        path = path.replaceAll(Constants.PREFIX_SAVED + Constants.fileSeparator + Constants.SUBDIR_XMOM, PACKAGE_DIR_SAVED);
        path = path.replaceAll(Constants.REVISION_PATH + Constants.fileSeparator + Constants.PREFIX_REVISION + 
                        Constants.SUFFIX_REVISION_WORKINGSET + Constants.fileSeparator + Constants.SUBDIR_SERVICES,
                        PACKAGE_DIR_SERVICES);
        path = path.replaceAll(Constants.REVISION_PATH + Constants.fileSeparator + Constants.PREFIX_REVISION + 
                        Constants.SUFFIX_REVISION_WORKINGSET + Constants.fileSeparator + Constants.SUBDIR_SHAREDLIBS,
                        PACKAGE_DIR_SHAREDLIBS);
        path = path.replaceAll(Constants.REVISION_PATH + Constants.fileSeparator + Constants.PREFIX_REVISION + 
                        Constants.SUFFIX_REVISION_WORKINGSET + Constants.fileSeparator + Constants.SUBDIR_FILTER,
                        PACKAGE_DIR_FILTER);
        path = path.replaceAll(Constants.REVISION_PATH + Constants.fileSeparator + Constants.PREFIX_REVISION + 
                        Constants.SUFFIX_REVISION_WORKINGSET + Constants.fileSeparator + Constants.SUBDIR_TRIGGER,
                        PACKAGE_DIR_TRIGGER);
        
        
        // create new Zip Entry
        ZipEntry ze = new ZipEntry(path);

        FileInputStream fis = new FileInputStream(f);
        try {
          zos.putNextEntry(ze);

          // copy bytes from input stream to zip stream: i.e. do the zipping
          byte[] data = new byte[2048];
          int length;

          while ((length = fis.read(data)) != -1) {
            zos.write(data, 0, length);
          }
        } finally {
          zos.closeEntry();
          fis.close();
        }
      }
    }

    // Create new zip entry for delivery.xml
    ZipEntry ze = new ZipEntry(DeliveryItemConstants.XML_FILE);

    try {
      zos.putNextEntry(ze);
      Document doc = buildDeliveryXML();
      XMLUtils.saveDomToOutputStream(zos, doc);
    } finally {
      zos.closeEntry();
    }

    zos.flush();
    zos.close();

    writeVerboseToOutputStream("Package creation complete.");

  }


  /**
   * Store all file from a base directory in a file list. Files are searched through all sub directories. Directories
   * are not added.
   * @param basedir the base directory to start the search from
   * @param list the list holding all found files
   * @param ff a file filter
   */
  private static void findFilesRecursively(File basedir, ArrayList<File> list, FilenameFilter ff) {
    File[] files = basedir.listFiles(ff);
    if (files == null)
      return;

    for (File f : files) {
      if (f.isDirectory()) {
        findFilesRecursively(f, list, ff);
      } else {
        list.add(f);
      }
    }
  }


  public void setVerboseOutput(boolean b) {
    verboseOutput = b;
    packageDefinition.setVerboseOutput(b);
  }


  public void setIncludeXynaComponents(boolean b) {
    packageDefinition.setIncludeXynaComponents(b);
  }

}
