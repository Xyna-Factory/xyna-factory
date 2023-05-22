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



import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.exceptions.XFMG_MDMObjectClassLoaderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoader;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.DeleteParameter;
import com.gip.xyna.xnwh.persistence.xmom.IFormula;
import com.gip.xyna.xnwh.persistence.xmom.QueryParameter;
import com.gip.xyna.xnwh.persistence.xmom.SelectionMask;
import com.gip.xyna.xnwh.persistence.xmom.SortCriterion;
import com.gip.xyna.xnwh.persistence.xmom.StoreParameter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xpce.OrderContext;

import xdev.xtestfactory.infrastructure.datatypes.TestCaseID;
import xdev.xtestfactory.infrastructure.datatypes.TestDataGenerationID;
import xdev.xtestfactory.infrastructure.exceptions.NoMatchingTestDataAvailable;
import xdev.xtestfactory.infrastructure.exceptions.TestDataHasAlreadyBeenUsed;
import xdev.xtestfactory.infrastructure.exceptions.TestDataNotFound;
import xdev.xtestfactory.infrastructure.storables.TestData;
import xdev.xtestfactory.infrastructure.storables.TestDataMetaData;
import xdev.xtestfactory.infrastructure.storables.TestDataUsageInfo;
import xdev.xtestfactory.infrastructure.storables.TestProject;
import xnwh.persistence.Storable;
import xnwh.persistence.XMOMStorableAccessException;



public class TestDataQueryAndNotification {

  private static ConcurrentMap<Long, Object> testDataLocks = new ConcurrentHashMap<>();


  @SuppressWarnings("unchecked")
  public static List<? extends Storable> queryWithTestDataSupport(XynaOrderServerExtension correlatedXynaOrder,
                                                                  xnwh.persistence.SelectionMask selectionMask,
                                                                  xnwh.persistence.FilterCondition filter,
                                                                  xnwh.persistence.QueryParameter queryParameter, TestCaseID testCaseId,
                                                                  TestDataGenerationID genId)
      throws XMOMStorableAccessException, NoMatchingTestDataAvailable {

    // TODO use XynaObjectAnnotation instead of Storable.class.getName() ?
    XMOMPersistenceManagement xmomPersMgmt =
        XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();
    if (!typeCheck(selectionMask.getRootType(), getRevision(correlatedXynaOrder))) {
      // does not extend TestData just query as it is
      return (List<? extends Storable>) query(xmomPersMgmt, correlatedXynaOrder, selectionMask, filter.getFormula(), queryParameter);
    }

    // this query changed: it needs to query over the current TestProject as the storable fq name is only unique in a workspace
    List<TestProject> testProjects = null;
    Workspace workspace;
    try {
      workspace = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getWorkspace(getRevision(correlatedXynaOrder));
      List<String> selectedColumns = new ArrayList<>();
      selectedColumns.add("%0%.testDataMetaData.iD");
      selectedColumns.add("%0%.testDataMetaData.name");
      selectedColumns.add("%0%.testDataMetaData.testDataFullQualifiedStorableName");
      selectedColumns.add("%0%.testDataMetaData.oneTimeTestData");
      testProjects =
          (List<TestProject>) query(xmomPersMgmt, correlatedXynaOrder, new SelectionMask(TestProject.class.getName(), selectedColumns),
                                    "%0%.workspaceReference==\"" + workspace.getName() + "\"", new QueryParameter(-1, false, null));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
    }
    if (testProjects == null || testProjects.size() <= 0) {
      return (List<? extends Storable>) query(xmomPersMgmt, correlatedXynaOrder, selectionMask, filter.getFormula(), queryParameter);
    }

    // Now find the TestDataMetaData object
    TestDataMetaData testDataMetaData = null;
    for (TestDataMetaData candidate : testProjects.get(0).getTestDataMetaData()) {
      if (candidate.getTestDataFullQualifiedStorableName() != null
          && candidate.getTestDataFullQualifiedStorableName().equals(selectionMask.getRootType())) {
        testDataMetaData = candidate;
        break;
      }
    }
    if (testDataMetaData == null) {
      throw new NoMatchingTestDataAvailable(selectionMask.getRootType());
    }

    // return if data is reusable
    if (!testDataMetaData.getOneTimeTestData()) {
      return (List<? extends Storable>) query(xmomPersMgmt, correlatedXynaOrder, selectionMask, filter != null ? filter.getFormula() : "",
                                              queryParameter);
    }

    // check if frequency controlled task (startedFromGUI == false)
    OrderContext poc = correlatedXynaOrder.getRootOrder().getOrderContext();
    Serializable ser = poc.get("startedFromGUI");
    boolean startedFromGUI = Boolean.valueOf(String.valueOf(ser));

    Object testDataLock = new Object();
    // The number of such locks is limited because the number of test data meta data objects is typically rather low
    Object existingLock = testDataLocks.putIfAbsent(testDataMetaData.getID(), testDataLock);
    if (existingLock != null) {
      testDataLock = existingLock;
    }
    synchronized (testDataLock) {
      
      TestData testData = null;
      if (startedFromGUI) {

        // Now look for usage information that dates back to older generation ids. If we included objects
        // with the same generationID as the current one, we would find the same object over and over again
        // in the case of multiple uses of the same test data meta data object within a single generation.
        // This particularly is a common case for test series.
        String notUsedFilter = calculateNotUsedFilterString(filter);
        String roottype = TestDataUsageInfo.class.getName();
        String formula = "(%0%.tDMetaDataID==\"" + testDataMetaData.getID() + "\")" + " && (%0%.occupyingTestCaseID==\""
            + testCaseId.getID() + "\")" + " && (%0%.generationID!=\"" + genId.getID() + "\")";
        QueryParameter queryParams = new QueryParameter(-1, false, new SortCriterion[] {new SortCriterion("%0%.generationID", true)});
        List<? extends TestDataUsageInfo> testDataUsageInfo =
            (List<? extends TestDataUsageInfo>) query(xmomPersMgmt, correlatedXynaOrder, new SelectionMask(roottype, (List<String>) null),
                                                      formula, queryParams);

        if (testDataUsageInfo.size() > 0) {
          TestDataUsageInfo latest = testDataUsageInfo.get(0);
          List<? extends Storable> storables =
              (List<? extends Storable>) query(xmomPersMgmt, correlatedXynaOrder, selectionMask,
                                               notUsedFilter + " && %0%.iD==\"" + latest.getID() + "\"",
                                               new QueryParameter(1, false, convertSortCriterions(queryParameter.getSortCriterion())));
          if (storables.size() > 0) {
            testData = (TestData) storables.get(0);
          }
        }
        if (testData == null) {
          QueryParameter minusOneFalseNullQueryParams = new QueryParameter(-1, false, null);
          List<? extends TestData> unusedTestData = (List<? extends TestData>) query(xmomPersMgmt, correlatedXynaOrder, selectionMask,
                                                                                     notUsedFilter, minusOneFalseNullQueryParams);
          if (unusedTestData.size() <= 0) {
            throw new NoMatchingTestDataAvailable(selectionMask.getRootType());
          }
          TestData unreservedTestDataObject = null;
          for (TestData unusedTestDataEntry : unusedTestData) {
            // Find usage info for this Test Data object (by other Test Case or by the same generation run of the test case at hand)
            /* The following conditions both need to be true to qualify as a violations:
             *   1) the test data ID is the same
             *   2) Either the test case is a different one or the entry stems from the same generation id
             *      (-> either test series or multiple call in one input generator)
             */
            String findViolatingEntries = "(%0%.testDataID==\"" + unusedTestDataEntry.getID() + "\")" + " && ((%0%.occupyingTestCaseID!=\""
                + testCaseId.getID() + "\") || (%0%.generationID==\"" + genId.getID() + "\"))";
            List<? extends TestDataUsageInfo> otherUsageInfo =
                (List<? extends TestDataUsageInfo>) query(xmomPersMgmt, correlatedXynaOrder,
                                                          new SelectionMask(roottype, (List<String>) null), findViolatingEntries,
                                                          minusOneFalseNullQueryParams);
            if (otherUsageInfo.size() <= 0) {
              unreservedTestDataObject = unusedTestDataEntry;
              break;
            }
          }
          if (unreservedTestDataObject == null) {
            // Steal one of the connected Test Data ENtries
            testData = unusedTestData.get(0);
          } else {
            // Use one of the unconnected entries
            testData = unreservedTestDataObject;
          }
        }
        TestDataUsageInfo newUsageInfo = new TestDataUsageInfo();
        newUsageInfo.setGenerationID(genId.getID());
        newUsageInfo.setOccupyingTestCaseID(testCaseId.getID());
        newUsageInfo.setStorableRootType(testDataMetaData.getTestDataFullQualifiedStorableName());
        newUsageInfo.setTDMetaDataID(testDataMetaData.getID());
        newUsageInfo.setTestDataID(testData.getID());
        store(xmomPersMgmt, correlatedXynaOrder, newUsageInfo, new StoreParameter(false, false, false));

        return new XynaObjectList<>(Storable.class, testData);

      } else {

        //In this case, test data entry should not be reserved/marked but directly set on used = true
        //The test data entry should not be reserved elsewhere

        String notUsedFilter = calculateNotUsedFilterString(filter);
        String roottype = TestDataUsageInfo.class.getName();

        QueryParameter minusOneFalseNullQueryParams = new QueryParameter(-1, false, null);
        List<? extends TestData> unusedTestData =
            (List<? extends TestData>) query(xmomPersMgmt, correlatedXynaOrder, selectionMask, notUsedFilter, minusOneFalseNullQueryParams);
        if (unusedTestData.size() <= 0) {
          throw new NoMatchingTestDataAvailable(selectionMask.getRootType());
        }

        for (TestData unusedTestDataEntry : unusedTestData) {

          String findReservedButNotUsedEntry = "(%0%.testDataID==\"" + unusedTestDataEntry.getID() + "\")";
          QueryParameter queryParams = new QueryParameter(-1, false, new SortCriterion[] {new SortCriterion("%0%.generationID", true)});
          List<? extends TestDataUsageInfo> testDataUsageInfo =
              (List<? extends TestDataUsageInfo>) query(xmomPersMgmt, correlatedXynaOrder, new SelectionMask(roottype, (List<String>) null),
                                                        findReservedButNotUsedEntry, queryParams);

          if (testDataUsageInfo.size() <= 0) {

            unusedTestDataEntry.setUsed(true);
            store(xmomPersMgmt, correlatedXynaOrder, unusedTestDataEntry, new StoreParameter(false, false, false));
            testData = unusedTestDataEntry;
            break;
          }
        }
        if (testData == null) {
          throw new RuntimeException("No unreserved TestData available for this order.");
        }
        return new XynaObjectList<>(Storable.class, testData);
      }
    }
  }


  private static String calculateNotUsedFilterString(xnwh.persistence.FilterCondition residualFilter) {
    String notUsedFilter = residualFilter != null ? residualFilter.getFormula() : null;
    if (notUsedFilter == null || notUsedFilter.length() <= 0) {
      notUsedFilter = "%0%.used!=\"true\"";
    } else {
      notUsedFilter += " && %0%.used!=\"true\"";
    }
    return notUsedFilter;
  }


  private static void store(XMOMPersistenceManagement xmomPersMgmt, XynaOrderServerExtension correlatedXynaOrder, XynaObject storable,
                            StoreParameter storeParameter)
      throws XMOMStorableAccessException {
    try {
      xmomPersMgmt.store(correlatedXynaOrder, storable, storeParameter);
    } catch (PersistenceLayerException e) {
      throw new XMOMStorableAccessException(storable.getClass().getName(), e);
    }
  }


  private static boolean typeCheck(String rootType, Long revision) {
    try {
      String fqClassName = GenerationBase.transformNameForJava(rootType);
      MDMClassLoader cl = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
          .getMDMClassLoader(fqClassName, revision, true);
      Class<?> clazz = cl.loadClass(fqClassName);
      return TestData.class.isAssignableFrom(clazz);
    } catch (XFMG_MDMObjectClassLoaderNotFoundException | XPRC_InvalidPackageNameException | ClassNotFoundException e) {
      return false;
    }
  }


  public static void notifyTestCaseExecutionService(XynaOrderServerExtension correlatedXynaOrder, TestDataGenerationID genId)
      throws XMOMStorableAccessException, TestDataNotFound, TestDataHasAlreadyBeenUsed {

    XMOMPersistenceManagement xmomPersMgmt =
        XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();

    List<String> selectedColumnsUsage = new ArrayList<>();
    selectedColumnsUsage.add("%0%.iD");
    selectedColumnsUsage.add("%0%.testDataID");
    selectedColumnsUsage.add("%0%.occupyingTestCaseID");
    selectedColumnsUsage.add("%0%.generationID");
    selectedColumnsUsage.add("%0%.tDMetaDataID");
    selectedColumnsUsage.add("%0%.storableRootType");

    @SuppressWarnings("unchecked")
    List<TestDataUsageInfo> testDataUsageObjects =
        (List<TestDataUsageInfo>) query(xmomPersMgmt, correlatedXynaOrder,
                                        new SelectionMask(TestDataUsageInfo.class.getName(), selectedColumnsUsage),
                                        "%0%.generationID==\"" + genId.getID() + "\"", new QueryParameter(-1, false, null));

    if (testDataUsageObjects == null || testDataUsageObjects.size() == 0) {
      // no test data has been reserved, job is done
      return;
    }

    SortedMap<Long, Object> relevantLocks = new TreeMap<>();
    for (TestDataUsageInfo nextUsageInfo : testDataUsageObjects) {

      Object testDataLock = new Object();
      // Theoretically it is possible that the lock is not in the map here if the service has been redeployed or the server has been
      // restarted. For these cases recreate the lock lazily.
      Object existingLock = testDataLocks.putIfAbsent(nextUsageInfo.getTDMetaDataID(), testDataLock);
      if (existingLock != null) {
        testDataLock = existingLock;
      }

      relevantLocks.put(nextUsageInfo.getTDMetaDataID(), testDataLock);

    }

    List<String> selectedColumnsTestData = new ArrayList<>();
    selectedColumnsUsage.add("%0%.*");
    for (TestDataUsageInfo nextUsageInfo : testDataUsageObjects) {

      Object relevantLock = relevantLocks.get(nextUsageInfo.getTDMetaDataID());

      // TODO make sure that under heavy load situations it is not an issue that the query will probably
      //      internally access a connection pool which is then a possible deadlock situation. Currently
      //      there does not seem to exist an easy way around this.
      synchronized (relevantLock) {

        @SuppressWarnings("unchecked")
        List<? extends TestData> reservedTestDataObject =
            (List<? extends TestData>) query(xmomPersMgmt, correlatedXynaOrder,
                                             new SelectionMask(nextUsageInfo.getStorableRootType(), selectedColumnsTestData),
                                             "%0%.iD==\"" + nextUsageInfo.getTestDataID() + "\"", new QueryParameter(1, false, null));

        if (reservedTestDataObject == null || reservedTestDataObject.size() == 0) {
          throw new TestDataNotFound(nextUsageInfo.getStorableRootType());
        }

        TestData foundTestData = reservedTestDataObject.get(0);

        if (foundTestData.getUsed()) {
          throw new TestDataHasAlreadyBeenUsed(nextUsageInfo.getStorableRootType());
        }

        foundTestData.setUsed(true);

        try {
          xmomPersMgmt.store(correlatedXynaOrder, foundTestData, new StoreParameter(false, false, false));
          xmomPersMgmt.delete(correlatedXynaOrder, nextUsageInfo, new DeleteParameter(true));
        } catch (PersistenceLayerException e) {
          throw new XMOMStorableAccessException("Failed to store updated test data object (id " + foundTestData.getID() + ")", e);
        }

      }

    }

  }


  public static List<? extends XynaObject> query(XMOMPersistenceManagement xmomPersMgmt, XynaOrderServerExtension correlatedXynaOrder,
                                                 xnwh.persistence.SelectionMask selectionMask, String filter,
                                                 xnwh.persistence.QueryParameter queryParameter)
      throws XMOMStorableAccessException {
    return query(xmomPersMgmt, correlatedXynaOrder, selectionMask, filter, convertQueryParameter(queryParameter));
  }


  public static List<? extends XynaObject> query(XMOMPersistenceManagement xmomPersMgmt, XynaOrderServerExtension correlatedXynaOrder,
                                                 xnwh.persistence.SelectionMask selectionMask, String filter, QueryParameter queryParameter)
      throws XMOMStorableAccessException {
    return query(xmomPersMgmt, correlatedXynaOrder, convertSelectionMask(selectionMask), filter, queryParameter);
  }


  public static List<? extends XynaObject> query(XMOMPersistenceManagement xmomPersMgmt, XynaOrderServerExtension correlatedXynaOrder,
                                                 SelectionMask selectionMask, String filter, QueryParameter queryParameter)
      throws XMOMStorableAccessException {
    try {
      return xmomPersMgmt.query(correlatedXynaOrder, selectionMask, new FilterCondition(filter), queryParameter,
                                getRevision(correlatedXynaOrder));
    } catch (PersistenceLayerException e) {
      throw new XMOMStorableAccessException(filter, e);
    }
  }


  public static class FilterCondition implements IFormula {

    public final static FilterCondition EMPTY = new FilterCondition("");

    private String filterString;


    FilterCondition(String filter) {
      filterString = filter;
    }


    public List<Accessor> getValues() {
      return Collections.emptyList();
    }


    public String getFormula() {
      return filterString;
    }

  }


  private static com.gip.xyna.xnwh.persistence.xmom.QueryParameter convertQueryParameter(xnwh.persistence.QueryParameter queryParameter) {
    return new com.gip.xyna.xnwh.persistence.xmom.QueryParameter(queryParameter.getMaxObjects(), queryParameter.getQueryHistory(),
                                                                 convertSortCriterions(queryParameter.getSortCriterion()));
  }


  private static SortCriterion[] convertSortCriterions(List<? extends xnwh.persistence.SortCriterion> sortCriterion) {
    if (sortCriterion == null || sortCriterion.size() == 0) {
      return null;
    }
    SortCriterion[] ret = new SortCriterion[sortCriterion.size()];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = convertSortCriterion(sortCriterion.get(i));
    }
    return ret;
  }


  private static SortCriterion convertSortCriterion(xnwh.persistence.SortCriterion sortCriterion) {
    return new SortCriterion(sortCriterion.getCriterion(), sortCriterion.getReverse());
  }


  private static SelectionMask convertSelectionMask(xnwh.persistence.SelectionMask mask) {
    return new SelectionMask(mask.getRootType(), mask.getColumns());
  }


  public static Long getRevision(XynaOrderServerExtension xose) {
    return xose.getRootOrder().getRevision();
  }

}
