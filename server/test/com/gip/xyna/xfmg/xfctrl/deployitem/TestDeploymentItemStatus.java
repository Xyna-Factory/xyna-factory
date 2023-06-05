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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.classextension.EasyMock;
import org.junit.Test;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentContext;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentTransition;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DisplayState;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.AccessChain;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.AccessChain.AccessPart;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItem;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceEmployment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceResolutionContext;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.MemberVariableInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface.ImplementationType;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface.OperationType;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.SupertypeInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

/**
 * Unit-Test for DeploymentItemState algorithms, DeploymentItems are manually build
 */
public class TestDeploymentItemStatus extends TestDeploymentItemBuildSetup {

  private DeploymentItemRegistry registry;
  
  
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    registry = new DeploymentItemStateRegistry(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  
  @Override
  protected void tearDown() throws Exception {
    registry = null;
    super.tearDown();
  }
  
  protected DeploymentItemRegistry getRegistry() {
    return registry;
  }
  
  /* A invokes B
   * registration of B -> no inconsistencies in saved
   * registration of A -> no inconsistencies in saved
   * */
  @Test
  public void testBasicInvocationRegistration() {
    DeploymentItem di_B = get_B_definition();
    registry.save(di_B);
    DeploymentItemState dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as fresh save.", DisplayState.SAVED, dis_B.deriveDisplayState());
    
    DeploymentItem di_A = new DeploymentItem("A", XMOMType.WORKFLOW);
    di_A.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A, true));
    di_A.addInterfaceEmployment(DeploymentLocation.SAVED, typeInterface(di_B));
    registry.save(di_A);
    DeploymentItemState dis_A = registry.get(di_A.getName());
    assertEquals("Object should appear as fresh save.", DisplayState.SAVED, dis_A.deriveDisplayState());
  }
  
  
  /* A invokes B
   * registration of A -> inconsistency as B does not yet exist and therefore the invoked interface is not present
   * registration of B -> no inconsistencies in saved
   *                 A -> no inconsistencies in saved
   *                 */
  @Test
  public void testBasicInvocationRegistrationInReverseOrder() {
    DeploymentItem di_B = get_B_definition();
    DeploymentItem di_A = get_A_definition();
    registry.save(di_A);
    DeploymentItemState dis_A = registry.get(di_A.getName());
    assertEquals("Object should be invalid as called interface is not saved.", DisplayState.INVALID, dis_A.deriveDisplayState());
    
    registry.save(di_B);
    DeploymentItemState dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as fresh save.", DisplayState.SAVED, dis_B.deriveDisplayState());
    assertEquals("Object should appear as fresh save.", DisplayState.SAVED, dis_A.deriveDisplayState());
  }
  
  
  /* Save an element several times and observe no change
   *
   */
  @Test
  public void testStaysSavedOnChangeIfNotDeployed() {
    DeploymentItem di_B = get_B_definition();
    registry.save(di_B);
    DeploymentItemState dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as fresh save.", DisplayState.SAVED, dis_B.deriveDisplayState());
    
    DeploymentItem di_A_DT = get_A_DT_definition();
    registry.save(di_A_DT);
    
    DeploymentItem di_B2 = get_B2_definition();
    registry.save(di_B2);
    DeploymentItemState dis_B2 = registry.get(di_B2.getName());
    assertEquals("Object should appear as fresh save.", DisplayState.SAVED, dis_B2.deriveDisplayState());
  }

  
  @Test
  public void testSingleElementSimpleLifeCycle() {
    DeploymentItem di_B = get_B_definition();
    registry.save(di_B);
    DeploymentItemState dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as fresh save.", DisplayState.SAVED, dis_B.deriveDisplayState());
    
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as deployed.", DisplayState.DEPLOYED, dis_B.deriveDisplayState());
    
    registry.save(di_B);
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as changed.", DisplayState.CHANGED, dis_B.deriveDisplayState());
    
    registry.undeploy(di_B.getName(), emptyCtx());
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as saved.", DisplayState.SAVED, dis_B.deriveDisplayState());
    
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    assertEquals("Object should appear as deployed.", DisplayState.DEPLOYED, dis_B.deriveDisplayState());
    
    registry.delete(di_B.getName(), emptyCtx());
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as deployed.", DisplayState.NON_EXISTENT, dis_B.deriveDisplayState());
  }
  
  
  @Test
  public void testSingleElementRollbackTransition() {
    DeploymentItem di_B = get_B_definition();
    registry.save(di_B);
    DeploymentItemState dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as fresh save.", DisplayState.SAVED, dis_B.deriveDisplayState());
    
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.ROLLBACK, true, Optional.of(new NullPointerException()));
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should still appear as saved after rollback.", DisplayState.SAVED, dis_B.deriveDisplayState());
    
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.SUCCESS, true, Optional.<Throwable>empty());
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as deployed.", DisplayState.DEPLOYED, dis_B.deriveDisplayState());
    
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.ROLLBACK, true, Optional.of(new NullPointerException()));
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should still appear as deployed after rollback.", DisplayState.DEPLOYED, dis_B.deriveDisplayState());
    
    registry.save(di_B);
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as changed.", DisplayState.CHANGED, dis_B.deriveDisplayState());
    
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.ROLLBACK, true, Optional.of(new NullPointerException()));
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should still appear as changed after rollback.", DisplayState.CHANGED, dis_B.deriveDisplayState());
  }
  
  
  @Test
  public void testSingleElementErrorDuringRollbackTransition() {
    DeploymentItem di_B = get_B_definition();
    registry.save(di_B);
    DeploymentItemState dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as fresh save.", DisplayState.SAVED, dis_B.deriveDisplayState());
    
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.ERROR_DURING_ROLLBACK, true, Optional.of(new NullPointerException()));
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as incomplete.", DisplayState.INCOMPLETE, dis_B.deriveDisplayState());
    
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as deployed.", DisplayState.DEPLOYED, dis_B.deriveDisplayState());
    
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.ERROR_DURING_ROLLBACK, true, Optional.of(new NullPointerException()));
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as incomplete.", DisplayState.INCOMPLETE, dis_B.deriveDisplayState());
    
    registry.save(di_B);
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should still appear as incomplete.", DisplayState.INCOMPLETE, dis_B.deriveDisplayState());
    
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as deployed.", DisplayState.DEPLOYED, dis_B.deriveDisplayState());
  }
  
  
  /* invalid in A_s, A_d, B_s, B_d
   * for A invokes B
   * */
  @Test
  public void testInvalidDetection_A_s() {
    DeploymentItem di_B = get_B_definition();
    registry.save(di_B);
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_A = get_A_definition();
    registry.save(di_A);
    registry.collectUsingObjectsInContext(di_A.getName(), emptyCtx());
    registry.deployFinished(di_A.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_A_DT = get_A_DT_definition();
    registry.save(di_A_DT);
    registry.collectUsingObjectsInContext(di_A_DT.getName(), emptyCtx());
    registry.deployFinished(di_A_DT.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_A2 = get_A2_definition();
    registry.save(di_A2);
    
    DeploymentItemState dis_A = registry.get(di_A.getName());
    DeploymentItemState dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_A.deriveDisplayState());
    assertSame("There should have been an inconsistency",  1, dis_A.getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.SAVED, false).size());
    assertSame("There should have been an inconsistency",  1, dis_A.getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.DEPLOYED, false).size());
    assertSame("There should have been no inconsistency",  0, dis_A.getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.SAVED, false).size());
    assertSame("There should have been no inconsistency",  0, dis_A.getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.DEPLOYED, false).size());
    assertEquals("Object should appear as invalid.", DisplayState.DEPLOYED, dis_B.deriveDisplayState());
  }
  
  @Test
  public void testInvalidDetection_A_d() {
    DeploymentItem di_B = get_B_definition();
    registry.save(di_B);
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_A_DT = get_A_DT_definition();
    registry.save(di_A_DT);
    registry.collectUsingObjectsInContext(di_A_DT.getName(), emptyCtx());
    registry.deployFinished(di_A_DT.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_A2 = get_A2_definition();
    registry.save(di_A2);
    registry.collectUsingObjectsInContext(di_A2.getName(), emptyCtx());
    registry.deployFinished(di_A2.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_A = get_A_definition();
    registry.save(di_A);
    
    DeploymentItemState dis_A = registry.get(di_A.getName());
    DeploymentItemState dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_A.deriveDisplayState());
    assertSame("There should have been no inconsistency",  0, dis_A.getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.SAVED, false).size());
    assertSame("There should have been no inconsistency",  0, dis_A.getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.DEPLOYED, false).size());
    assertSame("There should have been an inconsistency",  1, dis_A.getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.SAVED, false).size());
    assertSame("There should have been an inconsistency",  1, dis_A.getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.DEPLOYED, false).size());
    assertEquals("Object should appear as invalid.", DisplayState.DEPLOYED, dis_B.deriveDisplayState());
  }
  
  
  @Test
  public void testInvalidDetection_B_s() {
    DeploymentItem di_B = get_B_definition();
    registry.save(di_B);
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_A = get_A_definition();
    registry.save(di_A);
    registry.collectUsingObjectsInContext(di_A.getName(), emptyCtx());
    registry.deployFinished(di_A.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_A_DT = get_A_DT_definition();
    registry.save(di_A_DT);
    registry.collectUsingObjectsInContext(di_A_DT.getName(), emptyCtx());
    registry.deployFinished(di_A_DT.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_B2 = get_B2_definition();
    registry.save(di_B2);
    
    DeploymentItemState dis_A = registry.get(di_A.getName());
    DeploymentItemState dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_A.deriveDisplayState());
    assertSame("There should have been an inconsistency",  1, dis_A.getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.SAVED, false).size());
    assertSame("There should have been an inconsistency",  0, dis_A.getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.DEPLOYED, false).size());
    assertSame("There should have been no inconsistency",  1, dis_A.getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.SAVED, false).size());
    assertSame("There should have been no inconsistency",  0, dis_A.getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.DEPLOYED, false).size());
    assertEquals("Object should appear as invalid.", DisplayState.CHANGED, dis_B.deriveDisplayState());
  }
  
  
  @Test
  public void testInvalidDetection_B_d() {
    DeploymentItem di_A_DT = get_A_DT_definition();
    registry.save(di_A_DT);
    registry.collectUsingObjectsInContext(di_A_DT.getName(), emptyCtx());
    registry.deployFinished(di_A_DT.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_B2 = get_B2_definition();
    registry.save(di_B2);
    registry.collectUsingObjectsInContext(di_B2.getName(), emptyCtx());
    registry.deployFinished(di_B2.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_A = get_A_definition();
    registry.save(di_A);
    registry.collectUsingObjectsInContext(di_A.getName(), emptyCtx());
    registry.deployFinished(di_A.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_B = get_B_definition();
    registry.save(di_B);    
    
    DeploymentItemState dis_A = registry.get(di_A.getName());
    DeploymentItemState dis_B = registry.get(di_B.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_A.deriveDisplayState());
    assertSame("There should have been an inconsistency",  0, dis_A.getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.SAVED, false).size());
    assertSame("There should have been an inconsistency",  1, dis_A.getInconsistencies(DeploymentLocation.SAVED, DeploymentLocation.DEPLOYED, false).size());
    assertSame("There should have been no inconsistency",  0, dis_A.getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.SAVED, false).size());
    assertSame("There should have been no inconsistency",  1, dis_A.getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.DEPLOYED, false).size());
    assertEquals("Object should appear as invalid.", DisplayState.CHANGED, dis_B.deriveDisplayState());
  }
  
  
  @Test
  public void testCodeRegenerationAdditionForDeactivation() {
    DeploymentItem di_B = get_B_definition();
    registry.save(di_B);
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_A = get_A_definition();
    registry.save(di_A);
    registry.collectUsingObjectsInContext(di_A.getName(), emptyCtx());
    registry.deployFinished(di_A.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    DeploymentItemState dis_A = registry.get(di_A.getName());
    assertEquals("Object should appear as deployed.", DisplayState.DEPLOYED, dis_A.deriveDisplayState());
    
    DeploymentItem di_A_DT = get_A_DT_definition();
    registry.save(di_A_DT);
    registry.collectUsingObjectsInContext(di_A_DT.getName(), emptyCtx());
    registry.deployFinished(di_A_DT.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_B2 = get_B2_definition();
    registry.save(di_B2);
    
    GenerationBase gb = createGenBaseMock(di_B2);
    DeploymentContext ctx = new TestDeploymentContext(Collections.singletonList(Pair.of(gb, DeploymentMode.codeChanged)));
    registry.collectUsingObjectsInContext(di_B2.getName(), ctx);
    registry.deployFinished(di_B2.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    Optional<DeploymentMode> mode = ctx.getDeploymentMode(di_A.getType(), di_A.getName(), gb.getRevision());
    assertTrue("A should have been added for deactivation", mode.isPresent());
    assertEquals("A should have been added with regenerateDeployed", mode.get(), DeploymentMode.regenerateDeployed);
    
    dis_A = registry.get(di_A.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_A.deriveDisplayState());
  }
  
  
  @Test
  public void testCodeRegenerationAdditionForActivation() {
    DeploymentItem di_B = get_B_definition();
    registry.save(di_B);
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_A_DT = get_A_DT_definition();
    registry.save(di_A_DT);
    registry.collectUsingObjectsInContext(di_A_DT.getName(), emptyCtx());
    registry.deployFinished(di_A_DT.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_A2 = get_A2_definition();
    registry.save(di_A2);
    registry.collectUsingObjectsInContext(di_A2.getName(), emptyCtx());
    registry.deployFinished(di_A2.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    DeploymentItemState dis_A2 = registry.get(di_A2.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_A2.deriveDisplayState());
    
    DeploymentItem di_B2 = get_B2_definition();
    registry.save(di_B2);
    
    GenerationBase gb = createGenBaseMock(di_B2);
    DeploymentContext ctx = new TestDeploymentContext(Collections.singletonList(Pair.of(gb, DeploymentMode.codeChanged)));
    registry.collectUsingObjectsInContext(di_B2.getName(), ctx);
    registry.deployFinished(di_B2.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    Optional<DeploymentMode> mode = ctx.getDeploymentMode(di_A2.getType(), di_A2.getName(), gb.getRevision());
    assertTrue("A should have been added for deactivation", mode.isPresent());
    assertEquals("A should have been added with regenerateDeployed", mode.get(), DeploymentMode.regenerateDeployed);
    
    dis_A2 = registry.get(di_A2.getName());
    assertEquals("Object should appear as deployed.", DisplayState.DEPLOYED, dis_A2.deriveDisplayState());
  }
  
  
  @Test
  public void testCodeRegenerationAdditionFromUndeploymentOrDeletion() {
    DeploymentItem di_B = get_B_definition();
    registry.save(di_B);
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItem di_A = get_A_definition();
    registry.save(di_A);
    registry.collectUsingObjectsInContext(di_A.getName(), emptyCtx());
    registry.deployFinished(di_A.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    DeploymentItemState dis_A = registry.get(di_A.getName());
    assertEquals("Object should appear as deployed.", DisplayState.DEPLOYED, dis_A.deriveDisplayState());

    
    GenerationBase gb = createGenBaseMock(di_B);
    DeploymentContext ctx = new TestDeploymentContext(Collections.singletonList(Pair.of(gb, DeploymentMode.doNothing)));
    registry.undeploy(di_B.getName(), ctx);
    Optional<DeploymentMode> mode = ctx.getDeploymentMode(di_A.getType(), di_A.getName(), gb.getRevision());
    assertTrue("A should have been added for deactivation", mode.isPresent());
    assertEquals("A should have been added with regenerateDeployed", mode.get(), DeploymentMode.regenerateDeployed);
    
    dis_A = registry.get(di_A.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_A.deriveDisplayState());
    
    registry.collectUsingObjectsInContext(di_B.getName(), emptyCtx());
    registry.deployFinished(di_B.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    dis_A = registry.get(di_A.getName());
    assertEquals("Object should appear as deployed.", DisplayState.DEPLOYED, dis_A.deriveDisplayState());
    
    registry.delete(di_B.getName(), ctx);
    mode = ctx.getDeploymentMode(di_A.getType(), di_A.getName(), gb.getRevision());
    assertTrue("A should have been added for deactivation", mode.isPresent());
    assertEquals("A should have been added with regenerateDeployed", mode.get(), DeploymentMode.regenerateDeployed);
    
    dis_A = registry.get(di_A.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_A.deriveDisplayState());
  }
  
  
  @Test
  public void testMemberVarResolution() {
    DeploymentItem di_C = get_C_definition();
    registry.save(di_C);
    registry.collectUsingObjectsInContext(di_C.getName(), emptyCtx());
    registry.deployFinished(di_C.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItemState dis_C = registry.get(di_C.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_C.deriveDisplayState());
    
    DeploymentItem di_A_DT = get_A_DT_definition();
    registry.save(di_A_DT);
    registry.collectUsingObjectsInContext(di_A_DT.getName(), emptyCtx());
    registry.deployFinished(di_A_DT.getName(), DeploymentTransition.SUCCESS, true,  Optional.<Throwable>empty());
    
    DeploymentItemState dis_A_DT = registry.get(di_A_DT.getName());
    assertEquals("Object should appear as deployed.", DisplayState.DEPLOYED, dis_A_DT.deriveDisplayState());
    dis_C = registry.get(di_C.getName());
    assertEquals("Object should appear as deployed.", DisplayState.DEPLOYED, dis_C.deriveDisplayState());
  }
  
  
  @Test
  public void testMemberVarResolutionInSupertype() {
    DeploymentItem di_C = get_C_definition();
    registry.save(di_C);
    
    DeploymentItemState dis_C = registry.get(di_C.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_C.deriveDisplayState());
    
    DeploymentItem di_B_DT = get_B_DT_definition();
    registry.save(di_B_DT);
    
    DeploymentItemState dis_B_DT = registry.get(di_B_DT.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_B_DT.deriveDisplayState());
    
    DeploymentItem di_A_DT = get_A_DT_definition();
    registry.save(di_A_DT);
    
    DeploymentItemState dis_A_DT = registry.get(di_A_DT.getName());
    assertEquals("Object should appear as saved.", DisplayState.SAVED, dis_A_DT.deriveDisplayState());
    assertEquals("Object should appear as saved.", DisplayState.SAVED, dis_B_DT.deriveDisplayState());
    assertEquals("Object should appear as saved.", DisplayState.SAVED, dis_C.deriveDisplayState());
    
    DeploymentItem di_C2 = get_C2_definition();
    registry.save(di_C2);
    
    DeploymentItemState dis_C2 = registry.get(di_C2.getName());
    assertEquals("Object should appear as saved.", DisplayState.SAVED, dis_C2.deriveDisplayState());
  }
  
  
  @Test
  public void testOperationResolutionInSupertype() {
    DeploymentItem di_D = get_D_definition();
    registry.save(di_D);
    
    DeploymentItemState dis_D = registry.get(di_D.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_D.deriveDisplayState());
    
    DeploymentItem di_A_ISG = get_A_ISG_definition();
    registry.save(di_A_ISG);
    
    DeploymentItemState dis_A_ISG = registry.get(di_A_ISG.getName());
    assertEquals("Object should appear as saved.", DisplayState.SAVED, dis_A_ISG.deriveDisplayState());
    
    dis_D = registry.get(di_D.getName());
    assertEquals("Object should appear as saved.", DisplayState.SAVED, dis_D.deriveDisplayState());
    
    DeploymentItem di_D2 = get_D2_definition();
    registry.save(di_D2);
    
    DeploymentItemState dis_D2 = registry.get(di_D2.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_D2.deriveDisplayState());
    
    DeploymentItem di_B_ISG = get_B_ISG_definition();
    registry.save(di_B_ISG);
    
    DeploymentItemState dis_B_ISG = registry.get(di_B_ISG.getName());
    assertEquals("Object should appear as saved.", DisplayState.SAVED, dis_B_ISG.deriveDisplayState());
    assertEquals("Object should appear as saved.", DisplayState.SAVED, dis_D2.deriveDisplayState());
    
    registry.delete(di_A_ISG.getName(), emptyCtx());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_B_ISG.deriveDisplayState());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_D2.deriveDisplayState());
    
  }
  
  
  @Test
  public void testOperationResolutionWithSubType() {
    DeploymentItem di_A_SSG = get_A_SSG_definition();
    registry.save(di_A_SSG);
    
    DeploymentItemState dis_A_SSG = registry.get(di_A_SSG.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_A_SSG.deriveDisplayState());
    
    DeploymentItem di_A_DT = get_A_DT_definition();
    registry.save(di_A_DT);
    
    assertEquals("Object should appear as saved.", DisplayState.SAVED, dis_A_SSG.deriveDisplayState());
    
    DeploymentItem di_A_SSG_Invocation = new DeploymentItem("di_A_SSG_Invocation", XMOMType.WORKFLOW);
    di_A_SSG_Invocation.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A_SSG_Invocation));
    di_A_SSG_Invocation.addInterfaceEmployment(DeploymentLocation.SAVED, operationInvocation(di_A_SSG, "aStaticOperation1"));
    registry.save(di_A_SSG_Invocation);
    
    DeploymentItemState dis_A_SSG_Invocation = registry.get(di_A_SSG_Invocation.getName());
    assertEquals("Object should appear as saved.", DisplayState.SAVED, dis_A_SSG_Invocation.deriveDisplayState());
    
    di_A_SSG_Invocation = new DeploymentItem("di_A_SSG_Invocation", XMOMType.WORKFLOW);
    di_A_SSG_Invocation.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A_SSG_Invocation));
    di_A_SSG_Invocation.addInterfaceEmployment(DeploymentLocation.SAVED, operationInvocation(di_A_SSG, "aStaticOperation2"));
    setDefaultValues(di_A_SSG_Invocation);
    registry.save(di_A_SSG_Invocation);
    
    dis_A_SSG_Invocation = registry.get(di_A_SSG_Invocation.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_A_SSG_Invocation.deriveDisplayState());
    
    di_A_SSG_Invocation = new DeploymentItem("di_A_SSG_Invocation", XMOMType.WORKFLOW);
    di_A_SSG_Invocation.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A_SSG_Invocation));
    di_A_SSG_Invocation.addInterfaceEmployment(DeploymentLocation.SAVED, operationInvocation(di_A_SSG, "aStaticOperation2", Arrays.asList(di_A_DT)));
    setDefaultValues(di_A_SSG_Invocation);
    registry.save(di_A_SSG_Invocation);
    
    dis_A_SSG_Invocation = registry.get(di_A_SSG_Invocation.getName());
    assertEquals("Object should appear as invalid.", DisplayState.INVALID, dis_A_SSG_Invocation.deriveDisplayState());
    
    di_A_SSG_Invocation = new DeploymentItem("di_A_SSG_Invocation", XMOMType.WORKFLOW);
    di_A_SSG_Invocation.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A_SSG_Invocation));
    di_A_SSG_Invocation.addInterfaceEmployment(DeploymentLocation.SAVED, operationInvocation(di_A_SSG, "aStaticOperation2", Arrays.asList(di_A_DT, di_A_DT)));
    setDefaultValues(di_A_SSG_Invocation);
    registry.save(di_A_SSG_Invocation);
    
    dis_A_SSG_Invocation = registry.get(di_A_SSG_Invocation.getName());
    assertEquals("Object should appear as saved.", DisplayState.SAVED, dis_A_SSG_Invocation.deriveDisplayState());
    
    DeploymentItem di_B_DT = get_B_DT_definition();
    registry.save(di_B_DT);
    
    di_A_SSG_Invocation = new DeploymentItem("di_A_SSG_Invocation", XMOMType.WORKFLOW);
    di_A_SSG_Invocation.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A_SSG_Invocation));
    di_A_SSG_Invocation.addInterfaceEmployment(DeploymentLocation.SAVED, operationInvocation(di_A_SSG, "aStaticOperation2", Arrays.asList(di_A_DT, get_B_DT_definition())));
    setDefaultValues(di_A_SSG_Invocation);
    registry.save(di_A_SSG_Invocation);
    
    dis_A_SSG_Invocation = registry.get(di_A_SSG_Invocation.getName());
    assertEquals("B should now be registered.", DisplayState.SAVED, dis_A_SSG_Invocation.deriveDisplayState());
  }

  
  @Test
  public void testBadBehaviourOnOperationResolutionWithSubType() {
    DeploymentItem di_A_DT = get_A_DT_definition();
    registry.save(di_A_DT);
    
    DeploymentItem di_A_SSG = get_A_SSG_definition();
    registry.save(di_A_SSG);
    
    DeploymentItem di_A_SSG_Invocation = new DeploymentItem("di_A_SSG_Invocation", XMOMType.WORKFLOW);
    di_A_SSG_Invocation.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A_SSG_Invocation));
    di_A_SSG_Invocation.addInterfaceEmployment(DeploymentLocation.SAVED, operationInvocation(di_A_SSG, "aStaticOperation2", Arrays.asList(di_A_DT, get_B_DT_definition())));
    setDefaultValues(di_A_SSG_Invocation);
    registry.save(di_A_SSG_Invocation);
    // this testcase motivated the removal of an early break on interface resolution, reason being:
    // if registry.save(di_B_DT) happens after this call 
    // this save creates a non existing operation with B_DT in di_A_SSG because the inheritance hierarchy could not be resolved
    // once B_DT is registered this phantom does still exist and might be resolved before the actual method
    
    DeploymentItemState dis_A_SSG_Invocation = registry.get(di_A_SSG_Invocation.getName());
    assertEquals("B_DT is not registred.", DisplayState.INVALID, dis_A_SSG_Invocation.deriveDisplayState());
    
    DeploymentItem di_B_DT = get_B_DT_definition();
    registry.save(di_B_DT);
    
    dis_A_SSG_Invocation = registry.get(di_A_SSG_Invocation.getName());
    assertEquals("B_DT should now be registered.", DisplayState.SAVED, dis_A_SSG_Invocation.deriveDisplayState());
  }
  
  
  @Test
  public void testSimpleAccessChain() {
    // String = %0%.b.c().d
    DeploymentItem di_C_DT = new DeploymentItem("C", XMOMType.DATATYPE);;
    di_C_DT.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_C_DT, true));
    di_C_DT.addPublishedInterface(DeploymentLocation.SAVED, MemberVariableInterface.of("d", String.class.getName(), true, false));
    registry.save(di_C_DT);
    
    DeploymentItem di_B_DT = new DeploymentItem("B", XMOMType.DATATYPE);;
    di_B_DT.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_B_DT, true));
    di_B_DT.addPublishedInterface(DeploymentLocation.SAVED, publishedOperation("c", OperationType.INSTANCE_SERVICE, new ArrayList<DeploymentItem>(Arrays.asList(di_B_DT)), new ArrayList<DeploymentItem>(Arrays.asList(di_C_DT))));
    registry.save(di_B_DT);
    
    DeploymentItem di_A_DT = new DeploymentItem("A", XMOMType.DATATYPE);;
    di_A_DT.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A_DT, true));
    di_A_DT.addPublishedInterface(DeploymentLocation.SAVED, MemberVariableInterface.of("b", "B", false, false));
    registry.save(di_A_DT);
    
    TypeInterface expectedType = TypeInterface.of(String.class.getName());
    TypeInterface rootType = typeInterface(di_A_DT);
    
    AccessPart d_part = AccessChain.MemberVarAccessPart.of(MemberVariableInterface.of("d"), false);
    AccessPart c_part = AccessChain.InstanceMethodAccessPart.of(OperationInterface.of("c", new ArrayList<TypeInterface>(), new ArrayList<TypeInterface>(Arrays.asList(TypeInterface.anyType()))));
    AccessPart b_part = AccessChain.MemberVarAccessPart.of(MemberVariableInterface.of("b"), false);
    
    c_part.setNext(d_part);
    b_part.setNext(c_part);

    InterfaceResolutionContext.resCtx.set(new InterfaceResolutionContext(registry, DeploymentLocation.SAVED));
    InterfaceResolutionContext.resCtx.get().localState(registry.get(di_A_DT.getName()));
    try {
    
      AccessChain uac = AccessChain.of(rootType, b_part);
      assertTrue("UnqualifiedAccessChain should have been resolvable", uac.matches(expectedType));
      
    } finally {
      assertTrue(InterfaceResolutionContext.resCtx.get().pop());
      InterfaceResolutionContext.resCtx.remove();
    }
  }
  
  
  @Test
  public void testNestedAccessChain() {
    // String = %0%.b.c(%1%.e).d
    DeploymentItem di_E_DT = new DeploymentItem("E", XMOMType.DATATYPE);;
    di_E_DT.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_E_DT, true));
    di_E_DT.addPublishedInterface(DeploymentLocation.SAVED, MemberVariableInterface.of("e", Integer.class.getName(), true, false));
    di_E_DT.addPublishedInterface(DeploymentLocation.SAVED, MemberVariableInterface.of("e2", String.class.getName(), true, false));
    registry.save(di_E_DT);
    
    DeploymentItem di_C_DT = new DeploymentItem("C", XMOMType.DATATYPE);;
    di_C_DT.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_C_DT, true));
    di_C_DT.addPublishedInterface(DeploymentLocation.SAVED, MemberVariableInterface.of("d", String.class.getName(), true, false));
    registry.save(di_C_DT);
    
    DeploymentItem di_B_DT = new DeploymentItem("B", XMOMType.DATATYPE);;
    di_B_DT.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_B_DT, true));
    di_B_DT.addPublishedInterface(DeploymentLocation.SAVED, publishedOperation("c", OperationType.INSTANCE_SERVICE,
                                                                                    Arrays.asList(di_B_DT, new DeploymentItem(Integer.class.getName(), (XMOMType) null)),
                                                                                    Arrays.asList(di_C_DT)));
    registry.save(di_B_DT);
    
    DeploymentItem di_A_DT = new DeploymentItem("A", XMOMType.DATATYPE);;
    di_A_DT.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A_DT, true));
    di_A_DT.addPublishedInterface(DeploymentLocation.SAVED, MemberVariableInterface.of("b", "B", false, false));
    registry.save(di_A_DT);
    
    InterfaceResolutionContext.resCtx.set(new InterfaceResolutionContext(registry, DeploymentLocation.SAVED));
    InterfaceResolutionContext.resCtx.get().localState(registry.get(di_A_DT.getName()));
    try {
    
      TypeInterface sub_rootType = typeInterface(di_E_DT);
      
      AccessPart e_part = AccessChain.MemberVarAccessPart.of(MemberVariableInterface.of("e2"), false);
      AccessChain sub_uac = AccessChain.of(sub_rootType, e_part);
      
      TypeInterface expectedType = TypeInterface.of(String.class.getName());
      TypeInterface rootType = typeInterface(di_A_DT);
      
      AccessPart d_part = AccessChain.MemberVarAccessPart.of(MemberVariableInterface.of("d"), false);
      AccessPart c_part = AccessChain.InstanceMethodAccessPart.of(OperationInterface.of("c",
                                                                                        new ArrayList<TypeInterface>(Arrays.<TypeInterface>asList(sub_uac)),
                                                                                        new ArrayList<TypeInterface>(Arrays.asList(TypeInterface.anyType()))));
      AccessPart b_part = AccessChain.MemberVarAccessPart.of(MemberVariableInterface.of("b"), false);
      
      c_part.setNext(d_part);
      b_part.setNext(c_part);
    
    
      AccessChain main_uac = AccessChain.of(rootType, b_part);
      assertFalse("UnqualifiedAccessChain should not have been resolvable when accessing e2", main_uac.matches(expectedType));
      
      e_part = AccessChain.MemberVarAccessPart.of(MemberVariableInterface.of("e"), false);
      sub_uac = AccessChain.of(sub_rootType, e_part);
      
      d_part = AccessChain.MemberVarAccessPart.of(MemberVariableInterface.of("d"), false);
      c_part = AccessChain.InstanceMethodAccessPart.of(OperationInterface.of("c",
                                                                             new ArrayList<TypeInterface>(Arrays.<TypeInterface>asList(sub_uac)),
                                                                             new ArrayList<TypeInterface>(Arrays.asList(TypeInterface.anyType()))));
      b_part = AccessChain.MemberVarAccessPart.of(MemberVariableInterface.of("b"), false);
      
      c_part.setNext(d_part);
      b_part.setNext(c_part);
      
      main_uac = AccessChain.of(rootType, b_part);
      assertTrue("UnqualifiedAccessChain should have been resolvable when accessing e", main_uac.matches(expectedType));
    } finally {
      assertTrue(InterfaceResolutionContext.resCtx.get().pop());
      InterfaceResolutionContext.resCtx.remove();
    }
  }
  
  
  private void setDefaultValues(DeploymentItem di) {
    //di.setDeployed(true);
    di.setLastModified(System.currentTimeMillis());
  }

  private DeploymentItem get_A_definition() {
    DeploymentItem di_A = new DeploymentItem("A", XMOMType.WORKFLOW);
    di_A.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A, true));
    di_A.addInterfaceEmployment(DeploymentLocation.SAVED, operationInvocation(get_B_definition(), "B"));
    setDefaultValues(di_A);
    return di_A;
  }
  

  private DeploymentItem get_A2_definition() {
    DeploymentItem di_A2 = new DeploymentItem("A", XMOMType.WORKFLOW);
    di_A2.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A2, true));
    di_A2.addInterfaceEmployment(DeploymentLocation.SAVED, operationInvocation(get_B2_definition(), "B", Arrays.asList(get_A_DT_definition())));
    setDefaultValues(di_A2);
    return di_A2;
  }
  
  private DeploymentItem get_B_definition() {
    DeploymentItem di_B = new DeploymentItem("B", XMOMType.WORKFLOW);
    di_B.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_B, true));
    di_B.addPublishedInterface(DeploymentLocation.SAVED, publishedOperation("B", OperationType.WORKFLOW));
    setDefaultValues(di_B);
    return di_B;
  }
  
  private DeploymentItem get_B2_definition() {
    DeploymentItem di_B2 = new DeploymentItem("B", XMOMType.WORKFLOW);
    di_B2.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_B2, true));
    di_B2.addPublishedInterface(DeploymentLocation.SAVED, publishedOperation("B", OperationType.WORKFLOW, Arrays.asList(get_A_DT_definition())));
    setDefaultValues(di_B2);
    return di_B2;
  }
  
  // access memVar aVar on A_DT
  private DeploymentItem get_C_definition() {
    DeploymentItem di_C = new DeploymentItem("C", XMOMType.WORKFLOW);
    di_C.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_C, true));
    di_C.addPublishedInterface(DeploymentLocation.SAVED, publishedOperation("C", OperationType.WORKFLOW));
    di_C.addInterfaceEmployment(DeploymentLocation.SAVED, membVarAccess(get_A_DT_definition(), "aVar"));
    setDefaultValues(di_C);
    return di_C;
  }
  
  //access memVar aVar on B_DT extends A_DT
  private DeploymentItem get_C2_definition() {
    DeploymentItem di_C = new DeploymentItem("C", XMOMType.WORKFLOW);
    di_C.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_C, true));
    di_C.addPublishedInterface(DeploymentLocation.SAVED, publishedOperation("C", OperationType.WORKFLOW));
    di_C.addInterfaceEmployment(DeploymentLocation.SAVED, membVarAccess(get_B_DT_definition(), "aVar"));
    setDefaultValues(di_C);
    return di_C;
  }
  
  // invoke instance service aInstanceOperation on A_ISG
  private DeploymentItem get_D_definition() {
    DeploymentItem di_D = new DeploymentItem("D", XMOMType.WORKFLOW);
    di_D.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_D, true));
    di_D.addPublishedInterface(DeploymentLocation.SAVED, publishedOperation("D", OperationType.WORKFLOW));
    di_D.addInterfaceEmployment(DeploymentLocation.SAVED, operationInvocation(get_A_ISG_definition(), "aInstanceOperation"));
    setDefaultValues(di_D);
    return di_D;
  }
  
  //invoke instance service aInstanceOperation on B_ISG extends A_ISG
  private DeploymentItem get_D2_definition() {
    DeploymentItem di_D = new DeploymentItem("D", XMOMType.WORKFLOW);
    di_D.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_D, true));
    di_D.addPublishedInterface(DeploymentLocation.SAVED, publishedOperation("D", OperationType.WORKFLOW));
    di_D.addInterfaceEmployment(DeploymentLocation.SAVED, operationInvocation(get_B_ISG_definition(), "aInstanceOperation"));
    setDefaultValues(di_D);
    return di_D;
  }
  
  
  private DeploymentItem get_A_DT_definition() {
    DeploymentItem di_A_DT = new DeploymentItem("A_DT", XMOMType.DATATYPE);
    di_A_DT.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A_DT, true));
    di_A_DT.addPublishedInterface(DeploymentLocation.SAVED, MemberVariableInterface.of("aVar", String.class.getName(), true, false));
    setDefaultValues(di_A_DT);
    return di_A_DT;
  }
  
  
  private DeploymentItem get_B_DT_definition() {
    DeploymentItem di_B_DT = new DeploymentItem("B_DT", XMOMType.DATATYPE);
    di_B_DT.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_B_DT, true));
    di_B_DT.addPublishedInterface(DeploymentLocation.SAVED, supertype(get_A_DT_definition()));
    di_B_DT.addInterfaceEmployment(DeploymentLocation.SAVED, typeInterface(get_A_DT_definition()));
    di_B_DT.addPublishedInterface(DeploymentLocation.SAVED, MemberVariableInterface.of("bVar", String.class.getName(), true, false));
    setDefaultValues(di_B_DT);
    return di_B_DT;
  }
  
  private DeploymentItem get_A_SSG_definition() {
    DeploymentItem di_A_SSG = new DeploymentItem("A_SSG", XMOMType.DATATYPE);
    di_A_SSG.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A_SSG, true));
    di_A_SSG.addPublishedInterface(DeploymentLocation.SAVED, publishedOperation("aStaticOperation1", OperationType.STATIC_SERVICE));
    di_A_SSG.addPublishedInterface(DeploymentLocation.SAVED, publishedOperation("aStaticOperation2", OperationType.STATIC_SERVICE, Arrays.asList(get_A_DT_definition(), get_A_DT_definition())));
    di_A_SSG.addInterfaceEmployment(DeploymentLocation.SAVED, typeInterface(get_A_DT_definition()));
    setDefaultValues(di_A_SSG);
    return di_A_SSG;
  }
  
  private DeploymentItem get_A_ISG_definition() {
    DeploymentItem di_A_ISG = new DeploymentItem("A_ISG", XMOMType.DATATYPE);
    di_A_ISG.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_A_ISG, true));
    di_A_ISG.addPublishedInterface(DeploymentLocation.SAVED, publishedOperation("aInstanceOperation", OperationType.INSTANCE_SERVICE));
    setDefaultValues(di_A_ISG);
    return di_A_ISG;
  }
  
  
  private DeploymentItem get_B_ISG_definition() {
    DeploymentItem di_B_ISG = new DeploymentItem("B_ISG", XMOMType.DATATYPE);
    di_B_ISG.addPublishedInterface(DeploymentLocation.SAVED, typeInterface(di_B_ISG, true));
    di_B_ISG.addPublishedInterface(DeploymentLocation.SAVED, supertype(get_A_ISG_definition()));
    di_B_ISG.addPublishedInterface(DeploymentLocation.SAVED, publishedOperation("bInstanceOperation", OperationType.INSTANCE_SERVICE));
    di_B_ISG.addInterfaceEmployment(DeploymentLocation.SAVED, typeInterface(get_A_ISG_definition()));
    setDefaultValues(di_B_ISG);
    return di_B_ISG;
  }
  
  
  private GenerationBase createGenBaseMock(DeploymentItem di) {
    GenerationBase mock;
    switch (di.getType()) {
      case WORKFLOW :
        mock = EasyMock.createMock(WF.class);
        break;
      case DATATYPE :
        mock = EasyMock.createMock(DOM.class);
        break;
      case EXCEPTION :
        mock = EasyMock.createMock(ExceptionGeneration.class);
        break;
      default :
        throw new IllegalArgumentException("sfdajlksahfjklahsd");
    }
    EasyMock.expect(mock.getOriginalFqName()).andReturn(di.getName()).anyTimes();
    EasyMock.expect(mock.getRevision()).andReturn(-1L).anyTimes();
    EasyMock.replay(mock);
    return mock;
  }
  
  
  
  static SupertypeInterface supertype(DeploymentItem di) {
    return SupertypeInterface.of(di.getName(), di.getType());
  }
  
  static InterfaceEmployment operationInvocation(DeploymentItem di, String operationName) {
    return operationInvocation(di, operationName, Collections.<DeploymentItem>emptyList());
  }
  
  static InterfaceEmployment operationInvocation(DeploymentItem di, String operationName, List<DeploymentItem> input) {
    return operationInvocation(di, operationName, input, Collections.<DeploymentItem>emptyList());
  }
  
  static InterfaceEmployment operationInvocation(DeploymentItem di, String operationName, List<DeploymentItem> input, List<DeploymentItem> output) {
    List<TypeInterface> inputTypes = new ArrayList<TypeInterface>();
    for (DeploymentItem in : input) {
      inputTypes.add(typeInterface(in));
    }
    List<TypeInterface> outputTypes = new ArrayList<TypeInterface>();
    for (DeploymentItem out : output) {
      outputTypes.add(typeInterface(out));
    }
    return InterfaceEmployment.of(typeInterface(di), OperationInterface.of(operationName, inputTypes, outputTypes));
  }
  
  
  static InterfaceEmployment membVarAccess(DeploymentItem di, String variableName) {
    return InterfaceEmployment.of(typeInterface(di), MemberVariableInterface.of(variableName));
  }
  
  
  static TypeInterface typeInterface(DeploymentItem di, boolean withType) {
    if (withType) {
      return TypeInterface.of(di.getName(), di.getType());
    } else {
      return TypeInterface.of(di.getName());      
    }
  }
  
  static TypeInterface typeInterface(DeploymentItem di) {
    return typeInterface(di, false);
  }
  
  static DeploymentItemInterface publishedOperation(String operationName, OperationType type) {
    return publishedOperation(operationName, type, Collections.<DeploymentItem>emptyList());
  }    
  
  static DeploymentItemInterface publishedOperation(String operationName, OperationType type, List<DeploymentItem> input) {
    return publishedOperation(operationName, type, input, Collections.<DeploymentItem>emptyList());
  }
  
  static DeploymentItemInterface publishedOperation(String operationName, OperationType type, List<DeploymentItem> input, List<DeploymentItem> output) {
    return publishedOperation(operationName, type, input, output, Collections.<DeploymentItem>emptyList());
  }
  
  static DeploymentItemInterface publishedOperation(String operationName, OperationType type, List<DeploymentItem> input, List<DeploymentItem> output, List<DeploymentItem> exceptions) {
    List<TypeInterface> inputTypes = new ArrayList<TypeInterface>();
    for (DeploymentItem in : input) {
      inputTypes.add(typeInterface(in));
    }
    List<TypeInterface> outputTypes = new ArrayList<TypeInterface>();
    for (DeploymentItem out : output) {
      outputTypes.add(typeInterface(out));
    }
    List<TypeInterface> exceptionTypes = new ArrayList<TypeInterface>();
    for (DeploymentItem excep : exceptions) {
      exceptionTypes.add(typeInterface(excep));
    }
    return OperationInterface.of(operationName, type, ImplementationType.CONCRETE, inputTypes, outputTypes, exceptionTypes);
  }
  
  
  static class TestDeploymentContext extends DeploymentContext {
    
    private final EnumMap<XMOMType, Map<String, DeploymentMode>> objectsInCurrentDeployment;
    private final EnumMap<XMOMType, Map<String, DeploymentMode>> additionalObjectsForCodeRegeneration;
    

    public TestDeploymentContext(List<Pair<GenerationBase, DeploymentMode>> objectsInCurrentDeployment) {
      super(null);
      this.objectsInCurrentDeployment = new EnumMap<XMOMType, Map<String, DeploymentMode>>(XMOMType.class);
      this.additionalObjectsForCodeRegeneration = new EnumMap<XMOMType, Map<String, DeploymentMode>>(XMOMType.class);
      for (Pair<GenerationBase, DeploymentMode> gb : objectsInCurrentDeployment) {
        XMOMType type = getXMOMTypeByInstance(gb.getFirst());
        Map<String, DeploymentMode> subMap = this.objectsInCurrentDeployment.get(type);
        if (subMap == null) {
          subMap = new HashMap<String, GenerationBase.DeploymentMode>();
          this.objectsInCurrentDeployment.put(type, subMap);
        }
        subMap.put(gb.getFirst().getOriginalFqName(), gb.getSecond());
      }
    }
    
    
    private static XMOMType getXMOMTypeByInstance(GenerationBase gb) {
      if (gb instanceof WF) {
        return XMOMType.WORKFLOW;
      } else if (gb instanceof DOM) {
        return XMOMType.DATATYPE;
      } else if (gb instanceof ExceptionGeneration) {
        return XMOMType.EXCEPTION;
      } else {
        throw new IllegalArgumentException("Unknown GenerationBase class: " + gb.getClass().getName());
      }
    }
    
    public Optional<DeploymentMode> getDeploymentMode(XMOMType type, String fqName) {
      Map<String, DeploymentMode> subMap = objectsInCurrentDeployment.get(type);
      Optional<DeploymentMode> result;
      if (subMap == null) {
        result = Optional.empty();
      } else {
        result = Optional.of(subMap.get(fqName));
      }
      if (!result.isPresent()) {
        subMap = additionalObjectsForCodeRegeneration.get(type);
        if (subMap == null) {
          result = Optional.empty();
        } else {
          result = Optional.of(subMap.get(fqName));
        }
      }
      return result;
    }
    
    
    public void addObjectForCodeRegeneration(XMOMType type, String fqName) {
      if (type == null) {
        type = XMOMType.DATATYPE;
      }
      Map<String, DeploymentMode> subMap = additionalObjectsForCodeRegeneration.get(type);
      if (subMap == null) {
        subMap = new HashMap<String, DeploymentMode>();
        additionalObjectsForCodeRegeneration.put(type, subMap);
      }
      subMap.put(fqName, DeploymentMode.regenerateDeployed);
    }
    
    
  }
  
}
