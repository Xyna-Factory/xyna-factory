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
package com.gip.xyna.xfmg.xfctrl.deployitem;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.AccessChain;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.AccessChain.AccessChainedAssignment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.AccessChain.AccessPart;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.AccessChain.InstanceMethodAccessPart;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.AccessChain.MemberVarAccessPart;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.AccessChain.StaticChainResultType;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItem;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceEmployment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceResolutionContext;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.MemberVariableInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;

/**
 * Test DeploymentItemBuilder with provided xmls and validates it's output
 */
public class TestDeploymentItemBuilder extends TestDeploymentItemBuildSetup {

  @Override
  protected DeploymentItemRegistry getRegistry() {
    return null; // not used
  }
  
  @Test
  public void testEmptyWf() {
    Optional<DeploymentItem> odi = null;
    try {
       odi = DeploymentItemBuilder.build(B_WORKFLOW_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    } catch (Throwable t) {
      t.printStackTrace();
      fail("Build should have succeeded");
    }
    assertTrue(odi.isPresent());
    DeploymentItem wfBdi = odi.get();
    assertFalse("WF B should not appear as deployed", wfBdi.isDeployed());
    assertEquals("WF B should employ it's input params signature interfaces", 1,  wfBdi.getInterfaceEmployment().get(DeploymentLocation.SAVED).size());
    assertEquals("WF B should offer his signature interface and its type", 2,  wfBdi.getPublishedInterfaces().get(DeploymentLocation.SAVED).size());
    
  }
  
  
  @Test
  public void testCallChain() {
    Optional<DeploymentItem> odi = null;
    try {
      odi = DeploymentItemBuilder.build(A_WORKFLOW_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    } catch (Throwable t) {
      t.printStackTrace();
      fail("Build should have succeeded");
    }
    assertTrue(odi.isPresent());
    DeploymentItem wfAdi = odi.get();
    assertFalse("WF A should not appear as deployed", wfAdi.isDeployed());
    assertEquals("WF A should invoke an interfaces", 2,  wfAdi.getInterfaceEmployment().get(DeploymentLocation.SAVED).size());
    assertEquals("WF A should offer his signature interface and its type", 2,  wfAdi.getPublishedInterfaces().get(DeploymentLocation.SAVED).size());
    boolean foundInput = false;
    boolean foundSubWf = false;
    for (DeploymentItemInterface dii : wfAdi.getInterfaceEmployment().get(DeploymentLocation.SAVED)) {
      if (dii instanceof TypeInterface && ((TypeInterface)dii).getName().equals(A_DATATYPE_FQNAME)) {
        foundInput = true;
      }
      if (dii instanceof InterfaceEmployment && ((InterfaceEmployment)dii).getProvider().getName().equals(B_WORKFLOW_FQNAME)) {
        foundSubWf = true;
      }
    }
    assertTrue("The invoked interface of B should have been contained", foundSubWf);
    assertTrue("The used input parameter should have been contained", foundInput);
  }
  
  
  @Test
  public void testNonExistingItem() {
    try {
      Optional<DeploymentItem> odi = DeploymentItemBuilder.build("i.do.not.Exist", Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
      assertFalse(odi.isPresent());
    } catch (Throwable t) {
      t.printStackTrace();
      fail("Build should have succeeded");
    }
  }
  
  
  @Test
  public void testPartialConstantOutput() {
    Optional<DeploymentItem> odi = null;
    try {
       odi = DeploymentItemBuilder.build(D_WORKFLOW_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    } catch (Throwable t) {
      t.printStackTrace();
      fail("Build should have succeeded");
    }
    
    assertTrue(odi.isPresent());
    DeploymentItem wfDdi = odi.get();
    
    assertEquals("Should offer type and invocation", 2, wfDdi.getPublishedInterfaces().get(DeploymentLocation.SAVED).size());
    assertEquals("Should employ type and 1 member var", 2, wfDdi.getInterfaceEmployment().get(DeploymentLocation.SAVED).size());
    boolean foundADatatype = false;
    boolean foundADatatypeMemVar = false;
    for (DeploymentItemInterface dii : wfDdi.getInterfaceEmployment().get(DeploymentLocation.SAVED)) {
      if (dii instanceof TypeInterface && ((TypeInterface)dii).getName().equals(A_DATATYPE_FQNAME)) {
        foundADatatype = true;
      }
      if (dii instanceof InterfaceEmployment) {
        InterfaceEmployment employment = (InterfaceEmployment) dii;
        assertTrue("Should have been the memVar access", employment.unwrap() instanceof MemberVariableInterface);
        assertEquals("Should have been provided by ADatatype", A_DATATYPE_FQNAME, employment.getProvider().getName());
        MemberVariableInterface memVar = (MemberVariableInterface) employment.unwrap();
        assertEquals("",  "aMemvar", memVar.getName());
        foundADatatypeMemVar = true;
      }
    }
    assertTrue(foundADatatype);
    assertTrue(foundADatatypeMemVar);
  }
  
  
  @Test
  public void testConstantOperationInput() {
    Optional<DeploymentItem> odi = null;
    try {
       odi = DeploymentItemBuilder.build(E_WORKFLOW_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    } catch (Throwable t) {
      t.printStackTrace();
      fail("Build should have succeeded");
    }
    
    assertTrue(odi.isPresent());
    DeploymentItem wfEdi = odi.get();
    
    assertEquals("Should offer type and invocation", 2, wfEdi.getPublishedInterfaces().get(DeploymentLocation.SAVED).size());
    assertEquals("Should employ type and 2 member vars", 4, wfEdi.getInterfaceEmployment().get(DeploymentLocation.SAVED).size());
    boolean foundADatatype = false;
    boolean foundADatatypeAMemVar = false;
    boolean foundADatatypeBMemVar = false;
    boolean foundSubWf = false;
    for (DeploymentItemInterface dii : wfEdi.getInterfaceEmployment().get(DeploymentLocation.SAVED)) {
      if (dii instanceof TypeInterface && ((TypeInterface)dii).getName().equals(A_DATATYPE_FQNAME)) {
        foundADatatype = true;
      }
      if (dii instanceof InterfaceEmployment) {
        InterfaceEmployment employment = (InterfaceEmployment) dii;
        if (employment.unwrap() instanceof MemberVariableInterface) {
          assertEquals("Should have been provided by ADatatype", A_DATATYPE_FQNAME, employment.getProvider().getName());
          if (((MemberVariableInterface) employment.unwrap()).getName().equals("aMemvar")) {
            foundADatatypeAMemVar = true;
          } else if (((MemberVariableInterface) employment.unwrap()).getName().equals("bMemvar")) {
            foundADatatypeBMemVar = true;
          }
        } else if (employment.unwrap() instanceof OperationInterface) {
          assertEquals("Should have been provided by ADatatype", B_WORKFLOW_FQNAME, employment.getProvider().getName());
          foundSubWf = true;  
        }
      }
    }
    assertTrue(foundADatatype);
    assertTrue(foundADatatypeAMemVar);
    assertTrue(foundADatatypeBMemVar);
    assertTrue(foundSubWf);
  }
  
  
  @Test
  public void testConstantListOutput() {
    Optional<DeploymentItem> odi = null;
    try {
       odi = DeploymentItemBuilder.build(CONSTANT_LIST_OUTPUT_WF_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    } catch (Throwable t) {
      t.printStackTrace();
      fail("Build should have succeeded");
    }
    
    boolean foundADatatypeMemVar1 = false;
    boolean foundADatatypeMemVar2 = false;
    boolean foundBDatatypeADatatypeVar = false;
    boolean foundDDatatypeADatatypeVar = false;
    boolean foundDDatatypeBDatatypeVar = false;
    for (DeploymentItemInterface dii : odi.get().getInterfaceEmployment().get(DeploymentLocation.SAVED)) {
      if (dii instanceof InterfaceEmployment && ((InterfaceEmployment)dii).unwrap() instanceof MemberVariableInterface) {
        TypeInterface rootType = ((InterfaceEmployment)dii).getProvider();
        MemberVariableInterface memVarIf = (MemberVariableInterface)((InterfaceEmployment)dii).unwrap();
        if (rootType.getName().equals(A_DATATYPE_FQNAME)) {
          if (memVarIf.getName().equals("memberVariable1")) {
            foundADatatypeMemVar1 = true;
          } else if (memVarIf.getName().equals("memberVariable2")) {
            foundADatatypeMemVar2 = true;
          }
        } else if (rootType.getName().equals(B_DATATYPE_FQNAME)) {
          if (memVarIf.getName().equals("aDatatype")) {
            foundBDatatypeADatatypeVar = true;
          }
        } else if (rootType.getName().equals(D_DATATYPE_FQNAME)) {
          if (memVarIf.getName().equals("aDatatype")) {
            foundDDatatypeADatatypeVar = true;
          } else if (memVarIf.getName().equals("bDatatype")) {
            foundDDatatypeBDatatypeVar = true;
          } 
        }
      }
    }
    assertTrue(foundADatatypeMemVar1);
    assertTrue(foundADatatypeMemVar2);
    assertTrue(foundBDatatypeADatatypeVar);
    assertTrue(foundDDatatypeADatatypeVar);
    assertTrue(foundDDatatypeBDatatypeVar);
  }
  
  
  @Test
  public void testNullCheckAccessChainGeneration() throws UnsupportedEncodingException, IOException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    Optional<DeploymentItem> odi = DeploymentItemBuilder.build(NULL_CHECK_CHOICE_WF_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    assertTrue(odi.isPresent());
    
    Set<DeploymentItemInterface> diis = odi.get().getInterfaceEmployment().get(DeploymentLocation.SAVED);
    assertEquals("Should list input type and modelled expression", 2, diis.size());
    boolean foundInputType = false;
    boolean foundModelledExpression = false;
    for (DeploymentItemInterface dii : diis) {
      if (dii instanceof TypeInterface) {
        assertEquals(B_DATATYPE_FQNAME, ((TypeInterface)dii).getName());
        foundInputType = true;
      } else if (dii instanceof StaticChainResultType) {
        foundModelledExpression = true;
        StaticChainResultType scrt = (StaticChainResultType)dii;
        assertTrue("Choice-Formulas will need to evaluated as anyType", TypeInterface.anyType() == scrt.getSource());
        assertEquals("Root expression type (%0%) should be B", B_DATATYPE_FQNAME, scrt.getTarget().getRootType().getName());
        assertNull("Chain should only have one part", scrt.getTarget().getRootPart().getNext());
        assertTrue("Part should be MemVarAccess", scrt.getTarget().getRootPart() instanceof MemberVarAccessPart);
        assertEquals("aDatatype", ((MemberVarAccessPart)scrt.getTarget().getRootPart()).unwrap().getName());
      }
    }
    assertTrue(foundInputType);
    assertTrue(foundModelledExpression);
  }
  
  @Test
  public void testTemplateBlockInstanceInvocation() throws UnsupportedEncodingException, IOException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    Optional<DeploymentItem> odi = DeploymentItemBuilder.build(TEMPLATE_BLOCK_INSTANCE_INVOCATION_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    assertTrue(odi.isPresent());
    Set<DeploymentItemInterface> diis = odi.get().getInterfaceEmployment().get(DeploymentLocation.SAVED);
    boolean foundMappingAssignement = false;
    for (DeploymentItemInterface dii : diis) {
      if (dii instanceof AccessChainedAssignment) {
        foundMappingAssignement = true;
        AccessChain target =((AccessChainedAssignment)dii).getTarget();
        assertEquals("Template should map to DocumentPart", DOCUMENT_PART_FQNAME, target.getRootType().getName());
        assertNull("Chain should only have one part", target.getRootPart().getNext());
        assertTrue("Part should be MemVarAccess", target.getRootPart() instanceof MemberVarAccessPart);
        assertEquals("text", ((MemberVarAccessPart)target.getRootPart()).unwrap().getName());
        
        AccessChain source =((AccessChainedAssignment)dii).getSource();
        // %0%.cInstanceService(%1%,%2%).aDatatype.memberVariable1
        assertEquals("Source should be based of CDatatype", INSTANCE_SERVICE_DATATYPE_C_FQNAME, source.getRootType().getName());
        AccessPart part1 = source.getRootPart();
        assertNotNull(part1.getNext());
        assertTrue(part1 instanceof InstanceMethodAccessPart);
        List<TypeInterface> inputTypes = ((InstanceMethodAccessPart)part1).unwrap().getInput();
        assertEquals(2, inputTypes.size());
        assertEquals(A_DATATYPE_FQNAME, inputTypes.get(0).getName());
        assertEquals(A_DATATYPE_FQNAME, inputTypes.get(1).getName());
        AccessPart part2 = part1.getNext();
        assertNotNull(part2.getNext());
        assertTrue(part2 instanceof MemberVarAccessPart);
        assertEquals("aDatatype", ((MemberVarAccessPart)part2).unwrap().getName());
        AccessPart part3 = part2.getNext();
        assertNull(part3.getNext());
        assertTrue(part3 instanceof MemberVarAccessPart);
        assertEquals("memberVariable1", ((MemberVarAccessPart)part3).unwrap().getName());
      }
    }
    assertTrue(foundMappingAssignement);
  }
  
  
  @Test
  public void testMappingAssignmentGeneration() throws UnsupportedEncodingException, IOException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    Optional<DeploymentItem> odi = DeploymentItemBuilder.build(MAPPING_ASSIGNMENT_WF_FQNAME, Optional.of(XMOMType.WORKFLOW), TEST_REVISION);
    assertTrue(odi.isPresent());
    
    Set<DeploymentItemInterface> diis = odi.get().getInterfaceEmployment().get(DeploymentLocation.SAVED);
    boolean foundStringAssignment = false;
    boolean foundBDatatytpeAssignment = false;
    boolean foundMemVar1 = false;
    boolean foundMemVar2 = false;
    for (DeploymentItemInterface dii : diis) {
      if (dii instanceof AccessChainedAssignment) {
        AccessChain target = ((AccessChainedAssignment)dii).getTarget();
        AccessChain source = ((AccessChainedAssignment)dii).getSource();
        assertEquals(INSTANCE_SERVICE_DATATYPE_C_FQNAME, target.getRootType().getName());
        assertNotNull(target.getRootPart());
        assertTrue(target.getRootPart() instanceof MemberVarAccessPart);
        String sourceMemberVariableName = ((MemberVarAccessPart)target.getRootPart()).unwrap().getName(); 
        if (sourceMemberVariableName.equals("aString")) {
          foundStringAssignment = true;
          AccessChain accessChain = source;
          assertEquals(A_DATATYPE_FQNAME, accessChain.getRootType().getName());
          assertNotNull(accessChain.getRootPart());
          assertTrue(accessChain.getRootPart() instanceof MemberVarAccessPart);
          String targetMemberVariableName = ((MemberVarAccessPart)accessChain.getRootPart()).unwrap().getName(); 
          if (targetMemberVariableName.equals("memberVariable1")) {
            foundMemVar1 = true;
          } else if (targetMemberVariableName.equals("memberVariable2")) {
            foundMemVar2 = true;
          }
        } else if (sourceMemberVariableName.equals("bDatatype")) {
          foundBDatatytpeAssignment = true;
          assertTrue(source.getRootPart() instanceof InstanceMethodAccessPart);
          OperationInterface operation = ((InstanceMethodAccessPart)source.getRootPart()).unwrap();
          assertEquals(2, operation.getInput().size());
          assertEquals(A_DATATYPE_FQNAME, operation.getInput().get(0).getName());
          assertEquals(A_DATATYPE_FQNAME, operation.getInput().get(1).getName());
        }
      }
    }
    assertTrue(foundMemVar1);
    assertTrue(foundMemVar2);
    assertTrue(foundStringAssignment);
    assertTrue(foundBDatatytpeAssignment);
  }
  
  @Test
  public void testMemberVarMatchings() throws Exception {
    InterfaceResolutionContext.resCtx.set(new InterfaceResolutionContext(null, null) {
      @Override
      public Optional<TypeInterface> getLocalSupertype() {
        return Optional.<TypeInterface>empty();
      }
      @Override
      public Optional<TypeInterface> getSupertype(TypeInterface typeInterface) {
        return Optional.<TypeInterface>empty();
      }
    });
    try {
      MemberVariableInterface mvi_single1 = MemberVariableInterface.of("a", null, null, true);
      MemberVariableInterface mvi_single2 = MemberVariableInterface.of("a", "A", null, true);
      assertTrue(mvi_single1.matches(mvi_single2));
    } finally {
      InterfaceResolutionContext.resCtx.remove();
    }
  }



}
