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
package com.gip.xyna.xfmg.xopctrl;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.XMOM.base.IPv4;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_NameContainsInvalidCharacter;
import com.gip.xyna.xfmg.exceptions.XFMG_NamingConventionException;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RightDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserDoesNotExistException;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xopctrl.radius.PresharedKey;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSDomainSpecificData;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSServer;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSServerPort;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSUserAuthentication;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.LocalUserAuthentication;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserAuthentificationMethod;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.local.XynaLocalMemoryPersistenceLayer;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;


public class UserManagementTest extends TestCase {
  
  UserManagement um;
  static ODS ods;
  
  private final static String USERROLE = "testUserRole";
  private final static String EDITORROLE = "testEditorRole";
  private final static String ADMINROLE = "testAdminRole";
  
  private static UserData USERDATA = new UserData("testUser", USERROLE, "userPass");
  private static UserData EDITORDATA = new UserData("testEditor", EDITORROLE, "editorPass");
  private static UserData ADMINDATA = new UserData("testAdmin", ADMINROLE, "adminPass");
  
  static {
    ods = ODSImpl.getInstance(false);
    ods.registerPersistenceLayer(123L, XynaLocalMemoryPersistenceLayer.class);
    try {
      long id = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test",
                                                        ODSConnectionType.DEFAULT, new String[0]);
      ods.setDefaultPersistenceLayer(ODSConnectionType.HISTORY, id);
      ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
      ods.setDefaultPersistenceLayer(ODSConnectionType.ALTERNATIVE, id);
      ods.setDefaultPersistenceLayer(ODSConnectionType.INTERNALLY_USED, id);
      
      ods.registerStorable(User.class);
      ods.registerStorable(Role.class);
      ods.registerStorable(Right.class);
      ods.registerStorable(Domain.class);
    } catch (Throwable t) {
      t.printStackTrace();
    }
        
  }
  
  
  protected void setUp() throws Exception {
    super.setUp();
    
    XynaProcessingODS xprcODS = EasyMock.createMock(XynaProcessingODS.class);
    EasyMock.expect(xprcODS.getODS()).andReturn(ods).anyTimes();
    
    XynaProcessingBase xproc = EasyMock.createMock(XynaProcessingBase.class);
    EasyMock.expect(xproc.getXynaProcessingODS()).andReturn(xprcODS).anyTimes();
    
    Configuration conf = EasyMock.createMock(Configuration.class);
    EasyMock.expect(conf.getProperty(EasyMock.isA(String.class))).andReturn("XYNA").anyTimes();
    
    XynaFactoryManagementODS xfmods = EasyMock.createMock(XynaFactoryManagementODS.class);
    EasyMock.expect(xfmods.getConfiguration()).andReturn(conf).anyTimes();
    
    XynaOperatorControl xopctrl = EasyMock.createMock(XynaOperatorControl.class);
    //EasyMock.expect(xopctrl.getUserManagement()).andReturn(um).anyTimes();
    
    DependencyRegister depreg = EasyMock.createMock(DependencyRegister.class);
    EasyMock.expect(depreg.addDependency(EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class), EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class))).andReturn(true).anyTimes();
    
    XynaFactoryControl xfacctrl = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfacctrl.getDependencyRegister()).andReturn(depreg).anyTimes();
    
    XynaFactoryManagementBase xfm = EasyMock.createMock(XynaFactoryManagementBase.class);
    EasyMock.expect(xfm.getProperty(EasyMock.eq(XynaProperty.PROPERTYNAME_ALLOWED_ENTRIES))).andReturn("3").anyTimes();
    EasyMock.expect(xfm.getProperty(EasyMock.eq(XynaProperty.PASSWORD_RESTRICTIONS))).andReturn(UserManagement.DEFAULT_PASSWORD_RESTRICTION).anyTimes();

    EasyMock.expect(xfm.getProperty(EasyMock.eq(XynaProperty.GLOBAL_DOMAIN_OVERWRITE))).andReturn(null).anyTimes();
    EasyMock.expect(xfm.getProperty(EasyMock.eq(XynaProperty.DEFAULT_DOMAINS_FOR_NEW_USERS))).andReturn("XYNA").anyTimes();

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
    
    EasyMock.replay(depreg, xfacctrl, xprcODS, xproc, xfmods, fe, xfm, xf);

    um = new UserManagement();
    um.initInternally();
    
    EasyMock.expect(xopctrl.getUserManagement()).andReturn(um).anyTimes();
    EasyMock.replay(xopctrl);
    
    setUpTestRolesAndUsers();
  }


  protected void tearDown() throws Exception {
    super.tearDown();
  }
  

  public static class UserData {
    public String id;
    public String role;
    public String pass;
    
    public UserData(String id, String role, String pass) {
      this.id = id;
      this.role = role;
      this.pass = pass;
    }
  }
    
  
  public void setUpTestRolesAndUsers () throws XynaException {
    
    um.createRole(USERROLE);
    um.createRole(EDITORROLE);
    um.createRole(ADMINROLE);
    
    um.grantRightToRole(USERROLE, Rights.START_ORDER.toString());
    
    um.grantRightToRole(EDITORROLE, Rights.START_ORDER.toString());
    um.grantRightToRole(EDITORROLE, Rights.EDIT_MDM.toString());
    um.grantRightToRole(EDITORROLE, Rights.DEPLOYMENT_MDM.toString());
    
    um.grantRightToRole(ADMINROLE, Rights.START_ORDER.toString());
    um.grantRightToRole(ADMINROLE, Rights.EDIT_MDM.toString());
    um.grantRightToRole(ADMINROLE, Rights.DEPLOYMENT_MDM.toString());
    um.grantRightToRole(ADMINROLE, ScopedRight.CAPACITY.allAccess());
    um.grantRightToRole(ADMINROLE, Rights.MONITORING_LEVEL_MANAGEMENT.toString());
    um.grantRightToRole(ADMINROLE, Rights.PERSISTENCE_MANAGEMENT.toString());
    um.grantRightToRole(ADMINROLE, Rights.TRIGGER_FILTER_MANAGEMENT.toString());
    um.grantRightToRole(ADMINROLE, Rights.USER_MANAGEMENT.toString());
    um.grantRightToRole(ADMINROLE, Rights.PROCESS_MANUAL_INTERACTION.toString());
    
    um.createUser(USERDATA.id, USERDATA.role, USERDATA.pass, false);
    um.createUser(EDITORDATA.id, EDITORDATA.role, EDITORDATA.pass, false);
    um.createUser(ADMINDATA.id, ADMINDATA.role, ADMINDATA.pass, false);
  }
  
  
  public void testFunctionRightsMapping() throws XynaException {
    
    User testUser = um.authenticate(USERDATA.id, USERDATA.pass);
    assertNotNull(testUser);
    User testEditor = um.authenticate(EDITORDATA.id, EDITORDATA.pass);
    assertNotNull(testEditor);
    User testAdmin = um.authenticate(ADMINDATA.id, ADMINDATA.pass);
    assertNotNull(testAdmin);
    
    assertEquals(USERDATA.id, testUser.getName());
    assertEquals(USERDATA.role, testUser.getRole());
    assertEquals(EDITORDATA.id, testEditor.getName());
    assertEquals(EDITORDATA.role, testEditor.getRole());
    assertEquals(ADMINDATA.id, testAdmin.getName());
    assertEquals(ADMINDATA.role, testAdmin.getRole());
    
    // RIGHTS.START_ORDER 
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("startOrder"), testUser.getRole()));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("startOrder"), testEditor.getRole()));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("startOrder"), testAdmin.getRole()));
    
    //RIGHTS.EDIT_MDM
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("saveMDM"), testUser.getRole()));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("saveMDM"), testEditor.getRole()));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("saveMDM"), testAdmin.getRole()));
    
    //RIGHTS.DEPLOYMENT_MDM
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("deployMDM"), testUser.getRole()));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("deployMDM"), testEditor.getRole()));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("deployMDM"), testAdmin.getRole()));
    
    //RIGHTS.TRIGGER_FILTER_MANAGEMENT
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("addTrigger"), testUser.getRole()));
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("addTrigger"), testEditor.getRole()));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("addTrigger"), testAdmin.getRole()));
    
    //RIGHTS.MONITORING_LEVEL_MANAGEMENT
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("setDefaultMonitoringLevel"), testUser.getRole()));
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("setDefaultMonitoringLevel"), testEditor.getRole()));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("setDefaultMonitoringLevel"), testAdmin.getRole()));
    
    //RIGHTS.CAPACITY_MANAGEMENT
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("addCapacity"), testUser.getRole()));
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("addCapacity"), testEditor.getRole()));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("addCapacity"), testAdmin.getRole()));
    
    //RIGHTS.PROCESS_MANUAL_INTERACTION
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("processManualInteractionEntry"), testUser.getRole()));
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("processManualInteractionEntry"), testEditor.getRole()));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("processManualInteractionEntry"), testAdmin.getRole()));
    
    //RIGHTS.USER_MANAGEMENT
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("createUser"), testUser.getRole()));
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("createUser"), testEditor.getRole()));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("createUser"), testAdmin.getRole()));
  }
  
  
  public void testRoleManipulation() throws XynaException {
    final String NEWROLENAME = "testRoleManipulation";
    final String RIGHTNAME1 = ScopedRight.CAPACITY.allAccess();
    final String RIGHTNAME2 = Rights.PERSISTENCE_MANAGEMENT.toString();
    final String RIGHTNAME3 = Rights.MONITORING_LEVEL_MANAGEMENT.toString();
    
    assertEquals(true, um.createRole(NEWROLENAME));
    
    assertEquals(true, um.grantRightToRole(NEWROLENAME, RIGHTNAME1));
    assertEquals(true, um.grantRightToRole(NEWROLENAME, RIGHTNAME2));
    assertEquals(true, um.grantRightToRole(NEWROLENAME, RIGHTNAME3));

    //Role is already present, we expect a fail 
    assertEquals(false, um.createRole(NEWROLENAME));
    
    //Right is already present, we expect a fail 
    assertEquals(false, um.grantRightToRole(NEWROLENAME, RIGHTNAME1));
    assertEquals(false, um.grantRightToRole(NEWROLENAME, RIGHTNAME2));
    assertEquals(false, um.grantRightToRole(NEWROLENAME, RIGHTNAME3));

    assertEquals(true, um.revokeRightFromRole(NEWROLENAME, RIGHTNAME1));
    assertEquals(true, um.revokeRightFromRole(NEWROLENAME, RIGHTNAME2));
    assertEquals(true, um.revokeRightFromRole(NEWROLENAME, RIGHTNAME3));
    
    // Right is no longer present, we expect to fail
    assertEquals(false, um.revokeRightFromRole(NEWROLENAME, RIGHTNAME1));
    assertEquals(false, um.revokeRightFromRole(NEWROLENAME, RIGHTNAME2));
    assertEquals(false, um.revokeRightFromRole(NEWROLENAME, RIGHTNAME3));
    
    assertEquals(true, um.deleteRole(NEWROLENAME));
  }
  
  
  public void testUserManipulation() throws XynaException {
    final UserData NEWUSERDATA = new UserData("testUserManipulation", ADMINROLE, "testUserManipulationPass");
    final String CHANGEPASSTO = "shorterPass";
    final String CHANGEROLETO = EDITORROLE;
   
    boolean gotException = false;
    
    assertEquals(true, um.createUser(NEWUSERDATA.id, NEWUSERDATA.role, NEWUSERDATA.pass, false));
    
    // User already present, expect fail
    assertEquals(false, um.createUser(NEWUSERDATA.id, NEWUSERDATA.role, NEWUSERDATA.pass, false));
    
    try {
      assertEquals(true, um.changePassword(NEWUSERDATA.id, NEWUSERDATA.pass, CHANGEPASSTO, false));
    } catch (XynaException e) {
      gotException = true;
    }
    assertFalse(gotException);
    gotException = false;
    
    // wrong password
    try {
      assertEquals(false, um.changePassword(NEWUSERDATA.id, NEWUSERDATA.pass, CHANGEPASSTO, false));
    } catch (XynaException e) {
      gotException = true;
    }
    assertTrue(gotException);
    gotException = false;
    
    try {
      um.authenticate(NEWUSERDATA.id, NEWUSERDATA.pass);
    } catch (XynaException e) {
      gotException = true;
    }
    assertTrue(gotException);
    gotException = false;

    try {
      assertNotNull(um.authenticate(NEWUSERDATA.id, CHANGEPASSTO));
    } catch (Throwable t) {
      fail();
    }

    assertEquals(true, um.changeRole(NEWUSERDATA.id, CHANGEROLETO));
    
    // this should fail because the role is in use
    try {
      System.out.println("###################################################");
      System.out.println("###################################################");
      System.out.println("###################################################");
      um.deleteRole(CHANGEROLETO);
      fail();
    } catch (XFMG_RoleIsAssignedException e) {
      ; // this is expected
    }
    
    assertEquals(true, um.deleteUser(NEWUSERDATA.id));    
  }
  
  
//Test blocking, disabling and enabling
  public void testBlockingAndResetting() throws XynaException {
    final UserData NEWUSERDATA = new UserData("testAccountStatus", ADMINROLE, "testAccountStatusPass");
    final String WRONGPASS = "ThisIsNotYourPassword";
    
    boolean gotException = false;
    
    assertTrue("could not create testAccountStatus-User", um.createUser(NEWUSERDATA.id, NEWUSERDATA.role, NEWUSERDATA.pass, false));
    
    try {
      assertNull(um.authenticate(NEWUSERDATA.id, WRONGPASS));
    } catch (XynaException e) {
      gotException = true;
    }
    assertTrue(gotException);
    
    //this should fail because the user is not blocked
    assertFalse(um.resetPassword(NEWUSERDATA.id, WRONGPASS));
    
    gotException = false;
    try {
      assertNull(um.authenticate(NEWUSERDATA.id, WRONGPASS));
    } catch (XynaException e) {
      gotException = true;
    }
    assertTrue(gotException);
    
    //this should fail because the user is not blocked
    assertFalse(um.resetPassword(NEWUSERDATA.id, WRONGPASS));
    
    gotException = false;
    try {
      assertNull(um.authenticate(NEWUSERDATA.id, WRONGPASS));
    } catch (XynaException e) {
      gotException = true;
    }
    assertTrue(gotException);
    
    // now the User should be blocked
    assertTrue(um.resetPassword(NEWUSERDATA.id, WRONGPASS));
    
    assertTrue(um.deleteUser(NEWUSERDATA.id));
  }
  
  
  public void testBlockingResetAfterAuth() throws XynaException {
    final UserData NEWUSERDATA = new UserData("testAccountStatus", ADMINROLE, "testAccountStatusPass");
    final String WRONGPASS = "ThisIsNotYourPassword";
    
    boolean gotException = false;
    
    assertTrue("could not create testAccountStatus-User", um.createUser(NEWUSERDATA.id, NEWUSERDATA.role, NEWUSERDATA.pass, false));
    
    try {
      assertNull(um.authenticate(NEWUSERDATA.id, WRONGPASS));
    } catch (XynaException e) {
      gotException = true;
    }
    assertTrue(gotException);
    
    //this should fail because the user is not blocked
    assertFalse(um.resetPassword(NEWUSERDATA.id, WRONGPASS));
    
    gotException = false;
    try {
      assertNull(um.authenticate(NEWUSERDATA.id, WRONGPASS));
    } catch (XynaException e) {
      gotException = true;
    }
    assertTrue(gotException);
    
    //this should fail because the user is not blocked
    assertFalse(um.resetPassword(NEWUSERDATA.id, WRONGPASS));
    
    gotException = false;
    try {
      assertNotNull(um.authenticate(NEWUSERDATA.id, NEWUSERDATA.pass));
    } catch (XynaException e) {
      gotException = true;
    }
    assertFalse(gotException);
    
    //this should fail because the user is not blocked
    assertFalse(um.resetPassword(NEWUSERDATA.id, WRONGPASS));
    
    gotException = false;
    try {
      assertNull(um.authenticate(NEWUSERDATA.id, WRONGPASS));
    } catch (XynaException e) {
      gotException = true;
    }
    assertTrue(gotException);
    
    //this should fail because the user is not blocked
    assertFalse(um.resetPassword(NEWUSERDATA.id, WRONGPASS));
    
  }
  
  
  public void testRevokeGrantAfterRestart() throws XynaException {
    assertTrue(um.revokeRightFromRole(ADMINROLE, Rights.START_ORDER.toString()));
    
    User testAdmin = um.authenticate(ADMINDATA.id, ADMINDATA.pass);
    
    assertFalse(um.hasRight(um.resolveFunctionToRight("startOrder"), testAdmin.getRole()));
    
    assertTrue(um.grantRightToRole(ADMINROLE, Rights.START_ORDER.toString()));
    
    testAdmin = um.authenticate(ADMINDATA.id, ADMINDATA.pass);
    
    assertTrue(um.hasRight(um.resolveFunctionToRight("startOrder"), testAdmin.getRole()));
  }
   
  
  final UserData xynaAdmin = new UserData("XYNAADMIN", "ADMIN", "XYNAADMIN");
  final UserData xynaModeller = new UserData("XYNAMODELLER", "MODELLER", "XYNAMODELLER");
  
  public void testPredefinedExistence() throws XynaException {    
    User testUser = null;
    testUser = um.authenticate(xynaAdmin.id, xynaAdmin.pass);
    assertNotNull(testUser);
    assertEquals(xynaAdmin.role, testUser.getRole());
    
    testUser = null;
    testUser = um.authenticate(xynaModeller.id, xynaModeller.pass);
    assertNotNull(testUser);
    assertEquals(xynaModeller.role, testUser.getRole());
    
    // RIGHTS.START_ORDER 
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("startOrder"), xynaModeller.role));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("startOrder"), xynaAdmin.role));

    //RIGHTS.EDIT_MDM
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("saveMDM"), xynaModeller.role));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("saveMDM"), xynaAdmin.role));
    
    //RIGHTS.DEPLOYMENT_MDM
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("deployMDM"), xynaModeller.role));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("deployMDM"), xynaAdmin.role));
    
    //RIGHTS.TRIGGER_FILTER_MANAGEMENT
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("addTrigger"), xynaModeller.role));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("addTrigger"), xynaAdmin.role));
    
    //RIGHTS.MONITORING_LEVEL_MANAGEMENT
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("setDefaultMonitoringLevel"), xynaModeller.role));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("setDefaultMonitoringLevel"), xynaAdmin.role));
    
    //RIGHTS.CAPACITY_MANAGEMENT
    assertEquals(false, um.hasRight(um.resolveFunctionToRight("addCapacity"), xynaModeller.role));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("addCapacity"), xynaAdmin.role));
    
    //RIGHTS.PROCESS_MANUAL_INTERACTION
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("processManualInteractionEntry"), xynaModeller.role));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("processManualInteractionEntry"), xynaAdmin.role));
    
    //Rights.ORDERARCHIVE_MANAGEMENT
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("search"), xynaModeller.role));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("search"), xynaAdmin.role));
    
    //RIGHTS.USER_MANAGEMENT
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("createUser"), xynaModeller.role));
    assertEquals(true, um.hasRight(um.resolveFunctionToRight("createUser"), xynaAdmin.role));
  }
  
  
  public void testRestrictedAccessOnPredefined() throws PersistenceLayerException, XFMG_PredefinedXynaObjectException, XFMG_RoleIsAssignedException, XFMG_UserDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_RightDoesNotExistException, XFMG_NamingConventionException, XFMG_NameContainsInvalidCharacter {
    final String TEMPRIGHT = "de.TemporaryRight";
    
    // delete, changePassword, changeRole, grant, revoke
    try {
      um.deleteRight(Rights.EDIT_MDM.toString());
      fail();
    } catch (XFMG_PredefinedXynaObjectException e) {
      ; //this is expected
    }
    
    try {
      assertFalse(um.deleteRole(xynaAdmin.role));
    } catch (XFMG_PredefinedXynaObjectException e) {
      ; //this is expected
    }
    
    try {
      assertFalse(um.deleteUser(xynaAdmin.id));
    } catch (XFMG_PredefinedXynaObjectException e) {
      ; //this is expected
    }
    
    //This is now allowed
    //assertFalse(um.changePassword(xynaAdmin.id, xynaAdmin.pass, "Bratwurst"));
    //assertFalse(um.changePassword(xynaModeller.id, xynaModeller.pass, "Bratwurst"));
    
    try {
      assertFalse(um.changeRole(xynaAdmin.id, xynaModeller.role));
    } catch (XFMG_PredefinedXynaObjectException e) {
      ; //this is expected
    }
    try {
      assertFalse(um.changeRole(xynaModeller.id, xynaAdmin.role));
    } catch (XFMG_PredefinedXynaObjectException e) {
      ; //this is expected
    }
    
    
    assertTrue(um.createRight(TEMPRIGHT));
    assertTrue(um.grantRightToRole(xynaAdmin.role, TEMPRIGHT));
    assertTrue(um.grantRightToRole(xynaModeller.role, TEMPRIGHT));

    assertFalse(um.revokeRightFromRole(xynaAdmin.role, Rights.EDIT_MDM.toString()));
    assertFalse(um.revokeRightFromRole(xynaModeller.role, Rights.EDIT_MDM.toString()));

    
    assertTrue(um.revokeRightFromRole(xynaAdmin.role, TEMPRIGHT));
    assertTrue(um.revokeRightFromRole(xynaModeller.role, TEMPRIGHT));   
    assertTrue(um.deleteRight(TEMPRIGHT));
  }
  
  
  final static String TESTRIGHT = "de.testRight";
  final static String TESTINVALIDRIGHT = "startOrder";
  
  public void testDynamicRightLifecycle() throws PersistenceLayerException, XFMG_PredefinedXynaObjectException, XFMG_RoleIsAssignedException, XFMG_UserDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_RightDoesNotExistException, XFMG_NamingConventionException, XFMG_NameContainsInvalidCharacter {
    // hasn't got the right, grant it, revoke it, delete it
    assertTrue(um.createRight(TESTRIGHT));
    assertFalse(um.hasRight(TESTRIGHT, ADMINROLE));
    assertTrue(um.grantRightToRole(ADMINROLE, TESTRIGHT));
    assertTrue(um.hasRight(TESTRIGHT, ADMINROLE));
    assertTrue(um.revokeRightFromRole(ADMINROLE, TESTRIGHT));
    assertFalse(um.hasRight(TESTRIGHT, ADMINROLE));
    assertTrue(um.deleteRight(TESTRIGHT));
    
  }
  
  
  final static String TEMPRIGHT1 = "de.TemporaryRightOne";
  final static String TEMPRIGHT2 = "de.TemporaryRightTwo";
  final static String TEMPRIGHT3 = "de.TemporaryRightThree";
  
  // create 3, grant 2, revoke 1, delete 1
  public void testDynamicRightPersistencePart1() throws PersistenceLayerException, XFMG_PredefinedXynaObjectException, XFMG_RoleIsAssignedException, XFMG_UserDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_RightDoesNotExistException, XFMG_NamingConventionException, XFMG_NameContainsInvalidCharacter {
    assertTrue(um.createRight(TEMPRIGHT1));
    assertTrue(um.createRight(TEMPRIGHT2));
    assertTrue(um.createRight(TEMPRIGHT3));
    
    assertTrue(um.grantRightToRole(USERROLE, TEMPRIGHT1));
    assertTrue(um.grantRightToRole(EDITORROLE, TEMPRIGHT2));
    
    assertTrue(um.revokeRightFromRole(EDITORROLE, TEMPRIGHT2));
    
    assertTrue(um.deleteRight(TEMPRIGHT2));
  }
   
  
  // hasRight 2, revoke 1, delete 2
  public void testDynamicRightPersistencePart2() throws PersistenceLayerException, XFMG_PredefinedXynaObjectException, XFMG_RoleIsAssignedException, XFMG_UserDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_RightDoesNotExistException, XFMG_NamingConventionException {
    assertTrue(um.hasRight(TEMPRIGHT1, USERROLE));
    assertFalse(um.hasRight(TEMPRIGHT2, EDITORROLE));
    
    assertTrue(um.revokeRightFromRole(USERROLE, TEMPRIGHT1));
    assertFalse(um.revokeRightFromRole(EDITORROLE, TEMPRIGHT2));
    
    assertTrue(um.deleteRight(TEMPRIGHT1));
    assertTrue(um.deleteRight(TEMPRIGHT3));
  }
  
  
  public void testRevokeOnDelete() throws XynaException {
    assertTrue(um.createRight(TEMPRIGHT1));
    assertTrue(um.createRight(TEMPRIGHT2));
    
    assertTrue(um.grantRightToRole(USERROLE, TEMPRIGHT1));
    assertTrue(um.grantRightToRole(USERROLE, TEMPRIGHT2));
    
    User testUser = um.authenticate(USERDATA.id, USERDATA.pass);
    assertNotNull(testUser);

    assertTrue(um.hasRight(TEMPRIGHT1, testUser.getRole()));
    assertTrue(um.hasRight(TEMPRIGHT2, testUser.getRole()));
    assertTrue(um.hasRight(TEMPRIGHT1, USERROLE));
    assertTrue(um.hasRight(TEMPRIGHT2, USERROLE));
    
    assertTrue(um.deleteRight(TEMPRIGHT2));
    
    testUser = um.authenticate(USERDATA.id, USERDATA.pass);
    assertNotNull(testUser);

    assertTrue(um.hasRight(TEMPRIGHT1, testUser.getRole()));
    assertFalse(um.hasRight(TEMPRIGHT2, testUser.getRole()));
    assertTrue(um.hasRight(TEMPRIGHT1, USERROLE));
    assertFalse(um.hasRight(TEMPRIGHT2, USERROLE));
    
    assertTrue(um.deleteRight(TEMPRIGHT1));
    
    testUser = um.authenticate(USERDATA.id, USERDATA.pass);
    assertNotNull(testUser);

    assertFalse(um.hasRight(TEMPRIGHT1, testUser.getRole()));
    assertFalse(um.hasRight(TEMPRIGHT2, testUser.getRole()));
    assertFalse(um.hasRight(TEMPRIGHT1, USERROLE));
    assertFalse(um.hasRight(TEMPRIGHT2, USERROLE));
  }
  
  
  public void testRightNamingConventions() throws PersistenceLayerException, XFMG_PredefinedXynaObjectException, XFMG_RoleIsAssignedException, XFMG_UserDoesNotExistException, XFMG_RoleDoesNotExistException, XFMG_RightDoesNotExistException, XFMG_NamingConventionException, XFMG_NameContainsInvalidCharacter {
    //Pattern: ^([a-z]+\\.)+[a-zA-Z]+$
    // Success
    final String RIGHT1 = "a.b.c.RECHT";
    final String RIGHT2 = "a.a";
    final String RIGHT3 = "projekt.komponente.RECHT";
    // Fails
    final String RIGHT4 = "a";
    final String RIGHT5 = "A.testRecht";
    final String RIGHT6 = "..recht";
    final String RIGHT7 = "a.!recht";
    final String RIGHT8 = "a.recht~";
    final String RIGHT9 = "a.recht1";
    final String RIGHT10 = "a.re cht";
    final String RIGHT11 = " a.recht";
    final String RIGHT12 = "projekt.kompOnente.recht";
    
    assertTrue(um.createRight(RIGHT1));
    assertTrue(um.createRight(RIGHT2));
    assertTrue(um.createRight(RIGHT3));
    
    //now valid
    assertTrue(um.createRight(RIGHT9));
    
    try {um.createRight(RIGHT4);fail();} catch (XFMG_NamingConventionException e) {;}; 
    try {um.createRight(RIGHT5);fail();} catch (XFMG_NamingConventionException e) {;};
    try {um.createRight(RIGHT6);fail();} catch (XFMG_NamingConventionException e) {;};
    try {um.createRight(RIGHT7);fail();} catch (XFMG_NamingConventionException e) {;};
    try {um.createRight(RIGHT8);fail();} catch (XFMG_NamingConventionException e) {;};    
    try {um.createRight(RIGHT10);fail();} catch (XFMG_NamingConventionException e) {;};
    try {um.createRight(RIGHT11);fail();} catch (XFMG_NamingConventionException e) {;};
    try {um.createRight(RIGHT12);fail();} catch (XFMG_NamingConventionException e) {;};
    
    assertTrue(um.deleteRight(RIGHT1));
    assertTrue(um.deleteRight(RIGHT2));
    assertTrue(um.deleteRight(RIGHT3));
  }
  
  
  private final String DEFAULTDOMAINNAME = "XYNA";
  final String TESTDOMAINNAME = "TestRADIUSDom";
  
  public void testDomainCreationAndModification() throws Exception {    
    assertTrue(um.createDomain(TESTDOMAINNAME, DomainType.RADIUS, 3, 30));
    RADIUSDomainSpecificData data = new RADIUSDomainSpecificData();
    data.setAssociatedOrdertype("");
    List<RADIUSServer> servers = new ArrayList<RADIUSServer>();
    servers.add(new RADIUSServer(new IPv4("192.168.0.1"),
                                 new RADIUSServerPort(1812),
                                 new PresharedKey("Secret")));
    servers.add(new RADIUSServer(new IPv4("192.168.0.2"),
                                 new RADIUSServerPort(1812),
                                 new PresharedKey("VerySecret")));
    servers.add(new RADIUSServer(new IPv4("192.168.0.3"),
                                 new RADIUSServerPort(1812),
                                 new PresharedKey("EvenMoreSecret")));
    data.setServerList(servers);
    assertTrue(um.modifyDomainFieldDomainTypeSpecificData(TESTDOMAINNAME, data));
    
    assertFalse(um.createDomain(TESTDOMAINNAME, DomainType.RADIUS, 3, 30));
    try {
      um.modifyDomainFieldDomainTypeSpecificData("UnexistentDomain", data);
      fail();
    } catch (XFMG_DomainDoesNotExistException e) {
      ;
    }
    
  }
    
  
  public void testDomainDeletionAndFailureOnAssignedDomains() throws Exception {
    List<String> testDomList = new ArrayList<String>();
    testDomList.add(TESTDOMAINNAME);
    assertTrue(um.modifyUserFieldDomains(USERDATA.id, testDomList));
    
    try {
      um.deleteDomain(TESTDOMAINNAME);
      fail();
    } catch (XFMG_DomainIsAssignedException e) {
      ;
    }
    
    List<String> defaultDomList = new ArrayList<String>();
    defaultDomList.add(DEFAULTDOMAINNAME);
    assertTrue(um.modifyUserFieldDomains(USERDATA.id, defaultDomList));
    
    assertTrue(um.createRole(USERROLE, TESTDOMAINNAME));
    
    try {
      um.deleteDomain(TESTDOMAINNAME);
      fail();
    } catch (XFMG_DomainIsAssignedException e) {
      ;
    }
    assertTrue(um.deleteRole(USERROLE,TESTDOMAINNAME));
    
    assertTrue(um.deleteDomain(TESTDOMAINNAME));
  }
    
    
  public void testAuthentificationMethodGeneration() throws Exception {
    final String TESTDOMAIN1 = "testDom1";
    final String TESTDOMAIN2 = "testDom2";
    final String TESTDOMAIN3 = "testDom3";
    
    assertTrue(um.createDomain(TESTDOMAIN1, DomainType.RADIUS, 1, 10));
    assertTrue(um.createDomain(TESTDOMAIN2, DomainType.RADIUS, 1, 10));
    assertTrue(um.createDomain(TESTDOMAIN3, DomainType.RADIUS, 1, 10));
    
    RADIUSDomainSpecificData data = new RADIUSDomainSpecificData();
    data.setAssociatedOrdertype("xfmg.xopctrl.radius.TestAuthWF1");
    List<RADIUSServer> servers = new ArrayList<RADIUSServer>();
    servers.add(new RADIUSServer(new IPv4("192.168.0.1"),
                                 new RADIUSServerPort(1812),
                                 new PresharedKey("Secret")));
    data.setServerList(servers);
    assertTrue(um.modifyDomainFieldDomainTypeSpecificData(TESTDOMAIN1, data));
    
    data.setAssociatedOrdertype("xfmg.xopctrl.radius.TestAuthWF2");
    servers.clear();
    servers.add(new RADIUSServer(new IPv4("192.168.0.11"),
                                 new RADIUSServerPort(1812),
                                 new PresharedKey("Secret")));
    servers.add(new RADIUSServer(new IPv4("192.168.0.12"),
                                 new RADIUSServerPort(1812),
                                 new PresharedKey("VerySecret")));
    data.setServerList(servers);
    assertTrue(um.modifyDomainFieldDomainTypeSpecificData(TESTDOMAIN2, data));
    
    data.setAssociatedOrdertype("xfmg.xopctrl.radius.TestAuthWF3");
    servers.clear();
    servers.add(new RADIUSServer(new IPv4("192.168.0.21"),
                                 new RADIUSServerPort(1812),
                                 new PresharedKey("Secret")));
    servers.add(new RADIUSServer(new IPv4("192.168.0.22"),
                                 new RADIUSServerPort(1812),
                                 new PresharedKey("VerySecret")));
    servers.add(new RADIUSServer(new IPv4("192.168.0.23"),
                                 new RADIUSServerPort(1812),
                                 new PresharedKey("EvenMoreSecret")));
    data.setServerList(servers);
    assertTrue(um.modifyDomainFieldDomainTypeSpecificData(TESTDOMAIN2, data));
    
    List<String> domains = new ArrayList<String>();
    domains.add(TESTDOMAIN1);
    domains.add(DEFAULTDOMAINNAME);
    domains.add(TESTDOMAIN2);
    domains.add(TESTDOMAIN3);
    domains.add(DEFAULTDOMAINNAME);
    
    assertTrue(um.modifyUserFieldDomains(USERDATA.id, domains));
    
    List<UserAuthentificationMethod> methods = UserAuthentificationMethod.generateAuthenticationMethods(USERDATA.id);
    assertTrue(methods.get(0) instanceof RADIUSUserAuthentication);
    assertTrue(methods.get(1) instanceof LocalUserAuthentication);
    assertTrue(methods.get(2) instanceof RADIUSUserAuthentication);
    assertTrue(methods.get(3) instanceof RADIUSUserAuthentication);
    assertTrue(methods.get(4) instanceof LocalUserAuthentication);
  }
  
}
