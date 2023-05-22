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
package com.gip.xyna.xfmg.xopctrl;

import org.easymock.classextension.EasyMock;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_NameContainsInvalidCharacter;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RightDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleDoesNotExistException;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.local.XynaLocalMemoryPersistenceLayer;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;

import junit.framework.TestCase;


public class RightScopeTest extends TestCase {
  
  
  private final static String PERSISTENCE_RIGHT_SCOPE = "xnwh.persistence.Storables2:[read, write, insert, delete, *]:*:*";
  private final static String STARTORDER_RIGHT_SCOPE = "xprc.xpce.startorder.Test:/.*/:/.*/:/.*/";
  private final static String TEST_RIGHT_SCOPE = "xfmg.xopctrl.RightScopeTest:[Stadt, Land, Fluss, Baum]::*:/\\w+([.]\\w+)*/";
  
  UserManagement um;
  static ODS ods;
  
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    ods = ODSImpl.getInstance(false);
    ods.registerPersistenceLayer(123L, XynaLocalMemoryPersistenceLayer.class);
    long id = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test",
                                                      ODSConnectionType.DEFAULT, new String[0]);
    ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
    id = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test",
                                                 ODSConnectionType.HISTORY, new String[0]);
    ods.setDefaultPersistenceLayer(ODSConnectionType.HISTORY, id);
    id = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test",
                                                 ODSConnectionType.ALTERNATIVE, new String[0]);
    ods.setDefaultPersistenceLayer(ODSConnectionType.ALTERNATIVE, id);
    id = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test",
                                                 ODSConnectionType.INTERNALLY_USED, new String[0]);
    ods.setDefaultPersistenceLayer(ODSConnectionType.INTERNALLY_USED, id);
    
    XynaProcessingODS xprcODS = EasyMock.createMock(XynaProcessingODS.class);
    EasyMock.expect(xprcODS.getODS()).andReturn(ods).anyTimes();
    
    XynaProcessingBase xproc = EasyMock.createMock(XynaProcessingBase.class);
    EasyMock.expect(xproc.getXynaProcessingODS()).andReturn(xprcODS).anyTimes();
    
    Configuration conf = EasyMock.createMock(Configuration.class);
    EasyMock.expect(conf.getProperty(EasyMock.isA(String.class))).andReturn("XYNA").anyTimes();
    
    XynaFactoryManagementODS xfmods = EasyMock.createMock(XynaFactoryManagementODS.class);
    EasyMock.expect(xfmods.getConfiguration()).andReturn(conf).anyTimes();
    
    XynaOperatorControl xopctrl = EasyMock.createMock(XynaOperatorControl.class);
    
    DependencyRegister depreg = EasyMock.createMock(DependencyRegister.class);
    EasyMock.expect(depreg.addDependency(EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class), EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class))).andReturn(true).anyTimes();
    
    XynaFactoryControl xfacctrl = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfacctrl.getDependencyRegister()).andReturn(depreg).anyTimes();
    
    XynaFactoryManagementBase xfm = EasyMock.createMock(XynaFactoryManagementBase.class);
    EasyMock.expect(xfm.getProperty(EasyMock.isA(String.class))).andReturn("3").anyTimes();
    EasyMock.expect(xfm.getXynaFactoryManagementODS()).andReturn(xfmods).anyTimes();
    EasyMock.expect(xfm.getXynaOperatorControl()).andReturn(xopctrl).anyTimes();
    EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfacctrl).anyTimes();
    
    FutureExecution fe = EasyMock.createMock(FutureExecution.class);
    EasyMock.expect(fe.nextId()).andReturn(1).anyTimes();
    
    XynaFactoryBase xf = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(xf.getProcessing()).andReturn(xproc).anyTimes();
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfm).anyTimes();
    EasyMock.expect(xf.getFactoryManagementPortal()).andReturn(xfm).anyTimes();
    EasyMock.expect(xf.getFutureExecution()).andReturn(fe).anyTimes();
    xf.addComponentToBeInitializedLater(EasyMock.isA(UserManagement.class));
    EasyMock.expectLastCall().anyTimes();
    
    XynaFactory.setInstance(xf);
    
    EasyMock.replay(depreg, xfacctrl, xprcODS, xproc, xfmods, xfm, xf);

    um = new UserManagement();
    
    EasyMock.expect(xopctrl.getUserManagement()).andReturn(um).anyTimes();
    EasyMock.replay(xopctrl);
    
    um.initInternally();
  }
  
  
  @Override
  protected void tearDown() throws Exception {
    ODSImpl.clearInstances();
    super.tearDown();
  }
  
  
  public void testRightScopeCreation() throws PersistenceLayerException {
    System.out.println("version:"  + System.getProperty("java.version"));
    assertTrue("PERSISTENCE_RIGHT_SCOPE should have been succesfully created.",
               um.createRightScope(PERSISTENCE_RIGHT_SCOPE, "XMOM-Persistence RightScope", "EN"));
    assertTrue("STARTORDER_RIGHT_SCOPE should have been succesfully created, even without documentation",
               um.createRightScope(STARTORDER_RIGHT_SCOPE, null, "EN"));
    assertTrue("TEST_RIGHT_SCOPE should have been succesfully created.",
               um.createRightScope(TEST_RIGHT_SCOPE, "Testing", "EN"));
    assertFalse("TEST_RIGHT_SCOPE should not have been succesfully created, as it should already exist.",
               um.createRightScope(TEST_RIGHT_SCOPE, "Testing", "EN"));
    String invalidScopeDefinition1 = "c0mpl�xK$y:*:*";
    String invalidScopeDefinition2 = "invalid:[valu$, otherValue]:*";
    String invalidScopeDefinition3 = "invalid:][:*";
    String invalidScopeDefinition4 = "invalid:something:*";
    String invalidScopeDefinition5 = "invalid:[a,[b],c]:*";
    String[] invalidScopes = {invalidScopeDefinition1, invalidScopeDefinition2, invalidScopeDefinition3, invalidScopeDefinition4, invalidScopeDefinition5};
    for (String invalidScope : invalidScopes) {
      assertFalse("Invalid Scope '" + invalidScope + "' should not have been created.",
                  um.createRightScope(invalidScope, null, "EN"));
    }
  }
  
  
  public void testScopedRightAssignement() throws PersistenceLayerException, XFMG_DomainDoesNotExistException, XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_NameContainsInvalidCharacter {
    String TEST_ROLE_NAME = "TestRole";
    assertTrue("Role " + TEST_ROLE_NAME + " should have been successfully created.",
               um.createRole(TEST_ROLE_NAME));
    assertTrue("PERSISTENCE_RIGHT_SCOPE should have been succesfully created.",
               um.createRightScope(PERSISTENCE_RIGHT_SCOPE, "XMOM-Persistence RightScope", "EN"));
    String TEST_SCOPED_RIGHT_1 = "xnwh.persistence.Storables2:read:*:*";
    String TEST_SCOPED_RIGHT_2 = "xnwh.persistence.Storables2:write:bg.*:*";
    String TEST_SCOPED_RIGHT_3 = "xnwh.persistence.Storables2:*:bg.*:mac";
    String TEST_SCOPED_RIGHT_4 = "xnwh.persistence.Storables2:*:bg.test.Device:*";
    String TEST_SCOPED_RIGHT_5 = "xnwh.persistence.Storables2:delete:bg.test.Device:mac";
    String[] validRights = {TEST_SCOPED_RIGHT_1, TEST_SCOPED_RIGHT_2, TEST_SCOPED_RIGHT_3, TEST_SCOPED_RIGHT_4, TEST_SCOPED_RIGHT_5};
    for (String validRight : validRights) {
      assertTrue("Scoped Right " + validRight + " should have been successfully granted.", 
                 um.grantRightToRole(TEST_ROLE_NAME, validRight));
    }
    
    String TEST_INVALID_SCOPED_RIGHT_1 = "xnwh.persistence.Storables2:eat:*:*";
    String TEST_INVALID_SCOPED_RIGHT_2 = "xnwh.somepath.Storables:*:*:*";
    String TEST_INVALID_SCOPED_RIGHT_3 = "xnwh.persistence.Storables2.Suffix:*:*:*";
    String TEST_INVALID_SCOPED_RIGHT_4 = "xnwh.persistence.Storables2:*:bg.path.p|p$:*";
    String TEST_INVALID_SCOPED_RIGHT_5 = "xnwh.persistence.Storables2:*:bg.*.test:*";
    String[] invalidRights = {TEST_INVALID_SCOPED_RIGHT_1, TEST_INVALID_SCOPED_RIGHT_2, TEST_INVALID_SCOPED_RIGHT_3,
                              TEST_INVALID_SCOPED_RIGHT_4, TEST_INVALID_SCOPED_RIGHT_5};
    for (String invalidRight : invalidRights) {
      assertFalse("Scoped Right " + invalidRight + " should not have been successfully granted.", 
                  um.grantRightToRole(TEST_ROLE_NAME, invalidRight));
    }
  }
  
  
  public void testRegExpScopedRightAssignement() throws PersistenceLayerException, XFMG_DomainDoesNotExistException, XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_NameContainsInvalidCharacter {
    String TEST_ROLE_NAME = "TestRole";
    assertTrue("Role " + TEST_ROLE_NAME + " should have been successfully created.",
               um.createRole(TEST_ROLE_NAME));
    assertTrue("TEST_RIGHT_SCOPE should have been succesfully created.",
               um.createRightScope(TEST_RIGHT_SCOPE, "TEST_RIGHT_SCOPE", "EN")); //"xfmg.xopctrl.RightScopeTest:[Stadt, Land, Fluss, Baum]::*:/\\w+([.]\\w+)*/";
    String TEST_SCOPED_RIGHT_1 = "xfmg.xopctrl.RightScopeTest:Fluss:bg.test:bg.*:bg.test";
    String TEST_SCOPED_RIGHT_2 = "xfmg.xopctrl.RightScopeTest:Fluss:fluss.fluss:fluss:Fluss";
    String TEST_SCOPED_RIGHT_3 = "xfmg.xopctrl.RightScopeTest:Stadt:fluss.fluss:fluss.fluss:Stadt.Land.Fluss.Baum";
    String TEST_SCOPED_RIGHT_4 = "xfmg.xopctrl.RightScopeTest:Land:a.a:*:a";
    String TEST_SCOPED_RIGHT_5 = "xfmg.xopctrl.RightScopeTest:Baum:a.a:*:a.B.c.D.e";
    String[] validRights = {TEST_SCOPED_RIGHT_1, TEST_SCOPED_RIGHT_2, TEST_SCOPED_RIGHT_3, TEST_SCOPED_RIGHT_4, TEST_SCOPED_RIGHT_5};
    for (String validRight : validRights) {
      assertTrue("Scoped Right " + validRight + " should have been successfully granted.", 
                 um.grantRightToRole(TEST_ROLE_NAME, validRight));
    }
    
    String TEST_INVALID_SCOPED_RIGHT_1 = "xfmg.xopctrl.RightScopeTest:Haus:a:a:a";
    String TEST_INVALID_SCOPED_RIGHT_2 = "xfmg.xopctrl.RightScopeTest:*:a:a";
    String TEST_INVALID_SCOPED_RIGHT_3 = "xfmg.xopctrl.RightScopeTest:Land:*:a.a:a";
    String TEST_INVALID_SCOPED_RIGHT_4 = "xfmg.xopctrl.RightScopeTest:Land:a.a:c0mpl$x:a";
    String TEST_INVALID_SCOPED_RIGHT_5 = "xfmg.xopctrl.RightScopeTest:Land:a.a:a.a:c0mpl$x";
    String TEST_INVALID_SCOPED_RIGHT_6 = "xfmg.xopctrl.RightScopeTest:Land:a.a:a.a:.a";
    String TEST_INVALID_SCOPED_RIGHT_7 = "xfmg.xopctrl.RightScopeTest:Land:a.a:a.a:a.*";
    String[] invalidRights = {TEST_INVALID_SCOPED_RIGHT_1, TEST_INVALID_SCOPED_RIGHT_2, TEST_INVALID_SCOPED_RIGHT_3,
                              TEST_INVALID_SCOPED_RIGHT_4, TEST_INVALID_SCOPED_RIGHT_5, TEST_INVALID_SCOPED_RIGHT_6,
                              TEST_INVALID_SCOPED_RIGHT_7};
    for (String invalidRight : invalidRights) {
      assertFalse("Scoped Right " + invalidRight + " should not have been successfully granted.", 
                  um.grantRightToRole(TEST_ROLE_NAME, invalidRight));
    }
  }
  
  
  public void testScopedRightCoverage() throws XFMG_DomainDoesNotExistException, PersistenceLayerException, XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_NameContainsInvalidCharacter {
    String TEST_ROLE_NAME = "TestRole";
    assertTrue("Role " + TEST_ROLE_NAME + " should have been successfully created.",
               um.createRole(TEST_ROLE_NAME));
    assertTrue("PERSISTENCE_RIGHT_SCOPE should have been succesfully created.",
               um.createRightScope(PERSISTENCE_RIGHT_SCOPE, "XMOM-Persistence RightScope", "EN"));
    String RIGHT_1 = "xnwh.persistence.Storables2:read:bg.*:*";
    String RIGHT_2 = "xnwh.persistence.Storables2:delete:bg.test.*:*";
    String RIGHT_3 = "xnwh.persistence.Storables2:delete:bg.Deleteable:*";
    String RIGHT_4 = "xnwh.persistence.Storables2:*:bg.*:name";
    String RIGHT_5 = "xnwh.persistence.Storables2:*:bg.*:data.content.*";
    String[] rights = {RIGHT_1, RIGHT_2, RIGHT_3, RIGHT_4, RIGHT_5};
    for (String right : rights) {
      assertTrue("Scoped Right " + right + " should have been successfully granted.", 
                 um.grantRightToRole(TEST_ROLE_NAME, right));
    }
    
    String RIGHT_CHECK_1 = "xnwh.persistence.Storables2:read:bg.test.Device:name";
    String RIGHT_CHECK_2 = "xnwh.persistence.Storables2:read:bg.test.Device:data.content.documentation";
    String RIGHT_CHECK_3 = "xnwh.persistence.Storables2:delete:bg.test.Device:id";
    String RIGHT_CHECK_4 = "xnwh.persistence.Storables2:read:bg.a.b.c.D:a.b.c";
    String RIGHT_CHECK_5 = "xnwh.persistence.Storables2:delete:bg.Deleteable:name";
    String RIGHT_CHECK_6 = "xnwh.persistence.Storables2:delete:bg.Deleteable:a.b.c";
    String RIGHT_CHECK_7 = "xnwh.persistence.Storables2:write:bg.Deleteable:name";
    String RIGHT_CHECK_8 = "xnwh.persistence.Storables2:update:bg.a.b.c.D:name";
    String RIGHT_CHECK_9 = "xnwh.persistence.Storables2:read:bg.Deleteable:data.content.documentation";
    String RIGHT_CHECK_10 = "xnwh.persistence.Storables2:delete:bg.a.b.c.D:data.content.a.b.c";
    String[] checks = {RIGHT_CHECK_1, RIGHT_CHECK_2, RIGHT_CHECK_3, RIGHT_CHECK_4, RIGHT_CHECK_5,
                    RIGHT_CHECK_6, RIGHT_CHECK_7, RIGHT_CHECK_8, RIGHT_CHECK_9, RIGHT_CHECK_10};
    for (String check : checks) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + check,
                 um.hasRight(check, TEST_ROLE_NAME));
    }
    
    String INVALID_RIGHT_CHECK_1 = "xnwh.persistence.Storables2:read:xact.test.Device:mac";
    String INVALID_RIGHT_CHECK_2 = "xnwh.persistence.Storables2:read:xact.Device:mac";
    String INVALID_RIGHT_CHECK_3 = "xnwh.persistence.Storables2:delete:bg.teest.Device:mac";
    String INVALID_RIGHT_CHECK_4 = "xnwh.persistence.Storables2:delete:bg.teest.*:*";;
    String INVALID_RIGHT_CHECK_5 = "xnwh.persistence.Storables2:delete:bg.Deleteable.onAndOn:mac";
    String INVALID_RIGHT_CHECK_6 = "xnwh.persistence.Storables2:update:bg.test.Deleteable:mac";
    String INVALID_RIGHT_CHECK_7 = "xnwh.persistence.Storables2:write:bg.xact.Device:namee";
    String INVALID_RIGHT_CHECK_8 = "xnwh.persistence.Storables2:update:bg.a.b.c.D:mac.suffix";
    String INVALID_RIGHT_CHECK_9 = "xnwh.persistence.Storables2:write:bg.Deleteable:data.contentt.documentation";
    String INVALID_RIGHT_CHECK_10 = "xnwh.persistence.Storables2:delete:bg.a.b.c.D:data.a.b.c";
    String[] invalid_checks = {INVALID_RIGHT_CHECK_1, INVALID_RIGHT_CHECK_2, INVALID_RIGHT_CHECK_3, INVALID_RIGHT_CHECK_4, INVALID_RIGHT_CHECK_5,
                    INVALID_RIGHT_CHECK_6, INVALID_RIGHT_CHECK_7, INVALID_RIGHT_CHECK_8, INVALID_RIGHT_CHECK_9, INVALID_RIGHT_CHECK_10};
    for (String check : invalid_checks) {
      assertFalse("Role " + TEST_ROLE_NAME + " should not have been in possion of a right " + check,
                 um.hasRight(check, TEST_ROLE_NAME));
    }
  }
  
  
  public void testRegExpScopedRightCoverage() throws XFMG_DomainDoesNotExistException, PersistenceLayerException, XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_NameContainsInvalidCharacter {
    String TEST_ROLE_NAME = "TestRole";
    assertTrue("Role " + TEST_ROLE_NAME + " should have been successfully created.",
               um.createRole(TEST_ROLE_NAME));
    final String COMPLEX_REGEXP_RIGHT_SCOPE = "xfmg.xopctrl.RightScopeTest:/[abc][def][zZ]+/:/((?<!a)[bc])+/:/(abc|def|\\*)/:/[abc]+(\\.[abc*]+)*/";
    assertTrue("COMPLEX_REGEXP_RIGHT_SCOPE should have been succesfully created.",
               um.createRightScope(COMPLEX_REGEXP_RIGHT_SCOPE, "COMPLEX_REGEXP_RIGHT_SCOPE", "EN"));
    String RIGHT_1 = "xfmg.xopctrl.RightScopeTest:adz:b:*:a";
    String RIGHT_2 = "xfmg.xopctrl.RightScopeTest:bezzzZzZ:bbccbc:abc:abc.abc.*";
    String RIGHT_3 = "xfmg.xopctrl.RightScopeTest:cfzzzZzZ:c:def:a.*";
    String RIGHT_4 = "xfmg.xopctrl.RightScopeTest:afzzZ:bbc:abc:c.*.a.*";
    String RIGHT_5 = "xfmg.xopctrl.RightScopeTest:bfzZ:bc:def:a.*.abc";
    String[] rights = {RIGHT_1, RIGHT_2, RIGHT_3, RIGHT_4, RIGHT_5};
    for (String right : rights) {
      assertTrue("Scoped Right " + right + " should have been successfully granted.", 
                 um.grantRightToRole(TEST_ROLE_NAME, right));
    }
    
    String RIGHT_CHECK_1 = "xfmg.xopctrl.RightScopeTest:adz:b:abc:a";
    String RIGHT_CHECK_2 = "xfmg.xopctrl.RightScopeTest:adz:b:xact.Jump:a";
    String RIGHT_CHECK_3 = "xfmg.xopctrl.RightScopeTest:bezzzZzZ:bbccbc:abc:abc.abc.abc";
    String RIGHT_CHECK_4 = "xfmg.xopctrl.RightScopeTest:bezzzZzZ:bbccbc:abc:abc.abc.Baum";
    String RIGHT_CHECK_5 = "xfmg.xopctrl.RightScopeTest:cfzzzZzZ:c:def:a.b";
    String RIGHT_CHECK_6 = "xfmg.xopctrl.RightScopeTest:cfzzzZzZ:c:def:a.c.e.g.Alpha";
    String RIGHT_CHECK_7 = "xfmg.xopctrl.RightScopeTest:afzzZ:bbc:abc:c.d.a.b.c.d";
    String RIGHT_CHECK_8 = "xfmg.xopctrl.RightScopeTest:afzzZ:bbc:abc:c.b.a.whatever.a.b";
    String RIGHT_CHECK_9 = "xfmg.xopctrl.RightScopeTest:bfzZ:bc:def:a.ab.abc";
    String RIGHT_CHECK_10 = "xfmg.xopctrl.RightScopeTest:bfzZ:bc:def:a.c.b.a.bc.ab.abc";
    String[] checks = {RIGHT_CHECK_1, RIGHT_CHECK_2, RIGHT_CHECK_3, RIGHT_CHECK_4, RIGHT_CHECK_5,
                    RIGHT_CHECK_6, RIGHT_CHECK_7, RIGHT_CHECK_8, RIGHT_CHECK_9, RIGHT_CHECK_10};
    for (String check : checks) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + check,
                 um.hasRight(check, TEST_ROLE_NAME));
    }
    
    String INVALID_RIGHT_CHECK_1 = "xfmg.xopctrl.RightScopeTest:ad:b:a:a";
    String INVALID_RIGHT_CHECK_2 = "xfmg.xopctrl.RightScopeTest:adz:b:a:ab";
    String INVALID_RIGHT_CHECK_3 = "xfmg.xopctrl.RightScopeTest:bezzzZzZ:bbccbc:abc:abc";
    String INVALID_RIGHT_CHECK_4 = "xfmg.xopctrl.RightScopeTest:bezzzZzZ:bbccbc:def:abc.abc.abc";;
    String INVALID_RIGHT_CHECK_5 = "xfmg.xopctrl.RightScopeTest:cfzzzZzZ:a:def:a.a";
    String INVALID_RIGHT_CHECK_6 = "xfmg.xopctrl.RightScopeTest:cfzzzZzZ:c:def:a";
    String INVALID_RIGHT_CHECK_7 = "xfmg.xopctrl.RightScopeTest:afzzZ:bbc:abc:c.a.a";
    String INVALID_RIGHT_CHECK_8 = "xfmg.xopctrl.RightScopeTest:afzzZ:bbc:abc:c.b.a";
    String INVALID_RIGHT_CHECK_9 = "xfmg.xopctrl.RightScopeTest:bfzZZ:bc:def:a.a.abc";
    String INVALID_RIGHT_CHECK_10 = "xfmg.xopctrl.RightScopeTest:bfzZ:bc:def:a.abc";
    String[] invalid_checks = {INVALID_RIGHT_CHECK_1, INVALID_RIGHT_CHECK_2, INVALID_RIGHT_CHECK_3, INVALID_RIGHT_CHECK_4, INVALID_RIGHT_CHECK_5,
                    INVALID_RIGHT_CHECK_6, INVALID_RIGHT_CHECK_7, INVALID_RIGHT_CHECK_8, INVALID_RIGHT_CHECK_9, INVALID_RIGHT_CHECK_10};
    for (String check : invalid_checks) {
      assertFalse("Role " + TEST_ROLE_NAME + " should not have been in possion of a right " + check,
                 um.hasRight(check, TEST_ROLE_NAME));
    }
    
  }
  
  
  public void testScopedRightManagament() throws XFMG_DomainDoesNotExistException, PersistenceLayerException, XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_NameContainsInvalidCharacter {
    String TEST_ROLE_NAME = "TestRole";
    assertTrue("Role " + TEST_ROLE_NAME + " should have been successfully created.",
               um.createRole(TEST_ROLE_NAME));
    assertTrue("TEST_RIGHT_SCOPE should have been succesfully created.",
               um.createRightScope(TEST_RIGHT_SCOPE, "TEST_RIGHT_SCOPE", "EN")); //"xfmg.xopctrl.RightScopeTest:[Stadt, Land, Fluss, Baum]::*:/\\w+([.]\\w+)*/";
    String TEST_SCOPED_RIGHT_1 = "xfmg.xopctrl.RightScopeTest:Fluss:bg.test:bg.*:bg.test";
    String TEST_SCOPED_RIGHT_2 = "xfmg.xopctrl.RightScopeTest:Fluss:fluss.fluss:fluss:Fluss";
    String[] validRights = {TEST_SCOPED_RIGHT_1, TEST_SCOPED_RIGHT_2};
    for (String validRight : validRights) {
      assertTrue("Scoped Right " + validRight + " should have been successfully granted.", 
                 um.grantRightToRole(TEST_ROLE_NAME, validRight));
    }
    
    String RIGHT_CHECK_1 = "xfmg.xopctrl.RightScopeTest:Fluss:bg.test:bg.Baum:bg.test";
    String RIGHT_CHECK_2 = "xfmg.xopctrl.RightScopeTest:Fluss:fluss.fluss:fluss:Fluss";
    String[] checks = {RIGHT_CHECK_1, RIGHT_CHECK_2};
    for (String check : checks) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + check,
                 um.hasRight(check, TEST_ROLE_NAME));
    }
    
    assertTrue("The right " + TEST_SCOPED_RIGHT_2 + " should have been succesfully revoked from " + TEST_ROLE_NAME,
               um.revokeRightFromRole(TEST_ROLE_NAME, TEST_SCOPED_RIGHT_2));
    
    assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + RIGHT_CHECK_1,
               um.hasRight(RIGHT_CHECK_1, TEST_ROLE_NAME));
    assertFalse("Role " + TEST_ROLE_NAME + " should not have been in possion of a right " + RIGHT_CHECK_2,
                um.hasRight(RIGHT_CHECK_2, TEST_ROLE_NAME));
    
    assertTrue("Scoped Right " + TEST_SCOPED_RIGHT_2 + " should have been successfully granted.", 
               um.grantRightToRole(TEST_ROLE_NAME, TEST_SCOPED_RIGHT_2));
    
    for (String check : checks) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + check,
                 um.hasRight(check, TEST_ROLE_NAME));
    }
    
  }
  
  
  public void testWildRightCoverage() throws XFMG_DomainDoesNotExistException, PersistenceLayerException, XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_NameContainsInvalidCharacter {
    String TEST_ROLE_NAME = "TestRole";
    assertTrue("Role " + TEST_ROLE_NAME + " should have been successfully created.",
               um.createRole(TEST_ROLE_NAME));
    assertTrue("PERSISTENCE_RIGHT_SCOPE should have been succesfully created.",
               um.createRightScope(PERSISTENCE_RIGHT_SCOPE, "XMOM-Persistence RightScope", "EN"));
    String RIGHT_1 = "xnwh.persistence.Storables2:read:*:*";
    String RIGHT_2 = "xnwh.persistence.Storables2:delete:bg.Deleteable:*";
    String RIGHT_3 = "xnwh.persistence.Storables2:insert:bg.*:data.*";
    String RIGHT_4 = "xnwh.persistence.Storables2:write:bg.*:*";
    String[] rights = {RIGHT_1, RIGHT_2, RIGHT_3, RIGHT_4};
    for (String right : rights) {
      assertTrue("Scoped Right " + right + " should have been successfully granted.", 
                 um.grantRightToRole(TEST_ROLE_NAME, right));
    }
    
    
    String RIGHT_CHECK_1 = "xnwh.persistence.Storables2:read:*:*";
    String RIGHT_CHECK_2 = "xnwh.persistence.Storables2:read:bg.test.Device:address.*";
    String RIGHT_CHECK_3 = "xnwh.persistence.Storables2:delete:bg.Deleteable:*";
    String RIGHT_CHECK_4 = "xnwh.persistence.Storables2:delete:bg.Deleteable:mac.*";
    String RIGHT_CHECK_5 = "xnwh.persistence.Storables2:insert:bg.devices.*:data.name";
    String RIGHT_CHECK_6 = "xnwh.persistence.Storables2:insert:bg.devices.Device:data.content.documentations.*";
    String RIGHT_CHECK_7 = "xnwh.persistence.Storables2:write:bg.devices.Device:data.name";
    String RIGHT_CHECK_8 = "xnwh.persistence.Storables2:write:bg.devices.*:data.*";
    String[] checks = {RIGHT_CHECK_1, RIGHT_CHECK_2, RIGHT_CHECK_3, RIGHT_CHECK_4,
                    RIGHT_CHECK_5, RIGHT_CHECK_6, RIGHT_CHECK_7, RIGHT_CHECK_8};
    for (String check : checks) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + check,
                 um.hasRight(check, TEST_ROLE_NAME));
    }
    
    String INVALID_RIGHT_CHECK_1 = "xnwh.persistence.Storables2:delete:bg.*:*";
    String INVALID_RIGHT_CHECK_2 = "xnwh.persistence.Storables2:delete:bg.Deleteable.Sub:*";
    String INVALID_RIGHT_CHECK_3 = "xnwh.persistence.Storables2:insert:xact.Device:data.*";
    String INVALID_RIGHT_CHECK_4 = "xnwh.persistence.Storables2:insert:*:data.*";
    String INVALID_RIGHT_CHECK_5 = "xnwh.persistence.Storables2:insert:bg.*:*";
    String INVALID_RIGHT_CHECK_6 = "xnwh.persistence.Storables2:write:*:*";
    String[] invalid_checks = {INVALID_RIGHT_CHECK_1, INVALID_RIGHT_CHECK_2, INVALID_RIGHT_CHECK_3,
                    INVALID_RIGHT_CHECK_4, INVALID_RIGHT_CHECK_5, INVALID_RIGHT_CHECK_6};
    for (String check : invalid_checks) {
      assertFalse("Role " + TEST_ROLE_NAME + " should not have been in possion of a right " + check,
                 um.hasRight(check, TEST_ROLE_NAME));
    }
    
  }
  
  
  public void testEscapingInScopesAndRights() throws XFMG_DomainDoesNotExistException, PersistenceLayerException, XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_NameContainsInvalidCharacter {
    String TEST_ROLE_NAME = "TestRole";
    assertTrue("Role " + TEST_ROLE_NAME + " should have been successfully created.",
               um.createRole(TEST_ROLE_NAME));
    final String COMPLEX_REGEXP_RIGHT_SCOPE = "xfmg.xopctrl.RightScopeTest:/(([aA]bc)|(o==//======>)|(([abc](\\\\:)?)+\\*?))/";
    assertTrue("COMPLEX_REGEXP_RIGHT_SCOPE should have been succesfully created.",
               um.createRightScope(COMPLEX_REGEXP_RIGHT_SCOPE, "COMPLEX_REGEXP_RIGHT_SCOPE", "EN"));
    
    String RIGHT_1 = "xfmg.xopctrl.RightScopeTest:abc";
    String RIGHT_2 = "xfmg.xopctrl.RightScopeTest:Abc";
    String RIGHT_3 = "xfmg.xopctrl.RightScopeTest:o==//======>";
    String RIGHT_4 = "xfmg.xopctrl.RightScopeTest:a";
    String RIGHT_5 = "xfmg.xopctrl.RightScopeTest:a\\:b\\:c";
    String RIGHT_6 = "xfmg.xopctrl.RightScopeTest:a\\:a\\:a\\:*";
    String[] rights = {RIGHT_1, RIGHT_2, RIGHT_3, RIGHT_4, RIGHT_5, RIGHT_6};
    for (String right : rights) {
      assertTrue("Scoped Right " + right + " should have been successfully granted.", 
                 um.grantRightToRole(TEST_ROLE_NAME, right));
    }
    
    String RIGHT_CHECK_1 = "xfmg.xopctrl.RightScopeTest:a\\:a\\:a\\:*";
    String RIGHT_CHECK_2 = "xfmg.xopctrl.RightScopeTest:a\\:a\\:a\\:a\\:a";
    String RIGHT_CHECK_3 = "xfmg.xopctrl.RightScopeTest:a\\:a\\:a\\:Test";
    String[] checks = {RIGHT_CHECK_1, RIGHT_CHECK_2, RIGHT_CHECK_3};
    for (String check : checks) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + check,
                 um.hasRight(check, TEST_ROLE_NAME));
    }
    
    String INVALID_RIGHT_CHECK_1 = "xfmg.xopctrl.RightScopeTest:a\\:a\\:a*";
    String INVALID_RIGHT_CHECK_2 = "xfmg.xopctrl.RightScopeTest:a\\:b\\:c\\:*";
    String[] invalid_checks = {INVALID_RIGHT_CHECK_1, INVALID_RIGHT_CHECK_2};
    for (String check : invalid_checks) {
      assertFalse("Role " + TEST_ROLE_NAME + " should not have been in possion of a right " + check,
                 um.hasRight(check, TEST_ROLE_NAME));
    }
  }

  public void testStartOrderRight() throws XFMG_DomainDoesNotExistException, PersistenceLayerException, XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_NameContainsInvalidCharacter {
    String TEST_ROLE_NAME = "TestRole";
    assertTrue("Role " + TEST_ROLE_NAME + " should have been successfully created.",
               um.createRole(TEST_ROLE_NAME));
    assertTrue("STARTORDER_RIGHT_SCOPE should have been succesfully created.",
               um.createRightScope(STARTORDER_RIGHT_SCOPE, "COMPLEX_REGEXP_RIGHT_SCOPE", "EN"));
    
    String key = "xprc.xpce.startorder.Test";
    
    String RIGHT_1 = key + ":xfmg.xfctrl.appmgmt.ListApplications::";
    String RIGHT_2 = key + ":*.appmgmt.*:GlobalApplicationMgmt:1.*";
    String RIGHT_3 = key + ":*._123\\*.*:!Recht\\: * ^Sonderzeichen\"?:version\\E 1.*";
    String[] rights = {RIGHT_1, RIGHT_2, RIGHT_3};
    for (String right : rights) {
      assertTrue("Scoped Right " + right + " should have been successfully granted.", 
                 um.grantRightToRole(TEST_ROLE_NAME, right));
    }
    
    String RIGHT_CHECK_1 = key + ":xfmg.xfctrl.appmgmt.ListApplications::";
    String RIGHT_CHECK_2 = key + ":xfmg.xfctrl.appmgmt.StartApplications:GlobalApplicationMgmt:1.0";
    String RIGHT_CHECK_3 = key + ":xfmg._123\\*.StartApplications:!Recht\\: mit ^Sonderzeichen\"?:version\\E 1.0";
    String[] checks = {RIGHT_CHECK_1, RIGHT_CHECK_2, RIGHT_CHECK_3};
    for (String check : checks) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + check,
                 um.hasRight(check, TEST_ROLE_NAME));
    }
    
    String INVALID_RIGHT_CHECK_1 = key + ":xfmg.xfctrl.appmgmt.StartApplications::";
    String INVALID_RIGHT_CHECK_2 = key + ":xfmg.xfctrl.appmgmt.ListApplications:GlobalApplicationMgmt:2.0";
    String INVALID_RIGHT_CHECK_3 = key + ":xfmg._123.StartApplications:!Recht\\: mit ^Sonderzeichen\"?:version\\E 1.0";
    String[] invalid_checks = {INVALID_RIGHT_CHECK_1, INVALID_RIGHT_CHECK_2, INVALID_RIGHT_CHECK_3};
    for (String check : invalid_checks) {
      assertFalse("Role " + TEST_ROLE_NAME + " should not have been in possion of a right " + check,
                  um.hasRight(check, TEST_ROLE_NAME));
    }
  }
  
  
  public void testScopedRightLifecycle() throws XFMG_DomainDoesNotExistException, PersistenceLayerException, XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_PredefinedXynaObjectException, XFMG_NameContainsInvalidCharacter {
    String TEST_ROLE_NAME = "TestRole";
    assertTrue("Role " + TEST_ROLE_NAME + " should have been successfully created.",
               um.createRole(TEST_ROLE_NAME));
    assertTrue("TEST_RIGHT_SCOPE should have been succesfully created.",
               um.createRightScope(TEST_RIGHT_SCOPE, "TEST_RIGHT_SCOPE", "EN")); //"xfmg.xopctrl.RightScopeTest:[Stadt, Land, Fluss, Baum]::*:/\\w+([.]\\w+)*/";
    String TEST_SCOPED_RIGHT_1 = "xfmg.xopctrl.RightScopeTest:Fluss:bg.test:bg.*:bg.test";
    String TEST_SCOPED_RIGHT_2 = "xfmg.xopctrl.RightScopeTest:Fluss:fluss.fluss:fluss:Fluss";
    String[] validRights = {TEST_SCOPED_RIGHT_1, TEST_SCOPED_RIGHT_2};
    for (String validRight : validRights) {
      assertTrue("Scoped Right " + validRight + " should have been successfully granted.", 
                 um.grantRightToRole(TEST_ROLE_NAME, validRight));
    }
    
    String RIGHT_CHECK_1 = "xfmg.xopctrl.RightScopeTest:Fluss:bg.test:bg.Baum:bg.test";
    String RIGHT_CHECK_2 = "xfmg.xopctrl.RightScopeTest:Fluss:fluss.fluss:fluss:Fluss";
    String[] checks = {RIGHT_CHECK_1, RIGHT_CHECK_2};
    for (String check : checks) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + check,
                 um.hasRight(check, TEST_ROLE_NAME));
    }
    
    um.deleteRightScope(TEST_RIGHT_SCOPE);
    
    for (String check : checks) {
      assertFalse("Role " + TEST_ROLE_NAME + " should not have been in possion of a right " + check,
                  um.hasRight(check, TEST_ROLE_NAME));
    }
    
    Role role = um.getRole(TEST_ROLE_NAME);
    assertEquals("We'd expected 0 ScopedRights after their deletion.", 0, role.getScopedRights().size());
    
    try {
      um.deleteRightScope(XMOMPersistenceManagement.PERSISTENCE_RIGHT_SCOPE_KEY);
      fail("Deletion of PERSISTENCE_RIGHT_SCOPE should not have been possible.");
    } catch (XFMG_PredefinedXynaObjectException e) {
      ; //ntbd
    }
  }
  
  
  public void testChecking() throws XFMG_DomainDoesNotExistException, PersistenceLayerException, XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_NameContainsInvalidCharacter {
    String TEST_ROLE_NAME = "TestRole";
    assertTrue("Role " + TEST_ROLE_NAME + " should have been successfully created.",
               um.createRole(TEST_ROLE_NAME));
    assertTrue("PERSISTENCE_RIGHT_SCOPE should have been succesfully created.",
               um.createRightScope(PERSISTENCE_RIGHT_SCOPE, "XMOM-Persistence RightScope", "EN"));
    
    String TEST_SCOPED_RIGHT_1 = "xnwh.persistence.Storables2:*:*:*";
    assertTrue("Scoped Right " + TEST_SCOPED_RIGHT_1 + " should have been successfully granted.", 
               um.grantRightToRole(TEST_ROLE_NAME, TEST_SCOPED_RIGHT_1));
    
    String RIGHT_CHECK_1 = "xnwh.persistence.Storables2:*:*:*";
    long tries = 50000;
    
    Long start = System.currentTimeMillis();
    for (int i = 0; i < tries; i++) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + RIGHT_CHECK_1,
                  um.hasRight(RIGHT_CHECK_1, TEST_ROLE_NAME));
    }
    long duration1 = System.currentTimeMillis() - start;
    
    String RIGHT_CHECK_2 = "xnwh.persistence.Storables2:read:bg.Device:name";
    start = System.currentTimeMillis();
    for (int i = 0; i < tries; i++) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + RIGHT_CHECK_2,
                  um.hasRight(RIGHT_CHECK_2, TEST_ROLE_NAME));
    }
    long duration2 = System.currentTimeMillis() - start;
    
    assertTrue("The right " + TEST_SCOPED_RIGHT_1 + " should have been succesfully revoked from " + TEST_ROLE_NAME,
               um.revokeRightFromRole(TEST_ROLE_NAME, TEST_SCOPED_RIGHT_1));
    
    String TEST_SCOPED_RIGHT_2 = "xnwh.persistence.Storables2:read:bg.Device:name";
    assertTrue("Scoped Right " + TEST_SCOPED_RIGHT_2 + " should have been successfully granted.", 
               um.grantRightToRole(TEST_ROLE_NAME, TEST_SCOPED_RIGHT_2));
    
    String RIGHT_CHECK_3 = "xnwh.persistence.Storables2:read:bg.Device:name";
    start = System.currentTimeMillis();
    for (int i = 0; i < tries; i++) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + RIGHT_CHECK_3,
                  um.hasRight(RIGHT_CHECK_3, TEST_ROLE_NAME));
    }
    long duration3 = System.currentTimeMillis() - start;
    
    assertTrue("The right " + TEST_SCOPED_RIGHT_2 + " should have been succesfully revoked from " + TEST_ROLE_NAME,
               um.revokeRightFromRole(TEST_ROLE_NAME, TEST_SCOPED_RIGHT_2));
    
    String TEST_SCOPED_RIGHT_3 = "xnwh.persistence.Storables2:*:bg.test.*:data.content.*";
    assertTrue("Scoped Right " + TEST_SCOPED_RIGHT_3 + " should have been successfully granted.", 
               um.grantRightToRole(TEST_ROLE_NAME, TEST_SCOPED_RIGHT_3));
    
    String RIGHT_CHECK_4 = "xnwh.persistence.Storables2:*:bg.test.*:data.content.*";
    start = System.currentTimeMillis();
    for (int i = 0; i < tries; i++) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + RIGHT_CHECK_4,
                  um.hasRight(RIGHT_CHECK_4, TEST_ROLE_NAME));
    }
    long duration4 = System.currentTimeMillis() - start;
    
    String RIGHT_CHECK_5 = "xnwh.persistence.Storables2:update:bg.test.Device:data.content.documentation";
    start = System.currentTimeMillis();
    for (int i = 0; i < tries; i++) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + RIGHT_CHECK_5,
                  um.hasRight(RIGHT_CHECK_5, TEST_ROLE_NAME));
    }
    long duration5 = System.currentTimeMillis() - start;
    
    assertTrue("Scoped Right " + TEST_SCOPED_RIGHT_1 + " should have been successfully granted.", 
               um.grantRightToRole(TEST_ROLE_NAME, TEST_SCOPED_RIGHT_1));
    assertTrue("Scoped Right " + TEST_SCOPED_RIGHT_2 + " should have been successfully granted.", 
               um.grantRightToRole(TEST_ROLE_NAME, TEST_SCOPED_RIGHT_2));
    
    start = System.currentTimeMillis();
    for (int i = 0; i < tries; i++) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + RIGHT_CHECK_1,
                  um.hasRight(RIGHT_CHECK_1, TEST_ROLE_NAME));
    }
    long duration6 = System.currentTimeMillis() - start;
    start = System.currentTimeMillis();
    for (int i = 0; i < tries; i++) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + RIGHT_CHECK_2,
                  um.hasRight(RIGHT_CHECK_2, TEST_ROLE_NAME));
    }
    long duration7 = System.currentTimeMillis() - start;
    start = System.currentTimeMillis();
    for (int i = 0; i < tries; i++) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + RIGHT_CHECK_3,
                  um.hasRight(RIGHT_CHECK_3, TEST_ROLE_NAME));
    }
    long duration8 = System.currentTimeMillis() - start;
    start = System.currentTimeMillis();
    for (int i = 0; i < tries; i++) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + RIGHT_CHECK_4,
                  um.hasRight(RIGHT_CHECK_4, TEST_ROLE_NAME));
    }
    long duration9 = System.currentTimeMillis() - start;
    start = System.currentTimeMillis();
    for (int i = 0; i < tries; i++) {
      assertTrue("Role " + TEST_ROLE_NAME + " should have been in possion of a right " + RIGHT_CHECK_5,
                  um.hasRight(RIGHT_CHECK_5, TEST_ROLE_NAME));
    }
    long duration10 = System.currentTimeMillis() - start;
    
    /*System.out.println("direct wild: " + duration1);
    System.out.println("indirect wild: " + duration2);
    System.out.println("direct simple: " + duration3);
    System.out.println("direct partial wild: " + duration4);
    System.out.println("indirect partial wild: " + duration5);
    System.out.println("with all 3 rights granted");
    System.out.println("direct wild: " + duration6);
    System.out.println("indirect wild: " + duration7);
    System.out.println("direct simple: " + duration8);
    System.out.println("direct partial wild: " + duration9);
    System.out.println("indirect partial wild: " + duration10);*/
    
  }
  
  
  

}
