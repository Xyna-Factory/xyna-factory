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
package com.gip.xyna.xprc.xfractwfe.generation.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPortal;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.BatchRepositoryEvent;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.RuntimeContextRequirementXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XMOMXmlEntry;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.Service;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class XmomGenerator {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(Service.class);
  
  private final static String TEMPORARY_SAVE_USER_NAME = "XmomGeneration";
  private final static String TEMPORARY_SAVE_USER_ROLE = UserManagement.ADMIN_ROLE_NAME;
  private final static String TEMPORARY_SAVE_USER_PASSWORD = "aAbB12%&-#+34xXzZ";
  
  private final Map<String, Datatype> generationContext;
  private final Map<String, ExceptionType> generationContextExceptions;
  private final Long revision;
  private final DeploymentMode deploymentMode;
  private final WorkflowProtectionMode wpm;
  private final boolean overwrite;
  private final boolean inheritCodeChanged;
  private boolean saveSilently;
  private String username;
  private String password;
  private SessionCredentials creds;
  
  private XmomGenerator(Long revision, boolean overwrite, DeploymentMode deploymentMode, WorkflowProtectionMode wpm, boolean inheritCodeChanged) {
    this.generationContext = new HashMap<String, Datatype>();
    this.generationContextExceptions = new HashMap<String, ExceptionType>();
    this.revision = revision;
    this.deploymentMode = deploymentMode;
    this.wpm = wpm;
    this.overwrite = overwrite;
    this.inheritCodeChanged = inheritCodeChanged;
    saveSilently = !XynaFactory.isFactoryServer();
    username = TEMPORARY_SAVE_USER_NAME + revision;
    password = TEMPORARY_SAVE_USER_PASSWORD;
  }
  
  public void saveSilently(boolean value) {
    if (XynaFactory.isFactoryServer()) {
      saveSilently = value;
    } else {
      // we can't propagate if not inside factory
      saveSilently = false;
    }
  }
  
  public void setCredentials(String username, String password) {
    this.username = username;
    this.password = password;
  }
  
  public void setCredentials(String username, SessionCredentials creds) {
    this.username = username;
    this.creds = creds;
  }
  
  public void add(Datatype datatype) {
    generationContext.put(datatype.type.getFQTypeName(), datatype);
  }
  public void add(ExceptionType exceptionType) {
    generationContextExceptions.put(exceptionType.getFQTypeName(), exceptionType);
  }
  
  public Set<String> getAllFqNames() {
    return Collections.unmodifiableSet(generationContext.keySet());
  }
  public Collection<Datatype> getDatatypes() {
    return Collections.unmodifiableCollection(generationContext.values());
  }

  public boolean exists(XmomType type) {
    File location = new File(GenerationBase.getFileLocationOfXmlNameForSaving(type.getFQTypeName(), revision)+".xml");
    return location.exists();
  }
  
  public boolean exists(Datatype datatype) {
    return exists(datatype.type);
  }

  public void save() throws Ex_FileWriteException {
    SessionCredentials creds = initiateSave();
    BatchRepositoryEvent repositoryEvent = new BatchRepositoryEvent(revision);
    try {
      for (Datatype datatype : generationContext.values()) {
        save(datatype.getFQTypeName(), datatype.toXML(), creds, repositoryEvent);
      }
      for (ExceptionType exceptionType : generationContextExceptions.values()) {
        save(exceptionType.getFQTypeName(), exceptionType.toXML(), creds, repositoryEvent);
      }
    } finally {
      try {
        repositoryEvent.execute("Created xmom objects.");
        tearDownSaveSession(creds);
      } catch (Exception e) {
        // TODO at least log it
      }
    }
  }
  
  private File save(String fQTypeName, String xml, SessionCredentials creds, BatchRepositoryEvent repositoryEvent) throws Ex_FileWriteException {
    File location = new File(GenerationBase.getFileLocationOfXmlNameForSaving(fQTypeName, revision)+".xml");
    if (location.exists()) {
      if (overwrite) {
        location.delete();
      } else {
        throw new Ex_FileWriteException(location.getPath(), new RuntimeException("File does already exist."));
      }
    }
    if (XynaFactory.isFactoryServer()) {
      try {
        if (creds == null || saveSilently) {
          XynaFactory.getInstance().getXynaMultiChannelPortal().saveMDM(xml, revision, repositoryEvent);          
        } else {
          XynaFactory.getInstance().getXynaMultiChannelPortalSecurityLayer().saveMDM(xml, true, username, creds.getSessionId(), revision, repositoryEvent);
        }
      } catch (XynaException e) {
        FileUtils.writeStringToFile(xml,location);
      } finally {
        if (creds != null) {
          
        }
      }
    } else {
      FileUtils.writeStringToFile(xml,location);
    }
    return location;
  }
  
  public List<Datatype> listAlreadyExistingDatatypes() {
    List<Datatype> existing = new ArrayList<Datatype>();
    for (Datatype datatype : generationContext.values()) {
      File location = new File(GenerationBase.getFileLocationOfXmlNameForSaving(datatype.type.getFQTypeName(), revision)+".xml");
      if (location.exists()) {
        existing.add(datatype);
      }
    }
    return existing;
  }
  
  
  public void deploy() 
    throws MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
    EnumMap<XMOMType,List<String>> deploymentItems = new EnumMap<XMOMType,List<String>>(XMOMType.class);
    deploymentItems.put(XMOMType.DATATYPE, new ArrayList<String>(generationContext.keySet()) );
    deploymentItems.put(XMOMType.EXCEPTION, new ArrayList<String>(generationContextExceptions.keySet()) );
    
    try {
      GenerationBase.deploy(deploymentItems, deploymentMode, inheritCodeChanged, wpm, revision, "Generated XMOMs");
    } catch (MDMParallelDeploymentException e) {
      e.generateSerializableFailedObjects();
      throw e;
    }
  }
  
  public File buildApplication(String application, String version, List<RuntimeContextRequirementXmlEntry> requirements) throws XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException, IOException, Ex_FileAccessException, ParserConfigurationException {
    return buildApplication(application, version, "", requirements);
  }
  
  public File buildApplication(String application, String version, String comment, List<RuntimeContextRequirementXmlEntry> requirements) throws XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException, IOException, Ex_FileAccessException, ParserConfigurationException {
    ApplicationXmlEntry applicationXml = new ApplicationXmlEntry(application, version, comment);
    applicationXml.setFactoryVersion();
    for (String datatypeName : generationContext.keySet()) {
      applicationXml.getXmomEntries().add(new XMOMXmlEntry(false, datatypeName, ApplicationEntryType.DATATYPE.toString()));
    }
    for (String datatypeName : generationContextExceptions.keySet()) {
      applicationXml.getXmomEntries().add(new XMOMXmlEntry(false, datatypeName, ApplicationEntryType.EXCEPTION.toString()));
    }
    
    for (RuntimeContextRequirementXmlEntry rcrxe : requirements) {
      applicationXml.getRuntimeContextRequirements().add(rcrxe);
    }
    
    File tmpFile = File.createTempFile("app_", ".zip");
    File tmpDir = new File(tmpFile.getAbsolutePath() + "_build");
    
    writeXmomToFolder(tmpDir);

    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmpFile));
    try {
      FileUtils.zipDir(tmpDir, zos, tmpDir);

      ZipEntry ze = new ZipEntry(ApplicationManagementImpl.XML_APPLICATION_FILENAME);

      zos.putNextEntry(ze);
      Document doc = applicationXml.buildXmlDocument();
      XMLUtils.saveDomToOutputStream(zos, doc);

      zos.flush();
    } finally {
      zos.close();
    }

    FileUtils.deleteDirectoryRecursively(tmpDir);
    
    return tmpFile;
  }
  
  
  private void writeXmomToFolder(File rootFolder) throws Ex_FileWriteException {
    for (Entry<String, Datatype> datatypeEntry : generationContext.entrySet()) {
      String path = new StringBuilder(rootFolder.getAbsolutePath()).append(Constants.fileSeparator).append(Constants.SUBDIR_XMOM).append(Constants.fileSeparator).append(datatypeEntry.getKey().replaceAll("\\.", Constants.fileSeparator)).append(".xml").toString();
      FileUtils.writeStringToFile(datatypeEntry.getValue().toXML(), new File(path));
    }
    for (Entry<String, ExceptionType> exceptionEntry : generationContextExceptions.entrySet()) {
      String path = new StringBuilder(rootFolder.getAbsolutePath()).append(Constants.fileSeparator).append(Constants.SUBDIR_XMOM).append(Constants.fileSeparator).append(exceptionEntry.getKey().replaceAll("\\.", Constants.fileSeparator)).append(".xml").toString();
      FileUtils.writeStringToFile(exceptionEntry.getValue().toXML(), new File(path));
    }
  }

  
  @Deprecated
  public static XmomGenerator with(Long revision) {
    return with(revision, true);
  }

  
  @Deprecated
  public static XmomGenerator with(Long revision, boolean override) {
    return with(revision, override, DeploymentMode.codeChanged, false);
  }
  
  
  @Deprecated
  public static XmomGenerator with(Long revision, boolean override, DeploymentMode deploymentMode, boolean inheritCodeChanged) {
    return with(revision, override, deploymentMode, inheritCodeChanged, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES);
  }

  @Deprecated
  public static XmomGenerator with(Long revision, boolean override, DeploymentMode deploymentMode, boolean inheritCodeChanged, WorkflowProtectionMode wpm) {
    return new XmomGenerator(revision, override, deploymentMode, wpm, inheritCodeChanged);
  }
  
  public static XmomGeneratorBuilder inWorkspace(Workspace workspace) throws XFMG_NoSuchRevision {
    return new XmomGeneratorBuilder( workspace );
  }
  public static XmomGeneratorBuilder inRevision(long revision) throws XFMG_NoSuchRevision {
    return new XmomGeneratorBuilder( revision );
  }
  
  private SessionCredentials initiateSave() {
    CommandControl.tryLock(CommandControl.Operation.XMOM_SAVE, revision);
    boolean needToUnlock = true;
    if (creds != null) {
      return creds;
    }
    boolean usingTempUser = username.equals(TEMPORARY_SAVE_USER_NAME + revision) && password.equals(TEMPORARY_SAVE_USER_PASSWORD);
    XynaFactoryPortal factory = XynaFactory.getPortalInstance();
    try {
      if (usingTempUser) {
        if (!factory.getXynaMultiChannelPortalPortal()
                    .createUser(username, TEMPORARY_SAVE_USER_ROLE, TEMPORARY_SAVE_USER_PASSWORD, false)) {
          return null;
        }
        usingTempUser = true;
      }
      User user = factory.getFactoryManagementPortal().authenticate(username, password);
      SessionCredentials creds = factory.getXynaMultiChannelPortalSecurityLayer().getNewSession(user, true);
      needToUnlock = false;
      return creds;
    } catch (Exception e) {
      return null;
    } finally {
      if (needToUnlock) {
        CommandControl.unlock(CommandControl.Operation.XMOM_SAVE, revision);
        if (usingTempUser) {
          try {
            factory.getXynaMultiChannelPortalPortal().deleteUser(username);
          } catch (Exception e) {
            logger.error(e);
          }
        }
      }
    }
  }
  
  private void tearDownSaveSession(SessionCredentials creds) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException {
    CommandControl.unlock(CommandControl.Operation.XMOM_SAVE, revision);
    if (this.creds == null) {
      if (creds != null) {
        XynaFactoryPortal factory = XynaFactory.getPortalInstance();
        try {
          if (creds != null) {
            factory.getXynaMultiChannelPortalSecurityLayer().quitSession(creds.getSessionId());
          }
        } finally {
          boolean usingTempUser = username.equals(TEMPORARY_SAVE_USER_NAME + revision) && password.equals(TEMPORARY_SAVE_USER_PASSWORD);
          if (usingTempUser) {
            factory.getXynaMultiChannelPortalPortal().deleteUser(username);
          }
        }
      }
    }
  }
  
  public static class XmomGeneratorBuilder {
    private long revision;
    private boolean overwrite = false;
    boolean inheritCodeChanged = false;
    private DeploymentMode deploymentMode = DeploymentMode.codeChanged;
    private WorkflowProtectionMode workflowProtectionMode = WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES;
    //Au�erhalb der Factory (z.B. JUnit-Tests) wirft getRevisionManagement() eine NPE, 
    //mit dieser useDefaultRevisionForTests=true wird dies umgangen, stattdessen wird als Revision -1 verwendet
    public static boolean useDefaultRevisionForTests = false;
    
    public XmomGeneratorBuilder(Workspace workspace) throws XFMG_NoSuchRevision {
      this.revision = revisionFor(workspace, 0L);
    }
    public XmomGeneratorBuilder(long revision) throws XFMG_NoSuchRevision {
      this.revision = revisionFor(null, revision);
    }
    private long revisionFor(Workspace workspace, long revision) throws XFMG_NoSuchRevision {
      if( useDefaultRevisionForTests ) {
        return -1L;
      } else {
        RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        try {
          if( workspace == null ) {
            rm.getRuntimeContext(revision);
            return revision;
          } else {
            return rm.getRevision(workspace);
          }
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          String id = workspace == null ? String.valueOf(revision) : workspace.getName();
          throw new XFMG_NoSuchRevision(id, e);
        }
      }
    }
    public XmomGeneratorBuilder overwrite(boolean overwrite) {
      this.overwrite = overwrite;
      return this;
    }
    public XmomGeneratorBuilder inheritCodeChanged(boolean inheritCodeChanged) {
      this.inheritCodeChanged = inheritCodeChanged;
      return this;
    }
    public XmomGeneratorBuilder deploymentMode(DeploymentMode deploymentMode) {
      this.deploymentMode = deploymentMode;
      return this;
    }
    public XmomGeneratorBuilder workflowProtectionMode(WorkflowProtectionMode workflowProtectionMode) {
      this.workflowProtectionMode = workflowProtectionMode;
      return this;
    }
    public XmomGenerator build() {
      return new XmomGenerator(revision, overwrite, deploymentMode, workflowProtectionMode, inheritCodeChanged);
    }
  }


}
