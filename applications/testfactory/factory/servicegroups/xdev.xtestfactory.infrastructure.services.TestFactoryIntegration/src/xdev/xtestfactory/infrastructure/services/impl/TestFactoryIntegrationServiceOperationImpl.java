/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package xdev.xtestfactory.infrastructure.services.impl;



import java.io.ByteArrayOutputStream; 
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import org.apache.log4j.Logger;

import xdev.xtestfactory.infrastructure.datatypes.CounterID;
import xdev.xtestfactory.infrastructure.datatypes.ManagedFileID;
import xdev.xtestfactory.infrastructure.datatypes.TestCaseID;
import xdev.xtestfactory.infrastructure.datatypes.TestCaseInstance;
import xdev.xtestfactory.infrastructure.datatypes.TestDataGenerationID;
import xdev.xtestfactory.infrastructure.datatypes.TestObjectMapping;
import xdev.xtestfactory.infrastructure.datatypes.TestProjectCreationParameter;
import xdev.xtestfactory.infrastructure.datatypes.Workspacename;
import xdev.xtestfactory.infrastructure.exceptions.NoMatchingTestDataAvailable;
import xdev.xtestfactory.infrastructure.exceptions.SUTInstanceSpecificConfigurationFailed;
import xdev.xtestfactory.infrastructure.exceptions.TestDataHasAlreadyBeenUsed;
import xdev.xtestfactory.infrastructure.exceptions.TestDataNotFound;
import xdev.xtestfactory.infrastructure.exceptions.TestProjectNotUnique;
import xdev.xtestfactory.infrastructure.services.ExceptionCount;
import xdev.xtestfactory.infrastructure.services.OrderID;
import xdev.xtestfactory.infrastructure.services.TestFactoryIntegrationServiceOperation;
import xdev.xtestfactory.infrastructure.services.impl.complexerfunctions.OtherExportImportAndUtils;
import xdev.xtestfactory.infrastructure.services.impl.complexerfunctions.OtherExportImportAndUtils.ContentType;
import xdev.xtestfactory.infrastructure.services.impl.complexerfunctions.TestDataQueryAndNotification;
import xdev.xtestfactory.infrastructure.services.impl.complexerfunctions.TestProjectExportImport;
import xdev.xtestfactory.infrastructure.storables.Counter;
import xdev.xtestfactory.infrastructure.storables.Feature;
import xdev.xtestfactory.infrastructure.storables.Interface;
import xdev.xtestfactory.infrastructure.storables.SystemUnderTest;
import xdev.xtestfactory.infrastructure.storables.SystemUnderTestInstance;
import xdev.xtestfactory.infrastructure.storables.TestCase;
import xdev.xtestfactory.infrastructure.storables.TestCaseChain;
import xdev.xtestfactory.infrastructure.storables.TestData;
import xdev.xtestfactory.infrastructure.storables.TestDataMetaData;
import xdev.xtestfactory.infrastructure.storables.TestObject;
import xdev.xtestfactory.infrastructure.storables.TestProject;
import xdev.xtestfactory.infrastructure.storables.TestReport;
import xdev.xtestfactory.infrastructure.storables.TestReportEntryFeature;
import xdev.xtestfactory.infrastructure.storables.TestReportEntryTestCase;
import xdev.xtestfactory.infrastructure.util.testdata.legacy.LegacyTestDataReference;
import xnwh.persistence.FilterCondition;
import xnwh.persistence.QueryParameter;
import xnwh.persistence.SelectionMask;
import xnwh.persistence.Storable;
import xnwh.persistence.XMOMStorableAccessException;
import base.Text;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.streams.StreamUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveWorkspace;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchDataModelException;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchDataModelTypeException;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelManagement;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult.Message;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult.MessageGroup;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.DataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ModifyDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.RemoveWorkspaceParameters;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.DeleteParameter;
import com.gip.xyna.xnwh.persistence.xmom.DeleteParameter.BackwardReferenceHandling;
import com.gip.xyna.xnwh.persistence.xmom.DeleteParameter.ForwardReferenceHandling;
import com.gip.xyna.xnwh.persistence.xmom.StoreParameter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage.ChildOrderStorageStack;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xpce.cleanup.CleanupDispatcher;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringCodes;



public class TestFactoryIntegrationServiceOperationImpl
    implements
      ExtendedDeploymentTask,
      TestFactoryIntegrationServiceOperation {


  private static Logger logger = CentralFactoryLogging.getLogger(TestFactoryIntegrationServiceOperationImpl.class);

  // Watch out: The following parameters are also present in the migration service
  public static final String KEY_ORDERTYPE_TESTCASENAME = "testCaseName";
  private static final XynaPropertyString OIS_PREFIX = new XynaPropertyString("xdev.xtestfactory.infrastructure.testcase.orderinputsource.prefix", "Order Input Source for Test Case ");
  private static final String KEY_ORDERTYPE_TESTCASEID = "testCaseID";
  public static final String KEY_ORDERTYPE_INPUTGENERATOR = "orderTypeOfGeneratingWorkflow";
  private static final String XTF_CLEANUP_WF_NAME = "xdev.xtestfactory.infrastructure.services.TestFactoryCleanup";


  public void onDeployment() throws XynaException {
  }


  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If nul is returned, the factory default <IGNORE> will be used.
    return null;
  }


  //Workspace anlegen und Infrastruktur importieren
  @Override
  public TestProject createWorkspaceAndImportInfrastructure(TestProject testProject) {
    testProject = TestProjectExportImport.createWorkspace(testProject);
    importInfrastructure(testProject);
    return testProject; 
  }


  public static Workspace getWorkspace(TestProject testProject) {
    return new Workspace(testProject.getWorkspaceReference());
  }


  @Override
  public void deleteWorkspace(final TestProject testProject) {

    if (testProject.getWorkspaceReference() == null || testProject.getWorkspaceReference().length() == 0
        || testProject.getWorkspaceReference().equals(RevisionManagement.DEFAULT_WORKSPACE.getName())) {
      throw new RuntimeException("Attempt to delete empty or default workspace - this is not allowed.");
    }

    final String workspacename = testProject.getWorkspaceReference();
    if (logger.isDebugEnabled()) {
      logger
          .debug("Starting thread to delete workspace for Testproject " + testProject.getName() + ":" + workspacename);
    }

    final WorkspaceManagement workspaceManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    final RemoveWorkspaceParameters rwp = new RemoveWorkspaceParameters();
    rwp.setForce(true);
    rwp.setCleanupXmls(true);

    // Delete the workspace asynchronously so that the delete workflow can finish 
    Runnable deleterRunnable = new Runnable() {

      @Override
      public void run() {
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          // ignore
        }
        try {
          workspaceManagement.removeWorkspace(new Workspace(workspacename), rwp);
        } catch (XFMG_CouldNotRemoveWorkspace e1) {
          logger.error("Failed to delete workspace " + workspacename + " for test project '" + testProject.getName()
              + "'", e1);
        }
      }
    };
    Thread deleterThread = new Thread(deleterRunnable);
    deleterThread.start();
  }


  private static String getCompressedContentFromFileManagement(String filemanagementid) {
    FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    TransientFile file = fm.retrieve(filemanagementid);
    try (InputStream is = file.openInputStream()) {
      ZipInputStream zipinput = new ZipInputStream(is);
      zipinput.getNextEntry();
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        StreamUtils.copy(zipinput, out);
        String content = new String(out.toByteArray(), Constants.DEFAULT_ENCODING);
        logger.debug("Unzipped Content: " + content);
        return content;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public ManagedFileID exportSystemUnderTest(SystemUnderTest systemundertest) {
    ManagedFileID resultid = new ManagedFileID();
    String xmlrepresentation = systemundertest.toXml();
    logger.debug("XML Representation before zipping: " + xmlrepresentation);
    resultid.setID(OtherExportImportAndUtils.compressContentToFileManagement(OtherExportImportAndUtils
        .getBytes(xmlrepresentation), ContentType.XML));
    return resultid;
  }


  @Override
  public SystemUnderTest importSystemUnderTest(ManagedFileID mfid) {
    SystemUnderTest result = new SystemUnderTest();
    String content = getCompressedContentFromFileManagement(mfid.getID());
    try {
      result = (SystemUnderTest) XynaObject.fromXml(content, getRev());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return result;
  }


  private String getCSV(List<? extends TestCase> testcases) {
    String resultcsv = "";
    StringBuilder sb = new StringBuilder();
    sb.append(";Prio<=1;Prio<=2;Prio<=3;Prio<=4;Prio<=5;Prio<=6;Prio<=7;Prio<=8;Prio<=9;"); // Spezialspalten

    List<Feature> features = new ArrayList<Feature>();

    // Testcases auflisten und alle Features sammeln
    for (TestCase t : testcases) {
      sb.append(t.getName() + ";");
      for (Feature f : t.getCoveredFeatures()) {
        boolean found = false;
        for (Feature x : features) {
          // mit ID schauen, ob Feature schon in Liste ist
          if (x.getID() == f.getID())
            found = true;
        }
        if (!found)
          features.add(f);
      }
    }
    sb.append("\n"); // Spaltenbezeichnungszeile (Auflistung Testcases) zuende

    sb.append(";;;;;;;;;;"); // Prio Spezialspalten auslassen
    for (TestCase t : testcases) {
      sb.append(t.getPriority() + ";");
    }
    sb.append("\n"); // Spaltenbezeichnungszeile (Auflistung Priorities) zuende

    for (Feature f : features) {
      sb.append(f.getName() + ";");
      // Spezialspalte Anzahl Testcases mit Prio x
      int prio = 0;
      while (prio < 9) {
        prio++;
        int counter = 0;
        for (TestCase t : testcases) {
          int testcasepriority = t.getPriority();
          for (Feature cf : t.getCoveredFeatures()) {
            if (cf.getName() != null && cf.getName().equals(f.getName()) && (testcasepriority <= prio)) {
              counter++;
            }
          }
        }
        sb.append(counter + ";");
      }

      // Durchlauf zum Ankreuzen welcher Testcase das Feature benutzt
      for (TestCase t : testcases) {
        for (Feature cf : t.getCoveredFeatures()) {
          if (cf.getName() != null && cf.getName().equals(f.getName())) {
            sb.append("x");
          }
        }
        sb.append(";");
      }
      sb.append("\n");
    }

    resultcsv = sb.toString();
    return resultcsv;
  }


  @Override
  public ManagedFileID createCoverageMatrixAsCSV(List<? extends TestCase> testcases) {

    String coverageFileName = "CoverageMatrix";

    ManagedFileID resultid = new ManagedFileID();
    String resultcsv = getCSV(testcases);
    resultid
        .setID(OtherExportImportAndUtils.uncompressedToFileManagement(OtherExportImportAndUtils.getBytes(resultcsv),
                                                                      ContentType.CSV, coverageFileName));
    if (resultid.getID() == null) {
      throw new RuntimeException("Filemanagementresultid is null!");
    }
    return resultid;
  }


  private ByteArrayOutputStream convertCSVToExcelFileOutputStream(String csvcontent) {
    // csv in Excel Dokument umwandeln
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      WritableWorkbook workbook = Workbook.createWorkbook(os);
      WritableSheet sheet = workbook.createSheet("First Page", 0);

      int x = 0;
      int y = 0;
      String[] zeilen = csvcontent.split("\n");
      for (String s : zeilen) {
        String[] spalten = s.split(";");
        for (String cv : spalten) {
          Label label = new Label(x, y, cv);
          sheet.addCell(label);
          x++;
        }
        y++;
        x = 0;
      }

      workbook.write();
      workbook.close();
      os.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return os;
  }


  @Override
  public ManagedFileID createCoverageMatrixAsExcel(List<? extends TestCase> testcases) {
    String coverageFileName = "CoverageMatrix";
    ManagedFileID resultid = new ManagedFileID();
    String resultcsv = getCSV(testcases);
    ByteArrayOutputStream os = convertCSVToExcelFileOutputStream(resultcsv);
    resultid.setID(OtherExportImportAndUtils.uncompressedToFileManagement(os.toByteArray(), ContentType.XLS,
                                                                          coverageFileName));
    if (resultid.getID() == null)
      throw new RuntimeException("Filemanagementresultid is null!");
    return resultid;
  }


  @Override
  public void checkInterfaces(List<? extends Interface> inputinterfaces) {

    DataModelManagement dmm =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();

    for (Interface inputinterface : inputinterfaces) {
      String currentReference = inputinterface.getDataModelReference();
      if (currentReference != null) {
        boolean exists = dmm.hasDataModel(new DataModelParameters("XSD", currentReference));
        if (!exists) {
          throw new RuntimeException("No Datamodel found for: " + currentReference + " ! Please import first!");
        }
      }
    }

  }


  public static long getRev() {
    ChildOrderStorageStack childStack = ChildOrderStorage.childOrderStorageStack.get();
    if (childStack != null &&
        childStack.getCorrelatedXynaOrder() != null &&
        childStack.getCorrelatedXynaOrder().getRootOrder() != null) {
      return childStack.getCorrelatedXynaOrder().getRootOrder().getRevision();
    } else if (TestFactoryIntegrationServiceOperationImpl.class.getClassLoader() instanceof ClassLoaderBase) {
      ClassLoaderBase clb = (ClassLoaderBase) TestFactoryIntegrationServiceOperationImpl.class.getClassLoader();
      return clb.getRevision();
    } else {
      return 0;
    }
  }


  public static long getRev(Workspacename workspacename) {
    return getRev(new Workspace(workspacename.getWorkspacename()));
  }


  public static long getRev(Workspace workspace) {
    try {
      return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRevision(workspace);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void copyDatamodelToNewWorkspace(Workspacename wsn, List<? extends Interface> interfaces) {
    List<String> listOfDatamodelReferences = new ArrayList<String>();
    for (Interface i : interfaces) {
      String curref = i.getDataModelReference();
      if (curref != null && curref.length() > 0) {
        if (!listOfDatamodelReferences.contains(curref)) {
          listOfDatamodelReferences.add(i.getDataModelReference());
        }
      }
    }
    String workspacename = wsn.getWorkspacename();
    DataModelManagement dmm =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
    for (String nameofinterface : listOfDatamodelReferences) {
      DataModelResult dmr = new DataModelResult();
      boolean exists = dmm.hasDataModel(new DataModelParameters("XSD", nameofinterface));
      if (!exists) {
        throw new RuntimeException("Unexpected problem: Datamodel to copy (" + nameofinterface
            + ") does not exist on system.");
      }
      ModifyDataModelParameters mdmp = new ModifyDataModelParameters("XSD", nameofinterface);
      mdmp.addParameter("workspaces", workspacename);
      mdmp.addParameter("workspaceMode", "Add");
      try {
        boolean succeeded = dmm.modifyDataModel(dmr, mdmp);
        if (succeeded) {
          logger.debug("Successful copied datamodel to " + workspacename);
        } else {
          StringBuilder msg = new StringBuilder();

          if (dmr.hasSingleMessages()) {
            for (Message m : dmr.getSingleMessages()) {
              switch (m.getLevel()) {
                case Error :
                  msg.append("Error: " + m.getMessage() + "\n");
                  break;
                case Warning :
                  msg.append("Warning: " + m.getMessage() + "\n");
                  break;
                default :
                  break;
              }
            }
          }
          if (dmr.hasMessageGroups()) {
            for (MessageGroup m : dmr.getMessageGroups()) {
              switch (m.getLevel()) {
                case Error :
                  msg.append("Error: " + m.toSingleString(":\n  ", "\n  ") + "\n");
                  break;
                case Warning :
                  msg.append("Warning: " + m.toSingleString(":\n  ", "\n  ") + "\n");
                  break;
                default :
                  break;
              }
            }
          }
          if (dmr.getExceptions() != null && dmr.getExceptions().size() > 0) {
            throw new RuntimeException("Could not copy datamodel " + nameofinterface + " to workspace " + workspacename
                + "!\n Messages: " + msg.toString(), dmr.getExceptions().get(0));
          } else {
            throw new RuntimeException("Could not copy datamodel " + nameofinterface + " to workspace " + workspacename
                + "!\n Messages: " + msg.toString());
          }
        }
      } catch (XFMG_NoSuchDataModelTypeException | XFMG_NoSuchDataModelException | PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
    }
  }


  @Override
  public Text getFQN(Storable storable) {
    XynaObjectAnnotation annotation = storable.getClass().getAnnotation(XynaObjectAnnotation.class);
    String fqn = annotation.fqXmlName();
    base.Text fqnresult = new base.Text(fqn);
    return fqnresult;
  }


  @Override
  public Workspacename determineWorkspace() {
    Long revisionToResolve;
    ChildOrderStorageStack childStack = ChildOrderStorage.childOrderStorageStack.get();
    if (childStack != null) {
      revisionToResolve = childStack.getCorrelatedXynaOrder().getRootOrder().getRevision();
    } else {
      throw new RuntimeException("Failed to determine current workspace");
    }
    Workspace ownWs;
    try {
      ownWs = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getWorkspace(revisionToResolve);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException("Failed to determine current workspace", e);
    }
    
    return new Workspacename(ownWs.getName());
  }


  @Override
  public TestCaseID getTestCaseID() {
    String testCaseId = XynaProcessing.getOrderContext().getCustom0();
    if (testCaseId == null) {
      throw new RuntimeException("Test Case ID not found in field custom0.");
    }
    return new TestCaseID(Long.valueOf(testCaseId));
  }


  @Override
  public TestDataGenerationID getTestDataGenerationID() {
    String generationId = XynaProcessing.getOrderContext().getCustom3();
    if (generationId == null) {
      throw new RuntimeException("Generation ID not found in field custom3.");
    }
    return new TestDataGenerationID(Long.valueOf(generationId));
  }


  @Override
  public Container getOrderIDAndExceptionCount(XynaOrderServerExtension xo) {
    OrderID oid = new OrderID(xo.getId());
    ExceptionCount ec = new ExceptionCount(xo.getErrors() != null ? xo.getErrors().length : 0);
    TestCaseID tcid;
    if (xo.getCustom0() != null &&
        xo.getCustom0().length() > 0) {
      try {
        Long id = Long.valueOf(xo.getCustom0());
        tcid = new TestCaseID(id);
      } catch (NumberFormatException e) {
        tcid = new TestCaseID(-1l);
      }
    } else {
      tcid = new TestCaseID(-1l);
    }
    Container result = new Container(oid, ec, tcid);
    return result;
  }


  @Override
  public TestCase cloneReferencedOrderInputSource(TestCase inputTestCase, Workspacename source, Workspacename target,
                                                  TestProjectCreationParameter creationParams) {
    return cloneReferencedOrderInputSourceImpl(inputTestCase, source, target, creationParams);
  }
  
  public static TestCase cloneReferencedOrderInputSourceImpl(TestCase inputTestCase, Workspacename source, Workspacename target,
                                                          TestProjectCreationParameter creationParams) {
    TestCase result = inputTestCase;
    OrderInputSourceStorable ois = null;
    String reference = inputTestCase.getTestProcessReference();
    long sourceRevision = getRev(source);
    OrderInputSourceManagement management =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
    try {
      ois = management.getInputSourceByName(sourceRevision, reference, true);
      if (ois == null &&
          !reference.equals(reference.trim())) {
        ois = management.getInputSourceByName(sourceRevision, reference.trim(), true);
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }

    if (ois != null) {
      String name = "Order Input Source for Test Case " + inputTestCase.getName();
      if (ois.getParameters() == null || ois.getParameters().size() <= 0
          || !ois.getParameters().containsKey(KEY_ORDERTYPE_TESTCASEID)) {
        throw new RuntimeException("Orderinputsource " + reference + " is malformed, expected param '"
            + KEY_ORDERTYPE_TESTCASEID + "' could not be found!");
      }
      ois.getParameters().put(KEY_ORDERTYPE_TESTCASEID, String.valueOf(inputTestCase.getID()));
      ois.getParameters().put(KEY_ORDERTYPE_TESTCASENAME, inputTestCase.getName());
      String oldGeneratingOrdertype = ois.getParameters().get(KEY_ORDERTYPE_INPUTGENERATOR);
      ois.getParameters().put(KEY_ORDERTYPE_INPUTGENERATOR,
                              oldGeneratingOrdertype != null ? oldGeneratingOrdertype : "");
      ois.setName(name);
      ois.setRuntimeContext(new Workspace(target.getWorkspacename()));
      try {
        if (creationParams.getRenameOrderInputSource()) {
          management.modifyOrderInputSource(ois);
        } else {
          management.createOrderInputSource(ois);
        }
      } catch (XynaException e) {
        throw new RuntimeException(e);
      }
      result.setTestProcessReference(name);
    } else {
      result.setTestProcessReference(null);
      logger.debug("Orderinputsource " + reference + " not found! Reference removed from TestCase '" + result.getName() + "'.");
    }
    return result;
  }


  @Override
  public void deleteOrderInputSource(TestCase tc, Workspacename source) {
    OrderInputSourceManagement management =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
    long revision = getRev(source);
    OrderInputSourceStorable oiss;
    try {
      oiss = management.getInputSourceByName(revision, tc.getTestProcessReference());
      management.deleteOrderInputSource(oiss.getId());
      DestinationKey dk = new DestinationKey(oiss.getOrderType(), new Workspace(source.getWorkspacename()));
      try {
        CleanupDispatcher cp = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher();
        DestinationValue dv = cp.getDestination(dk);
        List<String> otherInputSources;
        if (dv.getDestinationType() == ExecutionType.XYNA_FRACTAL_WORKFLOW &&
            ((FractalWorkflowDestination)dv).getFQName().equals(XTF_CLEANUP_WF_NAME)) {
          try {
            otherInputSources = management.getReferencedOrderInputSources(dv, revision);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            otherInputSources = Collections.emptyList();
          } catch (PersistenceLayerException e) {
            throw new RuntimeException("Failed to determine Order Input Source usage for test case '" + tc.getName() + "'", e);
          }
          if (otherInputSources == null ||
              otherInputSources.size() <= 0) {
            try {
              cp.removeCustomDestination(dk, dv);
              cp.setDestination(dk, XynaDispatcher.DESTINATION_EMPTY_WORKFLOW, true);
            } catch (PersistenceLayerException e) {
              throw new RuntimeException("Failed to reset cleanup destination for '" + oiss.getOrderType() + "'", e);
            }
          }
        }
      } catch (XPRC_DESTINATION_NOT_FOUND e1) {
        // ntbd
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Failed to remove Order Input Source for test case '" + tc.getName() + "'", e);
    } catch (XynaException e) {
      throw new RuntimeException("Failed to remove Order Input Source for test case '" + tc.getName() + "'", e);
    }
  }


  @Override
  public void notifyTestCaseExecutionService(XynaOrderServerExtension correlatedXynaOrder)
      throws XMOMStorableAccessException, TestDataNotFound, TestDataHasAlreadyBeenUsed {
    TestDataQueryAndNotification.notifyTestCaseExecutionService(correlatedXynaOrder, getTestDataGenerationID());
  }


  @Override
  public List<? extends Storable> queryWithTestDataSupport(XynaOrderServerExtension correlatedXynaOrder,
                                                           SelectionMask selectionMask,
                                                           FilterCondition filter,
                                                           QueryParameter queryParameter)
      throws XMOMStorableAccessException, NoMatchingTestDataAvailable {
    return TestDataQueryAndNotification.queryWithTestDataSupport(correlatedXynaOrder, selectionMask, filter,
                                                                 queryParameter, getTestCaseID(),
                                                                 getTestDataGenerationID());
  }


  @Override
  public ManagedFileID createExcelFromTestReport(TestReport testreport,
                                                 List<? extends TestReportEntryFeature> features,
                                                 List<? extends TestReportEntryTestCase> testcases) {
    return OtherExportImportAndUtils.createExcelFromTestReport(testreport, features, testcases);
  }


  @Override
  public ManagedFileID exportTestproject(XynaOrderServerExtension xo, TestProject tp, Workspacename ws) {
    return TestProjectExportImport.exportTestproject(xo, tp, ws);
  }


  @Override
  public void importInfrastructure(TestProject tp) {
    TestProjectExportImport.importInfrastructure(tp);
  }


  @Override
  public TestProject importTestProject(XynaOrderServerExtension xo, ManagedFileID mf) {
    return TestProjectExportImport.importTestProject(xo, mf);
  }


  @Override
  public void checkTestProjectUniqueness(XynaOrderServerExtension xo, TestProject tp) throws TestProjectNotUnique {
    TestProjectExportImport.checkTestProjectUniqueness(xo, tp);
  }


  @Override
  public ManagedFileID createCSVFromTestdata(List<? extends Storable> testdata, TestDataMetaData fqn) {
    return OtherExportImportAndUtils.createCSVFromTestdata(testdata, fqn);
  }


  @Override
  public ManagedFileID createExcelFromTestdata(List<? extends Storable> testdata, TestDataMetaData metaData) {
    return OtherExportImportAndUtils.createExcelFromTestdata(testdata, metaData);
  }


  @Override
  public List<? extends TestCase> filterListOfTestCases(List<? extends TestCase> listToBeFiltered, TestCase toBeRemovedFromList) {

    XynaObjectList<TestCase> result = new XynaObjectList<>(TestCase.class);

    for (TestCase tc: listToBeFiltered) {
      if (tc.getID() != toBeRemovedFromList.getID()) {
        // tc is not the one to be removed, so add it to the result to be returned
        result.add(tc);
      }
    }

    return result;

  }


  @Override
  public List<? extends Counter> filterListOfCounters(List<? extends Counter> countersToFilter, Counter idToRemove) {
    XynaObjectList<Counter> result = new XynaObjectList<>(Counter.class);

    for (Counter counter : countersToFilter) {
      if ( (counter == null) || (counter.getID() != idToRemove.getID()) ) {
        // counter is not the one to be removed, so add it to the result to be returned
        result.add(counter);
      }
    }

    return result;
  }


  @Override
  public void startSUTConfigurationAdjustmentOrder(SystemUnderTest sut, SystemUnderTestInstance sutiOld,
                                                   SystemUnderTestInstance sutiNew)
      throws SUTInstanceSpecificConfigurationFailed {

    DestinationKey dk =
        new DestinationKey(sut.getConfigurationAdjustmentOrderType(), new Workspace(determineWorkspace()
            .getWorkspacename()));
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(dk, new Container(sutiOld, sutiNew));
    xocp.setMonitoringLevel(MonitoringCodes.STEP_MONITORING);
    try {
      XynaFactory.getInstance().getProcessing().startOrderSynchronously(xocp);
    } catch (XynaException e) {
      throw new SUTInstanceSpecificConfigurationFailed(e);
    }

  }


  @Override
  public List<? extends LegacyTestDataReference> findLegacyTestDataReferences(TestData td) {

    XynaObjectList<LegacyTestDataReference> result =
        new XynaObjectList<LegacyTestDataReference>(LegacyTestDataReference.class);

    for (String nextVarName : td.getVariableNames()) {
      Object nextContent;
      try {
        nextContent = td.get(nextVarName);
      } catch (InvalidObjectPathException e) {
        throw new RuntimeException(e);
      }
      if (nextContent instanceof String) {
        String nextStringContent = (String) nextContent;

        int beginIndex = nextStringContent.indexOf("##LIST");
        if (beginIndex > -1) {
          String relevantSubstring = nextStringContent.substring(beginIndex);
          relevantSubstring = relevantSubstring.substring("##LIST(".length(), relevantSubstring.indexOf(")"));
          relevantSubstring = relevantSubstring.trim();
          String targetList = relevantSubstring.substring(0, relevantSubstring.indexOf("."));
          String targetColumn = relevantSubstring.substring(relevantSubstring.indexOf(".") + 1, relevantSubstring.indexOf(","));
          
          LegacyTestDataReference nextLegacyReference = new LegacyTestDataReference();
          nextLegacyReference.setReferencingVariableName(nextVarName);
          nextLegacyReference.setReferencedTestDataList(targetList);
          nextLegacyReference.setReferencedColumn(targetColumn);
          result.add(nextLegacyReference);
        }
      }
    }

    return result;
  }


  @Override
  public TestCase buildEmptyOrderInputSource(TestCase tc, Workspacename ws) {

    OrderInputSourceManagement management =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();

    String name = OIS_PREFIX.get() + tc.getName();
    Map<String, String> parameters = new HashMap<>();
    //    parameters.put(KEY_ORDERTYPE_INPUTGENERATOR, tcCombination.getGeneratorOrderType());
    parameters.put(KEY_ORDERTYPE_TESTCASEID, tc.getID() + "");
    parameters.put(KEY_ORDERTYPE_TESTCASENAME, tc.getName());
    OrderInputSourceStorable ois =
        new OrderInputSourceStorable(name, "XTFInputSource", "", null, null, ws.getWorkspacename(), "", parameters);

    try {
      management.createOrderInputSource(ois);
    } catch (XynaException e) {
      throw new RuntimeException("Failed to create Order Input Source for Test Case " + tc.getName(), e);
    }

    tc.setTestProcessReference(name);

    return tc;

  }


  @Override
  public List<? extends Storable> getTestdataStorablesFromCSV(ManagedFileID id) {
    return OtherExportImportAndUtils.getTestdataStorablesFromCSV(id);
  }


  @Override
  public void configureODSName(TestDataMetaData tdMeta) {
    TestProjectExportImport.checkTestDataXmomOdsNamesForTestDataMetaData(tdMeta, getRev());
  }


}
