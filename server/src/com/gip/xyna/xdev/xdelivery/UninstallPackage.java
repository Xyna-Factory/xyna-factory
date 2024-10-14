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
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStoppedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionFileInvalidRootException;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionItemNotFoundException;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessingPortal.DispatcherIdentification;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;
import com.gip.xyna.xprc.exceptions.XPRC_ExclusiveDeploymentInProgress;
import com.gip.xyna.xprc.exceptions.XPRC_InternalObjectMayNotBeUndeployedException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingDatabase;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;



public class UninstallPackage {

  private static final Logger logger = CentralFactoryLogging.getLogger(UninstallPackage.class);

  private boolean verboseOutput = false;

  // Used for output of console information
  private PrintStream out;

  private PackageDefinition packageDefinition;


  public UninstallPackage(File packageFile, OutputStream os) {

    packageDefinition = new PackageDefinition(packageFile, os);
    out = new PrintStream(os);
  }


  public void doUninstall() throws XDEV_PackageDefinitionFileInvalidRootException, XDEV_PackageDefinitionItemNotFoundException,
      PersistenceLayerException, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
      XPRC_MDMUndeploymentException, XACT_TriggerCouldNotBeStoppedException, XPRC_DESTINATION_NOT_FOUND,
      XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {

    // parse the package definition and all used objects depending on them 
    packageDefinition.parsePackageDefinitionFile();

    Set<DependencyNode> allObjects = packageDefinition.getAllObjects();

    // create a set containing all dependencies
    Set<DependencyNode> excludeSet = new HashSet<DependencyNode>();
    // iterate over all dependencies and check against other dependencies
    for (DependencyNode node : allObjects) {

      // duplicate set since getDependencies returns a unmodifiable set
      Set<DependencyNode> dependencies = new HashSet<DependencyNode>(getDependencyRegister()
                      .getDependencies(node.getUniqueName(), node.getType()));

      dependencies.removeAll(allObjects);
      if (dependencies.size() > 0) {
        // there are remaining dependencies to other objects that are
        // not included in the allObjects set
        excludeSet.addAll(getDependencyRegister().getAllUsedNodes(node.getUniqueName(), node.getType(), true, true));
      }
    }

    // the correct exclude set contains only objects that are also in allObjects
    excludeSet.retainAll(allObjects);


    // print warnings
    for (DependencyNode node : excludeSet) {

      // list all dependencies
      // duplicate set since getDependencies returns a unmodifiable set
      Set<DependencyNode> dependencies = new HashSet<DependencyNode>(getDependencyRegister()
                      .getDependencies(node.getUniqueName(), node.getType()));

      dependencies.removeAll(allObjects);
      if (dependencies.size() == 0) {
        writeWarningToOutputStream("Warning: " + node.getType().toString() + ": " + node.getUniqueName()
                        + " cannot be uninstalled.");
      } else if (dependencies.size() == 1) {
        writeWarningToOutputStream("Warning: " + node.getType().toString() + ": " + node.getUniqueName()
                        + " cannot be uninstalled due to existing dependency:");
      } else {
        writeWarningToOutputStream("Warning: " + node.getType().toString() + ": " + node.getUniqueName()
                        + " cannot be uninstalled due to existing dependencies:");
      }

      for (DependencyNode dep : dependencies) {
        writeWarningToOutputStream("\t* " + dep.getType().toString() + ": " + dep.getUniqueName());
      }
    }

    Set<DependencyNode> objectsToBeUninstalled = new HashSet<DependencyNode>(allObjects);
    objectsToBeUninstalled.removeAll(excludeSet);

    /*    for(DependencyNode node : objectsToBeUninstalled) {
          writeWarningToOutputStream("uninstall: " + node.getType().toString() + ": " + node.getUniqueName());
        }
      */

    // uninstall the objects
    // The order of uninstallation depends on the occurence in a typical dependency chain.
    // e.g Filters call workflows, therfore undeploy filters first
    // e.g triggers can old be removed, when no filter ist connected
    uninstallCapacities(objectsToBeUninstalled);
    uninstallCronLikeOrders(objectsToBeUninstalled);
    uninstallFilters(objectsToBeUninstalled);
    uninstallTriggers(objectsToBeUninstalled);
    uninstallWorkflows(objectsToBeUninstalled);
    uninstallSharedLibs(objectsToBeUninstalled);
    uninstallDatatypes(objectsToBeUninstalled);
    uninstallExceptions(objectsToBeUninstalled);
    uninstallProperties(objectsToBeUninstalled);


    // rescan the saved workflows from hard disc
    XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase().updateSavedProcessFQs();
  }


  private void uninstallCronLikeOrders(Set<DependencyNode> objects) {

    // The mapping between workflows und cronls orders is one to one. 
    // If a workflow is to be uninstalled, delete cron like order as well

    HashSet<CronLikeOrderInformation> set = packageDefinition.getCronLikeOrderInformationToBeStored();

    for (DependencyNode node : objects) {
      if (node.getType() == DependencySourceType.WORKFLOW) {

        // find corresponding cron ls order
        Map<Long, CronLikeOrderInformation> map;
        try {
          map = XynaFactory.getInstance().getFactoryManagementPortal()
                          .getProcessMonitoring().getAllCronLikeOrders(Long.MAX_VALUE);
        } catch (XPRC_CronLikeSchedulerException e) {
          map = null;
        }

        // FIXME this may result in out of memory and it might become awfully slow
        if (map != null) {
          for (Entry<Long, CronLikeOrderInformation> entry : map.entrySet()) {
            if (entry.getValue().getTargetOrdertype().equals(node.getUniqueName())) {
              set.add(entry.getValue());
            }
          }
        }
      }
    }

    for (CronLikeOrderInformation cloi : set) {
      writeVerboseToOutputStream("Removing cron like order id " + cloi.getId() + " for workflow " + cloi.getTargetOrdertype());
      try {
        XynaFactory.getInstance().getProcessing().removeCronLikeOrder(cloi.getId());
      } catch (XPRC_CronLikeSchedulerException e) {
        logger.warn("Failed to remove cron like order with id '" + cloi.getId() + "'", e);
        writeWarningToOutputStream("Failed to remove cron like order with id '" + cloi.getId()
                        + "', see log file for more information.");
        continue;
      }
    }
  }


  private void uninstallCapacities(Set<DependencyNode> objects) throws Ex_FileWriteException, PersistenceLayerException {
    // A capacity can only be uninstalled if all workflows belonging to order types using this 
    // capacity are also uninstalled.

    // Create a wf string set. That makes comparison easier
    Set<String> wfSet = new HashSet<String>();

    for (DependencyNode node : objects) {
      if (node.getType() == DependencySourceType.WORKFLOW) {
        wfSet.add(node.getUniqueName());
      }
    }
    
    Set<String> workflows = new HashSet<String>();

    HashSet<CapacityInformation> set = packageDefinition.getCapacityInformationToBeStored();
    for (CapacityInformation ci : set) {
      Set<String> orderTypes = getOrderTypesBelongingToCapacity(ci.getName());
      for (String orderType : orderTypes) {
        workflows.addAll(getWorkflowsByOrderType(orderType));
      }
      // check if all wf names are contained in objects/wfSet
      if (wfSet.containsAll(workflows)) {
        // capacity can be safely removed

        for (String orderType : orderTypes) {

          try {
            XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal()
                .removeCapacityForOrderType(orderType, ci.getName());
          } catch (PersistenceLayerException e) {
            throw new RuntimeException(e);
          }
          writeVerboseToOutputStream("Removing capacity " + ci.getName() + " for order type " + orderType);

        }
        writeVerboseToOutputStream("Deleteing capacity " + ci.getName());
        XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal().removeCapacity(ci.getName());
      } else {
        //there are existing dependencies to workflows, print warnings
        orderTypes.removeAll(wfSet);

        if (orderTypes.size() == 0)
          writeWarningToOutputStream("Warning: Capacity " + ci.getName() + " cannot be uninstalled.");
        else if (orderTypes.size() == 1)
          writeWarningToOutputStream("Warning: Capacity " + ci.getName()
                          + " cannot be uninstalled due to existing dependency:");
        else
          writeWarningToOutputStream("Warning: Capacity " + ci.getName()
                          + " cannot be uninstalled due to existing dependencies:");

        for (String s : orderTypes) {
          writeWarningToOutputStream("\t* WORKFLOW: " + s);
        }

      }

    }
  }


  private HashSet<String> getOrderTypesBelongingToCapacity(String capacityName) {

    HashSet<String> resultSet = new HashSet<String>();

    CapacityMappingDatabase capDataBase = XynaFactory.getInstance().getProcessing().getXynaProcessingODS()
                    .getCapacityMappingDatabase();

    // iterate over all order types
    Set<DestinationKey> orderTypes = capDataBase.getDestinationKeys();

    for (DestinationKey key : orderTypes) {

      for (Capacity cap : capDataBase.getCapacities(key)) {
        if (cap.getCapName().equals(capacityName))
          resultSet.add(key.getOrderType());
      }
    }
    return resultSet;
  }
  
  private HashSet<String> getWorkflowsByOrderType(String orderType) {
    HashSet<String> resultSet = new HashSet<String>();
    
    Map<DestinationKey, DestinationValue> destinations = XynaFactory.getInstance().getProcessing()
               .getDestinations(DispatcherIdentification.Execution);

    for (Entry<DestinationKey, DestinationValue> e : destinations.entrySet()) {
      if (e.getKey().getOrderType().equals(orderType)) {
        resultSet.add(e.getValue().getFQName());
      }
    }

    return resultSet;
  }


  private void uninstallFilters(Set<DependencyNode> objects) throws PersistenceLayerException,
      XDEV_PackageDefinitionItemNotFoundException {
    for (DependencyNode node : objects) {
      if (node.getType() == DependencySourceType.FILTER) {
        writeVerboseToOutputStream("Undeploying filter: " + node.getUniqueName());
        try {
          XynaFactory.getPortalInstance().getActivationPortal()
              .removeFilterWithUndeployingInstances(node.getUniqueName());
        } catch (XACT_FilterNotFound e) {
          throw new XDEV_PackageDefinitionItemNotFoundException(GenerationBase.EL.DEPENDENCY_FILTER,
                                                                node.getUniqueName(), e);
        }
      }
    }
  }


  private void uninstallTriggers(Set<DependencyNode> objects) throws XDEV_PackageDefinitionItemNotFoundException,
                  PersistenceLayerException, XACT_TriggerCouldNotBeStoppedException {
    for (DependencyNode node : objects) {
      if (node.getType() == DependencySourceType.TRIGGER) {
        writeVerboseToOutputStream("Undeploying all trigger instances");

        // Iterate over all trigger instances
        EventListenerInstance<?,?>[] elis;
        try {
          elis = XynaFactory.getPortalInstance().getActivationPortal().getTriggerInstances(node.getUniqueName());
        } catch (XACT_TriggerNotFound e) {
          throw new XDEV_PackageDefinitionItemNotFoundException(GenerationBase.EL.DEPENDENCY_TRIGGER, node
                          .getUniqueName(), e);
        }
        if (elis != null) {
          for (int j = 0; j < elis.length; j++) {
            writeVerboseToOutputStream("Undeploying trigger instance " + elis[j].getInstanceName());

            try {
              XynaFactory.getPortalInstance().getActivationPortal().undeployTrigger(node.getUniqueName(),
                                                                                    elis[j].getInstanceName());
            } catch (XACT_TriggerNotFound e) {
              throw new RuntimeException(e); //sollte nicht vorkommen, weil man oben bereits überprüft hat, dass der trigger existiert
            } catch (XACT_TriggerInstanceNotFound e) {
              throw new RuntimeException(e); //sollte nicht vorkommen, weil man die instanz gerade erst ermittelt hat
            }
          }
        }
        writeVerboseToOutputStream("Removing of triggers is diabled as long as bug 9456 has not been fixed!!!");
        //        writeVerboseToOutputStream("Removing trigger: " + node.getUniqueName());
        //        XynaFactory.getPortalInstance().getActivationPortal().removeTrigger(node.getUniqueName());
      }
    }
  }


  private void uninstallWorkflows(Set<DependencyNode> objects) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress {
    for (DependencyNode node : objects) {
      if (node.getType() == DependencySourceType.WORKFLOW) {
        writeVerboseToOutputStream("Undeploying Workflow: " + node.getUniqueName());

        Map<String, DeploymentStatus> map = XynaFactory.getPortalInstance().getProcessingPortal()
                        .listDeploymentStatuses(RevisionManagement.REVISION_DEFAULT_WORKSPACE).get(ApplicationEntryType.WORKFLOW);
        if (map.get(node.getUniqueName()) == DeploymentStatus.DEPLOYED) {
          try {
            XynaFactory.getInstance().getProcessing().getWorkflowEngine().undeployWorkflow(node.getUniqueName(), true, false);
          } catch (XPRC_InvalidPackageNameException e) {
            throw new RuntimeException(e); //dann sind die dependency nodes nicht okay
          } catch (XPRC_InternalObjectMayNotBeUndeployedException e) {
            throw new RuntimeException(e); //dann ist das package nicht okay
          } catch (XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT e) {
            throw new RuntimeException(e); //dependencies sollen auch undeployed werden
          }
        }

        String wfNameFile = GenerationBase.getFileLocationOfXmlNameForSaving(node.getUniqueName()) + ".xml";
        File f = new File(wfNameFile);
        if (!f.exists()) {
          writeWarningToOutputStream("Warning: MDM File " + wfNameFile + " does not exists");
        } else {
          writeVerboseToOutputStream("Removing MDM File: " + wfNameFile);
          f.delete();
          unregisterWorkflowFromXMOMCache(node.getUniqueName());
        }
      }
    }
  }


  private void uninstallDatatypes(Set<DependencyNode> objects) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress {

    for (DependencyNode node : objects) {
      if (node.getType() == DependencySourceType.DATATYPE) {

        // If there are existing dependencies to other data types undeployDatatype will fail.
        // Therefore undeploydatatype is called with recuse=true. This is save, since all 
        // dependencies are handled by our uninstallation method.
        // But before undeploying a data type we have to check if it is still deployed
        // There is no method for checking the deployment state of data types!!!
        // The DependencyRegister cannot be used since it records only dependencies

        // Therefore we check the files system.
        String wfNameFile = GenerationBase.getFileLocationOfXmlNameForDeployment(node.getUniqueName()) + ".xml";
        File f = new File(wfNameFile);
        if (f.exists()) {
          writeVerboseToOutputStream("Undeploying datatype: " + node.getUniqueName());
          try {
            XynaFactory.getInstance().getProcessing().getWorkflowEngine().undeployDatatype(node.getUniqueName(), true, false);
          } catch (XPRC_InvalidPackageNameException e) {
            throw new RuntimeException(e); //dann ist dependency node ungültig
          } catch (XPRC_InternalObjectMayNotBeUndeployedException e) {
            throw new RuntimeException(e); //dann ist package ungültig
          } catch (XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT e) {
            throw new RuntimeException(e); //dependencies sollen auch undeployed werden
          }
          //FIXME: Wait a short period of time. The deletion of the xml may take a while. This is os dependent.
          try {
            Thread.sleep(200);
          } catch (InterruptedException e) {
          }
        } else {
          writeVerboseToOutputStream("Datatype is alreday undeployed: " + node.getUniqueName());
        }

        wfNameFile = GenerationBase.getFileLocationOfXmlNameForSaving(node.getUniqueName()) + ".xml";
        f = new File(wfNameFile);
        if (!f.exists()) {
          writeWarningToOutputStream("Warning: MDM File " + wfNameFile + " does not exists");
        } else {
          writeVerboseToOutputStream("Removing MDM File: " + wfNameFile);
          f.delete();
          unregisterDatatypeOrExceptionFromXMOMCache(node.getUniqueName());
        }

        //remove service implementations
        File dir = new File(Constants.DEPLOYED_SERVICES_DIR + Constants.fileSeparator + node.getUniqueName());
        if (!(dir.isDirectory() && dir.exists())) {
          // no warning, since not all data typs have a service implementation
          //writeWarningToOutputStream("Warning: service implementation directory " + dir.getCanonicalPath() + " not found.");
        } else {
          // filter jar files
          File[] files = dir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
              return name.endsWith(".jar");
            }
          });

          for (File jarfile : files) {
            writeVerboseToOutputStream("Removing service implementation: " + jarfile.getAbsolutePath());

            jarfile.delete();
          }
          writeVerboseToOutputStream("Removing service implementation directory: " + dir.getAbsolutePath());

          if (!dir.delete()) {
            writeWarningToOutputStream("Warning: service implementation directory " + dir.getAbsolutePath()
                            + " could not be deleted.");
          }
        }
      }
    }
  }
  
  
  private void uninstallExceptions(Set<DependencyNode> objects) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
      XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress {
    for (DependencyNode node : objects) {
      if (node.getType() == DependencySourceType.XYNAEXCEPTION) {

        // If there are existing dependencies to other data types undeployException will fail.
        // Therefore undeployException is called with recuse=true. This is save, since all 
        // dependencies are handled by our uninstallation method.
        // But before undeploying a data type we have to check if it is still deployed
        // There is no method for checking the deployment state of data types!!!
        // The DependencyRegister cannot be used since it records only dependencies

        // Therefore we check the files system.
        String wfNameFile = GenerationBase.getFileLocationOfXmlNameForDeployment(node.getUniqueName()) + ".xml";
        File f = new File(wfNameFile);

        if (f.exists()) {
          writeVerboseToOutputStream("Undeploying exception: " + node.getUniqueName());

          try {
            XynaFactory.getInstance().getProcessing().getWorkflowEngine()
                .undeployException(node.getUniqueName(), true, false);
          } catch (XPRC_InvalidPackageNameException e) {
            throw new RuntimeException(e); //dann ist dependency node ungültig
          } catch (XPRC_InternalObjectMayNotBeUndeployedException e) {
            throw new RuntimeException(e); //dann ist package ungültig
          } catch (XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT e) {
            throw new RuntimeException(e); //dependencies sollen auch undeployed werden
          }
          //FIXME: Wait a short period of time. The deletion of the xml may take a while. This is os dependent.
          try {
            Thread.sleep(200);
          } catch (InterruptedException e) {
          }
        } else {
          writeVerboseToOutputStream("Exception is alreday undeployed: " + node.getUniqueName());
        }

        wfNameFile = GenerationBase.getFileLocationOfXmlNameForSaving(node.getUniqueName()) + ".xml";
        f = new File(wfNameFile);

        if (!f.exists()) {
          writeWarningToOutputStream("Warning: MDM File " + wfNameFile + " does not exists");
        } else {
          writeVerboseToOutputStream("Removing MDM File: " + wfNameFile);
          f.delete();
          unregisterDatatypeOrExceptionFromXMOMCache(node.getUniqueName());
        }

        //remove service implementations
        File dir = new File(Constants.DEPLOYED_SERVICES_DIR + Constants.fileSeparator + node.getUniqueName());

        if (!(dir.isDirectory() && dir.exists())) {
          // no warning, since not all data typs have a service implementation
          //writeWarningToOutputStream("Warning: service implementation directory " + dir.getCanonicalPath() + " not found.");
        } else {
          // filter jar files
          File[] files = dir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
              return name.endsWith(".jar");
            }
          });

          for (File jarfile : files) {
            writeVerboseToOutputStream("Removing service implementation: " + jarfile.getAbsolutePath());

            jarfile.delete();
          }

          writeVerboseToOutputStream("Removing service implementation directory: " + dir.getAbsolutePath());

          if (!dir.delete()) {
            writeWarningToOutputStream("Warning: service implementation directory " + dir.getAbsolutePath()
                + " could not be deleted.");
          }
        }
      }
    }
  }


  private void uninstallSharedLibs(Set<DependencyNode> objects) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    for (DependencyNode node : objects) {
      if (node.getType() == DependencySourceType.SHAREDLIB) {
        writeVerboseToOutputStream("Removing shared lib: " + node.getUniqueName());
        try {
          XynaFactory.getInstance().getProcessing().getWorkflowEngine().undeployDatatype(node.getUniqueName(), false, false);
        } catch (XPRC_InvalidPackageNameException e) {
          throw new RuntimeException(e); //dann ist dependency node ungültig
        } catch (XPRC_InternalObjectMayNotBeUndeployedException e) {
          throw new RuntimeException(e); //dann ist package ungültig
        }

        File sharedLibDir = new File(Constants.SHAREDLIB_BASEDIR + node.getUniqueName());
        if (!(sharedLibDir.isDirectory() && sharedLibDir.exists())) {
          writeWarningToOutputStream("Warning: Shared lib directory " + sharedLibDir.getAbsolutePath() + " not found.");
        } else {
          // remove all jars
          // filter jar files
          File[] files = sharedLibDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
              return name.endsWith(".jar");
            }
          });

          for (File jarfile : files) {
            writeVerboseToOutputStream("Removing shared library: " + jarfile.getAbsolutePath());

            jarfile.delete();
          }
          writeVerboseToOutputStream("Removing shared library directory " + sharedLibDir.getAbsolutePath());

          if (!sharedLibDir.delete())
            writeWarningToOutputStream("Warning: Shared lib directory " + sharedLibDir.getAbsolutePath()
                            + " could not be deleted.");
        }
      }
    }
  }


  private void uninstallProperties(Set<DependencyNode> objects) throws PersistenceLayerException {
    for (DependencyNode node : objects) {
      if (node.getType() == DependencySourceType.XYNAPROPERTY) {
        writeVerboseToOutputStream("Removing property: " + node.getUniqueName());
        XynaFactory.getPortalInstance().getFactoryManagementPortal().removeProperty(node.getUniqueName()); 
      }
    }
  }


  private DependencyRegister getDependencyRegister() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister();
  }


  private void writeVerboseToOutputStream(String s) {
    if (verboseOutput) {
      out.println(s);
    }
  }


  private void writeWarningToOutputStream(String s) {
    out.println(s);
  }


  public void setVerboseOutput(boolean b) {
    verboseOutput = b;
    packageDefinition.setVerboseOutput(b);
  }


  public boolean getVerboseOutput() {
    return verboseOutput;
  }
  
  
  private void unregisterDatatypeOrExceptionFromXMOMCache(String fqname) {
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase().unregisterMOMObject(fqname, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    } catch (XynaException e) {
      logger.warn("Could not unregister " + fqname + " during package uninstall",e);
    }
  }
  
  
  private void unregisterWorkflowFromXMOMCache(String fqname) {
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase().unregisterMOMObject(fqname, GenerationBase.EL.SERVICE, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    } catch (XynaException e) {
      logger.warn("Could not unregister workflow " + fqname + " during package uninstall",e);
    }
  }


}
