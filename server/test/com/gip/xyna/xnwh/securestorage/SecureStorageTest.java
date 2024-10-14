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
package com.gip.xyna.xnwh.securestorage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyStorable;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.javaserialization.XynaJavaSerializationPersistenceLayer;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer.TransactionMode;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xsched.SchedulerBean;


public class SecureStorageTest extends TestCase {

  ODS ods;
  SecureStorage secStore;
  
  protected void setUp() throws Exception {
    super.setUp();
    
    ods = ODSImpl.getInstance(false);
    long persistenceLayerId = 42;
    ods.registerPersistenceLayer(persistenceLayerId, XMLPersistenceLayer.class);
    long instanceId = ods
                    .instantiatePersistenceLayerInstance(persistenceLayerId, XynaFactoryManagement.DEFAULT_NAME,
                                                         ODSConnectionType.DEFAULT, new String[] {"Configuration", TransactionMode.FULL_TRANSACTION.name(), "false"});
    ods.setPersistenceLayerForTable(instanceId, XynaPropertyStorable.TABLE_NAME, null);
    ods.registerStorable(XynaPropertyStorable.class);
    
    XynaProcessingODS xprcODS = EasyMock.createMock(XynaProcessingODS.class);
    EasyMock.expect(xprcODS.getODS()).andReturn(ods).anyTimes();
    
    XynaProcessingBase xproc = EasyMock.createMock(XynaProcessingBase.class);
    EasyMock.expect(xproc.getXynaProcessingODS()).andReturn(xprcODS).anyTimes();
    
    Configuration configuration = new Configuration();
    
    XynaFactoryManagementODS xfmods = EasyMock.createMock(XynaFactoryManagementODS.class);
    EasyMock.expect(xfmods.getConfiguration()).andReturn(configuration).anyTimes();
    
    DependencyRegister depReg = EasyMock.createMock(DependencyRegister.class);
    EasyMock.expect(depReg.addDependency(EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class),
                         EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class))).andReturn(true).anyTimes();
    EasyMock.expectLastCall();
    
    XynaFactoryControl xfctrl = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfctrl.getDependencyRegister()).andReturn(depReg).anyTimes();
    
    XynaFactoryManagementBase xfm = EasyMock.createMock(XynaFactoryManagementBase.class);
    EasyMock.expect(xfm.getXynaFactoryManagementODS()).andReturn(xfmods).anyTimes();
    EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfctrl).anyTimes();
    
    XynaFactoryBase xf = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(xf.getProcessing()).andReturn(xproc).anyTimes();
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfm).anyTimes();
    EasyMock.expect(xf.getFactoryManagementPortal()).andReturn(xfm).anyTimes();
    xf.addComponentToBeInitializedLater(EasyMock.isA(SecureStorage.class));
    EasyMock.expectLastCall();
    xf.addComponentToBeInitializedLater(EasyMock.isA(Configuration.class));
    EasyMock.expectLastCall();
    
    XynaFactory.setInstance(xf);
    
    EasyMock.replay(xprcODS, xproc, xfmods, depReg, xfctrl, xfm, xf);
    
    configuration.init();
    configuration.setProperty(SecureStorage.PROP_CACHE, "true");

    ods.registerStorable(SecuredStorable.class);
    ods.registerPersistenceLayer(43, XynaJavaSerializationPersistenceLayer.class);
    long id = ods.instantiatePersistenceLayerInstance(ods.getJavaPersistenceLayerID(), "test",
                                                      ODSConnectionType.DEFAULT, new String[0]);
    ods.setPersistenceLayerForTable(id, new SecuredStorable().getTableName(), null);
    
    secStore = SecureStorage.getInstance();
  }


  protected void tearDown() throws Exception {
    secStore.setInstance(null);
    ODSImpl.clearInstances();
    super.tearDown();
  }

  public final static String TESTDEST = "test.xnwh.securestorage";
  
  public void testPrimitives() throws PersistenceLayerException {      
    /*
     * String
     */
    final String TESTSTRINGKEY = "testString";
    final String TESTSTRINGDATA = "Test-Inhalt";
    
    String testString = TESTSTRINGDATA;
    
    assertTrue(secStore.store(TESTDEST, TESTSTRINGKEY, testString));
    
    String testRetrievalString = null;
    testRetrievalString = (String) secStore.retrieve(TESTDEST, TESTSTRINGKEY);
    
    assertNotNull(testRetrievalString);
    assertEquals(TESTSTRINGDATA, testRetrievalString);
    
    assertTrue(secStore.remove(TESTDEST, TESTSTRINGKEY));
    
    /*
     * Int
     */
    final String TESTINTKEY = "testInt";
    final int TESTINTDATA = 12345678;
    
    Integer testInt = TESTINTDATA;
    
    assertTrue(secStore.store(TESTDEST, TESTINTKEY, testInt));
    
    int testRetrievalInt = 0;
    testRetrievalInt = (Integer) secStore.retrieve(TESTDEST, TESTINTKEY);
    
    assertNotNull(testRetrievalInt);
    assertEquals(TESTINTDATA, testRetrievalInt);
    
    assertTrue(secStore.remove(TESTDEST, TESTINTKEY));
    
    /*
     * Boolean
     */
    final String TESTBOOLKEY = "testBool";
    final boolean TESTBOOLDATA = true;
    
    boolean testBool = TESTBOOLDATA;
    
    assertTrue(secStore.store(TESTDEST, TESTBOOLKEY, testBool));
    
    boolean testRetrievalBool = false;
    testRetrievalBool = (Boolean) secStore.retrieve(TESTDEST, TESTBOOLKEY);
    
    assertNotNull(testRetrievalBool);
    assertEquals(TESTBOOLDATA, testRetrievalBool);
    
    assertTrue(secStore.remove(TESTDEST, TESTBOOLKEY));
  }
  
  
  public void testComplex() throws  PersistenceLayerException {    
    /*
     * ArrayList
     */
    final String TESTLISTKEY = "testArrayList";
    final String LISTELEM1 = "Erstes Element";
    final String LISTELEM2 = "Zweites Element";
    final String LISTELEM3 = "Drittes Element";
    
    ArrayList<String> testArrayList = new ArrayList<String>();
    testArrayList.add(LISTELEM1);
    testArrayList.add(LISTELEM2);
    testArrayList.add(LISTELEM3);
    
    assertTrue(secStore.store(TESTDEST, TESTLISTKEY, testArrayList));
    
    ArrayList<String> testRetrievalList = null;
    testRetrievalList = (ArrayList<String>) secStore.retrieve(TESTDEST, TESTLISTKEY);
    
    assertNotNull(testRetrievalList);
    assertEquals(LISTELEM1, testRetrievalList.get(0));
    assertEquals(LISTELEM2, testRetrievalList.get(1));
    assertEquals(LISTELEM3, testRetrievalList.get(2));
    
    assertTrue(secStore.remove(TESTDEST, TESTLISTKEY));
    
    /*
     * HashMap
     */
    final String TESTMAPKEY = "testHashMap";
    final String TESTMAPKEY1 = "testKey1";
    final String TESTMAPELEM1 = "Erstes Element";
    final String TESTMAPKEY2 = "testKey2";
    final String TESTMAPELEM2 = "Zweites Element";
    final String TESTMAPKEY3 = "testKey3";
    final String TESTMAPELEM3 = "Drittes Element";
    
    HashMap<String, String> testHashMap = new HashMap<String, String>();
    testHashMap.put(TESTMAPKEY1, TESTMAPELEM1);
    testHashMap.put(TESTMAPKEY2, TESTMAPELEM2);
    testHashMap.put(TESTMAPKEY3, TESTMAPELEM3);
    
    assertTrue(secStore.store(TESTDEST, TESTMAPKEY, testHashMap));
    
    HashMap<String, String> testRetrievalMap = null;
    testRetrievalMap = (HashMap<String, String>) secStore.retrieve(TESTDEST, TESTMAPKEY);
    assertNotNull(testRetrievalMap);
    assertEquals(TESTMAPELEM1, testRetrievalMap.get(TESTMAPKEY1));
    assertEquals(TESTMAPELEM2, testRetrievalMap.get(TESTMAPKEY2));
    assertEquals(TESTMAPELEM3, testRetrievalMap.get(TESTMAPKEY3));
    
    assertTrue(secStore.remove(TESTDEST, TESTMAPKEY));
        
    /*
     * SchedulerBean
     */
    final String TESTBEANKEY = "schedBeanKey";
    final String CAP1NAME = "testCap1";
    final int CAP1CARD = 30;
    final String CAP2NAME = "testCap2";
    final int CAP2CARD = 20;
    final String CAP3NAME = "testCap3";
    final int CAP3CARD = 10;
    
    List<Capacity> testCapList= new ArrayList<Capacity>();
    testCapList.add(new Capacity(CAP1NAME, CAP1CARD));
    testCapList.add(new Capacity(CAP2NAME, CAP2CARD));
    testCapList.add(new Capacity(CAP3NAME, CAP3CARD)); 
    
    SchedulerBean testSchedB = new SchedulerBean(testCapList);
    
    assertTrue(secStore.store(TESTDEST, TESTBEANKEY, testSchedB));
    
    SchedulerBean testRetrievalBean = null;
    testRetrievalBean = (SchedulerBean) secStore.retrieve(TESTDEST, TESTBEANKEY);
    assertNotNull(testRetrievalBean);
    List<? extends Capacity> testRetrievalCaps = testRetrievalBean.getCapacities();
    assertEquals(CAP1NAME, testRetrievalCaps.get(0).getCapName());
    assertEquals(CAP2NAME, testRetrievalCaps.get(1).getCapName());
    assertEquals(CAP3NAME, testRetrievalCaps.get(2).getCapName());
    assertEquals(CAP1CARD, testRetrievalCaps.get(0).getCardinality());
    assertEquals(CAP2CARD, testRetrievalCaps.get(1).getCardinality());
    assertEquals(CAP3CARD, testRetrievalCaps.get(2).getCardinality());
       
    assertTrue(secStore.remove(TESTDEST, TESTBEANKEY));
  }
  
  
  public final static String TESTBEANKEY = "schedBeanKey";
  public final static String CAP1NAME = "testCap1";
  public final static int CAP1CARD = 30;
  public final static String CAP2NAME = "testCap2";
  public final static int CAP2CARD = 20;
  public final static String CAP3NAME = "testCap3";
  public final static int CAP3CARD = 10;

  // because the secStore is created in the setUp (and nulled at tearDown) every testMethod it will behave like it does at a restart
  public void testPersistencePart1Store() throws PersistenceLayerException {    
    List<Capacity> testCapList= new ArrayList<Capacity>();
    testCapList.add(new Capacity(CAP1NAME, CAP1CARD));
    testCapList.add(new Capacity(CAP2NAME, CAP2CARD));
    testCapList.add(new Capacity(CAP3NAME, CAP3CARD)); 
    
    SchedulerBean testSchedB = new SchedulerBean(testCapList);
    
    assertTrue(secStore.store(TESTDEST, TESTBEANKEY, testSchedB));
  }
  
  public void testPersistencePart2Retrieve() {    
    SchedulerBean testRetrievalBean = null;
    testRetrievalBean = (SchedulerBean) secStore.retrieve(TESTDEST, TESTBEANKEY);
    assertNotNull(testRetrievalBean);
    List<? extends Capacity> testRetrievalCaps = testRetrievalBean.getCapacities();
    assertEquals(CAP1NAME, testRetrievalCaps.get(0).getCapName());
    assertEquals(CAP2NAME, testRetrievalCaps.get(1).getCapName());
    assertEquals(CAP3NAME, testRetrievalCaps.get(2).getCapName());
    assertEquals(CAP1CARD, testRetrievalCaps.get(0).getCardinality());
    assertEquals(CAP2CARD, testRetrievalCaps.get(1).getCardinality());
    assertEquals(CAP3CARD, testRetrievalCaps.get(2).getCardinality());
  }
  
  public void testPersistencePart3Delete() throws PersistenceLayerException {    
    assertTrue(secStore.remove(TESTDEST, TESTBEANKEY));
  }
  
  
  public void testBehaviourWithUnstoredObject() throws PersistenceLayerException {
    final String NONEXISTENTDEST = "somewhere";
    final String EXISTENTDEST = "test.xnwh.securestorage";
    final String NONEXISTENTKEY = "somewhat";
    final String EXISTENTKEY = "testObj";
    
    final String EXISTENTCONTENT = "testContent";
    
    String testObj = new String(EXISTENTCONTENT);
    
    assertTrue(secStore.store(EXISTENTDEST, EXISTENTKEY, testObj));
    
    String testRetrievalString = null;
    testRetrievalString = (String) secStore.retrieve(NONEXISTENTDEST, EXISTENTKEY);
    assertNull(testRetrievalString);
    
    testRetrievalString = null;
    testRetrievalString = (String) secStore.retrieve(EXISTENTDEST, NONEXISTENTKEY);
    assertNull(testRetrievalString);
    
    testRetrievalString = null;
    testRetrievalString = (String) secStore.retrieve(NONEXISTENTDEST, NONEXISTENTKEY);
    assertNull(testRetrievalString);
    
    testRetrievalString = null;
    testRetrievalString = (String) secStore.retrieve(EXISTENTDEST, EXISTENTKEY);
    assertNotNull(testRetrievalString);
    assertEquals(EXISTENTCONTENT, testRetrievalString);
    
    assertTrue(secStore.remove(EXISTENTDEST, EXISTENTKEY));
    
    testRetrievalString = null;
    testRetrievalString = (String) secStore.retrieve(EXISTENTDEST, EXISTENTKEY);
    assertNull(testRetrievalString);    
    
    assertFalse(secStore.remove(EXISTENTDEST, EXISTENTKEY));
  }
  
  
  public void testPersistedFileForText() throws PersistenceLayerException, IOException {
    final String TESTKEY = "testText";
    final String TESTCONTENT = "Test Content Test Content Test Content Test Content";
    final String VARNAMEINFILE = "encryptedData";
    
    String testObj = new String(TESTCONTENT);
    
    assertTrue(secStore.store(TESTDEST, TESTKEY, testObj));
    
    final String PATH = "./persdir/securestorage/securestorage";
    
    BufferedReader in = new BufferedReader(new FileReader(PATH));

    if (!in.ready()) {
        throw new IOException();
    }
    Collection<String> file = new ArrayList<String>();
    String line = null;
    while ((line = in.readLine()) != null) {
        file.add(line);
    }
       
    boolean containsVarName = false;
    boolean containsTestContent = false;
    for (String str : file) {
      if (str.contains(VARNAMEINFILE)) {
        containsVarName = true;
      }
      if (str.contains("Test Content")) {
        containsVarName = true;
      }
    }
    
    // the name of the variable 'encryptedData' should be human readable in the file
    assertTrue(containsVarName);
    // the encrypted content shouldn't
    assertFalse(containsTestContent);
    
    assertTrue(secStore.remove(TESTDEST, TESTKEY));
  }
  

  public void testOverwrite() throws PersistenceLayerException {
    List<Capacity> testCapList= new ArrayList<Capacity>();
    testCapList.add(new Capacity(CAP1NAME, CAP1CARD));
    testCapList.add(new Capacity(CAP2NAME, CAP2CARD));
    testCapList.add(new Capacity(CAP3NAME, CAP3CARD)); 
    
    SchedulerBean testSchedB = new SchedulerBean(testCapList);
    
    assertTrue(secStore.store(TESTDEST, TESTBEANKEY, testSchedB));
    
    SchedulerBean testRetrievalBean = null;
    testRetrievalBean = (SchedulerBean) secStore.retrieve(TESTDEST, TESTBEANKEY);
    assertNotNull(testRetrievalBean);
    List<? extends Capacity> testRetrievalCaps = testRetrievalBean.getCapacities();
    assertEquals(CAP3NAME, testRetrievalCaps.get(2).getCapName());
    assertEquals(CAP3CARD, testRetrievalCaps.get(2).getCardinality());
    
    final String CAP3NEWNAME = "supersizedTestCap3";
    final int CAP3NEWCARD = 100;
    
    List<Capacity> testNewCapList= new ArrayList<Capacity>();
    testNewCapList.add(new Capacity(CAP1NAME, CAP1CARD));
    testNewCapList.add(new Capacity(CAP2NAME, CAP2CARD));
    testNewCapList.add(new Capacity(CAP3NEWNAME, CAP3NEWCARD)); 
    
    SchedulerBean testNewSchedB = new SchedulerBean(testNewCapList);
    
    assertTrue(secStore.store(TESTDEST, TESTBEANKEY, testNewSchedB));
    
    testRetrievalBean = null;
    testRetrievalBean = (SchedulerBean) secStore.retrieve(TESTDEST, TESTBEANKEY);
    assertNotNull(testRetrievalBean);
    testRetrievalCaps = testRetrievalBean.getCapacities();
    assertEquals(CAP3NEWNAME, testRetrievalCaps.get(2).getCapName());
    assertEquals(CAP3NEWCARD, testRetrievalCaps.get(2).getCardinality());
    
    assertTrue(secStore.remove(TESTDEST, TESTBEANKEY));
  }
}
