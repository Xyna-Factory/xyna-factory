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
package xdev.xtestfactory.infrastructure.migration.impl;



import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import xact.templates.Document;
import xdev.xtestfactory.infrastructure.datatypes.TestDataSelector;
import xdev.xtestfactory.infrastructure.datatypes.Workspacename;
import xdev.xtestfactory.infrastructure.migration.MigrationServicesServiceOperation;
import xdev.xtestfactory.infrastructure.migration.TestCaseAndOrderInputSourceCombination;
import xdev.xtestfactory.infrastructure.migration.TestSeriesAndListOfTestCaseOISCombination;
import xdev.xtestfactory.infrastructure.migration.XTF5TestcaseCSV;
import xdev.xtestfactory.infrastructure.migration.XTF5TestcasetestseriesCSV;
import xdev.xtestfactory.infrastructure.migration.XTF5TestdurchfuerhungsplanCSV;
import xdev.xtestfactory.infrastructure.migration.XTF5TestseriesCSV;
import xdev.xtestfactory.infrastructure.migration.testcases.InputGenerationMapping;
import xdev.xtestfactory.infrastructure.migration.testcases.TestProcessMapping;
import xdev.xtestfactory.infrastructure.migration.testseries.FunctionObject;
import xdev.xtestfactory.infrastructure.migration.testseries.RefID;
import xdev.xtestfactory.infrastructure.migration.testseries.SeriesMigrationName;
import xdev.xtestfactory.infrastructure.migration.testseries.SeriesMigrationPath;
import xdev.xtestfactory.infrastructure.migration.testseries.ServiceReference;
import xdev.xtestfactory.infrastructure.storables.TestCase;
import xdev.xtestfactory.infrastructure.storables.TestProject;
import xfmg.xopctrl.User;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.opencsv.CSVReader;



public class MigrationServicesServiceOperationImpl implements ExtendedDeploymentTask, MigrationServicesServiceOperation {


  public static final String KEY_ORDERTYPE_INPUTGENERATOR = "orderTypeOfGeneratingWorkflow";
  public static final String KEY_ORDERTYPE_TESTCASEID = "testCaseID";
  public static final String KEY_ORDERTYPE_TESTCASENAME = "testCaseName";
  public static final String KEY_RESPONSIBLE_USER = "author";

  private static final String OIS_PREFIX = "Order Input Source for Test Case ";

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
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
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  private static Pattern SPLIT_AT_COMMA_PATTERN = Pattern.compile(",");


  public Container obtainTestCaseObjectsWithoutInputSources(XTF5TestdurchfuerhungsplanCSV xTF5TestdurchfuerhungsplanCSV,
                                                            List<? extends InputGenerationMapping> inputGenerationMappingList,
                                                            List<? extends TestProcessMapping> testProcessMappingList,
                                                            XTF5TestcaseCSV xTF5TestcaseCSV,
                                                            XTF5TestseriesCSV xTF5TestseriesCSV,
                                                            XTF5TestcasetestseriesCSV xTF5TestcasetestseriesCSV) {

    Map<String, String> testProcessMappingToOrderType = new HashMap<>();
    if (testProcessMappingList != null && testProcessMappingList.size() > 0) {
      for (TestProcessMapping tpm : testProcessMappingList) {
        testProcessMappingToOrderType.put(tpm.getXTF5TestProcessName(), tpm.getXTF6Ordertype());
      }
    }

    Map<String, String> testPackageMappingToOrderType = new HashMap<>();
    if (inputGenerationMappingList != null && inputGenerationMappingList.size() > 0) {
      for (InputGenerationMapping igm : inputGenerationMappingList) {
        testPackageMappingToOrderType.put(igm.getXTF5TestPackageName(), igm.getXTF6Ordertype());
      }
    }

    try {

      StringReader sr = new StringReader(xTF5TestdurchfuerhungsplanCSV.getCSVStringWithColumnHeaders());

      CSVReader csvReader = new CSVReader(sr, ";".charAt(0));
      csvReader.readNext(); // ignore headers

      // read entries
      Map<String, TestCaseAndOrderInputSourceCombination> allTestCaseCombinations =
          new HashMap<String, TestCaseAndOrderInputSourceCombination>();

      String[] nextStorableData = null;
      while ((nextStorableData = csvReader.readNext()) != null) {

        TestCase nextTestCase = new TestCase();

        String testCaseName = nextStorableData[0];
        nextTestCase.setName(testCaseName);

        String testCaseDesc = nextStorableData[2];
        nextTestCase.setDescription(testCaseDesc);

        String testDataKennersString = nextStorableData[4];
        String[] testDataKenners = SPLIT_AT_COMMA_PATTERN.split(testDataKennersString);
        TestDataSelector nextSelectorObject = new TestDataSelector();
        if (testDataKenners != null && testDataKenners.length > 0) {
          nextSelectorObject.setSelector1(testDataKenners[0].trim());
          if (testDataKenners.length > 1) {
            nextSelectorObject.setSelector2(testDataKenners[1].trim());
            if (testDataKenners.length > 2) {
              nextSelectorObject.setSelector3(testDataKenners[2].trim());
              if (testDataKenners.length > 3) {
                nextSelectorObject.setSelector4(testDataKenners[3].trim());
                if (testDataKenners.length > 4) {
                  nextSelectorObject.setSelector5(testDataKenners[4].trim());
                  if (testDataKenners.length > 5) {
                    nextSelectorObject.setSelector6(testDataKenners[5].trim());
                    if (testDataKenners.length > 6) {
                      nextSelectorObject.setSelector7(testDataKenners[6].trim());
                      if (testDataKenners.length > 7) {
                        nextSelectorObject.setSelector8(testDataKenners[7].trim());
                      }
                    }
                  }
                }
              }
            }
          }
        }
        nextTestCase.setTestDateSelector(nextSelectorObject);

        String priorityString = nextStorableData[6];
        if (priorityString != null && priorityString.length() > 0) {
          try {
            nextTestCase.setPriority(Integer.valueOf(priorityString));
          } catch (RuntimeException e) {
            nextTestCase.setPriority(0);
          }
        }

        String creatorOld = nextStorableData[10];
        nextTestCase.setAuthor(new User(creatorOld));

        String responsibleOld = nextStorableData[11];
        nextTestCase.setResponsibleUser(new User(responsibleOld));

        TestCaseAndOrderInputSourceCombination nextCombination = new TestCaseAndOrderInputSourceCombination();
        nextCombination.setTestCase(nextTestCase);

        String testProcessOld = nextStorableData[7];
        String testProcessOrderType = testProcessMappingToOrderType.get(testProcessOld);
        nextCombination.setExecutionOrderType(testProcessOrderType);

        String testPackageOld = nextStorableData[8];
        String testPackageOrderType = testPackageMappingToOrderType.get(testPackageOld);
        nextCombination.setGeneratorOrderType(testPackageOrderType);

        allTestCaseCombinations.put(nextTestCase.getName(), nextCombination);

      }

      XynaObjectList<TestCaseAndOrderInputSourceCombination> result = new XynaObjectList<>(TestCaseAndOrderInputSourceCombination.class);
      for (TestCaseAndOrderInputSourceCombination nextTx : allTestCaseCombinations.values()) {
        result.add(nextTx);
      }

      // table "testseries"
      Map<Long, TestSeriesAndListOfTestCaseOISCombination> testSeriesNames = new HashMap<>();
      sr = new StringReader(xTF5TestseriesCSV.getCSVStringWithColumnHeaders());
      csvReader = new CSVReader(sr, ",".charAt(0));
      csvReader.readNext(); // ignore headers
      while ((nextStorableData = csvReader.readNext()) != null) {
        String nextId = nextStorableData[0];
        String nextName = nextStorableData[1].replaceAll(" ", "");
        String nextDesc = nextStorableData[3];
        TestSeriesAndListOfTestCaseOISCombination nextSeriesObject = new TestSeriesAndListOfTestCaseOISCombination();
        nextSeriesObject.setTestSeriesName(nextName);
        nextSeriesObject.setTestSeriesDescription(nextDesc);
        testSeriesNames.put(Long.valueOf(nextId), nextSeriesObject);
      }

      // Table "testcase"
      Map<Long, TestCaseAndOrderInputSourceCombination> testCaseIDsToName = new HashMap<>();
      sr = new StringReader(xTF5TestcaseCSV.getCSVStringWithColumnHeaders());
      csvReader = new CSVReader(sr, ",".charAt(0));
      csvReader.readNext(); // ignore headers
      while ((nextStorableData = csvReader.readNext()) != null) {
        Long nextId = Long.valueOf(nextStorableData[0]);
        String nextName = nextStorableData[1];
        TestCaseAndOrderInputSourceCombination nextCombi = allTestCaseCombinations.get(nextName);
        testCaseIDsToName.put(nextId, nextCombi);
      }

      // Table "testcasetestseries"
      Map<Long, SortedMap<Long, TestCaseAndOrderInputSourceCombination>> allTestSeries =
          new HashMap<>();
      sr = new StringReader(xTF5TestcasetestseriesCSV.getCSVStringWithColumnHeaders());
      csvReader = new CSVReader(sr, ",".charAt(0));
      csvReader.readNext(); // ignore headers
      while ((nextStorableData = csvReader.readNext()) != null) {
        Long nextTestSeriesId = Long.valueOf(nextStorableData[1]);
        Long nextTestCaseId = Long.valueOf(nextStorableData[2]);
        // TODO Sub-Serien beruecksichtigen
        Long sequence = Long.valueOf(nextStorableData[4]);
        SortedMap<Long, TestCaseAndOrderInputSourceCombination> testSeriesEntries = allTestSeries.get(nextTestSeriesId);
        if (testSeriesEntries == null) {
          testSeriesEntries = new TreeMap<Long, TestCaseAndOrderInputSourceCombination>();
          allTestSeries.put(nextTestSeriesId, testSeriesEntries);
        }
        TestCaseAndOrderInputSourceCombination testCaseCombi = testCaseIDsToName.get(nextTestCaseId);
        testSeriesEntries.put(sequence, testCaseCombi);
      }

      XynaObjectList<TestSeriesAndListOfTestCaseOISCombination> result2 =
          new XynaObjectList<>(TestSeriesAndListOfTestCaseOISCombination.class);

      for (Entry<Long, SortedMap<Long, TestCaseAndOrderInputSourceCombination>> nextSeriesEntry: allTestSeries.entrySet()) {
        TestSeriesAndListOfTestCaseOISCombination nextSeries = testSeriesNames.get(nextSeriesEntry.getKey());
        for (TestCaseAndOrderInputSourceCombination nextReferencedTestCase: nextSeriesEntry.getValue().values()) {
          nextSeries.addToTestCaseAndOrderInputSourceCombination(nextReferencedTestCase);
        }
        result2.add(nextSeries);
      }

      return new Container(result, result2);

    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException(e);
    }

  }


  @Override
  public TestCaseAndOrderInputSourceCombination buildAndStoreOrderInputSource(TestCaseAndOrderInputSourceCombination tcCombination,
                                                                              Workspacename ws) {

    OrderInputSourceManagement management =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();

    String name = OIS_PREFIX + tcCombination.getTestCase().getName();
    Map<String, String> parameters = new HashMap<>();
    parameters.put(KEY_ORDERTYPE_INPUTGENERATOR, tcCombination.getGeneratorOrderType());
    parameters.put(KEY_ORDERTYPE_TESTCASEID, tcCombination.getTestCase().getID() + "");
    parameters.put(KEY_ORDERTYPE_TESTCASENAME, tcCombination.getTestCase().getName());
    parameters.put(KEY_RESPONSIBLE_USER, tcCombination.getTestCase().getResponsibleUser().getName());
    OrderInputSourceStorable ois =
        new OrderInputSourceStorable(name, "XTFInputSource", tcCombination.getExecutionOrderType(), null, null,
                                     ws.getWorkspacename(), "", parameters);

    try {
      management.createOrderInputSource(ois);
    } catch (XynaException e) {
      throw new RuntimeException("Failed to create Order Input Source for Test Case "
          + tcCombination.getTestCase().getName(), e);
    }

    tcCombination.getTestCase().setTestProcessReference(name);

    return tcCombination;

  }


  @Override
  public Container getServiceAndFunctionObjects(TestSeriesAndListOfTestCaseOISCombination testSeries) {

    AtomicInteger currentId = new AtomicInteger(10);

    XynaObjectList<ServiceReference> serviceReferences = new XynaObjectList<>(ServiceReference.class);
    XynaObjectList<FunctionObject> functionObjects = new XynaObjectList<>(FunctionObject.class);

    Map<String, ServiceReference> testProcessOrdertypes = new HashMap<>();

    for (TestCaseAndOrderInputSourceCombination tcCombination : testSeries.getTestCaseAndOrderInputSourceCombination()) {

      String nextOrdertype = tcCombination.getExecutionOrderType();

      ServiceReference nextServiceReference = testProcessOrdertypes.get(nextOrdertype);
      if (nextServiceReference == null) {
        nextServiceReference = new ServiceReference();
        testProcessOrdertypes.put(nextOrdertype, nextServiceReference);
        String refName = GenerationBase.getSimpleNameFromFQName(nextOrdertype);
        String refPath = GenerationBase.getPackageNameFromFQName(nextOrdertype);
        nextServiceReference.setID(currentId.getAndIncrement());
        nextServiceReference.setReferenceName(refName);
        nextServiceReference.setReferencePath(refPath);
        nextServiceReference.setSourceRefIDs(new ArrayList<RefID>());
        nextServiceReference.setTargetRefIDs(new ArrayList<RefID>());
      }

      FunctionObject nextFunctionObject = new FunctionObject();
      nextFunctionObject.setID(currentId.getAndIncrement());
      nextFunctionObject.setServiceID(nextServiceReference.getID());
      nextFunctionObject.setSourceID(nextServiceReference.getID());
      nextFunctionObject.setTargetID(nextServiceReference.getID());
      nextFunctionObject.setCompensateID(currentId.getAndIncrement());
      nextFunctionObject.setOperationName(nextServiceReference.getReferenceName());
      nextFunctionObject.setOrderInputSourceReference(OIS_PREFIX + tcCombination.getTestCase().getName());

      nextServiceReference.addToSourceRefIDs(new RefID(nextFunctionObject.getServiceID()));
      nextServiceReference.addToTargetRefIDs(new RefID(nextFunctionObject.getServiceID()));

      functionObjects.add(nextFunctionObject);

    }

    serviceReferences.addAll(testProcessOrdertypes.values());

    return new Container(serviceReferences, functionObjects);

  }


  @Override
  public void storeAndDeployWorkflow(Document doc, SeriesMigrationPath migPath, SeriesMigrationName name,
                                     TestSeriesAndListOfTestCaseOISCombination seriesCombination) {

    long rev = ((ClassLoaderBase) getClass().getClassLoader()).getRevision();

    try {
      XynaFactory.getInstance().getXynaMultiChannelPortal().saveMDM(doc.getText(), rev);
      XynaFactory.getInstance().getProcessing().getWorkflowEngine()
          .deployWorkflow(migPath.getPath() + "." + name.getName(), WorkflowProtectionMode.BREAK_ON_USAGE, rev);
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }

  }


  @Override
  public TestCase determineTestCaseByNameFromGivenTestProject(TestProject testProject,
                                                              TestCaseAndOrderInputSourceCombination tcCombi) {

    for (TestCase tc: testProject.getTestCase()) {
      if (tc.getName() != null && tc.getName().equals(tcCombi.getTestCase().getName())) {
        return tc;
      }
    }
    throw new RuntimeException("Test case with name \"" + tcCombi.getTestCase().getName()
        + "\" not found in test project.");

  }

}
