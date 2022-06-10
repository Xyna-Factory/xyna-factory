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

import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_ABSTRACT_SUB_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_ABSTRACT_SUB_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_BASE_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_BASE_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_EXTENDED_SERVICE_PROVIDER_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_EXTENDED_SERVICE_PROVIDER_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_INSTANCE_SERVICE_PROVIDER_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_INSTANCE_SERVICE_PROVIDER_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_LIST_WRAPPER_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_LIST_WRAPPER_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_SUB1_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_SUB1_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_SUB2_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_SUB2_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_SUB3_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_SUB3_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_WRAPPER_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.DT_WRAPPER_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.EX_BASE_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.EX_BASE_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.EX_SUB1_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.EX_SUB1_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_CAST_EXCEPTION_FOR_MEMVAR_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_CAST_EXCEPTION_FOR_MEMVAR_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_CAST_FOR_PRIMITVIE_ASSIGN_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_CAST_FOR_PRIMITVIE_ASSIGN_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_CAST_INSTANCE_SERVICE_FOR_INVOCATION_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_CAST_INSTANCE_SERVICE_FOR_INVOCATION_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_CAST_INSTANCE_SERVICE_IN_AND_OUT_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_CAST_INSTANCE_SERVICE_IN_AND_OUT_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_CAST_SERVICE_PROVIDER_IN_TEMPLATE_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_CAST_SERVICE_PROVIDER_IN_TEMPLATE_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_CAST_TARGET_AND_ASSIGN_PRIMITIVE_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_CAST_TARGET_AND_ASSIGN_PRIMITIVE_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_IMPLICIT_EXCPTION_CAST_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_IMPLICIT_EXCPTION_CAST_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_INHERIT_CAST_FROM_ASSIGN_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_INHERIT_CAST_FROM_ASSIGN_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_NEW_AND_ASSIGN_CASTED_COMPLEX_TWICE_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_NEW_AND_ASSIGN_CASTED_COMPLEX_TWICE_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_NEW_EXCEPTION_AND_INITIALIZATION_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_NEW_EXCEPTION_AND_INITIALIZATION_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_NEW_IN_LIST_AND_ASSIGN_CASTED_LIST_ELEMENT_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_NEW_IN_LIST_AND_ASSIGN_CASTED_LIST_ELEMENT_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_SIMPLE_CAST_DISTRIBUTION_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_SIMPLE_CAST_DISTRIBUTION_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_SIMPLE_DOWNCAST_ASSIGNMENT_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_SIMPLE_NEW_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_SIMPLE_NEW_XML;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_WEIRED_CL_CASE_FQNAME;
import static com.gip.xyna.xprc.xfractwfe.generation.CastAndNewCodeTest.WF_WEIRED_CL_CASE_XML;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.shell.ShellCommand;
import com.gip.xyna.utils.shell.ShellExecution;
import com.gip.xyna.utils.shell.ShellExecutionResponse;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentTransition;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagementImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.Inconsistency;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ProblemType;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ResolutionFailure;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ServiceImplInconsistency;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ServiceImplInconsistencyState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DisplayState;
import com.gip.xyna.xfmg.xfctrl.deploystate.InconsistencyState;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItem;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceEmployment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.MemberVariableInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeOfUsage;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;


/**
 * Validates resolution algorithms on DeploymentStateItems build from provided xmls
 */
public class TestDeploymentItemLifecycle extends TestDeploymentItemBuildSetup {

  private DeploymentItemRegistry registry;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    registry = new DeploymentItemStateRegistry(TEST_REVISION);
    //DeploymentItemStateManagementImpl.reservedObjects = new DeploymentItemStateRegistry();
  }
  
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  protected DeploymentItemRegistry getRegistry() {
    return registry;
  }
  
  
  @Test
  public void testBasicLifecycle() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InvalidVariableIdException, XPRC_InvalidServiceIdException, IOException {
    Optional<DeploymentItem> odi_A = DeploymentItemBuilder.build(A_WORKFLOW_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    assertTrue(odi_A.isPresent());
    registry.save(odi_A.get()); // A -> x | x -> x
    DeploymentItemState dis_A = registry.get(A_WORKFLOW_FQNAME);
    assertEquals("Should be INVALID as B and A_DT do not exist", DisplayState.INVALID, dis_A.deriveDisplayState());
    DeploymentItemStateReport disr = dis_A.getStateReport();
    assertEquals(DisplayState.INVALID, disr.getState());
    assertNotNull(disr.getInconsitencies());
    assertEquals(2, disr.getInconsitencies().size());
    
    Optional<DeploymentItem> odi_B = DeploymentItemBuilder.build(B_WORKFLOW_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    assertTrue(odi_B.isPresent());
    registry.save(odi_B.get()); // A -> B | x -> x
    
    assertEquals("Should be INVALID as A_DT does not exist", DisplayState.INVALID, dis_A.deriveDisplayState());
    disr = dis_A.getStateReport();
    assertEquals(DisplayState.INVALID, disr.getState());
    assertNotNull(disr.getInconsitencies());
    assertEquals(1, disr.getInconsitencies().size());
    
    Optional<DeploymentItem> odi_A_DT = DeploymentItemBuilder.build(A_DATATYPE_FQNAME, Optional.of(XMOMType.DATATYPE), TEST_REVISION);
    assertTrue(odi_A_DT.isPresent());
    registry.save(odi_A_DT.get());
    
    
    assertEquals("Should be SAVED as B does now exist", DisplayState.SAVED, dis_A.deriveDisplayState());
    disr = dis_A.getStateReport();
    assertEquals(DisplayState.SAVED, disr.getState());
    
    registry.collectUsingObjectsInContext(odi_B.get().getName(), emptyCtx()); // A -> B | x -> B
    registry.deployFinished(odi_B.get().getName(), DeploymentTransition.SUCCESS, true, Optional.<Throwable>empty());
    
    assertEquals("Should be SAVED as B only got deployed in a consistent state", DisplayState.SAVED, dis_A.deriveDisplayState());
    
    setupWorkspace(B_WORKFLOW_FQNAME, b2WorkflowXML);
    Optional<DeploymentItem> odi_B2 = DeploymentItemBuilder.build(B_WORKFLOW_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    assertTrue(odi_B2.isPresent());
    registry.save(odi_B2.get()); // A -> B' | x -> B
    
    assertEquals(DisplayState.INVALID, dis_A.deriveDisplayState());
    
    registry.collectUsingObjectsInContext(odi_B2.get().getName(), emptyCtx());
    registry.deployFinished(odi_B2.get().getName(), DeploymentTransition.SUCCESS, true, Optional.<Throwable>empty()); // A -> B' | x -> B'
    
    setupWorkspace(B_WORKFLOW_FQNAME, bWorkflowXML);
    odi_B = DeploymentItemBuilder.build(B_WORKFLOW_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    registry.save(odi_B.get()); // A -> B | x -> B'
    System.out.println(registry.get(odi_B.get().getName()));
    
    assertEquals(DisplayState.SAVED, dis_A.deriveDisplayState());
    
    registry.collectUsingObjectsInContext(odi_A.get().getName(), emptyCtx());
    registry.deployFinished(odi_A.get().getName(), DeploymentTransition.SUCCESS, true, Optional.<Throwable>empty()); // A -> B | A -> B'
    
    assertEquals(DisplayState.INVALID, dis_A.deriveDisplayState());
    
    setupWorkspace(A_WORKFLOW_FQNAME, a3WorkflowXML);
    odi_A = DeploymentItemBuilder.build(A_WORKFLOW_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    registry.save(odi_A.get()); // A'' -> B | A -> B'
    
    dis_A = registry.get(A_WORKFLOW_FQNAME);
    System.out.println(dis_A);
    System.out.println(dis_A.deriveDisplayState());
  }
  
  
  @Test
  public void testNullCheckChoiceLifecycle() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    Optional<DeploymentItem> odi_NullCheckChoice = DeploymentItemBuilder.build(NULL_CHECK_CHOICE_WF_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    assertTrue(odi_NullCheckChoice.isPresent());
    registry.save(odi_NullCheckChoice.get());
    
    DeploymentItemState dis_NullCheckChoice = registry.get(NULL_CHECK_CHOICE_WF_FQNAME);
    Set<DeploymentItemInterface> inconsistencies = dis_NullCheckChoice.getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.SAVED, false);
    
    boolean foundBType = false;
    boolean foundBaDatatypeVariable = false;
    
    for (DeploymentItemInterface dii : inconsistencies) {
      if (dii instanceof TypeInterface) {
        if (((TypeInterface)dii).getName().equals(B_DATATYPE_FQNAME)) {
          foundBType = true;
        }
      } else if (dii instanceof InterfaceEmployment) {
        if (((InterfaceEmployment)dii).getProvider().getName().equals(B_DATATYPE_FQNAME)) {
          assertEquals("aDatatype", ((MemberVariableInterface)((InterfaceEmployment)dii).unwrap()).getName());
          foundBaDatatypeVariable = true;
        }
      }
    }
    assertTrue(foundBType);
    assertTrue(foundBaDatatypeVariable);
    
  }
  
  
  @Test
  public void testModelledExpressionsWithLists() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    save(A_DATATYPE_FQNAME, B_DATATYPE_FQNAME, D_DATATYPE_FQNAME, MODELLED_EXPRESSIONS_WITH_LISTS_WF_FQNAME);
    
    DeploymentItemState dis_MEWL = registry.get(MODELLED_EXPRESSIONS_WITH_LISTS_WF_FQNAME);
    assertNotNull(dis_MEWL);
    System.out.println(dis_MEWL);
    DeploymentItemStateReport report = dis_MEWL.getStateReport();
    assertEquals(DisplayState.SAVED, report.getState());
  }
  
  
  @Test
  public void testUpAndDownCasts() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    save(A_DATATYPE_FQNAME, A_DATATYPE_EXTENSION_FQNAME, UP_DOWN_CAST_WF_FQNAME);
    
    DeploymentItemState dis_UADC = registry.get(UP_DOWN_CAST_WF_FQNAME);
    assertNotNull(dis_UADC);
    System.out.println(dis_UADC);
    DeploymentItemStateReport report = dis_UADC.getStateReport();
    System.out.println("inc: " + report.getInconsitencies());
    System.out.println(report.getUnresolvable());
    assertEquals(DisplayState.SAVED, report.getState());
  }

  
  @Test
  public void testPrimitiveInstanceServiceReturn() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    save(DOCUMENT_FQNAME, PRIMITIVE_INSTANCE_SERVICE_RETURN_WF_FQNAME);
    deploy(DOCUMENT_FQNAME, PRIMITIVE_INSTANCE_SERVICE_RETURN_WF_FQNAME);
    
    DeploymentItemState dis_doc = registry.get(DOCUMENT_FQNAME);
    System.out.println(dis_doc);
    
    DeploymentItemState dis = registry.get(PRIMITIVE_INSTANCE_SERVICE_RETURN_WF_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
  }
  
  @Test
  public void testMapExtensionTypeIntoSubList() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    save(MAP_EXTENSION_INTO_SUBLIST_WF_FQNAME, A_DATATYPE_FQNAME, B_DATATYPE_FQNAME, A_DATATYPE_EXTENSION_FQNAME, D_DATATYPE_FQNAME);
    deploy(MAP_EXTENSION_INTO_SUBLIST_WF_FQNAME, A_DATATYPE_FQNAME, B_DATATYPE_FQNAME, A_DATATYPE_EXTENSION_FQNAME, D_DATATYPE_FQNAME);
    
    DeploymentItemState dis = registry.get(MAP_EXTENSION_INTO_SUBLIST_WF_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    System.out.println(dis);
  }
  
  
  @Test
  public void testCalculateWithString() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    save(CALCULATE_WITH_STRINGS_WF_FQNAME, A_DATATYPE_FQNAME);
    deploy(CALCULATE_WITH_STRINGS_WF_FQNAME, A_DATATYPE_FQNAME);
    
    DeploymentItemState dis = registry.get(CALCULATE_WITH_STRINGS_WF_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
  }
  
  
  @Test
  public void testSavedOnlyInstanceServiceGroupInterfaceChange() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, PersistenceLayerException, InterruptedException, UnsupportedEncodingException, IOException {
    save(A_INSTANCE_SERVICE_GROUP_FQNAME);
    XynaProperty.SERVICE_IMPL_INCONSISTENCY_TIME_LAG.set(Duration.valueOf("1", TimeUnit.SECONDS));
    
    DeploymentItemState dis = registry.get(A_INSTANCE_SERVICE_GROUP_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.SAVED, report.getState());
    
    Thread.sleep(1500);
    
    Optional<DeploymentItem> odi = DeploymentItemBuilder.build(A_INSTANCE_SERVICE_GROUP_FQNAME, Optional.<XMOMType>empty(), TEST_REVISION);
    assertTrue(odi.isPresent());
    registry.save(odi.get());
    
    report = dis.getStateReport();
    assertEquals(DisplayState.SAVED, report.getState());
    
    setupWorkspace(A_INSTANCE_SERVICE_GROUP_FQNAME, A_INSTANCE_SERVICE_GROUP_XML_CHANGED_INPUT, DeploymentLocation.SAVED);
    odi = DeploymentItemBuilder.build(A_INSTANCE_SERVICE_GROUP_FQNAME, Optional.<XMOMType>empty(), TEST_REVISION);
    assertTrue(odi.isPresent());
    registry.save(odi.get());
    
    report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    assertEquals(1, report.getServiceImplInconsistencies().size());
    ServiceImplInconsistency inc = report.getServiceImplInconsistencies().get(0);
    assertEquals(ServiceImplInconsistencyState.SAVED_INTERFACE_CHANGE, inc.getType());
  }
  
  
  @Test
  public void testInstanceServiceGroupInterfaceChange() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, PersistenceLayerException, InterruptedException, UnsupportedEncodingException, IOException {
    save(A_INSTANCE_SERVICE_GROUP_FQNAME);
    deploy(A_INSTANCE_SERVICE_GROUP_FQNAME);
    XynaProperty.SERVICE_IMPL_INCONSISTENCY_TIME_LAG.set(Duration.valueOf("1", TimeUnit.SECONDS));
    
    DeploymentItemState dis = registry.get(A_INSTANCE_SERVICE_GROUP_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    Thread.sleep(1500);
    
    Optional<DeploymentItem> odi = DeploymentItemBuilder.build(A_INSTANCE_SERVICE_GROUP_FQNAME, Optional.<XMOMType>empty(), TEST_REVISION);
    assertTrue(odi.isPresent());
    registry.save(odi.get());
    
    report = dis.getStateReport();
    assertEquals(DisplayState.CHANGED, report.getState());
    
    setupWorkspace(A_INSTANCE_SERVICE_GROUP_FQNAME, A_INSTANCE_SERVICE_GROUP_XML_CHANGED_INPUT, DeploymentLocation.SAVED);
    odi = DeploymentItemBuilder.build(A_INSTANCE_SERVICE_GROUP_FQNAME, Optional.<XMOMType>empty(), TEST_REVISION);
    assertTrue(odi.isPresent());
    registry.save(odi.get());
    
    report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    assertEquals(1, report.getServiceImplInconsistencies().size());
    ServiceImplInconsistency inc = report.getServiceImplInconsistencies().get(0);
    assertEquals(ServiceImplInconsistencyState.SAVED_INTERFACE_CHANGE, inc.getType());
    
    deploy(A_INSTANCE_SERVICE_GROUP_FQNAME);
    
    report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    assertEquals(2, report.getServiceImplInconsistencies().size());
    boolean foundSavedInconsistency = false;
    boolean foundDeployedInconsistency = false;
    for (ServiceImplInconsistency sii : report.getServiceImplInconsistencies()) {
      if (sii.getType() == ServiceImplInconsistencyState.SAVED_INTERFACE_CHANGE) {
        foundSavedInconsistency = true;
      } else if (sii.getType() == ServiceImplInconsistencyState.DEPLOYED_INTERFACE_CHANGE) {
        foundDeployedInconsistency = true;
      }
    }
    assertTrue(foundSavedInconsistency);
    assertTrue(foundDeployedInconsistency);
    
    touch(A_INSTANCE_SERVICE_GROUP_FQNAME, A_INSTANCE_SERVICE_GROUP_IMPL_PATH, DeploymentLocation.SAVED);
        
    report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    assertEquals(1, report.getServiceImplInconsistencies().size());
    inc = report.getServiceImplInconsistencies().get(0);
    assertEquals(ServiceImplInconsistencyState.DEPLOYED_INTERFACE_CHANGE, inc.getType());
    
    deploy(A_INSTANCE_SERVICE_GROUP_FQNAME);
    
    report = dis.getStateReport();
    for (Inconsistency inco : report.getInconsitencies()) {
      System.out.println(inco);
    }
    assertEquals(DisplayState.DEPLOYED, report.getState());
  }
  
  
  @Test
  public void testInstanceServiceGroupWithWFCall() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, PersistenceLayerException, InterruptedException, UnsupportedEncodingException, IOException {
    purgeWorkspace();
    setupWorkspace(A_INSTANCE_SERVICE_GROUP_FQNAME, A_INSTANCE_SERVICE_GROUP_XML_AS_WF_CALL, DeploymentLocation.SAVED);
    save(A_INSTANCE_SERVICE_GROUP_FQNAME);
    
    DeploymentItemState dis = registry.get(A_INSTANCE_SERVICE_GROUP_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    assertEquals(1, report.getInconsitencies().size());
    Inconsistency inc = report.getInconsitencies().get(0);
    assertEquals(InconsistencyState.INVALID_1000, inc.getType());
    assertFalse(inc.isItemExists());
  }
  
  
  @Test
  public void testListInvocationsString() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    save(A_DATATYPE_FQNAME, LIST_INPUT_WF_FQNAME, LIST_CALLER_WF_FQNAME);
    deploy(A_DATATYPE_FQNAME, LIST_INPUT_WF_FQNAME, LIST_CALLER_WF_FQNAME);
    
    DeploymentItemState dis = registry.get(LIST_CALLER_WF_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    setupWorkspace(LIST_INPUT_WF_FQNAME, LIST_INPUT_FLIPPED_WF_XML);
    save(LIST_INPUT_WF_FQNAME);
    
    dis = registry.get(LIST_CALLER_WF_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
  }
  
  
  @Test
  public void testListMappingMissmatchesString() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    save(A_DATATYPE_FQNAME, B_DATATYPE_FQNAME, D_DATATYPE_FQNAME, LIST_MAPPING_MISSMATCH_WF_FQNAME);
    deploy(A_DATATYPE_FQNAME, B_DATATYPE_FQNAME, D_DATATYPE_FQNAME, LIST_MAPPING_MISSMATCH_WF_FQNAME);
    
    DeploymentItemState dis = registry.get(LIST_MAPPING_MISSMATCH_WF_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    setupWorkspace(LIST_MAPPING_MISSMATCH_WF_FQNAME, LIST_MAPPING_MISSMATCH_WF_LIST_ONTO_SINGLE_XML);
    save(LIST_MAPPING_MISSMATCH_WF_FQNAME);
    
    dis = registry.get(LIST_MAPPING_MISSMATCH_WF_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    
    setupWorkspace(LIST_MAPPING_MISSMATCH_WF_FQNAME, LIST_MAPPING_MISSMATCH_WF_SINGLE_ONTO_LIST_XML);
    save(LIST_MAPPING_MISSMATCH_WF_FQNAME);
    
    dis = registry.get(LIST_MAPPING_MISSMATCH_WF_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
  }
  
  
  @Test
  public void testBaseChoiceHierarchy() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    save(BASE_CHOICE_DT_FQNAME, SUB_CHOICE_1_DT_FQNAME, SUB_CHOICE_2_DT_FQNAME, BASE_CHOICE_HIERARCHY_WF_FQNAME);
    deploy(BASE_CHOICE_DT_FQNAME, SUB_CHOICE_1_DT_FQNAME, SUB_CHOICE_2_DT_FQNAME, BASE_CHOICE_HIERARCHY_WF_FQNAME);
    
    DeploymentItemState dis = registry.get(BASE_CHOICE_HIERARCHY_WF_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    setupWorkspace(SUB_CHOICE_2_DT_FQNAME, SUB_CHOICE_2_WITHOUT_BASE_DT_XML);
    save(SUB_CHOICE_2_DT_FQNAME);
    
    dis = registry.get(BASE_CHOICE_HIERARCHY_WF_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
  }
  
  
  @Test
  public void testComplexObjectToString() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    save(A_DATATYPE_FQNAME, COMPLEX_OBJECT_TO_STRING_WF_FQNAME);
    deploy(A_DATATYPE_FQNAME, COMPLEX_OBJECT_TO_STRING_WF_FQNAME);
    
    DeploymentItemState dis = registry.get(COMPLEX_OBJECT_TO_STRING_WF_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
    try {
      XynaProperty.SUPPRESS_WARNINGS.set(false);
    } catch (PersistenceLayerException e) {
      fail();
    }
    
    report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
  }
  
  
  @Test
  public void testUnconnectedParams() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    setupWorkspace(UNCONNECTED_WF_FQNAME, UNCONNECTED_WF);
    save(A_DATATYPE_FQNAME, B_DATATYPE_FQNAME, F_WORKFLOW_FQNAME, UNCONNECTED_WF_FQNAME);
    deploy(A_DATATYPE_FQNAME, B_DATATYPE_FQNAME, F_WORKFLOW_FQNAME, UNCONNECTED_WF_FQNAME);
    
    DeploymentItemState dis = registry.get(UNCONNECTED_WF_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    
  }
  
  
  @Test
  public void testListFunctions() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    save(A_DATATYPE_FQNAME, LIST_FUNCTIONS_WF_FQNAME);
    deploy(A_DATATYPE_FQNAME, LIST_FUNCTIONS_WF_FQNAME);
    
    DeploymentItemState dis = registry.get(LIST_FUNCTIONS_WF_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
  }
  
  @Test
  public void testListFunctions2() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    save(A_DATATYPE_FQNAME, B_DATATYPE_FQNAME, LIST_FUNCTIONS_2_WF_FQNAME);
    deploy(A_DATATYPE_FQNAME, B_DATATYPE_FQNAME, LIST_FUNCTIONS_2_WF_FQNAME);
    
    DeploymentItemState dis = registry.get(LIST_FUNCTIONS_2_WF_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
  }
  
  
  @Test
  public void testPrototypeServiceCallEmployment() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    setupWorkspace(PROTOTYPE_SERVICE_CALL_FQNAME, PROTOTYPE_SERVICE_CALL_XML);
    save( PROTOTYPE_SERVICE_CALL_FQNAME);
    deploy( PROTOTYPE_SERVICE_CALL_FQNAME);
    
    DeploymentItemState dis = registry.get(PROTOTYPE_SERVICE_CALL_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    
    assertNotNull(report.getUnresolvable());
    assertTrue(report.getUnresolvable().size() == 1);
    assertEquals(TypeOfUsage.SERVICE_REFERENCE, report.getUnresolvable().get(0).getType());
    assertEquals(ProblemType.PROTOTYPE_ELEMENT.toString(), report.getUnresolvable().get(0).getAdditionalData().get(ResolutionFailure.PROBLEM_TYPE_KEY));
    
  }
  
  @Test
  public void testPrototypeInOutput() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    setupWorkspace(PROTOTYPE_IN_OUTPUT_FQNAME, PROTOTYPE_IN_OUTPUT_XML);
    save( PROTOTYPE_IN_OUTPUT_FQNAME);
    deploy( PROTOTYPE_IN_OUTPUT_FQNAME);
    
    DeploymentItemState dis = registry.get(PROTOTYPE_IN_OUTPUT_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    
    assertNotNull(report.getUnresolvable());
    assertTrue(report.getUnresolvable().size() == 2);
    boolean foundInput = false;
    boolean foundOutput = false;
    for (ResolutionFailure resFail : report.getUnresolvable()) {
      assertEquals(ProblemType.PROTOTYPE_ELEMENT.toString(), resFail.getAdditionalData().get(ResolutionFailure.PROBLEM_TYPE_KEY));
      if (resFail.getType() == TypeOfUsage.INPUT) {
        foundInput = true;
      } else if (resFail.getType() == TypeOfUsage.OUTPUT) {
        foundOutput = true;
      }
    }
    assertTrue(foundInput);
    assertTrue(foundOutput);
  }
  
  
  @Test
  public void testPrototypeServiceInOutput() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    setupWorkspace(PROTOTYPE_SERVICE_IN_OUT_FQNAME, PROTOTYPE_SERVICE_IN_OUT_XML);
    save( PROTOTYPE_SERVICE_IN_OUT_FQNAME);
    deploy( PROTOTYPE_SERVICE_IN_OUT_FQNAME);
    
    DeploymentItemState dis = registry.get(PROTOTYPE_SERVICE_IN_OUT_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    
    assertNotNull(report.getUnresolvable());
    assertTrue(report.getUnresolvable().size() == 1);
    
    assertEquals(TypeOfUsage.MODELLED_EXPRESSION, report.getUnresolvable().get(0).getType());
    assertEquals(ProblemType.PROTOTYPE_ELEMENT.toString(), report.getUnresolvable().get(0).getAdditionalData().get(ResolutionFailure.PROBLEM_TYPE_KEY));
  }
  
  
  private Map<String, String> getDefaultCastAndNewWorkspace() {
    Map<String, String> testXMOM = new HashMap<String, String>();
    testXMOM.put(DT_BASE_FQNAME, DT_BASE_XML);
    testXMOM.put(DT_SUB1_FQNAME, DT_SUB1_XML);
    testXMOM.put(DT_ABSTRACT_SUB_FQNAME, DT_ABSTRACT_SUB_XML);
    testXMOM.put(DT_SUB2_FQNAME, DT_SUB2_XML);
    testXMOM.put(DT_SUB3_FQNAME, DT_SUB3_XML);
    testXMOM.put(DT_WRAPPER_FQNAME, DT_WRAPPER_XML);
    testXMOM.put(DT_LIST_WRAPPER_FQNAME, DT_LIST_WRAPPER_XML);
    testXMOM.put(DT_INSTANCE_SERVICE_PROVIDER_FQNAME, DT_INSTANCE_SERVICE_PROVIDER_XML);
    testXMOM.put(DT_EXTENDED_SERVICE_PROVIDER_FQNAME, DT_EXTENDED_SERVICE_PROVIDER_XML);
    testXMOM.put(EX_BASE_FQNAME, EX_BASE_XML);
    testXMOM.put(EX_SUB1_FQNAME, EX_SUB1_XML);
    testXMOM.put(DOCUMENT_PART_FQNAME, DOCUMENT_PART_XML);
    testXMOM.put(DOCUMENT_FQNAME, DOCUMENT_XML);
    testXMOM.put(TEMPLATE_MANAGEMENT_FQNAME, TEMPLATE_MANAGEMENT_XML);
    testXMOM.put(DOCUMENT_TYPE_FQNAME, DOCUMENT_TYPE_XML);
    return testXMOM;
  }
  
  @Test
  public void test_WF_SIMPLE_NEW() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = getDefaultCastAndNewWorkspace();
    testXMOM.put(WF_SIMPLE_NEW_FQNAME, WF_SIMPLE_NEW_XML);
    setupWorkspace(testXMOM);
    for (String fqName : testXMOM.keySet()) {
      if (!fqName.equals(DT_SUB2_FQNAME)) {
        save(fqName);
      }
    }
    for (String fqName : testXMOM.keySet()) {
      if (!fqName.equals(DT_SUB2_FQNAME)) {
        deploy(fqName);
      }
    }
    
    DeploymentItemState dis = registry.get(WF_SIMPLE_NEW_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    
    assertEquals(DisplayState.INVALID, report.getState());
    for (Inconsistency inc : report.getInconsitencies()) {
      System.out.println(inc.toString());
    }
    assertEquals(2, report.getInconsitencies().size());
    boolean foundTargetTypeCast = false;
    for (Inconsistency inc : report.getInconsitencies()) {
      assertEquals(DT_SUB2_FQNAME, inc.getFqName());
      assertEquals(false, inc.isItemExists());
      if (inc.getEmploymentType() != null && inc.getEmploymentType().equals(ProblemType.TYPE_CAST)) {
        assertTrue(inc.getEmploymentDescription().contains(DT_BASE_FQNAME));
        foundTargetTypeCast = true;
      }
    }
    assertTrue(foundTargetTypeCast);
    
    save(DT_SUB2_FQNAME);
    dis = registry.get(WF_SIMPLE_NEW_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    
    assertEquals(DisplayState.INVALID, report.getState());
    assertEquals(2, report.getInconsitencies().size());
    foundTargetTypeCast = false;
    for (Inconsistency inc : report.getInconsitencies()) {
      assertEquals(DT_SUB2_FQNAME, inc.getFqName());
      assertEquals(InconsistencyState.INVALID_0101, inc.getType());
      if (inc.getEmploymentType() != null && inc.getEmploymentType().equals(ProblemType.TYPE_CAST)) {
        assertTrue(inc.getEmploymentDescription().contains(DT_BASE_FQNAME));
        foundTargetTypeCast = true;
      }
    }
    assertTrue(foundTargetTypeCast);
    
    deploy(DT_SUB2_FQNAME);
    dis = registry.get(WF_SIMPLE_NEW_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
  }
  
  
  @Test
  public void test_WF_CAST_TARGET_AND_ASSIGN_PRIMITIVE() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = getDefaultCastAndNewWorkspace();
    testXMOM.put(WF_CAST_TARGET_AND_ASSIGN_PRIMITIVE_FQNAME, WF_CAST_TARGET_AND_ASSIGN_PRIMITIVE_XML);
    setupWorkspace(testXMOM);
    for (String fqName : testXMOM.keySet()) {
      if (!fqName.equals(DT_SUB2_FQNAME)) {
        save( fqName);
      }
    }
    for (String fqName : testXMOM.keySet()) {
      if (!fqName.equals(DT_SUB2_FQNAME)) {
        deploy( fqName);
      }
    }
    
    
    DeploymentItemState dis = registry.get(WF_CAST_TARGET_AND_ASSIGN_PRIMITIVE_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    
    assertEquals(DisplayState.INVALID, report.getState());
    assertEquals(3, report.getInconsitencies().size());
    
    boolean foundTypeAccess = false;
    boolean foundVarAccess = false;
    boolean foundSupertypeDependency = false;
    for (Inconsistency inc : report.getInconsitencies()) {
      assertEquals(InconsistencyState.INVALID_11xx, inc.getType());
      assertEquals(true, inc.getFqName().equals(DT_SUB2_FQNAME)); 
      if (inc.getEmploymentType() == ProblemType.MEMBER_VARIABLE_ACCESS) {
        assertEquals("aString", inc.getEmploymentDescription());
        foundVarAccess = true;
      } else if (inc.getEmploymentType() != null && inc.getEmploymentType().equals(ProblemType.TYPE_CAST)) {
        assertTrue(inc.getEmploymentDescription().contains(DT_BASE_FQNAME));
        foundSupertypeDependency = true;
      } else {
        assertEquals(false, inc.isItemExists());
        foundTypeAccess = true;
      }
    }
    for (Inconsistency inc : report.getInconsitencies()) {
      System.out.println(inc.toString());
    }
    assertTrue(foundTypeAccess);
    assertTrue(foundVarAccess);
    assertTrue(foundSupertypeDependency);
    
    save(DT_SUB2_FQNAME);
    dis = registry.get(WF_CAST_TARGET_AND_ASSIGN_PRIMITIVE_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    
    foundTypeAccess = false;
    foundVarAccess = false;
    foundSupertypeDependency = false;
    for (Inconsistency inc : report.getInconsitencies()) {
      assertEquals(InconsistencyState.INVALID_0101, inc.getType());
      assertEquals(true, inc.getFqName().equals(DT_SUB2_FQNAME)); 
      if (inc.getEmploymentType() == ProblemType.MEMBER_VARIABLE_ACCESS) {
        assertEquals("aString", inc.getEmploymentDescription());
        foundVarAccess = true;
      } else if (inc.getEmploymentType() != null && inc.getEmploymentType().equals(ProblemType.TYPE_CAST)) {
        assertTrue(inc.getEmploymentDescription().contains(DT_BASE_FQNAME));
        foundSupertypeDependency = true;
      } else {
        assertEquals(true, inc.isItemExists());
        foundTypeAccess = true;
      }
    }
    assertTrue(foundTypeAccess);
    assertTrue(foundVarAccess);
    assertTrue(foundSupertypeDependency);
    
    deploy(DT_SUB2_FQNAME);
    dis = registry.get(WF_CAST_TARGET_AND_ASSIGN_PRIMITIVE_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    assertEquals(DisplayState.DEPLOYED, report.getState());
    
  }
  
  @Test
  public void test_validDeploymentItemGenerationForCastAndNew() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, UnsupportedEncodingException, IOException {
    Map<String, String> testXMOM = getDefaultCastAndNewWorkspace();
    testXMOM.put(WF_NEW_AND_ASSIGN_CASTED_COMPLEX_TWICE_FQNAME, WF_NEW_AND_ASSIGN_CASTED_COMPLEX_TWICE_XML);
    testXMOM.put(WF_NEW_IN_LIST_AND_ASSIGN_CASTED_LIST_ELEMENT_FQNAME, WF_NEW_IN_LIST_AND_ASSIGN_CASTED_LIST_ELEMENT_XML);
    testXMOM.put(WF_CAST_INSTANCE_SERVICE_IN_AND_OUT_FQNAME, WF_CAST_INSTANCE_SERVICE_IN_AND_OUT_XML);
    testXMOM.put(WF_CAST_INSTANCE_SERVICE_FOR_INVOCATION_FQNAME, WF_CAST_INSTANCE_SERVICE_FOR_INVOCATION_XML);
    testXMOM.put(WF_CAST_FOR_PRIMITVIE_ASSIGN_FQNAME, WF_CAST_FOR_PRIMITVIE_ASSIGN_XML);
    testXMOM.put(WF_CAST_SERVICE_PROVIDER_IN_TEMPLATE_FQNAME, WF_CAST_SERVICE_PROVIDER_IN_TEMPLATE_XML);
    testXMOM.put(WF_SIMPLE_CAST_DISTRIBUTION_FQNAME, WF_SIMPLE_CAST_DISTRIBUTION_XML);
    testXMOM.put(WF_INHERIT_CAST_FROM_ASSIGN_FQNAME, WF_INHERIT_CAST_FROM_ASSIGN_XML);
    testXMOM.put(WF_WEIRED_CL_CASE_FQNAME, WF_WEIRED_CL_CASE_XML);
    testXMOM.put(WF_NEW_EXCEPTION_AND_INITIALIZATION_FQNAME, WF_NEW_EXCEPTION_AND_INITIALIZATION_XML);
    testXMOM.put(WF_IMPLICIT_EXCPTION_CAST_FQNAME, WF_IMPLICIT_EXCPTION_CAST_XML);
    setupWorkspace(testXMOM);
    saveAndDeploy(testXMOM.keySet());
    
    for (String fqName : testXMOM.keySet()) {
      if (fqName.startsWith("bg.test.xfl.newAndCast")) {
        assertEquals(fqName + " was expected to be without inconsistency", DisplayState.DEPLOYED, getState(fqName));
      }
    }
  }
  
  
  public void test_WF_SIMPLE_DOWNCAST_ASSIGNMENT_withWrongTypeOfAssignmentVar() throws Exception {
    Map<String, String> testXMOM = getDefaultCastAndNewWorkspace();
    testXMOM.put(WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME, WF_SIMPLE_DOWNCAST_ASSIGNMENT_XML);
    testXMOM.put(DT_WRAPPER_FQNAME, DT_WRAPPER_WITH_DIFFERENT_SUB1_TYPE_XML);
    setupWorkspace(testXMOM);
    save(testXMOM.keySet());
    deploy(testXMOM.keySet());
    
    DeploymentItemState dis = registry.get(WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    
    assertEquals(DisplayState.INVALID, report.getState());
    assertEquals(1, report.getInconsitencies().size());
    assertEquals(ProblemType.TYPE_CAST, report.getInconsitencies().get(0).getEmploymentType());
    assertEquals(DT_SUB1_FQNAME, report.getInconsitencies().get(0).getAdditionalData().get(DeploymentItemStateReport.ResolutionFailure.TARGET_TYPE_KEY));
    assertEquals(DT_SUB2_FQNAME, report.getInconsitencies().get(0).getAdditionalData().get(DeploymentItemStateReport.ResolutionFailure.SOURCE_TYPE_KEY));
    
    setupWorkspace(DT_WRAPPER_FQNAME, DT_WRAPPER_XML);
    save(DT_WRAPPER_FQNAME);
    deploy(DT_WRAPPER_FQNAME);
    
    dis = registry.get(WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    
    assertEquals(DisplayState.DEPLOYED, report.getState());
   }
  
  
  public void test_WF_SIMPLE_DOWNCAST_ASSIGNMENT_withMissingSuperType() throws Exception {
    Map<String, String> testXMOM = getDefaultCastAndNewWorkspace();
    testXMOM.put(WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME, WF_SIMPLE_DOWNCAST_ASSIGNMENT_XML);
    testXMOM.put(DT_SUB1_FQNAME, DT_SUB1_WITHOUT_BASETYPE_XML);
    setupWorkspace(testXMOM);
    save(testXMOM.keySet());
    deploy(testXMOM.keySet());
    
    DeploymentItemState dis = registry.get(WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    
    assertEquals(DisplayState.INVALID, report.getState());
    assertEquals(1, report.getInconsitencies().size());
    assertEquals(ProblemType.TYPE_CAST, report.getInconsitencies().get(0).getEmploymentType());
    assertEquals(DT_SUB1_FQNAME, report.getInconsitencies().get(0).getAdditionalData().get(ResolutionFailure.TARGET_TYPE_KEY));
    assertEquals(DT_BASE_FQNAME, report.getInconsitencies().get(0).getAdditionalData().get(ResolutionFailure.SOURCE_TYPE_KEY));
    
    setupWorkspace(DT_SUB1_FQNAME, DT_SUB1_XML);
    save(DT_SUB1_FQNAME);
    deploy(DT_SUB1_FQNAME);
    
    dis = registry.get(WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    
    assertEquals(DisplayState.DEPLOYED, report.getState());
  }
  
  
  public void test_WF_SIMPLE_DOWNCAST_ASSIGNMENT_withMissingSubType() throws Exception {
    Map<String, String> testXMOM = getDefaultCastAndNewWorkspace();
    testXMOM.put(WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME, WF_SIMPLE_DOWNCAST_ASSIGNMENT_XML);
    testXMOM.remove(DT_SUB1_FQNAME);
    setupWorkspace(testXMOM);
    save(testXMOM.keySet());
    deploy(testXMOM.keySet());
    
    DeploymentItemState dis = registry.get(WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    assertEquals(2, report.getInconsitencies().size());
    for (Inconsistency inc : report.getInconsitencies()) {
      assertTrue(inc.getFqName().equals(DT_SUB1_FQNAME));
      assertFalse(inc.isItemExists());
    }
    assertEquals(0, report.getUnresolvable().size());
    
    setupWorkspace(DT_SUB1_FQNAME, DT_SUB1_XML);
    save(DT_SUB1_FQNAME);
    deploy(DT_SUB1_FQNAME);
    
    dis = registry.get(WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    
    assertEquals(DisplayState.DEPLOYED, report.getState());
  }
  
  
  public void test_WF_SIMPLE_DOWNCAST_ASSIGNMENT_withMissingBaseType() throws Exception {
    Map<String, String> testXMOM = getDefaultCastAndNewWorkspace();
    testXMOM.put(WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME, WF_SIMPLE_DOWNCAST_ASSIGNMENT_XML);
    testXMOM.remove(DT_BASE_FQNAME);
    setupWorkspace(testXMOM);
    save(testXMOM.keySet());
    deploy(testXMOM.keySet());
    
    DeploymentItemState dis = registry.get(WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    for (Inconsistency inc : report.getInconsitencies()) {
      assertTrue(inc.getFqName().equals(DT_BASE_FQNAME));
      assertFalse(inc.isItemExists());
    }
    assertEquals(1, report.getInconsitencies().size());
    assertEquals(0, report.getUnresolvable().size());
    
    setupWorkspace(DT_BASE_FQNAME, DT_BASE_XML);
    save(DT_BASE_FQNAME);
    deploy(DT_BASE_FQNAME);
    
    dis = registry.get(WF_SIMPLE_DOWNCAST_ASSIGNMENT_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    
    assertEquals(DisplayState.DEPLOYED, report.getState());
  }
  
  
  public void test_WF_CAST_EXCEPTION_FOR_MEMVAR_withMissingBaseException() throws Exception {
    Map<String, String> testXMOM = getDefaultCastAndNewWorkspace();
    testXMOM.put(WF_CAST_EXCEPTION_FOR_MEMVAR_FQNAME, WF_CAST_EXCEPTION_FOR_MEMVAR_XML);
    testXMOM.remove(EX_BASE_FQNAME);
    setupWorkspace(testXMOM);
    save(testXMOM.keySet());
    deploy(testXMOM.keySet());
    
    DeploymentItemState dis = registry.get(WF_CAST_EXCEPTION_FOR_MEMVAR_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    for (Inconsistency inc : report.getInconsitencies()) {
      assertTrue(inc.getFqName().equals(EX_BASE_FQNAME));
      assertFalse(inc.isItemExists());
    }
    assertEquals(1, report.getInconsitencies().size());
    assertEquals(0, report.getUnresolvable().size());
    
    setupWorkspace(EX_BASE_FQNAME, EX_BASE_XML);
    save(EX_BASE_FQNAME);
    deploy(EX_BASE_FQNAME);
    
    dis = registry.get(WF_CAST_EXCEPTION_FOR_MEMVAR_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    
    assertEquals(DisplayState.DEPLOYED, report.getState());
  }
  
  
  public void test_WF_CAST_EXCEPTION_FOR_MEMVAR_withMissingSub1Exception() throws Exception {
    Map<String, String> testXMOM = getDefaultCastAndNewWorkspace();
    testXMOM.put(WF_CAST_EXCEPTION_FOR_MEMVAR_FQNAME, WF_CAST_EXCEPTION_FOR_MEMVAR_XML);
    testXMOM.remove(EX_SUB1_FQNAME);
    setupWorkspace(testXMOM);
    save(testXMOM.keySet());
    deploy(testXMOM.keySet());
    
    DeploymentItemState dis = registry.get(WF_CAST_EXCEPTION_FOR_MEMVAR_FQNAME);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    assertEquals(DisplayState.INVALID, report.getState());
    boolean foundTypeEmployment = false;
    boolean foundMemVarAccess = false;
    boolean foundTypeCast = false;
    for (Inconsistency inc : report.getInconsitencies()) {
      assertTrue(inc.getFqName().equals(EX_SUB1_FQNAME));
      assertFalse(inc.isItemExists());
      if (inc.getEmploymentType() == null) {
        assertEquals(EX_SUB1_FQNAME, inc.getEmploymentDescription());
        foundTypeEmployment = true;
      } else if (inc.getEmploymentType() == ProblemType.MEMBER_VARIABLE_ACCESS) {
        assertTrue(inc.getEmploymentDescription().contains("stringInSub1Exception"));
        foundMemVarAccess = true;
      } else if (inc.getEmploymentType() == ProblemType.TYPE_CAST) {
        assertEquals(EX_BASE_FQNAME, inc.getEmploymentDescription());
        foundTypeCast = true;
      }
    }
    assertTrue(foundTypeEmployment);
    assertTrue(foundMemVarAccess);
    assertTrue(foundTypeCast);
    assertEquals(3, report.getInconsitencies().size());
    assertEquals(0, report.getUnresolvable().size());
    
    setupWorkspace(EX_SUB1_FQNAME, EX_SUB1_XML);
    save(EX_SUB1_FQNAME);
    deploy(EX_SUB1_FQNAME);
    
    dis = registry.get(WF_CAST_EXCEPTION_FOR_MEMVAR_FQNAME);
    assertNotNull(dis);
    report = dis.getStateReport();
    
    assertEquals(DisplayState.DEPLOYED, report.getState());
  }
  
  
  private DisplayState getState(String fqName) {
    DeploymentItemState dis = registry.get(fqName);
    assertNotNull(dis);
    DeploymentItemStateReport report = dis.getStateReport();
    if (report.getState().equals(DisplayState.INVALID)) {
      for (Inconsistency inc : report.getInconsitencies()) {
        System.out.println(inc);
      }
      for (ResolutionFailure inc : report.getUnresolvable()) {
        System.out.println(inc);
      }
    }
    return report.getState();
  }
  
  
  private void saveAndDeploy(Collection<String> fqNames) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    save(fqNames);
    deploy(fqNames);
  }
  
  private static void touch(String fqServiceName, String jarFileBackup, DeploymentLocation location) {
    try {
      ShellExecutionResponse response = new ShellExecution(ShellCommand.cmd("touch " + getJarFilePath(fqServiceName, jarFileBackup, location))).call();
      assertTrue(response.isSuccessfull());
    } catch (Exception e) {
      fail("touch command should have been succesfully executed");
    }
  }
  
  protected final static String b2WorkflowXML = 
                  "<Service ID=\"1\" Label=\"B\" TypeName=\"B\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
                  + "<Operation ID=\"0\" Label=\"B\" Name=\"B\">"
                  +   "<Input>"
                  +     "<Data ID=\"3\" Label=\"A Datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\"/>"
                  +     "<Data ID=\"4\" Label=\"A Datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\"/>"
                  +   "</Input>"
                  +   "<Output/>"
                  +   "<Assign ID=\"2\"/>"
                  + "</Operation>"
                  + "</Service>";
  
  protected final static String a3WorkflowXML = 
                  "<Service ID=\"1\" Label=\"A\" TypeName=\"A\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"
                  + "<Operation ID=\"0\" Label=\"A\" Name=\"A\">"
                  +   "<Input>"
                  +     "<Data ID=\"3\" Label=\"A Datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\">"
                  +       "<Target RefID=\"5\"/>"
                  +     "</Data>"
                  +   "</Input>"
                  +   "<Output/>"
                  +   "<ServiceReference ID=\"4\" Label=\"B\" ReferenceName=\"B\" ReferencePath=\"xfmg.xfctrl.deployitem\">"
                  +     "<Source RefID=\"5\"/>"
                  +     "<Target RefID=\"5\"/>"
                  +   "</ServiceReference>"
                  +   "<Function ID=\"5\" Label=\"B\">"
                  +     "<Source RefID=\"4\"/>"
                  +     "<Source RefID=\"3\"/>"
                  +     "<Target RefID=\"4\"/>"
                  +     "<Invoke ServiceID=\"4\" Operation=\"B\">"
                  +       "<Source RefID=\"3\"/>"
                  +       "<Source RefID=\"3\"/>"
                  +       "<Source RefID=\"3\"/>"
                  +     "</Invoke>"
                  +     "<Receive ServiceID=\"4\"/>"
                  +   "</Function>"
                  +   "<Assign ID=\"2\"/>"
                  + "</Operation>"
                  + "</Service>";
  
  protected final static String A_INSTANCE_SERVICE_GROUP_XML_CHANGED_INPUT =
                  "<DataType xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" Label=\"A instance service group\" TypeName=\"AInstanceServiceGroup\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\">"
                  +"  <Libraries>AInstanceServiceGroupImpl.jar</Libraries>"
                  +"  <Service Label=\"AInstance Service Group\" TypeName=\"AInstanceServiceGroup\">"
                  +"    <Operation IsStatic=\"false\" Label=\"a instance service\" Name=\"aInstanceService\">"
                  +"      <Input>"
                  +"        <Data Label=\"content\" VariableName=\"content\">"
                  +"          <Meta>"
                  +"            <Type>String</Type>"
                  +"          </Meta>"
                  +"        </Data>"
                  +"      </Input>"
                  +"      <Output/>"
                  +"      <SourceCode>"
                  +"        <CodeSnippet Type=\"Java\">getImplementationOfInstanceMethods().aInstanceService(content);</CodeSnippet>"
                  +"      </SourceCode>"
                  +"    </Operation>"
                  +"  </Service>"
                  +"</DataType>";
  
  protected final static String A_INSTANCE_SERVICE_GROUP_XML_AS_WF_CALL =
    "<DataType xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" Label=\"A instance service group\" TypeName=\"AInstanceServiceGroup\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\">"
    +"  <Service Label=\"AInstance Service Group\" TypeName=\"AInstanceServiceGroup\">"
    +"    <Operation IsStatic=\"false\" Label=\"a instance service\" Name=\"aInstanceService\">"
    +"      <Input/>"
    +"      <Output/>"
    +"      <Call ReferenceName=\"TestWF\" ReferencePath=\"xfmg.xfctrl.deployitem\"/>"
    +"    </Operation>"
    +"  </Service>"
    +"</DataType>";
  
  protected final static String UNCONNECTED_WF_NAME = "Unconnected";
  protected final static String UNCONNECTED_WF_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + UNCONNECTED_WF_NAME;

  protected final static String UNCONNECTED_WF =
                  "<Service ID=\"1\" Label=\"Unconnected\" TypeName=\"Unconnected\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"Unconnected\" Name=\"Unconnected\">"+
"    <Input/>"+
"    <Output>"+
"      <Data ID=\"6\" Label=\"a datatype\" ReferenceName=\"ADatatype\" ReferencePath=\"xfmg.xfctrl.deployitem\" VariableName=\"aDatatype\"/>"+
"    </Output>"+
"    <ServiceReference ID=\"21\" Label=\"F\" ReferenceName=\"F\" ReferencePath=\"xfmg.xfctrl.deployitem\">"+
"      <Source RefID=\"22\"/>"+
"      <Target RefID=\"22\"/>"+
"    </ServiceReference>"+
"    <Function ID=\"22\" Label=\"F\">"+
"      <Source RefID=\"21\"/>"+
"      <Target RefID=\"21\"/>"+
"      <Invoke ServiceID=\"21\" Operation=\"F\">"+
"        <Source/>"+
"      </Invoke>"+
"      <Receive ServiceID=\"21\"/>"+
"    </Function>"+
"    <Assign ID=\"5\"/>"+
"  </Operation>"+
"</Service>";
  
  protected final static String PROTOTYPE_IN_OUTPUT_NAME = "PrototypeInOutput";
  protected final static String PROTOTYPE_IN_OUTPUT_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + PROTOTYPE_IN_OUTPUT_NAME;

  protected final static String PROTOTYPE_IN_OUTPUT_XML =
                  "<Service ID=\"1\" Label=\"PrototypeInOutput\" TypeName=\"PrototypeInOutput\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"PrototypeInOutput\" Name=\"PrototypeInOutput\">"+
"    <Input>"+
"      <Data ID=\"6\" Label=\"Data Type\" IsAbstract=\"true\" ReferenceName=\"DataType\" VariableName=\"dataType\"/>"+
"    </Input>"+
"    <Output>"+
"      <Data ID=\"4\" Label=\"Data Type\" IsAbstract=\"true\" ReferenceName=\"DataType\" VariableName=\"dataType4\"/>"+
"    </Output>"+
"    <Assign ID=\"5\"/>"+
"  </Operation>"+
"</Service>";
  
  protected final static String PROTOTYPE_SERVICE_CALL_NAME = "PrototypeServiceCaller";
  protected final static String PROTOTYPE_SERVICE_CALL_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + PROTOTYPE_SERVICE_CALL_NAME;

  protected final static String PROTOTYPE_SERVICE_CALL_XML =
                  "<Service ID=\"1\" Label=\"PrototypeServiceCaller\" TypeName=\"PrototypeServiceCaller\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"PrototypeServiceCaller\" Name=\"PrototypeServiceCaller\">"+
"    <Input/>"+
"    <Output/>"+
"    <Function ID=\"3\" Label=\"Service\" IsAbstract=\"true\">"+
"      <Source RefID=\"2\"/>"+
"      <Target RefID=\"2\"/>"+
"      <Meta>"+
"        <Abstract.UID>C057EB77-3910-002B-E2CC-EE6643334B8F</Abstract.UID>"+
"      </Meta>"+
"      <ServiceReference ID=\"2\" Label=\"Service\" ReferenceName=\"AbstractService\">"+
"        <Source RefID=\"3\"/>"+
"        <Target RefID=\"3\"/>"+
"      </ServiceReference>"+
"      <Service Label=\"Service\" IsAbstract=\"true\" TypeName=\"AbstractService\">"+
"        <Operation IsAbstract=\"true\" Name=\"service\">"+
"          <Input/>"+
"          <Output/>"+
"        </Operation>"+
"      </Service>"+
"      <Invoke ServiceID=\"2\" Operation=\"service\"/>"+
"      <Receive ServiceID=\"2\"/>"+
"    </Function>"+
"    <Assign ID=\"4\"/>"+
"  </Operation>"+
"</Service>";
  
  protected final static String PROTOTYPE_SERVICE_IN_OUT_NAME = "PrototypeInOutputInServiceInvocation";
  protected final static String PROTOTYPE_SERVICE_IN_OUT_FQNAME = DEPLOYMENT_ITEM_TEST_PATH + "." + PROTOTYPE_SERVICE_IN_OUT_NAME;

  protected final static String PROTOTYPE_SERVICE_IN_OUT_XML =
  "<Service ID=\"1\" Label=\"PrototypeInOutputInServiceInvocation\" TypeName=\"PrototypeInOutputInServiceInvocation\" TypePath=\"xfmg.xfctrl.deployitem\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
"  <Operation ID=\"0\" Label=\"PrototypeInOutputInServiceInvocation\" Name=\"PrototypeInOutputInServiceInvocation\">"+
"    <Input/>"+
"    <Output/>"+
"    <Mappings Label=\"Mapping\">"+
"      <Input>"+
"        <Data Label=\"Data Type\" IsAbstract=\"true\" ReferenceName=\"DataType\" VariableName=\"dataType4\"/>"+
"      </Input>"+
"      <Output>"+
"        <Data Label=\"Data Type\" IsAbstract=\"true\" ReferenceName=\"DataType\" VariableName=\"dataType\"/>"+
"      </Output>"+
"      <Mapping>%1%~=%0%</Mapping>"+
"    </Mappings>"+
"    <Assign/>"+
"  </Operation>"+
"</Service>";
  
  
  public final static String DT_WRAPPER_WITH_DIFFERENT_SUB1_TYPE_XML =
                  "<DataType Label=\"Wrapper\" TypeName=\"Wrapper\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">"+
                  "  <Data Label=\"Base\" ReferenceName=\"Base\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"base\"/>"+
                  "  <Data Label=\"Sub 1\" ReferenceName=\"Sub2\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub1\"/>"+
                  "  <Data Label=\"Sub 2\" ReferenceName=\"Sub2\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub2\"/>"+
                  "  <Data Label=\"Sub 3\" ReferenceName=\"Sub3\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"sub3\"/>"+
                  "  <Data Label=\"Abstract Sub\" ReferenceName=\"AbstractSub\" ReferencePath=\"bg.test.xfl.newAndCast\" VariableName=\"abstractSub\"/>"+
                  "</DataType>";
  
  public final static String DT_SUB1_WITHOUT_BASETYPE_XML = 
                  "<DataType Label=\"Sub 1\" TypeName=\"Sub1\" TypePath=\"bg.test.xfl.newAndCast\" Version=\"1.8\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\"/>";
  
                  
  
}
