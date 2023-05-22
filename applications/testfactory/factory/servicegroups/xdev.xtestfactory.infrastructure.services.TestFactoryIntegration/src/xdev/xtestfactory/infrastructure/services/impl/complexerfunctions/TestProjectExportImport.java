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

package xdev.xtestfactory.infrastructure.services.impl.complexerfunctions;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.streams.StreamUtils;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildNewVersionForApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildNewWorkspace;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildWorkingSet;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotExportApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotImportApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveWorkspace;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateApplicationName;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateVersionForApplicationName;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToAddObjectToApplication;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState;
import com.gip.xyna.xfmg.xfctrl.appmgmt.BuildApplicationVersionParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.CopyApplicationIntoWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RemoveApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.ApplicationName;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.CreateWorkspaceResult;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.CreateWorkspaceResult.Result;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.RemoveWorkspaceParameters;
import com.gip.xyna.xmcp.PluginInformation;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidXMOMStorablePathException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_ODSNameChangedButNotDeployedException;
import com.gip.xyna.xnwh.exceptions.XNWH_ODSNameMustBeUniqueException;
import com.gip.xyna.xnwh.exceptions.XNWH_StorableNotFoundException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.DeleteParameter;
import com.gip.xyna.xnwh.persistence.xmom.DeleteParameter.BackwardReferenceHandling;
import com.gip.xyna.xnwh.persistence.xmom.DeleteParameter.ForwardReferenceHandling;
import com.gip.xyna.xnwh.persistence.xmom.IFormula;
import com.gip.xyna.xnwh.persistence.xmom.ODSRegistrationParameter;
import com.gip.xyna.xnwh.persistence.xmom.QueryParameter;
import com.gip.xyna.xnwh.persistence.xmom.SelectionMask;
import com.gip.xyna.xnwh.persistence.xmom.SortCriterion;
import com.gip.xyna.xnwh.persistence.xmom.StoreParameter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMapping;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMappingUtils;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XFMG_CouldNotModifyRuntimeContextDependenciesException;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import xdev.xtestfactory.infrastructure.datatypes.ManagedFileID;
import xdev.xtestfactory.infrastructure.datatypes.TestCaseInstance;
import xdev.xtestfactory.infrastructure.datatypes.TestObjectMapping;
import xdev.xtestfactory.infrastructure.datatypes.TestProjectCreationParameter;
import xdev.xtestfactory.infrastructure.datatypes.Workspacename;
import xdev.xtestfactory.infrastructure.exceptions.TestProjectNotUnique;
import xdev.xtestfactory.infrastructure.services.impl.TestFactoryIntegrationServiceOperationImpl;
import xdev.xtestfactory.infrastructure.storables.Feature;
import xdev.xtestfactory.infrastructure.storables.SystemUnderTest;
import xdev.xtestfactory.infrastructure.storables.SystemUnderTestInstance;
import xdev.xtestfactory.infrastructure.storables.TestCase;
import xdev.xtestfactory.infrastructure.storables.TestCaseChain;
import xdev.xtestfactory.infrastructure.storables.TestData;
import xdev.xtestfactory.infrastructure.storables.TestDataMetaData;
import xdev.xtestfactory.infrastructure.storables.TestObject;
import xdev.xtestfactory.infrastructure.storables.TestObjectMetaData;
import xdev.xtestfactory.infrastructure.storables.TestProject;
import xnwh.persistence.XMOMStorableAccessException;



public class TestProjectExportImport {

  private static Logger logger = CentralFactoryLogging.getLogger(TestProjectExportImport.class);

  private final static String TEST_DATA_EXPORT_XML_ZIP_ENTRY_PREFIX = "TestData";
  private final static String TEST_DATA_EXPORT_XML_ZIP_ENTRY_SUFFIX = ".xml";
  private final static String TEST_PROJECT_EXPORT_XML_ZIP_ENTRY_NAME = "TestProject.xml";
  private final static String TEST_OBJECT_DATA_EXPORT_XML_ZIP_ENTRY_NAME = "TestObjectData.xml";
  private final static String IMPORT_TEST_DATA_ORDERTYPE = "xdev.xtestfactory.infrastructure.actions.ImportTestData";
  private final static String IMPORT_TEST_OBJECT_DATA_ORDERTYPE = "xdev.xtestfactory.infrastructure.actions.ImportTestObjectData";
  private final static String GET_ALL_TEST_OBJECT_DATA_ORDERTYPE = "xdev.xtestfactory.infrastructure.util.GetAllTestObjects";

  private final static Pattern FILE_SYSTEM_ESCAPE_PATTERN = Pattern.compile("[^a-zA-Z0-9]");
  private final static Pattern WITHOUT_TRAILING_NUMBERS_PATTERN = Pattern.compile("(\\w*[a-zA-Z])[0-9]*");
  private final static IFormula EMPTY_FORMULA = new IFormula() {

    public List<Accessor> getValues() {
      return Collections.emptyList();
    }


    public String getFormula() {
      return null;
    }
  };


  // Testprojekt exportieren
  public static ManagedFileID exportTestproject(XynaOrderServerExtension correlatedXynaOrder, TestProject testProject,
                                                Workspacename source) {
    ManagedFileID resultid = new ManagedFileID();
    if (source == null || source.getWorkspacename() == null || source.getWorkspacename().length() <= 0) {
      source = new Workspacename(testProject.getWorkspaceReference());
    }
    File exportFile = buildApplicationFromTestProject(testProject, source);
    List<String> testDataExport = createTestDataExport(correlatedXynaOrder, testProject, source);
    String testObjectDataExport = createTestObjectDataExport(correlatedXynaOrder, testProject, source);
    TestProjectArchive archive = new TestProjectArchive(testProject.toXml(), testDataExport, testObjectDataExport, exportFile);
    try (InputStream exportStream = archive.getMergedInputStream()) {
      FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
      String fmid = fm.store("Testfactory", generateTestProjectExportFileName(testProject), exportStream);
      resultid.setID(fmid);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (resultid.getID() == null) {
      throw new RuntimeException("No File ManagementID given");
    }
    return resultid;
  }


  private static List<String> createTestDataExport(XynaOrderServerExtension correlatedXynaOrder,
                                                   TestProject testProject, Workspacename source) {
    List<String> exportCSVs = new ArrayList<>();
    for (TestDataMetaData testMeta : testProject.getTestDataMetaData()) {
      String testDataFqName = testMeta.getTestDataFullQualifiedStorableName();
      if (testDataFqName == null) {
        continue;
      }
      XMOMPersistenceManagement xmomPers =
          XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();
      try {
        Long parentRevision = TestFactoryIntegrationServiceOperationImpl.getRev(source);
        @SuppressWarnings("unchecked")
        List<TestData> testDataXOs =
            (List<TestData>) xmomPers.query(correlatedXynaOrder,
                                            new SelectionMask(testDataFqName, (List<String>) null), EMPTY_FORMULA,
                                            new QueryParameter(-1, false, new SortCriterion[0]), parentRevision);
        exportCSVs.add(new XynaObjectList<>(testDataXOs, GenerationBase.getSimpleNameFromFQName(testDataFqName),
                                            GenerationBase.getPackageNameFromFQName(testDataFqName)).toXml());
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
    }
    return exportCSVs;
  }
  
  private static String createTestObjectDataExport(XynaOrderServerExtension correlatedXynaOrder, TestProject testProject, Workspacename source) {
    Workspace workspace = new Workspace(testProject.getWorkspaceReference());
    
    try {
        XynaOrderCreationParameter xocp =
            new XynaOrderCreationParameter(new DestinationKey(GET_ALL_TEST_OBJECT_DATA_ORDERTYPE, workspace));
      @SuppressWarnings("unchecked")
      List<? extends TestObject> result = (List<? extends TestObject>) XynaFactory.getInstance().getXynaMultiChannelPortal().startOrderSynchronously(xocp);
      if (result.size() > 0) {
        return new XynaObjectList<>(result, GenerationBase.getSimpleNameFromFQName(testProject.getTestObjectType()),
                        GenerationBase.getPackageNameFromFQName(testProject.getTestObjectType())).toXml();  
      } else {
        return "";
      }
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }


  public static TestProject createWorkspace(TestProject testProject) {

    logger.debug("Creating Workspace for Testproject " + testProject.getName() + ":"
        + testProject.getWorkspaceReference());

    WorkspaceManagement workspaceManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();

    String originalWorkspaceRef = testProject.getWorkspaceReference();
    if (!testProjectReferenceIsUnique(testProject)) {
      int wsNameSuffix = 0;
      while (!testProjectReferenceIsUnique(testProject)) {
        wsNameSuffix++;
        testProject.setWorkspaceReference(originalWorkspaceRef + " " + wsNameSuffix);
      }
    }

    Workspace workspace = TestFactoryIntegrationServiceOperationImpl.getWorkspace(testProject);
    try {
      CreateWorkspaceResult result = workspaceManagement.createWorkspace(workspace);
      if (result.getResult() != Result.Success) {
        StringBuilder warnings = new StringBuilder();
        for (String s : result.getWarnings()) {
          warnings.append(s + ";");
        }
        throw new RuntimeException(warnings.toString());
      }
    } catch (XFMG_CouldNotBuildNewWorkspace e) {
      throw new RuntimeException(e);
    }

    return testProject;

  }


  private static boolean testProjectReferenceIsUnique(TestProject tp) {
    WorkspaceManagement workspaceManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    Map<Workspace, PluginInformation> workspaces = workspaceManagement.listWorkspaces();
    for (Entry<Workspace, PluginInformation> wsi: workspaces.entrySet()) {
      if (wsi.getKey().getName().equals(tp.getWorkspaceReference())) {
        return false;
      }
    }
    return true;
  }


  private static void importTestProject(File second, TestProject testProject) {

    String applicationName = testProject.getName();
    String versionName = createApplicationExportVersionName(testProject);

    ApplicationManagementImpl applicationManagement =
        (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
            .getApplicationManagement();

    // would throw RTE if workspace does already exist
    testProject = createWorkspace(testProject);

    boolean success = false;
    try {

      try {

        // first import the app
        applicationManagement.importApplication(second.getAbsolutePath(), false, false, false, false, false, true,
                                                true, false, null);

        // copy app to workspace
        CopyApplicationIntoWorkspaceParameters caiwp = new CopyApplicationIntoWorkspaceParameters();
        caiwp.setComment("TestProject import generated at " + new Date(System.currentTimeMillis()));
        caiwp.setTargetWorkspace(TestFactoryIntegrationServiceOperationImpl.getWorkspace(testProject));
        caiwp.setOverrideChanges(true);
        applicationManagement.copyApplicationIntoWorkspace(applicationName, versionName, caiwp);

        // now remove the application again
        RemoveApplicationParameters workingSetRemovalParams = new RemoveApplicationParameters();
        workingSetRemovalParams.setGlobal(true);
        workingSetRemovalParams.setExtraForce(true);
        workingSetRemovalParams
            .setParentWorkspace(TestFactoryIntegrationServiceOperationImpl.getWorkspace(testProject));
        applicationManagement.removeApplicationVersion(new ApplicationName(applicationName, null),
                                                       workingSetRemovalParams, false, null);

      } catch (XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState
          | XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain | XFMG_DuplicateVersionForApplicationName
          | XFMG_CouldNotImportApplication | XFMG_CouldNotRemoveApplication | XFMG_CouldNotBuildWorkingSet e) {
        throw new RuntimeException(e);
      } finally {

        boolean remove = false;

        // make sure that the previously imported application is removed again
        for (ApplicationInformation appInfo : applicationManagement.listApplications()) {
          if (appInfo.getName().equals(applicationName) && appInfo.getVersion().equals(versionName)) {
            remove = true;
            break;
          }
        }
        if (remove) {
          RemoveApplicationParameters rap = new RemoveApplicationParameters();
          rap.setKeepForAudits(false);
          rap.setGlobal(true);
          rap.setExtraForce(true);
          rap.setStopIfRunning(false); // it should not be running, fail if it does
          try {
            applicationManagement.removeApplicationVersion(new ApplicationName(applicationName, versionName), rap,
                                                           false, null);
          } catch (XFMG_CouldNotRemoveApplication e) {
            throw new RuntimeException(e);
          }
        }
      }

      // after removeApplicationVersion for less collisions
      checkTestDataXmomOdsNames(testProject);
      checkTestObjectDataXmomOdsName(testProject);
      success = true;

    } finally {

      // delete the created workspace if there was at least one exception in the code above
      if (!success) {
        WorkspaceManagement workspaceManagement =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
        RemoveWorkspaceParameters rwp = new RemoveWorkspaceParameters();
        rwp.setCleanupXmls(true);
        Workspace workspace = TestFactoryIntegrationServiceOperationImpl.getWorkspace(testProject);
        try {
          workspaceManagement.removeWorkspace(workspace, rwp);
        } catch (XFMG_CouldNotRemoveWorkspace e) {
          logger.warn("Failed to remove workspace '" + workspace.getName() + "' from failed import!", e);
        }
      }

    }
  }


  private static void checkTestDataXmomOdsNames(TestProject testProject) {

    if (testProject.getTestDataMetaData() != null && testProject.getTestDataMetaData().size() > 0) {
      Long revision;
      try {
        revision =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                .getRevision(new Workspace(testProject.getWorkspaceReference()));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
      for (TestDataMetaData testMeta : testProject.getTestDataMetaData()) {
        if (testMeta.getTestDataFullQualifiedStorableName() != null) {
          checkTestDataXmomOdsNamesForTestDataMetaData(testMeta, revision);
        }
      }
    }

  }
  
  
  private static void checkTestObjectDataXmomOdsName(TestProject testProject) {

    if (testProject.getTestObjectType() != null && testProject.getTestObjectType().length() > 0) {
      Long revision;
      try {
        revision =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                .getRevision(new Workspace(testProject.getWorkspaceReference()));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
      checkTestDataXmomOdsNamesForFqName(testProject.getTestObjectType(), revision);
    }

  }


  public static void checkTestDataXmomOdsNamesForTestDataMetaData(TestDataMetaData testMeta, long revision) {

    if (testMeta.getTestDataFullQualifiedStorableName() == null
        || testMeta.getTestDataFullQualifiedStorableName().length() == 0) {
      return;
    }

    checkTestDataXmomOdsNamesForFqName(testMeta.getTestDataFullQualifiedStorableName(), revision);

  }
  
  
  public static void checkTestDataXmomOdsNamesForFqName(String fqStorableName, long revision) {

    if (fqStorableName == null || fqStorableName.length() == 0) {
      return;
    }

    XMOMPersistenceManagement xmomPersMgmt =
        XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();

    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();

    Collection<XMOMODSMapping> mappingsThisDatatype;
    Set<String> allConfiguredNames = new HashSet<String>();
    try {
      mappingsThisDatatype = XMOMODSMappingUtils.getAllMappingsForRootType(fqStorableName, revision);
      Collection<Long> revisions = revMgmt.getAllRevisions();
      for (Long rev : revisions) {
        Collection<XMOMODSMapping> mappingsThisRevision = XMOMODSMappingUtils.getAllMappingsForRevision(rev);
        Set<String> tableNamesThisRevision = mappingsThisRevision.stream().map(x -> x.getTablename()).collect(Collectors.toSet());
        allConfiguredNames.addAll(tableNamesThisRevision);
      }
    } catch (PersistenceLayerException e1) {
      throw new RuntimeException(e1);
    }


    Optional<XMOMODSMapping> oldMapping = mappingsThisDatatype.stream().filter(x -> x.getPath() == null || x.getPath().isEmpty()).filter(x -> x.getColumnname() == null).findFirst();
    String ownName = oldMapping.isPresent() ? oldMapping.get().getTablename() : null;

    if (ownName != null && allConfiguredNames.contains(ownName)) {
      // TODO this generates confusing names if the originalName ends with numbers
      ownName = removeTrailingNumbers(ownName);
      int suffix = 1;
      while (allConfiguredNames.contains(ownName + suffix)) {
        suffix++;
      }
      try {
        ODSRegistrationParameter params = new ODSRegistrationParameter(fqStorableName, revision, "", ownName + suffix, null, false);
        xmomPersMgmt.setODSName(params);
      } catch (XNWH_InvalidXMOMStorablePathException | XNWH_StorableNotFoundException | XNWH_ODSNameMustBeUniqueException
          | PersistenceLayerException | XNWH_ODSNameChangedButNotDeployedException e) {
        throw new RuntimeException(e);
      }
    }

  }


  private static File buildApplicationFromTestProject(TestProject testProject, Workspacename source) {
    ApplicationManagementImpl applicationManagement =
        (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
            .getApplicationManagement();
    Workspace parentWorkspace = new Workspace(source.getWorkspacename());
    String applicationName = testProject.getName();
    String versionName = createApplicationExportVersionName(testProject);
    Long parentRevision;
    try {
      parentRevision = TestFactoryIntegrationServiceOperationImpl.getRev(parentWorkspace);
      applicationManagement.defineApplication(testProject.getName(),
                                              "Autogenerated Testproject Application created at "
                                                  + new Date(System.currentTimeMillis()), parentRevision);
      
      try {
        ApplicationDefinition newAppDef = new ApplicationDefinition(testProject.getName(), parentWorkspace);
        RuntimeContextDependencyManagement rcdMgmt =
                  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        rcdMgmt.modifyDependencies(newAppDef,
                                   new ArrayList<RuntimeDependencyContext>(rcdMgmt.getDependencies(parentWorkspace)),
                                   "TestFactory.buildApplicationFromTestProject");
        
        if (testProject.getTestObjectType() != null &&
            !testProject.getTestObjectType().isEmpty()) {
          applicationManagement.addObjectToApplication(testProject.getTestObjectType(), applicationName, null,
                                                       ApplicationEntryType.DATATYPE, parentRevision, false, null);
        }
        
        for (TestCase testCase : testProject.getTestCase()) {
          if (testCase.getTestProcessReference() != null && !testCase.getTestProcessReference().trim().isEmpty()) {
            applicationManagement.addObjectToApplication(testCase.getTestProcessReference(), applicationName, null,
                                                         ApplicationEntryType.ORDERINPUTSOURCE, parentRevision, false, null);
          }
        }
        RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        for (TestDataMetaData testDataMetaData : testProject.getTestDataMetaData()) {
          if (testDataMetaData.getTestDataFullQualifiedStorableName() != null
              && !testDataMetaData.getTestDataFullQualifiedStorableName().trim().isEmpty()) {
            if (rcdm.getRevisionDefiningXMOMObjectOrParent(testDataMetaData.getTestDataFullQualifiedStorableName(), parentRevision) == parentRevision) {
              applicationManagement.addObjectToApplication(testDataMetaData.getTestDataFullQualifiedStorableName(),
                                                           applicationName, parentRevision);
            }
          }
        }
        XynaActivationTrigger xt = XynaFactory.getInstance().getActivation().getActivationTrigger();
        Filter[] filters = xt.getFilters(parentRevision);
        for (Filter f : filters) {
          for (FilterInstanceStorable fis : xt.getFilterInstancesForFilter(f.getName(), parentRevision)) {
            applicationManagement.addObjectToApplication(fis.getFilterInstanceName(), applicationName, null, ApplicationEntryType.FILTERINSTANCE,
                                                         parentRevision, false, null);
          }
        }

        // Take the workflow that is part of the SUT to perform configuration adjustments
        // TODO this should generally rather be part of the SUT export but with the SUT export we currently don't have an application
        if (testProject.getSystemUnderTest() != null && testProject.getSystemUnderTest().getConfigurationAdjustmentOrderType() != null) {
          applicationManagement.addObjectToApplication(testProject.getSystemUnderTest()
              .getConfigurationAdjustmentOrderType(), applicationName, parentRevision);
        }

        BuildApplicationVersionParameters bavp = new BuildApplicationVersionParameters();
        bavp.setParentWorkspace(parentWorkspace);
        bavp.setComment("Autogenerated Testproject Application Build created at "
            + new Date(System.currentTimeMillis()));
        applicationManagement.buildApplicationVersion(applicationName, versionName, bavp);
        
        try {
          File exportFile;
          exportFile = File.createTempFile(escapeTestProjectNameForFileSystem(applicationName), null);
          applicationManagement.exportApplication(applicationName, versionName, exportFile.getPath(), true, null,
                                                  false, false, null);
          return exportFile;
        } finally {
          RemoveApplicationParameters rap = new RemoveApplicationParameters();
          rap.setParentWorkspace(parentWorkspace);
          rap.setExtraForce(true);
          rap.setKeepForAudits(false);
          applicationManagement.removeApplicationVersion(new ApplicationName(applicationName, versionName), rap, false,
                                                         null);
        }
      } finally {
        RemoveApplicationParameters rap = new RemoveApplicationParameters();
        rap.setParentWorkspace(parentWorkspace);
        applicationManagement.removeApplicationVersion(new ApplicationName(applicationName, null), rap, false, null);
      }
    } catch (XFMG_CouldNotExportApplication | IOException | XFMG_CouldNotBuildNewVersionForApplication | XFMG_CouldNotModifyRuntimeContextDependenciesException 
        | XFMG_FailedToAddObjectToApplication | XFMG_DuplicateApplicationName | XFMG_CouldNotRemoveApplication | PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  private static String createApplicationExportVersionName(TestProject testProject) {
    return "Export";
  }


  private static String generateTestProjectExportFileName(TestProject testProject) {
    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
    return "TestProject_" + escapeTestProjectNameForFileSystem(testProject) + "_" + timestamp +".export";
  }


  private static String escapeTestProjectNameForFileSystem(TestProject testProject) {
    return escapeTestProjectNameForFileSystem(testProject.getName());
  }


  private static String escapeTestProjectNameForFileSystem(String testProjectName) {
    return FILE_SYSTEM_ESCAPE_PATTERN.matcher(testProjectName).replaceAll("_");
  }


  private static String removeTrailingNumbers(String input) {
    Matcher matcher = WITHOUT_TRAILING_NUMBERS_PATTERN.matcher(input);
    if (matcher.matches()) {
      return matcher.group(1);
    } else {
      return input;
    }
  }


  


  private static void writeEntry(ZipOutputStream zos, InputStream input, String entryName) throws IOException {
    byte[] buffer = new byte[1024];
    int len;
    ZipEntry ze = new ZipEntry(entryName);
    zos.putNextEntry(ze);
    while ((len = input.read(buffer)) != -1) {
      zos.write(buffer, 0, len);
    }
    zos.closeEntry();
  }


  public static TestProject importTestProject(XynaOrderServerExtension correlatedXynaOrder, ManagedFileID mfid) {

    TestProjectArchive archive = null;
    try {

      FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
      TransientFile file = fm.retrieve(mfid.getID());
      try (InputStream importStream = file.openInputStream()) {
        archive = TestProjectArchive.read(importStream);
      }
      TestProject result =
          (TestProject) XynaObject.fromXml(archive.testProjectXml, TestFactoryIntegrationServiceOperationImpl.getRev());
      checkTestProjectUniqueness(correlatedXynaOrder, result);

      // take into account that on the exporting system the reference may have been obscured
      result.setWorkspaceReference(result.getName() + " " + result.getVersion());

      importTestProject(archive.exportFile, result);
      importTestData(result, archive.testData);
      List<? extends TestObjectMapping> testObjectData = importTestObjectData(result, archive.testObjectData);
      
      return adjustTestProject(correlatedXynaOrder, result, testObjectData);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (archive != null) {
        File tmpFile = archive.exportFile;
        if (tmpFile.exists()) {
          tmpFile.delete();
        }
      }
    }

  }
  
  
  private static TestProject adjustTestProject(XynaOrderServerExtension xo, TestProject referenceProject, List<? extends TestObjectMapping> testObjectMapping) throws TestProjectNotUnique, XMOMStorableAccessException {
    XMOMPersistenceManagement xmomPersMgmt =
                    XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();
    Workspacename sourceWs = new Workspacename(referenceProject.getWorkspaceReference());
    TestProject newProject = referenceProject.clone(true);
    //version
    if (newProject.getVersion() == null ||
        newProject.getVersion().isEmpty()) {
      newProject.setVersion("1");
    }
    //uniqueness
    checkTestProjectUniqueness(xo, newProject);
    //workspacereference
    if (newProject.getWorkspaceReference() == null ||
        newProject.getWorkspaceReference().isEmpty()) {
      newProject.setWorkspaceReference(newProject.getName() + " " + newProject.getVersion());
    }
    // Hide Testproject for selection
    newProject.setImportActive(true);
    
    newProject.setID(0);
    // testcase
    Map<Long, TestCase> testCaseDictionary = new HashMap<>();
    if (newProject.getTestCase() != null &&
        newProject.getTestCase().size() > 0) {
      for (TestCase testCase : newProject.getTestCase()) {
        TestCase newTestCase = testCase.clone();
        newTestCase.setID(0);
        testCaseDictionary.put(testCase.getID(), newTestCase);
      }
      newProject.setTestCase(new ArrayList<>(testCaseDictionary.values()));
    }
    // testdatametadata
    if (newProject.getTestDataMetaData() != null &&
        newProject.getTestDataMetaData().size() > 0) {
      List<TestDataMetaData> testDataMetaDataList = new ArrayList<>();
      for (TestDataMetaData testDataMetaData : newProject.getTestDataMetaData()) {
        TestDataMetaData newTestDataMetaData = testDataMetaData.clone();
        newTestDataMetaData.setID(0);
        testDataMetaDataList.add(newTestDataMetaData);
      }
      newProject.setTestDataMetaData(testDataMetaDataList);
    }
    // systemUnderTest
    if (newProject.getSystemUnderTest() != null) {
      xnwh.persistence.SelectionMask mask = new xnwh.persistence.SelectionMask.Builder().rootType(TestProject.class.getAnnotation(XynaObjectAnnotation.class).fqXmlName()).instance();
      xnwh.persistence.QueryParameter params = new xnwh.persistence.QueryParameter.Builder().maxObjects(1).queryHistory(false).instance();
      String filter = "%0%.name==\"" + newProject.getSystemUnderTest().getName() + "\" && %0%.version==\"" + newProject.getSystemUnderTest().getVersion() + "\"";
      List<? extends XynaObject> systemUnderTestSearchResult = TestDataQueryAndNotification.query(xmomPersMgmt, xo, mask, filter, params);
      if (systemUnderTestSearchResult.size() > 0) {
        // replace with reference from current system
        SystemUnderTest sut = (SystemUnderTest) systemUnderTestSearchResult.get(0);
        newProject.setSystemUnderTest(sut);
        if (newProject.getTestCase().size() > 0) {
          for (TestCase testCase : newProject.getTestCase()) {
            List<Feature> newFeatureList = new ArrayList<>();
            for (Feature aCoveredFeature : testCase.getCoveredFeatures()) {
              for (Feature aPossibleFeature : sut.getFeature()) {
                if (aCoveredFeature.getName().equals(aPossibleFeature.getName()) &&
                    aCoveredFeature.getDescription().equals(aPossibleFeature.getDescription())) {
                  newFeatureList.add(aPossibleFeature);
                }
              }
            }
            testCase.setCoveredFeatures(newFeatureList);
          }
        }
      } else {
        // reference not found, clean everything related to systemUnderTest
        newProject.setSystemUnderTest(null);
        for (TestCase testCase : newProject.getTestCase()) {
          testCase.setCoveredFeatures(null);
        }
      }
      // systemUnderTestInstance
      if (newProject.getSystemUnderTestInstance() != null &&
          newProject.getSystemUnderTest() != null) {
        SystemUnderTestInstance currentInstance = newProject.getSystemUnderTestInstance();
        newProject.setSystemUnderTestInstance(null);
        for (SystemUnderTestInstance possibleInstance : newProject.getSystemUnderTest().getSystemUnderTestInstance()) {
          if (possibleInstance.getName().equals(currentInstance.getName())) {
            newProject.setSystemUnderTestInstance(possibleInstance);
          }
        }
      }
    } else {
      newProject.setSystemUnderTestInstance(null);
    }
    
    try {
      xmomPersMgmt.store(xo, newProject, new StoreParameter(false, false, false));
    } catch (PersistenceLayerException e) {
      throw new XMOMStorableAccessException(TestProject.class.getName(), e);
    }
    
    boolean success = false;
    try {
      if (newProject.getTestCase() != null) {
        for (TestCase testCase : newProject.getTestCase()) {
          if (testCase.getTestProcessReference() != null &&
              !testCase.getTestProcessReference().isEmpty()) {
            TestProjectCreationParameter tpcp = new TestProjectCreationParameter(false, true);
            TestCase adjustedTestCase = TestFactoryIntegrationServiceOperationImpl.cloneReferencedOrderInputSourceImpl(testCase, sourceWs, new Workspacename(newProject.getWorkspaceReference()), tpcp);
            try {
              xmomPersMgmt.store(xo, adjustedTestCase, new StoreParameter(false, false, false));
            } catch (PersistenceLayerException e) {
              throw new XMOMStorableAccessException(TestCase.class.getName(), e);
            }
          }
        }
      }
      
      Map<Long, TestObject> toMap = new HashMap<>();
      for (TestObjectMapping toMapping : testObjectMapping) {
        toMap.put(toMapping.getOldId(), toMapping.getTestObject());
      }
      
      List<TestCaseChain> newChains = new ArrayList<>();
      if (newProject.getTestCaseChain() != null) {
        for (TestCaseChain testCaseChain : newProject.getTestCaseChain()) {
          TestCaseChain newChain = testCaseChain.clone(true);
          //- TestCaseChain.IDs
          newChain.setId(0);
          for (TestCaseInstance testCaseInstance : newChain.getTestCaseInstance()) {
            if (testCaseInstance.getTestCase() != null) {
              //- TestCaseChain.TestCaseInstance[].TestCase.IDs
              testCaseInstance.setTestCase(testCaseDictionary.get(testCaseInstance.getTestCase().getID()));
            }
          }
          for (TestCaseInstance testCaseInstance : newChain.getTestCaseInstance()) {
            //- TestCaseChain.TestCaseInstance[].TestObject
            TestObject to = new TestObject.Builder().id(toMap.get(testCaseInstance.getTestObject().getId()).getId()).instance();
            testCaseInstance.setTestObject(to);
          }
          List<TestObjectMetaData> newTestObjects = new ArrayList<>();
          for (TestObjectMetaData testObject : newChain.getTestObjectMetaData()) {
            TestObjectMetaData newTestObjectMetaData = new TestObjectMetaData();
            newTestObjectMetaData.setTestObjectId(toMap.get(testObject.getTestObjectId()).getId());
            newTestObjects.add(newTestObjectMetaData);
          }
          //- TestCaseChain.TestObject[].IDs
          newChain.setTestObjectMetaData(newTestObjects);
          //- TestCaseChain.testProjectId
          newChain.setTestProjectId(newProject.getID());
          newChains.add(newChain);
        }
        
        newProject.setTestCaseChain(newChains);
        
        try {
          // store project again for testCaseChains
          xmomPersMgmt.store(xo, newProject, new StoreParameter(false, false, false));
        } catch (PersistenceLayerException e) {
          throw new XMOMStorableAccessException(TestProject.class.getName(), e);
        }
      }
      // Enable Testproject selection
      newProject.setImportActive(false);
      
      try {
        // store project again
        xmomPersMgmt.store(xo, newProject, new StoreParameter(false, false, false));
      } catch (PersistenceLayerException e) {
        throw new XMOMStorableAccessException(TestProject.class.getName(), e);
      }
      
      success = true;
    } finally {
      if (!success) {
        try {
          xmomPersMgmt.delete(xo, newProject, new DeleteParameter(false, ForwardReferenceHandling.RECURSIVE_DELETE, BackwardReferenceHandling.DELETE));
        } catch (PersistenceLayerException e) {
          throw new XMOMStorableAccessException(TestProject.class.getName(), e);
        }
      }
    }
    
    return newProject;
  }


  public static void checkTestProjectUniqueness(XynaOrderServerExtension xo, TestProject tp) throws TestProjectNotUnique {
    XMOMPersistenceManagement xmomPersMgmt =
        XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();
    List<String> selectedColumns = new ArrayList<>();
    selectedColumns.add("%0%.name");
    selectedColumns.add("%0%.version");
    String filter = "(%0%.name==\"" + tp.getName() + "\") && (%0%.version==\"" + tp.getVersion() + "\")";
    try {
      List<TestProject> existingTestProjects =
          (List<TestProject>) TestDataQueryAndNotification.query(xmomPersMgmt, xo, new SelectionMask(TestProject.class.getName(),
                                                                                         selectedColumns), filter, new QueryParameter(-1, false, null));
      if (existingTestProjects.size() > 0) {
        throw new TestProjectNotUnique(tp.getName(), tp.getVersion());
      }
    } catch (XMOMStorableAccessException e) {
      throw new RuntimeException("Failed to determine test project uniqueness for project " + tp.getName() + ", Version " + tp.getVersion(), e);
    }
  }


  private static void importTestData(TestProject testProject, List<String> testDataXmls) {

    List<TestDataMetaData> metaDataObjects = (List<TestDataMetaData>) testProject.getTestDataMetaData();
    if (metaDataObjects == null || metaDataObjects.size() == 0) {
      return;
    }

    Workspace workspace = new Workspace(testProject.getWorkspaceReference());
    long revision = TestFactoryIntegrationServiceOperationImpl.getRev(workspace);

    try {
      for (String testDataXml : testDataXmls) {
        @SuppressWarnings("rawtypes")
        XynaObjectList xol = (XynaObjectList) XynaObject.generalFromXml(testDataXml, revision);
        if (xol.size() > 0) {
          XynaOrderCreationParameter xocp =
              new XynaOrderCreationParameter(new DestinationKey(IMPORT_TEST_DATA_ORDERTYPE, workspace), xol);
          try {
            XynaFactory.getInstance().getXynaMultiChannelPortal().startOrderSynchronously(xocp);
          } catch (XynaException e) {
            throw new RuntimeException(e);
          }
        }
      }
    } catch (XPRC_InvalidXMLForObjectCreationException | XPRC_XmlParsingException | XPRC_MDMObjectCreationException e) {
      throw new RuntimeException(e);
    }

  }
  
  private static List<? extends TestObjectMapping> importTestObjectData(TestProject testProject, String testObjectDataXml) {
    if (testObjectDataXml != null && 
        testObjectDataXml.length() > 0) {
      checkTestObjectDataXmomOdsName(testProject);
      
      Workspace workspace = new Workspace(testProject.getWorkspaceReference());
      long revision = TestFactoryIntegrationServiceOperationImpl.getRev(workspace);
      
      try {
        @SuppressWarnings("rawtypes")
        XynaObjectList xol = (XynaObjectList) XynaObject.generalFromXml(testObjectDataXml, revision);
        if (xol.size() > 0) {
          XynaOrderCreationParameter xocp =
              new XynaOrderCreationParameter(new DestinationKey(IMPORT_TEST_OBJECT_DATA_ORDERTYPE, workspace), xol);
          return (List<? extends TestObjectMapping>) XynaFactory.getInstance().getXynaMultiChannelPortal().startOrderSynchronously(xocp);
        } else {
          return Collections.emptyList();
        }
      } catch (XynaException e) {
        throw new RuntimeException(e);
      }
    } else {
      return Collections.emptyList();
    }
  }


  public static void importInfrastructure(TestProject testproject) {

    final String workspacename = testproject.getWorkspaceReference();
    if (workspacename == null) {
      throw new RuntimeException("Workspace reference may not be null");
    }
    logger.debug("Importing Testfactory Infrastructure from " + TestProjectExportImport.class + " - " + TestProjectExportImport.class.getClassLoader());
    
    logger.debug("Importing Testfactory Infrastructure to workspace " + workspacename);

    Workspace workspace = new Workspace(workspacename);
    CopyApplicationIntoWorkspaceParameters params = new CopyApplicationIntoWorkspaceParameters();

    params.setTargetWorkspace(workspace);
    params.setComment("Autogenerated Testproject Workspace created at " + new Date(System.currentTimeMillis()));
    params.setOverrideChanges(true);

    String applicationName = "XynaTestFactoryInfrastructure";
    String versionName = "";

    List<ApplicationInformation> appsinfo;
    try {
      appsinfo =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement().listApplications();
      for (ApplicationInformation applicationInformation : appsinfo) {
        logger.trace("lookforTestFactoryInfrastructureApp===>" + applicationInformation.getName() + "<>"
            + applicationInformation.getState() + "<>" + applicationInformation.getVersion());
        if (applicationInformation.getName().trim().equals(applicationName)) {
          if (logger.isTraceEnabled()) {
            logger.trace("Found Application, comparing versions: '" + versionName + "' and '"
                + applicationInformation.getVersion() + "'");
          }
          if (versionName.equals("") && applicationInformation.getState() == ApplicationState.STOPPED) {
            versionName = applicationInformation.getVersion();
          } else if (applicationInformation.getState() == ApplicationState.STOPPED
              && compareVersions(versionName, applicationInformation.getVersion()) > 0) {
            versionName = applicationInformation.getVersion();
          }
        }
      }
    } catch (PersistenceLayerException e1) {
      throw new RuntimeException(e1);
    }       
       

    logger.debug("Application with name " + applicationName + " and version " + versionName + " found.");
    try {
        Application xtf = new Application(applicationName,versionName);
        RuntimeContextDependencyManagement rcdMgmt =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      rcdMgmt.addDependency(workspace,xtf, "xtf", true);
    } catch (XFMG_CouldNotModifyRuntimeContextDependenciesException | PersistenceLayerException e) {
        throw new RuntimeException(e);
    }

  }


  private static class TestProjectArchive {
    
    private String testProjectXml;
    private List<String> testData;
    private String testObjectData;
    private File exportFile;
    
    private TestProjectArchive() {
      testData = new ArrayList<>();
    }
    
    
    public TestProjectArchive(String testProjectXml, List<String> testData, String testObjectData, File exportFile) {
      this.testProjectXml = testProjectXml;
      this.testData = testData;
      this.testObjectData = testObjectData;
      this.exportFile = exportFile;
    }


    public static TestProjectArchive read(InputStream stream) {
      TestProjectArchive archive = new TestProjectArchive();
      try (ZipInputStream zis = new ZipInputStream(stream)) {
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
          if (ze.getName().equals(TEST_PROJECT_EXPORT_XML_ZIP_ENTRY_NAME)) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
              StreamUtils.copy(zis, baos);
              archive.testProjectXml = new String(baos.toByteArray(), Constants.DEFAULT_ENCODING);
            }
          } else if (ze.getName().startsWith(TEST_DATA_EXPORT_XML_ZIP_ENTRY_PREFIX)) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
              StreamUtils.copy(zis, baos);
              archive.testData.add(new String(baos.toByteArray(), Constants.DEFAULT_ENCODING));
            }
          } else if (ze.getName().equals(TEST_OBJECT_DATA_EXPORT_XML_ZIP_ENTRY_NAME)) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
              StreamUtils.copy(zis, baos);
              archive.testObjectData = new String(baos.toByteArray(), Constants.DEFAULT_ENCODING);
            }
          } else {
            archive.exportFile = File.createTempFile("testProjectImport", null);
            FileUtils.writeStreamToFile(zis, archive.exportFile);
          }
          zis.closeEntry();
          ze = zis.getNextEntry();
        }
        if (archive.testProjectXml == null || archive.exportFile == null) {
          throw new RuntimeException("Archive appears to be incomplete.");
        }
        return archive;
      } catch (IOException | Ex_FileWriteException e) {
        throw new RuntimeException(e);
      }
    }
    
    
    private InputStream getMergedInputStream() {
      logger.debug("Exporting TestProject-PersistenceData, TestProject-Application and " + testData.size() + " TestData-PersistenceData");
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
          try (ByteArrayInputStream bais = new ByteArrayInputStream(OtherExportImportAndUtils.getBytes(testProjectXml))) {
            writeEntry(zos, bais, TEST_PROJECT_EXPORT_XML_ZIP_ENTRY_NAME);
          }
          int testDataCount = 1;
          for (String testDataExportXml : testData) {
            try (ByteArrayInputStream bais =
                new ByteArrayInputStream(OtherExportImportAndUtils.getBytes(testDataExportXml))) {
              writeEntry(zos, bais, TEST_DATA_EXPORT_XML_ZIP_ENTRY_PREFIX + testDataCount
                  + TEST_DATA_EXPORT_XML_ZIP_ENTRY_SUFFIX);
              testDataCount++;
            }
          }
          try (ByteArrayInputStream bais =
              new ByteArrayInputStream(OtherExportImportAndUtils.getBytes(testObjectData))) {
            writeEntry(zos, bais, TEST_OBJECT_DATA_EXPORT_XML_ZIP_ENTRY_NAME);
            testDataCount++;
          }
          try (FileInputStream fis = new FileInputStream(exportFile)) {
            writeEntry(zos, fis, exportFile.getName());
          }
          zos.flush();
        }
        return new ByteArrayInputStream(baos.toByteArray());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    
  }


  /**
   * @return -1 if the first argument is larger, +1 if the second is larger and 0 if they are equal. Ignores leading
   *         zeroes.
   */
  private static int compareVersions(String version1, String version2) {
    String[] version1Parts = version1.split("\\.");
    String[] version2Parts = version2.split("\\.");
    for (int i = 0; i < version1Parts.length; i++) {
      if (i < version2Parts.length) {
        try {
          int nextDigit1 = Integer.valueOf(version2Parts[i]);
          int nextDigit2 = Integer.valueOf(version1Parts[i]);
          if (nextDigit1 == nextDigit2) {
            continue;
          } else if (nextDigit1 < nextDigit2) {
            return -1;
          } else {
            return 1;
          }
        } catch (NumberFormatException e) {
          throw new RuntimeException("Currently only integer versions are supported: Tried to compare '" + version1
              + "' to '" + version2 + "'");
        }
      }
    }
    return 0;
  }

  public static void main(String[] args) {
    System.out.println(System.currentTimeMillis());
  }
  
}
