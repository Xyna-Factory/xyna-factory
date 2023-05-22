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
package com.gip.xyna.xdev.xlibdev.xmomaccess;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.update.Updater;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.BasicProjectCreationOrChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EmptyRepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EventType;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.FilterChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.ProjectCreationOrChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.TriggerChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.XMOMChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.XMOMMovementEvent;
import com.gip.xyna.xdev.exceptions.XDEV_PathNotFoundException;
import com.gip.xyna.xdev.exceptions.XDEV_RepositoryAccessException;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.ComponentType;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess.RecursionDepth;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess.RepositoryTransaction;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryItemModification;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryItemModification.RepositoryModificationType;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.XynaMultiChannelPortal.Identity;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_GENERAL_UPDATE_ERROR;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

public class XMOMAccess {

  private static final Logger logger = CentralFactoryLogging.getLogger(XMOMAccess.class);

  private static final String APPLICATION_CONFIGURATION_PREFIX = "Application_";
  
  private String name;
  private Long revision;
  private RepositoryAccess repositoryAccess;
  
  private ReentrantLock repositoryLock = new ReentrantLock();
  
  
  public static enum Component {
    xmom("xmom", PathType.XMOM),
    configuration("configuration", null),
    binaries("binaries", null),
    trigger("binaries" + Constants.fileSeparator + "trigger", PathType.TRIGGER),
    filter("binaries" + Constants.fileSeparator + "filter", PathType.FILTER),
    sharedlibs("binaries" + Constants.fileSeparator + "sharedlibs", PathType.SHAREDLIB),
    servicegroups("binaries" + Constants.fileSeparator + "servicegroups", PathType.SERVICE);
    
    private String repositorySubFolder;
    private PathType pathType;
    
    private Component(String subFolder, PathType pathType) {
      this.repositorySubFolder = subFolder;
      this.pathType = pathType;
    }
    
    public String getSavedDir(Long revision) {
      if (pathType == null) {
        throw new UnsupportedOperationException(this.toString() + " has no equivalent saved directory");
      }
      String dir =  RevisionManagement.getPathForRevision(pathType, revision, false);
      if (dir.endsWith(Constants.fileSeparator)) {
        dir = dir.substring(0, dir.length()-1);
      }
      return dir;
    }
    
    public String getSavedDir(Long revision, String specificFolder) {
      return getSavedDir(revision) + Constants.fileSeparator + specificFolder;
    }
    
    public String getRepositorySubFolder() {
      return repositorySubFolder;
    }
    
    public String getRepositorySubFolder(String specificFolder) {
      return getRepositorySubFolder() + Constants.fileSeparator + specificFolder;
    }
    
  }
  
  private static final EnumSet<Component> binaries = EnumSet.of(Component.trigger,
                                                                          Component.filter,
                                                                          Component.sharedlibs,
                                                                          Component.servicegroups);

  private class XmlFilenameFilter implements FilenameFilter{

    public boolean accept(File dir, String name) {
      return name.endsWith(".xml") || new File(dir, name).isDirectory();
    }
  }
  
  private static class JarFileFilter implements FilenameFilter{

    public boolean accept(File dir, String name) {
      return name.endsWith(".jar") || new File(dir, name).isDirectory();
    }
  }
  
  
  public XMOMAccess(String name, Long revision, RepositoryAccess repositoryAccess) {
    this.name = name;
    this.revision = revision;
    this.repositoryAccess = repositoryAccess;
  }


  /**
   * auschecken, ggfs deployment anstossen, etc
   * @param includeXynaProperties
   * @param includeCapacities
   */
  public void checkout(boolean includeXynaProperties, boolean includeCapacities, boolean deploy) {
    //auschecken
    refreshFromRepository();
    
    //xmls nach saved kopieren
    saveXMOM();
    
    //jars nach saved kopieren
    copyJarsToSavedDir();
    
    //Applications anlegen und ggf. Objekte deployen
    copyApplications(includeXynaProperties, includeCapacities, deploy);
    
    //TODO Trigger/Filter aus components.xml disabled deployen
  }
  
  
  /**
   * Deployment der Objekte aus einer Application.
   * @param componentName Name der Application
   * @throws XynaException
   */
  public void deployComponent(String componentName) throws XynaException {
    ApplicationManagementImpl applicationManagement =
                    (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    File applicationFile = new File(getAbsoluteAppConfigFileName(componentName));
    if (!applicationFile.exists()) {
      throw new IllegalArgumentException("Component '" + componentName + "' not found");
    }
    ApplicationXmlEntry applicationXml = applicationManagement.parseApplicationXml(applicationFile);
    applicationManagement.deployApplication(applicationXml, revision, "XMOM Access: Deploy component " + componentName);
  }

  /**
   * Deployment der Objekte aus allen Application.
   * @throws XynaException
   */
  public void deployAllComponents() throws XynaException {
    List<File> applicationXmls = new ArrayList<File>();
    String sourceDir = getLocalRepository(Component.configuration);
    FileUtils.findFilesRecursively(new File(sourceDir), applicationXmls, new XmlFilenameFilter());
    
    ApplicationManagementImpl applicationManagement =
                    (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

    for (File applicationFile : applicationXmls) {
      if (applicationFile.getName().startsWith(APPLICATION_CONFIGURATION_PREFIX)) {
        ApplicationXmlEntry applicationXml = applicationManagement.parseApplicationXml(applicationFile);
        applicationManagement.deployApplication(applicationXml, revision, "XMOM Access: Deploy all components");
      }
    }
  }
  
  private List<RepositoryItemModification> refreshFromRepository() {
    boolean success = false;
    final RepositoryTransaction transaction = repositoryAccess.beginTransaction("XMOMAccess initialization");
    try {
      List<RepositoryItemModification> modifiedFiles = null;
      //nur die Verzeichnise f�r XMOMAccess auschecken/updaten, damit beim CodeAcces die Files noch als modified erkannt werden
      try {
        if (!new File(repositoryAccess.getLocalRepository()).exists()) {
          transaction.checkout(new String[] {""}, repositoryAccess.getHeadVersion(), RecursionDepth.TARGET_ONLY);
        }
       modifiedFiles = transaction.update(new String[] {Component.binaries.getRepositorySubFolder(), Component.configuration.getRepositorySubFolder(), Component.xmom.getRepositorySubFolder()}, repositoryAccess.getHeadVersion(), RecursionDepth.FULL_RECURSION);
      } catch (XDEV_PathNotFoundException e) {
        // ok, dann muss das im svn erst angelegt werden.
        logger.info("Failed to checkout project head", e);
        return null;
      } catch (XDEV_RepositoryAccessException e) {
        logger.warn("Error during repository refresh", e);
        return null;
      }

      success = true;
      return modifiedFiles;
    }
    finally {
      endTransaction(transaction, !success);
    }
  }

  /**
   * Liest alle xmls aus dem xmom-Verzeichnis und kopiert sie ins saved-Verzeichnis
   */
  private void saveXMOM() {
    List<File> xmomXmls = new ArrayList<File>();
    String sourceDir = getLocalRepository(Component.xmom);
    FileUtils.findFilesRecursively(new File(sourceDir), xmomXmls, new XmlFilenameFilter());
    
    for (File xmlfile : xmomXmls) {
      try {
        String xml = FileUtils.readFileAsString(xmlfile);
        XynaFactory.getInstance().getXynaMultiChannelPortal().saveMDM(xml, revision, new EmptyRepositoryEvent());
      } catch (XynaException e){
        logger.warn("Could not copy " + xmlfile.getPath() + " to saved", e);
      }
    }
    
    //xmls auf aktuelle XMOM Version updaten
    //TODO f�r Performence-Verbesserung beim Einchecken die xml-Version merken (z.B. in componentes.xml)
    try {
      Updater.getInstance().updateMdm(revision);
    }
    catch (XPRC_GENERAL_UPDATE_ERROR e) {
      logger.warn("could not update mdm", e);
    }
  }
  
  
  /**
   * Liest alle jars aus dem binaries-Verzeichnis und kopiert sie ins saved-Verzeichnis
   */
  private void copyJarsToSavedDir() {
    for (Component binary : binaries) {
      String sourceDir = getLocalRepository(binary);
      String targetDir = binary.getSavedDir(revision);
      
      copyFiles(sourceDir, sourceDir, targetDir, new JarFileFilter());
    }
  }
  
  
  /**
   * Kopiert die Applications aus configuration in den Workspace, deployt die darin enthaltenen Objekte
   * und wendet die Konfigurationen an
   * @param includeXynaProperties
   * @param includeCapacities
   */
  private void copyApplications(boolean includeXynaProperties, boolean includeCapacities, boolean deploy) {
    List<File> applicationXmls = new ArrayList<File>();
    String sourceDir = getLocalRepository(Component.configuration);
    FileUtils.findFilesRecursively(new File(sourceDir), applicationXmls, new XmlFilenameFilter());
    
    ApplicationManagementImpl applicationManagement =
                    (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

    for (File applicationFile : applicationXmls) {
      if (applicationFile.getName().startsWith(APPLICATION_CONFIGURATION_PREFIX)) {
        try {
          ApplicationXmlEntry applicationXml = applicationManagement.parseApplicationXml(applicationFile);
          applicationManagement.saveApplication(applicationXml, revision, includeXynaProperties, includeCapacities);
          if (deploy) {
            applicationManagement.deployApplication(applicationXml, revision, "XMOM Access: Copy Applications");
          }
        } catch (XynaException e) {
          logger.warn("Could not copy application", e);
        }
      }
    }
  }
  
  
  private static class TransactionTask {
    private List<String> toAdd; //Pfade im Repository f�r Dateien, die hinzugef�gt werden sollen
    private List<String> toDelete; //Pfade im Repository f�r Dateien, die gel�scht werden sollen
    private StringBuilder commitMsg;
    
    private TransactionTask() {
      this.toAdd =  new ArrayList<String>();
      this.toDelete =  new ArrayList<String>();
      this.commitMsg =  new StringBuilder();
    }
    
    public void add(String toAdd) {
      this.toAdd.add(toAdd);
    }
    
    public void delete(String toDelete) {
      this.toDelete.add(toDelete);
    }
    
    public void appendCommitMsg(String message) {
      commitMsg.append(message);
    }
    
    public List<String> getToAdd() {
      return toAdd;
    }
    
    public List<String> getToDelete() {
      return toDelete;
    }
    
    public String getCommitMsg() {
      return commitMsg.toString();
    }
  }
  
  
  public void handleProjectEvents(Collection<? extends ProjectCreationOrChangeEvent> events, String commitMsg) {
    TransactionTask transactionTask = new TransactionTask();

    //�nderungen f�r alle Events im lokalen Repository durchf�hren
    for (ProjectCreationOrChangeEvent event : events) {
      switch (event.getType()) {
        case XMOM_DELETE:
        case XMOM_MODIFICATION :
        case XMOM_MOVE:
          changeXMOM((XMOMChangeEvent) event, transactionTask);
          break;
        case APPLICATION_DEFINE : 
        case APPLICATION_DEFINITION_CHANGE : 
          changeApplicationConfiguration(((BasicProjectCreationOrChangeEvent)event).getObjectIdentifier(), event.getType() == EventType.APPLICATION_DEFINE , true, transactionTask);
          break;
        case APPLICATION_REMOVE : 
          deleteApplicationConfiguration(((BasicProjectCreationOrChangeEvent)event).getObjectIdentifier(), transactionTask);
          break;
        case TRIGGER_ADD :
        case TRIGGER_REMOVE :
          TriggerChangeEvent triggerEvent = (TriggerChangeEvent)event;
          changeTrigger(triggerEvent.getObjectIdentifier(), triggerEvent.getSimpleFqClassName(), event.getType() == EventType.TRIGGER_REMOVE, transactionTask);
          break;
        case FILTER_ADD :
        case FILTER_REMOVE :
          FilterChangeEvent filterEvent = (FilterChangeEvent)event;
          changeFilter(filterEvent.getObjectIdentifier(), filterEvent.getSimpleFqClassName(), event.getType() == EventType.FILTER_REMOVE, transactionTask);
          break;
        case SERVICE_DEPLOY :
          changeServiceGroup(((BasicProjectCreationOrChangeEvent)event).getObjectIdentifier(), transactionTask, false);
          break;
        case SHAREDLIB_DEPLOY :
        case SHAREDLIB_REMOVE :
          changeSharedLib(((BasicProjectCreationOrChangeEvent)event).getObjectIdentifier(), event.getType() == EventType.SHAREDLIB_REMOVE, transactionTask);
          break;
        default :
          break;
      }
    }
    
    //�nderunen einchecken
    commit(transactionTask, commitMsg);
  }

  
  /**
   * Kopiert jars aus dem saved-Verzeichnis ins entsprechende binaries-Verzeichnis
   * im lokalen Repository
   * @param component
   * @param specificFolder
   */
  public void copyJarsToLocalRepository(Component component, String specificFolder){
    String baseSourceDir = component.getSavedDir(revision);
    String sourceDir = component.getSavedDir(revision, specificFolder);
    String targetDir = getLocalRepository(component);

    copyFiles(sourceDir, baseSourceDir, targetDir, new JarFileFilter());
  }
  
  /**
   * Kopiert alle Files aus sourceDir (die dem fileFilter entsprechen) ins targetDir,
   * relativ zum in relativeDir angegebenen pfad 
   * @param sourceDir
   * @param relativeDir
   * @param targetDir
   * @param fileFilter
   */
  private void copyFiles(String sourceDir, String relativeDir, String targetDir, FilenameFilter fileFilter) {
    ArrayList<File> sourceFiles = new ArrayList<File>();
    FileUtils.findFilesRecursively(new File(sourceDir), sourceFiles, fileFilter);
    
    try {
      FileUtils.copyFiles(sourceFiles, new File(relativeDir), new File(targetDir));
    } catch (Ex_FileAccessException e) {
      logger.warn("Could not copy files from " + sourceDir + " to " + targetDir, e);
    } catch (IOException e) {
      logger.warn("Could not copy files from " + sourceDir + " to " + targetDir, e);
    }
  }
  
  
  /**
   * Kopiert ein File aus dem saved-Verzeichnis ins lokale Repository
   * @param component
   * @param specificFile
   * @param transactionTask
   */
  public void copyFileToLocalRepository(Component component, String specificFile, TransactionTask transactionTask){
    String baseSourceDir = component.getSavedDir(revision);
    String sourceFile = component.getSavedDir(revision, specificFile);
    String targetDir = getLocalRepository(component);
    
    ArrayList<File> sourceFiles = new ArrayList<File>();
    sourceFiles.add(new File(sourceFile));
    
    try {
      FileUtils.copyFiles(sourceFiles, new File(baseSourceDir), new File(targetDir));
      String relativePath = FileUtils.getRelativePath(baseSourceDir, sourceFile);
      transactionTask.add(component.getRepositorySubFolder(relativePath));
    } catch (Ex_FileAccessException e) {
      logger.warn("Could not copy file " + sourceFile + " to " + targetDir, e);
    } catch (IOException e) {
      logger.warn("Could not copy file " + sourceFile + " to " + targetDir, e);
    }
  }
  
  
  
  /**
   * Erstellt, �ndert und l�scht die XMOM xmls im lokalen Repository
   * @param event
   * @param transactionTask
   */
  private void changeXMOM(XMOMChangeEvent event, TransactionTask transactionTask) {
    StringBuilder commitMsg = new StringBuilder();
    commitMsg.append("XMOM '");
    String fqXmlName = event.getObjectIdentifier();
    String fileName = convertFqNameToFileName(fqXmlName);
    
    switch(event.getType()) {
      case XMOM_MODIFICATION :
        //xmls ins lokale Repository kopieren
        copyFileToLocalRepository(Component.xmom, fileName, transactionTask);
        
        //service jars hinzuf�gen (falls nicht im CodeAccess)
        if (event.getXMOMType() != null && event.getXMOMType().equals(XMOMType.DATATYPE)) {
          changeServiceGroup(fqXmlName, transactionTask, false);
        }
        
        commitMsg.append(fqXmlName).append("' created or modified.");
        break;
      case XMOM_MOVE:
        //xmls ins lokale Repository kopieren
        copyFileToLocalRepository(Component.xmom, fileName, transactionTask);
        
        //neue service jars hinzuf�gen (falls nicht im CodeAccess)
        if (event.getXMOMType() != null && event.getXMOMType().equals(XMOMType.DATATYPE)) {
          changeServiceGroup(fqXmlName, transactionTask, false);
        }
        
        //zu l�schende xmls und service jars bestimmen
        String oldFqXmlName = ((XMOMMovementEvent) event).getOldFqName();
        String oldFileName = convertFqNameToFileName(oldFqXmlName);
        transactionTask.delete(Component.xmom.getRepositorySubFolder(oldFileName));
        
        if (event.getXMOMType() != null && event.getXMOMType().equals(XMOMType.DATATYPE)) {
          changeServiceGroup(oldFqXmlName, transactionTask, true);
        }
        
        commitMsg.append(oldFqXmlName).append("' moved to ").append(fqXmlName).append(".");
        break;
      case XMOM_DELETE:
        //zu l�schende xmls und service jars bestimmen
        transactionTask.delete(Component.xmom.getRepositorySubFolder(fileName));
        
        if (event.getXMOMType() != null && event.getXMOMType().equals(XMOMType.DATATYPE)) {
          changeServiceGroup(fqXmlName, transactionTask, true);
        }
        
        commitMsg.append(fqXmlName).append("' removed.");
        break;
      default :
        break;
    }
    commitMsg.append(Constants.LINE_SEPARATOR);
    
    transactionTask.appendCommitMsg(commitMsg.toString());
  }
  
  
  private String convertFqNameToFileName(String fqXmlName) {
    return fqXmlName.replaceAll("\\.", Constants.fileSeparator) + ".xml";
  }


  /**
   * Speichert die aktuelle Konfiguration einer Application
   * @param applicationName
   * @param ignoreExceptions
   * @param transactionTask
   */
  private void changeApplicationConfiguration(String applicationName, boolean firstCreation, boolean ignoreExceptions, TransactionTask transactionTask) {
    if (firstCreation) {
      String appConfig = getAbsoluteAppConfigFileName(applicationName);
      if (new File(appConfig).exists()) {
        //FIXME einen eindeutigen Namen f�r das xml generieren, statt abzubrechen
        throw new RuntimeException("Application configuration xml already exists");
      }
    }
    
    try {
      ApplicationManagementImpl applicationManagement =
                      (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
      ApplicationXmlEntry applicationXmlEntry = applicationManagement.createApplicationXmlEntry(applicationName, null, null, revision, ignoreExceptions, false);
      transactionTask.appendCommitMsg("application definition '" + applicationName + "' " + (firstCreation ? "created" : "modified") + "\n");
      saveApplicationConfiguration(applicationName, applicationXmlEntry, transactionTask);
    } catch (XynaException e) {
      logger.warn("Could not change configuration xml of application " + applicationName, e);
    }
  }
  
  private void saveApplicationConfiguration(String applicationName, ApplicationXmlEntry applicationXmlEntry, TransactionTask transactionTask) {
    Document doc;
    try {
      applicationXmlEntry.setFactoryVersion();
      doc = applicationXmlEntry.buildXmlDocument();
    } catch (ParserConfigurationException e) {
      logger.warn("Could not parse configuration of application " + applicationName, e);
      return;
    } catch (XPRC_VERSION_DETECTION_PROBLEM e) {
      logger.warn("Could not adjust configuration xml of application " + applicationName, e);
      return;
    } catch (PersistenceLayerException e) {
      logger.warn("Could not adjust configuration xml of application " + applicationName, e);
      return;
    }
    
    String appConfig = getAbsoluteAppConfigFileName(applicationName);
    File file = new File(appConfig);
    try {
      XMLUtils.saveDom(file, doc);
    } catch (Ex_FileAccessException e) {
      logger.warn("Could not save configuration xml of application " + applicationName, e);
    }
    
    transactionTask.add(getRelativeAppConfigFileName(applicationName));
  }
  
  public void saveAndCommitApplicationConfiguration(String applicationName, ApplicationXmlEntry applicationXmlEntry, String commitMsg) {
    TransactionTask transactionTask = new TransactionTask();
    saveApplicationConfiguration(applicationName, applicationXmlEntry, transactionTask);
    commit(transactionTask, commitMsg);
  }
  
  
  /**
   * L�scht das Application_<applicationName>.xml
   * @param applicationName
   */
  private void deleteApplicationConfiguration(String applicationName, TransactionTask transactionTask) {
    transactionTask.appendCommitMsg("application definition '" + applicationName + "' removed.\n");
    transactionTask.delete(getRelativeAppConfigFileName(applicationName));
  }
  
  private String getAppConfigFileName(String applicationName) {
    return APPLICATION_CONFIGURATION_PREFIX + applicationName.replaceAll("[^a-zA-Z0-9_]", "_") + ".xml";
  }
  
  public String getAbsoluteAppConfigFileName(String applicationName) {
    return getLocalRepository(Component.configuration) + Constants.fileSeparator + getAppConfigFileName(applicationName);
  }

  public String getRelativeAppConfigFileName(String applicationName) {
    return Component.configuration.repositorySubFolder + Constants.fileSeparator + getAppConfigFileName(applicationName);
  }
  
  
  /**
   * Legt einen neuen Branch an
   * @param branchName
   * @param commitMsg
   * @return
   */
  public String createBranch(String branchName, String commitMsg) {
    boolean success = false;
    String result;
    RepositoryTransaction transaction = repositoryAccess.beginTransaction(getIdentityString());
    try {
      result = transaction.createBranch(new String[] {""}, branchName, commitMsg);
      success = true;
      //TODO fehlerbehandlung verbessern
    } catch (XDEV_RepositoryAccessException e) {
      throw new RuntimeException(e);
    } finally { 
      endTransaction(transaction, !success);
    }
    
    return result;
  }
  
  /**
   * Listet alle vorhanden Branches auf.
   */
  public List<String> listBranches() throws XDEV_PathNotFoundException, XDEV_RepositoryAccessException {
    RepositoryTransaction transaction = repositoryAccess.beginTransaction(getIdentityString());
    try {
      return transaction.listBranches();
    } finally { 
      endTransaction(transaction, false);
    }
  }
  
  /**
   * Commitet die �nderungen aus transactionTask
   * @param transactionTask
   * @param commitMsg falls nicht gesetzt, wird die commitMsg aus dem TransactionTask verwendet
   */
  private void commit(TransactionTask transactionTask, String commitMsg) {
    boolean success = false;
    RepositoryTransaction repositoryTransaction = repositoryAccess.beginTransaction(getIdentityString());
    try {
      repositoryTransaction.setTransactionProperty("force", true);
      Set<String> toCommit = new HashSet<String>();
      
      //Lock, damit nicht ein anderer Thread die gerade geaddeten oder gel�schten Files commitet
      repositoryLock.lock();
      try {
        if (transactionTask.getToAdd().size() > 0) {
          toCommit.addAll(transactionTask.getToAdd());
          
          //die noch nicht versionierten Files (und ihre unversionierten parents) m�ssen geaddet werden
          List<String> unversioned = getUnversionedFiles(repositoryTransaction, transactionTask.getToAdd());
          if (unversioned.size() > 0) {
            List<RepositoryItemModification> modified = repositoryTransaction.add(unversioned.toArray(new String[unversioned.size()]), RecursionDepth.FULL_RECURSION);
            toCommit.addAll(getRelativePaths(modified));
          }
        }
        if (transactionTask.getToDelete().size() > 0) {
          //�berpr�fen, ob File noch existiert (k�nnte evtl. inzwischen gel�scht worden sein)
          Iterator<String> it = transactionTask.getToDelete().iterator();
          while(it.hasNext()) {
            File f = new File(repositoryAccess.getLocalRepository() + Constants.fileSeparator + it.next());
            if (!f.exists()) {
              it.remove();
            }
          }
          if (transactionTask.getToDelete().size() > 0) {
            toCommit.addAll(transactionTask.getToDelete());
            repositoryTransaction.delete(transactionTask.getToDelete().toArray(new String[transactionTask.getToDelete().size()]));
          }
        }

        if (toCommit.size() > 0) {
          if (commitMsg == null) {
            commitMsg = transactionTask.getCommitMsg();
          }
          repositoryTransaction.commit(toCommit.toArray(new String[toCommit.size()]), commitMsg, RecursionDepth.FULL_RECURSION);
        }
      }
      finally {
        repositoryLock.unlock();
      }
      success = true;
      //TODO fehlerbehandlung verbessern
    } catch (XDEV_RepositoryAccessException e) {
      logger.warn("Failed to commit transaction", e);
    } finally { 
      endTransaction(repositoryTransaction, !success);
    }
  }

  private List<String> getUnversionedFiles(RepositoryTransaction repositoryTransaction, List<String> files) {
    List<String> unversioned = new ArrayList<String>();
    for (String file : files) {
      try {
        for (RepositoryItemModification status : repositoryTransaction.status(new String[] {file})) {
          if (status.getModification().equals(RepositoryModificationType.Unversioned)) {
            unversioned.add(file);
          }
        }
      } catch (XDEV_RepositoryAccessException e) {
        //svn status schl�gt fehl, wenn das Parent-Verzeichnis noch nicht versioniert ist
        unversioned.add(file);
      }
    }
    
    return unversioned;
  }
  
  private Set<String> getRelativePaths(List<RepositoryItemModification> modified) {
    Set<String> relativePaths = new HashSet<String>();
    for (RepositoryItemModification mod : modified) {
      relativePaths.add(mod.getRelativePath(repositoryAccess.getLocalRepository()));
    }
    
    return relativePaths;
  }
  
  
  private void changeSharedLib(String sharedLibName, boolean delete, TransactionTask transactionTask) {
    String modifiedDir = Component.sharedlibs.getRepositorySubFolder(sharedLibName);
    
    if (delete || alreadyInCodeAccess(sharedLibName, ComponentType.SHARED_LIB)) {
      transactionTask.delete(modifiedDir);
      transactionTask.appendCommitMsg("sharedLib " + sharedLibName + " removed");
      if (!delete) {
        transactionTask.appendCommitMsg(", because already in code access");
      }
    } else {
      copyJarsToLocalRepository(Component.sharedlibs, sharedLibName);
      transactionTask.add(modifiedDir);
      transactionTask.appendCommitMsg("sharedLib " + sharedLibName + " created");
    }
    transactionTask.appendCommitMsg("\n");
  }
  

  private void changeTrigger(String triggerName, String triggerSimpleClassName, boolean delete, TransactionTask transactionTask) {
    String modifiedDir = Component.trigger.getRepositorySubFolder(triggerSimpleClassName);
    
    if (delete || alreadyInCodeAccess(triggerName, ComponentType.TRIGGER)) {
      transactionTask.delete(modifiedDir);
      transactionTask.appendCommitMsg("trigger " + triggerName + " removed");
      if (!delete) {
        transactionTask.appendCommitMsg(", because already in code access");
      }
    } else {
      copyJarsToLocalRepository(Component.trigger, triggerSimpleClassName);
      transactionTask.add(modifiedDir);
      transactionTask.appendCommitMsg("trigger " + triggerName + " created");
      //TODO Trigger in componentes.xml eintragen
    }
    transactionTask.appendCommitMsg("\n");
  }
  
  
  private void changeFilter(String filterName, String filterSimpleClassName, boolean delete, TransactionTask transactionTask) {
    String modifiedDir = Component.filter.getRepositorySubFolder(filterSimpleClassName);
    
    if (delete || alreadyInCodeAccess(filterName, ComponentType.FILTER)) {
      transactionTask.delete(modifiedDir);
      transactionTask.appendCommitMsg("filter " + filterName + " removed");
      if (!delete) {
        transactionTask.appendCommitMsg(", because already in code access");
      }
    } else {
      copyJarsToLocalRepository(Component.filter, filterSimpleClassName);
      transactionTask.add(modifiedDir);
      transactionTask.appendCommitMsg("filter " + filterName + " created");
      //TODO Filter in componentes.xml eintragen
    }
    transactionTask.appendCommitMsg("\n");
  }
  
  
  private void changeServiceGroup(String fqXmlName, TransactionTask transactionTask, boolean delete) {
    String jarDir = Component.servicegroups.getRepositorySubFolder(fqXmlName);
    File file = new File(repositoryAccess.getLocalRepository() + Constants.fileSeparator + jarDir);

    if (delete || alreadyInCodeAccess(fqXmlName, ComponentType.CODED_SERVICE)) {
      if (file.exists()) {
        transactionTask.delete(jarDir);
        transactionTask.appendCommitMsg("service " + fqXmlName + " removed");
        if (!delete) {
          transactionTask.appendCommitMsg(", because already in code access");
        }
      }
    } else {
      copyJarsToLocalRepository(Component.servicegroups, fqXmlName);
      if (file.exists()) {
        transactionTask.add(jarDir);
        transactionTask.appendCommitMsg("service " + fqXmlName + " created");
      }
    }
    transactionTask.appendCommitMsg("\n");
  }

  /**
   * Bereits im CodeAccess?
   * @param componentName
   * @param componentType
   */
  public boolean alreadyInCodeAccess(String componentName, ComponentType componentType) {
    CodeAccess ca =  XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getCodeAccessManagement()
                    .getCodeAccessInstance(revision);
    if (ca == null) {
      return false;
    }

    return ca.componentAlreadyExists(componentName, componentType);
  }
  
  
  private String getLocalRepository(Component component) {
    return repositoryAccess.getLocalRepository() + Constants.fileSeparator + component.getRepositorySubFolder();
  }
  
  
  private String getIdentityString() {
    Identity identity = XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.get();
    if (identity == null) {
      return "CLI access";
    } else {
      return identity.toString();
    }
  }
  
  
  private void endTransaction(RepositoryTransaction transaction, boolean rollback) {
    try {
      if (rollback) {
        try {
          transaction.rollback();
        } catch (XDEV_RepositoryAccessException e) {
          throw new RuntimeException(e);
        }
      }
    } finally {
      try {
        transaction.endTransaction();
      } catch (XDEV_RepositoryAccessException e) {
        logger.warn("Failed to end transaction", e);
      }
    }
  }
  
  
  public String getName() {
    return name;
  }
  
  
  public Long getRevision() {
    return revision;
  }
  
  public RepositoryAccess getRepositoryAccess() {
    return repositoryAccess;
  }
  
  @Override
  public String toString() {
    return name + " repositoryAccess=" + repositoryAccess.getName(); 
  }
}
