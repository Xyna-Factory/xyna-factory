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
package com.gip.xyna.xfmg.xfctrl.deployitem;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagementImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.Inconsistency;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ResolutionFailure;
import com.gip.xyna.xfmg.xfctrl.deploystate.DisplayState;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.formula.Functions;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;


public class TestCreationHints extends TestDeploymentItemBuildSetup {

private DeploymentItemRegistry registry;

private final static String CREATION_HINT_ITEM_TEST_PATH = "bg.test.deployitem.creationhints";
private final static String MISSING_DT_NAME = "MissingDT";
private final static String MISSING_DT_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_DT_NAME;
private final static String SUB_DT_NAME = "SubDT";
private final static String SUB_DT_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + SUB_DT_NAME;
private final static String MISSING_DT_USER1_NAME = "DTUser1";
private final static String MISSING_DT_USER1_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_DT_USER1_NAME;
private final static String MISSING_DT_USER2_NAME = "DTUser2";
private final static String MISSING_DT_USER2_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_DT_USER2_NAME;
private final static String MISSING_DT_USER3_NAME = "DTUser3";
private final static String MISSING_DT_USER3_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_DT_USER3_NAME;
private final static String MISSING_WF_NAME = "MissingWF";
private final static String MISSING_WF_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_WF_NAME;
private final static String INPUT_TYPE_1_NAME = "InputType1";
private final static String INPUT_TYPE_1_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + INPUT_TYPE_1_NAME;
private final static String INPUT_TYPE_2_NAME = "InputType2";
private final static String INPUT_TYPE_2_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + INPUT_TYPE_2_NAME;
private final static String MISSING_WF_USER1_NAME = "WFUser1";
private final static String MISSING_WF_USER1_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_WF_USER1_NAME;
private final static String MISSING_WF_USER2_NAME = "WFUser2";
private final static String MISSING_WF_USER2_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_WF_USER2_NAME;
private final static String MISSING_WF_USER3_NAME = "WFUser3";
private final static String MISSING_WF_USER3_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_WF_USER3_NAME;
private final static String MISSING_ISG_NAME = "MissingInstanceServiceGroup";
private final static String MISSING_ISG_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_ISG_NAME;
private final static String ISG_INPUT_TYPE_1_NAME = "InstanceServiceEmployedType1";
private final static String ISG_INPUT_TYPE_1_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + ISG_INPUT_TYPE_1_NAME;
private final static String ISG_INPUT_TYPE_2_NAME = "InstanceServiceEmployedType2";
private final static String ISG_INPUT_TYPE_2_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + ISG_INPUT_TYPE_2_NAME;
private final static String MISSING_ISG_USER1_NAME = "ISGUser1";
private final static String MISSING_ISG_USER1_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_ISG_USER1_NAME;
private final static String MISSING_ISG_USER2_NAME = "ISGUser2";
private final static String MISSING_ISG_USER2_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_ISG_USER2_NAME;
private final static String MISSING_ISG_USER3_NAME = "ISGUser3";
private final static String MISSING_ISG_USER3_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_ISG_USER3_NAME;
private final static String MISSING_EX_NAME = "MissingException";
private final static String MISSING_EX_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_EX_NAME;
private final static String MISSING_EX_USER1_NAME = "MissingExceptionUser1";
private final static String MISSING_EX_USER1_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_EX_USER1_NAME;
private final static String MISSING_EX_USER2_NAME = "MissingExceptionUser2";
private final static String MISSING_EX_USER2_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_EX_USER2_NAME;
private final static String MISSING_EX_USER3_NAME = "MissingExceptionUser3";
private final static String MISSING_EX_USER3_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_EX_USER3_NAME;
private final static String BASE_TYPE_NAME = "BaseType";
private final static String BASE_TYPE_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + BASE_TYPE_NAME;
private final static String SUB_TYPE_1_NAME = "SubType1";
private final static String SUB_TYPE_1_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + SUB_TYPE_1_NAME;
private final static String SUB_TYPE_2_NAME = "SubType2";
private final static String SUB_TYPE_2_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + SUB_TYPE_2_NAME;
private final static String MISSING_BASE_IN_WF_NAME = "MissingWorkflowWithBaseInput";
private final static String MISSING_BASE_IN_WF_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_BASE_IN_WF_NAME;
private final static String MISSING_BASE_WF_USER_1_NAME = "MissingBaseInputWFUser1";
private final static String MISSING_BASE_WF_USER_1_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_BASE_WF_USER_1_NAME;
private final static String MISSING_BASE_WF_USER_2_NAME = "MissingBaseInputWFUser2";
private final static String MISSING_BASE_WF_USER_2_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_BASE_WF_USER_2_NAME;
private final static String MISSING_BASE_WF_USER_3_NAME = "MissingBaseInputWFUser3";
private final static String MISSING_BASE_WF_USER_3_FQNAME = CREATION_HINT_ITEM_TEST_PATH + "." + MISSING_BASE_WF_USER_3_NAME;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(XYNA_EXCEPTION_FQNAME, XYNA_EXCEPTION_XML);
    testXMOM.put(XYNA_EXCEPTION_BASE_FQNAME, XYNA_EXCEPTION_BASE_XML);
    testXMOM.put(MISSING_EX_FQNAME, MISSING_EXCEPTION_XML);
    setupWorkspace(testXMOM);
    registry = new DeploymentItemStateRegistry(TEST_REVISION);
    /*DeploymentItemStateManagementImpl.reservedObjects = new DeploymentItemStateRegistry();
    DeploymentItemStateManagementImpl.reservedObjects.save(EXCEPTION_FQNAME);
    DeploymentItemStateManagementImpl.reservedObjects.save(XYNA_EXCEPTION_FQNAME);
    DeploymentItemStateManagementImpl.reservedObjects.save(XYNA_EXCEPTION_BASE_FQNAME);*/
  }
  
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  protected DeploymentItemRegistry getRegistry() {
    return registry;
  }
  
  @Test
  public void testSetupXMLs() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(MISSING_DT_FQNAME, MISSING_DT_XML);
    testXMOM.put(SUB_DT_FQNAME, SUB_DT_XML);
    testXMOM.put(MISSING_DT_USER1_FQNAME, MISSING_DT_USER1_XML);
    testXMOM.put(MISSING_DT_USER2_FQNAME, MISSING_DT_USER2_XML);
    testXMOM.put(MISSING_DT_USER3_FQNAME, MISSING_DT_USER3_XML);
    testXMOM.put(SUB_DT_FQNAME, SUB_DT_XML);
    testXMOM.put(MISSING_WF_FQNAME, MISSING_WF_XML);
    testXMOM.put(INPUT_TYPE_1_FQNAME, INPUT_TYPE_1_XML);
    testXMOM.put(INPUT_TYPE_2_FQNAME, INPUT_TYPE_2_XML);
    testXMOM.put(MISSING_WF_USER1_FQNAME, WF_USER_1_XML);
    testXMOM.put(MISSING_WF_USER2_FQNAME, WF_USER_2_XML);
    testXMOM.put(MISSING_WF_USER3_FQNAME, WF_USER_3_XML);
    testXMOM.put(MISSING_ISG_FQNAME, MISSING_INSTANCE_SERVICE_GROUP_XML);
    testXMOM.put(ISG_INPUT_TYPE_1_FQNAME, INSTANCE_INPUT_TYPE_1_XML);
    testXMOM.put(ISG_INPUT_TYPE_2_FQNAME, INSTANCE_INPUT_TYPE_2_XML);
    testXMOM.put(MISSING_ISG_USER1_FQNAME, ISG_USER_1_XML);
    testXMOM.put(MISSING_ISG_USER2_FQNAME, ISG_USER_2_XML);
    testXMOM.put(MISSING_ISG_USER3_FQNAME, ISG_USER_3_XML);
    testXMOM.put(DOCUMENT_PART_FQNAME, DOCUMENT_PART_XML);
    testXMOM.put(DOCUMENT_FQNAME, DOCUMENT_XML);
    testXMOM.put(TEMPLATE_MANAGEMENT_FQNAME, TEMPLATE_MANAGEMENT_XML);
    testXMOM.put(MISSING_EX_FQNAME, MISSING_EXCEPTION_XML);
    testXMOM.put(MISSING_EX_USER1_FQNAME, MISSING_EXCEPTION_USER_1_XML);
    testXMOM.put(MISSING_EX_USER2_FQNAME, MISSING_EXCEPTION_USER_2_XML);
    testXMOM.put(MISSING_EX_USER3_FQNAME, MISSING_EXCEPTION_USER_3_XML);
    testXMOM.put(BASE_TYPE_FQNAME, BASE_TYPE_XML);
    testXMOM.put(SUB_TYPE_1_FQNAME, SUB_TYPE_1_XML);
    testXMOM.put(SUB_TYPE_2_FQNAME, SUB_TYPE_2_XML);
    testXMOM.put(MISSING_BASE_IN_WF_FQNAME, MISSING_BASE_INPUT_WF_XML);
    testXMOM.put(MISSING_BASE_WF_USER_1_FQNAME, MISSING_BASE_INPUT_WF_USER_1_XML);
    testXMOM.put(MISSING_BASE_WF_USER_2_FQNAME, MISSING_BASE_INPUT_WF_USER_2_XML);
    testXMOM.put(MISSING_BASE_WF_USER_3_FQNAME, MISSING_BASE_INPUT_WF_USER_3_XML);
    setupWorkspace(testXMOM);
    save(MISSING_DT_FQNAME, SUB_DT_FQNAME, MISSING_DT_USER1_FQNAME, MISSING_DT_USER2_FQNAME, MISSING_DT_USER3_FQNAME,
         MISSING_WF_FQNAME, INPUT_TYPE_1_FQNAME, INPUT_TYPE_2_FQNAME, MISSING_WF_USER1_FQNAME, MISSING_WF_USER2_FQNAME, MISSING_WF_USER3_FQNAME,
         MISSING_ISG_FQNAME, ISG_INPUT_TYPE_1_FQNAME, ISG_INPUT_TYPE_2_FQNAME, MISSING_ISG_USER1_FQNAME, MISSING_ISG_USER2_FQNAME, MISSING_ISG_USER3_FQNAME,
         DOCUMENT_PART_FQNAME, DOCUMENT_FQNAME, TEMPLATE_MANAGEMENT_FQNAME,
         MISSING_EX_FQNAME, MISSING_EX_USER1_FQNAME, MISSING_EX_USER2_FQNAME, MISSING_EX_USER3_FQNAME,
         BASE_TYPE_FQNAME, SUB_TYPE_1_FQNAME, SUB_TYPE_2_FQNAME,
         MISSING_BASE_IN_WF_FQNAME, MISSING_BASE_WF_USER_1_FQNAME, MISSING_BASE_WF_USER_2_FQNAME, MISSING_BASE_WF_USER_3_FQNAME);
    deploy(MISSING_DT_FQNAME, SUB_DT_FQNAME, MISSING_DT_USER1_FQNAME, MISSING_DT_USER2_FQNAME, MISSING_DT_USER3_FQNAME,
           MISSING_WF_FQNAME, INPUT_TYPE_1_FQNAME, INPUT_TYPE_2_FQNAME, MISSING_WF_USER1_FQNAME, MISSING_WF_USER2_FQNAME, MISSING_WF_USER3_FQNAME,
           MISSING_ISG_FQNAME, ISG_INPUT_TYPE_1_FQNAME, ISG_INPUT_TYPE_2_FQNAME, MISSING_ISG_USER1_FQNAME, MISSING_ISG_USER2_FQNAME, MISSING_ISG_USER3_FQNAME,
           DOCUMENT_PART_FQNAME, DOCUMENT_FQNAME, TEMPLATE_MANAGEMENT_FQNAME,
           MISSING_EX_FQNAME, MISSING_EX_USER1_FQNAME, MISSING_EX_USER2_FQNAME, MISSING_EX_USER3_FQNAME,
           BASE_TYPE_FQNAME, SUB_TYPE_1_FQNAME, SUB_TYPE_2_FQNAME,
           MISSING_BASE_IN_WF_FQNAME, MISSING_BASE_WF_USER_1_FQNAME, MISSING_BASE_WF_USER_2_FQNAME, MISSING_BASE_WF_USER_3_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_DT_USER1_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    for (Inconsistency inc : report.getInconsitencies()) {
      System.out.println(inc);
    }
    for (ResolutionFailure inc : report.getUnresolvable()) {
      System.out.println(inc);
    }
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_DT_USER2_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_DT_USER3_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_WF_USER1_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_WF_USER2_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_WF_USER3_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_ISG_USER1_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_ISG_USER2_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_ISG_USER3_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_EX_USER1_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_EX_USER2_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_EX_USER3_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_BASE_WF_USER_1_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_BASE_WF_USER_2_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    dis = registry.get(MISSING_BASE_WF_USER_3_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
  }
  
  
  
  @Test
  public void testDTUser2Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(SUB_DT_FQNAME, SUB_DT_XML);
    testXMOM.put(MISSING_DT_USER2_FQNAME, MISSING_DT_USER2_XML);
    setupWorkspace(testXMOM);
    save(SUB_DT_FQNAME, MISSING_DT_USER2_FQNAME);
    deploy(SUB_DT_FQNAME, MISSING_DT_USER2_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_DT_USER2_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_DT_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/DataType[@TypeName='" + MISSING_DT_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpVarCheck = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='aVar']");
    assertEquals(1, xpVarCheck.size());
    List<String> xpVarTypeCheck = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='aVar']/Meta/Type/text()");
    assertEquals(1, xpVarTypeCheck.size());
    assertEquals(String.class.getSimpleName(), xpVarTypeCheck.get(0));
  }
  
  
  @Test
  public void testDTUser3Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(SUB_DT_FQNAME, SUB_DT_XML);
    testXMOM.put(MISSING_DT_USER3_FQNAME, MISSING_DT_USER3_XML);
    setupWorkspace(testXMOM);
    save(SUB_DT_FQNAME, MISSING_DT_USER3_FQNAME);
    deploy(SUB_DT_FQNAME, MISSING_DT_USER3_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_DT_USER3_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_DT_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/DataType[@TypeName='" + MISSING_DT_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpVarCheck = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='subDT']");
    assertEquals(1, xpVarCheck.size());
    List<String> xpVarTypeCheck = Functions.fxpath(creationHint, "/DataType/Data[@ReferenceName='" + SUB_DT_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpVarTypeCheck.size());
  }
  
  
  @Test
  public void testDTUser1Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(SUB_DT_FQNAME, SUB_DT_XML);
    testXMOM.put(MISSING_DT_USER1_FQNAME, MISSING_DT_USER1_XML);
    setupWorkspace(testXMOM);
    save(SUB_DT_FQNAME, MISSING_DT_USER1_FQNAME);
    deploy(SUB_DT_FQNAME, MISSING_DT_USER1_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_DT_USER1_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_DT_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/DataType[@TypeName='" + MISSING_DT_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpVarCheck = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='subDT']");
    assertEquals(1, xpVarCheck.size());
    List<String> xpVarTypeCheck = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='subDT']/Meta/Type/text()");
    assertEquals(1, xpVarTypeCheck.size());
    assertEquals(String.class.getSimpleName(), xpVarTypeCheck.get(0));
    List<String> xpVarCheck2 = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='bVar']");
    assertEquals(1, xpVarCheck2.size());
    List<String> xpVarTypeCheck2 = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='bVar']/Meta/Type/text()");
    assertEquals(1, xpVarTypeCheck2.size());
    assertEquals(String.class.getSimpleName(), xpVarTypeCheck2.get(0));
  }
  
  
  @Test
  public void testDTUser123Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(SUB_DT_FQNAME, SUB_DT_XML);
    testXMOM.put(MISSING_DT_USER1_FQNAME, MISSING_DT_USER1_XML);
    testXMOM.put(MISSING_DT_USER2_FQNAME, MISSING_DT_USER2_XML);
    testXMOM.put(MISSING_DT_USER3_FQNAME, MISSING_DT_USER3_XML);
    setupWorkspace(testXMOM);
    save(SUB_DT_FQNAME, MISSING_DT_USER1_FQNAME, MISSING_DT_USER2_FQNAME, MISSING_DT_USER3_FQNAME);
    deploy(SUB_DT_FQNAME, MISSING_DT_USER1_FQNAME, MISSING_DT_USER2_FQNAME, MISSING_DT_USER3_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_DT_USER1_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_DT_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/DataType[@TypeName='" + MISSING_DT_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpVarCheck = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='bVar']");
    assertEquals(1, xpVarCheck.size());
    List<String> xpVarTypeCheck = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='bVar']/Meta/Type/text()");
    assertEquals(1, xpVarTypeCheck.size());
    assertEquals(String.class.getSimpleName(), xpVarTypeCheck.get(0));
    
    List<String> xpVarCheck2 = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='aVar']");
    assertEquals(1, xpVarCheck2.size());
    List<String> xpVarTypeCheck2 = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='aVar']/Meta/Type/text()");
    assertEquals(1, xpVarTypeCheck2.size());
    assertEquals(String.class.getSimpleName(), xpVarTypeCheck2.get(0));
    
    List<String> xpVarCheck3 = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='subDT']");
    assertEquals(1, xpVarCheck3.size());
    List<String> xpVarTypeCheck3 = Functions.fxpath(creationHint, "/DataType/Data[@ReferenceName='" + SUB_DT_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpVarTypeCheck3.size());
  }
  
  
  
  @Test
  public void testWFUser1Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(INPUT_TYPE_1_FQNAME, INPUT_TYPE_1_XML);
    testXMOM.put(INPUT_TYPE_2_FQNAME, INPUT_TYPE_2_XML);
    testXMOM.put(MISSING_WF_USER1_FQNAME, WF_USER_1_XML);
    setupWorkspace(testXMOM);
    save(INPUT_TYPE_1_FQNAME, INPUT_TYPE_2_FQNAME, MISSING_WF_USER1_FQNAME);
    deploy(INPUT_TYPE_1_FQNAME, INPUT_TYPE_2_FQNAME, MISSING_WF_USER1_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_WF_USER1_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_WF_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/Service[@TypeName='" + MISSING_WF_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpOperationCheck = Functions.fxpath(creationHint, "/Service/Operation[@Name='" + MISSING_WF_NAME + "']");
    assertEquals(1, xpOperationCheck.size());
    List<String> xpInputTypeCheck1 = Functions.fxpath(creationHint, "/Service/Operation/Input/Data[@ReferenceName='" + INPUT_TYPE_1_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpInputTypeCheck1.size());
    List<String> xpInputTypeCheck2 = Functions.fxpath(creationHint, "/Service/Operation/Input/Data[@ReferenceName='" + INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpInputTypeCheck2.size());
    List<String> xpOutputTypeCheck = Functions.fxpath(creationHint, "/Service/Operation/Output/Data[@ReferenceName='" + INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(2, xpOutputTypeCheck.size());
  }
  
  @Test
  public void testWFUser2Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(INPUT_TYPE_1_FQNAME, INPUT_TYPE_1_XML);
    testXMOM.put(INPUT_TYPE_2_FQNAME, INPUT_TYPE_2_XML);
    testXMOM.put(MISSING_WF_USER2_FQNAME, WF_USER_2_XML);
    setupWorkspace(testXMOM);
    save(INPUT_TYPE_1_FQNAME, INPUT_TYPE_2_FQNAME, MISSING_WF_USER2_FQNAME);
    deploy(INPUT_TYPE_1_FQNAME, INPUT_TYPE_2_FQNAME, MISSING_WF_USER2_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_WF_USER2_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_WF_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/Service[@TypeName='" + MISSING_WF_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpOperationCheck = Functions.fxpath(creationHint, "/Service/Operation[@Name='" + MISSING_WF_NAME + "']");
    assertEquals(1, xpOperationCheck.size());
    List<String> xpInputTypeCheck1 = Functions.fxpath(creationHint, "/Service/Operation/Input/Data[@ReferenceName='" + INPUT_TYPE_1_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpInputTypeCheck1.size());
    List<String> xpInputTypeCheck2 = Functions.fxpath(creationHint, "/Service/Operation/Input/Data[@ReferenceName='" + INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpInputTypeCheck2.size());
    List<String> xpOutputTypeCheck = Functions.fxpath(creationHint, "/Service/Operation/Output/Data[@ReferenceName='" + INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(2, xpOutputTypeCheck.size());
  }
  
  @Test
  public void testWFUser3Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(INPUT_TYPE_1_FQNAME, INPUT_TYPE_1_XML);
    testXMOM.put(INPUT_TYPE_2_FQNAME, INPUT_TYPE_2_XML);
    testXMOM.put(MISSING_WF_USER3_FQNAME, WF_USER_3_XML);
    setupWorkspace(testXMOM);
    save(INPUT_TYPE_1_FQNAME, INPUT_TYPE_2_FQNAME, MISSING_WF_USER3_FQNAME);
    deploy(INPUT_TYPE_1_FQNAME, INPUT_TYPE_2_FQNAME, MISSING_WF_USER3_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_WF_USER3_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_WF_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/Service[@TypeName='" + MISSING_WF_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpOperationCheck = Functions.fxpath(creationHint, "/Service/Operation[@Name='" + MISSING_WF_NAME + "']");
    assertEquals(1, xpOperationCheck.size());
    List<String> xpInputTypeCheck1 = Functions.fxpath(creationHint, "/Service/Operation/Input/Data[@ReferenceName='" + INPUT_TYPE_1_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpInputTypeCheck1.size());
    List<String> xpInputTypeCheck2 = Functions.fxpath(creationHint, "/Service/Operation/Input/Data[@ReferenceName='" + INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpInputTypeCheck2.size());
    List<String> xpOutputTypeCheck = Functions.fxpath(creationHint, "/Service/Operation/Output/Data[@ReferenceName='" + INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(2, xpOutputTypeCheck.size());
  }
  
  
  @Test
  public void testWFUser123Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(INPUT_TYPE_1_FQNAME, INPUT_TYPE_1_XML);
    testXMOM.put(INPUT_TYPE_2_FQNAME, INPUT_TYPE_2_XML);
    testXMOM.put(MISSING_WF_USER1_FQNAME, WF_USER_1_XML);
    testXMOM.put(MISSING_WF_USER2_FQNAME, WF_USER_2_XML);
    testXMOM.put(MISSING_WF_USER3_FQNAME, WF_USER_3_XML);
    setupWorkspace(testXMOM);
    save(INPUT_TYPE_1_FQNAME, INPUT_TYPE_2_FQNAME, MISSING_WF_USER1_FQNAME, MISSING_WF_USER2_FQNAME, MISSING_WF_USER3_FQNAME);
    deploy(INPUT_TYPE_1_FQNAME, INPUT_TYPE_2_FQNAME, MISSING_WF_USER1_FQNAME, MISSING_WF_USER2_FQNAME, MISSING_WF_USER3_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_WF_USER1_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_WF_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/Service[@TypeName='" + MISSING_WF_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpOperationCheck = Functions.fxpath(creationHint, "/Service/Operation[@Name='" + MISSING_WF_NAME + "']");
    assertEquals(1, xpOperationCheck.size());
    List<String> xpInputTypeCheck1 = Functions.fxpath(creationHint, "/Service/Operation/Input/Data[@ReferenceName='" + INPUT_TYPE_1_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpInputTypeCheck1.size());
    List<String> xpInputTypeCheck2 = Functions.fxpath(creationHint, "/Service/Operation/Input/Data[@ReferenceName='" + INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpInputTypeCheck2.size());
    List<String> xpOutputTypeCheck = Functions.fxpath(creationHint, "/Service/Operation/Output/Data[@ReferenceName='" + INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(2, xpOutputTypeCheck.size());
  }
  
  
  @Test
  public void testISGUser1Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(ISG_INPUT_TYPE_1_FQNAME, INSTANCE_INPUT_TYPE_1_XML);
    testXMOM.put(ISG_INPUT_TYPE_2_FQNAME, INSTANCE_INPUT_TYPE_2_XML);
    testXMOM.put(MISSING_ISG_USER1_FQNAME, ISG_USER_1_XML);
    testXMOM.put(DOCUMENT_PART_FQNAME, DOCUMENT_PART_XML);
    testXMOM.put(DOCUMENT_FQNAME, DOCUMENT_XML);
    testXMOM.put(TEMPLATE_MANAGEMENT_FQNAME, TEMPLATE_MANAGEMENT_XML);
    setupWorkspace(testXMOM);
    save(ISG_INPUT_TYPE_1_FQNAME, ISG_INPUT_TYPE_2_FQNAME, MISSING_ISG_USER1_FQNAME, DOCUMENT_PART_FQNAME, DOCUMENT_FQNAME, TEMPLATE_MANAGEMENT_FQNAME);
    deploy(ISG_INPUT_TYPE_1_FQNAME, ISG_INPUT_TYPE_2_FQNAME, MISSING_ISG_USER1_FQNAME, DOCUMENT_PART_FQNAME, DOCUMENT_FQNAME, TEMPLATE_MANAGEMENT_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_ISG_USER1_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_ISG_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/DataType[@TypeName='" + MISSING_ISG_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpServiceCheck = Functions.fxpath(creationHint, "/DataType/Service[@TypeName='" + MISSING_ISG_NAME + "']");
    assertEquals(1, xpServiceCheck.size());
    
    List<String> xpIS2Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService2'][@IsStatic='false']");
    assertEquals(1, xpIS2Check.size());
    List<String> xpIS2InputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService2']/Input/Data");
    assertEquals(2, xpIS2InputCheck.size());
    List<String> xpIS2Input1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService2']/Input/Data[@ReferenceName='" + ISG_INPUT_TYPE_1_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpIS2Input1Check.size());
    List<String> xpIS2Input2Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService2']/Input/Data[@ReferenceName='" + ISG_INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpIS2Input2Check.size());
    List<String> xpIS2OutputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService2']/Output/Data");
    assertEquals(0, xpIS2OutputCheck.size());
    
    List<String> xpIS3Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3'][@IsStatic='false']");
    assertEquals(1, xpIS3Check.size());
    List<String> xpIS3InputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Input/Data");
    assertEquals(1, xpIS3InputCheck.size());
    List<String> xpIS3Input1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Input/Data[@ReferenceName='" + ISG_INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpIS3Input1Check.size());
    List<String> xpIS3OutputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Output/Data");
    assertEquals(1, xpIS3OutputCheck.size());
    List<String> xpIS3Output1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Output/Data[@ReferenceName='" + ISG_INPUT_TYPE_1_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpIS3Output1Check.size());
  }
  
  
  @Test
  public void testISGUser2Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(ISG_INPUT_TYPE_1_FQNAME, INSTANCE_INPUT_TYPE_1_XML);
    testXMOM.put(ISG_INPUT_TYPE_2_FQNAME, INSTANCE_INPUT_TYPE_2_XML);
    testXMOM.put(MISSING_ISG_USER2_FQNAME, ISG_USER_2_XML);
    testXMOM.put(DOCUMENT_PART_FQNAME, DOCUMENT_PART_XML);
    testXMOM.put(DOCUMENT_FQNAME, DOCUMENT_XML);
    testXMOM.put(TEMPLATE_MANAGEMENT_FQNAME, TEMPLATE_MANAGEMENT_XML);
    setupWorkspace(testXMOM);
    save(ISG_INPUT_TYPE_1_FQNAME, ISG_INPUT_TYPE_2_FQNAME, MISSING_ISG_USER2_FQNAME, DOCUMENT_PART_FQNAME, DOCUMENT_FQNAME, TEMPLATE_MANAGEMENT_FQNAME);
    deploy(ISG_INPUT_TYPE_1_FQNAME, ISG_INPUT_TYPE_2_FQNAME, MISSING_ISG_USER2_FQNAME, DOCUMENT_PART_FQNAME, DOCUMENT_FQNAME, TEMPLATE_MANAGEMENT_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_ISG_USER2_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_ISG_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/DataType[@TypeName='" + MISSING_ISG_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpServiceCheck = Functions.fxpath(creationHint, "/DataType/Service[@TypeName='" + MISSING_ISG_NAME + "']");
    assertEquals(1, xpServiceCheck.size());
    
    List<String> xpIS1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService1'][@IsStatic='false']");
    assertEquals(1, xpIS1Check.size());
    List<String> xpIS1InputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService1']/Input/Data");
    assertEquals(1, xpIS1InputCheck.size());
    List<String> xpIS1Input1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService1']/Input/Data/Meta/Type/text()");
    assertEquals(1, xpIS1Input1Check.size());
    assertEquals(String.class.getSimpleName(), xpIS1Input1Check.get(0));
    List<String> xpIS1OutputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService1']/Output/Data");
    assertEquals(0, xpIS1OutputCheck.size());
    
    List<String> xpIS3Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3'][@IsStatic='false']");
    assertEquals(1, xpIS3Check.size());
    List<String> xpIS3InputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Input/Data");
    assertEquals(1, xpIS3InputCheck.size());
    List<String> xpIS3Input1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Input/Data[@ReferenceName='" + ISG_INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpIS3Input1Check.size());
    List<String> xpIS3OutputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Output/Data");
    assertEquals(0, xpIS3OutputCheck.size());
  }
  
  
  @Test
  public void testISGUser3Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(ISG_INPUT_TYPE_1_FQNAME, INSTANCE_INPUT_TYPE_1_XML);
    testXMOM.put(ISG_INPUT_TYPE_2_FQNAME, INSTANCE_INPUT_TYPE_2_XML);
    testXMOM.put(MISSING_ISG_USER3_FQNAME, ISG_USER_3_XML);
    testXMOM.put(DOCUMENT_PART_FQNAME, DOCUMENT_PART_XML);
    testXMOM.put(DOCUMENT_FQNAME, DOCUMENT_XML);
    testXMOM.put(TEMPLATE_MANAGEMENT_FQNAME, TEMPLATE_MANAGEMENT_XML);
    setupWorkspace(testXMOM);
    save(ISG_INPUT_TYPE_1_FQNAME, ISG_INPUT_TYPE_2_FQNAME, MISSING_ISG_USER3_FQNAME, DOCUMENT_PART_FQNAME, DOCUMENT_FQNAME, TEMPLATE_MANAGEMENT_FQNAME);
    deploy(ISG_INPUT_TYPE_1_FQNAME, ISG_INPUT_TYPE_2_FQNAME, MISSING_ISG_USER3_FQNAME, DOCUMENT_PART_FQNAME, DOCUMENT_FQNAME, TEMPLATE_MANAGEMENT_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_ISG_USER3_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_ISG_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/DataType[@TypeName='" + MISSING_ISG_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpServiceCheck = Functions.fxpath(creationHint, "/DataType/Service[@TypeName='" + MISSING_ISG_NAME + "']");
    assertEquals(1, xpServiceCheck.size());
    
    List<String> xpIS1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService1'][@IsStatic='false']");
    assertEquals(1, xpIS1Check.size());
    List<String> xpIS1InputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService1']/Input/Data");
    assertEquals(1, xpIS1InputCheck.size());
    List<String> xpIS1Input1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService1']/Input/Data/Meta/Type/text()");
    assertEquals(1, xpIS1Input1Check.size());
    assertEquals(String.class.getSimpleName(), xpIS1Input1Check.get(0));
    List<String> xpIS1OutputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService1']/Output/Data");
    assertEquals(0, xpIS1OutputCheck.size());
    
    List<String> xpIS3Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3'][@IsStatic='false']");
    assertEquals(1, xpIS3Check.size());
    List<String> xpIS3InputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Input/Data");
    assertEquals(1, xpIS3InputCheck.size());
    List<String> xpIS3Input1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Input/Data[@ReferenceName='" + ISG_INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpIS3Input1Check.size());
    List<String> xpIS3OutputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Output/Data");
    assertEquals(0, xpIS3OutputCheck.size());
  }
  
  
  @Test
  public void testISGUser123Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(ISG_INPUT_TYPE_1_FQNAME, INSTANCE_INPUT_TYPE_1_XML);
    testXMOM.put(ISG_INPUT_TYPE_2_FQNAME, INSTANCE_INPUT_TYPE_2_XML);
    testXMOM.put(MISSING_ISG_USER1_FQNAME, ISG_USER_1_XML);
    testXMOM.put(MISSING_ISG_USER2_FQNAME, ISG_USER_2_XML);
    testXMOM.put(MISSING_ISG_USER3_FQNAME, ISG_USER_3_XML);
    testXMOM.put(DOCUMENT_PART_FQNAME, DOCUMENT_PART_XML);
    testXMOM.put(DOCUMENT_FQNAME, DOCUMENT_XML);
    testXMOM.put(TEMPLATE_MANAGEMENT_FQNAME, TEMPLATE_MANAGEMENT_XML);
    setupWorkspace(testXMOM);
    save(ISG_INPUT_TYPE_1_FQNAME, ISG_INPUT_TYPE_2_FQNAME, MISSING_ISG_USER1_FQNAME, MISSING_ISG_USER2_FQNAME, MISSING_ISG_USER3_FQNAME, DOCUMENT_PART_FQNAME, DOCUMENT_FQNAME, TEMPLATE_MANAGEMENT_FQNAME);
    deploy(ISG_INPUT_TYPE_1_FQNAME, ISG_INPUT_TYPE_2_FQNAME, MISSING_ISG_USER1_FQNAME, MISSING_ISG_USER2_FQNAME, MISSING_ISG_USER3_FQNAME, DOCUMENT_PART_FQNAME, DOCUMENT_FQNAME, TEMPLATE_MANAGEMENT_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_ISG_USER1_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_ISG_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/DataType[@TypeName='" + MISSING_ISG_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpServiceCheck = Functions.fxpath(creationHint, "/DataType/Service[@TypeName='" + MISSING_ISG_NAME + "']");
    assertEquals(1, xpServiceCheck.size());
    
    List<String> xpIS1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService1'][@IsStatic='false']");
    assertEquals(1, xpIS1Check.size());
    List<String> xpIS1InputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService1']/Input/Data");
    assertEquals(1, xpIS1InputCheck.size());
    List<String> xpIS1Input1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService1']/Input/Data/Meta/Type/text()");
    assertEquals(1, xpIS1Input1Check.size());
    assertEquals(String.class.getSimpleName(), xpIS1Input1Check.get(0));
    List<String> xpIS1OutputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService1']/Output/Data");
    assertEquals(0, xpIS1OutputCheck.size());
    
    List<String> xpIS2Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService2'][@IsStatic='false']");
    assertEquals(1, xpIS2Check.size());
    List<String> xpIS2InputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService2']/Input/Data");
    assertEquals(2, xpIS2InputCheck.size());
    List<String> xpIS2Input1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService2']/Input/Data[@ReferenceName='" + ISG_INPUT_TYPE_1_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpIS2Input1Check.size());
    List<String> xpIS2Input2Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService2']/Input/Data[@ReferenceName='" + ISG_INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpIS2Input2Check.size());
    List<String> xpIS2OutputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService2']/Output/Data");
    assertEquals(0, xpIS2OutputCheck.size());
    
    List<String> xpIS3Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3'][@IsStatic='false']");
    assertEquals(1, xpIS3Check.size());
    List<String> xpIS3InputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Input/Data");
    assertEquals(1, xpIS3InputCheck.size());
    List<String> xpIS3Input1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Input/Data[@ReferenceName='" + ISG_INPUT_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpIS3Input1Check.size());
    List<String> xpIS3OutputCheck = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Output/Data");
    assertEquals(1, xpIS3OutputCheck.size());
    List<String> xpIS3Output1Check = Functions.fxpath(creationHint, "/DataType/Service/Operation[@Name='instanceService3']/Output/Data[@ReferenceName='" + ISG_INPUT_TYPE_1_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpIS3Output1Check.size());
  }
  
  
  @Test
  public void testExUser1Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(SUB_DT_FQNAME, SUB_DT_XML);
    testXMOM.put(MISSING_EX_USER1_FQNAME, MISSING_EXCEPTION_USER_1_XML);
    setupWorkspace(testXMOM);
    save(SUB_DT_FQNAME, MISSING_EX_USER1_FQNAME);
    deploy(SUB_DT_FQNAME, MISSING_EX_USER1_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_EX_USER1_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_EX_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/ExceptionStore/ExceptionType[@TypeName='" + MISSING_EX_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpBaseTypeCheck = Functions.fxpath(creationHint, "/ExceptionStore/ExceptionType[@BaseTypeName='" + XYNA_EXCEPTION_BASE_NAME + "'][@BaseTypePath='core.exception']");
    assertEquals(1, xpBaseTypeCheck.size());
  }
  
  
  @Test
  public void testExUser2Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(SUB_DT_FQNAME, SUB_DT_XML);
    testXMOM.put(MISSING_EX_USER1_FQNAME, MISSING_EXCEPTION_USER_1_XML);
    testXMOM.put(MISSING_EX_USER2_FQNAME, MISSING_EXCEPTION_USER_2_XML);
    setupWorkspace(testXMOM);
    save(SUB_DT_FQNAME, MISSING_EX_USER1_FQNAME, MISSING_EX_USER2_FQNAME);
    deploy(SUB_DT_FQNAME, MISSING_EX_USER1_FQNAME, MISSING_EX_USER2_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_EX_USER2_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_EX_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/ExceptionStore/ExceptionType[@TypeName='" + MISSING_EX_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpBaseTypeCheck = Functions.fxpath(creationHint, "/ExceptionStore/ExceptionType[@BaseTypeName='" + XYNA_EXCEPTION_BASE_NAME + "'][@BaseTypePath='core.exception']");
    assertEquals(1, xpBaseTypeCheck.size());
  }
  
  
  @Test
  public void testExUser3Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(SUB_DT_FQNAME, SUB_DT_XML);
    testXMOM.put(MISSING_EX_USER3_FQNAME, MISSING_EXCEPTION_USER_3_XML);
    setupWorkspace(testXMOM);
    save(SUB_DT_FQNAME, MISSING_EX_USER3_FQNAME);
    deploy(SUB_DT_FQNAME, MISSING_EX_USER3_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_EX_USER3_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_EX_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/DataType[@TypeName='" + MISSING_EX_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpVarCheck = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='subDT']");
    assertEquals(1, xpVarCheck.size());
    List<String> xpVarTypeCheck = Functions.fxpath(creationHint, "/DataType/Data[@ReferenceName='" + SUB_DT_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpVarTypeCheck.size());
    List<String> xpVarCheck2 = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='data']");
    assertEquals(1, xpVarCheck2.size());
    List<String> xpVarTypeCheck2 = Functions.fxpath(creationHint, "/DataType/Data[@VariableName='data']/Meta/Type/text()");
    assertEquals(1, xpVarTypeCheck2.size());
    assertEquals(String.class.getSimpleName(), xpVarTypeCheck2.get(0));
  }
  
  
  @Test
  public void testExUser123Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(SUB_DT_FQNAME, SUB_DT_XML);
    testXMOM.put(MISSING_EX_USER1_FQNAME, MISSING_EXCEPTION_USER_1_XML);
    testXMOM.put(MISSING_EX_USER2_FQNAME, MISSING_EXCEPTION_USER_2_XML);
    testXMOM.put(MISSING_EX_USER3_FQNAME, MISSING_EXCEPTION_USER_3_XML);
    setupWorkspace(testXMOM);
    save(SUB_DT_FQNAME, MISSING_EX_USER1_FQNAME, MISSING_EX_USER2_FQNAME, MISSING_EX_USER3_FQNAME);
    deploy(SUB_DT_FQNAME, MISSING_EX_USER1_FQNAME, MISSING_EX_USER2_FQNAME, MISSING_EX_USER3_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_EX_USER3_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_EX_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/ExceptionStore/ExceptionType[@TypeName='" + MISSING_EX_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpBaseTypeCheck = Functions.fxpath(creationHint, "/ExceptionStore/ExceptionType[@BaseTypeName='" + XYNA_EXCEPTION_BASE_NAME + "'][@BaseTypePath='core.exception']");
    assertEquals(1, xpBaseTypeCheck.size());
    List<String> xpVarCheck = Functions.fxpath(creationHint, "/ExceptionStore/ExceptionType/Data[@VariableName='subDT']");
    assertEquals(1, xpVarCheck.size());
    List<String> xpVarTypeCheck = Functions.fxpath(creationHint, "/ExceptionStore/ExceptionType/Data[@ReferenceName='" + SUB_DT_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpVarTypeCheck.size());
    List<String> xpVarCheck2 = Functions.fxpath(creationHint, "/ExceptionStore/ExceptionType/Data[@VariableName='data']");
    assertEquals(1, xpVarCheck2.size());
    List<String> xpVarTypeCheck2 = Functions.fxpath(creationHint, "/ExceptionStore/ExceptionType/Data[@VariableName='data']/Meta/Type/text()");
    assertEquals(1, xpVarTypeCheck2.size());
    assertEquals(String.class.getSimpleName(), xpVarTypeCheck2.get(0));
  }
  
  
  @Test
  public void testBaseInputWFUser1Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(BASE_TYPE_FQNAME, BASE_TYPE_XML);
    testXMOM.put(SUB_TYPE_1_FQNAME, SUB_TYPE_1_XML);
    testXMOM.put(SUB_TYPE_2_FQNAME, SUB_TYPE_2_XML);
    testXMOM.put(MISSING_BASE_WF_USER_1_FQNAME, MISSING_BASE_INPUT_WF_USER_1_XML);
    setupWorkspace(testXMOM);
    save(BASE_TYPE_FQNAME, SUB_TYPE_1_FQNAME, SUB_TYPE_2_FQNAME, MISSING_BASE_WF_USER_1_FQNAME);
    deploy(BASE_TYPE_FQNAME, SUB_TYPE_1_FQNAME, SUB_TYPE_2_FQNAME, MISSING_BASE_WF_USER_1_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_BASE_WF_USER_1_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_BASE_IN_WF_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/Service[@TypeName='" + MISSING_BASE_IN_WF_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpOperationCheck = Functions.fxpath(creationHint, "/Service/Operation[@Name='" + MISSING_BASE_IN_WF_NAME + "']");
    assertEquals(1, xpOperationCheck.size());
    List<String> xpInputTypeCheck1 = Functions.fxpath(creationHint, "/Service/Operation/Input/Data[@ReferenceName='" + BASE_TYPE_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpInputTypeCheck1.size());
    List<String> xpOutputTypeCheck = Functions.fxpath(creationHint, "/Service/Operation/Output/Data");
    assertEquals(0, xpOutputTypeCheck.size());
  }
  
  
  @Test
  public void testBaseInputWFUser2Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(BASE_TYPE_FQNAME, BASE_TYPE_XML);
    testXMOM.put(SUB_TYPE_1_FQNAME, SUB_TYPE_1_XML);
    testXMOM.put(SUB_TYPE_2_FQNAME, SUB_TYPE_2_XML);
    testXMOM.put(MISSING_BASE_WF_USER_2_FQNAME, MISSING_BASE_INPUT_WF_USER_2_XML);
    setupWorkspace(testXMOM);
    save(BASE_TYPE_FQNAME, SUB_TYPE_1_FQNAME, SUB_TYPE_2_FQNAME, MISSING_BASE_WF_USER_2_FQNAME);
    deploy(BASE_TYPE_FQNAME, SUB_TYPE_1_FQNAME, SUB_TYPE_2_FQNAME, MISSING_BASE_WF_USER_2_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_BASE_WF_USER_2_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_BASE_IN_WF_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/Service[@TypeName='" + MISSING_BASE_IN_WF_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpOperationCheck = Functions.fxpath(creationHint, "/Service/Operation[@Name='" + MISSING_BASE_IN_WF_NAME + "']");
    assertEquals(1, xpOperationCheck.size());
    List<String> xpInputTypeCheck1 = Functions.fxpath(creationHint, "/Service/Operation/Input/Data[@ReferenceName='" + SUB_TYPE_1_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpInputTypeCheck1.size());
    List<String> xpOutputTypeCheck = Functions.fxpath(creationHint, "/Service/Operation/Output/Data");
    assertEquals(0, xpOutputTypeCheck.size());
  }
  
  
  @Test
  public void testBaseInputWFUser3Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(BASE_TYPE_FQNAME, BASE_TYPE_XML);
    testXMOM.put(SUB_TYPE_1_FQNAME, SUB_TYPE_1_XML);
    testXMOM.put(SUB_TYPE_2_FQNAME, SUB_TYPE_2_XML);
    testXMOM.put(MISSING_BASE_WF_USER_3_FQNAME, MISSING_BASE_INPUT_WF_USER_3_XML);
    setupWorkspace(testXMOM);
    save(BASE_TYPE_FQNAME, SUB_TYPE_1_FQNAME, SUB_TYPE_2_FQNAME, MISSING_BASE_WF_USER_3_FQNAME);
    deploy(BASE_TYPE_FQNAME, SUB_TYPE_1_FQNAME, SUB_TYPE_2_FQNAME, MISSING_BASE_WF_USER_3_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_BASE_WF_USER_3_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_BASE_IN_WF_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/Service[@TypeName='" + MISSING_BASE_IN_WF_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpOperationCheck = Functions.fxpath(creationHint, "/Service/Operation[@Name='" + MISSING_BASE_IN_WF_NAME + "']");
    assertEquals(1, xpOperationCheck.size());
    List<String> xpInputTypeCheck1 = Functions.fxpath(creationHint, "/Service/Operation/Input/Data[@ReferenceName='" + SUB_TYPE_2_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpInputTypeCheck1.size());
    List<String> xpOutputTypeCheck = Functions.fxpath(creationHint, "/Service/Operation/Output/Data");
    assertEquals(0, xpOutputTypeCheck.size());
  }
  
  
  @Test
  public void testBaseInputWFUser123Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(BASE_TYPE_FQNAME, BASE_TYPE_XML);
    testXMOM.put(SUB_TYPE_1_FQNAME, SUB_TYPE_1_XML);
    testXMOM.put(SUB_TYPE_2_FQNAME, SUB_TYPE_2_XML);
    testXMOM.put(MISSING_BASE_WF_USER_1_FQNAME, MISSING_BASE_INPUT_WF_USER_1_XML);
    testXMOM.put(MISSING_BASE_WF_USER_2_FQNAME, MISSING_BASE_INPUT_WF_USER_2_XML);
    testXMOM.put(MISSING_BASE_WF_USER_3_FQNAME, MISSING_BASE_INPUT_WF_USER_3_XML);
    setupWorkspace(testXMOM);
    save(BASE_TYPE_FQNAME, SUB_TYPE_1_FQNAME, SUB_TYPE_2_FQNAME, MISSING_BASE_WF_USER_1_FQNAME, MISSING_BASE_WF_USER_2_FQNAME, MISSING_BASE_WF_USER_3_FQNAME);
    deploy(BASE_TYPE_FQNAME, SUB_TYPE_1_FQNAME, SUB_TYPE_2_FQNAME, MISSING_BASE_WF_USER_1_FQNAME, MISSING_BASE_WF_USER_2_FQNAME, MISSING_BASE_WF_USER_3_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_BASE_WF_USER_1_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_BASE_IN_WF_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/Service[@TypeName='" + MISSING_BASE_IN_WF_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpOperationCheck = Functions.fxpath(creationHint, "/Service/Operation[@Name='" + MISSING_BASE_IN_WF_NAME + "']");
    assertEquals(1, xpOperationCheck.size());
    List<String> xpInputTypeCheck1 = Functions.fxpath(creationHint, "/Service/Operation/Input/Data[@ReferenceName='" + BASE_TYPE_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpInputTypeCheck1.size());
    List<String> xpOutputTypeCheck = Functions.fxpath(creationHint, "/Service/Operation/Output/Data");
    assertEquals(0, xpOutputTypeCheck.size());
  }
  
  @Test
  public void testBaseInputWFUser23Hints() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(BASE_TYPE_FQNAME, BASE_TYPE_XML);
    testXMOM.put(SUB_TYPE_1_FQNAME, SUB_TYPE_1_XML);
    testXMOM.put(SUB_TYPE_2_FQNAME, SUB_TYPE_2_XML);
    testXMOM.put(MISSING_BASE_WF_USER_2_FQNAME, MISSING_BASE_INPUT_WF_USER_2_XML);
    testXMOM.put(MISSING_BASE_WF_USER_3_FQNAME, MISSING_BASE_INPUT_WF_USER_3_XML);
    setupWorkspace(testXMOM);
    save(BASE_TYPE_FQNAME, SUB_TYPE_1_FQNAME, SUB_TYPE_2_FQNAME, MISSING_BASE_WF_USER_2_FQNAME, MISSING_BASE_WF_USER_3_FQNAME);
    deploy(BASE_TYPE_FQNAME, SUB_TYPE_1_FQNAME, SUB_TYPE_2_FQNAME, MISSING_BASE_WF_USER_2_FQNAME, MISSING_BASE_WF_USER_3_FQNAME);
    
    DeploymentItemState dis = registry.get(MISSING_BASE_WF_USER_2_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    Inconsistency inconsistency = null;
    for (Inconsistency inc : report.getInconsitencies()) {
      if (inc.getFqName().equals(MISSING_BASE_IN_WF_FQNAME) && !inc.isItemExists()) {
        inconsistency = inc;
      }
    }
    assertNotNull(inconsistency);
    String creationHint = inconsistency.getCreationHint();
    assertNotNull(creationHint);
    assertTrue(creationHint.length() > 0);
    
    
    List<String> xpTypeCheck = Functions.fxpath(creationHint, "/Service[@TypeName='" + MISSING_BASE_IN_WF_NAME + "'][@TypePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpTypeCheck.size());
    List<String> xpOperationCheck = Functions.fxpath(creationHint, "/Service/Operation[@Name='" + MISSING_BASE_IN_WF_NAME + "']");
    assertEquals(1, xpOperationCheck.size());
    List<String> xpInputTypeCheck1 = Functions.fxpath(creationHint, "/Service/Operation/Input/Data[@ReferenceName='" + BASE_TYPE_NAME + "'][@ReferencePath='" + CREATION_HINT_ITEM_TEST_PATH + "']");
    assertEquals(1, xpInputTypeCheck1.size());
    List<String> xpOutputTypeCheck = Functions.fxpath(creationHint, "/Service/Operation/Output/Data");
    assertEquals(0, xpOutputTypeCheck.size());
  }
  
  /*
   * Datatype test elements
   */
  
  private final static String MISSING_DT_XML = 
                  "<DataType xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" Label=\"Missing DT\" TypeName=\"MissingDT\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\">"
                                  +"  <Meta>"
                                  +"    <IsServiceGroupOnly>false</IsServiceGroupOnly>"
                                  +"  </Meta>"
                                  +"  <Data Label=\"a var\" VariableName=\"aVar\">"
                                  +"    <Meta>"
                                  +"      <Type>Integer</Type>"
                                  +"    </Meta>"
                                  +"  </Data>"
                                  +"  <Data Label=\"b var\" VariableName=\"bVar\">"
                                  +"    <Meta>"
                                  +"      <Type>String</Type>"
                                  +"    </Meta>"
                                  +"  </Data>"
                                  +"  <Data Label=\"Sub DT\" ReferenceName=\"SubDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"subDT\"/>"
                                  +"</DataType>";
  
  private final static String SUB_DT_XML = 
                  "<DataType xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" Label=\"Sub DT\" TypeName=\"SubDT\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\">"
                  +"  <Data Label=\"a var\" VariableName=\"aVar\">"
                  +"    <Meta>"
                  +"      <Type>String</Type>"
                  +"    </Meta>"
                  +"  </Data>"
                  +"</DataType>";
  
  // input, access on .bVar & .subDT.aVar  
  private final static String MISSING_DT_USER1_XML = 
                  "<Service xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" ID=\"1\" Label=\"DT User 1\" TypeName=\"DTUser1\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\">"
                                  +"  <Operation ID=\"0\" Label=\"DT User 1\" Name=\"DTUser1\">"
                                  +"    <Input>"
                                  +"      <Data ID=\"5\" Label=\"Missing DT\" ReferenceName=\"MissingDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingDT\">"
                                  +"        <Target RefID=\"8\"/>"
                                  +"      </Data>"
                                  +"    </Input>"
                                  +"    <Output/>"
                                  +"    <Mappings ID=\"8\">"
                                  +"      <Source RefID=\"5\"/>"
                                  +"      <Target RefID=\"7\"/>"
                                  +"      <Meta>"
                                  +"        <IsTemplate>true</IsTemplate>"
                                  +"      </Meta>"
                                  +"      <Input>"
                                  +"        <Data ID=\"9\" Label=\"Missing DT\" ReferenceName=\"MissingDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingDT9\"/>"
                                  +"        <Source RefID=\"5\"/>"
                                  +"      </Input>"
                                  +"      <Input>"
                                  +"        <Data ID=\"11\" Label=\"Missing DT\" ReferenceName=\"MissingDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingDT11\"/>"
                                  +"        <Source RefID=\"5\">"
                                  +"          <Meta>"
                                  +"            <LinkType>UserConnected</LinkType>"
                                  +"          </Meta>"
                                  +"        </Source>"
                                  +"      </Input>"
                                  +"      <Output>"
                                  +"        <Data ID=\"10\" Label=\"Document part\" ReferenceName=\"DocumentPart\" ReferencePath=\"xact.templates\" VariableName=\"documentPart\"/>"
                                  +"        <Target RefID=\"7\"/>"
                                  +"      </Output>"
                                  +"      <Mapping>%2%.text=concat(\"\",%0%.bVar,\"\n\",%1%.subDT.aVar,\"\")</Mapping>"
                                  +"    </Mappings>"
                                  +"    <Data ID=\"7\" Label=\"Document part\" ReferenceName=\"DocumentPart\" ReferencePath=\"xact.templates\" VariableName=\"documentPart7\">"
                                  +"      <Source RefID=\"8\"/>"
                                  +"    </Data>"
                                  +"    <Assign ID=\"6\"/>"
                                  +"  </Operation>"
                                  +"</Service>";
  
  //input & access on .aVar
  private final static String MISSING_DT_USER2_XML = 
  "<Service xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" ID=\"1\" Label=\"DT User 2\" TypeName=\"DTUser2\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\">"
  +"  <Meta>"
  +"    <FixedDetailOptions>hideDetailAreas,highDetailsMode</FixedDetailOptions>"
  +"  </Meta>"
  +"  <Operation ID=\"0\" Label=\"DT User 2\" Name=\"DTUser2\">"
  +"    <Input>"
  +"      <Data ID=\"5\" Label=\"Missing DT\" ReferenceName=\"MissingDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingDT\">"
  +"        <Target RefID=\"8\"/>"
  +"      </Data>"
  +"    </Input>"
  +"    <Output/>"
  +"    <Mappings ID=\"8\">"
  +"      <Source RefID=\"5\"/>"
  +"      <Target RefID=\"7\"/>"
  +"      <Meta>"
  +"        <IsTemplate>true</IsTemplate>"
  +"      </Meta>"
  +"      <Input>"
  +"        <Data ID=\"9\" Label=\"Missing DT\" ReferenceName=\"MissingDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingDT9\"/>"
  +"        <Source RefID=\"5\"/>"
  +"      </Input>"
  +"      <Output>"
  +"        <Data ID=\"10\" Label=\"Document part\" ReferenceName=\"DocumentPart\" ReferencePath=\"xact.templates\" VariableName=\"documentPart\"/>"
  +"        <Target RefID=\"7\"/>"
  +"      </Output>"
  +"      <Mapping>%1%.text=concat(\"\",%0%.aVar,\"\")</Mapping>"
  +"    </Mappings>"
  +"    <Data ID=\"7\" Label=\"Document part\" ReferenceName=\"DocumentPart\" ReferencePath=\"xact.templates\" VariableName=\"documentPart7\">"
  +"      <Source RefID=\"8\"/>"
  +"    </Data>"
  +"    <Assign ID=\"6\"/>"
  +"  </Operation>"
  +"</Service>";
  
  //input & access on .subDT
  private final static String MISSING_DT_USER3_XML = 
                  "<Service xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" ID=\"1\" Label=\"DT User 3\" TypeName=\"DTUser3\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\">"
                                  +"  <Meta>"
                                  +"    <FixedDetailOptions>hideDetailAreas,highDetailsMode</FixedDetailOptions>"
                                  +"  </Meta>"
                                  +"  <Operation ID=\"0\" Label=\"DT User 3\" Name=\"DTUser3\">"
                                  +"    <Input>"
                                  +"      <Data ID=\"15\" Label=\"Sub DT\" ReferenceName=\"SubDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"subDT\">"
                                  +"        <Target RefID=\"17\"/>"
                                  +"      </Data>"
                                  +"    </Input>"
                                  +"    <Output>"
                                  +"      <Data ID=\"5\" Label=\"Missing DT\" ReferenceName=\"MissingDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingDT\">"
                                  +"        <Source RefID=\"6\"/>"
                                  +"      </Data>"
                                  +"    </Output>"
                                  +"    <Mappings ID=\"17\" Label=\"Mapping\">"
                                  +"      <Source RefID=\"15\"/>"
                                  +"      <Target RefID=\"14\"/>"
                                  +"      <Meta>"
                                  +"        <FixedDetailOptions>FlatMode,FillMode,openConfiguration</FixedDetailOptions>"
                                  +"      </Meta>"
                                  +"      <Input>"
                                  +"        <Data ID=\"16\" Label=\"Sub DT\" ReferenceName=\"SubDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"subDT16\"/>"
                                  +"        <Source RefID=\"15\"/>"
                                  +"      </Input>"
                                  +"      <Output>"
                                  +"        <Data ID=\"18\" Label=\"Missing DT\" ReferenceName=\"MissingDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingDT18\"/>"
                                  +"        <Target RefID=\"14\"/>"
                                  +"      </Output>"
                                  +"      <Mapping>%1%.subDT~=%0%</Mapping>"
                                  +"    </Mappings>"
                                  +"    <Data ID=\"14\" Label=\"Missing DT\" ReferenceName=\"MissingDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingDT14\">"
                                  +"      <Source RefID=\"17\"/>"
                                  +"      <Target RefID=\"6\"/>"
                                  +"    </Data>"
                                  +"    <Assign ID=\"6\">"
                                  +"      <Source RefID=\"14\"/>"
                                  +"      <Target RefID=\"5\"/>"
                                  +"      <Copy>"
                                  +"        <Source RefID=\"14\"/>"
                                  +"        <Target RefID=\"5\"/>"
                                  +"      </Copy>"
                                  +"    </Assign>"
                                  +"  </Operation>"
                                  +"</Service>";
  
  /*
   * Workflow test elements
   */
  
  private final static String INPUT_TYPE_1_XML = 
                  "<DataType Label=\"Input Type 1\" TypeName=\"InputType1\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
                                  "  <Data Label=\"Data\" VariableName=\"data\">"+
                                  "    <Meta>"+
                                  "      <Type>String</Type>"+
                                  "    </Meta>"+
                                  "  </Data>"+
                                  "</DataType>";
  
  private final static String INPUT_TYPE_2_XML = 
                  "<DataType Label=\"Input Type 2\" TypeName=\"InputType2\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
                                  "  <Data Label=\"Data\" VariableName=\"data\">"+
                                  "    <Meta>"+
                                  "      <Type>String</Type>"+
                                  "    </Meta>"+
                                  "  </Data>"+
                                  "</DataType>";
  
  private final static String MISSING_WF_XML = 
                  "<Service ID=\"1\" Label=\"Missing WF\" TypeName=\"MissingWF\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"Missing WF\" Name=\"MissingWF\">"+
"    <Input>"+
"      <Data Label=\"Input Type 1\" ReferenceName=\"InputType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType1\"/>"+
"      <Data ID=\"4\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType2\">"+
"        <Target RefID=\"3\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output>"+
"      <Data ID=\"2\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType22\">"+
"        <Source RefID=\"3\"/>"+
"      </Data>"+
"      <Data ID=\"5\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType25\">"+
"        <Source RefID=\"3\"/>"+
"      </Data>"+
"    </Output>"+
"    <Assign ID=\"3\">"+
"      <Source RefID=\"4\"/>"+
"      <Target RefID=\"2\"/>"+
"      <Target RefID=\"5\"/>"+
"      <Copy>"+
"        <Source RefID=\"4\"/>"+
"        <Target RefID=\"2\"/>"+
"      </Copy>"+
"      <Copy>"+
"        <Source RefID=\"4\"/>"+
"        <Target RefID=\"5\"/>"+
"      </Copy>"+
"    </Assign>"+
"  </Operation>"+
"</Service>";
  
  
  private final static String WF_USER_1_XML =
                  "<Service ID=\"1\" Label=\"WF User 1\" TypeName=\"WFUser1\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"WF User 1\" Name=\"WFUser1\">"+
"    <Input/>"+
"    <Output/>"+
"    <ServiceReference ID=\"2\" Label=\"Missing WF\" ReferenceName=\"MissingWF\" ReferencePath=\"bg.test.deployitem.creationhints\">"+
"      <Source RefID=\"3\"/>"+
"      <Target RefID=\"3\"/>"+
"    </ServiceReference>"+
"    <Function ID=\"3\" Label=\"Missing WF\">"+
"      <Source RefID=\"2\"/>"+
"      <Source RefID=\"6\"/>"+
"      <Source RefID=\"7\"/>"+
"      <Target RefID=\"2\"/>"+
"      <Target RefID=\"4\"/>"+
"      <Target RefID=\"5\"/>"+
"      <Invoke ServiceID=\"2\" Operation=\"MissingWF\">"+
"        <Source RefID=\"6\">"+
"          <Meta>"+
"            <LinkType>Constant</LinkType>"+
"          </Meta>"+
"        </Source>"+
"        <Source RefID=\"7\">"+
"          <Meta>"+
"            <LinkType>Constant</LinkType>"+
"          </Meta>"+
"        </Source>"+
"      </Invoke>"+
"      <Receive ServiceID=\"2\">"+
"        <Target RefID=\"4\"/>"+
"        <Target RefID=\"5\"/>"+
"      </Receive>"+
"    </Function>"+
"    <Data ID=\"4\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType22\">"+
"      <Source RefID=\"3\"/>"+
"    </Data>"+
"    <Data ID=\"5\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType25\">"+
"      <Source RefID=\"3\"/>"+
"    </Data>"+
"    <Data ID=\"6\" Label=\"Input Type 1\" ReferenceName=\"InputType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType1\">"+
"      <Target RefID=\"3\"/>"+
"      <Data ID=\"8\" Label=\"Data\" VariableName=\"data\">"+
"        <Meta>"+
"          <Type>String</Type>"+
"        </Meta>"+
"        <Value>asfsa</Value>"+
"      </Data>"+
"    </Data>"+
"    <Data ID=\"7\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType2\">"+
"      <Target RefID=\"3\"/>"+
"      <Data ID=\"9\" Label=\"Data\" VariableName=\"data\">"+
"        <Meta>"+
"          <Type>String</Type>"+
"        </Meta>"+
"        <Value>sadfasfd</Value>"+
"      </Data>"+
"    </Data>"+
"    <Assign ID=\"10\"/>"+
"  </Operation>"+
"</Service>";
  
  private final static String WF_USER_2_XML =
                  "<Service ID=\"1\" Label=\"WF User 2\" TypeName=\"WFUser2\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"WF User 2\" Name=\"WFUser2\">"+
"    <Input>"+
"      <Data ID=\"8\" Label=\"Input Type 1\" ReferenceName=\"InputType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType18\">"+
"        <Target RefID=\"3\"/>"+
"      </Data>"+
"      <Data ID=\"9\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType29\">"+
"        <Target RefID=\"3\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output/>"+
"    <ServiceReference ID=\"2\" Label=\"Missing WF\" ReferenceName=\"MissingWF\" ReferencePath=\"bg.test.deployitem.creationhints\">"+
"      <Source RefID=\"3\"/>"+
"      <Target RefID=\"3\"/>"+
"    </ServiceReference>"+
"    <Function ID=\"3\" Label=\"Missing WF\">"+
"      <Source RefID=\"2\"/>"+
"      <Source RefID=\"8\"/>"+
"      <Source RefID=\"9\"/>"+
"      <Target RefID=\"2\"/>"+
"      <Target RefID=\"4\"/>"+
"      <Target RefID=\"5\"/>"+
"      <Invoke ServiceID=\"2\" Operation=\"MissingWF\">"+
"        <Source RefID=\"8\"/>"+
"        <Source RefID=\"9\"/>"+
"      </Invoke>"+
"      <Receive ServiceID=\"2\">"+
"        <Target RefID=\"4\"/>"+
"        <Target RefID=\"5\"/>"+
"      </Receive>"+
"    </Function>"+
"    <Data ID=\"4\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType22\">"+
"      <Source RefID=\"3\"/>"+
"    </Data>"+
"    <Data ID=\"5\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType25\">"+
"      <Source RefID=\"3\"/>"+
"    </Data>"+
"    <Assign ID=\"10\"/>"+
"  </Operation>"+
"</Service>";
  
  private final static String WF_USER_3_XML =
                  "<Service ID=\"1\" Label=\"WF User 3\" TypeName=\"WFUser3\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"WF User 3\" Name=\"WFUser3\">"+
"    <Input>"+
"      <Data ID=\"8\" Label=\"Input Type 1\" ReferenceName=\"InputType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType18\">"+
"        <Target RefID=\"3\"/>"+
"      </Data>"+
"      <Data ID=\"9\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType29\">"+
"        <Target RefID=\"3\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output>"+
"      <Data ID=\"10\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType2\">"+
"        <Source RefID=\"11\"/>"+
"      </Data>"+
"      <Data ID=\"12\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType212\">"+
"        <Source RefID=\"11\"/>"+
"      </Data>"+
"    </Output>"+
"    <ServiceReference ID=\"2\" Label=\"Missing WF\" ReferenceName=\"MissingWF\" ReferencePath=\"bg.test.deployitem.creationhints\">"+
"      <Source RefID=\"3\"/>"+
"      <Target RefID=\"3\"/>"+
"    </ServiceReference>"+
"    <Function ID=\"3\" Label=\"Missing WF\">"+
"      <Source RefID=\"2\"/>"+
"      <Source RefID=\"8\"/>"+
"      <Source RefID=\"9\"/>"+
"      <Target RefID=\"2\"/>"+
"      <Target RefID=\"4\"/>"+
"      <Target RefID=\"5\"/>"+
"      <Invoke ServiceID=\"2\" Operation=\"MissingWF\">"+
"        <Source RefID=\"8\"/>"+
"        <Source RefID=\"9\"/>"+
"      </Invoke>"+
"      <Receive ServiceID=\"2\">"+
"        <Target RefID=\"4\"/>"+
"        <Target RefID=\"5\"/>"+
"      </Receive>"+
"    </Function>"+
"    <Data ID=\"4\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType22\">"+
"      <Source RefID=\"3\"/>"+
"      <Target RefID=\"11\"/>"+
"    </Data>"+
"    <Data ID=\"5\" Label=\"Input Type 2\" ReferenceName=\"InputType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"inputType25\">"+
"      <Source RefID=\"3\"/>"+
"      <Target RefID=\"11\"/>"+
"    </Data>"+
"    <Assign ID=\"11\">"+
"      <Source RefID=\"4\"/>"+
"      <Source RefID=\"5\"/>"+
"      <Target RefID=\"10\"/>"+
"      <Target RefID=\"12\"/>"+
"      <Copy>"+
"        <Source RefID=\"4\">"+
"          <Meta>"+
"            <LinkType>UserConnected</LinkType>"+
"          </Meta>"+
"        </Source>"+
"        <Target RefID=\"10\"/>"+
"      </Copy>"+
"      <Copy>"+
"        <Source RefID=\"5\">"+
"          <Meta>"+
"            <LinkType>UserConnected</LinkType>"+
"          </Meta>"+
"        </Source>"+
"        <Target RefID=\"12\"/>"+
"      </Copy>"+
"    </Assign>"+
"  </Operation>"+
"</Service>";
  
  
  /*
   * Instance service group elements
   */
  
  private final static String INSTANCE_INPUT_TYPE_1_XML =
                  "<DataType Label=\"Instance Service Employed Type 1\" TypeName=\"InstanceServiceEmployedType1\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Meta>"+
"    <IsServiceGroupOnly>false</IsServiceGroupOnly>"+
"  </Meta>"+
"  <Data Label=\"Data\" VariableName=\"data\">"+
"    <Meta>"+
"      <Type>String</Type>"+
"    </Meta>"+
"  </Data>"+
"</DataType>";
  
  private final static String INSTANCE_INPUT_TYPE_2_XML =
                  "<DataType Label=\"Instance Service Employed Type 2\" TypeName=\"InstanceServiceEmployedType2\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Meta>"+
"    <IsServiceGroupOnly>false</IsServiceGroupOnly>"+
"  </Meta>"+
"  <Data Label=\"Data\" VariableName=\"data\">"+
"    <Meta>"+
"      <Type>String</Type>"+
"    </Meta>"+
"  </Data>"+
"  <Data Label=\"Instance Service Employed Type 1\" ReferenceName=\"InstanceServiceEmployedType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType1\"/>"+
"</DataType>";
  
  private final static String MISSING_INSTANCE_SERVICE_GROUP_XML =
                  "<DataType Label=\"Missing Instance Service Group\" TypeName=\"MissingInstanceServiceGroup\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Meta>"+
"    <IsServiceGroupOnly>false</IsServiceGroupOnly>"+
"  </Meta>"+
"  <Service Label=\"New Data Type\" TypeName=\"MissingInstanceServiceGroup\">"+
"    <Operation Label=\"instance service 1\" IsStatic=\"false\" Name=\"instanceService1\">"+
"      <Input>"+
"        <Data Label=\"primitive input\" VariableName=\"input\">"+
"          <Meta>"+
"            <Type>Integer</Type>"+
"          </Meta>"+
"        </Data>"+
"      </Input>"+
"      <Output>"+
"        <Data Label=\"primitive output\" VariableName=\"output\">"+
"          <Meta>"+
"            <Type>String</Type>"+
"          </Meta>"+
"        </Data>"+
"      </Output>"+
"      <SourceCode>"+
"        <CodeSnippet Type=\"Java\">return String.valueOf(input);</CodeSnippet>"+
"      </SourceCode>"+
"    </Operation>"+
"    <Operation Label=\"instance service 2\" IsStatic=\"false\" Name=\"instanceService2\">"+
"      <Input>"+
"        <Data Label=\"Instance Service Employed Type 1\" ReferenceName=\"InstanceServiceEmployedType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType1\"/>"+
"        <Data Label=\"Instance Service Employed Type 2\" ReferenceName=\"InstanceServiceEmployedType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType2\"/>"+
"      </Input>"+
"      <Output/>"+
"      <SourceCode>"+
"        <CodeSnippet Type=\"Java\">getImplementationOfInstanceMethods().instanceService2(instanceServiceEmployedType1, instanceServiceEmployedType2);</CodeSnippet>"+
"      </SourceCode>"+
"    </Operation>"+
"    <Operation Label=\"instance service 3\" IsStatic=\"false\" Name=\"instanceService3\">"+
"      <Input>"+
"        <Data Label=\"Instance Service Employed Type 2\" ReferenceName=\"InstanceServiceEmployedType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType2\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data Label=\"Instance Service Employed Type 1\" ReferenceName=\"InstanceServiceEmployedType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType1\"/>"+
"      </Output>"+
"      <SourceCode>"+
"        <CodeSnippet Type=\"Java\">return instanceServiceEmployedType2.getInstanceServiceEmployedType1();</CodeSnippet>"+
"      </SourceCode>"+
"    </Operation>"+
"  </Service>"+
"</DataType>";
  
  private final static String ISG_USER_1_XML =
                  "<Service ID=\"1\" Label=\"ISG user 1\" TypeName=\"ISGUser1\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"ISG user 1\" Name=\"ISGUser1\">"+
"    <Input>"+
"      <Data ID=\"12\" Label=\"Missing Instance Service Group\" ReferenceName=\"MissingInstanceServiceGroup\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingInstanceServiceGroup12\">"+
"        <Target RefID=\"8\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output>"+
"      <Data ID=\"25\" Label=\"Instance Service Employed Type 1\" ReferenceName=\"InstanceServiceEmployedType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType125\">"+
"        <Source RefID=\"14\"/>"+
"      </Data>"+
"    </Output>"+
"    <ServiceReference ID=\"2\" Label=\"New Data Type\" ReferenceName=\"MissingInstanceServiceGroup.MissingInstanceServiceGroup\" ReferencePath=\"bg.test.deployitem.creationhints\">"+
"      <Source RefID=\"3\"/>"+
"      <Source RefID=\"8\"/>"+
"      <Target RefID=\"3\"/>"+
"      <Target RefID=\"8\"/>"+
"    </ServiceReference>"+
"    <Function ID=\"8\" Label=\"instance service 3\">"+
"      <Source RefID=\"12\"/>"+
"      <Source RefID=\"2\"/>"+
"      <Source RefID=\"23\"/>"+
"      <Target RefID=\"2\"/>"+
"      <Target RefID=\"22\"/>"+
"      <Invoke ServiceID=\"2\" Operation=\"instanceService3\">"+
"        <Source RefID=\"12\"/>"+
"        <Source RefID=\"23\">"+
"          <Meta>"+
"            <LinkType>Constant</LinkType>"+
"          </Meta>"+
"        </Source>"+
"      </Invoke>"+
"      <Receive ServiceID=\"2\">"+
"        <Target RefID=\"22\"/>"+
"      </Receive>"+
"    </Function>"+
"    <Function ID=\"3\" Label=\"instance service 2\">"+
"      <Source RefID=\"2\"/>"+
"      <Source RefID=\"4\"/>"+
"      <Source RefID=\"22\"/>"+
"      <Source RefID=\"24\"/>"+
"      <Target RefID=\"2\"/>"+
"      <Invoke ServiceID=\"2\" Operation=\"instanceService2\">"+
"        <Source RefID=\"4\">"+
"          <Meta>"+
"            <LinkType>Constant</LinkType>"+
"          </Meta>"+
"        </Source>"+
"        <Source RefID=\"22\"/>"+
"        <Source RefID=\"24\">"+
"          <Meta>"+
"            <LinkType>Constant</LinkType>"+
"          </Meta>"+
"        </Source>"+
"      </Invoke>"+
"      <Receive ServiceID=\"2\"/>"+
"    </Function>"+
"    <Data ID=\"4\" Label=\"Missing Instance Service Group\" ReferenceName=\"MissingInstanceServiceGroup\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"const_MissingInstanceServiceGroup\">"+
"      <Target RefID=\"3\"/>"+
"    </Data>"+
"    <Data ID=\"22\" Label=\"Instance Service Employed Type 1\" ReferenceName=\"InstanceServiceEmployedType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType1\">"+
"      <Source RefID=\"8\"/>"+
"      <Target RefID=\"3\"/>"+
"      <Target RefID=\"14\"/>"+
"    </Data>"+
"    <Data ID=\"23\" Label=\"Instance Service Employed Type 2\" ReferenceName=\"InstanceServiceEmployedType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType2\">"+
"      <Target RefID=\"8\"/>"+
"      <Data Label=\"Data\" VariableName=\"data\">"+
"        <Meta>"+
"          <Type>String</Type>"+
"        </Meta>"+
"      </Data>"+
"      <Data Label=\"Instance Service Employed Type 1\" ReferenceName=\"InstanceServiceEmployedType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType1\">"+
"        <Data Label=\"Data\" VariableName=\"data\">"+
"          <Meta>"+
"            <Type>String</Type>"+
"          </Meta>"+
"        </Data>"+
"      </Data>"+
"    </Data>"+
"    <Data ID=\"24\" Label=\"Instance Service Employed Type 2\" ReferenceName=\"InstanceServiceEmployedType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType224\">"+
"      <Target RefID=\"3\"/>"+
"      <Data Label=\"Data\" VariableName=\"data\">"+
"        <Meta>"+
"          <Type>String</Type>"+
"        </Meta>"+
"      </Data>"+
"      <Data Label=\"Instance Service Employed Type 1\" ReferenceName=\"InstanceServiceEmployedType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType1\">"+
"        <Data Label=\"Data\" VariableName=\"data\">"+
"          <Meta>"+
"            <Type>String</Type>"+
"          </Meta>"+
"        </Data>"+
"      </Data>"+
"    </Data>"+
"    <Assign ID=\"14\">"+
"      <Source RefID=\"22\"/>"+
"      <Target RefID=\"25\"/>"+
"      <Copy>"+
"        <Source RefID=\"22\"/>"+
"        <Target RefID=\"25\"/>"+
"      </Copy>"+
"    </Assign>"+
"  </Operation>"+
"</Service>";
  
  private final static String ISG_USER_2_XML =
                  "<Service ID=\"1\" Label=\"ISG user 2\" TypeName=\"ISGUser2\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"ISG user 2\" Name=\"ISGUser2\">"+
"    <Input>"+
"      <Data ID=\"12\" Label=\"Missing Instance Service Group\" ReferenceName=\"MissingInstanceServiceGroup\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingInstanceServiceGroup12\">"+
"        <Target RefID=\"24\"/>"+
"      </Data>"+
"      <Data ID=\"29\" Label=\"Instance Service Employed Type 1\" ReferenceName=\"InstanceServiceEmployedType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType1\"/>"+
"      <Data ID=\"31\" Label=\"Instance Service Employed Type 2\" ReferenceName=\"InstanceServiceEmployedType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType2\">"+
"        <Target RefID=\"24\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output/>"+
"    <Mappings ID=\"24\">"+
"      <Source RefID=\"31\"/>"+
"      <Source RefID=\"12\"/>"+
"      <Target RefID=\"23\"/>"+
"      <Meta>"+
"        <IsTemplate>true</IsTemplate>"+
"      </Meta>"+
"      <Input>"+
"        <Data ID=\"32\" Label=\"Missing Instance Service Group\" ReferenceName=\"MissingInstanceServiceGroup\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingInstanceServiceGroup\"/>"+
"        <Source RefID=\"12\">"+
"          <Meta>"+
"            <LinkType>UserConnected</LinkType>"+
"          </Meta>"+
"        </Source>"+
"      </Input>"+
"      <Input>"+
"        <Data ID=\"25\" Label=\"Missing Instance Service Group\" ReferenceName=\"MissingInstanceServiceGroup\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingInstanceServiceGroup25\"/>"+
"        <Source RefID=\"12\">"+
"          <Meta>"+
"            <LinkType>UserConnected</LinkType>"+
"          </Meta>"+
"        </Source>"+
"      </Input>"+
"      <Input>"+
"        <Data ID=\"30\" Label=\"Instance Service Employed Type 2\" ReferenceName=\"InstanceServiceEmployedType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType230\"/>"+
"        <Source RefID=\"31\">"+
"          <Meta>"+
"            <LinkType>UserConnected</LinkType>"+
"          </Meta>"+
"        </Source>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"33\" Label=\"Document part\" ReferenceName=\"DocumentPart\" ReferencePath=\"xact.templates\" VariableName=\"documentPart\"/>"+
"        <Target RefID=\"23\"/>"+
"      </Output>"+
"      <Mapping>%3%.text=concat(\"\",%0%.instanceService1(\"1\"),\"\n\",%1%.instanceService3(%2%).data,\"\n\")</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"23\" Label=\"Document part\" ReferenceName=\"DocumentPart\" ReferencePath=\"xact.templates\" VariableName=\"documentPart23\">"+
"      <Source RefID=\"24\"/>"+
"    </Data>"+
"    <Assign ID=\"14\"/>"+
"  </Operation>"+
"</Service>";
  
  private final static String ISG_USER_3_XML =
                  "<Service ID=\"1\" Label=\"ISG user 3\" TypeName=\"ISGUser3\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"ISG user 3\" Name=\"ISGUser3\">"+
"    <Input>"+
"      <Data ID=\"12\" Label=\"Missing Instance Service Group\" ReferenceName=\"MissingInstanceServiceGroup\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingInstanceServiceGroup12\">"+
"        <Target RefID=\"38\"/>"+
"      </Data>"+
"      <Data ID=\"40\" Label=\"Instance Service Employed Type 2\" ReferenceName=\"InstanceServiceEmployedType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType240\">"+
"        <Target RefID=\"38\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output>"+
"      <Data ID=\"41\" Label=\"Instance Service Employed Type 1\" ReferenceName=\"InstanceServiceEmployedType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType141\">"+
"        <Source RefID=\"14\"/>"+
"      </Data>"+
"    </Output>"+
"    <Mappings ID=\"38\" Label=\"Mapping\">"+
"      <Source RefID=\"12\"/>"+
"      <Source RefID=\"40\"/>"+
"      <Target RefID=\"36\"/>"+
"      <Meta>"+
"        <FixedDetailOptions>FlatMode,FillMode,openConfiguration</FixedDetailOptions>"+
"      </Meta>"+
"      <Input>"+
"        <Data ID=\"37\" Label=\"Missing Instance Service Group\" ReferenceName=\"MissingInstanceServiceGroup\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingInstanceServiceGroup37\"/>"+
"        <Source RefID=\"12\"/>"+
"      </Input>"+
"      <Input>"+
"        <Data ID=\"31\" Label=\"Instance Service Employed Type 2\" ReferenceName=\"InstanceServiceEmployedType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType2\"/>"+
"        <Source RefID=\"40\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data ID=\"39\" Label=\"Instance Service Employed Type 1\" ReferenceName=\"InstanceServiceEmployedType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType139\"/>"+
"        <Target RefID=\"36\"/>"+
"      </Output>"+
"      <Mapping>%2%~=%0%.instanceService3(%1%)</Mapping>"+
"      <Mapping>%2%.data~=%0%.instanceService1(\"1\")</Mapping>"+
"    </Mappings>"+
"    <Data ID=\"36\" Label=\"Instance Service Employed Type 1\" ReferenceName=\"InstanceServiceEmployedType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"instanceServiceEmployedType1\">"+
"      <Source RefID=\"38\"/>"+
"      <Target RefID=\"14\"/>"+
"    </Data>"+
"    <Assign ID=\"14\">"+
"      <Source RefID=\"36\"/>"+
"      <Target RefID=\"41\"/>"+
"      <Copy>"+
"        <Source RefID=\"36\"/>"+
"        <Target RefID=\"41\"/>"+
"      </Copy>"+
"    </Assign>"+
"  </Operation>"+
"</Service>";
  
  
  private final static String MISSING_EXCEPTION_XML =
                  "<ExceptionStore xmlns=\"http://www.gip.com/xyna/3.0/utils/message/storage/1.1\" Name=\"ExceptionStore\" Version=\"1.8\">"+
"  <ExceptionType BaseTypeName=\"XynaExceptionBase\" BaseTypePath=\"core.exception\" Code=\"DEVEL-00000\" Label=\"Missing Exception\" TypeName=\"MissingException\" TypePath=\"bg.test.deployitem.creationhints\">"+
"    <Data Label=\"Data\" VariableName=\"data\">"+
"      <Meta>"+
"        <Type>String</Type>"+
"      </Meta>"+
"    </Data>"+
"    <Data Label=\"Sub DT\" ReferenceName=\"SubDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"subDT\"/>"+
"    <MessageText Language=\"EN\">tree</MessageText>"+
"    <MessageText Language=\"DE\">baum</MessageText>"+
"  </ExceptionType>"+
"</ExceptionStore>";
  
  private final static String MISSING_EXCEPTION_USER_1_XML =
                  "<Service ID=\"1\" Label=\"Missing exception user 1\" TypeName=\"MissingExceptionUser1\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"Missing exception user 1\" Name=\"MissingExceptionUser1\">"+
"    <Input/>"+
"    <Output/>"+
"    <Throws>"+
"      <Exception Label=\"Missing Exception\" ReferenceName=\"MissingException\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingException\"/>"+
"    </Throws>"+
"    <Throw ID=\"2\" Label=\"Throw Missing Exception\" ExceptionID=\"3\">"+
"      <Source RefID=\"3\">"+
"        <Meta>"+
"          <LinkType>Constant</LinkType>"+
"        </Meta>"+
"      </Source>"+
"    </Throw>"+
"    <Exception ID=\"3\" Label=\"Missing Exception\" ReferenceName=\"MissingException\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"const_MissingException\">"+
"      <Target RefID=\"2\"/>"+
"      <Data ID=\"5\" Label=\"Data\" VariableName=\"data\">"+
"        <Meta>"+
"          <Type>String</Type>"+
"        </Meta>"+
"      </Data>"+
"      <Data ID=\"7\" Label=\"Sub DT\" ReferenceName=\"SubDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"subDT\">"+
"        <Data ID=\"6\" Label=\"a var\" VariableName=\"aVar\">"+
"          <Meta>"+
"            <Type>String</Type>"+
"          </Meta>"+
"        </Data>"+
"      </Data>"+
"    </Exception>"+
"    <Assign ID=\"8\"/>"+
"  </Operation>"+
"</Service>";
  
  private final static String MISSING_EXCEPTION_USER_2_XML =
                  "<Service ID=\"1\" Label=\"Missing exception user 2\" TypeName=\"MissingExceptionUser2\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"Missing exception user 2\" Name=\"MissingExceptionUser2\">"+
"    <Input/>"+
"    <Output/>"+
"    <Function ID=\"5\" Label=\"Missing exception user 1\">"+
"      <Source RefID=\"4\"/>"+
"      <Target RefID=\"4\"/>"+
"      <Target RefID=\"6\"/>"+
"      <Meta>"+
"        <FixedDetailOptions>openCatches</FixedDetailOptions>"+
"      </Meta>"+
"      <Invoke ServiceID=\"4\" Operation=\"MissingExceptionUser1\"/>"+
"      <Receive ServiceID=\"4\"/>"+
"      <Catch ID=\"7\" ExceptionID=\"6\">"+
"        <Assign ID=\"8\"/>"+
"      </Catch>"+
"    </Function>"+
"    <ServiceReference ID=\"4\" Label=\"Missing exception user 1\" ReferenceName=\"MissingExceptionUser1\" ReferencePath=\"bg.test.deployitem.creationhints\">"+
"      <Source RefID=\"5\"/>"+
"      <Target RefID=\"5\"/>"+
"    </ServiceReference>"+
"    <Exception ID=\"6\" Label=\"Missing Exception\" ReferenceName=\"MissingException\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingException6\">"+
"      <Source RefID=\"5\"/>"+
"    </Exception>"+
"    <Assign ID=\"9\"/>"+
"  </Operation>"+
"</Service>";
  
  private final static String MISSING_EXCEPTION_USER_3_XML =
                  "<Service ID=\"1\" Label=\"Missing exception user 3\" TypeName=\"MissingExceptionUser3\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"Missing exception user 3\" Name=\"MissingExceptionUser3\">"+
"    <Input>"+
"      <Data ID=\"13\" Label=\"Sub DT\" ReferenceName=\"SubDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"subDT\">"+
"        <Target RefID=\"15\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output>"+
"      <Exception ID=\"8\" Label=\"Missing Exception\" ReferenceName=\"MissingException\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingException\">"+
"        <Source RefID=\"9\"/>"+
"      </Exception>"+
"    </Output>"+
"    <Mappings ID=\"15\" Label=\"Mapping\">"+
"      <Source RefID=\"13\"/>"+
"      <Target RefID=\"12\"/>"+
"      <Meta>"+
"        <FixedDetailOptions>FlatMode,FillMode,openConfiguration</FixedDetailOptions>"+
"      </Meta>"+
"      <Input>"+
"        <Data ID=\"14\" Label=\"Sub DT\" ReferenceName=\"SubDT\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"subDT14\"/>"+
"        <Source RefID=\"13\"/>"+
"      </Input>"+
"      <Output>"+
"        <Exception ID=\"16\" Label=\"Missing Exception\" ReferenceName=\"MissingException\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingException16\"/>"+
"        <Target RefID=\"12\"/>"+
"      </Output>"+
"      <Mapping>%1%.data~=\"sfdsfd\"</Mapping>"+
"      <Mapping>%1%.subDT~=%0%</Mapping>"+
"    </Mappings>"+
"    <Exception ID=\"12\" Label=\"Missing Exception\" ReferenceName=\"MissingException\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"missingException12\">"+
"      <Source RefID=\"15\"/>"+
"      <Target RefID=\"9\"/>"+
"    </Exception>"+
"    <Assign ID=\"9\">"+
"      <Source RefID=\"12\"/>"+
"      <Target RefID=\"8\"/>"+
"      <Copy>"+
"        <Source RefID=\"12\"/>"+
"        <Target RefID=\"8\"/>"+
"      </Copy>"+
"    </Assign>"+
"  </Operation>"+
"</Service>";
  
  private final static String BASE_TYPE_XML =
                  "<DataType Label=\"Base Type\" TypeName=\"BaseType\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Data Label=\"base var\" VariableName=\"baseVar\">"+
"    <Meta>"+
"      <Type>String</Type>"+
"    </Meta>"+
"  </Data>"+
"</DataType>";
  
  private final static String SUB_TYPE_1_XML =
                  "<DataType Label=\"Sub Type 1\" TypeName=\"SubType1\" TypePath=\"bg.test.deployitem.creationhints\" BaseTypeName=\"BaseType\" BaseTypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Data Label=\"sub type 1 var\" VariableName=\"subType1Var\">"+
"    <Meta>"+
"      <Type>String</Type>"+
"    </Meta>"+
"  </Data>"+
"</DataType>";
  
  private final static String SUB_TYPE_2_XML =
                  "<DataType Label=\"Sub Type 2\" TypeName=\"SubType2\" TypePath=\"bg.test.deployitem.creationhints\" BaseTypeName=\"BaseType\" BaseTypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Data Label=\"sub type 2 var\" VariableName=\"subType2Var\">"+
"    <Meta>"+
"      <Type>String</Type>"+
"    </Meta>"+
"  </Data>"+
"</DataType>";
  
  private final static String MISSING_BASE_INPUT_WF_XML =
                  "<Service ID=\"1\" Label=\"Missing Workflow with base input\" TypeName=\"MissingWorkflowWithBaseInput\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"Missing Workflow with base input\" Name=\"MissingWorkflowWithBaseInput\">"+
"    <Input>"+
"      <Data Label=\"Base Type\" ReferenceName=\"BaseType\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"baseType\"/>"+
"    </Input>"+
"    <Output/>"+
"    <Assign/>"+
"  </Operation>"+
"</Service>";
  
  private final static String MISSING_BASE_INPUT_WF_USER_1_XML =
                  "<Service ID=\"1\" Label=\"Missing base input WF user 1\" TypeName=\"MissingBaseInputWFUser1\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Meta>"+
"    <FixedDetailOptions>hideDetailAreas,highDetailsMode</FixedDetailOptions>"+
"  </Meta>"+
"  <Operation ID=\"0\" Label=\"Missing base input WF user 1\" Name=\"MissingBaseInputWFUser1\">"+
"    <Input>"+
"      <Data ID=\"4\" Label=\"Base Type\" ReferenceName=\"BaseType\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"baseType\">"+
"        <Target RefID=\"3\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output/>"+
"    <ServiceReference ID=\"2\" Label=\"Missing Workflow with base input\" ReferenceName=\"MissingWorkflowWithBaseInput\" ReferencePath=\"bg.test.deployitem.creationhints\">"+
"      <Source RefID=\"3\"/>"+
"      <Target RefID=\"3\"/>"+
"    </ServiceReference>"+
"    <Function ID=\"3\" Label=\"Missing Workflow with base input\">"+
"      <Source RefID=\"2\"/>"+
"      <Source RefID=\"4\"/>"+
"      <Target RefID=\"2\"/>"+
"      <Invoke ServiceID=\"2\" Operation=\"MissingWorkflowWithBaseInput\">"+
"        <Source RefID=\"4\"/>"+
"      </Invoke>"+
"      <Receive ServiceID=\"2\"/>"+
"    </Function>"+
"    <Assign ID=\"5\"/>"+
"  </Operation>"+
"</Service>";
  
  private final static String MISSING_BASE_INPUT_WF_USER_2_XML =
                  "<Service ID=\"1\" Label=\"Missing base input WF user 2\" TypeName=\"MissingBaseInputWFUser2\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"Missing base input WF user 2\" Name=\"MissingBaseInputWFUser2\">"+
"    <Input>"+
"      <Data ID=\"5\" Label=\"Sub Type 1\" ReferenceName=\"SubType1\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"subType1\">"+
"        <Target RefID=\"3\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output/>"+
"    <ServiceReference ID=\"2\" Label=\"Missing Workflow with base input\" ReferenceName=\"MissingWorkflowWithBaseInput\" ReferencePath=\"bg.test.deployitem.creationhints\">"+
"      <Source RefID=\"3\"/>"+
"      <Target RefID=\"3\"/>"+
"    </ServiceReference>"+
"    <Function ID=\"3\" Label=\"Missing Workflow with base input\">"+
"      <Source RefID=\"2\"/>"+
"      <Source RefID=\"5\"/>"+
"      <Target RefID=\"2\"/>"+
"      <Invoke ServiceID=\"2\" Operation=\"MissingWorkflowWithBaseInput\">"+
"        <Source RefID=\"5\"/>"+
"      </Invoke>"+
"      <Receive ServiceID=\"2\"/>"+
"    </Function>"+
"    <Assign ID=\"6\"/>"+
"  </Operation>"+
"</Service>";
  
  private final static String MISSING_BASE_INPUT_WF_USER_3_XML =
                  "<Service ID=\"1\" Label=\"Missing base input WF user 3\" TypeName=\"MissingBaseInputWFUser3\" TypePath=\"bg.test.deployitem.creationhints\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"Missing base input WF user 3\" Name=\"MissingBaseInputWFUser3\">"+
"    <Input>"+
"      <Data ID=\"6\" Label=\"Sub Type 2\" ReferenceName=\"SubType2\" ReferencePath=\"bg.test.deployitem.creationhints\" VariableName=\"subType2\">"+
"        <Target RefID=\"3\"/>"+
"      </Data>"+
"    </Input>"+
"    <Output/>"+
"    <ServiceReference ID=\"2\" Label=\"Missing Workflow with base input\" ReferenceName=\"MissingWorkflowWithBaseInput\" ReferencePath=\"bg.test.deployitem.creationhints\">"+
"      <Source RefID=\"3\"/>"+
"      <Target RefID=\"3\"/>"+
"    </ServiceReference>"+
"    <Function ID=\"3\" Label=\"Missing Workflow with base input\">"+
"      <Source RefID=\"2\"/>"+
"      <Source RefID=\"6\"/>"+
"      <Target RefID=\"2\"/>"+
"      <Invoke ServiceID=\"2\" Operation=\"MissingWorkflowWithBaseInput\">"+
"        <Source RefID=\"6\"/>"+
"      </Invoke>"+
"      <Receive ServiceID=\"2\"/>"+
"    </Function>"+
"    <Assign/>"+
"  </Operation>"+
"</Service>";
  
}
