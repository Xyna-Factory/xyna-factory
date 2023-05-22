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


import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateSessionException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownSessionIDException;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xfmg.xopctrl.managedsessions.ManagedSession;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionDetails;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.AChangeEvent;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.AChangeNotificationListener;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.ANotificationConnection;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.MdmModificationChangeEvent;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.MdmModificationChangeListener;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.ProcessProgressChangeEvent;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.ProcessProgressChangeListener;
import com.gip.xyna.xfmg.xopctrl.managedsessions.priviliges.MDMModificationPrivilige;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmlshell.XynaXMLShellPersistenceLayer;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;


public class SessionManagementTest extends TestCase {

  SessionManagement sm;
  ODSImpl ods;
  Role testRole;
  
  protected void setUp() throws Exception {
    super.setUp();
    
    ods = ODSImpl.getInstance(false);
    ods.registerPersistenceLayer(33, XynaXMLShellPersistenceLayer.class);
    String[] test = {"test"};
    long id = ods.instantiatePersistenceLayerInstance(33, "test", ODSConnectionType.DEFAULT, test);
    ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
    
    
    UserManagement um = EasyMock.createMock(UserManagement.class);
    EasyMock.expect(um.resolveRole(EasyMock.isA(String.class))).andReturn(new Role("testRole","testDomain")).anyTimes();
      
    XynaOperatorControl xopctrl = EasyMock.createMock(XynaOperatorControl.class);
    EasyMock.expect(xopctrl.getUserManagement()).andReturn(um).anyTimes();
    
    DependencyRegister depReg = EasyMock.createMock(DependencyRegister.class);
    EasyMock.expect(depReg.addDependency(EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class), EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class))).andReturn(true).anyTimes();
    
    XynaFactoryControl xfc = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfc.getDependencyRegister()).andReturn(depReg).anyTimes();
    
    Configuration conf = EasyMock.createMock(Configuration.class);
    conf.addPropertyChangeListener(EasyMock.isA(IPropertyChangeListener.class));
    EasyMock.expectLastCall();
    
    XynaFactoryManagementODS xfmods = EasyMock.createMock(XynaFactoryManagementODS.class);
    EasyMock.expect(xfmods.getConfiguration()).andReturn(conf).anyTimes();
    
    XynaFactoryManagementBase xfm = EasyMock.createMock(XynaFactoryManagementBase.class);
    EasyMock.expect(xfm.getXynaOperatorControl()).andReturn(xopctrl).anyTimes();
    EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfc).anyTimes();
    EasyMock.expect(xfm.getXynaFactoryManagementODS()).andReturn(xfmods).anyTimes();
    EasyMock.expect(xfm.getProperty(EasyMock.isA(String.class))).andReturn("2").anyTimes();
    
    XynaProcessingODS pods = EasyMock.createMock(XynaProcessingODS.class);
    EasyMock.expect(pods.getODS()).andReturn(ods).anyTimes();
    
    XynaProcessingBase xproc = EasyMock.createMock(XynaProcessingBase.class);
    EasyMock.expect(xproc.getXynaProcessingODS()).andReturn(pods).anyTimes();
    
    XynaFactoryBase xf = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfm).anyTimes();
    EasyMock.expect(xf.getFactoryManagementPortal()).andReturn(xfm).anyTimes();
    EasyMock.expect(xf.getProcessing()).andReturn(xproc).anyTimes();
    /*xf.addComponentToBeInitializedLater(EasyMock.isA(UserManagement.class));
    EasyMock.expectLastCall();*/
    xf.addComponentToBeInitializedLater(EasyMock.isA(SessionManagement.class));
    EasyMock.expectLastCall();
    
    XynaFactory.setInstance(xf);
    
    EasyMock.replay(conf, xfmods, um, xopctrl, depReg, xfc, xfm, pods, xproc, xf);
    
    sm = new SessionManagement();
    sm.initInternally();
  }


  protected void tearDown() throws Exception {
    ODSConnection con = ods.openConnection();
    try {
      con.deleteAll(ManagedSession.class);
    } finally {
      con.closeConnection();
    }
    sm.shutDownInternally();
    ODSImpl.clearInstances();
    super.tearDown();    
  }
  
  
  public void testAcquisitionAndKeepAlive() throws XynaException {
    // No need to change this anymore, we return 2000 for the XynaProperty (see setup)
    //sm.DEFAULT_GUI_SESSION_TIMEOUT_MILLISECONDS = 2000L;
    testRole = new Role("testRole", "testDomain");
    testRole.grantRight(Rights.EDIT_MDM.toString());
    User testUser = new User("testUser", testRole.getName(), "testUserPass", false);
    
    SessionCredentials testSession = sm.getNewSession(testUser, false);
    String testId = testSession.getSessionId();
    String testToken = testSession.getToken();
    
    assertNotNull(testSession); 
    sm.authenticateSession(testId, testToken);
    assertTrue(sm.keepAlive(testId));
    assertNotNull(sm.getRole(testId));

    
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      fail("Thread interrupted while sleeping");
    }
    
    assertNotNull(sm.getRole(testId));
    sm.authenticateSession(testId, testToken);
    assertTrue(sm.keepAlive(testId));
   
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      fail("Thread interrupted while sleeping");
    }
    
    
    try {
      sm.authenticateSession(testId, testToken);
      fail("Call is not supposed to succeed");
    } catch (XynaException e) {
      ;
    }
    assertFalse(sm.keepAlive(testId));
    //assertNull(sm.getRole(testId));
    try {
      sm.getRole(testId);
      fail("you are supposed to fail");
    } catch (XynaException e) {
      ;
    }
    
    
    //invalid id
    assertFalse(sm.keepAlive("-1"));
  }
  
  
  public void testAcquisitionAndQuit() throws PersistenceLayerException, XFMG_UnknownSessionIDException, XFMG_DuplicateSessionException {    
    testRole = new Role("testRole","testDomain");
    testRole.grantRight(Rights.EDIT_MDM.toString());
    User testUser = new User("testUser", testRole.getName(), "testUserPass", false);
    
    SessionCredentials testSession = sm.getNewSession(testUser, false);
    String testId = testSession.getSessionId();
    
    assertNotNull(testSession);
    assertNotNull(sm.getRole(testId));
    
    sm.quitSession(testId);
    
    assertFalse(sm.keepAlive(testId));
    //assertNull(sm.getRole(testId));
    try {
      sm.getRole(testId);
      fail("you are supposed to fail");
    } catch (XynaException e) {
      ;
    }
  }
  
  
  public void testTestPriviligeAquisition() throws  PersistenceLayerException, XFMG_DuplicateSessionException {
    testRole = new Role("testRole","testDomain");
    testRole.grantRight(Rights.EDIT_MDM.toString());
    User testUser = new User("testUser", testRole.getName(), "testUserPass", false);
    
    SessionCredentials testSession1 = sm.getNewSession(testUser, false);
    String testId1 = testSession1.getSessionId();
    assertNotNull(testSession1);
    
    SessionCredentials testSession2 = sm.getNewSession(testUser, false);
    String testId2 = testSession2.getSessionId();
    
    assertNotNull(testSession2);   
    assertNotSame(testId1, testId2);
    
    TestPrivilege testPrivilege1 = new TestPrivilege("MyConflict");
    try {
      assertTrue(sm.requestSessionPriviliges(testId1, testPrivilege1));
    } catch (XynaException e) {
      fail("ManagedSessionException while requesting privilege");
    }
    
    TestPrivilege testPrivilege2 = new TestPrivilege("MyConflict");
    try {
      assertFalse(sm.requestSessionPriviliges(testId2, testPrivilege2));
    } catch (XynaException e) {
      fail();
    }
    
    testPrivilege2 = new TestPrivilege("MyOwnConflict");
    try {
      assertTrue(sm.requestSessionPriviliges(testId2, testPrivilege2));
    } catch (XynaException e) {
      fail("ManagedSessionException while requesting privilege");
    }
    
    assertTrue(sm.releaseSessionPriviliges(testId1));
    assertTrue(sm.releaseSessionPriviliges(testId2, testPrivilege2));
    
    //should fail with invalid id
    assertFalse(sm.releaseSessionPriviliges("-1"));
    assertFalse(sm.releaseSessionPriviliges("-1", testPrivilege2));    
  }

  
  public void testMDMModPriviligeAquisition() throws PersistenceLayerException, XFMG_DuplicateSessionException {
    final String TESTWFNAME = "testWF";
    final String TESTSERVNAME = "testService";
    final String TESTDTNAME = "testDatatype";
    
    testRole = new Role("testRole","testDomain");
    testRole.grantRight(Rights.EDIT_MDM.toString());
    User testUser = new User("testUser", testRole.getName(), "testUserPass", false);
    
    SessionCredentials testSession1 = sm.getNewSession(testUser, false);
    String testId1 = testSession1.getSessionId();
    
    assertNotNull(testSession1);
    
    SessionCredentials testSession2 = sm.getNewSession(testUser, false);
    String testId2 = testSession2.getSessionId();
    
    assertNotNull(testSession2);    
    assertNotSame(testId1, testId2);
    
    List<String> affectedFullyQualifiedNames = new ArrayList<String>();
    affectedFullyQualifiedNames.add(TESTWFNAME);
    affectedFullyQualifiedNames.add(TESTSERVNAME);
    affectedFullyQualifiedNames.add(TESTDTNAME);
    
    MDMModificationPrivilige mdmPriv = new MDMModificationPrivilige(affectedFullyQualifiedNames);

    sm.requestSessionPriviliges(testId1, mdmPriv);

    
    List<String> afqn1 = new ArrayList<String>();
    afqn1.add(TESTWFNAME);
    afqn1.add(TESTSERVNAME);
    afqn1.add(TESTDTNAME);
    
    mdmPriv = new MDMModificationPrivilige(afqn1);

    //should fail for another session and be allowed for the session already holding a privilege for the locked fqNames 
    assertFalse(sm.requestSessionPriviliges(testId2, mdmPriv));
    assertTrue(sm.requestSessionPriviliges(testId1, mdmPriv));

    
    List<String> afqn2 = new ArrayList<String>();
    afqn2.add(TESTWFNAME);
    mdmPriv = new MDMModificationPrivilige(afqn2);

    assertFalse(sm.requestSessionPriviliges(testId2, mdmPriv));
    assertTrue(sm.requestSessionPriviliges(testId1, mdmPriv));


    
    List<String> afqn3 = new ArrayList<String>();
    afqn3.add(TESTSERVNAME);
    mdmPriv = new MDMModificationPrivilige(afqn3);

    assertFalse(sm.requestSessionPriviliges(testId2, mdmPriv));
    assertTrue(sm.requestSessionPriviliges(testId1, mdmPriv));

    
    List<String> afqn4 = new ArrayList<String>();
    afqn4.add(TESTDTNAME);
    mdmPriv = new MDMModificationPrivilige(afqn4);

    assertFalse(sm.requestSessionPriviliges(testId2, mdmPriv));
    assertTrue(sm.requestSessionPriviliges(testId1, mdmPriv));
    
    final String UNRELATED_MDMNAME = "unrelatedMDM";
    List<String> afqn5 = new ArrayList<String>();
    afqn5.add(UNRELATED_MDMNAME);
    mdmPriv = new MDMModificationPrivilige(afqn5);
    assertEquals(true, sm.requestSessionPriviliges(testId2, mdmPriv));
  }
  
  
  private class TestConnection extends ANotificationConnection {

    private List<String> reply = new ArrayList<String>();
    private int replyLength = 0;
    
    @Override
    public void reply(String s) {
      reply.add(s);
      replyLength++;
    }

    @Override
    public void replyList(int i) {
      replyLength = i;      
    }   
    
    public String getReply() {
      if (replyLength > 1 || replyLength == 0) {
        fail("found invalid reply in TestConnection");
      } else {
        return reply.get(0);
      }
      return null;
    }    
    
    public List<String> getReplyList() {
      if (replyLength <= 1) {
        fail("found invalid reply in TestConnection");
      } else {
        return reply;
      }
      return null;
    }
    
    public void clear() {
      reply.clear();
      replyLength = 0;
    }
    
    public boolean isEmpty() {
      if (replyLength == 0 && reply.size() == 0) {
        return true;
      } else {
        return false;
      }
    }
  }
  
 public class DummyEvent extends AChangeEvent {
    
  }
  
  public class TestChangeEvent extends AChangeEvent {
    
    public String message;
    
    public TestChangeEvent(String yourMessage) {
      this.message = yourMessage;
    }
  }
  
  public class TestListChangeEvent extends AChangeEvent {
    
    public List<String> message;
    
    public TestListChangeEvent(List<String> yourMessage) {
      this.message = yourMessage;
    }
  }
  
  public class TestNotificationListener extends AChangeNotificationListener {

    private ANotificationConnection con;
    
    public TestNotificationListener(ANotificationConnection connection) {
      this.con = connection;
    }
    
    @Override
    public void onChange(AChangeEvent event) {
      if (event instanceof TestChangeEvent) {
        con.reply(((TestChangeEvent)event).message);
      } else if (event instanceof TestListChangeEvent) {
        int messLen = ((TestListChangeEvent)event).message.size();
        con.replyList(messLen);
        for (int i = 0; i < messLen; i++) {
          con.reply(((TestListChangeEvent)event).message.get(i));          
        }       
      } else {
        fail("Listener received unexpected match");
      }      
    }

    @Override
    public boolean matches(AChangeEvent event) {
      if (event instanceof TestChangeEvent || event instanceof TestListChangeEvent) {
        return true;
      } else {
        return false;
      }
    }
    
  }
  
  public void testTestNotification() throws PersistenceLayerException, XFMG_DuplicateSessionException {
    testRole = new Role("testRole","testDomain");
    testRole.grantRight(Rights.EDIT_MDM.toString());
    User testUser = new User("testUser", testRole.getName(), "testUserPass", false);
    
    SessionCredentials testSession = sm.getNewSession(testUser, false);
    String testId = testSession.getSessionId();
    
    assertNotNull(testSession);
    
    TestConnection tCon = new TestConnection();
    
    TestNotificationListener tNotLis = new TestNotificationListener(tCon);
    
    Long remId = sm.signupSessionForNotification(testId, tNotLis);
    assertNotSame(-1L, remId);
    
    final String MESSAGE = "Single Event occured";
    TestChangeEvent testEvent = new TestChangeEvent(MESSAGE);
    sm.performChangeNotification(testEvent);
    
    assertEquals(MESSAGE, tCon.getReply());
    tCon.clear();
    
    remId = sm.signupSessionForNotification(testId, tNotLis);
    assertNotSame(-1L, remId);
    
    final String MESSAGE0 = "List Event occured";
    final String MESSAGE1 = "First List Element";
    final String MESSAGE2 = "Second List Element";
    final String MESSAGE3 = "Third List Element";
    List<String> testList = new ArrayList<String>();
    testList.add(MESSAGE0);
    testList.add(MESSAGE1);
    testList.add(MESSAGE2);
    testList.add(MESSAGE3);
    TestListChangeEvent testListEvent = new TestListChangeEvent(testList);
    sm.performChangeNotification(testListEvent);    
  
    
    List<String> replyList = tCon.getReplyList();
    assertEquals(4, replyList.size());
    assertEquals(MESSAGE0, replyList.get(0));
    assertEquals(MESSAGE1, replyList.get(1));
    assertEquals(MESSAGE2, replyList.get(2));
    assertEquals(MESSAGE3, replyList.get(3));
    tCon.clear();
    
    remId = sm.signupSessionForNotification(testId, tNotLis);
    assertNotSame(-1L, remId);
    
    DummyEvent dummy = new DummyEvent();
    sm.performChangeNotification(dummy);
    
    assertTrue(tCon.isEmpty());
    
    sm.removeNotificationListener(testId, remId);
    
    //Listener should be unregistered
    sm.performChangeNotification(testEvent);
    assertTrue(tCon.isEmpty());
    
    sm.performChangeNotification(testListEvent);
    assertTrue(tCon.isEmpty());
    
    remId = sm.signupSessionForNotification(testId, tNotLis);
    // Listener should only receive one event
    sm.performChangeNotification(testEvent);
    assertFalse(tCon.isEmpty());
    tCon.clear();
    sm.performChangeNotification(testEvent);
    assertTrue(tCon.isEmpty());   

  }
  
  public void testMDMNotification() throws PersistenceLayerException, XFMG_DuplicateSessionException {
    testRole = new Role("testRole","testDomain");
    testRole.grantRight(Rights.EDIT_MDM.toString());
    User testUser = new User("testUser", testRole.getName(), "testUserPass", false);
    
    SessionCredentials testSession = sm.getNewSession(testUser, false);
    String testId = testSession.getSessionId();
    
    assertNotNull(testSession);
    
    TestConnection tCon = new TestConnection();
    
    MdmModificationChangeListener MdmLis = new MdmModificationChangeListener(tCon);
    
    Long remId = sm.signupSessionForNotification(testId, MdmLis);
    assertNotSame(-1L, remId);
    
    final String MDMNAME1 = "com.gip.xyna.testObj1";
    final String MDMNAME2 = "com.gip.xyna.testObj2";
    final String MDMNAME3 = "com.gip.xyna.testObj3";
        
    List<String> testList = new ArrayList<String>();
    testList.add(MDMNAME1);
    testList.add(MDMNAME2);
    testList.add(MDMNAME3);
    MdmModificationChangeEvent MdmEvent = new MdmModificationChangeEvent(testList);
    sm.performChangeNotification(MdmEvent);
    
    assertFalse(tCon.isEmpty());
    List<String> replyList= tCon.getReplyList();
    assertEquals(AChangeEvent.CHANGE_EVENT_MDM_MODIFICATION ,replyList.get(0));
    assertEquals(MDMNAME1 ,replyList.get(1));
    assertEquals(MDMNAME2 ,replyList.get(2));
    assertEquals(MDMNAME3 ,replyList.get(3));
    tCon.clear();   
        
  }
  
  public void testWFNotification() throws  PersistenceLayerException, XFMG_DuplicateSessionException {
    testRole = new Role("testRole","testDomain");
    testRole.grantRight(Rights.EDIT_MDM.toString());
    User testUser = new User("testUser", testRole.getName(), "testUserPass", false);
    
    SessionCredentials testSession = sm.getNewSession(testUser, false);
    String testId = testSession.getSessionId();
    
    assertNotNull(testSession);
    
    TestConnection tCon = new TestConnection();
    
    final Long orderId = new Long(1L);
    final Long otherOrderId = new Long(2L);
    ProcessProgressChangeListener procLis = new ProcessProgressChangeListener(tCon, orderId);
    
    Long remId = sm.signupSessionForNotification(testId, procLis);
    assertNotSame(-1L, remId);
    
    ProcessProgressChangeEvent ppce = new ProcessProgressChangeEvent(orderId);
    sm.performChangeNotification(ppce);
    
    assertFalse(tCon.isEmpty());
    List<String> replyList= tCon.getReplyList();
    assertEquals(AChangeEvent.CHANGE_EVENT_PROCESS_PROGRESS, replyList.get(0));
    assertEquals(orderId.toString(), replyList.get(1));
    tCon.clear();    
    
    remId = sm.signupSessionForNotification(testId, procLis);
    assertNotSame(-1L, remId);
    
    ppce = new ProcessProgressChangeEvent(otherOrderId);
    sm.performChangeNotification(ppce);
    assertTrue(tCon.isEmpty());
  }
  
  
  //test authentication
  public void testAuthentication() throws XynaException {
    testRole = new Role("testRole","testDomain");
    testRole.grantRight(Rights.EDIT_MDM.toString());
    User testUser = new User("testUser", testRole.getName(), "testUserPass", false);
    
    SessionCredentials testSession = sm.getNewSession(testUser, false);
    String testToken = testSession.getToken();
    String testId = testSession.getSessionId();
    
    Role testAuthenticatedRole = sm.authenticateSession(testId, testToken);
    assertNotNull(testAuthenticatedRole);
    assertEquals(testRole.getName(), testAuthenticatedRole.getName());
    
    try {
      testAuthenticatedRole = sm.authenticateSession(testId, "InvalidToken");
      fail("Authentication should not have been successfull");
    } catch (XynaException e) {
      ;
    }
  }
  
  
  //test deletion on timeout & quit
  public void testDeletionOnTimeoutAndQuit() throws XynaException {
    testRole = new Role("testRole","testDomain");
    testRole.grantRight(Rights.EDIT_MDM.toString());
    User testUser = new User("testUser", testRole.getName(), "testUserPass", false);
    
    SessionCredentials testSession = sm.getNewSession(testUser, false);
    String testId = testSession.getSessionId();
    String testToken = testSession.getToken();
    sm.quitSession(testId);
    
    ODSConnection con = ods.openConnection();
    try {
      assertEquals(false, con.containsObject(new ManagedSession(testSession.getSessionId(), null, null)));
    } finally {
      con.closeConnection();
    }
    
    testSession = sm.getNewSession(testUser, false);
    testId = testSession.getSessionId();
    testToken = testSession.getToken();
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      fail("Thread interrupted while sleeping");
    }
    
    try {
      sm.authenticateSession(testId, testToken);
      fail("Call is not supposed to suceed");
    } catch (XynaException e) {
      ;
    }
    
    con = ods.openConnection();
    try {
      assertEquals(false, con.containsObject(new ManagedSession(testId, null, null)));
    } finally {
      con.closeConnection();
    }
  }
  
  //test getDetails
  public void testGetDetails() throws XynaException {

    final String ROLENAME = "testRole";
    testRole = new Role(ROLENAME, "testDomain");
    testRole.grantRight(Rights.EDIT_MDM.toString());
    testRole.grantRight(Rights.PERSISTENCE_MANAGEMENT.toString());
    testRole.grantRight(Rights.MONITORING_LEVEL_MANAGEMENT.toString());
    User testUser = new User("testUser", testRole.getName(), "testUserPass", false);
    
    SessionCredentials testSession = sm.getNewSession(testUser, false);
    assertNotNull(testSession);
    String testId = testSession.getSessionId();
    
    SessionDetails testDetails = sm.getSessionDetails(testId);
    //assertEquals(ROLENAME, testDetails.getRole());
    //Set<String> testRights = testDetails.getRights();
    //assertEquals(3, testRights.size());
    //assertEquals(true, testRights.contains(Rights.EDIT_MDM.toString()));
    //assertEquals(true, testRights.contains(Rights.PERSISTENCE_MANAGEMENT.toString()));
    //assertEquals(true, testRights.contains(Rights.MONITORING_LEVEL_MANAGEMENT.toString()));
    assertEquals(testId, testDetails.getSessionId());
    assertEquals(true, testDetails.getStartTime() <= System.currentTimeMillis());
    
  }
  
}
